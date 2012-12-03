/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest.composite;

import org.glassfish.api.ActionReport;

/**
 *
 * @author jdlee
 */
public interface EntityBuilder {
    RestModel get(ActionReport report);
}
