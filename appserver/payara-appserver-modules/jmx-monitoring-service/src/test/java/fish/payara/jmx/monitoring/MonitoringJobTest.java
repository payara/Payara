/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.jmx.monitoring;

import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import java.util.ArrayList;
import java.util.List;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author savage
 */
public class MonitoringJobTest {

    private static final MBeanServer TEST_SERVER = getPlatformMBeanServer();
    private static final String THREAD_MBEAN_NAME = "java.lang:type=Threading";
    private static final String THREAD_ATTRIBUTE_NAME = "ThreadCount";
    private static final String HEAP_MBEAN_NAME = "java.lang:type=Memory";
    private static final String HEAP_ATTRIBUTE_NAME = "HeapMemoryUsage";
    private static final String HEAP_SUBATTRIBUTE_NAME = "HeapMemoryUsage.max";
    private static final String MAXHEAP_SUBATTRIBUTE_KEY = "max" + HEAP_ATTRIBUTE_NAME;
    private static final String USEDHEAP_SUBATTRIBUTE_KEY = "used" + HEAP_ATTRIBUTE_NAME;
    private static final String INITHEAP_SUBATTRIBUTE_KEY = "init" + HEAP_ATTRIBUTE_NAME;
    private static final String COMMITTEDHEAP_SUBATTRIBUTE_KEY = "committed" + HEAP_ATTRIBUTE_NAME;

    /**
     * Tests the JMXMonitoringJob is able to get a basic attribute correctly.
     */
    @Test
    public void basicAttributeShouldReturnKeyValueString() {
        ObjectName testMBean;
        
        try { 
            testMBean = new ObjectName(THREAD_MBEAN_NAME);
        } catch (MalformedObjectNameException ex) {
            Assert.fail("While trying to create ObjectName with name " + 
                    THREAD_MBEAN_NAME + " encountered " + ex.getMessage());
            return;
        }

        List<String> testAttributes = new ArrayList<>();
        testAttributes.add(THREAD_ATTRIBUTE_NAME);
        JMXMonitoringJob job;

        try {
            job = new JMXMonitoringJob(testMBean, testAttributes);
        } catch (MalformedObjectNameException ex) {
            Assert.fail("While trying to create MonitoringJob with ObjectName " + 
                    testMBean.getCanonicalName() + " encountered " + ex.getMessage());
            return;
        }

        String[] testTokens = job.getMonitoringInfo(TEST_SERVER).split("=",2);

        try {
            String testAttributeName = testTokens[0];
            Long.valueOf(testTokens[1].trim());
            Assert.assertEquals(THREAD_ATTRIBUTE_NAME, testAttributeName);
        } catch (ArrayIndexOutOfBoundsException ex) {
            Assert.fail("While trying to verify values encountered " + ex.getMessage());
        } catch (NumberFormatException ex) {
            Assert.fail("While getting the Long value for key " + THREAD_ATTRIBUTE_NAME +
                    " encountered " + ex.getMessage());
        }
    }

    /**
     * Tests the JMXMonitoringJob is able to get a composite attribute correctly.
     */
    @Test
    public void compositeAttributeShouldReturnSeriesKeyValueString() {
        ObjectName testMBean;
        boolean max = false, init = false, used = false, committed = false;
        
        try { 
            testMBean = new ObjectName(HEAP_MBEAN_NAME);
        } catch (MalformedObjectNameException ex) {
            Assert.fail("While trying to create ObjectName with name " + 
                    HEAP_MBEAN_NAME + " encountered " + ex.getMessage());
            return;
        }

        List<String> testAttributes = new ArrayList<>();
        testAttributes.add(HEAP_ATTRIBUTE_NAME);
        JMXMonitoringJob job;

        try {
            job = new JMXMonitoringJob(testMBean, testAttributes);
        } catch (MalformedObjectNameException ex) {
            Assert.fail("While trying to create MonitoringJob with ObjectName " + 
                    testMBean.getCanonicalName() + " encountered " + ex.getMessage());
            return;
        }

        String[] testMonitoringInfoStrings = job.getMonitoringInfo(TEST_SERVER).trim().split(" ");
        for (String testString : testMonitoringInfoStrings) {
            String[] testTokens = testString.split("=",2);

            try {
                String testAttributeName = testTokens[0];
                Long.valueOf(testTokens[1].trim());
                switch (testAttributeName) {
                    case MAXHEAP_SUBATTRIBUTE_KEY:
                        max = true; 
                        break;
                    case INITHEAP_SUBATTRIBUTE_KEY:
                        init = true;
                        break;
                    case USEDHEAP_SUBATTRIBUTE_KEY:
                        used = true;
                        break;
                    case COMMITTEDHEAP_SUBATTRIBUTE_KEY:
                        committed = true;
                        break;
                    default:
                        Assert.fail(testAttributeName + " was not expected in the monitoring information.");
                } 
            } catch (ArrayIndexOutOfBoundsException ex) {
                Assert.fail("While trying to verify values encountered " + ex.getMessage());
            } catch (NumberFormatException ex) {
                Assert.fail("While getting the Long value for key " + MAXHEAP_SUBATTRIBUTE_KEY +
                        " encountered " + ex.getMessage());
            }
        }

        if (!max) {
            Assert.fail("Expected " + MAXHEAP_SUBATTRIBUTE_KEY + " in the monitoring information.");
        }
        if (!init) {
            Assert.fail("Expected " + INITHEAP_SUBATTRIBUTE_KEY + " in the monitoring information.");
        }
        if (!used) {
            Assert.fail("Expected " + USEDHEAP_SUBATTRIBUTE_KEY + " in the monitoring information.");
        }
        if (!committed) {
            Assert.fail("Expected " + COMMITTEDHEAP_SUBATTRIBUTE_KEY + " in the monitoring information.");
        }
    }

