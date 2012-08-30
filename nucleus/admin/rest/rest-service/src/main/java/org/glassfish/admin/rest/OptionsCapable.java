/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.admin.rest;

import javax.ws.rs.core.UriInfo;

/**
 *
 * @author jdlee
 */
public interface OptionsCapable {
    UriInfo getUriInfo();
    void setUriInfo(UriInfo uriInfo);
}
