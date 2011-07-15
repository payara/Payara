/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admingui.common.factories;

import com.sun.jsftemplating.annotation.UIComponentFactory;
import com.sun.jsftemplating.component.factory.ComponentFactoryBase;
import com.sun.jsftemplating.layout.descriptors.LayoutComponent;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

/**
 * The NavigationFactory provides an abstraction layer for the Woodstock tree
 * component (currently), giving us the ability to change the tree implementation to
 * another component or set, or a different component type altogether.  The supported
 * attributes are:
 * <ul>
 * <li>id</li>
 * <li>label - The text label for the top of the tree</li>
 * <li>url - An optional URL</li>
 * <li>icon - The URL to an image for the tree's root icon</li>
 * <li>target</li>
 * </ul>
 */
@UIComponentFactory("gf:navNodeContainer")
public class NavNodeContainerFactory extends ComponentFactoryBase {

    /**
     *	<p> This is the factory method responsible for creating the
     *	    <code>UIComponent</code>.</p>
     *
     *	@param	context	    The <code>FacesContext</code>
     *	@param	descriptor  The {@link LayoutComponent} descriptor associated
     *			    with the requested <code>UIComponent</code>.
     *	@param	parent	    The parent <code>UIComponent</code>
     *
     *	@return	The newly created <code>Tree</code>.
     */
    public UIComponent create(FacesContext context, LayoutComponent descriptor, UIComponent parent) {
        // Create the UIComponent
        UIComponent comp = createComponent(context, COMPONENT_TYPE, descriptor, parent);
        String compId = (String) descriptor.getId(context, comp.getParent());
        if ((compId != null) && (!compId.equals(""))) {
            comp.setId(compId);
        }

        final Object url = descriptor.getOption("url");
        final Object icon = descriptor.getOption("icon");
        final Object label = descriptor.getOption("label");
        final Object target = descriptor.getOption("target");

        if (label != null) {
            setOption(context, comp, descriptor, "text",label);
        }
        if (target != null) {
            setOption(context, comp, descriptor, "target",target);
        }
        if (icon != null) {
            setOption(context, comp, descriptor, "imageURL", icon);
        }
        if (url != null) {
            setOption(context, comp, descriptor, "url", url);
        }
        setOption(context, comp, descriptor, "clientSide", Boolean.TRUE);

        // Return the component
        return comp;
    }
    /**
     *	<p> The <code>UIComponent</code> type that must be registered in the
     *	    <code>faces-config.xml</code> file mapping to the UIComponent class
     *	    to use for this <code>UIComponent</code>.</p>
     */
    public static final String COMPONENT_TYPE = "com.sun.webui.jsf.Tree";
}
