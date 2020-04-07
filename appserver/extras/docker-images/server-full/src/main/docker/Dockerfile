FROM @docker.java.image@

# Default payara ports to expose
# 4848: admin console
# 9009: debug port (JPDA)
# 8080: http
# 8181: https
EXPOSE 4848 9009 8080 8181

# Initialize the configurable environment variables
ENV HOME_DIR=/opt/payara\
    PAYARA_DIR=/opt/payara/appserver\
    SCRIPT_DIR=/opt/payara/scripts\
    CONFIG_DIR=/opt/payara/config\
    DEPLOY_DIR=/opt/payara/deployments\
    PASSWORD_FILE=/opt/payara/passwordFile\
    # Payara Server Domain options
    DOMAIN_NAME=@docker.payara.domainName@\
    ADMIN_USER=admin\
    ADMIN_PASSWORD=admin \
    # Utility environment variables
    JVM_ARGS=\
    MEM_MAX_RAM_PERCENTAGE=70.0 \
    MEM_XSS=512k \
    PAYARA_ARGS=\
    DEPLOY_PROPS=\
    POSTBOOT_COMMANDS=/opt/payara/config/post-boot-commands.asadmin\
    PREBOOT_COMMANDS=/opt/payara/config/pre-boot-commands.asadmin
ENV PATH="${PATH}:${PAYARA_DIR}/bin"

ARG TINI_VERSION=v0.18.0

# Download tini
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini \
    https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini.asc /

RUN true \
    # Install dependencies
    && apt-get update \
    && apt-get install -y gpg \
    && rm -rf /var/lib/apt/lists/* \
    # Create and set the Payara user and working directory owned by the new user
    && addgroup --gid 1000 payara \
    && adduser --system --uid 1000 --no-create-home --shell /bin/bash --home "${HOME_DIR}" --gecos "" --ingroup payara payara \
    && echo payara:payara | chpasswd \
    && mkdir -p ${DEPLOY_DIR} \
    && mkdir -p ${CONFIG_DIR} \
    && mkdir -p ${SCRIPT_DIR} \
    && chown -R payara: ${HOME_DIR} \
    # Verify tini
    && gpg --verbose --keyserver @docker.keyserver.url@ --recv-keys 595E85A6B1B4779EA4DAAEC70B588DFF0527A9B7 \
    && gpg --verify /tini.asc \
    && chmod +x /tini \
    && true

USER payara
WORKDIR ${HOME_DIR}

COPY --chown=payara:payara maven/artifacts/@docker.payara.rootDirectoryName@ ${PAYARA_DIR}/
COPY --chown=payara:payara maven/bin/* ${SCRIPT_DIR}/

RUN true \
    # Configure the password file for configuring Payara
    && echo "AS_ADMIN_PASSWORD=\nAS_ADMIN_NEWPASSWORD=${ADMIN_PASSWORD}" > /tmp/tmpfile \
    && echo "AS_ADMIN_PASSWORD=${ADMIN_PASSWORD}" >> ${PASSWORD_FILE} \
    # Configure the payara domain
    && ${PAYARA_DIR}/bin/asadmin --user ${ADMIN_USER} --passwordfile=/tmp/tmpfile change-admin-password --domain_name=${DOMAIN_NAME} \
    && ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} start-domain ${DOMAIN_NAME} \
    && ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} enable-secure-admin \
    && for MEMORY_JVM_OPTION in \
       $(${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} list-jvm-options | grep "Xm[sx]\|Xss"); \
       do\
         ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} delete-jvm-options $MEMORY_JVM_OPTION;\
       done \
    && ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-jvm-options \
      '-XX\:+UseContainerSupport:-XX\:MaxRAMPercentage=${ENV=MEM_MAX_RAM_PERCENTAGE}:-Xss${ENV=MEM_XSS}' \

    && ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} \
      set-log-attributes com.sun.enterprise.server.logging.GFFileHandler.logtoFile=false \
    && ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} stop-domain ${DOMAIN_NAME} \
    # Cleanup after initialization
    && rm -rf \
        /tmp/tmpFile \
        ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/osgi-cache \
        ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/logs \
    && true

ENTRYPOINT ["/tini", "--"]
CMD "${SCRIPT_DIR}/entrypoint.sh"
