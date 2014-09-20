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

package org.glassfish.admingui.common.tree;

import com.sun.jsftemplating.component.ComponentUtil;
import com.sun.jsftemplating.component.factory.tree.TreeAdaptor;
import com.sun.jsftemplating.component.factory.tree.TreeAdaptorBase;
import com.sun.jsftemplating.layout.descriptors.LayoutComponent;
import com.sun.jsftemplating.layout.event.CommandActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.faces.component.ActionSource;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import org.glassfish.admingui.common.util.GuiUtil;


/**
 *  <p> The <code>ListTreeAdaptor</code> implementation must have a
 *	<code>public static ListTreeAdaptor getInstance(FacesContext,
 *	LayoutComponent, UIComponent)</code> method in order to get access to
 *	an instance of the <code>ListTreeAdaptor</code> instance.</p>
 *
 *  <p>	This class is used by <code>DynamicTreeNodeFactory</code>.</p>
 *
 *  <p>	Valid options for this implementation:</p>
 *
 *  <ul><li>children (required) -- The children to display.</li>
 *	<li>text -- The root TreeNode text.</li>
 *	<li>url -- The root TreeNode URL.</li>
 *	<li>imageURL -- The root TreeNode Image URL.</li>
 *	<li>target -- The target of the link for the root TreeNode.</li>
 *	<li>action -- The JSF EL expression to an action associated with the
 *	    root TreeNode.</li>
 *	<li>actionListener -- The turner JSF actionListener for the root
 *	    TreeNode.</li>
 *	<li>expanded -- Whether the root TreeNode should be expanded by
 *	    default (true/false)</li>
 *	<li>rendered -- The JSF rendered flag for the root TreeNode.</li>
 *	<li>childNameKey -- When the child is represented by a Map, the "key"
 *	    used to extract the name of the TreeNode from the Map.</li>
 *	<li>childURL -- The child TreeNode URL.</li>
 *	<li>childImageURL -- The child TreeNode Image URL.</li>
 *	<li>childCommand -- The child TreeNode JSFTemplating command.</li>
 *	<li>childTarget -- The child TreeNode link target.</li>
 *	<li>childAction --The child TreeNode JSF action.</li>
 *	<li>targetConfigName -- The child TreeNode target config name.</li>
 *	<li>childActionListener -- The JSF ActionListener for the child
 *	    TreeNode hyperlink.</li>
 *	<li>childExpanded -- Boolean flag indicating whether to render the
 *	    child TreeNodes expanded.</li>
 * 	</ul>
 *
 *  @author Ken Paulsen (ken.paulsen@oracle.com)
 */
public class ListTreeAdaptor extends TreeAdaptorBase {

    /**
     *	<p> This constructor is not used.</p>
     */
    private ListTreeAdaptor() {
    }

    /**
     *	<p> This constructor saves the <code>LayoutComponent</code> descriptor
     *	    and the <code>UIComponent</code> associated with this
     *	    <code>TreeAdaptor</code>.  This constructor is used by the
     *	    getInstance() method.</p>
     */
    protected ListTreeAdaptor(LayoutComponent desc, UIComponent parent) {
	super(desc, parent);
    }

    /**
     *	<p> This method provides access to an <code>ListTreeAdaptor</code>
     *	    instance.  Each time it is invoked, it returns a new instance.</p>
     */
    public static TreeAdaptor getInstance(FacesContext ctx, LayoutComponent desc, UIComponent parent) {
	return new ListTreeAdaptor(desc, parent);
    }

    /**
     *	<p> This method is called shortly after
     *	    {@link #getInstance(FacesContext, LayoutComponent, UIComponent)}.
     *	    It provides a place for post-creation initialization to take
     *	    occur.</p>
     */
    public void init() {
	// The following method should set the "key" to the node containing all
	// the children... the children will also have keys which must be
	// retrievable by the next method (getChildTreeNodeObjects)... these
	// "keys" will be used by the rest of the methods in this file for
	// getting information about the TreeNode that should be built.
	setTreeNodeObject(TOP_ID);
    }

