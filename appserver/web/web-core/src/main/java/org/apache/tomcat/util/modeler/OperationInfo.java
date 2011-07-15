/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.apache.tomcat.util.modeler;


import javax.management.Descriptor;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;
import java.io.Serializable;
import java.util.Locale;


/**
 * <p>Internal configuration information for an <code>Operation</code>
 * descriptor.</p>
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.6 $ $Date: 2003/07/20 07:35:13 $
 */

public class OperationInfo extends FeatureInfo {
    static final long serialVersionUID = 4418342922072614875L;
    // ----------------------------------------------------------- Constructors


    /**
     * Standard zero-arguments constructor.
     */
    public OperationInfo() {

        super();

    }


    /**
     * Special constructor for setting up getter and setter operations.
     *
     * @param name Name of this operation
     * @param getter Is this a getter (as opposed to a setter)?
     * @param type Data type of the return value (if this is a getter)
     *  or the parameter (if this is a setter)
     * 
     */
    public OperationInfo(String name, boolean getter, String type) {

        super();
        setName(name);
        if (getter) {
            setDescription("Attribute getter method");
            setImpact("INFO");
            setReturnType(type);
            setRole("getter");
        } else {
            setDescription("Attribute setter method");
            setImpact("ACTION");
            setReturnType("void");
            setRole("setter");
            addParameter(new ParameterInfo("value", type,
                                           "New attribute value"));
        }

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The <code>ModelMBeanOperationInfo</code> object that corresponds
     * to this <code>OperationInfo</code> instance.
     */
    transient ModelMBeanOperationInfo info = null;
    protected String impact = "UNKNOWN";
    protected String role = "operation";
    protected String returnType = "void";    // FIXME - Validate
    protected ParameterInfo parameters[] = new ParameterInfo[0];


    // ------------------------------------------------------------- Properties


    /**
     * Override the <code>description</code> property setter.
     *
     * @param description The new description
     */
    public void setDescription(String description) {
        super.setDescription(description);
        this.info = null;
    }


    /**
     * Override the <code>name</code> property setter.
     *
     * @param name The new name
     */
    public void setName(String name) {
        super.setName(name);
        this.info = null;
    }


    /**
     * The "impact" of this operation, which should be a (case-insensitive)
     * string value "ACTION", "ACTION_INFO", "INFO", or "UNKNOWN".
     */
    public String getImpact() {
        return (this.impact);
    }

    public void setImpact(String impact) {
        if (impact == null)
            this.impact = null;
        else
            this.impact = impact.toUpperCase(Locale.ENGLISH);
    }


    /**
     * The role of this operation ("getter", "setter", "operation", or
     * "constructor").
     */
    public String getRole() {
        return (this.role);
    }

    public void setRole(String role) {
        this.role = role;
    }


    /**
     * The fully qualified Java class name of the return type for this
     * operation.
     */
    public String getReturnType() {
        return (this.returnType);
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
    }

    /**
     * The set of parameters for this operation.
     */
    public ParameterInfo[] getSignature() {
        return (this.parameters);
    }

    // --------------------------------------------------------- Public Methods


    /**
     * Add a new parameter to the set of arguments for this operation.
     *
     * @param parameter The new parameter descriptor
     */
    public void addParameter(ParameterInfo parameter) {

        synchronized (parameters) {
            ParameterInfo results[] = new ParameterInfo[parameters.length + 1];
            System.arraycopy(parameters, 0, results, 0, parameters.length);
            results[parameters.length] = parameter;
            parameters = results;
            this.info = null;
        }

    }


    /**
     * Create and return a <code>ModelMBeanOperationInfo</code> object that
     * corresponds to the attribute described by this instance.
     */
    public ModelMBeanOperationInfo createOperationInfo() {

        // Return our cached information (if any)
        if (info != null)
            return (info);

        // Create and return a new information object
        ParameterInfo params[] = getSignature();
        MBeanParameterInfo parameters[] =
            new MBeanParameterInfo[params.length];
        for (int i = 0; i < params.length; i++)
            parameters[i] = params[i].createParameterInfo();
        int impact = ModelMBeanOperationInfo.UNKNOWN;
        if ("ACTION".equals(getImpact()))
            impact = ModelMBeanOperationInfo.ACTION;
        else if ("ACTION_INFO".equals(getImpact()))
            impact = ModelMBeanOperationInfo.ACTION_INFO;
        else if ("INFO".equals(getImpact()))
            impact = ModelMBeanOperationInfo.INFO;

        info = new ModelMBeanOperationInfo
            (getName(), getDescription(), parameters,
             getReturnType(), impact);
        Descriptor descriptor = info.getDescriptor();
        descriptor.removeField("class");
        descriptor.setField("role", getRole());
        addFields(descriptor);
        info.setDescriptor(descriptor);
        return (info);

    }


    /**
     * Return a string representation of this operation descriptor.
     */
    public String toString() {

        StringBuilder sb = new StringBuilder("OperationInfo[");
        sb.append("name=");
        sb.append(name);
        sb.append(", description=");
        sb.append(description);
        sb.append(", returnType=");
        sb.append(returnType);
        sb.append(", parameters=");
        sb.append(parameters.length);
        sb.append("]");
        return (sb.toString());

    }


}
