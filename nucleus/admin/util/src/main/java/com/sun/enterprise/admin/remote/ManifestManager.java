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

package com.sun.enterprise.admin.remote;

import java.io.*;
import java.net.URLDecoder;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.universal.NameValue;
import com.sun.enterprise.universal.glassfish.AdminCommandResponse;

/**
 *
 * @author bnevins
 */
class ManifestManager implements ResponseManager {
    ManifestManager(InputStream inStream, Logger logger)
                                throws RemoteException, IOException  {
        this.logger = logger;
        response = new AdminCommandResponse(inStream);
    }

    public Map<String,String> getMainAtts() {
        return response.getMainAtts();
    }

    public void process() throws RemoteException {
        logger.finer("PROCESSING MANIFEST...");

        // remember these are "succeed-fast".  They will throw a
        // RemoteSuccessException if they succeed...
        processGeneratedManPage();
        processManPage();
        processGeneric();

        // No RemoteSuccessException was thrown -- this is now an Error!!
        throw new RemoteFailureException("Could not process");
    }


    private void processManPage() throws RemoteSuccessException {
        String manPage = response.getValue(AdminCommandResponse.MANPAGE);

        if(!ok(manPage))
            return;

        throw new RemoteSuccessException(manPage);
    }

    private void processGeneratedManPage() throws RemoteException {
        if(!response.isGeneratedHelp())
            return;
        GeneratedManPageManager mgr = new GeneratedManPageManager(response);
        mgr.process();
    }

    private void processGeneric() throws RemoteSuccessException, RemoteFailureException {
        StringBuilder sb = new StringBuilder();
        String msg = response.getMainMessage();
        if(ok(msg)) {
            sb.append(msg);
        }

        boolean useMainChildrenAttr = Boolean.valueOf(response.getMainAtts().get("use-main-children-attribute"));

        if (useMainChildrenAttr) {
            sb = processMainChildrenAttribute(response.getMainAtts(), sb);
        } else {
            processOneLevel("", null, response.getMainAtts(), sb);
        }

        if (response.wasFailure()) {
            final String cause = response.getCause();
            if(ok(cause)){
                if (logger.isLoggable(Level.FINER)) {
                    if (sb.length() > 0) sb.append(EOL);
                    sb.append(cause);
                }
                throw new RemoteFailureException(sb.toString(), cause);
            }
            throw new RemoteFailureException(sb.toString());
        }

        throw new RemoteSuccessException(sb.toString());
    }

    // this is just HORRIBLE -- but that's the way it is presented from the
    // server.  I imagine tons of bug reports on this coming up...
    private void processOneLevel(String prefix, String key,
            Map<String,String> atts, StringBuilder sb) {

        if(atts == null)
            return;

        // we probably should not show props to the user
        // processProps(prefix, atts, sb);
        processChildren(prefix, key, atts, sb);
    }

    private void processChildren(String prefix, String parent, Map<String, String> atts, StringBuilder sb) {

        Map<String,Map<String,String>> kids = response.getChildren(atts);

        if(kids == null || kids.isEmpty())
            return;

        String childrenType = atts.get(AdminCommandResponse.CHILDREN_TYPE);
        int index = (parent == null) ? 0 : parent.length() + 1;

        for(Map.Entry<String, Map<String,String>> entry : kids.entrySet()) {
            String container = entry.getKey();

            if (sb.length() > 0) sb.append(EOL);
            if(ok(childrenType)) {
                sb.append(prefix).append(childrenType).append(" : ");
            }
            try {
                sb.append(java.net.URLDecoder.decode(container.substring(index), "UTF-8"));
            } catch (Exception e) {
                sb.append(container.substring(index));
            }
            processOneLevel(prefix + TAB, container, entry.getValue(), sb);
        }
    }

   /* Issue 5918 Keep output sorted. Grab "children" from main attributes
    * which has the original order of output returned from server-side
    */
    private StringBuilder processMainChildrenAttribute(Map<String,String> atts, StringBuilder sb) {
        String allChildren = atts.get("children");
        if (ok(allChildren)) {
            String[] children = allChildren.split(";");
            for (String child : children) {
                if (sb.length() > 0) sb.append(EOL);
                sb.append(decode(child));
            }
        }
        return sb;
    }

    private boolean ok(String s) {
        return s != null && s.length() > 0 && !s.equals("null");
    }

    private String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        } catch (IllegalArgumentException e1) {
            return value;
        }
    }

    private Logger logger;
    private AdminCommandResponse response;
    private static final String EOL = StringUtils.NEWLINE;
    private static final String TAB = "    ";
}

