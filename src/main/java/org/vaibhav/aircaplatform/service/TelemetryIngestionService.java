package org.vaibhav.aircaplatform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.vaibhav.aircaplatform.dto.RcaReport;
import org.vaibhav.aircaplatform.dto.TelemetryMessage;
import org.vaibhav.aircaplatform.model.Incident;
import org.vaibhav.aircaplatform.model.RcaReportEntity;
import org.vaibhav.aircaplatform.repository.IncidentRepository;
import org.vaibhav.aircaplatform.repository.RcaReportRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetryIngestionService {

    private final IncidentRepository incidentRepository;
    private final RcaReportRepository rcaReportRepository;
    private final RcaOrchestratorService rcaOrchestratorService;
    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "app.telemetry.raw", groupId = "rca-core-consumers")
    public void processIncomingTelemetry(String rawJsonPayload) {
        try {
            // 1. Parse incoming log entry packet
            TelemetryMessage telemetry = objectMapper.readValue(rawJsonPayload, TelemetryMessage.class);

            log.info("Received telemetry packet from service [{}] with trace [{}]", telemetry.service(), telemetry.traceId());

            // 2. Filter for active exceptional system limits or exceptions
            if (telemetry.exception() != null && !telemetry.exception().isBlank()) {

                // Avoid duplicating active incidents for the same distributed trace context ID
                if (incidentRepository.findByTraceId(telemetry.traceId()).isEmpty()) {

                    Incident liveIncident = Incident.builder()
                            .id(UUID.randomUUID().toString())
                            .traceId(telemetry.traceId())
                            .rootService(telemetry.service())
                            .triggerException(telemetry.exception())
                            .status("OPEN")
                            .build();

                    incidentRepository.save(liveIncident);
                    log.warn("🚨 New Incident generated for Trace context: {}. Initiating state capture.", telemetry.traceId());

                    RcaReport report = rcaOrchestratorService.runAnalysis(telemetry.exception(), telemetry.message(), telemetry.service());

                    // 3. Save the structured analysis output to rca_reports table
                    RcaReportEntity entity = RcaReportEntity.builder()
                            .id(UUID.randomUUID().toString())
                            .incident(liveIncident)
                            .rootCause(report.rootCause())
                            .confidenceScore(report.confidence())
                            .impactedServices(String.join(",", report.affectedServices()))
                            .remediations(String.join(";", report.recommendation()))
                            .build();
                    rcaReportRepository.save(entity);
                    log.info("✅ Structured RCA Report successfully generated for incident: {}", liveIncident.getId());

                    // 4. FEEDBACK LOOP: Seed the vector store so the system learns from this incident
                    String vectorSummary = String.format("Log: %s | Cause: %s | Fix: %s",
                            telemetry.message(), report.rootCause(), String.join("; ", report.recommendation()));
                    vectorStore.add(List.of(new Document(vectorSummary, Map.of("service", telemetry.service()))));
                    log.info("🧠 Fed incident pattern back into pgvector knowledge base.");
                }
            }
        } catch (Exception ex) {
            log.error("Failed to process string payload from stream topology block: ", ex);
        }
    }
}
