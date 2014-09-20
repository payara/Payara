/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.cli.remote;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.IOException;

import java.net.CookieStore;
import java.net.URISyntaxException;
import java.net.HttpCookie;
import java.net.URI;
import java.util.List;

/* 
 * This is a derivation of the CookieStore which provides load and store
 * methods which allows the cookies to be stored to and retreived from
 * a file.
 * This CookieStore is specialized for maintaining session cookies for
 * session routing and therefor has side effects that a generalized
 * cookie store would not have.   For example when cookies are stored
 * the URI associated with the cookie is not maintained.
 */
public class ClientCookieStore implements CookieStore {

    private final CookieStore cookieStore;
    private File cookieStoreFile;
    private static final int CACHE_WRITE_DELTA = 60 * 60 * 1000;  // 60 minutes
    private static final String COOKIE_URI = "http://CLI_Session/";
    private static final String CACHE_COMMENT = 
              "# These cookies are used strictly for command routing.\n"
            + "# These cookies are not used for authentication and they are\n"
            + "# not assoicated with server state."; 

    protected URI uri = null;


    public ClientCookieStore(CookieStore cookieStore, File file) {
        this.cookieStore = cookieStore;
        this.cookieStoreFile = file;
    }

    @Override
    public void add(URI uri, HttpCookie cookie) {
        cookieStore.add(uri, cookie);
    }

    @Override
    public List<HttpCookie> get(URI uri) {
        return cookieStore.get(uri);
    }

    @Override
    public List<HttpCookie> getCookies() {
        return cookieStore.getCookies();
    }

    @Override
    public List<URI> getURIs() {
        return cookieStore.getURIs();
    }

    @Override
    public boolean remove(URI uri, HttpCookie cookie) {
        return cookieStore.remove(uri, cookie);
    }

    @Override
    public boolean removeAll() {
        return cookieStore.removeAll();
    }

    public URI getStaticURI() {
        if (uri == null) {
            try {
                uri = new URI(COOKIE_URI);
            } catch (URISyntaxException e) {
                // This should never happen.
            }
        }
        return uri;
    }

    /**
     * Load the persisted cookies into the CookieStore.
     * 
     * The store has this schema:
     * 
     * COOKIE1=xxx; ...
     * COOKIE2=yyy; ...
     **/
    public void load() throws IOException {
        BufferedReader in = null;

        this.removeAll();

        if (!cookieStoreFile.exists()) {
            throw new IOException("File does not exist: " + cookieStoreFile.toString());
        }

        try {
            in = new BufferedReader(new FileReader(cookieStoreFile));
            String str;
            while ((str = in.readLine()) != null) {

                // Ignore comment lines
                if (str.startsWith("#")) continue;

                List<HttpCookie> cookies = HttpCookie.parse(str);
                for (HttpCookie cookie: cookies) {
                    this.add(getStaticURI(), cookie);
                }
            }
        } finally {
            try {
                if (in != null) {
                    in.close();
                } 
            } catch (IOException e) {

            }
        }
    }

        
    /**
     * Store the cookies in the CookieStore to the provided location.
     * This method will overwrite the contents of the target file.
     * 
     **/
    public void store() throws IOException {
        PrintWriter out = null;

        // Create the directory if it doesn't exist.
        if (!cookieStoreFile.getParentFile().exists() &&
                !cookieStoreFile.getParentFile().mkdirs()) {
            throw new IOException("Unable to create directory: " + cookieStoreFile.toString());
        }

        out = new PrintWriter(new BufferedWriter(
                              new FileWriter(cookieStoreFile)));

        // Write comment at top of cache file.
        out.println(CACHE_COMMENT);

        for (URI uri: this.getURIs()) {
            for (HttpCookie cookie: this.get(uri)) {

                // Expire the cookie immediately if Max-Age = 0 or -1
                // Expire the cookie immediately if Discard is true
                if (cookie.getMaxAge() < 1 || cookie.getDiscard()) continue;

                StringBuilder sb = new StringBuilder();

                sb.append(cookie.getName()).append("=").append(cookie.getValue());

                if (cookie.getPath() != null) {
                    sb.append("; Path=").append(cookie.getPath());
                }
                if (cookie.getDomain() != null) {
                    sb.append("; Domain=").append(cookie.getDomain());
                }
                if (cookie.getComment() != null) {
                    sb.append("; Comment=").append(cookie.getComment());
                }
                if (cookie.getCommentURL() != null) {
                    sb.append("; CommentURL=\"").append(cookie.getCommentURL()).append("\"");
                }
                sb.append("; Max-Age=").append(cookie.getMaxAge());
                if (cookie.getPortlist() != null) {
                    sb.append("; Port=\"").append(cookie.getPortlist()).append("\"");
                }

                //XXX: Can we safely ignore Secure attr?
                sb.append("; Version=").append(cookie.getVersion());

                out.println(sb);
            }
        }
        out.close();
    }

    /*
     * Updates the last modification time of the cache file if it is more
     * than CACHE_WRITE_DELTA old.  The file's modification time is use
     * as the creation time for any cookies stored in the cache.   We use
     * this time to determine when to expire cookies.   As an optimization
     * we don't update the file if a reponse has the *same* set-cookies as
     * what's in the file and the file is less than CACHE_WRITE_DELTA old.
     */
    public boolean touchStore() {
        if (cookieStoreFile.lastModified() + CACHE_WRITE_DELTA >
                System.currentTimeMillis()) {
            // The cache is less than CACHE_WRITE_DELTA so we
            // won't update the mod time on the file.
            return false;
        }

        return (cookieStoreFile.setLastModified(System.currentTimeMillis()));
    }
}
