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
 * Used to provide information to the deployer about the configuration of
 * a message driven bean in its operational environment.
 * <p>
 * The following standard properties are recognized for JMS message driven
 * beans:
 * <ul>
 * <li> <code>acknowledgeMode</code>.  This property is used to specify
 * the JMS acknowledgement mode for the message delivery when bean-managed 
 * transaction demarcation is used.
 * Its values are <code>Auto_acknowledge</code> or <code>Dups_ok_acknowledge</code>.  
 * If this property is not specified, JMS auto acknowledge semantics are assumed.
 *
 * <li> <code>messageSelector</code>.  This property is used to specify
 * the JMS message selector to be used in determining which messages a
 * JMS message driven bean is to receive.
 *
 * <li> <code>destinationType</code>.  This property is used to specify
 * whether the message driven bean is intended to be used with a queue or
 * a topic.  The value must be either <code>javax.jms.Queue</code> or
 * <code>javax.jms.Topic</code>.
 *
 * <li> <code>subscriptionDurability</code>.  If the message driven bean is
 * intended to be used with a topic, this property may be used to indicate
 * whether a durable or non-durable subscription should be used.   The
 * value of this property must be either <code>Durable</code> or <code>NonDurable</code>
 * </ul>
 *
 * @since EJB 3.0
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface ActivationConfigProperty {
    String propertyName();
    String propertyValue();
}
