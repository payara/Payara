/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */

package fish.payara.test.containers.tools.container;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * List of ports usually used by Payara Server (not always).
 *
 * @author David Matejcek
 */
public enum TestablePayaraPort {

    /** HTTP/HTTPS 4848 */
    DAS_ADMIN_PORT(4848),
    /** HTTP 8080 */
    DAS_HTTP_PORT(8080),
    /** HTTP 8181 */
    DAS_HTTPS_PORT(8181),
    /** Hazelcast 4900 */
    DAS_HAZELCAST_PORT(4900),

    /** Instance HTTP port used for first clustered instance running on the same host as DAS */
    CLUSTERED_INSTANCE_HTTP_PORT(28080),
    /** Hazelcast 6900 */
    MICRO_HAZELCAST_PORT(6900),
    //
    ;

    private int port;

    TestablePayaraPort(int port) {
        this.port = port;
    }


    /**
     * @return port number
     */
    public int getPort() {
        return this.port;
    }


    /**
     * Returns the port number.
     */
    @Override
    public String toString() {
        return Integer.toString(getPort());
    }


    /**
     * @return port numbers used by Payara Server. It does not mean it will always listen!
     */
    public static Integer[] getAllPossiblePortValues() {
        return Arrays.stream(values()).map(TestablePayaraPort::getPort).toArray(Integer[]::new);
    }

    /**
     * @return port numbers used by Payara Server.
     */
    public static Integer[] getFullServerPortValues() {
        return Stream.of(DAS_ADMIN_PORT, DAS_HTTP_PORT, DAS_HTTPS_PORT, DAS_HAZELCAST_PORT)
            .map(TestablePayaraPort::getPort).toArray(Integer[]::new);
    }


    /**
     * @return port numbers used by Payara Micro.
     */
    public static Integer[] getMicroPortValues() {
        return Stream.of(DAS_HTTP_PORT, MICRO_HAZELCAST_PORT).map(TestablePayaraPort::getPort)
            .toArray(Integer[]::new);
    }
}
