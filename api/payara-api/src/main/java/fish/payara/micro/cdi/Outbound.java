/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.micro.cdi;

import javax.enterprise.util.Nonbinding;
import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * Annotation to be applied to a CDI event @Inject point to send it remotely via CDI event bus. Such events can be observed using the {@link Inbound} qualifier.
 *
 * @author steve
 */
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
@Target({METHOD, FIELD, PARAMETER})
public @interface Outbound {

    /**
     * Provides a further level of filtering. Specify an eventname to restrict
     * event callbacks to events with the specific name
     * @return
     */
    @Nonbinding
    String eventName() default "";

    /**
     * Property to set whether the message should also fire on the same instance as well
     * default is false it won't be fired as an Inbound message on the same instance
     * @return
     */
    @Nonbinding
    boolean loopBack() default false;

    /**
     * Property to restrict the outbound event to specific named server or micro instances.
     * Default behavior is to fire on all server and micro instances.
     * Set one or more instance names to restrict the event to firing only
     * on the specified instances.
     * @return
     */
    @Nonbinding
    String[] instanceName() default "";
}