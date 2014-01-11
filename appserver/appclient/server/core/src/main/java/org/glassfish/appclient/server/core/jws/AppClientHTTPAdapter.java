/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.appclient.server.core.jws;

import org.glassfish.orb.admin.config.IiopListener;
import org.glassfish.orb.admin.config.IiopService;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.glassfish.appclient.server.core.jws.servedcontent.ACCConfigContent;
import org.glassfish.appclient.server.core.jws.servedcontent.DynamicContent;
import org.glassfish.appclient.server.core.jws.servedcontent.StaticContent;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.enterprise.iiop.api.GlassFishORBFactory;
import org.glassfish.grizzly.http.server.Session;

/**
 * GrizzlyAdapter for serving static and dynamic content.
 *
 * @author tjquinn
 */
public class AppClientHTTPAdapter extends RestrictedContentAdapter {

    public final static String GF_JWS_SESSION_CACHED_JNLP_NAME = "org.glassfish.jws.mainJNLP";
    public final static String GF_JWS_SESSION_IS_MAIN_PROCESSED_NAME = "org.glassfish.jws.isMainProcessed";
        
    private final static String IF_UNMODIFIED_SINCE = "If-Unmodified-Since";

    private static final String ARG_QUERY_PARAM_NAME = "arg";
    private static final String PROP_QUERY_PARAM_NAME = "prop";
    private static final String VMARG_QUERY_PARAM_NAME = "vmarg";
    private static final String ACC_ARG_QUERY_PARAM_NAME = "accarg";
    private static final String JWS_ARG_QUERY_PARAM_NAME = "jwsaccarg";

    private static final String DEFAULT_ORB_LISTENER_ID = "orb-listener-1";

    private final static String LINE_SEP = System.getProperty("line.separator");
    private static final String NEW_LINE = "\r\n";

    private final Map<String,DynamicContent> dynamicContent;
    private final Properties tokens;

    private final IiopService iiopService;
    private final GlassFishORBFactory orbFactory;
    private final ACCConfigContent accConfigContent;
    private final LoaderConfigContent loaderConfigContent;

    /**
     * Prepares a full URI from the request.
     * 
     * @param gReq the request
     * @return URI for the request
     * @throws URISyntaxException 
     */
    public static URI requestURI(final Request gReq) throws URISyntaxException {
        return new URI(gReq.getScheme(), 
                null /* userInfo */, 
                gReq.getLocalName(), 
                gReq.getLocalPort(), 
                gReq.getPathInfo(), 
                gReq.getQueryString(), 
                null /* fragment */);
    }
    
    public AppClientHTTPAdapter(
            final String contextRoot,
            final Properties tokens,
            final File domainDir,
            final File installDir,
            final IiopService iiopService,
            final GlassFishORBFactory orbFactory) throws IOException {
        this(contextRoot,
                new HashMap<String,StaticContent>(),
                new HashMap<String,DynamicContent>(),
                tokens,
                domainDir,
                installDir,
                iiopService,
                orbFactory);
    }
    
    public AppClientHTTPAdapter(
            final String contextRoot,
            final Map<String,StaticContent> staticContent,
            final Map<String,DynamicContent> dynamicContent,
            final Properties tokens,
            final File domainDir,
            final File installDir,
            final IiopService iiopService,
            final GlassFishORBFactory orbFactory) throws IOException {
        super(contextRoot, staticContent);
        this.dynamicContent = dynamicContent;
        this.tokens = tokens;
        this.iiopService = iiopService;
        this.orbFactory = orbFactory;
        this.accConfigContent = new ACCConfigContent(
                new File(domainDir, "config"),
                new File(new File(installDir, "lib"), "appclient"));
        this.loaderConfigContent = new LoaderConfigContent(installDir);

        if (logger.isLoggable(Level.FINE)) {
            logger.fine(dumpContent(this.dynamicContent));
        }
    }

