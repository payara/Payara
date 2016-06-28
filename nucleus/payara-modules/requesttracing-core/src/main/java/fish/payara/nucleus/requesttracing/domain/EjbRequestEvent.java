/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2016 C2B2 Consulting Limited. All rights reserved.
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
package fish.payara.nucleus.requesttracing.domain;

/**
 * @author mertcaliskan
 *
 * Stores EJB specific event values
 * {@link fish.payara.nucleus.requesttracing.interceptor.PayaraEjbContainerInterceptor}.
 */
public class EjbRequestEvent extends RequestEvent {

    private String homeClassName;
    private String ejbClassName;
    private String remoteClassName;
    private String jndiName;
    private String transactionType;
    private boolean isLocalBean;

    public String getEjbClassName() {
        return ejbClassName;
    }

    public void setEjbClassName(String ejbClassName) {
        this.ejbClassName = ejbClassName;
    }

    public String getHomeClassName() {
        return homeClassName;
    }

    public void setHomeClassName(String homeClassName) {
        this.homeClassName = homeClassName;
    }

    public boolean isLocalBean() {
        return isLocalBean;
    }

    public void setIsLocalBean(boolean isLocalBean) {
        this.isLocalBean = isLocalBean;
    }

    public String getJndiName() {
        return jndiName;
    }

    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    public String getRemoteClassName() {
        return remoteClassName;
    }

    public void setRemoteClassName(String remoteClassName) {
        this.remoteClassName = remoteClassName;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    @Override
    public String toString() {
        return "EjbRequestEvent{" +
                "ejbClassName='" + ejbClassName + '\'' +
                ", homeClassName='" + homeClassName + '\'' +
                ", remoteClassName='" + remoteClassName + '\'' +
                ", jndiName='" + jndiName + '\'' +
                ", transactionType='" + transactionType + '\'' +
                ", isLocalBean='" + isLocalBean + '\'' +
                ", " + super.toString() +
                "} ";
    }
}
