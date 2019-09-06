#!/usr/bin/env bash
set -e

DOCKER_CONTAINER_ID="$(cat /proc/self/cgroup | grep :/docker/  | sed s/\\//\\n/g | tail -1)"
echo "Docker Container ID is: ${DOCKER_CONTAINER_ID}"

if [ -z ${DOCKER_CONTAINER_IP} ]; then
    echo "No Docker container IP override given, setting to first result from 'hostname -I'"
    DOCKER_CONTAINER_IP="$(hostname -I | cut -f1 -d ' ')"
fi

echo "Docker Container IP is: ${DOCKER_CONTAINER_IP}"

### Functions ###
function createNewNode {
    echo "WARNING: Could not find a matching Docker Node: Creating a temporary node specific to this container - cleanup of this container cannot be done by Payara Server"
    if [ -z ${PAYARA_NODE_NAME} ]; then
        echo "./payara5/bin/asadmin -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} _create-node-hidden --nodehost ${DOCKER_CONTAINER_IP}"
        PAYARA_NODE_NAME="$(./payara5/bin/asadmin -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} _create-node-hidden --nodehost ${DOCKER_CONTAINER_IP})"
    fi
}

function createNewInstance {
    echo "Running command create-local-instance:"
    if [ -z ${PAYARA_INSTANCE_NAME} ]; then
        if [ -z ${PAYARA_CONFIG_NAME} ]; then
            echo "./payara5/bin/asadmin -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --dockernode true"
            PAYARA_INSTANCE_NAME="$(./payara5/bin/asadmin -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --dockernode true)"
        else
            if [ "${PAYARA_CONFIG_NAME}" == "server-config" ] || [ "${PAYARA_CONFIG_NAME}" == "default-config" ]; then
                echo "You cannot use 'server-config' or 'default-config', ignoring provided config name."
                echo "./payara5/bin/asadmin -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --dockernode true"
                PAYARA_INSTANCE_NAME="$(./payara5/bin/asadmin -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --dockernode true)"
            else
                echo "./payara5/bin/asadmin -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --config ${PAYARA_CONFIG_NAME} --dockernode true"
                PAYARA_INSTANCE_NAME="$(./payara5/bin/asadmin -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --config ${PAYARA_CONFIG_NAME} --dockernode true)"
            fi
        fi
    else
        if [ -z ${PAYARA_CONFIG_NAME} ]; then
            echo "./payara5/bin/asadmin -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --dockernode true ${PAYARA_INSTANCE_NAME}"
            PAYARA_INSTANCE_NAME="$(./payara5/bin/asadmin -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --dockernode true ${PAYARA_INSTANCE_NAME})"
        else
            if [ "${PAYARA_CONFIG_NAME}" == "server-config" ] || [ "${PAYARA_CONFIG_NAME}" == "default-config" ]; then
                echo "You cannot use 'server-config' or 'default-config', ignoring provided config name."
                echo "./payara5/bin/asadmin -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --dockernode true ${PAYARA_INSTANCE_NAME}"
                PAYARA_INSTANCE_NAME="$(./payara5/bin/asadmin -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --dockernode true ${PAYARA_INSTANCE_NAME})"
            else
                echo "./payara5/bin/asadmin -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --config ${PAYARA_CONFIG_NAME} --dockernode true ${PAYARA_INSTANCE_NAME}"
                PAYARA_INSTANCE_NAME="$(./payara5/bin/asadmin -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --config ${PAYARA_CONFIG_NAME} --dockernode true ${PAYARA_INSTANCE_NAME})"
            fi
        fi
    fi

    # Register Docker container ID to DAS
    echo "Setting Docker Container ID for instance ${PAYARA_INSTANCE_NAME}: ${DOCKER_CONTAINER_ID}"
    echo "./payara5/bin/asadmin -I false -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} _set-docker-container-id --instance ${PAYARA_INSTANCE_NAME} --id ${DOCKER_CONTAINER_ID}"
    ./payara5/bin/asadmin -I false -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} _set-docker-container-id --instance ${PAYARA_INSTANCE_NAME} --id ${DOCKER_CONTAINER_ID}
}

### Setup ###
# Check if we actually have a node name. If we don't have a node name, we can assume that we need to create a node from scratch
if [ -z ${PAYARA_NODE_NAME} ]; then
    echo "No node name given."
    createNewNode
