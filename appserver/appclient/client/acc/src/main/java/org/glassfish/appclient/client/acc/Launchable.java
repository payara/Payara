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
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.archivist.AppClientArchivist;
import com.sun.enterprise.module.bootstrap.BootException;
import com.sun.enterprise.util.LocalStringManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import javax.xml.stream.XMLStreamException;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.hk2.api.ServiceLocator;
import org.xml.sax.SAXParseException;

/**
 * Something launchable by the ACC - an app client archive or a class.
 *
 * @author tjquinn
 */
interface Launchable {

    /**
     * Returns the main class for this Launchable.
     * @return the main class
     *
     * @throws java.lang.ClassNotFoundException
     */
    Class getMainClass() throws ClassNotFoundException;

    ApplicationClientDescriptor getDescriptor(URLClassLoader loader) throws IOException, SAXParseException;

    void validateDescriptor();

    URI getURI();

    String getAnchorDir();

    static class LaunchableUtil {

        private static final LocalStringManager localStrings = new LocalStringManagerImpl(Launchable.LaunchableUtil.class);
        static Launchable newLaunchable(final URI uri,
                final String callerSuppliedMainClassName,
                final String callerSuppliedAppName,
                final ServiceLocator habitat) throws IOException, BootException, URISyntaxException, XMLStreamException, SAXParseException, UserError {
            /*
             * Make sure the requested URI exists and is readable.
             */
            ArchiveFactory af = ACCModulesManager.getService(ArchiveFactory.class);
            ReadableArchive ra = null;
            try {
                ra = af.openArchive(uri);
            } catch (IOException e) {
                final String msg = localStrings.getLocalString(
                        Launchable.class,
                        "appclient.cannotFindJarFile",
                        "Could not locate the requested client JAR file {0}; please try again with an existing, valid client JAR",
                        new Object[] {uri});
                throw new UserError(msg);
            }

            Launchable result = FacadeLaunchable.newFacade(
                    habitat, ra, callerSuppliedMainClassName, callerSuppliedAppName);
            if (result == null) {
                /*
                 * If newFacade found a facade JAR but could not find a suitable
                 * client it will have thrown a UserError.  If we're here, then
                 * newFacade did not have a facade to work with.  So the caller-
                 * provided URI should refer to an undeployed EAR or an undeployed
                 * app client JAR.
                 */
                result = UndeployedLaunchable.newUndeployedLaunchable(habitat, ra,
                        callerSuppliedMainClassName, callerSuppliedAppName,
                        Thread.currentThread().getContextClassLoader());
            }
            if ( ! (result instanceof JWSFacadeLaunchable)) {
                URL clientOrFacadeURL = new URL("file:" + result.getURI().getSchemeSpecificPart());
                /*
                 * For the embedded case especially there might not be an
                 * ACCClassLoader instance yet.  Create one if needed
                 * before proceeding.
                 */
                ACCClassLoader cl = ACCClassLoader.instance();
                if (cl == null) {
                    cl = ACCClassLoader.newInstance(Thread.currentThread().getContextClassLoader(), false);
                }
                cl.appendURL(clientOrFacadeURL);
            }
            return result;
        }
        
        static Launchable newLaunchable(final ServiceLocator habitat, final Class mainClass) {
            return new MainClassLaunchable(habitat, mainClass);
        }

        static ApplicationClientDescriptor openWithAnnoProcessingAndTempLoader(
                final AppClientArchivist archivist, final URLClassLoader loader,
                final ReadableArchive facadeRA,
                final ReadableArchive clientRA) throws IOException, SAXParseException {
            archivist.setAnnotationProcessingRequested(true);
            final ACCClassLoader tempLoader = AccessController.doPrivileged(
                    new PrivilegedAction<ACCClassLoader>() {

                        @Override
                        public ACCClassLoader run() {
                            return new ACCClassLoader(loader.getURLs(), loader.getParent());
                        }
                    }
                );
            archivist.setClassLoader(tempLoader);

            final ApplicationClientDescriptor acDesc = archivist.open(facadeRA, clientRA);
            archivist.setDescriptor(acDesc);
            return acDesc;

        }
        static boolean matchesAnyClass(final ReadableArchive archive, final String callerSpecifiedMainClassName) throws IOException {
            return (callerSpecifiedMainClassName != null) &&
                            archive.exists(classNameToArchivePath(callerSpecifiedMainClassName));
        }

        static String moduleID(
                final URI groupFacadeURI,
                final URI clientURI,
                final ApplicationClientDescriptor clientFacadeDescriptor) {
            String moduleID = clientFacadeDescriptor.getModuleID();
            /*
             * If the moduleID was never set explicitly in the descriptor then
             * it will fall back to  the URI of the archive...ending in .jar we
             * presume.  In that case, change the module ID to be the path
             * relative to the downloaded root directory.
             */
            if (moduleID.endsWith(".jar")) {
                moduleID = deriveModuleID(groupFacadeURI, clientURI);
            }
            return moduleID;
        }

        static boolean matchesName(
                final String moduleID,
                final URI groupFacadeURI,
                final ApplicationClientDescriptor clientFacadeDescriptor,
                final String appClientName) throws IOException, SAXParseException {

            /*
             * The ReadableArchive argument should be the facade archive and
             * not the developer's original one, because when we try to open it
             * the archivist needs to have the augmented descriptor (which is
             * in the client facade) not the minimal or non-existent
             * descriptor (which could be in the developer's original client).
             */
            final String displayName = clientFacadeDescriptor.getDisplayName();
            return (   (moduleID != null && moduleID.equals(appClientName))
                    || (displayName != null && displayName.equals(appClientName)));
        }

        private static String classNameToArchivePath(final String className) {
            return new StringBuilder(className.replace('.', '/'))
                    .append(".class").toString();
        }

        private static String deriveModuleID(final URI groupFacadeURI,
                final URI clientArchiveURI) {
            /*
             * The groupFacadeURI will be something like x/y/appName.jar and
             * the clientArchiveURI will be something like x/y/appName/a/b/clientName.jar.
             * The derived moduleID should be the client archive's URI relative
             * to the x/y/appName directory with no file type: in this example a/b/clientName
             */
            URI dirURI = stripDotJar(groupFacadeURI);
            URI clientArchiveRelativeURI = stripDotJar(
                    dirURI.relativize(URI.create("file:" + clientArchiveURI.getRawSchemeSpecificPart())));
            return clientArchiveRelativeURI.getRawSchemeSpecificPart();
        }

        private static URI stripDotJar(final URI uri) {
            String pathWithoutDotJar = uri.getRawSchemeSpecificPart();
            pathWithoutDotJar = pathWithoutDotJar.substring(0,
                    pathWithoutDotJar.length() - ".jar".length());
            return URI.create("file:" + pathWithoutDotJar);
        }
    }
}
