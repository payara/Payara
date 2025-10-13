#!/usr/bin/env bash
################################################################################
#    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
#    Copyright (c) 2023 Payara Foundation and/or its affiliates. All rights reserved.
#
#    The contents of this file are subject to the terms of either the GNU
#    General Public License Version 2 only ("GPL") or the Common Development
#    and Distribution License("CDDL") (collectively, the "License").  You
#    may not use this file except in compliance with the License.  You can
#    obtain a copy of the License at
#    https://github.com/payara/Payara/blob/main/LICENSE.txt
#    See the License for the specific
#    language governing permissions and limitations under the License.
#
#    When distributing the software, include this License Header Notice in each
#    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
#
#    GPL Classpath Exception:
#    The Payara Foundation designates this particular file as subject to the "Classpath"
#    exception as provided by the Payara Foundation in the GPL Version 2 section of the License
#    file that accompanied this code.
#
#    Modifications:
#    If applicable, add the following below the License Header, with the fields
#    enclosed by brackets [] replaced by your own identifying information:
#    "Portions Copyright [year] [name of copyright owner]"
#
#    Contributor(s):
#    If you wish your version of this file to be governed by only the CDDL or
#    only the GPL Version 2, indicate your decision by adding "[Contributor]
#    elects to include this software in this distribution under the [CDDL or GPL
#    Version 2] license."  If you don't indicate a single choice of license, a
#    recipient has the option to distribute your version of this file under
#    either the CDDL, the GPL Version 2 or to extend the choice of license to
#    its licensees as provided above.  However, if you add GPL Version 2 code
#    and therefore, elected the GPL Version 2 license, then the option applies
#    only if the new code is made subject to such option by the copyright
#    holder.
################################################################################

set -e

DOCKER_CONTAINER_ID="$(cat /proc/self/cgroup | grep :/docker/  | sed s/\\//\\n/g | tail -1)"

ASADMIN="${PAYARA_DIR}/bin/asadmin"
echo "Docker Container ID is: ${DOCKER_CONTAINER_ID}"

if [ -z "${DOCKER_CONTAINER_IP}" ]; then
    echo "No Docker container IP override given, setting to first result from 'hostname -I'"
    DOCKER_CONTAINER_IP="$(hostname -I | cut -f1 -d ' ')"
    echo "Hostname is ${DOCKER_CONTAINER_IP}"
fi

echo "Docker Container IP is: ${DOCKER_CONTAINER_IP}"

### Functions ###
function checkAndCreateNewNodeIfRequired {
    if [ -z "${PAYARA_NODE_NAME}" ]; then
        echo "No node name given."
        AUTOGENERATE_NODE_NAME=true
        createNewNode
    else
        # Check if node exists and matches this IP address
        echo "Node name provided, checking if node details match this container."
        NODE_EXISTS="$(${ASADMIN} -I false -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} get nodes.node.${PAYARA_NODE_NAME}.name)" || true
        if [ ! -z "${NODE_EXISTS}" ]; then
            # Cut off the "Command completed succesfully bit
            NODE_EXISTS="$(echo ${NODE_EXISTS} | cut -f1 -d ' ')"
            if [ "${NODE_EXISTS}" == "nodes.node.${PAYARA_NODE_NAME}.name=${PAYARA_NODE_NAME}" ]; then
                echo "Node with matching name found, checking node details."
                NODE_HOST="$(${ASADMIN} -I false -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} get nodes.node.${PAYARA_NODE_NAME}.node-host)" || true
                if [ ! -z "${NODE_HOST}" ]; then
                    # Cut off the "Command completed succesfully bit
                    NODE_HOST="$(echo ${NODE_HOST} | cut -f1 -d ' ')"
                    echo "Node Host of matching node is ${NODE_HOST}"
                    if [ "${NODE_HOST}" == "nodes.node.${PAYARA_NODE_NAME}.node-host=${DOCKER_CONTAINER_IP}" ]; then
                        echo "Node details match, no need to create a new node."
                    else
                        echo "Node details do not match, creating a new node."
                        AUTOGENERATE_NODE_NAME=true
                        createNewNode
                    fi
                else
                    echo "Could not retrieve node host, creating a new node."
                    AUTOGENERATE_NODE_NAME=true
                    createNewNode
                fi
            else
                echo "No node with matching name found."
                AUTOGENERATE_NODE_NAME=false
                createNewNode
            fi
        else
            echo "No node with matching name found."
            AUTOGENERATE_NODE_NAME=false
            createNewNode
        fi
    fi
}

