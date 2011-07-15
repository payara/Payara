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

package com.sun.enterprise.jbi.serviceengine.component;

import com.sun.enterprise.jbi.serviceengine.core.*;
import com.sun.enterprise.jbi.serviceengine.bridge.*;
import com.sun.logging.LogDomains;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.ObjectName;
import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.ComponentLifeCycle;
import org.glassfish.api.ContractProvider;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.component.Habitat;

/**
 * Provide initialization, start, stop, and  shutdown processing. The JBI
 * implementation queries the component for the implementation of this
 * interface using the <code>Component.getComponentLifeCycle()</code> method.
 * The methods in this interface comprise the life cycle contract between the
 * JBI implementation and the component. The life cycle of a component begins
 * with a call to the <code>init()</code> method on an instance of the
 * component's implementation of this interface, and ends with the first call
 * to the <code>shutDown()</code> method on that instance. Between these two
 * calls, there can be any number of <code>stop()</code> and
 * <code>start()</code> calls.
 * @author Manisha Umbarje
 */

public class JavaEEServiceEngineLifeCycle implements ComponentLifeCycle {
    
    /**
     * This context provides access to data needed by all JBI components running in
     * the JBI environment.
     */
    
    private JavaEEServiceEngineContext context;
    private ComponentContext jbiContext;
    private Thread managerThread;
    
    /**
     * Internal handle to the logger instance
     */
    protected static final Logger logger =
            LogDomains.getLogger(JavaEEServiceEngineLifeCycle.class, LogDomains.SERVER_LOGGER);
    
    /** Creates a new instance of JavaEEServiceEngineLifeCycle */
    public JavaEEServiceEngineLifeCycle() {
    }
    
    /**
     * Get the JMX <code>ObjectName</code> for any additional MBean for this
     * component. If there is none, return <code>null</code>.
     * @return the JMX object name of the additional MBean or <code>null</code>
     * if there is no additional MBean.
     */
    public ObjectName getExtensionMBeanName(){
        return null;
    }
    
    /**
     * Initialize the component. This performs initialization required by the
     * component but does not make it ready to process messages. This method
     * is called once for each life cycle of the component.
     * @param context the component's context.
     * @throws javax.jbi.JBIException if the component is unable to initialize.
     */
    public void init(ComponentContext jbiContext) throws JBIException {
        this.jbiContext = jbiContext;
    }
    
    /**
     * Shut down the component. This performs cleanup before the component is
     * terminated. Once this method has been called, <code>init()</code> must
     * be called before the component can be started again with a call to
     * <code>start()</code>.
     * @throws javax.jbi.JBIException if the component is unable to shut down.
     */
    public void shutDown() throws JBIException {
        
    }
    
    /**
     * Start the component. This makes the component ready to process messages.
     * This method is called after <code>init()</code> completes when the JBI
     * implementation is starting up, and when the component is being restarted
     * after a previous call to <code>shutDown()</code>. If <code>stop()</code>
     * was called previously but <code>shutDown()</code> was not, then
     * <code>start()</code> can be called again without another call to
     * <code>init()</code>.
     * @throws javax.jbi.JBIException if the component is unable to start.
     */
    public void start() throws JBIException{
        try {
            //Initialize RuntimeHelper Service
            Habitat habitat = Globals.getDefaultHabitat();
            habitat.getComponent(ContractProvider.class, "ServiceEngineRuntimeHelper");

            if(ServiceEngineUtil.isServiceEngineEnabled()) {
                logger.log(Level.FINE, "Service Engine starting");
                context = JavaEEServiceEngineContext.getInstance();
                context.setJBIContext(jbiContext);
                context.initialize();
                
                if(context.isSunESB()) {
                    jbiContext.getMBeanServer().registerMBean(
                            new JavaEEDeployer(),
                            jbiContext.getMBeanNames().createCustomComponentMBeanName("JavaEEDeployer"));
                    logger.log(Level.FINE, "Successfully registered JavaEEDeployer.");
                }

                // Starts all the threads such as thread accepting a message from
                // DeliveryChannel and delivering it to WorkManager
                managerThread = new Thread(context.getWorkManager());
                managerThread.start();
                logger.log(Level.INFO, "serviceengine.success_start");
                
            } else {
                logger.log(Level.INFO,
                        "Java EE Service Engine is not active as it is disabled " +
                        "by setting the JVM flag com.sun.enterprise.jbi.se.disable to true");
            }
            
            
        }catch(Exception e) {
            logger.log(Level.SEVERE, "serviceengine.error_start",
                    new Object[]{e.getMessage()});
                    throw new JBIException(e.getMessage());
        }
    }
    
    /**
     * Stop the component. This makes the component stop accepting messages for
     * processing. After a call to this method, <code>start()</code> can be
     * called again without first calling <code>init()</code>.
     * @throws javax.jbi.JBIException if the component is unable to stop.
     */
    public void stop() throws JBIException {
        //Stop multiple threads involved in request processing by the
        //JavaEEServiceEngine
        try {
            if(ServiceEngineUtil.isServiceEngineEnabled()) {
                context.getDeliveryChannel().close();
                context.getWorkManager().stop();
                managerThread.join();
                if(context.isSunESB()) {
                    jbiContext.getMBeanServer().unregisterMBean(
                        jbiContext.getMBeanNames().createCustomComponentMBeanName("JavaEEDeployer"));
                }
                logger.log(Level.INFO, "serviceengine.success_stop");
            }
        } catch(Exception e) {
            e.printStackTrace();
            throw new JBIException(e.getMessage());
        }
    }    
}
