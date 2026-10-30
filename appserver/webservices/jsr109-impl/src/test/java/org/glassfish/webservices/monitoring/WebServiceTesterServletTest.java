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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
package org.glassfish.webservices.monitoring;

import org.junit.Assert;
import org.junit.Test;

public class WebServiceTesterServletTest {

    @Test
    public void testIpv4Regex() {
        // Loopback should work
        Assert.assertTrue(WebServiceTesterServlet.checkValidIpv4DottedDecimal("127.0.0.1"));

        // Typical default should work
        Assert.assertTrue(WebServiceTesterServlet.checkValidIpv4DottedDecimal("192.168.1.1"));

        // Empty string should fail
        Assert.assertFalse(WebServiceTesterServlet.checkValidIpv4DottedDecimal(""));

        // DNS name should fail
        Assert.assertFalse(WebServiceTesterServlet.checkValidIpv4DottedDecimal("payara.fish"));

        // All 0's allowed
        Assert.assertTrue(WebServiceTesterServlet.checkValidIpv4DottedDecimal("0.0.0.0"));

        // No 0 prefixes
        Assert.assertFalse(WebServiceTesterServlet.checkValidIpv4DottedDecimal("012.168.35.64"));

        // 0 suffixes allowed
        Assert.assertTrue(WebServiceTesterServlet.checkValidIpv4DottedDecimal("102.120.35.240"));

        // Numbers greater than 255 not allowed
        Assert.assertFalse(WebServiceTesterServlet.checkValidIpv4DottedDecimal("192.168.43.256"));
        Assert.assertFalse(WebServiceTesterServlet.checkValidIpv4DottedDecimal("192.168.43.1234"));

        // Must be 4 segments
        Assert.assertFalse(WebServiceTesterServlet.checkValidIpv4DottedDecimal("192"));
        Assert.assertFalse(WebServiceTesterServlet.checkValidIpv4DottedDecimal("192.168"));
        Assert.assertFalse(WebServiceTesterServlet.checkValidIpv4DottedDecimal("192.168.43"));
    }

    @Test
    public void testDnsNameRegex() {
        // Standalone name allowed e.g. "localhost"
        Assert.assertTrue(WebServiceTesterServlet.checkValidDnsName("payara"));

        // "." allowed to denote segments
        Assert.assertTrue(WebServiceTesterServlet.checkValidDnsName("payara.fish"));

        // "-" is allowed
        Assert.assertTrue(WebServiceTesterServlet.checkValidDnsName("payara-jaxws"));

        // "-" is allowed, and "." allowed to denote segments
        Assert.assertTrue(WebServiceTesterServlet.checkValidDnsName("payara-jaxws.fish"));

        // Letters and numbers allowed
        Assert.assertTrue(WebServiceTesterServlet.checkValidDnsName("payara2021"));

        // Multiple segments allowed, and letters & numbers allowed
        Assert.assertTrue(WebServiceTesterServlet.checkValidDnsName("payara.2021.swims"));

        // "-" is allowed, "." allowed to denote segments, and so is a trailing "."
        Assert.assertTrue(WebServiceTesterServlet.checkValidDnsName("payara-jaxws.fish."));

        // Double trailing "." not allowed
        Assert.assertFalse(WebServiceTesterServlet.checkValidDnsName("payara-jaxws.fish.."));
        // No non-ASCII alphanumerics allowed
        Assert.assertFalse(WebServiceTesterServlet.checkValidDnsName("faß.de"));
        // No symbol other than "-" or "." allowed
        Assert.assertFalse(WebServiceTesterServlet.checkValidDnsName("malicious/payload.wsdl"));
        Assert.assertFalse(WebServiceTesterServlet.checkValidDnsName("malicious/payload.wsdl#"));
    }

}
