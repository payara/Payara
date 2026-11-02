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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Indicates that this property or the field value must be injected from
 * an XML element in a configuration file.
 *
 * @author Kohsuke Kawaguchi
 * @see Attribute
 */
@Retention(RUNTIME)
@Target({FIELD,METHOD})
public @interface Element {
    /**
     * Element name.
     *
     * See {@link Attribute#value()} for how the default value is inferred.
     */
    String value() default "";

    /**
     * Indicates that this property becomes the name of the component.
     * There can be only one key on a class.
     */
    boolean key() default false;

    /**
     * Indicates that this element is required.
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
     * <p>
     * On XML, this is represented as a string value that points
     * to the {@link #key() value of the key property} of the target
     * inhabitant. See the following example:
     *
     * <pre>
     * &#x40;Configured
     * class VirtualHost {
     *   &#x40;Attribute(key=true)
     *   String name;
     * }
     *
     * &#x40;Configured
     * class HttpListener {
     *   &#x40;Attribute(reference=true)
     *   VirtualHost host;
     * }
     * </pre>
     *
     * <pre><xmp>
     * <virtual-host name="foo" />
     * <http-listener host="foo" />
     * </xmp></pre>
     */
    boolean reference() default false;

    /**
     * Indicates that the variable expansion should be performed on this proeprty.
     *
     * <p>
     * The configuration mechanism supports the Ant/Maven like {@link VariableResolver variable expansion}
     * in the configuration XML out of the box. Normally this happens transparently to objects in modules,
     * hence this property is set to true by default.
     *
     * <p>
     * However, in a rare circumstance you might want to get values injected before the variables
     * are expanded, in which case you can set this property to false to indicate so. Note that such
     * property must be of type {@link String} (or its collection/array.)
     *
     * <p>
     * Also note the inhabitants can always access the XML infoset by talking to {@link Dom} directly.
     */
    boolean variableExpansion() default true;
}
