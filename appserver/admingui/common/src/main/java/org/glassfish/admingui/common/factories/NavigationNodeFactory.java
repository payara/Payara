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

package org.glassfish.admingui.common.factories;

import com.sun.jsftemplating.annotation.UIComponentFactory;
import com.sun.jsftemplating.component.factory.ComponentFactoryBase;
import com.sun.jsftemplating.layout.descriptors.LayoutComponent;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

/**
 *  <p>	The <code>NavigationNodeFactory</code> provides an abstraction layer
 *	for the a tree node component, currently implemented as a Woodstock
 *	treeNode component.  This provides the ability to change the treeNode
 *	implementation to another component or set, or a different component
 *	type altogether.  The supported attributes are:</p>
 *
 *  <ul><li><code>id</code> - The ID of the component.  While IDs are
 *	    optional, it is a good idea to provide a specific ID, especially if
 *	    one expects to want to add nodes under this node in the future.</li>
 *	<li><code>label</code> - The text label for the navigation node</li>
 *	<li><code>url</code> - An optional URL</li>
 *	<li><code>icon</code> - The URL to an image for the tree's root
 *	    icon</li>
 *	<li><code>target</code> - An optional target to specify on the link
 *	    created for this node (e.g., '_blank')</li>
 *	<li><code>expanded</code> - A boolean indicating whether or not this
 *	    node should be expanded by default.</li>
 *	<li><code>template</code> - An optional parameter which indicates what
 *	    template should be used to decorate the page to which this node
 *	    links.  The value will be a relative path to a template file
 *	    provided by the Admin Console or one of its plugins (e.g.,
 *	    '/pluginId/templates/customLayout.xhtml').  The default value is
 *	    <code>/templates/default.layout</code>.  If the <code>url</code>
 *	    parameter points to an external resource, the URL rendered will
 *	    point a page in the admin console.  This page will then read the
 *	    contents of the users-specified URL and display those contents in
 *	    the appropriate spot in the specified template.</li>
 *	<li><code>processPage</code> - This option is intended to be used in
 *	    conjunction with the <code>template</code> parameter.  By default,
 *	    the page referred to by the URL will displayed as is.  If, however,
 *	    the plugin author provides a page which does not represent HTML,
 *	    but instead represents the admin console's native data format, the
 *	    parameter should be set to <code>true</code>.</li></ul>
 *
 *  @author Jason Lee
 *  @author Ken Paulsen (ken.paulsen@sun.com)
 */
@UIComponentFactory("gf:navNode")
public class NavigationNodeFactory extends ComponentFactoryBase {

    /**
     *	<p> This is the factory method responsible for creating the
     *	    <code>UIComponent</code>.</p>
     *
     *	@param	context	    The <code>FacesContext</code>.
     *	@param	descriptor  The {@link LayoutComponent} descriptor associated.
     *			    with the requested <code>UIComponent</code>.
     *	@param	parent	    The parent <code>UIComponent</code>.
     *
     *	@return	The newly created <code>TreeNode</code>.
     */
    public UIComponent create(FacesContext context, LayoutComponent descriptor, UIComponent parent) {
        // Create the UIComponent
        UIComponent comp = createComponent(context, COMPONENT_TYPE, descriptor, parent);
        String compId = (String) descriptor.getId(context, comp.getParent());
        if ((compId != null) && (!compId.equals(""))) {
            comp.setId(compId);
        }

        Object url = descriptor.getOption("url");
        final Object icon = descriptor.getOption("icon");
        final Object label = descriptor.getOption("label");
        final Object target = descriptor.getOption("target");
        final Object expanded = descriptor.getOption("expanded");
        final Object template = descriptor.getOption("template");
        final Object processPage = descriptor.getOption("processPage");
        final Object toolTip = descriptor.getOption("toolTip");

        // Set all the attributes / properties
        applyOption(context, comp, descriptor, "text", label);
        applyOption(context, comp, descriptor, "target", target);
        applyOption(context, comp, descriptor, "expanded", expanded);
        applyOption(context, comp, descriptor, "imageURL", icon);
        applyOption(context, comp, descriptor, "toolTip", toolTip);
        if (url != null) {
            final boolean externalResource = ((String) url).contains("://");
// FIXME: There does not seem to be any way to have an external URL which does *not* use a template!
            if (externalResource) {
// FIXME: Why doesn't this use setOption instead?  This may prevent #{} from being used.
                comp.getAttributes().put(REAL_URL, url);
                comp.getAttributes().put("template", (template != null) ? template : "/templates/default.layout");
		// NOTE: contextPath (which is directly accessible via the
		// NOTE: ExternalContext) is not needed for WS Hyperlink
		// NOTE: components this is automatically added.
                url = "/common/" + "pluginPage.jsf?id=" + comp.getClientId(context);
            }
            setOption(context, comp, descriptor, "url", url);
            if (icon != null) {
                UIComponent imageHyperlink = context.getApplication().createComponent("com.sun.webui.jsf.ImageHyperlink");
                applyOption(context, imageHyperlink, descriptor, "imageURL", icon);
                applyOption(context, imageHyperlink, descriptor, "url", url);
                applyOption(context, imageHyperlink, descriptor, "border", 0);
                applyOption(context, imageHyperlink, descriptor, "target", target);
                applyOption(context, imageHyperlink, descriptor, "alt", toolTip);
                comp.getFacets().put("image", imageHyperlink);
            }
        }

	// FIXME: Maybe we should have made this a "contentType" property?  Or
	// FIXME: something that specifies what the content is so that it can be
	// FIXME: processed via a pluggable mechanism??
// FIXME: Why doesn't this use setOption instead?  This may prevent #{} from being used.
        comp.getAttributes().put("processPage", (processPage != null) ? processPage : false);

        // Return the component
        return comp;
    }

    protected void applyOption(FacesContext context, UIComponent comp, LayoutComponent lc, String name, Object value) {
        if (value != null) {
            this.setOption(context, comp, lc, name, value);
        }
    }

    /**
     *	<p> The <code>UIComponent</code> type that must be registered in the
     *	    <code>faces-config.xml</code> file mapping to the UIComponent class
     *	    to use for this <code>UIComponent</code>.</p>
     */
    public static final String COMPONENT_TYPE = "com.sun.webui.jsf.TreeNode";

    /**
     *	<p> The attribute name on the TreeNode component that contains the real
     *	    URL of the content in which to include.  This is only used if the
     *	    user wishes to take advantage of the admin console templating to
     *	    add the header and navigation content to their page.</p>
     */
    public static final String REAL_URL = "realUrl";
}
