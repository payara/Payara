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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.StringTokenizer;

import java.net.HttpURLConnection;

import javax.jbi.JBIException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.MessagingException;

import javax.xml.soap.Detail;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.Name;

import javax.activation.DataHandler;

import org.w3c.dom.Document;
import org.w3c.dom.DOMException;
import org.w3c.dom.NodeList;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Source;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
/**
 * This implementation converts a SOAP request message to a JBI specific format which can
 * be understood by other JBI components.
 *
 * @author Sun Microsystems, Inc.
 */
public class MessageNormalizerImpl implements MessageNormalizer
{
    /**
     * Internal handle to the logger instance
     */
    private Logger mLogger;

    /**
     * Internal handle to String Translator instance.
     */
    private StringTranslator mStringTranslator;

    /**
     * Creates a new instance of MessageNormalizerImpl.
     *
     */
    public MessageNormalizerImpl()
    {
        mLogger = Logger.getLogger(this.getClass().getPackage().getName());
        mStringTranslator = new StringTranslator(this.getClass().getPackage().getName(), this.getClass().getClassLoader());
    }

    /**
     * Converts a SOAP Message to a NormalizedMessage format.
     *
     * @param soapWrapper request message.
     * @param normalizedMessage jbi specific format.
     * @param operation operation requested.
     *
     * @throws JBIException if the message cannot be normalized.
     */
    public void normalizeMessage(
        SOAPWrapper soapWrapper, NormalizedMessage normalizedMessage,
        Operation operation)
        throws JBIException
    {
        if ( soapWrapper.getStatus() == HttpURLConnection.HTTP_INTERNAL_ERROR )
        {
            normalizeFaultMessage( soapWrapper, normalizedMessage);
        }
        else
        {
            normalizeResponseMessage( soapWrapper, normalizedMessage, operation);
        }
    }

    /**
     * Converts a SOAP Fault Message to a NormalizedMessage format. 
     *
     * @param soapWrapper request message.
     * @param normalizedMessage jbi specific format.
     *
     * @throws JBIException if the message cannot be normalized.
     */
    public void normalizeFaultMessage(
        SOAPWrapper soapWrapper, NormalizedMessage normalizedMessage)
        throws JBIException
    {

        try
        {
            SOAPMessage soapMessage = soapWrapper.getMessage();

            if (soapMessage != null)
            {
                SOAPPart soapPart = soapMessage.getSOAPPart();
                SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
                SOAPBody soapBody = soapEnvelope.getBody();
                if ( soapBody.hasFault() )
                {
                    // The Message contains a fault detail element.
                    // Propogate the details in the message content.
                    // ,fault string  and fault actor in the message properties.

		    mLogger.fine(mStringTranslator.getString("SBC_FAULT_ELEMENT_FOUND"));

                    SOAPFault soapFault = soapBody.getFault();
                    Detail soapDetail = soapFault.getDetail();
                    if ( soapDetail != null )
                    {
                        normalizedMessage.setContent(
                                            new DOMSource(
                                                getChildElement(soapDetail)));
                        // Populate the SOAP Header into the message context
                        SOAPHeader soapHeader = soapEnvelope.getHeader();

                        if (soapHeader != null)
                        {
                            normalizedMessage.setProperty(
                                SOAPConstants.HEADER_PROPERTY_NAME,
                                soapHeader);
                        }
                        normalizedMessage.setProperty(
                                        SOAPConstants.FAULT_STRING_PROPERTY_NAME,
                                        soapFault.getFaultString());
                        normalizedMessage.setProperty(
                                        SOAPConstants.FAULT_CODE_PROPERTY_NAME,
                                        extractFaultCode( soapFault.getFaultCode()) );
                    }
                    else
                    {
                        // The Message does not contain fault detail. Propogate details
                        // as a JBIException.
                        throw new JBIException( soapFault.getFaultString() );
                    }


                }
                else
                {
                    // this should not happen.
		    mLogger.severe(mStringTranslator.getString("SBC_ALGORITHM_ERROR"));
                }
            }
        }
        catch (SOAPException soapException)
        {
	    mLogger.severe ( mStringTranslator.getString("SBC_NORMALIZE_FAULT_MESSAGE_FAILURE") );

	    mLogger.severe( mStringTranslator.getString("SBC_ERROR_DETAILS") );
            JBIException jbiException =
                    new JBIException(
                       mStringTranslator.getString(
                                     "SBC_NORMALIZE_FAULT_MESSAGE_FAILURE") );
            jbiException.initCause(soapException);
            throw jbiException;
        }
    }


