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
 * General event for notifying listeners of significant changes on a component
 * that implements the Lifecycle interface.  In particular, this will be useful
 * on Containers, where these events replace the ContextInterceptor concept in
 * Tomcat 3.x.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.1.2.1 $ $Date: 2007/08/17 15:46:26 $
 */

public final class LifecycleEvent
    extends EventObject {


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new LifecycleEvent with the specified parameters.
     *
     * @param lifecycle Component on which this event occurred
     * @param type Event type (required)
     */
    public LifecycleEvent(Lifecycle lifecycle, String type) {

        this(lifecycle, type, null);

    }


    /**
     * Construct a new LifecycleEvent with the specified parameters.
     *
     * @param lifecycle Component on which this event occurred
     * @param type Event type (required)
     * @param data Event data (if any)
     */
    public LifecycleEvent(Lifecycle lifecycle, String type, Object data) {

        super(lifecycle);
        this.lifecycle = lifecycle;
        this.type = type;
        this.data = data;

    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The event data associated with this event.
     */
    private Object data = null;


    /**
     * The Lifecycle on which this event occurred.
     */
    private transient Lifecycle lifecycle = null;


    /**
     * The event type this instance represents.
     */
    private String type = null;


    // ------------------------------------------------------------- Properties


    /**
     * Return the event data of this event.
     */
    public Object getData() {

        return (this.data);

    }


    /**
     * Return the Lifecycle on which this event occurred.
     */
    public Lifecycle getLifecycle() {

        return (this.lifecycle);

    }


    /**
     * Return the event type of this event.
     */
    public String getType() {

        return (this.type);

    }


}
