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

package com.sun.enterprise.tools.verifier;

import java.io.ByteArrayInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import com.sun.enterprise.tools.verifier.util.LogDomains;
import com.sun.enterprise.tools.verifier.util.XMLValidationHandler;

public class NameToken {

    final static String XMLtop =
            "<!DOCTYPE NameToken [ <!ELEMENT NameToken EMPTY> <!ATTLIST NameToken value NMTOKEN #REQUIRED>]> <NameToken value=\""; // NOI18N


    final static String XMLbottom = "\"/>"; // NOI18N

    // Logger to log messages
    private static Logger logger = LogDomains.getLogger(
            LogDomains.AVK_VERIFIER_LOGGER);

    /**
     * Determine is value is legal NMToken type
     *
     * @param value xml element to be checked
     * @return <code>boolean</code> true if xml element is legal NMToken,
     *         false otherwise
     */
    public static boolean isNMTOKEN(String value) {
/*
        com.sun.enterprise.util.LocalStringManagerImpl smh =
            StringManagerHelper.getLocalStringsManager();
*/
        String XMLdoc = XMLtop + value + XMLbottom;
        logger.log(Level.FINE,
                "com.sun.enterprise.tools.verifier.NameToken.print", // NOI18N
                new Object[]{XMLdoc});

        try {
            InputSource source = new InputSource(
                    new ByteArrayInputStream(XMLdoc.getBytes()));
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setValidating(true);
            // ValidatingParser p = new ValidatingParser();
            XMLReader p = spf.newSAXParser().getXMLReader();
//            XMLErrorHandler eh = new XMLErrorHandler(null);
            p.setErrorHandler(new XMLValidationHandler());
            p.parse(source);
            return true;

        } catch (Exception e) {
            return false;
        } 
    }
}


