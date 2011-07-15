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
 * ParameterTable.java
 *
 * Created on April 12, 2000
 */

package com.sun.jdo.spi.persistence.support.sqlstore.query.jqlc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import com.sun.jdo.api.persistence.support.JDOQueryException;
import com.sun.jdo.api.persistence.support.JDOFatalInternalException;
import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.Type;
import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.StringType;
import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.PrimitiveType;
import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.WrapperClassType;
import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.MathType;
import com.sun.jdo.spi.persistence.support.sqlstore.query.util.type.DateType;

import org.glassfish.persistence.common.I18NHelper;
import com.sun.jdo.spi.persistence.support.sqlstore.ValueFetcher;
import com.sun.jdo.spi.persistence.utility.JavaTypeHelper;
import com.sun.jdo.spi.persistence.utility.ParameterInfo;

/**
 * The query parameter table
 *
 * @author  Michael Bouschen
 * @version 0.1
 */
public class ParameterTable
{
    /** Query parameter names */
    List names = null;

    /** Query parameter types */
    List types = null;

    /** Query Parameter values */
    transient List values = null;
    
    /** null key */
    private static final String NULL_ = "null"; //NOI18N

    /** true key */
    private static final String TRUE_ = "true"; //NOI18N

    /** false key */
    private static final String FALSE_ = "false"; //NOI18N

    /** other key */
    private static final String OTHER_ = "other"; //NOI18N

    /** noparams key */
    private static final String NOPARAMS_ = "noparams"; //NOI18N
    
    /** key parameter separator */
    private static final char PARAMKEY_SEPARATOR = '/';

    /**
     * Objects of this class represent the value for an unbound query parameter
     */
    static class Unbound { }

    /**
     * Singleton representing the value for an unbound query parameter
     */
    static final Unbound unbound = new Unbound();

    /** I18N support */
    protected final static ResourceBundle messages =
        I18NHelper.loadBundle(ParameterTable.class);

    /**
     *
     */
    public ParameterTable()
    {}

    /**
     * Copy constructor.
     * @param other the ParameterTable to be copied
     */
    public ParameterTable(ParameterTable other)
    {
        this.names = other.names;
        this.types = other.types;
        this.values = other.values;
    }

    /**
     * Adds a new query parameter with the specified type to the query 
     * parameter table. 
     */
    public void add(String name, Type type)
    {
        names.add(name);
        types.add(type);
    }

    /**
     * Initializes the parameter declarations (names and types list). 
     * Needs to be called prior to any add call.
     */
    public void init()
    {
        this.names = new ArrayList();
        this.types = new ArrayList();
    }

    /**
     * Initializes the parameter values. This methods sets the values for all 
     * declared parameters to unbound.
     */
    public void initValueHandling()
    {
        values = new ArrayList(names.size());
        final int size = names.size();
        for (int i = 0; i < size; i++) {
            values.add(unbound);
        }
    }
    
    /**
     * Check actual query parameters specified as array and return the
     * ValueFetcher for the inputparameters.
     * @param actualParams
     */
    public void setValues(Object[] actualParams)
    {
        if (actualParams != null)
        {
            for (int i = 0; i < actualParams.length; i++)
            {
                Object value = actualParams[i];
                defineValueByIndex(i, value);
            }
        }
    }

    /**
     * Checks whether all parameters have an actual value.
     */
    public void checkUnboundParams()
    {
        final int size = values.size();
        for (int i = 0; i < size; i++)
        {
            if (values.get(i) == unbound)
            {
                throw new JDOQueryException(
                    I18NHelper.getMessage(messages, "jqlc.parametertable.checkunboundparams.unboundparam",  //NOI18N
                                          names.get(i)));
            }
        }
    }

    /**
     * Check actual query parameters specified as map and return the
     * ValueFetcher for the inputparameters.
     * @param actualParams
     */
    public void setValues(Map actualParams)
    {
        if (actualParams != null)
        {
            for (Iterator i = actualParams.entrySet().iterator(); i.hasNext();)
            {
                Map.Entry actualParam = (Map.Entry)i.next();
                String name = (String)actualParam.getKey();
                Object value = actualParam.getValue();
                defineValueByName(name, value);
            }
        }
    }

    /**
     * Returns the value of the parameter with the specified name.
     */
    public Object getValueByName(String name)
    {
        int index = names.indexOf(name);
        if (index == -1)
            throw new JDOFatalInternalException(I18NHelper.getMessage(
                messages,
                "jqlc.parametertable.getvaluebyname.undefined", //NOI18N
                name));

        return getValueByIndex(index);
    }

    /**
     * Returns the value of the parameter with the specified index.
     */
    public Object getValueByIndex(int index)
    {
        if ((index < 0) || (index >= values.size()))
            throw new JDOFatalInternalException(I18NHelper.getMessage(
                messages,
                "jqlc.parametertable.getvaluebyindex.wrongindex", //NOI18N
                String.valueOf(index)));

        return values.get(index);
    }

    /** Returns the list of parameter values. */
    public List getValues()
    {
        return values;
    }

    /**
     * Wraps the actual parameter array into a ValueFetcher instnace.
     * @return Instance of ValueFetcher
     */
    public ValueFetcher getValueFetcher()
    {
        return new QueryValueFetcher(values.toArray(new Object[values.size()]));
    }

