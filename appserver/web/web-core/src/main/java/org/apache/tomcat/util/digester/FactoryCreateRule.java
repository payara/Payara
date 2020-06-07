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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// Portions Copyright [2019] Payara Foundation and/or affiliates

package org.apache.tomcat.util.digester;

import org.xml.sax.Attributes;

import java.util.logging.Level;


/**
 * <p>Rule implementation that uses an {@link ObjectCreationFactory} to create
 * a new object which it pushes onto the object stack.  When the element is
 * complete, the object will be popped.</p>
 *
 * <p>This rule is intended in situations where the element's attributes are
 * needed before the object can be created.  A common senario is for the
 * ObjectCreationFactory implementation to use the attributes  as parameters
 * in a call to either a factory method or to a non-empty constructor.
 */

public class FactoryCreateRule extends Rule {

    // ----------------------------------------------------------- Fields
    
    /** Should exceptions thrown by the factory be ignored? */
    private boolean ignoreCreateExceptions;
    /** Stock to manage */
    private ArrayStack<Boolean> exceptionIgnoredStack;

    // ----------------------------------------------------------- Constructors


    /**
     * Construct a factory create rule that will use the specified
     * class name to create an {@link ObjectCreationFactory} which will
     * then be used to create an object and push it on the stack.
     *
     * @param digester The associated Digester
     * @param className Java class name of the object creation factory class
     *
     * @deprecated The digester instance is now set in the {@link Digester#addRule} method. 
     * Use {@link #FactoryCreateRule(String className)} instead.
     */
    public FactoryCreateRule(Digester digester, String className) {

        this(className);

    }


    /**
     * Construct a factory create rule that will use the specified
     * class to create an {@link ObjectCreationFactory} which will
     * then be used to create an object and push it on the stack.
     *
     * @param digester The associated Digester
     * @param clazz Java class name of the object creation factory class
     *
     * @deprecated The digester instance is now set in the {@link Digester#addRule} method. 
     * Use {@link #FactoryCreateRule(Class clazz)} instead.
     */
    public FactoryCreateRule(Digester digester, Class<?> clazz) {

        this(clazz);

    }


    /**
     * Construct a factory create rule that will use the specified
     * class name (possibly overridden by the specified attribute if present)
     * to create an {@link ObjectCreationFactory}, which will then be used
     * to instantiate an object instance and push it onto the stack.
     *
     * @param digester The associated Digester
     * @param className Default Java class name of the factory class
     * @param attributeName Attribute name which, if present, contains an
     *  override of the class name of the object creation factory to create.
     *
     * @deprecated The digester instance is now set in the {@link Digester#addRule} method. 
     * Use {@link #FactoryCreateRule(String className, String attributeName)} instead.
     */
    public FactoryCreateRule(Digester digester,
                             String className, String attributeName) {

        this(className, attributeName);

    }


    /**
     * Construct a factory create rule that will use the specified
     * class (possibly overridden by the specified attribute if present)
     * to create an {@link ObjectCreationFactory}, which will then be used
     * to instantiate an object instance and push it onto the stack.
     *
     * @param digester The associated Digester
     * @param clazz Default Java class name of the factory class
     * @param attributeName Attribute name which, if present, contains an
     *  override of the class name of the object creation factory to create.
     *
     * @deprecated The digester instance is now set in the {@link Digester#addRule} method. 
     * Use {@link #FactoryCreateRule(Class clazz, String attributeName)} instead.
     */
    public FactoryCreateRule(Digester digester,
                             Class<?> clazz, String attributeName) {

        this(clazz, attributeName);

    }


    /**
     * Construct a factory create rule using the given, already instantiated,
     * {@link ObjectCreationFactory}.
     *
     * @param digester The associated Digester
     * @param creationFactory called on to create the object.
     *
     * @deprecated The digester instance is now set in the {@link Digester#addRule} method. 
     * Use {@link #FactoryCreateRule(ObjectCreationFactory creationFactory)} instead.
     */
    public FactoryCreateRule(Digester digester,
                             ObjectCreationFactory creationFactory) {

        this(creationFactory);

    }    

    /**
     * <p>Construct a factory create rule that will use the specified
     * class name to create an {@link ObjectCreationFactory} which will
     * then be used to create an object and push it on the stack.</p>
     *
     * <p>Exceptions thrown during the object creation process will be propagated.</p>
     *
     * @param className Java class name of the object creation factory class
     */
    public FactoryCreateRule(String className) {

        this(className, false);

    }


