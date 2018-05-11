package fish.payara.microprofile.openapi.impl.model.media;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.media.Encoding;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

public class EncodingImpl extends ExtensibleImpl implements Encoding {

    protected String contentType;
    protected Map<String, Header> headers = new HashMap<>();
    protected Style style;
    protected Boolean explode;
    protected Boolean allowReserved;

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public Encoding contentType(String contentType) {
        setContentType(contentType);
        return this;
    }

    @Override
    public Map<String, Header> getHeaders() {
        return headers;
    }

    @Override
    public void setHeaders(Map<String, Header> headers) {
        this.headers = headers;
    }

    @Override
    public Encoding headers(Map<String, Header> headers) {
        setHeaders(headers);
        return this;
    }

    @Override
    public Style getStyle() {
        return style;
    }

    @Override
    public void setStyle(Style style) {
        this.style = style;
    }

    @Override
    public Encoding style(Style style) {
        setStyle(style);
        return this;
    }

    @Override
    public Boolean getExplode() {
        return explode;
    }

    @Override
    public void setExplode(Boolean explode) {
        this.explode = explode;
    }

    @Override
    public Encoding explode(Boolean explode) {
        setExplode(explode);
        return this;
    }

    @Override
    public Boolean getAllowReserved() {
        return allowReserved;
    }

    @Override
    public void setAllowReserved(Boolean allowReserved) {
        this.allowReserved = allowReserved;
    }

    @Override
    public Encoding allowReserved(Boolean allowReserved) {
        setAllowReserved(allowReserved);
        return this;
    }

}
