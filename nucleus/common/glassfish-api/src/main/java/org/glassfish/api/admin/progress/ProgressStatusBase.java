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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.glassfish.api.admin.ProgressStatus;

/** Basic <i>abstract</i> implementation of {@code ProgressStatus}.
 *
 * @author mmares
 */
//TODO: Move to kernel if possible. It is now in API only because ProgressStatusImpl is here, too
public abstract class ProgressStatusBase implements ProgressStatus {
    
    public class ChildProgressStatus {
        
        private int allocatedSteps;
        private ProgressStatusBase progressStatus;
        
        public ChildProgressStatus(int allocatedSteps, ProgressStatusBase progressStatus) {
            if (allocatedSteps > 0) {
                this.allocatedSteps = allocatedSteps;
            } else {
                this.allocatedSteps = 0;
            }
            this.progressStatus = progressStatus;
        }

        public int getAllocatedSteps() {
            return allocatedSteps;
        }

        public ProgressStatusBase getProgressStatus() {
            return progressStatus;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ChildProgressStatus other = (ChildProgressStatus) obj;
            if (this.allocatedSteps != other.allocatedSteps) {
                return false;
            }
            if (this.progressStatus != other.progressStatus && (this.progressStatus == null || !this.progressStatus.equals(other.progressStatus))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return this.progressStatus != null ? this.progressStatus.hashCode() : 0;
        }
        
    }
    
    protected String name;
    protected String id;
    protected int totalStepCount = -1;
    protected int currentStepCount = 0;
    private ProgressStatusBase parent;
    private boolean completed = false;
    protected Set<ChildProgressStatus> childs = new HashSet<ChildProgressStatus>();

    /** Construct unnamed {@code ProgressStatus}
     * 
     * @param parent Parent {@code ProgressStatus}
     * @param id Is useful for event transfer
     */ 
    protected ProgressStatusBase(ProgressStatusBase parent, String id) {
        this(null, -1, parent, id);
    }
    
    /** Construct named {@code ProgressStatus}.
     * 
     * @param name of the {@code ProgressStatus} implementation is used 
     *        to identify source of progress messages.
     * @param parent Parent {@code ProgressStatus}
     * @param id Is useful for event transfer
     */
    protected ProgressStatusBase(String name, ProgressStatusBase parent, String id) {
        this(name, -1, parent, id);
    }
    
    /** Construct named {@code ProgressStatus} with defined expected count 
     * of steps.
     * 
     * @param name of the {@code ProgressStatus} implementation is used 
     *        to identify source of progress messages.
     * @param totalStepCount How many steps are expected in this 
     *        {@code ProgressStatus}
     * @param parent Parent {@code ProgressStatus}
     * @param id Is useful for event transfer
     */
    protected ProgressStatusBase(String name, int totalStepCount, 
            ProgressStatusBase parent, String id) {
        this.name = name;
        this.totalStepCount = totalStepCount;
        this.parent = parent;
        if (id == null || id.isEmpty()) {
            id = UUID.randomUUID().toString();
        }
        this.id = id;
    } 
    
    /** Fires {@link ProgressStatusEvent} to parent.
     */
    protected void fireEvent(ProgressStatusEvent event) {
        if (parent != null) {
            parent.fireEvent(event);
        }
    }
    
    protected final void fireEvent(String message, ProgressStatusEvent.Changed... changed) {
        fireEvent(new ProgressStatusEvent(this, message, changed));
    }
    
    @Override
    public synchronized void setTotalStepCount(int totalStepCount) {
        if (completed || this.totalStepCount == totalStepCount) {
            return;
        }
        this.totalStepCount = totalStepCount;
        if (totalStepCount >= 0 && this.currentStepCount > totalStepCount) {
            this.currentStepCount = totalStepCount;
        }
        fireEvent(null, ProgressStatusEvent.Changed.TOTAL_STEPS);
    }

    @Override
    public int getTotalStepCount() {
        return totalStepCount;
    }

    @Override
    public int getRemainingStepCount() {
        int childAlocSteps = 0;
        for (ChildProgressStatus childProgressStatus : childs) {
            childAlocSteps += childProgressStatus.getAllocatedSteps();
        }
        return totalStepCount - currentStepCount - childAlocSteps;
    }

    @Override
    public synchronized void progress(int steps, String message) {
        if (completed) {
            return;
        }
        boolean stepsChanged = false;
        if (steps > 0) {
            if (totalStepCount < 0) {
                currentStepCount += steps;
                stepsChanged = true;
            } else if (currentStepCount < totalStepCount) {
                currentStepCount += steps;
                if (currentStepCount > totalStepCount) {
                    currentStepCount = totalStepCount;
                }
                stepsChanged = true;
            }
        }
        if (stepsChanged || (message != null && !message.isEmpty())) {
            fireEvent(message, stepsChanged ? ProgressStatusEvent.Changed.STEPS : null);
        }
    }