    /**
     *	<p> Returns child <code>TreeNode</code>s for the given
     *	    <code>TreeNode</code> model Object.</p>
     */
    public List getChildTreeNodeObjects(Object nodeObject) {
	if (nodeObject == null) {
	    return null;
	}
	if ((nodeObject instanceof Integer) && nodeObject.equals(TOP_ID)) {
	    // In this implementation TOP_ID represents the top-level,
	    // Find/Return children here
	    if (_children != null) {
		return _children;
	    }

	    // Find the children...
	    FacesContext ctx = FacesContext.getCurrentInstance();

	    // This is the descriptor for this dynamic TreeNode, it contains all
	    // information (options) necessary for this Adaptor
	    LayoutComponent desc = getLayoutComponent();
	    Object val = desc.getOption("children");
	    if (val == null) {
		throw new IllegalArgumentException("'children' must be specified!");
	    }
	    val = desc.resolveValue(ctx, getParentUIComponent(), val.toString());
	    if ((val != null) && (val instanceof Map)) {
		_childMap = (Map<String, Object>) val;
		val = new ArrayList<Object>(_childMap.keySet());
                Collections.sort((List)val);
	    }
	    _children =	(List<Object>) val;
		
	    // Ok, we got the result, provide an event in case we want to
	    // do some filtering
	    Object retVal = getLayoutComponent().dispatchHandlers(
		    ctx, FilterTreeEvent.EVENT_TYPE,
		    new FilterTreeEvent(getParentUIComponent(), _children));
	    if ((retVal != null) && (retVal instanceof List)) {
		// We have a return value, use it instead of the original list
		_children = (List<Object>) retVal;
	    }
	} else {
	    // Currently multiple levels are not implemented
	    return null;
	}

	return _children; 
    }

    /**
     *	<p> This method returns the "options" that should be supplied to the
     *	    factory that creates the <code>TreeNode</code> for the given tree
     *	    node model object.</p>
     *
     *	<p> Some useful options for the standard <code>TreeNode</code>
     *	    component include:<p>
     *
     *	<ul><li>text</li>
     *	    <li>url</li>
     *	    <li>imageURL</li>
     *	    <li>target</li>
     *	    <li>action<li>
     *	    <li>actionListener</li>
     *	    <li>expanded</li></ul>
     *
     *	<p> See Tree / TreeNode component documentation for more details.</p>
     */
    public Map<String, Object> getFactoryOptions(Object nodeObject) {
	if (nodeObject == null) {
	    return null;
	}

	LayoutComponent desc = getLayoutComponent();
	Map<String, Object> props = new HashMap<String, Object>();
	if ((nodeObject instanceof Integer) && nodeObject.equals(TOP_ID)) {
	    // This case deals with the top node.

	    // NOTE: All supported options must be handled here,
	    //		otherwise they'll be ignored.
	    // NOTE: Options will be evaluated later, do not eval here.
	    setProperty(props, "text", desc.getOption("text"));
	    setProperty(props, "url", desc.getOption("url"));
	    setProperty(props, "imageURL", desc.getOption("imageURL"));
	    setProperty(props, "target", desc.getOption("target"));
	    setProperty(props, "action", desc.getOption("action"));
	   

	    // NOTE: Although actionListener is supported, LH currently
	    //	     implements this to be the ActionListener of the "turner"
	    //	     which is inconsistent with "action".  We should make use
	    //	     of the "Handler" feature which provides a "toggle"
	    //	     CommandEvent.
	    setProperty(props, "actionListener", desc.getOption("actionListener"));
	    setProperty(props, "expanded", desc.getOption("expanded"));
	    setProperty(props, "rendered", desc.getOption("rendered"));
	} else {
	    // This case deals with the children
	    if (nodeObject instanceof Map) {
		String key = (String) desc.getOption("childNameKey");
		if (key == null) {
		    key = "name";
		}
		setProperty(props, "text", ((Map) nodeObject).get(key));
	    } else {
		// Use the object itself...
		setProperty(props, "text", nodeObject);
	    }

	    // Finish setting the child properties
	    setProperty(props, "url", desc.getOption("childURL"));
	    setProperty(props, "imageURL", desc.getOption("childImageURL"));
	    setProperty(props, "action", desc.getOption("childAction"));
	    String tt = (String) desc.getOption("targetConfigName");
	    if (!GuiUtil.isEmpty(tt)){
		setProperty(props, "targetConfigName", tt);
	    }
	/*
	    String check = (String) desc.getOption("checkAdminServer");
	    if (!GuiUtil.isEmpty(check)) {
		String serverName = (String) props.get("text");
		if (serverName.equals("server")) {
		    setProperty(props, "text", "server (Admin Server)");
		    setProperty(props, "serverName", "server");
		} else {
		    setProperty(props, "serverName", serverName);
		}
	    }
	*/
	    setProperty(props, "encodedText",
		    GuiUtil.encode((String) props.get("text"), null, null));
// We are using "childActionListener" for the hyperlink, not the TreeNode
//	    setProperty(props, "actionListener", desc.getOption("childActionListener"));
	    setProperty(props, "expanded", desc.getOption("childExpanded"));
	}

	// Return the options
	return props;
    }

