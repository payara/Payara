package fish.payara.test.containers.app.xatxcorba.entities;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author fabio
 */
public class DataPackage implements Serializable{

    private Integer id;
    private String contents;

    public DataPackage() {
    }

    public DataPackage(Integer id, String contents) {
        this.id = id;
        this.contents = contents;
    }

    public Integer getId() {
        return id;
    }

    public String getContents() {
        return contents;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DataPackage other = (DataPackage) obj;
        return Objects.equals(this.id, other.id);
    }

    @Override
    public String toString() {
        return "DataPackage{" + "id=" + id + ", contents=" + contents + '}';
    }
}
