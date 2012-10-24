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
 */

package com.sun.enterprise.admin.servermgmt.cli;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import com.sun.enterprise.admin.cli.*;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.single.StaticModulesRegistry;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.logging.Level;

import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.bootstrap.HK2Populator;
import org.glassfish.hk2.bootstrap.impl.ClasspathDescriptorFileFinder;
import org.glassfish.hk2.bootstrap.impl.Hk2LoaderPopulatorPostProcessor;
import org.glassfish.internal.api.*;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.*;
import org.jvnet.hk2.config.ConfigParser;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DomDocument;

/**
 * Implementation for the CLI command verify-domain-xml
 * Verifies the content of the domain.xml file
 *
 * verify-domain-xml [--domaindir install_dir/domains] [domain_name]
 * 
 * @author Nandini Ektare
 */
@Service(name = "verify-domain-xml")
@org.glassfish.hk2.api.PerLookup
public final class VerifyDomainXmlCommand extends LocalDomainCommand {

    @Param(name = "domain_name", primary = true, optional = true)
    private String domainName0;

    private static final LocalStringsImpl strings =
            new LocalStringsImpl(VerifyDomainXmlCommand.class);

    @Override
    protected void validate()
            throws CommandException, CommandValidationException  {
        setDomainName(domainName0);
        super.validate();
    }

    /**
     */
    @Override
    protected int executeCommand()
            throws CommandException, CommandValidationException {

        File domainXMLFile = getDomainXml();
        logger.log(Level.FINER, "Domain XML file = {0}", domainXMLFile);
        try {
            // get the list of JAR files from the modules directory
            ArrayList<URL> urls = new ArrayList<URL>();
            File idir = new File(System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY));
            File mdir = new File(idir, "modules");
            for (File f : mdir.listFiles()) {
                if (f.toString().endsWith(".jar")) {
                    urls.add(f.toURI().toURL());
                }
            }
                       
            final URL[] urlsA = urls.toArray(new URL[urls.size()]);   
            
            ClassLoader cl = (ClassLoader)AccessController.doPrivileged(
                    new PrivilegedAction() {
                        @Override
                        public Object run() {
                            return new URLClassLoader(urlsA, Globals.class.getClassLoader());
                        }
                    }
                );
            
            ModulesRegistry registry = new StaticModulesRegistry(cl);
            ServiceLocator serviceLocator = registry.createServiceLocator("default");

            ConfigParser parser = new ConfigParser(serviceLocator);
            URL domainURL = domainXMLFile.toURI().toURL();
            DomDocument doc = parser.parse(domainURL);
            Dom domDomain = doc.getRoot();
            Domain domain = domDomain.createProxy(Domain.class);            
            DomainXmlVerifier validator = new DomainXmlVerifier(domain);

            if (validator.invokeConfigValidator()) return 1;
        } catch (Exception e) {
            throw new CommandException(e);
        }
        return 0;
    }
}
