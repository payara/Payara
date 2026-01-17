/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) [2025] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/main/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.data.core.activation;

import com.sun.enterprise.module.HK2Module;
import fish.payara.data.core.connector.JakartaDataContainer;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.enterprise.deploy.shared.ModuleType;
import org.glassfish.api.container.Sniffer;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

/**
 * This is a sniffer for Jakarta Data to identify Repository annotation on an element before the deployment is done
 * @author Alfonso Valdez
 */
@Service
@PerLookup
public class JakartaDataRepositorySniffer implements Sniffer {
    
    @Override
    public boolean handles(DeploymentContext context) {
        final ReadableArchive archive = context.getSource();
        return handles(archive);
    }

    @Override
    public boolean handles(ReadableArchive source) {
        return false;
    }

    @Override
    public String[] getURLPatterns() {
        return new String[0];
    }

    @Override
    public Class<? extends Annotation>[] getAnnotationTypes() {
        return new Class[] {
                jakarta.data.repository.Repository.class
        };
    }

    @Override
    public String[] getAnnotationNames(DeploymentContext context) {
        final Class<?>[] types = getAnnotationTypes();
        if (types == null) {
            return null;
        }
        return Arrays.stream(types).map(Class::getName).collect(Collectors.toList()).toArray(String[]::new);
    }

    @Override
    public String getModuleType() {
        return "data";
    }

    @Override
    public HK2Module[] setup(String containerHome, Logger logger) throws IOException {
        return new HK2Module[0];
    }

    @Override
    public void tearDown() {

    }

    protected Class<?> getContainersClass() {
        return JakartaDataContainer.class;
    }

    @Override
    public String[] getContainersNames() {
        final String[] result = new String[1];
        result[0] = getContainersClass().getName();
        return result;
    }

    @Override
    public boolean isUserVisible() {
        return true;
    }

    @Override
    public boolean isJavaEE() {
        return false;
    }

    @Override
    public Map<String, String> getDeploymentConfigurations(ReadableArchive source) throws IOException {
        return Map.of();
    }

    @Override
    public String[] getIncompatibleSnifferTypes() {
        return new String[0];
    }

    @Override
    public boolean supportsArchiveType(ArchiveType archiveType) {
        return archiveType.toString().equals(ModuleType.WAR.toString()) ||
                archiveType.toString().equals(ModuleType.EJB.toString()) ||
                archiveType.toString().equals(ModuleType.CAR.toString());
    }
}
