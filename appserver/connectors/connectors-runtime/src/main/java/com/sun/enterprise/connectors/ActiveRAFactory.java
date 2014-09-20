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

package com.sun.enterprise.connectors;

import com.sun.appserv.connectors.internal.api.*;
import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.util.Utility;
import com.sun.logging.LogDomains;

import javax.inject.Inject;
import javax.resource.spi.ResourceAdapter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;

import org.glassfish.api.admin.ProcessEnvironment;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Singleton;

/**
 * Factory creating Active Resource adapters.
 *
 * @author Binod P.G
 */
@Service
@Singleton
public class ActiveRAFactory {
    private static Logger _logger = LogDomains.getLogger(ActiveRAFactory.class,LogDomains.RSR_LOGGER);

    @Inject
    private ServiceLocator activeRAHabitat;
    /**
     * Creates an active resource adapter.
     *
     * @param cd         Deployment descriptor object for connectors.
     * @param moduleName Module name of the resource adapter.
     * @param loader     Class Loader,
     * @return An instance of <code> ActiveResourceAdapter </code> object.
     * @throws ConnectorRuntimeException when unable to create the runtime for RA
     */
    public ActiveResourceAdapter createActiveResourceAdapter(
            ConnectorDescriptor cd, String moduleName, ClassLoader loader)
            throws ConnectorRuntimeException {

        ActiveResourceAdapter activeResourceAdapter = null;
        ClassLoader originalContextClassLoader = null;

        ProcessEnvironment.ProcessType processType = ConnectorRuntime.getRuntime().getEnvironment();
        ResourceAdapter ra = null;
        String raClass = cd.getResourceAdapterClass();

        try {

            // If raClass is available, load it...

            if (raClass != null && !raClass.equals("")) {
                if (processType == ProcessEnvironment.ProcessType.Server) {
                    ra = (ResourceAdapter)
                            loader.loadClass(raClass).newInstance();
                } else {
                    //ra = (ResourceAdapter) Class.forName(raClass).newInstance();
                    ra = (ResourceAdapter)
                            Thread.currentThread().getContextClassLoader().loadClass(raClass).newInstance();
                }
            }

            originalContextClassLoader = Utility.setContextClassLoader(loader);
            activeResourceAdapter = instantiateActiveResourceAdapter(cd, moduleName, loader, ra);

        } catch (ClassNotFoundException Ex) {
            ConnectorRuntimeException cre = new ConnectorRuntimeException(
                    "Error in creating active RAR");
            cre.initCause(Ex);
            _logger.log(Level.SEVERE, "rardeployment.class_not_found", raClass);
            _logger.log(Level.SEVERE, "", cre);
            throw cre;
        } catch (InstantiationException Ex) {
            ConnectorRuntimeException cre = new ConnectorRuntimeException("Error in creating active RAR");
            cre.initCause(Ex);
            _logger.log(Level.SEVERE, "rardeployment.class_instantiation_error", raClass);
            _logger.log(Level.SEVERE, "", cre);
            throw cre;
        } catch (IllegalAccessException Ex) {
            ConnectorRuntimeException cre = new ConnectorRuntimeException("Error in creating active RAR");
            cre.initCause(Ex);
            _logger.log(Level.SEVERE, "rardeployment.illegalaccess_error", raClass);
            _logger.log(Level.SEVERE, "", cre);
            throw cre;
        } finally {
            if (originalContextClassLoader != null) {
                Utility.setContextClassLoader(originalContextClassLoader);
            }
        }
        return activeResourceAdapter;
    }

    private  ActiveResourceAdapter instantiateActiveResourceAdapter(ConnectorDescriptor cd,
                                                                    String moduleName, ClassLoader loader,
                                                                    ResourceAdapter ra) throws ConnectorRuntimeException {
        ActiveResourceAdapter activeResourceAdapter = getActiveRA(cd, moduleName);
        activeResourceAdapter.init(ra, cd, moduleName, loader);            
        return activeResourceAdapter;
    }

    private ActiveResourceAdapter getActiveRA(ConnectorDescriptor cd, String moduleName)
            throws ConnectorRuntimeException{
        Collection<ActiveResourceAdapter> activeRAs =  activeRAHabitat.getAllServices(ActiveResourceAdapter.class);
        for(ActiveResourceAdapter activeRA : activeRAs){
            if(activeRA.handles(cd, moduleName)){
                if(_logger.isLoggable(Level.FINEST)){
                    _logger.log(Level.FINEST,"found active-RA for the module [ "+moduleName+" ] " +
                        activeRA.getClass().getName());
                }
                return activeRA;
            }
        }

        if(cd.getInBoundDefined()){
            // did not find a suitable Active RA above.
            // [Possibly the profile (eg: WEB profile) does not support it]
            // Let us provide outbound support.
            _logger.log(Level.INFO, "Deployed RAR [ "+moduleName+" ] has inbound artifacts, but the runtime " +
                    "does not support it. Providing only outbound support ");

            return activeRAHabitat.getService(ActiveResourceAdapter.class, ConnectorConstants.AORA);
        }
        //could not fine any impl.
        throw new ConnectorRuntimeException("Unable to get active RA for module " + moduleName);
    }
}
