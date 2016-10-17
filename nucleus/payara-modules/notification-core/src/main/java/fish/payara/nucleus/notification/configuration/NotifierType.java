/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2016 Payara Foundation. All rights reserved.
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
package fish.payara.nucleus.notification.configuration;

/**
 * @author mertcaliskan
 *
 * The type of notifer types that notification service supports.
 */
public enum NotifierType {
    LOG
    // More types will be here soon! Things we have in mind:
    // PAYARA-704 - Slack NotifierConfiguration
    // PAYARA-703 - HipChat NotifierConfiguration
    // PAYARA-702 - XMPP NotifierConfiguration
    // PAYARA-701 - SNMP NotifierConfiguration
    // PAYARA-700 - JMS NotifierConfiguration
    // PAYARA-698 - Email NotifierConfiguration
}