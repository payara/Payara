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

import java.io.Serializable;
import java.util.*;
import org.glassfish.api.admin.CommandProgress;
import org.glassfish.api.admin.ProgressStatus;

/** Basic <i>abstract</i> implementation of {@code ProgressStatus}.
 *
 * @author mmares
 */
//TODO: Move to utils if possible. It is now in API only because ProgressStatusImpl is here, too
public abstract class ProgressStatusBase implements ProgressStatus, Serializable {
    
    private static final long serialVersionUID = -2501719606059507140L;

    public static class ChildProgressStatus implements Serializable {
        
        private final int allocatedSteps;
        private final ProgressStatusBase progressStatus;
        
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

        @Override
        public String toString() {
            return "ChildProgressStatus{" + "allocatedSteps=" + allocatedSteps + ", progressStatus=" + progressStatus + '}';
        }
        
    }
    
    protected String name;
    protected String id;
    protected int totalStepCount = -1;
    protected int currentStepCount = 0;
    protected ProgressStatusBase parent;
    protected boolean completed = false;
    protected Set<ChildProgressStatus> children = new HashSet<ChildProgressStatus>();

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
    
    @Override
    public synchronized void setTotalStepCount(int totalStepCount) {
        if (completed || this.totalStepCount == totalStepCount) {
            return;
        }
        this.totalStepCount = totalStepCount;
        if (totalStepCount >= 0 && this.currentStepCount > totalStepCount) {
            this.currentStepCount = totalStepCount;
        }
        fireEvent(new ProgressStatusEventSet(id, totalStepCount, null));
    }

    @Override
    public synchronized int getTotalStepCount() {
        return totalStepCount;
    }

    @Override
    public int getRemainingStepCount() {
        int childAlocSteps = 0;
        for (ChildProgressStatus childProgressStatus : children) {
            childAlocSteps += childProgressStatus.getAllocatedSteps();
        }
        return totalStepCount - currentStepCount - childAlocSteps;
    }

    @Override
    public synchronized void progress(int steps, String message, boolean spinner) {
        if (completed) {
            return;
        }
        int lastCurrentStepCount = this.currentStepCount;
        if (steps > 0) {
            if (totalStepCount < 0) {
                currentStepCount += steps;
            } else if (currentStepCount < totalStepCount) {
                currentStepCount += steps;
                if (currentStepCount > totalStepCount) {
                    currentStepCount = totalStepCount;
                }
            }
        }
        if (currentStepCount != lastCurrentStepCount || message != null || spinner != getSpinnerStatus()) {
            fireEvent(new ProgressStatusEventProgress(id, steps, message, spinner));
        }
    }
    
    @Override
    public synchronized void progress(int steps, String message) {
        progress(steps, message, false);
    }

    @Override
    public void progress(int steps) {
        progress(steps, null, false);
    }

    @Override
    public void progress(String message) {
        progress(0, message, false);
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
            fireEvent(new ProgressStatusEventSet(id, null, stepCount));
        }
    }

    @Override
    public void complete(String message) {
        if (completeSilently()) {
            fireEvent(new ProgressStatusEventComplete(id, message));
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
        for (ChildProgressStatus child : children) {
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
            for (ChildProgressStatus child : children) {
                allocatedSteps += child.getAllocatedSteps();
            }
            if ((allocatedSteps + currentStepCount) > totalStepCount) {
                currentStepCount = totalStepCount - allocatedSteps;
                if (currentStepCount < 0) {
                    currentStepCount = 0;
                    totalStepCount = allocatedSteps;
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
        children.add(new ChildProgressStatus(allocatedSteps, result));
        fireEvent(new ProgressStatusEventCreateChild(id, name, result.getId(), allocatedSteps, totalStepCount));
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
    
    public synchronized int getCurrentStepCount() {
        return this.currentStepCount;
    }
    
    protected synchronized float computeCompleteSteps() {
        if (isComplete()) {
            return totalStepCount;
        }
        float realStepCount = currentStepCount;
        for (ChildProgressStatus child : children) {
            float childPortion = child.progressStatus.computeCompletePortion();
            if (childPortion < 0) {
                return -1;
            }
            realStepCount += ((float) child.getAllocatedSteps()) * childPortion;
        }
        return realStepCount;
    }
    
    public synchronized float computeCompletePortion() {
        if (isComplete()) {
            return 1;
        }
//        if (totalStepCount < 0) {
//            return -1;
//        }
        float realSteps = computeCompleteSteps();
        if (realSteps < 0) {
            return -1;
        }
        if (realSteps == 0) {
            return 0;
        }
        if (totalStepCount < 0) {
            return -1;
        } else if (totalStepCount > 0) {
            return realSteps / ((float) totalStepCount);
        } else {
            return 1;
        }
    }
    
    public synchronized int computeSumSteps() {
        int result = 0;
        for (ChildProgressStatus child : children) {
            if (child.allocatedSteps > 0) {
                result += child.progressStatus.computeSumSteps();
            }
        }
        return getCurrentStepCount() + result;
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
        Collection<ProgressStatusBase> result = new ArrayList<ProgressStatusBase>(children.size());
        for (ChildProgressStatus chld : children) {
            result.add(chld.getProgressStatus());
        }
        return result;
    }

    public Set<ChildProgressStatus> getChildProgressStatuses() {
        return children;
    }
    
    @Override
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    
    public ProgressStatusBase getParrent() {
        return parent;
    }
    
    /** Recursive search for child by id.
     */
    protected ProgressStatusBase findById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        if (id.equals(getId())) {
            return this;
        }
        for (ChildProgressStatus child : getChildProgressStatuses()) {
            ProgressStatusBase result = child.getProgressStatus().findById(id);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
    
    /** Returns spinner status or null if status was not possible to check.
     */
    private boolean getSpinnerStatus() {
        if (parent == null) {
            return false;
        }
        if (parent instanceof CommandProgress) {
            return ((CommandProgress) parent).isSpinnerActive();
        }
        return ((ProgressStatusBase) parent).getSpinnerStatus();
    }
    
}
