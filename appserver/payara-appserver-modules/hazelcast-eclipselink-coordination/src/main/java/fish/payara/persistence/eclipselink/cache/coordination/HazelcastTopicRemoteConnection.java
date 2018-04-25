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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.eclipse.persistence.internal.sessions.coordination.broadcast.BroadcastRemoteConnection;
import org.eclipse.persistence.sessions.coordination.Command;
import org.eclipse.persistence.sessions.coordination.RemoteCommandManager;

/**
 * Hazelcast {@link BroadcastRemoteConnection} implementing the HZ {@link MessageListener} interface.
 *
 * @author Sven Diedrichsen
 */
public class HazelcastTopicRemoteConnection extends BroadcastRemoteConnection implements MessageListener<HazelcastPayload> {

    /**
     * The topic to publish commands to and receive messages from.
     */
    private final HazelcastTopic topic;

    HazelcastTopicRemoteConnection(RemoteCommandManager rcm) {
        super(rcm);
        this.topic = new HazelcastTopic(rcm.getChannel(), this);
    }

    /**
     * Publishes the provided command via {@link HazelcastTopic}.
     * @param o The command to execute.
     * @return NULL
     * @throws Exception In case of an error.
     */
    @Override
    protected Object executeCommandInternal(Object o) throws Exception {
        if (o != null) {
            Object[] debugInfo = null;
            if (this.rcm.shouldLogDebugMessage()) {
                debugInfo = this.logDebugBeforePublish(null);
            }
            if(Command.class.isAssignableFrom(o.getClass())) {
                this.topic.publish(new HazelcastPayload.Command((Command) o));
            } else if (o.getClass().isArray()) {
                this.topic.publish(new HazelcastPayload.Bytes((byte[]) o));
            }
            if (debugInfo != null) {
                this.logDebugAfterPublish(debugInfo, "");
            }
        }
        return null;
    }

    @Override
    protected void closeInternal() throws Exception {
        this.topic.destroy();
    }

    /**
     * Receives any published message containing hz payload.
     * @param message the message to process.
     */
    @Override
    public void onMessage(final Message<HazelcastPayload> message) {
        if (hasHazelcastPayload(message)) {
            if(!topic.hasPublished(message.getMessageObject())) {
                HazelcastTopicStorage.getInstance().process(
                    () -> {
                        String messageId = message.getPublishingMember().getUuid() + "-" + String.valueOf(message.getPublishTime());
                        try {
                            this.processReceivedObject(message.getMessageObject().getCommand(this.rcm), messageId);
                        } catch (Exception e) {
                            this.failDeserializeMessage(messageId, e);
                        }
                    }
                );
            }
        } else {
            Object[] args = new Object[]{message.getClass().getName(), topic};
            this.rcm.logWarningWithoutLevelCheck("received_unexpected_message_type", args);
        }
    }

    /**
     * Signals if the message carries a {@link HazelcastPayload} instance.
     * @param message The message to query for its content.
     * @return Content is of type {@link HazelcastPayload}
     */
    private boolean hasHazelcastPayload(Message<HazelcastPayload> message) {
        return HazelcastPayload.class.isAssignableFrom(message.getMessageObject().getClass());
    }

}
