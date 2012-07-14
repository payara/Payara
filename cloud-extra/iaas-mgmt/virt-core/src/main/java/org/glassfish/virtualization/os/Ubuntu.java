/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.virtualization.ShellExecutor;
import org.glassfish.virtualization.spi.PhysicalServerPool;
import org.glassfish.virtualization.spi.Machine;
import org.glassfish.virtualization.spi.OsInterface;
import org.glassfish.virtualization.config.ServerPoolConfig;
import org.glassfish.virtualization.util.OsInterfaceFactory;
import org.glassfish.virtualization.util.RuntimeContext;
import javax.inject.Inject;

import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

/**
 * OS specifc commands for Ubuntu linux
 * @author Jerome Dochez
 */
@Service(name="ubuntu")
@ContractsProvided(OsInterfaceProvider.class)
public class Ubuntu implements OsInterface, OsInterfaceProvider {

    @Inject
    ShellExecutor shell;

    @Inject
    RuntimeContext context;

    @Override
    public void suspend(Machine machine) throws IOException {
        try {
            machine.sleep();
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public void resume(Machine machine) throws IOException {
        // send magic packet with wakeonlan utility
        // TODO replace this with java datagram broadcast
        try {
            shell.executeAndWait(new File("/"), wol() + " " + machine.getConfig().getMacAddress());
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    @Override
    public Map<String, String> populateMacToIpsTable(PhysicalServerPool group) {

        ServerPoolConfig groupConfig = group.getConfig();
        String subNet = groupConfig.getSubNet();
        Map<String, String> macToIps = new HashMap<String, String>();
        RuntimeContext.logger.info("Populating IP addresses tables, this may take a while...");
        try {
            Process result = shell.executeAndWait(new File(fpingPath()), "fping -c 1 -g " + subNet);
            if (result.exitValue()==1) {
                // now executeAndWait the arp command to get the list of mac address for each IP
                result = shell.execute(new File(arpPath()), getArpCommand());
                // I have to do this because the command does not always exit and return.
                Thread.sleep(1000);
                LineNumberReader lnReader = null;
                try {
                    lnReader = new LineNumberReader(new InputStreamReader(result.getInputStream()));
                    String line;
                    // first line is skipped, it's the column titles.
                    lnReader.readLine();
                    while ((line = lnReader.readLine())!=null) {
                        Thread.sleep(25);
                        scanArpLine(line, macToIps);
                    }
                    lnReader.close();
                } catch (IOException ioe) {
                    try {
                        if (lnReader!=null) lnReader.close();
                    } catch(IOException ioe2) {
                        // ignore
                    }
                }
                result.destroy();
            } else {
                System.out.println("error : " + shell.output(result));
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        RuntimeContext.logger.info("Finished populating IP addresses");
        return macToIps;
    }

    @Override
    public String macAddressGen() {
        try {
            StringBuffer buffer = new StringBuffer();
            buffer.append("52:54");
            Random random = new Random();
            // generate four hexadecimal 2 digits numbers.
            for (int i = 0; i < 4; i++) {
                buffer.append(String.format(":%02x", random.nextInt(256)));
            }
            return buffer.toString();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected String getArpCommand() {
        return "arp -n";
    }

    protected void scanArpLine(String arpLine, Map<String, String> macToIps) {
        Scanner scanner = new Scanner(arpLine);
        String ipAddress = scanner.next();
        String hwType = scanner.next();
        if (hwType.equals("(incomplete)")) return;
        String macAddress = scanner.next();
        macToIps.put(macAddress, ipAddress);
        if (context.debug)
            System.out.println("--->" + ipAddress + " is at " + macAddress);
    }

    protected String fpingPath() {
        return "/usr/bin";
    }

    protected String arpPath() {
        return "/usr/sbin";
    }

    protected String wol() {
        return "/usr/bin/wakeonlan";
    }

    @Override
    public String userId() {
       try {
            Process result = shell.executeAndWait(new File("/usr/bin"), "id -u");
            return shell.output(result);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String groupId() {
        try {
            Process result = shell.executeAndWait(new File("/usr/bin"), "id -g");
            return shell.output(result);
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

	@Override
	public OsInterface provideOsInterface() {
		return this;
	}
}
