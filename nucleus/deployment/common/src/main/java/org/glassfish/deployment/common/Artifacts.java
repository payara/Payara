/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.deployment.DeploymentContext;

/**
 * Records information about artifacts (files) that a deployer might need to
 * track.  For example, a deployer might generate artifacts as it runs, and these
 * might need to be cleaned up at some point.  Also, a deployer might need to
 * flag certain files for download to the client as part of "deploy --retrieve" or
 * "get-client-stubs."
 * <p>
 * Artifacts can be recorded into a DeploymentContext or into a Properties object.
 * Storing into a Properties object would normally be to store the Artifacts into
 * the application properties of an Application object so they can be persisted
 * into domain.xml.  The property names are
 * (keyPrefix)Artifact.(partURI) and the value is the corresponding fullURI.
 * <p>
 * Artifacts can also be optionally marked as temporary.  The intent is that
 * such artifacts will be deleted after they are placed into the response
 * payload for download.
 *
 * @author Tim Quinn
 */
public class Artifacts {

    public static final Logger deplLogger = org.glassfish.deployment.common.DeploymentContextImpl.deplLogger;

    /** the actual artifacts tracked - the part URI and the full URI */
    private final Set<FullAndPartURIs> artifacts = new HashSet<FullAndPartURIs>();

    /**
     * used as part of the key in getting/setting transient DC metadata and
     * in defining properties for each of the URI pairs
     */
    private final String keyPrefix;

    /**
     * Returns the Artifacts object from the deployment context with the
     * sepcified key prefix, creating a new one and storing it in the DC if
     * no matching Artifacts object already exists.
     * @param dc the deployment context
     * @param keyPrefix key prefix by which to look up or store the artifacts
     * @return
     */
    public static Artifacts get(
            final DeploymentContext dc,
            final String keyPrefix) {
        final String key = transientAppMetadataKey(keyPrefix);
        synchronized (dc) {
            Artifacts result = dc.getTransientAppMetaData(
                    transientAppMetadataKey(keyPrefix), Artifacts.class);

            if (result == null) {
                result = new Artifacts(keyPrefix);
                dc.addTransientAppMetaData(key, result);
            }
            return result;
        }
    }

    /**
     * Records the Artifacts object into the specified deployment context.
     * @param dc the DeploymentContent in which to persist the Artifacts object
     */
    public void record(final DeploymentContext dc) {
        synchronized (dc) {
            /*
             * Note that "addTransientAppMetaData" actually "puts" into a map,
             * so it's more like a "set" operation.
             */
            dc.addTransientAppMetaData(transientAppMetadataKey(), this);
        }
    }

    /**
     * Gets the artifacts matching the key prefix from the application properties
     * of the specified application.
     * @param app the application of interest
     * @param keyPrefix type of artifacts of interest (e.g., downloadable, generated)
     * @return
     */
    public static Artifacts get(
            final Properties props,
            final String keyPrefix) {
        final Artifacts result = new Artifacts(keyPrefix);

        for (String propName : props.stringPropertyNames()) {
            final String propNamePrefix = propNamePrefix(keyPrefix);
            if (propName.startsWith(propNamePrefix)) {
                /*
                 * The part URI is in the property name, after the keyPrefix and
                 * the separating dot.
                 */
                final URI fullURI = URI.create(props.getProperty(propName));
                result.addArtifact(fullURI, propName.substring(propNamePrefix.length()));
            }
        }
        return result;
    }

