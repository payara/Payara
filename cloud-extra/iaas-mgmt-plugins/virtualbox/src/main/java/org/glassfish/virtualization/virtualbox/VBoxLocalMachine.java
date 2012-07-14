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
package org.glassfish.virtualization.virtualbox;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.CountDownLatch;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.virtualization.config.VirtualMachineConfig;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.util.RuntimeContext;
import javax.inject.Inject;
import org.jvnet.hk2.component.Habitat;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.api.ServiceLocator;

import java.util.logging.Level;
import org.glassfish.virtualization.config.MachineConfig;
import org.glassfish.virtualization.config.Template;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.runtime.AbstractMachine;
import org.glassfish.virtualization.spi.VirtualCluster;
import org.glassfish.virtualization.runtime.VirtualMachineLifecycle;
import org.glassfish.virtualization.util.Host;

import org.glassfish.virtualization.spi.EventSource;
import org.glassfish.virtualization.util.ListenableFutureImpl;

import org.virtualbox_4_1.*;

/**
 * Abstraction for this machine, assumptions are being made that this java process runs with
 * with the same user's credentials that vbox is expecting to manage the virtualization environment
 * of this machine.
 *
 * @author  Ludovic Champenois
 */
public class VBoxLocalMachine extends AbstractMachine implements PostConstruct {

     final Map<String, VBoxVirtualMachine> domains = new HashMap<String, VBoxVirtualMachine>();

    @Inject
    Virtualizations virtualizations;

    @Inject
    Domain domain;
    
    @Inject
    Habitat habitat;
     
     @Inject
    VirtualMachineLifecycle vmLifecycle;
    
    @Inject
    Host host;

    public static VBoxLocalMachine from(ServiceLocator injector, VBoxGroup group, MachineConfig config) {
        VBoxLocalMachine vboxLocalMachine = new VBoxLocalMachine( group, config);
		injector.inject(vboxLocalMachine);
		
		return vboxLocalMachine;
    }

    protected VBoxLocalMachine(VBoxGroup group, MachineConfig config) {
        super(group, config);
    }

    @Override
    public void postConstruct() {
        setState(isUp()? VBoxMachine.State.READY: VBoxMachine.State.SUSPENDED);
        super.postConstruct();
    }

