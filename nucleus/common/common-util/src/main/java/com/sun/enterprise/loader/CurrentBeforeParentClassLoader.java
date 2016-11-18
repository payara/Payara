/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright [2016] [Payara Foundation and/or its affiliates]
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

package com.sun.enterprise.loader;

import com.sun.enterprise.util.CULoggerInfo;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.util.logging.Logger;

/**
 * As the name suggests, loads classes via current class loader ahead of paren class loader
 * by default, classes are loaded from the parent ClassLoader first, thus this class is 
 * needed to allow overriding App Server's Modules / JAR files by placing them
 * into &lt;domain_dir&gt;/lib or EAR lib directory
 * @author lprimak
 */
public class CurrentBeforeParentClassLoader extends URLClassLoader {
    public CurrentBeforeParentClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public CurrentBeforeParentClassLoader(URL[] urls) {
        super(urls);
    }

    public CurrentBeforeParentClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }
    
    
    /**
     * Local-first class loading, instead of parent-first as the ClassLoader.loadClass() does
     * 
     * @param name
     * @param resolve
     * @return Loaded class
     * @throws ClassNotFoundException 
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        boolean isWhitelisted = isWhitelistEnabled() && isWhiteListed(name);
        if((!currentBeforeParentEnabled && (isWhitelistEnabled()? isWhitelisted : true)) || isAlwaysDelegate(name))
        {
            return super.loadClass(name, resolve);
        }
        synchronized (getClassLoadingLock(name)) {
            // First, check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
            if(c != null) {
                return c;
            }
            try {
                c = findClass(name);
                logger.finest(String.format("Found Locally: %s - %s", name, c.getName()));
            }
            catch(ClassNotFoundException e) {
                if(!isWhitelistEnabled() || isWhitelisted) {
                    logger.finest(String.format("Not Found Locally - Looking in Parent: %s", name));
                    return parent.loadClass(name);
                }
                else {
                    throw new ClassNotFoundException(String.format("Whitelist enabled, but class [%s] is not whitelisted", name), e);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }


    /**
     * support for extreme class loading
     *
     * @param className
     * @return true if white-listed
     */
    protected boolean isWhiteListed(String className) {
        return false;
    }


    /**
     * @return true if extreme classloading is enabled
     */
    protected boolean isWhitelistEnabled() {
        return false;
    }
    
    
    /**
     * enable current-first behavior
     * conditional upon PARENT_CLASSLOADER_DELEGATE_PROPERTY system property being turned on
     */
    final public void enableCurrentBeforeParent() {
        String parentClassLoaderDelegateStr = System.getProperty(PARENT_CLASSLOADER_DELEGATE_PROPERTY, "true");
        if(!Boolean.parseBoolean(parentClassLoaderDelegateStr)) {
            currentBeforeParentEnabled = true;
        }
    }
    
    /**
     * enable current-first behavior unconditionally, regardless of system property
     * used by application configuration parser, so if application developer uses the config xml element,
     * they presumably want the behavior regardless of the system property settings
     */
    final public void enableCurrentBeforeParentUnconditional() {
        currentBeforeParentEnabled = true;
    }

    /**
     * disable functionality
     */
    final public void disableCurrentBeforeParent() {
        currentBeforeParentEnabled = false;
    }
    
    private boolean isAlwaysDelegate(String name) {
        return name.startsWith("sun") || name.startsWith("javax");
    }


    protected boolean currentBeforeParentEnabled = false;
    
    private final ClassLoader system = getClass().getClassLoader();
    private final ClassLoader _parent = getParent();
    private final ClassLoader parent = _parent != null? _parent : system;
    private static final Logger logger = CULoggerInfo.getLogger();
    public static final String PARENT_CLASSLOADER_DELEGATE_PROPERTY = "fish.payara.classloading.delegate";
}
