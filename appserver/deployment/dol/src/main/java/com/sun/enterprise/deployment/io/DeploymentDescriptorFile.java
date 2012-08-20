/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.io;

import com.sun.enterprise.deployment.node.J2EEDocumentBuilder;
import com.sun.enterprise.deployment.node.RootXMLNode;
import com.sun.enterprise.deployment.node.SaxParserHandler;
import com.sun.enterprise.deployment.node.SaxParserHandlerFactory;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.ArchiveType;
import org.glassfish.deployment.common.Descriptor;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;

/**
 * This abstract class defines common behaviour for classes responsible
 * for loading/saving XML deployment descriptors
 *
 * @author Jerome Dochez
 */

public abstract class DeploymentDescriptorFile<T extends Descriptor> {
    
    public final static String FULL_VALIDATION = "full";
    public final static String PARSING_VALIDATION = "parsing";
    
    // should we validate the XML ?
    private boolean xmlValidation = true;
    
    // error reporting level
    private String validationLevel=PARSING_VALIDATION;
    
    // error reporting string, used for xml validation error
    private String errorReportingString=null;
    
    // for i18N
    private static LocalStringManagerImpl localStrings=
	    new LocalStringManagerImpl(DeploymentDescriptorFile.class);        

    private ArchiveType archiveType;
    
    /** Creates a new instance of DeploymentDescriptorFile */
    public DeploymentDescriptorFile() {
    }
    
    /**
     * @return a non validating SAX Parser to read an XML file (containing 
     * Deployment Descriptors) into DOL descriptors
     */
    public SAXParser getSAXParser() {
        return getSAXParser(false);
    }
    