    @Override
    public StoragePool addStoragePool(String name, long capacity) throws VirtException {
        return null;
    }   

    
    public String description() {
        StringBuffer sb = new StringBuffer();
        sb.append("Machine ").append(getName());
        try {
            ///   connection();
            /*          int [] domainIds = connect.listDomains();
            if (domainIds==null || domainIds.length==0) {
            sb.append(" with no virtual machines defined");
            } else {
            sb.append(" with domains : [");
            for (int domainId : domainIds) {
            Domain testDomain = connect.domainLookupByID(domainId);
            sb.append("[ domain:").append(
            testDomain.getName()).append(
            " id ").append(
            testDomain.getID()).append(
            " running ").append(
            testDomain.getOSType()).append(
            " ]");
            }
            sb.append("]");
            
            }*/
        } catch (Exception e) {
            RuntimeContext.logger.log(Level.SEVERE, "Exception caught :" + e, e);
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    @Override
    public Collection<? extends VirtualMachine> getVMs() throws VirtException {
        // Map<String, VBoxVirtualMachine> domains = new HashMap<String, VBoxVirtualMachine>();
        try {
            //    System.setProperty("vbox.home", "/Applications/VirtualBox.app/Contents/MacOS");
            VirtualBoxManager mgr = ConnectionManager.connection(getIpAddress());
            List<IMachine> machs = mgr.getVBox().getMachines();
            for (IMachine m : machs) {
                try {
                    String domainName = m.getName();                                        
                    if (!domains.containsKey(domainName)) {
                        for (Cluster cluster : domain.getClusters().getCluster()) {
                            for (VirtualMachineConfig vmc : cluster.getExtensionsByType(VirtualMachineConfig.class)) {
                                if (vmc.getName().equals(domainName)) {
                                    VBoxVirtualMachine gfVM = new VBoxVirtualMachine(vmc, vmc.getTemplate().getUser(), this, domainName);
                                    domains.put(domainName, gfVM);
                                }
                            }
                        }


                    }
                } catch (Exception e) {
                }
            }
            mgr.disconnect();
            mgr.cleanup();


        } catch (Exception e) {
            RuntimeContext.logger.log(Level.SEVERE, "Exception while populating list of domains ", e);
            throw new VirtException(e);
        }
        return domains.values();

    }

    @Override
    public Map<String, ? extends org.glassfish.virtualization.spi.StoragePool> getStoragePools() throws VirtException {

        return null;
    }

    @Override
    public VirtualMachine byName(String name) throws VirtException {
        if (!domains.containsKey(name)) {
            try {
                getVMs();
            } catch (VirtException e) {
                RuntimeContext.logger.log(Level.SEVERE, "Exception while populating list of domains ", e);
            }
        }
        return domains.get(name);
    }
    
    public void sleep() throws IOException, InterruptedException  {
        throw new IOException("Impossible to put myself to sleep");
    }


    protected VirtualBoxManager connection() throws VirtException {
        return ConnectionManager.connection(getIpAddress());
    }

    @Override
    public PhasedFuture<AllocationPhase, VirtualMachine> create(TemplateInstance template,
            VirtualCluster cluster, EventSource<AllocationPhase> source) throws VirtException, IOException {

        // populate();
        final String name = cluster.getConfig().getName() + cluster.allocateToken();

        source.fireEvent(AllocationPhase.VM_PREPARE);        
        // 1. copy the template to the destination machine.
        final String diskLocation = System.getProperty("user.home") + "/" + config.getDisksLocation();

        execute(new MachineOperations<Object>() {

            @Override
            public Object run(FileOperations fileOperations) throws IOException {
                fileOperations.mkdir(config.getDisksLocation());


                fileOperations.delete(diskLocation + "/" + name + "cust.iso");
                fileOperations.delete(diskLocation + "/" + name + ".vdi");
                fileOperations.delete(diskLocation + "/" + name + ".vmdk");

                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        File machineDisks = absolutize(new File(virtualizations.getDisksLocation(), serverPool.getName()));
        machineDisks = new File(machineDisks, getName());
        if (!machineDisks.exists()) {
            if (!machineDisks.mkdirs()) {
                throw new IOException("cannot create disk cache on local machine");
            }
        }

        createVboxVDIMachine(template.getConfig(), cluster, diskLocation, name);

        try {
            //       Domain domain = connection().domainDefineXML(getConfig(vmConfig));
            source.fireEvent(AllocationPhase.VM_SPAWN);
            final CountDownLatch latch =  vmLifecycle.inStartup(name);
            VirtualMachineConfig vmConfig = VirtualMachineConfig.Utils.create(
                    name,
                    template.getConfig(),
                    serverPool.getConfig(),
                    cluster.getConfig());

            final VBoxVirtualMachine vm = new VBoxVirtualMachine(
                    vmConfig, template.getConfig().getUser(), this, name);
            domains.put(name, vm);
            cluster.add(vm);
            ListenableFutureImpl<AllocationPhase, VirtualMachine> future =
                    new ListenableFutureImpl<AllocationPhase, VirtualMachine>(latch, vm, source);

            future.fireEvent(AllocationPhase.VM_START);
            vmLifecycle.start(vm);

            return future;
        } catch (Exception e) {

            throw new VirtException(e);
        }
    }
   
    private void createVboxVDIMachine(final Template template,
            final VirtualCluster cluster, String diskLocation, String name) throws VirtException {
        VirtualBoxManager connect = null;
        try {
            connect = ConnectionManager.connection(getIpAddress());
            IVirtualBox vbox = connect.getVBox();
            String machineName = name;
            IMachine iap = vbox.createMachine(null, machineName, "Linux", null, true);

            List<IStorageController> lsc = iap.getStorageControllers();
            for (IStorageController aaa : lsc) {
                System.out.println("storage controller " + aaa.getName());
            }
            iap.getUSBController().setEnabled(false);
            iap.setMemorySize(512L);
            String path = diskLocation + "/" + name + ".vdi";
            IMedium newm = vbox.createHardDisk("VDI", path);
            File templateDirLocation = new File(virtualizations.getTemplatesLocation(), template.getName() );
            // Define a filter for j files ending with .vdi
            FilenameFilter select = new VDIFileFilter();
            File[] contents = templateDirLocation.listFiles(select);
            if (contents == null || contents.length==0) {
                throw new VirtException("No .vdi file in " + templateDirLocation.getAbsolutePath());
            }
            if (contents.length !=1) {
                throw new VirtException("there should be only 1 .vdi file in " + templateDirLocation.getAbsolutePath());
            }
            File VDITemplate = contents[0];
            //open the sourcefile template 
            IMedium im = vbox.openMedium(VDITemplate.getAbsolutePath(), DeviceType.HardDisk, AccessMode.ReadWrite, true);
            IProgress prog = im.cloneTo(newm, im.getVariant(), null);
            prog.waitForCompletion(-1);
            iap.addStorageController("SCSI Controller", StorageBus.SCSI);
            INetworkAdapter inadap = iap.getNetworkAdapter(0L);
            inadap.setEnabled(Boolean.FALSE);
            inadap.setAdapterType(NetworkAdapterType.I82540EM);
            inadap.setAttachmentType(NetworkAttachmentType.Bridged);
            //inadap.attachToBridgedInterface();
            //   inadap.attachToHostOnlyInterface();
            String goodPortName =null;
            StringTokenizer portNames = new StringTokenizer(serverPool.getConfig().getPortName(), ";");
            while (portNames.hasMoreTokens()) {
                if (goodPortName!=null){
                    break;
                }
                String portName = portNames.nextToken();
                List<IHostNetworkInterface> lll = vbox.getHost().getNetworkInterfaces();
                for (IHostNetworkInterface imnet : lll) {
                    System.out.println("possible interface name= "+ imnet.getName());
                    if (imnet.getName().equals(portName)) {
                        goodPortName = portName;
                        break;
                    }
                }
            }

                if (goodPortName==null){
      //              goodPortName = "en0: Ethernet"; //TODO!!!! FIXME
             throw new VirtException("Cannot find a good portName from this list: "+serverPool.getConfig().getPortName());
               
                }
            inadap.setBridgedInterface(goodPortName);
            //   inadap.setHostInterface("vboxnet0");
            inadap.setMACAddress(null);
            inadap.setEnabled(Boolean.TRUE);

            iap.addStorageController("IDE Controller", StorageBus.IDE);
            File ISOFile = createISOCustomization(template, cluster, name, diskLocation);

            //open the cust ISO image for this VM
            IMedium cdrommedium = vbox.openMedium(ISOFile.getAbsolutePath(), DeviceType.DVD, AccessMode.ReadOnly, true);
            iap.saveSettings();

            vbox.registerMachine(iap);

            IMachine newMachine = vbox.findMachine(machineName);

            ISession session = connect.openMachineSession(newMachine);
            session.unlockMachine();
            try {
                newMachine.lockMachine(session, LockType.Write);
                IMedium im2 = vbox.openMedium(path, DeviceType.HardDisk, AccessMode.ReadWrite, true);

                session.getMachine().attachDevice("SCSI Controller", 0, 0, DeviceType.HardDisk, im2);
                //second param 1 represents IDE secondary master
                session.getMachine().attachDevice("IDE Controller", 1, 0, DeviceType.DVD, cdrommedium);
                session.getMachine().saveSettings();
            } finally {
                session.unlockMachine();
            }
        } catch (Exception e) {
            throw new VirtException(e);
        } finally {
            if (connect != null) {
                connect.disconnect();
                connect.cleanup();
            }
        }

    }

 
    // createISOCustomization (template, cluster.getConfig().getName(), name ,diskLocation);
    private File getCustomizationDir(String machineAlias){
        File custDir = null;
 
        try {

            File machineDisks = absolutize(new File(virtualizations.getDisksLocation(), serverPool.getName()));
            machineDisks = new File(machineDisks, getName());
            custDir = new File(machineDisks, machineAlias + "cust");
            if (!custDir.exists()) {
                if (!custDir.mkdirs()) {
                    throw new IOException("cannot create disk cache on local machine");
                }
            }
            return custDir;
        }catch (Exception e){
            return null;
        }

    }
    private File createISOCustomization(Template template, VirtualCluster cluster, String machineAlias,
            String diskLocation) throws VirtException {
        File custDir = getCustomizationDir( machineAlias);
        Disk custISO = habitat.getComponent(Disk.class);
        try {

            // create the customized properties.
            custDir =  prepareCustDirectory( machineAlias,  cluster.getConfig(),  template ) ;
 

////mkisofs -l -allow-lowercase -allow-multidot -L -o my3.iso test 

            File ret = new File(diskLocation + "/" + machineAlias + "cust.iso");
            custISO.createISOFromDirectory(custDir, ret);
            return ret;


        } catch (Exception e) {
            throw new VirtException(e);
        } finally {
            //  if (tmpDir!=null && tmpDir.exists())
            //      FileUtils.whack(tmpDir);
        }
    }
    
    private static class VDIFileFilter implements FilenameFilter {

        @Override
        public boolean accept(File directory, String filename) {
            return filename.endsWith(".vdi");
        }
    }
}
