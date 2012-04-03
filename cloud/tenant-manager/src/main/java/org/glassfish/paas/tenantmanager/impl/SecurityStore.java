package org.glassfish.paas.tenantmanager.impl;

import org.jvnet.hk2.annotations.Contract;

/**
 * Identity store, will be replaced with the real impl provided by security.
 *  
 * @author Andriy Zhdanov
 *
 */
@Contract
public interface SecurityStore {
    /**
     * Create security entity.
     * 
     * @param name name.
     * @param password password.
     */
    void create(String name, char[] password);

}
