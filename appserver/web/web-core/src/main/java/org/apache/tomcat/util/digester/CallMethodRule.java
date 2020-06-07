/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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


import org.glassfish.web.util.IntrospectionUtils;
import org.xml.sax.Attributes;

import java.util.logging.Level;


/**
 * <p>Rule implementation that calls a method on an object on the stack
 * (normally the top/parent object), passing arguments collected from 
 * subsequent <code>CallParamRule</code> rules or from the body of this
 * element. </p>
 *
 * <p>By using {@link #CallMethodRule(String methodName)} 
 * a method call can be made to a method which accepts no
 * arguments.</p>
 *
 * <p>Incompatible method parameter types are converted 
 * using <code>org.apache.commons.beanutils.ConvertUtils</code>.
 * </p>
 *
 * <p>This rule now uses
 * <a href="http://jakarta.apache.org/commons/beanutils/apidocs/org/apache/commons/beanutils/MethodUtils.html">
 * org.apache.commons.beanutils.MethodUtils#invokeMethod
 * </a> by default.
 * This increases the kinds of methods successfully and allows primitives
 * to be matched by passing in wrapper classes.
 * There are rare cases when org.apache.commons.beanutils.MethodUtils#invokeExactMethod 
 * (the old default) is required.
 * This method is much stricter in its reflection.
 * Setting the <code>UseExactMatch</code> to true reverts to the use of this 
 * method.</p>
 *
 * <p>Note that the target method is invoked when the  <i>end</i> of
 * the tag the CallMethodRule fired on is encountered, <i>not</i> when the
 * last parameter becomes available. This implies that rules which fire on
 * tags nested within the one associated with the CallMethodRule will 
 * fire before the CallMethodRule invokes the target method. This behaviour is
 * not configurable. </p>
 *
 * <p>Note also that if a CallMethodRule is expecting exactly one parameter
 * and that parameter is not available (eg CallParamRule is used with an
 * attribute name but the attribute does not exist) then the method will
 * not be invoked. If a CallMethodRule is expecting more than one parameter,
 * then it is always invoked, regardless of whether the parameters were
 * available or not (missing parameters are passed as null values).</p>
 */

public class CallMethodRule extends Rule {

    // ----------------------------------------------------------- Constructors


    /**
     * Construct a "call method" rule with the specified method name.  The
     * parameter types (if any) default to java.lang.String.
     *
     * @param digester The associated Digester
     * @param methodName Method name of the parent method to call
     * @param paramCount The number of parameters to collect, or
     *  zero for a single argument from the body of this element.
     *
     *
     * @deprecated The digester instance is now set in the {@link Digester#addRule} method. 
     * Use {@link #CallMethodRule(String methodName,int paramCount)} instead.
     */
    public CallMethodRule(Digester digester, String methodName,
                          int paramCount) {

        this(methodName, paramCount);

    }


    /**
     * Construct a "call method" rule with the specified method name.
     *
     * @param digester The associated Digester
     * @param methodName Method name of the parent method to call
     * @param paramCount The number of parameters to collect, or
     *  zero for a single argument from the body of ths element
     * @param paramTypes The Java class names of the arguments
     *  (if you wish to use a primitive type, specify the corresonding
     *  Java wrapper class instead, such as <code>java.lang.Boolean</code>
     *  for a <code>boolean</code> parameter)
     *
     * @deprecated The digester instance is now set in the {@link Digester#addRule} method. 
     * Use {@link #CallMethodRule(String methodName,int paramCount, String [] paramTypes)} instead.
     */
    public CallMethodRule(Digester digester, String methodName,
                          int paramCount, String paramTypes[]) {

        this(methodName, paramCount, paramTypes);

    }


    /**
     * Construct a "call method" rule with the specified method name.
     *
     * @param digester The associated Digester
     * @param methodName Method name of the parent method to call
     * @param paramCount The number of parameters to collect, or
     *  zero for a single argument from the body of ths element
     * @param paramTypes The Java classes that represent the
     *  parameter types of the method arguments
     *  (if you wish to use a primitive type, specify the corresonding
     *  Java wrapper class instead, such as <code>java.lang.Boolean.TYPE</code>
     *  for a <code>boolean</code> parameter)
     *
     * @deprecated The digester instance is now set in the {@link Digester#addRule} method. 
     * Use {@link #CallMethodRule(String methodName,int paramCount, Class [] paramTypes)} instead.
     */
    public CallMethodRule(Digester digester, String methodName,
                          int paramCount, Class paramTypes[]) {

        this(methodName, paramCount, paramTypes);
    }