    /**
     * Responds to all requests routed to the context root with which this
     * adapter was registered to the RequestDispatcher.
     *
     * @param gReq
     * @param gResp
     */
    @Override
    public void service(Request gReq, Response gResp) {
        if (logger.isLoggable(Level.FINER)) {
            dumpHeaders(gReq);
        }
        final String savedRequestURI = gReq.getRequestURI();
        Session s = gReq.getSession(false);
        logger.log(Level.FINE, "Req " + savedRequestURI + ", session was " + (s == null ? "NONE" : s.getIdInternal() + ":" + s.getSessionTimeout()));
        final String relativeURIString =
                relativizeURIString(contextRoot(), savedRequestURI);
        if (relativeURIString == null) {
            respondNotFound(gResp);
        } else if (dynamicContent.containsKey(relativeURIString)) {
            try {
                processDynamicContent(tokens, relativeURIString, gReq, gResp);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            } finally {
                if (logger.isLoggable(Level.FINER)) {
                    dumpHeaders(gResp, savedRequestURI);
                }
            }
        } else try {
            if (!serviceContent(gReq, gResp)) {
                respondNotFound(gResp);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (logger.isLoggable(Level.FINER)) {
                dumpHeaders(gResp, savedRequestURI);
            }
        }
    }

    private void dumpHeaders(final Response gResp, final String savedRequestURI) {
        if (logger.isLoggable(Level.FINER)) {
            final StringBuilder sb = new StringBuilder();
            sb.append("JWS response for URI=").append(savedRequestURI).append(", status=").append(gResp.getStatus()).append(LINE_SEP);
            for (String headerName : gResp.getHeaderNames()) {
                final String header = gResp.getHeader(headerName);
                sb.append("  ").append(headerName).append("=").append(header).append(LINE_SEP);
            }
            logger.log(Level.FINER, sb.toString());
        }
    }
    
    private void dumpHeaders(final Request gReq) {
        final StringBuilder sb = new StringBuilder();
        sb.append("JWS request: method=").append(gReq.getMethod().toString()).append(", URI=").append(gReq.getRequestURI()).append(LINE_SEP);
        for (String headerName : gReq.getHeaderNames()) {
            final String header = gReq.getHeader(headerName);
            sb.append("  ").append(headerName).append("=").append(header).append(LINE_SEP);
        }
        logger.log(Level.FINER, sb.toString());
    }
    
    public void addContentIfAbsent(final Map<String,StaticContent> staticAdditions,
            final Map<String,DynamicContent> dynamicAdditions) throws IOException {
        addContentIfAbsent(staticAdditions);
        addDynamicContentIfAbsent(dynamicAdditions);
    }

    private void addDynamicContentIfAbsent(final Map<String,DynamicContent> additions) {
        for (Map.Entry<String,DynamicContent> entry : additions.entrySet()) {
            addContentIfAbsent(entry.getKey(), entry.getValue());
        }
    }

    private void addContentIfAbsent(final String relativeURIString, final DynamicContent addition) {
        if ( ! dynamicContent.containsKey(relativeURIString)) {
            dynamicContent.put(relativeURIString, addition);
        }
    }

    private void processDynamicContent(final Properties tokens,
            final String relativeURIString,
            final Request gReq, final Response gResp) throws IOException, URISyntaxException {
        final DynamicContent dc = dynamicContent.get(relativeURIString);
        if (dc == null) {
            respondNotFound(gResp);
            logger.log(Level.FINE, "{0} Could not find dynamic content requested using {1}",
                    new Object[]{logPrefix(), relativeURIString});
            return;
        }
        final URI requestURI = requestURI(gReq);
        if ( ! dc.isAvailable(requestURI)) {
            finishErrorResponse(gResp, contentStateToResponseStatus(dc, requestURI));
            logger.log(Level.FINE, "{0} Found dynamic content ({1} but is is not marked as available",
                    new Object[]{logPrefix(), relativeURIString});
            return;
        }

        /*
         * Assign values for all the properties which we must compute
         * at request time, such as the scheme, host, port, and
         * items from the query string.  This merges the request-time
         * tokens with those that were known when this adapter was created.
         */
        Properties allTokens = null;
        try {
            allTokens = prepareRequestPlaceholders(tokens, gReq);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "prepareRequestPlaceholder", e);
            finishErrorResponse(gResp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        /*
         * Create an instance of the dynamic content using the dynamic
         * content's template and the just-prepared properties.
         */
        final DynamicContent.Instance instance = dc.getOrCreateInstance(allTokens);
        final Date instanceTimestamp = instance.getTimestamp();

        if (returnIfClientCacheIsCurrent(relativeURIString, gReq,
                instanceTimestamp.getTime())) {
            return;
        }

        gResp.setDateHeader(LAST_MODIFIED_HEADER_NAME, instanceTimestamp.getTime());
        gResp.setDateHeader(DATE_HEADER_NAME, System.currentTimeMillis());
        gResp.setContentType(dc.getMimeType());
        gResp.setStatus(HttpServletResponse.SC_OK);
        String text = instance.getText();
        
        if (dc.isMain()) {
            saveJNLPWithSession(gReq, text, requestURI);
        }
        
        /*
         * Only for GET should the response actually contain the content.
         * Java Web Start uses HEAD to find out when the target was last
         * modified to see if it should ask for the entire target.
         */
        final Method methodType = gReq.getMethod();
        if (Method.GET.equals(methodType)) {
            writeData(text, gReq.getResponse());
        }
        logger.log(Level.FINE, "{0}Served dyn content for {1}: {2}{3}",
                new Object[]{logPrefix(), methodType, relativeURIString,
                logger.isLoggable(Level.FINEST) ? "->" + instance.getText() : ""});
        finishResponse(gResp, HttpServletResponse.SC_OK);
    }

    private void saveJNLPWithSession(final Request gReq, final String text,
            final URI requestURI) {
        /*
         * If this is a request for the main JNLP document then the GF JWS-related
         * session attribute will not be present.  In that case, save the just-
         * generated JNLP content as a session attribute.
         */
        final Session session = gReq.getSession();
        
        
        final Boolean isMainJNLPProcessed = booleanAttr(session.getAttribute(GF_JWS_SESSION_IS_MAIN_PROCESSED_NAME));
        if ( ! isMainJNLPProcessed) {
            byte[] jnlp;
            jnlp = text.getBytes();
            session.setAttribute(GF_JWS_SESSION_IS_MAIN_PROCESSED_NAME, Boolean.TRUE);
            session.setAttribute(GF_JWS_SESSION_CACHED_JNLP_NAME, jnlp);
            logger.log(Level.FINE, "Session {1} contains no GF/JWS attr; caching {0} and setting attr to main JNLP content", 
                    new Object[] {requestURI, session.getIdInternal()});
        } else {
            logger.log(Level.FINE, "Session {0} already contains cached JNLP", session.getIdInternal());
        }
    }
    
    /**
     * Converts an Object to a Boolean.
     * 
     * @param attrValue Object (preferably a Boolean) to convert
     * @return if the argument is a Boolean, its value; false otherwise
     */
    public static Boolean booleanAttr(final Object attrValue) {
        if (attrValue == null) {
            return false;
        }
        if ( ! (attrValue instanceof Boolean)) {
            return false;
        }
        return (Boolean) attrValue;
    }
    
    /**
     * Initializes a Properties object with the token names and values for
     * substitution in the dynamic content template.
     *
     * @param the incoming request
     * @return Properties object containing the token names and values
     * @throws ServletException in case of an error preparing the placeholders
     */
    private Properties prepareRequestPlaceholders(
            final Properties adapterTokens,
            Request request) throws FileNotFoundException, IOException {
        final Properties answer = new Properties(adapterTokens);

        answer.setProperty("request.scheme", request.getScheme());
        answer.setProperty("request.host", request.getServerName());
        answer.setProperty("request.port", Integer.toString(request.getServerPort()));
        answer.setProperty("request.adapter.context.root", contextRoot());
        
        
        answer.setProperty("request.glassfish-acc.xml.content", 
                Util.toXMLEscaped(accConfigContent.sunACC()));
        answer.setProperty("request.appclient.login.conf.content",
                Util.toXMLEscaped(accConfigContent.appClientLogin()));
        answer.setProperty("request.message.security.config.provider.security.config",
                Util.toXMLEscaped(accConfigContent.securityConfig()));
        answer.setProperty("loader.config",
                Util.toXMLEscaped(loaderConfigContent.content()));

        /*
         *Treat query parameters with the name "arg" as command line arguments to the
         *app client.
         */

        final String queryString = request.getQueryString();
        final StringBuilder queryStringPropValue = new StringBuilder();
        if (queryString != null && queryString.length() > 0) {
            queryStringPropValue.append("?").append(queryString);
        }
        /*
         * Need to escape the query string which might contain arguments to the
         * acc or the jwsacc.
         */
        answer.setProperty("request.quoted.query.string", 
                Util.toXMLEscapedInclAmp(queryStringPropValue.toString()));

        processQueryParameters(queryString, answer);

        return answer;
    }

    /**
     * Returns the expression "-targetserver=host:port[,...]" representing the
     * currently-active ORBs to which the ACC could attempt to bootstrap.
     * @return
     */
    private String targetServerSetting(final Properties props) {
        String result = null;
        try {
            result = orbFactory.getIIOPEndpoints();
        } catch (NullPointerException npe) {
            /*
             * orbFactory.getIIOPEndpoints is supposed to return a valid
             * answer whether this server is in a cluster or not.  A bug
             * causes it to throw a NullPointerException in the non-cluster case.
             * So catch that and use the configured listener for this server.
             *
             * Find the IIOP listener with the default listener ID.
             */
            String port = null;
            for (IiopListener listener : iiopService.getIiopListener()) {
                if (listener.getId().equals(DEFAULT_ORB_LISTENER_ID)) {
                    port = listener.getPort();
                    break;
                }
            }
            result = props.getProperty("request.host") + ":" + port;
        }
        return result;
    }

    private void processQueryParameters(String queryString, final Properties answer) {
        if (queryString == null) {
            queryString = "";
        }
        String [] queryParams = null;
        try {
            queryParams = URLDecoder.decode(queryString, "UTF-8").split("&");
        } catch (UnsupportedEncodingException e) {
            // This should never happen.  We'd better know about UTF-8!
            throw new RuntimeException(e);
        }

        QueryParams arguments = new ArgQueryParams();
        QueryParams properties = new PropQueryParams();
        QueryParams vmArguments = new VMArgQueryParams();
        QueryParams accArguments = new ACCArgQueryParams(targetServerSetting(answer));
        QueryParams jwsaccArguments = new JWSACCArgQueryParams();
        QueryParams [] paramTypes = new QueryParams[] {arguments, properties, 
            vmArguments, accArguments, jwsaccArguments};

        for (String param : queryParams) {
            for (QueryParams qpType : paramTypes) {
                if (qpType.processParameter(param)) {
                    break;
                }
            }
        }

        answer.setProperty("request.arguments", arguments.toString());
        answer.setProperty("request.properties", properties.toString());
        answer.setProperty("request.vmargs", vmArguments.toString());
        answer.setProperty("request.extra.agent.args", accArguments.toString());

        answer.setProperty("request.javaws.acc.properties", jwsaccArguments.toString());
    }

    /**
     * Some stolen from Grizzly's StaticResourcesAdapter -- maybe it'll get
     * refactored out later?
     *
     * @param resource
     * @param req
     * @param res
     */
    private void writeData(final String data,
            final Response res) {
        try {
            res.setStatus(HttpServletResponse.SC_OK);


            res.setContentLength(data.length());
            res.flush();

            Writer pw = res.getWriter();
            pw.write(data);
            pw.write(NEW_LINE);
            pw.flush();
        } catch (Exception e) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            res.setError();
            return;
        }
    }
    
    private String commaIfNeeded(final int origLength) {
        return origLength > 0 ? "," : "";
    }

    //    public static class DynamicContent {
//        private final String content;
//        private final Date timestamp;
//
//        public DynamicContent(final String content, final Date timestamp) {
//            this.content = content;
//            this.timestamp = timestamp;
//        }
//
//        public String content() {
//            return content;
//        }
//
//        public Date timestamp() {
//            return timestamp;
//        }
//    }
    private abstract class QueryParams {
        private String prefix;

        protected QueryParams(String prefix) {
            this.prefix = prefix;
        }

        private boolean handles(String prefix) {
            return prefix.equals(this.prefix);
        }

        protected abstract void processValue(String value);

        @Override
        public abstract String toString();

        public boolean processParameter(String param) {
            boolean result = false;
            final int equalsSign = param.indexOf("=");
            String value = "";
            String paramPrefix;
            if (equalsSign != -1) {
                paramPrefix = param.substring(0, equalsSign);
            } else {
                paramPrefix = param;
            }
            if (handles(paramPrefix)) {
                result = true;
                if ((equalsSign + 1) < param.length()) {
                    value = param.substring(equalsSign + 1);
                }
                processValue(value);
            }
            return result;
        }
    }

    private class ArgQueryParams extends QueryParams {
        private StringBuilder arguments = new StringBuilder();

        public ArgQueryParams() {
            super(ARG_QUERY_PARAM_NAME);
        }

        @Override
        public void processValue(String value) {
            if (value.length() == 0) {
                value = "#missing#";
            }
            arguments.append("<argument>").append(value).append("</argument>").append(LINE_SEP);
        }

        @Override
        public String toString() {
            return arguments.toString();
        }
    }

    /**
     * Processes query string parameters as ACC arguments.
     * <p>
     * The URL which launches the app client might contain query arguments
     * of the form accarg=xxx or accarg=xxx=yyy.  Convert these into
     * additional agent arguments the same way the appclient script does:
     * arg=(whatever the query argument is).  For example,
     * <code>
     * ?accarg=-user=roland
     * </code> in the URL
     * translates to the agent argument
     * <code>
     * arg=-user=roland
     * </code> in the agent arguments.
     */
    private class ACCArgQueryParams extends QueryParams {
        private StringBuilder settings = new StringBuilder();
        private final String targetServerSetting;


        public ACCArgQueryParams(final String targetServerSetting) {
            super (ACC_ARG_QUERY_PARAM_NAME);
            this.targetServerSetting = "arg=-targetserver,arg=" + targetServerSetting;
        }

        @Override
        public void processValue(String value) {
            settings.append(commaIfNeeded(settings.length())).append("arg=").append(value);
        }

        @Override
        public String toString() {
            return settings.toString() + commaIfNeeded(settings.length()) +
                    targetServerSetting;
        }
    }

    private class JWSACCArgQueryParams extends QueryParams {

        private final static String JWS_ACC_PROPERTY_PREFIX = "javaws.acc.";
        private final Properties props = new Properties();

        private JWSACCArgQueryParams() {
            super(JWS_ARG_QUERY_PARAM_NAME);
        }

        @Override
        protected void processValue(String value) {
            /*
             * An = sign might separate the arg name from its value, or maybe not.
             */
            final int equals = value.indexOf('=');
            final String propName = (equals == -1 ? value : value.substring(1, equals));
            final String propValue = (equals == -1 ? "" : value.substring(equals+1));
            props.setProperty(propName, propValue);
        }

        @Override
        public String toString() {
            /*
             * Return zero or more JNLP property settings like this:
             * 
             * <property name="javaws.acc.i" value="argName[=argValue]"/>
             *
             * where i goes from 0 upwards.
             */
            int slot = 0;
            final StringBuilder sb = new StringBuilder();
            for (Map.Entry<Object,Object> entry : props.entrySet()) {
                sb.append("<property name=\"").
                        append(JWS_ACC_PROPERTY_PREFIX).
                        append(slot++).
                        append("\" value=\"").
                        append((String) entry.getKey()).
                        append("=").
                        append((String) entry.getValue()).
                        append("\"/>").
                        append(LINE_SEP);
            }
            return sb.toString();
        }
    }

    private class PropQueryParams extends QueryParams {
        private StringBuilder properties = new StringBuilder();

        public PropQueryParams() {
            super(PROP_QUERY_PARAM_NAME);
        }

        @Override
        public void processValue(String value) {
            if (value.length() > 0) {
                final int equalsSign = value.indexOf('=');
                String propValue = "";
                String propName;
                if (equalsSign > 0) {
                    propName = value.substring(0, equalsSign);
                    if ((equalsSign + 1) < value.length()) {
                        propValue = value.substring(equalsSign + 1);
                    }
                    properties.append("<property name=\"").
                               append(propName).
                               append("\" value=\"").
                               append(propValue).
                               append("\"/>").
                               append(LINE_SEP);
                }
            }
        }

        @Override
        public String toString() {
            return properties.toString();
        }

    }

    private class VMArgQueryParams extends QueryParams {
        private StringBuilder vmArgs = new StringBuilder();

        public VMArgQueryParams() {
            super(VMARG_QUERY_PARAM_NAME);
        }

        @Override
        public void processValue(String value) {
            vmArgs.append(value).append(" ");
        }

        @Override
        public String toString() {
            return vmArgs.length() > 0 ? " java-vm=args=\"" + vmArgs.toString() + "\"" : "";
        }
    }

    protected String dumpContent(final Map<String,DynamicContent> dc) {
        if (dc == null) {
            return "   Dynamic content: not initialized";
        }
        if (dc.isEmpty()) {
            return "  Dynamic content: empty" + LINE_SEP;
        }
        final StringBuilder sb = new StringBuilder("  Dynamic content:");
        for (Map.Entry<String,DynamicContent> entry : dc.entrySet()) {
            sb.append("  ").
               append(entry.getKey());
            if (logger.isLoggable(Level.FINER)) {
                sb.append("  ====").append(LINE_SEP).append(entry.getValue().toString())
                        .append("  ====").append(LINE_SEP);
            }
        }
        sb.append("  ========");
        return sb.toString();
    }
}
