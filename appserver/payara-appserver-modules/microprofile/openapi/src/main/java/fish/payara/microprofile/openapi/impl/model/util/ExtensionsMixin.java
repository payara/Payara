package fish.payara.microprofile.openapi.impl.model.util;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class ExtensionsMixin {

    @JsonAnyGetter
    public abstract Map<String, Object> getExtensions();

    @JsonAnySetter
    public abstract void addExtension(String name, Object value);

    @JsonProperty("enum")
    public abstract void getEnumeration();

    @JsonProperty("default")
    public abstract void getDefaultValue();

    @JsonProperty("$ref")
    public abstract void getRef();

    // TODO: Fix ignored additional properties
    @JsonIgnore
    public abstract void setAdditionalProperties(Boolean additionalProperties);

}