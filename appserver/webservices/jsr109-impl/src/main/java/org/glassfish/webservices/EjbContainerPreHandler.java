/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices;

import javax.xml.namespace.QName;

import javax.xml.rpc.handler.GenericHandler;
import javax.xml.rpc.handler.MessageContext;

import java.util.logging.Logger;
import java.lang.reflect.Method;
import java.text.MessageFormat;

import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.ejb.api.EJBInvocation;

/**
 * This handler is inserted first in the handler chain for an
 * ejb web service endpoint.  It performs security authorization 
 * before any of the application handlers are invoked, as required
 * by JSR 109.
 *
 * @author Kenneth Saks
 */
public class EjbContainerPreHandler extends GenericHandler {

    private static final Logger logger = LogUtils.getLogger();
    private final WsUtil wsUtil = new WsUtil();

    public EjbContainerPreHandler() {}

    @Override
    public QName[] getHeaders() {
        return new QName[0];
    }

    @Override
    public boolean handleRequest(MessageContext context) {
        EJBInvocation inv = null;
        try {
            WebServiceContractImpl wscImpl = WebServiceContractImpl.getInstance();
            InvocationManager invManager = wscImpl.getInvocationManager();
            inv = EJBInvocation.class.cast(invManager.getCurrentInvocation());
            Method method = wsUtil.getInvMethod(
                    (com.sun.xml.rpc.spi.runtime.Tie)inv.getWebServiceTie(), context);
            inv.setWebServiceMethod(method);
            if ( !inv.authorizeWebService(method)  ) {
                throw new Exception(format(logger.getResourceBundle().getString(LogUtils.CLIENT_UNAUTHORIZED),
                        method.toString()));
            }
        } catch(Exception e) {
            wsUtil.throwSOAPFaultException(e.getMessage(), context);
        }
        return true;
    }

    private String format(String key, String ... values){
        return MessageFormat.format(key, (Object [])values);
    }
}
