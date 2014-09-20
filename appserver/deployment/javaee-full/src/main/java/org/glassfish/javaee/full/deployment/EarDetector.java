/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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


package org.glassfish.javaee.full.deployment;

import java.io.IOException;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.api.deployment.archive.ArchiveDetector;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.deployment.EarType;
import com.sun.enterprise.deployment.deploy.shared.Util;

/**
 * Detects ear type archives.
 * It's rank can be set using system property {@link #EAR_DETECTOR_RANK_PROP}.
 * Default rank is {@link #DEFAULT_EAR_DETECTOR_RANK}.
 *
 * @author sanjeeb.sahoo@oracle.com
 */
@Service(name = EarDetector.ARCHIVE_TYPE)
@Singleton
public class EarDetector implements ArchiveDetector {

    public static final String EAR_DETECTOR_RANK_PROP = "glassfish.ear.detector.rank";
    public static final int DEFAULT_EAR_DETECTOR_RANK = 100;
    public static final String ARCHIVE_TYPE = EarType.ARCHIVE_TYPE;

    @Inject private ServiceLocator serviceLocator;
    @Inject private EarSniffer sniffer;
    @Inject private EarType archiveType;
    private ArchiveHandler archiveHandler;

    private static final String APPLICATION_XML = "META-INF/application.xml";
    private static final String SUN_APPLICATION_XML = "META-INF/sun-application.xml";
    private static final String GF_APPLICATION_XML = "META-INF/glassfish-application.xml";
    private static final String EAR_EXTENSION = ".ear";
    private static final String EXPANDED_WAR_SUFFIX = "_war";
    private static final String EXPANDED_RAR_SUFFIX = "_rar";
    private static final String EXPANDED_JAR_SUFFIX = "_jar";


    private Logger logger = Logger.getLogger(getClass().getPackage().getName());

    @Override
    public int rank() {
        return Integer.getInteger(EAR_DETECTOR_RANK_PROP, DEFAULT_EAR_DETECTOR_RANK);
    }

    @Override
    public boolean handles(ReadableArchive archive) throws IOException {
        boolean isEar = false;
        try{
            if (Util.getURIName(archive.getURI()).endsWith(EAR_EXTENSION)) {
                return true;
            }

            isEar = archive.exists(APPLICATION_XML) ||
                    archive.exists(SUN_APPLICATION_XML) ||
                    archive.exists(GF_APPLICATION_XML);

            if (!isEar) {
                isEar = isEARFromIntrospecting(archive);
            }
        }catch(IOException ioe){
            //ignore
        }
        return isEar;
    }

    @Override
    public ArchiveHandler getArchiveHandler() {
        synchronized (this) {
            if(archiveHandler == null) {
                try {
                    sniffer.setup(null, logger);
                } catch (IOException e) {
                    throw new RuntimeException(e); // TODO(Sahoo): Proper Exception Handling
                }
                archiveHandler = serviceLocator.getService(ArchiveHandler.class,ARCHIVE_TYPE);
            }
            return archiveHandler;
        }
    }

    @Override
    public ArchiveType getArchiveType() {
        return archiveType;
    }

    // introspecting the sub archives to see if any of them
    // ended with expected suffix
    private static boolean isEARFromIntrospecting(ReadableArchive archive)
        throws IOException {
        for (String entryName : archive.getDirectories()) {
            // we don't have other choices but to look if any of
            // the subdirectories is ended with expected suffix
            if ( entryName.endsWith(EXPANDED_WAR_SUFFIX) ||
                 entryName.endsWith(EXPANDED_RAR_SUFFIX) ||
                 entryName.endsWith(EXPANDED_JAR_SUFFIX) ) {
                return true;
            }
        }
        return false;
    }
}

