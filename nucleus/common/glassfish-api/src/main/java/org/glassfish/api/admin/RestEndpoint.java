/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.jvnet.hk2.config.ConfigBeanProxy;

/**
 *
 * @author Jason Lee
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RestEndpoint {
    enum OpType { GET, PUT, POST, DELETE}

    /**
     * ConfigBean to which to attach the AdminCommand
     *
     * @return the name of the target ConfigBean
     */
    Class<? extends ConfigBeanProxy> configBean();

    /**
     * Rest operation type that should trigger a redirect to an actual asadmin
     * command invocation. The default is GET.
     *
     * @return the rest operation type for this redirect
     */
    OpType opType() default OpType.GET;

    /**
     * This is the value of the last segment in the generated URL.  If blank, this
     * will default to the value of the name attribute on the commands @Service annotation
     * @return 
     */
    String path() default "";
    
    /**
     * The description of the endpoint. This is used primarily in the REST HTML interface.
     * @return 
     */
    String description() default "";
    
    /**
     * A list of one or more @RestParam annotations representing the parameters to be
     * used in the AdminCommand call
     * @return 
     */
    RestParam[] params() default {};
    
    /**
     * Whether this RestEndpoint should be used for command authorization
     * decisions automatically.  Setting this to true causes the admin command
     * framework automatically to use the configBean attribute to compute the resource name
     * and the OpType to compute the action.
     * @return 
     */
    boolean useForAuthorization() default false;
}