    /**
     * Converts a SOAP Response Message to a JBI NormalizedMessage.
     *
     * @param soapWrapper request message.
     * @param normalizedMessage jbi normalized message.
     * @param operation operation details.
     *
     * @throws JBIException if the message cannot be normalized.
     */
    public void normalizeResponseMessage(
        SOAPWrapper soapWrapper, NormalizedMessage normalizedMessage,
        Operation operation)
        throws JBIException
    {
	mLogger.fine( mStringTranslator.getString("SBC_NORMALIZE_SOAP_MESSAGE") );
        try
        {
            SOAPMessage soapMessage = soapWrapper.getMessage();

            if (soapMessage != null)
            {
                SOAPPart soapPart = soapMessage.getSOAPPart();
                SOAPEnvelope soapEnvelope = soapPart.getEnvelope();
                SOAPBody soapBody = soapEnvelope.getBody();

                // Check whether the soap body has all namespace prefixes resolved.
                // If not resolve them
                // Populate the SOAP body into the message content.
                org.w3c.dom.Node bodyContents = getBodyContentsAsNode(soapBody);
                DOMSource ds = new DOMSource(bodyContents);
                //dump(ds);
                normalizedMessage.setContent(ds);
                       /* extractPayload(soapBody, operation,
                        isFault(soapWrapper.getStatus()))));*/

                // Attach attachments to the normalizedMessage
                normalizeAttachments(soapMessage, normalizedMessage);

                // Populate the SOAP Header into the message context
                SOAPHeader soapHeader = soapEnvelope.getHeader();

                if (soapHeader != null)
                {
                    //normalizedMessage.setProperty(
                    //    SOAPConstants.HEADER_PROPERTY_NAME, soapHeader);
                }
            }

            Iterator messageProperties = soapWrapper.getProperties();

            for (; messageProperties.hasNext();)
            {
                String propertyName = (String) messageProperties.next();
                normalizedMessage.setProperty(
                    propertyName, soapWrapper.getValue(propertyName));
            }
        }
        catch (RuntimeException runtimeException)
        {
            // This should not happen.
	    mLogger.severe ( mStringTranslator.getString("SBC_NORMALIZE_SOAP_MESSAGE_FAILURE_RT_EXP") );

            JBIException jbiException = new JBIException(
                                            mStringTranslator.getString(
                                            "SBC_NORMALIZE_SOAP_MESSAGE_FAILURE") );
            jbiException.initCause(runtimeException);
            throw jbiException;
        }
        catch (SOAPException soapException)
        {
	    mLogger.severe( mStringTranslator.getString("SBC_NORMALIZE_SOAP_MESSAGE_FAILURE") );
	    mLogger.severe( mStringTranslator.getString("SBC_ERROR_DETAILS", soapException.toString()) );

            JBIException jbiException = new JBIException(
                                    mStringTranslator.getString(
                                     "SBC_NORMALIZE_SOAP_MESSAGE_FAILURE") );
            jbiException.initCause(soapException);
            throw jbiException;
        }
        catch(Exception ex)
        {
            mLogger.severe("Some Exception while dumping Source.");
            ex.printStackTrace();
        }

	mLogger.fine( mStringTranslator.getString("SBC_SUCCESS_NORMALISE_SUCCESS") );
    }

    private org.w3c.dom.Node getBodyContentsAsNode(SOAPBody body)
    {
        org.w3c.dom.Node dNode = null;
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document mDoc = builder.newDocument();

            Iterator iter2 = body.getChildElements();
            //This code will not work if there are multiple child elements under
            // soap:Body
            while (iter2.hasNext()) {
                javax.xml.soap.Node n = (javax.xml.soap.Node)iter2.next();
                if(n instanceof SOAPElement)
                {
                    dNode = createDOMNodeFromSOAPNode(n, mDoc);
                    //dump(new DOMSource(dNode));
                    break;
                }
            }
        }
        catch(ParserConfigurationException pce)
        {
            pce.printStackTrace();
            return null;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            return null;
        }
        
