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

package com.sun.common.util.logging;

import org.jvnet.hk2.annotations.Contract;

import java.io.IOException;
import java.util.Map;

/**
 * Interface for Logging Commands
 *
 * @author Naman Mehta
 */


@Contract
public interface LoggingConfig {

    /* set propertyName to be propertyValue.  The logManager
        *  readConfiguration is not called in this method.
        */

    String setLoggingProperty(String propertyName, String propertyValue) throws IOException;

    /* set propertyName to be propertyValue.  The logManager
	*  readConfiguration is not called in this method.
	*/

    String setLoggingProperty(String propertyName, String propertyValue, String targetServer) throws IOException;

    /* update the properties to new values.  properties is a Map of names of properties and
       * their cooresponding value.  If the property does not exist then it is added to the
       * logging.properties file.
       *
       * The readConfiguration method is called on the logManager after updating the properties.
      */

    Map<String, String> updateLoggingProperties(Map<String, String> properties) throws IOException;

    /* update the properties to new values for given target server..  properties is a Map of names of properties and
	 * their cooresponding value.  If the property does not exist then it is added to the
	 * logging.properties file.
	 *
	 * The readConfiguration method is called on the logManager after updating the properties.
	*/

    Map<String, String> updateLoggingProperties(Map<String, String> properties, String targetServer) throws IOException;

    /* get the properties and corresponding values in the logging.properties file for given target server..
        */

    Map<String, String> getLoggingProperties(String targetServer) throws IOException;

    /* get the properties and corresponding values in the logging.properties file.
	*/

    Map<String, String> getLoggingProperties() throws IOException;

    /* creates zip file for given sourceDirectory
        */

    String createZipFile(String sourceDir) throws IOException;

    /* delete the properties from logging.properties file for given target.
      */

    public void deleteLoggingProperties(Map<String, String> properties, String targetConfigName) throws IOException;

    /* delete the properties from logging.properties file. 
      */

    public void deleteLoggingProperties(Map<String, String> properties) throws IOException;

}
