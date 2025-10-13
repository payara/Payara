/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2019-2020] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.bootstrap;

import com.sun.enterprise.module.HK2Module;
import com.sun.enterprise.module.LifecyclePolicy;
import com.sun.enterprise.module.ModuleState;
import com.sun.enterprise.module.common_impl.LogHelper;
import java.util.logging.Level;

/**
 *
 * @author dochez
 */
public class H2Lifecycle implements LifecyclePolicy {
    
    /** Creates a new instance of DerbyLifecycle */
    public H2Lifecycle() {
    }
    
    /**
     * Callback when the module enters the {@link ModuleState#READY READY} state.
     * This is a good time to do any type of one time initialization 
     * or set up access to resources
     * @param module the module instance
     */
    public void start(HK2Module module) {
   
        try {
            final HK2Module myModule = module;
            Thread thread = new Thread() {
                public void run() {
                    try {
                        try {                     
                            Class driverClass = myModule.getClassLoader().loadClass("org.h2.Driver");
                            myModule.setSticky(true);
                            driverClass.newInstance();
                        } catch(ClassNotFoundException e) {
                            LogHelper.getDefaultLogger().log(Level.SEVERE, "Cannot load H2 Driver ",e);
                        } catch(java.lang.InstantiationException e) {
                            LogHelper.getDefaultLogger().log(Level.SEVERE, "Cannot instantiate H2 Driver", e);
                        } catch(IllegalAccessException e) {
                            LogHelper.getDefaultLogger().log(Level.SEVERE, "Cannot instantiate H2 Driver", e);
                        }                   
                    }   
                    catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            };
            thread.start();
        } catch (Throwable t) {
            t.printStackTrace();
        }        

        
    }
    
    /** 
     * Callback before the module starts being unloaded. The runtime will 
     * free all the module resources and returned to a {@link ModuleState#NEW NEW} state.
     * @param module the module instance
     */
    public void stop(HK2Module module) {
    
    }
    
}
