#
#    DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
#    Copyright (c) 2023-2025 Payara Foundation and/or its affiliates. All rights reserved.
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
#    file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
EXPOSE 6900 8080

# PAYARA_HOME is deprecated - it is here for backward compatibility
ENV PAYARA_HOME=/opt/payara
ENV HOME_DIR=${PAYARA_HOME}
ENV PAYARA_DIR=${HOME_DIR} \
    SCRIPT_DIR=${HOME_DIR} \
    DEPLOY_DIR=/opt/payara/deployments \
    JVM_ARGS="-Djdk.util.zip.disableZip64ExtraFieldValidation=true" \
    MEM_MAX_RAM_PERCENTAGE="70.0" \
    MEM_XSS="512k"

# Add Tini
RUN apk add --no-cache tini

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

ENTRYPOINT ["tini", "--", "/opt/payara/entrypoint.sh"]
CMD ["--deploymentDir", "/opt/payara/deployments"]