    /**
     * Construct a "call method" rule with the specified method name.  The
     * parameter types (if any) default to java.lang.String.
     *
     * @param methodName Method name of the parent method to call
     * @param paramCount The number of parameters to collect, or
     *  zero for a single argument from the body of this element.
     */
    public CallMethodRule(String methodName,
                          int paramCount) {
        this(0, methodName, paramCount);
    }

    /**
     * Construct a "call method" rule with the specified method name.  The
     * parameter types (if any) default to java.lang.String.
     *
     * @param targetOffset location of the target object. Positive numbers are
     * relative to the top of the digester object stack. Negative numbers 
     * are relative to the bottom of the stack. Zero implies the top
     * object on the stack.
     * @param methodName Method name of the parent method to call
     * @param paramCount The number of parameters to collect, or
     *  zero for a single argument from the body of this element.
     */
    public CallMethodRule(int targetOffset,
                          String methodName,
                          int paramCount) {

        this.targetOffset = targetOffset;
        this.methodName = methodName;
        this.paramCount = paramCount;        
        if (paramCount == 0) {
            this.paramTypes = new Class[] { String.class };
        } else {
            this.paramTypes = new Class[paramCount];
            for (int i = 0; i < this.paramTypes.length; i++) {
                this.paramTypes[i] = String.class;
            }
        }

    }

    /**
     * Construct a "call method" rule with the specified method name.  
     * The method should accept no parameters.
     *
     * @param methodName Method name of the parent method to call
     */
    public CallMethodRule(String methodName) {
    
        this(0, methodName, 0, (Class[]) null);
    
    }
    

    /**
     * Construct a "call method" rule with the specified method name.  
     * The method should accept no parameters.
     *
     * @param targetOffset location of the target object. Positive numbers are
     * relative to the top of the digester object stack. Negative numbers 
     * are relative to the bottom of the stack. Zero implies the top
     * object on the stack.
     * @param methodName Method name of the parent method to call
     */
    public CallMethodRule(int targetOffset, String methodName) {
    
        this(targetOffset, methodName, 0, (Class[]) null);
    
    }
    

    /**
     * Construct a "call method" rule with the specified method name and
     * parameter types. If <code>paramCount</code> is set to zero the rule
     * will use the body of this element as the single argument of the
     * method, unless <code>paramTypes</code> is null or empty, in this
     * case the rule will call the specified method with no arguments.
     *
     * @param methodName Method name of the parent method to call
     * @param paramCount The number of parameters to collect, or
     *  zero for a single argument from the body of ths element
     * @param paramTypes The Java class names of the arguments
     *  (if you wish to use a primitive type, specify the corresonding
     *  Java wrapper class instead, such as <code>java.lang.Boolean</code>
     *  for a <code>boolean</code> parameter)
     */
    public CallMethodRule(
                            String methodName,
                            int paramCount, 
                            String paramTypes[]) {
        this(0, methodName, paramCount, paramTypes);
    }

    /**
     * Construct a "call method" rule with the specified method name and
     * parameter types. If <code>paramCount</code> is set to zero the rule
     * will use the body of this element as the single argument of the
     * method, unless <code>paramTypes</code> is null or empty, in this
     * case the rule will call the specified method with no arguments.
     *
     * @param targetOffset location of the target object. Positive numbers are
     * relative to the top of the digester object stack. Negative numbers 
     * are relative to the bottom of the stack. Zero implies the top
     * object on the stack.
     * @param methodName Method name of the parent method to call
     * @param paramCount The number of parameters to collect, or
     *  zero for a single argument from the body of ths element
     * @param paramTypes The Java class names of the arguments
     *  (if you wish to use a primitive type, specify the corresonding
     *  Java wrapper class instead, such as <code>java.lang.Boolean</code>
     *  for a <code>boolean</code> parameter)
     */
    public CallMethodRule(  int targetOffset,
                            String methodName,
                            int paramCount, 
                            String paramTypes[]) {

        this.targetOffset = targetOffset;
        this.methodName = methodName;
        this.paramCount = paramCount;
        if (paramTypes == null) {
            this.paramTypes = new Class[paramCount];
            for (int i = 0; i < this.paramTypes.length; i++) {
                this.paramTypes[i] = "abc".getClass();
            }
        } else {
            // copy the parameter class names into an array
            // the classes will be loaded when the digester is set 
            this.paramClassNames = new String[paramTypes.length];
            this.paramClassNames = paramTypes.clone();
        }

    }


