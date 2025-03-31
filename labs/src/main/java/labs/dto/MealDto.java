package labs.dto;

import java.util.List;
import labs.model.Meal;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MealDto {
    private int id;
    private String mealType;
    private int dayId;
    private List<ProductDto> products;

    public Meal fromDto() {
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
}
