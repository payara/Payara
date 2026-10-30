/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/main/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

package fish.payara.distributions.docker;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;


/**
 * Generic Payara Container
 *
 * @author David Matejcek
 */
public class PayaraContainer extends GenericContainer<PayaraContainer> {

    /** In container path to passwordfile.txt */
    public static final String PASSWORDFILE_TXT = "/passwordfile.txt";
    private static final String HTTP = "http";
    private static final String HTTPS = "https";


    /**
     * Container created from basic repository name, tag is detected from JVM option
     * - use -Ddocker.payara.tag set to test options (see maven surefire documentation or pom.xml)
     *
     * @param baseRepositoryName
     */
    public PayaraContainer(final String baseRepositoryName) {
        super(baseRepositoryName + ":" + getTagFromJvmOption());
        final String passwordfile = System.getProperty("passwordfile");
        if (passwordfile != null) {
            addEnv("PAYARA_PASSWORD_FILE", PASSWORDFILE_TXT);
            addFileSystemBind(passwordfile, PASSWORDFILE_TXT, BindMode.READ_ONLY);
        }
    }


    /**
     * Converts internal container url to url usable in tests
     *
     * @param port internal port
     * @return i.e. https://localhost:33015
     */
    public URL getHttpsUrl(final int port) {
        return getUrl(HTTPS, port);
    }


    /**
     * Converts internal container url to url usable in tests
     *
     * @param port internal port
     * @return i.e. http://localhost:33015
     */
    public URL getHttpUrl(final int port) {
        return getUrl(HTTP, port);
    }


    private static String getTagFromJvmOption() {
        final String tag = System.getProperty("docker.payara.tag");
        return Objects.requireNonNull(tag, "tag is null");
    }


    private URL getUrl(final String protocol, final int port) {
        final Integer mappedPort = getMappedPort(port);
        if (mappedPort == null) {
            return null;
        }
        try {
            return new URL(protocol + "://" + getContainerIpAddress() + ":" + mappedPort);
        } catch (final MalformedURLException e) {
            throw new IllegalStateException(
                "Could not create mapped url for port " + port + " and protocol " + protocol, e);
        }
    }
}