function createNewNode {
    echo "WARNING: Could not find a matching Docker Node: Creating a temporary node specific to this container - cleanup of this container cannot be done by Payara Server"
    if [ "${AUTOGENERATE_NODE_NAME}" ]; then
        echo "Creating a temporary node with an autogenerated name."
        ASADMIN_COMMAND="${ASADMIN} -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} _create-node-temp --nodehost ${DOCKER_CONTAINER_IP}"
        echo "${ASADMIN_COMMAND}"
        PAYARA_NODE_NAME="$(${ASADMIN_COMMAND})"
    else
        echo "Creating a temporary node with provided name."
        ASADMIN_COMMAND="${ASADMIN} -I false -T -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} _create-node-temp --nodehost ${DOCKER_CONTAINER_IP} ${PAYARA_NODE_NAME}"
        echo "${ASADMIN_COMMAND}"
        PAYARA_NODE_NAME="$(${ASADMIN_COMMAND})"
    fi
}

function createNewInstance {
    # Check if we actually have a node name. If we don't have a node name, we can assume that we need to create a node from scratch
    checkAndCreateNewNodeIfRequired

    echo "Running command create-local-instance:"
    if [ -z "${PAYARA_INSTANCE_NAME}" ]; then
            if [ -z "${PAYARA_CONFIG_NAME}" ]; then
                if [ -z "${PAYARA_DEPLOYMENT_GROUP}" ]; then
                    ASADMIN_COMMAND="${ASADMIN} -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --dockernode true --ip ${DOCKER_CONTAINER_IP}"
                    echo "${ASADMIN_COMMAND}"
                    PAYARA_INSTANCE_NAME="$(${ASADMIN_COMMAND})"
                else
                    ASADMIN_COMMAND="${ASADMIN} -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --dockernode true --ip ${DOCKER_CONTAINER_IP} --deploymentgroup ${PAYARA_DEPLOYMENT_GROUP}"
                    echo "${ASADMIN_COMMAND}"
                    PAYARA_INSTANCE_NAME="$(${ASADMIN_COMMAND})"
                fi
            else
                if [ "${PAYARA_CONFIG_NAME}" == "server-config" ] || [ "${PAYARA_CONFIG_NAME}" == "default-config" ]; then
                    echo "You cannot use 'server-config' or 'default-config', ignoring provided config name."
                    if [ -z "${PAYARA_DEPLOYMENT_GROUP}" ]; then
                        ASADMIN_COMMAND="${ASADMIN} -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --dockernode true --ip ${DOCKER_CONTAINER_IP}"
                        echo "${ASADMIN_COMMAND}"
                        PAYARA_INSTANCE_NAME="$(${ASADMIN_COMMAND})"
                    else
                        ASADMIN_COMMAND="${ASADMIN} -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --dockernode true --ip ${DOCKER_CONTAINER_IP} --deploymentgroup ${PAYARA_DEPLOYMENT_GROUP}"
                        echo "${ASADMIN_COMMAND}"
                        PAYARA_INSTANCE_NAME="$(${ASADMIN_COMMAND})"
                    fi
                else
                    if [ -z "${PAYARA_DEPLOYMENT_GROUP}" ]; then
                        ASADMIN_COMMAND="${ASADMIN} -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --config ${PAYARA_CONFIG_NAME} --dockernode true --ip ${DOCKER_CONTAINER_IP}"
                        echo "${ASADMIN_COMMAND}"
                        PAYARA_INSTANCE_NAME="$(${ASADMIN_COMMAND})"
                    else
                        ASADMIN_COMMAND="${ASADMIN} -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --config ${PAYARA_CONFIG_NAME} --dockernode true --ip ${DOCKER_CONTAINER_IP} --deploymentgroup ${PAYARA_DEPLOYMENT_GROUP}"
                        echo "${ASADMIN_COMMAND}"
                        PAYARA_INSTANCE_NAME="$(${ASADMIN_COMMAND})"
                    fi
                fi
            fi
        else
            if [ -z "${PAYARA_CONFIG_NAME}" ]; then
                if [ -z "${PAYARA_DEPLOYMENT_GROUP}" ]; then
                    ASADMIN_COMMAND="${ASADMIN} -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --dockernode true --ip ${DOCKER_CONTAINER_IP} ${PAYARA_INSTANCE_NAME}"
                    echo "${ASADMIN_COMMAND}"
                    PAYARA_INSTANCE_NAME="$(${ASADMIN_COMMAND})"
                else
                    ASADMIN_COMMAND="${ASADMIN} -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --dockernode true --ip ${DOCKER_CONTAINER_IP} ${PAYARA_INSTANCE_NAME} --deploymentgroup ${PAYARA_DEPLOYMENT_GROUP}"
                    echo "${ASADMIN_COMMAND}"
                    PAYARA_INSTANCE_NAME="$(${ASADMIN_COMMAND})"
                fi
            else
                if [ "${PAYARA_CONFIG_NAME}" == "server-config" ] || [ "${PAYARA_CONFIG_NAME}" == "default-config" ]; then
                    if [ -z "${PAYARA_DEPLOYMENT_GROUP}" ]; then
                        echo "You cannot use 'server-config' or 'default-config', ignoring provided config name."
                        ASADMIN_COMMAND="${ASADMIN} -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --dockernode true --ip ${DOCKER_CONTAINER_IP} ${PAYARA_INSTANCE_NAME}"
                        echo "${ASADMIN_COMMAND}"
                        PAYARA_INSTANCE_NAME="$(${ASADMIN_COMMAND})"
                    else
                        echo "You cannot use 'server-config' or 'default-config', ignoring provided config name."
                        ASADMIN_COMMAND="${ASADMIN} -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --dockernode true --ip ${DOCKER_CONTAINER_IP} ${PAYARA_INSTANCE_NAME} --deploymentgroup ${PAYARA_DEPLOYMENT_GROUP}"
                        echo "${ASADMIN_COMMAND}"
                        PAYARA_INSTANCE_NAME="$(${ASADMIN_COMMAND})"
                    fi
            else
                if [ -z "${PAYARA_DEPLOYMENT_GROUP}" ]; then
                    ASADMIN_COMMAND="${ASADMIN} -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --config ${PAYARA_CONFIG_NAME} --dockernode true --ip ${DOCKER_CONTAINER_IP} ${PAYARA_INSTANCE_NAME}"
                    echo "${ASADMIN_COMMAND}"
                    PAYARA_INSTANCE_NAME="$(${ASADMIN_COMMAND})"
                else
                    ASADMIN_COMMAND="${ASADMIN} -I false -T -a -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} create-local-instance --node ${PAYARA_NODE_NAME} --config ${PAYARA_CONFIG_NAME} --dockernode true --ip ${DOCKER_CONTAINER_IP} ${PAYARA_INSTANCE_NAME} --deploymentgroup ${PAYARA_DEPLOYMENT_GROUP}"
                    echo "${ASADMIN_COMMAND}"
                    PAYARA_INSTANCE_NAME="$(${ASADMIN_COMMAND})"
                fi
            fi
        fi
    fi

    if [ ! -z "${DOCKER_CONTAINER_ID}" ]; then
       # Register Docker container ID to DAS
       echo "Setting Docker Container ID for instance ${PAYARA_INSTANCE_NAME}: ${DOCKER_CONTAINER_ID}"
       ASADMIN_COMMAND="${ASADMIN} -I false -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} _set-docker-container-id --instance ${PAYARA_INSTANCE_NAME} --id ${DOCKER_CONTAINER_ID}"
       echo "${ASADMIN_COMMAND}"
       ${ASADMIN_COMMAND}
    fi
}

