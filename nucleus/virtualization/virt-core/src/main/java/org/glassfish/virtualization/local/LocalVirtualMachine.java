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

package org.glassfish.virtualization.local;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.util.ExecException;
import com.sun.enterprise.util.ProcessExecutor;
import org.glassfish.virtualization.config.VirtualMachineConfig;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.util.AbstractVirtualMachine;
import org.glassfish.virtualization.util.RuntimeContext;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.logging.Level;

/**
 * abstraction of a non virtualization bare metal PC.
 *
 * @author Jerome Dochez
 */
class LocalVirtualMachine extends AbstractVirtualMachine {

    final String vmName;
    final Machine machine;
    final LocalServerPool pool;

    LocalVirtualMachine(LocalServerPool pool, Machine machine, String vmName) {
        this.vmName = vmName;
        this.pool = pool;
        this.machine = machine;
    }


    @Override
    public String getName() {
        return vmName;
    }

    @Override
    public String getAddress() {
        // we need to return our ip address.
        Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            RuntimeContext.logger.log(Level.SEVERE, "Cannot get list of network interfaces",e);
            return "127.0.0.1";
        }
        while (interfaces.hasMoreElements()) {
            NetworkInterface networkInterface = interfaces.nextElement();
            // we need a way to customize this through configuration
            Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
            while (addresses.hasMoreElements()) {
                String address = addresses.nextElement().getHostAddress();
                if (address.contains(".")) {
                    return address;
                }
            }
        }
        return "127.0.0.1";

    }

    @Override
    public void setAddress(String address) {
        // ignore/
    }

    @Override
    public void start() throws VirtException {
        for (Cluster cluster : pool.domain.getClusters().getCluster()) {
            for (VirtualMachineConfig vmc : cluster.getExtensionsByType(VirtualMachineConfig.class)) {
                if (vmc.getName().equals(getName())) {
                    TemplateInstance ti = pool.templateRepository .byName(vmc.getTemplate().getName());
                    if (ti!=null) {
                        ti.getCustomizer().start(this);
                    }
                    return;
                }
            }
        }
        throw new RuntimeException("Cannot find registered virtual machine " + getName());
    }

    @Override
    public void suspend() throws VirtException {
        throw new RuntimeException("Local processes cannot be suspended or resumed");
    }

    @Override
    public void resume() throws VirtException {
        throw new RuntimeException("Local processes cannot be suspended or resumed");
    }

    @Override
    public void stop() throws VirtException {
        // nothing to do ?
    }

    @Override
    public void delete() throws VirtException {
        // nothing to do, the delete-instance took care of it.
    }

    @Override
    public VirtualMachineInfo getInfo() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ServerPool getServerPool() {
        return pool;
    }

    @Override
    public Machine getMachine() {
        return machine;
    }

    @Override
    public String executeOn(String[] args) throws IOException, InterruptedException {
        ProcessExecutor processExecutor = new ProcessExecutor(args);
        try {
            String[] returnLines = processExecutor.execute(true);
            StringBuffer stringBuffer = new StringBuffer();
            for (String returnLine : returnLines) {
                stringBuffer.append(returnLine);
            }
            return stringBuffer.toString();
        } catch (ExecException e) {
            throw new RuntimeException(e);
        }
    }
}
