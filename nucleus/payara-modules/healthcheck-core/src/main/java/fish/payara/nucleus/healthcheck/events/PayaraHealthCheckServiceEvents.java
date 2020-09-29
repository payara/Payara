/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.nucleus.healthcheck.events;

import fish.payara.nucleus.healthcheck.HealthCheckTask;
import org.glassfish.api.event.EventTypes;

/**
 *
 * @author Susan Rai
 */
public class PayaraHealthCheckServiceEvents {
    // public static final EventTypes<ApplicationInfo> APPLICATION_STOPPED;
     public static final EventTypes<HealthCheckTask> HEALTHCHECK_SERVICE_DISPLAY_ON_HEALTH_ENDPOINT_STARTED = EventTypes.create("healthcheck_display_on_health_endpoint_started", HealthCheckTask.class);
     public static final EventTypes<HealthCheckTask> HEALTHCHECK_SERVICE_DISPLAY_ON_HEALTH_ENDPOINT_STOPED = EventTypes.create("healthcheck_display_on_health_endpoint_stoped", HealthCheckTask.class);
     public static final EventTypes HEALTHCHECK_SERVICE_DISABLED = EventTypes.create("healthcheck_service_disabled");
}
