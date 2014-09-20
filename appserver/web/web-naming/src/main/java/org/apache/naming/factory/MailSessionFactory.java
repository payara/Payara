/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.naming.factory;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import javax.mail.Session;
import javax.naming.Name;
import javax.naming.Context;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

/**
 * <p>Factory class that creates a JNDI named JavaMail Session factory,
 * which can be used for managing inbound and outbound electronic mail
 * messages via JavaMail APIs.  All messaging environment properties
 * described in the JavaMail Specification may be passed to the Session
 * factory; however the following properties are the most commonly used:</p>
 * <ul>
 * <li>
 * <li><strong>mail.smtp.host</strong> - Hostname for outbound transport
 *     connections.  Defaults to <code>localhost</code> if not specified.</li>
 * </ul>
 *
 * <p>This factory can be configured in a <code>&lt;DefaultContext&gt;</code>
 * or <code>&lt;Context&gt;</code> element in your <code>conf/server.xml</code>
 * configuration file.  An example of factory configuration is:</p>
 * <pre>
 * &lt;Resource name="mail/smtp" auth="CONTAINER"
 *           type="javax.mail.Session"/&gt;
 * &lt;ResourceParams name="mail/smtp"&gt;
 *   &lt;parameter&gt;
 *     &lt;name&gt;factory&lt;/name&gt;
 *     &lt;value&gt;org.apache.naming.factory.MailSessionFactory&lt;/value&gt;
 *   &lt;/parameter&gt;
 *   &lt;parameter&gt;
 *     &lt;name&gt;mail.smtp.host&lt;/name&gt;
 *     &lt;value&gt;mail.mycompany.com&lt;/value&gt;
 *   &lt;/parameter&gt;
 * &lt;/ResourceParams&gt;
 * </pre>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.2 $ $Date: 2005/12/08 01:29:07 $
 */

public class MailSessionFactory implements ObjectFactory {


    /**
     * The Java type for which this factory knows how to create objects.
     */
    protected static final String factoryType = "javax.mail.Session";


    /**
     * Create and return an object instance based on the specified
     * characteristics.
     *
     * @param refObj Reference information containing our parameters, or null
     *  if there are no parameters
     * @param name The name of this object, relative to context, or null
     *  if there is no name
     * @param context The context to which name is relative, or null if name
     *  is relative to the default initial context
     * @param env Environment variables, or null if there are none
     *
     * @exception Exception if an error occurs during object creation
     */
    public Object getObjectInstance(Object refObj, Name name, Context context,
				    Hashtable<?,?> env) throws Exception 
    {

        // Return null if we cannot create an object of the requested type
	final Reference ref = (Reference) refObj;
        if (!ref.getClassName().equals(factoryType))
            return (null);

        // Create a new Session inside a doPrivileged block, so that JavaMail
        // can read its default properties without throwing Security
        // exceptions
        return AccessController.doPrivileged( new PrivilegedAction<Session>() {
		public Session run() {

                    // Create the JavaMail properties we will use
                    Properties props = new Properties();
                    props.put("mail.transport.protocol", "smtp");
                    props.put("mail.smtp.host", "localhost");
                    Enumeration<RefAddr> attrs = ref.getAll();
                    while (attrs.hasMoreElements()) {
                        RefAddr attr = attrs.nextElement();
                        if ("factory".equals(attr.getType()))
                            continue;
                        props.put(attr.getType(), attr.getContent());
                    }

                    // Create and return the new Session object
                    Session session = Session.getInstance(props, null);
                    return (session);

		}
	    } );

    }


}
