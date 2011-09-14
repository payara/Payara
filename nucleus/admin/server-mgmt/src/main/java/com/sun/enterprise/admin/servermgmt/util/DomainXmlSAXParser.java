/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.servermgmt.util;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.EntityResolver;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class DomainXmlSAXParser extends DefaultHandler {
  private static final String PROPERTY = "property";
  private int level = 0;
  private String domainXmlEventListenerClass= null;
  
  public String getDomainXmlEventListenerClass() {
	return domainXmlEventListenerClass;
  }

  public void parse(java.io.File domainXml) throws javax.xml.parsers.ParserConfigurationException,org.xml.sax.SAXException,java.io.IOException {
            SAXParser saxParser; 
	    SAXParserFactory factory = SAXParserFactory.newInstance();
	    saxParser = factory.newSAXParser();
            saxParser.getXMLReader().setEntityResolver((EntityResolver)this);
	    saxParser.parse(domainXml,this);
  }


    @Override
  public void startElement(String namespaceURI, String localName, String qName, Attributes attrs)
  throws SAXException {
      level++;
      if ( level==2 && PROPERTY.equals(qName)){
          if (attrs != null) {
              for (int i = 0; i < attrs.getLength(); i++) {
                  String aName = attrs.getQName(i); // Attr name
                  String aValue = attrs.getValue(aName);
                  if ("DomainXmlEventListenerClass".equals(aValue)) {
                      domainXmlEventListenerClass=attrs.getValue("value");
                  }
              }
          }
      }
  }


    @Override
  public void endElement(String namespaceURI, String localName, String qName)
      throws SAXException {
      level--;
  }

    @Override
  public InputSource resolveEntity(String publicId, String systemId) throws SAXException {
      return null;
  }
}


