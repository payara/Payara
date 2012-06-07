/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.v3.server;


import com.sun.enterprise.util.Result;
import org.glassfish.api.FutureProvider;
import org.glassfish.api.Startup;
import org.glassfish.api.StartupRunLevel;
import org.jvnet.hk2.annotations.Priority;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Inhabitant;
import org.jvnet.hk2.component.RunLevelService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;


/**
 * Provides a bridge from {@link Startup} to the {@link RunLevelService} based approach.
 * Also implements {@link FutureProvider} to provide a list of {@link Future futures} for
 * startup service inhabitant activation.
 * 
 * @author Jeff Trent
 * @author Tom Beerbower
 */
@SuppressWarnings("deprecation")
@Priority(2) // run early
@StartupRunLevel
@Service
public class StartupRunLevelBridge extends RunLevelBridge implements FutureProvider<Result<Thread>>{

    // ----- data members ----------------------------------------------------
    /**
     * List of futures obtained from services that implement {@link FutureProvider}.
     */
    private final ArrayList<Future<Result<Thread>>> futures = new ArrayList<Future<Result<Thread>>>();


    // ----- Constructors ----------------------------------------------------

    public StartupRunLevelBridge() {
        super(Startup.class);
    }

    public StartupRunLevelBridge(Class additionalShutdownClass) {
        super(Startup.class, additionalShutdownClass);
    }


    // ----- RunLevelBridge overrides ----------------------------------------

    @Override
    protected void activate(Inhabitant<?> i) {
        Object service = i.get();

        if (service instanceof FutureProvider) {
            futures.addAll(((FutureProvider) service).getFutures());
        }
    }


    // ----- FutureProvider --------------------------------------------------

    @Override
    public List<Future<Result<Thread>>> getFutures() {
        return futures;
    }
}
