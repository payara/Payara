/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ejb.portable;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.IOException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NameNotFoundException;

import javax.ejb.spi.HandleDelegate;
import javax.ejb.EJBException;

/**
 * Common code for looking up the java:comp/HandleDelegate.
 *
 * This class can potentially be instantiated in another vendor's container
 * so it must not refer to any non-portable RI-specific classes.
 *
 * @author Kenneth Saks
 */
public class HandleDelegateUtil 
{

    private static final String JNDI_PROPERTY_FILE_NAME = 
        "com.sun.ejb.portable.jndi.propertyfilename";

    // Class-level state for jndi properties override.  Flag is used
    // so that we only check once for jndi properties file override.
    private static boolean checkedJndiProperties = false;

    // contents of file referred to by jndi properties override
    private static Properties jndiProperties = null;

    private static volatile HandleDelegate _cachedHandleDelegate;

    static HandleDelegate getHandleDelegate()
        throws NamingException
    {
        if (_cachedHandleDelegate == null) {
            synchronized (HandleDelegateUtil.class) {
                if (_cachedHandleDelegate == null) {
                    _cachedHandleDelegate = createHandleDelegate();
                }
            }
        }

        return _cachedHandleDelegate;
    }

    private static HandleDelegate createHandleDelegate()
        throws NamingException
    {
        HandleDelegate handleDelegate;
        try {
            InitialContext ctx = new InitialContext();
            handleDelegate = (HandleDelegate) 
                ctx.lookup("java:comp/HandleDelegate");
        } catch(NamingException ne) {
      
            // If the lookup fails, it's probably because the default 
            // InitialContext settings needed to access the correct
            // java:comp/HandleDelegate have been overridden in this VM.  
            // In that case, check if the system value class override 
            // property file is available and if so use it.
            Properties props = null;
            try {
                props = getJndiProperties();
            } catch(Exception e) {
                // Exception while attempting to access jndi property override.
                // Create new NamingException that describes the error.
                NamingException ne2 = new NamingException
                    ("Error while accessing " + JNDI_PROPERTY_FILE_NAME +
                     " : " + e.getMessage());
                ne2.initCause(e);
                throw ne2;
            }
        
            if( props == null ) {                    
                // There was no property override set.
                NamingException ne3 = new NamingException
                    ("java:comp/HandleDelegate not found. Unable to " +
                     " use jndi property file override since " +
                     JNDI_PROPERTY_FILE_NAME + " has NOT been set");
                ne3.initCause(ne);
                throw ne3;
            }
        
            try {
                InitialContext ctx = new InitialContext(props);
                handleDelegate = (HandleDelegate) 
                    ctx.lookup("java:comp/HandleDelegate");
            } catch(NamingException ne4) {
                NamingException overrideEx = 
                    new NamingException("Unable to lookup HandleDelegate " +
                                        "with override properties = " + 
                                        props.toString());
                overrideEx.initCause(ne4);
                throw overrideEx;
            }
        }

        return handleDelegate;
    }

    /**
     * Internal method for accessing jndi properties override.  We only
     * look for properties file at most once, whether it is present or not.
     * 
     */
    private static Properties getJndiProperties() 
        throws Exception
    {
        synchronized(HandleDelegateUtil.class) {
            if( !checkedJndiProperties ) {
                FileInputStream fis = null;
                try {
                    String jndiPropertyFileName = 
                        System.getProperty(JNDI_PROPERTY_FILE_NAME);
                    
                    if( jndiPropertyFileName != null ) {
                        fis = new FileInputStream(jndiPropertyFileName);
                        jndiProperties = new Properties();
                        jndiProperties.load(fis);
                        // Let an exception encountered here bubble up, so
                        // we can include its info in the exception propagated
                        // to the application.
                    }
                } finally {
                    // Always set to true so we don't keep doing this 
                    // system property and file access multiple times
                    checkedJndiProperties = true;
                    if(fis != null) {
                        try {
                            fis.close();
                        } catch (IOException e) {
                            //ignore
                        }
                    }
                }
            }
        }

        return jndiProperties;
    }
}
