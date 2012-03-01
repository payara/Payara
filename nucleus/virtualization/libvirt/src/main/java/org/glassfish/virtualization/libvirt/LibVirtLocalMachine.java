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
package org.glassfish.virtualization.libvirt;

import com.sun.enterprise.config.serverbeans.Cluster;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.inject.Injector;
import org.glassfish.virtualization.config.*;
import org.glassfish.virtualization.libvirt.config.LibvirtVirtualization;
import org.glassfish.virtualization.libvirt.jna.Connect;
import org.glassfish.virtualization.libvirt.jna.Domain;
import org.glassfish.virtualization.runtime.*;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.glassfish.virtualization.spi.EventSource;
import org.glassfish.virtualization.util.ListenableFutureImpl;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.PostConstruct;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import org.glassfish.internal.api.ClassLoaderHierarchy;

/**
 * Abstraction for this machine, assumptions are being made that this java process runs with
 * with the same user's credentials that libvirt is expecting to manage the virtualization environment
 * of this machine.
 *
 * @author  Jerome Dochez
 */
public class LibVirtLocalMachine extends AbstractMachine implements PostConstruct {


    final Map<String, LibVirtVirtualMachine> domains = new HashMap<String, LibVirtVirtualMachine>();
    final Map<String, LibVirtStoragePool> storagePools = new HashMap<String, LibVirtStoragePool>();

    Connect connect;

    @Inject
    Virtualizations virtualizations;

    @Inject
    Services services;

    @Inject
    VirtualMachineLifecycle vmLifecycle;

    @Inject
    com.sun.enterprise.config.serverbeans.Domain domainConfig;
    
    @Inject
    ClassLoaderHierarchy clh;

    public static LibVirtLocalMachine from(Injector injector,  LibVirtServerPool group, MachineConfig config) {
        return injector.inject(new LibVirtLocalMachine(group, config));
    }

    protected  LibVirtLocalMachine(LibVirtServerPool group, MachineConfig config) {
        super(group, config);
    }

    @Override
    public void postConstruct() {
        setState(isUp()? LibVirtMachine.State.READY: LibVirtMachine.State.SUSPENDED);
        super.postConstruct();
    }

    @Override
    public LibVirtStoragePool addStoragePool(String name, long capacity) throws VirtException {

           try {
               StringBuilder sb = new StringBuilder();
               sb.append("<pool type='dir'>\n").append(
                       "  <name>").append(name).append("</name>\n").append(
                       "  <uuid>").append(UUID.randomUUID()).append("</uuid>\n").append(
                       "  <capacity>").append(capacity).append("</capacity>\n").append(
//                    "  <allocation>0</allocation>\n" +
//                    "  <available>225705984000</available>\n" +
                       "  <source>\n").append(
                       "  </source>\n").append(
                       "  <target>\n").append(
                       "    <path>").append(getUserHome()).append("/").append(config.getDisksLocation()).append("</path>\n").append(
                       "    <permissions>\n").append(
                       "      <mode>0700</mode>\n").append(
                       "      <owner>").append(getUser().getUserId()).append("</owner>\n").append(
                       "      <group>").append(getUser().getGroupId()).append("</group>\n").append(
                       "      <serverPool>").append(getServerPool().getName()).append("</serverPool>\n").append(
                       "    </permissions>\n").append(
                       "  </target>\n").append(
                       "</pool>");

               LibVirtStoragePool storagePool = new LibVirtStoragePool(this, connection().storagePoolCreateXML(sb.toString(), 0));
               storagePools.put(name, storagePool);
               return storagePool;
           } catch(VirtException e) {
               throw new VirtException(e);
           }
       }


