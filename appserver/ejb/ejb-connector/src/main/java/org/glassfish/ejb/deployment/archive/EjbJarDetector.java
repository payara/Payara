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


package org.glassfish.ejb.deployment.archive;

import org.glassfish.api.deployment.archive.ArchiveDetector;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.deployment.common.GenericAnnotationDetector;
import org.glassfish.hk2.api.ServiceLocator;

import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Singleton;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Detects EJB jar type archive.
 * It's rank can be set using system property {@link #EJB_JAR_DETECTOR_RANK_PROP}.
 * Default rank is {@link #DEFAULT_EJB_JAR_DETECTOR_RANK}.
 *
 * @author sanjeeb.sahoo@oracle.com
 */
@Service(name = EjbJarDetector.ARCHIVE_TYPE)
@Singleton
public class EjbJarDetector implements ArchiveDetector {
    public static final String EJB_JAR_DETECTOR_RANK_PROP = "glassfish.ejb.jar.detector.rank";
    public static final int DEFAULT_EJB_JAR_DETECTOR_RANK = 400;
    public static final String ARCHIVE_TYPE = EjbType.ARCHIVE_TYPE;

    @Inject
    private EjbSniffer sniffer;
    @Inject
    private EjbType archiveType;
    @Inject
    private ServiceLocator baseServiceLocator;
    
    private ArchiveHandler archiveHandler; // lazy initialisation
    private Logger logger = Logger.getLogger(getClass().getPackage().getName());

    private static final String EJB_JAR_XML = "META-INF/ejb-jar.xml";
    private static final String SUN_EJB_JAR_XML = "META-INF/sun-ejb-jar.xml";
    private static final String GF_EJB_JAR_XML = "META-INF/glassfish-ejb-jar.xml";
    @Override
    public int rank() {
        return Integer.getInteger(EJB_JAR_DETECTOR_RANK_PROP, DEFAULT_EJB_JAR_DETECTOR_RANK);
    }

    @Override
    public boolean handles(ReadableArchive archive) {
        try {
            if (archive.exists(EJB_JAR_XML) ||
                archive.exists(SUN_EJB_JAR_XML) ||
                archive.exists(GF_EJB_JAR_XML)) {
                return true;
            }

            GenericAnnotationDetector detector =
                new GenericAnnotationDetector(sniffer.getAnnotationTypes());

            return detector.hasAnnotationInArchive(archive);
        }catch(IOException ioe){
            //ignore
        }
        return false;
    }

    @Override
    public ArchiveHandler getArchiveHandler() {
        synchronized (this) {
            if (archiveHandler == null) {
                try {
                    sniffer.setup(null, logger);
                } catch (IOException e) {
                    throw new RuntimeException(e); // TODO(Sahoo): Proper Exception Handling
                }
                archiveHandler = baseServiceLocator.getService(ArchiveHandler.class, ARCHIVE_TYPE);
            }
            return archiveHandler;
        }
    }

    @Override
    public ArchiveType getArchiveType() {
        return archiveType;
    }
}
