/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 * 
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License.
 * 
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 * 
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 * 
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 * 
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package org.glassfish.web.deployment.io.runtime;

import static com.sun.enterprise.deployment.io.DescriptorConstants.PAYARA_WEB_JAR_ENTRY;
import static org.glassfish.web.sniffer.WarType.ARCHIVE_TYPE;

import java.util.List;
import java.util.Map;

import org.glassfish.deployment.common.Descriptor;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl;
import org.glassfish.web.deployment.node.runtime.gf.PayaraWebBundleRuntimeNode;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.deployment.io.ConfigurationDeploymentDescriptorFile;
import com.sun.enterprise.deployment.io.ConfigurationDeploymentDescriptorFileFor;
import com.sun.enterprise.deployment.node.RootXMLNode;

/**
 * Handles the XML Configuration for Payara
 * 
 * @author jonathan coustick
 */
@ConfigurationDeploymentDescriptorFileFor(ARCHIVE_TYPE)
@Service
@PerLookup
public class PayaraWebRuntimeDDFile extends ConfigurationDeploymentDescriptorFile {

    @Override
    public String getDeploymentDescriptorPath() {
        return PAYARA_WEB_JAR_ENTRY;
    }

    @Override
    public RootXMLNode getRootXMLNode(Descriptor descriptor) {
        if (descriptor instanceof WebBundleDescriptorImpl) {
            return new PayaraWebBundleRuntimeNode((WebBundleDescriptorImpl) descriptor);
        }

        return null;
    }

    @Override
    public void registerBundle(Map<String, Class<?>> registerMap, Map<String, String> publicIDToDTD, Map<String, List<Class<?>>> versionUpgrades) {
        registerMap.put(PayaraWebBundleRuntimeNode.registerBundle(publicIDToDTD, versionUpgrades), PayaraWebBundleRuntimeNode.class);
    }
}
