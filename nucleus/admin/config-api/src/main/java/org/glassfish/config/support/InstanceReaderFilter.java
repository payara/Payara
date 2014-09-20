/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.config.support;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.ServerRef;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.EarlyLogger;
import com.sun.enterprise.util.StringUtils;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import javax.xml.stream.XMLInputFactory;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import org.glassfish.config.support.DomainXmlPreParser.DomainXmlPreParserException;
import static org.glassfish.config.support.Constants.*;

/**
 * The "pre-parser" goes through the domain.xml and finds all the 'special'
 * elements that should be retained.  This filter class just checks the elements
 * against those lists...
 * @author Byron Nevins
 */
class InstanceReaderFilter extends ServerReaderFilter {

    InstanceReaderFilter(String theServerName, URL theDomainXml,
            XMLInputFactory theXif) throws XMLStreamException, DomainXmlPreParserException {

        super(theDomainXml, theXif);
        instanceName = theServerName;
        dxpp = new DomainXmlPreParser(domainXml, xif, instanceName);
    }

    /**
     * This method is called for every element.  We are very interested
     * in server, config and cluster.
     * We will only filter out config and server and cluster elements never other elements
     * We use this as a handy hook to get info about other elements -- which really
     * is a side-effect.
     *
     * @return true to NOT parse this sub-tree
     * @throws XMLStreamException
     */
    @Override
    final boolean filterOut() throws XMLStreamException {
        try {
            XMLStreamReader reader = getParent();
            String elementName = reader.getLocalName();

            if (!StringUtils.ok(elementName))
                return true; // famous last words:  "this can never happen" ;-)

            // possibly filter out from these 3 kinds of elements
            if (elementName.equals(SERVER))
                return handleServer(reader);

            if (elementName.equals(CONFIG))
                return handleConfig(reader);

            if (elementName.equals(CLUSTER))
                return handleCluster(reader);

            // keep everything else
            return false;
        }
        catch (Exception e) {
            // I don't trust the XML parser code in the JDK -- it likes to throw
            // unchecked exceptions!!
            throw new XMLStreamException(
                    Strings.get("InstanceReaderFilter.UnknownException",
                    e.toString()), e);
        }
    }

    @Override
    final String configWasFound() {
        // preparser already threw an exception if the config wasn't found
        return null;
    }

    /**
     * @return true if we want to filter out this server element
     */
    private boolean handleServer(XMLStreamReader r) {
        String name = r.getAttributeValue(null, NAME);

        if (StringUtils.ok(name) && dxpp.getServerNames().contains(name))
            return false;

        return true;
    }

    /**
     * @return true if we want to filter out this config element
     */
    private boolean handleConfig(XMLStreamReader reader) {
        String name = reader.getAttributeValue(null, NAME);

        if (dxpp.getConfigName().equals(name))
            return false;

        return true;
    }

    /**
     * Note that dxpp.getClusterName() will definitely return null
     * for stand-alone instances.  This is normal.
     *
     * @return true if we want to filter out this cluster element
     */
    private boolean handleCluster(XMLStreamReader reader) {
        String name = reader.getAttributeValue(null, NAME);
        String myCluster = dxpp.getClusterName();

        if (StringUtils.ok(myCluster) && myCluster.equals(name))
            return false;

        return true;
    }

    private final DomainXmlPreParser dxpp;
    private final String instanceName;
}
