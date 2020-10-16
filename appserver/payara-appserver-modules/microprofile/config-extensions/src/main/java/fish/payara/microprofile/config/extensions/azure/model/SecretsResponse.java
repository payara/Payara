/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.microprofile.config.extensions.azure.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 *
 * @author SusanRai
 */
public class SecretsResponse {

    public SecretsResponse() {
    }

    private List<Secret> value;

    public List<Secret> getValue() {
        return value;
    }

    public void setValue(List<Secret> value) {
        this.value = value;
    }
}
