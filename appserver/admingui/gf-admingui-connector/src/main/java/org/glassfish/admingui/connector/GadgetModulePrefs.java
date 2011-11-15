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
 *  <p>	This class is configured via XML.  This is done via the HK2
 *	<code>ConfigParser</code>.</p>
 *
 *  @author Ken Paulsen	(ken.paulsen@sun.com)
 */
@Configured(name="ModulePrefs")
public class GadgetModulePrefs {

    /**
     *	<p> Accessor for the known Admin Console
     *	    {@link IntegrationPoint}s.<?p>
    public List<IntegrationPoint> getIntegrationPoints() {
	return this.integrationPoints;
    }
     */

    /**
     *	<p> {@link IntegrationPoint}s setter.</p>
    @Element("integration-point")
    void setIntegrationPoints(List<IntegrationPoint> integrationPoints) {
	this.integrationPoints = integrationPoints;
    }
     */

    /**
     *	<p> A unique identifier for the GadgetModule instance.</p>
     */
    public String getTitle() {
	return this.title;
    }

    /**
     *	<p> Setter for the title.</p>
     */
    @Attribute(value="title", required=false)
    void setTitle(String title) {
	this.title = title;
    }

    private String title;

    /**
     *	<p> A unique identifier for the GadgetModule instance.</p>
     */
    public String getTitleUrl() {
	return this.titleUrl;
    }

    /**
     *	<p> Setter for the titleUrl.</p>
     */
    @Attribute(value="title_url", required=false)
    void setTitleUrl(String titleUrl) {
	this.titleUrl = titleUrl;
    }

    private String titleUrl;

    /**
     *	<p> A unique identifier for the GadgetModule instance.</p>
     */
    public String getDescription() {
	return this.description;
    }

    /**
     *	<p> Setter for the description.</p>
     */
    @Attribute(value="description", required=false)
    void setDescription(String description) {
	this.description = description;
    }

    private String description;

    /**
     *	<p> A unique identifier for the GadgetModule instance.</p>
     */
    public String getAuthor() {
	return this.author;
    }

    /**
     *	<p> Setter for the author.</p>
     */
    @Attribute(value="author", required=false)
    void setAuthor(String author) {
	this.author = author;
    }

    private String author;

    /**
     *	<p> A unique identifier for the GadgetModule instance.</p>
     */
    public String getAuthorEmail() {
	return this.authorEmail;
    }

    /**
     *	<p> Setter for the authorEmail.</p>
     */
    @Attribute(value="author_email", required=false)
    void setAuthorEmail(String authorEmail) {
	this.authorEmail = authorEmail;
    }

    private String authorEmail;

    /**
     *	<p> A unique identifier for the GadgetModule instance.</p>
     */
    public String getScreenshot() {
	return this.screenshot;
    }

    /**
     *	<p> Setter for the screenshot.</p>
     */
    @Attribute(value="screenshot", required=false)
    void setScreenshot(String screenshot) {
	this.screenshot = screenshot;
    }

    private String screenshot;

    /**
     *	<p> A unique identifier for the GadgetModule instance.</p>
     */
    public String getThumbnail() {
	return this.thumbnail;
    }

    /**
     *	<p> Setter for the thumbnail.</p>
     */
    @Attribute(value="thumbnail", required=false)
    void setThumbnail(String thumbnail) {
	this.thumbnail = thumbnail;
    }

    private String thumbnail;
}
