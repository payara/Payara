/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.obrbuilder;

import org.glassfish.obrbuilder.xmlentities.ObrXmlReaderWriter;
import org.glassfish.obrbuilder.xmlentities.Repository;

import java.io.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

/**
 * Simple program which does the following:
 * 1) Reads obr.xml from a specified file.
 * 2) Reads OSGi bundle metadata for all bundles from a specified directory tree.
 * 3) Updates the OBR with the bundle information.
 * 4) Saves the new data in the same obr.xml. 
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class Main {
    public static void main(String[] args) throws Exception {
        if(args.length != 2) {
            System.out.println("Usage: java " + Main.class + " <directory containing OSGi bundles> <path to obr.xml>");
            return;
        }
        File dir = new File(args[0]);
        File obrXML = new File(args[1]);
        final ObrXmlReaderWriter obrParser = new ObrXmlReaderWriter();
        final Repository obr = obrXML.exists() ? obrParser.read(obrXML.toURI()) : new Repository();
        dir.listFiles(new FileFilter(){
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) {
                    pathname.listFiles(this);
                } else {
                    if(isBundle(pathname)) {
                        processBundle(obr, pathname);
                    }
                }
                return false;
            }
        });
        obrParser.write(obr, obrXML);
    }

    private static void processBundle(Repository obr, File bundle) {
        System.out.println("bundle = " + bundle.getAbsolutePath());
        // TODO(Sahoo): Add bundle details to obr
    }

    private static boolean isBundle(File file) {
        // Existence of any of these artifact is considered a bundle
        String headersToCheck[] = {"Bundle-ManifestVersion",
                "Bundle-SymbolicName",
                "Bundle-Version",
                "Export-Package",
                "Import-Package",
                "DynamicImport-Package"
        };
        try {
            JarFile jar = new JarFile(file);
            final Attributes attrs = jar.getManifest().getMainAttributes();
            for (String header : headersToCheck) {
                if (attrs.getValue(header) != null) return true;
            }
            jar.close();
        } catch (IOException e) {
            // ignore and continue
        }
        return false;
    }

}
