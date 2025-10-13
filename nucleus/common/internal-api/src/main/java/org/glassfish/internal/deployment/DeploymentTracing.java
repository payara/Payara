/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2019-2021] Payara Foundation and/or its affiliates
package org.glassfish.internal.deployment;

import com.sun.enterprise.module.HK2Module;
import com.sun.enterprise.module.ModuleState;
import com.sun.enterprise.module.ModulesRegistry;
import org.glassfish.internal.deployment.analysis.StructuredDeploymentTracing;

import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tracing facility for all the deployment backend activities.
 *
 * @author Jerome Dochez
 */
public class DeploymentTracing {

    private final StructuredDeploymentTracing structured;

    public enum Mark {
        ARCHIVE_OPENED,
        ARCHIVE_HANDLER_OBTAINED,
        INITIAL_CONTEXT_CREATED,
        APPINFO_PROVIDED,
        DOL_LOADED,
        APPNAME_DETERMINED,
        TARGET_VALIDATED,
        CONTEXT_CREATED,
        DEPLOY,
        CLASS_LOADER_HIERARCHY,
        PARSING_DONE,
        CLASS_LOADER_CREATED,
        CONTAINERS_SETUP_DONE,
        PREPARE,
        PREPARED,
        LOAD,
        LOAD_EVENTS,
        LOADED,
        START,
        START_EVENTS,
        STARTED,
        REGISTRATION

    }

    /**
     * Various stages that are tracked in StructuredDeploymentTracing. The stages can carry additional information -
     * a component.
     */
    public enum AppStage {
        /**
         * Activities related to reading contents of application archive. Component refers to type of service being
         * looked up in the server.
         */
        OPENING_ARCHIVE,

        /**
         * Activities related to transforming the contents of application
         * archive from javax.* to jakarta.* and vice-versa.
         */
        TRANSFORM_ARCHIVE,

        /**
         * Validate whether deployment is fit for specified target. Component {@code command} validates correctness of the command
         * parameter, component {@code registry} validates possibility of deployment to target instance(s).
         */

        VALIDATE_TARGET,

        /**
         * Creation of deployment context. Deployment context aggregates all information on a deployment process, and
         * there are two phases with two distinct contexts - initial and full.
         */
        CREATE_DEPLOYMENT_CONTEXT,

        /**
         * Processing of event hooks. During deployment, the process exposes several extensions points by means of event
         * bus. The component of this span lists {@code EventTypes.name} of the event being sent and synchronously processed
         * by respective hooks.
         */
        PROCESS_EVENTS,

        /**
         * Determine name of an app. App name is looked up in variety of descriptors and algorithms defined by the specifications.
         * Component distinguishes these parts of determination.
         */
        DETERMINE_APP_NAME,


        /**
         * Cleanup deletes data of inactive application. Is can also cause surprising delayes due to filesystem locks
         * (usually on Windows). Components hints on server subdirectory being cleaned (applications or generated).
         */
        CLEANUP,

        /**
         * Coordinate version switch. Version switching involves disabling previous version of application on the target,
         * and that can take substantial time
         */
        SWITCH_VERSIONS,

        /**
         * Preparation of server components that will handle the application and validation of application.
         * Context of the event points at container being prepared, and component at type of server component in preparation.
         * An example of preparation step is EclipseLink's class weaving.
         */
        PREPARE,

        /**
         * Final step before starting an application.
         * Good example of initialization step is starting up EJB Timer Service when needed.
         */
        INITIALIZE,

        /**
         * Starting application. This is where application code gets first executed, all of the application-defined
         * components declared to run at startup are run.
         */
        START,

        /**
         * Update domain configuration with information on new application.
         */
        REGISTRATION,

        /**
         * Collect information on annotated types.
         */
        CLASS_SCANNING,

        /**
         * First start of a subsystem handling specific container.
         */
        CONTAINER_START,

        /**
         * Creation of application classloader. This is usually followed by making this classloader a context classloader
         * for deploying thread.
         */
        CREATE_CLASSLOADER,

        /**
         * Load the application code and prepare runtime structures.
         */
        LOAD,
        
        /**
         *
         * Reload the application code.
         */
        RELOAD,
    }

    public enum ModuleMark {
        PREPARE,
        PREPARE_EVENTS,
        PREPARED,
        LOAD,
        LOADED,
        START,
        STARTED

    }

    public enum ContainerMark {
        SNIFFER_DONE,
        BEFORE_CONTAINER_SETUP,
        AFTER_CONTAINER_SETUP,
        GOT_CONTAINER,
        GOT_DEPLOYER,
        PREPARE,
        PREPARED,
        LOAD,
        LOADED,
        START,
        STARTED
    }

    final long inception = System.currentTimeMillis();

    public DeploymentTracing(StructuredDeploymentTracing structured) {
        this.structured = structured;
    }

    public void close() {
        structured.close();
    }

    public long elapsed() {
        return System.currentTimeMillis() - inception;
    }

    public void addMark(Mark mark) {
        structured.addApplicationMark(mark);
    }

    public void addContainerMark(ContainerMark mark, String name) {
        structured.addContainerMark(name, mark);
    }

    public void addModuleMark(ModuleMark mark, String moduleName) {
        structured.addModuleMark(moduleName, mark);
    }

    public void print(PrintStream ps) {
        structured.print(ps);
    }

    public static void printModuleStatus(ModulesRegistry registry, Level level, Logger logger)
    {
        if (!logger.isLoggable(level)) {

            return;
        }
        int counter=0;

        StringBuilder sb = new StringBuilder("Module Status Report Begins\n");
        // first started :

        for (HK2Module m : registry.getModules()) {
            if (m.getState()== ModuleState.READY) {
                sb.append(m).append("\n");
                counter++;
            }
        }
        sb.append("there were ").append(counter).append(" modules in ACTIVE state");
        sb.append("\n");
        counter=0;
        // then resolved
        for (HK2Module m : registry.getModules()) {
            if (m.getState()== ModuleState.RESOLVED) {
                sb.append(m).append("\n");
                counter++;
            }
        }
        sb.append("there were ").append(counter).append(" modules in RESOLVED state");
        sb.append("\n");
        counter=0;
        // finally installed
        for (HK2Module m : registry.getModules()) {
            if (m.getState()!= ModuleState.READY && m.getState()!=ModuleState.RESOLVED) {
                sb.append(m).append("\n");
                counter++;
            }
        }
        sb.append("there were ").append(counter).append(" modules in INSTALLED state");
        sb.append("Module Status Report Ends");
        logger.log(level, sb.toString());
    }    
}
