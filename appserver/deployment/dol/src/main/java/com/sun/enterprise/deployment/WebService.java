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

package com.sun.enterprise.deployment;

import org.glassfish.deployment.common.Descriptor;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;


/**
 * Information about a single webservice-description in webservices.xml
 *
 * @author Kenneth Saks
 * @author Jerome Dochez
 */

public class WebService extends Descriptor {

    private String wsdlFileUri;

    /**
     * Derived, non-peristent location of wsdl file.
     * Only used at deployment/runtime.
     */
    private URL wsdlFileUrl;

    private String mappingFileUri;

    /**
     * Derived, non-peristent location of mapping file.
     * Only used at deployment/runtime.
     */
    private File mappingFile;

    private HashMap<String, WebServiceEndpoint> endpoints;

    // The set of web services to which this service belongs.
    private WebServicesDescriptor webServicesDesc;

    //
    // Runtime info
    //

    // Optional file URL to which final wsdl should be published.
    // This represents a directory on the file system from which deployment
    // is initiated.  URL schemes other than file: are legal but ignored.
    private URL publishUrl;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    /** type JAX-WS or JAX-RPC */
    private String type;

    private Boolean isJaxWSBased = null;

    /**
     * Default constructor. 
     */
    public WebService() {
        this("");
    }

    /**
     * If this returns non-null value, then it is verified that all the endpoints are of the same type.
     * @return
     */
    public Boolean isJaxWSBased() {
        return isJaxWSBased;
    }

    /**
     * This is called after verifying that all the endpoints are of the same type, either JAX-WS or JAX-RPC 
     * @param isJaxWSBased
     */
    public void setJaxWSBased(boolean isJaxWSBased) {
        this.isJaxWSBased = isJaxWSBased;
    }

    /**
     * copy constructor. 
     */
    public WebService(WebService other) {
        super(other);
        wsdlFileUri = other.wsdlFileUri; // String
        wsdlFileUrl = other.wsdlFileUrl;
        mappingFileUri = other.mappingFileUri; // String
        mappingFile = other.mappingFile;
        publishUrl = other.publishUrl;
        webServicesDesc = other.webServicesDesc; // copy as-is
        type = other.type;
        if (other.endpoints != null) {
            endpoints = new HashMap<String, WebServiceEndpoint>();
            for (WebServiceEndpoint wsep : other.endpoints.values()) {
                wsep.setWebService(this);
                endpoints.put(wsep.getEndpointName(), wsep);
            }
        } else {
            endpoints = null;
        }
    }

    public WebService(String name) {
        setName(name);
        endpoints = new HashMap();
    }

    public void setWebServicesDescriptor(WebServicesDescriptor webServices) {
        webServicesDesc = webServices;
    }

    public WebServicesDescriptor getWebServicesDescriptor() {
        return webServicesDesc;
    }

    public BundleDescriptor getBundleDescriptor() {
        return webServicesDesc.getBundleDescriptor();
    }

    public boolean hasWsdlFile() {
        return (wsdlFileUri != null);
    }

    public void setWsdlFileUri(String uri) {
        wsdlFileUri = uri;
    }

    public String getWsdlFileUri() {
        return wsdlFileUri;
    }

    public URL getWsdlFileUrl() {
        return wsdlFileUrl;
    }

    public void setWsdlFileUrl(URL url) {
        wsdlFileUrl = url;
    }

    public String getGeneratedWsdlFilePath() {
        if (hasWsdlFile()) {
            String xmlDir = getBundleDescriptor().getApplication().getGeneratedXMLDirectory();
            if(!getBundleDescriptor().getModuleDescriptor().isStandalone()) {
                String uri = getBundleDescriptor().getModuleDescriptor().getArchiveUri();
                xmlDir = xmlDir + File.separator + uri.replaceAll("\\.", "_");
            }
            if(xmlDir == null) {
                return null;
            }
            return  xmlDir + File.separator + wsdlFileUri;
        } else {
            return getWsdlFileUrl().getPath();
        }
    }

    public boolean hasMappingFile() {
        return (mappingFileUri != null);
    }

    public void setMappingFileUri(String uri) {
        mappingFileUri = uri;
    }

    public String getMappingFileUri() {
        return mappingFileUri;
    }

    public File getMappingFile() {
        return mappingFile;
    }

    public void setMappingFile(File file) {
        mappingFile = file;
    }

    public void addEndpoint(WebServiceEndpoint endpoint) {
        endpoint.setWebService(this);
        endpoints.put(endpoint.getEndpointName(), endpoint);
    }

    public void removeEndpointByName(String endpointName) {
        WebServiceEndpoint endpoint = (WebServiceEndpoint)
                endpoints.remove(endpointName);
        endpoint.setWebService(null);
    }

    public WebServiceEndpoint getEndpointByName(String name) {
        return endpoints.get(name);

    }

    public void removeEndpoint(WebServiceEndpoint endpoint) {
        removeEndpointByName(endpoint.getEndpointName());
    }

    public Collection<WebServiceEndpoint> getEndpoints() {
        HashMap shallowCopy = new HashMap(endpoints);
        return shallowCopy.values();
    }

    public boolean hasClientPublishUrl() {
        return (publishUrl != null);
    }

    public void setClientPublishUrl(URL url) {
        publishUrl = url;
    }

    public URL getClientPublishUrl() {
        return publishUrl;
    }

    public boolean hasUrlPublishing() {
        return (!hasFilePublishing());
    }

    public boolean hasFilePublishing() {
        return (hasClientPublishUrl() &&
                publishUrl.getProtocol().equals("file"));
    }

    /**
     * Select one of this webservice's endpoints to use for converting 
     * relative imports.  
     */
    public WebServiceEndpoint pickEndpointForRelativeImports() {
        WebServiceEndpoint pick = null;

        // First secure endpoint takes precedence.  
        for(WebServiceEndpoint wse : endpoints.values()) {
            if( wse.isSecure() ) {
                pick = wse;
                break;
            }
            pick = wse;
        }
        return pick;
    }

    /**
     * Returns a formatted String of the attributes of this object.
     */
    public void print(StringBuffer toStringBuffer) {
        super.print(toStringBuffer);
        toStringBuffer.append( "\n wsdl file : ").append( wsdlFileUri);
        toStringBuffer.append( "\n mapping file ").append(mappingFileUri);
        toStringBuffer.append( "\n publish url ").append(publishUrl);
        toStringBuffer.append( "\n final wsdl ").append(wsdlFileUrl);
        toStringBuffer.append( "\n endpoints ").append(endpoints);
    }

}    
