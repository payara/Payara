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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.ssi;


import java.io.PrintWriter;
import java.text.ParseException;
/**
 * SSI command that handles all conditional directives.
 * 
 * @version $Revision: 1.2 $
 * @author Paul Speed
 * @author David Becker
 */
public class SSIConditional implements SSICommand {
    /**
     * @see SSICommand
     */
    public long process(SSIMediator ssiMediator, String commandName,
            String[] paramNames, String[] paramValues, PrintWriter writer)
            throws SSIStopProcessingException {
    	// Assume anything using conditionals was modified by it
    	long lastModified = System.currentTimeMillis();
        // Retrieve the current state information
        SSIConditionalState state = ssiMediator.getConditionalState();
        if ("if".equalsIgnoreCase(commandName)) {
            // Do nothing if we are nested in a false branch
            // except count it
            if (state.processConditionalCommandsOnly) {
                state.nestingCount++;
                return lastModified;
            }
            state.nestingCount = 0;
            // Evaluate the expression
            if (evaluateArguments(paramNames, paramValues, ssiMediator)) {
                // No more branches can be taken for this if block
                state.branchTaken = true;
            } else {
                // Do not process this branch
                state.processConditionalCommandsOnly = true;
                state.branchTaken = false;
            }
        } else if ("elif".equalsIgnoreCase(commandName)) {
            // No need to even execute if we are nested in
            // a false branch
            if (state.nestingCount > 0) return lastModified;
            // If a branch was already taken in this if block
            // then disable output and return
            if (state.branchTaken) {
                state.processConditionalCommandsOnly = true;
                return lastModified;
            }
            // Evaluate the expression
            if (evaluateArguments(paramNames, paramValues, ssiMediator)) {
                // Turn back on output and mark the branch
                state.processConditionalCommandsOnly = false;
                state.branchTaken = true;
            } else {
                // Do not process this branch
                state.processConditionalCommandsOnly = true;
                state.branchTaken = false;
            }
        } else if ("else".equalsIgnoreCase(commandName)) {
            // No need to even execute if we are nested in
            // a false branch
            if (state.nestingCount > 0) return lastModified;
            // If we've already taken another branch then
            // disable output otherwise enable it.
            state.processConditionalCommandsOnly = state.branchTaken;
            // And in any case, it's safe to say a branch
            // has been taken.
            state.branchTaken = true;
        } else if ("endif".equalsIgnoreCase(commandName)) {
            // If we are nested inside a false branch then pop out
            // one level on the nesting count
            if (state.nestingCount > 0) {
                state.nestingCount--;
                return lastModified;
            }
            // Turn output back on
            state.processConditionalCommandsOnly = false;
            // Reset the branch status for any outer if blocks,
            // since clearly we took a branch to have gotten here
            // in the first place.
            state.branchTaken = true;
        } else {
            throw new SSIStopProcessingException();
            //throw new SsiCommandException( "Not a conditional command:" +
            // cmdName );
        }
        return lastModified;
    }


    /**
     * Retrieves the expression from the specified arguments and peforms the
     * necessary evaluation steps.
     */
    private boolean evaluateArguments(String[] names, String[] values,
            SSIMediator ssiMediator) throws SSIStopProcessingException {
        String expr = getExpression(names, values);
        if (expr == null) {
            throw new SSIStopProcessingException();
            //throw new SsiCommandException( "No expression specified." );
        }
        try {
            ExpressionParseTree tree = new ExpressionParseTree(expr,
                    ssiMediator);
            return tree.evaluateTree();
        } catch (ParseException e) {
            //throw new SsiCommandException( "Error parsing expression." );
            throw new SSIStopProcessingException();
        }
    }


    /**
     * Returns the "expr" if the arg name is appropriate, otherwise returns
     * null.
     */
    private String getExpression(String[] paramNames, String[] paramValues) {
        if ("expr".equalsIgnoreCase(paramNames[0])) return paramValues[0];
        return null;
    }
}
