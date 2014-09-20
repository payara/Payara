/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

/*
  * DeploymentStatusImplWithError.java
  *
  * Created on August 13, 2004, 8:54 AM
  */

package org.glassfish.deployapi;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import javax.enterprise.deploy.shared.StateType;
import javax.enterprise.deploy.shared.CommandType;

/**
  *Simple implementation of DeploymentStatus intended to describe an exception that
  *occurred during a DeploymentManager method invocation.
  * @author  tjquinn
  */
public class DeploymentStatusImplWithError extends DeploymentStatusImpl {
    
    /** Records the error, if any, associated with this status object */
    private Throwable cause = null;
    
    /** Creates a new instance of DeploymentStatusImplWithError */
    public DeploymentStatusImplWithError() {
    }
    
    /** Creates a new instance of DeploymentStatusImplWithError */
    public DeploymentStatusImplWithError(CommandType commandType, Throwable cause) {
        super();
        initCause(cause);
        setState(StateType.FAILED);
        setCommand(commandType);
    }
    
    /**
     *Assigns the cause for this status.
     *@param Throwable that describes the error to be reported
     */
    public void initCause(Throwable cause) {
        this.cause = cause;
        setMessage(cause.getMessage());
    }
    
    /**
     *Returns the cause for this status.
     *@return Throwable that describes the error associated with this status
     */
    public Throwable getCause() {
        return cause;
    }
    
    /**
     *Displays the status as a string, including stack trace information if error is present.
     *@return String describing the status, including stack trace info from the error (if present).
     */
    public String toString() {
        StringBuffer result = new StringBuffer(super.toString());
        if (cause != null) {
            String lineSep = System.getProperty("line.separator");
            result.append(lineSep).append("Cause: ");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter pw = new PrintWriter(baos);
            cause.printStackTrace(pw);
            pw.close();
            result.append(baos.toString());
        }
        return result.toString();
    }
}
