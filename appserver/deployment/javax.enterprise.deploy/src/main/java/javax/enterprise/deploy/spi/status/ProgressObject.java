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

package javax.enterprise.deploy.spi.status;

import javax.enterprise.deploy.spi.TargetModuleID;
import javax.enterprise.deploy.spi.exceptions.OperationUnsupportedException;

/**
 * The ProgressObject interface tracks and reports
 * the progress of the deployment activities,
 * distribute, start, stop, undeploy.  
 *
 * <p>This class has an <code> optional</code> cancel 
 * method.  The support of the cancel function can 
 * be tested by the isCancelSupported method.
 * </p>
 *
 * <p>The ProgressObject structure allows the
 * user the option of polling for status or to
 * provide a callback.
 * </p>
 */
public interface ProgressObject
{

   /**
    * Retrieve the status of this activity.
    *
    * @return An object containing the status
    *          information.
    */
   public DeploymentStatus getDeploymentStatus();


   /**
    * Retrieve the list of TargetModuleIDs successfully
    * processed or created by the associated DeploymentManager
    * operation. 
    *
    * @return a list of TargetModuleIDs.
    */
	public TargetModuleID [] getResultTargetModuleIDs();


   /**
    * Return the ClientConfiguration object associated with the
    * TargetModuleID.
    *
    * @return ClientConfiguration for a given TargetModuleID or 
    *         null if none exists.
    */
    public ClientConfiguration getClientConfiguration(TargetModuleID id); 


   /**
    * Tests whether the vendor supports a cancel 
    * opertation for deployment activities.
    *
    * @return <code>true</code> if canceling an
    *         activity is supported by this platform.
    */
   public boolean isCancelSupported();

   /**
    * (optional)
    * A cancel request on an in-process operation 
    * stops all further processing of the operation and returns
    * the environment to it original state before the operation
    * was executed.  An operation that has run to completion 
    * cannot be cancelled.
    *
    * @throws OperationUnsupportedException this optional command
    *         is not supported by this implementation.
    */
   public void cancel() throws OperationUnsupportedException;

   /**
    * Tests whether the vendor supports a stop
    * opertation for deployment activities.
    *
    * @return <code>true</code> if canceling an
    *         activity is supported by this platform.
    */
   public boolean isStopSupported();

   /**
    * (optional)
    * A stop request on an in-process operation allows the 
    * operation on the current TargetModuleID to run to completion but 
    * does not process any of the remaining unprocessed TargetModuleID 
    * objects.  The processed TargetModuleIDs must be returned by the 
    * method getResultTargetModuleIDs.
    *
    * @throws OperationUnsupportedException this optional command
    *         is not supported by this implementation.
    */
   public void stop() throws OperationUnsupportedException;

   /**
    * Add a listener to receive Progress events on deployment
    * actions.
    *
    * @param pol the listener to receive events
    * @see ProgressEvent 
    */
   public void addProgressListener(ProgressListener pol);

   /**
    * Remove a ProgressObject listener.
    *
    * @param pol the listener being removed
    * @see ProgressEvent 
    */
   public void removeProgressListener(ProgressListener pol);
}
