/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web;

import com.sun.enterprise.util.net.JarURIPattern;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.api.web.TldProvider;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PostConstruct;
import javax.inject.Singleton;

import java.io.File;
import java.io.FileFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Implementation of TldProvider for libraries in lib and DOMAIN_ROOT/lib/applibs.
 * @author Shing Wai Chan
 */

@Service(name="librariesTld")
@Singleton
public class LibrariesTldProvider implements TldProvider, PostConstruct {

    @Inject
    private ServerEnvironment serverEnvironment;

    private Map<URI, List<String>> tldMap = new HashMap<URI, List<String>>();

    /**
     * Gets the name of this TldProvider
     */
    public String getName() {
        return "librariesTld";
    }

    /**
     * Gets a mapping from JAR files to their TLD resources.
     */
    public Map<URI, List<String>> getTldMap() {
        return cloneTldMap();
    }

    /**
     * Gets a mapping from JAR files to their TLD resources
     * that are known to contain listener declarations.
     */
    public Map<URI, List<String>> getTldListenerMap() {
        // return the whole map as the content for tld files is not known
        return cloneTldMap();
    }

    @SuppressWarnings("unchecked")
    private Map<URI, List<String>> cloneTldMap() {
        return (Map<URI, List<String>>)((HashMap<URI, List<String>>)tldMap).clone();
    }
 
    public void postConstruct() {
        File[] domainLibJars = serverEnvironment.getLibPath().listFiles(
                new FileFilter() {
                    public boolean accept(File path) {
                        return (path.isFile() && path.getName().endsWith(".jar"));
                    }
                });

        if (domainLibJars != null && domainLibJars.length > 0) {
            List <URI> uris = new ArrayList<URI>();
            for (File f : domainLibJars) {
               uris.add(f.toURI());
            }

            Pattern pattern = Pattern.compile("META-INF/.*\\.tld");
            for (URI uri : uris) {
                List<String> entries =  JarURIPattern.getJarEntries(uri, pattern);
                if (entries != null && entries.size() > 0) {
                    tldMap.put(uri, entries);
                }
            }
        }
    }
}
