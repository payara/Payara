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

package org.glassfish.appclient.client.acc;

import com.sun.enterprise.deploy.shared.ArchiveFactory;
import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.archivist.AppClientArchivist;
import com.sun.enterprise.deployment.archivist.Archivist;
import com.sun.enterprise.deployment.archivist.ArchivistFactory;
import com.sun.enterprise.deployment.util.DOLUtils;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.deployment.common.ModuleDescriptor;

import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import java.io.IOException;
import java.net.URI;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.RootDeploymentDescriptor;
import org.glassfish.hk2.api.ServiceLocator;
import org.xml.sax.SAXParseException;

import javax.enterprise.deploy.shared.ModuleType;

/**
 *
 * @author tjquinn
 */
public class UndeployedLaunchable implements Launchable {

    private static final LocalStringsImpl localStrings = new LocalStringsImpl(UndeployedLaunchable.class);

    private final String callerSuppliedMainClassName;

    private ApplicationClientDescriptor acDesc = null;

    private AppClientArchivist archivist = null;

    private final ReadableArchive clientRA;

    private ClassLoader classLoader = null;

    static UndeployedLaunchable newUndeployedLaunchable(
            final ServiceLocator habitat,
            final ReadableArchive ra,
            final String callerSuppliedMainClassName,
            final String callerSuppliedAppName,
            final ClassLoader classLoader) throws IOException, SAXParseException, UserError {

        ArchivistFactory af = Util.getArchivistFactory();

        /*
         * Try letting the factory decide what type of archive this is.  That
         * will often allow an app client or an EAR archive to be detected
         * automatically.
         */
        Archivist archivist = af.getArchivist(ModuleType.CAR.toString(), classLoader);
        if (archivist == null) {
            throw new UserError(localStrings.get("appclient.invalidArchive",
                    ra.getURI().toASCIIString()));
        }

        final ArchiveType moduleType = archivist.getModuleType();
        if (moduleType != null && moduleType.equals(DOLUtils.carType())) {
            return new UndeployedLaunchable(habitat, ra,
                    (AppClientArchivist) archivist, callerSuppliedMainClassName);
        } else if (moduleType != null && moduleType.equals(DOLUtils.earType())) {
            /*
             * Locate the app client submodule that matches the main class name
             * or the app client name.
             */

            Application app = (Application) archivist.open(ra);
            for (ModuleDescriptor<BundleDescriptor> md : app.getModules()) {
                if ( ! md.getModuleType().equals(DOLUtils.carType())) {
                    continue;
                }

                ApplicationClientDescriptor acd = (ApplicationClientDescriptor) md.getDescriptor();

                final String displayName = acd.getDisplayName();
                final String appName = acd.getModuleID();

                ArchiveFactory archiveFactory = Util.getArchiveFactory();
                ReadableArchive clientRA = archiveFactory.openArchive(ra.getURI().resolve(md.getArchiveUri()));

                /*
                 * Choose this nested app client if the caller-supplied name
                 * matches, or if the caller-supplied main class matches, or
                 * if neither was provided.  
                 */
                final boolean useThisClient =
                        (displayName != null && displayName.equals(callerSuppliedAppName))
                     || (appName != null && appName.equals(callerSuppliedAppName))
                     || (callerSuppliedMainClassName != null && clientRA.exists(classToResource(callerSuppliedMainClassName))
                     || (callerSuppliedAppName == null && callerSuppliedMainClassName == null));

                if (useThisClient) {
                    return new UndeployedLaunchable(habitat, clientRA, acd,
                            callerSuppliedMainClassName);
                }
                clientRA.close();
            }

            throw new UserError(localStrings.get("appclient.noMatchingClientInEAR",
                    ra.getURI(), callerSuppliedMainClassName, callerSuppliedAppName));
        } else {
            /*
             * There is a possibility that the user is trying to launch an
             * archive that is more than one type of archive: such as an EJB
             * but also an app client (because the manifest identifies a main
             * class, for example).
             *
             * Earlier the archivist factory might have returned the other type
             * of archivist - such as the EJB archivist.  Now see if the app
             * client archivist will work when selected directly.
             */
            archivist = af.getArchivist(DOLUtils.carType());

            /*
             * Try to open the archive as an app client archive just to see
             * if it works.
             */
            RootDeploymentDescriptor tempACD = archivist.open(ra);
            if (tempACD != null && tempACD instanceof ApplicationClientDescriptor) {
                /*
                 * Start with a fresh archivist - unopened - so we can request
                 * anno processing, etc. before opening it for real.
                 */
                archivist = af.getArchivist(DOLUtils.carType());
                return new UndeployedLaunchable(habitat, ra, (AppClientArchivist) archivist,
                                callerSuppliedMainClassName);
            }
            throw new UserError(
                    localStrings.get("appclient.unexpectedArchive", ra.getURI()));
        }
    }

