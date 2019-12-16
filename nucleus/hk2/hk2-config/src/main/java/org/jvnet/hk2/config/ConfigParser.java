/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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
 *
 * Portions Copyright [2019] Payara Foundation and/or affiliates
 */

package org.jvnet.hk2.config;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.Dom.Child;

/**
 * Parses configuration files, builds {@link Inhabitant}s,
 * and add them to {@link Habitat}.
 *
 * <p>
 * This class also maintains the model of various elements in the configuration file.
 *
 * <p>
 * This class can be sub-classed to create a {@link ConfigParser} with a custom non-standard behavior.
 *
 * @author Kohsuke Kawaguchi
 */
public class ConfigParser {

    private static final Logger LOGGER = Logger.getLogger(ConfigParser.class.getName());

    /**
     * This is where we put parsed inhabitants into.
     */
    protected final ServiceLocator habitat;

    private final List<String> errors;

    private boolean logErrors;

    public ConfigParser(ServiceLocator habitat) {
        this(habitat, true);
    }

    public ConfigParser(ServiceLocator habitat, boolean logErrors) {
        this.habitat = habitat;
        this.logErrors = logErrors;
        this.errors = new ArrayList<>();
    }

    public DomDocument parse(XMLStreamReader in) throws XMLStreamException {
        DomDocument document = new DomDocument(habitat);
        parse(in, document);
        return document;
    }

    public void parse(XMLStreamReader in, DomDocument document) throws XMLStreamException {
        parse(in, document, null);
    }

    public void parse(XMLStreamReader in, DomDocument document, Dom parent) throws XMLStreamException {
        this.errors.clear();
        try {
            in.nextTag();
            document.root = handleElement(in, document, parent);
        }
        finally {
            in.close();
        }
    }

    /**
     * Parses the given source as a config file, and adds resulting
     * {@link Dom}s into {@link Habitat} as {@link Inhabitant}s.
     */
    public DomDocument parse(URL source) {
        return parse(source, new DomDocument(habitat));
    }

    public DomDocument parse(URL source, DomDocument document) {
        return parse(source, document, null);
    }

    public DomDocument parse(URL source, DomDocument document, Dom parent) {
        InputStream inputStream = null;
        try {
            
            inputStream = source.openStream();
        }
        catch (IOException e) {
            throw new ConfigurationException("Failed to open "+source,e);
        }
        
        try {
            parse(xif.createXMLStreamReader(new StreamSource(inputStream)), document, parent);
            return document;
        } catch (XMLStreamException e) {
            throw new ConfigurationException("Failed to parse "+source,e);
        }
        finally {
            try {
                inputStream.close();
            }
            catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * @return all the logged problems from the last object parsed.
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Parses a whole XML tree and builds a {@link Dom} tree.
     *
     * <p>
     * This is the entry point for the root element of a configuration tree.
     *
     * @param in
     *      pre-condition:  'in' is at the start element.
     *      post-condition: 'in' is at the end element.
     * @param document
     *      The document that we are building right now.
     *      Newly created {@link Dom} will belong to this document.
     * @param parent
     *      The parent element
     * @return
     *      Null if the XML element didn't yield anything (which can happen if the element is skipped.)
     *      Otherwise fully parsed valid {@link Dom} object.
     */
    protected Dom handleElement(XMLStreamReader in, DomDocument document, Dom parent) throws XMLStreamException {
        ConfigModel model = document.getModelByElementName(in.getLocalName());
        if(model==null) {
            // Get the element name
            String localName = in.getLocalName();

            log(SEVERE, "Ignoring unrecognized element %s at %s", in.getLocalName(), in.getLocation());

            // flush the sub element content from the parser
            int depth=1;
            while(depth>0) {
                final int tag = in.nextTag();
                if (tag==START_ELEMENT && in.getLocalName().equals(localName)) {
                    log(FINE, "Found child of same type %s ignoring too", localName);
                    depth++;
                }
                if (tag==END_ELEMENT && in.getLocalName().equals(localName)) {
                    log(FINE, "Closing element type %s", localName);
                    depth--;
                }
                if (tag == START_ELEMENT) {
                    log(FINE, "Jumping over %s", in.getLocalName());
                }
            }
            return null;
        }
        return handleElement(in,document,parent,model);
    }

    /**
     * If the provided level is not provided, this method will do nothing.
     * Otherwise, it will format the message and add it to {@link #getErrors()}. If
     * {@link #logErrors} is true, this method will also log the message.
     * 
     * @param level          the level to log at
     * @param baseLogMessage the message to be passed to {@see String#format(String,
     *                       Object...)}
     * @param parameters     the parameters to be filled into the base log message
     */
    private void log(Level level, String baseLogMessage, Object... parameters) {
        if (level != null && LOGGER.isLoggable(level)) {
            String logMessage = String.format(baseLogMessage, parameters);
            if (logErrors) {
                LOGGER.log(level, logMessage);
            }
            this.errors.add(logMessage);
        }
    }

    /**
     * Parses a whole XML tree and builds a {@link Dom} tree, by using the given model
     * for the top-level element.
     *
     * <p>
     * This is the entry point for recursively parsing inside a configuration tree.
     * Since not every element is global, you don't always want to infer the model
     * just from the element name (as is the case with {@link #handleElement(XMLStreamReader, DomDocument, Dom)}.
     * 
     * @param in
     *      pre-condition:  'in' is at the start element.
     *      post-condition: 'in' is at the end element.
     * @param document
     *      The document that we are building right now.
     *      Newly created {@link Dom} will belong to this document.
     * @param parent
     *      The parent element
     * @return
     *      Null if the XML element didn't yield anything (which can happen if the element is skipped.)
     *      Otherwise fully parsed valid {@link Dom} object.
     */
    protected Dom handleElement(XMLStreamReader in, DomDocument document, Dom parent, ConfigModel model) throws XMLStreamException {
        final Dom dom = document.make(habitat, in, parent, model);

        // read values and fill DOM
        dom.fillAttributes(in);

        List<Child> children=null;

        while(in.nextTag()==START_ELEMENT) {
            String name = in.getLocalName();
            ConfigModel.Property a = model.elements.get(name);

            if(children==null)
                children = new ArrayList<Child>();

            if(a==null) {
                // global look up
                Dom child = handleElement(in, document, dom);
                if(child!=null)
                    children.add(new Dom.NodeChild(name, child));
            } else
            if(a.isLeaf()) {
                children.add(new Dom.LeafChild(name,in.getElementText()));
            } else {
                Dom child = handleElement(in, document, dom, ((ConfigModel.Node) a).model);
                children.add(new Dom.NodeChild(name, child));
            }
        }

        if (children==null) {
            children = new ArrayList<Dom.Child>();
        }
        dom.ensureConstraints(children);

        if(!children.isEmpty())
            dom.setChildren(children);
        
        dom.register();

        dom.initializationCompleted();
        
        return dom;
    }

    private static final XMLInputFactory xif =  XMLInputFactory.newInstance();
}
