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
package org.glassfish.admingui.console.component.dnd;

import javax.faces.component.UIComponentBase;
import javax.faces.context.FacesContext;
import javax.el.MethodExpression;
import java.util.List;
import java.util.ArrayList;
import javax.el.ValueExpression;
import javax.faces.application.ResourceDependencies;
import javax.faces.application.ResourceDependency;
import javax.faces.component.FacesComponent;

@FacesComponent(Droppable.COMPONENT_TYPE)
@ResourceDependencies({
    @ResourceDependency(library="glassfish/js", name="jquery-1.6.2.min.js", target="head"),
    @ResourceDependency(library="glassfish/js", name="jquery-ui-1.8.15.min.js", target="head"),
    @ResourceDependency(library="glassfish/js", name="jquery-ui-base.css", target="head")
})
public class Droppable extends UIComponentBase {

    public static final String COMPONENT_TYPE = "org.glassfish.admingui.console.component.Droppable";
    public static final String COMPONENT_FAMILY = COMPONENT_TYPE;
    private static final String DEFAULT_RENDERER = COMPONENT_TYPE;
    private static final String OPTIMIZED_PACKAGE = "org.glassfish.admingui.console.component.";

    protected enum PropertyKeys {

        widgetVar, forValue("for"), disabled, hoverStyleClass, activeStyleClass, onDropUpdate, dropListener, onDrop, accept, scope, tolerance, datasource;
        String toString;

        PropertyKeys(String toString) {
            this.toString = toString;
        }

        PropertyKeys() {
        }

        public String toString() {
            return ((this.toString != null) ? this.toString : super.toString());
        }
    }

    public Droppable() {
        setRendererType(DEFAULT_RENDERER);
    }

    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    public java.lang.String getWidgetVar() {
        return (java.lang.String) getStateHelper().eval(PropertyKeys.widgetVar, null);
    }

    public void setWidgetVar(java.lang.String _widgetVar) {
        getStateHelper().put(PropertyKeys.widgetVar, _widgetVar);
        handleAttribute("widgetVar", _widgetVar);
    }

    public java.lang.String getFor() {
        final String id = (java.lang.String) getStateHelper().eval(PropertyKeys.forValue, null);
        return (id != null) ? id : getParent().getId();
    }

    public void setFor(java.lang.String _for) {
        getStateHelper().put(PropertyKeys.forValue, _for);
        handleAttribute("forValue", _for);
    }

    public boolean isDisabled() {
        return (java.lang.Boolean) getStateHelper().eval(PropertyKeys.disabled, false);
    }

    public void setDisabled(boolean _disabled) {
        getStateHelper().put(PropertyKeys.disabled, _disabled);
        handleAttribute("disabled", _disabled);
    }

    public java.lang.String getHoverStyleClass() {
        return (java.lang.String) getStateHelper().eval(PropertyKeys.hoverStyleClass, null);
    }

    public void setHoverStyleClass(java.lang.String _hoverStyleClass) {
        getStateHelper().put(PropertyKeys.hoverStyleClass, _hoverStyleClass);
        handleAttribute("hoverStyleClass", _hoverStyleClass);
    }

    public java.lang.String getActiveStyleClass() {
        return (java.lang.String) getStateHelper().eval(PropertyKeys.activeStyleClass, null);
    }

    public void setActiveStyleClass(java.lang.String _activeStyleClass) {
        getStateHelper().put(PropertyKeys.activeStyleClass, _activeStyleClass);
        handleAttribute("activeStyleClass", _activeStyleClass);
    }

    public java.lang.String getOnDropUpdate() {
        return (java.lang.String) getStateHelper().eval(PropertyKeys.onDropUpdate, null);
    }

    public void setOnDropUpdate(java.lang.String _onDropUpdate) {
        getStateHelper().put(PropertyKeys.onDropUpdate, _onDropUpdate);
        handleAttribute("onDropUpdate", _onDropUpdate);
    }

    public javax.el.MethodExpression getDropListener() {
        return (javax.el.MethodExpression) getStateHelper().eval(PropertyKeys.dropListener, null);
    }

    public void setDropListener(javax.el.MethodExpression _dropListener) {
        getStateHelper().put(PropertyKeys.dropListener, _dropListener);
        handleAttribute("dropListener", _dropListener);
    }

    public java.lang.String getOnDrop() {
        return (java.lang.String) getStateHelper().eval(PropertyKeys.onDrop, null);
    }

    public void setOnDrop(java.lang.String _onDrop) {
        getStateHelper().put(PropertyKeys.onDrop, _onDrop);
        handleAttribute("onDrop", _onDrop);
    }

    public java.lang.String getAccept() {
        return (java.lang.String) getStateHelper().eval(PropertyKeys.accept, null);
    }

    public void setAccept(java.lang.String _accept) {
        getStateHelper().put(PropertyKeys.accept, _accept);
        handleAttribute("accept", _accept);
    }

    public java.lang.String getScope() {
        return (java.lang.String) getStateHelper().eval(PropertyKeys.scope, null);
    }

    public void setScope(java.lang.String _scope) {
        getStateHelper().put(PropertyKeys.scope, _scope);
        handleAttribute("scope", _scope);
    }

    public java.lang.String getTolerance() {
        return (java.lang.String) getStateHelper().eval(PropertyKeys.tolerance, null);
    }

    public void setTolerance(java.lang.String _tolerance) {
        getStateHelper().put(PropertyKeys.tolerance, _tolerance);
        handleAttribute("tolerance", _tolerance);
    }

    public java.lang.String getDatasource() {
        return (java.lang.String) getStateHelper().eval(PropertyKeys.datasource, null);
    }

    public void setDatasource(java.lang.String _datasource) {
        getStateHelper().put(PropertyKeys.datasource, _datasource);
        handleAttribute("datasource", _datasource);
    }

    public void broadcast(javax.faces.event.FacesEvent event) throws javax.faces.event.AbortProcessingException {
        super.broadcast(event);

        FacesContext facesContext = FacesContext.getCurrentInstance();
        MethodExpression me = getDropListener();

        if (me != null) {
            me.invoke(facesContext.getELContext(), new Object[]{event});
        }
    }

    protected FacesContext getFacesContext() {
        return FacesContext.getCurrentInstance();
    }

    public void handleAttribute(String name, Object value) {
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
                } else if (!setAttributes.contains(name)) {
                    setAttributes.add(name);
                }
            }
        }
    }
}