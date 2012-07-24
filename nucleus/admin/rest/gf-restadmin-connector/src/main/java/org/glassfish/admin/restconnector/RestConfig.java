/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.restconnector;

import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;

import org.jvnet.hk2.config.Configured;

/**
 * RestConfig configuration.  This defines a rest-config element.
 *
 * @author Ludovic Champenois
 *
 */
@Configured
public interface RestConfig extends ConfigExtension {

    public static String  DEBUG = "debug";
    public static String  INDENTLEVEL ="indentLevel";
    public static String  SHOWHIDDENCOMMANDS ="showHiddenCommands";
    public static String  WADLGENERATION ="wadlGeneration";
    public static String  LOGRESPONSES ="logOutput";
    public static String  LOGINPUTS ="logInput";
    public static String  SHOWDEPRECATEDITEMS = "showDeprecatedItems";
    public static String  SESSIONTOKENTIMEOUT = "sessionTokenTimeout";

    @Attribute(defaultValue = "false", dataType = Boolean.class)
    public String getDebug();

    public void setDebug(String debugFlag);

    @Attribute(defaultValue = "-1", dataType = Integer.class)
    public String getIndentLevel();

    public void setIndentLevel(String indentLevel);

    @Attribute(defaultValue = "false", dataType = Boolean.class)
    public String getWadlGeneration();

    public void setWadlGeneration(String wadlGeneration);

    @Attribute(defaultValue = "false", dataType = Boolean.class)
    public String getShowHiddenCommands();

    public void setShowHiddenCommands(String showHiddenCommands);

    @Attribute(defaultValue = "false", dataType = Boolean.class)
    public String getLogOutput();

    public void setLogOutput(String logOutput);

    @Attribute(defaultValue = "false", dataType = Boolean.class)
    public String getLogInput();

    public void setLogInput(String logInput);

    @Attribute(defaultValue = "false", dataType = Boolean.class)
    public String getShowDeprecatedItems();

    public void setShowDeprecatedItems(String showDeprecatedItems);

    @Attribute(defaultValue = "30", dataType = Integer.class)
    public String getSessionTokenTimeout();

    public void setSessionTokenTimeout(String timeout);
}
