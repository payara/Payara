/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admingui.common.handlers;

import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.component.ComponentUtil;
import com.sun.jsftemplating.layout.LayoutDefinitionManager;
import com.sun.jsftemplating.layout.LayoutViewHandler;
import com.sun.jsftemplating.layout.descriptors.ComponentType;
import com.sun.jsftemplating.layout.descriptors.LayoutComponent;
import com.sun.jsftemplating.layout.descriptors.LayoutDefinition;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import com.sun.jsftemplating.util.FileUtil;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.glassfish.admingui.common.factories.NavigationNodeFactory;
import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.plugin.ConsolePluginService;
import org.glassfish.admingui.connector.IntegrationPoint;
import org.glassfish.admingui.plugin.IntegrationPointComparator;
import org.glassfish.hk2.api.ServiceLocator;

/**
 *  <p>	This class will provide JSFTemplating <code>Handler</code>s that
 *	provide access to {@link IntegrationPoint}s and possibily other
 *	information / services needed to provide plugin functionality 
 *	i.e. getting resources, etc.).</p>
 *
 * @author Ken Paulsen (ken.paulsen@sun.com)
 */
public class PluginHandlers {

// NOTE: This file used to exist at: v3/admingui/core/src/main/java/org/glassfish/handlers/PluginHandlers.java.

    /**
     *	<p> Constructor.</p>
     */
    protected PluginHandlers() {
    }

    /**
     *	<p> Find and return the <code>ConsolePluginService</code>.  This method
     *	    uses the HK2 <code>Habitat</code> to locate the
     *	    <code>ConsolePluginService</code>.</p>
     *
     *	@param	ctx The <code>FacesContext</code>.
     *
     *	@returns The <code>ConsolePluginService</code>.
     */
    public static ConsolePluginService getPluginService(FacesContext ctx) {
	// We need to get the ServletContext to find the Habitat
	ServletContext servletCtx = (ServletContext)
	    (ctx.getExternalContext()).getContext();

	// Get the Habitat from the ServletContext
	ServiceLocator habitat = (ServiceLocator) servletCtx.getAttribute(
	    org.glassfish.admingui.common.plugin.ConsoleClassLoader.HABITAT_ATTRIBUTE);

//	System.out.println("Habitat:" + habitat);

	return habitat.getService(ConsolePluginService.class);
    }

    /**
     *	<p> This handler returns a
     *	    <code>Map&lt;String id, List&lt;URL&gt;&gt;</code> containing all
     *	    the matches of the requested resource.  Each <code>List</code> in
     *	    the <code>Map</code> is associated with a GUI Plugin, and the key
     *	    to the <code>Map</code> is the plugin id.</p>
     *
     *	@param	handlerCtx	The <code>HandlerContext</code>.
     */
    @Handler(id="getPluginResources",
	input={
	    @HandlerInput(name="name", type=String.class, required=true)},
	output={
	    @HandlerOutput(name="resources", type=Map.class)})
    public static void getPluginResources(HandlerContext handlerCtx) {
	String name = (String) handlerCtx.getInputValue("name");
	ConsolePluginService cps = getPluginService(
	    handlerCtx.getFacesContext());
	handlerCtx.setOutputValue("resources", cps.getResources(name));
    }

    /**
     *	<p> This handler provides access to {@link IntegrationPoint}s for the
     *	    requested key.</p>
     *
     *	@param	handlerCtx	The <code>HandlerContext</code>.
     */
    @Handler(id="getIntegrationPoints",
	input={
	    @HandlerInput(name="type", type=String.class, required=true)},
	output={
	    @HandlerOutput(name="points", type=List.class)})
    public static void getIntegrationPoints(HandlerContext handlerCtx) {
	String type = (String) handlerCtx.getInputValue("type");
	List<IntegrationPoint> value =
	    getIntegrationPoints(handlerCtx.getFacesContext(), type);
	handlerCtx.setOutputValue("points", value);
    }

    /**
     *
     */
    public static List<IntegrationPoint> getIntegrationPoints(FacesContext context, String type) {
	return getPluginService(context).getIntegrationPoints(type);
    }

