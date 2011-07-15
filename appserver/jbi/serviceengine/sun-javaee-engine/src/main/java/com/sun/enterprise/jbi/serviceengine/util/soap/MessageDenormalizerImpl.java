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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jbi.messaging.Fault;
import javax.jbi.messaging.NormalizedMessage;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.AttachmentPart;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import javax.activation.DataHandler;

import java.util.logging.Logger;

/**
 * This Basic Profile 1.0 aware implementation is used to denormalize a JBI Normalized
 * Message and convert it into a SOAP message format.
 *
 * @author Sun Microsystems, Inc.
 */
public class MessageDenormalizerImpl implements MessageDenormalizer
{
    /**
     * Namespace prefix for the payload.
     */
    private static final String PAYLOAD_NAMESPACE_PREFIX = "jbisb0";

    /**
     * SOAP Namespace prefix.
     */
    private static final String SOAP_NAMESPACE_PREFIX = "soap";

    /**
     * XML Schema Instance prefix.
     */
    private static final String XML_SCHEMA_INSTANCE_NAMESPACE_PREFIX = "xsi";

    /**
     * Internal handle to the message factory
     */
    private MessageFactory mMessageFactory;

    /**
     * Internal handle to the logger instance
     */
    private Logger mLogger;

    /**
     * Internal handle to String Translator instance.
     */
    private StringTranslator mStringTranslator;

    /**
     * Internal handle to the transformer instance
     */
    private Transformer mTransformer;

    /**
     * Creates a new instance of MessageDenormalizerImpl.
     */
    public MessageDenormalizerImpl()
    {
        try
        {
            mLogger = Logger.getLogger(this.getClass().getPackage().getName());
            mStringTranslator = new StringTranslator(this.getClass().getPackage().getName(), this.getClass().getClassLoader());
            mMessageFactory = MessageFactory.newInstance();

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            mTransformer = transformerFactory.newTransformer();
            mTransformer.setOutputProperty("method", "xml");
            mTransformer.setOutputProperty("omit-xml-declaration", "yes");
        }
        catch (Exception exception)
        {
            // This should not happen. In case it does, log the exception and
            // set the factory object to null
	    mLogger.severe( mStringTranslator.getString("SBC_MESSAGE_FACTORY_CREATION_FAILURE") );
	    mLogger.severe( mStringTranslator.getString("SBC_ERROR_DETAILS", exception.toString()) );
            mMessageFactory = null;
            mTransformer = null;
        }
    }

    /**
     * Converts a JBI normalized message to a <code> javax.jbi.soap.SOAPMessage </code>
     * instance. The SOAP Header information is extracted from the NormalizedMessage
     * property "SoapHeader" and the SOAP Body content is extracted from the Normalized
     * Message content. Any attachments present in the NormalizedMessage are also
     * denormalized and added to the created <code> javax.jbi.soap.SOAPMessage  </code>
     * instance.
     *
     * @param normalizedMessage message to be denormalized.
     * @param operation operation invoked
     * @param isResponse indicates if a response messages needs to be generated
     *
     * @return the SOAP Message.
     */
    public SOAPWrapper denormalizeMessage(
        NormalizedMessage normalizedMessage, Operation operation, boolean isResponse)
    {
        SOAPWrapper wrapper = null;
        Writer writer = null;
	mLogger.fine( mStringTranslator.getString("SBC_DENORMALIZE_JBI_MESSAGE") );

        try
        {
            // Create a SOAP Message
            ByteArrayOutputStream bufferedStream = new ByteArrayOutputStream();
            writer = new OutputStreamWriter(bufferedStream, "UTF-8");

            writeEnvelopeHeader(writer);

            if ( normalizedMessage != null)
            {
                // Writer the header to the writer instance.
                writeHeader(normalizedMessage, writer);
            }

            // Extract the body information from the Normalized Message
            writeBody(normalizedMessage, operation, isResponse, writer);
            writeEnvelopeFooter(writer);
            writer.flush();

            // Create a soap message
            SOAPMessage soapMessage = createSOAPMessage(bufferedStream);

            // Denormalize Attachments
            denormalizeAttachments ( soapMessage, normalizedMessage);
            // Create a soap response wrapper
            wrapper = new SOAPWrapper(soapMessage);

            if (normalizedMessage instanceof Fault)
            {
                wrapper.setStatus(SOAPConstants.JBI_FAULT);
            }
            else
            {
                wrapper.setStatus(SOAPConstants.JBI_SUCCESS);
            }
        }
        catch (RuntimeException runtimeException)
        {
	    mLogger.severe( mStringTranslator.getString("SBC_DENORMALIZE_JBI_MESSAGE_FAILURE_RT_EXP") );
            // Create a soap fault wrapper
            wrapper = denormalizeMessage(runtimeException);
        }
        catch (Exception exception)
        {
	    mLogger.warning( mStringTranslator.getString("SBC_DENORMALIZE_JBI_MESSAGE_FAILURE_EXP") );
	    mLogger.warning( mStringTranslator.getString("SBC_ERROR_DETAILS", exception.toString()) );
	    mLogger.warning( mStringTranslator.getString("SBC_CREATE_SOAP_FAULT") );

            // Create a soap fault wrapper
            wrapper = denormalizeMessage(exception);
        }
        finally
        {
            closeWriter(writer);
        }

	mLogger.fine( mStringTranslator.getString("SBC_SUCCESS_DENORMALIZE_JBI_MESSAGE") );

        return wrapper;
    }

