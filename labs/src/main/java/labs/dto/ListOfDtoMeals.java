package labs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class ListOfDtoMeals {
    @Schema(description = "List of meals to add with bulk operation")
    @Valid
    @NotNull(message = "List of meals should be filled")
    @Size(max = 3, min = 1, message = "List of meals should contain from 1 to 3 meals")
    private List<MealDto> meals;
}
