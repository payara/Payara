package fish.payara.samples.data.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Test entity for Bean Validation scenarios in Jakarta Data TCK.
 * This entity is designed specifically for testing validation constraints.
 */
@Entity
@Table(name = "rectangles")
public class Rectangle {

    @Id
    @NotNull
    public UUID id;

    @Column(nullable = false)
    @NotNull(message = "Name cannot be null")
    public String name;

    @Column(nullable = false)
    @NotNull(message = "Width cannot be null")
    @Min(value = 1, message = "Width must be at least 1")
    public Integer width;

    @Column(nullable = false)
    @NotNull(message = "Height cannot be null") 
    @Min(value = 1, message = "Height must be at least 1")
    public Integer height;

    // Default constructor for JPA
    public Rectangle() {}

    // Constructor for creating instances
    public Rectangle(UUID id, String name, Integer width, Integer height) {
        this.id = id;
        this.name = name;
        this.width = width;
        this.height = height;
    }

    // Static factory method for convenience
    public static Rectangle of(UUID id, String name, Integer width, Integer height) {
        return new Rectangle(id, name, width, height);
    }

    /**
     * Calculate the area of the rectangle.
     */
    public int getArea() {
        return (width != null && height != null) ? width * height : 0;
    }

    /**
     * Check if this is a square.
     */
    public boolean isSquare() {
        return width != null && height != null && width.equals(height);
    }

    @Override
    public String toString() {
        return String.format("Rectangle{id=%s, name='%s', width=%d, height=%d}", 
                           id, name, width, height);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Rectangle rectangle = (Rectangle) obj;
        return id != null ? id.equals(rectangle.id) : rectangle.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}