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

package remoteview;

import javax.ejb.*;
import javax.annotation.*;
import java.util.concurrent.Future;

@Stateless(mappedName="HH")
@RemoteHome(HelloHome.class)
@Remote(Hello.class)
@TransactionManagement(TransactionManagementType.BEAN)
public class HelloBean {

    @Resource
	private SessionContext sessionCtx;

    @PostConstruct
    public void init() {
	System.out.println("In HelloBean::init()");
    }

    public String hello() {
	System.out.println("In HelloBean::hello()");
	return "hello, world\n";
    }

    @Asynchronous
    public Future<String> helloAsync() {
	System.out.println("In HelloBean::helloAsync()");
	return new AsyncResult<String>("helo, async world!\n");
    }

    @Asynchronous
    public Future<String> asyncBlock(int seconds) {
	System.out.println("In HelloBean::asyncBlock");
	sleep(seconds);
	return new AsyncResult<String>("blocked successfully");
    }

    @Asynchronous 
    public void fireAndForget() {
	System.out.println("In HelloBean::fireAndForget()");
	sleep(5);
    }
	
    @Asynchronous 
    public Future<String> asyncThrowException(String exceptionType) {
	System.out.println("In HelloBean::asyncThrowException");
	throwException(exceptionType);
	return new AsyncResult<String>("should have thrown exception");
    }

    @Asynchronous 
    public Future<String> asyncCancel(int seconds) throws Exception
    {
	System.out.println("In HelloBean::asyncCancel");
	sleep(seconds);
	if( sessionCtx.wasCancelCalled() ) {
	    throw new Exception("Canceled after " + seconds + " seconds");
	}
	return new AsyncResult<String>("asyncCancel() should have been cancelled");
    }

    public void throwException(String exceptionType) {
	if( exceptionType.equals("javax.ejb.EJBException") ) {
	    throw new EJBException(exceptionType);
	} else if( exceptionType.equals("javax.ejb.ConcurrentAccessException") ) {
	    throw new ConcurrentAccessException(exceptionType);
	} else if( exceptionType.equals("javax.ejb.ConcurrentAccessTimeoutException") ) {
	    throw new ConcurrentAccessTimeoutException(exceptionType);
	} else if( exceptionType.equals("javax.ejb.IllegalLoopbackException") ) {
	    throw new IllegalLoopbackException(exceptionType);
	}

	throw new IllegalArgumentException(exceptionType);
    }

    private void sleep(int seconds) {

	System.out.println("In HelloBean::sleeping for " + seconds + 
			   "seconds");
	try {
	    Thread.currentThread().sleep(seconds * 1000);
	    System.out.println("In HelloBean::woke up from " + seconds + 
			       "second sleep");
	} catch(Exception e) {
	    e.printStackTrace();
	}

    }


    @PreDestroy
    public void destroy() {
	System.out.println("In HelloBean::destroy()");
    }


}
