/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.jbi.serviceengine.util.soap;

import com.ibm.wsdl.extensions.soap.SOAPAddressImpl;
import com.ibm.wsdl.extensions.soap.SOAPBindingImpl;
import com.ibm.wsdl.extensions.soap.SOAPBodyImpl;
import com.ibm.wsdl.extensions.soap.SOAPFaultImpl;
import com.ibm.wsdl.extensions.soap.SOAPHeaderImpl;
import com.ibm.wsdl.extensions.soap.SOAPOperationImpl;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.wsdl.Binding;
import javax.wsdl.BindingFault;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.extensions.ElementExtensible;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPFault;
import javax.wsdl.extensions.soap.SOAPHeader;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;
import org.w3c.dom.Element;

/**
 *
 * Converts any non-SOAP WSDL to a SOAP WSDL.
 *
 * If the original WSDL is a SOAP WSDL then it is not modified.
 *
 * @author bhavanishankar@dev.java.net
 */
public class WSDLConverter {
    
    ExtensionRegistry extnReg;
    WSDLReader wsdlReader;
    WSDLWriter wsdlWriter;
    
    public static final String SOAP_NS = "http://schemas.xmlsoap.org/wsdl/soap/";
    public static final String SOAP_PREFIX = "soap";
    public static final String SOAP_BINDING_SUFFIX = "binding";
    public static final String SOAP_OPERATION_SUFFIX = "operation";
    public static final String SOAP_ADDRESS_SUFFIX = "address";
    public static final String SOAP_BODY_SUFFIX = "body";
    public static final String SOAP_HEADER_SUFFIX = "header";
    public static final String SOAP_FAULT_SUFFIX = "fault";
    
    public static final String SOAP_HTTP_TRANSPORT_URI = "http://schemas.xmlsoap.org/soap/http";
    public static final String SOAP_ADDRESS_LOCATION = "REPLACE_WITH_ACTUAL_URL";
    
    public static final String RPC_STYLE = "rpc";
    public static final String DOCUMENT_STYLE = "document";
    public static final String USE_LITERAL = "literal";
    
    public WSDLConverter() throws Exception {
        
        WSDLFactory factory = WSDLFactory.newInstance();
        wsdlReader = factory.newWSDLReader();
        wsdlWriter = factory.newWSDLWriter();
        
        extnReg = new ExtensionRegistry();
        
        extnReg.mapExtensionTypes(Binding.class,
                new QName(SOAP_NS,SOAP_BINDING_SUFFIX),
                SOAPBindingImpl.class);
        
        extnReg.mapExtensionTypes(ExtensibilityElement.class,
                new QName(SOAP_NS,SOAP_OPERATION_SUFFIX),
                SOAPOperationImpl.class);
        
        extnReg.mapExtensionTypes(ExtensibilityElement.class,
                new QName(SOAP_NS,SOAP_ADDRESS_SUFFIX),
                SOAPAddressImpl.class);
        
        extnReg.mapExtensionTypes(SOAPBody.class,
                new QName(SOAP_NS,SOAP_BODY_SUFFIX),
                SOAPBodyImpl.class);
        
        extnReg.mapExtensionTypes(SOAPHeader.class,
                new QName(SOAP_NS,SOAP_HEADER_SUFFIX),
                SOAPHeaderImpl.class);
        
        extnReg.mapExtensionTypes(SOAPFault.class,
                new QName(SOAP_NS,SOAP_FAULT_SUFFIX),
                SOAPFaultImpl.class);
        
    }
    
    private SOAPBinding createSoapBinding() throws Exception {
        SOAPBinding soapBinding = (SOAPBinding)extnReg.createExtension(Binding.class,
                new QName(SOAP_NS,SOAP_BINDING_SUFFIX));
        soapBinding.setTransportURI(SOAP_HTTP_TRANSPORT_URI);
        return soapBinding;
    }
    
