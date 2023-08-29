#
#    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
#    Copyright (c) 2023 Payara Foundation and/or its affiliates. All rights reserved.
#
#    The contents of this file are subject to the terms of either the GNU
#    General Public License Version 2 only ("GPL") or the Common Development
#    and Distribution License("CDDL") (collectively, the "License").  You
#    may not use this file except in compliance with the License.  You can
#    obtain a copy of the License at
#    https://github.com/payara/Payara/blob/master/LICENSE.txt
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
    # OpenSSL contains security vulnerabilities and is unused by Payara. To prevent recurring issues simply remove OpenSSL
    && apt-get remove -y openssl \
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
