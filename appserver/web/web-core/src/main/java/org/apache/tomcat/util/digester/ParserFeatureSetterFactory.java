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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tomcat.util.digester;

import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import java.util.Properties;

/**
 * Creates a <code>SAXParser</code> based on the underlying parser.
 * Allows logical properties depending on logical parser versions
 * to be set.
 *
 * @since 1.6
 */
public class ParserFeatureSetterFactory{

    /**
     * <code>true</code> is Xerces is used.
     */
    private static boolean isXercesUsed; 

    static {
        try{
            // Use reflection to avoid a build dependency with Xerces.
            Class.forName("org.apache.xerces.impl.Version");
            isXercesUsed = true;
        } catch (Exception ex){
            isXercesUsed = false;
        }
    }

    /**
     * Create a new <code>SAXParser</code>
     * @param properties (logical) properties to be set on parser
     * @return a <code>SAXParser</code> configured based on the underlying
     * parser implementation.
     */
    public static SAXParser newSAXParser(Properties properties)
            throws ParserConfigurationException,
                   SAXException,
                   SAXNotRecognizedException, 
                   SAXNotSupportedException {

        if (isXercesUsed){
            return XercesParser.newSAXParser(properties);
        } else {
            return GenericParser.newSAXParser(properties);
        }
    }

}
