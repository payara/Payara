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

package com.sun.enterprise.transaction.jts.iiop;

import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.jts.pi.InterceptorImpl;
import com.sun.jts.jta.TransactionServiceProperties;
import com.sun.jts.CosTransactions.Configuration;
import com.sun.jts.CosTransactions.DefaultTransactionService;

import org.glassfish.enterprise.iiop.api.IIOPInterceptorFactory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.api.admin.ProcessEnvironment;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.logging.LogDomains;

import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;

import org.glassfish.pfl.basic.func.NullaryFunction ;

import org.omg.CORBA.*;
import org.omg.IOP.Codec;
import org.omg.PortableInterceptor.Current;
import org.omg.PortableInterceptor.ClientRequestInterceptor;
import org.omg.PortableInterceptor.ORBInitInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import com.sun.corba.ee.spi.misc.ORBConstants;
import com.sun.corba.ee.spi.legacy.interceptor.ORBInitInfoExt;
import com.sun.corba.ee.spi.logging.POASystemException;
import com.sun.corba.ee.impl.txpoa.TSIdentificationImpl;

/**
 *
 * @author mvatkina
 */
@Service(name="TransactionIIOPInterceptorFactory")
public class TransactionIIOPInterceptorFactory implements IIOPInterceptorFactory{

    // The log message bundle is in com.sun.jts package
    private static Logger _logger =
            LogDomains.getLogger(InterceptorImpl.class, LogDomains.TRANSACTION_LOGGER);

    private static Properties jtsProperties = new Properties();
    private static TSIdentificationImpl tsIdent = new TSIdentificationImpl();
    private static boolean txServiceInitialized = false;
    private InterceptorImpl interceptor = null;

    @Inject private ServiceLocator serviceLocator;
    @Inject private ProcessEnvironment processEnv;

    public ClientRequestInterceptor createClientRequestInterceptor(ORBInitInfo info, Codec codec) {
        if (!txServiceInitialized) {
            createInterceptor(info, codec);
        }

        return interceptor;
    }

    public ServerRequestInterceptor createServerRequestInterceptor(ORBInitInfo info, Codec codec) {
        if (!txServiceInitialized) {
            createInterceptor(info, codec);
        }

        return interceptor;
    }

    private void createInterceptor(ORBInitInfo info, Codec codec) {
        if( processEnv.getProcessType().isServer()) {
            try {
                System.setProperty(
                        InterceptorImpl.CLIENT_POLICY_CHECKING, String.valueOf(false));
            } catch ( Exception ex ) {
                _logger.log(Level.WARNING,"iiop.readproperty_exception",ex);
            }

            initJTSProperties(true);
        } else {
            initJTSProperties(false);
        }

        try {
            // register JTS interceptors
            // first get hold of PICurrent to allocate a slot for JTS service.
            Current pic = (Current)info.resolve_initial_references("PICurrent");

            // allocate a PICurrent slotId for the transaction service.
            int[] slotIds = new int[2];
            slotIds[0] = info.allocate_slot_id();
            slotIds[1] = info.allocate_slot_id();

            interceptor = new InterceptorImpl(pic, codec, slotIds, null);
            // Get the ORB instance on which this interceptor is being
            // initialized
            com.sun.corba.ee.spi.orb.ORB theORB = ((ORBInitInfoExt)info).getORB();

            // Set ORB and TSIdentification: needed for app clients,
            // standalone clients.
            interceptor.setOrb(theORB);
            try {
                DefaultTransactionService jts = new DefaultTransactionService();
                jts.identify_ORB(theORB, tsIdent, jtsProperties ) ;
                interceptor.setTSIdentification(tsIdent);

                // V2-XXX should jts.get_current() be called everytime
                // resolve_initial_references is called ??
                org.omg.CosTransactions.Current transactionCurrent = jts.get_current();

                theORB.getLocalResolver().register( ORBConstants.TRANSACTION_CURRENT_NAME,
                        NullaryFunction.Factory.makeConstant(
                        (org.omg.CORBA.Object)transactionCurrent));

                // the JTS PI use this to call the proprietary hooks
                theORB.getLocalResolver().register( "TSIdentification", 
                        NullaryFunction.Factory.makeConstant((org.omg.CORBA.Object)tsIdent));
                txServiceInitialized = true;
            } catch (Exception ex) {
                throw new INITIALIZE("JTS Exception: " + ex,
                        POASystemException.JTS_INIT_ERROR, CompletionStatus.COMPLETED_MAYBE);
            }

            // Add IOR Interceptor only for OTS tagged components
            TxIORInterceptor iorInterceptor = new TxIORInterceptor(codec, serviceLocator);
            info.add_ior_interceptor(iorInterceptor);

        } catch (Exception e) {
            if(_logger.isLoggable(Level.FINE)){
                _logger.log(Level.FINE,"Exception registering JTS interceptors",e);
            }
            throw new RuntimeException(e.getMessage());
        }
    }

    private void initJTSProperties(boolean isServer) {
        if (serviceLocator != null) {
            jtsProperties = TransactionServiceProperties.getJTSProperties(serviceLocator, true);
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                            "++++ Server id: "
                            + jtsProperties.getProperty(ORBConstants.ORB_SERVER_ID_PROPERTY));
            }
            if (isServer) {
                Configuration.setProperties(jtsProperties);
            }
        }
    }
}
