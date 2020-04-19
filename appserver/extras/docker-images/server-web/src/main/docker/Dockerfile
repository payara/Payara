FROM payara/basic:@docker.payara.tag@

# Default payara ports to expose
# 4848: admin console
# 9009: debug port (JPDA)
# 8080: http
# 8181: https
EXPOSE 4848 9009 8080 8181

ENV DOMAIN_NAME=@docker.payara.domainName@ \
    PAYARA_ARGS="" \
    PREBOOT_COMMANDS=${CONFIG_DIR}/pre-boot-commands.asadmin \
    POSTBOOT_COMMANDS=${CONFIG_DIR}/post-boot-commands.asadmin \
    DEPLOY_PROPS=""

COPY --chown=payara:payara maven/bin/* ${SCRIPT_DIR}/
COPY --chown=payara:payara maven/artifacts/@docker.payara.rootDirectoryName@ ${PAYARA_DIR}/

RUN true \
    && echo "AS_ADMIN_PASSWORD=\nAS_ADMIN_NEWPASSWORD=${ADMIN_PASSWORD}" > /tmp/password-change-file.txt \
    && echo "AS_ADMIN_PASSWORD=${ADMIN_PASSWORD}" >> ${PASSWORD_FILE} \
    && ${PAYARA_DIR}/bin/asadmin --user ${ADMIN_USER} --passwordfile=/tmp/password-change-file.txt change-admin-password --domain_name=${DOMAIN_NAME} \
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
      set-log-attributes fish.payara.logging.jul.PayaraLogHandler.logtoFile=false \
    && ${PAYARA_DIR}/bin/asadmin --user=${ADMIN_USER} --passwordfile=${PASSWORD_FILE} stop-domain ${DOMAIN_NAME} \
    && rm -rf \
        /tmp/password-change-file.txt \
        ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/osgi-cache \
        ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/logs \
    && true

ENTRYPOINT ["/tini", "--"]
CMD "${SCRIPT_DIR}/entrypoint.sh"
