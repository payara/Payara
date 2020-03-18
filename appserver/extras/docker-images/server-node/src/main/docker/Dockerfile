FROM "${docker.basicJdkImage}"

ENV HOME_DIR=/opt/payara
ENV PAYARA_DIR=${HOME_DIR}/payara5 \
    SCRIPT_DIR=/opt/payara\
    PAYARA_DAS_HOST="localhost" \
    PAYARA_DAS_PORT="4848" \
    PAYARA_NODE_NAME="" \
    PAYARA_CONFIG_NAME="" \
    PAYARA_INSTANCE_NAME="" \
    DOCKER_CONTAINER_IP=""
ENV PAYARA_PASSWORD_FILE_DIR=${HOME_DIR}/passwords
ENV PAYARA_PASSWORD_FILE=${PAYARA_PASSWORD_FILE_DIR}/passwordfile.txt

# Create and set the Payara user and working directory owned by the new user
RUN true \
    && mkdir ${HOME_DIR} \
    && mkdir ${PAYARA_DIR} \
    && mkdir ${PAYARA_PASSWORD_FILE_DIR} \
    && groupadd -g 1000 payara \
    && useradd -u 1000 -M -s /bin/bash -d ${HOME_DIR} payara -g payara \
    && echo payara:payara | chpasswd \
    && chown -R payara:payara ${HOME_DIR} \
    && true

USER payara
WORKDIR ${HOME_DIR}

# Install Payara Server
ARG PAYARA_UNPACKED
ARG ADDITIONAL_SCRIPTS

COPY --chown=payara:payara ${PAYARA_UNPACKED}/payara5 ${PAYARA_DIR}
COPY --chown=payara:payara ${ADDITIONAL_SCRIPTS} ${HOME_DIR}
# Fixes executable permissions in builds on windows machines
RUN find ${HOME_DIR} -type f -regextype sed -regex ".*/\(.*\.sh\|asadmin\)" -print -exec chmod +x {} \;

# Start the instance
ENTRYPOINT "${SCRIPT_DIR}/entrypoint.sh"
