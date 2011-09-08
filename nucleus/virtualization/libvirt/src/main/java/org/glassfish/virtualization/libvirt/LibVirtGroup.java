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
package org.glassfish.virtualization.libvirt;

import org.glassfish.hk2.Services;
import org.glassfish.hk2.inject.Injector;
import org.glassfish.virtualization.config.*;
import org.glassfish.virtualization.runtime.VirtualCluster;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.util.EventSource;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.config.*;

import java.beans.PropertyChangeEvent;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runtime representation of a serverPool, with its members and such.
 */
@Service(name="libvirt")
@Scoped(PerLookup.class)
public class LibVirtGroup implements PhysicalServerPool, ConfigListener {

    @Inject
    Injector injector;

    @Inject
    ExecutorService executorService;

    @Inject
    RuntimeContext rtContext;

    @Inject
    com.sun.enterprise.config.serverbeans.Domain domain;

    @Inject
    Services services;

    public ServerPoolConfig config;
    final ConcurrentMap<String, Machine> machines = new ConcurrentHashMap<String, Machine>();
    final AtomicInteger allocationCount = new AtomicInteger();

    public void setConfig(ServerPoolConfig config) {
        this.config = config;
        Dom.unwrap(config).addListener(this);
        populateGroup();
    }

    @Override
    public ServerPoolConfig getConfig() {
        return config;
    }

    @Override
    public String getName() {
        return config.getName();
    }

    @Override
    public Collection<VirtualMachine> getVMs() throws VirtException {
        List<VirtualMachine> vms = new ArrayList<VirtualMachine>();
        for (Machine machine : machines.values()) {
            vms.addAll(machine.getVMs());
        }
        return vms;
    }

    @Override
    public Iterable<? extends Machine> machines() {
        return machines.values();
    }

    public int size() {
        return machines.size();
    }

    @Override
    public VirtualMachine vmByName(String name) throws VirtException {
        for (Machine machine : machines.values()) {
            VirtualMachine vm = machine.byName(name);
            if (vm!=null) return vm;
        }
        return null;
    }

    @Override
    public UnprocessedChangeEvents changed(PropertyChangeEvent[] propertyChangeEvents) {
        return ConfigSupport.sortAndDispatch(propertyChangeEvents, new Changed() {
            @Override
            public <T extends ConfigBeanProxy> NotProcessed changed(TYPE type, Class<T> tClass, T t) {
                    if (t instanceof MachineConfig) {
                        MachineConfig machineConfig = MachineConfig.class.cast(t);
                        if (type.equals(TYPE.ADD)) {
                            if (machineConfig.getIpAddress()==null) {
                                Map<String, String> macToIps = macAddressesToIps();
                                addMachine(machineConfig, macToIps.get(machineConfig.getMacAddress()));
                            } else {
                                addMachine(machineConfig, machineConfig.getIpAddress());
                            }
                            // we should update our cache as well...
                        }
                    }
                return null;
            }
        }, Logger.getAnonymousLogger());
    }

    public Machine byName(String machineName) {
        return machines.get(machineName);
    }


    private Map<String, String> macAddressesToIps() {
        OsInterface os = getHabitat().getComponent(OsInterface.class);
        return os.populateMacToIpsTable(this);
    }

    private void populateGroup() {

        Properties cached = new Properties();
        File cache = null;
        // load the cache
        try {
            cache = new File(RuntimeContext.getCacheDir(), config.getName() + ".cache");
            if (cache.exists()) {
                populateCachedValues(cached, cache);
            }
        } catch(IOException e) {
            RuntimeContext.logger.log(Level.INFO, "Error while reading cache, recalculating all machines IPs", e);
        }

        Map<String, String> macToIps = null;
        boolean dirtyCache = false;
        // now update our runtime tables.
        for (MachineConfig machineConfig : config.getMachines()) {
            if (cached.containsKey(machineConfig.getName())) {
                // we still need to see if the cached IP address respond as the machine may have changed its IP since we
                // created the cache.
                String ipAddress = cached.getProperty(machineConfig.getName());
                // this is a hack we will need to do better
                Machine machine = (machineConfig.getNetworkName()!=null && machineConfig.getNetworkName().equals("localhost")?
                        LibVirtLocalMachine.from(injector, this, machineConfig):
                        LibVirtMachine.from(injector, this, machineConfig, ipAddress));
                if (machine.isUp()) {
                    addMachine(machine);
                    continue;
                }
            }
            // the cache did not contain an entry for this machine or the cached IP address is not responding, le'ts be safe.
            String ipAddress=machineConfig.getIpAddress();
            if (ipAddress==null) {
                String macAddress = machineConfig.getMacAddress();
                if (macAddress!=null && macToIps==null) {
                    macToIps = macAddressesToIps();
                }
                if (macToIps!=null) {
                    ipAddress=macToIps.get(macAddress);
                }
            }
            addMachine(machineConfig, ipAddress);
            if (ipAddress!=null) {
                cached.put(machineConfig.getName(), ipAddress);
                dirtyCache = true;

            }
        }
        // saves the cache
        if (dirtyCache && cache!=null) {
            try {
                saveCachedValues(cached, cache);
            } catch (IOException e) {
                RuntimeContext.logger.log(Level.INFO, "Error while writing cache", e);
            }
        }

        // count the number of allocation virtual machines in this serverPool.
        for (Machine machine : machines()) {
            try {
                for (VirtualMachine vm : machine.getVMs()) {
                    allocationCount.incrementAndGet();
                }
            } catch(VirtException e) {
                RuntimeContext.logger.log(Level.SEVERE, "Cannot obtain list of virtual machines", e);
            }
        }

        // finally register a listener.
        Dom.unwrap(config).addListener(new ConfigListener() {
            @Override
            public UnprocessedChangeEvents changed(PropertyChangeEvent[] propertyChangeEvents) {
                return ConfigSupport.sortAndDispatch(propertyChangeEvents, new Changed() {
                    @Override
                    public <T extends ConfigBeanProxy> NotProcessed changed(TYPE type, Class<T> tClass, T t) {
                        if (t instanceof MachineConfig) {
                            MachineConfig machineConfig = MachineConfig.class.cast(t);
                            if (type.equals(TYPE.ADD)) {
                                addMachine(machineConfig, machineConfig.getIpAddress());
                            }
                            if (type.equals(TYPE.REMOVE)) {
                                synchronized (this) {
                                    machines.remove(machineConfig.getName());
                                }
                            }
                        }
                        return null;
                    }
                }, RuntimeContext.logger);
            }
        });
    }


