/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *    Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.logging.jul.internal;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * @author David Matejcek
 */
public class LogRecordBuffer {

    private final int capacity;
    private final int maxWait;
    private final ArrayBlockingQueue<EnhancedLogRecord> pendingRecords;


    /**
     * The buffer for log records.
     * <p>
     * If it is full and another record is comming to the buffer, the record will wait until the
     * buffer would have a free capacity, maybe forever.
     * <p>
     * See also the another constructor.
     *
     * @param capacity capacity of the buffer.
     */
    public LogRecordBuffer(final int capacity) {
        this(capacity, 0);
    }


    /**
     * The buffer for log records.
     * <p>
     * If it is full and another record is comming to the buffer, the record will wait until the
     * buffer would have a free capacity, but only for a maxWait seconds.
     * <p>
     * If the buffer would not have free capacity even after the maxWait time, the buffer will be
     * automatically cleared, the incomming record will be lost and there will be a stacktrace in
     * standard error output - but that may be redirected to JUL again, so this must be reliable.
     * <ul>
     * <li>After this error handling procedure the logging will be available again in full capacity
     * but it's previous unprocessed log records would be lost.
     * <li>If the maxWait is lower than 1, the calling thread would be blocked until some records would
     * be processed. It may remain blocked forever.
     * </ul>
     *
     * @param capacity capacity of the buffer.
     * @param maxWait maximal time in seconds to wait for the free capacity. If &lt; 1, can wait
     *            forever.
     */
    public LogRecordBuffer(final int capacity, final int maxWait) {
        this.capacity = capacity;
        this.maxWait = maxWait;
        this.pendingRecords = new ArrayBlockingQueue<>(capacity);
    }


    /**
     * @return true if there are not pending records to provide.
     */
    public boolean isEmpty() {
        return this.pendingRecords.isEmpty();
    }


    public int getSize() {
        return this.pendingRecords.size();
    }


    public int getCapacity() {
        return this.capacity;
    }


    /**
     * Waits for a record or thread interrupt signal
     *
     * @return {@link EnhancedLogRecord} or null if interrupted.
     */
    public EnhancedLogRecord pollOrWait() {
        try {
            return this.pendingRecords.take();
        } catch (final InterruptedException e) {
            return null;
        }
    }


    public EnhancedLogRecord poll() {
        return this.pendingRecords.poll();
    }


    public void add(final EnhancedLogRecord record) {
        if (maxWait > 0) {
            addWithTimeout(record);
        } else {
            addWithUnlimitedWaiting(record);
        }
    }


    /**
     * This prevents deadlock - when the waiting is not successful, it forcibly drops all waiting records.
     * Logs an error after that.
     */
    private void addWithTimeout(final EnhancedLogRecord record) {
        try {
            if (this.pendingRecords.offer(record)) {
                return;
            }
            Thread.yield();
            if (this.pendingRecords.offer(record, this.maxWait, TimeUnit.SECONDS)) {
                return;
            }
        } catch (final InterruptedException e) {
            // do nothing
        }

        this.pendingRecords.clear();
        // note: the record is not meaningful for the message. The cause is in another place.
        this.pendingRecords.offer(new EnhancedLogRecord(Level.SEVERE, //
            this + ": The buffer was forcibly cleared after " + maxWait + " s timeout for adding another log record." //
                + " Log records were lost." //
                + " It might be caused by a recursive deadlock," //
                + " you can increase the capacity or the timeout to avoid this."));
    }


    /**
     * This prevents losing any records, but may end up in deadlock if the capacity is reached.
     */
    private void addWithUnlimitedWaiting(final EnhancedLogRecord record) {
        if (this.pendingRecords.offer(record)) {
            return;
        }
        try {
            Thread.yield();
            this.pendingRecords.put(record);
        } catch (final InterruptedException e) {
            // do nothing
        }
    }


    /**
     * Returns simple name of this class and size/capacity
     *
     * @return ie.: LogRecordBuffer@2b488078[5/10000]
     */
    @Override
    public String toString() {
        return super.toString() + "[" + getSize() + "/" + getCapacity() + "]";
    }
}
