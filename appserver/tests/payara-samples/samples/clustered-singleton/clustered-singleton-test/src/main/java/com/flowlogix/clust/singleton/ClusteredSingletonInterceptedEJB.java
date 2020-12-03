/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2017] Payara Foundation and/or its affiliates. All rights reserved.
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
package com.flowlogix.clust.singleton;

import com.flowlogix.clust.singleton.api.InterceptedSingletonAPI;
import com.flowlogix.clust.singleton.interceptor.ClusteredInterceptor;
import com.flowlogix.clust.singleton.interceptor.TimerRanFlag;
import fish.payara.cluster.Clustered;
import java.io.Serializable;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.enterprise.inject.Vetoed;
import javax.interceptor.Interceptors;
import lombok.SneakyThrows;
import lombok.experimental.Delegate;
import lombok.extern.java.Log;

/**
 *
 * @author lprimak
 */
@Clustered
@Singleton(name = "ClusteredSingletonInterceptedEJB")
@Vetoed
@Log
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Interceptors(ClusteredInterceptor.class)
public class ClusteredSingletonInterceptedEJB implements InterceptedSingletonAPI, Serializable {
    public ClusteredSingletonInterceptedEJB() {
        log.log(Level.INFO, "{0} - Constructor", getClass().getSimpleName());
    }

    @PostConstruct
    void init() {
        log.log(Level.INFO, "{0} - PostConstruct", getClass().getSimpleName());
        ts.createSingleActionTimer(0, new TimerConfig(null, false));
    }

    @PreDestroy
    void destroy() {
        log.log(Level.INFO, "{0} - PreDestroy", getClass().getSimpleName());
    }

    @Override
    public String getHello() {
        invocationIntercepted = (ctx.getContextData().get(ClusteredInterceptor.AroundInvokeKey) != null) ?
                ctx.getContextData().get(ClusteredInterceptor.AroundInvokeKey).equals(ClusteredInterceptor.AroundInvokeValue)
                : "".equals(ClusteredInterceptor.AroundInvokeValue);
        return String.format("Intercepted Annotated EJB Hello: %s, consistent: %s", sc, isConsistent()? "yes" : "NO!!!");
    }

    @Override
    @SneakyThrows(InterruptedException.class)
    public void waitForTimer() {
        for(int ii = 0; ii < 1000 && !timerRan.isTimerRan(); ++ii) {
            Thread.sleep(5);
        }
    }

    @Override
    public boolean isConsistent() {
        return constructorIntercepted && timerIntercepted && invocationIntercepted;
    }

    public void setConstructorInterceptorCalled() {
        constructorIntercepted = true;
    }

    @Override
    @Asynchronous
    public void async() {
        log.info(String.format("Async called: %s", getState()));
    }

    @Timeout
    private void timeout() {
        log.info("Timer Action");
        timerRan.setTimerRan(true);
        timerIntercepted = (ctx.getContextData().get(ClusteredInterceptor.AroundTimeoutKey) != null) ?
                ctx.getContextData().get(ClusteredInterceptor.AroundTimeoutKey).equals(ClusteredInterceptor.AroundTimeoutValue)
                : "".equals(ClusteredInterceptor.AroundTimeoutValue);
    }

    private final @Delegate SingletonCommon sc = new SingletonCommon(this);
    private transient @Resource TimerService ts;
    private transient @Resource SessionContext ctx;
    private boolean constructorIntercepted;
    private boolean timerIntercepted;
    private boolean invocationIntercepted;
    private transient @EJB TimerRanFlag timerRan;

    private static final long serialVersionUID = 1L;
}
