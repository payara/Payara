/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices.connector;

import org.glassfish.internal.deployment.GenericSniffer;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Singleton;
import javax.enterprise.deploy.shared.ModuleType;

import java.io.IOException;

/**
 * This is the Sniffer for Webservices
 * @author Bhakti Mehta
 */
@Service(name="webservices")
@Singleton
public class WebServicesSniffer extends GenericSniffer {

    private static final Class[]  handledAnnotations = new Class[] {javax.jws.WebService.class,
            javax.xml.ws.WebServiceProvider.class, javax.xml.ws.WebServiceRef.class};

    final String[] containers = {
        "org.glassfish.webservices.WebServicesContainer",
        "org.glassfish.webservices.metroglue.MetroContainer"
    };

    public WebServicesSniffer() {
        super("webservices", null, null);
    }

    /**
     * .ear (the resource can be present in lib dir of the ear)
     * Returns true if the archive contains webservices.xml either in WEB-INF or META-INF directories
     */
    @Override
    public boolean handles(ReadableArchive location) {
        return isEntryPresent(location, "WEB-INF/webservices.xml") ||
                isEntryPresent(location, "META-INF/webservices.xml");
    }

    private boolean isEntryPresent(ReadableArchive location, String entry) {
        boolean entryPresent = false;
        try {
            entryPresent = location.exists(entry);
        } catch (IOException e) {
            // ignore
        }
        return entryPresent;
    }

    @Override
    public String[] getContainersNames() {
        return containers;
    }

    @Override
    public Class<? extends java.lang.annotation.Annotation>[] getAnnotationTypes() {
        return handledAnnotations;
    }

    @Override
    public boolean isUserVisible() {
        return true;
    }

    @Override
    public String[] getURLPatterns() {
        return null;
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
    public boolean supportsArchiveType(ArchiveType archiveType) {
        if (archiveType.toString().equals(ModuleType.WAR.toString()) ||
            archiveType.toString().equals(ModuleType.EJB.toString())) {
            return true;
        }
        return false;
    }
}
