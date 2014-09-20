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

import java.util.ArrayList;
import java.util.List;
import org.glassfish.api.admin.ProgressStatus;

/** This implementation is used for modeling of command execution with 
 * supplemental commands. It only mirrors all sizes (total steps and current
 * steps from its children.
 * 
 *
 * @author mmares
 */
//TODO: Move to kernel if possible. It is now in API only because ProgressStatusImpl is here, too
public class ProgressStatusMirroringImpl extends ProgressStatusBase {
    
    //private Collection<ProgressStatusBase> mirroreds = new ArrayList<ProgressStatusBase>();
    
    public ProgressStatusMirroringImpl(String name, ProgressStatusBase parent, String id) {
        super(name, -1, parent, id);
    }

    @Override
    protected ProgressStatusBase doCreateChild(String name, int totalStepCount) {
        String childId = (id == null ? "" : id) + "." + (children.size() + 1);
        return new ProgressStatusImpl(name, totalStepCount, this, childId);
    }
    
    @Override
    public void setTotalStepCount(int totalStepCount) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void progress(int steps, String message) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void setCurrentStepCount(int stepCount) {
        throw new UnsupportedOperationException();
    }
    
    /** Ignores allocated steps. It's mirroring implementation.
     */
    @Override
    public synchronized ProgressStatus createChild(String name, 
            int allocatedSteps, int totalStepCount) {
        ProgressStatusBase result = doCreateChild(name, totalStepCount);
        children.add(new ChildProgressStatus(allocatedSteps, result));
        fireEvent(new ProgressStatusEventCreateChild(id, name, result.getId(), 0, totalStepCount));
        return result;
    }
    
    @Override
    protected synchronized void fireEvent(ProgressStatusEvent event) {
        recount();
        super.fireEvent(event);
    }
    
    private void recount() {
        int newTotalStepCount = 0;
        boolean unknown = false;
        int newCurrentStepCount = 0;
        for (ChildProgressStatus child : children) {
            ProgressStatusBase mirr = child.getProgressStatus();
            if (!unknown) {
                int tsc = mirr.getTotalStepCount();
                if (tsc < 0) {
                    unknown = true;
                    newTotalStepCount = -1;
                } else {
                    newTotalStepCount += tsc;
                }
            }
            newCurrentStepCount += mirr.getCurrentStepCount();
        }
        //Event
        ProgressStatusEventSet event = new ProgressStatusEventSet(id);
        if (newCurrentStepCount != currentStepCount) {
            currentStepCount = newCurrentStepCount;
            event.setCurrentStepCount(currentStepCount);
        }
        if (newTotalStepCount != totalStepCount) {
            totalStepCount = newTotalStepCount;
            event.setTotalStepCount(totalStepCount);
        }
        if (event.getCurrentStepCount() != null || event.getTotalStepCount() != null) {
            super.fireEvent(event);
        }
    }
    
    @Override
    protected synchronized float computeCompleteSteps() {
        return currentStepCount;
    }
    
    @Override
    public synchronized float computeCompletePortion() {
        if (isComplete()) {
            return 1;
        }
        if (totalStepCount == 0) {
            if (currentStepCount == 0) {
                return 0;
            } else {
                return 1;
            }
        }
        return ((float) currentStepCount) / ((float) totalStepCount);
    }
    
}
