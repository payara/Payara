/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.session;


public class SessionLock {
    
    private static final String BACKGROUND_LOCK = "background_lock";
    private static final String FOREGROUND_LOCK = "foreground_lock";
    
    /** Creates a new instance of SessionLock */
    public SessionLock() {
    }

    /**
     * get the lock type
     *
     */     
    public String getLockType() {
        return _lockType;
    }

    /**
     * set the lock type - lockType must be BACKGROUND_LOCK or FOREGROUND_LOCK
     *
     * @param lockType the type of the lock
     */    
    public void setLockType(String lockType) {
        _lockType = lockType;
        // If resetting lock then also reset the _hasNonHttpLockOccurred
        if (lockType == null) {
            _hasNonHttpLockOccurred = false;
        }
    }
    
    /**
     * get the foregroundRefCount
     *
     */     
    public int getForegroundRefCount() {
        return _foregroundRefCount;
    }
    
    /**
     * set the foregroundRefCount
     *
     * @param foregroundRefCount
     */    
    public void setForegroundRefCount(int foregroundRefCount) {
        _foregroundRefCount = foregroundRefCount;
    }
    
    /**
     * increment the foregroundRefCount
     *
     */      
    public void incrementForegroundRefCount() {
        _foregroundRefCount++;
    }
    
    /**
     * decrement the foregroundRefCount
     *
     */    
    public void decrementForegroundRefCount() {
        _foregroundRefCount--;
    }    
    
    /**
     * return whether lock is background locked
     *
     */    
    public boolean isBackgroundLocked() {
        if(_lockType == null) {
            return false;
        }
        return (_lockType.equals(BACKGROUND_LOCK));
    }
    
    /**
     * return whether lock is foreground locked
     *
     */     
    public boolean isForegroundLocked() {
        if(_lockType == null) {
            return false;
        }
        return (_lockType.equals(FOREGROUND_LOCK));
    }
    
    /**
     * return whether lock is locked (either foreground or background)
     *
     */       
    public boolean isLocked() {
        return (_lockType != null);
    }
   
    /**
     * @return true if lock has been locked by non-http request, false otherwise
     */       
    public boolean hasNonHttpLockOccurred() {    
        return _hasNonHttpLockOccurred;
    }

    /**
     * unlock the lock
     * if background locked the lock will become fully unlocked
     * if foreground locked the lock will become fully unlocked
     * if foregroundRefCount was 1; otherwise it will
     * decrement the foregroundRefCount and the lock will remain foreground locked
     *
     */    
    public void unlock() {
        if(!isLocked())
            return;
        if(isBackgroundLocked()) {
            this.setLockType(null);
            this.setForegroundRefCount(0);
            return;
        }
        if(isForegroundLocked()) {
            decrementForegroundRefCount();
            if(_foregroundRefCount == 0) {
                this.setLockType(null);
            }
        }                        
    } 
    
    /**
     * unlock the lock for the foreground locked case
     * the lock will be unlocked
     * if foregroundRefCount was 1; otherwise it will
     * decrement the foregroundRefCount and the lock will remain foreground locked
     *
     */    
    public void unlockForeground() {
        //unlock if the lock is foreground locked
        //else do nothing        
        if(!isLocked())
            return;
        if(isForegroundLocked()) {
            decrementForegroundRefCount();
            if(_foregroundRefCount == 0) {
                this.setLockType(null);
            }
        }                        
    }  
    
    /**
     * unlock the lock
     * this is a force unlock; foregroundRefCount is ignored
     *
     */      
    public void unlockForegroundCompletely() {
        //unlock completely if the lock is foreground locked
        //else do nothing        
        if(!isLocked())
            return;
        if(isForegroundLocked()) {
            this.setForegroundRefCount(0);
            this.setLockType(null);
        }                        
    }      
    
    /**
     * unlock the lock for the background locked case
     * the lock will be unlocked
     *
     */     
    public void unlockBackground() {
        //unlock if the lock is background locked
        //else do nothing
        if(!isLocked())
            return;
        if(isBackgroundLocked()) {
            this.setLockType(null);
            this.setForegroundRefCount(0);
            return;
        }                        
    }     
    
    /**
     * if possible, the lock will be foreground locked
     * if it was already foreground locked; it will
     * remain so and the foregroundRefCount will be incremented
     *
     * if the lock is already background locked the method
     * will return false and the lock remains background locked 
     * (i.e. lock failed) otherwise it will return true (lock succeeded)
     */     
    public synchronized boolean lockForeground() {
        return lockForeground(true);
    }

    public synchronized boolean lockForeground(boolean isHttp) {
        if (!isHttp) {
            _hasNonHttpLockOccurred = true;
        }
        if (isBackgroundLocked()) {
            return false;
        }
        if (isForegroundLocked()) {
            incrementForegroundRefCount();
        } else {
            setForegroundRefCount(1);
        }
        setLockType(FOREGROUND_LOCK);

        return true;
    }
    
    /**
     * if possible, the lock will be background locked
     *
     * if the lock is already foreground locked the method
     * will return false and the lock remains foreground locked 
     * (i.e. lock failed) otherwise it will return true (lock succeeded)
     */      
    public synchronized boolean lockBackground() {
        if (isForegroundLocked()) {
            return false;
        } 
        setLockType(BACKGROUND_LOCK);
        setForegroundRefCount(0);

        return true;
    }
    
    /**
     * returns String representation of the state of the lock
     */     
    public String toString() {
        StringBuilder sb = new StringBuilder(50);
        sb.append("_lockType= " + _lockType);
        sb.append("\n" + "foregroundRefCount= " + _foregroundRefCount);
        return sb.toString();
    }
    
    private String _lockType = null;
    private int _foregroundRefCount = 0;
    private boolean _hasNonHttpLockOccurred = false;    
}
