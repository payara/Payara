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
 * MathType.java
 *
 * Created on August 24, 2001
 */

package com.sun.jdo.spi.persistence.support.sqlstore.query.util.type;

import java.math.BigDecimal;
import java.math.BigInteger;

/** 
 * This class represents the types java.math.BigDecimal and java.math.BigInteger.
 *
 *
 * @author  Michael Bouschen
 * @version 0.1
 */
public class MathType
    extends ClassType
    implements NumberType
{
    /**
     *
     */
    public MathType(String name, Class clazz, int enumType, TypeTable typetab)
    {
        super(name, clazz, enumType, typetab);
    }
    
    /**
     * A numeric wrapper class type defines an ordering.
     */
    public boolean isOrderable()
    {
        return true;
    }

    /**
     * Converts the specified value into a value of this numeric type.
     * E.g. an Integer is converted into a BigDecimal, if this represents 
     * the type BigDecimal.
     * @param value value to be converted
     * @return converted value
     */
    public Number getValue(Number value)
    {
        Number ret = null;

        if (value == null)
            ret = null;
        else if ("java.math.BigDecimal".equals(getName()))
        {
            if (value instanceof BigDecimal)
                ret = value;
            else if (value instanceof BigInteger)
                ret = new BigDecimal((BigInteger)value);
            else if (value instanceof Double)
                ret = new BigDecimal(((Double)value).toString());
            else if (value instanceof Float)
                ret = new BigDecimal(((Float)value).toString());
            else if (value instanceof Number)
                ret = BigDecimal.valueOf(((Number)value).longValue());
        }
        else if ("java.math.BigInteger".equals(getName()))
        {
            if (value instanceof BigInteger)
                ret = value;
            else if (value instanceof Double)
                ret = (new BigDecimal(((Double)value).toString())).toBigInteger();
            else if (value instanceof Float)
                ret = (new BigDecimal(((Float)value).toString())).toBigInteger();
            else if (value instanceof Number)
                ret = BigInteger.valueOf(((Number)value).longValue());
        }
        
        return ret;
    }

    /**
     * Returns -value. 
     * @param value value to be negated
     * @return -value
     */
    public Number negate(Number value)
    {
        Number ret = null;

        if (value == null)
            ret = null;
        else if ("java.math.BigDecimal".equals(getName()))
        {
            if (value instanceof BigDecimal)
                ret = ((BigDecimal)value).negate();
            else if (value instanceof BigInteger)
                ret = new BigDecimal(((BigInteger)value).negate());
            else if (value instanceof Double)
                ret = (new BigDecimal(((Double)value).toString())).negate();
            else if (value instanceof Float)
                ret = (new BigDecimal(((Float)value).toString())).negate();
            else if (value instanceof Number)
                ret = BigDecimal.valueOf(-((Number)value).longValue());
        }
        else if ("java.math.BigInteger".equals(getName()))
        {
            if (value instanceof BigInteger)
                ret = ((BigInteger)value).negate();
            else if (value instanceof Double)
                ret = (new BigDecimal(((Double)value).toString())).negate().toBigInteger();
            else if (value instanceof Float)
                ret = (new BigDecimal(((Float)value).toString())).negate().toBigInteger();
            else if (value instanceof Number)
                ret = BigInteger.valueOf(-((Number)value).longValue());
        }
        
        return ret;
    }
    
}
