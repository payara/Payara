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
// Portions Copyright [2019] Payara Foundation and/or affiliates
package com.sun.enterprise.server.logging;

import com.sun.common.util.logging.LoggingConfig;
import com.sun.common.util.logging.LoggingConfigFactory;
import com.sun.enterprise.config.serverbeans.Domain;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Handler;
import org.glassfish.api.admin.FileMonitoring;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.config.UnprocessedConfigListener;
import org.glassfish.server.ServerEnvironmentImpl;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.*;

/**
 *
 * @author patrik
 */
public class LogManagerServiceTest {

    private static PrintStream origOut;
    private static PrintStream origErr;
    
    @BeforeClass
    public static void saveStreams() {
        origOut = System.out;
        origErr = System.err;
    }
    
    @Before
    public void resetStreams() {
        System.setOut(origOut);
        System.setErr(origErr);
    }
    
    @Test
    public void by_default_std_streams_redirected() throws IOException {
        LogManagerService service = prepareService(prepareProperties());
        service.postConstruct();
        
        assertNotSame("System.out should be redirected", System.out, origOut);
        assertNotSame("System.err should be redirected", System.err, origErr);
    }
    
    @Test
    public void with_logging_prop_std_streams_not_redirected() throws IOException {
        final Map<String, String> props = prepareProperties();
        props.put("fish.payara.enterprise.server.logging.stdio.disable", "true");
        LogManagerService service = prepareService(props);
        service.postConstruct();
        
        assertSame("System.out should not be redirected", System.out, origOut);
        assertSame("System.err should not be redirected", System.err, origErr);
    }    
    
    private LogManagerService prepareService(Map<String, String> stringProps) throws IOException, MultiException {
        LogManagerService manager = new LogManagerService();
        manager.env = new ServerEnvironmentImpl(new File("src/test/domain"));
        manager.habitat = mock(ServiceLocator.class);
        manager.fileMonitoring = mock(FileMonitoring.class);
        manager.loggingConfigFactory = mock(LoggingConfigFactory.class);
        manager.ucl = new UnprocessedConfigListener();
        manager.domain = mock(Domain.class);
        when(manager.habitat.getAllServices(Handler.class)).thenReturn(Collections.emptyList());
        
        LoggingConfig conf = mock(LoggingConfig.class);
        when(conf.getLoggingProperties()).thenReturn(stringProps);
        when(manager.loggingConfigFactory.provide()).thenReturn(conf);
        return manager;
    }

    private Map<String, String> prepareProperties() throws IOException {
        Properties props = new Properties();
        Map<String,String> stringProps = new HashMap<>();
        props.load(new FileReader("src/test/domain/config/logging.properties"));
        props.forEach((k,v) -> stringProps.put((String)k, (String)v));
        return stringProps;
    }
}