    /**
     * Converts an exception to a SOAP Message. It uses the default Server fault code 
     * for denormalization.
     *
     * @param exception exception instance
     *
     * @return denormalized exception object
     */
    public SOAPWrapper denormalizeMessage(Exception exception)
    {
        return denormalizeMessage(exception, SOAPConstants.SERVER_FAULT_CODE);
    }

    /**
     * Converts an exception to a SOAP Message using the provided faultCode. The code
     * expects the faultcode passed to be part of the soap namespace.
     *
     * @param exception exception instance
     * @param faultCode fault code
     *
     * @return denormalized exception object
     */
    public SOAPWrapper denormalizeMessage(Exception exception, String faultCode)
    {
        SOAPWrapper wrapper = null;
        Writer writer = null;

	mLogger.fine( mStringTranslator.getString("SBC_DENORMALIZE_EXCEPTION") );

        try
        {
            // Create the ws-i compliant fault message from the exception
            ByteArrayOutputStream bufferedStream = new ByteArrayOutputStream();
            writer = new OutputStreamWriter(bufferedStream, "UTF-8");

            if (exception == null)
            {
		mLogger.warning( mStringTranslator.getString("SBC_NULL_OBJECT_DENORMALIZATION") );
            }

            writeEnvelopeHeader(writer);
            writer.write("<" + SOAP_NAMESPACE_PREFIX + ":Body>");
            writeFault(exception, faultCode, writer);
            writer.write("</" + SOAP_NAMESPACE_PREFIX + ":Body>");
            writeEnvelopeFooter(writer);
            writer.flush();

            // Create a soap message
            SOAPMessage soapMessage = createSOAPMessage(bufferedStream);

            // Create a SOAP wrapper with service url as null
            wrapper = new SOAPWrapper(soapMessage);
            wrapper.setStatus(SOAPConstants.JBI_ERROR);
        }
        catch (RuntimeException runtimeException)
        {
	    mLogger.severe( mStringTranslator.getString( "SBC_SOAP_FAULT_GENERATION_FAILURE_RT_EXP") );
        }
        catch (Exception denormalizationException)
        {
            // This should not happen. In case it does do nothing. Log message
	    mLogger.severe( mStringTranslator.getString("SBC_SOAP_FAULT_GENERATION_FAILURE") );
        }
        finally
        {
            closeWriter(writer);
        }

	mLogger.fine( mStringTranslator.getString("SBC_SUCCESS_DENORMALIZE_EXCEPTION") );

        return wrapper;
    }

    /**
     * This method extracts the payload from the Normalized Message and writes it
     * using the writer stream. The payload content is enclosed between the SOAP:Body 
     * header and SOAP:Body footer information.
     *
     * @param normalizedMessage normalized message
     * @param operation operation invoked
     * @param isResponse indicates if a response messages needs to be generated
     * @param writer writer object to be used
     *
     * @throws Exception if the body cannot be written
     */
    protected void writeBody(
        NormalizedMessage normalizedMessage, Operation operation, boolean isResponse,
        Writer writer) throws Exception
    {
        StringWriter stringWriter = null;

        try
        {
            boolean isEmptyResponse = isResponse && ( normalizedMessage == null );
            // Add the body information
            writeBodyHeader(operation, writer, isEmptyResponse);
            if ( normalizedMessage != null)
            {
                stringWriter = new StringWriter();
                Result result = new StreamResult(stringWriter);
                mTransformer.transform(normalizedMessage.getContent(), result);
                writer.write(stringWriter.toString());
            }
            writeBodyFooter(operation, writer, isEmptyResponse);
            writer.flush();
        }
        finally
        {
            closeWriter(stringWriter);
        }
    }

