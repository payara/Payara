/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019-2022] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.boot.runtime;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 *
 * @author Gaurav Gupta
 */
public class BootCommandsTest {

    @Test
    public void parseCommand() throws IOException {
        BootCommands bootCommands = new BootCommands();
        String commandText = "create-custom-resource --restype java.lang.String -s v --name='custom-res' --description=\"results \\\"in\\\" error\" --property value=\"${ENV=ini_ws_uri}\" vfp/vfp-menu/ini.ws.uri";
        try (Reader reader = new StringReader(commandText)){
            bootCommands.parseCommandScript(reader, false);
        }
        assertThat(bootCommands.getCommands().size(), is(1));
        
        BootCommand command = bootCommands.getCommands().get(0);
        assertThat(command.getArguments().length, is(9));
        assertEquals(command.getArguments()[0], "--restype");
        assertEquals(command.getArguments()[1], "java.lang.String");
        assertEquals(command.getArguments()[2], "-s");
        assertEquals(command.getArguments()[3], "v");
        assertEquals(command.getArguments()[4], "--name='custom-res'");
        assertEquals(command.getArguments()[5], "--description=\"results \\\"in\\\" error\"");
        assertEquals(command.getArguments()[6], "--property");
        assertEquals(command.getArguments()[7], "value=\"${ENV=ini_ws_uri}\"");
        assertEquals(command.getArguments()[8], "vfp/vfp-menu/ini.ws.uri");
    }

}
