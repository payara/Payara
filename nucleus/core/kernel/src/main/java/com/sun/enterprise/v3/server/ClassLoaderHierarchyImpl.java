/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2012 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.internal.api.ClassLoaderHierarchy;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.jvnet.hk2.config.TranslationException;
import org.jvnet.hk2.config.VariableResolver;
import org.glassfish.internal.api.DelegatingClassLoader;
import org.glassfish.internal.api.ConnectorClassLoaderService;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.DeploymentContext;

import java.net.URI;
import java.net.MalformedURLException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.jar.Manifest;
import java.io.File;
import java.io.IOException;

import com.sun.enterprise.module.*;
import com.sun.enterprise.module.common_impl.DirectoryBasedRepository;
import com.sun.enterprise.module.common_impl.Tokenizer;


/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
@Service
public class ClassLoaderHierarchyImpl implements ClassLoaderHierarchy {
    @Inject APIClassLoaderServiceImpl apiCLS;

    @Inject CommonClassLoaderServiceImpl commonCLS;

    //For distributions where connector module is not available.
    @Inject @Optional ConnectorClassLoaderService connectorCLS;

    @Inject
    AppLibClassLoaderServiceImpl applibCLS;

    @Inject
    ModulesRegistry modulesRegistry;

    @Inject
    Logger logger;

    @Inject
    ServiceLocator habitat;

    SystemVariableResolver resolver = new SystemVariableResolver();

    public ClassLoader getAPIClassLoader() {
        return apiCLS.getAPIClassLoader();
    }

    public ClassLoader getCommonClassLoader() {
        return commonCLS.getCommonClassLoader();
    }

    public String getCommonClassPath() {
        return commonCLS.getCommonClassPath();
    }

    public DelegatingClassLoader getConnectorClassLoader(String application) {
        // For distributions where connector module (connector CL) is not available, use empty classloader with parent
        if(connectorCLS != null){
            return connectorCLS.getConnectorClassLoader(application);
        }else{
            return AccessController.doPrivileged(new PrivilegedAction<DelegatingClassLoader>() {
                public DelegatingClassLoader run() {
                    return new DelegatingClassLoader(commonCLS.getCommonClassLoader());
                }
            });
        }
    }

    public ClassLoader getAppLibClassLoader(String application, List<URI> libURIs) throws MalformedURLException {
        return applibCLS.getAppLibClassLoader(application, libURIs);
    }

    public DelegatingClassLoader.ClassFinder getAppLibClassFinder(List<URI> libURIs) throws MalformedURLException {
        return applibCLS.getAppLibClassFinder(libURIs);
    }

    /**
     * Sets up the parent class loader for the application class loader.
     * Application class loader are under the control of the ArchiveHandler since
     * a special archive file format will require a specific class loader.
     *
     * However GlassFish needs to be able to add capabilities to the application
     * like adding APIs accessibility, this is done through its parent class loader
     * which we create and maintain.
     *
     * @param parent the parent class loader
     * @param context deployment context
     * @return class loader capable of loading public APIs identified by the deployers
     * @throws ResolveError if one of the deployer's public API module is not found.
     */
    public ClassLoader createApplicationParentCL(ClassLoader parent, DeploymentContext context)
        throws ResolveError {

        final ReadableArchive source = context.getSource();
        List<ModuleDefinition> defs = new ArrayList<ModuleDefinition>();

        // now let's see if the application is requesting any module imports
        Manifest m=null;
        try {
            m = source.getManifest();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Cannot load application's manifest file :", e.getMessage());
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, e.getMessage(), e);
            }
        }
        if (m!=null) {
            String importedBundles = m.getMainAttributes().getValue(ManifestConstants.BUNDLE_IMPORT_NAME);
            if (importedBundles!=null) {
                for( String token : new Tokenizer(importedBundles,",")) {
                    Collection<Module> modules = modulesRegistry.getModules(token);
                    if (modules.size() ==1) {
                        defs.add(modules.iterator().next().getModuleDefinition());
                    } else {
                        throw new ResolveError("Not able to locate a unique module by name " + token);
                    }
                }
            }

	    // Applications can add an additional osgi repos...
            String additionalRepo = m.getMainAttributes().getValue(org.glassfish.api.ManifestConstants.GLASSFISH_REQUIRE_REPOSITORY);
	    if (additionalRepo != null) {
                for (String token : new Tokenizer(additionalRepo, ",")) {
		    // Each entry should be name=path
		    int equals = token.indexOf('=');
		    if (equals == -1) {
			// Missing '='...
			throw new IllegalArgumentException("\""
			    + org.glassfish.api.ManifestConstants.GLASSFISH_REQUIRE_REPOSITORY
			    + ": " + additionalRepo + "\" is missing an '='.  "
			    + "It must be in the format: name=path[,name=path]...");
		    }
		    String name = token.substring(0, equals);
		    String path = token.substring(++equals);
		    addRepository(name, resolver.translate(path));
		}
	    }

	    // Applications can also request to be wired to implementors of certain services.
	    // That means that any module implementing the requested service will be accessible
	    // by the parent class loader of the application.
            String requestedWiring = m.getMainAttributes().getValue(org.glassfish.api.ManifestConstants.GLASSFISH_REQUIRE_SERVICES);
            if (requestedWiring!=null) {
                for (String token : new Tokenizer(requestedWiring, ",")) {
                    for (Object impl : habitat.getAllServices(BuilderHelper.createContractFilter(token))) {
                        Module wiredBundle = modulesRegistry.find(impl.getClass());
                        if (wiredBundle!=null) {
                            defs.add(wiredBundle.getModuleDefinition());
                        }
                    }
                }
            }
        }

        if (defs.isEmpty()) {
            return parent;
        }  else {
            return modulesRegistry.getModulesClassLoader(parent, defs);
        }
    }

    /**
     *	<p> This method installs the admin console OSGi bundle respository so
     *	    our plugins can be found.</p>
     */
    private void addRepository(String name, String path) {
	File pathFile = new File(path);
	Repository repo = new DirectoryBasedRepository(
		name, pathFile);
	modulesRegistry.addRepository(repo);
	try {
	    repo.initialize(); 
	} catch (IOException ex) {
	    logger.log(Level.SEVERE,
		"Problem initializing additional repository!", ex);
	}
    }

    /**
     *	<p> This class helps resolve ${} variables in Strings.</p>
     */
    private static class SystemVariableResolver extends VariableResolver {
	SystemVariableResolver() {
	    super();
	}

	protected String getVariableValue(final String varName) throws TranslationException {
	    String result = null;

	    // first look for a system property
	    final Object value = System.getProperty(varName);
	    if (value != null) {
		result = "" + value;
	    } else {
		result = "${" + varName + "}";
	    }
	    return result;
	}

	/**
	    Return true if the string is a template string of the for ${...}
	 */
	public static boolean needsResolving(final String value) {
	    return (value != null) && (value.indexOf("${") != -1);
	}

	/**
	 *  Resolve the given String.
	 */
	public String resolve(final String value) throws TranslationException {
	    final String result = translate(value);
	    return result;
	}
    }
}
