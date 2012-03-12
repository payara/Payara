package com.sun.s1asdev.ejb.ejb30.hello.mdb;

import javax.annotation.Resource;
import javax.ejb.*;
import javax.jms.*;

//Messages received from InQueue
@MessageDriven(mappedName="jms/ejb_ejb30_hello_mdb_InQueue")
 public class MessageBean implements MessageListener {

    @Resource(mappedName="jms/ejb_ejb30_hello_mdb_QCF") 
    QueueConnectionFactory qcFactory;
    
    //Destination Queue
    @Resource(mappedName="jms/ejb_ejb30_hello_mdb_OutQueue")
    Queue replyQueue;

    public void onMessage(Message message) {
        System.out.println("MessageBean::  onMessage :: Got message!!!" + message);

        QueueConnection connection = null;
	QueueSession session = null;
        try {
            connection = qcFactory.createQueueConnection();
            session = connection.createQueueSession(false,
                                   Session.AUTO_ACKNOWLEDGE);
            QueueSender sender = session.createSender(replyQueue);
            TextMessage tmessage = session.createTextMessage();
	    String msgText =  "Reply for " + ((TextMessage) message).getText();
            tmessage.setText(msgText);
            System.out.println("Sending " + msgText);
            sender.send(tmessage);
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
		if (session != null) {
		    session.close();
		}
                if(connection != null) {
                    connection.close();
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }
}
