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

package org.glassfish.deployapi;


import javax.enterprise.deploy.spi.status.DeploymentStatus;
import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.shared.CommandType;
import javax.enterprise.deploy.shared.ActionType;
/**
 *
 * @author  dochez
 */
public class DeploymentStatusImpl implements DeploymentStatus {
    
    ProgressObjectImpl progressObject;
    StateType stateType = null;
    String lastMsg = null;
    CommandType commandType = null;
    
    /** Creates a new instance of DeploymentStatusImpl */
    public DeploymentStatusImpl(ProgressObjectImpl progressObject) {
        this.progressObject = progressObject;
    }

    public DeploymentStatusImpl() {
    }
    
    /** Retrieve the deployment ActionType for this event.
     *
     * @return the ActionType Object
     */
    public ActionType getAction() {
        return ActionType.EXECUTE;
    }
    
    /** Retrieve the deployment CommandType of this event.
     *
     * @return the CommandType Object
     */
    public CommandType getCommand() {
        if (progressObject!=null) {
            return progressObject.getCommandType();
        } else {
            return commandType;
        }
    }
    
    /** Retrieve any additional information about the
     * status of this event.
     *
     * @return message text
     */
    public String getMessage() {
        return lastMsg;
    }
    
    /** Retrieve the StateType value.
     *
     * @return the StateType object
     */
    public StateType getState() {
        return stateType;
    }
    
    /** A convience method to report if the operation is
     * in the completed state.
     *
     * @return true if this command has completed successfully
     */
    public boolean isCompleted() {
        return StateType.COMPLETED.equals(stateType);        
    }
    
    /** A convience method to report if the operation is
     * in the failed state.
     *
     * @return true if this command has failed
     */
    public boolean isFailed() {
        return StateType.FAILED.equals(stateType);        
    }
    
    /** A convience method to report if the operation is
     * in the running state.
     *
     * @return true if this command is still running
     */
    public boolean isRunning() {
        return StateType.RUNNING.equals(stateType);
    }
    
    public void setState(StateType stateType) {
        this.stateType = stateType;
    }
    
    public void setMessage(String message) {
        lastMsg = message;
    }
    
    public void setCommand(CommandType commandType) {
        this.commandType = commandType;
    }
}
