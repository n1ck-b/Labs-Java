package labs.dao;

import labs.model.Day;
import labs.model.Meal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MealRepository extends JpaRepository<Meal, Integer> {
    List<Meal> findAllByDay_Id(int dayId);

    @Query("SELECT m.id FROM Meal m WHERE m.day.id = :dayId")
    List<Integer> findAllMealIdByDayId(int dayId);

    void deleteAllByDay_Id(int dayId);

    @Modifying
    @Query("DELETE FROM Meal WHERE id=:mealId AND day.id = :dayId")
    void deleteMealByIdAndDayId(int mealId, int dayId);

    @Query(nativeQuery = true, value = "SELECT meal_id FROM meal_product WHERE product_id = :id")
    List<Integer> findMealIdsByProductId(int id);

    @Query(nativeQuery = true, value = "SELECT * FROM meals WHERE id in (SELECT meal_id FROM meal_product WHERE product_id = (SELECT id FROM products WHERE name = :productName))")
    List<Meal> getMealsByProductName(String productName);

    @Query("SELECT id FROM Meal")
    List<Integer> findAllMealsIds();

    @Query(nativeQuery = true, value = "SELECT id FROM meals WHERE id in (SELECT meal_id FROM meal_product WHERE product_id = (SELECT id FROM products WHERE name = :productName))")
    List<Integer> getMealIdsByProductName(String productName);
}
