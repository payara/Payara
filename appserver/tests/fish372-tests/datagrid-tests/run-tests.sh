mvn -DskipITs -DskipPostIntegrationTest clean install
mvn -DskipPreIntegrationTest -DskipPostIntegrationTest verify
mvn -DskipITs -DskipPreIntegrationTest post-integration-test