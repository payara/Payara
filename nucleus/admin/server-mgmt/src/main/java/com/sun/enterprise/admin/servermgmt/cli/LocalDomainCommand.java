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

package com.sun.enterprise.admin.servermgmt.cli;

import com.sun.enterprise.util.io.DomainDirs;
import com.sun.enterprise.util.io.ServerDirs;
import java.io.*;

import org.glassfish.api.Param;
import org.glassfish.api.admin.*;

/**
 * A class that's supposed to capture all the behavior common to operation
 * on a "local" domain.  It's supposed to act as the abstract base class that
 * provides more functionality to the commands that operate on a local domain.
 * @author &#2325;&#2375;&#2342;&#2366;&#2352 (km@dev.java.net)
 * @author Byron Nevins  (bnevins@dev.java.net)
 */
public abstract class LocalDomainCommand extends LocalServerCommand {
    @Param(name = "domaindir", optional = true)
    protected String domainDirParam = null;
    // subclasses decide whether it's optional, required, or not allowed
    //@Param(name = "domain_name", primary = true, optional = true)
    private String userArgDomainName;
    // the key for the Domain Root in the main attributes of the
    // manifest returned by the __locations command
    private static final String DOMAIN_ROOT_KEY = "Domain-Root";
    private DomainDirs dd = null;

    /*
     * The prepare method must ensure that the superclass' implementation of
     * the method is called.  
     * The reason we override here is that we can get into trouble with layers 
     * of NPE possibilities.  So here the ServerDirs object is initialized
     * right away.  It will return null for all non-boolean method calls.  But
     * we never have to do a null-check on the ServerDirs object itself.
     * ServerDirs is 100% immutable.  A new one will be made later if needed.
     */
    @Override
    protected void prepare()
            throws CommandException, CommandValidationException {
        super.prepare();
        setServerDirs(new ServerDirs()); // do-nothing ServerDirs object...
    }

    @Override
    protected void validate()
            throws CommandException, CommandValidationException {

        initDomain();
    }

    protected final File getDomainsDir() {
        return dd.getDomainsDir();
    }

    protected final File getDomainRootDir() {
        return dd.getDomainDir();
    }

    protected final String getDomainName() {
        // can't just use "dd" since it may be half-baked right now!

        if (dd != null && dd.isValid())
            return dd.getDomainName();
        else // too early!
            return userArgDomainName;  // might be and is ok to be null
    }

    /**
     * We need this so that @Param values for domainname can be remembered later
     * when the ServerDirs object is made.
     * @param name the user-specified domain name.
     */
    protected final void setDomainName(String name) {
        dd = null;
        userArgDomainName = name;
    }

    protected void initDomain() throws CommandException {
        try {
            File domainsDirFile = null;

            if (ok(domainDirParam))
                domainsDirFile = new File(domainDirParam);

            dd = new DomainDirs(domainsDirFile, getDomainName());
            setServerDirs(dd.getServerDirs());
        } catch (Exception e) {
            throw new CommandException(e.getMessage(), e);
        }
        setLocalPassword();
    }

    protected boolean isThisDAS(File ourDir) {
        return isThisServer(ourDir, DOMAIN_ROOT_KEY);
    }

}
