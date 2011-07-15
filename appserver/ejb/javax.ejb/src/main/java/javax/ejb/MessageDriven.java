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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Component-defining annotation for a message driven bean.  
 * <p>
 * The message driven bean must implement the appropriate message
 * listener interface for the messaging type that the message-driven
 * bean supports or specify the message listener interface using the
 * <code>messageListenerInterface</code> element of this annotation.
 *
 * @see ActivationConfigProperty
 *
 * @since EJB 3.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MessageDriven {

    /**
     * The ejb-name for this bean.  Defaults to the unqualified name of
     * the message driven bean class.
     */
    String name() default "";

    /**
     * Message-listener interface.  If the message-driven bean class
     * implements more than one interface other than <code>java.io.Serializable</code>,
     * <code>java.io.Externalizable</code>, or any of the interfaces defined by the
     * <code>javax.ejb</code> package, the message listener interface must be 
     * specified.
     */
    Class messageListenerInterface() default Object.class;

    /**
     * Activation config properties.
     */
    ActivationConfigProperty[] activationConfig() default {}; 

    /**
      * A product specific name(e.g. global JNDI name of a queue) 
      * that this message-driven bean should be mapped to.  
      * 
      * Application servers are not required to support any particular 
      * form or type of mapped name, nor the ability to use mapped names. 
      * The mapped name is product-dependent and often installation-dependent. 
      * No use of a mapped name is portable. 
      */ 
    String mappedName() default "";

    /**
     * A string describing the message driven bean.
     */ 
    String description() default "";
}

