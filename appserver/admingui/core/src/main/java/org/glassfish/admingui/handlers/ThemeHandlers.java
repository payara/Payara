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

package org.glassfish.admingui.handlers;


import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import com.sun.webui.theme.ThemeContext;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.faces.context.FacesContext;
import org.glassfish.admingui.common.handlers.PluginHandlers;
import org.glassfish.admingui.common.plugin.ConsoleClassLoader;
import org.glassfish.admingui.connector.IntegrationPoint;
import org.glassfish.admingui.theme.AdminguiThemeContext;

/**
 *
 * @author anilam
 */
public class ThemeHandlers {

        /**
     *	<p> This method initializes the theme using the given
     *	    <code>themeName</code> and <code>themeVersion</code>.  If these
     *	    values are not supplied, "suntheme" and "4.2" will be used
     *	    respectively.  This method should be invoked before the theme is
     *	    accessed (for example on the initPage or beforeCreate of the login
     *	    page).</p>
     *
     */
    @Handler(id = "getTheme", input = {
        @HandlerInput(name = "themeName", type = String.class),
        @HandlerInput(name = "themeVersion", type = String.class)
        },
        output = {
            @HandlerOutput(name = "themeContext", type = ThemeContext.class)
        })
    public static void getTheme(HandlerContext handlerCtx) {
        String themeName = (String) handlerCtx.getInputValue("themeName");
        String themeVersion = (String) handlerCtx.getInputValue("themeVersion");
        ThemeContext themeContext = AdminguiThemeContext.getInstance(
                handlerCtx.getFacesContext(), themeName, themeVersion);
        handlerCtx.setOutputValue("themeContext", themeContext);
    }

    /**
     *	<p> This method gets the <code>themeName</code> and <code>themeVersion</code>
     *	    via <code>Integration Point</code>.  If more than one is provided
     *	    the one with the lowest <code>priority</code> number will be used.
     *	    This method should be invoked before the theme is
     *	    accessed (for example on the initPage or beforeCreate of the login page).</p>
     */
    @Handler(id = "getThemeFromIntegrationPoints", output = {
        @HandlerOutput(name = "themeContext", type = ThemeContext.class)
    })
    public static void getThemeFromIntegrationPoints(HandlerContext handlerCtx) {
        FacesContext ctx = handlerCtx.getFacesContext();
        String type = "org.glassfish.admingui:customtheme";
        List<IntegrationPoint> ipList = PluginHandlers.getIntegrationPoints(ctx, type);
        if (ipList != null) {
            //if more than one integration point is provided then we
            //need to find the lowest priority number
            int lowest = getLowestPriorityNum(ipList);
            for (IntegrationPoint ip : ipList) {
                int priority = ip.getPriority();
                if (priority == lowest) {
                    String content = ip.getContent();
                    if (content == null || content.equals("")) {
                        throw new IllegalArgumentException("No Properties File Name Provided!");
                    }
                    ClassLoader pluginCL = ConsoleClassLoader.findModuleClassLoader(ip.getConsoleConfigId());
                    URL propertyFileURL = pluginCL.getResource("/" + content);
                    try {
                        Properties propertyMap = new Properties();
                        propertyMap.load(propertyFileURL.openStream());
                        ThemeContext themeContext =
			    AdminguiThemeContext.getInstance(ctx, propertyMap);
			themeContext.setDefaultClassLoader(pluginCL);
                        handlerCtx.setOutputValue("themeContext", themeContext);
                    } catch (Exception ex) {
                        throw new RuntimeException(
                                "Unable to access properties file '" + content + "'!", ex);
                    }
                }
            }
        }

    }

    private static int getLowestPriorityNum(List ipList) {
        Iterator iter = ipList.iterator();
            //assuming priority values can only be 1 to 100
            int lowest = 101;
            while (iter.hasNext()) {
                IntegrationPoint iP = (IntegrationPoint) iter.next();
                if (iP.getPriority() < lowest) {
                    lowest = iP.getPriority();
                }
            }

        return lowest;
    }

}
