/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) 2021 Payara Foundation and/or its affiliates. All rights reserved.
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

package fish.payara.jul.handler;

import fish.payara.jul.record.EnhancedLogRecord;
import fish.payara.jul.record.MessageResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * This special {@link Handler} can be used for testing purposes.
 * It collects log records passing through the given {@link Logger} instance.
 *
 * @author David Matejcek
 */
public class LogCollectorHandler extends Handler {

    private static final MessageResolver RESOLVER = new MessageResolver();
    private final LogRecordBuffer buffer;
    private final Logger logger;

    /**
     * @param loggerToFollow this handler will be added to this logger.
     */
    public LogCollectorHandler(final Logger loggerToFollow) {
        this.buffer = new LogRecordBuffer(100, 5);
        logger = loggerToFollow;
        logger.addHandler(this);
    }


    @Override
    public void publish(LogRecord record) {
        if (isLoggable(record)) {
            this.buffer.add(RESOLVER.resolve(record));
        }
    }


    @Override
    public void flush() {
        // nothing
    }


    @Override
    public void close() throws SecurityException {
        this.logger.removeHandler(this);
        reset();
    }


    /**
     * @return the first {@link EnhancedLogRecord} in the buffer.
     */
    public EnhancedLogRecord pop() {
        return this.buffer.poll();
    }


    /**
     * @return all collected records
     */
    public List<EnhancedLogRecord> getAll() {
        final List<EnhancedLogRecord> list = new ArrayList<>(this.buffer.getSize());
        while (!this.buffer.isEmpty()) {
            list.add(this.buffer.poll());
        }
        return list;
    }


    /**
     * Drops all collected records.
     */
    public void reset() {
        while (!this.buffer.isEmpty()) {
            this.buffer.poll();
        }
    }
}
