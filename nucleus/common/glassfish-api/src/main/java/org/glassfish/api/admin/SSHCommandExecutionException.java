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

/**
 * Created by IntelliJ IDEA.
 * User: cmott
 * Date: Jul 29, 2010
 * Time: 6:56:38 PM
 */
public class SSHCommandExecutionException extends CommandException{


    /**
     * Creates a new <code>SSHCommandExecutionException</code> without
     * detail message.
     */
    public SSHCommandExecutionException() {
        super();
    }


    /**
     * Constructs a <code>SSHCommandExecutionException</code> with the specified
     * detail message.
     * @param msg the detail message.
     */
    public SSHCommandExecutionException(String msg) {
        super(msg);
    }


    /**
     * Constructs a new <code>SSHCommandExecutionException</code> exception with
     * the specified cause.
     */
    public SSHCommandExecutionException(Throwable cause) {
	super(cause);
    }


    /**
     * Constructs a new <code>SSHCommandExecutionException</code> exception with
     * the specified detailed message and cause.
     */
    public SSHCommandExecutionException(String msg, Throwable cause) {
	super(msg, cause);
    }

    private String SSHSettings = null;
    private String fullCommand = null;

    /* Stores the settings for the SSH connection that apply to node that was
     * used in the command execution
     */
    public void setSSHSettings(String sshSettings){
        SSHSettings = sshSettings;
    }
    /* Returns the settings for the SSH connection that apply to node that was
     * used in the command execution
     */

    public String getSSHSettings() {
        return SSHSettings;
    }
    /* Stores the fully qualified command that was run on the remote node over SSH
     */

    public void setCommandRun(String fullcommand){
        fullCommand= fullcommand;
    }
    /* Returns the fully qualified command that was run on the remote node over SSH 
     */

    public String getCommandRun() {
        return fullCommand;
    }
}

