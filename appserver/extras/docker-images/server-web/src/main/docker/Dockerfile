#
#    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
#    Copyright (c) 2023-2024 Payara Foundation and/or its affiliates. All rights reserved.
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
#    file and include the License file at glassfish/legal/LICENSE.txt.
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

FROM @docker.java.image@

# Default payara ports to expose
# 4848: admin console
# 9009: debug port (JPDA)
# 8080: http
# 8181: https
EXPOSE 4848 9009 8080 8181

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
    MEM_XSS="512k" \
    DOMAIN_NAME=@docker.payara.domainName@ \
    PAYARA_ARGS="" \
    DEPLOY_PROPS=""
ENV PATH="${PATH}:${PAYARA_DIR}/bin" \
    PREBOOT_COMMANDS=${CONFIG_DIR}/pre-boot-commands.asadmin \
    PREBOOT_COMMANDS_FINAL=${CONFIG_DIR}/pre-boot-commands-final.asadmin \
    POSTBOOT_COMMANDS=${CONFIG_DIR}/post-boot-commands.asadmin \
    POSTBOOT_COMMANDS_FINAL=${CONFIG_DIR}/post-boot-commands-final.asadmin

RUN true \
    && apt-get update \
    # OpenSSL contains security vulnerabilities and is unused by Payara. To prevent recurring issues simply remove OpenSSL
    && apt-get remove -y openssl \
    && apt-get install -y tini \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p ${HOME_DIR} \
    && addgroup --gid 1000 payara \
    && adduser --system --uid 1000 --no-create-home --shell /bin/bash --home "${HOME_DIR}" --gecos "" --ingroup payara payara \
    && echo payara:payara | chpasswd \
    && mkdir -p ${PAYARA_DIR} \
    && mkdir -p ${DEPLOY_DIR} \
    && mkdir -p ${CONFIG_DIR} \
    && mkdir -p ${SCRIPT_DIR} \
    && chown -R payara:payara ${HOME_DIR}

USER payara
WORKDIR ${HOME_DIR}

COPY --chown=payara:payara maven/bin/* ${SCRIPT_DIR}/
COPY --chown=payara:payara maven/artifacts/@docker.payara.rootDirectoryName@ ${PAYARA_DIR}/

RUN true \
    && echo "AS_ADMIN_PASSWORD=\nAS_ADMIN_NEWPASSWORD=${ADMIN_PASSWORD}" > /tmp/password-change-file.txt \
    && echo "AS_ADMIN_PASSWORD=${ADMIN_PASSWORD}" >> ${PASSWORD_FILE} \
    && ${PAYARA_DIR}/bin/asadmin --user ${ADMIN_USER} --passwordfile=/tmp/password-change-file.txt change-admin-password --domain_name=${DOMAIN_NAME} \
    && sed -i 's/<\/java-config>/  <jvm-options>-Djdk\.util\.zip\.disableZip64ExtraFieldValidation=true<\/jvm-options>\n      <\/java-config>/g'  ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/config/domain.xml \
    && sed -i 's/<\/java-config>/<\/java-config>\n      <phone-home-runtime-configuration enabled="false"><\/phone-home-runtime-configuration>/g'  ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/config/domain.xml \
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
    && rm -rf \
        /tmp/password-change-file.txt \
        ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/osgi-cache \
        ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/logs \
    && sed -i '/<phone-home-runtime-configuration/d' ${PAYARA_DIR}/glassfish/domains/${DOMAIN_NAME}/config/domain.xml \
    && true

ENTRYPOINT ["tini", "--"]
CMD "${SCRIPT_DIR}/entrypoint.sh"
