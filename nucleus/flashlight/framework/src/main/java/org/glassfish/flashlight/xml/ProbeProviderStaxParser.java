/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.flashlight.xml;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;
import static org.glassfish.flashlight.xml.XmlConstants.*;
import org.glassfish.flashlight.FlashlightLoggerInfo;
import static org.glassfish.flashlight.FlashlightLoggerInfo.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;

/**
 * Read the XML file, parse it and return a list of ProbeProvider objects
 * @author bnevins
 */
public class ProbeProviderStaxParser extends StaxParser{

    private static final Logger logger =
        FlashlightLoggerInfo.getLogger();
    public final static LocalStringManagerImpl localStrings =
                            new LocalStringManagerImpl(ProbeProviderStaxParser.class);

    public ProbeProviderStaxParser(File f) throws XMLStreamException {
        super(f);
    }

    public ProbeProviderStaxParser(InputStream in) throws XMLStreamException {
        super(in);
    }

    public List<Provider> getProviders() {
        if(providers == null) {
            try {
                read();
            } 
            catch(Exception ex) {
                // normal 
                close();
            }
        }
        if (providers.isEmpty()) {
            // this line snatched from the previous implementation (DOM)
            logger.log(Level.SEVERE, NO_PROVIDER_IDENTIFIED_FROM_XML);
        }

        return providers;
    }

    @Override
    protected void read() throws XMLStreamException, EndDocumentException{
        providers = new ArrayList<Provider>();
        // move past the root -- "probe-providers".
        skipPast("probe-providers");

        while(true) {
            providers.add(parseProbeProvider());
        }
    }

    private Provider parseProbeProvider() throws XMLStreamException {
        if(!parser.getLocalName().equals(PROBE_PROVIDER)) {
            String errStr = localStrings.getLocalString("invalidStartElement",
                                "START_ELEMENT is supposed to be {0}" +
                                ", found: {1}", PROBE_PROVIDER, parser.getLocalName());
            throw new XMLStreamException(errStr);
        }
        
        Map<String,String> atts = parseAttributes();
        List<XmlProbe> probes = parseProbes();

        return new Provider(atts.get(MODULE_PROVIDER_NAME),
                            atts.get(MODULE_NAME),
                            atts.get(PROBE_PROVIDER_NAME),
                            atts.get(PROBE_PROVIDER_CLASS),
                            probes );
    }

    private List<XmlProbe> parseProbes() throws XMLStreamException {
        List<XmlProbe> probes = new ArrayList<XmlProbe>();

        boolean done = false;
        
        // Prime the pump here, and advance to the next start. Note that further advances
        // will be done as the elements within a probe are handled, not at this level.
        try {
            nextStart();
        }
        catch (EndDocumentException ex) {
            // ignore -- this must be the last START_ELEMENT in the doc
            // that's normal
            done = true;
        }

        while(!done) {
            // If we've been advanced to the end of the document, we' done...
            if (parser.getEventType() == END_DOCUMENT)
                break;
            if(parser.getLocalName().equals(PROBE))
                probes.add(parseProbe());  // Note that this will advance to the next probe if there is one
            else
                done = true;  // Done at this level, bounce out.
        }
        return probes;
    }

    private XmlProbe parseProbe() throws XMLStreamException {
        if(!parser.getLocalName().equals(PROBE)) {
            String errStr = localStrings.getLocalString("invalidStartElement",
                                "START_ELEMENT is supposed to be {0}" +
                                ", found: {1}", PROBE, parser.getLocalName());
            throw new XMLStreamException(errStr);
        }

        // for some unknown reason method is an element not an attribute
        // Solution -- use the last item if there are more than one

        List<XmlProbeParam> params = new ArrayList<XmlProbeParam>();
        Map<String,String> atts = parseAttributes();
        String method = null;
        String name = atts.get(PROBE_NAME);
        boolean self = Boolean.parseBoolean(atts.get(PROBE_SELF));
        boolean hidden = Boolean.parseBoolean(atts.get(PROBE_HIDDEN));
        boolean stateful = Boolean.parseBoolean(atts.get(PROBE_STATEFUL));
        boolean statefulReturn = Boolean.parseBoolean(atts.get(PROBE_STATEFUL_RETURN));
        boolean statefulException = Boolean.parseBoolean(atts.get(PROBE_STATEFUL_EXCEPTION));
        String profileNames = atts.get(PROBE_PROFILE_NAMES);
        boolean done = false;
        while(!done) {
            try {
                nextStart();
                String localName = parser.getLocalName();

                if(localName.equals(METHOD))
                    method = parser.getElementText();
                else if(localName.equals(PROBE_PARAM))
                    params.add(parseParam());
                else
                    done = true;
            }
            catch (EndDocumentException ex) {
                // ignore -- possibly normal -- but stop!
                done = true;
            }
        }
        return new XmlProbe(name, method, params, self, hidden, stateful, statefulReturn, statefulException, profileNames);
    }
    private XmlProbeParam parseParam() throws XMLStreamException {
        if(!parser.getLocalName().equals(PROBE_PARAM)){
            String errStr = localStrings.getLocalString("invalidStartElement",
                                "START_ELEMENT is supposed to be {0}" +
                                ", found: {1}", PROBE_PARAM, parser.getLocalName());
            throw new XMLStreamException(errStr);
        }

        Map<String,String> atts = parseAttributes();

        return new XmlProbeParam(atts.get(PROBE_PARAM_NAME), atts.get(PROBE_PARAM_TYPE));
    }

    private List<Provider> providers = null;
}

