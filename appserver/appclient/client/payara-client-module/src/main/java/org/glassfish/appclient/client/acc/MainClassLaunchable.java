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

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.deployment.ApplicationClientDescriptor;
import com.sun.enterprise.deployment.archivist.AppClientArchivist;
import com.sun.enterprise.deployment.archivist.ArchivistFactory;
import com.sun.enterprise.deployment.deploy.shared.MemoryMappedArchive;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.hk2.api.ServiceLocator;
import org.xml.sax.SAXParseException;

/**
 * Represents a Launchable main class which the caller specifies by the
 * main class itself, rather than a facade JAR or an original developer-provided
 * JAR file.
 *
 * @author tjquinn
 */
public class
        MainClassLaunchable implements Launchable {

    private final Class mainClass;
    private ApplicationClientDescriptor acDesc = null;
    private ClassLoader classLoader = null;
    private AppClientArchivist archivist = null;

    MainClassLaunchable(final ServiceLocator habitat, final Class mainClass) {
        super();
        this.mainClass = mainClass;
    }

    public Class getMainClass() throws ClassNotFoundException {
        return mainClass;
    }

    public ApplicationClientDescriptor getDescriptor(final URLClassLoader loader) throws IOException, SAXParseException {
        /*
         * There is no developer-provided descriptor possible so just
         * use a default one.
         */
        if (acDesc == null) {
            ReadableArchive tempArchive = null;
            final ACCClassLoader tempLoader = AccessController.doPrivileged(
                    new PrivilegedAction<ACCClassLoader>() {

                        @Override
                        public ACCClassLoader run() {
                            return new ACCClassLoader(loader.getURLs(), loader.getParent());
                        }
                    });
            tempArchive = createArchive(tempLoader, mainClass);
            final AppClientArchivist acArchivist = getArchivist(tempArchive, tempLoader);
            archivist.setClassLoader(tempLoader);
            archivist.setDescriptor(acDesc);
            archivist.setAnnotationProcessingRequested(true);
            acDesc = acArchivist.open(tempArchive);
            Application.createVirtualApplication(null, acDesc.getModuleDescriptor());
            acDesc.getApplication().setAppName(appNameFromMainClass(mainClass));
            this.classLoader = loader;
        }
        return acDesc;
    }

    private String appNameFromMainClass(final Class c) {
        return c.getName();
    }
    
//    private ReadableArchive createArchive(final ClassLoader loader,
//            final Class mainClass) throws IOException, URISyntaxException {
//        Manifest mf = new Manifest();
//        mf.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass.getName());
//        final File tempFile = File.createTempFile("acc", ".jar");
//        tempFile.deleteOnExit();
//        JarOutputStream jos = new JarOutputStream(
//                new BufferedOutputStream(new FileOutputStream(tempFile)), mf);
//        final String mainClassResourceName = mainClass.getName().replace('.', '/') + ".class";
//        final ZipEntry mainClassEntry = new ZipEntry(mainClassResourceName);
//        jos.putNextEntry(mainClassEntry);
//        InputStream is = loader.getResourceAsStream(mainClassResourceName);
//        int bytesRead;
//        byte[] buffer = new byte[1024];
//        while ( (bytesRead = is.read(buffer)) != -1) {
//            jos.write(buffer, 0, bytesRead);
//        }
//        is.close();
//        jos.closeEntry();
//        jos.close();
//
//        final InputJarArchive result = new InputJarArchive();
//        result.open(new URI("jar", tempFile.toURI().toASCIIString(), null));
//        return result;
//    }

    private ReadableArchive createArchive(final ClassLoader loader,
            final Class mainClass) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Manifest mf = new Manifest();
        Attributes mainAttrs = mf.getMainAttributes();
        /*
         * Note - must set the version or the attributes won't write
         * themselves to the output stream!
         */
        mainAttrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        mainAttrs.put(Attributes.Name.MAIN_CLASS, mainClass.getName());
        JarOutputStream jos = new JarOutputStream(baos, mf);

        final String mainClassResourceName = mainClass.getName().replace('.', '/') + ".class";
        final ZipEntry mainClassEntry = new ZipEntry(mainClassResourceName);
        jos.putNextEntry(mainClassEntry);
        InputStream is = loader.getResourceAsStream(mainClassResourceName);
        int bytesRead;
        byte[] buffer = new byte[1024];
        while ( (bytesRead = is.read(buffer)) != -1) {
            jos.write(buffer, 0, bytesRead);
        }
        is.close();
        jos.closeEntry();
        jos.close();

        MemoryMappedArchive mma = new MemoryMappedArchive(baos.toByteArray());
        /*
         * Some archive-related processing looks for the file type from the URI, so set it
         * to something.
         */
        mma.setURI(URI.create("file:///tempClient.jar"));

        return mma;
    }


    private AppClientArchivist getArchivist(final ReadableArchive clientRA,
            final ClassLoader classLoader) throws IOException {
        if (archivist == null) {
            ArchivistFactory af = Util.getArchivistFactory();
            /*
             * Get the archivist by type rather than by archive to avoid
             * having to set the URI to some fake URI that the archivist
             * factory would understand.
             */
            archivist = completeInit((AppClientArchivist) 
                    af.getArchivist(DOLUtils.carType()));
        }
        return archivist;
    }

    private AppClientArchivist completeInit(final AppClientArchivist arch) {
            arch.setAnnotationProcessingRequested(true);
            return arch;
    }

    public void validateDescriptor() {
        archivist.validate(classLoader);
    }

    public URI getURI() {
        return null;
    }

    public String getAnchorDir() {
        return null;
    }
}
