package org.glassfish.paas.tenantmanager.impl;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.xml.stream.XMLStreamReader;

import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.Dom;
/**
 * Global lock.
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
        return super.getLock(); // TODO: use lock;
    }
    
    // lock configuration bean if document is locked, this is not strictly correct but enough,
    // we just don't want to bypass TenantManager.executeUpdate.
    @SuppressWarnings("unused") // TODO: remove
    final private Lock lock = new Lock() {
        private final Lock beanLock = TenantConfigBean.super.getLock();
        private final ReentrantLock docLock = TenantConfigBean.this.getDocument().getLock();

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
            if (!docLock.isLocked()) {
                throw new RuntimeException("ConfigBean document is not locked");
            }
            return beanLock.tryLock(time, unit);
        }

        @Override
        public void unlock() {
            beanLock.unlock();
        }

        @Override
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    };
}