    public String description() {
        StringBuffer sb = new StringBuffer();
        sb.append("Machine ").append(getName());
        try {
            connection();
            int [] domainIds = connect.listDomains();
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
            }
        } catch (Exception e) {
            RuntimeContext.logger.log(Level.SEVERE, "Exception caught :" + e,e);
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    @Override
    public Collection<? extends VirtualMachine> getVMs() throws VirtException {
        try {
            populate();
        } catch(VirtException e) {
            RuntimeContext.logger.log(Level.SEVERE, "Exception while populating list of domains ", e);
        }
        return domains.values();

    }

    @Override
    public Map<String, ? extends org.glassfish.virtualization.spi.StoragePool> getStoragePools() throws VirtException {
        try {
            populate();
        } catch(VirtException e) {
            RuntimeContext.logger.log(Level.SEVERE, "Exception while populating list of storage pools ", e);
        }

        return Collections.unmodifiableMap(storagePools);
    }

    @Override
    public VirtualMachine byName(String name) throws VirtException {
        if (!domains.containsKey(name)) {
            try {
                populate();
            } catch(VirtException e) {
                RuntimeContext.logger.log(Level.SEVERE, "Exception while populating list of domains ", e);
            }
        }
        return domains.get(name);
    }

    public void sleep() throws IOException, InterruptedException  {
        throw new IOException("Impossible to put myself to sleep");
    }

    private LibvirtVirtualization getVirtualizationConfig() {
        return (LibvirtVirtualization) getServerPool().getConfig().getVirtualization();
    }

    protected Connect connection() throws VirtException {
        if (connect==null && getUser()!=null && getUser().getName()!=null) {
            try{
                String connectionString = getVirtualizationConfig().getConnectionString();
                if (getUser().getAuthMethod().length()>0) {
                    connectionString = connectionString.replace("#{auth.sep}", "+");
                    connectionString = connectionString.replace("#{auth.method}", getUser().getAuthMethod());
                } else {
                    connectionString = connectionString.replace("#{auth.sep}", "");
                    connectionString = connectionString.replace("#{auth.method}", "");
                }

                connectionString = connectionString.replace("#{user.name}", getUser().getName());
                connectionString = connectionString.replace("#{target.host}", getIpAddress());

                System.out.println("Connecting to " + connectionString);
                connect = new Connect(connectionString);
            } catch (VirtException e){
                System.out.println("exception caught:"+e);
                throw e;
            }
        }
        return connect;
    }

    private void populate() throws VirtException {
        if (getIpAddress()==null) {
            RuntimeContext.logger.log(Level.INFO, "Cannot find IP address for " + getName());
            return;
        }
        try {
            Connect connection = connection();
            if (connection!=null) {
                populateStoragePools(connection().listStoragePools());
                Collection<StorageVol> storageVols = new ArrayList<StorageVol>();
                for (StoragePool pool : storagePools.values()) {
                    for (StorageVol vol : pool.volumes()) {
                        storageVols.add(vol);
                    }
                }
                for (int domainId : connection().listDomains())  {
                    try {
                        populateDomain(domainId, storageVols);
                    } catch (VirtException e) {
                        // the virtual machine may have disappeared.
                        for (int d : connection().listDomains()) {
                            if (d == domainId) {
                                throw e;
                            }
                        }
                    }
                }
                for (String domainId : connection().listDefinedDomains())  {
                    try {
                        populateDomain(domainId, storageVols);
                    } catch (VirtException e) {
                        // the virtual machine may have disappeared.
                        for (String d : connection().listDefinedDomains()) {
                            if (d.equals(domainId)) {
                                throw e;
                            }
                        }
                    }
                }
            }

        } catch(VirtException e) {
            RuntimeContext.logger.log(Level.SEVERE, "Exception while populating list of domains ", e);
            throw e;
        }

    }

    private void populateStoragePools(String[] poolsNames) throws VirtException {
        for (String poolName : poolsNames) {
            populateStoragePool(poolName);
        }
    }

    private void populateStoragePool(String name) throws VirtException {
        LibVirtStoragePool gfPool = new LibVirtStoragePool(this, connection().storagePoolLookupByName(name));
        storagePools.put(name, gfPool);
    }

    private void populateDomain(int domainId, Collection<StorageVol> volumes) throws VirtException {
        addDomain(connection().domainLookupByID(domainId), volumes);
    }

    private void populateDomain(String domainId, Collection<StorageVol> volumes) throws VirtException {
        addDomain(connect.domainLookupByName(domainId), volumes);
    }


    private void addDomain(Domain domain, Collection<StorageVol> volumes) throws VirtException {
        String domainName = domain.getName();
        if (!domains.containsKey(domainName)) {
            for (Cluster cluster : domainConfig.getClusters().getCluster()) {
                for (VirtualMachineConfig vmc : cluster.getExtensionsByType(VirtualMachineConfig.class)) {
                    if (vmc.getName().equals(domainName)) {
                        List<StorageVol> storageVols = new ArrayList<StorageVol>();
                        for (StorageVol storageVol : volumes) {
                            if (storageVol.getName().startsWith(domainName)) {
                                storageVols.add(storageVol);
                            }
                        }
                        LibVirtVirtualMachine gfVM = new LibVirtVirtualMachine(vmc, vmc.getTemplate().getUser(), this, domain, storageVols);
                        domains.put(domainName, gfVM );
                        return;
                    }
                }
            }
            // if we end up, that means this virtual machine is not one managed by this group master
        }
    }

    public PhasedFuture<AllocationPhase, VirtualMachine> create(
                final TemplateInstance template,
                final VirtualCluster cluster,
                final EventSource<AllocationPhase> source)

            throws VirtException, IOException {

        populate();

        source.fireEvent(AllocationPhase.VM_PREPARE);

        final String name = cluster.getConfig().getName() + cluster.allocateToken();

        // 2. load the xml dump from the template ?
        File xml = template.getFileByExtension("xml");
        Element xmlConfig = loadConfigFile(xml);

        List<StorageVol> volumes = prepare(template, name, cluster);

        File machineDisks = absolutize(new File(virtualizations.getDisksLocation(), serverPool.getName()));
        machineDisks = new File(machineDisks, getName());

        File custDirectory = prepareCustDirectory(name, cluster.getConfig(), template.getConfig());
        final File custFile = new File(machineDisks, name + "cust.iso");
        prepareCustomization(custDirectory, custFile,  name);

        // copy the customization file over.
        final String diskLocation = config.getDisksLocation();
        execute(new MachineOperations<Object>() {
            @Override
            public Object run(FileOperations fileOperations) throws IOException {
                int maxTries=5;
                while(maxTries>0) {
                    try {
                        RuntimeContext.logger.log(Level.INFO, "Transfer of customization disk started");
                        fileOperations.copy(custFile, new File(diskLocation));
                        RuntimeContext.logger.log(Level.INFO, "Transfer of customization disk finished");
                        return null;
                    } catch (IOException e) {
                        RuntimeContext.logger.log(Level.SEVERE, "Cannot copy customization disk to target machine", e);
                        maxTries--;
                        if (maxTries==0) throw e;
                        String remotePath = new File(diskLocation, custFile.getName()).getPath();
                        RuntimeContext.logger.info("Deleting invalid copy at " + remotePath);
                        try {
                            fileOperations.delete(remotePath);
                        } catch (IOException e1) {
                            // ignore.
                        }
                        RuntimeContext.logger.log(Level.INFO, "Retrying copy...");
                    }
                }
                throw new IOException("Dead code, file a bug");
            }
        });


        OsInterface os = services.forContract(OsInterface.class).get();

        // 3. get the uuid for the new machine ?
        String uuid = UUID.randomUUID().toString();
        // 3. generate mac address
        String macAddress = os.macAddressGen();

        xmlConfig.setAttribute("type", getVirtualizationConfig().getName());
        NodeList children = xmlConfig.getChildNodes();
        for (int k=0;k<children.getLength();k++) {

            Node node = children.item(k);
            if (node.getNodeName().equals("name")) {
                node.getChildNodes().item(0).setNodeValue(name);
            }
            if (node.getNodeName().equals("uuid")) {
                node.getChildNodes().item(0).setNodeValue(uuid);
            }
            if (node.getNodeName().equals("devices")) {
                NodeList devices =  node.getChildNodes();
                for (int i=0;i<devices.getLength();i++) {
                    Node device = devices.item(i);
                    if (device.getNodeName().equals("disk")) {
                        node.removeChild(device);
                    }

                    if (device.getNodeName().equals("interface") && device.getAttributes().getNamedItem("type").getNodeValue().equals("bridge")) {
                        NodeList intfInfos = device.getChildNodes();
                        for (int j=0;j<intfInfos.getLength();j++) {
                            if (intfInfos.item(j).getNodeName().equals("mac")) {
                                intfInfos.item(j).getAttributes().getNamedItem("address").setNodeValue(macAddress);
                            }
                        }
                    }
                    if (device.getNodeName().equals("emulator")) {
                        device.getChildNodes().item(0).setNodeValue(getVirtualizationConfig().getEmulatorPath());
                    }
                }
                // add our volumes
                int position=0;
                for (StorageVol aVol : volumes) {
                    if (aVol instanceof LibVirtStorageVol) {
                        Node newNode = ((LibVirtStorageVol) aVol).getXML(node, position++);
                        node.appendChild(newNode);
                    }
                }
                // make the volume
                DiskReference cdRom = new CDRomDisk();
                Node cdRomNode = cdRom.save(this.getUserHome() + "/" + config.getDisksLocation() + "/" + custFile.getName(), node, 0);
                node.appendChild(cdRomNode);
            }
        }

        // write out to a temporary file.
        File destXml = new File(System.getProperty("java.io.tmpdir"), name + ".xml");
        writeConfig(xmlConfig, destXml);
        RuntimeContext.logger.info("XML definition file for VM at " + destXml.getAbsolutePath());

        System.out.println("I would use " + uuid + " id with mac " + macAddress);

        LibVirtVirtualMachine vm=null;
        try {
            Domain domain = connection().domainDefineXML(getConfig(xmlConfig));
            source.fireEvent(AllocationPhase.VM_SPAWN);
            final CountDownLatch latch = vmLifecycle.inStartup(name);
            VirtualMachineConfig vmConfig = VirtualMachineConfig.Utils.create(
                    domain.getName(),
                    template.getConfig(),
                    serverPool.getConfig(),
                    cluster.getConfig());

            vm = new LibVirtVirtualMachine(vmConfig,
                    template.getConfig().getUser(), this, domain, volumes);
                domains.put(name, vm);
                cluster.add(vm);

            ListenableFutureImpl<AllocationPhase, VirtualMachine> future =
                    new ListenableFutureImpl<AllocationPhase, VirtualMachine>(latch, vm, source);

            future.fireEvent(AllocationPhase.VM_START);
            vmLifecycle.start(vm);

            return future;
        } catch(VirtException e) {
            try {
                RuntimeContext.logger.log(Level.SEVERE, "Exception while allocating the virtual machine", e);
                if (vm!=null) {
                    vm.delete();
                }
                for (StorageVol volume : volumes) {
                    volume.delete();
                }
            } catch (VirtException e1) {
                RuntimeContext.logger.log(Level.SEVERE, "Exception while cleaning failed virtual machine creation", e1);
            }
            throw new VirtException(e);
        }
    }

    private Element loadConfigFile(File xml) {
        ClassLoader tcc = Thread.currentThread().getContextClassLoader();
        try {
          // first of all we request out
          // DOM-implementation:
          Thread.currentThread().setContextClassLoader(clh.getCommonClassLoader());
          DocumentBuilderFactory factory =
            DocumentBuilderFactory.newInstance();
          // then we have to create document-loader:
          DocumentBuilder loader = factory.newDocumentBuilder();

          // loading a DOM-tree...
          Document document = loader.parse(xml);
          // at last, we get a root element:
         return  document.getDocumentElement();

        } catch (IOException ex) {
          // any IO errors occur:
          handleError(ex);
        } catch (SAXException ex) {
          // parse errors occur:
          handleError(ex);
        } catch (ParserConfigurationException ex) {
          // document-loader cannot be created which,
          // satisfies the configuration requested
          handleError(ex);
        } catch (FactoryConfigurationError ex) {
          // DOM-implementation is not available
          // or cannot be instantiated:
          handleError(ex);
        } finally {
            Thread.currentThread().setContextClassLoader(tcc);
        }
        return null;
    }

    private void writeConfig(Node doc, File destination)  {
        try {
            write(doc, new StreamResult(destination));
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private String getConfig(Node doc) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            write(doc, new StreamResult(baos));
            return baos.toString();
        } catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void write(Node doc, Result result) throws TransformerException {
        // Write the DOM document to the file
        ClassLoader tcc = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(clh.getCommonClassLoader());
        Transformer xformer = TransformerFactory.newInstance().newTransformer();
        xformer.transform(new DOMSource(doc), result);
        Thread.currentThread().setContextClassLoader(tcc);
    }

    private void handleError(Throwable e) {
        e.printStackTrace();
    }
}
