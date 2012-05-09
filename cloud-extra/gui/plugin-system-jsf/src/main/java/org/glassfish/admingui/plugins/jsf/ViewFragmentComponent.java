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
package org.glassfish.admingui.plugins.jsf;

import com.sun.faces.application.ApplicationAssociate;
import com.sun.faces.facelets.Facelet;
import com.sun.faces.facelets.impl.DefaultFaceletFactory;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.el.ValueExpression;
import javax.faces.component.FacesComponent;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UINamingContainer;
import javax.faces.component.UIOutput;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.PostAddToViewEvent;
import javax.faces.event.SystemEvent;
import javax.faces.event.SystemEventListener;
import javax.servlet.ServletContext;
import org.glassfish.admingui.plugins.ConsolePluginMetadata;
import org.jvnet.hk2.component.Habitat;

/**
 *
 * @author jasonlee
 */
@FacesComponent(ViewFragmentComponent.COMPONENT_TYPE)
public class ViewFragmentComponent extends UIOutput implements SystemEventListener, NamingContainer {

    public static final String COMPONENT_FAMILY = "org.glassfish.admingui.pluginprototype.ViewFragmentComponent";
    public static final String COMPONENT_TYPE = COMPONENT_FAMILY;
    public static final String RENDERER_TYPE = COMPONENT_FAMILY;
    private static Habitat _habitat;
    private String _type;
    private Object[] _state = null;

    public ViewFragmentComponent() {
        super();
        setRendererType(RENDERER_TYPE);
        FacesContext.getCurrentInstance().getViewRoot().subscribeToViewEvent(PostAddToViewEvent.class, this);
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public boolean isListenerForSource(Object source) {
        return (source instanceof UIViewRoot);
    }

    @Override
    public void processEvent(SystemEvent event) throws AbortProcessingException {
        FacesContext context = FacesContext.getCurrentInstance();
        int count = 1;

        for (ConsolePluginMetadata cpm : PluginUtil.getPluginService().getPlugins()) {
            final List<String> viewIds = cpm.getViewFragments(getType());
            if (viewIds != null) {
                for (String viewId : viewIds) {
                    UINamingContainer wrapper = new UINamingContainer();
                    wrapper.setId("_vfwrapper" + count++);
                    try {
                        processViewFragment(viewId, context, wrapper);
                        getChildren().add(wrapper);
                        wrapper.setParent(this);
                    } catch (IOException ex) {
                        Logger.getLogger(ViewFragmentComponent.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    protected void processViewFragment(String viewId, FacesContext ctx, UIComponent parent) throws IOException {
        ApplicationAssociate associate = ApplicationAssociate.getInstance(ctx.getExternalContext());
        DefaultFaceletFactory faceletFactory = (DefaultFaceletFactory)associate.getFaceletFactory();
        Facelet f = faceletFactory.getFacelet(
//                getClass().getResource(viewId)
                faceletFactory.getResourceResolver().resolveUrl(viewId)
        );

        f.apply(ctx, parent);
    }

    private synchronized Habitat getHabitat(FacesContext context) {
        if (_habitat == null) {
            _habitat = (Habitat) ((ServletContext) context.getExternalContext().getContext()).getAttribute("org.glassfish.servlet.habitat");
//            _habitat = HabitatUtil.getNewHabitat();
        }
        return _habitat;
    }

    public String getType() {
        if (null != this._type) {
            return this._type;
        }
        ValueExpression _ve = getValueExpression("type");
        return (_ve != null) ? (String) _ve.getValue(getFacesContext().getELContext()) : null;
    }

    public void setType(String type) {
        this._type = type;
    }

    @Override
    public void restoreState(FacesContext _context, Object _state) {
        this._state = (Object[]) _state;
        super.restoreState(_context, this._state[0]);
        _type = (String) this._state[1];
    }

    @Override
    public Object saveState(FacesContext _context) {
        if (_state == null) {
            _state = new Object[2];
        }
        _state[0] = super.saveState(_context);
        _state[1] = _type;

        return _state;
    }
}