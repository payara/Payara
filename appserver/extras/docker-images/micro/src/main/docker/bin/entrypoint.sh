#!/bin/sh
set -e

java -XX:MaxRAMPercentage=${MEM_MAX_RAM_PERCENTAGE} -Xss${MEM_XSS} -XX:+UseContainerSupport ${JVM_ARGS} -jar payara-micro.jar --deploymentDir "${DEPLOY_DIR}"
