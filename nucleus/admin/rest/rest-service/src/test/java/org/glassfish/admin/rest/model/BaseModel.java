package org.glassfish.admin.rest.model;

import org.glassfish.admin.rest.composite.RestModel;

import java.util.List;

public interface BaseModel extends RestModel {
    public String getName();
    public void setName(String name);

    public int getCount();
    public void setCount(int count);

    public List<RelatedModel> getRelated();
    public void setRelated(List<RelatedModel> related);
}
