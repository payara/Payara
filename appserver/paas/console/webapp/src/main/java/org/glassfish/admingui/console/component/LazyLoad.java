/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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