    /**
     * <p>Construct a factory create rule that will use the specified
     * class to create an {@link ObjectCreationFactory} which will
     * then be used to create an object and push it on the stack.</p>
     *
     * <p>Exceptions thrown during the object creation process will be propagated.</p>
     *
     * @param clazz Java class name of the object creation factory class
     */
    public FactoryCreateRule(Class<?> clazz) {

        this(clazz, false);

    }


    /**
     * <p>Construct a factory create rule that will use the specified
     * class name (possibly overridden by the specified attribute if present)
     * to create an {@link ObjectCreationFactory}, which will then be used
     * to instantiate an object instance and push it onto the stack.</p>
     *
     * <p>Exceptions thrown during the object creation process will be propagated.</p>
     *
     * @param className Default Java class name of the factory class
     * @param attributeName Attribute name which, if present, contains an
     *  override of the class name of the object creation factory to create.
     */
    public FactoryCreateRule(String className, String attributeName) {

        this(className, attributeName, false);

    }


    /**
     * <p>Construct a factory create rule that will use the specified
     * class (possibly overridden by the specified attribute if present)
     * to create an {@link ObjectCreationFactory}, which will then be used
     * to instantiate an object instance and push it onto the stack.</p>
     *
     * <p>Exceptions thrown during the object creation process will be propagated.</p>
     *
     * @param clazz Default Java class name of the factory class
     * @param attributeName Attribute name which, if present, contains an
     *  override of the class name of the object creation factory to create.
     */
    public FactoryCreateRule(Class<?> clazz, String attributeName) {

        this(clazz, attributeName, false);

    }


    /**
     * <p>Construct a factory create rule using the given, already instantiated,
     * {@link ObjectCreationFactory}.</p>
     *
     * <p>Exceptions thrown during the object creation process will be propagated.</p>
     *
     * @param creationFactory called on to create the object.
     */
    public FactoryCreateRule(ObjectCreationFactory creationFactory) {

        this(creationFactory, false);

    }
    
    /**
     * Construct a factory create rule that will use the specified
     * class name to create an {@link ObjectCreationFactory} which will
     * then be used to create an object and push it on the stack.
     *
     * @param className Java class name of the object creation factory class
     * @param ignoreCreateExceptions if true, exceptions thrown by the object
     *  creation factory
     * will be ignored.
     */
    public FactoryCreateRule(String className, boolean ignoreCreateExceptions) {

        this(className, null, ignoreCreateExceptions);

    }


    /**
     * Construct a factory create rule that will use the specified
     * class to create an {@link ObjectCreationFactory} which will
     * then be used to create an object and push it on the stack.
     *
     * @param clazz Java class name of the object creation factory class
     * @param ignoreCreateExceptions if true, exceptions thrown by the
     *  object creation factory
     * will be ignored.
     */
    public FactoryCreateRule(Class<?> clazz, boolean ignoreCreateExceptions) {

        this(clazz, null, ignoreCreateExceptions);

    }


    /**
     * Construct a factory create rule that will use the specified
     * class name (possibly overridden by the specified attribute if present)
     * to create an {@link ObjectCreationFactory}, which will then be used
     * to instantiate an object instance and push it onto the stack.
     *
     * @param className Default Java class name of the factory class
     * @param attributeName Attribute name which, if present, contains an
     *  override of the class name of the object creation factory to create.
     * @param ignoreCreateExceptions if true, exceptions thrown by the object
     *  creation factory will be ignored.
     */
    public FactoryCreateRule(
                                String className, 
                                String attributeName,
                                boolean ignoreCreateExceptions) {

        this.className = className;
        this.attributeName = attributeName;
        this.ignoreCreateExceptions = ignoreCreateExceptions;

    }


    /**
     * Construct a factory create rule that will use the specified
     * class (possibly overridden by the specified attribute if present)
     * to create an {@link ObjectCreationFactory}, which will then be used
     * to instantiate an object instance and push it onto the stack.
     *
     * @param clazz Default Java class name of the factory class
     * @param attributeName Attribute name which, if present, contains an
     *  override of the class name of the object creation factory to create.
     * @param ignoreCreateExceptions if true, exceptions thrown by the object
     *  creation factory will be ignored.
     */
    public FactoryCreateRule(
                                Class<?> clazz, 
                                String attributeName,
                                boolean ignoreCreateExceptions) {

        this(clazz.getName(), attributeName, ignoreCreateExceptions);

    }


    /**
     * Construct a factory create rule using the given, already instantiated,
     * {@link ObjectCreationFactory}.
     *
     * @param creationFactory called on to create the object.
     * @param ignoreCreateExceptions if true, exceptions thrown by the object
     *  creation factory will be ignored.
     */
    public FactoryCreateRule(
                            ObjectCreationFactory creationFactory, 
                            boolean ignoreCreateExceptions) {

        this.creationFactory = creationFactory;
        this.ignoreCreateExceptions = ignoreCreateExceptions;
    }

