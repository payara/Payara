/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.glassfish.bootstrap;

import java.io.*;
import java.util.*;
import java.util.jar.*;
import java.nio.channels.*;
import java.nio.ByteBuffer;

/**
 * Created by IntelliJ IDEA.
 * User: dochez
 * Date: Nov 10, 2008
 * Time: 8:53:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class Rejar {

    public Rejar() {
    }

    public void rejar(File out, File modules) throws IOException {

        Map<String, ByteArrayOutputStream> metadata = new HashMap<>();
        try (FileOutputStream fos = new FileOutputStream(out)) {
            Set<String> names = new HashSet<>();
            names.add(Attributes.Name.MAIN_CLASS.toString());
            try (JarOutputStream jos = new JarOutputStream(fos, getManifest())) {
                processDirectory(jos, modules, names, metadata);
                File[] directories = modules.listFiles(pathname -> pathname.isDirectory());
                if (directories != null) {
                    for (File directory : directories) {
                        processDirectory(jos, directory, names, metadata);
                    }
                }
                // copy the inhabitants files.
                for (Map.Entry<String, ByteArrayOutputStream> e : metadata.entrySet()) {
                    copy(e.getValue().toByteArray(), e.getKey(), jos);
                }
                jos.flush();
            }
        }
    }

    protected Manifest getManifest() throws IOException {
        Manifest m = new Manifest();
        m.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        m.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "com.sun.enterprise.glassfish.bootstrap.ASMain");
        return m;
    }

    protected void processDirectory(JarOutputStream jos, File directory, Set<String> names, Map<String, ByteArrayOutputStream> metadata ) throws IOException {
        File[] jars = directory.listFiles(pathname -> pathname.getName().endsWith("jar"));
        if (jars != null) {
            for (File module : jars) {
                // add module
                try (JarFile in = new JarFile(module)) {
                    Enumeration<JarEntry> entries = in.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry je = entries.nextElement();
                        if (je.getName().endsWith("MANIFEST.MF") || names.contains(je.getName())) {
                            continue;
                        }
                        if (je.isDirectory())
                            continue;

                        if (je.getName().startsWith("META-INF/inhabitants/")
                                || je.getName().startsWith("META-INF/services/")) {
                            ByteArrayOutputStream stream = metadata.get(je.getName());
                            if (stream==null) {
                                metadata.put(je.getName(), stream = new ByteArrayOutputStream());
                            }
                            stream.write(("# from "+ module.getName() + "\n").getBytes());
                            copy(in, je, stream);
                        } else {
                            names.add(je.getName());
                            copy(in, je, jos);
                        }
                    }
                }
            }
        }
    }

    protected  void copy(JarFile in, JarEntry je, JarOutputStream jos) throws IOException {
        try {
            jos.putNextEntry(new JarEntry(je.getName()));
            copy(in, je, (OutputStream) jos);
        } finally {
            jos.flush();
            jos.closeEntry();
        }
    }

    protected void copy(JarFile in, JarEntry je, OutputStream os) throws IOException {
        copy(in, je, Channels.newChannel(os));
    }

    protected void copy(JarFile in, JarEntry je, WritableByteChannel out) throws IOException {
        try (ReadableByteChannel inChannel = Channels.newChannel(in.getInputStream(je))) {
            ByteBuffer byteBuffer = ByteBuffer.allocate(Long.valueOf(je.getSize()).intValue());
            inChannel.read(byteBuffer);
            byteBuffer.rewind();
            out.write(byteBuffer);
        }
    }

    protected void copy(byte[] bytes, String name, JarOutputStream jos) throws IOException {
        try {
            jos.putNextEntry(new JarEntry(name));
            jos.write(bytes);
        } finally {
            jos.closeEntry();
        }
    }
}
