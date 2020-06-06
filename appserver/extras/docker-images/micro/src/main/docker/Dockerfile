FROM @docker.java.image@

# Default payara ports to expose
EXPOSE 6900 8080

# PAYARA_HOME is deprecated - it is here for backward compatibility
ENV PAYARA_HOME=/opt/payara
ENV HOME_DIR=${PAYARA_HOME}
ENV PAYARA_DIR=${HOME_DIR} \
    SCRIPT_DIR=${HOME_DIR} \
    DEPLOY_DIR=/opt/payara/deployments \
    JVM_ARGS="" \
    MEM_MAX_RAM_PERCENTAGE="70.0" \
    MEM_XSS="512k"

RUN true \
    && mkdir -p "${HOME_DIR}" \
    && addgroup --gid 1000 payara \
    && adduser --system --uid 1000 --no-create-home --shell /bin/bash --home "${HOME_DIR}" --gecos "" --ingroup payara payara \
    && echo payara:payara | chpasswd \
    && mkdir -p "${PAYARA_DIR}" \
    && mkdir -p "${SCRIPT_DIR}" \
    && mkdir -p "${DEPLOY_DIR}" \
    && chown -R payara:payara ${HOME_DIR} \
    && true

USER payara
WORKDIR ${HOME_DIR}

COPY --chown=payara:payara maven/bin/* ${SCRIPT_DIR}/
COPY --chown=payara:payara maven/artifacts/payara-micro.jar .

ENTRYPOINT ["/bin/sh", "entrypoint.sh"]
CMD ["--deploymentDir","/opt/payara/deployments"]