    /**
     *	<p> This handler adds {@link IntegrationPoint}s of a given type to a
     *	    <code>UIComponent</code> tree.  It looks for
     *	    {@link IntegrationPoint}s using the given <code>type</code>.  It
     *	    then sorts the results (if any) by <code>parentId</code>, and then
     *	    by priority.  It next interates over each one looking for a
     *	    <code>UIComponent</code> with an <code>id</code> which matches the
     *	    its own <code>parentId</code> value.  It then uses the content of
     *	    the {@link IntegrationPoint} to attempt to include the .jsf page
     *	    it refers to under the identified parent component.</p>
     */
    @Handler(id="includeIntegrations",
	input={
	    @HandlerInput(name="type", type=String.class, required=true),
	    @HandlerInput(name="root", type=UIComponent.class, required=false)})
    public static void includeIntegrations(HandlerContext handlerCtx) {
	// Get the input
	String type = (String) handlerCtx.getInputValue("type");
	UIComponent root = (UIComponent) handlerCtx.getInputValue("root");

        try{
	// Get the IntegrationPoints
            FacesContext ctx = handlerCtx.getFacesContext();
            List<IntegrationPoint> points = getIntegrationPoints(ctx, type);
            // Include them
            includeIntegrationPoints(ctx, root, getSortedIntegrationPoints(points));
        }catch(Exception ex){
            GuiUtil.getLogger().severe("Error in includeIntegrations ; \ntype = " + type + "; root=" + root.toString());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
        }
    }

    /**
     *	Includes the first IP based on priority for the given type.  It adds
     *	the content to the given UIComponent root.  If the IP content looks
     *	like a URL (contains ://), a StaticText component will be added with
     *	the value of the content from the URL.
     */
    @Handler(id="includeFirstIntegrationPoint",
	input={
	    @HandlerInput(name="type", type=String.class, required=true),
	    @HandlerInput(name="root", type=UIComponent.class, required=false)})
    public static void includeFirstIntegrationPoint(HandlerContext handlerCtx) throws java.io.IOException {
	// Get the input
	String type = (String) handlerCtx.getInputValue("type");
	UIComponent root = (UIComponent) handlerCtx.getInputValue("root");

	// Get the IntegrationPoints
	FacesContext ctx = handlerCtx.getFacesContext();
	Set<IntegrationPoint> points =
	    getSortedIntegrationPoints(getIntegrationPoints(ctx, type));
	if (points != null) {
	    Iterator<IntegrationPoint> it = points.iterator();
	    if (it.hasNext()) {
		// Get the first one...
		IntegrationPoint point = it.next();
		root = getIntegrationPointParent(ctx, root, point);

		// Check to see if IP points to an external URL...
		if (point.getContent().lastIndexOf("://", 15) != -1) {
		    // Treat content as a url...
		    URL contentURL = FileUtil.searchForFile(
			    point.getContent(), null);
		    if (contentURL == null) {
			throw new IOException("Unable to locate file: "
				+ point.getContent());
		    }

		    // Read the content...
		    String content = new String(FileUtil.readFromURL(contentURL));

		    // Create a StaticText component and add it under the
		    // "root" component.
		    LayoutComponent stDesc = new LayoutComponent(null,
			"externalContent", new ComponentType("tmpTextCT",
			"com.sun.jsftemplating.component.factory.basic.StaticTextFactory"));
		    stDesc.addOption("value", content);
		    ComponentUtil.getInstance(ctx).createChildComponent(ctx, stDesc, root);
		} else {
		    // Include the first one...
		    includeIntegrationPoint(ctx, root, point);
		}
	    }
	}
    }

    /**
     *	Finds the integration point of the specified type.  Returns the contents of this IP type as a list.
     *  The content can be a comma separated String.
     * This is useful for the case such as dropdown or list box to allow additional options in the component.
     */
    @Handler(id="getContentOfIntegrationPoints",
	input={
	    @HandlerInput(name="type", type=String.class, required=true)},
	output={
	    @HandlerOutput(name="labels",  type=List.class),
	    @HandlerOutput(name="values",  type=List.class)})
    public static void getContentOfIntegrationPoints(HandlerContext handlerCtx) throws java.io.IOException {
	// Get the input
	String type = (String) handlerCtx.getInputValue("type");

	// Get the IntegrationPoints
	FacesContext ctx = handlerCtx.getFacesContext();
	Set<IntegrationPoint> points = getSortedIntegrationPoints(getIntegrationPoints(ctx, type));
	List labels = new ArrayList();
	List values = new ArrayList();
	if (points != null) {
	    for(IntegrationPoint it : points){
		String content = it.getContent();
		if (GuiUtil.isEmpty(content)){
		    GuiUtil.getLogger().warning("No Content specified for Integration Point: " + type + " id : " + it.getId());
		    continue;
		}
		List<String> labelsAndValues = GuiUtil.parseStringList(content, "|");
                values.add(labelsAndValues.get(0));
                labels.add(GuiUtil.getMessage(labelsAndValues.get(1), labelsAndValues.get(2)));
	    }
	}
	handlerCtx.setOutputValue("labels", labels);
	handlerCtx.setOutputValue("values", values);
    }

