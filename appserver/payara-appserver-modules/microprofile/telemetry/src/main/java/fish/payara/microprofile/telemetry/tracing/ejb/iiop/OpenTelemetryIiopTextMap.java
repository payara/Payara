package fish.payara.microprofile.telemetry.tracing.ejb.iiop;

import java.io.Serializable;
import java.util.HashMap;

import static fish.payara.microprofile.telemetry.tracing.ejb.iiop.OpenTelemetryIiopInterceptorFactory.OPENTRACING_IIOP_SERIAL_VERSION_UID;

public class OpenTelemetryIiopTextMap extends HashMap<String, String> implements Serializable {

    private static final long serialVersionUID = OPENTRACING_IIOP_SERIAL_VERSION_UID;
    
    @Override
    public String put(String key, String value) {
        return super.put(key, value);
    }
}
