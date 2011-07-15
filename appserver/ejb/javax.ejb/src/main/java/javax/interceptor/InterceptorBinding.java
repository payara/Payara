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

package javax.interceptor;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <p>Specifies that an annotation type is an interceptor binding type.</p>
 * 
 * <pre>
 * &#064;Inherited 
 * &#064;InterceptorBinding 
 * &#064;Target({TYPE, METHOD}) 
 * &#064;Retention(RUNTIME) 
 * public &#064;interface Valid {}
 * </pre>
 * 
 * <p>Interceptor bindings
 * are intermediate annotations that may be used to associate 
 * interceptors with target beans.</p>
 * 
 * <p>The interceptor bindings of an interceptor are specified by annotating 
 * the interceptor class with the binding types and the
 * {@link javax.interceptor.Interceptor Interceptor} annotation.</p>
 * 
 * <pre>
 * &#064;Valid &#064;Interceptor
 * public class ValidationInterceptor { ... }
 * </pre>
 * 
 * <p>An interceptor may specify multiple interceptor bindings.</p>
 * 
 * <p>An interceptor binding of a bean 
 * may be declared by annotating the bean class, or a method of the bean class, 
 * with the interceptor binding type.</p>
 * 
 * <pre>
 * &#064;Valid
 * public class Order { ... }
 * </pre>
 * 
 * <pre>
 * &#064;Valid &#064;Secure
 * public void updateOrder(Order order) { ... }
 * </pre>
 * 
 * <p>A bean class or method of a bean class may declare multiple interceptor 
 * bindings.</p>
 * 
 * <p>An interceptor binding type may declare other interceptor bindings.</p>
 * 
 * <pre>
 * &#064;Inherited 
 * &#064;InterceptorBinding 
 * &#064;Target({TYPE, METHOD}) 
 * &#064;Retention(RUNTIME) 
 * &#064;Valid
 * public &#064;interface Secure {}
 * </pre>
 * 
 * <p>Interceptor bindings are transitive&mdash;an interceptor binding declared 
 * by an interceptor binding type is inherited by all beans and other interceptor 
 * binding types that declare that interceptor binding type.</p>
 * 
 * @see javax.interceptor.Interceptor
 *
 * @since Interceptors 1.1
 */
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
@Documented
public @interface InterceptorBinding {}
