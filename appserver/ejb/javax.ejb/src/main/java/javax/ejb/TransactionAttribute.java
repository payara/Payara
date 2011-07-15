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

package javax.ejb;

import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * The <code>TransactionAttribute</code> annotation specifies whether
 * the container is to invoke a business method within a transaction
 * context.  
 *
 * The <code>TransactionAttribute</code> annotation can be used for
 * session beans and message driven beans.  It can only be specified
 * if container managed transaction demarcation is used. 
 * <p>
 *
 * The annotation can be specified on the bean class and/or it can be
 * specified on methods of the class that are methods of the business
 * interface or no-interface view. 
 * <p>
 * Specifying the <code>TransactionAttribute</code> annotation on the
 * bean class  means that it applies to all applicable business 
 * methods of the class. Specifying the annotation on a 
 * method applies it to that method only. If the annotation is applied 
 * at both the class and the method level, the method value overrides 
 * if the two disagree.  
 *
 * <p>
 * The values of the <code>TransactionAttribute</code> annotation are
 * defined by the enum <code>TransactionAttributeType</code>.  If 
 * the <code>TransactionAttribute</code> annotation is not specified, and 
 * the bean uses container managed transaction demarcation, the semantics of 
 * the <code>REQUIRED</code>  transaction attribute are assumed.
 *
 * @see TransactionAttributeType
 *
 * @since EJB 3.0
 */

@Target({METHOD, TYPE})
@Retention(RUNTIME)
public @interface TransactionAttribute {
    TransactionAttributeType value() default TransactionAttributeType.REQUIRED;
}
