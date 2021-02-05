/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *     The contents of this file are subject to the terms of either the GNU
 *     General Public License Version 2 only ("GPL") or the Common Development
 *     and Distribution License("CDDL") (collectively, the "License").  You
 *     may not use this file except in compliance with the License.  You can
 *     obtain a copy of the License at
 *     https://github.com/payara/Payara/blob/master/LICENSE.txt
 *     See the License for the specific
 *     language governing permissions and limitations under the License.
 *
 *     When distributing the software, include this License Header Notice in each
 *     file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *     GPL Classpath Exception:
 *     The Payara Foundation designates this particular file as subject to the "Classpath"
 *     exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *     file that accompanied this code.
 *
 *     Modifications:
 *     If applicable, add the following below the License Header, with the fields
 *     enclosed by brackets [] replaced by your own identifying information:
 *     "Portions Copyright [year] [name of copyright owner]"
 *
 *     Contributor(s):
 *     If you wish your version of this file to be governed by only the CDDL or
 *     only the GPL Version 2, indicate your decision by adding "[Contributor]
 *     elects to include this software in this distribution under the [CDDL or GPL
 *     Version 2] license."  If you don't indicate a single choice of license, a
 *     recipient has the option to distribute your version of this file under
 *     either the CDDL, the GPL Version 2 or to extend the choice of license to
 *     its licensees as provided above.  However, if you add GPL Version 2 code
 *     and therefore, elected the GPL Version 2 license, then the option applies
 *     only if the new code is made subject to such option by the copyright
 *     holder.
 */
package fish.payara.test.containers.tools.log4j;

import java.util.ArrayList;
import java.util.function.Predicate;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

/**
 * LOG4J Appender - catches log events into a FIFO cache. Logs are later returned and removed via the
 * {@link #pop()} method and can be for example checked. Cache can contain up to
 * {@value #MAX_LOGS_DEFAULT} events at the same time.
 * <p>
 * <b>Usage:</b><br>
 * 1) Setup this appender for some logger:<br>
 * <code>
 * log4j.appender.TEST_LOG_COLLECTOR=fish.payara.test.containers.tools.log4j.EventCollectorAppender
 * log4j.logger.SomeLogger=INFO, TEST_LOG_COLLECTOR
 * </code>
 * <p>
 * 2) Access it in junit test:<br>
 * <code>
 * CollectorAppender appender = (EventCollectorAppender) AUDITLOG.getAppender("TEST_LOG_COLLECTOR");
 * </code>
 * <p>
 * 3) Compare results:<br>
 * <code>
 * assertEquals(..., appender.pop()...);
 * </code>
 * <p>
 * 4) After each junit test drop the collected events (if some remained):</br> <code>
 * appender.clearCache();
 * </code>
 *
 * @author David Matějček
 */
public class EventCollectorAppender extends AppenderSkeleton {

    private static final int MAX_LOGS_DEFAULT = 10000;

    private int capacity;
    private boolean capacityExceedingdReported;
    private Predicate<LoggingEvent> eventFilter = e -> true;
    private ArrayList<LoggingEvent> logs = new ArrayList<>();



    /**
     * Creates new initialized instance.
     */
    public EventCollectorAppender() {
        super();
        this.capacity = MAX_LOGS_DEFAULT;
    }


    /**
     * Set the maximal count of logs that can be hold. If the capacity is reached, further logs will
     * be dropped.
     *
     * @param capacity
     */
    public void setCapacity(final int capacity) {
        this.capacity = capacity;
        this.capacityExceedingdReported = false;
    }


    /**
     * Set the maximal count of logs that can be hold. If the capacity is reached, further logs will
     * be dropped.
     *
     * @param cacheCapacity
     * @return this
     */
    public EventCollectorAppender withCapacity(final int cacheCapacity) {
        this.capacity = cacheCapacity;
        this.capacityExceedingdReported = false;
        return this;
    }


    /**
     * @return the maximal count of logs that can be hold.
     */
    public int getCapacity() {
        return this.capacity;
    }


    /**
     * @param filter filter for filtering incomming events.
     * @return this
     */
    public EventCollectorAppender withEventFilter(final Predicate<LoggingEvent> filter) {
        this.eventFilter = filter;
        return this;
    }


    /**
     * @return filter for filtering incomming events.
     */
    public Predicate<LoggingEvent> getEventFilter() {
        return this.eventFilter;
    }


    /**
     * Returns and deletes the first log message from the FIFO cache.
     *
     * @return the first log event from the cache. Null if empty.
     */
    public LoggingEvent pop() {
        if (this.logs.isEmpty()) {
            return null;
        }
        return this.logs.remove(0);
    }


    /**
     * Cleares all messages from the FIFO cache
     */
    public void clearCache() {
        this.logs.clear();
        this.capacityExceedingdReported = false;
    }


    /**
     * @return count of the logs in cache.
     */
    public int getSize() {
        return this.logs.size();
    }


    @Override
    public void close() {
        this.logs = new ArrayList<>();
    }


    @Override
    public boolean requiresLayout() {
        return false;
    }


    /**
     * If collector cache contains less than #MAX_LOGS_DEFAULT records, puts the logMessage into
     * cache. Otherwise writes a warning to System.err.
     */
    @Override
    protected void append(final LoggingEvent event) {
        if (logs.size() == MAX_LOGS_DEFAULT) {
            if (!this.capacityExceedingdReported) {
                System.err.println("WARN: Log collector overflow. Holding " + getSize() + " logs in memory.");
                this.capacityExceedingdReported = true;
            }
            return;
        }
        if (this.eventFilter.test(event)) {
            logs.add(event);
        }
    }
}
