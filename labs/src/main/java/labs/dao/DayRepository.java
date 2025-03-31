package labs.dao;

import java.time.LocalDate;
import java.util.List;
import labs.model.Day;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DayRepository extends JpaRepository<Day, Integer> {
    @Query("SELECT id FROM Day")
    List<Integer> findAllDaysIds();

    @Query("SELECT id FROM Day WHERE date = :date")
    List<Integer> findDaysIdsByDate(LocalDate date);

    @Query("SELECT d FROM Day d WHERE d.id = (SELECT day.id FROM Meal WHERE id = :mealId)")
    Day findDayByMealId(int mealId);
}
