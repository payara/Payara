/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.kernel.embedded;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.bootstrap.PopulatorPostProcessor;
import org.glassfish.hk2.utilities.DescriptorImpl;
import org.kohsuke.MetaInfServices;

import com.sun.enterprise.server.logging.LogManagerService;
import com.sun.enterprise.v3.admin.PrivateAdminAdapter;
import com.sun.enterprise.v3.admin.PublicAdminAdapter;
import com.sun.enterprise.v3.admin.adapter.AdminConsoleAdapter;
import com.sun.enterprise.v3.server.DomainXmlPersistence;
import com.sun.enterprise.v3.server.GFDomainXml;

import javax.inject.Inject;

/**
 * Kernel's decoration for embedded environment.
 *
 * @author Jerome Dochez
 */
@MetaInfServices
public class EmbeddedInhabitantsParser implements PopulatorPostProcessor {
  
	@Inject
	private ServiceLocator serviceLocator;

	public EmbeddedInhabitantsParser() {
	}
	
    public String getName() {
        return "Embedded";
    }

//    private void decorate(InhabitantsParser parser) {
//
//        // we don't want to reconfigure the loggers.
//
//        parser.drop(AdminConsoleAdapter.class);
//
//        String enableCLI = System.getenv("GF_EMBEDDED_ENABLE_CLI");
//        if (enableCLI == null || !enableCLI.equalsIgnoreCase("true")) {
//            parser.drop(PublicAdminAdapter.class);
//            parser.drop(LogManagerService.class);
//            parser.drop(PrivateAdminAdapter.class);
//        }
//        parser.replace(GFDomainXml.class, EmbeddedDomainXml.class);
//        
//        parser.replace(DomainXmlPersistence.class, EmbeddedDomainPersistence.class);
//
//    }

	@Override
	public DescriptorImpl process(ServiceLocator serviceLocator, DescriptorImpl descriptorImpl) {

		// we don't want to reconfigure the loggers.

		boolean skip = false;

		if (AdminConsoleAdapter.class.getCanonicalName().equals(
				descriptorImpl.getImplementation())) {
			skip = true;
		}

		String enableCLI = System.getenv("GF_EMBEDDED_ENABLE_CLI");
		if (enableCLI == null || !enableCLI.equalsIgnoreCase("true")) {

			if (PublicAdminAdapter.class.getCanonicalName().equals(
					descriptorImpl.getImplementation())
					|| LogManagerService.class.getCanonicalName().equals(
							descriptorImpl.getImplementation())
					|| PrivateAdminAdapter.class.getCanonicalName().equals(
							descriptorImpl.getImplementation())) {
				skip = true;
			}
		}

		if (GFDomainXml.class.getCanonicalName().equals(
				descriptorImpl.getImplementation())) {
			descriptorImpl.setImplementation(EmbeddedDomainXml.class
					.getCanonicalName());
		}

		if (DomainXmlPersistence.class.getCanonicalName().equals(
				descriptorImpl.getImplementation())) {
			descriptorImpl.setImplementation(EmbeddedDomainPersistence.class
					.getCanonicalName());
		}

		if (!skip) {
			return descriptorImpl;
		}
		return null;
	}
}

