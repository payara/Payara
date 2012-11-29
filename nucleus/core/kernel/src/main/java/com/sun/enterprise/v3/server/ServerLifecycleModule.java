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

package com.sun.enterprise.v3.server;

import com.sun.appserv.server.LifecycleEvent;
import com.sun.appserv.server.LifecycleEventContext;
import com.sun.appserv.server.LifecycleListener;
import com.sun.appserv.server.ServerLifecycleException;
import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.kernel.KernelLoggerInfo;
import org.glassfish.loader.util.ASClassLoaderUtil;

/**
 * @author Sridatta Viswanath
 */

public final class ServerLifecycleModule {
    
    private LifecycleListener slcl;
    private String name;
    private String className;
    private String classpath;
    private int loadOrder;
    private boolean isFatal = false;
    private String statusMsg = "OK";
    
    private ServerContext ctx;
    private LifecycleEventContext leContext;
    private ClassLoader urlClassLoader;
    private Properties props = new Properties();

    private static final Logger _logger = KernelLoggerInfo.getLogger();
    private static boolean _isTraceEnabled = false;

    private final static String LIFECYCLE_PREFIX = "lifecycle_"; 

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ServerLifecycleModule.class);

    ServerLifecycleModule(ServerContext ctx, String name, String className) {
        this.name = name;
        this.className = className;
        this.ctx = ctx;
        this.leContext = new LifecycleEventContextImpl(ctx);

        _isTraceEnabled = _logger.isLoggable(Level.FINE);
    }
    
    void setClasspath(String classpath) {
        this.classpath = classpath;
    }
    
    void setProperty(String name, String value) {
        props.put(name, value);
    }
    
    Properties getProperties() {
        return this.props;
    }
    
    void setLoadOrder(int loadOrder) {
        this.loadOrder = loadOrder;
    }
    
    void setIsFatal(boolean isFatal) {
        this.isFatal = isFatal;
    }
        
    String getName() {
        return this.name;
    }
    
    String getClassName() {
        return this.className;
    }

    String getClasspath() {
        return this.classpath;
    }

    int getLoadOrder() {
        return this.loadOrder;
    }
    
    boolean isFatal() {
        return isFatal;
    }
    
    LifecycleListener loadServerLifecycle() throws ServerLifecycleException {
        ClassLoader classLoader = ctx.getLifecycleParentClassLoader();

        try {
            if (this.classpath != null) {
                URL[] urls = getURLs();

                if (urls != null) {
                    StringBuffer sb = new StringBuffer(128);
                    for(int i=0;i<urls.length;i++) {
                        sb.append(urls[i].toString());
                    }
                    if (_isTraceEnabled)
                        _logger.fine("Lifecycle module = " + getName() + 
                                        " has classpath URLs = " + sb.toString());
                }

                this.urlClassLoader = new URLClassLoader(urls, classLoader);
                classLoader = this.urlClassLoader;
            }

            Class cl = Class.forName(className, true, classLoader);
            slcl = (LifecycleListener) cl.newInstance();
        } catch (Exception ee) {
            _logger.log(Level.SEVERE, KernelLoggerInfo.exceptionLoadingLifecycleModule,
                    new Object[] {this.name, ee}) ;
            if (isFatal) {
                throw new ServerLifecycleException(localStrings.getLocalString("lifecyclemodule.loadExceptionIsFatal", "Treating failure loading the lifecycle module as fatal", this.name));
            }
        }

        return slcl;
    }
    
    private URL[] getURLs() {
        List<URL> urlList = ASClassLoaderUtil.getURLsFromClasspath(
            this.classpath, File.pathSeparator, "");
        return ASClassLoaderUtil.convertURLListToArray(urlList);
    }

    private void postEvent(int eventType, Object data)
                                    throws ServerLifecycleException {
        if (slcl == null) {
            if (isFatal) {
                throw new ServerLifecycleException(localStrings.getLocalString("lifecyclemodule.loadExceptionIsFatal", "Treating failure loading the lifecycle module as fatal", this.name));
            }

            return;
        }

        if (urlClassLoader != null)
            setClassLoader();

        LifecycleEvent slcEvent= new LifecycleEvent(this, eventType, data, this.leContext);
        try {
            slcl.handleEvent(slcEvent);
        } catch (ServerLifecycleException sle) {
            _logger.log(Level.WARNING, KernelLoggerInfo.serverLifecycleException, 
                    new Object[] {this.name, sle});

            if (isFatal)
                throw sle;
        } catch (Exception ee) {
            _logger.log(Level.WARNING, KernelLoggerInfo.lifecycleModuleException,
                    new Object[] {this.name, ee});

            if (isFatal) {
                throw new ServerLifecycleException(localStrings.getLocalString("lifecyclemodule.event_exceptionIsFatal", "Treating the exception from lifecycle module event handler as fatal"), ee);
            }
        }
    }
    
    public void onInitialization() 
                                throws ServerLifecycleException {
        postEvent(LifecycleEvent.INIT_EVENT, props);
    }

    public void onStartup()
                                    throws ServerLifecycleException {
        postEvent(LifecycleEvent.STARTUP_EVENT, props);
    }
    
    public void onReady() throws ServerLifecycleException {
        postEvent(LifecycleEvent.READY_EVENT, props);
    }

    public void onShutdown() throws ServerLifecycleException {
        postEvent(LifecycleEvent.SHUTDOWN_EVENT, props);
    }
    
    public void onTermination() throws ServerLifecycleException {
        postEvent(LifecycleEvent.TERMINATION_EVENT, props);
    }
    
    private void setClassLoader() {
         // set the url class loader as the thread context class loader
        java.security.AccessController.doPrivileged(
            new java.security.PrivilegedAction() {
                public Object run() {
                    Thread.currentThread().setContextClassLoader(urlClassLoader);
                    return null;
                }
            }
        );
    }

    /**
     * return status of this lifecycle module as a string
     */
    public String getStatus() {
        return statusMsg;
    }

    public String toString() {
        return "Server LifecycleListener support";
    }
}
