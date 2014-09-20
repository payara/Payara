/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.provider;

import org.glassfish.admin.rest.results.GetResultList;
import org.glassfish.admin.rest.utils.DomConfigurator;
import org.jvnet.hk2.config.Dom;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.glassfish.admin.rest.utils.Util.*;
import static org.glassfish.admin.rest.provider.ProviderUtil.*;

/**
 * @author Rajeshwar Patil
 * @author Ludovic Champenois ludo@dev.java.net
 */
@Provider
@Produces(MediaType.TEXT_HTML)
public class GetResultListHtmlProvider extends BaseProvider<GetResultList> {

    public GetResultListHtmlProvider() {
        super(GetResultList.class, MediaType.TEXT_HTML_TYPE);
    }

    @Override
    public String getContent(GetResultList proxy) {
        String result = getHtmlHeader(uriInfo.get().getBaseUri().toASCIIString());
        final String typeKey = upperCaseFirstLetter((decode(getName(uriInfo.get().getPath(), '/'))));
        result = result + "<h1>" + typeKey + "</h1>";

        String postCommand = getHtmlRespresentationsForCommand(proxy.getMetaData().getMethodMetaData("POST"), "POST", "Create", uriInfo.get());
        result = getHtmlForComponent(postCommand, "Create " + typeKey, result);

        String childResourceLinks = getResourcesLinks(proxy.getDomList());
        result = getHtmlForComponent(childResourceLinks, "Child Resources", result);

        String commandLinks = getCommandLinks(proxy.getCommandResourcesPaths());
        result = getHtmlForComponent(commandLinks, "Commands", result);

        result = result + "</html></body>";
        return result;
    }

    private String getResourcesLinks(List<Dom> proxyList) {
        StringBuilder result = new StringBuilder("<div>");
        Collections.sort(proxyList, new DomConfigurator());
        for (Map.Entry<String, String> link : getResourceLinks(proxyList).entrySet()) {
            result.append("<a href=\"")
                    .append(link.getValue())
                    .append("\">")
                    .append(link.getKey())
                    .append("</a><br>");
        }

        result.append("</div><br/>");
        return result.toString();
    }

    private String getCommandLinks(String[][] commandResourcesPaths) {
        StringBuilder result = new StringBuilder("<div>");
        for (String[] commandResourcePath : commandResourcesPaths) {
            try {
                result.append("<a href=\"")
                        .append(getElementLink(uriInfo.get(), commandResourcePath[0]))
                        .append("\">")
                        .append(commandResourcePath[0])
                        .append("</a><br/>");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        result.append("</div><br/>");
        return result.toString();
    }
}
