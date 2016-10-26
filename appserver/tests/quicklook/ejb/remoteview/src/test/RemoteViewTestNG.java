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
// Portions Copyright [2016] [Payara Foundation and/or its affiliates]
package test.ejb.remoteview;


import java.util.Properties;
import org.testng.annotations.*;
import org.testng.Assert;
import javax.naming.InitialContext;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import remoteview.*;

public class RemoteViewTestNG {
    private String appName="remoteview";

    @Test(groups = { "init" })
    public void helloRemote() throws Exception{
        boolean test_result = false;
        try {
            HelloHome helloHome = (HelloHome) new InitialContext(jndiProperties()).lookup("java:global/" + appName + "/HelloBean!remoteview.HelloHome");

            Hello hello = (Hello) new InitialContext(jndiProperties()).lookup("HH#remoteview.Hello");
            Future<String> future = hello.helloAsync();
            hello.helloAsync();
            hello.helloAsync();
            hello.helloAsync();
            hello.fireAndForget();

            String result = future.get();
            //System.out.println("helloAsync() says : " + result);
            test_result = true;
	    } catch(ExecutionException e) {
            test_result = false;
            System.out.println("Got async ExecutionException. Cause is " +
                e.getCause().getMessage());
                e.getCause().printStackTrace();
	    }
        Assert.assertEquals(test_result, true,"Unexpected Results");
    }

    @Test(dependsOnGroups = { "init.*" })
    public void portableGlobal() throws Exception{
        boolean test_result = false;
        // Fully-qualified portable global
        try{
            HelloHome helloHome2 = (HelloHome) new InitialContext(jndiProperties()).lookup("java:global/" + appName + "/HelloBean!remoteview.HelloHome");
            callHome(helloHome2);

            Hello hello2 = (Hello) new InitialContext(jndiProperties()).lookup("java:global/" + appName + "/HelloBean!remoteview.Hello");
            callBusHome(hello2);
            test_result = true;
        } catch(Exception e) {
            test_result = false;
            System.out.println("Exception from portableGlobal:"+e);
        }
        Assert.assertEquals(test_result, true,"Unexpected Results");
    }

    @Test(dependsOnGroups = { "init.*" })
    public void nonPortableGlobal() throws Exception{
        boolean test_result = false;
        // non-portable global
        try{
            HelloHome helloHome5 = (HelloHome) new InitialContext(jndiProperties()).lookup("HH");
            callHome(helloHome5);

            Hello hello5 = (Hello) new InitialContext(jndiProperties()).lookup("HH#remoteview.Hello");
            callBusHome(hello5);
            test_result = true;
        } catch(Exception e) {
            test_result = false;
            System.out.println("Exception from portableGlobal:"+e);
        }
        Assert.assertEquals(test_result, true,"Unexpected Results");
    }

    private static void callHome(HelloHome home) throws Exception {
        HelloRemote hr = home.create();
        //System.out.println("2.x HelloRemote.hello() says " + hr.hello());
    }
    private static void callBusHome(Hello h) {
	String hret = h.hello();
        //System.out.println("Hello.hello() says " + h.hello());
    }
    
    private static Properties jndiProperties() {
        Properties jndiProps = new Properties();
        jndiProps.put("java.naming.factory.initial", "com.sun.enterprise.naming.impl.SerialInitContextFactory");
        jndiProps.put("java.naming.factory.url.pkgs", "com.sun.enterprise.naming");
        jndiProps.put("java.naming.factory.state", "com.sun.corba.ee.impl.presentation.rmi.JNDIStateFactoryImpl");
        jndiProps.put("java.naming.provider.url", "iiop://localhost:5037");
        jndiProps.setProperty("org.omg.CORBA.ORBInitialHost", "127.0.0.1");
        jndiProps.setProperty("org.omg.CORBA.ORBInitialPort", "5037");
        return jndiProps;
    }
}
