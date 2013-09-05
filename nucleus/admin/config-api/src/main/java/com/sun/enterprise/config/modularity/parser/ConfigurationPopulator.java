/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.modularity.parser;

import com.sun.enterprise.config.util.ConfigApiLoggerInfo;
import com.sun.enterprise.util.LocalStringManager;
import com.sun.enterprise.util.LocalStringManagerImpl;

import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigParser;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DomDocument;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * populate the a DomDocument from the given configuration snippet file containing a config bean configuration.
 *
 * @author Bhakti Mehta
 * @author Masoud Kalali
 */
public class ConfigurationPopulator {

    private final static Logger LOG = ConfigApiLoggerInfo.getLogger();
    private final DomDocument doc;
    private final ConfigBeanProxy parent;
    private final String xmlContent;

    public ConfigurationPopulator(String xmlContent, DomDocument doc, ConfigBeanProxy parent) {
        this.xmlContent = xmlContent;
        this.doc = doc;
        this.parent = parent;
    }

    public void run(ConfigParser parser) {
        try {
            InputStream is = new ByteArrayInputStream(xmlContent.getBytes());
            XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(is, "utf-8");
            parser.parse(reader, doc, Dom.unwrap((ConfigBeanProxy) parent));
        } catch (XMLStreamException e) {
            LOG.log(Level.SEVERE, ConfigApiLoggerInfo.DEFAULT_CFG_READ_FAILED, e);
        }
    }
}
