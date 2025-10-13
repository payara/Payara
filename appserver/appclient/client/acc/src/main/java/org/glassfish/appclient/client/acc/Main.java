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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018] [Payara Foundation and/or its affiliates]
package org.glassfish.appclient.client.acc;

/**
 * This is the main program invoked from the command line.
 *
 * It processes the command line arguments to prepare the Map of options, then
 * passes the Map to an instance of the embedded ACC.
 */
public class Main {
    
    private static final String CLIENT = "-client";
    private static final String NAME = "-name";
    private static final String MAIN_CLASS = "-mainclass";
    private static final String TEXT_AUTH = "-textauth";
    private static final String XML_PATH = "-xml";
    private static final String ACC_CONFIG_XML = "-configxml";    
    private static final String DEFAULT_CLIENT_CONTAINER_XML = "sun-acc.xml";
    // duplicated in com.sun.enterprise.jauth.ConfigXMLParser
    private static final String SUNACC_XML_URL = "sun-acc.xml.url";
    private static final String NO_APP_INVOKE = "-noappinvoke";
    //Added for allow user to pass user name and password through command line.
    private static final String USER = "-user";
    private static final String PASSWORD = "-password";
    private static final String PASSWORD_FILE = "-passwordfile";
    private static final String LOGIN_NAME = "j2eelogin.name";
    private static final String LOGIN_PASSWORD = "j2eelogin.password";
    private static final String DASH = "-";

    private static final String lineSep = System.getProperty("line.separator");

}