    // ----------------------------------------------------- Instance Variables


    /**
     * The attribute containing an override class name if it is present.
     */
    protected String attributeName = null;


    /**
     * The Java class name of the ObjectCreationFactory to be created.
     * This class must have a no-arguments constructor.
     */
    protected String className = null;


    /**
     * The object creation factory we will use to instantiate objects
     * as required based on the attributes specified in the matched XML
     * element.
     */
    protected ObjectCreationFactory creationFactory = null;


    // --------------------------------------------------------- Public Methods


    /**
     * Process the beginning of this element.
     *
     * @param attributes The attribute list of this element
     */
    @Override
    public void begin(String namespace, String name, Attributes attributes) throws Exception {
        
        if (ignoreCreateExceptions) {
        
            if (exceptionIgnoredStack == null) {
                exceptionIgnoredStack = new ArrayStack<Boolean>();
            }
            
            try {
                Object instance = getFactory(attributes).createObject(attributes);
                
                if (digester.log.isLoggable(Level.FINE)) {
                    digester.log.log(Level.FINE, "[FactoryCreateRule]'{'{0}'}' New {1}", new Object[]{digester.match, instance.getClass().getName()});
                }
                digester.push(instance);
                exceptionIgnoredStack.push(Boolean.FALSE);
                
            } catch (Exception e) {
                // log message and error
                if (digester.log.isLoggable(Level.INFO)) {
                    digester.log.log(Level.INFO, "[FactoryCreateRule] Create exception ignored: {0}",
                            (e.getMessage() == null) ? e.getClass().getName() : e.getMessage());
                    if (digester.log.isLoggable(Level.FINE)) {
                        digester.log.log(Level.FINE, "[FactoryCreateRule] Ignored exception:{0}", e.getMessage());
                    }
                }
                exceptionIgnoredStack.push(Boolean.TRUE);
                
            }
            
        } else {
            Object instance = getFactory(attributes).createObject(attributes);
            
            if (digester.log.isLoggable(Level.FINE)) {
                digester.log.log(Level.FINE, "[FactoryCreateRule]'{'{0}'}' New {1}", new Object[]{digester.match, instance.getClass().getName()});
            }
            digester.push(instance);
        }
    }


    /**
     * Process the end of this element.
     */
    @Override
    public void end(String namespace, String name) throws Exception {
        
        // check if object was created 
        // this only happens if an exception was thrown and we're ignoring them
        if (	
                ignoreCreateExceptions &&
                exceptionIgnoredStack != null &&
                !(exceptionIgnoredStack.empty())) {
                
            if (exceptionIgnoredStack.pop()) {
                // creation exception was ignored
                // nothing was put onto the stack
                if (digester.log.isLoggable(Level.FINEST)) {
                    digester.log.log(Level.FINEST, "[FactoryCreateRule] No creation so no push so no pop");
                }
                return;
            }
        } 

        Object top = digester.pop();
        if (digester.log.isLoggable(Level.FINE)) {
            digester.log.log(Level.FINE, "[FactoryCreateRule]'{'{0}'}' Pop {1}", new Object[]{digester.match, top.getClass().getName()});
        }

    }


    /**
     * Clean up after parsing is complete.
     */
    @Override
    public void finish() throws Exception {

        if (attributeName != null) {
            creationFactory = null;
        }

    }


    /**
     * Render a printable version of this Rule.
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder("FactoryCreateRule[");
        sb.append("className=");
        sb.append(className);
        sb.append(", attributeName=");
        sb.append(attributeName);
        if (creationFactory != null) {
            sb.append(", creationFactory=");
            sb.append(creationFactory);
        }
        sb.append("]");
        return (sb.toString());

    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Return an instance of our associated object creation factory,
     * creating one if necessary.
     *
     * @param attributes Attributes passed to our factory creation element
     *
     * @exception Exception if any error occurs
     */
    protected ObjectCreationFactory getFactory(Attributes attributes)
            throws Exception {

        if (creationFactory == null) {
            String realClassName = className;
            if (attributeName != null) {
                String value = attributes.getValue(attributeName);
                if (value != null) {
                    realClassName = value;
                }
            }
            if (digester.log.isLoggable(Level.FINE)) {
                digester.log.log(Level.FINE, "[FactoryCreateRule]'{'{0}'}' New factory {1}", new Object[]{digester.match, realClassName});
            }
            Class<?> clazz = digester.getClassLoader().loadClass(realClassName);
            creationFactory = (ObjectCreationFactory)
                    clazz.newInstance();
            creationFactory.setDigester(digester);
        }
        return (creationFactory);

    }    
}