    @Handler(id="getAppEditIntegrationPoint",
	input={
	    @HandlerInput(name="type", type=String.class, required=true)},
	output={
	    @HandlerOutput(name="appEditPageMap",  type=Map.class)})
    public static void getAppEditIntegrationPoint(HandlerContext handlerCtx) throws java.io.IOException {
	// Get the input
	String type = (String) handlerCtx.getInputValue("type");

	// Get the IntegrationPoints
	FacesContext ctx = handlerCtx.getFacesContext();
	Set<IntegrationPoint> points = getSortedIntegrationPoints(getIntegrationPoints(ctx, type));
	Map result = new HashMap();
	if (points != null) {
	    for(IntegrationPoint it : points){
		String content = it.getContent();
		if (GuiUtil.isEmpty(content)){
		    GuiUtil.getLogger().warning("No Content specified for Integration Point: " + type + " id : " + it.getId());
		    continue;
		}
		List<String> vv = GuiUtil.parseStringList(content, ":");
		if (vv.size()!=2){
		    GuiUtil.getLogger().warning("Invalid content specified for Integration Point: " + type + " id : " + it.getId());
		    continue;
		}
		result.put(vv.get(0), vv.get(1));
	    }
	}
	handlerCtx.setOutputValue("appEditPageMap", result);
    }

    /**
     *	<p> This method sorts the given {@link IntegrationPoint}'s by parentId
     *	    and then by priority.  It returns a <code>SortedSet</code> of the
     *	    results with the ABC order parentId.  When parentId's match, the
     *	    highest piority will appear first.</p>
     */
    public static Set<IntegrationPoint> getSortedIntegrationPoints(List<IntegrationPoint> points) {
	// Make sure we have something...
	if (points == null) {
	    return null;
	}

	// Use a TreeSet to sort automatically
	Set<IntegrationPoint> sortedSet =
	    new TreeSet<IntegrationPoint>(
		IntegrationPointComparator.getInstance());
// FIXME: Check for duplicates! Modify "id" if there is a duplicate?
	sortedSet.addAll(points);
	return sortedSet;
    }

    /**
     *
     *	@param	points	This parameter should be the {@link IntegrationPoint}s
     *			to include in the order in which you want to include
     *			them if that matters (i.e. use <code>SortedSet</code>).
     */
    public static void includeIntegrationPoints(FacesContext ctx, UIComponent root, Set<IntegrationPoint> points) {
	if (points == null) {
	    // Do nothing...
	    return;
	}
	if (root == null) {
	    // No root is specified, search whole page
	    root = ctx.getViewRoot();
	}

	// Iterate
	IntegrationPoint point;
	Iterator<IntegrationPoint> it = null;
	int lastSize = 0;
	int currSize = points.size();
	String parentId = null;
	String lastParentId = null;
	while (currSize != lastSize) {
	    // Stop loop by comparing previous size
	    lastSize = currSize;
	    it = points.iterator();
	    lastParentId = "";
	    UIComponent parent = root;

	    // Iterate through the IntegrationPoints
	    while (it.hasNext()) {
		point = it.next();

		// Optimize for multiple plugins for the same parent
		parentId = point.getParentId();
		// Resolve any EL that may be used in identifying the parent ID
		parentId = (String) ComponentUtil.getInstance(ctx).resolveValue(ctx, null, root, parentId);
		if ((parentId == null) || !parentId.equals(lastParentId)) {
		    // New parent (or root -- null)
		    parent = getIntegrationPointParent(ctx, root, point);
		}
		if (parent == null) {
		    // Didn't find the one specified!
// FIXME: log FINE!  Note this may not be a problem, keep iterating to see if we find it later.
//System.out.println("The specified parentId (" + parentId + ") was not found!"); 
		    lastParentId = null;
		    continue;
		}
		lastParentId = parent.getId();

		// Add the content
		includeIntegrationPoint(ctx, parent, point);

		// We found the parent, remove from our list of IPs to add
		it.remove();

	    }

	    // Get the set size to see if we have any left to process
	    currSize = points.size();
	}
    }