    /**
     *	<p> Helper method for setting Properties while avoiding NPE's.</p>
     */
    private void setProperty(Map props, String key, Object value) {
	if (value != null) {
	    props.put(key, value);
	}
    }

    /**
     *	<p> This method returns the <code>id</code> for the given tree node
     *	    model object.</p>
     */
    public String getId(Object nodeObject) {
	if (nodeObject == null) {
	    return "nullNodeObject";
	}
	if ((nodeObject instanceof Integer) && nodeObject.equals(TOP_ID)) {
	    // Top level can use the ID of the LayoutComponent
	    return getLayoutComponent().getId(
		FacesContext.getCurrentInstance(), getParentUIComponent());
	}

	return GuiUtil.genId(nodeObject.toString());
    }

    /**
     *	<p> This method returns any facets that should be applied to the
     *	    <code>TreeNode (comp)</code>.  Useful facets for the sun
     *	    <code>TreeNode</code> component are: "content" and "image".</p>
     *
     *	<p> Facets that already exist on <code>comp</code>, or facets that
     *	    are directly added to <code>comp</code> do not need to be returned
     *	    from this method.</p>
     *
     *	<p> This implementation directly adds a "content" and "image" facet and
     *	    returns <code>null</code> from this method.</p>
     *
     *	@param	comp	    The tree node <code>UIComponent</code>.
     *	@param	nodeObject  The (model) object representing the tree node.
     */
    public Map<String, UIComponent> getFacets(UIComponent comp, Object nodeObject) {
	if (nodeObject == null){
	    return null;
	}
	if ((nodeObject instanceof Integer) && nodeObject.equals(TOP_ID)) {
	    // Top-level facets can be added directly, not needed here
	    return null;
	}

	Properties props = new Properties();
	LayoutComponent desc = this.getLayoutComponent();

	// Check to see if a childActionListener was added
	// NOTE: This is not needed when a "command" event is used.  In the
	//	 case of a CommandEvent an ActionListener will be
	//	 automatically registered by the ComponentFactoryBase class
	//	 during "setOptions()".  Also, setting a childActionListener
	//	 here should not stop "command" handlers from being invoked.
	setProperty(props, "actionListener", desc.getOption("childActionListener"));

	// Also se the target and text...
	setProperty(props, "target", desc.getOption("childTarget"));
// FIXME: Add support for other hyperlink properties??

	// Create Hyperlink
	// NOTE: Last attribute "content" will be the facet named used.
	FacesContext ctx = FacesContext.getCurrentInstance();
	ComponentUtil compUtil = ComponentUtil.getInstance(ctx);
	UIComponent imageLink = compUtil.getChild(
	    comp, "imagelink",
	    "com.sun.jsftemplating.component.factory.sun.ImageHyperlinkFactory",
	    props, "image");

	// Force HTML renderer so we can use dynafaces safely.
	imageLink.setRendererType("com.sun.webui.jsf.ImageHyperlink");

	// We don't want the imageHyperlink to have the following property, so
	// set it after creating it
	setProperty(props, "text", comp.getAttributes().get("text"));
	UIComponent link = compUtil.getChild(
	    comp, "link",
	    "com.sun.jsftemplating.component.factory.sun.HyperlinkFactory",
	    props, "content");

	// Force HTML renderer so we can use dynafaces safely.
	link.setRendererType("com.sun.webui.jsf.Hyperlink");

	// Check to see if we have a childURL, evalute it here (after component
	// is created, before rendered) so we can use the link itself to define
	// the URL.  This has proven to be useful...
	Object val = desc.getOption("childURL");
	if (val != null) {
	    val = desc.resolveValue(ctx, link, val);
	    link.getAttributes().put("url", val);
	    imageLink.getAttributes().put("url", val);
	}

	// Set the image URL
	val = desc.getOption("childImageURL");
	if (val != null) {
	    imageLink.getAttributes().put("imageURL", desc.
		resolveValue(ctx, link, val));
	}

	// Set href's handlers...
	// We do it this way rather than earlier b/c the factory will not
	// recognize this as a property, it requires it to be defined in the
	// LayoutComponent as a handler.  So we must do this manually like
	// this.
	List handlers = desc.getHandlers("childCommand");
	if (handlers != null) {
	    link.getAttributes().put("command", handlers);
	    imageLink.getAttributes().put("command", handlers);
	    // This adds the required action listener to proces the commands
	    // This is needed here b/c the factory has already executed -- the
	    // factory is normally the place where this is added (iff there is
	    // at least one command handler).
        (ActionSource.class.cast(link)).addActionListener(
		    CommandActionListener.getInstance());
	    (ActionSource.class.cast(imageLink)).addActionListener(
		    CommandActionListener.getInstance());
	}

	// We already added the facet, return null...
	return null;
    }

