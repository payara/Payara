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

package org.glassfish.admingui.common.help;

import com.sun.jsftemplating.component.factory.tree.TreeAdaptor;
import com.sun.jsftemplating.component.factory.tree.TreeAdaptorBase;
import com.sun.jsftemplating.layout.descriptors.LayoutComponent;
import java.util.ArrayList;
import java.util.Collections;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.Serializable;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.glassfish.admingui.connector.TOC;
import org.glassfish.admingui.connector.TOCItem;


/**
 *  <p> The <code>HelpTreeAdaptor</code> implementation must have a
 *	<code>public static HelpTreeAdaptor getInstance(FacesContext,
 *	LayoutComponent, UIComponent)</code> method in order to get access to
 *	an instance of the <code>HelpTreeAdaptor</code> instance.</p>
 *
 *  <p>	This class is used by <code>DynamicTreeNodeFactory</code>.</p>
 *
 *  @author Ken Paulsen (ken.paulsen@sun.com)
 */
public class HelpTreeAdaptor extends TreeAdaptorBase {

    /**
     *	<p> This constructor is not used.</p>
     */
    private HelpTreeAdaptor() {
    }

    /**
     *	<p> This constructor saves the <code>LayoutComponent</code> descriptor
     *	    and the <code>UIComponent</code> associated with this
     *	    <code>TreeAdaptor</code>.  This constructor is used by the
     *	    getInstance() method.</p>
     */
    protected HelpTreeAdaptor(LayoutComponent desc, UIComponent parent) {
	super(desc, parent);
    }

    /**
     *	<p> This method provides access to an <code>HelpTreeAdaptor</code>
     *	    instance.  Each time it is invoked, it returns a new instance.</p>
     */
    public static TreeAdaptor getInstance(FacesContext ctx, LayoutComponent desc, UIComponent parent) {
	return new HelpTreeAdaptor(desc, parent);
    }

    /**
     *	<p> This method is called shortly after
     *	    {@link #getInstance(FacesContext, LayoutComponent, UIComponent)}.
     *	    It provides a place for post-creation initialization to take
     *	    occur.</p>
     */
    public void init() {
	// Get the FacesContext
	FacesContext ctx = FacesContext.getCurrentInstance();

	// This is the descriptor for this dynamic TreeNode, it contains all
	// information (options) necessary for this Adaptor
	LayoutComponent desc = getLayoutComponent();

	// The parent UIComponent
	UIComponent parent = getParentUIComponent();

	// Get the TOC
        TOC toc = (TOC) desc.getEvaluatedOption(ctx, "toc", parent);

	// The following method should set the "key" to the node containing all
	// the children... the children will also have keys which must be
	// retrievable by the next method (getChildTreeNodeObjects)... these
	// "keys" will be used by the rest of the methods in this file for
	// getting information about the TreeNode that should be built.
	setTreeNodeObject(toc);
    }

    /**
     *	<p> Returns child <code>TOCItem</code>s for the given
     *	    <code>TOCItem</code> or <code>TOC</code> model Object.  If
     *	    <code>null</code> is supplied, <code>null</code> is returned
     *	    any other <code>Object</code> type will result in an
     *	    <code>IllegalArgumentException</code>.</p>
     */
    public List getChildTreeNodeObjects(Object nodeObject) {
	if (nodeObject == null) {
	    return null;
	}
        List<TOCItem> result = null;
	if (nodeObject instanceof TOCItem) {
	    result = new ArrayList<TOCItem>(((TOCItem) nodeObject).getTOCItems());
	}
	if (nodeObject instanceof TOC) {
	    result = new ArrayList<TOCItem>(((TOC) nodeObject).getTOCItems());
	}
        if (null != result) {
            Collections.sort(result, new HelpTreeAdaptor.TOCItemComparator());
            return result;
        }
	throw new IllegalArgumentException("Invalid node type for TOC: "
		+ nodeObject.getClass().getName());
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
	Map<String, Object> props = new HashMap<String, Object>();
	if (nodeObject instanceof TOC) {
	    // This case deals with the top node.
	    // Do (almost) nothing so that the root node does not show up...
	    props.put("clientSide", true);
	    Object value = getLayoutComponent().getOption("style");
	    if (value != null) {
		props.put("style", value);
	    }
	    return props;
	} else if (!(nodeObject instanceof TOCItem)) {
	    throw new IllegalArgumentException("Invalid node type for TOC: "
		    + nodeObject.getClass().getName());
	}


	TOCItem item = (TOCItem) nodeObject;

	// Setup the properties...
	// NOTE: All supported options must be handled here,
	//		otherwise they'll be ignored.
	// NOTE: Options will be evaluated later, do not eval here.
	props.put("expanded", item.isExpand());
	props.put("text", item.getText());
	// Add leading "/resource/" to ensure it's treated as *context root* relative.
	props.put("url", "/resource/" + item.getTargetPath());
//	LayoutComponent desc = getLayoutComponent();
//	setProperty(props, "imageURL", desc.getOption("imageURL"));
//	setProperty(props, "target", desc.getOption("target"));
//	setProperty(props, "action", desc.getOption("action"));
// NOTE: Although actionListener is supported, LH currently
//	     implements this to be the ActionListener of the "turner"
//	     which is inconsistent with "action".  We should make use
//	     of the "Handler" feature which provides a "toggle"
//	     CommandEvent.
//	setProperty(props, "rendered", desc.getOption("rendered"));
//	setProperty(props, "actionListener", desc.getOption("actionListener"));
// Use "childActionListener" for the hyperlink, not the TreeNode??
//	    setProperty(props, "actionListener", desc.getOption("childActionListener"));

	// Return the options
	return props;
    }

