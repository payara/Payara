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

package org.glassfish.admingui.connector;

import org.glassfish.api.admingui.ConsoleProvider;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;

import java.io.Serializable;


/**
 *  <p>	An <code>IntegrationPoint</code> represents the data necessary to
 *	describe a particular integration point into the admin console.  Some
 *	examples of integration points include:</p>
 *
 *  <ul><li>Add a TreeNode to the navigation tree</li>
 *	<li>Add a HelpContent to the application</li>
 *	<li>Add initialization logic to execute when a user logins into the
 *	    application</li>
 *	<li>Add content to a specific page within the application</li></ul>
 *
 *  <p>	The design of this data structure is intentionally very general so
 *	that this data structure can be applied to a wide range of situations.
 *	It also intentionally does not refer to gui concepts such as "tree" or
 *	other UI elements so that these concepts be independent of this data as
 *	much as possible.  This will allow some flexibility by the consumer of
 *	this data to render it in an appropriate way (to some extent).  This
 *	will however, be limited by the content of the data this structure
 *	refers to (if any).</p>
 *
 *  <p>	The <code>IntegrationPoint</code> contains the following properties:</p>
 *
 *  <ul><li><code>type</code> - (required) A String specifying the integration
 *	    type.</li>
 *	<li><code>parentId</code> - The parent
 *	    <code>IntegrationPoint</code>'s id.</li>
 *	<li><code>priority</code> - A priority of this component, often used to
 *	    compare or sort <code>IntegrationPoint</code>s.</li>
 *	<li><code>providerId</code> - The {@link ConsoleProvider}'s id which provided
 *	    this <code>IntegrationPoint</code>.</li>
 *	<li><code>content</code> - A value pointing to additional content
 *	    to implement this <code>IntegrationPoint</code></li>
 *	<li><code>handlerId</code> - An <code>Handler</code> name which should
 *	    be invoked to help implement this <code>IntegrationPoint</code></li>
 *	<li><code>id</code> - A value used to identify this specific
 *	    integration point.</li></ul>
 *
 *  <p>	All values in this class are immutable.</p>
 *
 *  @author Ken Paulsen	(ken.paulsen@sun.com)
 */
@Configured
public class IntegrationPoint implements Serializable {
    /**
     *	<p> Default constructor.</p>
     */
    public IntegrationPoint() {
    }

// FIXME: Implement event / handler declarations

    /**
     *	<p> The identifier of this <code>IntegrationPoint</code>.</p>
     */
    public String getId() {
	return id;
    }

    /**
     *	<p> Setter for the identifier of the <code>IntegrationPoint</code>.</p>
     */
    @Attribute(required=true)
    void setId(String id) {
	this.id = id;
    }

    /**
     *	<p> This specifies the type of this <code>IntegrationPoint</code>.  The
     *	    type might specify that this integration is a "navigation"
     *	    integration, "help" integration, "applicationPage" integration, or
     *	    any other supported integration type.</p>
     */
    public String getType() {
	return type;
    }

    /**
     *	<p> Setter for the type of the <code>IntegrationPoint</code>.</p>
     */
    @Attribute(required=true)
    void setType(String type) {
	this.type = type;
    }

    /**
     *	<p> The parent identifier.  Useful when an
     *	    <code>IntegrationPoint</code> needs to refer to another integration
     *	    point (such as when specifying which TreeNode to fall under.</p>
     */
    public String getParentId() {
	return parentId;
    }

    /**
     *	<p> Setter for the parentId of the <code>IntegrationPoint</code>.</p>
     */
    @Attribute("parentId")
    void setParentId(String parentId) {
	this.parentId = parentId;
    }

    /**
     *	<p> A reference to extra information needed to use this
     *	    <code>IntegrationPoint</code>.  An example might be a .jsf page
     *	    reference which should be included.  Or a location used to find
     *	    HelpSet information.</p>
     */
    public String getContent() {
	return content;
    }

    /**
     *	<p> Setter for the content of the <code>IntegrationPoint</code>.</p>
     */
    @Attribute
    void setContent(String content) {
	this.content = content;
    }

    /**
     *	<p> This specifies the relative priority of this
     *	    <code>IntegrationPoint</code>.  This may be used for sorting
     *	    multiple <code>IntegrationPoints</code>, or for other
     *	    implementation-specific purposes.</p>
     */
    public int getPriority() {
	return priority;
    }

    /**
     *	<p> Setter for the priority of the <code>IntegrationPoint</code>.</p>
     */
    @Attribute
    void setPriority(int priority) {
	this.priority = priority;
    }

    /**
     *	<p> Overrides the toString() method.</p>
     */
    public String toString() {
	return "[IntegrationPoint: '" + id + "' = '" + content + "']";
    }

    /**
     *	<p> This provides access to the {@link ConsoleConfig} which provided
     *	    this <code>IntegrationPoint</code>.
     */
    public String getConsoleConfigId() {
	return this.configId;
    }

    /**
     *	<p> This method should only be called by the
     *	    {@link ConsolePluginService}.  This associates this
     *	    <code>IntegrationPoint</code> with the {@link ConsoleConfig}
     *	    which specified it.</p>
     */
    public void setConsoleConfigId(String id) {
	this.configId = id;
    }


    private String id;
    private String type;
    private String parentId;
    private String content;
    private int priority;
    private String configId = null;
}
