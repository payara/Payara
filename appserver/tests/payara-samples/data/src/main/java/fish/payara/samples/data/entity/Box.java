package fish.payara.samples.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;

/**
 * Test entity demonstrating custom ID field name for Jakarta Data TCK scenarios.
 * This entity uses 'code' as the ID field instead of the conventional 'id'.
 */
@Entity
@Table(name = "boxes")
public class Box {

    /**
     * Primary key with custom field name 'code' instead of 'id'.
     * This tests the Jakarta Data capability to handle ID attributes with different names.
     */
    @Id
    @Column(name = "box_code")
    public String code;

    @Column(name = "name")
    public String name;

    @Column(name = "description")
    public String description;

    @Column(name = "width_cm")
    public Integer widthCm;

    @Column(name = "height_cm")
    public Integer heightCm;

    @Column(name = "depth_cm")
    public Integer depthCm;

    @Column(name = "material")
    public String material; // e.g., "CARDBOARD", "WOOD", "PLASTIC", "METAL"

    @Column(name = "color")
    public String color;

    @Column(name = "is_stackable")
    public Boolean stackable;

    // Default constructor for JPA
    public Box() {}

    // Constructor for creating instances
    public Box(String code, String name, String description, Integer widthCm, 
               Integer heightCm, Integer depthCm, String material, String color, Boolean stackable) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.widthCm = widthCm;
        this.heightCm = heightCm;
        this.depthCm = depthCm;
        this.material = material;
        this.color = color;
        this.stackable = stackable;
    }

    // Static factory method for convenience
    public static Box of(String code, String name, String description, Integer widthCm, 
                        Integer heightCm, Integer depthCm, String material, String color, Boolean stackable) {
        return new Box(code, name, description, widthCm, heightCm, depthCm, material, color, stackable);
    }
    
    // Simplified static factory method for tests
    public static Box of(String code, String name, String material, String color, 
                        int widthCm, int heightCm, int depthCm, boolean stackable) {
        return new Box(code, name, name + " description", widthCm, heightCm, depthCm, material, color, stackable);
    }

    /**
     * Calculate the volume of the box in cubic centimeters.
     */
    public long getVolume() {
        if (widthCm != null && heightCm != null && depthCm != null) {
            return (long) widthCm * heightCm * depthCm;
        }
        return 0;
    }

    /**
     * Check if this is a cube (all dimensions are equal).
     */
    public boolean isCube() {
        return widthCm != null && heightCm != null && depthCm != null &&
               widthCm.equals(heightCm) && heightCm.equals(depthCm);
    }

    /**
     * Get the surface area of the box.
     */
    public long getSurfaceArea() {
        if (widthCm != null && heightCm != null && depthCm != null) {
            return 2L * (widthCm * heightCm + heightCm * depthCm + depthCm * widthCm);
        }
        return 0;
    }

    /**
     * Check if the box is small (volume less than 1000 cubic cm).
     */
    public boolean isSmall() {
        return getVolume() < 1000;
    }

    /**
     * Check if the box is large (volume greater than 100000 cubic cm).
     */
    public boolean isLarge() {
        return getVolume() > 100000;
    }

    @Override
    public String toString() {
        return String.format("Box{code='%s', name='%s', dimensions=%dx%dx%d cm, material='%s', color='%s', stackable=%s}", 
                           code, name, widthCm, heightCm, depthCm, material, color, stackable);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Box box = (Box) obj;
        return code != null ? code.equals(box.code) : box.code == null;
    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }
}