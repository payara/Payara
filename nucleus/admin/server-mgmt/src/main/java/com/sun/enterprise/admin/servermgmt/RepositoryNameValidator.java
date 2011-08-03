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

package com.sun.enterprise.admin.servermgmt;

import javax.xml.parsers.*;
import org.xml.sax.*;
import java.io.ByteArrayInputStream;

import javax.management.ObjectName;

import com.sun.enterprise.util.i18n.StringManager;

/**
 * Validates the repository name. A repository name must be a
 * - valid file name, 
 * - valid xml CDATA value &
 * - valid javax.management.ObjectName property value.
 */
public class RepositoryNameValidator extends StringValidator
{
    private static final String VALID_CHAR = 
        "[^\\,\\/ \\&\\;\\`\\'\\\\\"\\|\\*\\!\\?\\~\\<\\>\\^\\(\\)\\[\\]\\{\\}\\$\\:\\%]*";

    private static final String IAS_NAME = "com.sun.appserv:name=";

    private static final String XML_1 = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <xml>";

    private static final String XML_2 = "</xml>";

    /**
     * i18n strings manager object
     */
    private static final StringManager strMgr = 
        StringManager.getManager(RepositoryNameValidator.class);

    /**
     * Constructs new RepositoryNameValidator object.
     * @param name
     */
    public RepositoryNameValidator(String name)
    {
        super(name);
    }

    /**
     * Validates the given value for the given entry. This method first invokes
     * its superclass's validate method and then performs additional validations.
     * @throws InvalidConfigException
     */
    public void validate(Object str) throws InvalidConfigException
    {
        super.validate(str);
        checkValidName((String)str);
        checkValidXmlToken((String)str);
        checkValidObjectNameToken((String)str);
    }

    public void checkValidName(String name) throws InvalidConfigException
    {
        if (!name.matches(VALID_CHAR))
        {
            throw new InvalidConfigException(
                strMgr.getString("validator.invalid_value", getName(), name));
        }
    }

    /**
     * Implementation copied from 
     * com.sun.enterprise.admin.verifier.tests.StaticTest
     */
    public void checkValidXmlToken(String name) throws InvalidConfigException
    {
        try
        {
            //Construct a valid xml string
            String xml = XML_1 + name + XML_2;
            ByteArrayInputStream bais = new ByteArrayInputStream(xml.getBytes());
            InputSource is = new InputSource(bais);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.parse(is);
        }
        catch (Exception e)
        {
            throw new InvalidConfigException(
                strMgr.getString("validator.invalid_value", getName(), name));
        }
    }

    public void checkValidObjectNameToken(String name) 
        throws InvalidConfigException
    {
        try
        {
            new ObjectName(IAS_NAME + name);
        }
        catch (Exception e)
        {
            throw new InvalidConfigException(
                strMgr.getString("validator.invalid_value", getName(), name));
        }
    }
}
