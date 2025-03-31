package labs.dto;

import java.util.List;
import labs.model.Meal;
import labs.model.Product;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ProductDto {
    private int id;
    private String name;
    private List<Integer> mealIds;
    private float weight;
    private float calories;
    private float proteins;
    private float carbs;
    private float fats;

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
