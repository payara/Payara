package fish.payara.microprofile.openapi.test.app.application.schema;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

// FIXME remove when we will know, how it differs from Schema2Simple
@Schema(name = "ShipmentData", description = "Shipment data", implementation = ShipmentData.class)
public class ShipmentData {

    @Schema(name = "salutation", example = "MR", description = "Salutation of the delivery address contact person", enumeration = {"MR", "MS"}, implementation = String.class)
    private String salutation;

    @Schema(name = "quantity", example = "1", description = "Item quantity", implementation = Integer.class)
    private Integer quantity;

//    @Schema(name = "partner", description = "what will happen?", implementation = Partner.class)
//    private Partner partner;

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public void setSalutation(String salutation) {
        this.salutation = salutation;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getSalutation() {
        return salutation;
    }

//    public Partner getPartner() {
//        return partner;
//    }
//
//    public void setPartner(Partner partner) {
//        this.partner = partner;
//    }

}
