/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashSet;
import java.util.Set;

/** 
 *
 * @author mmares
 */
public class ProgressStatusDTO {
    
    public static class ChildProgressStatusDTO {
        
        private final int allocatedSteps;
        private final ProgressStatusDTO progressStatus;

        public ChildProgressStatusDTO(int allocatedSteps, ProgressStatusDTO progressStatus) {
            this.allocatedSteps = allocatedSteps;
            this.progressStatus = progressStatus;
        }

        public int getAllocatedSteps() {
            return allocatedSteps;
        }

        public ProgressStatusDTO getProgressStatus() {
            return progressStatus;
        }

        @Override
        public String toString() {
            return "ChildProgressStatusDTO{" + "allocatedSteps=" + allocatedSteps + ", progressStatus=" + progressStatus + '}';
        }
        
    }
    
    protected String name;
    protected String id;
    protected int totalStepCount = -1;
    protected int currentStepCount = 0;
    protected boolean completed = false;
    protected Set<ChildProgressStatusDTO> children = new HashSet<ChildProgressStatusDTO>();

    public ProgressStatusDTO() {
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int getCurrentStepCount() {
        return currentStepCount;
    }

    public void setCurrentStepCount(int currentStepCount) {
        this.currentStepCount = currentStepCount;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if ("null".equals(name)) {
            this.name = null; //TODO: Debug to find out where is time to time "null" from
        }
    }

    public int getTotalStepCount() {
        return totalStepCount;
    }

    public void setTotalStepCount(int totalStepCount) {
        this.totalStepCount = totalStepCount;
    }

    public Set<ChildProgressStatusDTO> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        return "ProgressStatusDTO{" + "name=" + name + ", id=" + id + ", totalStepCount=" + totalStepCount + ", currentStepCount=" + currentStepCount + ", completed=" + completed + ", children=" + children + '}';
    }
    
    
    
}
