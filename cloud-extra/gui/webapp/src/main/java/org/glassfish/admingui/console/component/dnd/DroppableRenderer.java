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

import java.io.IOException;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.UIForm;
import javax.faces.component.UINamingContainer;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.FacesRenderer;
import javax.faces.render.Renderer;

import javax.servlet.http.HttpServletRequest;
import org.apache.myfaces.trinidad.component.core.data.CoreTable;
import org.glassfish.admingui.console.event.DragDropEvent;

@FacesRenderer(componentFamily=Droppable.COMPONENT_FAMILY, rendererType=Droppable.COMPONENT_TYPE)
public class DroppableRenderer extends Renderer {

    @Override
    public void decode(FacesContext context, UIComponent component) {
        final ExternalContext externalContext = context.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
        Map<String, String> params = externalContext.getRequestParameterMap();
        if (component instanceof Droppable) {
            Droppable droppable = (Droppable) component;
            String datasourceId = droppable.getDatasource();

            if(params.containsKey("dnd")) {
                String dragId = params.get("dragId");
                String dropId = params.get("dropId");
                DragDropEvent event = null;

                if(datasourceId != null) {
                    CoreTable datasource = findDatasource(context, droppable, datasourceId);
                    String[] idTokens = dragId.split(String.valueOf(UINamingContainer.getSeparatorChar(context)));
                    int rowIndex = Integer.parseInt(idTokens[idTokens.length - 2]);
                    datasource.setRowIndex(rowIndex);
                    Object data = datasource.getRowData();
                    datasource.setRowIndex(-1);

                    event = new DragDropEvent(droppable, dragId, dropId, data);

                }
                else {
                    event = new DragDropEvent(droppable, dragId, dropId);
                }


                droppable.queueEvent(event);
            }
        } else {
            throw new FacesException("Cannot cast component \"" + component.getClass().getName() + "\" to Droppable.");
        }
    }

    @Override
    public void encodeEnd(FacesContext context, UIComponent component) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        if (component instanceof Droppable) {
            Droppable droppable = (Droppable) component;
            String target = findTarget(context, droppable).getClientId(context);
            String clientId = droppable.getClientId(context);
            String onDropUpdate = droppable.getOnDropUpdate();

            writer.startElement("script", droppable);
            writer.writeAttribute("type", "text/javascript", null);

            writer.write("$(Console.escapeClientId('" + target + "'))");
            writer.write(".droppable({\n");

            if(droppable.isDisabled()) {
                writer.write("disabled:true,\n");
            }

            if(droppable.getHoverStyleClass() != null) {
                writer.write("hoverClass:'" + droppable.getHoverStyleClass() + "',\n");
            }

            if(droppable.getActiveStyleClass() != null) {
                writer.write("activeClass:'" + droppable.getActiveStyleClass() + "',\n");
            }

            if(droppable.getAccept() != null) {
                writer.write("accept:'" + droppable.getAccept() + "',\n");
            }

            if(droppable.getScope() != null) {
                writer.write("scope:'" + droppable.getScope() + "',\n");
            }

            if(droppable.getTolerance() != null) {
                writer.write("tolerance:'" + droppable.getTolerance() + "',\n");
            }

            if(droppable.getDropListener() != null && onDropUpdate != null) {
                UIComponent form = findParentForm(context, droppable);
                if (form == null) {
                    throw new FacesException("Droppable: '" + clientId + "' must be inside a form");
                }

                writer.write("drop: function(event, ui) {\n");
                if(droppable.getOnDrop() != null) {
                    writer.write(droppable.getOnDrop() + ".call(event, ui);\n");
                } else {
                    writer.write("ui.draggable.fadeOut('fast');\n");
                }

                writer.write("jsf.ajax.request('" + target + "', event, {\n");
                writer.write("execute:'" + clientId + "',\n");
                writer.write("render:'@form',\n");
                writer.write("dnd: '" + clientId + "',\n");
                writer.write("dragId: ui.draggable.attr('id'),\n");
                writer.write("dropId: '" + target + "'\n");
                writer.write("});\n");
                writer.write("}\n");
            }

            writer.write("});\n");

            writer.endElement("script");
        } else {
            throw new FacesException("Cannot cast component \"" + component.getClass().getName() + "\" to Droppable.");
        }
    }

    protected UIComponent findTarget(FacesContext facesContext, Droppable droppable) {
        String _for = droppable.getFor();

        if(_for != null) {
            UIComponent component = droppable.findComponent(_for);
            if (component == null)
                throw new FacesException("Cannot find component \"" + _for + "\" in view.");
            else
                return component;
        } else {
            return droppable.getParent();
        }
    }

    protected CoreTable findDatasource(FacesContext context, Droppable droppable, String datasourceId) {
        UIComponent datasource = droppable.findComponent(datasourceId);

        if(datasource == null) {
            throw new FacesException("Cannot find component \"" + datasourceId + "\" in view.");
        } else {
            return (CoreTable) datasource;
        }
    }

    protected static UIComponent findParentForm(FacesContext context, UIComponent component) {
        UIComponent parent = component.getParent();

        while(parent != null) {
            if ((parent instanceof UIForm) || (parent.getClass().getSimpleName().endsWith("Form"))) {
                return parent;
            }

            parent = parent.getParent();
        }

        return null;
    }
}
