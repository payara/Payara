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

package org.glassfish.internal.api;

import com.sun.enterprise.config.serverbeans.Server;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;

import java.util.List;

/**
 * Interface that augment the AdminCommand responsibilities by adding the ability
 * to undo a previously successful execution of an administrative command.
 *
 * <p>The ability to rollback is not meant to be used as an exception handling
 * mechanism while in the {@link AdminCommand#execute(AdminCommandContext)} invocation.
 * The ability to rollback is meant for undoing a successful command execution that
 * need to be roll-backed for reasons outside of the knowledge of the command
 * implementation.
 *
 * <p>Roll-backing can be very useful in clustering mode where actions can be performed
 * successfully on some instances but fail on others necessitating to rollback the
 * entire set of instances to a state previous to the action execution.
 *
 * <p>The implementations of this interface must retain any pertinent information necessary
 * to undo the command within its instance context. Therefore all UndoableCommand implementations
 * must have a {@link org.glassfish.hk2.api.PerLookup} scope otherwise the system will flag it
 * as an error and will refuse to execute the command.
 *
 * <p>An undo-able command has a slightly more complicated set of phases execution as compared
 * to the AdminCommand.
 *
 * <p>During the first phase, called the prepare phase, the framework will call
 * this command prepare method as well as all supplemented commands prepare methods (if such
 * supplemented commands implement the UndoableCommand interface). If the prepare phase is
 * not successful, the command execution stops here and the command feedback is returned to the
 * initiator.
 *
 * <p>Once the prepare phase has succeeded, the normal {@link AdminCommand#execute(AdminCommandContext)}
 * method is invoked (and any supplemented methods).
 *
 * <p>If the framework is electing that successful commands execution need to be rolled back, it will
 * call the {@link #undo(AdminCommandContext, ParameterMap, List<Server>)} method on the same instance that was used for the
 * {@link #execute(AdminCommandContext)} invocation, as well as any supplemented commands that implement
 * this interface.
 * 
 * @author Jerome Dochez
 */
public interface UndoableCommand extends AdminCommand {

    /**
     * Checks whether the command execution has a chance of success before the execution is
     * attempted. This could be useful in clustering environment where you must check certain
     * pre-conditions before attempting to run an administrative change.
     *
     * <p>For instance, the change-admin-password should probably not be attempted if all the
     * servers instances are on-line and can be notified of the change.
     *
     * <p>No changes to the configuration should be made within the implementation of the
     * prepare method since {@link #undo(AdminCommandContext, ParameterMap, List<Server>)} will not be called if the command
     * execution stops at the prepare phase.
     *
     * <p>Note that if, as part of prepare, remote instances have to be contacted, then it is the responsibility of
     * the command implementation to invoke remote instaces for such verification.
     *
     * The framework will call prepare() method in DAS before execution of the main command
     * and the execution of the main command will happen only if the prepare() call returns ActionReport.ExitCode.SUCCESS
     * on DAS.
     *
     * @param context the command's context
     * @param parameters parameters to the commands.
     */
    @IfFailure(FailurePolicy.Error)
    public ActionReport.ExitCode prepare(AdminCommandContext context, ParameterMap parameters);

    /**
     * Undo a previously successful execution of the command implementation. The context
     * for undoing the administrative operation should be obtained from either the
     * parameters passed to the command execution or the command instance context.
     * The list of servers indicates to the command implementation on which servers the command succeeded
     * The command implementation is guaranteed that the  {@link #undo(AdminCommandContext, ParameterMap, List<Server>)}
     * is called on DAS if the main command execution failed on one instance but it is the responsiblity of the command
     * implementation to invoke remote instances to do the actual undo if required
     *
     * @param context the command's context
     * @param parameters parameters passed to the command.
     * @param instances instances on which the command succeeded
     */
    public void undo(AdminCommandContext context, ParameterMap parameters, List<Server> instances);
}
