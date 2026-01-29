/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.microprofile.connector;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.sun.enterprise.module.HK2Module;

import org.glassfish.api.container.Sniffer;
import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.web.sniffer.WarType;
import org.jvnet.hk2.annotations.Contract;

@Contract
public abstract class MicroProfileSniffer implements Sniffer {

    private static final Logger LOGGER = Logger.getLogger(MicroProfileSniffer.class.getName());

    @Override
    public boolean handles(DeploymentContext context) {
        final ReadableArchive archive = context.getSource();

        final String archivePath = archive.getURI().getPath();
        // Ignore system applications
        if (archivePath.contains("glassfish/lib/install")) {
            return false;
        }
        if (archivePath.contains("h2db/bin")) {
            return false;
        }
        if (archivePath.contains("mq/lib")) {
            return false;
        }

        return handles(archive);
    }

    protected abstract Class<?> getContainersClass();

    @Override
    public final String[] getContainersNames() {
        final String[] result = new String[1];
        result[0] = getContainersClass().getName();
        return result;
    }

    @Override
    public boolean handles(ReadableArchive archive) {
        return false;
    }

    @Override
    public final String[] getURLPatterns() {
        return new String[0];
    }

    @Override
    public final String[] getAnnotationNames(DeploymentContext ctx) {
        final Class<?>[] types = getAnnotationTypes();
        if (types == null) {
            return null;
        }
        final String[] names = new String[types.length];
        for (int i = 0; i < names.length; i++) {
            names[i] = types[i].getName();
        }
        return names;
    }

    @Override
    public final HK2Module[] setup(String containerHome, Logger logger) throws IOException {
        return new HK2Module[0];
    }

    @Override
    public final void tearDown() {
    }

    @Override
    public final boolean supportsArchiveType(ArchiveType type) {
        final String extension = (type == null)? null : type.getExtension();
        switch (extension) {
            //case EarType.ARCHIVE_EXTENSION:
            case WarType.ARCHIVE_EXTENSION:
            //case EjbType.ARCHIVE_EXTENSION:
                return true;
            default:
                LOGGER.fine("Unsupported ArchiveType: " + extension);
        }
        return false;
    }

    @Override
    public String[] getIncompatibleSnifferTypes() {
        final String[] types = new String[1];
        types[0] = "connector";
        return types;
    }
    
    @Override
    public final boolean isJavaEE() {
        return false;
    }

    @Override
    public boolean isUserVisible() {
        return true;
    }

    @Override
    public Map<String, String> getDeploymentConfigurations(ReadableArchive archive) throws IOException {
        return new HashMap<>();
    }

}
