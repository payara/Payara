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

import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.render.FacesRenderer;
import javax.faces.render.Renderer;

@FacesRenderer(
        componentFamily=Draggable.COMPONENT_FAMILY,
        rendererType=Draggable.COMPONENT_TYPE
)
public class DraggableRenderer extends Renderer {

    @Override
    public void encodeEnd(FacesContext facesContext, UIComponent component) throws IOException {
        ResponseWriter writer = facesContext.getResponseWriter();
        Draggable draggable;
        if (component instanceof Draggable) {
            draggable = (Draggable) component;
        } else {
            throw new FacesException("Cannot cast component to Draggable for \"" + component.getId());
        }
        String target = findTarget(facesContext, draggable);

        writer.startElement("script", draggable);
        writer.writeAttribute("type", "text/javascript", null);

        writer.write("$(Console.escapeClientId('" + target + "'))");
        writer.write(".draggable({");

        writer.write("cursor:'" + draggable.getCursor() + "'");

        //Configuration
        if (draggable.isDisabled()) {
            writer.write(",disabled:true");
        }

        if (draggable.getAxis() != null) {
            writer.write(",axis:'" + draggable.getAxis() + "'");
        }

        if (draggable.getContainment() != null) {
            writer.write(",containment:'" + draggable.getContainment() + "'");
        }

        if (draggable.getHelper() != null) {
            writer.write(",helper:'" + draggable.getHelper() + "'");
        }

        if (draggable.getRevert() != null) {
            final String revert = draggable.getRevert();

            writer.write(",revert:" + 
                    (("true".equalsIgnoreCase(revert) || "false".equals(revert)) ?
                    revert : "'" + revert + "'"));
        }

        if (draggable.getZindex() != -1) {
            writer.write(",zIndex:" + draggable.getZindex());
        }

        if (draggable.getHandle() != null) {
            writer.write(",handle:'" + draggable.getHandle() + "'");
        }

        if (draggable.getOpacity() != 1.0) {
            writer.write(",opacity:" + draggable.getOpacity());
        }

        if (draggable.getRevertDuration() != null) {
            writer.write(",revertDuration:" + draggable.getRevertDuration());
        }

        if (draggable.getStack() != null) {
            writer.write(",stack:'" + draggable.getStack() + "'");
        }

        if (draggable.getGrid() != null) {
            writer.write(",grid:[" + draggable.getGrid() + "]");
        }

        if (draggable.getScope() != null) {
            writer.write(",scope:'" + draggable.getScope() + "'");
        }

        if (draggable.isSnap()) {
            writer.write(",snap:true");
            writer.write(",snapTolerance:" + draggable.getSnapTolerance());
            if (draggable.getSnapMode() != null)
                writer.write(",snapMode:'" + draggable.getSnapMode() + "'");
        }

        writer.write("});");

        writer.endElement("script");
    }

    protected String findTarget(FacesContext facesContext, Draggable draggable) {
        String _for = draggable.getFor();

        if (_for != null) {
            UIComponent component = draggable.findComponent(_for);
            if (component == null)
                throw new FacesException("Cannot find component \"" + _for + "\" in view.");
            else
                return component.getClientId(facesContext);
        } else {
            return draggable.getParent().getClientId(facesContext);
        }
    }
}
