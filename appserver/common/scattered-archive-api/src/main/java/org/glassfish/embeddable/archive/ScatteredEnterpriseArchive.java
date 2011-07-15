/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.embeddable.archive;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstraction for a Scattered Java EE Application.
 * <p/>
 * <p/>
 * Usage example :
 * <p/>
 * <style type="text/css">
 * .ln { color: rgb(0,0,0); font-weight: normal; font-style: normal; }
 * .s0 { color: rgb(128,128,128); }
 * .s1 { }
 * .s2 { color: rgb(0,0,255); }
 * .s3 { color: rgb(128,128,128); font-weight: bold; }
 * .s4 { color: rgb(255,0,255); }
 * </style>
 * <pre>
 * <a name="l56">        GlassFish glassfish = GlassFishRuntime.bootstrap().newGlassFish();
 * <a name="l57">        glassfish.start();
 * <a name="l58">
 * <a name="l59">        </span><span class="s0">// Create a scattered web application.</span><span class="s1">
 * <a name="l60">        ScatteredArchive webmodule = </span><span class="s2">new </span><span class="s1">ScatteredArchive(</span><span class="s4">&quot;testweb&quot;</span><span class="s1">, ScatteredArchive.Type.WAR);
 * <a name="l61">        </span><span class="s0">// target/classes directory contains my complied servlets</span><span class="s1">
 * <a name="l62">        webmodule.addClassPath(</span><span class="s2">new </span><span class="s1">File(</span><span class="s4">&quot;target&quot;</span><span class="s1">, </span><span class="s4">&quot;classes&quot;</span><span class="s1">));
 * <a name="l63">        </span><span class="s0">// resources/sun-web.xml is my WEB-INF/sun-web.xml</span><span class="s1">
 * <a name="l64">        webmodule.addMetadata(</span><span class="s2">new </span><span class="s1">File(</span><span class="s4">&quot;resources&quot;</span><span class="s1">, </span><span class="s4">&quot;sun-web.xml&quot;</span><span class="s1">));
 * <a name="l65">
 * <a name="l66">        </span><span class="s0">// Create a scattered enterprise archive.</span><span class="s1">
 * <a name="l67">        ScatteredEnterpriseArchive archive = </span><span class="s2">new </span><span class="s1">ScatteredEnterpriseArchive(</span><span class="s4">&quot;testapp&quot;</span><span class="s1">);
 * <a name="l68">        </span><span class="s0">// src/application.xml is my META-INF/application.xml</span><span class="s1">
 * <a name="l69">        archive.addMetadata(</span><span class="s2">new </span><span class="s1">File(</span><span class="s4">&quot;src&quot;</span><span class="s1">, </span><span class="s4">&quot;application.xml&quot;</span><span class="s1">));
 * <a name="l70">        </span><span class="s0">// Add scattered web module to the scattered enterprise archive.</span><span class="s1">
 * <a name="l71">        </span><span class="s0">// src/application.xml references Web module as &quot;scattered.war&quot;. Hence specify the name while adding the archive.</span><span class="s1">
 * <a name="l72">        archive.addArchive(webmodule.toURI(), </span><span class="s4">&quot;scattered.war&quot;</span><span class="s1">);
 * <a name="l73">        </span><span class="s0">// lib/mylibrary.jar is a library JAR file.</span><span class="s1">
 * <a name="l74">        archive.addArchive(</span><span class="s2">new </span><span class="s1">File(</span><span class="s4">&quot;lib&quot;</span><span class="s1">, </span><span class="s4">&quot;mylibrary.jar&quot;</span><span class="s1">));
 * <a name="l75">        </span><span class="s0">// target/ejbclasses contain my compiled EJB module.</span><span class="s1">
 * <a name="l76">        </span><span class="s0">// src/application.xml references EJB module as &quot;ejb.jar&quot;. Hence specify the name while adding the archive.</span><span class="s1">
 * <a name="l77">        archive.addArchive(</span><span class="s2">new </span><span class="s1">File(</span><span class="s4">&quot;target&quot;</span><span class="s1">, </span><span class="s4">&quot;ejbclasses&quot;</span><span class="s1">), </span><span class="s4">&quot;ejb.jar&quot;</span><span class="s1">);
 * <a name="l78">
 * <a name="l79">        Deployer deployer = glassfish.getDeployer();
 * <a name="l80">        </span><span class="s0">// Deploy my scattered web application</span><span class="s1">
 * <a name="l81">        deployer.deploy(webmodule.toURI());
 * </pre>
 *
 * @author bhavanishankar@java.net
 */
public class ScatteredEnterpriseArchive {

    String name;
    static final String type = "ear";
    Map<String, File> archives = new HashMap<String, File>();
    Map<String, File> metadatas = new HashMap<String, File>();

    /**
     * Construct a new scattered enterprise archive.
     *
     * @param name Name of the enterprise archive.
     * @throws NullPointerException if name is null.
     */
    public ScatteredEnterpriseArchive(String name) {
        if (name == null) {
            throw new NullPointerException("name must not be null.");
        }
        this.name = name;
    }

    /**
     * Add a module or a library to this scattered enterprise archive.
     * <p/>
     * The addArchive(archiveURI) method has the same effect as:
     * <pre>
     *      addMetadata(archiveURI, null)
     * </pre>
     * Follows the same semantics as {@link #addArchive(URI, String)} method.
     */
    public void addArchive(URI archiveURI) throws IOException {
        addArchive(archiveURI, null);
    }

