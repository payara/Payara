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
package com.sun.enterprise.server.logging;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.ErrorManager;
import java.util.logging.LogRecord;

/**
 * @author David Matejcek
 */
public class LogRecordBuffer {

    private final int capacity;
    private final BlockingQueue<LogRecord> pendingRecords;

    public LogRecordBuffer(final int capacity) {
        this.capacity = capacity;
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


    public LogRecord pollOrWait() {
        try {
            return this.pendingRecords.take();
        } catch (InterruptedException e) {
            return null;
        }
    }


    public LogRecord poll() {
        return this.pendingRecords.poll();
    }


    public void add(final LogRecord record) {
        if (pendingRecords.offer(record)) {
            return;
        }
        // queue is full, start waiting.
        error("GFFileHandler: Queue full. Waiting to submit.", null);
        try {
            pendingRecords.put(record);
        } catch (final InterruptedException e) {
            error("GFFileHandler: Waiting was interrupted. Log record lost.", e);
        }
    }


    /**
     * Returns simple name of this class and size/capacity
     *
     * @return ie.: LogRecordBuffer[5/10000]
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + getSize() + "/" + getCapacity() + "]";
    }


    private void error(final String message, final Exception cause) {
        new ErrorManager().error(message, cause, ErrorManager.GENERIC_FAILURE);
    }
}
