
package fromjava.client;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the fromjava.client package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _AddNumbers_QNAME = new QName("http://server.fromjava/", "addNumbers");
    private final static QName _AddNumbersResponse_QNAME = new QName("http://server.fromjava/", "addNumbersResponse");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: fromjava.client
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link AddNumbers_Type }
     * 
     */
    public AddNumbers_Type createAddNumbers_Type() {
        return new AddNumbers_Type();
    }

    /**
     * Create an instance of {@link AddNumbersResponse }
     * 
     */
    public AddNumbersResponse createAddNumbersResponse() {
        return new AddNumbersResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddNumbers_Type }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.fromjava/", name = "addNumbers")
    public JAXBElement<AddNumbers_Type> createAddNumbers(AddNumbers_Type value) {
        return new JAXBElement<AddNumbers_Type>(_AddNumbers_QNAME, AddNumbers_Type.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddNumbersResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://server.fromjava/", name = "addNumbersResponse")
    public JAXBElement<AddNumbersResponse> createAddNumbersResponse(AddNumbersResponse value) {
        return new JAXBElement<AddNumbersResponse>(_AddNumbersResponse_QNAME, AddNumbersResponse.class, null, value);
    }

}
