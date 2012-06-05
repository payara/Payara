package org.glassfish.admin.rest.model;

import org.glassfish.admin.rest.composite.RestModel;

public interface RelatedModel extends RestModel {
    public String getId();
    public void setId(String id);

    public String getDescription();
    public void setDescription(String desc);
}
