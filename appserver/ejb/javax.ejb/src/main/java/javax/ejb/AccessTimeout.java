/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

package javax.ejb;

import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.*;

import java.util.concurrent.TimeUnit;

/**
 * Specifies the amount of time in a given time unit that a concurrent 
 * access attempt should block before timing out.  
 * <p>
 * This annotation may be applied to a stateful session bean or to a
 * singleton session bean that uses container managed concurrency.
 * <p>
 * By default, clients are allowed to make concurrent calls to a
 * stateful session object and the container is required to serialize
 * such concurrent requests.  The <code>AccessTimeout</code>
 * annotation is used to specify the amount of time a stateful session
 * bean request should block in the case that the bean instance 
 * is already processing a different request.  Use of the
 * <code>AccessTimeout</code> annotation with a value of 0 specifies
 * to the container that concurrent client requests to a stateful
 * session bean are prohibited.
 * <p>
 * The <code>AccessTimeout</code> annotation can be specified on a
 * business method or a bean class.  If it is specified on a class, it
 * applies to all business methods of that class.  If it is specified
 * on both a class and on a business method of the class, the
 * method-level annotation takes precedence for the given method.
 * <p>
 * Access timeouts for a singleton session bean only apply to methods
 * eligible for concurrency locks.  The <code>AccessTimeout</code> annotation can
 * be specified on the singleton session bean class or on an eligible
 * method of the class.  If <code>AccessTimeout</code> is specified on
 * both a class and on a method of that class, the method-level annotation 
 * takes precedence for the given method.
 * <p>
 * The semantics of the <code>value</code> element are as follows:
 * <ul>
 * <li>A value <code>&#062;</code> 0 indicates a timeout value in the units
 * specified by the <code>unit</code> element.
 * <li>A value of 0 means concurrent access is not permitted.
 * <li>A value of -1 indicates that the client request will block
 * indefinitely until forward progress it can proceed.
 * </ul>
 * Values less than -1 are not valid.
 *
 * @since EJB 3.1
 */
@Target({METHOD, TYPE}) 
@Retention(RUNTIME)
public @interface AccessTimeout {

    /**
     * The semantics of the <code>value</code> element are as follows:
     * <ul>
     * <li>A value <code>&#062;</code> 0 indicates a timeout value in the units
     * specified by the <code>unit</code> element.
     * <li>A value of 0 means concurrent access is not permitted.
     * <li>A value of -1 indicates that the client request will block
     * indefinitely until forward progress it can proceed.
     * </ul>
     * Values less than -1 are not valid.
     */
    long value();

    /**
     * Units used for the specified value.
     */
    TimeUnit unit() default TimeUnit.MILLISECONDS;

}
