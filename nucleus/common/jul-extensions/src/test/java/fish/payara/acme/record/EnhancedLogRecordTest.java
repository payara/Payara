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

package fish.payara.acme.record;

import fish.payara.logging.jul.record.EnhancedLogRecord;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.ArrayMatching.arrayContaining;
import static org.hamcrest.collection.IsArrayWithSize.arrayWithSize;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;


/**
 * @author David Matejcek
 */
public class EnhancedLogRecordTest {

    @Test
    void serialization() throws Exception {
        final EnhancedLogRecord record = new EnhancedLogRecord(Level.CONFIG, "message");
        record.setLevel(Level.FINEST);
        record.setMessage("message2");
        record.setLoggerName("loggerNameX");
        record.setMillis(10001L);
        record.setParameters(new Object[] {3000, "value2", new NonSerializableClass()});
        record.setResourceBundleName("resourceBundleName");
        record.setSequenceNumber(1L);
        record.setSourceClassName("SourceClassName");
        record.setSourceMethodName("sourceMethodName");
        record.setThreadID(1000);
        record.setThrown(new RuntimeException("Exception Message"));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ObjectOutputStream os = new ObjectOutputStream(output)) {
            os.writeObject(record);
        }

        EnhancedLogRecord record2;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(output.toByteArray()))) {
            Object object = input.readObject();
            assertAll(
                () -> assertThat(object, instanceOf(EnhancedLogRecord.class)),
                () -> assertNotSame(object, record)
            );
            record2 = (EnhancedLogRecord) object;
        }

        assertAll(
            // we set the level and message in constructor and then reset it with setters.
            () -> assertEquals(Level.FINEST, record2.getLevel()),
            () -> assertEquals(record.getLoggerName(), record2.getLoggerName()),
            () -> assertEquals("message2", record2.getMessage()),
            () -> assertEquals(record.getMessageKey(), record2.getMessageKey()),
            () -> assertEquals(record.getMillis(), record2.getMillis()),
            () -> assertThat(record2.getParameters(), arrayWithSize(3)),
            () -> assertThat(record2.getParameters(),
                arrayContaining(equalTo("3000"), equalTo("value2"), equalTo("NonSerializableClass"))),
            () -> assertEquals(record.getResourceBundle(), record2.getResourceBundle()),
            () -> assertEquals(record.getResourceBundleName(), record2.getResourceBundleName()),
            () -> assertEquals(record.getSequenceNumber(), record2.getSequenceNumber()),
            () -> assertEquals(record.getSourceClassName(), record2.getSourceClassName()),
            () -> assertEquals(record.getSourceMethodName(), record2.getSourceMethodName()),
            () -> assertEquals(record.getThreadID(), record2.getThreadID()),
            () -> assertEquals(record.getThreadName(), record2.getThreadName()),
            () -> assertNotEquals(record.getThrown(), record2.getThrown()),
            () -> assertThat(record2.getThrown(), instanceOf(record.getThrown().getClass())),
            () -> assertEquals(record.getThrownStackTrace(), record2.getThrownStackTrace()),
            () -> assertEquals(record.getThrown().getMessage(), record2.getThrown().getMessage()),
            () -> assertEquals(record.getTime(), record2.getTime()),
            () -> assertEquals(record.toString(), record2.toString())
        );
    }


    private static final class NonSerializableClass {
        @Override
        public String toString() {
            return "NonSerializableClass";
        }
    }

}
