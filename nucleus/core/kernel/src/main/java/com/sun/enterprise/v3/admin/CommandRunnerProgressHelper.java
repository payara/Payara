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
package com.sun.enterprise.v3.admin;

import com.sun.enterprise.v3.admin.progress.CommandProgressImpl;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.SupplementalCommandExecutor.SupplementalCommand;
import org.glassfish.api.admin.progress.ProgressStatusMirroringImpl;

/** Helper class for {@code ProgressStatus} manipulation during 
 * {@code CommandRunner} execution.<br/><br/>
 * <b>Life cycle:</b><br/>
 * <ul>
 *   <li>Constructs</li>
 *   <li>setReplicationCount</li>
 *   <li>addProgressStatusToSupplementalCommand <i>(optional)</i></li>
 *   <li>wrapContext4MainCommand</li>
 *   <li><i>do replication</i></li>
 *   <li>complete</li>
 * </ul>
 * 
 * @author mmares
 */
class CommandRunnerProgressHelper {
    
    //From constructor
    private AdminCommand command;
    private Progress progressAnnotation;
    private CommandProgressRegistry registry;
    private CommandProgressImpl commandProgress;
    private int replicationCount = 0;

    //Changed during lifecycle 
    private ProgressStatus progressForMainCommand = null;
    private ProgressStatusMirroringImpl progressMirroring = null;
    
    public CommandRunnerProgressHelper(AdminCommand command, String name, CommandProgressRegistry registry) {
        this.command = command;
        this.registry = registry;
        progressAnnotation = command.getClass().getAnnotation(Progress.class);
        if (progressAnnotation != null) {
            if (progressAnnotation.name() == null || progressAnnotation.name().isEmpty()) {
                commandProgress = new CommandProgressImpl(name);
            } else {
                commandProgress = new CommandProgressImpl(progressAnnotation.name());
            }
            registry.registr(commandProgress);
        }
    }

    public int getReplicationCount() {
        return replicationCount;
    }

    public void setReplicationCount(int replicationCount) {
        this.replicationCount = replicationCount;
    }
    
    public void addProgressStatusToSupplementalCommand(SupplementalCommand supplemental) {
        if (commandProgress == null || supplemental == null) {
            return;
        }
        if (progressForMainCommand != null && progressMirroring == null) {
            throw new IllegalStateException("Suplmenetal commands must be filled with ProgressStatus before main command!");
        }
        if (replicationCount < 0) {
            throw new IllegalStateException("Replication count must be provided first");
        }
        if (supplemental.getProgressAnnotation() != null) {
            if (progressMirroring == null) {
                commandProgress.setTotalStepCount(replicationCount + 1);
                progressMirroring = commandProgress.createMirroringChild(1);
                progressForMainCommand = progressMirroring.createChild(null, 0, progressAnnotation.totalStepCount());
            }
            supplemental.setProgressStatus(progressMirroring.createChild(supplemental.getProgressAnnotation().name(),
                    0, supplemental.getProgressAnnotation().totalStepCount()));
        }
    }
    
    public AdminCommandContext wrapContext4MainCommand(AdminCommandContext context) {
        if (progressForMainCommand != null) {
            return new AdminCommandContextForInstance(context, progressForMainCommand);
        }
        if (commandProgress != null) {
            if (replicationCount > 0) {
                commandProgress.setTotalStepCount(replicationCount + 1);
                progressForMainCommand = commandProgress.createChild(null, 1, progressAnnotation.totalStepCount());
            } else {
                commandProgress.setTotalStepCount(progressAnnotation.totalStepCount());
                progressForMainCommand = commandProgress;
            }
            return new AdminCommandContextForInstance(context, progressForMainCommand);
        }
        return context;
    }
    
    public void complete(AdminCommandContext context) {
        if (commandProgress != null) {
            commandProgress.complete(context.getActionReport(), context.getOutboundPayload());
        }
    }
    
}
