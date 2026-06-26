package org.vaibhav.aircaplatform.config;


import org.vaibhav.aircaplatform.model.graph.ServiceNode;
import org.vaibhav.aircaplatform.repository.graph.ServiceGraphRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class GraphDataInitializer implements CommandLineRunner {

    private final Driver neo4jDriver; // Inject native configuration channel directly

    @Override
    public void run(String... args) throws Exception {
        log.info("Seeding Neo4j Microservice Topology Map via Native Cypher...");

        try (Session session = neo4jDriver.session()) {
            // 1. Wipe the current database state clean
            session.run("MATCH (n) DETACH DELETE n");
            log.info("Neo4j database wiped clean.");

            // 2. Build the graph topology chain explicitly: customer-service -> account-service -> notification-service
            String seedQuery = """
                MERGE (cust:Service {name: 'customer-service'})
                MERGE (acct:Service {name: 'account-service'})
                MERGE (notif:Service {name: 'notification-service'})
                
                MERGE (cust)-[:CALLS]->(acct)
                MERGE (acct)-[:CALLS]->(notif)
                """;

            session.run(seedQuery);
            log.info("✅ Neo4j Topology Mesh Seeded Successfully using Native Session Mapping.");

        } catch (Exception e) {
            log.error("❌ Failed to execute native Neo4j seeding operations: ", e);
        }
    }
}
