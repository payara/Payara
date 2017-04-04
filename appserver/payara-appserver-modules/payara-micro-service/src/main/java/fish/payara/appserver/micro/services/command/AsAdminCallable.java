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

import fish.payara.appserver.micro.services.PayaraInstanceImpl;
import fish.payara.micro.ClusterCommandResult;
import java.io.Serializable;
import java.util.concurrent.Callable;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;

/**
 *
 * @author steve
 */
public class AsAdminCallable implements Callable<ClusterCommandResult>, Serializable {
    private static final long serialVersionUID = 1L;
    
    private final String command;
    private final String args[];

    public AsAdminCallable(String command, String... args) {
        this.command = command;
        this.args = args;
    }


    @Override
    public ClusterCommandResult call() throws Exception {
        ServiceLocator locator = Globals.getDefaultBaseServiceLocator();
        PayaraInstanceImpl instance = locator.getService(PayaraInstanceImpl.class, "payara-instance");
        return instance.executeLocalAsAdmin(command, args);
    }
    
}
