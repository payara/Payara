/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 *  or packager/legal/LICENSE.txt.  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at packager/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  Oracle designates this particular file as subject to the "Classpath"
 *  exception as provided by Oracle in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package org.glassfish.virtualization.virtualbox;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.glassfish.api.admin.CommandLock;
import org.glassfish.virtualization.config.VirtUser;
import org.glassfish.virtualization.config.VirtualMachineConfig;
import org.glassfish.virtualization.spi.ServerPool;
import org.glassfish.virtualization.util.RuntimeContext;



import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.cluster.ssh.launcher.SSHLauncher;
import org.glassfish.virtualization.spi.Machine;
import org.glassfish.virtualization.spi.MemoryListener;
import org.glassfish.virtualization.spi.StoragePool;
import org.glassfish.virtualization.spi.StorageVol;
import org.glassfish.virtualization.spi.VirtException;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.glassfish.virtualization.spi.VirtualMachineInfo;
import org.glassfish.virtualization.util.AbstractVirtualMachine;
import org.virtualbox_4_1.CleanupMode;
import org.virtualbox_4_1.IConsole;
import org.virtualbox_4_1.IMachine;
import org.virtualbox_4_1.IMedium;
import org.virtualbox_4_1.IMediumAttachment;
import org.virtualbox_4_1.IProgress;
import org.virtualbox_4_1.ISession;
import org.virtualbox_4_1.LockType;
import org.virtualbox_4_1.MachineState;
import org.virtualbox_4_1.SessionState;
import org.virtualbox_4_1.VirtualBoxManager;

/**
 * Representation of a virtual machine in the VBox world.
 *
 * @author Ludovic Champenois
 */
public class VBoxVirtualMachine extends AbstractVirtualMachine {

    final private Machine owner;
    final private String name;
    private InetAddress address;

    protected VBoxVirtualMachine(VirtualMachineConfig config, VirtUser user, Machine owner, String name)
            throws VirtException {
        super(config, user);
        this.owner = owner;
        this.name = name;

    }

    @Override
    public void setAddress(InetAddress address) {
        this.address = address;
    }

    @Override
    public InetAddress getAddress() {
        return address;
    }

    private IMachine getIMachine(VirtualBoxManager mgr) throws VirtException {
        try {
            return mgr.getVBox().findMachine(name);

        } catch (Exception e) {
            throw new VirtException(e);
        }
        //return null;
    }

    @Override
    public String executeOn(String[] args) throws IOException, InterruptedException {
        SSHLauncher sshLauncher = new SSHLauncher();
        File home = new File(System.getProperty("user.home"));
        String keyFile = new File(home,".ssh/id_rsa").getAbsolutePath();
        sshLauncher.init(getUser().getUserId(), address.getHostAddress(), 22, null, keyFile, null, Logger.getAnonymousLogger());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringBuilder stringBuilder = new StringBuilder();
        for (String arg : args) {
            stringBuilder.append(arg);
            stringBuilder.append(" ");
        }
        sshLauncher.runCommand(stringBuilder.toString().trim(), baos);
        return baos.toString();
    }

    enum Action {

        START, STOP, RESUME, DELETE, SUSPEND
    };

