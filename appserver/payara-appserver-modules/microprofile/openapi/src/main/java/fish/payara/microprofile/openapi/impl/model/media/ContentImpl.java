package fish.payara.microprofile.openapi.impl.model.media;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema;

public class ContentImpl extends LinkedHashMap<String, MediaType> implements Content {

    private static final long serialVersionUID = 1575356277308242221L;

    @Override
    public Content addMediaType(String name, MediaType item) {
        this.put(name, item);
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.media.Content from, Content to,
            boolean override, Map<String, Schema> currentSchemas) {
        if (from == null) {
            return;
        }
        if (!isAnnotationNull(from.schema()) || from.encoding().length > 0) {
            // Get the name of the media type
            String typeName = from.mediaType();
            if (typeName == null || typeName.isEmpty()) {
                typeName = javax.ws.rs.core.MediaType.WILDCARD;
            }

            // Get the current mediaType, or create a new one
            MediaType mediaType = to.getOrDefault(typeName, new MediaTypeImpl());
            to.put(typeName, mediaType);

            if (!isAnnotationNull(from.schema())) {
                // Get the current schema, or create a new one
                Schema schema = mediaType.getSchema();
                if (schema == null) {
                    schema = new SchemaImpl();
                    mediaType.setSchema(schema);
                }
                SchemaImpl.merge(from.schema(), schema, override, currentSchemas);
            }
        }
    }

}
