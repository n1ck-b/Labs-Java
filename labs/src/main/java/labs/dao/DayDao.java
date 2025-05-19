package labs.dao;

import java.time.LocalDate;
import java.util.List;
import labs.model.Day;
import org.springframework.http.ResponseEntity;

public interface DayDao {
    Day getDayById(int id);

    Day addDay(Day day);

    List<Day> getAllDays();

    ResponseEntity<String> deleteDayById(int id);

    Day updateDayById(int id, Day updatedDay);

    List<Day> getDayByDate(LocalDate date);

    boolean existsById(int id);
}