    /**
     * @return a SAX Parser to read an XML file (containing 
     * Deployment Descriptors) into DOL descriptors
     * 
     * @param validating true if the parser should excercise DTD validation
     */
    public SAXParser getSAXParser (boolean validating) { 
        // always use system SAXParser to parse DDs, see IT 8229
        ClassLoader currentLoader =
            Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(
            getClass().getClassLoader());
        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();

            // set the namespace awareness
            spf.setNamespaceAware(true);
            
	    // turn validation on for deployment descriptor XML files
            spf.setValidating(validating);    

	    // this feature is needed for backward compat with old DDs 
	    // constructed by J2EE1.2 which used Java encoding names
	    // such as ISO8859_1 etc.
            
            // this is a hack for a few days so people can continue runnning
            // with crimson
            if (spf.getClass().getName().indexOf("xerces")!=-1) {
                spf.setFeature(
                    "http://apache.org/xml/features/allow-java-encodings", true);
            } else {
                DOLUtils.getDefaultLogger().log(Level.WARNING, "modify your java command line to include the -Djava.endorsed.dirs option");
            }
	    
	    try {
                if (!validating) {
                    // if we are not validating, let's not load the DTD
                    if (getDeploymentDescriptorPath().indexOf(DescriptorConstants.WLS) != -1) {
                        // and let's only turn it off for weblogic*.xml for now
                        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                    }
                }

		// Validation part 2a: set the schema language if necessary            
		spf.setFeature("http://apache.org/xml/features/validation/schema",validating);		
	    
            	SAXParser sp = spf.newSAXParser();
                
                // put the default schema for this deployment file type
                String path = getDefaultSchemaSource();
                if (path!=null) {
                    sp.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation",path);
                }

		// Set Xerces feature to allow dynamic validation. This prevents
		// SAX errors from being reported when no schemaLocation attr
		// is seen for a DTD based (J2EE1.3) XML descriptor.
		sp.getXMLReader().setFeature(
		    "http://apache.org/xml/features/validation/dynamic", validating);
		    
		return sp;
		
            } catch (SAXNotRecognizedException x) {
                // This can happen if the parser does not support JAXP 1.2
                DOLUtils.getDefaultLogger().log(Level.SEVERE,
                    "INFO: JAXP SAXParser property not recognized: "
                    + SaxParserHandler.JAXP_SCHEMA_LANGUAGE);
                 DOLUtils.getDefaultLogger().log(Level.SEVERE,
                    "Check to see if parser conforms to JAXP 1.2 spec.");

            }            
        } catch (Exception e) {
            DOLUtils.getDefaultLogger().log(Level.SEVERE, "enterprise.deployment.backend.saxParserError",
                                new Object[]{e.getMessage()});
            DOLUtils.getDefaultLogger().log(Level.WARNING, "Error occurred", e);

        } finally {
                Thread.currentThread().setContextClassLoader(currentLoader);
        }
        return null;
    }
    /**
     * @return a DOM parser to read XML File into a DOM tree
     *
     * @param validating true if validation should happen
     */
    public DocumentBuilder getDocumentBuilder(boolean validating) {
        try {
            // always use system default to parse DD
            System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            System.clearProperty("javax.xml.parsers.DocumentBuilderFactory"); 

            // set the namespace awareness
            dbf.setNamespaceAware(true);
            
	    // turn validation on for deployment descriptor XML files
            dbf.setValidating(validating);            

            // Validation part 2a: set the schema language if necessary
            try     {
                // put the default schema for this deployment file type
                String path = getDefaultSchemaSource();
                if (path!=null) {
                    dbf.setAttribute("http://apache.org/xml/properties/schema/external-schemaLocation",path);
                }
                return dbf.newDocumentBuilder();
            } catch (ParserConfigurationException x) {
                // This can happen if the parser does not support JAXP 1.2
                DOLUtils.getDefaultLogger().log(Level.SEVERE,
                    "Error: JAXP DOMParser property not recognized: "
                    + SaxParserHandler.JAXP_SCHEMA_LANGUAGE);
                DOLUtils.getDefaultLogger().log(Level.SEVERE,
                    "Check to see if parser conforms to JAXP 1.2 spec.");

            }            
        } catch (Exception e) {
            DOLUtils.getDefaultLogger().log(Level.SEVERE, "enterprise.deployment.backend.saxParserError",
                                new Object[]{e.getMessage()});
            DOLUtils.getDefaultLogger().log(Level.WARNING, "Error occurred", e);
        }
        return null;
    }
    
    /**
     * read and parse a J2EE Deployment Descriptor input file and 
     * return the constructed DOL descriptors for the J2EE Module
     * 
     * @param is the input stream for the XML file
     * @return the DOL descriptor for the J2EE Module
     */
    public T read(InputStream is)
        throws IOException, SAXParseException {    
        return read(null, is);
    }
       
    /**
     * read and parse a J2EE Deployment Descriptor input file and 
     * return the constructed DOL descriptors for the J2EE Module
     * 
     * @param descriptor the read is incremental, the descriptor to apply the DDs to
     * @param in the input stream for the XML file
     * @return the DOL descriptor for the J2EE Module
     */
    public T read(T descriptor, File in)
        throws IOException, SAXParseException {
        
        FileInputStream fis = new FileInputStream(in);
        try {
            return read(descriptor, fis);
        } finally {
            fis.close();
        }
    }
    
    /**
     * read and parse a J2EE Deployment Descriptor input file and 
     * return the constructed DOL descriptors for the J2EE Module
     * 
     * @param descriptor the read is incremental, the descriptor to apply the DDs to
     * @param in the input archive abstraction for the XML file
     * @return the DOL descriptor for the J2EE Module
     */    
    public T read(T descriptor, ReadableArchive in)
        throws IOException, SAXParseException {
            
            InputStream is = in.getEntry(getDeploymentDescriptorPath());
            try {
                return read(descriptor, is);
            } finally {
                is.close();
            }
    }
    
    /**
     * read and parse a J2EE Deployment Descriptor input file and 
     * return the constructed DOL descriptors for the J2EE Module
     * 
     * @param descriptor if the read is incremental, the descriptor to apply the DDs to
     * @param is the input stream for the XML file
     * @return the DOL descriptor for the J2EE Module
     */
    @SuppressWarnings("unchecked")
    public T read(T descriptor, InputStream is) 
        throws IOException, SAXParseException {        
        
        errorReportingString = FileUtils.revertFriendlyFilenameExtension(errorReportingString);
        String error = (errorReportingString == null)? errorReportingString:new File(errorReportingString).getName();
        String errorReporting = localStrings.getLocalString(
			"enterprise.deployment.io.errorcontext",
			"archive {0} and deployment descriptor file {1}",
                        error, getDeploymentDescriptorPath());
        
        SAXParser sp = getSAXParser(getXMLValidation());
        SaxParserHandler dh = SaxParserHandlerFactory.newInstance();
	if (validationLevel.equals(FULL_VALIDATION)) {
	    dh.setStopOnError(true);
	} 
        if (descriptor!=null) {
            dh.setTopNode(getRootXMLNode(descriptor));
        }

        dh.setErrorReportingString(errorReporting);
        
        InputSource input =new InputSource(is);
        try {
            sp.parse(input,dh);
        } catch(SAXParseException e) {
            DOLUtils.getDefaultLogger().log(Level.SEVERE, "enterprise.deployment.backend.saxParserError",
                                new Object[]{e.getMessage()});
            
            errorReporting += "  " + e.getLocalizedMessage();
            SAXParseException spe = new SAXParseException(errorReporting,
                                                        e.getSystemId(),
                                                        e.getPublicId(),
                                                        e.getLineNumber(),
                                                        e.getColumnNumber(),
                                                        e);

            throw spe;            
        } catch(SAXException e) {
            DOLUtils.getDefaultLogger().log(Level.SEVERE, "enterprise.deployment.backend.saxParserError",
                                new Object[]{e.getMessage()});            
            DOLUtils.getDefaultLogger().log(Level.SEVERE, "Error occurred", e);
            return null;
        } catch (IOException e) {
            DOLUtils.getDefaultLogger().log(Level.SEVERE, "enterprise.deployment.backend.saxParserError",
                                e.getMessage() == null ? "" : new Object[]{e.getMessage()});            

            // Let's check if the root cause of this IOException is failing to 
            // connect. If yes, it means two things: 
            // 1. The public id declared is not one of the pre-defined ones. 
            //    So we need to ask user the check for typo.
            // 2. If the user does intend to use the system id to go outside.
            //    We need to ask them to check whether they have proper 
            //    access to the internet (proxy setting etc).      
            for (StackTraceElement stElement : e.getStackTrace()) {
                if (stElement.getClassName().equals("java.net.Socket") &&
                        stElement.getMethodName().equals("connect")) {
                    String msg = localStrings.getLocalString(
                            "enterprise.deployment.can_not_locate_dtd",
                            "Unable to locate the DTD to validate your deployment descriptor file [{1}] in archive [{0}]. Please make sure the DOCTYPE is correct (no typo in public ID or system ID) and you have proper access to the Internet.",
                            error, getDeploymentDescriptorPath());
                    IOException ioe = new IOException(msg);
                    ioe.initCause(e);
                    throw ioe;
                }
            }

            IOException ioe = new IOException(localStrings.getLocalString(
                    "enterprise.deployment.backend.error_parsing_descr",
                    "Error parsing descriptor: {0}", errorReporting));
            ioe.initCause(e);
            throw ioe;
        }
        if (dh.getTopNode()!=null) {
            return ((RootXMLNode<T>) dh.getTopNode()).getDescriptor();
        }
        return null;
    }                
    
    /**
     * @return a Document for the passed descriptor
     * @param descriptor
     */    
    public Document getDocument(T descriptor) {
        return J2EEDocumentBuilder.getDocument(descriptor, getRootXMLNode(descriptor));
    }
    
    /**
     * writes the descriptor to an output stream
     * 
     * @param descriptor the descriptor
     * @param os the output stream
     */
    public void write(T descriptor, OutputStream os) throws IOException {
        try {
            J2EEDocumentBuilder.write(descriptor, getRootXMLNode(descriptor), os);
        } catch(IOException ioe) {
            throw ioe;
        } catch(Exception e) {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        }
    }
    
    /**
     * writes the descriptor classes into a new XML file
     * 
     * @param descriptor the DOL descriptor to write
     * @param path the file to use
     */
    public void write(T descriptor, String path) throws IOException {
        
        String dir;
        String fileName = getDeploymentDescriptorPath();
        if (fileName.lastIndexOf('/')!=-1) {
            dir = path + File.separator + fileName.substring(0, fileName.lastIndexOf('/'));
            fileName = fileName.substring(fileName.lastIndexOf('/')+1);
        } else {
            dir = path;
        }
        File dirs = new File(dir.replace('/', File.separatorChar));
        if (!dirs.exists()) {
            boolean ok = dirs.mkdirs();
            if (! ok)
              throw new IOException(dirs.getAbsolutePath() + " not created");
        }
        File out = new File(dirs, fileName);        
        write(descriptor, out);
    }    
    
    /**
     * writes the descriptor classes into a new XML file
     * 
     * @param descriptor the DOL descriptor to write
     * @param out the file to use
     */
    public void write(T descriptor, File out) throws IOException {
        FileOutputStream fos = new FileOutputStream(out);
        try {
            write(descriptor, fos );
        } catch(Exception e) {
            IOException ioe = new IOException(e.getMessage());
            ioe.initCause(e);
            throw ioe;
        } finally {
            fos.close();
        }
    }
    
    /** 
     * @return the location of the deployment descriptor file for a 
     * particular type of Java EE Archive
     */
    public abstract String getDeploymentDescriptorPath();
        
    /**
     * @return a RootXMLNode responsible for handling the deployment
     * descriptors associated with this Java EE module
     *
     * @param descriptor the descriptor for which we need the node
     */
    public abstract RootXMLNode<T> getRootXMLNode(T descriptor);
     
    /**
     * @return true if XML validation should be performed at load time
     */    
    protected boolean getXMLValidation() {
        return xmlValidation;
    }
    
    /**
     * sets wether XML validation should be performed at load time
     * @param validate true to validate
     */    
    public void setXMLValidation(boolean validate) {
        xmlValidation = validate;
    }
    
    /**
    * Sets the xml validation error reporting/recovering level.
    * The reporting level is active only when xml validation is
    * turned on @see setXMLValidation.
    * so far, two values can be passed, medium which reports the 
    * xml validation and continue and full which reports the 
    * xml validation and stop the xml parsing.
    */
    public void setXMLValidationLevel(String level) {
	validationLevel = level;
    }
    
    /**
    * @return the xml validation reporting level
    */
    public String getXMLValidationLevel() {
	return validationLevel;
    }    
    
    /**
    * @return the default schema source for this deployment descriptors
    */
    protected String getDefaultSchemaSource() {
	RootXMLNode<?> node = getRootXMLNode(null);
	if (node!=null) {
	    List<String> systemIDs = node.getSystemIDs();
            if (systemIDs != null) {
                StringBuilder path = new StringBuilder();
                for (String systemID : systemIDs) {
                    if (path.length()>0)
                        path.append(' ');
                    path.append(systemID);
                }
                return path.toString();
            }
	}
	return null;
    }
    
    /**
     * Sets the error reporting string
     */
    public void setErrorReportingString(String s) {
        this.errorReportingString = s;
    }
    
    /**
     * @return the archive type associated with this deployment descriptor file
     */
    public ArchiveType getArchiveType() {
        return archiveType;
    }

    /**
     * @param the archive type to set on this deployment descriptor file
     */
    public void setArchiveType(ArchiveType type) {
        archiveType = type;
    }
}
