FROM azul/zulu-openjdk:8u212

ENV WORK_DIR=/opt/payara
ENV PAYARA_HOME=${WORK_DIR}/payara5 \
    PAYARA_PASSWORD_FILE_DIR=${WORK_DIR}/passwords \
    PAYARA_DAS_HOST="localhost" \
    PAYARA_DAS_PORT="4848" \
    PAYARA_NODE_NAME="LocalDockerNode1" \
    PAYARA_INSTANCE_NAME="LocalDockerInstance1"
ENV PAYARA_PASSWORD_FILE=${PAYARA_PASSWORD_FILE_DIR}/passwordfile.txt

# Create and set the Payara user and working directory owned by the new user
RUN mkdir ${WORK_DIR} && \
    mkdir ${PAYARA_HOME} && \
    mkdir ${PAYARA_PASSWORD_FILE_DIR} && \
    groupadd -g 1000 payara && \
    useradd -u 1000 -M -s /bin/bash -d ${WORK_DIR} payara -g payara && \
    echo payara:payara | chpasswd && \
    chown -R payara:payara ${WORK_DIR}

USER payara
WORKDIR ${WORK_DIR}

# Install Payara Server and remove unused domains
ARG PAYARA_INSTALL
COPY --chown=payara:payara ${PAYARA_INSTALL} ${PAYARA_HOME}
RUN rm -rf ${PAYARA_HOME}/glassfish/domains/domain1/ && \
    rm -rf ${PAYARA_HOME}/glassfish/domains/production

# Install entrypoint script
ARG ENTRYPOINT_SCRIPT
COPY --chown=payara:payara ${ENTRYPOINT_SCRIPT} ${WORK_DIR}
RUN chmod +x ${WORK_DIR}/entrypoint.sh

# Start the instance
ENTRYPOINT ["/opt/payara/entrypoint.sh"]