    /**
     *	<p> Helper method for setting Properties while avoiding NPE's.</p>
    private void setProperty(Map props, String key, Object value) {
	if (value != null) {
	    props.put(key, value);
	}
    }
     */

    /**
     *	<p> This method returns the <code>id</code> for the given tree node
     *	    model object.</p>
     */
    public String getId(Object nodeObject) {
	String id = "invalideNodeObjectType";
	if (nodeObject == null) {
	    id = "nullNodeObject";
	} else if (nodeObject instanceof TOCItem) {
	    id = genId(((TOCItem) nodeObject).getTarget());
	} else if (nodeObject instanceof TOC) {
	    id = getLayoutComponent().getId(
		FacesContext.getCurrentInstance(), getParentUIComponent());
	}
	return id;
    }

    /**
     *	<p> This method generates an ID that is safe for JSF for the given
     *	    String.  It does not guarantee that the id is unique, it is the
     *	    responsibility of the caller to pass in a String that will result
     *	    in a UID.  All non-ascii characters will be stripped.</p>
     *
     *	@param	uid	A non-null String.
     */
    private String genId(String uid) {
	char [] chArr = uid.toCharArray();
	int len = chArr.length;
	int newIdx = 0;
	for (int idx=0; idx<len; idx++) {
            char test = chArr[idx];
	    if (Character.isLetterOrDigit(test) || test=='_' || test=='-' ) {
		chArr[newIdx++] = test;
	    }
	}
	return new String(chArr, 0, newIdx);
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
	// Do nothing
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
	// Do nothing
	return null;
    }

    /**
     *	<p> This method returns the <code>UIComponent</code> factory class
     *	    implementation that should be used to create a
     *	    <code>TreeNode</code> for the given tree node model object.</p>
     *
     *	<p> This implementation returns the default return value, unless the
     *	    <code>nodeObject</code> represents the root node. In that case it
     *	    will return a the TreeFactory factory class:</p>
     *
     *	<p> com.sun.jsftemplating.component.factory.sun.TreeFactory</p>
     */
    public String getFactoryClass(Object nodeObject) {
	String factory = super.getFactoryClass(nodeObject);
	if (nodeObject instanceof TOC) {
	    // For the root... make it the tree itself.
	    factory = 
		"com.sun.jsftemplating.component.factory.sun.TreeFactory";
	}
	return factory;
    }

    /**
     *	<p> Comparator class for {@link TOCItems}.  Uses "text" for comparison,
     *	    ignoring case.</p>
     */
    private static class TOCItemComparator implements Comparator<TOCItem>, Serializable{
        @Override
        public int compare(TOCItem x, TOCItem y) {
            int result = 0;
            if (null != x && null != y) {
                if (!x.equals(y)) {
                    String xText = x.getText(), yText = y.getText();
                    if (null != xText && null != yText) {
                        result = xText.compareToIgnoreCase(yText);
                    }
                }
            } else {
                if (null == x && null == y) {
                    result = 0;
                } else {
                    // consider null to be less.
                    if (null == x) {
                        result = -1;
                    } else {
                        result = 1;
                    }
                }
            }
            return result;
        }
    }
}
