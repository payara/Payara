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

package org.glassfish.admingui.connector;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;

import java.util.List;


/**
 *  <p>	This class is configured via XML (i.e. a console-config.xml file).
 *  	This is done via the HK2 <code>ConfigParser</code>.</p>
 *
 *  <p>	Each module that wishes to provide an integration with the GlassFish
 *	admin console should provide a <code>console-config.xml</code> file
 *	which provides all the {@link IntegrationPoint} information for the
 *	module.  Here is an example of what that file might look like:</p>
 *
 *  <p><code><pre>
 *	<?xml version="1.0" encoding="UTF-8"?>
 *
 *	<console-config id="uniqueId">
 *	    <integration-point id="someId" type="tree" priority="840"
 *		    parentId="rootNode" content="/myTreeNode.jsf" />
 *	    <integration-point id="anotherId" type="webApplicationTab"
 *		    priority="22" parentId="appTab" content="/myTab.jsf" />
 *	    <integration-point id="fooId" type="tree" priority="400"
 *		    parentId="appNode" content="/fooNode.jsf" />
 *	</console-config>
 *
 *  </pre></code></p>
 *
 *  <p>	Normally a <code>console-config.xml</code> file should exist at
 *	"<code>META-INF/admingui/console-config.xml</code>" inside your module
 *	jar file.</p>
 *
 *  @author Ken Paulsen	(ken.paulsen@sun.com)
 */
@Configured
public class ConsoleConfig {
    /**
     *	<p> Accessor for the known Admin Console
     *	    {@link IntegrationPoint}s.<?p>
     */
    public List<IntegrationPoint> getIntegrationPoints() {
	return this.integrationPoints;
    }

    /**
     *	<p> {@link IntegrationPoint}s setter.</p>
     */
    @Element("integration-point")
    void setIntegrationPoints(List<IntegrationPoint> integrationPoints) {
	this.integrationPoints = integrationPoints;
    }

    /**
     *	<p> A unique identifier for the ConsoleConfig instance.</p>
     */
    public String getId() {
	return this.id;
    }

    /**
     *	<p> Setter for the id.</p>
     */
    @Attribute(required=true)
    void setId(String id) {
	this.id = id;
    }

    private String id;
    private List<IntegrationPoint> integrationPoints;
}
