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

import org.jvnet.hk2.annotations.Contract;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * Marks inhabitants that require configuration for instantiation.
 *
 * @author Kohsuke Kawaguchi
 */
@Contract
@Retention(RUNTIME)
@Target(TYPE)
@Inherited
public @interface Configured {
    /**
     * XML element name that this configured object maps to.
     *
     * See {@link Attribute#value()} for how the default value is inferred.
     *
     * @see #local()
     */
    String name() default "";

    /**
     * Designates the configuration element as local element.
     *
     * <p>
     * By default, element names for configured inhabitant are global, meaning
     * component can be referenced from anywhere that allow a valid substitution, such
     * as:
     *
     * <pre>
     * &#64;Element("*")
     * List&lt;Object> any;
     * </pre>
     *
     * <p>
     * Setting this flag to true will prevent this, and instead make this element local.
     * Such configuration inhabitants will not have any associated element name, and therefore
     * can only participate in the configuration XML when explicitly referenced from its parent.
     * For example:
     *
     * <pre>
     * &#64;Configured
     * class Foo {
     *   &#64;Element List&lt;Property> properties;
     * }
     *
     * &#64;Configured(local=true)
     * class Property {
     *   &#64;FromAttribute String name;
     *   &#64;FromAttribute String value;
     * }
     *
     * &lt;foo>
     *   &lt;property name="aaa" value="bbb" />
     * &lt;/foo>
     * </pre>
     *
     * <p>
     * This switch is mutually exclusive with {@link #name()}.
     */
    boolean local() default false;

    /**
     * Designates that a configured component creates
     * a new symbol space for given kinds of children.
     */
    Class[] symbolSpace() default {};
}
