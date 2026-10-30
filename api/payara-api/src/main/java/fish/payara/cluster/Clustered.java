/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2022] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package fish.payara.cluster;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be added to @Singleton EJB beans
 * and @ApplicationScoped CDI beans to specify that they are
 * cluster-wide singletons, not just a singleton per server instance
 *
 * @author lprimak
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Clustered {
    /**
     * key in the distributed map to bind this clustered object to.
     * Default is the name of the bean
     */
    String keyName() default "";

    /**
     * specifies the type of distributed locking to be performed
     * For EJB beans, only INHERIT and LOCK_NONE are valid
     * for CDI beans, INHERIT is equivalent to NONE,
     * and the other valid value for CDI beans is LOCK
     */
    DistributedLockType lock() default DistributedLockType.INHERIT;

    /**
     * Specifies whether to call @PostConstruct when the singleton is attached to a cluster
     * and this singleton already exists on the other node. (not truly created)
     * Default is true
     */
    boolean callPostConstructOnAttach() default true;

    /**
     * Specifies whether to call @PreDestroy when the singleton is detached from a cluster
     * and this singleton also exists on the other node. (not truly destroyed)
     * Default is true
     */
    boolean callPreDestroyOnDetach () default true;
}
