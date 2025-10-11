/*
 * Copyright (c) [2021] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package org.glassfish.web.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.jar.JarFile;

/**
 * Wraps JarURLConnection to force no cache usage,
 * which can bring in side effects
 *
 * @author lprimak
 */
public class NonCachedJarStreamHandler extends URLStreamHandler {
    private static final NonCachedJarStreamHandler INSTANCE = new NonCachedJarStreamHandler();

    /**
     * JDK caches JarFiles, which causes unwanted side effects
     * in multi-threaded environments.
     * force URLs returned from the class loader to not used the cache
     *
     * @param url
     * @return
     */
    public static URL forceNonCachedJarURL(URL url) {
        if (url == null) {
            return null;
        } else if (!"jar".equals(url.getProtocol())) {
            return url;
        }
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<URL>)
                    () -> new URL(url, url.toExternalForm(), INSTANCE));
        } catch (PrivilegedActionException ex) {
            throw new IllegalArgumentException(ex.getException());
        }
    }

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        return new JarURLConnection(u) {
            private JarURLConnection urlConnection;

            @Override
            public JarFile getJarFile() throws IOException {
                connect();
                return urlConnection.getJarFile();
            }

            @Override
            public void connect() throws IOException {
                if (urlConnection == null) {
                    // set our own flag for consistency's sake and easier debugging,
                    // even though it is not used
                    setUseCaches(false);
                    URLConnection conn = new URL(u.toExternalForm()).openConnection();
                    conn.setUseCaches(false);
                    urlConnection = (JarURLConnection) conn;
                }
            }

            @Override
            public InputStream getInputStream() throws IOException {
                connect();
                return urlConnection.getInputStream();
            }
        };
    }
}
