/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.api.deployment.archive;

import org.jvnet.hk2.annotations.Contract;


/**
 * ArchiveType is an extension over ModuleType defined in jsr88 API.
 * It is analogous to type of an archive  or a module or deployment unit, whichever way you prefer to call them.
 * Adding a new archive type (or ArchiveType) is a very expensive operation.
 * For example, there has been no new archive types introduced in Java EE since RAR type.
 * Adding a new archive type involves writing an ArchiveHandler which involves writing logic to create class loaders.
 * Now, that's not same as adding adding a technology type like jersey or jpa.
 * <p/>
 * This is only a contract. Actual types are made available as services by appropriate containers.
 * <p/>
 * GlassFish deployment framework uses the word container to refer to services. Containers are actually defined in
 * Java EE platform spec. ArchiveType maps to the way containers are defined in the Java EE platform spec.
 *
 * @author Sanjeeb Sahoo
 */
@Contract
@javax.inject.Singleton
public abstract class ArchiveType {
    /**
     * File extension for this type of archive. Empty string is used if there is no extension specified.
     */
    private String extension;

    /**
     * String value of this type
     */
    private String value;

    /**
     * Return the file extension string for this module type.
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Ensure a correct value is passed as that's what is returned by {@link #toString()} which is sometimes used during
     * comparison as some old APIs like {@link ArchiveHandler#getArchiveType()} use String.
     *
     * @param value     value of this archive type as reported in {@link #toString()}
     * @param extension file extension for this type of archive
     */
    protected ArchiveType(String value, String extension) {
        this.extension = extension;
        this.value = value;
    }

    /**
     * Same as calling #ArchiveType(String, String) with an empty string as extension.
     */
    protected ArchiveType(String value) {
        this(value, "");
    }

    /**
     * @return the string equivalent of this type.
     */
    @Override
    public final String toString() {
        return value;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArchiveType) {
            return toString().equals(obj.toString());
        }
        return false;
    }
}
