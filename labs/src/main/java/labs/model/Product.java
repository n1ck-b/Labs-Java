package labs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.List;
import java.util.Objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "products_seq")
    @SequenceGenerator(name = "products_seq", sequenceName = "products_seq", allocationSize = 1)
    private int id;

    @Column(name = "name")
    private String name;

    @JsonProperty("serving_size_g")
    @Column(name = "weight")
    private float weight;

    @JsonProperty("calories")
    @Column(name = "calories")
    private float calories;

    @JsonProperty("protein_g")
    @Column(name = "proteins")
    private float proteins;

    @JsonProperty("carbohydrates_total_g")
    @Column(name = "carbs")
    private float carbs;

    @JsonProperty("fat_total_g")
    @Column(name = "fats")
    private float fats;

    @JsonIgnore
    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST})
    @JoinTable(name = "meal_product",
            joinColumns = @JoinColumn(name = "product_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "meal_id", referencedColumnName = "id"))
    private List<Meal> meals;

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
//        return id == product.id && Float.compare(weight, product.weight) == 0 && Float.compare(calories, product.calories) == 0 && Float.compare(proteins, product.proteins) == 0 && Float.compare(carbs, product.carbs) == 0 && Float.compare(fats, product.fats) == 0 && Objects.equals(name, product.name) && Objects.equals(meals, product.meals);
        return id == product.id;
    }

//    public int sizeInBytes() {
//        return Integer.SIZE / 8 + name.length() * 2 + (Float.SIZE / 8) * 5 + meals.stream().mapToInt(Meal::sizeInBytes).sum();
//    }
}