    /**
     * The method extracts the header information from the Normalized Message property 
     * "SoapHeader" and writes it using the writer instance. The header information
     * is expected to be propagated as a <code> javax.xml.soap.SOAPHeader </code> 
     * implementation instance.
     *
     * @param normalizedMessage normalizedMessage
     * @param writer writer object to be used
     *
     * @throws Exception if header cannot be used to write to the writer instance
     */
    protected void writeHeader(NormalizedMessage normalizedMessage, Writer writer)
        throws Exception
    {
        // Extract header information from the Normalized Message
        SOAPHeader soapHeader =
            (SOAPHeader) normalizedMessage.getProperty(
                SOAPConstants.HEADER_PROPERTY_NAME );
        StringWriter stringWriter = null;

        if ( soapHeader != null)
        {
            try
            {
                stringWriter = new StringWriter();

                Source source = new DOMSource( soapHeader );
                Result result = new StreamResult(stringWriter);
                mTransformer.transform(source, result);

                // Add the header information
                writer.write("<" + SOAP_NAMESPACE_PREFIX + ":Header>");
                writer.write(stringWriter.toString());
                writer.write("</" + SOAP_NAMESPACE_PREFIX + ":Header>");
                writer.flush();
            }
            finally
            {
                closeWriter(stringWriter);
            }
        }
        else
        {
	    mLogger.fine( mStringTranslator.getString("SBC_NO_HEADER") );
        }
    }

    /**
     * Uses the writer object to write the SOAP:Body header information. This method
     * is invoked before the body payload is written.
     *
     * @param operation operation invoked
     * @param writer writer object to be used
     * @param isEmptyResponse indicates if an empty response message needs to be generated
     *
     * @throws Exception if body header cannot be written.
     */
    protected void writeBodyHeader(
        Operation operation, Writer writer, boolean isEmptyResponse)
        throws Exception
    {
        writer.write("<" + SOAP_NAMESPACE_PREFIX + ":Body>");

        if ( isEmptyResponse)
        {
            writer.write("<" + PAYLOAD_NAMESPACE_PREFIX + ":");
            writer.write(operation.getName() + "Response");
            writer.write(" xmlns:" + PAYLOAD_NAMESPACE_PREFIX + "=\"");
            writer.write(operation.getOutputNamespace() + "\"");
            writer.write(">");
        }

        writer.flush();
    }

    /**
     * Uses writer object to write the SOAP:Body footer information. This method is
     * invoked after the body payload has been written. 
     *
     * @param operation operation invoked
     * @param writer writer object.
     * @param isEmptyResponse indicates if a response messages needs to be generated
     *
     * @throws Exception if body footer cannot be written
     */
    protected void writeBodyFooter(
        Operation operation, Writer writer, boolean isEmptyResponse)
        throws Exception
    {

        if (  isEmptyResponse )
        {
            writer.write("</" + PAYLOAD_NAMESPACE_PREFIX + ":");
            writer.write(operation.getName() + "Response>");
        }
        writer.write("</" + SOAP_NAMESPACE_PREFIX + ":Body>");
        writer.flush();
    }

    /**
     * Uses the provided input data to create a <code> javax.xml.soap.SOAPMessage </code>
     * instance.
     *
     * @param byteStream Stream which contains the soap messages information as bytes.
     *
     * @return SOAP Message object
     *
     * @throws SOAPException if soap message object cannot be created.
     * @throws IOException if soap message object cannot be created.
     */
    protected SOAPMessage createSOAPMessage(ByteArrayOutputStream byteStream)
        throws SOAPException, IOException
    {
        if (mLogger.isLoggable(Level.FINEST))
        {
	    mLogger.finest( mStringTranslator.getString("SBC_DEONRMALIZED_MESSAGE_DETAILS", byteStream.toString()) );
        }

        // Create a soap message
        SOAPMessage soapMessage = mMessageFactory.createMessage();

        // Populate the fault message in the soap Message
        byte[] data = byteStream.toByteArray();
        ByteArrayInputStream soapInputStream = new ByteArrayInputStream(data);
        StreamSource streamSource = new StreamSource(soapInputStream);
        soapMessage.getSOAPPart().setContent(streamSource);
        soapInputStream.close();

        return soapMessage;
    }

