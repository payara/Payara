# JCache REST Test

This test verifies JCache functionality in a clustered Payara Micro environment using TestContainers.

## Prerequisites

- Java 11 or later
- Maven 3.6.0 or later
- Docker (for running TestContainers)

## Test Overview

The test performs the following:
1. Sets up a cluster of 3 Payara Micro instances using TestContainers
2. Deploys a test web application (`jcache-rest.war`) to each instance
3. Verifies that cache operations are properly synchronized across the cluster

The test application (`jcache-rest.war`) is compiled from the [Payara Examples repository](https://github.com/payara/Payara-Examples/tree/main/javaee/jcache/jcache-rest).

## Running the Test

1. Ensure Docker is running on your system
2. Navigate to the test directory:
   ```bash
   cd appserver/extras/docker-images/tests/src/test/java/fish/payara/jcache-test
   ```
3. Run the test using Maven:
   ```bash
   mvn clean test -Dpayara.version=6.2025.9
   ```

## Test Details

The test uses the following components:
- Payara Micro 6.2025.8 (Docker image: `payara/micro:6.2025.8`)
- TestNG for test execution
- Java 11+ HTTP Client for making REST calls
- TestContainers for managing the Docker containers with Payara Micro

## Test Endpoints

The test verifies the following REST endpoints:
- `PUT /jcache-rest/api/cache/{key}` - Store a value in the cache
- `GET /jcache-rest/api/cache/{key}` - Retrieve a value from the cache
- `DELETE /jcache-rest/api/cache/{key}` - Remove a value from the cache