    private void populateCachedValues(Properties cached, File cache) throws IOException {
        Reader reader = null;
        try {
            reader = new FileReader(cache);
            cached.load(reader);
        } finally {
            if (reader!=null)
                reader.close();
        }
    }

    private void saveCachedValues(Properties cached, File cache) throws IOException  {
        Writer writer = null;
        try {
            writer = new FileWriter(cache);
            cached.store(writer, "Cache file for serverPool " + config.getName());
        } finally {
            if (writer!=null) writer.close();
        }
    }

    private void addMachine(MachineConfig machineConfig, String ipAddress) {
        Machine machine = (machineConfig.getNetworkName()!=null && machineConfig.getNetworkName().equals("localhost")?
                LibVirtLocalMachine.from(injector,  this, machineConfig):
                LibVirtMachine.from(injector, this, machineConfig, ipAddress));
        addMachine(machine);
    }

    private synchronized void addMachine(Machine machine) {
        machines.put(machine.getConfig().getName(), machine);
    }

    private Habitat getHabitat() {
        return Dom.unwrap(config).getHabitat();
    }

    @Override
    public PhasedFuture<AllocationPhase, VirtualMachine> allocate(
            final TemplateInstance template, final VirtualCluster cluster, final EventSource<AllocationPhase> source)
            throws VirtException {
        // for now, could not be simpler, iterate over machines I own and ask for a virtual machine to
        // each of them.
        // Eventually the algorithm below will need to be refined using each machine capabilities as
        // a deciding factor.
        int park = size();
        if (park==0) {
            throw new VirtException("Cannot allocate virtual machine to a serverPool with no machine");
        }

        Iterator<? extends Machine> machines = machines().iterator();

        // allocate a virtual machine on i%park machine...
        Machine machine;
        int machineTried = 0;
        do {
            if (!machines.hasNext()) {
                machines = machines().iterator();
            }
            machine = machines.next();
            machineTried++;

            if (!machine.isUp()) {
                RuntimeContext.logger.info("Waking up machine " + machine.getName());
                try {
                    Habitat habitat = Dom.unwrap(config).getHabitat();
                    habitat.getComponent(OsInterface.class).resume(machine);
                } catch (IOException e) {
                    RuntimeContext.logger.log(Level.SEVERE, "Error while waking up machine "
                            + machine.getName(), e);
                }
                // waiting for machine to wake up for 5 seconds max...
                int tries = 0;
                do {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // ignore
                    }
                    tries++;
                } while (!machine.isUp() && tries < 5);
                if (!machine.isUp()) {
                    RuntimeContext.logger.log(Level.SEVERE, "Cannot wake up machine " + machine.getConfig().getDisksLocation());
                }
            }
        } while (!machine.isUp() || machineTried > park);

        if (!machine.isUp()) {
            RuntimeContext.logger.log(Level.SEVERE, "All the machines of this serverPool are shutdown and cannot be started");
            throw new VirtException("Cannot start any of the serverPool's machine");
        }

        final Machine targetMachine = machine;
        allocationCount.incrementAndGet();

        PhasedFuture<AllocationPhase, VirtualMachine> vm = null;
        try {
            vm = targetMachine.create(template, cluster, source);
        } catch (IOException e) {
            throw new VirtException(e);
        }

        RuntimeContext.logger.info("Virtual machine allocated in serverPool " + getName() +
                " for cluster " + cluster.getConfig().getName());
        return vm;
    }

    @Override
    public void install(TemplateInstance template) throws IOException {
        IOException lastException = null;
        for (Machine machine : machines.values()) {
            try {
                machine.install(template);
            } catch (IOException e) {
                lastException = e;
                RuntimeContext.logger.log(Level.SEVERE, "Error while installing template " +
                        template.getConfig().getName() +
                    " on " + machine.getName(), e);
            }
        }
        if (lastException!=null) throw lastException;
    }
}
