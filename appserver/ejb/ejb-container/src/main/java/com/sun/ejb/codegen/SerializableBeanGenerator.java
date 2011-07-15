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

package com.sun.ejb.codegen;

import java.lang.reflect.Method;
import java.io.*;
import java.util.*;
import java.util.logging.*;
import com.sun.logging.*;
import com.sun.ejb.EJBUtils;

import javax.ejb.EnterpriseBean;
import javax.ejb.SessionBean;
import javax.ejb.EntityBean;
import com.sun.enterprise.deployment.*;
import com.sun.enterprise.util.LocalStringManagerImpl;

import static java.lang.reflect.Modifier.*;

import static com.sun.corba.ee.spi.orbutil.codegen.Wrapper.*;
import com.sun.corba.ee.spi.orbutil.codegen.Type;



/**
 * This class is used to generate a Serializable sub-class
 * of a 3.0 stateful session bean.
 */

public class SerializableBeanGenerator extends Generator 
    implements ClassGeneratorFactory {

    private static LocalStringManagerImpl localStrings =
	new LocalStringManagerImpl(SerializableBeanGenerator.class);


    private Class beanClass;
    private String generatedSerializableClassName;

    private ClassLoader loader;

    /**
     * Get the fully qualified name of the generated class.
     * @return the name of the generated class.
     */
    public String getGeneratedClass() {
        return generatedSerializableClassName;
    }

    // For corba codegen infrastructure
    public String className() {
        return getGeneratedClass();
    }

    public SerializableBeanGenerator(ClassLoader cl,
                                     String beanClassName) 
	throws GeneratorException 
    {
	    super();

        loader = cl;
	    try {
	        beanClass = cl.loadClass(beanClassName);
	    } catch (ClassNotFoundException ex) {
	        throw new InvalidBean(
		    localStrings.getLocalString(
		    "generator.remote_interface_not_found",
		    "Remote interface not found "));
	    }

        generatedSerializableClassName = 
            EJBUtils.getGeneratedSerializableClassName(beanClassName);

    }

    public void evaluate() {

        _clear();

	    String packageName = getPackageName(generatedSerializableClassName);
        String simpleName = getBaseName(generatedSerializableClassName);
        
        if( packageName != null ) {
            _package(packageName);
        } else {
            // no-arg _package() call is required for default package
            _package();
        }

        List toImplement = new LinkedList<Type>();
        toImplement.add(_t("java.io.Serializable"));
        _class(PUBLIC, simpleName, _t(beanClass.getName()), toImplement);

        _constructor( PUBLIC ) ;
        _body();
        _expr(_super(_s(_void()))) ;
        _end();

        _method( PRIVATE, _void(), "writeObject", _t("java.io.IOException"));
            _arg( _t("java.io.ObjectOutputStream"), "oos" );
        _body();
            _expr(_call( _t("com.sun.ejb.EJBUtils"), "serializeObjectFields",
                   _s(_void(), 
                      _Class(), _Object(), _t("java.io.ObjectOutputStream")),
                   _const(_t(beanClass.getName())), _this(), 
                   _v("oos")));
        _end();


        _method( PRIVATE, _void(), "readObject", _t("java.io.IOException"),
                 _t("java.lang.ClassNotFoundException") );
            _arg( _t("java.io.ObjectInputStream"), "ois" );
        _body();
            _expr(_call( _t("com.sun.ejb.EJBUtils"), "deserializeObjectFields",
                   _s(_void(), 
                      _Class(), _Object(), _t("java.io.ObjectInputStream")),
                   _const(_t(beanClass.getName())), _this(), 
                   _v("ois")));
        _end();
     
        _end();

         _classGenerator();

        return;
    }



}
