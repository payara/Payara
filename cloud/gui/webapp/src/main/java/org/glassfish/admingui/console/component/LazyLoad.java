/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admingui.console.component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.el.ValueExpression;
import javax.faces.application.Resource;
import javax.faces.application.ResourceDependencies;
import javax.faces.application.ResourceDependency;
import javax.faces.component.FacesComponent;
import javax.faces.component.UIComponent;
import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

/**
 *
 * @author jdlee
 */
@FacesComponent(LazyLoad.COMPONENT_TYPE)
@ResourceDependencies({
    @ResourceDependency(library = "glassfish/js", name = "jquery-1.6.2.min.js", target = "head"),
    @ResourceDependency(library = "glassfish/js", name = "glassfish.js")
})
public class LazyLoad extends UIComponentBase {

    public static final String COMPONENT_TYPE = "org.glassfish.admingui.console.component.LazyLoad";
    public static final String COMPONENT_FAMILY = COMPONENT_TYPE;
    private static final String DEFAULT_RENDERER = COMPONENT_TYPE;
    private static final String OPTIMIZED_PACKAGE = "org.glassfish.admingui.console.component.";

    protected enum PropertyKeys {
        style;
    }

    public LazyLoad() {
        setRendererType(DEFAULT_RENDERER);
    }

    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public void encodeBegin(FacesContext fc) throws IOException {
        if (!isAjaxRequest(fc)) {
            Resource ajaxLoader = fc.getApplication().getResourceHandler().createResource("ajax-loader.gif", "glassfish/images");
            ResponseWriter writer = fc.getResponseWriter();
            writer.startElement("span", this);
            writer.writeAttribute("id", getClientId(), "id");
            writer.writeAttribute("class", "__lazyload", "class");
            writer.writeAttribute("style", getStyle(), "style");
            writer.startElement("img", this);
            writer.writeAttribute("src", ajaxLoader.getRequestPath(), "src");
            writer.writeAttribute("style", "vertical-align: middle; padding-right: 10px;", "style");
            writer.endElement("img");
            writer.write("Loading content...");
        } else {
        }
    }

    @Override
    public void encodeChildren(FacesContext fc) throws IOException {
        if (isAjaxRequest(fc)) {
            for (UIComponent child : getChildren()) {
                child.encodeAll(fc);
            }
        }
    }

    @Override
    public void encodeEnd(FacesContext fc) throws IOException {
        if (!isAjaxRequest(fc)) {
            ResponseWriter writer = fc.getResponseWriter();
            Resource javascript = fc.getApplication().getResourceHandler().createResource("glassfish.js", "glassfish/js");
            writer.endElement("span");
            writer.startElement("script", this);
            writer.writeAttribute("type", "text/javascript", "type");
            writer.writeAttribute("src", javascript.getRequestPath(), "src");
            writer.endElement("script");
            writer.startElement("script", this);
            writer.writeAttribute("type", "text/javascript", "type");
            writer.write("doLazyLoad();");
//            writer.write("\n$(document).ready(\nfunction(){\nlazyLoadElements.push('" + getClientId() + "');\ndoLazyLoad()\n});");
            writer.endElement("script");
        } else {
        }
    }

    @Override
    public boolean getRendersChildren() {
        return true;
    }

    public java.lang.String getStyle() {
        return (java.lang.String) getStateHelper().eval(PropertyKeys.style);

    }

    public void setStyle(java.lang.String style) {
        getStateHelper().put(PropertyKeys.style, style);
        handleAttribute("style", style);
    }

    protected boolean isAjaxRequest(FacesContext fc) {
        return fc.getPartialViewContext().isAjaxRequest() || fc.getPartialViewContext().isPartialRequest();
    }

    private void handleAttribute(String name, Object value) {
        List<String> setAttributes = (List<String>) this.getAttributes().get("javax.faces.component.UIComponentBase.attributesThatAreSet");
        if (setAttributes == null) {
            String cname = this.getClass().getName();
            if (cname != null && cname.startsWith(OPTIMIZED_PACKAGE)) {
                setAttributes = new ArrayList<String>(6);
                this.getAttributes().put("javax.faces.component.UIComponentBase.attributesThatAreSet", setAttributes);
            }
        }
        if (setAttributes != null) {
            if (value == null) {
                ValueExpression ve = getValueExpression(name);
                if (ve == null) {
                    setAttributes.remove(name);
                }
            } else if (!setAttributes.contains(name)) {
                setAttributes.add(name);
            }
        }
    }
}