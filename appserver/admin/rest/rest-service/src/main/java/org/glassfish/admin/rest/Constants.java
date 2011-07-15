/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest;

/**
 * REST Interface Constants
 * @author Rajeshwar Paitl
 */
public interface Constants {
    public static final String INDENT = "  ";
    public static final String JAVA_STRING_TYPE = "java.lang.String";
    public static final String JAVA_BOOLEAN_TYPE = "java.lang.Boolean";
    public static final String JAVA_INT_TYPE = "java.lang.Integer";
    public static final String JAVA_PROPERTIES_TYPE = "java.util.Properties";
    public static final String XSD_STRING_TYPE = "string";
    public static final String XSD_BOOLEAN_TYPE = "boolean";
    public static final String XSD_INT_TYPE = "int";
    public static final String XSD_PROPERTIES_TYPE = "string"; //?
    public static final String TYPE = "type";
    public static final String KEY = "key";
    public static final String OPTIONAL = "optional";
    public static final String DEFAULT_VALUE = "defaultValue";
    public static final String ACCEPTABLE_VALUES = "acceptableValues";
    public static final String DEPRECATED = "deprecated";

    public static final String VAR_PARENT = "$parent";
    public static final String VAR_GRANDPARENT = "$grandparent";
    
    public static final String ENCODING = "UTF-8";
}