/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.results.GetResultList;
import org.jvnet.hk2.config.Dom;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import java.util.List;
import java.util.Map;

import static org.glassfish.admin.rest.provider.ProviderUtil.*;

/**
 *
 * @author Rajeshwar Patil
 * @author Ludovic Champenois ludo@dev.java.net
 */
@Provider
@Produces(MediaType.APPLICATION_XML)
public class GetResultListXmlProvider extends BaseProvider<GetResultList> {

    public GetResultListXmlProvider() {
        super(GetResultList.class, MediaType.APPLICATION_XML_TYPE);
    }

    @Override
    public String getContent(GetResultList proxy) {
        StringBuilder result = new StringBuilder();
        String indent = Constants.INDENT;

         result.append(getStartXmlElement(KEY_ENTITY))
                 .append("\n\n")
                 .append(indent)
                 .append(getStartXmlElement(KEY_METHODS))
                 .append(getXmlForMethodMetaData(proxy.getMetaData(), indent + Constants.INDENT))
                 .append("\n")
                 .append(indent)
                 .append(getEndXmlElement(KEY_METHODS));

        //do not display empty child resources array
        if (proxy.getDomList().size() > 0) {
            result.append("\n\n")
                    .append(indent)
                    .append(getStartXmlElement(KEY_CHILD_RESOURCES))
                    .append(getResourcesLinks(proxy.getDomList(), indent + Constants.INDENT))
                    .append("\n")
                    .append(indent)
                    .append(getEndXmlElement(KEY_CHILD_RESOURCES));
        }
        if (proxy.getCommandResourcesPaths().length > 0) {
            result.append("\n\n")
                    .append(indent)
                    .append(getStartXmlElement(KEY_COMMANDS))
                    .append(getXmlCommandLinks(proxy.getCommandResourcesPaths(), indent + Constants.INDENT))
                    .append("\n")
                    .append(indent)
                    .append(getEndXmlElement(KEY_COMMANDS));
        }

        result.append("\n\n")
                .append(getEndXmlElement(KEY_ENTITY));
        return result.toString();
    }

    protected String getXmlResourcesLinks(List<Dom> proxyList, String[][] commandResourcesPaths, String indent) {
        return null;
    }

    private String getResourcesLinks(List<Dom> proxyList, String indent) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, String> link : getResourceLinks(proxyList).entrySet()) {
            try {
                    result.append("\n")
                            .append(indent)
                            .append(getStartXmlElement(KEY_CHILD_RESOURCE))
                            .append(link.getValue())
                            .append(getEndXmlElement(KEY_CHILD_RESOURCE));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return result.toString();
    }
}
