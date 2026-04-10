/*
 * Copyright (c) 2022 Eclipse Foundation and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
// Portions Copyright [2025] [Payara Foundation and/or its affiliates]
// Payara Foundation and/or its affiliates elects to include this software in this distribution under the GPL Version 2 license

package com.sun.ejb.codegen;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.pfl.dynamic.codegen.spi.Type;

import static java.util.logging.Level.FINE;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._clear;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._setClassLoader;

/**
 * @author David Matejcek
 */
public class EjbClassGeneratorFactory implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(EjbClassGeneratorFactory.class.getName());

    private final ClassLoader loader;

    public EjbClassGeneratorFactory(final ClassLoader loader) {
        this.loader = loader;
        // WARN: Calls also org.glassfish.pfl.dynamic.codegen.spi.Type.clearCaches() which
        //       makes them inaccessible for some classloader implementations (and for
        //       some classloaders it means they will be reloaded.
        _setClassLoader(loader);
    }

    /**
     * @return {@link ClassLoader} owning the generated class.
     */
    protected ClassLoader getClassLoader() {
        return this.loader;
    }


    public Class<?> ensureGenericHome(final Class<?> anchorClass) throws GeneratorException {
        GenericHomeGenerator generator = new GenericHomeGenerator(loader, anchorClass);
        Class<?> clazz = loadClassIgnoringExceptions(loader, generator.getGeneratedClassName());
        if (clazz != null) {
            return clazz;
        }
        return generate(generator);
    }


    public Class<?> ensureServiceInterface(Class<?> ejbClass) throws GeneratorException {
        ServiceInterfaceGenerator generator = new ServiceInterfaceGenerator(loader, ejbClass);
        Class<?> clazz = loadClassIgnoringExceptions(loader, generator.getGeneratedClassName());
        if (clazz != null) {
            return clazz;
        }
        return generate(generator);
    }


    /**
     * The generated remote business interface and the client wrapper
     * for the business interface are produced dynamically.
     * This call must be made before any EJB 3.0 Remote business interface
     * runtime behavior is needed for a particular classloader.
     */
    public Class<?> ensureRemote(String businessInterfaceName) throws GeneratorException {
        String generatedRemoteIntfName = RemoteGenerator.getGeneratedRemoteIntfName(businessInterfaceName);
        String wrapperClassName = Remote30WrapperGenerator.getGeneratedRemoteWrapperName(businessInterfaceName);
        Class<?> foundRemoteIntf = loadClassIgnoringExceptions(loader, generatedRemoteIntfName);
        Class<?> foundRemoteWrapper = loadClassIgnoringExceptions(loader, wrapperClassName);
        if (foundRemoteIntf != null && foundRemoteWrapper != null) {
            return foundRemoteIntf;
        }
        final Class<?> remoteIntf;
        if (foundRemoteIntf == null) {
            RemoteGenerator generator = new RemoteGenerator(loader, businessInterfaceName);
            remoteIntf = generate(generator);
        } else {
            remoteIntf = foundRemoteIntf;
        }
        if (foundRemoteWrapper == null) {
            Remote30WrapperGenerator generator
                = new Remote30WrapperGenerator(loader, businessInterfaceName, generatedRemoteIntfName);
            generate(generator);
        }
        return remoteIntf;
    }


    private Class<?> generate(Generator generator) throws GeneratorException {
        // if more threads use the same classloader and same classes, we don't want
        // to allow any collisions.
        synchronized (loader) {
            // Other implementations don't clear after themselves, so we have to do it.
            _clear();
            Class<?> clazz = loadClassIgnoringExceptions(loader, generator.getGeneratedClassName());
            if (clazz != null) {
                return clazz;
            }
            final ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(getClassLoader());
                final Class<?> generatedClass = generator.generate();
                LOG.log(Level.CONFIG, "Generated class: {0} by generator: {1}",
                    new Object[] {generatedClass, generator});

                // Some classloaders don't remember generated classes (ie. ASURLClassLoader used by EJB clients)
                // Type has an internal cache. Otherwise it would try to load the class and fail.
                Objects.requireNonNull(Type.type(generatedClass));
                return generatedClass;
            } catch (IllegalAccessException | RuntimeException e) {
                throw new GeneratorException("Generator failed: " + generator, e);
            } finally {
                _clear();
                Thread.currentThread().setContextClassLoader(origClassLoader);
            }
        }
    }


    @Override
    public void close() {
        _setClassLoader(null);
    }


    private static Class<?> loadClassIgnoringExceptions(ClassLoader classLoader, String className) {
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            LOG.log(FINE, "Could not load class: " + className + " by classloader " + classLoader, e);
            return null;
        }
    }


}
