/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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


package com.sun.enterprise.glassfish.bootstrap.osgi;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;

/**
 * This class is used to cache vital information of a bundle or a jar file
 * that is used during later processing. It also overrides hashCode and
 * equals methods so that it can be used in various Set operations.
 * It uses file's path as the primary key.
 *
 * @author Sanjeeb.Sahoo@Sun.COM
 */
class Jar
{
    private final URI uri;
    private final long lastModified;
    private final long bundleId;

    Jar(File file)
    {
        // Convert to a URI because the location of a bundle
        // is typically a URI. At least, that's the case for
        // autostart bundles.
        // No need to normalize, because file.toURI() removes unnecessary slashes
        // /tmp/foo and /tmp//foo differently.
        uri = file.toURI();
        lastModified = file.lastModified();
        bundleId = -1L;
    }

    Jar(Bundle b) throws URISyntaxException
    {
        // Convert to a URI because the location of a bundle
        // is typically a URI. At least, that's the case for
        // autostart bundles.
        // Normalisation is needed to ensure that we don't treat (e.g.)
        // /tmp/foo and /tmp//foo differently.
        String location = b.getLocation();
        if (location != null &&
                !location.equals(Constants.SYSTEM_BUNDLE_LOCATION))
        {
            uri = new URI(b.getLocation()).normalize();
        }
        else {
            uri = null;
        }
        
        lastModified = b.getLastModified();
        bundleId = b.getBundleId();
    }

    public Jar(URI uri) {
        this.uri = uri.normalize();
        long localLastModified = -1L;
        bundleId = -1L;
        
        try {
            File f = new File(uri);
            localLastModified = f.lastModified();
        } catch (Exception e) {
            // can't help
        }
        
        lastModified = localLastModified;
    }

    public URI getURI()
    {
        return uri;
    }

    public String getPath() {
        return uri == null ? null : uri.getPath();
    }

    public long getLastModified()
    {
        return lastModified;
    }

    public long getBundleId()
    {
        return bundleId;
    }

    public boolean isNewer(Jar other)
    {
        return (getLastModified() > other.getLastModified());
    }

    // Override hashCode and equals as this object is used in Set
    public int hashCode()
    {
        return uri == null ? 0 : uri.hashCode();
    }

    public boolean equals(Object obj)
    {
        if (obj == null || !(obj instanceof Jar)) return false;
        
        Jar other = (Jar) obj;
        
        if (uri == null) {
            if (other.uri == null) return true;
            return false;
        }
        if (other.uri == null) return false;
            
        // For optimization reason, we use toString.
        // It works, as we anyway use normalize()
        return uri.toString().equals(other.uri.toString());
    }
}
