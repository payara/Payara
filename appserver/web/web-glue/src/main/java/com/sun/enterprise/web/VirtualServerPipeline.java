/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.web;

import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.core.StandardPipeline;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.String;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.grizzly.http.util.CharChunk;
import org.glassfish.web.LogFacade;

/**
 * Pipeline associated with a virtual server.
 *
 * This pipeline inherits the state (off/disabled) of its associated
 * virtual server, and will abort execution and return an appropriate response
 * error code if its associated virtual server is off or disabled.
 */
public class VirtualServerPipeline extends StandardPipeline {

    private static final Logger logger = LogFacade.getLogger();

    private static final ResourceBundle rb = logger.getResourceBundle();

    private VirtualServer vs;

    private boolean isOff;
    private boolean isDisabled;

    private ArrayList<RedirectParameters> redirects;

    private ConcurrentLinkedQueue<CharChunk> locations;

    /**
     * Constructor.
     *
     * @param vs Virtual server with which this VirtualServerPipeline is being
     * associated
     */
    public VirtualServerPipeline(VirtualServer vs) {
        super(vs);
        this.vs = vs;
        locations = new ConcurrentLinkedQueue<CharChunk>();
    }

    /**
     * Processes the specified request, and produces the appropriate
     * response, by invoking the first valve (if any) of this pipeline, or
     * the pipeline's basic valve.
     *
     * @param request The request to process
     * @param response The response to return
     */
    public void invoke(Request request, Response response)
            throws IOException, ServletException {

        if (isOff) {
            String msg = rb.getString(LogFacade.VS_VALVE_OFF);
            msg = MessageFormat.format(msg, new Object[] { vs.getName() });
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, msg);
            }
            ((HttpServletResponse) response.getResponse()).sendError(
                                            HttpServletResponse.SC_NOT_FOUND,
                                            msg);
        } else if (isDisabled) {
            String msg = rb.getString(LogFacade.VS_VALVE_DISABLED);
            msg = MessageFormat.format(msg, new Object[] { vs.getName() });
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, msg);
            }
            ((HttpServletResponse) response.getResponse()).sendError(
                                            HttpServletResponse.SC_FORBIDDEN,
                                            msg);
        } else {
            boolean redirect = false;
            if (redirects != null) {
                redirect = redirectIfNecessary(request, response);
            }
            if (!redirect) {
                super.invoke(request, response);
            }
        }
    }


    /**
     * Sets the <code>disabled</code> state of this VirtualServerPipeline.
     *
     * @param isDisabled true if the associated virtual server has been
     * disabled, false otherwise
     */
    void setIsDisabled(boolean isDisabled) {
        this.isDisabled = isDisabled;
    }


    /**
     * Sets the <code>off</code> state of this VirtualServerPipeline.
     *
     * @param isOff true if the associated virtual server is <code>off</code>,
     * false otherwise
     */
    void setIsOff(boolean isOff) {
        this.isOff = isOff;
    }


    /**
     * Adds the given redirect instruction to this VirtualServerPipeline.
     *
     * @param from URI prefix to match
     * @param url Redirect URL to return to the client
     * @param urlPrefix New URL prefix to return to the client
     * @param escape true if redirect URL returned to the client is to be
     * escaped, false otherwise
     */
    void addRedirect(String from, String url, String urlPrefix,
                     boolean escape) {

        if (redirects == null) {
            redirects = new ArrayList<RedirectParameters>();
        }

        redirects.add(new RedirectParameters(from, url, urlPrefix, escape));
    }


    /**
     * @return true if this VirtualServerPipeline has any redirects
     * configured, and false otherwise.
     */
    boolean hasRedirects() {
        return ((redirects != null) && (redirects.size() > 0));
    }


    /**
     * Clears all redirects.
     */
    void clearRedirects() {
        if (redirects != null) {
            redirects.clear();
        }
    }


    /**
     * Checks to see if the given request needs to be redirected.
     *
     * @param request The request to process
     * @param response The response to return
     *
     * @return true if redirect has occurred, false otherwise
     */
    private boolean redirectIfNecessary(Request request, Response response)
            throws IOException {

        if (redirects == null) {
            return false;
        }
   
        HttpServletRequest hreq = (HttpServletRequest) request.getRequest();
        HttpServletResponse hres = (HttpServletResponse) request.getResponse();
        String requestURI = hreq.getRequestURI();
        RedirectParameters redirectMatch = null;

        // Determine the longest 'from' URI prefix match
        int size = redirects.size();
        for (int i=0; i<size; i++) {
            RedirectParameters elem = redirects.get(i);
            String elemFromWithTrailingSlash = elem.from;
            if (!elemFromWithTrailingSlash.endsWith("/")) {
                elemFromWithTrailingSlash += "/";
            }
            if (requestURI.equals(elem.from) ||
                    requestURI.startsWith(elemFromWithTrailingSlash)) {
                if (redirectMatch != null) {
                    if (elem.from.length() > redirectMatch.from.length()) {
                        redirectMatch = elem;
                    }
                } else {
                    redirectMatch = elem;
                }
            }
        }

        if (redirectMatch != null) {
            // Redirect prefix match found, need to redirect
            String location = null;
            String uriSuffix = requestURI.substring(
                            redirectMatch.from.length());
            if ("/".equals(redirectMatch.from)) {
                uriSuffix = "/" + uriSuffix;
                // START 6810361
                if (redirectMatch.urlPrefixPath != null &&
                        uriSuffix.startsWith(redirectMatch.urlPrefixPath)) {
                    return false;
                }
                // END 6810361
            }
            // START 6810361
            // Implements welcome page only redirection
            if ("".equals(redirectMatch.from)) {
                if (!("/".equals(requestURI))) return false;
            }
            // END 6810361
            if (redirectMatch.urlPrefix != null) {
                // Replace 'from' URI prefix with URL prefix
                location = redirectMatch.urlPrefix + uriSuffix;
            } else {
                // Replace 'from' URI prefix with complete URL
                location = redirectMatch.url;
            }
  
            String queryString = hreq.getQueryString();
            if (queryString != null) {
                location += "?" + queryString;
            }
     
            CharChunk locationCC = null;

            if (redirectMatch.isEscape) {
                try {
                    URL url = new URL(location);
                    locationCC = locations.poll();
                    if (locationCC == null) {
                        locationCC = new CharChunk();
                    }
                    locationCC.append(url.getProtocol());
                    locationCC.append("://");
                    locationCC.append(url.getHost());
                    if (url.getPort() != -1) {
                        locationCC.append(":");
                        locationCC.append(String.valueOf(url.getPort()));
                    }
                    locationCC.append(response.encode(url.getPath()));
                    if (queryString != null) {
                        locationCC.append("?");
                        locationCC.append(url.getQuery());
                    }
                    location = locationCC.toString();
                } catch (MalformedURLException mue) {
                    if (redirectMatch.validURI) {
                        logger.log(Level.WARNING,
                            LogFacade.INVALID_REDIRECTION_LOCATION,
                            location);
                    } else {
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE,
                                LogFacade.INVALID_REDIRECTION_LOCATION,
                                location);
                        }
                    }
                } finally {
                    if (locationCC != null) {
                        locationCC.recycle();
                        locations.offer(locationCC);
                    }
                }
            }

            hres.sendRedirect(location);
            return true;
        }

        return false;
    }


    /**
     * Class representing redirect parameters
     */
    static class RedirectParameters {

        private String from;

        private String url;

        private String urlPrefix;

        // START 6810361
        /*
         * The path portion of the urlPrefix, in case urlPrefix is
         * specified as an absolute URL (including protocol etc.)
         */
        private String urlPrefixPath;
        // END 6810361

        private boolean validURI;

        private boolean isEscape;

        RedirectParameters(String from, String url, String urlPrefix,
                           boolean isEscape) {
            this.from = from;
            this.url = url;
            this.urlPrefix = urlPrefix;
            this.isEscape = isEscape;
            this.validURI = true;

            // START 6810361
            try {
                URL u = new URL(urlPrefix);
                urlPrefixPath = u.getPath();
            } catch (MalformedURLException e) {
                urlPrefixPath = urlPrefix;
                this.validURI = false;
            }
            // END 6810361
        }
    }

}
