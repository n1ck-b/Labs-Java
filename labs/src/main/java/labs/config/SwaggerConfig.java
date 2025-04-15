package labs.config;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@OpenAPIDefinition(info = @Info (
        title = "Calorie calculation API",
        version = "1.0",
        description = "Service for calculation of calories, proteins, fats and carbs of food"),
        externalDocs = @ExternalDocumentation(
                description = "External API, that is used to get data about products",
                url = "https://calorieninjas.com/api"
        )
)
public class SwaggerConfig {
}
