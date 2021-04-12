/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package org.glassfish.j2ee.statistics;

/**
 * The Statistic model and its sub-models specify the data models which are requried to be used to provide the performance data described by the specific attributes in the Stats models. 
 */
public interface Statistic {
    /**
     * The name of this Statistic. 
     */
    String getName();

    /**
     * The unit of measurement for this Statistic.
     * Valid values for TimeStatistic measurements are "HOUR", "MINUTE", "SECOND", "MILLISECOND", "MICROSECOND" and "NANOSECOND". 
     */
    String getUnit();

    /**
     * A human-readable description of the Statistic. 
     */
    String getDescription();

    /**
     * The time of the first measurement represented as a long, whose value is the number of milliseconds since January 1, 1970, 00:00:00. 
     */
    long getStartTime();

    /**
     * The time of the last measurement represented as a long, whose value is the number of milliseconds since January 1, 1970, 00:00:00. 
     */
    long getLastSampleTime();
}