    /**
     * Tests the JMXMonitoringJob is able to get a sub-attribute of a composite attribute correctly.
     */
    @Test
    public void compositeAttributeSubAttributeShouldReturnKeyValueString() {
        ObjectName testMBean;
        
        try { 
            testMBean = new ObjectName(HEAP_MBEAN_NAME);
        } catch (MalformedObjectNameException ex) {
            Assert.fail("While trying to create ObjectName with name " + 
                    HEAP_MBEAN_NAME + " encountered " + ex.getMessage());
            return;
        }

        List<String> testAttributes = new ArrayList<>();
        testAttributes.add(HEAP_SUBATTRIBUTE_NAME);
        JMXMonitoringJob job;

        try {
            job = new JMXMonitoringJob(testMBean, testAttributes);
        } catch (MalformedObjectNameException ex) {
            Assert.fail("While trying to create MonitoringJob with ObjectName " + 
                    testMBean.getCanonicalName() + " encountered " + ex.getMessage());
            return;
        }

        String testMonitoringInfoString = job.getMonitoringInfo(TEST_SERVER);
        Assert.assertEquals(1, testMonitoringInfoString.trim().split(" ").length);
        String[] testTokens = testMonitoringInfoString.split("=",2);

        try {
            String testAttributeName = testTokens[0];
            Long.valueOf(testTokens[1].trim());
            Assert.assertEquals(MAXHEAP_SUBATTRIBUTE_KEY, testAttributeName);
        } catch (ArrayIndexOutOfBoundsException ex) {
            Assert.fail("While trying to verify values encountered " + ex.getMessage());
        } catch (NumberFormatException ex) {
            Assert.fail("While getting the Long value for key " + MAXHEAP_SUBATTRIBUTE_KEY +
                    " encountered " + ex.getMessage());
        }
    }
   
    /**
     * Tests the JMXMonitoringJob is able to have attributes added to it.
     */
    @Test
    public void shouldAddAttributeToJob() {
        ObjectName testMBean;
        
        try { 
            testMBean = new ObjectName(THREAD_MBEAN_NAME);
        } catch (MalformedObjectNameException ex) {
            Assert.fail("While trying to create ObjectName with name " + 
                    THREAD_MBEAN_NAME + " encountered " + ex.getMessage());
            return;
        }

        List<String> testAttributes = new ArrayList<>();
        JMXMonitoringJob job;

        try {
            job = new JMXMonitoringJob(testMBean, testAttributes);
        } catch (MalformedObjectNameException ex) {
            Assert.fail("While trying to create MonitoringJob with ObjectName " + 
                    testMBean.getCanonicalName() + " encountered " + ex.getMessage());
            return;
        }

        Assert.assertEquals(0, job.getAttributes().size());
        
        job.addAttribute(THREAD_ATTRIBUTE_NAME);

        Assert.assertEquals(true, job.getAttributes().contains(THREAD_ATTRIBUTE_NAME));
    }

    /**
     * Tests the JMXMonitoringJob does not add duplicate attributes to it.
     */
    @Test
    public void shouldNotAddExistingAttributeToJob() {
        ObjectName testMBean;
        
        try { 
            testMBean = new ObjectName(THREAD_MBEAN_NAME);
        } catch (MalformedObjectNameException ex) {
            Assert.fail("While trying to create ObjectName with name " + 
                    THREAD_MBEAN_NAME + " encountered " + ex.getMessage());
            return;
        }

        List<String> testAttributes = new ArrayList<>();
        JMXMonitoringJob job;

        try {
            job = new JMXMonitoringJob(testMBean, testAttributes);
        } catch (MalformedObjectNameException ex) {
            Assert.fail("While trying to create MonitoringJob with ObjectName " + 
                    testMBean.getCanonicalName() + " encountered " + ex.getMessage());
            return;
        }

        Assert.assertEquals(0, job.getAttributes().size());
        job.addAttribute(THREAD_ATTRIBUTE_NAME);
        Assert.assertEquals(true, job.getAttributes().contains(THREAD_ATTRIBUTE_NAME));
        
        int size = job.getAttributes().size();
        job.addAttribute(THREAD_ATTRIBUTE_NAME);
        Assert.assertEquals(true, job.getAttributes().contains(THREAD_ATTRIBUTE_NAME));
        Assert.assertEquals(size, job.getAttributes().size());
    }
}