### Setup ###

# Check if we actually have an instance name. If we don't have an instance name, we can assume that we need to create an instance from scratch
if [ -z "${PAYARA_INSTANCE_NAME}" ]; then
    echo "No Instance name given."
    createNewInstance
else
    # Check if instance already created before running create command
    if [ ! -d "payara7/glassfish/nodes/${PAYARA_NODE_NAME}/${PAYARA_INSTANCE_NAME}" ]; then
        echo "Instance name provided, but local file system for instance missing, checking if file system or new instance needs to be created."

        # Check if an instance with this name is actually registered
        echo "Checking if an instance with name ${PAYARA_INSTANCE_NAME} has been registered with the DAS"
        ASADMIN_COMMAND="${ASADMIN} -I false -t -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} list-instances --nostatus ${PAYARA_INSTANCE_NAME}"
        echo "${ASADMIN_COMMAND}"
        INSTANCE_EXISTS="$(${ASADMIN_COMMAND})" || true

        if [ ! -z "${INSTANCE_EXISTS}" ]; then
            # Check if Docker container ID registered against the instance name is the same
            echo "Found an instance with name ${PAYARA_INSTANCE_NAME} registered to the DAS, checking if registered Docker Container ID matches this container's ID"
            ASADMIN_COMMAND="${ASADMIN} -I false -t -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} _get-docker-container-id --instance ${PAYARA_INSTANCE_NAME}"
            echo "${ASADMIN_COMMAND}"
            REGISTERED_DOCKER_CONTAINER_ID="$(${ASADMIN_COMMAND})" || true

            if [ ! -z "${REGISTERED_DOCKER_CONTAINER_ID}" ]; then
                # If they're the same, simply create the folders, otherwise create and register a new instance
                echo "Registered Docker Container ID is: ${REGISTERED_DOCKER_CONTAINER_ID}"
                if [ "${REGISTERED_DOCKER_CONTAINER_ID}" == "${DOCKER_CONTAINER_ID}" ]; then
                    echo "Docker Container IDs match, creating local instance filesystem: "
                    ASADMIN_COMMAND="${ASADMIN} -I false -T -H ${PAYARA_DAS_HOST} -p ${PAYARA_DAS_PORT} -W ${PAYARA_PASSWORD_FILE} _create-instance-filesystem --node ${PAYARA_NODE_NAME} --dockernode true ${PAYARA_INSTANCE_NAME}"
                    ${ASADMIN_COMMAND}
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
ASADMIN_COMMAND="${ASADMIN} --passwordfile ${PAYARA_PASSWORD_FILE} start-local-instance --node ${PAYARA_NODE_NAME} --verbose ${PAYARA_INSTANCE_NAME}"
${ASADMIN_COMMAND}
