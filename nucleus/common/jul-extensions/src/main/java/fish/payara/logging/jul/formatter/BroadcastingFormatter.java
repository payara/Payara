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

package fish.payara.logging.jul.formatter;

import fish.payara.logging.jul.event.LogEvent;
import fish.payara.logging.jul.event.LogEventBroadcaster;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;


/**
 * @author David Matejcek
 */
public abstract class BroadcastingFormatter extends Formatter implements LogEventBroadcaster {

    private LogEventBroadcaster logEventBroadcasterDelegate;
    private String productId;

    protected abstract BroadcastingFormatterOutput formatRecord(LogRecord record);


    public final String getProductId() {
        return productId;
    }


    public final void setProductId(final String productId) {
        this.productId = productId;
    }


    public void setLogEventBroadcaster(final LogEventBroadcaster logEventBroadcaster) {
        logEventBroadcasterDelegate = logEventBroadcaster;
    }


    @Override
    public String formatMessage(final LogRecord record) {
        throw new UnsupportedOperationException("String formatMessage(LogRecord record)");
    }


    @Override
    public final String format(final LogRecord record) {
        final BroadcastingFormatterOutput output = formatRecord(record);
        informLogEventListeners(output.logEvent);
        return output.formattedRecord;
    }


    /**
     * @deprecated this method is called only internally. Don't call it.
     */
    @Deprecated
    @Override
    public void informLogEventListeners(LogEvent logEvent) {
        if (logEventBroadcasterDelegate != null && logEvent != null) {
            logEventBroadcasterDelegate.informLogEventListeners(logEvent);
        }
    }


    protected static final class BroadcastingFormatterOutput {

        /**
         * String message - can be formatted as JSON, YAML, XML, single line or whatever
         * the formatter does.
         */
        public final String formattedRecord;

        /**
         * {@link LogEvent} used to broadcast the logging event to other objects.
         */
        public final LogEvent logEvent;

        /**
         * @param formattedRecord nullable
         * @param logEvent nullable
         */
        protected BroadcastingFormatterOutput(final String formattedRecord, final LogEvent logEvent) {
            this.formattedRecord = formattedRecord;
            this.logEvent = logEvent;
        }
    }
}
