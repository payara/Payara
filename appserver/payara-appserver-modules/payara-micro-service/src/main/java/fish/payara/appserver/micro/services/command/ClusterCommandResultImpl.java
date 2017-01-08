/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.appserver.micro.services.command;

import fish.payara.micro.ClusterCommandResult;
import org.glassfish.embeddable.CommandResult;

/**
 *
 * @author steve
 */
public class ClusterCommandResultImpl implements ClusterCommandResult {
    private static final long serialVersionUID = 1L;
    
    private final ClusterCommandResult.ExitStatus status;
    private final String output;
    private final Throwable failureCause;
    
    public ClusterCommandResultImpl(CommandResult result) {
        status = ClusterCommandResult.ExitStatus.valueOf(result.getExitStatus().name());
        output = result.getOutput();
        failureCause = result.getFailureCause();
    }

    public ClusterCommandResultImpl(ExitStatus status, String output, Throwable failureCause) {
        this.status = status;
        this.output = output;
        this.failureCause = failureCause;
    }
    
    @Override
    public ExitStatus getExitStatus() {
        return status;
    }
    
    @Override
    public String getOutput() {
        return output;
    }

    @Override
    public Throwable getFailureCause() {
        return failureCause;
    }
    
}
