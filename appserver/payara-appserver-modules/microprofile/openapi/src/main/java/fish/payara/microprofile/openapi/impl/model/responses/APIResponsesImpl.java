package fish.payara.microprofile.openapi.impl.model.responses;

import java.util.LinkedHashMap;

import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;

public class APIResponsesImpl extends LinkedHashMap<String, APIResponse> implements APIResponses {

    private static final long serialVersionUID = 2811935761440110541L;

	@Override
    public APIResponses addApiResponse(String name, APIResponse item) {
        put(name, item);
        return this;
    }

    @Override
    public APIResponse getDefault() {
        return this.get(DEFAULT);
    }

    @Override
    public void setDefaultValue(APIResponse defaultValue) {
        addApiResponse(DEFAULT, defaultValue);
    }

    @Override
    public APIResponses defaultValue(APIResponse defaultValue) {
        setDefaultValue(defaultValue);
        return this;
    }

}
