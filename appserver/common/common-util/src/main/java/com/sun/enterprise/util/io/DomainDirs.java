/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.util.io;

import com.sun.enterprise.universal.glassfish.ASenvPropertyReader;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;

/**
 * A class for keeping track of the directories that a domain lives in and under.
 *
 * @author Byron Nevins
 * @since 3.1
 * Created: April 19, 2010
 */
public final class DomainDirs {
    /**
     * This convenience constructor is used when nothing is known about the
     * domain-dir
     */
    public DomainDirs() throws IOException {
        this(null, null);
    }

    /**
     * This constructor is used when both the name of the domain is known and
     * the domains-dir is known.
     */
    public DomainDirs(File domainsDir, String domainName) throws IOException {

        if (domainsDir == null) {
            domainsDir = getDefaultDomainsDir();
        }

        if (!domainsDir.isDirectory()) {
            throw new IOException(strings.get("Domain.badDomainsDir", domainsDir));
        }

        File domainDir;

        if (domainName != null) {
            domainDir = new File(domainsDir, domainName);
        }
        else {
            domainDir = getTheOneAndOnlyDir(domainsDir);
        }

        if (!domainDir.isDirectory()) {
            throw new IOException(strings.get("Domain.badDomainDir", domainDir));
        }

        dirs = new ServerDirs(domainDir);
    }

    /**
     * This constructor is used when the path of the domain-directory is known.
     * @param domainsDir
     * @param domainName
     * @throws IOException
     */
    public DomainDirs(File domainDir) throws IOException {
        dirs = new ServerDirs(domainDir);
    }

    /**
     * Create a DomainDir from the more general ServerDirs instance.
     * along with getServerDirs() you can convert freely back and forth
     *
     * @param aServerDir
     */
    public DomainDirs(ServerDirs sd) {
        dirs = sd;
    }

    @Override
    public String toString() {
        return dirs.toString();
    }

    public final String getDomainName() {
        return dirs.getServerName();
    }

    public final File getDomainDir() {
        return dirs.getServerDir();
    }

    public final File getDomainsDir() {
        return dirs.getServerParentDir();
    }

    public final ServerDirs getServerDirs() {
        return dirs;
    }

    public final boolean isValid() {
        try {
            return dirs.isValid();
        }
        catch(Exception e) {
            return false;
        }
    }

    public static File getDefaultDomainsDir() throws IOException {
        Map<String, String> systemProps = new ASenvPropertyReader().getProps();
        String defDomains =
                systemProps.get(SystemPropertyConstants.DOMAINS_ROOT_PROPERTY);

        if (defDomains == null)
            throw new IOException(strings.get("Domain.noDomainsDir",
                    SystemPropertyConstants.DOMAINS_ROOT_PROPERTY));

        return new File(defDomains);

    }

    ///////////////////////////////////////////////////////////////////////////
    ///////////           All Private Below           /////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    private File getTheOneAndOnlyDir(File parent) throws IOException {
        // look for subdirs in the parent dir -- there must be one and only one

        File[] files = parent.listFiles(new FileFilter() {
            public boolean accept(File f) {
                File config = new File(f, "config");
                File dxml = new File(config, "domain.xml");
                return f.isDirectory() && config.isDirectory() &&
                        dxml.isFile();
            }
        });

        if (files == null || files.length == 0)
            throw new IOException(strings.get("Domain.noDomainDirs", parent));

        if(files.length > 1) {
            StringBuilder names = new StringBuilder();
            
            for(int i = 0 ; i < files.length; i++) {
                if(i > 0)
                    names.append(", ");
                names.append(files[i].getName());
            }
            
            throw new IOException(strings.get("Domain.tooManyDomainDirs", parent, names.toString()));
        }

        return files[0];
    }

    private final ServerDirs dirs;
    private final static LocalStringsImpl strings = new LocalStringsImpl(DomainDirs.class);
}
