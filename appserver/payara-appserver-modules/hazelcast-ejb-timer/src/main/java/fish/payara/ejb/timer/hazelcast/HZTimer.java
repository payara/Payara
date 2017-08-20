/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2017 Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.ejb.timer.hazelcast;

import com.sun.ejb.containers.EJBTimerSchedule;
import com.sun.ejb.containers.TimerPrimaryKey;
import java.io.Serializable;
import java.util.Date;
import javax.ejb.TimerConfig;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
public class HZTimer implements Serializable {

    private final TimerPrimaryKey key;
    private String hzMemberName;
    private String ownerId;
    private final long containerId;
    private long applicationId;
    private final Serializable timedObjectPk;
    private final Date initialExpiration;
    private final long intervalDuration;
    private final EJBTimerSchedule schedule;
    private final Serializable info;
    private Date lastExpiration;

    public HZTimer(TimerPrimaryKey timerId, long containerId, long applicationId, Object timedObjectPrimaryKey, String hzMemberName, String ownerId, Date initialExpiration, long intervalDuration, EJBTimerSchedule schedule, TimerConfig timerConfig) {
        this.key = timerId;
        this.containerId = containerId;
        this.applicationId = applicationId;
        
        if (timedObjectPrimaryKey instanceof Serializable) {
            this.timedObjectPk = (Serializable) timedObjectPrimaryKey;
        } else {
            this.timedObjectPk = null;
        }
        
        this.hzMemberName = hzMemberName;
        this.ownerId = ownerId;
        this.initialExpiration = initialExpiration;
        this.intervalDuration  = intervalDuration;
        this.schedule = schedule;
        if (timerConfig.isPersistent()) {
            this.info = timerConfig.getInfo();
        } else {
            info = null;
        }
    }
    
    public void setOwnerId(String id) {
        this.ownerId = id;
    }
    
    public String getOwnerId() {
        return ownerId;
    }

    public void setMemberName(String serverName) {
        this.hzMemberName = serverName;
    }

    public TimerPrimaryKey getKey() {
        return key;
    }

    public String getMemberName() {
        return hzMemberName;
    }

    public long getContainerId() {
        return containerId;
    }

    public long getApplicationId() {
        return applicationId;
    }

    void setApplicationId(long applicationId) {
        this.applicationId = applicationId;
    }

    public Object getTimedObjectPk() {
        return timedObjectPk;
    }

    public Date getInitialExpiration() {
        return initialExpiration;
    }

    public long getIntervalDuration() {
        return intervalDuration;
    }

    public EJBTimerSchedule getSchedule() {
        return schedule;
    }

    public TimerConfig getTimerConfig() {
        return new TimerConfig(info, true);
    }

    Date getLastExpiration() {
        return lastExpiration;
    }

    void setLastExpiration(Date now) {
        lastExpiration = now;
    }
    
    

}
