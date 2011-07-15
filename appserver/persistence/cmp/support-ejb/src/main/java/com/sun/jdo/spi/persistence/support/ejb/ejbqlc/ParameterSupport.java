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

/*
 * ParameterSupport.java
 *
 * Created on December 07, 2001
 */

package com.sun.jdo.spi.persistence.support.ejb.ejbqlc;

import java.lang.reflect.Method;
import java.util.ResourceBundle;

import org.glassfish.persistence.common.I18NHelper;

/** 
 * Helper class to handle EJBQL query parameters. 
 *
 * @author  Michael Bouschen
 * @author  Shing Wai Chan
 */
public class ParameterSupport
{
    /** The types of the parameters of the finder/selector method. */
    private Class[] parameterTypes;
    
    /**
     * The EJB names corresponding to types of parameters of the
     * finder/selector method.
     */
    private String[] parameterEjbNames;
    
    /** I18N support. */
    protected final static ResourceBundle msgs = I18NHelper.loadBundle(
        ParameterSupport.class);

    /** 
     * Constructor. 
     * @param method the Method instance of the finder/selector method.
     */
    public ParameterSupport(Method method)
    {
        this.parameterTypes = 
            (method == null) ? new Class[0] : method.getParameterTypes();
        this.parameterEjbNames = new String[this.parameterTypes.length];
    }

    /** 
     * Returns type of the EJBQL parameter by input parameter declaration 
     * string. The specified string denotes a parameter application in EJBQL. 
     * It has the form "?<number>" where <number> is the parameter number 
     * starting with 1.
     * @return class instance representing the parameter type.
     */
    public Class getParameterType(String ejbqlParamDecl)
    {
        return getParameterType(getParamNumber(ejbqlParamDecl));
    }

    /** 
     * Returns the type of the EJBQL parameter by number. 
     * Note, the numbering of EJBQL parameters starts with 1, 
     * so the method expects 1 as the number of the first parameter.
     * @return class instance representing the parameter type.
     */
    public Class getParameterType(int paramNumber)
    {
        // InputParams are numbered starting at 1, so adjust for
        // array indexing.
        return parameterTypes[paramNumber - 1];
    }

    /** 
     * Get EJB name corresponding to the EJBQL parameter by input
     * parameter declaration string.
     * @param ejbqlParamDecl denotes a parameter application in EJBQL. 
     * It has the form "?<number>" where <number> is the parameter number 
     * starting with 1.
     * @return class instance representing the parameter type.
     */
    public String getParameterEjbName(String ejbqlParamDecl)
    {
        return getParameterEjbName(getParamNumber(ejbqlParamDecl));
    }

    /** 
     * Get EJB name corresponding to the EJBQL parameter number.
     * @param paramNumber numbering of parameters starting with 1
     * @return class instance representing the parameter type.
     */
    public String getParameterEjbName(int paramNumber)
    {
        return parameterEjbNames[paramNumber - 1];
    }

    /** 
     * Set EJB name corresponding to the EJBQL parameter by input
     * parameter declaration string.
     * @param ejbqlParamDecl denotes a parameter application in EJBQL. 
     * It has the form "?<number>" where <number> is the parameter number 
     * starting with 1.
     * @param ejbName
     */
    public void setParameterEjbName(String ejbqlParamDecl, String ejbName)
    {
        parameterEjbNames[getParamNumber(ejbqlParamDecl) - 1] = ejbName;
    }

    /** 
     * Get all EJB names corresponding to the EJBQL parameters.
     * @return class instance representing the parameter type.
     */
    public String[] getParameterEjbNames()
    {
        return parameterEjbNames;
    }

    /** 
     * Returns the name of the corresponding JDO parameter.
     * The specified string denotes a parameter application in EJBQL. 
     * It has the form "?<number>" where <number> is the parameter number 
     * starting with 1.
     * @return name of JDOQL parameter 
     */
    public String getParameterName(String ejbqlParamDecl)
    {
        return getParameterName(getParamNumber(ejbqlParamDecl));
    }

    /** 
     * Returns the name of the corresponding JDO parameter by parameter number.
     * @return name of JDOQL parameter 
     */
    public String getParameterName(int paramNumber)
    {
        return "_jdoParam" + String.valueOf(paramNumber);
    }

    /** 
     * Returns the number of parameters.
     * @return parameter count.
     */
    public int getParameterCount()
    {
        return parameterTypes.length;
    }

    // Internal methods

    /** 
     * Internal method to extract the number from a parameter application 
     * in EJBQL. 
     */
    private int getParamNumber(String ejbqlParamDecl)
    {
        int paramNum = 0;
        try {
            paramNum = Integer.parseInt(ejbqlParamDecl.substring(1));
        } catch(Exception ex) {
            ErrorMsg.error(I18NHelper.getMessage(
                msgs, "EXC_InvalidParameterIndex", //NOI18N
                ejbqlParamDecl, String.valueOf(parameterTypes.length)));
        }
        if (paramNum < 1 || paramNum > parameterTypes.length) {
            ErrorMsg.error(I18NHelper.getMessage(
                msgs, "EXC_InvalidParameterIndex", //NOI18N
                ejbqlParamDecl, String.valueOf(parameterTypes.length)));
        }
        return paramNum;
    }
}
