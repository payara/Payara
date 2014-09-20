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

package org.glassfish.deployment.client;

import javax.enterprise.deploy.spi.status.ProgressObject;

/**
 * This interface extends the JSR88 interface for providing 
 * deployment operations feedback and progress information. 
 * In particular, it allows to retrieve the complete JES 
 * deployment status object with all the phases information.
 *
 * @author Jerome Dochez
 */
public abstract class DFProgressObject implements ProgressObject {
    
    /** 
     * Once the progress object has reached a completed or 
     * failed state, this API will permit to retrieve the 
     * final status information for the deployment
     * @return the deployment status
     */
    public abstract DFDeploymentStatus getCompletedStatus();
    
    /**
     * Waits for the operation which this progress object is monitoring to 
     * complete.
     * @return the completed status
     */
    public DFDeploymentStatus waitFor() {
        DFDeploymentStatus status = null;
        do {
            try {
                Thread.currentThread().sleep(100);
            } catch (InterruptedException ie) {
                // Exception swallowed deliberately
            }
            status = getCompletedStatus();
        } while(status == null);
        return status;
    }
}
