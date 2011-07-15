/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.web.connector.extension;

import org.apache.catalina.*;

/**
 * Listener used to receive events from Catalina when a <code>Context</code>
 * is removed or when a <code>Host</code> is removed.
 *
 * @author Jean-Francois Arcand
 */
public class CatalinaListener  implements ContainerListener{
    
    public void containerEvent(ContainerEvent event) {    
        if (Container.REMOVE_CHILD_EVENT.equals(event.getType()) ) {
            Context context;
            String contextPath;
            Host host;

            Object container = event.getData();            
            if ( container instanceof Context) {
                context = (Context)container;
                
                if (!context.hasConstraints() &&
                        context.findFilterDefs().length == 0 ){        
                    contextPath = context.getPath();
                    host = (Host)context.getParent();
                    String[] names = host.getNetworkListenerNames();
                    for (String name : names) {
                        removeContextPath(name, contextPath); 
                    }
                }
            } 
        }  
    }  
    
    
    /**
     * Remove from the <code>FileCache</code> all entries related to 
     * the <code>Context</code> path.
     * @param id the <code>FileCacheFactory</code> id
     * @param contextPath the <code>Context</code> path
     */
    private void removeContextPath(String id, String contextPath) {
        // FIXME: I can't spot where Grizzly is registering mbeans, and this code
        // tries to invoke it and fails during the undeployment.
        // Commented out for now for the sake of JavaOne demo.

//        ArrayList<GrizzlyConfig> list =
//                GrizzlyConfig.getGrizzlyConfigInstances();
//        for(GrizzlyConfig config: list){
//            if (config.getPort() == port){
//                config.invokeGrizzly("removeCacheEntry",
//                        new Object[]{contextPath},
//                        new String[]{"java.lang.String"});
//            }
//        }
    }  
}

