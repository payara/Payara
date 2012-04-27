/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.util;

import com.sun.enterprise.deployment.annotation.introspection.AnnotationScanner;
import com.sun.enterprise.deployment.annotation.introspection.ClassFile;
import com.sun.enterprise.deployment.annotation.introspection.ConstantPoolInfo;
import com.sun.enterprise.deployment.util.DOLUtils;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.hk2.classmodel.reflect.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileFilter;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.Collection;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URI;
import java.net.URL;

/**
 * Abstract superclass for specific types of annotation detectors.
 *
 * @author Jerome Dochez
 */
public class AnnotationDetector {
    
    protected final ClassFile classFile;
    protected final AnnotationScanner scanner;

    public AnnotationDetector(AnnotationScanner scanner) {
        this.scanner = scanner;
        ConstantPoolInfo poolInfo = new ConstantPoolInfo(scanner);
        classFile = new ClassFile(poolInfo);
    }
    
    public boolean hasAnnotationInArchiveWithNoScanning(ReadableArchive archive) throws IOException {
        Types types = null;
        if (archive.getParentArchive() != null) {
            types = archive.getParentArchive().getExtraData(Types.class);
        } else {
            types = archive.getExtraData(Types.class);
        }

        // we are on the client side so we need to scan annotations
        if (types == null) {
            return hasAnnotationInArchive(archive);
        }

        List<URI> uris = new ArrayList<URI>();
        uris.add(archive.getURI());
        try {
            uris.addAll(DOLUtils.getLibraryJarURIs(null, archive));
        } catch (Exception e) {
            DOLUtils.getDefaultLogger().log(Level.WARNING, e.getMessage(), e);
        }


        // force populating the annotations field in the scanner
        scanner.isAnnotation("foo");

        Set<String> annotations = scanner.getAnnotations();
        if (annotations == null) {
            return false;
        }

        for (String annotationType : annotations)  {
            Type type = types.getBy(annotationType);
             // we never found anyone using that type
            if (type==null) continue;
            if (type instanceof AnnotationType) {
                Collection<AnnotatedElement> elements = ((AnnotationType) type).allAnnotatedTypes();
                for (AnnotatedElement element : elements) {
                    Type t = (element instanceof Member?((Member) element).getDeclaringType():(Type) element);
                    if (t.wasDefinedIn(uris)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean hasAnnotationInArchive(ReadableArchive archive) throws IOException {

        Enumeration<String> entries = archive.entries();
        while (entries.hasMoreElements()) {
            String entryName = entries.nextElement();
            if (entryName.endsWith(".class")) {
                if (containsAnnotation(archive, entryName)) {
                    return true;
                }
            } 
        }
        return false;
    }

    public boolean containsAnnotation(ReadableArchive archive, String entryName) throws IOException {
        return containsAnnotation(archive.getEntry(entryName), archive.getEntrySize(entryName));    
    }

    protected boolean containsAnnotation(InputStream is, long size) 
        throws IOException {
        boolean result = false;
        // check if it contains top level annotations...
        ReadableByteChannel channel = null;
        try {
            channel = Channels.newChannel(is);
            if (channel!=null) {
                result = classFile.containsAnnotation(channel, size);
             }
             return result;
        } finally {
            if (channel != null) {
                channel.close();
            }
        }
    }
}
