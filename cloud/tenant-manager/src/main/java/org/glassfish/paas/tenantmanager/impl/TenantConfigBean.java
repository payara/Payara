/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.paas.tenantmanager.impl;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import javax.xml.stream.XMLStreamReader;

import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.Dom;
/**
 * Extends ConfigBean with global reentrant per process lock against document file.
 * 
 * @author Andriy Zhdanov
 *
 */
//TODO: extend GlassfishConfigBean to translate configured attributes containing
// properties like ${foo.bar} into system properties values?
public class TenantConfigBean extends ConfigBean {


    public TenantConfigBean(Habitat habitat, TenantDocument document, ConfigBean parent, ConfigModel model, XMLStreamReader in) {
        super(habitat, document, parent, model, in);                
    }

    public TenantConfigBean(Dom source, Dom parent) {
        super(source, parent);
    }

    /**
     * 
     * @return
     */
    public TenantDocument getDocument() {
        return (TenantDocument) document;
    }

    @Override
    public Lock getLock() {
        return lock;
    }
    
    // lock document for process first then just element for thread.
    final private Lock lock = new Lock() {
        private final Lock beanLock = TenantConfigBean.super.getLock();
        private final Lock docLock = TenantConfigBean.this.getDocument().getLock();

        @Override
        public void lock() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean tryLock() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            if (docLock.tryLock(time, unit)) {
                boolean locked = false;
                try {
                    locked = beanLock.tryLock(time, unit);
                } finally {
                    if (!locked) {
                        docLock.unlock();
                    }
                }
                return locked;
            } else {
                return false;
            }
        }

        @Override
        public void unlock() {
            beanLock.unlock();
            docLock.unlock();
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    };
}
