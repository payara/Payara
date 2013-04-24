/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.apache.catalina;


import java.util.EventObject;


/**
 * General event for notifying listeners of significant changes on a Container.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.4 $ $Date: 2007/03/22 18:04:03 $
 */

public final class ContainerEvent extends EventObject {

    public static final String PRE_DESTROY = "predestroy";

    public static final String BEFORE_CONTEXT_INITIALIZED
        = "beforeContextInitialized";

    public static final String AFTER_CONTEXT_INITIALIZED
        = "afterContextInitialized";
    
    public static final String BEFORE_CONTEXT_DESTROYED
        = "beforeContextDestroyed";

    public static final String AFTER_CONTEXT_DESTROYED
        = "afterContextDestroyed";

    public static final String BEFORE_CONTEXT_ATTRIBUTE_ADDED
        = "beforeContextAttributeAdded";

    public static final String AFTER_CONTEXT_ATTRIBUTE_ADDED
        = "afterContextAttributeAdded";

    public static final String BEFORE_CONTEXT_ATTRIBUTE_REMOVED
        = "beforeContextAttributeRemoved";

    public static final String AFTER_CONTEXT_ATTRIBUTE_REMOVED
        = "afterContextAttributeRemoved";

    public static final String BEFORE_CONTEXT_ATTRIBUTE_REPLACED
        = "beforeContextAttributeReplaced";

    public static final String AFTER_CONTEXT_ATTRIBUTE_REPLACED
        = "afterContextAttributeReplaced";

    public static final String BEFORE_REQUEST_INITIALIZED
        = "beforeRequestInitialized";

    public static final String AFTER_REQUEST_INITIALIZED
        = "afterRequestInitialized";

    public static final String BEFORE_REQUEST_DESTROYED
        = "beforeRequestDestroyed";

    public static final String AFTER_REQUEST_DESTROYED
        = "afterRequestDestroyed";

    public static final String BEFORE_SESSION_CREATED
        = "beforeSessionCreated";

    public static final String AFTER_SESSION_CREATED
        = "afterSessionCreated";

    public static final String BEFORE_SESSION_DESTROYED
        = "beforeSessionDestroyed";

    public static final String AFTER_SESSION_DESTROYED
        = "afterSessionDestroyed";

    public static final String BEFORE_SESSION_ID_CHANGED
            = "beforeSessionIdChanged";

    public static final String AFTER_SESSION_ID_CHANGED
            = "afterSessionIdChanged";

    public static final String BEFORE_SESSION_ATTRIBUTE_ADDED
        = "beforeSessionAttributeAdded";

    public static final String AFTER_SESSION_ATTRIBUTE_ADDED
        = "afterSessionAttributeAdded";

    public static final String BEFORE_SESSION_ATTRIBUTE_REMOVED
        = "beforeSessionAttributeRemoved";

    public static final String AFTER_SESSION_ATTRIBUTE_REMOVED
        = "afterSessionAttributeRemoved";

    public static final String BEFORE_SESSION_ATTRIBUTE_REPLACED
        = "beforeSessionAttributeReplaced";

    public static final String AFTER_SESSION_ATTRIBUTE_REPLACED
        = "afterSessionAttributeReplaced";

    public static final String BEFORE_SESSION_VALUE_UNBOUND
        = "beforeSessionValueUnbound";

    public static final String AFTER_SESSION_VALUE_UNBOUND
        = "afterSessionValueUnbound";

    public static final String BEFORE_FILTER_INITIALIZED
        = "beforeFilterInitialized";

    public static final String AFTER_FILTER_INITIALIZED
        = "afterFilterInitialized";

    public static final String BEFORE_FILTER_DESTROYED
        = "beforeFilterDestroyed";

    public static final String AFTER_FILTER_DESTROYED
        = "afterFilterDestroyed";

    public static final String BEFORE_UPGRADE_HANDLER_INITIALIZED
        = "beforeUpgradeHandlerInitialized";

    public static final String AFTER_UPGRADE_HANDLER_INITIALIZED
        = "afterUpgradeHandlerInitialized";

    public static final String BEFORE_UPGRADE_HANDLER_DESTROYED
        = "beforeUpgradeHandlerDestroyed";

    public static final String AFTER_UPGRADE_HANDLER_DESTROYED
        = "afterUpgradeHandlerDestroyed";

    public static final String BEFORE_READ_LISTENER_ON_DATA_AVAILABLE
        = "beforeReadListenerOnDataAvailable";

    public static final String AFTER_READ_LISTENER_ON_DATA_AVAILABLE
        = "afterReadListenerOnDataAvailable";

    public static final String BEFORE_READ_LISTENER_ON_ALL_DATA_READ
        = "beforeReadListenerOnAllDataRead";

    public static final String AFTER_READ_LISTENER_ON_ALL_DATA_READ
        = "afterReadListenerOnAllDataRead";

    public static final String BEFORE_READ_LISTENER_ON_ERROR
        = "beforeReadListenerOnError";

    public static final String AFTER_READ_LISTENER_ON_ERROR
        = "afterReadListenerOnError";

    public static final String BEFORE_WRITE_LISTENER_ON_WRITE_POSSIBLE
        = "beforeWriteListenerOnWritePossible";

    public static final String AFTER_WRITE_LISTENER_ON_WRITE_POSSIBLE
        = "afterWriteListenerOnWritePossible";

    public static final String BEFORE_WRITE_LISTENER_ON_ERROR
        = "beforeWriteListenerOnError";

    public static final String AFTER_WRITE_LISTENER_ON_ERROR
        = "afterWriteListenerOnError";

    /**
     * The Container on which this event occurred.
     */
    private transient Container container = null;


    /**
     * The event data associated with this event.
     */
    private Object data = null;


    /**
     * The event type this instance represents.
     */
    private String type = null;


    /**
     * Construct a new ContainerEvent with the specified parameters.
     *
     * @param container Container on which this event occurred
     * @param type Event type
     * @param data Event data
     */
    public ContainerEvent(Container container, String type, Object data) {

        super(container);
        this.container = container;
        this.type = type;
        this.data = data;

    }


    /**
     * Return the event data of this event.
     */
    public Object getData() {

        return (this.data);

    }


    /**
     * Return the Container on which this event occurred.
     */
    public Container getContainer() {

        return (this.container);

    }


    /**
     * Return the event type of this event.
     */
    public String getType() {

        return (this.type);

    }


    /**
     * Return a string representation of this event.
     */
    public String toString() {

        return ("ContainerEvent['" + getContainer() + "','" +
                getType() + "','" + getData() + "']");

    }


}
