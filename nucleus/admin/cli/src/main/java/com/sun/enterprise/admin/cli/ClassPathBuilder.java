/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.cli;

import java.io.File;
import java.io.FileFilter;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Builds up a classpath.
 *
 * @author Kohsuke Kawaguchi
 */
public class ClassPathBuilder implements Iterable<File> {
    private final List<File> elements = new ArrayList<File>();

    public Iterator<File> iterator() {
        return elements.iterator();
    }

    /**
     * Adds a single jar file or a class file directory.
     */
    public ClassPathBuilder add(File f) {
        elements.add(f);
        return this;
    }

    /**
     * Allows one to write {@code add(f,"lib","a.jar")} instead of
     * <tt>add(new File(new File(f,"lib"),"a.jar")</tt>
     */
    public ClassPathBuilder add(File f, String... pathFragments) {
        for (String p : pathFragments)
            f = new File(f,p);
        return add(f);
    }

    /**
     * Adds all the files in the given directory that match the given filter.
     */
    public ClassPathBuilder addAll(File dir, FileFilter filter) {
        File[] files = dir.listFiles(filter);
        if(files!=null)
            addAll(files);
        return this;
    }

    public ClassPathBuilder addAll(File... files) {
        for (File f : files)
            add(f);
        return this;
    }

    /**
     * Formats the path in a single-argument format suitable
     * after the "-cp" JVM option.
     */
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (File f : elements) {
            if(buf.length()>0)  buf.append(File.pathSeparatorChar);
            // this method is normally used to create an argument for another process,
            // so better resolve relative path to absolute path.
            buf.append(f.getAbsolutePath());
        }
        return buf.toString();
    }
}
