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

import java.net.URL;
import java.util.*;
import javax.wsdl.*;
import javax.wsdl.extensions.mime.MIMEContent;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.wsdl.extensions.soap.*;
import java.util.logging.*;
import javax.xml.soap.SOAPMessage;
import javax.xml.namespace.QName;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import com.sun.logging.LogDomains;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.factory.WSDLFactory;

/**
 * Represents wsdl metadata of an endpoint. This contains
 * partially resolved operation names, which will be compared
 * with the soap message for getting the exact operation name.
 *
 * @author Binod PG
 */
public class EndpointMetaData {
    
    /**
     * Internal handle to the logger instance
     */
    protected static final Logger logger =
            LogDomains.getLogger(EndpointMetaData.class, LogDomains.SERVER_LOGGER);
    
    private Definition def = null;
    private QName serviceName = null;
    private String epName = null;
    
    private String operationName;
    private OperationMetaData[] opmds = null;
    
    /**
     * Constructor. Saves required information for resolving the operation.
     */
    public EndpointMetaData(Definition def, QName serviceName, String epName) {
        this.def = def;
        this.serviceName = serviceName;
        this.epName = epName;
    }
    
    public EndpointMetaData(URL wsdlLocation, QName serviceName, String epName) {
        this.def = readWSDLDefinition(wsdlLocation);
        this.serviceName = serviceName;
        this.epName = epName;
    }
    
    private Definition readWSDLDefinition(URL wsdlLocation) {
        try {
            WSDLFactory mFactory = WSDLFactory.newInstance();
            javax.wsdl.xml.WSDLReader mReader = mFactory.newWSDLReader();
            return mReader.readWSDL(wsdlLocation.toURI().toString());
        } catch(Exception ex) {
            logger.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }
    }
    
    /**
     * Constructor without service name and endpoint name.
     * Such an endpoint metadata is just a holder object
     * for wsdl definition object.
     */
    public EndpointMetaData(Definition def) {
        this.def = def;
    }
    
    /**
     * Partially resolve the operation name.
     *
     * 1. If there is only one operation, use that as the operation name.
     * 2. If there is more than one operation name, save the input parameter
     *    for matching with the soap message.
     * 3. Since we want to work with any kind of binding, we need to only
     *    consider the abstract WSDL.
     */
    public void resolve() {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Resolving the WSDL for : " + serviceName + " : " + epName);
        }
        Binding binding = getBinding(def, serviceName, epName);
        javax.wsdl.Operation[] ops = getOperations(binding);
        
        if (ops.length < 1) {
            throw new RuntimeException("WSDL operation not resolved");
        }
        
        if (ops.length == 1) {
            this.operationName = ops[0].getName();
        }
        
