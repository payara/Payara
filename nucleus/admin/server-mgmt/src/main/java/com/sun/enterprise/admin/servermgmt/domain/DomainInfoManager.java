/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.servermgmt.domain;

import java.io.File;
import java.io.FileOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.glassfish.api.logging.LogHelper;

import com.sun.enterprise.admin.servermgmt.SLogger;
import com.sun.enterprise.admin.servermgmt.xml.domaininfo.DomainInfo;
import com.sun.enterprise.admin.servermgmt.xml.domaininfo.ObjectFactory;
import com.sun.enterprise.admin.servermgmt.xml.domaininfo.TemplateRef;
import com.sun.enterprise.admin.servermgmt.xml.templateinfo.TemplateInfo;
import com.sun.enterprise.util.SystemPropertyConstants;

public class DomainInfoManager {

    private static final Logger _logger = SLogger.getLogger();

    private static final String JAVA_HOME = "JAVA_HOME";

    /**
     * Parses template information file and uses its information to create 
     * domain info file.
     */
    public void process(DomainTemplate domainTemplate, File domainDir) {
        FileOutputStream outputStream = null;
        try {
            TemplateInfo templateInfo = domainTemplate.getInfo();
            File infoDir = new File(domainDir, DomainConstants.INFO_DIRECTORY);
            if(!infoDir.exists() && !infoDir.mkdirs()) {
                _logger.log(Level.INFO, SLogger.DIR_CREATION_ERROR, infoDir.getAbsolutePath());
                return;
            }
            File domainInfoXML = new File(infoDir, DomainConstants.DOMAIN_INFO_XML);
            outputStream = new FileOutputStream(domainInfoXML);
            ObjectFactory objFactory = new ObjectFactory();
            DomainInfo domainInfo = objFactory.createDomainInfo();
            String javaHome = System.getenv(JAVA_HOME);
            if (javaHome == null || javaHome.isEmpty()) {
                javaHome = System.getProperty("java.home");
            }
            domainInfo.setJavahome(javaHome);
            domainInfo.setMwhome(System.getProperty(SystemPropertyConstants.PRODUCT_ROOT_PROPERTY));
            TemplateRef templateRef = new TemplateRef();
            templateRef.setName(templateInfo.getName());
            templateRef.setVersion(templateInfo.getVersion());
            templateRef.setLocation(domainTemplate.getLocation());
            domainInfo.setDomainTemplateRef(templateRef);

            JAXBContext context = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(javax.xml.bind.Marshaller.JAXB_ENCODING, "UTF-8");
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(objFactory.createDomainInfo(domainInfo), outputStream);
        } catch (Exception e) {
        	LogHelper.log(_logger, Level.WARNING, 
        		SLogger.DOMAIN_INFO_CREATION_ERROR, e, 
        		DomainConstants.DOMAIN_INFO_XML);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception io)
                { /** ignore*/ }
            }
        }
    }
}