    /**
     *	<p> This method returns the parent for the content of the given
     *	    {@link IntegrationPoint}.</p>
     *
     *	@param	root	The <code>UIComponent</code> in which to search for
     *			the parent.
     *	@param	point	The {@link IntegrationPoint} which is looking for its
     *			parent <code>UIComponent</code>.
     */
    public static UIComponent getIntegrationPointParent(FacesContext ctx, UIComponent root, IntegrationPoint point) {
	UIComponent parent = null;
	String parentId = point.getParentId();
	parentId = (String) ComponentUtil.getInstance(ctx).resolveValue(ctx, null, root, parentId);
	if (parentId == null) {
	    // If not specified, just stick it @ the root
	    //parentId = root.getId();
	    parent = root;
	} else {
	    parent = findComponentById(root, parentId);
	}

	// Return the IntegrationPoint parent
	return parent;
    }

    /**
     *	<p> This method includes a single {@link IntegrationPoint} under the
     *	    given parent <code>UIComponent</code>.</p>
     *
     *	@param	ctx	The <code>FacesContext</code>.
     *	@param	parent	The parent for the {@link IntegrationPoint}.
     *	@param	point	The {@link IntegrationPoint}.
     */
    public static void includeIntegrationPoint(FacesContext ctx, UIComponent parent, IntegrationPoint point) {
	// Add the content
	String content = point.getContent();
	while (content.startsWith("/")) {
	    content = content.substring(1);
	}
	String key = content;
	if (!key.contains("://")) {
	    key = "/" + point.getConsoleConfigId() + "/" + content;
	}
	LayoutDefinition def = LayoutDefinitionManager.getLayoutDefinition(ctx, key);
	LayoutViewHandler.buildUIComponentTree(ctx, parent, def);
    }

    /**
     *	<p> This method search for the requested simple id in the given
     *	    <code>UIComponent</code>.  If the id matches the UIComponent, it
     *	    is returned, otherwise, it will search the children and facets
     *	    recursively.</p>
     *
     *	@param	base	The <code>UIComponent</code> to search.
     *	@param	id	The <code>id</code> we're looking for.
     *
     *	@return	The UIComponent, or null.
     */
    private static UIComponent findComponentById(UIComponent base, String id) {
	// Check if this is the one we're looking for
	if (id.equals(base.getId())) {
	    return base;
	}

	// Not this one, check its kids
	Iterator<UIComponent> it = base.getFacetsAndChildren();
	UIComponent comp = null;
	while (it.hasNext()) {
	    // Recurse
	    comp = findComponentById(it.next(), id);
	    if (comp != null) {
		// Found!
		return comp;
	    }
	}

	// Not found
	return null;
    }

    /**
     *	<p> This handler is used for the navigation nodes that request content
     *	    from an external URL.  This handler pulls the "real url" from from
     *	    the component specified by the <code>compId</code> parameter (this
     *	    necessarily depends on the presence of the navigation container in
     *	    the view for the component look up to work).  Once the component
     *	    has been found, the url is retrieved from the attribute map, and
     *	    its contents retrieved.  If <code>processPage</code> is true, the
     *	    URL contents are interpretted and the resulting component(s) are
     *	    added to the component tree (This feature is not currently
     *	    supported)..  Otherwise, the contents are returned in the output
     *	    parameter <code>pluginPage</code> to be output as-is on the
     *	    page.</p>
     *
     * @param handlerCtx    The <code>HandlerContext</code>.
     */
    @Handler(id = "retrievePluginPageContents",
	     input = {@HandlerInput(name = "compId", type = String.class, required = true)},
	     output = {@HandlerOutput(name = "pluginPage", type = String.class)})
    public static void retrievePluginPageContents(HandlerContext handlerCtx) {
	String id = (String) handlerCtx.getInputValue("compId");
	UIComponent comp = handlerCtx.getFacesContext().getViewRoot().findComponent(id);
	String urlContents = "";
	if (comp != null) {
	    String url = (String) comp.getAttributes().get(NavigationNodeFactory.REAL_URL);
	    try {
		// Read from the URL...
		URL contentUrl = FileUtil.searchForFile(url, null);
		if (contentUrl == null) {
		    throw new IOException("Unable to locate file: " + url);
		}
		urlContents = new String(FileUtil.readFromURL(contentUrl));

		// FIXME: Implement processPage support
		/*
		if (processPage) {
		    // probably do something like what includeIntegrations does
		    ...
		}
		*/
	    } catch (IOException ex) {
            GuiUtil.getLogger().log(Level.SEVERE, "Unable to read url: " + url, ex);
	    }
	}

	// Set the content to output...
	handlerCtx.setOutputValue("pluginPage", urlContents);
    }