        opmds = new OperationMetaData[ops.length];
        int i = 0 ;
        for (javax.wsdl.Operation op : ops) {
            Message input = null;
            OperationMetaData md = new OperationMetaData();
            BindingOperation bindingOp = getBindingOperation(op, binding);
            if (op.getInput() != null) {
                input = op.getInput().getMessage();
                md.setInputMessage(input);
                md.setOperationInputName(op.getInput().getName());
                md.setInputPartBindings(getPartBindings(
                        bindingOp.getBindingInput().getExtensibilityElements(),
                        input));
            }
            Set s = getInputPartNames(input);
            md.setOperationName(op.getName());
            md.setInputParameters(s);
            if(op.getOutput() != null) {
                md.setOperationOutputName(op.getOutput().getName());
                md.setOutputMessage(op.getOutput().getMessage());
                md.setOutputPartBindings(getPartBindings(
                        bindingOp.getBindingOutput().getExtensibilityElements(),
                        op.getOutput().getMessage()));
                
            }
            md.setBindingStyle(getStyleFor(op, binding));
            md.setFaults(op.getFaults());
            opmds[i++] = md;
        }
    }
    
    private void setPartBinding(int[] partBindings, List<String> orderedParts, ExtensibilityElement extElem) {
        if(extElem instanceof SOAPBody) {
            SOAPBody body = (SOAPBody) extElem;
            List<String> parts = body.getParts();
            if(parts != null) {
                for(String part : parts) {
                    int index = orderedParts.indexOf(part);
                    if(index != -1) {
                        partBindings[index] = SOAPConstants.SOAP_BODY_BINDING;
                    }
                }
            }
        } else if(extElem instanceof SOAPHeader) {
            SOAPHeader header = (SOAPHeader)extElem;
            String part = header.getPart();
            if(part != null) {
                int index = orderedParts.indexOf(part);
                if(index != -1) {
                    partBindings[index] = SOAPConstants.SOAP_HEADER_BINDING;
                }
            }
        } else if(extElem instanceof MIMEContent) {
            MIMEContent mimeContent = (MIMEContent)extElem;
            String part = mimeContent.getPart();
            if(part != null) {
                int index = orderedParts.indexOf(part);
                if(index != -1) {
                    partBindings[index] = SOAPConstants.SOAP_ATTACHMENT_BINDING;
                }
            }
        }
    }
    
    private int[] getPartBindings(List<ExtensibilityElement> extensibleElems, Message message) {
        
        List<String> orderedParts = new ArrayList<String>();
        for(Part p : (List<Part>)message.getOrderedParts(null)) {
            orderedParts.add(p.getName());
        }
        int partBindings[] = new int[orderedParts.size()]; // all default to SOAPConstants.SOAP_BODY_BINDING
        
        for(ExtensibilityElement extElem : extensibleElems) {
            if(extElem instanceof MIMEMultipartRelated) {
                MIMEMultipartRelated mpr = (MIMEMultipartRelated) extElem;
                for(MIMEPart mp : (List<MIMEPart>) mpr.getMIMEParts()) {
                    for(ExtensibilityElement mpe : (List<ExtensibilityElement>)mp.getExtensibilityElements()) {
                        setPartBinding(partBindings, orderedParts, mpe);
                    }
                }
            } else {
                setPartBinding(partBindings, orderedParts, extElem);
            }
        }
        
        //Debug
        if(logger.isLoggable(Level.FINE)) {
            for(int i=0; i<partBindings.length; i++) {
                logger.log(Level.FINE, "PartName = " + orderedParts.get(i) + ", PartBinding : " + partBindings[i]);
            }
        }
        
        return partBindings;
    }
    
    private BindingOperation getBindingOperation(
            javax.wsdl.Operation interfaceOperation,
            javax.wsdl.Binding binding) {
        String inputMsgName = interfaceOperation.getInput() != null
                ? interfaceOperation.getInput().getName()
                : null;
        String outputMsgName = interfaceOperation.getOutput() != null
                ? interfaceOperation.getOutput().getName()
                : null;
        BindingOperation bindingOperation = binding.getBindingOperation(
                interfaceOperation.getName(),
                inputMsgName,
                outputMsgName
                );
        if(bindingOperation == null) {
            bindingOperation = binding.getBindingOperation(
                    interfaceOperation.getName(), null, null);
        }
        return bindingOperation;
    }
    
    private String getStyleFor(javax.wsdl.Operation interfaceOperation,
            javax.wsdl.Binding binding) {
        String style = null;
        BindingOperation bindingOperation = getBindingOperation(interfaceOperation, binding);
        if(bindingOperation != null) {
            style = getStyleFor(bindingOperation);
            if(style == null) {
                style = getStyleFor(binding);
            }
        }
        return style;
    }
    
    private String getStyleFor(javax.wsdl.Binding binding) {
        String sty = null;
        
        java.util.List extList = binding.getExtensibilityElements();
        if (extList != null) {
            for (int i = 0; i < extList.size(); i++) {
                if ( extList.get(i) instanceof SOAPBinding) {
                    SOAPBinding sb = (SOAPBinding) extList.get(i);
                    sty = sb.getStyle();
                    break;
                }
            }
        }
        return sty;
    }
    
    private String getStyleFor(javax.wsdl.BindingOperation bo) {
        String style = null;
        java.util.List extList = bo.getExtensibilityElements();
        if (extList != null) {
            for (int i = 0; i < extList.size(); i++) {
                if ( extList.get(i) instanceof SOAPOperation) {
                    SOAPOperation sa = (SOAPOperation) extList.get(i);
                    style = sa.getStyle();
                    break;
                }
            }
        }
        return style;
    }
    
    public String getOperationName() {
        return this.operationName;
    }
    
    public String getOperationName(SOAPMessage soapMsg) {
        if (this.operationName != null) {
            return this.operationName;
        }
        
        List nodeNames = new ArrayList();
        try {
            javax.xml.soap.SOAPBody soapBody = soapMsg.getSOAPBody();
            NodeList nl = soapBody.getChildNodes();
            
            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                
                if (Node.ELEMENT_NODE == n.getNodeType()) {
                    String nodeName = n.getLocalName();
                    if (nodeName != null) {
                        return nodeName;
                        //nodeNames.add(nodeName);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        for (OperationMetaData om : opmds) {
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Matching for" + om.getOperationName());
            }
            Set inputs = om.getInputParameters();
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Inputs" + inputs);
                logger.finest("Nodes" + nodeNames);
            }
            if (inputs.containsAll(nodeNames)) {
                return om.getOperationName();
            }
        }
        return null;
    }
    
    public Definition getDefinition() {
        return def;
    }
    
    private Set getInputPartNames(Message msg) {
        Set set = new HashSet();
        
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Getting input parameters for: "+ msg);
        }
        if (msg != null) {
            Iterator bodyIterator = null;
            Map msgParts = msg.getParts();
            if (logger.isLoggable(Level.FINE)) {
                logger.fine("Message Parts are: "+ msgParts);
            }
            if (msgParts != null) {
                bodyIterator = msgParts.keySet().iterator();
            }
            // construct a set of expected names
            while (bodyIterator != null && bodyIterator.hasNext()) {
                String bodyPart = (String) bodyIterator.next();
                Part part = msg.getPart(bodyPart);
                if (part == null) {
                    throw new IllegalStateException("WSDL error");
                }
                QName typeQName = part.getTypeName();
                QName elemQName = part.getElementName();
                
                if (typeQName != null) {
                    // it uses type, so the root node name is the part name
                    set.add(part.getName());
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Added partName: "+ part.getName());
                    }
                } else if (elemQName != null) {
                    //it uses element, so the root node name is the element name
                    if (logger.isLoggable(Level.FINE)) {
                        logger.fine("Added root node: "+ elemQName.getLocalPart());
                        logger.fine("Part name is : " + part.getName());
                    }
                    set.add(elemQName.getLocalPart());
                }
            }
        }
        
        return set;
    }
    
    public Message getOutputMessage(String operationName) {
        for (OperationMetaData om : opmds) {
            if(operationName.equals(om.getOperationName())) {
                return om.getOutputMessage();
            }
        }
        return null;
    }
    
    private boolean isSimpleType(Part part) {
        QName parttype = part.getTypeName();
        if (parttype != null) {
            String s = parttype.getNamespaceURI();
            if ( s != null && s.trim().equals("http://www.w3.org/2001/XMLSchema")) {
                return true;
            }
        }
        return false;
    }
    
    private String fixNull(String s) {
        return s == null ? "" : s;
    }
    
    /** 
     * Checks whether the localName & namespaceUri of the qName
     * matches with the given localName & nsUri.
     */
    private boolean match(QName qName, String localName, String nsUri) {
        if(qName == null) {
            return false;
        }
        String qLocalName = fixNull(qName.getLocalPart());
        String qNsUri = fixNull(qName.getNamespaceURI());
        localName = fixNull(localName);
        nsUri = fixNull(nsUri);
        
        if(qLocalName.equals(localName) && qNsUri.equals(nsUri)) {
            return true;
        }
        return false;
    }
    
    public Message getFaultMessage(String operationName, 
            String faultDetailElementName,
            String faultDetailNsUri) {
        for (OperationMetaData om : opmds) {
            if(operationName.equals(om.getOperationName())) {
                Map<String,Object> m = om.getFaults();
                for(String s : m.keySet()) {
                    Message msg = ((Fault)m.get(s)).getMessage();
                    List<Part> orderedParts = msg.getOrderedParts(null);
                    for(Part p : orderedParts) {
                        if(match(p.getElementName(),
                                faultDetailElementName, faultDetailNsUri)) {
                            return msg;
                        }
                        if(match(p.getTypeName(),
                                faultDetailElementName, faultDetailNsUri)) {
                            return msg;
                        }
                        if(isSimpleType(p) && faultDetailElementName == null) {
                            return msg;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    public String getOperationOutputName(String operationName) {
        for (OperationMetaData om : opmds) {
            if(operationName.equals(om.getOperationName())) {
                return om.getOperationOutputName();
            }
        }
        return null;
    }
    
    public Message getInputMessage(String operationName) {
        for (OperationMetaData om : opmds) {
            if(operationName.equals(om.getOperationName())) {
                return om.getInputMessage();
            }
        }
        return null;
    }
    
    public int[] getInputPartBindings(String operationName) {
        for (OperationMetaData om : opmds) {
            if(operationName.equals(om.getOperationName())) {
                return om.getInputPartBindings();
            }
        }
        return null;
    }
    
    public int[] getOutputPartBindings(String operationName) {
        for (OperationMetaData om : opmds) {
            if(operationName.equals(om.getOperationName())) {
                return om.getOutputPartBindings();
            }
        }
        return null;
    }
    
    public String getOperationInputName(String operationName) {
        for (OperationMetaData om : opmds) {
            if(operationName.equals(om.getOperationName())) {
                return om.getOperationInputName();
            }
        }
        return null;
    }
    
    public String getBindingStyle(String operationName) {
        for (OperationMetaData om : opmds) {
            if(operationName.equals(om.getOperationName())) {
                return om.getBindingStyle();
            }
        }
        return null;
    }
    
    public static javax.wsdl.Operation[] getOperations(Binding binding) {
        if (binding != null) {
            PortType pt = binding.getPortType();
            if (pt != null) {
                List l = pt.getOperations();
                if (l != null && l.size() > 0) {
                    return (javax.wsdl.Operation[])
                    l.toArray(new javax.wsdl.Operation[0]);
                }
            }
        }
        return null;
    }
    
    public static Binding getBinding(Definition def, QName serviceName, String endpointName) {
        String location = null;
        Service svc = def.getService(serviceName);
        if (svc == null) {
            return null;
        }
        Port port = svc.getPort(QName.valueOf(endpointName).getLocalPart());
        if (port == null) {
            return null;
        } else {
            return port.getBinding();
        }
    }
    
    /**
     * Class that holds operation and input parameters.
     */
    private class OperationMetaData {
        private String operationName;
        private String operationInputName; // operation's input name.
        private String operationOutputName; // operation's output name.
        private Set inputParams;
        private Message outputMessage;
        private Message inputMessage;
        private String bindingStyle;
        private Map<String,Object> faults; // operation's faults.
        private int[] inputPartBindings; // represent which part is bound to what?
        private int[] outputPartBindings; // represent which part is bound to what?
        
        void setOperationName(String name) {
            this.operationName = name;
        }
        
        void setInputParameters(Set params) {
            this.inputParams = params;
        }
        
        String getOperationName() {
            return this.operationName;
        }
        
        Set getInputParameters() {
            return inputParams;
        }
        
        Message getOutputMessage() {
            return outputMessage;
        }
        
        void setOutputMessage(Message outputMessage) {
            this.outputMessage = outputMessage;
        }
        
        public String getOperationOutputName() {
            return operationOutputName;
        }
        
        public void setOperationOutputName(String operationOutputName) {
            this.operationOutputName = operationOutputName;
        }
        
        String getBindingStyle() {
            return bindingStyle;
        }
        
        void setBindingStyle(String bindingStyle) {
            this.bindingStyle = bindingStyle;
        }
        
        public Message getInputMessage() {
            return inputMessage;
        }
        
        public void setInputMessage(Message inputMessage) {
            this.inputMessage = inputMessage;
        }
        
        public String getOperationInputName() {
            return operationInputName;
        }
        
        public void setOperationInputName(String operationInputName) {
            this.operationInputName = operationInputName;
        }
        
        public Map<String,Object> getFaults() {
            return faults;
        }
        
        public void setFaults(Map<String,Object> faults) {
            this.faults = (faults == null) ? new HashMap<String,Object>() : faults;
        }
        
        public int[] getInputPartBindings() {
            return inputPartBindings;
        }
        
        private void setInputPartBindings(int[] inputPartBindings) {
            this.inputPartBindings = inputPartBindings;
        }
        
        public int[] getOutputPartBindings() {
            return outputPartBindings;
        }
        
        public void setOutputPartBindings(int[] outputPartBindings) {
            this.outputPartBindings = outputPartBindings;
        }
        
    }
}
