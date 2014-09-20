/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ejb.deployment.descriptor.runtime;

import com.sun.enterprise.deployment.DescriptorConstants;
import org.glassfish.deployment.common.Descriptor;

/** iAS specific DD Element (see the ias-ejb-jar_2_0.dtd for this element)
 * @author Ludo
 * @since JDK 1.4
 */
public class BeanCacheDescriptor extends Descriptor implements DescriptorConstants{

        private Boolean isCacheOverflowAllowed;       
        private String victimSelectionPolicy;
        
        //initialized default values for class variables
        private int maxCacheSize = MAX_CACHE_SIZE_DEFAULT;
        private int resizeQuantity = RESIZE_QUANTITY_DEFAULT;
        private int cacheIdleTimeoutInSeconds = CACHE_IDLE_TIMEOUT_DEFAULT;
        private int removalTimeoutInSeconds = REMOVAL_TIMEOUT_DEFAULT; 
        
        /** Default constructor. */
	public BeanCacheDescriptor() {
	}

        /** 
         * Getter for property cacheIdleTimeoutInSeconds.
         * @return Value of property cacheIdleTimeoutInSeconds.
         */
        public int getCacheIdleTimeoutInSeconds() {
            return cacheIdleTimeoutInSeconds;
        }        
        
        /** 
         * Setter for property cacheIdleTimeoutInSeconds.
         * @param cacheIdleTimeoutInSeconds New value of property cacheIdleTimeoutInSeconds.
         */

        public void setCacheIdleTimeoutInSeconds(int cacheIdleTimeoutInSeconds) {
            this.cacheIdleTimeoutInSeconds = cacheIdleTimeoutInSeconds;
        }
        
        /** 
         * Getter for property isCacheOverflowAllowed.
         * @return Value of property isCacheOverflowAllowed.
         */
        public Boolean isIsCacheOverflowAllowed() {
            return isCacheOverflowAllowed;
        }
        
        /** 
         * Setter for property isCacheOverflowAllowed.
         * @param isCacheOverflowAllowed New value of property isCacheOverflowAllowed.
         */
        public void setIsCacheOverflowAllowed(boolean isCacheOverflowAllowed) {
            this.isCacheOverflowAllowed =  Boolean.valueOf(isCacheOverflowAllowed);
        }
        
        /** 
         * Setter for property isCacheOverflowAllowed.
         * @param isCacheOverflowAllowed New value of property isCacheOverflowAllowed.
         */
        public void setIsCacheOverflowAllowed(Boolean isCacheOverflowAllowed) {
            this.isCacheOverflowAllowed =  isCacheOverflowAllowed;
        }

        /** 
         * Getter for property maxCacheSize.
         * @return Value of property maxCacheSize.
         */
        public int getMaxCacheSize() {
            return maxCacheSize;
        }
        
        /** 
         * Setter for property maxCacheSize.
         * @param maxCacheSize New value of property maxCacheSize.
         */
        public void setMaxCacheSize(int maxCacheSize) {
            this.maxCacheSize = maxCacheSize;
        }
        
        /** 
         * Getter for property resizeQuantity.
         * @return Value of property resizeQuantity.
         */
        public int getResizeQuantity() {
            return resizeQuantity;
        }
        
        /** 
         * Setter for property resizeQuantity.
         * @param resizeQuantity New value of property resizeQuantity.
         */
        public void setResizeQuantity(int resizeQty) {
            this.resizeQuantity = resizeQty;
        }
        
        /** 
         * Getter for property removalTimeoutInSeconds.
         * @return Value of property removalTimeoutInSeconds.
         */
        public int getRemovalTimeoutInSeconds() {
            return removalTimeoutInSeconds;
        }
        
        /** 
         * Setter for property removalTimeoutInSeconds.
         * @param removalTimeoutInSeconds New value of property removalTimeoutInSeconds.
         */
        public void setRemovalTimeoutInSeconds(int removalTimeoutInSeconds) {
            this.removalTimeoutInSeconds = removalTimeoutInSeconds;
        }
        
        /** 
         * Getter for property victimSelectionPolicy.
         * @return Value of property victimSelectionPolicy.
         */
        public java.lang.String getVictimSelectionPolicy() {
            return victimSelectionPolicy;
        }
        
        /** 
         * Setter for property victimSelectionPolicy.
         * @param victimSelectionPolicy New value of property victimSelectionPolicy.
         */
        public void setVictimSelectionPolicy(java.lang.String victimSelectionPolicy) {
            this.victimSelectionPolicy = victimSelectionPolicy;
        }
}
