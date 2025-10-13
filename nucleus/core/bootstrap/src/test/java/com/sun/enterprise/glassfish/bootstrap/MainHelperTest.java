/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

package com.sun.enterprise.glassfish.bootstrap;

/**
 * Created by kokil on 5/18/17.
 */

import static org.junit.Assert.*;
import org.junit.Test;
import com.sun.enterprise.glassfish.bootstrap.MainHelper;
import java.io.*;
import java.util.*;

public class MainHelperTest {

    /* This test is used to test the regex pattern of "parseAsEnv" method of "MainHelper.java".
       It creates two temporary files (asenv.conf and asenv.bat) for testing purpose.
       The "parseAsEnv()" method of "MainHelper.java" reads the "asenv.*" file line by line to generate
       the Properties "asenvProps" whose assertion has been done in this unit test.
    */

    @Test
    public void parseAsEnvTest() {
        try {
            File resources = File.createTempFile("helperTestResources", "config");
            resources.delete();      // delete the temp file
            resources.mkdir();       // reuse the name for a directory
            resources.deleteOnExit();
            File config = new File(resources, "config");
            config.mkdir();
            config.deleteOnExit();
            File asenv_bat = new File(config, "asenv.bat"); //test resource for windows
            File asenv_conf = new File(config, "asenv.conf");//test resource for linux
            asenv_bat.deleteOnExit();
            asenv_conf.deleteOnExit();

            PrintWriter pw1 = new PrintWriter(asenv_bat);
            pw1.println("set AbcVar=value1");
            pw1.println("SET Avar=\"value2\"");
            pw1.println("Set Bvar=\"value3\"");
            pw1.println("set setVar=\"value4\"");
            pw1.println("set SetVar=value5");
            pw1.println("set seVar=\"value6\"");
            pw1.println("set sVar=\"value7\"");
            pw1.close();
            PrintWriter pw2 = new PrintWriter(asenv_conf);
            pw2.println("AbcVar=value1");
            pw2.println("Avar=\"value2\"");
            pw2.println("Bvar=\"value3\"");
            pw2.println("setVar=\"value4\"");
            pw2.println("SetVar=value5");
            pw2.println("seVar=\"value6\"");
            pw2.println("sVar=\"value7\"");
            pw2.close();

            File installRoot = new File(resources.toString());
            Properties asenvProps = MainHelper.parseAsEnv(installRoot);
            assertEquals("value1",asenvProps.getProperty("AbcVar"));
            assertEquals("value2",asenvProps.getProperty("Avar"));
            assertEquals("value3",asenvProps.getProperty("Bvar"));
            assertEquals("value4",asenvProps.getProperty("setVar"));
            assertEquals("value5",asenvProps.getProperty("SetVar"));
            assertEquals("value6",asenvProps.getProperty("seVar"));
            assertEquals("value7",asenvProps.getProperty("sVar"));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}