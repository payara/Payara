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

package com.sun.enterprise.deployment.runtime;

import org.glassfish.deployment.common.Descriptor;
import com.sun.enterprise.deployment.DescriptorConstants;

/** iAS specific DD Element (see the ias-ejb-jar_2_0.dtd for this element)
 * @author Ludo
 * @since JDK 1.4
 */
public class BeanPoolDescriptor extends Descriptor implements DescriptorConstants {
       
    private int maxPoolSize = MAX_POOL_SIZE_DEFAULT;
    private int poolIdleTimeoutInSeconds = POOL_IDLE_TIMEOUT_DEFAULT;
    private int maxWaitTimeInMillis = MAX_WAIT_TIME_DEFAULT;
    private int poolResizeQuantity = POOL_RESIZE_QTY_DEFAULT;
    private int steadyPoolSize = STEADY_POOL_SIZE_DEFAULT;

    
    /** Default constructor. */
    public BeanPoolDescriptor() {
    }

     /** Getter for property poolIdleTimeoutInSeconds.
     * @return Value of property idleTimeoutInSeconds.
     */
    public int getPoolIdleTimeoutInSeconds() {
        return poolIdleTimeoutInSeconds;
    }    
    
    /** Setter for property poolIdleTimeoutInSeconds.
     * @param poolIdleTimeoutInSeconds New value of property poolIdleTimeoutInSeconds.
     */
    public void setPoolIdleTimeoutInSeconds(int poolIdleTimeoutInSeconds) {
        this.poolIdleTimeoutInSeconds = poolIdleTimeoutInSeconds;
    }
    
    /** Getter for property maxPoolSize.
     * @return Value of property maxPoolSize.
     */
    public int getMaxPoolSize() {
        return maxPoolSize;
    }
    
    /** Setter for property maxPoolSize.
     * @param maxPoolSize New value of property maxPoolSize.
     */
    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }
    
    /** Getter for property maxWaitTimeInMillis.
     * @return Value of property maxWaitTimeInMillis.
     */
    public int getMaxWaitTimeInMillis() {
        return maxWaitTimeInMillis;
    }
    
    /** Setter for property maxWaitTimeInMillis.
     * @param maxWaitTimeInMillis New value of property maxWaitTimeInMillis.
     */
    public void setMaxWaitTimeInMillis(int maxWaitTimeInMillis) {
        this.maxWaitTimeInMillis = maxWaitTimeInMillis;
    }
    
    /** Getter for property poolResizeQuantity
     * @return Value of property poolResizeQuantity.
     */
    public int getPoolResizeQuantity() {
        return poolResizeQuantity;
    }
    
    /** Setter for property poolResizeQuantity.
     * @param poolResizeQuantity New value of property poolResizeQuantity.
     */
    public void setPoolResizeQuantity(int poolResizeQuantity) {
        this.poolResizeQuantity = poolResizeQuantity;
    }   
    
    /** Getter for property steadyPoolSize
    * @return Value of property steadyPoolSize.
    */
    public int getSteadyPoolSize() {
        return steadyPoolSize;
    }
  
    /** Setter for property steadyPoolSize.
     * @param steadyPoolSize New value of property steadyPoolSize.
     */
    public void setSteadyPoolSize(int steadyPoolSize) {
        this.steadyPoolSize = steadyPoolSize;
    }
}

