package labs.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Setter
@Getter
@NoArgsConstructor
@Entity
@Table(name = "days")
public class Day {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "days_seq")
    @SequenceGenerator(name = "days_seq", sequenceName = "days_seq", allocationSize = 1)
    private int id;

    @Column(name = "date")
    private LocalDate date;

    //@OnDelete(action = OnDeleteAction.CASCADE)
    @JsonManagedReference
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "day_id")
    private List<Meal> meals;

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Day day = (Day) o;
//        return id == product.id && Float.compare(weight, product.weight) == 0 && Float.compare(calories, product.calories) == 0 && Float.compare(proteins, product.proteins) == 0 && Float.compare(carbs, product.carbs) == 0 && Float.compare(fats, product.fats) == 0 && Objects.equals(name, product.name) && Objects.equals(meals, product.meals);
        return id == day.id;
    }

//    public int sizeInBytes() {
//        return Integer.SIZE / 8 + 16 + meals.stream().mapToInt(Meal::sizeInBytes).sum();
//    }
}