    /**
     * Closes the writer instance. This method handles any exceptions thrown
     * while handling this request.
     *
     * @param writer writer instance.
     */
    protected void closeWriter(Writer writer)
    {
        if (writer != null)
        {
            try
            {
                writer.close();
            }
            catch (Exception ioException)
            {
                // This should not happen. In case it does do nothing
		mLogger.warning( mStringTranslator.getString("SBC_CLOSE_OUTPUT_STREAM") );
		mLogger.warning( mStringTranslator.getString("SBC_ERROR_DETAILS", ioException.toString()) );
            }
        }
    }

    /**
     * Uses writer object to write the SOAP:Envelope header information. This method
     * is invoked before writing the envelope content ( header and body content).
     *
     * @param writer writer object.
     *
     * @throws IOException if envelope header information cannot be written.
     */
    protected void writeEnvelopeHeader(Writer writer) throws IOException
    {
        // Write the soap envelope
        writer.write(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<" + SOAP_NAMESPACE_PREFIX +
            ":Envelope xmlns:" + SOAP_NAMESPACE_PREFIX +
            "=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:" +
            XML_SCHEMA_INSTANCE_NAMESPACE_PREFIX +
            "=\"http://www.w3.org/2001/XMLSchema-instance\">");
        writer.flush();
    }

    /**
     * Uses writer object to write the SOAP:Envelope footer information. This method
     * is invoked after writing the envelope content ( header and body content).
     *
     * @param writer writer object
     *
     * @throws IOException if envelope footer information cannot be written.
     */
    protected void writeEnvelopeFooter(Writer writer) throws IOException
    {
        writer.write("</" + SOAP_NAMESPACE_PREFIX + ":Envelope>");
        writer.flush();
    }

    /**
     * Create the SOAP:Fault message based on the provided exception details and writes
     * it using the writer instance.
     *
     * @param exception  exception thrown
     * @param faultCode fault code
     * @param writer writer object
     *
     * @throws IOException if fault message cannot be generated.
     */
    protected void writeFault(Exception exception, String faultCode, Writer writer)
        throws IOException
    {
        writer.write(
            "<" + SOAP_NAMESPACE_PREFIX + ":Fault><faultcode>" + SOAP_NAMESPACE_PREFIX +
            ":" + faultCode + "</faultcode>");

        if (exception != null)
        {
            writer.write("<faultstring>" +
                         sanitizeMessage(exception.getMessage()) + "</faultstring>");
        }

        writer.write("</" + SOAP_NAMESPACE_PREFIX + ":Fault>");
        writer.flush();
    }

    /**
     * Converts a JBI Fault mesage to a standard <code> javax.xml.soap.SOAPMessage </code>
     * message instance. It uses the default Server fault code for denormalization.
     *
     * @param faultMessage JBI fault message.
     *
     * @return a new SOAPWrapper instance which contains the SOAP fault Message.
     */
    public SOAPWrapper denormalizeFaultMessage(Fault faultMessage)
    {
        return denormalizeFaultMessage(faultMessage, SOAPConstants.SERVER_FAULT_CODE);
    }

