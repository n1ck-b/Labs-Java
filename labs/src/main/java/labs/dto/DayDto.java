package labs.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import labs.Day;
import lombok.Data;

@Data
public class DayDto {
    private int id;
    private LocalDate date;
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
        Day day = new Day();
        day.setId(this.id);
        day.setDate(this.date);
        day.setMeals(this.meals.stream().map(MealDto::fromDto).toList());
        return day;
    }
}
