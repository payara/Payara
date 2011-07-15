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
 */

package com.sun.appserv.web.taglibs.cache;

/**
 * Class responsible for caching and expiring the execution result of a JSP
 * fragment.
 */
public class CacheEntry
{
    public static final int NO_TIMEOUT = -1;

    String content;
    volatile long expireTime; 

    /**
     * Constructs a CacheEntry using the response string to be
     * cached and the timeout after which the entry will expire
     */
    public CacheEntry(String response, int timeout) {
        content = response; 
        computeExpireTime(timeout);  
    }

    /**
     * set the real expire time
     * @param expireTime in milli seconds
     */
    public void setExpireTime(long expireTime) {
        this.expireTime = expireTime;
    }

    /**
     * Gets the cached content.
     *
     * @return The cached content
     */
    public String getContent() {
        return this.content;
    }

    /**
     * compute when this entry to be expired based on timeout relative to 
     * current time.
     * @param timeout in seconds
     */
    public void computeExpireTime(int timeout) {
        // timeout is relative to current time
        this.expireTime = (timeout == NO_TIMEOUT) ? timeout :
                          System.currentTimeMillis() + (timeout * 1000L);
    }

    /**
     * is this response still valid?
     */
    public boolean isValid() {
        return (expireTime > System.currentTimeMillis() ||
                expireTime == NO_TIMEOUT);
    }

    /** 
     * clear the contents
     */
    public void clear() {
        content = null;
        expireTime = 0L;
    }
}
