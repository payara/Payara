/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2021 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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

package fish.payara.samples.http;

import com.gargoylesoftware.htmlunit.WebClient;
import fish.payara.samples.CliCommands;
import fish.payara.samples.PayaraArquillianTestRunner;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Ignore;

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;
import static org.junit.Assert.assertTrue;

/**
 * @author James Hillyard
 */

@RunWith(PayaraArquillianTestRunner.class)
@Ignore("EE10 TODO: Grizzly 4 removed direct attributes for compression")
public class CompressionTest {
    private static final Logger log = Logger.getLogger(CompressionTest.class.getName());

    private static long uncompressedSize;
    private long compressionLevelNegativeOne;
    private long compressionLevelOne;
    private long compressionLevelNine;

    private static WebClient WEB_CLIENT;
    private static final String URL = "http://localhost:8080/";

    private static final String CONFIG_COMPRESSION_STRATEGY =
            "configs.config.server-config.network-config.protocols.protocol.http-listener-1.http.compression-strategy=";
    private static final String CONFIG_COMPRESSION_LEVEL =
            "configs.config.server-config.network-config.protocols.protocol.http-listener-1.http.compression-level=";

    @BeforeClass
    public static void beforeClass() throws Exception {
        WEB_CLIENT = new WebClient();
        WEB_CLIENT.getOptions().setThrowExceptionOnFailingStatusCode(false);
        WEB_CLIENT.getCache().setMaxSize(0); // disable cache
        uncompressedSize = WEB_CLIENT.getPage(URL).getWebResponse().getContentLength();
        log.log(Level.FINE, "********* uncompressedSize = {0}", uncompressedSize);

        CliCommands.payaraGlassFish("set",
                "configs.config.server-config.network-config.protocols.protocol.http-listener-1.http.http2-enabled=false");
        CliCommands.payaraGlassFish("set",
                "configs.config.server-config.network-config.protocols.protocol.http-listener-1.http.compression=on");
        WEB_CLIENT.addRequestHeader("Accept-Encoding", "gzip,deflate");
    }

    //Resets http-listener-1 back to its original state
    @AfterClass
    public static void afterClass() {
        CliCommands.payaraGlassFish("set",
                "configs.config.server-config.network-config.protocols.protocol.http-listener-1.http.http2-enabled=true");
        CliCommands.payaraGlassFish("set",
                "configs.config.server-config.network-config.protocols.protocol.http-listener-1.http.compression=off");
    }

    @Test
    public void defaultCompressionStrategyLevelsTest() throws IOException {
        CliCommands.payaraGlassFish("set", "'" + CONFIG_COMPRESSION_STRATEGY + "Default'");
        getCompressionLevelValues();
        //Check default compression is smaller than uncompressed
        assertTrue(compressionLevelNegativeOne < uncompressedSize);
        //Check fastest compression is bigger than default compression
        assertTrue(compressionLevelOne > compressionLevelNegativeOne);
        //Check fastest compression is bigger than best compression
        assertTrue(compressionLevelOne > compressionLevelNine);
    }

    @Test
    public void filteredCompressionStrategyLevelsTest() throws IOException {
        CliCommands.payaraGlassFish("set", "'" + CONFIG_COMPRESSION_STRATEGY + "Filtered'");
        getCompressionLevelValues();
        //Check default compression is smaller than uncompressed
        assertTrue(compressionLevelNegativeOne < uncompressedSize);
        //Check fastest compression is bigger than default compression
        assertTrue(compressionLevelOne > compressionLevelNegativeOne);
        //Check fastest compression is bigger than best compression
        assertTrue(compressionLevelOne > compressionLevelNine);
    }

    @Test
    public void huffmanCompressionStrategyLevelsTest() throws IOException {
        CliCommands.payaraGlassFish("set", "'" + CONFIG_COMPRESSION_STRATEGY + "Huffman Only'");
        getCompressionLevelValues();
        //Check default compression is smaller than uncompressed
        assertTrue(compressionLevelNegativeOne < uncompressedSize);
        //Huffman Only is not affected by compression level in this test case.
    }

    private void getCompressionLevelValues() throws IOException {
        CliCommands.payaraGlassFish("set", "'" + CONFIG_COMPRESSION_LEVEL + "1'");
        compressionLevelOne = WEB_CLIENT.getPage(URL).getWebResponse().getContentLength();
        log.log(Level.FINE, "********* compressionLevelOne = {0}", compressionLevelOne);

        CliCommands.payaraGlassFish("set", "'" + CONFIG_COMPRESSION_LEVEL + "9'");
        compressionLevelNine = WEB_CLIENT.getPage(URL).getWebResponse().getContentLength();
        log.log(Level.FINE, "********* compressionLevelNine = {0}", compressionLevelNine);

        CliCommands.payaraGlassFish("set", "'" + CONFIG_COMPRESSION_LEVEL + "-1'");
        compressionLevelNegativeOne = WEB_CLIENT.getPage(URL).getWebResponse().getContentLength();
        log.log(Level.FINE, "********* compressionLevelNegativeOne = {0}", compressionLevelNegativeOne);
    }
}
