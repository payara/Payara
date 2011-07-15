/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admingui.theme;

import com.sun.webui.jsf.theme.JSFThemeContext;
import com.sun.webui.theme.ServletThemeContext;
import com.sun.webui.theme.ThemeContext;

import org.glassfish.admingui.common.util.GuiUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Enumeration;

import javax.faces.context.FacesContext;


/**
 *  <p>	This class allows us to use a <code>Map</code> to pass in parameters
 *	which alter the Woodstock theme instead of using context-params from
 *	the web.xml.  This allows us greater runtime control over the theme and
 *	helps allow the Woodstock theme properties to be overridden by
 *	plugins.</p>
 *
 *  @author ana
 *  @author Ken
 */
public class AdminguiThemeContext extends ServletThemeContext {

    /**
     *	<p> This constructor takes in the theme name and version to use by
     *	    default.</p>
     *
     *	@param	themeName	The theme name (i.e. suntheme).
     *	@param	themeVersion	The version number (i.e. 4.2).
     */
    protected AdminguiThemeContext(String themeName, String themeVersion) {
	super(setThemeParams(themeName, themeVersion));

	// The following ThemeContext is created to allow us to delegate our
	// logic to it.
	jsfThemeCtx = JSFThemeContext.getInstance(FacesContext.getCurrentInstance());
        jsfThemeCtx.setThemeServletContext("/theme");
    }

    
    /**
     *	<p> Return an instance of <code>ThemeContext</code> creating one
     *	    if necessary and persisting it in the <code>ApplicationMap</code>.
     */
    public synchronized static ThemeContext getInstance(FacesContext context, String themeName, String themeVersion) {
	Map map = context.getExternalContext().getApplicationMap();
	String themeKey = THEME_CONTEXT + themeName + themeVersion;
	ThemeContext themeContext = (ThemeContext) map.get(themeKey);
	if (themeContext == null) {
	    themeContext = new AdminguiThemeContext(themeName, themeVersion);
	    map.put(themeKey, themeContext);
	}
	return themeContext;
    }
    
/**
     *	<p> Return an instance of <code>ThemeContext</code> 
     *	    using properties provided via <code>Integration point</code>.
     */
    public synchronized static ThemeContext getInstance(FacesContext context,  Properties propMap) {
	Map map = context.getExternalContext().getApplicationMap();        
        String themeName = (String)propMap.get(THEME_NAME_KEY);
        String themeVersion = (String)propMap.get(THEME_VERSION_KEY);
	String themeKey = THEME_CONTEXT + themeName + themeVersion;
	ThemeContext themeContext = (ThemeContext) map.get(themeKey);
	if (themeContext == null) {
	    themeContext = new AdminguiThemeContext(themeName, themeVersion);
	    map.put(themeKey, themeContext);
	}
	return themeContext;
    }    

    
    /**
     *	<p> Creates a <code>Map</code> object with the theme name and
     *	    version.</p>
     */
    public static Map setThemeParams(String theme, String version) {
	Map map = new HashMap();
	if (theme == null) {
	    theme = "suntheme";
	}
	map.put(ThemeContext.DEFAULT_THEME, theme);
	if (version == null) {
	    version = "4.2";
	}
	map.put(ThemeContext.DEFAULT_THEME_VERSION, version);
	return map;
    }
    
    
    /**
     *	<p> This method delegates to <code>JSFThemeContext</code>.</p>
     */
    public ClassLoader getDefaultClassLoader() {
	return jsfThemeCtx.getDefaultClassLoader();
    }

    /**
     *	<p> This method delegates to <code>JSFThemeContext</code>.</p>
     */
    public void setDefaultClassLoader(ClassLoader classLoader) {
	jsfThemeCtx.setDefaultClassLoader(classLoader);
    }

    /**
     *	<p> This method delegates to <code>JSFThemeContext</code>.</p>
     */
    public String getRequestContextPath() {
	return jsfThemeCtx.getRequestContextPath();
    }

    /**
     *	<p> This method delegates to <code>JSFThemeContext</code>.</p>
     */
    public void setRequestContextPath(String path) {
	jsfThemeCtx.setRequestContextPath(path);
    }

    /**
     *	<p> This method delegates to <code>JSFThemeContext</code>.</p>
     */
    public String getResourcePath(String path) {
	return jsfThemeCtx.getResourcePath(path);
    }


    /**
     *	<p> This hold a reference to an instance of JSFThemeContext which
     *	    will help us implement the functionality of this class.</p>
     */
    private ThemeContext jsfThemeCtx = null;
    
    /**
     *	<p> These keys are used when getting the property values
     *	    provided in the custom theme plugin properties file.</p>
     */
    public static final String THEME_NAME_KEY = "com.sun.webui.theme.DEFAULT_THEME";
    public static final String THEME_VERSION_KEY = "com.sun.webui.theme.DEFAULT_THEME_VERSION";
}
