/*
 **********************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICES file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Portions Copyright 2020 Payara Foundation and/or affiliates
 **********************************************************************/
package fish.payara.microprofile.metrics.tck;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.MetricRegistry.Type;
import org.junit.Test;

import fish.payara.microprofile.metrics.impl.MetricRegistryImpl;

/**
 * Unit test variant of MP TCK test of same name {@code org.eclipse.microprofile.metrics.tck.MetricIDTest}.
 *
 * @author Jan Bernitt
 */
public class MetricIDTest {

    private MetricRegistry registry = new MetricRegistryImpl(Type.APPLICATION);

    @SuppressWarnings({ "deprecation", "unused" })
    @Test
    public void removalTest() {

        Tag tagEarth = new Tag("planet", "earth");
        Tag tagRed = new Tag("colour", "red");
        Tag tagBlue = new Tag("colour", "blue");

        String counterName = "org.eclipse.microprofile.metrics.tck.MetricIDTest.counterColour";

        Counter counterColour = registry.counter(counterName);
        Counter counterRed = registry.counter(counterName,tagEarth,tagRed);
        Counter counterBlue = registry.counter(counterName,tagEarth,tagBlue);

        MetricID counterColourMID = new MetricID(counterName);
        MetricID counterRedMID = new MetricID(counterName, tagEarth,tagRed);
        MetricID counterBlueMID = new MetricID(counterName, tagEarth,tagRed);

        //check multi-dimensional metrics are registered
        assertThat("Counter is not registered correctly", registry.getCounter(counterColourMID), notNullValue());
        assertThat("Counter is not registered correctly", registry.getCounter(counterRedMID), notNullValue());
        assertThat("Counter is not registered correctly", registry.getCounter(counterBlueMID), notNullValue());

        //remove one metric
        registry.remove(counterColourMID);
        assertThat("Registry did not remove metric", registry.getCounters().size(), equalTo(2));
        assertThat("Counter is not registered correctly", registry.getCounter(counterColourMID), nullValue());

        //remove all metrics with the given name
        registry.remove(counterName);
        assertThat("Counter is not registered correctly", registry.getCounter(counterRedMID), nullValue());
        assertThat("Counter is not registered correctly", registry.getCounter(counterBlueMID), nullValue());
    }
}
