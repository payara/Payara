/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.serverbeans;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.component.Injectable;

import java.beans.PropertyVetoException;
import org.glassfish.config.support.datatypes.NonNegativeInteger;
import org.glassfish.config.support.datatypes.PositiveInteger;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 *
 */

/* @XmlType(name = "") */

@Configured
@Deprecated
public interface HttpFileCache extends ConfigBeanProxy, Injectable  {

    /**
     * Gets the value of the globallyEnabled property.
     *
     * Globally enables the file cache
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true", dataType=Boolean.class)
    String getGloballyEnabled();

    /**
     * Sets the value of the globallyEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setGloballyEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the fileCachingEnabled property.
     *
     * Enables the caching of file content if the file size is less than the one
     * specified ny med-file-size-limit
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true", dataType=Boolean.class)
    String getFileCachingEnabled();

    /**
     * Sets the value of the fileCachingEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setFileCachingEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the maxAgeInSeconds property.
     *
     * Maximum age of a valid cache entry
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="30")
    @Min(value=0)
    @Max(value=Integer.MAX_VALUE)
    String getMaxAgeInSeconds();

    /**
     * Sets the value of the maxAgeInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setMaxAgeInSeconds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the mediumFileSizeLimitInBytes property.
     *
     * Maximum size of a cached file that can be stored as a memory mapped file.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="537600")
    @Min(value=1)
    @Max(value=Integer.MAX_VALUE)
    String getMediumFileSizeLimitInBytes();

    /**
     * Sets the value of the mediumFileSizeLimitInBytes property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setMediumFileSizeLimitInBytes(String value) throws PropertyVetoException;

    /**
     * Gets the value of the mediumFileSpaceInBytes property.
     *
     * Total size of all files that are cached as memory mapped files.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute  (defaultValue="10485760") //is positive integer enough?
    @Min(value=1)
    @Max(value=Integer.MAX_VALUE)
    String getMediumFileSpaceInBytes();

    /**
     * Sets the value of the mediumFileSpaceInBytes property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setMediumFileSpaceInBytes(String value) throws PropertyVetoException;

    /**
     * Gets the value of the smallFileSizeLimitInBytes property.
     *
     * Maximum size of a file that can be read into memory.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="2048")
    @Min(value=1)
    @Max(value=Integer.MAX_VALUE)
    String getSmallFileSizeLimitInBytes();

    /**
     * Sets the value of the smallFileSizeLimitInBytes property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSmallFileSizeLimitInBytes(String value) throws PropertyVetoException;

    /**
     * Gets the value of the smallFileSpaceInBytes property.
     *
     * Total size of the files that are read into memory.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="1048576")
    @Min(value=1)
    @Max(value=Integer.MAX_VALUE)
    String getSmallFileSpaceInBytes();

    /**
     * Sets the value of the smallFileSpaceInBytes property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSmallFileSpaceInBytes(String value) throws PropertyVetoException;

    /**
     * Gets the value of the fileTransmissionEnabled property.
     *
     * This is valid on Windows only. Enables the TransmitFileSystem call.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false", dataType=Boolean.class)
    String getFileTransmissionEnabled();

    /**
     * Sets the value of the fileTransmissionEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setFileTransmissionEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the maxFilesCount property.
     *
     * Maximum no. of files in the file cache.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="1024")
    @Min(value=1)
    @Max(value=Integer.MAX_VALUE)
    String getMaxFilesCount();

    /**
     * Sets the value of the maxFilesCount property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setMaxFilesCount(String value) throws PropertyVetoException;

    /**
     * Gets the value of the hashInitSize property.
     *
     * Initial no. of hash buckets.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="0")
    @Min(value=0)
    @Max(value=Integer.MAX_VALUE)    
    String getHashInitSize();

    /**
     * Sets the value of the hashInitSize property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setHashInitSize(String value) throws PropertyVetoException;
}
