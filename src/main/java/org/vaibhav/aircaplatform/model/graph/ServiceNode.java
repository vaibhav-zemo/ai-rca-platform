package org.vaibhav.aircaplatform.model.graph;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import lombok.*;
import java.util.HashSet;
import java.util.Set;

@Node("Service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceNode {

    @Id
    private String name; // e.g., "account-service"

    @Relationship(type = "CALLS", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<ServiceNode> downstreamServices = new HashSet<>();
}