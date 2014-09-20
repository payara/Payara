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
package com.sun.enterprise.admin.progress;

import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.StringUtils;
import java.util.HashMap;
import java.util.Map;
import org.glassfish.api.admin.CommandProgress;
import org.glassfish.api.admin.ProgressStatus;
import org.glassfish.api.admin.progress.ProgressStatusBase;
import org.glassfish.api.admin.progress.ProgressStatusDTO;
import org.glassfish.api.admin.progress.ProgressStatusEvent;
import org.glassfish.api.admin.progress.ProgressStatusEventCreateChild;

/** Provides mirroring of events into given ProgressStatus substructure.
 * Never rewrites name in base ProgressStatus (i.e. only children will have copied
 * names).
 *
 * @author mmares
 */
public class ProgressStatusClient {
    
    private static final LocalStringsImpl strings =
            new LocalStringsImpl(ProgressStatusClient.class);
    
    private ProgressStatus status;
    private final Map<String, ProgressStatus> map = new HashMap<String, ProgressStatus>();

    /** Mirror incoming events and structures into given ProgressStatus.
     * If null, CommandProgess will be created with first event or structure.
     * @param status 
     */
    public ProgressStatusClient(ProgressStatus status) {
        this.status = status;
    }
    
    private synchronized void preventNullStatus(String name, String id) {
        if (status == null) {
            status = new CommandProgressImpl(name, id);
        }
        map.put(id, status);
    }
    
    public synchronized void mirror(ProgressStatusDTO dto) {
        if (dto == null) {
            return;
        }
        preventNullStatus(dto.getName(), dto.getId());
        mirror(dto, status);
    }
    
    private void mirror(ProgressStatusDTO dto, ProgressStatus stat) {
        //TODO: copy-paste problem because of ProgressStatusDTO and ProgressStatusBase we have to create shared interface
        stat.setTotalStepCount(dto.getTotalStepCount());
        stat.setCurrentStepCount(dto.getCurrentStepCount());
        if (dto.isCompleted()) {
            stat.complete();
        }
        for (ProgressStatusDTO.ChildProgressStatusDTO chld : dto.getChildren()) {
            ProgressStatus dst = map.get(chld.getProgressStatus().getId());
            if (dst == null) {
                dst = stat.createChild(chld.getProgressStatus().getName(), chld.getAllocatedSteps());
                map.put(chld.getProgressStatus().getId(), dst);
            }
            mirror(chld.getProgressStatus(), dst);
        }
    }
    
    public synchronized void mirror(ProgressStatusBase source) {
        if (source == null) {
            return;
        }
        preventNullStatus(source.getName(), source.getId());
        mirror(source, status);
    }
    
    private void mirror(ProgressStatusBase source, ProgressStatus stat) {
        stat.setTotalStepCount(source.getTotalStepCount());
        stat.setCurrentStepCount(source.getCurrentStepCount());
        if (source.isComplete()) {
            stat.complete();
        }
        for (ProgressStatusBase.ChildProgressStatus chld : source.getChildProgressStatuses()) {
            ProgressStatus dst = map.get(chld.getProgressStatus().getId());
            if (dst == null) {
                dst = stat.createChild(chld.getProgressStatus().getName(), chld.getAllocatedSteps());
                map.put(chld.getProgressStatus().getId(), dst);
            }
            mirror(chld.getProgressStatus(), dst);
        }
    }
    
    /** Applies event on existing structures. If not appliable do nothing.
     */
    public synchronized void mirror(ProgressStatusEvent event) {
        if (event == null) {
            return;
        }
        ProgressStatus effected = map.get(event.getSourceId());
        ProgressStatus result = event.apply(effected);
        if (event instanceof ProgressStatusEventCreateChild) {
            map.put(((ProgressStatusEventCreateChild) event).getChildId(), result);
        }
    }

    public synchronized ProgressStatus getProgressStatus() {
        return status;
    }
    
    public static String composeMessageForPrint(CommandProgress cp) {
        if (cp == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        synchronized (cp) {
            //Measurements
            int percentage = Math.round(cp.computeCompletePortion() * 100);
            if (percentage >= 0) {
                result.append(percentage);
                switch (result.length()) {
                    case 1:
                        result.insert(0, "  ");
                        break;
                    case 2:
                        result.insert(0, ' ');
                        break;
                    default:
                        break;
                }
                result.append('%');
            } else {
                int sumSteps = cp.computeSumSteps();
                result.append(sumSteps);
            }
            //Message
            String message = cp.getLastMessage();
            if (!StringUtils.ok(message) && StringUtils.ok(cp.getName())) {
                message = strings.getString("progressstatus.message.starting", "Starting");
            }
            if (StringUtils.ok(message)) {
                result.append(": ");
                result.append(message);
            }
        }
        return result.toString();
    }
    
}
