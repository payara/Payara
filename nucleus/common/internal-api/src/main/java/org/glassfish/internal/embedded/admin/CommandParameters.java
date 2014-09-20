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

package org.glassfish.internal.embedded.admin;

import java.util.*;
import org.glassfish.api.admin.ParameterMap;

/**
 * Command Parameters, needs to be refined...
 *
 * @author Jerome Dochez
 */
public class CommandParameters {

    private ParameterMap params = new ParameterMap();
    private List<String> operands = new ArrayList<String>();;

    /**
     * Sets the command primary (operand) parameter.
     * @param operand the command operand
     */
    public void setOperand(String operand) {
        operands.clear();
	operands.add(operand);
    }

    /**
     * Adds a command primary (operand) parameter.
     * @param operand the command operand
     */
    public void addOperand(String operand) {
	operands.add(operand);
    }

    /**
     * Get the first operand.
     */
    public String getOperand() {
        return operands.get(0);
    }

    /**
     * Get the operands.
     */
    public List<String> getOperands() {
	return new ArrayList<String>(operands);
    }

    /**
     * Sets a command option as the user would specify it using the
     * CLI command for instance
     *
     * @param optionName option name (without leading -- chars)
     * @param optionValue option value
     */
    public void setOption(String optionName, String optionValue) {
        params.set(optionName, optionValue);
    }

    /**
     * Adds a command option as the user would specify it using the
     * CLI command for instance
     *
     * @param optionName option name (without leading -- chars)
     * @param optionValue option value
     */
    public void addOption(String optionName, String optionValue) {
        params.add(optionName, optionValue);
    }

    public ParameterMap getOptions() {
        return new ParameterMap(params);
    }
}
