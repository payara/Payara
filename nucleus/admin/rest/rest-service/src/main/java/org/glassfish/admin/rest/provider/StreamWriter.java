package org.glassfish.admin.rest.provider;

/**
 * Defines an interface that will handle streaming a data structure to an
 * {@link java.io.OutputStream}.
 */
public interface StreamWriter {

    /**
     * Returns a string of data that must appear at the end of the data stream.
     *
     * @return postfix
     */
    public String getPostfix();

    /**
     * Returns a string of data that must appear at the start of the data
     * stream. E.g. a namespace declaration.
     *
     * @return prefix
     */
    public String getPrefix();

    /**
     * Writes the data that is compulsory to appear at the start of the data
     * structure.
     * <br>
     * E.g. for XML this might be: {@code <?xml version="1.0" ?>}
     *
     * @throws Exception Any exception thrown while writing. E.g. for JSON this
     * might be a
     * {@link javax.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void writeStartDocument() throws Exception;

    /**
     * Writes the data that is compulsory to appear at the end of the data
     * structure.
     *
     * @throws Exception Any exception thrown while writing. E.g. for JSON this
     * might be a
     * {@link javax.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void writeEndDocument() throws Exception;

    /**
     * Writes the start of a new object, with the specified {@code name}.
     *
     * @param element The name of the object
     * @throws Exception Any exception thrown while writing. E.g. for JSON this
     * might be a
     * {@link javax.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void writeStartObject(String element) throws Exception;

    /**
     * Writes the end of an object. An object must have been started, or an
     * exception will be thrown.
     *
     * @throws Exception Any exception thrown while writing. E.g. for JSON this
     * might be a
     * {@link javax.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void writeEndObject() throws Exception;

    /**
     * Writes the start of an array, with the specified {@code name}. If the
     * data structure doesn't support arrays, this method will do nothing.
     *
     * @param element The name of the array
     * @throws Exception Any exception thrown while writing. E.g. for JSON this
     * might be a
     * {@link javax.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void writeStartArray(String element) throws Exception;

    /**
     * Writes the end of an array. An array must have been started, or an
     * exception will be thrown. If the data structure doesn't support arrays,
     * this method will do nothing.
     *
     * @throws Exception Any exception thrown while writing. E.g. for JSON this
     * might be a
     * {@link javax.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void writeEndArray() throws Exception;

    /**
     * Writes some raw data.
     *
     * @param value The data to write.
     * @throws Exception Any exception thrown while writing. E.g. for JSON this
     * might be a
     * {@link javax.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void write(String value) throws Exception;

    /**
     * Writes a {@code String} attribute with the specified {@code name} and
     * {@code value}. E.g. for JSON this will write: {@code "name":"value"}.
     *
     * @param name The name of the attribute
     * @param value The value of the attribute
     * @throws Exception Any exception thrown while writing. E.g. for JSON this
     * might be a
     * {@link javax.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void writeAttribute(String name, String value) throws Exception;

    /**
     * Writes a {@code Boolean} attribute with the specified {@code name} and
     * {@code value}. E.g. for JSON this will write: {@code "name":true/false}.
     *
     * @param name The name of the attribute
     * @param value The value of the attribute
     * @throws Exception Any exception thrown while writing. E.g. for JSON this
     * might be a
     * {@link javax.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void writeAttribute(String name, Boolean value) throws Exception;

    /**
     * Closes the {@link java.io.OutputStream} associated with this object. Some
     * OutputStreams require closing before any data will be written.
     *
     * @throws Exception Any exception thrown while closing the OutputStream.
     * E.g. for JSON this might be a
     * {@link javax.json.stream.JsonGenerationException JsonGenerationException}.
     */
    public void close() throws Exception;

}