    /**
     * Calculates and returns the key for the RetrieveDesc cache based,
     * on the actual parameter values.
     * A <code>null</code> return means, the RetrieveDesc should not be
     * cached. 
     * Note, this method needs to be in sync with method inline.
     */
    public String getKeyForRetrieveDescCache()
    {
        StringBuffer key = new StringBuffer();
        final int size = values.size();
        for (int i = 0; i < size; i++) {
            // Do not cache RetrieveDesc if the parameter type is pc class
            // or java.lang.Object => return null
            if (isInlineType(types.get(i)))
                return null;

            Object item = values.get(i);
            if (item == null) {
                key.append(ParameterTable.NULL_);
            }
            else if (item instanceof Boolean) {
                if (((Boolean)item).booleanValue()) {
                    key.append(ParameterTable.TRUE_);
                } else {
                    key.append(ParameterTable.FALSE_);
                }
            } else {
                key.append(ParameterTable.OTHER_);
            }
            key.append(ParameterTable.PARAMKEY_SEPARATOR);
        }

        // If the key is 0 in length, the Query does not use any parameters.
        // But nevertheless we want cache the RD, thus we return a key for
        // no-parameter-queries
        if (key.length() == 0) {
            key.append(ParameterTable.NOPARAMS_);
        }

        return key.toString();
    }

    /**
     * Returns true if the parameter with the specified index should be inlined
     * by the optimizer.
     * Note, this method needs to be in sync with method
     * getKeyForRetrieveDescCache. 
     * @param paramName the parameter
     * @return true if the specified parameter should be inlined.
     */
    public boolean inline(String paramName)
    {
        int index = names.indexOf(paramName);
        Object value = values.get(index);

        if (isInlineType(types.get(index))) return true;

        if (value == null) return true;

        if (value instanceof Boolean) return true;

        return false;
    }

    /**
     * Returns <code>true</code> if the specified parameter denotes a type
     * whose values should be inlined by the query optimizer if a query 
     * parameter s is declared with such a type.
     */
    private boolean isInlineType(Object type)
    { 
        // Check for types that are supported by JDBC, such that the
        // parameter can be mapped to a JDBC parameter, these are:
        // - String
        // - primitive types (int, float, etc.)
        // - wrapper class types (Integer, Float, etc.)
        // - BigDecimal, BigInteger
        // - Date class types
        // All other types including pc classes, java.lang.Object, etc.
        // should be inlined.
        if ((type instanceof StringType) ||
            (type instanceof PrimitiveType) ||
            (type instanceof WrapperClassType) ||
            (type instanceof MathType) ||
            (type instanceof DateType))
            return false;
        return true;
    }

    /**
     * Returns the parameter index for the specified parameter name.
     * @deprecated
     */
    public Integer getIndexForParamName(String paramName)
    {
        return new Integer(names.indexOf(paramName));
    }

    /**
     * Returns the parameter info for the specified parameter name.
     * @param paramName
     * @return corresponding parameterInfo
     */
    public ParameterInfo getParameterInfoForParamName(String paramName)
    {
        return getParameterInfoForParamName(paramName, null);
    }

    /**
     * Returns the parameter info for the specified parameter name
     * and associated field.
     * If the associated field is not known, then null is used as
     * input parameter.
     * @param paramName
     * @param associatedField
     * @return corresponding parameterInfo
     */
    public ParameterInfo getParameterInfoForParamName(String paramName,
            String associatedField)
    {
        int index = names.indexOf(paramName);
        Type type = (Type)types.get(index);
        return new ParameterInfo(index, type.getEnumType(), associatedField);
    }

    /**
     *
     */
    private void defineValueByName(String name, Object value)
    {
        int index = names.indexOf(name);
        if (index == -1)
            throw new JDOQueryException(
                I18NHelper.getMessage(messages, "jqlc.parametertable.definevaluebyname.undefinedparam", name)); //NOI18N
        defineValueByIndex(index, value);
    }

    /**
     *
     */
    private void defineValueByIndex(int index, Object value)
    {
        // index < 0 => implementation error
        if (index < 0)
            throw new JDOFatalInternalException(I18NHelper.getMessage(
                messages,
                "jqlc.parametertable.definevaluebyindex.wrongindex", //NOI18N
                String.valueOf(index)));          

        // index > type.size => too many actual parameters
        if (index >= types.size())
            throw new JDOQueryException(
                I18NHelper.getMessage(messages, "jqlc.parametertable.definevaluebyindex.wrongnumberofargs")); //NOI18N

        // check type compatibility of actual and formal parameter
        Class formalType = ((Type)types.get(index)).getJavaClass();
        if (!isCompatibleValue(formalType, value))
        {
            String actualTypeName = ((value==null) ? "<type of null>" : value.getClass().getName());
            throw new JDOQueryException(
                I18NHelper.getMessage(messages, "jqlc.parametertable.definevaluebyindex.typemismatch",  //NOI18N
                                      actualTypeName, formalType.getName()));
        }

        // everything is ok => set the actual parameters's value
        values.set(index, value);
    }

    /**
     * Checks whether the type of the specified value is compatible with the
     * specified formal type.
     * @param name the formal type.
     * @param the value to be checked
     * @return <code>true</code> if the type of the value is compatible with the
     * formal type; <code>false</code> otherwise.
     */
    private boolean isCompatibleValue(Class formalType, Object value)
    {
        boolean isCompatible = true;

        // handle value == null
        if (value == null) {
            isCompatible = !formalType.isPrimitive();
        }
        else {
            Class actualType = value.getClass();
            if (formalType.isPrimitive())
                formalType = JavaTypeHelper.getWrapperClass(formalType);

            isCompatible = formalType.isAssignableFrom(actualType);
        }
        return isCompatible;
    }

}

