/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.jvnet.hk2.config;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Indicates that this property or the field value must be injected from
 * an XML attribute in a configuration file.
 *
 * @author Kohsuke Kawaguchi
 * @see Element
 */
@Retention(RUNTIME)
@Target({FIELD,METHOD})
public @interface Attribute {
    /**
     * Attribute name.
     *
     * <h2>Default Name</h2>
     * <p>
     * If this value is omitted, the default name is inferred from the field/method name.
     * First, if this is a method and the name starts with "set", then "set" will be trimmed
     * off.
     *
     * <p>
     * Then names are tokenized according to the camel case word separator,
     * then tokens are combined with '-', then finally the whole thing is converted
     * to the lower case.
     *
     * <p>
     * Therefore, for example, a field name "httpBufferSize" would yield
     * "http-buffer-size", and a method name "setThreadCount" would yield
     * "thread-count" 
     */
    String value() default "";

    /**
     * Indicates that this property becomes the name of the component.
     * There can be only one key on a class.
     */
    boolean key() default false;

    /**
     * Indicates that this attribute is required.
     *
     * <p>
     * To specify the default value, simply use the field initializer
     * to set it to a certain value. The field/method values are only
     * set when the value is present.
     */
    boolean required() default false;

    /**
     * Indicates that this property is a reference to another
     * configured inhabitant.
     *
     * See {@link Element#reference()} for more details of the semantics.
     *
     * <p>
     * When a reference property is a collection/array, then the key values
     * are separated by ',' with surrounding whitespaces ignored. That is,
     * it can be things like " foo , bar " (which would mean the same thing as
     * "foo,bar".)
     */
    boolean reference() default false;

    /**
     * Indicates that the variable expansion should be performed on this proeprty.
     *
     * See {@link Element#variableExpansion()} for more details.
     */
    boolean variableExpansion() default true;

    /** Specifies the default value of the attribute.
     * @return default value as String
     */
    String defaultValue() default "\u0000";
        
    /** Specifies the data type. It should be the fully qualified name of
     *  the class that identifies the real data type. For attributes that
     *  are of type defined by basic Java primitives (or wrappers), there is
     *  no need to specify this field. The default value is derived from
     *  method/field declaration.
     * @see {@link DataType}
     * @return String specifying the name of the data type for the values of this
     * attribute
     */
    Class dataType() default String.class;
}
