/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
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
package fish.payara.deployment.util;

import com.sun.enterprise.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * General URI manipulation utilities.
 *
 * @author avpinchuk
 */
public final class URIUtils {
    // Suppress default constructor
    private URIUtils() {
        throw new AssertionError();
    }

    /**
     * Opens a connection to the {@code uri} and returns an {@code InputStream} for reading
     * from that connection.
     * <p/>
     * If {@code uri} represents an HTTP(S) resource and contains the user info part, set the
     * <em>Authorization</em> header with Basic authentication credentials.
     *
     * @param uri the {@code URI} to returns {@code InputStream} from.
     * @return An input stream for reading from the {@code URI}.
     * @throws IOException if an I/O exception occurs.
     * @throws ClassCastException if the {@code uri} is not a file and does not represent HTTP(S) resource.
     */
    public static InputStream openStream(URI uri) throws IOException {
        if (hasFileScheme(uri)) {
            return Files.newInputStream(Paths.get(uri));
        }
        return openHttpConnection(uri).getInputStream();
    }

    /**
     * Returns a {@link HttpURLConnection} instance that represents a connection to the
     * remote object referred by the {@code uri}.
     * <p/>
     * The {@code uri} must represent a HTTP(S) resource.
     * <p/>
     * If {@code uri} contains the user info part, set the <em>Authorization</em> header
     * with Basic authentication credentials.
     *
     * @param uri the {@code URI} to open connection from.
     * @return A {@code HttpURLConnection} to the {@code uri}.
     * @throws IOException if an I/O exception occurs.
     * @throws ClassCastException if the connection is not instance of {@code HttpURLConnection}.
     */
    private static HttpURLConnection openHttpConnection(URI uri) throws IOException {
        URL url = uri.toURL();
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        String userInfo = url.getUserInfo();
        if (userInfo != null) {
            String encodedUserInfo = Base64.getEncoder().encodeToString(userInfo.getBytes());
            httpConnection.setRequestProperty("Authorization", "Basic " + encodedUserInfo);
        }
        return httpConnection;
    }

    /**
     * Downloads {@code uri} to a temporary file.
     *
     * @param uri the URI to download.
     * @return The temporary file.
     * @throws IOException if an I/O exception occurs.
     * @throws ClassCastException if the {@code uri} is not a file and does not represent HTTP(S) resource.
     */
    public static File convertToFile(URI uri) throws IOException {
        if (hasFileScheme(uri)) {
            return new File(uri);
        }

        HttpURLConnection connection = openHttpConnection(uri);
        try {
            return FileUtils.createTempFile(connection.getInputStream(), "app", "tmp");
        } catch (IOException e) {
            try (InputStream err = connection.getErrorStream()) {
                if (err != null) {
                    // Dry error stream
                    FileUtils.copy(err, new OutputStream() {
                        @Override
                        public void write(int b) {
                            // ignore
                        }
                    }, 0L);
                }
            } catch (IOException ex) {
                // ignore
            }
            // Rethrow original exception
            throw e;
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Tests whether resource represented by {@code uri} exists.
     *
     * @param uri the URI to test.
     * @return {@code true} if a resource exists; {@code false} otherwise.
     * @throws IOException if an I/O error occurs.
     * @throws ClassCastException if the {@code uri} is not a file and does not represent HTTP(S) resource.
     */
    public static boolean exists(URI uri) throws IOException {
        if (hasFileScheme(uri)) {
            return Files.exists(Paths.get(uri));
        }

        HttpURLConnection connection = openHttpConnection(uri);
        connection.setRequestMethod("HEAD");

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return true;
        }

        connection.disconnect();

        return false;
    }

    /**
     * Tests whether {@code uri} has the {@code file} scheme.
     *
     * @param uri the URI to test.
     * @return {@code true} if {@code uri} has the {@code file} scheme; {@code false} otherwise.
     */
    public static boolean hasFileScheme(URI uri) {
        return "file".equalsIgnoreCase(uri.getScheme());
    }
}
