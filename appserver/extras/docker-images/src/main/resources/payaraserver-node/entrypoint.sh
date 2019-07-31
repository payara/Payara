#!/usr/bin/env bash
set -e

# Check if we actually have an instance name. If we don't have an instance name, we can assume that we need to create an instance from scratch
if [ -z ${PAYARA_INSTANCE_NAME} ]; then
    PAYARA_INSTANCE_NAME = ./payara5/bin/asadmin -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} --passwordfile ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --dockernode true
else
    # Check if instance already created before running create command
    if [ ! -d "payara5/glassfish/nodes/${PAYARA_NODE_NAME}/${PAYARA_INSTANCE_NAME}" ]; then
        ./payara5/bin/asadmin -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} _create-instance-filesystem --node ${PAYARA_NODE_NAME} --dockernode true ${PAYARA_INSTANCE_NAME}
    fi
fi

# Start the instance
./payara5/bin/asadmin --passwordfile ${PAYARA_PASSWORD_FILE} start-local-instance --verbose ${PAYARA_INSTANCE_NAME}
