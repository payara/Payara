/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 *  Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
 *  file and include the License.
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
package fish.payara.nucleus.hazelcast.test;

import fish.payara.nucleus.hazelcast.DnsDiscoveryService;
import java.util.Iterator;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests that DNS records are returned for a domain name
 * @author jonathan coustick
 * @since 4.1.2.184
 */
public class DNSDiscoveryTest {
    
    //chosen as it has multiple DNS records
    private static final String DOMAIN_YAHOO = "yahoo.com";
    //chosen as it has a single A record
    private static final String DOMAIN_PAYARA = "payara.fish:4900";
    
    @Ignore("Unstable. Env name service dependant")
    @Test
    public void multipleRecordTest() {
        DnsDiscoveryService service = new DnsDiscoveryService(new String[]{DOMAIN_YAHOO});
        Iterator nodes = service.discoverNodes().iterator();
        Assert.assertTrue(nodes.hasNext());
        nodes.next();
        Assert.assertTrue(nodes.hasNext());
        //there nay be more possible, but this is not tested as DNS records are subject to change
    }
    
    @Test
    public void singleRecordTest() {
        DnsDiscoveryService service = new DnsDiscoveryService(new String[]{DOMAIN_PAYARA});
        Iterator nodes = service.discoverNodes().iterator();
        Assert.assertTrue(nodes.hasNext());
        nodes.next();
        Assert.assertFalse(nodes.hasNext());
    }
    
    @Ignore("Unstable. Env name service dependant")
    @Test
    public void multipleDomainTest() {
        DnsDiscoveryService service = new DnsDiscoveryService(new String[]{DOMAIN_PAYARA, DOMAIN_YAHOO});
        Iterator nodes = service.discoverNodes().iterator();
        Assert.assertTrue(nodes.hasNext());
        nodes.next();
        Assert.assertTrue(nodes.hasNext());
        nodes.next();
        Assert.assertTrue(nodes.hasNext());
        
    }
    
}
