/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.api.admin.progress;

import org.glassfish.api.admin.ProgressStatus;

/** Change some value in ProgressStatus using set method.
 *
 * @author martinmares
 */
public class ProgressStatusEventSet extends ProgressStatusEvent {
    
    private Integer totalStepCount;
    private Integer currentStepCount;

    public ProgressStatusEventSet(String id, Integer totalStepCount, Integer currentStepCount) {
        super(id);
        this.totalStepCount = totalStepCount;
        this.currentStepCount = currentStepCount;
    }

    public ProgressStatusEventSet(String progressStatusId) {
        super(progressStatusId);
    }
    
    public Integer getTotalStepCount() {
        return totalStepCount;
    }

    public Integer getCurrentStepCount() {
        return currentStepCount;
    }

    public void setTotalStepCount(Integer totalStepCount) {
        this.totalStepCount = totalStepCount;
    }

    public void setCurrentStepCount(Integer currentStepCount) {
        this.currentStepCount = currentStepCount;
    }
    
    @Override
    public ProgressStatus apply(ProgressStatus ps) {
        if (totalStepCount != null) {
            ps.setTotalStepCount(totalStepCount);
        }
        if (currentStepCount != null) {
            ps.setCurrentStepCount(currentStepCount);
        }
        return ps;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 53 * hash + (this.totalStepCount != null ? this.totalStepCount.hashCode() : 0);
        hash = 53 * hash + (this.currentStepCount != null ? this.currentStepCount.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ProgressStatusEventSet other = (ProgressStatusEventSet) obj;
        if ((this.totalStepCount == null) ? (other.totalStepCount != null) : !this.totalStepCount.equals(other.totalStepCount)) {
            return false;
        }
        if ((this.currentStepCount == null) ? (other.currentStepCount != null) : !this.currentStepCount.equals(other.currentStepCount)) {
            return false;
        }
        return true;
    }

    
    
}