    /**
     *	<p> Advanced framework feature which provides better handling for
     *	    things such as expanding TreeNodes, beforeEncode, and other
     *	    events.</p>
     *
     *	<p> This method should return a <code>Map</code> of <code>List</code>
     *	    of <code>Handler</code> objects.  Each <code>List</code> in the
     *	    <code>Map</code> should be registered under a key that cooresponds
     *	    to to the "event" in which the <code>Handler</code>s should be
     *	    invoked.</p>
     */
    public Map getHandlersByType(UIComponent comp, Object nodeObject) {
	/* These handlers apply to the TreeNode not the Hyperlink */
	/*
	LayoutComponent lc = this.getLayoutComponent();
	List list = lc.getHandlers("childCommand");
	if (list != null) {
	    Map m = new HashMap();
	    m.put("command", list);
	    return m;
	}
	*/
	return null;
    }

    /**
     *	<p> This method returns the <code>UIComponent</code> factory class
     *	    implementation that should be used to create a
     *	    <code>TreeNode</code> for the given tree node model object.</p>

	This method provides a means use different TreeNodeFactories,
	currently we are using the default which is defined in the superclass,
	if needed we can change this.

    public String getFactoryClass(Object nodeObject) {
	return "com.sun.enterprise.tools.jsfext.component.factory.basic.TreeNodeFactory";
    }
     */

    /**
     *	<p> Used to mark the top element.</p>
     */
    private static final Integer	    TOP_ID	= -98734;

    /**
     *	This sub-nodes of the top-level Node.
     */
    private transient List<Object>	    _children	=   null;
    private transient Map<String, Object>   _childMap	=   null;
}
