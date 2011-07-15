/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;

/**
 * A parameter mapper acts as a bridge between supplied parameters (by the
 * user on the command line for instance) and expected parameters by a
 * action.
 * <p>
 * For example, a command execution requires parameters from the command line
 * while a supplemented command may require a different set of parameter names
 * which can be either discovered or translated from the originally supplied
 * list.
 *
 * @author Jerome Dochez
 */
public interface ParameterBridge {

    /**
     * Returns the parameter value as expected by the injection code when a dependency
     * injection annotated field or method (for instance, annotated with @Param
     * or @Inject) needs to be resolved.
     *
     * @param map is the original set of parameters as used to inject the supplemented
     * command.
     * @param resourceName the name of the resource as defined by the action
     * @return the value used to inject the parameter identified by its resourceName
     */
    public String getOne(ParameterMap map, String resourceName);

    /**
     * Returns the parameter values as expected by the injection code when a dependency
     * injection annotated field or method (for instance, annotated with @Param
     * or @Inject) needs to be resolved.
     *
     * @param map is the original set of parameters as used to inject the supplemented
     * command.
     * @param resourceName the name of the resource as defined by the action
     * @return a list of values used to inject the parameter identified by its resourceName
     */
    public List<String> get(ParameterMap map, String resourceName);

    /**
     * Provided mapper that does not change parameters names or values from the input set.
     */
    final class NoMapper implements ParameterBridge {
        @Override
        public String getOne(ParameterMap map, String resourceName) {
            return map.getOne(resourceName);
        }

        @Override
        public List<String> get(ParameterMap map, String resourceName) {
            return map.get(resourceName);
        }
    }
}
