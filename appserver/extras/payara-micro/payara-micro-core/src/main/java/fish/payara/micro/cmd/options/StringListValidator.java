/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.micro.cmd.options;

/**
 * A validator that checks whether an option is in a list of allowed values
 * @since 5.0
 * @author steve
 */
public class StringListValidator extends Validator {
    
    private String allowedValues[];

    public StringListValidator(String ... allowedValues) {
        this.allowedValues = allowedValues;
    }

    @Override
    boolean validate(String optionValue) throws ValidationException {
        boolean result = false;
        for (String allowedValue : allowedValues) {
            if (allowedValue.equals(optionValue)) {
                result = true;
                break;
            }
        }
        return result;
    }
}
