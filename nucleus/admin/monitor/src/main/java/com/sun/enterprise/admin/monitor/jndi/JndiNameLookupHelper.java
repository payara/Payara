/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.monitor.jndi;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NameNotFoundException;
import javax.naming.InitialContext;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;
import static org.glassfish.admin.monitor.MLogger.*;


public class JndiNameLookupHelper {
    private InitialContext context;
    private static final Logger logger = getLogger();
    private static final StringManager sm =
        StringManager.getManager(JndiNameLookupHelper.class);
    private final String SYSTEM_SUBCONTEXT = "__SYSTEM";

    /** Creates a new instance of JndiMBeanHelper */
    public JndiNameLookupHelper() {
        initialize();
    }

    /**
     * Initializes the JndiMBeanHelper object upon creation. It specifically
     * creates an InitialContext instance for querying the naming service
     * during certain method invocations.
     */
    void initialize() {
        try {
            context = new InitialContext();
        } catch(javax.naming.NamingException e) {
            logger.log(Level.WARNING, UNHANDLED_EXCEPTION, e);
        }
    }

    /**
     * Gets the jndi entries from the application server's naming
     * service given a particular context/subcontext.
     *
     * @param contextPath The naming context or subcontext to query.
     * @return An {@link ArrayList} of {@link javax.naming.NameClassPair} objects.
     * @throws NamingException if an error occurs when connection with
     *         the naming service is established or retrieval fails.
     */
    public ArrayList<String> getJndiEntriesByContextPath(String contextPath)
            throws NamingException {
        ArrayList<String> names;
        NamingEnumeration ee;
        if(contextPath == null) { contextPath = ""; }
        try {
            ee = context.list(contextPath);
        } catch(NameNotFoundException e) {
            String msg = sm.getString("monitor.jndi.context_notfound",
                new Object[]{contextPath});
            logger.log(Level.WARNING, msg);
            throw new NamingException(msg);
        }
        names = toNameClassPairArray(ee);
        return names;
    }

    /**
     * Changes a NamingEnumeration object into an ArrayList of
     * NameClassPair objects.
     *
     * @param ee An {@link NamingEnumeration} object to be transformed.
     * @return An {@link ArrayList} of {@link javax.naming.NameClassPair} objects.
     *
     * @throws NamingException if an error occurs when connection with
     *         the naming service is established or retrieval fails.
     */
    ArrayList<String> toNameClassPairArray(NamingEnumeration ee)
            throws javax.naming.NamingException{
        ArrayList<String> names = new ArrayList<String>();
        while(ee.hasMore()) {
            // don't add the __SYSTEM subcontext - Fix for 6041360
            Object o = ee.next();
            if(o.toString().indexOf(SYSTEM_SUBCONTEXT) == -1) {
                names.add(o.toString());
            }
        }
        return names;
    }
}
