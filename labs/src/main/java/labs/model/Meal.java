package labs.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "meals")
public class Meal {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "meals_seq")
    @SequenceGenerator(name = "meals_seq", sequenceName = "meals_seq", allocationSize = 1)
    private int id;

    @Column(name = "meal_type")
    private String mealType;

    @JsonBackReference
    @ManyToOne(cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "day_id")
    private Day day;

    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinTable(name = "meal_product",
            joinColumns = @JoinColumn(name = "meal_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "product_id", referencedColumnName = "id"))
    private List<Product> products;

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Meal meal = (Meal) o;
//        return id == product.id && Float.compare(weight, product.weight) == 0 && Float.compare(calories, product.calories) == 0 && Float.compare(proteins, product.proteins) == 0 && Float.compare(carbs, product.carbs) == 0 && Float.compare(fats, product.fats) == 0 && Objects.equals(name, product.name) && Objects.equals(meals, product.meals);
        return id == meal.id;
    }

//    public int sizeInBytes() {
//        if (StackWalker.getInstance()
//                .walk(frames -> frames
//                        .filter(f -> f.getClassName().equals("Meal") &&
//                                f.getMethodName().equals("sizeInBytes")).count()) == 1) {
//            return Integer.SIZE / 8 + mealType.length() * 2 + day.sizeInBytes() + products.stream().mapToInt(Product::sizeInBytes).sum();
//        }
//        return 0;
//    }
}