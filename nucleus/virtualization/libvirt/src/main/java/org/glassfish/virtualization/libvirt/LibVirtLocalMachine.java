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
import org.glassfish.virtualization.libvirt.jna.Connect;
import org.glassfish.virtualization.libvirt.jna.Domain;
import org.glassfish.virtualization.runtime.*;
import org.glassfish.virtualization.spi.*;
import org.glassfish.virtualization.spi.VirtualMachine;
import org.glassfish.virtualization.util.EventSource;
import org.glassfish.virtualization.util.EventSourceImpl;
import org.glassfish.virtualization.util.ListenableFutureImpl;
import org.glassfish.virtualization.util.RuntimeContext;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.component.Habitat;
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

/**
 * Abstraction for this machine, assumptions are being made that this java process runs with
 * with the same user's credentials that libvirt is expecting to manage the virtualization environment
 * of this machine.
 *
 * @author  Jerome Dochez
 */
public class LibVirtLocalMachine extends LocalMachine implements PostConstruct {


    final Map<String, LibVirtVirtualMachine> domains = new HashMap<String, LibVirtVirtualMachine>();
    final Map<String, LibVirtStoragePool> storagePools = new HashMap<String, LibVirtStoragePool>();

    Connect connect;

    @Inject
    Virtualizations virtualizations;

    @Inject
    Services services;

    @Inject
    VirtualMachineLifecycle vmLifecycle;

    public static LibVirtLocalMachine from(Injector injector, LibVirtGroup group, MachineConfig config) {
        return injector.inject(new LibVirtLocalMachine(injector, group, config));
    }

    protected  LibVirtLocalMachine(Injector injector, LibVirtGroup group, MachineConfig config) {
        super(injector, group, config);
    }

    @Override
    public void postConstruct() {
        setState(isUp()? LibVirtMachine.State.READY: LibVirtMachine.State.SUSPENDED);

        // by default all our templates are local to this machine until the TemplateTask ran
        Virtualization virt= serverPool.getConfig().getVirtualization();
        for (Template template : virtualizations.getTemplates() ) {
            installedTemplates.put(template.getName(),
                    new LocalTemplate(virtualizations.getTemplatesLocation(), template));
        }
        if (getState().equals(LibVirtMachine.State.READY)) {
            RuntimeContext.es.submit(new TemplateTask());
        }
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
                       "      <serverPool>").append(getUser().getUserId()).append("</serverPool>\n").append(
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

    protected Connect connection() throws VirtException {
        if (connect==null) {
            try{
                String connectionString = getEmulator().getConnectionString();
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

    private final class TemplateTask implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            for (VMTemplate template : LibVirtLocalMachine.this.installedTemplates.values()) {
                try {
                    if (template.isLocal())  {
                        template.copyTo(LibVirtLocalMachine.this, LibVirtLocalMachine.this.config.getTemplatesLocation());
                        LibVirtLocalMachine.this.installedTemplates.put(template.getDefinition().getName(),
                            new RemoteTemplate(LibVirtLocalMachine.this,
                                    LibVirtLocalMachine.this.config.getTemplatesLocation(),
                                    template.getDefinition()));
                    }
                } catch (IOException e) {
                    // ignore, logging should have already been provided.
                }

            }
            return null;
        }
    }

    private void populate() throws VirtException {
        try {
            populateStoragePools(connection().listStoragePools());
            populateDomain(connection().listDomains());
            populateDomain(connection().listDefinedDomains());

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

    private void populateDomain(int[] domainIds) throws VirtException {
        for (int domainId : domainIds) {
            addDomain(connection().domainLookupByID(domainId));
        }
    }

    private void populateDomain(String[] domainIds) throws VirtException {
        for (String domainId : domainIds) {
            addDomain(connect.domainLookupByName(domainId));
        }
    }

    private void addDomain(Domain domain) throws VirtException {
        String domainName = domain.getName();
        if (!domains.containsKey(domainName)) {
            VirtualMachineConfig vmc = serverPool.getConfig().virtualMachineRefByName(domainName);
            // if config is null, that means this VM is not one of ours.
            if (vmc!=null) {
                TemplateRepository templateRepository = services.forContract(TemplateRepository.class).get();
                TemplateInstance templateInstance = templateRepository.byName(vmc.getTemplate().getName());
                LibVirtVirtualMachine gfVM = new LibVirtVirtualMachine(this, domain);
                domains.put(domainName, gfVM );
            }
        }
    }

    public ListenableFuture<AllocationPhase, VirtualMachine> create(
                final TemplateInstance template,
                final VirtualCluster cluster,
                final EventSource<AllocationPhase> source)

            throws VirtException, IOException {

        populate();

        source.fireEvent(AllocationPhase.VM_PREPARE);

        final String name = cluster.getConfig().getName() + cluster.allocateToken();

        // 2. load the xml dump from the template ?
        File xml = new File(new File(virtualizations.getTemplatesLocation(), template.getConfig().getName()),
                template.getConfig().getName() + ".xml");
        Element vmConfig = loadConfigFile(xml);

        List<StorageVol> volumes = prepare(template.getConfig(), name, cluster);

        File machineDisks = absolutize(new File(virtualizations.getDisksLocation(), serverPool.getName()));
        machineDisks = new File(machineDisks, getName());

        File custDirectory = prepareCustDirectory(name, cluster.getConfig(), template.getConfig());
        File custFile = new File(machineDisks, name + "cust.iso");
        prepareCustomization(custDirectory, custFile,  name);

        // copy the customization file over.
        final String diskLocation = config.getDisksLocation();
        delete(diskLocation + "/" + custFile.getName());
        copy(custFile, new File(diskLocation));

        OsInterface os = services.forContract(OsInterface.class).get();

        // 3. get the uuid for the new machine ?
        String uuid = UUID.randomUUID().toString();
        // 3. generate mac address
        String macAddress = os.macAddressGen();

        vmConfig.setAttribute("type", getEmulator().getVirtType());
        NodeList children = vmConfig.getChildNodes();
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
                        device.getChildNodes().item(0).setNodeValue(getEmulator().getEmulatorPath());
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
        File destXml = new File(System.getProperty("java.io.tmpdir"),"foo.xml");
        writeConfig(vmConfig, destXml);

        System.out.println("I would use " + uuid + " id with mac " + macAddress);


        try {
            Domain domain = connection().domainDefineXML(getConfig(vmConfig));
            source.fireEvent(AllocationPhase.VM_SPAWN);
            final CountDownLatch latch = vmLifecycle.inStartup(name);
            final LibVirtVirtualMachine vm = new LibVirtVirtualMachine(this, domain);
            domains.put(name, vm);
            cluster.add(template, vm);

            ListenableFutureImpl<AllocationPhase, VirtualMachine> future =
                    new ListenableFutureImpl<AllocationPhase, VirtualMachine>(latch, vm, source);

            future.fireEvent(AllocationPhase.VM_START);
            vmLifecycle.start(vm);

            return future;
        } catch(VirtException e) {
            for (StorageVol volume : volumes) {
                volume.delete();
            }
            throw new VirtException(e);
        }
    }

    private Element loadConfigFile(File xml) {
        try {
          // first of all we request out
          // DOM-implementation:
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
        Transformer xformer = TransformerFactory.newInstance().newTransformer();
        xformer.transform(new DOMSource(doc), result);
    }

    private void handleError(Throwable e) {
        e.printStackTrace();
    }
}
