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

package com.sun.enterprise.tools.verifier.web;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import javax.servlet.descriptor.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.sun.enterprise.deployment.WebBundleDescriptor;
import org.glassfish.deployment.common.ModuleDescriptor;
import com.sun.enterprise.tools.verifier.util.LogDomains;
import com.sun.enterprise.tools.verifier.util.XMLValidationHandler;
import com.sun.enterprise.tools.verifier.VerifierTestContext;
import com.sun.enterprise.tools.verifier.VerifierFrameworkContext;
import com.sun.enterprise.tools.verifier.StringManagerHelper;
import com.sun.enterprise.tools.verifier.TagLibDescriptor;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.deploy.shared.FileArchive;
import org.glassfish.web.deployment.descriptor.WebBundleDescriptorImpl;

/** This is the factory class used for obtainig the TagLibDescriptor objects for
 * the tag libraries defined in the war archive.
 *
 * @author Sudipto Ghosh    
 */

public class TagLibFactory {

    private DocumentBuilder builder;
    private VerifierTestContext context;
    private VerifierFrameworkContext verifierFrameworkContext;
    private Logger logger = LogDomains.getLogger(
            LogDomains.AVK_VERIFIER_LOGGER);
    private boolean uninitialised = false;
    private final LocalStringManagerImpl smh = StringManagerHelper.getLocalStringsManager();


    /**
     * Constructor to create a factory object.
     *
     * @param context
     * @param verifierFrameworkContext
     */
    public TagLibFactory(VerifierTestContext context, VerifierFrameworkContext verifierFrameworkContext) {
        this.context = context;
        this.verifierFrameworkContext = verifierFrameworkContext;
    }

    /**
     * This method is responsible for creating a document object from the tld
     * files and then returning the array TagLibDescriptor objects based on tld
     * version.
     *
     * @param descriptor the web bundle descriptor giving the tlds contained
     * in the war.
     * @return the tag lib descriptor array for all the tlds defined in the war.
     */
    public TagLibDescriptor[] getTagLibDescriptors(
            WebBundleDescriptor descriptor) {
        ArrayList<TagLibDescriptor> tmp = new ArrayList<TagLibDescriptor>();
        Iterable<TaglibDescriptor> taglibConfig = null;

        if (((WebBundleDescriptorImpl)descriptor).getJspConfigDescriptor() != null) {
            taglibConfig = ((WebBundleDescriptorImpl)descriptor).getJspConfigDescriptor().getTaglibs();
        } else {
            return null;
        }
        init();
        for (TaglibDescriptor taglibDescriptor : taglibConfig) {
            // test all the Tag lib descriptors.
            String taglibLocation = taglibDescriptor.getTaglibLocation();
            Document d = null;
            try {
                d = createDocument(taglibLocation, descriptor);
            } catch (Exception e) {
                logger.log(Level.WARNING, smh.getLocalString
                        (getClass().getName() + ".exception", // NOI18N
                                "Continuing, though problem in creating taglib document. Cause: {0}", // NOI18N
                                new Object[]{e.getMessage()}));
                if (e instanceof SAXParseException) {
                    LogRecord logRecord = new LogRecord(Level.SEVERE,
                                            smh.getLocalString
                                                (getClass().getName() + ".exception2", // NOI18N
                                                "XML Error line : {0} in [ {1} ]. {2}", // NOI18N
                                                new Object[] {((SAXParseException) e).getLineNumber(),
                                                        taglibLocation,
                                                        e.getLocalizedMessage()}));
                    logRecord.setThrown(e);
                    verifierFrameworkContext.getResultManager().log(logRecord);
                }
                continue; // we are continuing with creating the next document.
            }
            String version = getTLDSpecVersion(d);
            TagLibDescriptor taglib = null;
            taglib = new TagLibDescriptor(d, version, taglibLocation);
            tmp.add(taglib);
        }

        int count = tmp.size();
        TagLibDescriptor arr[] = new TagLibDescriptor[count];
        int i = 0;
        for (Iterator e = tmp.iterator(); e.hasNext(); i++) {
            arr[i] = (TagLibDescriptor) e.next();
        }
        return arr;
    }