    /**
     * Add a module or a library to this scattered enterprise archive.
     * <p/>
     * The specified archiveURI must be one of the following:
     * <pre>
     *      ScatteredArchive URI obtained via {@link ScatteredArchive#toURI()}.
     *      Location of a library JAR file. Must be a File URI.
     *      Location of a Java EE module. Must be a File URI.
     * </pre>
     * If the specified name is null, then the name is computed as the name of the
     * File as located by archiveURI.
     *
     * @param archiveURI Module or library archive URI.
     * @param name       name of the module/library as specified in META-INF/application.xml
     * @throws NullPointerException if archiveURI is null
     * @throws IOException          if the archiveURI location is not found.
     */
    public void addArchive(URI archiveURI, String name) throws IOException {
        addArchive(archiveURI != null ? new File(archiveURI) : null, name);
    }

    /**
     * Add a module or a library to this scattered enterprise archive.
     * <p/>
     * The addArchive(archive) method has the same effect as:
     * <pre>
     *      addArchive(archive, null)
     * </pre>
     * Follows the same semantics as {@link #addArchive(File, String)} method.
     * archive must be a file location.
     */
//    public void addArchive(String archive) {
//        addArchive(archive, null);
//    }

    /**
     * Add a module or a library to this scattered enterprise archive.
     * <p/>
     * Follows the same semantics as {@link #addArchive(File, String)} method.
     * archive must be a file location.
     */
//    public void addArchive(String archive, String name) {
//        addArchive(archive != null ? new File(archive) : null, name);
//    }

    /**
     * Add a module or a library to this scattered enterprise archive.
     * <p/>
     * The addArchive(archive) method has the same effect as:
     * <pre>
     *      addArchive(archive, null)
     * </pre>
     * Follows the same semantics as {@link #addArchive(File, String)} method.
     */
    public void addArchive(File archive) throws IOException {
        addArchive(archive, null);
    }

    /**
     * Add a module or a library to this scattered enterprise archive.
     * <p/>
     * The specified archive location should be one of the following:
     * <pre>
     *      Location of a library JAR file.
     *      Location of a Java EE module.
     * </pre>
     * If the specified name is null, then the name is computed as archive.getName()
     *
     * @param archive Location of module or library archive.
     * @param name    name of the module/library as specified in META-INF/application.xml
     * @throws NullPointerException if archive is null
     * @throws IOException          if the archive file is not found
     */
    public void addArchive(File archive, String name) throws IOException {
        if (archive == null) {
            throw new NullPointerException("archive must not be null.");
        }
        if (!archive.exists()) {
            throw new FileNotFoundException(archive + " does not exist.");
        }
//        if (archive.isDirectory()) {
//            throw new IllegalArgumentException(archive + " is a directory.");
//        }
        if (name == null) {
            name = archive.getName();
        }
        this.archives.put(name, archive);
    }

    /**
     * Add a new metadata to this scattered enterprise archive.
     * <p/>
     * The addMetadata(metadata) method has the same effect as:
     * <pre>
     *      addMetadata(metadata, null)
     * </pre>
     * Follows the same semantics as {@link #addMetadata(String, String)} method.
     */
//    public void addMetadata(String metadata) {
//        addMetadata(metadata, null);
//    }

    /**
     * Add a new metadata to this scattered enterprise archive.
     * <p/>
     * The addMetadata(metadata) method has the same effect as:
     * <pre>
     *      addMetadata(metadata, null)
     * </pre>
     * Follows the same semantics as {@link #addMetadata(File, String)} method.
     */
    public void addMetadata(File metadata) throws IOException {
        addMetadata(metadata, null);
    }

    /**
     * Add a new metadata to this enterprise archive.
     * <p/>
     * Follows the same semantics as {@link #addMetadata(File, String)} method.
     * metatdata must be a file location.
     */
//    public void addMetadata(String metadata, String name) {
//        addMetadata(metadata != null ? new File(metadata) : null, name);
//    }

    /**
     * Add a new metadata to this enterprise archive.
     * <p/>
     * A metadata is identified by its name (e.g., META-INF/application.xml)
     * If the specified name is null, then the name is computed as
     * "META-INF/" + metadata.getName()
     * <p/>
     * If the scattered enterprise archive already contains the metadata with
     * the same name, the old value is replaced.
     *
     * @param metadata location of metdata.
     * @param name     name of the metadata (e.g., META-INF/application.xml)
     * @throws NullPointerException     if metadata is null
     * @throws IOException              if metadata is not found
     * @throws IllegalArgumentException if metadata is a directory.
     */

    public void addMetadata(File metadata, String name) throws IOException {
        if (metadata == null) {
            throw new NullPointerException("metadata must not be null.");
        }
        if (!metadata.exists()) {
            throw new IOException(metadata + " does not exist.");
        }
        if (metadata.isDirectory()) {
            throw new IllegalArgumentException(metadata + " is a directory.");
        }
        if (name == null) {
            name = "META-INF/" + metadata.getName();
        }
        this.metadatas.put(name, metadata);
    }

    /**
     * Get the deployable URI for this scattered enterprise archive.
     * <p/>
     * <i>Note : java.io.tmpdir is used while building the URI.</i>
     *
     * @return Deployable scattered enterprise Archive URI.
     * @throws IOException if any I/O error happens while building the URI
     *                     or while reading metadata, archives.
     */
    public URI toURI() throws IOException {
        return new Assembler().assemble(this);
    }
}
