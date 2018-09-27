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
 */

package org.jvnet.hk2.config;

import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.Dom.Child;

import javax.validation.constraints.NotNull;
import javax.xml.stream.XMLInputFactory;
import static javax.xml.stream.XMLStreamConstants.*;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

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
    /**
     * This is where we put parsed inhabitants into.
     */
    protected final ServiceLocator habitat;


    public ConfigParser(ServiceLocator habitat) {
        this.habitat = habitat;
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
    protected Dom handleElement(XMLStreamReader in,DomDocument document, Dom parent) throws XMLStreamException {
        ConfigModel model = document.getModelByElementName(in.getLocalName());
        if(model==null) {
            String localName = in.getLocalName();
            Logger.getAnonymousLogger().severe("Ignoring unrecognized element "+in.getLocalName() + " at " + in.getLocation());
            // flush the sub element content from the parser
            int depth=1;
            while(depth>0) {
                final int tag = in.nextTag();
                if (tag==START_ELEMENT && in.getLocalName().equals(localName)) {
                    if (Logger.getAnonymousLogger().isLoggable(Level.FINE)) {
                        Logger.getAnonymousLogger().fine("Found child of same type "+localName+" ignoring too");
                    }
                    depth++;
                }
                if (tag==END_ELEMENT && in.getLocalName().equals(localName)) {
                    if (Logger.getAnonymousLogger().isLoggable(Level.FINE)) {
                        Logger.getAnonymousLogger().fine("closing element type " + localName);
                    }
                    depth--;
                }
                if (Logger.getAnonymousLogger().isLoggable(Level.FINE) && tag==START_ELEMENT) {
                    Logger.getAnonymousLogger().fine("Jumping over " + in.getLocalName());
                }
            }
            return null;
        }
        return handleElement(in,document,parent,model);
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

    // In JDK 1.6, StAX is part of JRE, so we use no argument variant of
    // newInstance(), where as on JDK 1.5, we use two argument version of
    // newInstance() so that we can pass the classloader that loads
    // XMLInputFactory to load the factory, otherwise by default StAX uses
    // Thread's context class loader to locate the factory. See:
    // https://glassfish.dev.java.net/issues/show_bug.cgi?id=6428

    // as of Hk2 version 1.5, we do not support JDK 1.5 any more.
    private static final XMLInputFactory xif =  XMLInputFactory.newInstance();
//            XMLInputFactory.class.getClassLoader() == null ?
//                    XMLInputFactory.newInstance() :
//                    XMLInputFactory.newInstance(XMLInputFactory.class.getName(),
//                            XMLInputFactory.class.getClassLoader());
}