    public URI getURI() {
        return clientRA.getURI();
    }

    public String getAnchorDir() {
        return null;
    }

    private static String classToResource(final String className) {
        return className.replace('.', '/') + ".class";
    }

    private UndeployedLaunchable(final ServiceLocator habitat,
            final ReadableArchive clientRA,
            final String callerSuppliedMainClass) throws IOException, SAXParseException {
        this.callerSuppliedMainClassName = callerSuppliedMainClass;
        this.clientRA = clientRA;
    }
    private UndeployedLaunchable(final ServiceLocator habitat,
            final ReadableArchive clientRA,
            final ApplicationClientDescriptor acd,
            final String callerSuppliedMainClass) throws IOException, SAXParseException {
        this(habitat, clientRA, callerSuppliedMainClass);
        this.acDesc = acd;
    }

    private UndeployedLaunchable(final ServiceLocator habitat,
            final ReadableArchive clientRA,
            final AppClientArchivist archivist,
            final String callerSuppliedMainClass) throws IOException, SAXParseException {
        this(habitat, clientRA, callerSuppliedMainClass);
        this.archivist = completeInit(archivist);
    }

    public Class getMainClass() throws ClassNotFoundException {
        try {
            String mainClassName = mainClassNameToLaunch();
            return Class.forName(mainClassName, true, getClassLoader());
        } catch (Exception e) {
            throw new ClassNotFoundException("<mainclass>");
        }
    }

    private ClassLoader getClassLoader() {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
        return classLoader;
    }

    private String mainClassNameToLaunch() throws IOException, SAXParseException {
        return (callerSuppliedMainClassName != null ? callerSuppliedMainClassName :
            extractMainClassFromArchive(clientRA));
    }

    private String extractMainClassFromArchive(final ReadableArchive clientRA) throws IOException  {
        final Manifest mf = clientRA.getManifest();
        if (mf == null) {
            return null;
        }
        return mf.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
    }

    public ApplicationClientDescriptor getDescriptor(final URLClassLoader loader) throws IOException, SAXParseException {
        this.classLoader = loader;
        if (acDesc == null) {
            final AppClientArchivist _archivist = getArchivist(
                    AccessController.doPrivileged(new PrivilegedAction<ACCClassLoader>() {

                        @Override
                        public ACCClassLoader run() {
                            return new ACCClassLoader(loader.getURLs(), loader.getParent());
                        }
                    }));
                    
            _archivist.setAnnotationProcessingRequested(true);
            acDesc = _archivist.open(clientRA);
            
            Application.createVirtualApplication(null, acDesc.getModuleDescriptor());
            acDesc.getApplication().setAppName(getDefaultApplicationName(clientRA));
        }
        return acDesc;
    }

    public String getDefaultApplicationName(ReadableArchive archive) {
        String appName = archive.getName();
        int lastDot = appName.lastIndexOf('.');
        if (lastDot != -1) {
            if (appName.substring(lastDot).equalsIgnoreCase(".ear") ||
                appName.substring(lastDot).equalsIgnoreCase(".jar")) {
                appName = appName.substring(0, lastDot);
            }
        }
        return appName;
    }

    private AppClientArchivist completeInit(final AppClientArchivist arch) {
            arch.setDescriptor(acDesc);
            arch.setAnnotationProcessingRequested(true);
            return arch;
    }

    private AppClientArchivist getArchivist(final ClassLoader classLoader) throws IOException {
        if (archivist == null) {
            ArchivistFactory af = Util.getArchivistFactory();

            archivist = completeInit((AppClientArchivist) af.getArchivist(ModuleType.CAR.toString()));
        }
        archivist.setClassLoader(classLoader);
        return archivist;
    }

    public void validateDescriptor() {
        try {
            getArchivist(classLoader).validate(classLoader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
