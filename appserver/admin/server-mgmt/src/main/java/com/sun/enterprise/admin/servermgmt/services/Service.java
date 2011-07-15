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

package com.sun.enterprise.admin.servermgmt.services;
import com.sun.enterprise.util.io.ServerDirs;
import java.util.Map;

/** Represents an abstract Service. This interface defines sufficient methods
 *  for any platform integration of application server with various service
 *  control mechanisms on various platforms. An example is SMF for Solaris.
 * @since SJSAS 9.1
 * @see #isConfigValid
 * @see ServiceHandler
 * @author Kedar Mhaswade
 */
 public interface Service {
    
    /** get the dirs with this thread-safe immutable guaranteed object.
     * It saves a LOT of error checking...
     * You should set the variable in the constructor.  You are not allowed
     * to change it later
     * @param dirs
     */
    ServerDirs  getServerDirs();
    
     int getTimeoutSeconds();
    /** Sets timeout in seconds before the master boot restarter should
     * give up starting this service.
     * @param number a non-negative integer representing timeout. A value of zero implies infinite timeout.
     */
     void setTimeoutSeconds(final int number);
    
    /** Returns the additional properties of the Service.
     * @return String representing addtional properties of the service. May return default properties as well.
     */
     String getServiceProperties();
    
    /** Sets the additional service properties that are specific to it.
     * @param must be a colon separated String, if not null. No effect, if null is passed.
     */
     void setServiceProperties(final String cds);
    
    /** Determines if the configuration of the method is valid. When this class
     * is constructed, appropriate defaults are used. But before attempting to create
     * the service in the Solaris platform, it is important that the necessary
     * configuration is done by the users via various mutator methods of this class.
     * This method must be called to guard against some abnormal failures before
     * creating the service. It makes sure that the caller has set all the necessary
     * parameters reasonably. Note that it does not validate the actual values.
     * @throws RuntimeException if the configuration is not valid
     * @return true if the configuration is valid, an exception is thrown otherwise
     */
     boolean isConfigValid();
    
    /** Returns the tokens and values of the service as a map.
     *  This method converts a service into corresponding tokens and their values.
     * @return tokens and values as a Map<String, String>.
     */
     Map<String, String> tokensAndValues();
    /** Returns the absolute location of the manifest file as service understands it.
     * It takes into account the name, type and configuration location of the 
     * service. It is expected that these are set before calling this method.
     * If the <b> Fully Qualified Service Name </b> is invalid, a RuntimeException results.
     */
     String getManifestFilePath();
    /** Returns the absolute location of the template for the given service.
     * The type of the service must be set before calling this method, otherwise
     * a runtime exception results.
     */
     String getManifestFileTemplatePath();
    /** Creates an arbitrary service, specified by certain parameters. The implementations
     * should dictate the mappings in the parameters received. The creation of service is
     * either successful or not. In other words, the implementations must retain the original
     * state of the operating platform if the service creation is not successful completely.
     * @param params a Map between Strings that represents the name value pairs required to create the service
     * @throws RuntimeException if there is any error is creation of service
     */
     void createService();
    
     String getSuccessMessage();

     void writeReadmeFile(String msg);
     String getLocationArgsStart();
     String getLocationArgsRestart();
     String getLocationArgsStop();
     boolean isDomain();
     boolean isInstance();
     PlatformServicesInfo getInfo();
     void initializeInternal();
     void createServiceInternal();
     void deleteService();
     void deleteServiceInternal();
}
