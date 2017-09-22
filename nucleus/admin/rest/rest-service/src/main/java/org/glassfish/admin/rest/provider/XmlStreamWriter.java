package org.glassfish.admin.rest.provider;

import java.io.IOException;
import java.io.OutputStream;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.glassfish.admin.rest.Constants;

public class XmlStreamWriter implements StreamWriter {

    private final String prefix;
    private final String postfix;
    private final OutputStream os;
    private final XMLStreamWriter writer;

    public XmlStreamWriter(OutputStream os, String prefix, String postfix) throws XMLStreamException {
        this.prefix = prefix;
        this.postfix = postfix;
        this.os = os;
        writer = XMLOutputFactory.newInstance().createXMLStreamWriter(os, Constants.ENCODING);
    }

    public XmlStreamWriter(OutputStream os) throws XMLStreamException {
        this(os, null, null);
    }

    @Override
    public String getPostfix() {
        return postfix;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void writeStartDocument() throws XMLStreamException, IOException {
        if (prefix != null) {
            os.write(prefix.getBytes(Constants.ENCODING));
        }
        writer.writeStartDocument();
    }

    @Override
    public void writeEndDocument() throws XMLStreamException, IOException {
        writer.writeEndDocument();
        if (postfix != null) {
            os.write(postfix.getBytes(Constants.ENCODING));
        }
    }

    @Override
    public void writeStartObject(String element) throws XMLStreamException {
        writer.writeStartElement(element);
    }

    @Override
    public void writeEndObject() throws XMLStreamException {
        writer.writeEndElement();
    }

    @Override
    public void write(String value) throws XMLStreamException {
        writer.writeCharacters(value);
    }

    @Override
    public void writeAttribute(String name, String value) throws XMLStreamException {
        writer.writeAttribute(name, value);
    }

    @Override
    public void close() throws XMLStreamException {
        writer.close();
    }

    @Override
    public void writeStartArray(String element) throws Exception {
        // No arrays in XML
    }

    @Override
    public void writeEndArray() throws Exception {
        // No arrays in XML
    }

}
