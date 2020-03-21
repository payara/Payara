/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2020] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.server.logging;

import com.sun.enterprise.admin.monitor.callflow.Agent;
import fish.payara.logging.jul.PayaraLogHandler;
import fish.payara.logging.jul.PayaraLogManager;
import java.io.File;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

/**
 * GFFileHandler publishes formatted log Messages to a FILE.
 *
 * @author Jerome Dochez
 * @author Carla Mott
 * @author David Matejcek
 */
@Service
@Singleton
@ContractsProvided({GFFileHandler.class, LoggingRuntime.class})
public class GFFileHandler implements LoggingRuntime {

    @Inject
    @Optional
    private Agent agent;

    @Override
    public File getCurrentLogFile() {
        final PayaraLogHandler payaraLogHandler = getLogHandler();
        return payaraLogHandler == null ? null : payaraLogHandler.getConfiguration().getLogFile();
    }


    public void rotate() {
        final PayaraLogHandler payaraLogHandler = getLogHandler();
        if (payaraLogHandler != null) {
            payaraLogHandler.rotate();
        }
    }


    private PayaraLogHandler getLogHandler() {
        if (!PayaraLogManager.isPayaraLogManager()) {
            throw new UnsupportedOperationException(
                "Rotation is not supported when using other than " + PayaraLogManager.class.getName());
        }
        return PayaraLogManager.getLogManager().getPayaraLogHandler();
    }
}
