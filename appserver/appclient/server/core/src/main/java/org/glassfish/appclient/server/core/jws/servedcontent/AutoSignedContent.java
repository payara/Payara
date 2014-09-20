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

package org.glassfish.appclient.server.core.jws.servedcontent;

import com.sun.enterprise.util.net.NetUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.jar.Attributes;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents otherwise fixed content that must be automatically signed
 * if it does not yet exist or if the underlying unsigned file has changed
 * since the signed version was created.
 *
 * @author tjquinn
 */
public class AutoSignedContent extends FixedContent {

    private static final String JWS_PERMISSIONS_NAME = "Permissions"; 
    private static final String JWS_PERMISSIONS_VALUE = "all-permissions";
    private static final String JWS_CODEBASE_NAME = "Codebase";
    private static final String JWS_TRUSTED_NAME = "Trusted-Library";
    private static final String JWS_TRUSTED_VALUE = "true";
    private static final String JWS_APP_NAME = "Application-Name";
    
    static Attributes createJWSAttrs(final URI requestURI, final String appName) {
        final Attributes attrs = new Attributes(3);
        attrs.putValue(JWS_PERMISSIONS_NAME, JWS_PERMISSIONS_VALUE);
        attrs.putValue(JWS_APP_NAME, appName);
        try {
            final URI trimmedURI = new URI(requestURI.getScheme(), null /* userInfo */, requestURI.getHost(), requestURI.getPort(), 
                    null /* path */, null /* query */, null /* fragment */);
            attrs.putValue(JWS_CODEBASE_NAME, trimmedURI.toASCIIString());
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
            
        attrs.putValue(JWS_TRUSTED_NAME, JWS_TRUSTED_VALUE);
        return attrs;
    }
    
    private final File unsignedFile;
    private final File signedFile;
    private final String userProvidedAlias;
    private final ASJarSigner jarSigner;
    private final URI relativeURI;
    private final String appName;

    public AutoSignedContent(final File unsignedFile,
            final File signedFile, 
            final String userProvidedAlias,
            final ASJarSigner jarSigner,
            final String relativeURI,
            final String appName) throws FileNotFoundException {
        if ( ! unsignedFile.exists() || ! unsignedFile.canRead()) {
            throw new FileNotFoundException(unsignedFile.getAbsolutePath());
        }
        this.unsignedFile = unsignedFile;
        this.signedFile = signedFile;
        this.userProvidedAlias = userProvidedAlias;
        this.jarSigner = jarSigner;
        this.relativeURI = URI.create(relativeURI);
        this.appName = appName;
    }

    /**
     * Returns a File object for where the signed file will be once it is
     * created.  Note that any use of the File returned by this method MUST
     * be preceded by an invocation of isAvailable.
     *
     * @return File for where the signed copy of the file will reside
     * @throws IOException
     */
    @Override
    public File file() throws IOException {
        return signedFile;
    }
    
    File unsignedFile() {
        return unsignedFile;
    }
    
    String appName() {
        return appName;
    }

    /**
     * Reports whether the signed content is available.  As a side-effect, this
     * method will create the signed file if it does not exist or is obsolete.
     * @return
     * @throws IOException
     */
    @Override
    public boolean isAvailable(final URI requestURI) throws IOException {
        if ( ! isSignedFileReady()) {
            try {
                createSignedFile(requestURI);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }
        return super.isAvailable(requestURI);
    }

    ASJarSigner jarSigner() {
        return jarSigner;
    }
    
    String userProvidedAlias() {
        return userProvidedAlias;
    }
    
        
    private boolean isSignedFileReady() {
        return signedFile.exists() &&
                (signedFile.lastModified() >= unsignedFile.lastModified());
    }

    private void createSignedFile(final URI requestURI) throws Exception {
        /*
         * The code that instantiated this auto-signed content decides where
         * the signed file will reside.  It might not have wanted to create
         * the containing directory ahead of time.
         */
        if ( ! signedFile.getParentFile().exists() && ! signedFile.getParentFile().mkdirs()) {
            final ResourceBundle rb = ResourceBundle.getBundle(getClass().getPackage().getName() + ".LogStrings");
            if (rb != null) {
                throw new IOException(MessageFormat.format(rb.getString("enterprise.deployment.appclient.errormkdirs"),
                        signedFile.getParentFile().getAbsolutePath()));
            }
        }
        final Attributes attrs = createJWSAttrs(requestURI, appName);
        jarSigner.signJar(unsignedFile, signedFile, userProvidedAlias, attrs);
    }

    @Override
    public String toString() {
        return "AutoSignedContent:" + (signedFile == null ? "(stream)" : signedFile.getAbsolutePath());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AutoSignedContent other = (AutoSignedContent) obj;
        if (this.unsignedFile != other.unsignedFile && (this.unsignedFile == null || !this.unsignedFile.equals(other.unsignedFile))) {
            return false;
        }
        if (this.signedFile != other.signedFile && (this.signedFile == null || !this.signedFile.equals(other.signedFile))) {
            return false;
        }
        if ((this.userProvidedAlias == null) ? (other.userProvidedAlias != null) : !this.userProvidedAlias.equals(other.userProvidedAlias)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + (this.unsignedFile != null ? this.unsignedFile.hashCode() : 0);
        hash = 83 * hash + (this.signedFile != null ? this.signedFile.hashCode() : 0);
        hash = 83 * hash + (this.userProvidedAlias != null ? this.userProvidedAlias.hashCode() : 0);
        return hash;
    }
}
