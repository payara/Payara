/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
 */
package fish.payara.asadmin;

import java.io.Serializable;

/**
 *
 * @author steve
 * @since 5.202
 */
public interface CommandResult extends Serializable {

    /**
     * A command can have following types of exit status.
     */
    public enum ExitStatus {
        SUCCESS,
        WARNING,
        FAILURE
    }
    
    /**
     * Gets the resulting status of the command
     * @return exit status of the command
     */
    public ExitStatus getExitStatus();

    /**
     * This method returns any exception raised during command invocation.
     * 
     * If the command succeeded, then this method will return {@code null}.
     * If the command had a warning occur this will probably return an exception,
     * but may return {@code null}.
     * @return any exception that occurred during this command execution.
     */
    public Throwable getFailureCause();

    /**
     * The output of the command
     * @return command output
     */
    public String getOutput();
    
}
