package org.glassfish.admin.rest.provider;

public interface StreamWriter {

    public static enum WriterType {
        XML, JSON
    }

    public String getPostfix();

    public String getPrefix();

    public void writeStartDocument() throws Exception;

    public void writeEndDocument() throws Exception;

    public void writeStartObject(String element) throws Exception;

    public void writeEndObject() throws Exception;

    public void writeStartArray(String element) throws Exception;

    public void writeEndArray() throws Exception;

    public void write(String value) throws Exception;

    public void writeAttribute(String name, String value) throws Exception;

    public void close() throws Exception;

}
