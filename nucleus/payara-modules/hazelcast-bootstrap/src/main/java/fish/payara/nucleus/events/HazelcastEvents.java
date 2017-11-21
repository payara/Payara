/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.events;

import org.glassfish.api.event.EventTypes;

/**
 * Class defining Hazelcast events
 * @author Andrew Pielage
 */
public class HazelcastEvents
{
    public static final EventTypes HAZELCAST_BOOTSTRAP_COMPLETE = EventTypes.create("hazelcast_bootstrap_complete");
    public static final EventTypes HAZELCAST_SHUTDOWN_COMPLETE = EventTypes.create("hazelcast_shutdown_complete");
    public static final EventTypes HAZELCAST_GENERATED_NAME_CHANGE = EventTypes.create("hazelcast_generated_name_change");
}
