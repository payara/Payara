FROM @docker.java.image@

ENV HOME_DIR=/opt/payara
ENV PAYARA_DIR=${HOME_DIR}/appserver \
    SCRIPT_DIR=${HOME_DIR}/scripts \
    CONFIG_DIR=${HOME_DIR}/config \
    DEPLOY_DIR=${HOME_DIR}/deployments \
    PASSWORD_FILE=${HOME_DIR}/passwordFile \
    ADMIN_USER=admin \
    ADMIN_PASSWORD=admin \
    JVM_ARGS="" \
    MEM_MAX_RAM_PERCENTAGE="70.0" \
    MEM_XSS="512k"
ENV PATH="${PATH}:${PAYARA_DIR}/bin"

ARG TINI_VERSION=v0.19.0

# Download tini
ADD https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini \
    https://github.com/krallin/tini/releases/download/${TINI_VERSION}/tini.asc /

RUN true \
    && apt-get update \
    && apt-get install -y gpg \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p ${HOME_DIR} \
    && addgroup --gid 1000 payara \
    && adduser --system --uid 1000 --no-create-home --shell /bin/bash --home "${HOME_DIR}" --gecos "" --ingroup payara payara \
    && echo payara:payara | chpasswd \
    && mkdir -p ${PAYARA_DIR} \
    && mkdir -p ${DEPLOY_DIR} \
    && mkdir -p ${CONFIG_DIR} \
    && mkdir -p ${SCRIPT_DIR} \
    && chown -R payara:payara ${HOME_DIR} \
    # Verify tini
    && gpg --verbose --keyserver @docker.keyserver.url@ --recv-keys 595E85A6B1B4779EA4DAAEC70B588DFF0527A9B7 \
    && gpg --verify /tini.asc \
    && chmod +x /tini \
    && true

USER payara
WORKDIR ${HOME_DIR}
