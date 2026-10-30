/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2022-2023] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.metrics.jmx;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class MetricsMetadataTest {

    @Test
    public void isValid_basic() {
        //todo review MetricsMetadata
        MetricsMetadata metadata = new MetricsMetadata("m", "name"
                , "Display", Counter.class.getTypeName(), "description");
        List<XmlTag> tags = new ArrayList<>();
        tags.add(new XmlTag("test", "JUnit"));
        metadata.addTags(tags);
        Assert.assertTrue(metadata.isValid());
    }

    @Test
    public void isValid_EmptyTag() {
        MetricsMetadata metadata = new MetricsMetadata("m", "name", 
                "Display", Counter.class.getTypeName(), "description");
        List<XmlTag> tags = new ArrayList<>();
        tags.add(new XmlTag("test", "JUnit"));
        metadata.addTags(tags);
        Assert.assertTrue(metadata.isValid());
    }

    @Test
    public void isValid_PlaceHolder() {
        MetricsMetadata metadata = new MetricsMetadata("amx:type=jdbc-connection-pool-mon,pp=/mon/server-mon[local-instance],name=resources/%sPool/numconnfree#current"
                , "jdbc.connection.pool.%s.pool.instance.free.connections"
                , "Free Connections (Instance)"
                , Gauge.class.getTypeName(), 
                "The total number of free connections in the pool as of the last sampling");
        List<XmlTag> tags = new ArrayList<>();
        metadata.addTags(tags);
        Assert.assertTrue(metadata.isValid());
    }

    @Test
    public void isValid_PlaceHolderSomeTag() {
        MetricsMetadata metadata = new MetricsMetadata("amx:type=jdbc-connection-pool-mon,pp=/mon/server-mon[local-instance],name=resources/%sPool/numconnfree#current"
                , "jdbc.connection.pool.%s.pool.instance.free.connections"
                , "Free Connections (Instance)"
                ,Gauge.class.getTypeName(), 
                "The total number of free connections in the pool as of the last sampling");
        List<XmlTag> tags = new ArrayList<>();
        tags.add(new XmlTag("test", "JUnit"));
        metadata.addTags(tags);
        Assert.assertTrue(metadata.isValid());
    }

    @Test
    public void isValid_PlaceHolderPlaceHolderTag() {
        MetricsMetadata metadata = new MetricsMetadata("amx:type=jdbc-connection-pool-mon,pp=/mon/server-mon[local-instance],name=resources/%sPool/numconnfree#current"
                , "jdbc.connection.pool.instance.free.connections"
                , "Free Connections (Instance)"
                ,Gauge.class.getTypeName(), "The total number of free connections in the pool as of the last sampling");
        List<XmlTag> tags = new ArrayList<>();
        tags.add(new XmlTag("pool", "%s"));
        metadata.addTags(tags);
        Assert.assertTrue(metadata.isValid());
    }

    @Test
    public void isValid_PlaceHolderEmptyTag() {
        // FISH-5801
        MetricsMetadata metadata = new MetricsMetadata("amx:type=jdbc-connection-pool-mon,pp=/mon/server-mon[local-instance],name=resources/%sPool/numconnfree#current"
                , "jdbc.connection.pool.%s.pool.instance.free.connections"
                , "Free Connections (Instance)"
                ,Gauge.class.getTypeName(), 
                "The total number of free connections in the pool as of the last sampling");
        List<XmlTag> tags = new ArrayList<>();
        tags.add(new XmlTag("test", null));
        metadata.addTags(tags);
        Assert.assertTrue(metadata.isValid());
    }

    @Test
    public void isValid_Service() {
        MetricsMetadata metadata = new MetricsMetadata(
                "jdbc.connection.pool.${attribute}.pool.instance.free.connections"
                , "Free Connections (Instance)"
                , Gauge.class.getTypeName()
                , "The total number of free connections in the pool as of the last sampling");
        metadata.setService("healthcheck-cpool/${attribute}#freeConnection");
        List<XmlTag> tags = new ArrayList<>();
        tags.add(new XmlTag("test", "JUnit"));
        metadata.addTags(tags);
        Assert.assertTrue(metadata.isValid());
    }

}