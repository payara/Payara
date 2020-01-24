FROM "${docker.basicJdkImage}"

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
    DOMAIN_NAME=${docker.payara.domainName} \
    ADMIN_USER=admin\
    ADMIN_PASSWORD=admin \
    # Utility environment variables
    JVM_ARGS=\
    PAYARA_ARGS=\
    DEPLOY_PROPS=\
    POSTBOOT_COMMANDS=/opt/payara/config/post-boot-commands.asadmin\
    PREBOOT_COMMANDS=/opt/payara/config/pre-boot-commands.asadmin
ENV PATH="${PATH}:${PAYARA_DIR}/bin"

ARG PAYARA_UNPACKED
ARG ADDITIONAL_SCRIPTS
ARG TINI_VERSION=v0.18.0

# Create and set the Payara user and working directory owned by the new user
RUN true \
    && apt-get update \
    && apt-get install -y wget unzip coreutils gpg \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 1000 payara \
    && useradd -u 1000 -M -s /bin/bash -d ${HOME_DIR} payara -g payara \
    && echo payara:payara | chpasswd \
    && mkdir -p ${DEPLOY_DIR} \
    && mkdir -p ${CONFIG_DIR} \
    && mkdir -p ${SCRIPT_DIR} \
    && chown -R payara: ${HOME_DIR} \
    && wget --no-verbose -O /tini https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini \
    && wget --no-verbose -O /tini.asc https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini.asc \
    && mkdir ~/.gnupg && echo "disable-ipv6" >> ~/.gnupg/dirmngr.conf \
    && gpg --batch --keyserver "hkp://p80.pool.sks-keyservers.net:80" --recv-keys 595E85A6B1B4779EA4DAAEC70B588DFF0527A9B7 \
    && gpg --batch --verify /tini.asc /tini \
    && chmod +x /tini \
    && true

USER payara
WORKDIR ${HOME_DIR}

# Download and unzip the Payara distribution
COPY --chown=payara:payara ${PAYARA_UNPACKED}/payara5 ${PAYARA_DIR}
COPY --chown=payara:payara ${ADDITIONAL_SCRIPTS} ${SCRIPT_DIR}/

RUN true \
    && chmod +x ${SCRIPT_DIR}/* \
    # Configure the password file for configuring Payara
    && echo "AS_ADMIN_PASSWORD=\nAS_ADMIN_NEWPASSWORD=${ADMIN_PASSWORD}" > /tmp/tmpfile \
    && echo "AS_ADMIN_PASSWORD=${ADMIN_PASSWORD}" >> ${PASSWORD_FILE} \
    # Configure the payara domain
    && ${PAYARA_DIR}/bin/asadmin --user ${ADMIN_USER} --passwordfile=/tmp/tmpfile change-admin-password --domain_name=${DOMAIN_NAME} \
    && ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} start-domain ${DOMAIN_NAME} \
    && ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} enable-secure-admin \
    && for MEMORY_JVM_OPTION in \
       $(${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} list-jvm-options | grep "Xm[sx]"); \
       do\
         ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} delete-jvm-options $MEMORY_JVM_OPTION;\
       done \
    # FIXME: when upgrading this container to Java 10+, this needs to be changed to
    #        '-XX:+UseContainerSupport' and '-XX:MaxRAMPercentage'
    && ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} create-jvm-options \
      '-XX\:+UnlockExperimentalVMOptions:-XX\:+IgnoreUnrecognizedVMOptions:-XX\:+UseCGroupMemoryLimitForHeap:-XX\:MaxRAMFraction=1' \
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
CMD ${SCRIPT_DIR}/entrypoint.sh
