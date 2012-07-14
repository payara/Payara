/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.virtualization.os;

import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.util.Map;
import java.util.Scanner;

/**
 * OS specific commands to Mac OS X
 * @author Jerome Dochez
 */
@Service(name="Mac_OS_X")
@ContractsProvided(OsInterfaceProvider.class)
public class Darwin extends Ubuntu {

    @Override
    protected String fpingPath() {
        return "/opt/local/sbin";
    }

    @Override
    protected void scanArpLine(String arpLine, Map<String, String> macToIps) {
        Scanner scanner = new Scanner(arpLine);
        scanner.next(); // skip ? character
        String ipAddress = scanner.next();
        ipAddress = ipAddress.substring(1, ipAddress.length()-1); // remove surround ()
        scanner.next(); // skip at
        String macAddress = scanner.next();
        if (macAddress.equals("(incomplete)")) return;
        if (macAddress.startsWith("0")) {
            macAddress = "0" + macAddress;
        }
        macToIps.put(macAddress, ipAddress);
        if (context.debug)
            System.out.println("--->" + ipAddress + " is at " + macAddress);
    }

    @Override
    protected String wol() {
        return "/Users/dochez/java/tools/wol/wol";
    }

    @Override
    protected String getArpCommand() {
        return "arp -n -i en0 -a";
    }
}
