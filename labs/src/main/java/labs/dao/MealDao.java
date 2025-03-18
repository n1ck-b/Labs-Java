package labs.dao;

import java.util.List;
import labs.Meal;
import org.springframework.http.ResponseEntity;

public interface MealDao {
    Meal getMealById(int id);

    List<Meal> getMealsByDayId(int dayId);

    int addMeal(int dayId, Meal meal);

    ResponseEntity<String> deleteMealsByDayId(int dayId);

    ResponseEntity<String> deleteMealsByDayIdAndMealId(int dayId, int mealId);

    Meal updateMeal(int id, Meal updatedMeal);

    List<Meal> getAllMeals();

    ResponseEntity<String> deleteMealById(int id);
}
