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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 *
 */
// Portions Copyright [2019-2021] [Payara Foundation and/or its affiliates]

package org.glassfish.admingui.theme;

import com.sun.webui.jsf.theme.JSFThemeContext;
import com.sun.webui.theme.ServletThemeContext;
import com.sun.webui.theme.ThemeContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import jakarta.faces.context.FacesContext;


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

    private static final Logger LOG = Logger.getLogger(AdminguiThemeContext.class.getName());

    /**
     * These keys are used when getting the property values
     * provided in the custom theme plugin properties file.
     */
    public static final String THEME_NAME_KEY = "com.sun.webui.theme.DEFAULT_THEME";
    public static final String THEME_VERSION_KEY = "com.sun.webui.theme.DEFAULT_THEME_VERSION";

    /**
     * This hold a reference to an instance of JSFThemeContext which
     * will help us implement the functionality of this class.
     */
    private ThemeContext jsfThemeCtx = null;


    /**
     * This constructor takes in the theme name and version to use by default.
     *
     * @param themeName The theme name (i.e. suntheme).
     * @param themeVersion The version number (i.e. 4.2).
     */
    protected AdminguiThemeContext(String themeName, String themeVersion) {
        super(setThemeParams(themeName, themeVersion));

        // The following ThemeContext is created to allow us to delegate our
        // logic to it.
        jsfThemeCtx = JSFThemeContext.getInstance(FacesContext.getCurrentInstance());
        jsfThemeCtx.setThemeServletContext("/theme");
    }


    /**
     * @return an instance of <code>ThemeContext</code> creating one
     * if necessary and persisting it in the <code>ApplicationMap</code>.
     */
    public synchronized static ThemeContext getInstance(FacesContext context, String themeName, String themeVersion) {
        LOG.finest(() -> String.format("getInstance(context=%s, themeName=%s, themeVersion=%s)", context, themeName,
            themeVersion));
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
     * @return an instance of <code>ThemeContext</code>
     * using properties provided via <code>Integration point</code>.
     */
    public synchronized static ThemeContext getInstance(FacesContext context, Properties propMap) {
        Map<String, Object> map = context.getExternalContext().getApplicationMap();
        String themeName = (String) propMap.get(THEME_NAME_KEY);
        String themeVersion = (String) propMap.get(THEME_VERSION_KEY);
        String themeKey = THEME_CONTEXT + themeName + themeVersion;
        LOG.finest(() -> "theme context key: " + themeKey);
        ThemeContext themeContext = (ThemeContext) map.get(themeKey);
        if (themeContext == null) {
            themeContext = new AdminguiThemeContext(themeName, themeVersion);
            map.put(themeKey, themeContext);
        }
        return themeContext;
    }


    /**
     * Creates a <code>Map</code> object with the theme name and
     * version.
     */
    private static Map<String, String> setThemeParams(final String theme, final String version) {
        LOG.finest(() -> String.format("setThemeParams(theme=%s, version=%s)", theme, version));
        final Map<String, String> map = new HashMap<>();
        final String themeForMap = theme == null ? "suntheme" : theme;
        map.put(ThemeContext.DEFAULT_THEME, themeForMap);
        final String versionForMap = version == null ? "4.2" : version;
        map.put(ThemeContext.DEFAULT_THEME_VERSION, versionForMap);
        return map;
    }


    /**
     * This method delegates to <code>JSFThemeContext</code>.
     */
    @Override
    public ClassLoader getDefaultClassLoader() {
        return jsfThemeCtx.getDefaultClassLoader();
    }


    /**
     * This method delegates to <code>JSFThemeContext</code>.
     */
    @Override
    public void setDefaultClassLoader(ClassLoader classLoader) {
        jsfThemeCtx.setDefaultClassLoader(classLoader);
    }


    /**
     * This method delegates to <code>JSFThemeContext</code>.
     */
    @Override
    public String getRequestContextPath() {
        return jsfThemeCtx.getRequestContextPath();
    }


    /**
     * This method delegates to <code>JSFThemeContext</code>.
     */
    @Override
    public void setRequestContextPath(String path) {
        jsfThemeCtx.setRequestContextPath(path);
    }


    /**
     * This method delegates to <code>JSFThemeContext</code>.
     */
    @Override
    public String getResourcePath(String path) {
        return jsfThemeCtx.getResourcePath(path);
    }
}
