/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.cli.cluster;

import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.io.*;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandValidationException;
import com.sun.enterprise.admin.servermgmt.cli.LocalServerCommand;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author bnevins
 */
public class LocalInstanceCommandTest extends LocalInstanceCommand{

    public LocalInstanceCommandTest() {
    }


    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        me = new LocalInstanceCommandTest();
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of validate method, of class LocalInstanceCommand.
     */
    @Test
    public void testValidate() throws Exception {
        System.out.println("test LocalInstanceCommand.validate");
        try {
            nodeDir = nodeAgentsDir.getAbsolutePath();
            instanceName = "i1";
            isCreateInstanceFilesystem = true;
            validate();
        }
        catch(CommandException e) {
            fail("validate failed!!!");
            throw e;
        }
    }

    @Override
    protected int executeCommand() throws CommandException, CommandValidationException {
        System.out.println("Do nothing!");
        return 0;
    }

    private LocalInstanceCommandTest me;
    private static File installDir;
    private static File nodeAgentsDir;

    static {
        String installDirPath = LocalInstanceCommandTest.class.getClassLoader().getResource("fake_gf_install_dir").getPath();
        installDir = SmartFile.sanitize(new File(installDirPath));
        nodeAgentsDir = new File(installDir, "nodes");
    }
}
