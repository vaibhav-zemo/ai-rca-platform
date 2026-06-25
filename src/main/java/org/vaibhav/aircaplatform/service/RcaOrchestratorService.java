package org.vaibhav.aircaplatform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.vaibhav.aircaplatform.dto.RcaReport;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RcaOrchestratorService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    public RcaOrchestratorService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
    }

    public RcaReport runAnalysis(String triggerException, String rawLogMessage) {
        log.info("Running Semantic RAG Lookup for Exception: {}", triggerException);

        // 1. Query pgvector for historically similar incidents
        List<Document> similarIncidents = vectorStore.similaritySearch(
                SearchRequest.builder().query(rawLogMessage)
                        .topK(2)
                        .build()
        );

        String ragContext = similarIncidents.isEmpty() ?
                "No historical matching incidents found for this signature." :
                similarIncidents.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n---\n"));

        log.info("Executing Gemini Structured Analysis Loop...");

        // 2. Execute Prompt Engineering Strategy with Structured output mapping
        return this.chatClient.prompt()
                .system(sp -> sp.text("""
                    You are an expert Reliability Engineer. Analyze the incoming error log.
                    
                    Use the following historical resolutions as a baseline context reference:
                    {rag_context}
                    """)
                        .param("rag_context", ragContext))
                .user(u -> u.text("Analyze this diagnostic signature: {log}").param("log", rawLogMessage))
                .call()
                .entity(RcaReport.class); // Assures type-safe JSON mapping straight from Gemini
    }
}
