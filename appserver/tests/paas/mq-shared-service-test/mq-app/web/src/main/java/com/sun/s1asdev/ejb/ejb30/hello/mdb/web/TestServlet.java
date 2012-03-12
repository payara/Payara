/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.s1asdev.ejb.ejb30.hello.mdb.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.*;
import java.util.*;
import javax.jms.*;
import javax.annotation.Resource;


//@WebServlet(name="testServlet", urlPatterns={"/mdbtest"}, initParams={ @WebInitParam(name="n1", value="v1"), @WebInitParam(name="n2", value="v2") })
public class TestServlet extends HttpServlet {

    private static long TIMEOUT = 90000;

    @Resource(mappedName="jms/ejb_ejb30_hello_mdb_QCF")
    private QueueConnectionFactory queueConFactory;
    
    //Target Queue
    @Resource(mappedName="jms/ejb_ejb30_hello_mdb_InQueue")
    private javax.jms.Queue msgBeanQueue;

    //Reply Queue
    @Resource(mappedName="jms/ejb_ejb30_hello_mdb_OutQueue")
    private javax.jms.Queue clientQueue;

    private QueueConnection queueCon;
    private QueueSession queueSession;
    private QueueSender queueSender;
    private QueueReceiver queueReceiver;
    private int numMessages = 2;


    public void service(HttpServletRequest req, HttpServletResponse res)
            throws IOException, ServletException {

        PrintWriter writer = res.getWriter();
        writer.write("filterMessage=" + req.getAttribute("filterMessage"));
        writer.write("testattribute=" + req.getAttribute("testattribute"));
        String msg = "";
        Enumeration en = getInitParameterNames();
        while (en.hasMoreElements()) {
            String name = (String)en.nextElement();
            String value = getInitParameter(name);
            msg += name + "=" + value + ", ";
        } 
        writer.write(", initParams: " + msg + "\n");
        doTest(writer);
    }

 public void doTest(PrintWriter writer) {
        try {
            setup();
            doTest(numMessages);
            writer.write("EJB 3.0 MDB" + "PASS");
        } catch(Throwable t) {
            writer.write("EJB 3.0 MDB" + "FAIL");
            t.printStackTrace();
        } finally {
            cleanup();
        }
    }

    public void setup() throws Exception {
        queueCon = queueConFactory.createQueueConnection();
        queueSession = queueCon.createQueueSession
            (false, Session.AUTO_ACKNOWLEDGE);

        // Destination will be specified when actual msg is sent.
        queueSender = queueSession.createSender(null);
        queueReceiver = queueSession.createReceiver(clientQueue);
        queueCon.start();
    }

    public void cleanup() {
        try {
            if( queueCon != null ) {
                queueCon.close();
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
    }
  public void sendMsgs(javax.jms.Queue queue, int num)
        throws JMSException {
        for(int i = 0; i < num; i++) {
            Message message = queueSession.createTextMessage("foo #" + (i + 1));
            System.out.println("Sending message " + i + " to " + queue +
                               " at time " + System.currentTimeMillis());
            queueSender.send(queue, message);

            System.out.println("Sent message " + i + " to " + queue +
                               " at time " + System.currentTimeMillis());
        }
    }

    public void doTest(int num)
        throws Exception {
        sendMsgs((javax.jms.Queue) msgBeanQueue, num);

        //Now attempt to receive responses to our message
        System.out.println("Waiting for queue message");
        Message recvdmessage = queueReceiver.receive(TIMEOUT);
        if( recvdmessage != null ) {
            System.out.println("Received message : " +
                                   ((TextMessage)recvdmessage).getText());
        } else {
            System.out.println("timeout after " + TIMEOUT + " seconds");
            throw new JMSException("timeout" + TIMEOUT + " seconds");
        }
    }

}
