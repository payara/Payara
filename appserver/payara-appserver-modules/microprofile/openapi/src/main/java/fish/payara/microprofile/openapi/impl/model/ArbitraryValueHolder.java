package fish.payara.microprofile.openapi.impl.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;

import java.util.Map;

/**
 * A class can only have one @JsonAnyGetter method.
 * This interface serves as a means to decouple the annotation from Extensible, allowing implementing
 * classes to have arbitrary values without interfering with Extensions. 
 */
public interface ArbitraryValueHolder {
    @JsonAnyGetter
    Map<String, ?> getArbitraryValues();
}
