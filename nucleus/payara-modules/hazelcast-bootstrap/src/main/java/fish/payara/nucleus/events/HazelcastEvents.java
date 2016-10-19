/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.nucleus.events;

import org.glassfish.api.event.EventTypes;

/**
 *
 * @author Andrew Pielage
 */
public class HazelcastEvents
{
    public static final EventTypes HAZELCAST_BOOTSTRAP_COMPLETE = EventTypes.create("hazelcast_bootstrap_complete");
    public static final EventTypes HAZELCAST_SHUTDOWN_COMPLETE = EventTypes.create("hazelcast_shutdown_complete");
}
