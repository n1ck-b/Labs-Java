package labs.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import labs.Day;
import labs.dao.DayDao;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class DayDaoImpl implements DayDao {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Day getDayById(int id) {
        return entityManager.find(Day.class, id);
    }

    @Transactional
    @Override
    public int addDay(Day day) {
        if (!day.getMeals().isEmpty()) {
            day.getMeals().stream().forEach(meal -> meal.setDay(day));
        }
        entityManager.persist(day);
        return day.getId();
    }

    @Override
    public List<Day> getAllDays() {
        return entityManager.createQuery("SELECT d FROM Day d", Day.class).getResultList();
    }

    @Transactional
    @Override
    public ResponseEntity<String> deleteDayById(int id) {
        if (entityManager.createQuery("DELETE FROM Day WHERE id = :id").setParameter("id", id)
                .executeUpdate() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok("Deleted successfully");
    }

    @Override
    @Transactional
    public Day updateDayById(int id, Day updatedDay) {
        Day day = getDayById(id);
        updatedDay.setMeals(day.getMeals());
        entityManager.merge(updatedDay);
        entityManager.flush();
        return updatedDay;
    }

    @Override
    public List<Day> getDayByDate(LocalDate date) {
        return entityManager.createQuery("SELECT d FROM Day d WHERE date = :date", Day.class)
                .setParameter("date", date).getResultList();
    }
}
