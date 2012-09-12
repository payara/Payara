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
import java.util.List;

/** {@code ProgressStatus} is changed
 *
 * @author mmares
 */
//TODO: Move to AdminUtil if possible. It is now in API only because ProgressStatusImpl is here, too
public class ProgressStatusEvent {
    
    public enum Changed {
        NEW_CHILD, STEPS, TOTAL_STEPS, COMPLETED;
    }
    
    private final ProgressStatusDTO source;
    private final String parentSourceId;
    private final List<Changed> changed;
    private final String message;
    private final int allocatedSteps; //For new child only

    private ProgressStatusEvent(ProgressStatusBase source, String message, int allocatedSteps, Changed... changed) {
        this.source = base2DTO(source);
        this.changed = new ArrayList<Changed>();
        if (changed != null) {
            for (Changed chng : changed) {
                if (chng != null) {
                    this.changed.add(chng);
                }
            }
        }
        this.message = message;
        this.allocatedSteps = allocatedSteps;
        ProgressStatusBase parrent = source.getParrent();
        if (parrent != null) {
            this.parentSourceId = source.getParrent().getId();
        } else {
            this.parentSourceId = null;
        }
    }
    
    public ProgressStatusEvent(ProgressStatusBase source, String message, Changed... changed) {
        this(source, message, 0, changed);
    }

    /** Constructor only for {@code Changed.NEW_CHILD}
     */
    public ProgressStatusEvent(ProgressStatusBase source, int allocatedSteps) {
        this(source, null, allocatedSteps, new Changed[] {Changed.NEW_CHILD});
    }

    public ProgressStatusEvent(ProgressStatusDTO source, String parentSourceId, String message, int allocatedSteps, Changed... changed) {
        this.source = source;
        this.parentSourceId = parentSourceId;
        this.changed = new ArrayList<Changed>();
        if (changed != null) {
            for (Changed chng : changed) {
                if (chng != null) {
                    this.changed.add(chng);
                }
            }
        }
        this.message = message;
        this.allocatedSteps = allocatedSteps;
    }
    
    private static ProgressStatusDTO base2DTO(ProgressStatusBase source) {
        if (source == null) {
            return null;
        }
        ProgressStatusDTO result = new ProgressStatusDTO();
        result.setId(source.getId());
        result.setName(source.getName());
        result.setTotalStepCount(source.getTotalStepCount());
        result.setCurrentStepCount(source.getCurrentStepCount());
        result.setCompleted(source.isComplete());
        return result;
    }

    public int getAllocatedSteps() {
        return allocatedSteps;
    }

    public List<Changed> getChanged() {
        return changed;
    }

    public String getMessage() {
        return message;
    }

    public ProgressStatusDTO getSource() {
        return source;
    }

    public String getParentSourceId() {
        return parentSourceId;
    }
    
}
