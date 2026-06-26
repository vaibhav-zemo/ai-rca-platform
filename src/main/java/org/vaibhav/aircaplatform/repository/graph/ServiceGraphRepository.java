package org.vaibhav.aircaplatform.repository.graph;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;
import org.vaibhav.aircaplatform.model.graph.ServiceNode;

import java.util.List;

@Repository
public interface ServiceGraphRepository extends Neo4jRepository<ServiceNode, String> {

}