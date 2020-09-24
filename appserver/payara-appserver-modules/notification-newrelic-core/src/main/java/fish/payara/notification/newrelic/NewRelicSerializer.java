package fish.payara.notification.newrelic;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import fish.payara.internal.notification.PayaraNotification;

/**
 * A custom serializer that determines what to write to the new relic endpoint
 */
public class NewRelicSerializer extends StdSerializer<PayaraNotification> {

    private static final long serialVersionUID = 1L;

    private NewRelicSerializer() {
        super((Class<PayaraNotification>) null);
    }

    @Override
    public void serialize(PayaraNotification value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("eventType", value.getEventType());
        gen.writeStringField("serverName", value.getServerName());
        gen.writeStringField("hostName", value.getHostName());
        gen.writeStringField("domainName", value.getDomainName());
        gen.writeStringField("instanceName", value.getInstanceName());
        gen.writeStringField("subject", getDetailedSubject(value));
        gen.writeObjectField("data", value.getData());
        gen.writeEndObject();
    }

    private static String getDetailedSubject(PayaraNotification event) {
        return String.format("%s. (host: %s, server: %s, domain: %s, instance: %s)", 
                event.getSubject(),
                event.getHostName(),
                event.getServerName(),
                event.getDomainName(),
                event.getInstanceName());
    }

    public static Module createModule() {
        SimpleModule module = new SimpleModule("NewRelicModule");
        module.addSerializer(PayaraNotification.class, new NewRelicSerializer());
        return module;
    }

}
