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

package com.sun.enterprise.jbi.serviceengine.core;

import com.sun.logging.LogDomains;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents JavaEEDeployer private MBean, responsible for
 * deploy/undeploy/start/stop of JavaEE application packaged inside the service assembly.
 *
 * @author bhavanishankar@dev.java.net
 */

public class JavaEEDeployer extends JavaEEServiceEngineSUManager
        implements JavaEEDeployerMBean {

    protected static final Logger logger=LogDomains.getLogger(JavaEEDeployer.class, LogDomains.DPL_LOGGER);

    private String className = "JavaEEDeployer :: ";
    
    /**
     * Creates a new instance of JavaEEServiceEngineServiceUnitManager
     */
    public JavaEEDeployer() {
        super();
    }
    
    public void init(String saName, String suName, String suPath, String target)
    throws Exception {
        String methodSig = className + "init(String, String, String, String)";
        logger.log(Level.FINE,
                methodSig + " saName = " + saName + ", suName = " + suName +
                ". suPath = " + suPath + ", target = " + target);
    }
    
    public void shutDown(String saName, String suName, String target) throws
            Exception {
        String methodSig = className + "shutDown(String, String, String)";
        logger.log(Level.FINE,
                methodSig + " saName = " + saName + ", suName = " + suName +
                ", target = " + target);
    }
    
    public String deploy(String saName,
            String suName,
            String suPath,
            String target) throws Exception {
        String methodSig = className + "deploy(String, String, String, String)";
        logger.log(Level.FINE,
                methodSig + "saName = " + saName + ", suName = " + suName +
                ". suPath = " + suPath + ", target = " + target);
        
        return doDeploy(suName, suPath, target);
    }
    
    public String undeploy(String saName,
            String suName,
            String suPath,
            String target) throws Exception {
        String methodSig =
                className + "undeploy(String, String, String, String)";
        logger.log(Level.FINE,
                methodSig + " saName = " + saName + ", suName = " + suName +
                ". suPath = " + suPath + ", target = " + target);
        return doUnDeploy(suName, target);
    }
    
    public void start(String saName, String suName, String target) throws
            Exception {
        String methodSig = className + "start(String, String, String)";
        logger.log(Level.FINE,
                methodSig + " saName = " + saName + ", suName = " + suName +
                ", target = " + target);
        
        doStart(suName, target);
    }
    
    
    public void stop(String saName, String suName, String target) throws
            Exception {
        String methodSig = className + "stop(String, String, String)";
        logger.log(Level.FINE,
                methodSig + " saName = " + saName + ", suName = " + suName +
                ", target = " + target);
        
        doStop(suName, target);
    }
    
}