    @Handler(id="getPluginIdFromViewId",
        input={@HandlerInput(name="viewId",type=String.class,required=true)},
        output={@HandlerOutput(name="pluginId",type=String.class)}
    )
    public static void getPluginIdFromViewId(HandlerContext handlerCtx) {
        String viewId = (String) handlerCtx.getInputValue("viewId");
        if (viewId == null) {
            return;
        }
        ConsolePluginService cps = getPluginService(handlerCtx.getFacesContext());
        String pluginId = "common";
        int next = viewId.indexOf("/", 1);
        if (next > -1) {
            pluginId = viewId.substring(0, next);
            String resource = viewId.substring(next);

            if (pluginId.startsWith("/")) {
                pluginId = pluginId.substring(1);
            }

            ClassLoader cl = cps.getModuleClassLoader(pluginId);
            URL url = null;
            if (cl != null) {
                url = cl.getResource(resource);
            }
            if (url == null) {
                pluginId = "common";
            }
        }
        handlerCtx.setOutputValue("pluginId", pluginId);
    }

    @Handler(id="calculateHelpUrl",
        input={
            @HandlerInput(name="pluginId",type=String.class,required=true),
            @HandlerInput(name="helpKey",type=String.class,required=true)
        },
        output={
            @HandlerOutput(name="url",type=String.class)
        }
    )
    public static void calculateHelpUrl(HandlerContext handlerCtx) {
        String pluginId = (String) handlerCtx.getInputValue("pluginId");
        String helpKey = (String)handlerCtx.getInputValue("helpKey");
        ConsolePluginService cps = getPluginService(handlerCtx.getFacesContext());

        ClassLoader cl = cps.getModuleClassLoader(pluginId);

        // Try the viewRoot locale first
        String path = getHelpPathForResource(helpKey, handlerCtx.getFacesContext().getViewRoot().getLocale(), cl);
        if (path == null) {
            // Try the default locale
            path = getHelpPathForResource(helpKey, Locale.getDefault(), cl);

            // Default to en
            if (path == null) {
                path = "/en/help/" + helpKey;
            }
        }

        handlerCtx.setOutputValue("url", path);
    }

    /**
     *  <p> This function attempts to calculate a <em>help</em> path with the
     *      given locale and classloader.  It only succeeds if it is able to
     *      confirm a file exists at the generated path as determined by
     *      <code>ClassLoader.getResource(path)</code>.  The paths checked are
     *      the following in this order:</p>
     *
     *  <ul><li><code>/locale.toString()/help/&lt;resource&gt;</code></li>
     *      <li><code>/locale.getLanguage()_locale.getCountry()/help/&lt;resource&gt;</code></li>
     *      <li><code>/locale.getLanguage()/help/&lt;resource&gt;</code></li></ul>
     *
     *  <p> If all of those fail to yield a file in the classpath, then
     *      <code>null</code> will be returned.</p>
     */
    public static String getHelpPathForResource(String resource, Locale locale, ClassLoader cl) {
        String path = "/" + locale.toString() + "/help/" + resource;
        
        // Try with full locale
        boolean found = (cl.getResource(path) != null);

        // Try with language_COUNTRY
        if (!found) {
            String language = locale.getLanguage();
            String country = locale.getCountry();
            path = "/" + language + "_" + country + "/help/" + resource;
            found = (cl.getResource(path) != null);

            // Try with just language
            if (!found) {
                path = "/" + language + "/help/" + resource;
                found = (cl.getResource(path) != null);
                if (!found) {
                    // Still not found, so return null
                    path = null;
                }
            }
        }

        return path;
    }

    /*
    private static String getPluginIdFromViewId(ConsolePluginService cps, String viewId) {
        String pluginId = "";
        int next = viewId.indexOf("/", 1);
        if (next > -1) {
            pluginId = viewId.substring(0, next);
            String resource = viewId.substring(next);

            if (pluginId.startsWith("/")) {
                pluginId = pluginId.substring(1);
            }

            ClassLoader cl = cps.getModuleClassLoader(pluginId);
            if (cl != null) {
                if (cl.getResource(resource) == null) {
                    pluginId = "";
                }
            }
        }

        return pluginId;
    }
    */
}