    private void execAction(Action action) throws VirtException {
        VirtualBoxManager connect = null;
        ISession session = null;
        try {
            connect = ConnectionManager.connection(owner.getIpAddress());
            // IVirtualBox vbox = connect.getVBox();
            switch (action) {
                case START: {
                    System.out.println("\nAttempting to start VM '" + name + "'");
                    if (!connect.startVm(name, null, 7000)) {
                        throw new VirtException("Failed to start virtual machine" + name);
                    }
                    break;
                }
                case STOP: {
                    session = connect.getSessionObject();
                    IMachine mach = getIMachine(connect);
                    try {
                        mach.lockMachine(session, LockType.Shared);
                        if (!session.getState().equals(SessionState.Locked)) {
                            System.out.println("error!!! wrong state");
                            return;
                        }
                        IConsole cons = session.getConsole();
                        MachineState curState = cons.getState();
                        if (MachineState.Running.equals(curState)) {
                            IProgress prog = cons.powerDown();
                            prog.waitForCompletion(-1);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        session.unlockMachine();
                    }
                    break;
                }
                case RESUME: {
                    session = connect.getSessionObject();
                    IMachine mach = getIMachine(connect);
                    try {
                        mach.lockMachine(session, LockType.Shared);
                        if (!session.getState().equals(SessionState.Locked)) {
                            System.out.println("error!!! wrong state");
                            session.unlockMachine();
                        }
                        IConsole cons = session.getConsole();
                        // MachineState curState = cons.getState();
                        // if (curState.Running.equals(curState)) {
                        cons.resume();
                        // }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        session.unlockMachine();
                    }
                    break;
                }
                case DELETE: {
                    //stop();
                    IMachine mach = getIMachine(connect);
                    session = connect.getSessionObject();
                    System.out.println("Session state " + session.getState());
                    try {
                        mach.lockMachine(session, LockType.Shared);
                        System.out.println("Session state " + session.getState());
                        if (!session.getState().equals(SessionState.Locked)) {
                            System.out.println("error!!! wrong state");
                            return;
                        }
                        IConsole cons = session.getConsole();
                        MachineState curState = cons.getState();
                        if (MachineState.Running.equals(curState)) {
                            IProgress prog = cons.powerDown();
                            prog.waitForCompletion(-1);
                        }

                        IMachine machine = getIMachine(connect);
                        while(!machine.getState().equals(MachineState.PoweredOff)) {
                            System.out.println("Waiting for machine to power down");
                            Thread.sleep(1000);
                        }
                        // While waiting, our lock may have been automatically released.
                        if (!session.getState().equals(SessionState.Locked)) {
                            mach.lockMachine(session, LockType.Shared);
                        }
                        System.out.println("Waiting for machine reported as powered down");

                        List<IMedium> attachedDevices = new ArrayList<IMedium>();
                        if (!session.getState().equals(SessionState.Locked)) {
                            System.out.println("error!!! wrong state");
                            return;
                        }
                        IMachine workingM = session.getMachine();
                        List<IMediumAttachment> lma = workingM.getMediumAttachments();
                        for (IMediumAttachment ma : lma) {
                            attachedDevices.add(ma.getMedium());
                            workingM.detachDevice(ma.getController(), ma.getPort(), ma.getDevice());
                        }
                        workingM.saveSettings();
                        session.unlockMachine();
                        Thread.sleep(1000);

                        System.out.println("Session state " + session.getState());

                        List<IMedium> devices = machine.unregister(CleanupMode.Full);
                        System.out.println("About to release disks");
                        machine.delete(devices);
                        for (IMedium attachedDevice : attachedDevices) {
                            IProgress progress = null;
                            try {
                                progress = attachedDevice.deleteStorage();
                            } catch (Exception e) {
                                // Ignore those exceptions for now.
                            }
                            if (progress!=null) {
                                progress.waitForCompletion(10000);
                            }
                        }
                        System.out.println("done with releasing disks");

                    } catch (Exception e) {
                        e.printStackTrace();
                        //throw new VirtException(e);
                    } finally {
                        System.out.println("Session state " + session.getState());
                        try {
                            if (session.getState().equals(SessionState.Locked))
                                session.unlockMachine();
                        } catch(Exception e) {
                            RuntimeContext.logger.log(Level.WARNING, "Exception while unlocking machine : " + e.getMessage());
                        }
                        session = null;
                    }
                    break;

                }
                case SUSPEND: {
                    session = connect.getSessionObject();
                    IMachine mach = getIMachine(connect);
                    mach.lockMachine(session, LockType.Shared);
                    if (!session.getState().equals(SessionState.Locked)) {
                        System.out.println("error!!! wrong state");
                        session.unlockMachine();
                    }
                    IConsole cons = session.getConsole();
                    MachineState curState = cons.getState();
                    if (curState.Running.equals(curState)) {
                        cons.pause();
                    }
                    try {
                        session.unlockMachine();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                }

            }

        } catch (Exception e) {
            throw new VirtException(e);
        } finally {

            if (session != null) {
                try {
                    connect.closeMachineSession(session);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
            if (connect != null) {
                try {
                    connect.disconnect();
                    connect.cleanup();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void start() throws VirtException {
        execAction(Action.START);
    }

    @Override
    public void stop() throws VirtException {
        execAction(Action.STOP);

    }

    @Override
    public void resume() throws VirtException {
        execAction(Action.RESUME);

    }

    @Override
    public void suspend() throws VirtException {
        execAction(Action.SUSPEND);

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void delete() throws VirtException {

        execAction(Action.DELETE);

    }

    public Iterable<StorageVol> volumes() throws VirtException {

        List<StorageVol> volumes = new ArrayList<StorageVol>();
        for (StoragePool pool : owner.getStoragePools().values()) {
            for (StorageVol volume : pool.volumes()) {
                if (volume.getName().startsWith(getName())) {
                    volumes.add(volume);
                }
            }
        }
        return volumes;
    }

    @Override
    public VirtualMachineInfo getInfo() {
        return new VirtualMachineInfo() {

            @Override
            public int nbVirtCpu() throws VirtException {
                try {
                    return 1;// domain.getInfo().nrVirtCpu;
                } catch (Exception e) {
                    throw new VirtException(e);
                }
            }

            @Override
            public long memory() throws VirtException {
                try {
                    return 30000L;// domain.getInfo().memory;
                } catch (Exception e) {
                    throw new VirtException(e);
                }
            }

            @Override
            public long maxMemory() throws VirtException {
                try {
                    return 30000L;// domain.getInfo().maxMem;
                } catch (Exception e) {
                    throw new VirtException(e);
                }
            }

            @Override
            public Machine.State getState() throws VirtException {


                VirtualBoxManager connect = null;
                ISession session = null;
                try {
                    connect = ConnectionManager.connection(owner.getIpAddress());
                    session = connect.openMachineSession(getIMachine(connect));
                    IConsole console = session.getConsole();
                    MachineState currentState = console.getState();
                    if (MachineState.Running.equals(currentState)) {
                        return Machine.State.READY;
                    } else if (MachineState.PoweredOff.equals(currentState)) {
                        return Machine.State.SUSPENDED;

                    }//todo a lot more states
                } catch (Exception e) {
                    throw new VirtException(e);
                } finally {
                    if (session != null) {
                        connect.closeMachineSession(session);
                    }
                    if (connect != null) {
                        connect.disconnect();
                        connect.cleanup();
                    }

                }
                return Machine.State.SUSPENDED;




            }
            private final Map<MemoryListener, ScheduledFuture> listeners =
                    new HashMap<MemoryListener, ScheduledFuture>();

            @Override
            public void registerMemoryListener(final MemoryListener ml, long period, TimeUnit unit) {
                final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                final VirtualMachine owner = VBoxVirtualMachine.this;

                listeners.put(ml,
                        executor.schedule(new Runnable() {

                    @Override
                    public void run() {
                        try {
                             ml.notified(owner, memory(), cpuTime());
                        } catch (VirtException e) {
                            RuntimeContext.logger.log(Level.FINE, "Exception while notifying of vm load ", e);
                        }
                    }
                }, period, unit));
            }

            @Override
            public void unregisterMemoryListener(final MemoryListener ml) {

                listeners.get(ml).cancel(false);
            }

            @Override
            public long cpuTime() throws VirtException {
               return 3L;// throw new UnsupportedOperationException("Not supported yet.");
            }
        };
    }
    
    @Override
    public ServerPool getServerPool() {
        return owner.getServerPool();    }

    @Override
    public Machine getMachine() {
        return owner;
    }

}