    @Override
    public void progress(int steps) {
        progress(steps, null);
    }

    @Override
    public void progress(String message) {
        progress(0, message);
    }

    @Override
    public synchronized void setCurrentStepCount(int stepCount) {
        if (completed) {
            return;
        }
        boolean stepsChanged = false;
        if (stepCount >= 0 && stepCount != currentStepCount) {
            if (totalStepCount < 0 || stepCount < totalStepCount) {
                currentStepCount = stepCount;
                stepsChanged = true;
            } else if (currentStepCount != totalStepCount) {
                currentStepCount = totalStepCount;
                stepsChanged = true;
            }
        }
        if (stepsChanged) {
            fireEvent(null, ProgressStatusEvent.Changed.STEPS);
        }
    }

    @Override
    public void complete(String message) {
        if (completeSilently()) {
            fireEvent(message, ProgressStatusEvent.Changed.COMPLETED);
        }
    }
    
    /** Complete this {@code ProgressStatus} and all sub-ProgressStatuses 
     * but does not fire event to parent statuses.
     */
    protected synchronized boolean completeSilently() {
        if (completed) {
            return false;
        }
        if (totalStepCount >= 0) {
            currentStepCount = totalStepCount;
        }
        completed = true;
        for (ChildProgressStatus child : childs) {
            child.getProgressStatus().completeSilently();
        }
        return true;
    }

    @Override
    public void complete() {
        complete(null);
    }

    @Override
    public boolean isComplete() {
        return completed;
    }
    
    protected abstract ProgressStatusBase doCreateChild(String name, int totalStepCount);
    
    protected void allocateStapsForChildProcess(int allocatedSteps) {
        if (allocatedSteps < 0) {
            allocatedSteps = 0;
        }
        if (totalStepCount >= 0) {
            for (ChildProgressStatus child : childs) {
                allocatedSteps += child.getAllocatedSteps();
            }
            if ((allocatedSteps + currentStepCount) > totalStepCount) {
                currentStepCount = totalStepCount - allocatedSteps;
                if (currentStepCount < 0) {
                    currentStepCount = 0;
                    totalStepCount = allocatedSteps;
                    fireEvent(null, ProgressStatusEvent.Changed.STEPS, ProgressStatusEvent.Changed.TOTAL_STEPS);
                } else {
                    fireEvent(null, ProgressStatusEvent.Changed.STEPS);
                }
            }
        }
    }
    
    public synchronized ProgressStatus createChild(String name, 
            int allocatedSteps, int totalStepCount) {
        if (allocatedSteps < 0) {
            allocatedSteps = 0;
        }
        allocateStapsForChildProcess(allocatedSteps);
        ProgressStatusBase result = doCreateChild(name, totalStepCount);
        childs.add(new ChildProgressStatus(allocatedSteps, result));
        fireEvent(new ProgressStatusEvent(result, allocatedSteps));
        return result;
    }

    @Override
    public ProgressStatus createChild(String name, int allocatedSteps) {
        return createChild(name, allocatedSteps, -1);
    }

    @Override
    public ProgressStatus createChild(int allocatedSteps) {
        return createChild(null, allocatedSteps);
    }
    
    protected int getCurrentStepCount() {
        return this.currentStepCount;
    }
    
    protected synchronized float computeCompleteSteps() {
        if (isComplete()) {
            return totalStepCount;
        }
        float realStepCount = currentStepCount;
        for (ChildProgressStatus child : childs) {
            float childPortion = child.progressStatus.computeCompletePortion();
            if (childPortion < 0) {
                return -1;
            }
            realStepCount += ((float) child.getAllocatedSteps()) * child.progressStatus.computeCompletePortion();
        }
        return realStepCount;
    }
    
    protected synchronized float computeCompletePortion() {
        if (totalStepCount < 0) {
            return -1;
        }
        float realSteps = computeCompleteSteps();
        if (realSteps < 0) {
            return -1;
        }
        if (totalStepCount > 0) {
            return realSteps / ((float) totalStepCount);
        } else {
            return 1;
        }
    }
    
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        if (name == null) {
            result.append("NoName ");
        } else {
            result.append(name).append(' ');
        }
        float realSteps = computeCompleteSteps();
        if (realSteps < 0) {
            result.append(currentStepCount).append(" / ").append('?');
        } else {
            result.append(Math.round(realSteps)).append(" / ");
            result.append(totalStepCount < 0 ? "?" : String.valueOf(totalStepCount));
        }
        
        return result.toString();
    }
    
    public synchronized Collection<ProgressStatusBase> getChildren() {
        Collection<ProgressStatusBase> result = new ArrayList<ProgressStatusBase>(childs.size());
        for (ChildProgressStatus chld : childs) {
            result.add(chld.getProgressStatus());
        }
        return result;
    }
    
}
