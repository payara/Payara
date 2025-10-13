/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

// Portions Copyright [2019] [Payara Foundation and/or its affiliates]

package com.sun.common.util.logging;

import java.io.IOException;
import java.util.Map;

import org.jvnet.hk2.annotations.Contract;

/**
 * Handles I/O for a logging file.
 *
 * @author Naman Mehta
 */
@Contract
public interface LoggingConfig {

    /**
     * Initializes the configuration for a given target.
     * @param target the target to fetch the logs from.
     * @throws IOException if an error occurred while reading from the managed file.
     */
    void initialize(String target) throws IOException;

    /**
     * Gets a given property from the managed file.
     * 
     * @param propertyName the name of the property to get.
     * @return the value of the property, or null if it doesn't exist.
     * @throws IOException if an error occurred while reading from the managed file.
     */
    String getLoggingProperty(String propertyName) throws IOException;

    /**
     * Get all properties from the managed file.
     * 
     * @return all properties contained within the managed file.
     * @throws IOException if an error occurred while reading from the managed file.
     */
    Map<String, String> getLoggingProperties() throws IOException;

    /**
     * Get all properties from the managed file.
     * 
     * @param usePlaceHolderReplacement whether to 
     * @return all properties contained within the managed file.
     * @throws IOException if an error occurred while reading from the managed file.
     */
    Map<String, String> getLoggingProperties(boolean usePlaceholderReplacement) throws IOException;

    /**
     * Sets the given property within the managed file.
     * 
     * @param propertyName  the name of the property to set.
     * @param propertyValue the value of the property to set.
     * @return the property value in the file.
     * @throws IOException if an error occurred while writing to the managed file.
     */
    String setLoggingProperty(String propertyName, String propertyValue) throws IOException;

    /**
     * Sets all properties within the managed file.
     * 
     * @return all properties to set within the managed file.
     * @throws IOException if an error occurred while writing to the managed file.
     */
    Map<String, String> setLoggingProperties(Map<String, String> props) throws IOException;

    /**
     * Deletes all properties from the provided list.
     * 
     * @param props all properties to delete within the managed file.
     * @return the list of deleted properties.
     * @throws IOException if an error occurred while writing to the managed file.
     */
    Map<String, String> deleteLoggingProperties(Map<String, String> props) throws IOException;

    /**
     * @return the file for the GFHandler to log to.
     * @throws IOException if an error occurred while reading from the managed file.
     */
    String getLoggingFileDetails() throws IOException;

    /**
     * Creates a ZIP file from a given directory.
     * 
     * @param sourceDir the directory to ZIP.
     * @throws IOException if an error occurred while creating the ZIP.
     */
    String createZipFile(String sourceDir) throws IOException;

}