    private SOAPAddress createSoapAddress() throws Exception {
        SOAPAddress soapAddress = (SOAPAddress)extnReg.createExtension(ExtensibilityElement.class,
                new QName(SOAP_NS,SOAP_ADDRESS_SUFFIX));
        soapAddress.setLocationURI(SOAP_ADDRESS_LOCATION);
        return soapAddress;
    }
    
    private SOAPOperation createSoapOperation() throws Exception {
        SOAPOperation soapOperation = (SOAPOperation)extnReg.createExtension(
                ExtensibilityElement.class,
                new QName(SOAP_NS, SOAP_OPERATION_SUFFIX));
        return soapOperation;
    }
    
    private SOAPBody createSoapBody() throws Exception {
        SOAPBody soapBody = (SOAPBody)extnReg.createExtension(
                SOAPBody.class,
                new QName(SOAP_NS, SOAP_BODY_SUFFIX));
        soapBody.setUse(USE_LITERAL);
        return soapBody;
    }
    
    private SOAPHeader createSoapHeader() throws Exception {
        SOAPHeader soapHeader = (SOAPHeader)extnReg.createExtension(
                SOAPHeader.class,
                new QName(SOAP_NS, SOAP_HEADER_SUFFIX));
        soapHeader.setUse(USE_LITERAL);
        return soapHeader;
    }
    
    private SOAPFault createSoapFault() throws Exception {
        SOAPFault soapFault = (SOAPFault)extnReg.createExtension(
                SOAPFault.class,
                new QName(SOAP_NS, SOAP_FAULT_SUFFIX));
        soapFault.setUse(USE_LITERAL);
        return soapFault;
    }
    
