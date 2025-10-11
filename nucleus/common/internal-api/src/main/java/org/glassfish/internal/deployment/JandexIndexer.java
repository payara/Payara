/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2025] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.internal.deployment;

import org.glassfish.api.deployment.DeploymentContext;
import org.jvnet.hk2.annotations.Contract;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

@Contract
public interface JandexIndexer {
    enum SettingType {
        UNSET,
        TRUE,
        FALSE,
        ;

        public boolean isSet() {
            return this != UNSET;
        }

        public boolean isTrue() {
            return this == TRUE;
        }

        static SettingType of(BooleanSupplier supplier) {
            return supplier.getAsBoolean() ? TRUE : FALSE;
        }
    }

    class Index implements Serializable {
        private static final long serialVersionUID = 1L;

        private transient org.jboss.jandex.Index jandexIndex;
        private SettingType implicitBeanArchive = SettingType.UNSET;
        private SettingType hasCDIEnablingAnnotations = SettingType.UNSET;
        private SettingType isValidBdaBasedOnExtensionAndBeansXml = SettingType.UNSET;

        public Index(org.jboss.jandex.Index index) {
            this.jandexIndex = index;
        }

        public org.jboss.jandex.Index getIndex() {
            return jandexIndex;
        }

        public void setIndex(org.jboss.jandex.Index index) {
            this.jandexIndex = index;
        }

        public boolean implicitBeanArchive(BooleanSupplier settingTypeSupplier) {
            if (!implicitBeanArchive.isSet()) {
                implicitBeanArchive = SettingType.of(settingTypeSupplier);
            }
            return implicitBeanArchive.isTrue();
        }

        public boolean hasCDIEnablingAnnotations(BooleanSupplier settingTypeSupplier) {
            if (!hasCDIEnablingAnnotations.isSet()) {
                hasCDIEnablingAnnotations = SettingType.of(settingTypeSupplier);
            }
            return hasCDIEnablingAnnotations.isTrue();
        }

        public boolean isValidBdaBasedOnExtensionAndBeansXml(BooleanSupplier settingTypeSupplier) {
            if (!isValidBdaBasedOnExtensionAndBeansXml.isSet()) {
                isValidBdaBasedOnExtensionAndBeansXml = SettingType.of(settingTypeSupplier);
            }
            return isValidBdaBasedOnExtensionAndBeansXml.isTrue();
        }
    }

    void index(DeploymentContext deploymentContext) throws IOException;
    void reindex(DeploymentContext deploymentContext) throws IOException;
    boolean isJakartaEEApplication(DeploymentContext deploymentContext) throws IOException;
    Index getRootIndex(DeploymentContext deploymentContext);
    Map<String, Index> getAllIndexes(DeploymentContext deploymentContext);
    Map<String, Index> getIndexesByURI(DeploymentContext deploymentContext, Collection<URI> uris);
    boolean hasAnyAnnotations(DeploymentContext deploymentContext, List<URI> uris, String... annotations);
}
