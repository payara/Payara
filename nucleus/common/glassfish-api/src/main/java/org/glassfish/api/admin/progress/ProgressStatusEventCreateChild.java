/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

/** Progress method was called.
 *
 * @author martinmares
 */
public class ProgressStatusEventCreateChild extends ProgressStatusEvent {

    private String childId;
    private String name;
    private int allocatedSteps;
    private int totalSteps;

    public ProgressStatusEventCreateChild(String progressStatusId, String name, String childId, int allocatedSteps, int totalSteps) {
        super(progressStatusId);
        this.name = name;
        this.childId = childId;
        this.allocatedSteps = allocatedSteps;
        this.totalSteps = totalSteps;
    }

    public ProgressStatusEventCreateChild(String progressStatusId) {
        super(progressStatusId);
    }

    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }

    public int getAllocatedSteps() {
        return allocatedSteps;
    }

    public void setAllocatedSteps(int allocatedSteps) {
        this.allocatedSteps = allocatedSteps;
    }

    public int getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(int totalSteps) {
        this.totalSteps = totalSteps;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public ProgressStatus apply(ProgressStatus ps) {
        ProgressStatus chld;
        if (ps instanceof ProgressStatusBase) {
            ProgressStatusBase psb = (ProgressStatusBase) ps;
            chld = psb.createChild(name, allocatedSteps, totalSteps);
        } else {
            chld = ps.createChild(name, allocatedSteps);
            if (totalSteps >= 0) {
                chld.setTotalStepCount(totalSteps);
            }
        }
        return chld;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + (this.childId != null ? this.childId.hashCode() : 0);
        hash = 19 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 19 * hash + this.allocatedSteps;
        hash = 19 * hash + this.totalSteps;
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
        final ProgressStatusEventCreateChild other = (ProgressStatusEventCreateChild) obj;
        if ((this.childId == null) ? (other.childId != null) : !this.childId.equals(other.childId)) {
            return false;
        }
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if (this.allocatedSteps != other.allocatedSteps) {
            return false;
        }
        if (this.totalSteps != other.totalSteps) {
            return false;
        }
        return true;
    }
    
}
