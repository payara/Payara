/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jdbcruntime.config.validation;

import com.sun.enterprise.config.serverbeans.Domain;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.validation.ConstraintViolationException;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jdbc.config.JdbcResource;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.Before;
import org.glassfish.tests.utils.Utils;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.TransactionFailure;
import org.glassfish.jdbcruntime.config.ConfigApiTest;

import static org.junit.Assert.*;

/**
 *
 * @author mmares
 */
public class ReferenceConstrainTest extends ConfigApiTest {
    
//    private Logger logger = Logger.getLogger(ReferenceConstrainTest.class.getName());
    private ServiceLocator habitat;

    @Override
    public String getFileName() {
        return "DomainTest";
    }

    @Override
    public ServiceLocator getHabitat() {
        return habitat;
    }
    
    private ConstraintViolationException findConstrViolation(Throwable thr) {
        if (thr == null) {
            return null;
        }
        if (thr instanceof ConstraintViolationException) {
            return (ConstraintViolationException) thr;
        }
        return findConstrViolation(thr.getCause());
    }
    
    @Before
    public void createNewHabitat() {
        this.habitat = Utils.instance.getHabitat(this);
    }
    
    @Test // @Ignore
    public void doChangeToValidPool() throws TransactionFailure {
        Domain domain = habitat.getService(Domain.class);
        //Find JdbcResource to chenge its values
        Iterator<JdbcResource> iterator = domain.getResources().getResources(JdbcResource.class).iterator();
        JdbcResource jdbc = null;
        while (iterator.hasNext()) {
            JdbcResource res = iterator.next();
            if ("__TimerPool".equals(res.getPoolName())) {
                jdbc = res;
                break;
            }
        }
        assertNotNull(jdbc);
        ConfigBean poolConfig = (ConfigBean) ConfigBean.unwrap(jdbc);
        Map<ConfigBean, Map<String, String>> changes = new HashMap<ConfigBean, Map<String, String>>();
        Map<String, String> configChanges = new HashMap<String, String>();
        configChanges.put("pool-name", "DerbyPool");
        changes.put(poolConfig, configChanges);
        try {
            ConfigSupport cs = getHabitat().getService(ConfigSupport.class);
            cs.apply(changes);
        } catch (TransactionFailure tf) {
            fail();
        }
    }
   
    @Test
    public void doChangeToInValidPool() throws TransactionFailure {
        Domain domain = habitat.getService(Domain.class);
        //Find JdbcResource to chenge its values
        Iterator<JdbcResource> iterator = domain.getResources().getResources(JdbcResource.class).iterator();
        JdbcResource jdbc = null;
        while (iterator.hasNext()) {
            JdbcResource res = iterator.next();
            if ("DerbyPool".equals(res.getPoolName())) {
                jdbc = res;
                break;
            }
        }
        assertNotNull(jdbc);
        ConfigBean poolConfig = (ConfigBean) ConfigBean.unwrap(jdbc);
        Map<ConfigBean, Map<String, String>> changes = new HashMap<ConfigBean, Map<String, String>>();
        Map<String, String> configChanges = new HashMap<String, String>();
        configChanges.put("pool-name", "WrongPointer");
        changes.put(poolConfig, configChanges);
        try {
            ConfigSupport cs = getHabitat().getService(ConfigSupport.class);
            cs.apply(changes);
            fail("Can not reach this point");
        } catch (TransactionFailure tf) {
            ConstraintViolationException cv = findConstrViolation(tf);
//            cv.printStackTrace(System.out);
            assertNotNull(cv);
        }
    }
    
}
