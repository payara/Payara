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

/*
 * DeploymentEventListenerImpl.java
 *
 * Created on April 8, 2003.
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbc;

//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//
//import java.sql.Connection;
//import java.sql.Statement;
//import java.sql.SQLException;
//
//import java.util.Iterator;
//import java.util.ResourceBundle;
//import java.util.Properties;
//import java.util.Collection;
//
//import com.sun.enterprise.util.io.FileUtils;
//
//import com.sun.enterprise.deployment.Application;
//import com.sun.enterprise.deployment.EjbBundleDescriptor;
//import com.sun.enterprise.deployment.IASEjbCMPEntityDescriptor;
//import com.sun.enterprise.deployment.PersistenceUnitsDescriptor;
//import com.sun.enterprise.deployment.PersistenceUnitDescriptor;
//import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
//
//import com.sun.enterprise.deployment.backend.DeploymentEvent;
//import com.sun.enterprise.deployment.backend.DeploymentEventInfo;
//import com.sun.enterprise.deployment.backend.DeploymentEventListener;
//import com.sun.enterprise.deployment.backend.DeploymentEventManager;
//import com.sun.enterprise.deployment.backend.DeploymentRequest;
//import com.sun.enterprise.deployment.backend.DeploymentStatus;
//import com.sun.enterprise.deployment.backend.IASDeploymentException;
//
//import com.sun.enterprise.server.Constants;
//
//import com.sun.jdo.api.persistence.support.JDOFatalUserException;
//
//import com.sun.jdo.spi.persistence.support.sqlstore.LogHelperSQLStore;
//
//import com.sun.jdo.spi.persistence.utility.logging.Logger;
//import com.sun.jdo.spi.persistence.utility.database.DatabaseConstants;
////import com.sun.jdo.spi.persistence.support.sqlstore.ejb.*;
//
///** Implementation of the DeploymentEventListener interface for
//* creating and dropping database schema definitions at the appropriate
//* deployment/undeployment events.
//*
//*/
public class DeploymentEventListenerImpl {
//    implements DeploymentEventListener, DatabaseConstants {
//
//    /** I18N message handler */
//    private final static ResourceBundle messages = I18NHelper.loadBundle(
//        "com.sun.jdo.spi.persistence.support.ejb.ejbc.Bundle", // NOI18N
//        DeploymentEventListenerImpl.class.getClassLoader());
//
//    /** The logger */
//    private static Logger logger = LogHelperSQLStore.getLogger();
//
//    /** Garantees singleton.
//     * Registers itself during initial load
//     */
//    static {
//        DeploymentEventManager.addListener (new DeploymentEventListenerImpl());
//    }
//
//    /** Default constructor should not be public */
//    DeploymentEventListenerImpl() { }
//
//    /**
//     * This method is called when a <code>DeploymentEventManager</code>
//     * needs to deliver a code>DeploymentEvent</code> event.
//     * @param event the DeploymentEvent to be delivered.
//     */
//    public void notifyDeploymentEvent(DeploymentEvent event) {
//        int type = event.getEventType();
//        switch (type) {
//            case DeploymentEvent.POST_DEPLOY:
//                processEvent(event.getEventInfo(), true);
//                break;
//            case DeploymentEvent.PRE_UNDEPLOY:
//            case DeploymentEvent.PRE_DEPLOY:
//                processEvent(event.getEventInfo(), false);
//                break;
//            default:
//                break;
//        }
//    }
//
//    /** Event handling.
//     * @param source the event source.
//     * @param create true if we need to create tables as part of this event.
//     */
//    private void processEvent(DeploymentEventInfo info, boolean create) {
//        // Get the CLI overrides.
//        String cliCreateTables = null;
//        String cliDropTables = null;
//
//        DeploymentRequest request = info.getDeploymentRequest();
//
//        // Do nothing for drop tables on the deploy
//        if (isDeploy(request) && !create) {
//            return;
//        }
//
//        Properties cliOverrides = request.getOptionalArguments();
//
//        String cliDropAndCreateTables = cliOverrides.getProperty(
//                Constants.CMP_DROP_AND_CREATE_TABLES, Constants.UNDEFINED);
//
//        if (create) {
//            cliCreateTables = cliOverrides.getProperty(
//                Constants.CMP_CREATE_TABLES, Constants.UNDEFINED);
//
//            if (cliCreateTables.equals(Constants.UNDEFINED)) {
//                // It might have been specified as CMP_DROP_AND_CREATE_TABLES.
//                cliCreateTables = cliDropAndCreateTables;
//            }
//        } else {
//            cliDropTables = cliOverrides.getProperty(
//                Constants.CMP_DROP_TABLES, Constants.UNDEFINED);
//        }
//
//        Application application = info.getApplicationDescriptor();
//        if ( application == null) {
//            return;
//        }
//
//        processApplication(request, info, create, cliCreateTables,
//            cliDropAndCreateTables, cliDropTables);
//    } //processEvent
//
//    /**
//     * This is the method that does the actual processing of the deployment event.
//     * For each application process the cmp 2.x beans if any followed by processing
//     * the ejb 3.0 beans.
//     * @param request the deployment request object
//     * @param info the event source
//     * @param create true if we need to create tables as part of this event.
//     * @param cliCreateTables the cli option for creating tables
//     * @param cliDropAndCreateTables the cli option for dropping old tables and creating new tables
//     * @param cliDropTables the cli option to drop tables at undeploy time
//     */
//    private void processApplication(DeploymentRequest request,
//            DeploymentEventInfo info, boolean create,
//            String cliCreateTables, String cliDropAndCreateTables,
//            String cliDropTables) {
//
//        if (logger.isLoggable(logger.FINE)) {
//            logger.fine("ejb.DeploymentEventListenerImpl.processingevent", //NOI18N
//                (isRedeploy(request)? "redeploy" : ((create)? "deploy" : "undeploy")), //NOI18N
//                info.getApplicationDescriptor().getRegistrationName());
//        }
//
//        // Get status value for our use and initialize it.
//        DeploymentStatus status = request.getCurrentDeploymentStatus();
//        status.setStageStatus(DeploymentStatus.SUCCESS);
//        status.setStageStatusMessage("");
//
//        BaseProcessor processor =
//                new CMPProcessor(info, create, cliCreateTables,
//            cliDropAndCreateTables, cliDropTables);
//        processor.processApplication();
//
//        processor  =
//                new PersistenceProcessor(info, create, cliCreateTables,
//            cliDropAndCreateTables, cliDropTables);
//        processor.processApplication();
//    }
//
//    /**
//     * This method returns a boolean value that determines if we are
//     * trying to do a redeployment of an existing application but doesn't
//     * throw an exception.
//     * @param request the deployment request object
//     * @return true if we are trying to redeploy an existing application
//     */
//    private boolean isRedeploy(DeploymentRequest request) {
//        boolean redeploy = false;
//        try {
//            if (request != null) {
//                redeploy = request.isReDeploy();
//            }
//        } catch (IASDeploymentException e) {
//            // Ignore? This is a strange exception.
//        }
//        return redeploy;
//    }
//
//    /**
//     * This method returns a boolean value that determines if we are
//     * trying to do a deployment of an existing application but doesn't
//     * throw an exception.
//     * @param request the deployment request object
//     * @return true if we are trying to redeploy an existing application
//     */
//    private boolean isDeploy(DeploymentRequest request) {
//        boolean deploy = false;
//        try {
//            if (request != null) {
//                deploy = request.isDeploy();
//            }
//        } catch (IASDeploymentException e) {
//            // Ignore? This is a strange exception.
//        }
//        return deploy;
//    }
//
} //DeploymentEventListenerImpl
