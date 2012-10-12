/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.persistence.ejb.entitybean.container.distributed;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.ejb.containers.EjbContainerUtilImpl;
import com.sun.logging.LogDomains;


class DistributedReadOnlyBeanServiceImpl
    implements DistributedReadOnlyBeanService {
    
	private Logger _logger = LogDomains.getLogger(DistributedReadOnlyBeanServiceImpl.class, LogDomains.EJB_LOGGER);
    
    private ConcurrentHashMap<Long, ReadOnlyBeanRefreshHandlerInfo> refreshHandlers
        = new ConcurrentHashMap<Long, ReadOnlyBeanRefreshHandlerInfo>();
    
    
    private DistributedReadOnlyBeanNotifier robNotifier;
    
    public void setDistributedReadOnlyBeanNotifier(
            DistributedReadOnlyBeanNotifier notifier) {
        this.robNotifier = notifier;
        _logger.log(Level.INFO, "Registered ReadOnlyBeanNotifier: "
                + notifier);
    }
    
    public void addReadOnlyBeanRefreshEventHandler(
            long ejbID, ClassLoader loader,
            ReadOnlyBeanRefreshEventHandler handler) {
        refreshHandlers.put(ejbID, new ReadOnlyBeanRefreshHandlerInfo(
                loader, handler));
        _logger.log(Level.INFO, "Registered ReadOnlyBeanRefreshEventHandler: "
                + ejbID + "; " + handler);
    }

    public void removeReadOnlyBeanRefreshEventHandler(long ejbID) {
        refreshHandlers.remove(ejbID);
    }

    public void notifyRefresh(long ejbID, Object pk) {
        if (robNotifier != null) {
            byte[] pkData = null;
            
            ByteArrayOutputStream bos = null;
            ObjectOutputStream oos = null;
            try {
                bos = new ByteArrayOutputStream();
                oos = new ObjectOutputStream(bos);
                
                oos.writeObject(pk);
                oos.flush();
                bos.flush();
                pkData = bos.toByteArray();
                robNotifier.notifyRefresh(ejbID, pkData);
            } catch (Exception ex) {
                _logger.log(Level.WARNING, "Error during notifyRefresh", ex);
            } finally {
                if (oos != null) {
                    try { oos.close(); } catch(IOException ioEx) {};
                }
                if (bos != null) {
                    try { bos.close(); } catch(IOException ioEx) {};
                }
            }
        } else {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                        "DistributedReadOnlyBeanService ignoring request "
                        + "for notifyRefresh: " + ejbID);
            }
        }
    }

    public void notifyRefreshAll(long ejbID) {
        if (robNotifier != null) {
            robNotifier.notifyRefreshAll(ejbID);
        }  else {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                        "DistributedReadOnlyBeanService ignoring request "
                        + "for notifyRefreshAll: " + ejbID);
            }
        }
    }
    
    public void handleRefreshRequest(long ejbID, byte[] pkData) {
        refreshRequestReceived(false, ejbID, pkData);
    }
    
    public void handleRefreshAllRequest(long ejbID) {
        refreshRequestReceived(true, ejbID, null);
    }
    
    private void refreshRequestReceived(boolean refreshAll,
            long ejbID, byte[] pkData) {

        final ReadOnlyBeanRefreshHandlerInfo info = refreshHandlers.get(ejbID);
        if (info == null) {
            //TODO: Log something
            return;
        }
        
        final Thread currentThread = Thread.currentThread();
        final ClassLoader prevClassLoader = currentThread.getContextClassLoader();
        
        try {
            if(System.getSecurityManager() == null) {
                currentThread.setContextClassLoader(info.loader);
            } else {
                java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedAction() {
                    public java.lang.Object run() {
                        currentThread.setContextClassLoader(info.loader);
                        return null;
                    }
                });
            }
            
            if (! refreshAll) {
                ByteArrayInputStream bis = null;
                ObjectInputStream ois = null;
                Serializable pk = null;
                try {
                    bis = new ByteArrayInputStream(pkData);
                    ois = new ObjectInputStream(bis);
                    
                    pk = (Serializable) ois.readObject();
                } catch (IOException ioEx) {
                    _logger.log(Level.WARNING, "Error during refresh", ioEx);
                } catch (ClassNotFoundException cnfEx) {
                    _logger.log(Level.WARNING, "Error during refresh", cnfEx);
                } finally {
                    if (ois != null) {
                        try {
                            ois.close();
                        } catch(IOException ioEx) {
                            _logger.log(Level.WARNING, 
                                    "Error while closing object stream", ioEx);
                        };
                    }
                    if (bis != null) {
                        try {
                            bis.close();
                        } catch(IOException ioEx) {
                            _logger.log(Level.WARNING, 
                                    "Error while closing byte stream", ioEx);
                        };
                    }
                }
                if (pk != null) {
                    info.handler.handleRefreshRequest(pk);
                }
            } else {
                info.handler.handleRefreshAllRequest();
            }
        } catch (Exception ex) {
            _logger.log(Level.WARNING, "Error during refresh", ex);
        } finally {
            if(System.getSecurityManager() == null) {
                currentThread.setContextClassLoader(prevClassLoader);
            } else {
                java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedAction() {
                    public java.lang.Object run() {
                        currentThread.setContextClassLoader(prevClassLoader);
                        return null;
                    }
                });
            }
        }        
    }
    
    private static class ReadOnlyBeanRefreshHandlerInfo {
        public  ClassLoader                     loader;
        public  ReadOnlyBeanRefreshEventHandler handler;
        
        public ReadOnlyBeanRefreshHandlerInfo(
                ClassLoader loader, ReadOnlyBeanRefreshEventHandler handler) {
            this.loader = loader;
            this.handler = handler;
        }
    }
}
