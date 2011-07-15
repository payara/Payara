/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package com.sun.mdb;

import javax.ejb.MessageDriven;
import javax.ejb.EJBException;
import javax.ejb.NoSuchEJBException;
import javax.ejb.EJB;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.ejb.ActivationConfigProperty;

import javax.jms.MessageListener;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.QueueSender;
import javax.jms.TextMessage;
import javax.jms.Session;

import javax.annotation.Resource;

@TransactionManagement(TransactionManagementType.BEAN)
@MessageDriven(mappedName="jms/ejb_ejb30_hello_mdb_InQueue", description="mymessagedriven bean description")
 public class MessageBean implements MessageListener {

    @EJB private Hello1 hello1;
    @EJB private Hello2 hello2;

    @Resource(name="jms/MyQueueConnectionFactory", 
              mappedName="jms/ejb_ejb30_hello_mdb_QCF") 
    QueueConnectionFactory qcFactory;

    @Resource(mappedName="jms/ejb_ejb30_hello_mdb_OutQueue") Queue clientQueue;

    public void onMessage(Message message) {
        System.out.println("Got message!!!");

        QueueConnection connection = null;
        try {
            
            System.out.println("Calling hello1 stateless bean");
            hello1.hello("local ejb3.0 stateless");
            System.out.println("Calling hello2 stateful bean");
            hello2.hello("local ejb3.0 stateful");
            hello2.removeMethod();
            try {
                hello2.hello("this call should not go through");
                throw new Exception("bean should have been removed " +
                                    "after removeMethod()");
            } catch(NoSuchEJBException e) {
                System.out.println("Successfully caught EJBException after " +
                                   " accessing removed SFSB");
            }

            connection = qcFactory.createQueueConnection();
            QueueSession session = connection.createQueueSession(false,
                                   Session.AUTO_ACKNOWLEDGE);
            QueueSender sender = session.createSender(clientQueue);
            TextMessage tmessage = session.createTextMessage();
            tmessage.setText("mdb() invoked");
            System.out.println("Sending message");
            sender.send(tmessage);
            System.out.println("message sent");

        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if(connection != null) {
                    connection.close();
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

    }

}
