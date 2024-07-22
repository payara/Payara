/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2017] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package fish.payara.monitoring.rest.app;

/**
 *
 * @author Fraser Savage
 */
public final class RestMonitoringAppResponseToken {

    private static final String REQUEST_KEY = "request";
    private static final String VALUE_KEY = "value";
    private static final String STACKTRACE_KEY = "stacktrace";
    private static final String ERROR_TYPE_KEY = "error_type";
    private static final String ERROR_KEY = "error";
    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String HTTP_STATUS_KEY = "status";

    private static final String MBEAN_NAME_KEY = "mbean";
    private static final String ATTRIBUTE_NAME_KEY = "attribute";
    private static final String REQUEST_TYPE_KEY = "type";
    
    private static final String READ_REQUEST_TOKEN = "read";
    private static final String VERSION_REQUEST_TOKEN = "version";
    
    public static String getRequestKey() {
        return REQUEST_KEY;
    }

    public static String getValueKey() {
        return VALUE_KEY;
    }

    public static String getStacktraceKey() {
        return STACKTRACE_KEY;
    }

    public static String getErrorTypeKey() {
        return ERROR_TYPE_KEY;
    }

    public static String getErrorKey() {
        return ERROR_KEY;
    }

    public static String getTimestampKey() {
        return TIMESTAMP_KEY;
    }

    public static String getHttpStatusKey() {
        return HTTP_STATUS_KEY;
    }

    public static String getMbeanNameKey() {
        return MBEAN_NAME_KEY;
    }

    public static String getAttributeNameKey() {
        return ATTRIBUTE_NAME_KEY;
    }

    public static String getRequestTypeKey() {
        return REQUEST_TYPE_KEY;
    }

    public static String getReadRequestToken() {
        return READ_REQUEST_TOKEN;
    }

    public static String getVersionRequestToken() {
        return VERSION_REQUEST_TOKEN;
    }
}
