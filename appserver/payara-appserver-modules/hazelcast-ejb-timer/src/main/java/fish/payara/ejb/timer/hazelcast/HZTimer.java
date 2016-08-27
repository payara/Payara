/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2016 Payara Foundation and/or its affiliates.
 All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.ejb.timer.hazelcast;

import com.sun.ejb.containers.EJBTimerSchedule;
import com.sun.ejb.containers.TimerPrimaryKey;
import java.io.Serializable;
import java.util.Date;
import javax.ejb.TimerConfig;

/**
 *
 * @author steve
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
