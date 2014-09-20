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

package com.sun.enterprise.admin.servermgmt.services;

import com.sun.enterprise.universal.PropertiesDecoder;
import com.sun.enterprise.util.io.ServerDirs;
import java.io.*;
import java.util.Map;

/**
 * The original implementation of Services had serious design problems.  The Service interface
 * is ENORMOUSLY fat and non OO in the sense that outside callers had to set things
 * to make things work.  The interface is not generic -- it is very SMF specific
 * It is extremely difficult to implement the interface because it has SO MANY methods
 * that non-SMF don't need.
 * This "Adapter" makes it easier to implement the interface.  Eventually we should
 * have one adapter for all services but the SMF code is difficult and time-consuming
 * to change.
 * Meantime I'm adding new functionality (August 2010, bnevins) for instances.  I'm
 * moving implementations of the new interface methods to "ServiceAdapter" which ALL
 * services extend.
 *
 * @author bnevins
 */
public abstract class NonSMFServiceAdapter extends ServiceAdapter {

    NonSMFServiceAdapter(ServerDirs dirs, AppserverServiceType type) {
        super(dirs, type);
    }

    @Override
    public final int getTimeoutSeconds() {
        throw new UnsupportedOperationException("getTimeoutSeconds() is not supported on this platform");
    }

    @Override
    public final void setTimeoutSeconds(int number) {
        throw new UnsupportedOperationException("setTimeoutSeconds() is not supported on this platform");
    }

    @Override
    public final String getServiceProperties() {
        return flattenedServicePropertes;
    }

    /*
     * @author Byron Nevins
     * 11/14/11
     * The --serviceproperties option was being completely ignored!
     * The existing structure is brittle, hard to understand, and has wired-in
     * the implementation details to the interface.  I.e. there are tons of problems
     * maintaining the code.
     * What I'm doing here is taking the map with all of the built-in values and
     * overlaying it with name-value pairs that the user specified.
     * I discovered the original problem by trying to change the display name, "ENTITY_NAME"
     * at the command line as a serviceproperty.  It was completely ignored!!
     */
    final Map<String, String> getFinalTokenMap() {
        Map<String, String> map = getTokenMap();
        map.putAll(tokensAndValues());
        return map;
    }

    @Override
    public final void setServiceProperties(String cds) {
        flattenedServicePropertes = cds;
    }

    @Override
    public final Map<String, String> tokensAndValues() {
        return PropertiesDecoder.unflatten(flattenedServicePropertes);
    }

    @Override
    public final String getManifestFilePath() {
        throw new UnsupportedOperationException("getManifestFilePath() is not supported in this platform.");
    }

    @Override
    public final String getManifestFileTemplatePath() {
        throw new UnsupportedOperationException("getManifestFileTemplatePath() is not supported in this platform.");
    }

    @Override
    public final boolean isConfigValid() {
        // SMF-only
        return true;
    }

    //////////////////////////////////////////////////////////////////////
    //////////////  pkg-private //////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////

    File getTemplateFile() {
        return templateFile;
    }

    void setTemplateFile(String name) {
        templateFile = new File(info.libDir, "install/templates/" + name);
    }
    private String flattenedServicePropertes;
    private File templateFile;
}
