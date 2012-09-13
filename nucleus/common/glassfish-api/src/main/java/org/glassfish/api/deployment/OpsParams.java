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

package org.glassfish.api.deployment;

import org.glassfish.api.admin.CommandParameters;
import org.glassfish.api.Param;

/**
 * Support class for all types of deployment operation parameters.
 *
 * @author Jerome Dochez
 */
public abstract class OpsParams implements CommandParameters {

    /**
     * There can be so far 6 types of events that can trigger deployment
     * activities.
     *
     * load when an already deployed application is being reloaded.
     * deploy when a new application is deployed on DAS
     * deploy_instance when a new application is deployed on instance
     * unload when a loaded application is stopped
     * undeploy when a deployed application is removed from the system.
     * create_application_ref when an application reference is being created
     * mt_provision when provisioning an application to tenant
     * mt_unprovision when unprovisioning an application from tenant
     */
    public enum Origin { 
        load, deploy, deploy_instance, unload, undeploy, create_application_ref,        mt_provision, mt_unprovision;

        // whether it's part of the deployment, on DAS or on instance
        public boolean isDeploy() {
            if (this == Origin.deploy || this == Origin.deploy_instance) {
                return true;
            }    
            else {
                return false;
            }
        }

        // whether it's loading application only
        public boolean isLoad() {
            if (this == Origin.load) {
                return true;
            }    
            else {
                return false;
            }
        }

        // whether the artifacts are already present and no need to 
        // generate
        public boolean isArtifactsPresent() {
            if (this == Origin.load || this == Origin.deploy_instance || 
                this == Origin.create_application_ref) {
                return true;
            }
            else {
                return false;
            }
        }

        // whether we need to clean the artifacts
        // we need to do this for undeployment and deployment failure 
        // clean up
        public boolean needsCleanArtifacts() {
            if (this == Origin.undeploy || this == Origin.deploy 
                || this == Origin.mt_unprovision) {
                return true;
            }
            else {
                return false;
            }
        }

        // whether it's undeploy
        public boolean isUndeploy() {
            if (this == Origin.undeploy) {
                return true;
            }    
            else {
                return false;
            }
        }

        // whether it's unloading application only
        public boolean isUnload() {
            if (this == Origin.unload) {
                return true;
            }    
            else {
                return false;
            }
        }

        // whether it's creating application reference
        public boolean isCreateAppRef() {
            if (this == Origin.create_application_ref) {
                return true;
            }
            else {
                return false;
            }
        }
    }

    /**
     * There can be cases where the container code wants to find out  
     * the command associated with the operation when the Origin information
     * is not sufficient
     * 
     */
    public enum Command { 
        deploy, undeploy, enable, disable, _deploy, create_application_ref, delete_application_ref, startup_server, shutdown_server 
    }

    /**
     * Type of deployment operation, by default it's deployment
     */
    public Origin origin = Origin.deploy; 
    public Origin getOrigin() {
      return origin;
    }

    /**
     * The command associated with this operation, by default it's deploy
     */
    public Command command = Command.deploy;
    public Command getCommand() {
      return command;
    }

    public abstract String name();

    public abstract String libraries();

    // internal hidden param
    // if this param is set to true, a classic style deployment will be
    // executed regardless of the virtualization settings
    @Param(optional=true, defaultValue="false")
    public Boolean _classicstyle = false;
}
