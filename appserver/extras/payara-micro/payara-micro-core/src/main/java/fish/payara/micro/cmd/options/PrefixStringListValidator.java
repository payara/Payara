/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.micro.cmd.options;

/**
 *
 * @author steve
 */
public class PrefixStringListValidator extends Validator {
    
    private String allowedValues[];

    public PrefixStringListValidator(String ... allowedValues) {
        this.allowedValues = allowedValues;
    }

    @Override
    boolean validate(String optionValue) throws ValidationException {
        boolean result = false;
        for (String allowedValue : allowedValues) {
            if (optionValue.startsWith(allowedValue)) {
                result = true;
                break;
            }
        }
        return result;
    }
}
