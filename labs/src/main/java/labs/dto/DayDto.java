package labs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import labs.exception.ExceptionMessages;
import labs.model.Day;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DayDto {
    @Schema(description = "ID of the day", accessMode = Schema.AccessMode.READ_ONLY)
    @Null(message = ExceptionMessages.ID_VALIDATION_ERROR)
    private Integer id;

    @Schema(description = "Date in ISO format the day belongs to", example = "2025-04-01")
    @NotNull
    private LocalDate date;

    @Schema(description = "Meals that belong to this day", accessMode = Schema.AccessMode.READ_ONLY)
    private List<MealDto> meals;

    public static DayDto toDto(Day day) {
        DayDto dto = new DayDto();
        dto.setId(day.getId());
        dto.setDate(day.getDate());
        List<MealDto> meals = day.getMeals().stream().map(MealDto::toDto).collect(Collectors.toList());
        dto.setMeals(meals);
        return dto;
    }

    public Day fromDto() {
        if (this.id == null) {
            this.id = 0;
        }
        Day day = new Day();
        day.setId(this.id);
        day.setDate(this.date);
        day.setMeals(this.meals.stream().map(MealDto::fromDto).toList());
        return day;
    }
}
