package labs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import labs.exception.ExceptionMessages;
import labs.model.Meal;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MealDto {
    @Schema(description = "ID of the meal", accessMode = Schema.AccessMode.READ_ONLY)
    @Null(message = ExceptionMessages.ID_VALIDATION_ERROR)
    private Integer id;

    @Schema(description = "Type of the meal", allowableValues = {"breakfast", "lunch", "dinner"})
    @NotNull
    @NotBlank
    private String mealType;

    @Schema(description = "ID of the day this meal belongs to", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer dayId;

    @Schema(description = "Product that belong to this meal", accessMode = Schema.AccessMode.READ_ONLY)
    @Size(max = 0, message = "List of products shouldn't be filled")
    private List<ProductDto> products;

    public Meal fromDto() {
        if (this.id == null) {
            this.id = 0;
        }
        if (this.products == null) {
            products = new ArrayList<>();
        }
        Meal meal = new Meal();
        meal.setId(this.id);
        meal.setMealType(this.mealType);
        meal.setProducts(this.products.stream().map(ProductDto::fromDto).toList());
        return meal;
    }

    public static MealDto toDto(Meal meal) {
        MealDto mealDto = new MealDto();
        mealDto.setId(meal.getId());
        mealDto.setMealType(meal.getMealType());
        mealDto.setProducts(meal.getProducts().stream().map(ProductDto::toDto).toList());
        mealDto.setDayId(meal.getDay().getId());
        return mealDto;
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || getClass() != object.getClass()) return false;
        MealDto mealDto = (MealDto) object;
        return Objects.equals(id, mealDto.id) && Objects.equals(mealType, mealDto.mealType) &&
                Objects.equals(dayId, mealDto.dayId) && Objects.equals(products, mealDto.products);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, mealType, dayId, products);
    }
}
