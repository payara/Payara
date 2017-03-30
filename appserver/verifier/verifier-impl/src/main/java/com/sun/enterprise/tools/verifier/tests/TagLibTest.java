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
 */

package com.sun.enterprise.tools.verifier.tests;

import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Method;

import com.sun.enterprise.tools.verifier.tests.web.WebTest;

/**
 * @author Sudipto Ghosh
 */
public abstract class TagLibTest extends WebTest {

    /**
     * This method parses the function-signature argument and returns the list of
     * parameters as String array.
     *
     * signature param to this method contains the value of this element in tld.
     * <function-signature>java.lana.String nickName( java.lang.String, int )
     * </function-signature>
     *
     * @param signature
     * @return The String array containing the parameter class type.
     */
    public String[] getParameters(String signature) {
        StringBuilder sb = new StringBuilder();
        String[] tokens = (signature.split("\\s"));
        for (int i = 1; i < tokens.length; i++)
            sb.append(tokens[i]);
        String fullParamString = sb.toString().substring(sb.indexOf("(") + 1, sb.indexOf(")"));
        String[] params = getParams(fullParamString);
        return params;
    }

    /**
     *
     * @param fullParamString This is the parsed string from which the parameters
     * will be separated and added as a list of Strings
     * eg: for <function-signature>java.lana.String nickName( java.lang.String, int )
     * </function-signature>. The parsed String will look like this
     * java.lang.String,int
     * @return String array containing all the parameter Types
     */

    public String[] getParams(String fullParamString) {
        List<String> list = new ArrayList<String>();
        if (fullParamString.contains(",")) {
            String rest = fullParamString.substring(fullParamString.indexOf(",") + 1, fullParamString.length());
            fullParamString = fullParamString.substring(0, fullParamString.indexOf(","));
            list.add(fullParamString);
        } else list.add(fullParamString);

        return list.toArray(new String[0]);
    }


    /**
     * This method returns the first token of this string, which must be the return
     * type of the method.
     * @param signature string from the function-signature element of tld
     * @return return type of the method.
     */
    public String getRetType(String signature) {
        String[] tokens = (signature.split("\\s"));
        return tokens[0];
    }

    /**
     * This method will return the method name from the signature String passed
     * as argument.
     * eg: for <function-signature>java.lana.String nickName( java.lang.String, int )
     * </function-signature>
     * @param signature
     * @return method name
     */
    public String getName(String signature) {
        StringBuilder sb = new StringBuilder();
        String[] tokens = (signature.split("\\s"));
        for (int i = 1; i < tokens.length; i++)
            sb.append(tokens[i]);
        String name = sb.toString();
        name = name.substring(0, name.indexOf("("));
        return name;
    }


    /**
     * Checks if the parameter array matches with this method's parameters
     * @param m method object
     * @param param parameter class array
     * @return true if parameters match, false otherwise
     */
    public boolean parametersMatch(Method m, Class[] param) {
        boolean match = false;
        Class[] types = m.getParameterTypes();
        if (types.length!=param.length)
            return false;
        for (int i=0; i<types.length; i++) {
            match = types[i].equals(param[i]);
        }
        return match;
    }

    /**
     * Checks if the return type specified in this method m and the return type
     * required matches
     * @param m
     * @param retType
     * @return true if retType and m.getReturnType matches, false otherwise
     */
    public boolean returnTypeMatch(Method m, String retType) {
        Class ret = m.getReturnType();
        Class retTypeClass  = checkIfPrimitive(retType);
        if (retTypeClass == null);
        try {
            retTypeClass = Class.forName(retType);
        } catch (ClassNotFoundException e) {
            //do nothing. If return type is incorrect, it will be caught by
            // another test in verifier.
        }
        if(retTypeClass != null)            //this may happen if retType is
            return retTypeClass.equals(ret);//non-primitive and invalid
        else return false;
    }

    /**
     * given a parameter array contained in a method, this method returns the
     * Class array representation of the parameters
     * @param par
     * @param cl
     * @return class array representation of the parameters
     */
    public Class[] getParamTypeClass(String[] par, ClassLoader cl) {
        List<Class> list = new ArrayList<Class>();
        for(String s : par) {
            Class c = checkIfPrimitive(s);
            if (c != null)
                list.add(c);
            else {
                try {
                    c = Class.forName(s, false, cl);
                    list.add(c);
                } catch (ClassNotFoundException e) {
                  //do nothing. Other test will report the problem if parameter
                  // is not specified correctly
                }
            }
        }
        return list.toArray(new Class[0]);
    }
}
