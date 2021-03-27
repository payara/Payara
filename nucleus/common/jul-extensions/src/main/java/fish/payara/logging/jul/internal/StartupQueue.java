/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2020 Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
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

package fish.payara.logging.jul.internal;

import fish.payara.logging.jul.PayaraLogger;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.LogRecord;
import java.util.stream.Stream;

/**
 * @author David Matejcek
 */
public class StartupQueue {

    private static final StartupQueue INSTANCE = new StartupQueue();
    private final ConcurrentLinkedQueue<DeferredRecord> queue = new ConcurrentLinkedQueue<>();

    private StartupQueue() {
        // hidden
    }

    public static StartupQueue getInstance() {
        return INSTANCE;
    }

    public void add(PayaraLogger logger, LogRecord record) {
        queue.add(new DeferredRecord(logger, record));
    }


    public Stream<DeferredRecord> toStream() {
        return queue.stream().sorted();
    }


    public void reset() {
        this.queue.clear();
    }


    public static final class DeferredRecord implements Comparable<DeferredRecord> {
        private final PayaraLogger logger;
        private final LogRecord record;

        DeferredRecord(final PayaraLogger logger, final LogRecord record) {
            this.logger = logger;
            this.record = record;
        }


        public PayaraLogger getLogger() {
            return logger;
        }


        public LogRecord getRecord() {
            return record;
        }


        @Override
        public int compareTo(final DeferredRecord another) {
            if (this.record.getSequenceNumber() < another.getRecord().getSequenceNumber()) {
                return -1;
            } else if (this.record.getSequenceNumber() > another.getRecord().getSequenceNumber()) {
                return 1;
            }
            return 0;
        }


        /** Useful for debugging */
        @Override
        public String toString() {
            return super.toString() + "[seq=" + this.record.getSequenceNumber() + ", level=" + this.record.getLevel()
                + ", message=" + this.record.getMessage() + "]";
        }
    }
}
