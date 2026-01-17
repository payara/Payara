/*
 * Copyright (c) 1997, 2020 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021-2022 Contributors to the Eclipse Foundation
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

import com.sun.ejb.containers.GenericEJBHome;
import java.rmi.Remote;
import java.rmi.RemoteException;

import static java.lang.reflect.Modifier.ABSTRACT;
import static java.lang.reflect.Modifier.PUBLIC;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._String;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._arg;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._end;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._interface;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._method;
import static org.glassfish.pfl.dynamic.codegen.spi.Wrapper._t;

/**
 * This class is used to generate a sub-interface of the
 * GenericEJBHome interface that will be loaded within each
 * application.
 */
public class GenericHomeGenerator extends Generator {

    public static final String GENERIC_HOME_CLASSNAME = "GenericEJBHome_Generated";
    private final Class<?> anchorClass;
    private final String packageName;

    /**
     * @param loader {@link ClassLoader} owning generated classes
     * @param anchorClass
     */
    public GenericHomeGenerator(final ClassLoader loader, final Class<?> anchorClass) {
        super(loader);
        this.anchorClass = anchorClass;
        this.packageName = getClass().getPackageName();
    }


    @Override
    public String getPackageName() {
        return this.packageName;
    }


    /**
     * Get the fully qualified name of the generated class.
     * @return the name of the generated class.
     */
    @Override
    public final String getGeneratedClassName() {
        return packageName + "." + GENERIC_HOME_CLASSNAME;
    }


    @Override
    public Class<?> getAnchorClass() {
        return this.anchorClass;
    }


    @Override
    public void defineClassBody() {
        _interface(PUBLIC, GENERIC_HOME_CLASSNAME, _t(GenericEJBHome.class.getName()));
        _method(PUBLIC | ABSTRACT, _t(Remote.class.getName()), "create", _t(RemoteException.class.getName()));
        _arg(_String(), "generatedBusinessIntf");
        _end();
    }
}
