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

package org.glassfish.admingui.common.plugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

//import static com.sun.enterprise.web.Constants.HABITAT_ATTRIBUTE;

import org.glassfish.admingui.plugin.ConsolePluginService;
import org.glassfish.hk2.api.ServiceLocator;


/**
 *  <p>	This <code>ClassLoader</code> makes it possible to access plugin
 *	resources by finding the appropriate plugin module's
 *	<code>ClassLoader</code> and loading resources from it.</p>
 *
 *  @author Ken Paulsen	(ken.paulsen@sun.com)
 */
public class ConsoleClassLoader extends ClassLoader {

    // This is defined in the web module, but for now I don't want to depend
    // on that module to get the value of this variable.
    public static final String HABITAT_ATTRIBUTE = "org.glassfish.servlet.habitat";

    /**
     *	<p> This constructor should not normally be used.  You should use
     *	    the one that allows you to provide the parent
     *	    <code>ClassLoader</code>.</p>
     */
    protected ConsoleClassLoader() {
	super();
    }

    /**
     *	<p> This constructor creates an instance of this
     *	    <code>ClassLoader</code> and will use the given
     *	    <code>ClassLoader</code> as its parent <code>ClassLoader</code>.</p>
     *
     *	@param	parent	The parent <code>ClassLoader</code>
     */
    public ConsoleClassLoader(ClassLoader parent) {
	super(parent);
    }

    /**
     *	<p> This method will attempt to look for a module...</p>
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Class c = findLoadedClass(name);
        if(c!=null) return c;
        return super.findClass(name);
    }
     */

    /**
     *	<p> In order for this method to find the Resource...
     *	    </p>
     */
    public URL findResource(String name) {
//System.out.println("Find Resource: " + name);
	// Find module name
	int end = name.indexOf('/');
	int start = 0;
	while (end == start) {
	    end = name.indexOf('/', ++start);
	}
	if (end == -1) {
	    // Not a request for a module resource
	    return null;
	}
	String moduleName = name.substring(start, end);
	name = name.substring(end + 1);
	if (start != 0) {
	    // This means the original request was prefixed with a "/"
	    name = "/" + name;
	}

	// Get the Module ClassLoader
	ClassLoader moduleCL = findModuleClassLoader(moduleName);
	if (moduleCL != null) {
	    // Use the Module ClassLoader to find the resource
	    if (moduleCL instanceof URLClassLoader) {
		URL url = ((URLClassLoader) moduleCL).findResource(name);
//System.out.println("findResource("+name+"), URL: " + url);
		return url;
	    } else {
		return moduleCL.getResource(name);
	    }
	}

	// Not found.
	return null;
    }

    /**
     *	<p> This method find the <code>ClassLoader</code> associated with the
     *	    named module.</p>
     */
    public static ClassLoader findModuleClassLoader(String moduleName) {
//System.out.println("Find module ClassLoader: " + moduleName);
	// Get the ServletContext
	ServletContext servletCtx = (ServletContext)
	    (FacesContext.getCurrentInstance().getExternalContext()).getContext();

	// Get the Habitat from the ServletContext
	ServiceLocator habitat = (ServiceLocator) servletCtx.getAttribute(HABITAT_ATTRIBUTE);

	// Use the Habitat to find the ConsolePluginService and return the
	// correct ClassLoader for the requested module (or null)
	return habitat.<ConsolePluginService>getService(ConsolePluginService.class).
	    getModuleClassLoader(moduleName);
    }

// FIXME: I need to finish implementing this class!  So far I only support getResource()

/*
    public Enumeration<URL> findResources(String name) throws IOException {

        for (ClassLoaderFacade classLoader : facadeSurrogates) {

            Enumeration<URL> enumerat = classLoader.getResources(name);
            if (enumerat!=null && enumerat.hasMoreElements()) {
                return enumerat;
            }
        }
        for (ClassLoader classLoader : surrogates) {
            Enumeration<URL> enumerat = classLoader.getResources(name);
            if (enumerat!=null && enumerat.hasMoreElements()) {
                return enumerat;
            }

        }
        return super.findResources(name);
    }
    */
}
