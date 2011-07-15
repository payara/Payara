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

package org.glassfish.appclient.server.core;

import java.util.Map;
import java.util.Hashtable;
import java.util.logging.Level;

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.deployment.util.DOLUtils;

/**
 * All client jar file are created in a separate thread when the client
 * jar file is not requested at deployment time. These threads are 
 * registered in this singleton registry so that when client jar files are 
 * requested from the deployment clients, we check if the client jar 
 * file is not in the process of being created.
 *
 * @author Jerome Dochez
 */
public class ClientJarMakerRegistry {
    
    // I am a singleton class
    private static ClientJarMakerRegistry theRegistry;
    
    // the registered client jar creator threads
    Map registeredThreads=null;
    
    /** Creates a new instance of ClientJarMakerRegistry */
    protected ClientJarMakerRegistry() {
        // I use hashtable since it needs to synchronized
        registeredThreads = new Hashtable();
    }
    
    /**
     * @return the singleton instance of ClientJarMakerRegistry
     */
    public synchronized static ClientJarMakerRegistry getInstance() {
        
        if (theRegistry==null) {
            theRegistry = new ClientJarMakerRegistry();
        }
        return theRegistry;
    }
    
    
    /**
     * Register a new thread in the registry 
     * @param the module ID we are creating the client jar for
     * @param the thread object responsible for creating the 
     * client jar file
     */
    public void register(String moduleID, Thread clientJarMaker) {
        
        registeredThreads.put(moduleID, clientJarMaker);
    }
    
    /**
     * @return true if the passed module ID has a registered thread
     */
    public boolean isRegistered(String moduleID) {
        
        return registeredThreads.containsKey(moduleID);
    }
    
    /**
     * Unregister a thread in the registry. This is done when the 
     * thread has finished its execution
     * @param the module ID identifying the module this thread was
     * creating the client jar for.
     */
    public void unregister(String moduleID) {
        
        registeredThreads.remove(moduleID);
    }
    
    /**
     * wait for a particular thread maker to finish process before 
     * returning
     */
    public void waitForCompletion(String moduleID) {
        
        Thread maker = (Thread) registeredThreads.get(moduleID);
        if (maker==null) 
            return;
        
        try {
            maker.join();
        } catch(InterruptedException e) {
            StringManager localStrings = StringManager.getManager( ClientJarMakerRegistry.class );            
            DOLUtils.getDefaultLogger().log(Level.SEVERE, 
                localStrings.getString("enterprise.deployment.error_creating_client_jar", 
                    e.getLocalizedMessage()) ,e);            
        }
        
        return;
    }
    
}
