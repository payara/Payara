/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.config.support;

import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.glassfish.api.admin.AdminCommandContext;

import javax.inject.Inject;
import javax.inject.Named;
import java.lang.annotation.Annotation;

/**
 * A config resolver is responsible for finding the target object of a specified
 * type on which a creation command invocation will be processed.
 *
 * Implementation of these interfaces can be injected with the command invocation
 * parameters in order to determine which object should be returned
 *
 * @author Jerome Dochez
 */
@Contract
public interface CrudResolver {

    /**
     * Retrieves the existing configuration object a command invocation is intented to mutate.
     *
     * @param context the command invocation context
     * @param type the type of the expected instance
     * @return the instance or null if not found 
     */
    <T extends ConfigBeanProxy> T resolve(AdminCommandContext context, Class<T> type);

    @Service
    public static final class DefaultResolver implements CrudResolver {
        
        @Inject
        @Named("type")
        @Optional
        CrudResolver defaultResolver=null;

        @Override
        public <T extends ConfigBeanProxy> T resolve(AdminCommandContext context, Class<T> type) {
            if (defaultResolver!=null) {
                return defaultResolver.resolve(context, type);
            }
            return null;
        }
    }
}
