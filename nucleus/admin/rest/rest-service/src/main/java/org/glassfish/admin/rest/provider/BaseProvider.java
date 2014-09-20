/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.glassfish.admin.rest.Constants;
import static org.glassfish.admin.rest.provider.ProviderUtil.*;
import org.glassfish.admin.rest.utils.ConfigModelComparator;
import org.glassfish.admin.rest.utils.DomConfigurator;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.restconnector.RestConfig;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.Dom;

/**
 * @author Jason Lee
 */
@Provider
public abstract class BaseProvider<T> implements MessageBodyWriter<T> {
    public static final String HEADER_DEBUG = "__debug";

    public static final String JSONP_CALLBACK = "jsoncallback";

    @Context
    protected javax.inject.Provider<UriInfo> uriInfo;

    @Context
    protected javax.inject.Provider<HttpHeaders> requestHeaders;

    @Context
    protected ServiceLocator habitat;

    protected Class desiredType;

    protected MediaType[] supportedMediaTypes;

    public BaseProvider(Class desiredType, MediaType ... mediaType) {
        this.desiredType = desiredType;
        if (mediaType == null) {
            mediaType = new MediaType[0];
        }
        this.supportedMediaTypes = mediaType;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] antns, MediaType mt) {
        if (isGivenTypeWritable(type, genericType)) {
            for (MediaType supportedMediaType : supportedMediaTypes) {
                if (mt.isCompatible(supportedMediaType)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Overwrite this if you need different test of type compatibility.
     * Used from isWritable method.
     */
    protected boolean isGivenTypeWritable(Class<?> type, Type genericType) {
        return desiredType.isAssignableFrom(type);
    }

    @Override
    public long getSize(T t, Class<?> type, Type type1, Annotation[] antns, MediaType mt) {
        return -1;
    }

    @Override
    public void writeTo(T proxy, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        entityStream.write(getContent(proxy).getBytes(Constants.ENCODING));
    }

    public abstract String getContent(T proxy);

    protected int getFormattingIndentLevel() {
        RestConfig rg = ResourceUtil.getRestConfig(habitat);
        if (rg == null){
            return -1;
        }
        else {
            return Integer.parseInt(rg.getIndentLevel());
        }

    }

     /**
      * returns true if the HTML viewer displays the hidden CLI command links
      */
    protected boolean canShowHiddenCommands() {

        RestConfig rg = ResourceUtil.getRestConfig(habitat);
        if ((rg != null) && (rg.getShowHiddenCommands().equalsIgnoreCase("true"))) {
            return true;
        }
        return false;
    }

    /**
     * returns true if the HTML viewer displays the deprecated elements or attributes
     * of a config bean
     */

    protected boolean canShowDeprecatedItems() {

        RestConfig rg = ResourceUtil.getRestConfig(habitat);
        if ((rg != null) && (rg.getShowDeprecatedItems().equalsIgnoreCase("true"))) {
            return true;
        }
        return false;
    }
    /**
     * check for the __debug request header
     *
     */
    protected boolean isDebug() {

        RestConfig rg = ResourceUtil.getRestConfig(habitat);
        if ((rg != null) && (rg.getDebug().equalsIgnoreCase("true"))) {
            return true;
        }

        if (requestHeaders == null) {
            return true;
        }
        List header = requestHeaders.get().getRequestHeader(HEADER_DEBUG);
        return (header != null) && ("true".equals(header.get(0)));
    }

    /**
     * if a query param of name "jsoncallback" is there, returns its value
     * or returns null otherwise.
     */
    protected String getCallBackJSONP() {
        if (uriInfo == null) {
            return null;
        }

        MultivaluedMap<String, String> l = uriInfo.get().getQueryParameters();

        if (l == null) {
            return null;
        }
        return l.getFirst(JSONP_CALLBACK);
    }

    protected String getXmlCommandLinks(String[][] commandResourcesPaths, String indent) {
        StringBuilder result = new StringBuilder();
        for (String[] commandResourcePath : commandResourcesPaths) {
            result.append("\n").append(indent).append(getStartXmlElement(KEY_COMMAND)).append(getElementLink(uriInfo.get(), commandResourcePath[0])).append(getEndXmlElement(KEY_COMMAND));
        }
        return result.toString();
    }

    protected Map<String, String> getResourceLinks(Dom dom) {
        Map<String, String> links = new TreeMap<String, String>();
        Set<String> elementNames = dom.model.getElementNames();

        for (String elementName : elementNames) { //for each element
            if (elementName.equals("*")) {
                ConfigModel.Node node = (ConfigModel.Node) dom.model.getElement(elementName);
                ConfigModel childModel = node.getModel();
                List<ConfigModel> lcm = ResourceUtil.getRealChildConfigModels(childModel, dom.document);
                Collections.sort(lcm, new ConfigModelComparator());

                if (lcm != null) {
                    Collections.sort(lcm, new ConfigModelComparator());
                    for (ConfigModel cmodel : lcm) {
                        links.put(cmodel.getTagName(), ProviderUtil.getElementLink(uriInfo.get(), cmodel.getTagName()));
                    }
                }
            } else {
                links.put(elementName, ProviderUtil.getElementLink(uriInfo.get(), elementName));
            }
        }

        return links;
    }

    protected Map<String, String> getResourceLinks(List<Dom> proxyList) {
        Map<String, String> links = new TreeMap<String, String>();
        Collections.sort(proxyList, new DomConfigurator());
        for (Dom proxy : proxyList) { //for each element
            try {
                links.put(proxy.getKey(), getElementLink(uriInfo.get(), proxy.getKey()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return links;
    }
}
