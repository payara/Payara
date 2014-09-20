/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.realm;

import org.apache.catalina.*;
import org.apache.catalina.authenticator.AuthenticatorBase;
import org.apache.catalina.connector.Response;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardServer;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.deploy.SecurityCollection;
import org.apache.catalina.deploy.SecurityConstraint;
import org.apache.catalina.util.HexUtils;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.MD5Encoder;
import org.apache.catalina.util.StringManager;

import javax.management.ObjectName;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.enterprise.util.Utility;
import javax.servlet.ServletContext;
import org.glassfish.logging.annotation.LogMessageInfo;
// END SJSWS 6324431

/**
 * Simple implementation of <b>Realm</b> that reads an XML file to configure
 * the valid users, passwords, and roles.  The file format (and default file
 * location) are identical to those currently supported by Tomcat 3.X.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.14 $ $Date: 2007/04/18 17:27:23 $
 */

public abstract class RealmBase
    implements Lifecycle, Realm {

    protected static final Logger log = StandardServer.log;
    protected static final ResourceBundle rb = log.getResourceBundle();

    @LogMessageInfo(
            message = "Illegal digestEncoding: {0}",
            level = "SEVERE",
            cause = "Could not convert the char array to byte array with respect to given charset",
            action = "Verify the current charset"
    )
    public static final String ILLEGAL_DIGEST_ENCODING_EXCEPTION = "AS-WEB-CORE-00312";

    @LogMessageInfo(
            message = "Access to the requested resource has been denied",
            level = "WARNING"
    )
    public static final String ACCESS_RESOURCE_DENIED = "AS-WEB-CORE-00313";

    @LogMessageInfo(
            message = "Configuration error: Cannot perform access control without an authenticated principal",
            level = "WARNING"
    )
    public static final String CONFIG_ERROR_NOT_AUTHENTICATED = "AS-WEB-CORE-00314";

    @LogMessageInfo(
            message = "Username {0} has role {1}",
            level = "FINE"
    )
    public static final String USERNAME_HAS_ROLE = "AS-WEB-CORE-00315";

    @LogMessageInfo(
            message = "Username {0} does NOT have role {1}",
            level = "FINE"
    )
    public static final String USERNAME_NOT_HAVE_ROLE = "AS-WEB-CORE-00316";

    @LogMessageInfo(
            message = "This Realm has already been started",
            level = "INFO"
    )
    public static final String REALM_BEEN_STARTED = "AS-WEB-CORE-00317";

    @LogMessageInfo(
            message = "Invalid message digest algorithm {0} specified",
            level = "WARNING"
    )
    public static final String INVALID_ALGORITHM_EXCEPTION = "AS-WEB-CORE-00318";

    @LogMessageInfo(
            message = "This Realm has not yet been started",
            level = "INFO"
    )
    public static final String REALM_NOT_BEEN_STARTED = "AS-WEB-CORE-00319";

    @LogMessageInfo(
            message = "Error digesting user credentials",
            level = "SEVERE",
            cause = "Could not digest user credentials",
            action = "Verify the current credential"
    )
    public static final String ERROR_DIGESTING_USER_CREDENTIAL_EXCEPTION = "AS-WEB-CORE-00320";

    @LogMessageInfo(
            message = "Couldn't get MD5 digest",
            level = "SEVERE",
            cause = "Could not get instance of MessageDigest based on MD5",
            action = "Verify if it supports a MessageDigestSpi implementation " +
                     "for the specified algorithm"
    )
    public static final String CANNOT_GET_MD5_DIGEST_EXCEPTION = "AS-WEB-CORE-00321";


    //START SJSAS 6202703
    /**
     * "Expires" header always set to Date(1), so generate once only
     */
    private static final String DATE_ONE =
            (new SimpleDateFormat(Response.HTTP_RESPONSE_DATE_HEADER,
            Locale.US)).format(new Date(1));
    //END SJSAS 6202703

    // ----------------------------------------------------- Instance Variables

     /**
     * The debugging detail level for this component.
     */
    protected int debug = 0;

    /**
     * The Container with which this Realm is associated.
     */
    protected Container container = null;


    /**
     * Flag indicating whether a check to see if the request is secure is
     * required before adding Pragma and Cache-Control headers when proxy 
     * caching has been disabled
     */
    protected boolean checkIfRequestIsSecure = false;


    /**
     * Digest algorithm used in storing passwords in a non-plaintext format.
     * Valid values are those accepted for the algorithm name by the
     * MessageDigest class, or <code>null</code> if no digesting should
     * be performed.
     */
    protected String digest = null;

    /**
     * The encoding charset for the digest.
     */
    protected String digestEncoding = null;


    /**
     * Descriptive information about this Realm implementation.
     */
    protected static final String info =
        "org.apache.catalina.realm.RealmBase/1.0";


    /**
     * The lifecycle event support for this component.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * The MessageDigest object for digesting user credentials (passwords).
     */
    protected volatile MessageDigest md = null;


    /**
     * The MD5 helper object for this class.
     */
    protected static final MD5Encoder md5Encoder = new MD5Encoder();


    /**
     * MD5 message digest provider.
     */
    protected static volatile MessageDigest md5Helper;


    /**
     * Has this component been started?
     */
    protected boolean started = false;


    /**
     * The property change support for this component.
     */
    protected PropertyChangeSupport support = new PropertyChangeSupport(this);


    /**
     * Should we validate client certificate chains when they are presented?
     */
    protected boolean validate = true;


    // ------------------------------------------------------------- Properties


    /**
     * Return the Container with which this Realm has been associated.
     */
    public Container getContainer() {

        return (container);

    }

    /**
     * Return the debugging detail level for this component.
     */
    public int getDebug() {

        return (this.debug);

    }


    /**
     * Set the debugging detail level for this component.
     *
     * @param debug The new debugging detail level
     */
    public void setDebug(int debug) {

        this.debug = debug;

    }

    /**
     * Set the Container with which this Realm has been associated.
     *
     * @param container The associated Container
     */
    public void setContainer(Container container) {

        Container oldContainer = this.container;
        this.container = container;
        this.checkIfRequestIsSecure = container.isCheckIfRequestIsSecure();
        support.firePropertyChange("container", oldContainer, this.container);

    }

    /**
     * Return the digest algorithm  used for storing credentials.
     */
    public String getDigest() {

        return digest;

    }


    /**
     * Set the digest algorithm used for storing credentials.
     *
     * @param digest The new digest algorithm
     */
    public void setDigest(String digest) {

        this.digest = digest;

    }

    /**
     * Returns the digest encoding charset.
     *
     * @return The charset (may be null) for platform default
     */
    public String getDigestEncoding() {
        return digestEncoding;
    }

    /**
     * Sets the digest encoding charset.
     *
     * @param charset The charset (null for platform default)
     */
    public void setDigestEncoding(String charset) {
        digestEncoding = charset;
    }

    /**
     * Return descriptive information about this Realm implementation and
     * the corresponding version number, in the format
     * <code>&lt;description&gt;/&lt;version&gt;</code>.
     */
    public String getInfo() {

        return info;

    }


    /**
     * Return the "validate certificate chains" flag.
     */
    public boolean getValidate() {

        return (this.validate);

    }


    /**
     * Set the "validate certificate chains" flag.
     *
     * @param validate The new validate certificate chains flag
     */
    public void setValidate(boolean validate) {

        this.validate = validate;

    }


    // --------------------------------------------------------- Public Methods


    
    /**
     * Add a property change listener to this component.
     *
     * @param listener The listener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {

        support.addPropertyChangeListener(listener);

    }


    /**
     * Return the Principal associated with the specified username and
     * credentials, if there is one; otherwise return <code>null</code>.
     *
     * @param username Username of the Principal to look up
     * @param credentials Password or other credentials to use in
     *  authenticating this username
     */
    public Principal authenticate(String username, char[] credentials) {

        char[] serverCredentials = getPassword(username);

        boolean validated ;
        if ( serverCredentials == null ) {
            validated = false;
        } else if(hasMessageDigest()) {
            validated = equalsIgnoreCase(serverCredentials, digest(credentials));
        } else {
            validated = Arrays.equals(serverCredentials, credentials);
        }
        if(! validated ) {
            return null;
        }
        return getPrincipal(username);
    }


    /**
     * Return the Principal associated with the specified username, which
     * matches the digest calculated using the given parameters using the
     * method described in RFC 2069; otherwise return <code>null</code>.
     *
     * @param username Username of the Principal to look up
     * @param clientDigest Digest which has been submitted by the client
     * @param nOnce Unique (or supposedly unique) token which has been used
     * for this request
     * @param realm Realm name
     * @param md5a2 Second MD5 digest used to calculate the digest :
     * MD5(Method + ":" + uri)
     */
    public Principal authenticate(String username, char[] clientDigest,
                                  String nOnce, String nc, String cnonce,
                                  String qop, String realm,
                                  char[] md5a2) {

        char[] md5a1 = getDigest(username, realm);
        if (md5a1 == null)
            return null;

        int nOnceLength = ((nOnce != null) ? nOnce.length() : 0);
        int ncLength = ((nc != null) ? nc.length() : 0);
        int cnonceLength = ((cnonce != null) ? cnonce.length() : 0);
        int qopLength = ((qop != null) ? qop.length() : 0);
        int md5a2Length = ((md5a2 != null) ? md5a2.length : 0);

        // serverDigestValue = md5a1:nOnce:nc:cnonce:qop:md5a2
        char[] serverDigestValue = new char[md5a1.length + 1 + 
            nOnceLength + 1 + ncLength + 1 + cnonceLength + 1 +
            qopLength + 1 + md5a2Length];

        System.arraycopy(md5a1, 0, serverDigestValue, 0, md5a1.length);
        int ind = md5a1.length;
        serverDigestValue[ind++] = ':';
        if (nOnce != null) {
            System.arraycopy(nOnce.toCharArray(), 0, serverDigestValue, ind, nOnceLength);
            ind += nOnceLength;
        }
        serverDigestValue[ind++] = ':';
        if (nc != null) {
            System.arraycopy(nc.toCharArray(), 0, serverDigestValue, ind, ncLength);
            ind += ncLength;
        }
        serverDigestValue[ind++] = ':';
        if (cnonce != null) {
            System.arraycopy(cnonce.toCharArray(), 0, serverDigestValue, ind, cnonceLength);
            ind += cnonceLength;
        }
        serverDigestValue[ind++] = ':';
        if (qop != null) {
            System.arraycopy(qop.toCharArray(), 0, serverDigestValue, ind, qopLength);
            ind += qopLength;
        }
        serverDigestValue[ind++] = ':';
        if (md5a2 != null) {
            System.arraycopy(md5a2, 0, serverDigestValue, ind, md5a2Length);
        }

        byte[] valueBytes = null;

        try {
            valueBytes = Utility.convertCharArrayToByteArray(
                    serverDigestValue, getDigestEncoding());
        } catch (CharacterCodingException cce) {
            String msg = MessageFormat.format(rb.getString(ILLEGAL_DIGEST_ENCODING_EXCEPTION),
                                              getDigestEncoding());
            log.log(Level.SEVERE, msg, cce);
            throw new IllegalArgumentException(cce.getMessage());
        }

        char[] serverDigest = null;
        // Bugzilla 32137
        synchronized(md5Helper) {
            serverDigest = md5Encoder.encode(md5Helper.digest(valueBytes));
        }

        if (log.isLoggable(Level.FINE)) {
            String msg = "Username:" + username
                         + " ClientSigest:" + Arrays.toString(clientDigest) + " nOnce:" + nOnce
                         + " nc:" + nc + " cnonce:" + cnonce + " qop:" + qop
                         + " realm:" + realm + "md5a2:" + Arrays.toString(md5a2)
                         + " Server digest:" + String.valueOf(serverDigest);
            log.log(Level.FINE, msg);
        }
        
        if (Arrays.equals(serverDigest, clientDigest)) {
            return getPrincipal(username);
        } else {
            return null;
        }
    }



    /**
     * Return the Principal associated with the specified chain of X509
     * client certificates.  If there is none, return <code>null</code>.
     *
     * @param certs Array of client certificates, with the first one in
     *  the array being the certificate of the client itself.
     */
    public Principal authenticate(X509Certificate certs[]) {

        if ((certs == null) || (certs.length < 1))
            return (null);

        // Check the validity of each certificate in the chain
        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Authenticating client certificate chain");
        if (validate) {
            for (int i = 0; i < certs.length; i++) {
                if (log.isLoggable(Level.FINE))
                    log.log(Level.FINE, "Checking validity for '" +
                            certs[i].getSubjectDN().getName() + "'");
                try {
                    certs[i].checkValidity();
                } catch (Exception e) {
                    if (log.isLoggable(Level.FINE))
                        log.log(Level.FINE, "Validity exception", e);
                    return (null);
                }
            }
        }

        // Check the existence of the client Principal in our database
        return (getPrincipal(certs[0].getSubjectDN().getName()));

    }

    
    /**
     * Execute a periodic task, such as reloading, etc. This method will be
     * invoked inside the classloading context of this container. Unexpected
     * throwables will be caught and logged.
     */
    public void backgroundProcess() {
    }


    /**
     * Return the SecurityConstraints configured to guard the request URI for
     * this request, or <code>null</code> if there is no such constraint.
     *
     * @param request Request we are processing
     * @param context Context the Request is mapped to
     */
    public SecurityConstraint[] findSecurityConstraints(
            HttpRequest request, Context context) {
        return findSecurityConstraints(
            request.getRequestPathMB().toString(),
            ((HttpServletRequest) request.getRequest()).getMethod(),
            context);
    }

    /**
     * Gets the security constraints configured by the given context
     * for the given request URI and method.
     *
     * @param uri the request URI (minus the context Path)
     * @param method the request method
     * @param context the context
     *
     * @return the security constraints configured by the given context
     * for the given request URI and method, or null
     */
    public SecurityConstraint[] findSecurityConstraints(
            String uri, String method, Context context) {

        ArrayList<SecurityConstraint> results = null;

        // Are there any defined security constraints?
        if (!context.hasConstraints()) {
            if (log.isLoggable(Level.FINE))
                log.log(Level.FINE, "  No applicable constraints defined");
            return (null);
        }
        
        // START SJSWS 6324431
        String origUri = uri;
        boolean caseSensitiveMapping = 
            ((StandardContext)context).isCaseSensitiveMapping();
        if (uri != null && !caseSensitiveMapping) {
            uri = uri.toLowerCase(Locale.ENGLISH);
        }
        // END SJSWS 6324431

        boolean found = false;

        List<SecurityConstraint> constraints = context.getConstraints();
        Iterator<SecurityConstraint> i = constraints.iterator(); 
        while (i.hasNext()) {
            SecurityConstraint constraint = i.next();
            SecurityCollection[] collection = constraint.findCollections();
                     
            // If collection is null, continue to avoid an NPE
            // See Bugzilla 30624
            if (collection == null) {
                continue;
            }

            if (log.isLoggable(Level.FINEST)) {
                /* SJSWS 6324431
                log.trace("  Checking constraint '" + constraints[i] +
                    "' against " + method + " " + uri + " --> " +
                    constraints[i].included(uri, method));
                */
                // START SJSWS 6324431
                String msg = "Checking constraint '" + constraint +
                             "' against " + method + " " + origUri +
                             " --> " +
                             constraint.included(uri, method,
                                                 caseSensitiveMapping);
                log.log(Level.FINEST, msg);
                // END SJSWS 6324431
            }
            /* SJSWS 6324431
            if (log.isDebugEnabled() && constraints[i].included(
                    uri, method)) {
                log.debug("  Matched constraint '" + constraints[i] +
                    "' against " + method + " " + uri);
            }
            */
            // START SJSWS 6324431
            if (log.isLoggable(Level.FINE)
                    && constraint.included(uri, method,
                                           caseSensitiveMapping)) {

                log.log(Level.FINE, "  Matched constraint '" + constraint +
                        "' against " + method + " " + origUri);

            }
            // END SJSWS 6324431

            for (int j=0; j < collection.length; j++){
                String[] patterns = collection[j].findPatterns();
                // If patterns is null, continue to avoid an NPE
                // See Bugzilla 30624
                if ( patterns == null) {
                    continue;
                }

                for(int k=0; k < patterns.length; k++) {
                    /* SJSWS 6324431
                    if(uri.equals(patterns[k])) {
                    */
                    // START SJSWS 6324431
                    String pattern = caseSensitiveMapping ? patterns[k] :
                        patterns[k].toLowerCase(Locale.ENGLISH);
                    if (uri != null && uri.equals(pattern)) {
                    // END SJSWS 6324431
                        found = true;
                        if(collection[j].findMethod(method)) {
                            if(results == null) {
                                results = new ArrayList<SecurityConstraint>();
                            }
                            results.add(constraint);
                        }
                    }
                }
            }
        } // while

        if (found) {
            return resultsToArray(results);
        }

        int longest = -1;

        i = constraints.iterator(); 
        while (i.hasNext()) {
            SecurityConstraint constraint = i.next();
            SecurityCollection [] collection =
                constraint.findCollections();
            
            // If collection is null, continue to avoid an NPE
            // See Bugzilla 30624
            if ( collection == null) {
                continue;
            }

            if (log.isLoggable(Level.FINEST)) {
                /* SJSWS 6324431
                log.trace("  Checking constraint '" + constraints[i] +
                    "' against " + method + " " + uri + " --> " +
                    constraints[i].included(uri, method));
                */
                // START SJSWS 6324431
                String msg = "  Checking constraint '" + constraint +
                             "' against " + method + " " + origUri +
                             " --> " +
                             constraint.included(uri, method,
                                                 caseSensitiveMapping);
                log.log(Level.FINE, msg);
                // END SJSWS 6324431
            }
            /* SJSWS 6324431
            if (log.isDebugEnabled() && constraints[i].included(
                    uri, method)) {
                log.debug("  Matched constraint '" + constraints[i] +
                    "' against " + method + " " + uri);
            }
            */
            // START SJSWS 6324431
            if (log.isLoggable(Level.FINE) &&
                    constraint.included(uri, method,
                                        caseSensitiveMapping)) {
                log.log(Level.FINE, "  Matched constraint '" + constraint +
                        "' against " + method + " " + origUri);
            }
            // END SJSWS 6324431

            for (int j=0; j < collection.length; j++){
                String[] patterns = collection[j].findPatterns();
                // If patterns is null, continue to avoid an NPE
                // See Bugzilla 30624
                if (patterns == null) {
                    continue;
                }

                boolean matched = false;
                int length = -1;
                for (int k=0; k < patterns.length; k++) {
                    /* SJSWS 6324431
                    String pattern = patterns[k];
                    */
                    // START SJSWS 6324431
                    String pattern = caseSensitiveMapping ?
                        patterns[k]:patterns[k].toLowerCase(Locale.ENGLISH);
                    // END SJSWS 6324431
                    if (pattern.startsWith("/") &&
                            pattern.endsWith("/*") && 
                            pattern.length() >= longest) {
                            
                        if (pattern.length() == 2) {
                            matched = true;
                            length = pattern.length();
                        } else if (uri != null
                                && (pattern.regionMatches(
                                        0,uri,0,pattern.length()-1)
                                    || (pattern.length()-2 == uri.length()
                                        && pattern.regionMatches(
                                            0,uri,0,pattern.length()-2)))) {
                            matched = true;
                            length = pattern.length();
                        }
                    }
                }
                if (matched) {
                    found = true;
                    if (length > longest) {
                        if (results != null) {
                            results.clear();
                        }
                        longest = length;
                    }
                    if (collection[j].findMethod(method)) {
                        if (results == null) {
                            results = new ArrayList<SecurityConstraint>();
                        }
                        results.add(constraint);
                    }
                }
            }
        } // while

        if (found) {
            return resultsToArray(results);
        }

        i = constraints.iterator(); 
        while (i.hasNext()) {
            SecurityConstraint constraint = i.next();
            SecurityCollection[] collection = constraint.findCollections();

            // If collection is null, continue to avoid an NPE
            // See Bugzilla 30624
            if ( collection == null) {
                continue;
            }
            
            if (log.isLoggable(Level.FINEST)) {
                /* SJSWS 6324431
                log.trace("  Checking constraint '" + constraints[i] +
                    "' against " + method + " " + uri + " --> " +
                    constraints[i].included(uri, method));
                */
                // START SJSWS 6324431
                String msg = "  Checking constraint '" + constraint +
                        "' against " + method + " " + origUri +
                        " --> " +
                        constraint.included(uri, method,
                                caseSensitiveMapping);
                log.log(Level.FINEST, msg);
                // END SJSWS 6324431
            }
            /* SJSWS 6324431
            if (log.isDebugEnabled() && constraints[i].included(
                    uri, method)) {
                log.debug("  Matched constraint '" + constraints[i] +
                    "' against " + method + " " + uri);
            }
            */
            // START SJSWS 6324431
            if (log.isLoggable(Level.FINE) &&
                    constraint.included(uri, method,
                                        caseSensitiveMapping)) {

                log.log(Level.FINE, "  Matched constraint '" + constraint +
                        "' against " + method + " " + origUri);
            }
            // END SJSWS 6324431

            boolean matched = false;
            int pos = -1;
            for (int j=0; j < collection.length; j++){
                String [] patterns = collection[j].findPatterns();
                // If patterns is null, continue to avoid an NPE
                // See Bugzilla 30624
                if (patterns == null) {
                    continue;
                }

                for(int k=0; k < patterns.length && !matched; k++) {
                    /* SJSWS 6324431
                    String pattern = patterns[k];
                    */
                    // START SJSWS 6324431
                    String pattern = caseSensitiveMapping ? 
                        patterns[k]:patterns[k].toLowerCase(Locale.ENGLISH);
                    // END SJSWS 6324431
                    if (uri != null && pattern.startsWith("*.")){
                        int slash = uri.lastIndexOf("/");
                        int dot = uri.lastIndexOf(".");
                        if (slash >= 0 && dot > slash &&
                                dot != uri.length()-1 &&
                                uri.length()-dot == pattern.length()-1) {
                            if (pattern.regionMatches(
                                    1,uri,dot,uri.length()-dot)) {
                                matched = true;
                                pos = j;
                            }
                        }
                    }
                }
            }
    
            if (matched) {
                found = true;
                if (collection[pos].findMethod(method)) {
                    if(results == null) {
                        results = new ArrayList<SecurityConstraint>();
                    }
                    results.add(constraint);
                }
            }
        } // while

        if (found) {
            return resultsToArray(results);
        }

        i = constraints.iterator(); 
        while (i.hasNext()) {
            SecurityConstraint constraint = i.next();
            SecurityCollection[] collection = constraint.findCollections();
            
            // If collection is null, continue to avoid an NPE
            // See Bugzilla 30624
            if (collection == null) {
                continue;
            }

            if (log.isLoggable(Level.FINEST)) {
                /* SJSWS 6324431
                log.trace("  Checking constraint '" + constraints[i] +
                    "' against " + method + " " + uri + " --> " +
                    constraints[i].included(uri, method));
                */
                // START SJSWS 6324431
                String msg = "  Checking constraint '" + constraint +
                        "' against " + method + " " + origUri +
                        " --> " +
                        constraint.included(uri, method,
                                caseSensitiveMapping);
                log.log(Level.FINEST, msg);
                // END SJSWS 6324431
            }
            /* SJSWS 6324431
            if (log.isDebugEnabled() && constraints[i].included(
                    uri, method)) {
                log.debug("  Matched constraint '" + constraints[i] +
                    "' against " + method + " " + uri);
            }
            */
            // START SJSWS 6324431
            if (log.isLoggable(Level.FINE) &&
                    constraint.included(uri, method,
                                        caseSensitiveMapping)) {
                log.log(Level.FINE, "  Matched constraint '" + constraint +
                        "' against " + method + " " + origUri);
            }
            // END SJSWS 6324431

            for (int j=0; j < collection.length; j++){
                String[] patterns = collection[j].findPatterns();

                // If patterns is null, continue to avoid an NPE
                // See Bugzilla 30624
                if (patterns == null) {
                    continue;
                }

                boolean matched = false;
                for (int k=0; k < patterns.length && !matched; k++) {
                    /* SJSWS 6324431
                    String pattern = patterns[k];
                    */
                    // START SJSWS 6324431
                    String pattern = caseSensitiveMapping ? 
                        patterns[k]:patterns[k].toLowerCase(Locale.ENGLISH);
                    // END SJSWS 6324431
                    if (pattern.equals("/")){
                        matched = true;
                    }
                }
                if (matched) {
                    if (results == null) {
                        results = new ArrayList<SecurityConstraint>();
                    }                    
                    results.add(constraint);
                }
            }
        } // while

        if (results == null) {
            // No applicable security constraint was found
            if (log.isLoggable(Level.FINE))
                log.log(Level.FINE, "  No applicable constraint located");
        }

        return resultsToArray(results);
    }
 
    /**
     * Convert an ArrayList to a SecurityContraint [].
     */
    private SecurityConstraint [] resultsToArray(ArrayList<SecurityConstraint> results) {
        if(results == null) {
            return null;
        }
        SecurityConstraint [] array = new SecurityConstraint[results.size()];
        results.toArray(array);
        return array;
    }

    
    /**
     * Perform access control based on the specified authorization constraint.
     * Return <code>true</code> if this constraint is satisfied and processing
     * should continue, or <code>false</code> otherwise.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param constraints Security constraint we are enforcing
     * @param context The Context to which client of this class is attached.
     *
     * @exception IOException if an input/output error occurs
     */
    public boolean hasResourcePermission(HttpRequest request,
                                         HttpResponse response,
                                         SecurityConstraint []constraints,
                                         Context context)
        throws IOException {

        if (constraints == null || constraints.length == 0)
            return (true);

        // Which user principal have we already authenticated?
        Principal principal = ((HttpServletRequest)request.getRequest())
                                                            .getUserPrincipal();
        for(int i=0; i < constraints.length; i++) {
            SecurityConstraint constraint = constraints[i];
            String roles[] = constraint.findAuthRoles();
            if (roles == null)
                roles = new String[0];

            if (constraint.getAllRoles())
                return (true);

            if (log.isLoggable(Level.FINE))
                log.log(Level.FINE, "  Checking roles " + principal);

            if (roles.length == 0) {
                if(constraint.getAuthConstraint()) {

                    // BEGIN S1AS 4878272
                    ((HttpServletResponse) response.getResponse()).sendError
                        (HttpServletResponse.SC_FORBIDDEN);
                    response.setDetailMessage(rb.getString(ACCESS_RESOURCE_DENIED));
                    // END S1AS 4878272

                    if (log.isLoggable(Level.FINE)) log.log(Level.FINE, "No roles ");
                    return (false); // No listed roles means no access at all
                } else {
                    if (log.isLoggable(Level.FINE)) {
                        log.log(Level.FINE, "Passing all access");
                    }
                    return (true);
                }
            } else if (principal == null) {
                if (log.isLoggable(Level.FINE))
                    log.log(Level.FINE, "  No user authenticated, cannot grant access");

                // BEGIN S1AS 4878272
                ((HttpServletResponse) response.getResponse()).sendError
                    (HttpServletResponse.SC_FORBIDDEN);
                response.setDetailMessage(rb.getString(CONFIG_ERROR_NOT_AUTHENTICATED));
                // END S1AS 4878272
                return (false);
            }


            for (int j = 0; j < roles.length; j++) {
                if (hasRole(principal, roles[j])) {
                    if (log.isLoggable(Level.FINE))
                        log.log(Level.FINE, "Role found:  " + roles[j]);
                    return (true);
                } else {
                    if (log.isLoggable(Level.FINE))
                        log.log(Level.FINE, "No role found:  " + roles[j]);
                }
            }
        }
        // Return a "Forbidden" message denying access to this resource
        /* S1AS 4878272
        ((HttpServletResponse) response.getResponse()).sendError
        */
        // BEGIN S1AS 4878272
        ((HttpServletResponse) response.getResponse()).sendError
            (HttpServletResponse.SC_FORBIDDEN);
        response.setDetailMessage(rb.getString(ACCESS_RESOURCE_DENIED));
        // END S1AS 4878272
        return (false);

    }
    
    //START SJSAS 6232464
    /**
     * Return <code>true</code> if the specified Principal has the specified
     * security role, within the context of this Realm; otherwise return
     * <code>false</code>.  This method can be overridden by Realm
     * implementations. The default implementation is to forward to
     * hasRole(Principal principal, String role).
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param principal Principal for whom the role is to be checked
     * @param role Security role to be checked
     */
    public boolean hasRole(HttpRequest request, 
                           HttpResponse response, 
                           Principal principal, 
                           String role) {
        return hasRole(principal, role);
    }
    //END SJSAS 6232464
    
    //START SJSAS 6202703
    /**
     * Checks whether or not authentication is needed.
     * Returns an int, one of AUTHENTICATE_NOT_NEEDED, AUTHENTICATE_NEEDED,
     * or AUTHENTICATED_NOT_AUTHORIZED.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param constraints Security constraint we are enforcing
     * @param disableProxyCaching whether or not to disable proxy caching for
     *        protected resources.
     * @param securePagesWithPragma true if we add headers which 
     * are incompatible with downloading office documents in IE under SSL but
     * which fix a caching problem in Mozilla.
     * @param ssoEnabled true if sso is enabled
     * @exception IOException if an input/output error occurs
     */
    public int preAuthenticateCheck(HttpRequest request,
                                    HttpResponse response,
                                    SecurityConstraint[] constraints,
                                    boolean disableProxyCaching,
                                    boolean securePagesWithPragma,
                                    boolean ssoEnabled)
                                    throws IOException {
        for(int i=0; i < constraints.length; i++) {
            if (constraints[i].getAuthConstraint()) {
                disableProxyCaching(request, response, disableProxyCaching, securePagesWithPragma);
                return Realm.AUTHENTICATE_NEEDED;
            }
        }
        return Realm.AUTHENTICATE_NOT_NEEDED;
    }
    
    
    /**
     * Authenticates the user making this request, based on the specified
     * login configuration.  Return <code>true</code> if any specified
     * requirements have been satisfied, or <code>false</code> if we have
     * created a response challenge already.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param context The Context to which client of this class is attached.
     * @param authenticator the current authenticator.
     * @exception IOException if an input/output error occurs
     */
    public boolean invokeAuthenticateDelegate(HttpRequest request,
                                              HttpResponse response,
                                              Context context,
                                              Authenticator authenticator,
                                              boolean calledFromAuthenticate)
          throws IOException {
        LoginConfig config = context.getLoginConfig();
        return ((AuthenticatorBase) authenticator).authenticate(
                        request, response, config);
    }
    
    /**
     * Post authentication for given request and response.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param context The Context to which client of this class is attached.
     * @exception IOException if an input/output error occurs
     */
    public boolean invokePostAuthenticateDelegate(HttpRequest request,
                                              HttpResponse response,
                                              Context context)
          throws IOException {
         return true;
    }
    
    //END SJSAS 6202703

    /**
     * Return <code>true</code> if the specified Principal has the specified
     * security role, within the context of this Realm; otherwise return
     * <code>false</code>.  This method can be overridden by Realm
     * implementations, but the default is adequate when an instance of
     * <code>GenericPrincipal</code> is used to represent authenticated
     * Principals from this Realm.
     *
     * @param principal Principal for whom the role is to be checked
     * @param role Security role to be checked
     */
    public boolean hasRole(Principal principal, String role) {

        // Should be overridden in JAASRealm - to avoid pretty inefficient conversions
        if ((principal == null) || (role == null) ||
            !(principal instanceof GenericPrincipal))
            return (false);

        GenericPrincipal gp = (GenericPrincipal) principal;
        if (!(gp.getRealm() == this)) {
            if (log.isLoggable(Level.FINE)) {
                log.log(Level.FINE, "Different realm " + this + " " + gp.getRealm());
            }
        }
        boolean result = gp.hasRole(role);
        if (log.isLoggable(Level.FINE)) {
            String name = principal.getName();
            if (result) {
                log.log(Level.FINE, USERNAME_HAS_ROLE, new Object[] {name, role});
            }
            else {
                log.log(Level.FINE, USERNAME_NOT_HAVE_ROLE, new Object[] {name, role});
            }
        }
        return (result);

    }

    
    /**
     * Enforce any user data constraint required by the security constraint
     * guarding this request URI.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param constraints Security constraint being checked
     *
     * @exception IOException if an input/output error occurs
     * 
     * @return <code>true</code> if this constraint was not violated and
     * processing should continue, or <code>false</code> if we have created
     * a response already
     */
    public boolean hasUserDataPermission(HttpRequest request,
                                         HttpResponse response,
                                         SecurityConstraint[] constraints)
        throws IOException {
        return hasUserDataPermission(request,response,constraints,null,null);
    }

    /**
     * Checks if the given request URI and method are the target of any
     * user-data-constraint with a transport-guarantee of CONFIDENTIAL,
     * and whether any such constraint is already satisfied.
     * 
     * If <tt>uri</tt> and <tt>method</tt> are null, then the URI and method
     * of the given <tt>request</tt> are checked.
     *
     * If a user-data-constraint exists that is not satisfied, then the 
     * given <tt>request</tt> will be redirected to HTTPS.
     *
     * @param request the request that may be redirected
     * @param response the response that may be redirected
     * @param constraints the security constraints to check against
     * @param uri the request URI (minus the context path) to check
     * @param method the request method to check
     *
     * @return true if the request URI and method are not the target of any
     * unsatisfied user-data-constraint with a transport-guarantee of
     * CONFIDENTIAL, and false if they are (in which case the given request
     * will have been redirected to HTTPS)
     */
    public boolean hasUserDataPermission(HttpRequest request,
                                         HttpResponse response,
                                         SecurityConstraint[] constraints,
                                         String uri,
                                         String method)
        throws IOException {
        // Is there a relevant user data constraint?
        if (constraints == null || constraints.length == 0) {
            if (log.isLoggable(Level.FINE))
                log.log(Level.FINE, "  No applicable security constraint defined");
            return (true);
        }

        for(int i=0; i < constraints.length; i++) {
            SecurityConstraint constraint = constraints[i];
            String userConstraint = constraint.getUserConstraint();
            if (userConstraint == null) {
                if (log.isLoggable(Level.FINE))
                    log.log(Level.FINE, "  No applicable user data constraint defined");
                return (true);
            }
            if (userConstraint.equals(Constants.NONE_TRANSPORT)) {
                if (log.isLoggable(Level.FINE))
                    log.log(Level.FINE, "  User data constraint has no restrictions");
                return (true);
            }

        }

        // Validate the request against the user data constraint
        if (request.getRequest().isSecure()) {
            if (log.isLoggable(Level.FINE))
                log.log(Level.FINE, "  User data constraint already satisfied");
            return (true);
        }

        // Initialize variables we need to determine the appropriate action
        HttpServletRequest hrequest = (HttpServletRequest)
            request.getRequest();
        HttpServletResponse hresponse = (HttpServletResponse)
            response.getResponse();
        int redirectPort = request.getConnector().getRedirectPort();

        // Is redirecting disabled?
        if (redirectPort <= 0) {
            if (log.isLoggable(Level.FINE))
                log.log(Level.FINE, "  SSL redirect is disabled");
            /* S1AS 4878272
            hresponse.sendError
            response.sendError
                (HttpServletResponse.SC_FORBIDDEN,
                 hrequest.getRequestURI());
            */
            // BEGIN S1AS 4878272
            hresponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            response.setDetailMessage(hrequest.getRequestURI());
            // END S1AS 4878272
            return (false);
        }

        // Redirect to the corresponding SSL port
        StringBuilder file = new StringBuilder();
        String protocol = "https";
        String host = hrequest.getServerName();
        // Protocol
        file.append(protocol).append("://").append(host);
        // Host with port
        if(redirectPort != 443) {
            file.append(":").append(redirectPort);
        }
        // URI
        file.append(hrequest.getRequestURI());
        String requestedSessionId = hrequest.getRequestedSessionId();
        if ((requestedSessionId != null) &&
            hrequest.isRequestedSessionIdFromURL()) {
            String sessionParameterName = ((request.getContext() != null)?
                    request.getContext().getSessionParameterName() :
                    Globals.SESSION_PARAMETER_NAME);
            file.append(";" + sessionParameterName + "=");
            file.append(requestedSessionId);
        }
        String queryString = hrequest.getQueryString();
        if (queryString != null) {
            file.append('?');
            file.append(queryString);
        }
        if (log.isLoggable(Level.FINE))
            log.log(Level.FINE, "Redirecting to " + file.toString());
        hresponse.sendRedirect(file.toString());

        return (false);
    }
    
    
    /**
     * Remove a property change listener from this component.
     *
     * @param listener The listener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {

        support.removePropertyChangeListener(listener);

    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {

        lifecycle.addLifecycleListener(listener);

    }


    /**
     * Gets the (possibly empty) list of lifecycle listeners associated
     * with this Realm.
     */
    public List<LifecycleListener> findLifecycleListeners() {
        return lifecycle.findLifecycleListeners();
    }


    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener The listener to remove
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        lifecycle.removeLifecycleListener(listener);
    }

    /**
     * Prepare for the beginning of active use of the public methods of this
     * component.  This method should be called before any of the public
     * methods of this component are utilized.  It should also send a
     * LifecycleEvent of type START_EVENT to any registered listeners.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    public void start() throws LifecycleException {

        // Validate and update our current component state
        if (started) {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.FINE, REALM_BEEN_STARTED);
            }
            return;
        }
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        // Create a MessageDigest instance for credentials, if desired
        if (digest != null) {
            try {
                md = MessageDigest.getInstance(digest);
            } catch (NoSuchAlgorithmException e) {
                String msg = MessageFormat.format(rb.getString(INVALID_ALGORITHM_EXCEPTION),
                                                  digest);
                throw new LifecycleException(msg, e);
            }
        }

    }


    /**
     * Gracefully terminate the active use of the public methods of this
     * component.  This method should be the last one called on a given
     * instance of this component.  It should also send a LifecycleEvent
     * of type STOP_EVENT to any registered listeners.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop()
        throws LifecycleException {

        // Validate and update our current component state
        if (!started) {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, REALM_NOT_BEEN_STARTED);
            }
            return;
        }
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        // Clean up allocated resources
        md = null;
        
        destroy();
    
    }
    
    public void destroy() {
        // no op
    }

    @Override
    public void logout(HttpRequest hreq) {
        // no-op
    }

    @Override
    public boolean isSecurityExtensionEnabled(ServletContext servletContext) {
        return false;
    }

    // ------------------------------------------------------ Protected Methods


    /**
     * Digest the password using the specified algorithm and
     * convert the result to a corresponding hexadecimal string.
     * If exception, the plain credentials string is returned.
     *
     * @param credentials Password or other credentials to use in
     *  authenticating this username
     */
    protected char[] digest(char[] credentials)  {

        // If no MessageDigest instance is specified, return unchanged
        if (hasMessageDigest() == false)
            return (credentials);

        // Digest the user credentials and return as hexadecimal
        synchronized (this) {
            try {
                md.reset();
    
                byte[] bytes = null;
                try {
                    bytes = Utility.convertCharArrayToByteArray(
                            credentials, getDigestEncoding());
                } catch(CharacterCodingException cce) {
                    String msg = MessageFormat.format(rb.getString(ILLEGAL_DIGEST_ENCODING_EXCEPTION),
                                                      getDigestEncoding());
                    log.log(Level.SEVERE, msg, cce);
                        throw new IllegalArgumentException(cce.getMessage());
                }
                md.update(bytes);

                return (HexUtils.convert(md.digest()));
            } catch (Exception e) {
                log.log(Level.SEVERE, ERROR_DIGESTING_USER_CREDENTIAL_EXCEPTION, e);
                return (credentials);
            }
        }

    }

    protected boolean hasMessageDigest() {
        return !(md == null);
    }

    /**
     * Return the digest associated with given principal's user name.
     */
    protected char[] getDigest(String username, String realmName) {
        if (md5Helper == null) {
            try {
                md5Helper = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                log.log(Level.SEVERE, CANNOT_GET_MD5_DIGEST_EXCEPTION, e);
                throw new IllegalStateException(e.getMessage());
            }
        }

    	if (hasMessageDigest()) {
    		// Use pre-generated digest
    		return getPassword(username);
    	}
    	
        char[] pwd = getPassword(username);
        int usernameLength = ((username != null) ? username.length() : 0);
        int realmNameLength = ((realmName != null) ? realmName.length() : 0);
        int pwdLength = ((pwd != null) ? pwd.length : 0);

        // digestValue = username:realmName:pwd
        char[] digestValue = new char[usernameLength + 1 + realmNameLength + 1 + pwdLength];
        int ind = 0;
        if (username != null) {
            System.arraycopy(username.toCharArray(), 0, digestValue, 0, usernameLength);
            ind = usernameLength;
        }
        digestValue[ind++] = ':';
        if (realmName != null) {
            System.arraycopy(realmName.toCharArray(), 0, digestValue, ind, realmNameLength);
            ind += realmNameLength;
        }
        digestValue[ind++] = ':';
        if (pwd != null) {
            System.arraycopy(pwd, 0, digestValue, ind, pwdLength);
        }

        byte[] valueBytes = null;
        try {
            valueBytes = Utility.convertCharArrayToByteArray(
                digestValue, getDigestEncoding());
        } catch(CharacterCodingException cce) {
            String msg = MessageFormat.format(rb.getString(ILLEGAL_DIGEST_ENCODING_EXCEPTION),
                                             getDigestEncoding());
            log.log(Level.SEVERE, msg, cce);
            throw new IllegalArgumentException(cce.getMessage());
        }

        byte[] digest = null;
        // Bugzilla 32137
        synchronized(md5Helper) {
            digest = md5Helper.digest(valueBytes);
        }

        return md5Encoder.encode(digest);
    }


    /**
     * Return a short name for this Realm implementation, for use in
     * log messages.
     */
    protected abstract String getName();


    /**
     * Return the password associated with the given principal's user name.
     */
    protected abstract char[] getPassword(String username);


    /**
     * Return the Principal associated with the given user name.
     */
    protected abstract Principal getPrincipal(String username);


    /**
     * Log a message on the Logger associated with our Container (if any)
     *
     * @param message Message to be logged
     */
    protected void log(String message) {
        org.apache.catalina.Logger logger = null;
        String name = null;
        if (container != null) {
            logger = container.getLogger();
            name = container.getName();
        }
        if (logger != null) {
            logger.log(getName()+"[" + name + "]: " + message);
        } else {
            if (log.isLoggable(Level.INFO)) {
                log.log(Level.INFO, getName()+"[" + name + "]: " + message);
            }
        }
    }


    /**
     * Log a message on the Logger associated with our Container (if any)
     *
     * @param message Message to be logged
     * @param t Associated exception
     */
    protected void log(String message, Throwable t) {
        org.apache.catalina.Logger logger = null;
        String name = null;
        if (container != null) {
            logger = container.getLogger();
            name = container.getName();
        }
        if (logger != null) {
            logger.log(getName()+"[" + name + "]: " + message, t,
                org.apache.catalina.Logger.WARNING);
        } else {
            log.log(Level.WARNING, getName()+"[" + name + "]: " + message, t);
        }
    }
    
    //START SJSAS 6202703
    protected void disableProxyCaching(HttpRequest request,
                                       HttpResponse response, 
                                       boolean disableProxyCaching,
                                       boolean securePagesWithPragma) {
        HttpServletRequest hsrequest = (HttpServletRequest) request.getRequest();
        // Make sure that constrained resources are not cached by web proxies
        // or browsers as caching can provide a security hole
        if (disableProxyCaching
                && !"POST".equalsIgnoreCase(hsrequest.getMethod())
                && (!checkIfRequestIsSecure || !hsrequest.isSecure())) {
            HttpServletResponse sresponse =
                    (HttpServletResponse) response.getResponse();
            if (securePagesWithPragma) {
                // FIXME: These cause problems with downloading office docs
                // from IE under SSL and may not be needed for newer Mozilla
                // clients.
                sresponse.setHeader("Pragma", "No-cache");
                sresponse.setHeader("Cache-Control", "no-cache");
            } else {
                sresponse.setHeader("Cache-Control", "private");
            }
            sresponse.setHeader("Expires", DATE_ONE);
        }
    }
    //END SJSAS 6202703


    // -------------------- JMX and Registration  --------------------

    protected ObjectName controller;

    public ObjectName getController() {
        return controller;
    }

    public void setController(ObjectName controller) {
        this.controller = controller;
    }

    // BEGIN IASRI 4808401, 4934562
    /**
     * Return an alternate principal from the request if available.
     * Tomcat realms do not implement this so always return null as default.
     *
     * @param req The request object.
     * @return Alternate principal or null.
     */
    public Principal getAlternatePrincipal(HttpRequest req) {
        return null;
    }

        
    /**
     * Return an alternate auth type from the request if available.
     * Tomcat realms do not implement this so always return null as default.
     *
     * @param req The request object.
     * @return Alternate auth type or null.
     */
    public String getAlternateAuthType(HttpRequest req) {
        return null;
    }
    // END IASRI 4808401


    // BEGIN IASRI 4856062,4918627,4874504
    /**
     * Set the name of the associated realm.
     *
     * @param name the name of the realm.
     */
    public void setRealmName(String name, String authMethod) {
        // DO NOTHING. PRIVATE EXTENSION
    }


    /**
     * Returns the name of the associated realm.
     *
     * @return realm name or null if not set.
     */
    public String getRealmName(){
        // DO NOTHING. PRIVATE EXTENSION
        return null;
    }
    // END IASRI 4856062,4918627,4874504
    public Principal authenticate(HttpServletRequest hreq) {
        throw new UnsupportedOperationException();
    }


    private boolean equalsIgnoreCase(char[] arr1, char[] arr2) {
        if (arr1 == null) {
            return (arr2 == null);
        } else { // arr1 is not null
            if (arr2 == null || arr1.length != arr2.length) {
                return false;
            }
        }
        
        //here, arr1 and arr2 are not null with equal length
        boolean result = true;
        for (int i = 0; i < arr1.length; i++) {
            if (Character.toLowerCase(arr1[i]) != Character.toLowerCase(arr2[i])) {
                return false;
            }
        }

        return result;
    }
}
