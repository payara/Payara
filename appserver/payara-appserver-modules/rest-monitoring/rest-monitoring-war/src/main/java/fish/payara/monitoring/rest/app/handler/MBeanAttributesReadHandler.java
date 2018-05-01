package fish.payara.monitoring.rest.app.handler;

import java.util.Arrays;

import javax.inject.Singleton;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.ws.rs.core.Response;

import fish.payara.monitoring.rest.app.MBeanServerDelegate;
import fish.payara.monitoring.rest.app.RestMonitoringAppResponseToken;
import fish.payara.monitoring.rest.app.processor.ProcessorFactory;
import fish.payara.monitoring.rest.app.processor.TypeProcessor;

/**
 * @author Krassimir Valev
 */
public class MBeanAttributesReadHandler extends ReadHandler {

    private final String mbeanname;
    private final String[] attributenames;

    /**
     * Creates an instance of MBeanAttributeReadHandler, which handles bulk MBean
     * attribute read requests.
     *
     * @param delegate
     *            The {@link MBeanServerDelegate} to get information from.
     * @param mbeanname
     *            The {@link ObjectName} of the MBean to get information from.
     * @param attributename
     *            The name of the MBean attribute to get values for.
     */
    public MBeanAttributesReadHandler(@Singleton final MBeanServerDelegate delegate,
            final String mbeanname, final String[] attributenames) {
        super(delegate);
        this.mbeanname = mbeanname;
        this.attributenames = attributenames;
    }

    @Override
    public JsonObject getRequestObject() {
        JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
        try {
            objectBuilder.add(RestMonitoringAppResponseToken.getMbeanNameKey(), mbeanname);
            objectBuilder.add(RestMonitoringAppResponseToken.getAttributeNameKey(),
                    Json.createArrayBuilder(Arrays.asList(attributenames)));
            objectBuilder.add(RestMonitoringAppResponseToken.getRequestTypeKey(), requesttype);
        } catch (JsonException ex) {
            super.setStatus(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return objectBuilder.build();
    }

    @Override
    public JsonValue getValueObject() throws JsonException {
        try {
            AttributeList attributes = delegate.getMBeanAttributes(mbeanname, attributenames);

            // the javax.management.Attribute type does not inherit from OpenType<T>, so the existing
            // ProcessorFactory and TypeProcessor infrastructure cannot be used
            JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();

            for (Attribute attribute : attributes.asList()) {
                TypeProcessor<?> processor = ProcessorFactory.getTypeProcessor(attribute.getValue());

                JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
                objectBuilder.add(attribute.getName(), processor.processObject(attribute.getValue()));
                arrayBuilder.add(objectBuilder);
            }

            return arrayBuilder.build();
        } catch (InstanceNotFoundException | ReflectionException | MalformedObjectNameException ex) {
            super.setStatus(Response.Status.NOT_FOUND);
            return getTraceObject(ex);
        }
    }
}