    private Artifacts(final String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    private static String propNamePrefix(final String keyPrefix) {
        return keyPrefix + "Artifact.";
    }

    private String propNamePrefix() {
        return propNamePrefix(keyPrefix);
    }

    /**
     * Adds an artifact.
     * @param full the full URI to the file to be tracked
     * @param part the (typically) relative URI to be associated with the part
     * @param isTemporary whether the artifact can be deleted once it is added to an output stream (typically for download)
     * (a frequent use of Artifacts is for working with Payloads which are
     * composed of parts - hence the "part" term)
     */
    public synchronized void addArtifact(URI full, URI part, boolean isTemporary) {
        FullAndPartURIs fullAndPart = new FullAndPartURIs(full, part, isTemporary);
        artifacts.add(fullAndPart);
        if (deplLogger.isLoggable(Level.FINE)) {
            deplLogger.log(Level.FINE, "Added {1} artifact: {0}",
                           new Object[] {fullAndPart, keyPrefix});
        }
    }

    /**
     * Adds an artifact.
     * @param full the full URI to the file to be tracked
     * @param part the (typically) relative URI, expressed as a String, to be
     * associated with the part
     */
    public synchronized void addArtifact(URI full, String part) {
        addArtifact(full, URI.create(part));
    }
    
    public synchronized void addArtifact(final URI full, final String part, final boolean isTemporary) {
        addArtifact(full, URI.create(part), isTemporary);
    }
    
    /**
     * Adds an artifact.
     * @param full
     * @param part 
     */
    public synchronized void addArtifact(final URI full, final URI part) {
        addArtifact(full, part, false);
    }

    /**
     * Adds multiple artifacts at once.
     * @param urisCollection the URI pairs to add
     */
    public synchronized void addArtifacts(Collection<FullAndPartURIs> urisCollection) {
        artifacts.addAll(urisCollection);
        if (deplLogger.isLoggable(Level.FINE)) {
            deplLogger.log(Level.FINE, "Added downloadable artifacts: {0}", urisCollection);
        }
    }

    private static String transientAppMetadataKey(final String prefix) {
        return prefix + "Artifacts";
    }

    private String transientAppMetadataKey() {
        return transientAppMetadataKey(keyPrefix);
    }

    private String propName(final URI partURI) {
        return propNamePrefix() + partURI.toASCIIString();
    }

    private String propValue(final URI fullURI) {
        return fullURI.toASCIIString();
    }

    /**
     * Returns the URI pairs tracked by this Artifacts object.
     * @return
     */
    public synchronized Set<FullAndPartURIs> getArtifacts() {
        return artifacts;
    }

    /**
     * Records the artifacts in the provided Properties object.
     *
     * @param props
     * @throws URISyntaxException
     */
    public synchronized void record(
            final Properties props) throws URISyntaxException {
        for (Artifacts.FullAndPartURIs artifactInfo : artifacts) {
            props.setProperty(
                    propName(artifactInfo.getPart()),
                    propValue(artifactInfo.getFull()));
        }
    }

    /**
     * Clears the URI pairs recorded in this Artifacts object.
     */
    public synchronized void clearArtifacts() {
        artifacts.clear();
    }

    /**
     * Represents a file to be tracked (the full URI) and a relative URI to be
     * associated with that file if is to be downloaded (e.g., as a part in
     * a Payload).
     */
    public static class FullAndPartURIs {
        private final URI full;
        private final URI part;
        private final boolean isTemporary;

        public FullAndPartURIs(URI full, URI part) {
            this(full, part, false);
        }

        public FullAndPartURIs(URI full, String part) {
            this(full, part, false);
        }
        
        public FullAndPartURIs(URI full, String part, boolean isTemporary) {
            this(full, URI.create(part), isTemporary);
        }
        
        public FullAndPartURIs(URI full, URI part, boolean isTemporary) {
            this.full = full;
            this.part = part;
            this.isTemporary = isTemporary;
        }

        public URI getFull() {
            return full;
        }

        public URI getPart() {
            return part;
        }
        
        public boolean isTemporary() {
            return isTemporary;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final FullAndPartURIs other = (FullAndPartURIs) obj;
            if (this.full != other.full && (this.full == null || !this.full.equals(other.full))) {
                return false;
            }
            if (this.part != other.part && (this.part == null || !this.part.equals(other.part))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 29 * hash + (this.full != null ? this.full.hashCode() : 0);
            hash = 29 * hash + (this.part != null ? this.part.hashCode() : 0);
            hash = 29 * hash + (isTemporary ? 0 : 1);
            return hash;
        }

        @Override
        public String toString() {
            return "full URI=" + full + "; part URI=" + part + "; isTemporary=" + isTemporary;
        }
    }
}
