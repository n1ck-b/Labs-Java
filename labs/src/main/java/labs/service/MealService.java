package labs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import java.util.List;
import labs.dto.MealDto;
import org.springframework.http.ResponseEntity;

public interface MealService {
    List<MealDto> getMealsByDayId(int dayId);

    int addMeal(int dayId, MealDto meal);

    ResponseEntity<String> deleteMealsByDayId(int dayId);

    ResponseEntity<String> deleteMealByDayIdAndMealId(int dayId, int mealId);

    ResponseEntity<MealDto> updateMealById(JsonPatch json, int id) throws
            JsonPatchException, JsonProcessingException;

    List<MealDto> getAllMeals();

    MealDto getMealById(int id);

    ResponseEntity<String> deleteMealById(int id);

    List<MealDto> getMealsByProductName(String productName);
}