        return dNode;
        
    }

   /**
    * Creates a DOM Node from a SOAP Node, using the given DOM Document object
    * to own the DOM Node.
    */
    private org.w3c.dom.Node createDOMNodeFromSOAPNode(javax.xml.soap.Node soapNode, Document document)
    throws ParserConfigurationException
    {
    org.w3c.dom.Node result = null;

    // First figure out what type the soapNode is. Unlike DOM nodes, there
    // is no "nodeType" property, so we have to use reflection.
    if (soapNode instanceof SOAPElement)
    {
        SOAPElement soapElement = (SOAPElement) soapNode;
        Name name = soapElement.getElementName();

        // Create the DOM Element.
        if( (name.getURI().length() != 0) && (name.getQualifiedName().length() != 0) )
            result = document.createElementNS(name.getURI(), name.getQualifiedName());
        else if(name.getLocalName() != null)
            result = document.createElement(name.getLocalName());
        else
        {
            //What to do??
        }

        // Iterate through the attributes of the SOAP node and add each one
        // to the DOM Node.
        for (Iterator iter = soapElement.getAllAttributes();iter.hasNext(); )
        {
            Name attrName = (Name) iter.next();
            String attrValue = soapElement.getAttributeValue(attrName);

            // The createAttributeNS method fails if you give it a null URI.
            Attr attribute = null;
            if (attrName.getURI() == null)
                attribute = document.createAttribute(attrName.getQualifiedName());
            else
                attribute = document.createAttributeNS(attrName.getURI(),attrName.getQualifiedName());

            attribute.setValue(attrValue);

            ((Element) result).setAttributeNodeNS(attribute);
        }

        // Iterate through the child elements of the SOAP node, recursing
        // on this method to add the child SOAP node to the newly created
        // DOM node.
        for (Iterator iter = soapElement.getChildElements(); iter.hasNext(); )
        {
            javax.xml.soap.Node childSOAPNode = (javax.xml.soap.Node) iter.next();
            appendSOAPNodeToDOMNode(document, result, childSOAPNode);
        }
    }
    else if (soapNode instanceof javax.xml.soap.Text)
    {
        javax.xml.soap.Text textNode = (javax.xml.soap.Text) soapNode;
        String textValue = textNode.getValue();

        // A text node can either be a comment or a real text node.
        if (textNode.isComment())
            result = document.createComment(textValue);
        else
            result = document.createTextNode(textValue);
    }
    else
    {
    // Not sure what to do here.
    }

    return (result);
    }

    /**
    * Appends a SOAP Node to a DOM Node, by creating DOM Node objects to
    * represent the same information in the SOAP Node. The Document object is
    * needed as a factory to create DOM Node objects.
    */
    private void appendSOAPNodeToDOMNode(Document document, org.w3c.dom.Node domNode,javax.xml.soap.Node soapNode)
    throws ParserConfigurationException
    {
    org.w3c.dom.Node newDOMNode = createDOMNodeFromSOAPNode(soapNode, document);

    // Now that the new element is completely constructed (including its
    // children), add it to the parent element.

    domNode.appendChild(newDOMNode);
    }
    
    
    private static void dump(Source source) throws Exception
    {
         TransformerFactory  tf      = TransformerFactory.newInstance();
         Transformer         t       = tf.newTransformer();
         StreamResult        stdOut  = new StreamResult(System.out);

         System.out.println("[BEGIN_MESSAGE_DUMP]");
         t.transform(source, stdOut);
         System.out.println("[END_MESSAGE_DUMP]");
    }
    
    
    /**
     * Extracts request/response payload from the soap body.
     *
     * @param soapBody soap body message.
     * @param operation operation requested.
     * @param isFault boolean indicating if it is a fault.
     *
     * @return request payload
     *
     * @throws JBIException - if request could not be extracted.
     */
    protected Node extractPayload(SOAPBody soapBody, Operation operation, boolean isFault)
        throws JBIException
    {
	mLogger.fine( mStringTranslator.getString("SBC_EXTRACT_REQUEST_PAYLOAD") );

        return getChildElement(soapBody);
    }

    /**
     * Used to check if the response code corresponds to a fault.
     *
     * @param responseCode response code
     *
     * @return true if it is a fault; false otherwise.
     */
    public boolean isFault(int responseCode)
    {
        return false;
    }

    /**
     * Extracts the first Element node from the parent node.
     *
     * @param parentNode parent node
     *
     * @return first child element node.
     */
    private Node getChildElement(Node parentNode)
    {
        NodeList childNodes = parentNode.getChildNodes();
        Node currentNode = null;
        Node elementNode = null;

        for (int i = 0; i < childNodes.getLength(); i++)
        {
            currentNode = childNodes.item(i);

            if (currentNode.getNodeType() == Node.ELEMENT_NODE)
            {
                elementNode = currentNode;

                break;
            }
        }

        return elementNode;
    }

    /**
     * Extracts the fault code from the String.
     *
     * @param completeFaultCode fault code containing the namespace prefix and the code.
     *
     * @return the fault code without the namespace prefix
     */
    private String extractFaultCode(String completeFaultCode)
    {
        String faultCode;
        StringTokenizer tokenizer = new StringTokenizer(completeFaultCode,
                                                        ":");
        if ( tokenizer.countTokens() == 1)
        {
            faultCode = completeFaultCode;
        }
        else
        {
            // Discard the first token which is hte namespace prefix.
            tokenizer.nextToken();
            faultCode = tokenizer.nextToken();
        }
        return faultCode;
    }

    /**
     * Normalizes the attachments sent as part of the SoapMessage.
     *
     * @param soapMessage soap Message
     * @param normalizedMessage normalized Message
     *
     * @throws SOAPException if soap message cannot be read
     * @throws MessagingException if attachments cannot be added to normalized message.
     */
    private void normalizeAttachments(SOAPMessage soapMessage,
                                      NormalizedMessage normalizedMessage)
        throws SOAPException, MessagingException
    {
        if ( soapMessage != null)
        {
            if ( soapMessage.countAttachments() > 0  )
            {
                Iterator attachmentIter = soapMessage.getAttachments();
                for (; attachmentIter.hasNext();)
                {
                    AttachmentPart attachment = (AttachmentPart) attachmentIter.next();
                    DataHandler dataHandler = attachment.getDataHandler();
                    String contentId = attachment.getContentId();
                    normalizedMessage.addAttachment( contentId, dataHandler);
                }
            }
        }
    }
}