    /**
     * Construct a "call method" rule with the specified method name and
     * parameter types. If <code>paramCount</code> is set to zero the rule
     * will use the body of this element as the single argument of the
     * method, unless <code>paramTypes</code> is null or empty, in this
     * case the rule will call the specified method with no arguments.
     *
     * @param methodName Method name of the parent method to call
     * @param paramCount The number of parameters to collect, or
     *  zero for a single argument from the body of ths element
     * @param paramTypes The Java classes that represent the
     *  parameter types of the method arguments
     *  (if you wish to use a primitive type, specify the corresonding
     *  Java wrapper class instead, such as <code>java.lang.Boolean.TYPE</code>
     *  for a <code>boolean</code> parameter)
     */
    public CallMethodRule(
                            String methodName,
                            int paramCount, 
                            Class<?> paramTypes[]) {
        this(0, methodName, paramCount, paramTypes);
    }

    /**
     * Construct a "call method" rule with the specified method name and
     * parameter types. If <code>paramCount</code> is set to zero the rule
     * will use the body of this element as the single argument of the
     * method, unless <code>paramTypes</code> is null or empty, in this
     * case the rule will call the specified method with no arguments.
     *
     * @param targetOffset location of the target object. Positive numbers are
     * relative to the top of the digester object stack. Negative numbers 
     * are relative to the bottom of the stack. Zero implies the top
     * object on the stack.
     * @param methodName Method name of the parent method to call
     * @param paramCount The number of parameters to collect, or
     *  zero for a single argument from the body of ths element
     * @param paramTypes The Java classes that represent the
     *  parameter types of the method arguments
     *  (if you wish to use a primitive type, specify the corresonding
     *  Java wrapper class instead, such as <code>java.lang.Boolean.TYPE</code>
     *  for a <code>boolean</code> parameter)
     */
    public CallMethodRule(  int targetOffset,
                            String methodName,
                            int paramCount, 
                            Class<?> paramTypes[]) {

        this.targetOffset = targetOffset;
        this.methodName = methodName;
        this.paramCount = paramCount;
        if (paramTypes == null) {
            this.paramTypes = new Class[paramCount];
            for (int i = 0; i < this.paramTypes.length; i++) {
                this.paramTypes[i] = "abc".getClass();
            }
        } else {
            this.paramTypes = new Class[paramTypes.length];
            System.arraycopy(paramTypes, 0, this.paramTypes, 0, this.paramTypes.length);
        }

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The body text collected from this element.
     */
    protected String bodyText = null;


    /** 
     * location of the target object for the call, relative to the
     * top of the digester object stack. The default value of zero
     * means the target object is the one on top of the stack.
     */
    protected int targetOffset = 0;

    /**
     * The method name to call on the parent object.
     */
    protected String methodName = null;


    /**
     * The number of parameters to collect from <code>MethodParam</code> rules.
     * If this value is zero, a single parameter will be collected from the
     * body of this element.
     */
    protected int paramCount = 0;


    /**
     * The parameter types of the parameters to be collected.
     */
    protected Class<?> paramTypes[] = null;

    /**
     * The names of the classes of the parameters to be collected.
     * This attribute allows creation of the classes to be postponed until the digester is set.
     */
    protected String paramClassNames[] = null;
    
    /**
     * Should <code>MethodUtils.invokeExactMethod</code> be used for reflection.
     */
    protected boolean useExactMatch = false;
    
    // --------------------------------------------------------- Public Methods
    
    /**
     * Should <code>MethodUtils.invokeExactMethod</code>
     * be used for the reflection.
     */
    public boolean getUseExactMatch() {
        return useExactMatch;
    }
    
    /**
     * Set whether <code>MethodUtils.invokeExactMethod</code>
     * should be used for the reflection.
     */    
    public void setUseExactMatch(boolean useExactMatch)
    { 
        this.useExactMatch = useExactMatch;
    }

    /**
     * Set the associated digester.
     * If needed, this class loads the parameter classes from their names.
     */
    @Override
    public void setDigester(Digester digester) {
        // call superclass
        super.setDigester(digester);
        // if necessary, load parameter classes
        if (this.paramClassNames != null) {
            this.paramTypes = new Class[paramClassNames.length];
            for (int i = 0; i < this.paramClassNames.length; i++) {
                try {
                    this.paramTypes[i] =
                            digester.getClassLoader().loadClass(this.paramClassNames[i]);
                } catch (ClassNotFoundException e) {
                    // use the digester log
                    digester.getLogger().log(Level.SEVERE, "(CallMethodRule) Cannot load class " + this.paramClassNames[i], e);
                    this.paramTypes[i] = null; // Will cause NPE later
                }
            }
        }
    }

    /**
     * Process the start of this element.
     *
     * @param attributes The attribute list for this element
     */
    @Override
    public void begin(Attributes attributes) throws Exception {

        // Push an array to capture the parameter values if necessary
        if (paramCount > 0) {
            Object parameters[] = new Object[paramCount];
            for (int i = 0; i < parameters.length; i++) {
                parameters[i] = null;
            }
            digester.pushParams(parameters);
        }

    }


    /**
     * Process the body text of this element.
     *
     * @param bodyText The body text of this element
     */
    @Override
    public void body(String bodyText) throws Exception {
        if (paramCount == 0) {
            this.bodyText = bodyText.trim();
        }
    }


    /**
     * Process the end of this element.
     */
    @Override
    public void end() throws Exception {

        // Retrieve or construct the parameter values array
        Object parameters[] = null;
        if (paramCount > 0) {

            parameters = (Object[]) digester.popParams();
            
            if (digester.log.isLoggable(Level.FINEST)) {
                for (int i=0,size=parameters.length;i<size;i++) {
                    digester.log.log(Level.FINEST, "[CallMethodRule]({0}){1}", new Object[]{i, parameters[i]}) ;
                }
            }
            
            // In the case where the parameter for the method
            // is taken from an attribute, and that attribute
            // isn't actually defined in the source XML file,
            // skip the method call
            if (paramCount == 1 && parameters[0] == null) {
                return;
            }

        } else if (paramTypes != null && paramTypes.length != 0) {

            // In the case where the parameter for the method
            // is taken from the body text, but there is no
            // body text included in the source XML file,
            // skip the method call
            if (bodyText == null) {
                return;
            }

            parameters = new Object[1];
            parameters[0] = bodyText;
        }

        // Construct the parameter values array we will need
        // We only do the conversion if the param value is a String and
        // the specified paramType is not String.
        Object paramValues[] = null;
        if (paramTypes != null) {
            paramValues = new Object[paramTypes.length];
            for (int i = 0; i < paramTypes.length; i++) {
                // convert nulls and convert stringy parameters
                // for non-stringy param types
                if (parameters[i] == null ||
                        (parameters[i] instanceof String && !String.class.isAssignableFrom(paramTypes[i]))) {
                    paramValues[i] =
                            IntrospectionUtils.convert((String) parameters[i], paramTypes[i]);
                } else {
                    paramValues[i] = parameters[i];
                }
            }
        }

        // Determine the target object for the method call
        Object target;
        if (targetOffset >= 0) {
            target = digester.peek(targetOffset);
        } else {
            target = digester.peek( digester.getCount() + targetOffset );
        }
        
        if (target == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("[CallMethodRule]{");
            sb.append(digester.match);
            sb.append("} Call target is null (");
            sb.append("targetOffset=");
            sb.append(targetOffset);
            sb.append(",stackdepth=");
            sb.append(digester.getCount());
            sb.append(")");
            throw new org.xml.sax.SAXException(sb.toString());
        }
        
        // Invoke the required method on the top object
        if (digester.log.isLoggable(Level.FINE)) {
            StringBuilder sb = new StringBuilder("[CallMethodRule]{");
            sb.append(digester.match);
            sb.append("} Call ");
            sb.append(target.getClass().getName());
            sb.append(".");
            sb.append(methodName);
            sb.append("(");
            for (int i = 0; paramValues!=null && i<paramValues.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                if (paramValues[i] == null) {
                    sb.append("null");
                } else {
                    sb.append(paramValues[i].toString());
                }
                sb.append("/");
                if (paramTypes[i] == null) {
                    sb.append("null");
                } else {
                    sb.append(paramTypes[i].getName());
                }
            }
            sb.append(")");
            digester.log.fine(sb.toString());
        }
        Object result = IntrospectionUtils.callMethodN(target, methodName,
                paramValues, paramTypes);   
        processMethodCallResult(result);
    }


    /**
     * Clean up after parsing is complete.
     */
    @Override
    public void finish() throws Exception {
        bodyText = null;
    }

    /**
     * Subclasses may override this method to perform additional processing of the 
     * invoked method's result.
     *
     * @param result the Object returned by the method invoked, possibly null
     */
    protected void processMethodCallResult(Object result) {
        // do nothing
    }

    /**
     * Render a printable version of this Rule.
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder("CallMethodRule[");
        sb.append("methodName=");
        sb.append(methodName);
        sb.append(", paramCount=");
        sb.append(paramCount);
        sb.append(", paramTypes={");
        if (paramTypes != null) {
            for (int i = 0; i < paramTypes.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(paramTypes[i].getName());
            }
        }
        sb.append("}");
        sb.append("]");
        return (sb.toString());

    }


}
