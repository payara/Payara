FROM payara/basic:@docker.payara.tag@

ENV PAYARA_DAS_HOST="localhost" \
    PAYARA_DAS_PORT="4848" \
    PAYARA_NODE_NAME="" \
    PAYARA_CONFIG_NAME="" \
    PAYARA_INSTANCE_NAME="" \
    DOCKER_CONTAINER_IP=""
ENV PAYARA_PASSWORD_FILE_DIR=${HOME_DIR}/passwords
ENV PAYARA_PASSWORD_FILE=${PAYARA_PASSWORD_FILE_DIR}/passwordfile.txt

# Create and set the Payara user and working directory owned by the new user
RUN true \
    && mkdir -p ${PAYARA_PASSWORD_FILE_DIR} \
    && true

COPY --chown=payara:payara maven/artifacts/@docker.payara.rootDirectoryName@ ${PAYARA_DIR}
COPY --chown=payara:payara maven/bin/* ${SCRIPT_DIR}

ENTRYPOINT "${SCRIPT_DIR}/entrypoint.sh"