    /**
     * Converts a JBI Fault mesage to a SOAP Message using the specified fault code.
     *
     * @param faultMessage JBI fault message.
     * @param faultCode fault code to be used in the fault message
     *
     * @return a new SOAPWrapper instance which contains the SOAP fault Message.
     */
    public SOAPWrapper denormalizeFaultMessage(Fault faultMessage, String faultCode)
    {
        SOAPWrapper wrapper = null;
        Writer writer = null;

	mLogger.fine( mStringTranslator.getString("SBC_DENORMALIZE_FAULT_MESSAGE") );

        try
        {
            // Create the ws-i compliant fault message from the exception
            ByteArrayOutputStream bufferedStream = new ByteArrayOutputStream();
            String messageFaultCode = (String) faultMessage.getProperty(
                                        SOAPConstants.FAULT_CODE_PROPERTY_NAME);
            String faultString = (String) faultMessage.getProperty(
                                        SOAPConstants.FAULT_STRING_PROPERTY_NAME);

            if ( messageFaultCode != null )
            {
                // Override the fault code with the message fault code.
                faultCode = messageFaultCode;
            }

            if ( faultString == null )
            {
                faultString = mStringTranslator.getString("SBC_DEFAULT_FAULT_STRING");
            }
            writer = new OutputStreamWriter(bufferedStream, "UTF-8");
            writeEnvelopeHeader(writer);
            writer.write("<" + SOAP_NAMESPACE_PREFIX + ":Body>");
            writer.write(
                "<" + SOAP_NAMESPACE_PREFIX + ":Fault " +
                XML_SCHEMA_INSTANCE_NAMESPACE_PREFIX + ":type=\"" +
                SOAP_NAMESPACE_PREFIX + ":Fault\"" + "><faultcode>" +
                SOAP_NAMESPACE_PREFIX + ":" + faultCode + "</faultcode>");
            writer.write("<faultstring>" + faultString + "</faultstring>");
            writeFaultDetail(faultMessage, writer);
            writer.write("</" + SOAP_NAMESPACE_PREFIX + ":Fault>");
            writer.write("</" + SOAP_NAMESPACE_PREFIX + ":Body>");
            writeEnvelopeFooter(writer);
            writer.flush();

            // Create a soap message
            SOAPMessage soapMessage = createSOAPMessage(bufferedStream);

            // Create a SOAP wrapper with service url as null
            wrapper = new SOAPWrapper(soapMessage);
            wrapper.setStatus(SOAPConstants.JBI_FAULT);
        }
        catch (RuntimeException runtimeException)
        {
	    mLogger.severe( mStringTranslator.getString( "SBC_SOAP_FAULT_GENERATION_FAILURE_RT_EXP") );
        }
        catch (Exception exception)
        {
            // This should not happen. In case it does do nothing. Log message
	    mLogger.severe( mStringTranslator.getString("SBC_SOAP_FAULT_GENERATION_FAILURE") );
        }
        finally
        {
            closeWriter(writer);
        }

	mLogger.fine( mStringTranslator.getString("SBC_SUCCESS_DENORMALIZE_FAULT") );

        return wrapper;
    }

    /**
     * Writes the detailed fault message using the provided writer instance.
     *
     * @param faultMessage JBI Fault object which contains the fault details.
     * @param writer writer object to be used.
     *
     * @throws Exception if the fault detail vould not be written.
     */
    private void writeFaultDetail(Fault faultMessage, Writer writer)
        throws Exception
    {
        StringWriter stringWriter = null;

        try
        {
            stringWriter = new StringWriter();

            Result result = new StreamResult(stringWriter);
            mTransformer.transform(faultMessage.getContent(), result);

            // Add the fault detail
            String detailString = stringWriter.toString().trim();

            if (!detailString.equals(""))
            {
                writer.write("<detail>");
                writer.write(detailString);
                writer.write("</detail>");
                writer.flush();
            }
        }
        finally
        {
            closeWriter(stringWriter);
        }
    }

    /**
     * Sanitizes the messages so that it can be properly read by an XML parser.
     *
     * @param errorMessage error message to be sanitized.
     *
     * @return sanitized error message.
     */
    protected String sanitizeMessage(String errorMessage)
    {
        StringBuffer sanitizedBuffer = new StringBuffer();

        for (int i = 0; (errorMessage != null) && (i < errorMessage.length()); i++)
        {
            char currentChar = errorMessage.charAt(i);

            switch (currentChar)
            {
            case '"':
                sanitizedBuffer.append("&quot;");

                break;

            case '&':
                sanitizedBuffer.append("&amp;");

                break;

            case '<':
                sanitizedBuffer.append("&lt;");

                break;

            case '>':
                sanitizedBuffer.append("&gt;");

                break;

            default:
                sanitizedBuffer.append(currentChar);
            }
        }

        if (errorMessage == null)
        {
            return "INTERNAL SERVER ERROR";
        }
        else
        {
            return sanitizedBuffer.toString();
        }
    }

    /**
     * Denormalizes the attachments present in the JBI Normalized Message and adds
     * them to the <code> javax.xml.soap.SoapMessage </code> instance.
     *
     * @param soapMessage soap message.
     * @param normalizedMessage  normalized message instance.
     */
    private void denormalizeAttachments ( SOAPMessage soapMessage,
                                          NormalizedMessage normalizedMessage)
    {
        if ( normalizedMessage != null )
        {
            Iterator attachmentIter = normalizedMessage.getAttachmentNames().iterator();
            for (; attachmentIter.hasNext();)
            {
                String attachmentIdentifier = (String) attachmentIter.next();
                DataHandler dataHandler =
                            normalizedMessage.getAttachment( attachmentIdentifier);
                AttachmentPart attachment =
                    soapMessage.createAttachmentPart( dataHandler);
                attachment.setContentId ( attachmentIdentifier);
                attachment.setContentType ( dataHandler.getContentType());
                soapMessage.addAttachmentPart ( attachment );
            }
        }
    }
}
