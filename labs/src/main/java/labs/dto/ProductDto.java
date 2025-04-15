package labs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import labs.exception.ExceptionMessages;
import labs.model.Meal;
import labs.model.Product;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProductDto {
    @Schema(description = "ID of the product", accessMode = Schema.AccessMode.READ_ONLY)
    @Null(message = ExceptionMessages.ID_VALIDATION_ERROR)
    private Integer id;

    @Schema(description = "Name of the product")
    @NotNull
    @NotBlank
    private String name;

    @Schema(description = "List of meal IDs this product belongs to",
            accessMode = Schema.AccessMode.READ_ONLY)
    @Size(max = 0, message = "List of meals shouldn't be filled")
    private List<Integer> mealIds;

    @Schema(description = "Weight of the product in grams")
    @NotNull
    @Min(1)
    @Positive
    private Float weight;

    @Schema(description = "Calories of the product")
    @NotNull
    @Positive
    private Float calories;

    @Schema(description = "Proteins of the product in grams")
    @NotNull
    @Positive
    private Float proteins;

    @Schema(description = "Carbohydrates of the product in grams")
    @NotNull
    @Positive
    private Float carbs;

    @Schema(description = "Fats of the product in grams")
    @NotNull
    @Positive
    private Float fats;

    public static ProductDto toDto(Product product) {
        ProductDto productDto = new ProductDto();
        productDto.setId(product.getId());
        productDto.setName(product.getName());
        productDto.setWeight(product.getWeight());
        productDto.setCalories(product.getCalories());
        productDto.setProteins(product.getProteins());
        productDto.setCarbs(product.getCarbs());
        productDto.setFats(product.getFats());
        List<Meal> meals = product.getMeals();
        if (meals != null) {
            productDto.setMealIds(meals.stream().map(Meal::getId).toList());
        }
        return productDto;
    }

    public Product fromDto() {
        Product product = new Product();
        if (this.id == null) {
            this.id = 0;
        }
        product.setId(this.id);
        product.setName(this.name);
        product.setWeight(this.weight);
        product.setCalories(this.calories);
        product.setProteins(this.proteins);
        product.setCarbs(this.carbs);
        product.setFats(this.fats);
        return product;
    }
}