    /**
     * Style can be calculated by looking into how the input/output parts are defined.
     * Style can not be calculated by looking into fault.
     *
     * WS-I Basic Profile 1.0 :
     *
     * R2203   An rpc-literal binding in a DESCRIPTION MUST refer,
     * in its soapbind:body element(s), only to wsdl:part element(s)
     * that have been defined using the type attribute.
     *
     * R2204   A document-literal binding in a DESCRIPTION MUST refer,
     * in each of its soapbind:body element(s), only to wsdl:part element(s)
     * that have been defined using the element attribute.
     *
     */
    private String getStyle(Operation portTypeOperation) {
        try {
            if(portTypeOperation.getInput() != null) {
                List<Part> parts = portTypeOperation.getInput().getMessage().getOrderedParts(null);
                for(Part p : parts) {
                    if(p.getTypeName() != null) {
                        return RPC_STYLE;
                    }
                }
            }
            if(portTypeOperation.getOutput() != null) {
                List<Part> parts = portTypeOperation.getOutput().getMessage().getOrderedParts(null);
                for(Part p : parts) {
                    if(p.getTypeName() != null) {
                        return RPC_STYLE;
                    }
                }
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return DOCUMENT_STYLE;
    }
    
    private void processElement(
            ElementExtensible elementExtensible,
            Message message,
            String style) throws Exception {
        if(RPC_STYLE.equals(style)) {
            SOAPBody soapBody = createSoapBody();
            /**
             * namespace needs to be set in <soap:body> only for 'rpc' style.
             *
             * WS-I Basic Profile 1.0 :
             *
             * R2716   A document-literal binding in a DESCRIPTION MUST NOT have
             * the namespace  attribute specified on contained soapbind:body,
             * soapbind:header, soapbind:headerfault and  soapbind:fault elements.
             *
             * R2717 : An rpc-literal binding in a DESCRIPTION MUST have
             * the namespace attribute specified, the value of which MUST
             * be an absolute URI, on contained  soapbind:body elements.
             *
             * R2726   An rpc-literal binding in a DESCRIPTION MUST NOT have
             * the namespace attribute specified on contained soapbind:header,
             * soapbind:headerfault and soapbind:fault elements.
             *
             */
            soapBody.setNamespaceURI(
                    message.getQName().getNamespaceURI());
            elementExtensible.addExtensibilityElement(soapBody);
        } else {
            List<Part> orderedParts = message.getOrderedParts(null);
            boolean partBoundToBody = false; // only one part can be bound to <soap:body>.
            for(int i=orderedParts.size()-1; i>=0; i--) {
                Part p = orderedParts.get(i);
                if( (p.getElementName().getLocalPart().equals
                        (message.getQName().getLocalPart())) // wrapped doc/literal
                        || (i==0 && !partBoundToBody) // bare doc/literal
                        ) {
                    partBoundToBody = true;
                    SOAPBody soapBody = createSoapBody();
                    List<String> parts = new ArrayList<String>();
                    parts.add(p.getName());
                    soapBody.setParts(parts);
                    elementExtensible.addExtensibilityElement(soapBody);
                } else {
                    SOAPHeader soapHeader = createSoapHeader();
                    soapHeader.setPart(p.getName());
                    soapHeader.setMessage(message.getQName());
                    elementExtensible.addExtensibilityElement(soapHeader);
                }
            }
        }
        //return elementExtensible;
    }
    
    /**
     * Converts all <wsdl:service> sections.
     */
    private boolean convertServices(Definition defn) throws Exception  {
        
        boolean wsdlModified = false;
        
        Map<QName,Service> services = defn.getServices();
        
        if(services == null || services.isEmpty()) {
            return wsdlModified;
        }
        
        Service[] originalServices = services.values().toArray(new Service[0]);
        
        for(Service originalService : originalServices) {
            
            Map<String,Port> originalPorts = originalService.getPorts();
            
            boolean isSOAP = false;
            Service newService = defn.createService();
            
            for(String key : originalPorts.keySet()) {
                
                Port originalPort = originalPorts.get(key);
                
                /**
                 * Step1. Skip if it is already SOAP
                 */
                List<ExtensibilityElement> extList = originalPort.getExtensibilityElements();
                for(ExtensibilityElement extElem : extList) {
                    if(extElem instanceof SOAPAddress) {
                        isSOAP = true;
                        break;
                    }
                }
                if(isSOAP) {
                    continue;
                }
                
                wsdlModified = true;
                
                /**
                 * Step2. Collect the required information from
                 * the existing <wsdl:port>
                 */
                String originalPortName = originalPort.getName();
                Element originalPortDocElem = originalPort.getDocumentationElement();
                Binding originalBinding = originalPort.getBinding();
                
                /**
                 * Step3. Create a new port and add it to <wsdl:service>
                 *
                 *  <wsdl:port name="#name" binding="#tns:binding">
                 *      <soap:address location="REPLACE_WITH_ACTUAL_URL"/>
                 *  </wsdl:port>
                 *
                 */
                Port newPort = defn.createPort();
                // Add -> name="#name"
                newPort.setName(originalPortName);
                
                // Add -> binding="#tns:binding"
                // Since 'originalBinding' might have been deleted already, so get the binding from defn
                newPort.setBinding(defn.getBinding(originalBinding.getQName()));
                newPort.setDocumentationElement(originalPortDocElem);
                
                // Add -> <soap:address location="REPLACE_WITH_ACTUAL_URL"/>
                SOAPAddress soapAddress = createSoapAddress();
                
                //sa.setRequired(true);
                newPort.addExtensibilityElement(soapAddress);
                
                /**
                 * Step4. Add the newly created <wsdl:port> to <wsdl:service>.
                 */
                newService.setQName(originalService.getQName());
                newService.addPort(newPort);
            }
            
            if(!isSOAP) {
                defn.removeService(originalService.getQName());
                defn.addService(newService);
            }
        }
        return wsdlModified;
    }
    
    /**
     * Converts all <wsdl:binding> sections.
     */
    private boolean convertBindings(Definition defn) throws Exception {
        
        boolean wsdlModified = false;
        
        Map<QName,Binding> bindings = defn.getBindings();
        
        if(bindings == null || bindings.isEmpty()) {
            return wsdlModified;
        }
        
        Binding[] originalBindings = bindings.values().toArray(new Binding[0]);
        
        for(Binding originalBinding : originalBindings) {
            
            /**
             * Step 1. Skip if it is already SOAP
             */
            boolean isSOAP = false;
            
            List<ExtensibilityElement> extList = originalBinding.getExtensibilityElements();
            for(ExtensibilityElement extElem : extList) {
                if(extElem instanceof SOAPBinding) {
                    isSOAP = true;
                    break;
                }
            }
            
            if(isSOAP) {
                continue;
            }
            
            wsdlModified = true;
            
            /**
             * Step 2. Collect the required information from
             * the existing <wsdl:port>
             */
            
            PortType p = originalBinding.getPortType();
            QName originalBindingQname = originalBinding.getQName();
            List<BindingOperation> originalBindingOps = originalBinding.getBindingOperations();
            
            
            /**
             * Step 3. Create a new binding
             *
             *  <wsdl:binding name="#name" type="#type">
             *      <soap:binding transport="http://schemas.xmlsoap.org/soap/http"/>
             *      <wsdl:operation name="#operation1">
             *          <soap:operation/>
             *          <wsdl:input>
             *              <soap:body use="literal" namespace="#namespace"/>
             *          </wsdl:input>
             *          <wsdl:output>
             *              <soap:body use="literal" namespace="#namespace"/>
             *          </wsdl:output>
             *          <wsdl:fault name="#faultname">
             *              <soap:fault name="#faultname" use="literal"/>
             *          </wsdl:fault>
             *          ...more faults...
             *      </wsdl:operation>
             *      ...more operations...
             *  </wsdl:binding>
             *
             */
            Binding newBinding = defn.createBinding();
            // Add -> name="#name"
            newBinding.setQName(originalBindingQname);
            // Add -> type="#type"
            newBinding.setPortType(p);
            newBinding.setUndefined(false);
            
            // Add -> <soap:binding tranport="http://schemas.xmlsoap.org/soap/http"
            SOAPBinding soapBinding = createSoapBinding();
            newBinding.addExtensibilityElement(soapBinding);
            
            /**
             * Add ->
             *
             *  <wsdl:operation name="#operation1">
             *      ....
             *  </wsdl:operation>
             *  ... more operations...
             *
             */
            String style = null;
            
            for(BindingOperation originalBindingOp : originalBindingOps) {
                BindingOperation newBindingOp = defn.createBindingOperation();
                
                newBindingOp.setOperation(originalBindingOp.getOperation());
                Operation portTypeOperation = newBindingOp.getOperation();
                
                /**
                 *
                 * Style should be set in <soap:binding/>, not in <soap:operation/>
                 *
                 * WS-I Basic Profile 1.0 :
                 *
                 * 5.6.3 Consistency of style Attribute :
                 *
                 * The style, "document" or "rpc", of an interaction is specified
                 * at the wsdl:operation level, permitting wsdl:bindings whose wsdl:operations
                 * have different styles. This has led to interoperability problems.
                 *
                 * R2705 A wsdl:binding in a DESCRIPTION MUST use either be a rpc-literal binding
                 * or a document-literal binding.
                 *
                 */
                if(style == null) {
                    style = getStyle(portTypeOperation);
                    if(RPC_STYLE.equals(style)) { // default style is 'document', no need to set.
                        soapBinding.setStyle(style);
                    }
                }
                
                // Add > name="#operation".
                newBindingOp.setName(originalBindingOp.getName());
                newBindingOp.setDocumentationElement(originalBindingOp.getDocumentationElement());
                
                // Add -> <soap:operation/>
                SOAPOperation soapOperation = createSoapOperation();
                newBindingOp.addExtensibilityElement(soapOperation);
                
                // Add -> <wsdl:input><soap:body use="literal" namespace="#namespace"/></wsdl:input>
                if(portTypeOperation.getInput() != null) {
                    BindingInput newBindingInput = defn.createBindingInput();
                    processElement(
                            newBindingInput,
                            portTypeOperation.getInput().getMessage(),
                            style);
                    newBindingOp.setBindingInput(newBindingInput);
                }
                
                // Add -> <wsdl:output><soap:body use="literal" namespace="#namespace"/></wsdl:output>
                if(portTypeOperation.getOutput() != null) {
                    BindingOutput newBindingOutput = defn.createBindingOutput();
                    processElement(
                            newBindingOutput,
                            portTypeOperation.getOutput().getMessage(),
                            style);
                    newBindingOp.setBindingOutput(newBindingOutput);
                }
                
                // Add -> <wsdl:fault name="faultname"><soap:fault use="literal" name="#faultname"/></wsdl:fault>....more faults...
                if(portTypeOperation.getFaults() != null) {
                    Map<String,Fault> faults = portTypeOperation.getFaults();
                    for(String faultName : faults.keySet()) {
                        BindingFault newBindingFault = defn.createBindingFault();
                        newBindingFault.setName(faultName);
                        
                        SOAPFault soapFault = createSoapFault();
                        soapFault.setName(faultName);
                        
                        newBindingFault.addExtensibilityElement(soapFault);
                        newBindingOp.addBindingFault(newBindingFault);
                    }
                }
                
                newBinding.addBindingOperation(newBindingOp);
            }
            
            /**
             * Step4. Replace the existing <wsdl:binding> with new one.
             */
            defn.removeBinding(originalBindingQname);
            defn.addBinding(newBinding);
        }
        
        return wsdlModified;
    }
    
    private void writeWSDL(Definition defn, String wsdl) throws Exception {
        
        defn.addNamespace(SOAP_PREFIX, SOAP_NS);
        /**
         * Take care of i18n while creating a FileWriter.
         */
        FileWriter fw = new FileWriter(wsdl);
        wsdlWriter.writeWSDL(defn, fw);
        fw.close();
    }
    
    public boolean convert(String wsdl) throws Exception {
        
        String wsdlLocationUri = new File(wsdl).toURI().toString();
        
        Definition defn = wsdlReader.readWSDL(wsdlLocationUri);
        
        boolean wsdlModified = false;
        /**
         * Modify the <wsdl:binding> sections.
         */
        wsdlModified = convertBindings(defn);
        
        /**
         * Modify the <wsdl:service> sections.
         */
        wsdlModified = convertServices(defn);
        
        if(wsdlModified) {
            System.out.println("Modified WSDL : " + wsdl);
            writeWSDL(defn, wsdl);
        }
        
        return wsdlModified;
    }
    
    List<String> listWSDLs(File baseDir) {
        List<String> files = new ArrayList();
        for(File f : baseDir.listFiles()) {
            if(f.isDirectory()) {
                files.addAll(listWSDLs(f));
            } else if(f.getAbsolutePath().endsWith("wsdl")) {
                files.add(f.getAbsolutePath());
            }
        }
        return files;
    }
    
    public List<String> convertAllWSDLs(String baseDir) {
        List<String> convertedWSDLs = new ArrayList<String>();
        List<String> wsdls = listWSDLs(new File(baseDir));
        for(String wsdl : wsdls) {
            try {
                if(convert(wsdl)) {
                    convertedWSDLs.add(wsdl.substring(baseDir.length() + 1));
                }
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
        System.out.println("BaseDir = " + baseDir + ", Converted WSDLs = " + convertedWSDLs);
        return convertedWSDLs;
    }
    
    public static List<String> convertWSDLs(String baseDir) {
        try {
            WSDLConverter converter = new WSDLConverter();
            return converter.convertAllWSDLs(baseDir);
        } catch(Exception ex) {
            ex.printStackTrace();
        }
        return new ArrayList<String>();
    }
    
    void print(String s) {
        System.out.println(s);
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        
        convertWSDLs("/tmp/wsdl");//System.getProperty("java.io.tmpdir"));
        
    }
    
    
}
