/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2018 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.persistence.eclipselink.cache.coordination;

import org.eclipse.persistence.internal.sessions.AbstractSession;
import org.eclipse.persistence.sessions.coordination.RemoteCommandManager;
import org.eclipse.persistence.sessions.serializers.JavaSerializer;
import org.eclipse.persistence.sessions.serializers.Serializer;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents the payload transported via Hazelcast topic.
 *
 * @author Sven Diedrichsen
 */
public abstract class HazelcastPayload implements Serializable {

    private static final long serialVersionUID = 1;

    /**
     * This payloads id.
     */
    private final UUID id = UUID.randomUUID();

    public abstract org.eclipse.persistence.sessions.coordination.Command getCommand(RemoteCommandManager rcm);

    public UUID getId() { return id; }

    /**
     * Implements a payload for raw bytes to transfer.
     */
    public static class Bytes extends HazelcastPayload {

        private final byte[] bytes;

        public Bytes(byte[] bytes) {
            this.bytes = bytes;
        }

        /**
         * Returns the serialized {@link org.eclipse.persistence.sessions.coordination.Command} from
         * the provided bytes.
         * @param rcm The {@link RemoteCommandManager} to use for serialization.
         * @return coordination command serialized from bytes.
         */
        public org.eclipse.persistence.sessions.coordination.Command getCommand(RemoteCommandManager rcm) {
            Serializer serializer = rcm.getSerializer();
            if (serializer == null) {
                serializer = JavaSerializer.instance;
            }
            return (org.eclipse.persistence.sessions.coordination.Command)serializer.deserialize(this.bytes, (AbstractSession) rcm.getCommandProcessor());
        }
    }

    /**
     * Implements a payload consisting of a coordination command.
     */
    public static class Command extends HazelcastPayload {

        private final org.eclipse.persistence.sessions.coordination.Command command;

        public Command(org.eclipse.persistence.sessions.coordination.Command command) {
            this.command = command;
        }

        /**
         * Simply returns the original command.
         * @param rcm The {@link RemoteCommandManager} to use for serialization. Will not be used.
         * @return The original command.
         */
        @Override
        public org.eclipse.persistence.sessions.coordination.Command getCommand(RemoteCommandManager rcm) {
            return this.command;
        }
    }

}
