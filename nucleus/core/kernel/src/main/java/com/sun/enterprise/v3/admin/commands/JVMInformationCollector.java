/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.admin.commands;

import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.util.i18n.StringManager;
import java.text.NumberFormat;
import java.util.Hashtable;
import javax.management.*;

/**
 */
public class JVMInformationCollector extends StandardMBean implements JVMInformationMBean {
    
    static final String SERVER_NAME_KEY_IN_ON = "server"; // the key to identify the server
    private MBeanServerConnection mbsc;
    private static final StringManager sm = StringManager.getManager(JVMInformationCollector.class);
    public JVMInformationCollector() throws NotCompliantMBeanException {
        super(JVMInformationMBean.class);
    }
    @Override
    public String getThreadDump(final String processName) {
        final ObjectName on = processTarget(processName);
        final String title = sm.getString("thread.dump.title", getInstanceNameFromObjectName(on));
        final String td = title + "\n" + invokeMBean(on, "getThreadDump");
        return ( td );
    }

    @Override
    public String getSummary(final String processName) {
        final ObjectName on = processTarget(processName);
        final String title = sm.getString("summary.title", getInstanceNameFromObjectName(on));
        final String s = title + "\n" + invokeMBean(on, "getSummary");
        return ( s );
    }

    @Override
    public String getMemoryInformation(final String processName) {
        final ObjectName on = processTarget(processName);
        final String title = sm.getString("memory.info.title", getInstanceNameFromObjectName(on));
        final String mi = title + "\n" + invokeMBean(on, "getMemoryInformation");
        return ( mi );
    }

    @Override
    public String getClassInformation(final String processName) {
        final ObjectName on = processTarget(processName);
        final String title = sm.getString("class.info.title", getInstanceNameFromObjectName(on));
        final String ci = title + "\n " + invokeMBean(on, "getClassInformation");
        return ( ci );
    }
    @Override
    public String getLogInformation(String processName) {
        ObjectName on  = processTarget(processName);
        String title   = sm.getString("log.info.title", getInstanceNameFromObjectName(on));
        String li      = title + "\n" + invokeMBean(on, "getLogInformation");
        return ( li );
    }
    
    private ObjectName processTarget(final String processName) throws RuntimeException {
        try {
            //get the object-name of the "other" real implementation of JVMInformationMBean interface :)
            final String sn = processName == null ? SERVER_NAME_KEY_IN_ON : processName;
            final String cn = JVMInformation.class.getSimpleName();
            final ObjectName on = formObjectName(sn, cn);
            if (! this.mbsc.isRegistered(on)) {
                final String msg = sm.getString("server.unreachable", sn);
                throw new RuntimeException(msg);
            }
            return (on);
        } catch (final RuntimeException re) {
            throw(re);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String invokeMBean(final ObjectName jvm, final String method) throws RuntimeException {
        try {
            //though proxies work fine, for now (jul 2005/8), I am not going to use them because I am not sure how they work with cascading
            //it is okay to assume that the methods in this mbean take String as parameter
            final Object[] params   = {null};
            final String[] sign     = {"java.lang.String"};
            final Object ret        = this.mbsc.invoke(jvm, method, params, sign);
            
            return ( (String) ret );
            
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void postRegister(Boolean registrationDone) {
    }

    @Override
    public ObjectName preRegister(final MBeanServer server, final ObjectName name) throws Exception {
        this.mbsc = server;
        final String sn = System.getProperty(SystemPropertyConstants.SERVER_NAME);
        final ObjectName on = formObjectName(sn, JVMInformationCollector.class.getSimpleName());
        return ( on );
    }

    @Override
    public void preDeregister() throws Exception {
    }

    @Override
    public void postDeregister() {
    }
    
    /* package private */ static ObjectName formObjectName(final String sn, final String cName) throws Exception {
        /* domain:type=impl-class,server=target-server*/
        final String domain = "amx-internal";
        final Hashtable<String, String> props = new Hashtable<String, String> ();
        props.put("type", cName);
        props.put("category", "monitor");
        final String snk = SERVER_NAME_KEY_IN_ON;
        props.put(snk, sn);
        return ( new ObjectName(domain, props) );
    }
    
    private String getInstanceNameFromObjectName(ObjectName on) {
        return ( on.getKeyProperty(SERVER_NAME_KEY_IN_ON) );
    }
    
    static String millis2HoursMinutesSeconds(final long millis) {
        final long secmin = millis / (long) 1000;
        final long sec = secmin % 60;
        final long minhr = secmin / 60;
        final long min = minhr % 60;
        final long hr = minhr / 60;
        final String msg = sm.getString("m2hms", hr, min, sec);
        
        return ( msg );
    }
    static String millis2SecondsMillis(final long millis) {
        final long sec    = millis / (long) 1000;
        final long ms     = millis % 1000;
        final String msg  = sm.getString("m2sms", sec, ms);
        return ( msg );
    }
    static String formatLong(final long sayBytes) {
        final NumberFormat n = NumberFormat.getInstance();
        return ( n.format(sayBytes) );
    }
}
