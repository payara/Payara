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
 * Specifies the statistics provided by a JMS session. 
 *
 * @author Hans Hrasna
 */
public interface JMSSessionStats extends Stats {

     /**
     * Returns an array of JMSProducerStats that provide statistics about the message
     * producers associated with the referencing JMS session statistics.
     */
    JMSProducerStats[] getProducers();

    /**
     * Returns an array of JMSConsumerStats that provide statistics about the message
     * consumers associated with the referencing JMS session statistics. 
     */
    JMSConsumerStats[] getConsumers();

    /**
     * Number of messages exchanged. 
     */
    CountStatistic getMessageCount();

    /**
     * Number of pending messages. 
     */
    CountStatistic getPendingMessageCount();

    /**
     * Number of expired messages. 
     */
    CountStatistic getExpiredMessageCount();

    /**
     * Time spent by a message before being delivered. 
     */
    TimeStatistic getMessageWaitTime();

    /**
     * Number of durable subscriptions. 
     */
    CountStatistic getDurableSubscriptionCount();
}
