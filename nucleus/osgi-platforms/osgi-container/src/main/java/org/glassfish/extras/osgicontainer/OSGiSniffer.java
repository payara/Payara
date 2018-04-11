/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package org.glassfish.extras.osgicontainer;

import org.glassfish.api.deployment.DeploymentContext;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.internal.deployment.GenericSniffer;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;

/**
 * Sniffer for OSGi bundles
 *
 * @author Jerome Dochez, Sanjeeb Sahoo
 */

@Service(name = "osgi")
public class OSGiSniffer extends GenericSniffer  {

    /*
    It should extends from GenericCompositeSniffer, but I think there is some issues in deployment backend if we
    model it as a composite sniffer, so for now we treat osgi slightly differently.
     */

    @Inject
    private OSGiArchiveType osgiArchiveType;
    public static final String CONTAINER_NAME = "osgi";

    public OSGiSniffer() {
        super(CONTAINER_NAME, null, null);
    }

    @Override
    public boolean handles(ReadableArchive location) {
        // I always return false, unless --type is used to specifically request OSGi
        // bundle installation.
        return false;
    }

    @Override
    public String[] getContainersNames() {
        return new String[]{CONTAINER_NAME};
    }

    @Override
    /**
     * @return whether this sniffer should be visible to user
     *
     */
    public boolean isUserVisible() {
        return true;
    }

    @Override
    public boolean handles(DeploymentContext context) {
        ArchiveType archiveType = habitat.getService(ArchiveType.class, context.getArchiveHandler().getArchiveType());
        return supportsArchiveType(archiveType);
    }

    /**
     *
     * This API is used to help determine if the sniffer should recognize
     * the current archive.
     * If the sniffer does not support the archive type associated with
     * the current deployment, the sniffer should not recognize the archive.
     *
     * @param archiveType the archive type to check
     * @return whether the sniffer supports the archive type
     *
     */
    @Override
    public boolean supportsArchiveType(ArchiveType archiveType) {
        if (archiveType.toString().equals(osgiArchiveType.toString())) {
            return true;
        }
        return false;
    }
}
