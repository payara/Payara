/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.tests.embedded.mdb;

import org.junit.Assert;
import org.junit.Test;

import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import java.io.File;
import java.io.FileInputStream;

/**
 * @author bhavanishankar@dev.java.net
 */

public class UnitTest {

    @Test
    public void testTimer() throws Exception {
        QueueConnection queueConnection = null;
        QueueSession queueSession = null;
        try {
            InitialContext ic = new InitialContext();

            QueueConnectionFactory qcf = (QueueConnectionFactory)
                    ic.lookup("jms/TestQueueConnectionFactory");
            javax.jms.Queue queue = (javax.jms.Queue) ic.lookup("jms/TestQueue");

            queueConnection = qcf.createQueueConnection();
            queueConnection.start();

            queueSession = queueConnection.createQueueSession(false,
                    Session.AUTO_ACKNOWLEDGE);

            QueueSender sender = queueSession.createSender(queue);

            String str = "Hi From BHAVANI";
            TextMessage msg = queueSession.createTextMessage(str);
            sender.send(msg);

            Thread.sleep(5000);

            byte[] message = new byte[msg.getText().length()];

            File savedFile = new File(System.getProperty("java.io.tmpdir"),
                    "embedded_mdb_onmessage.txt");
            FileInputStream is = new FileInputStream(savedFile);
            is.read(message);

            String savedMsg = new String(message);

            if(!savedMsg.equals(str)) {
                throw new Exception("Sent message [" + str +
                        " ] does not match the received message [" + savedMsg + "]");
            } else {
                System.out.println("Sent message [" + str +
                        " ]  matches the received message [" + savedMsg + "]");
            }
            savedFile.delete();
        } finally {
            try {
                queueSession.close();
                queueConnection.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
