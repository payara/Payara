/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.resources.javamail.naming;

import com.sun.enterprise.deployment.MailConfiguration;
import com.sun.logging.LogDomains;
import org.glassfish.resources.javamail.MailLogOutputStream;
import org.glassfish.resources.javamail.MailSessionAuthenticator;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import java.io.PrintStream;
import java.util.Properties;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MailNamingObjectFactory implements ObjectFactory {

    private static Logger _logger = LogDomains.getLogger(MailNamingObjectFactory.class, LogDomains.JNDI_LOGGER);

    public MailNamingObjectFactory() {
    }


    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment)
            throws Exception {
        Reference ref = (Reference) obj;
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,"MailNamingObjectFactory: " + ref +
                " Name:" + name);
        }
        MailConfiguration config = (MailConfiguration) ref.get(0).getContent();

        // Note: javax.mail.Session is not serializable,
        // but we need to get a new instance on every lookup.
        Properties props = config.getMailProperties();
        javax.mail.Session s = javax.mail.Session.getInstance(props, new MailSessionAuthenticator(props));
        if("smtps".equals(props.getProperty("mail.transport.protocol"))) {
            s.setProtocolForAddress("rfc822", "smtps");
        }
        s.setDebugOut(new PrintStream(new MailLogOutputStream()));
        s.setDebug(true);

        return s;
    }
}
