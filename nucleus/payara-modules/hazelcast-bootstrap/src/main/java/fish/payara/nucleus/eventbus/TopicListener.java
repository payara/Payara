/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.eventbus;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import java.util.HashSet;

/**
 *
 * @author steve
 */
public class TopicListener implements MessageListener {
    
    private String topicName;
    private String registrationID;
    private HashSet<MessageReceiver> receivers;

    public TopicListener(String topicName) {
        this.topicName = topicName;
        receivers = new HashSet<MessageReceiver>(2);
    }

    public String getTopicName() {
        return topicName;
    }

    public String getRegistrationID() {
        return registrationID;
    }

    public void setRegistrationID(String registrationID) {
        this.registrationID = registrationID;
    }
    

    @Override
    @SuppressWarnings("unchecked")
    public void onMessage(Message msg) {
        for (MessageReceiver receiver : receivers) {
            receiver.receiveMessage((ClusterMessage)msg.getMessageObject());
        }
    }

    void addMessageReceiver(MessageReceiver mr) {
        receivers.add(mr);
    }
    
    int removeMessageReceiver(MessageReceiver mr) {
        receivers.remove(mr);
        return receivers.size();
    }
    
    
    
}
