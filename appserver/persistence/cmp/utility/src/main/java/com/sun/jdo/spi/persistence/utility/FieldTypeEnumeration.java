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
 * FieldTypeEnumeration
 *
 * Created on January 31, 2003
 */

package com.sun.jdo.spi.persistence.utility;

/**
 *
 */
public interface FieldTypeEnumeration
{

    //Not Enumerated
    public static final int NOT_ENUMERATED        = 0;

    //Primitive
    public static final int BOOLEAN_PRIMITIVE     = 1;
    public static final int CHARACTER_PRIMITIVE   = 2;
    public static final int BYTE_PRIMITIVE        = 3;
    public static final int SHORT_PRIMITIVE       = 4;
    public static final int INTEGER_PRIMITIVE     = 5;
    public static final int LONG_PRIMITIVE        = 6;
    public static final int FLOAT_PRIMITIVE       = 7;
    public static final int DOUBLE_PRIMITIVE      = 8;
    //Number
    public static final int BOOLEAN               = 11;
    public static final int CHARACTER             = 12;
    public static final int BYTE                  = 13;
    public static final int SHORT                 = 14;
    public static final int INTEGER               = 15;
    public static final int LONG                  = 16;
    public static final int FLOAT                 = 17;
    public static final int DOUBLE                = 18;
    public static final int BIGDECIMAL            = 19;
    public static final int BIGINTEGER            = 20;
    //String
    public static final int STRING                = 21;
    //Dates
    public static final int UTIL_DATE             = 22;
    public static final int SQL_DATE              = 23;
    public static final int SQL_TIME              = 24;
    public static final int SQL_TIMESTAMP         = 25;
    //Arrays
    public static final int ARRAY_BYTE_PRIMITIVE  = 51;

}
