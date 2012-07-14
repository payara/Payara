/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.api.admin;


import java.lang.annotation.*;

/**
 * Annotation to qualify when an action like a command is targeted to be run
 * on a cluster or a set of instances.
 * 
 * Some actions may run only on DAS, or only on instances, by default they run
 * on both the DAS and the instances.
 *
 * @author Jerome Dochez
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target(ElementType.TYPE)
public @interface ExecuteOn {

    /**
     * Returns an array of process types on which the annotated action should run
     *
     * @return array of target process types
     */
    RuntimeType[] value() default { RuntimeType.DAS, RuntimeType.INSTANCE};

    /**
     * Identifies the {@link ClusterExecutor} that is responsible for remotely executing
     * commands on the target clusters or instances. The provider will be looked up
     * in the habitat by its type.
     * 
     * @return a {@link ClusterExecutor} type or null to use the default executor that takes the
     * "target" command parameter to 
     */
    Class<? extends ClusterExecutor> executor() default TargetBasedExecutor.class;


    /**
     * Identifies the expected behaviour from the framework if any of the clustered
     * invocation could not be invoked because the remote server was offline.
     *
     * @return the action the framework should perform if any of the remote invocation
     * of this command cannot be executed due to the server being offline.
     */
    FailurePolicy ifOffline() default FailurePolicy.Warn;

    /**
     * Identifies the expected behavior from the framework if any of the clustered
     * invocation could not be invoked because the remote server has never been
     * started.
     *
     * @return the action the framework should perform if any of the remote invocation
     * of this command cannot be executed due to the server being offline.
     */
    FailurePolicy ifNeverStarted() default FailurePolicy.Ignore;

    /**
     * Identifies the expected behavior from the framework if any of the clustered
     * invocation failed.
     *
     * @return the action the framework should perform if any of the remote invocation
     * of this command fails.
     */
    FailurePolicy ifFailure() default FailurePolicy.Error;
}
