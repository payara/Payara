/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest.composite;

/**
 *
 * @author jdlee
 */
public interface Property extends RestModel {
    String getName();
    void setName(String name);

    String getValue();
    void setValue(String value);

    String getDescription();
    void setDescription(String description);
}