else
    # Check if node exists and matches this IP address
    echo "Node name provided, checking if node details match this container."
    NODE_EXISTS="$(./payara5/bin/asadmin -I false -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} get nodes.node.${PAYARA_NODE_NAME}.name)" || true
    if [ ! -z ${NODE_EXISTS} ]; then
        if [ "${NODE_EXISTS}" == "nodes.node.${PAYARA_NODE_NAME}.name=${PAYARA_NODE_NAME}" ]; then
          echo "Node with matching name found, checking node details."
          NODE_HOST="$(./payara5/bin/asadmin -I false -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} get nodes.node.${PAYARA_NODE_NAME}.node-host)" || true
          if [ ! -z ${NODE_HOST} ]; then
              if [ "${NODE_HOST}" == "nodes.node.${NODE_HOST}.node-host=${DOCKER_CONTAINER_IP})" ]; then
                  echo "Node details match, no need to create a new node."
              else
                  echo "Node details do not match, creating a new node."
                  createNewNode
              fi
          fi
        else
            echo "No node with matching name found."
            createNewNode
        fi
    fi

fi

# Check if we actually have an instance name. If we don't have an instance name, we can assume that we need to create an instance from scratch
if [ -z ${PAYARA_INSTANCE_NAME} ]; then
    echo "No Instance name given."
    createNewInstance
else
    # Check if instance already created before running create command
    if [ ! -d "payara5/glassfish/nodes/${PAYARA_NODE_NAME}/${PAYARA_INSTANCE_NAME}" ]; then
        echo "Instance name provided, but local file system for instance missing, checking if file system or new instance needs to be created."

        # Check if an instance with this name is actually registered
        echo "Checking if an instance with name ${PAYARA_INSTANCE_NAME} has been registered with the DAS"
        echo "./payara5/bin/asadmin -I false -t -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} list-instances --nostatus ${PAYARA_INSTANCE_NAME}"
        INSTANCE_EXISTS="$(./payara5/bin/asadmin -I false -t -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} list-instances --nostatus ${PAYARA_INSTANCE_NAME})" || true

        if [ ! -z ${INSTANCE_EXISTS} ]; then
            # Check if Docker container ID registered against the instance name is the same
            echo "Found an instance with name ${PAYARA_INSTANCE_NAME} registered to the DAS, checking if registered Docker Container ID matches this container's ID"
            echo "./payara5/bin/asadmin -I false -t -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} _get-docker-container-id --instance ${PAYARA_INSTANCE_NAME}"
            REGISTERED_DOCKER_CONTAINER_ID="$(./payara5/bin/asadmin -I false -t -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} _get-docker-container-id --instance ${PAYARA_INSTANCE_NAME})" || true

            if [ ! -z ${REGISTERED_DOCKER_CONTAINER_ID} ]; then
                # If they're the same, simply create the folders, otherwise create and register a new instance
                echo "Registered Docker Container ID is: ${REGISTERED_DOCKER_CONTAINER_ID}"
                if [ "${REGISTERED_DOCKER_CONTAINER_ID}" == "${DOCKER_CONTAINER_ID}" ]; then
                    echo "Docker Container IDs match, creating local instance filesystem: "
                    echo "./payara5/bin/asadmin -I false -T -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} _create-instance-filesystem --node ${PAYARA_NODE_NAME} --dockernode true ${PAYARA_INSTANCE_NAME}"
                    ./payara5/bin/asadmin -I false -T -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} _create-instance-filesystem --node ${PAYARA_NODE_NAME} --dockernode true ${PAYARA_INSTANCE_NAME}
                else
                    echo "Docker Container IDs do not match, creating a new instance."
                    createNewInstance
                fi
            else
                echo "Could not retrieve registered Docker Container ID, creating a new instance"
                createNewInstance
            fi
        else
            createNewInstance
        fi
    fi
fi

### Start ###
echo "Starting instance ${PAYARA_INSTANCE_NAME}"
echo "./payara5/bin/asadmin --passwordfile ${PAYARA_PASSWORD_FILE} start-local-instance --verbose ${PAYARA_INSTANCE_NAME}"
./payara5/bin/asadmin --passwordfile ${PAYARA_PASSWORD_FILE} start-local-instance --verbose ${PAYARA_INSTANCE_NAME}