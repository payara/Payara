/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.tools.verifier;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.ArchiveHandler;
import org.glassfish.api.deployment.archive.Archive;
import org.glassfish.api.deployment.DeployCommandParameters;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.internal.deployment.Deployment;
import com.sun.enterprise.deployment.archivist.Archivist;
import com.sun.enterprise.deployment.archivist.ArchivistFactory;
import com.sun.enterprise.deployment.archivist.ApplicationFactory;
import com.sun.enterprise.config.serverbeans.DasConfig;
import com.sun.enterprise.deployment.Application;
import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.util.ApplicationVisitor;
import com.sun.enterprise.deployment.util.ApplicationValidator;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.deployment.common.DeploymentContextImpl;
import org.glassfish.deployment.common.DeploymentUtils;
import org.glassfish.internal.deployment.ExtendedDeploymentContext;
import com.sun.enterprise.v3.common.HTMLActionReporter;
import org.glassfish.api.ActionReport;

import org.jvnet.hk2.annotations.Service;
import javax.inject.Inject;

import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.IOException;

/**
 * @author Hong.Zhang@Sun.COM
 * @author Sanjeeb.Sahoo@Sun.COM
 */
@Service
public class DescriptorFactory
{
    public static class ResultHolder {
        Application application;
        Archive archive;
    }

    @Inject 
    Deployment deployment;

    @Inject
    protected ArchiveFactory archiveFactory;

    @Inject
    ArchivistFactory archivistFactory;

    @Inject
    protected ApplicationFactory applicationFactory;

    @Inject
    DasConfig dasConfig;

    @Inject
    ServerEnvironment env;

    /**
     * Returns the parsed DOL object from archive
     *
     * @param archiveFile original archive file
     * @param destRootDir root destination directory where the application
     *        should be expanded under in case of archive deployment
     * @param parentCl parent classloader
     *
     * @return the parsed DOL object
     */
    public ResultHolder createApplicationDescriptor(File archiveFile, File destRootDir, ClassLoader parentCl) throws IOException {
        ReadableArchive archive = null;
        Application application = null;
        try {
            Descriptor.setBoundsChecking(false);
            archive = archiveFactory.openArchive(archiveFile);
            ArchiveHandler archiveHandler = deployment.getArchiveHandler(archive);
            ActionReport dummyReport = new HTMLActionReporter();

            String appName = DeploymentUtils.getDefaultEEName(archiveFile.getName());

            DeployCommandParameters params = new DeployCommandParameters();
            params.name = appName;

            ExtendedDeploymentContext context = new DeploymentContextImpl(dummyReport, archive, params, env);
            context.setArchiveHandler(archiveHandler);

            if (!archiveFile.isDirectory()) {
                // expand archive
                File destDir = new File(destRootDir, appName);
                if (destDir.exists()) {
                    FileUtils.whack(destDir);
                }
                destDir.mkdirs();
                archiveHandler.expand(archive, archiveFactory.createArchive(destDir), context);
                archive.close();
                archive = archiveFactory.openArchive(destDir);
                context.setSource(archive);
            }

            context.addTransientAppMetaData(ExtendedDeploymentContext.IS_TEMP_CLASSLOADER, Boolean.TRUE); // issue 14564
            String archiveType = context.getArchiveHandler().getArchiveType();
            ClassLoader cl = archiveHandler.getClassLoader(parentCl, context);
            Archivist archivist = archivistFactory.getArchivist(archiveType, cl);
            if (archivist == null) {
                throw new IOException("Cannot determine the Java EE module type for " + archive.getURI());
            }
            archivist.setAnnotationProcessingRequested(true);
            String xmlValidationLevel = dasConfig.getDeployXmlValidation();
            archivist.setXMLValidationLevel(xmlValidationLevel);
            if (xmlValidationLevel.equals("none")) {
                archivist.setXMLValidation(false);
            }
            archivist.setRuntimeXMLValidation(false);
            try {
                application = applicationFactory.openArchive(
                        appName, archivist, archive, true);
            } catch(SAXParseException e) {
                throw new IOException(e);
            }
            if (application != null) {
                application.setClassLoader(cl);
                application.visit((ApplicationVisitor) new ApplicationValidator());
            }
        } finally {
            if (archive != null) {
                archive.close();
            }
            // We need to reset it after descriptor building
            Descriptor.setBoundsChecking(true);
        }

        ResultHolder result = new ResultHolder();
        result.application = application;
        result.archive = archive;
        return result;
    }

}