    /**
     * this method is called once to create the Documentbuilder required for
     * creating documen objects from tlds
     */

    private void init() {
        if (!uninitialised) {
            synchronized (this) {
                if (!uninitialised) {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    String W3C_XML_SCHEMA = "http://www.w3c.org/2001/XMLSchema"; // NOI18N
                    String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage"; // NOI18N
                    factory.setNamespaceAware(true);
                    factory.setValidating(true);
                    factory.setAttribute(
                            "http://apache.org/xml/features/validation/schema", // NOI18N
                            Boolean.TRUE);
                    factory.setAttribute(
                            "http://xml.org/sax/features/validation", // NOI18N
                            Boolean.TRUE);
                    factory.setAttribute(
                            "http://apache.org/xml/features/allow-java-encodings", // NOI18N
                            Boolean.TRUE);
                    factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
                    try {
                        builder = factory.newDocumentBuilder();
                    } catch (ParserConfigurationException e) {
                        logger.log(Level.SEVERE, e.getMessage());
                    }
                    EntityResolver dh = new XMLValidationHandler(true);
                    ErrorHandler eh = new XMLValidationHandler(true);
                    builder.setErrorHandler(eh);
                    builder.setEntityResolver(dh);
                    uninitialised = true;
                }
            }
        }
    }

    /**
     * Helper method to create the document object from the tld files specified
     * by the location in web.xml of the war archive.
     *
     * @param location location of jsp tlds
     * @param webd has all the information about the web.xml of the war archive
     * @return document object created from tlds specified at the given location
     * @throws IOException
     * @throws SAXException
     */
    private Document createDocument(String location, WebBundleDescriptor webd)
            throws IOException, SAXException {
        Document document = null;
        InputSource source = null;
        if (location.startsWith("/")) // NOI18N
            location = location.substring(1);
        else
            location = "WEB-INF/" + location; // NOI18N
        ModuleDescriptor moduleDesc = webd.getModuleDescriptor();
        String archBase = context.getAbstractArchive().getURI().getPath();
        String uri = null;
        if(moduleDesc.isStandalone()){
            uri = archBase;
        } else {
            uri = archBase + File.separator + 
                FileUtils.makeFriendlyFilename(moduleDesc.getArchiveUri());
        }
        FileArchive arch = new FileArchive();
        arch.open(uri);
        InputStream is = arch.getEntry(location);
        try {
        // is can be null if wrong location of tld is specified in web.xml
            if (is == null)
                throw new IOException(smh.getLocalString
                        (getClass().getName() + ".exception1", // NOI18N
                                "Wrong tld [ {0} ] specified in the web.xml of [ {1} ]", // NOI18N
                                new Object[]{location, moduleDesc.getArchiveUri()}));
            source = new InputSource(is);
            document = builder.parse(source);
        } finally {
            try{
                if(is != null)
                    is.close();
            } catch(Exception e) {}
        }

        return document;
    }

    /**
     * Returns the spec version of the tag lib descriptor file.
     *
     * @param doc document object created from tlds
     * @return spec version of the tag lib descriptor file defined by this doc.
     */
    private String getTLDSpecVersion(Document doc) {
        String str = null;
        if (doc.getDoctype() != null) {
            NodeList nl = doc.getElementsByTagName("jsp-version"); // NOI18N
            if (nl != null && nl.getLength() != 0) {
                str = nl.item(0).getFirstChild().getNodeValue(); // web-jsptaglib dtd vesion 1.2
            } else {
                nl = doc.getElementsByTagName("jspversion"); // NOI18N
                if (nl.getLength() != 0) {
                    str = nl.item(0).getFirstChild().getNodeValue(); // web-jsptaglib dtd vesion 1.1
                } else
                    return "1.1"; // NOI18N
            }
        } else
            str = doc.getElementsByTagName("taglib").item(0).getAttributes() // NOI18N
                    .getNamedItem("version") // NOI18N
                    .getNodeValue();

        return str;
    }
}
