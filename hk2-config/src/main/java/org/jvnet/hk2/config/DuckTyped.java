/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;
import java.lang.annotation.Documented;

/**
 * Indicates that this method on {@link ConfigBeanProxy} is a duck-typed method
 * that are implemented by the static method on the nested static {@code Duck} class.
 *
 * <h2>Usage</h2>
 * <p>
 * Often it's convenient if one can define some convenience methods on the config bean proxy.
 * This mechanism allows you to do that by putting this annotation on a convenience method,
 * then write the nested static {@code Duck} class and places the actual implementation there.
 *
 * <pre>
 * interface MyConfigBean extends ConfigBeanProxy {
 *    &#64;Element
 *    List&lt;Property> getProperties();
 *
 *    &lt;DuckTyped
 *    String getPropertyValue(String name);
 *
 *    class Duck {
 *        public static String getPropertyValue(MyConfigBean me, String name) {
 *            for( Property p : me.getProperties() )
 *                if(p.getName().equals(name))
 *                    return p.getValue();
 *            return null;
 *        }
 *    }
 * }
 * </pre>
 *
 * <p>
 * The invocation of the <tt>getPropertyValue</tt> above will cause HK2 to in turn invoke
 * the corresponding static method on {@code Duck}, where the first argument will be the
 * original {@code this} value.
 *
 * @author Kohsuke Kawaguchi
 * @see ConfigBeanProxy
 */
@Retention(RUNTIME)
@Target(METHOD)
@Documented
public @interface DuckTyped {
}
