package labs.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import labs.Meal;
import labs.dao.DayDao;
import labs.dao.MealDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class MealDaoImpl implements MealDao {
    @PersistenceContext
    EntityManager entityManager;

    private final DayDao dayDao;

    @Autowired
    public MealDaoImpl(DayDao dayDao) {
        this.dayDao = dayDao;
    }

    @Override
    public Meal getMealById(int id) {
        return entityManager.find(Meal.class, id);
    }

    @Override
    public List<Meal> getMealsByDayId(int dayId) {
        return entityManager.createQuery("SELECT m FROM Meal m WHERE day.id = :id", Meal.class)
                .setParameter("id", dayId).getResultList();
    }

    @Override
    @Transactional
    public int addMeal(int dayId, Meal meal) {
        meal.setDay(dayDao.getDayById(dayId));
        entityManager.persist(meal);
        return meal.getId();
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteMealsByDayId(int dayId) {
        List<Integer> mealIds = entityManager
                .createQuery("SELECT m.id FROM Meal m WHERE day.id = :id", Integer.class)
                .setParameter("id", dayId)
                .getResultList();
        if (mealIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        mealIds.stream().forEach(this::deleteProductsIfNotUsed);
        if (entityManager.createQuery("DELETE FROM Meal WHERE day.id = :id").setParameter("id", dayId)
                .executeUpdate() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok("Deleted successfully");
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteMealsByDayIdAndMealId(int dayId, int mealId) {
        deleteProductsIfNotUsed(mealId);
        if (entityManager.createQuery("DELETE FROM Meal WHERE day.id = :dayId AND id = :mealId")
                        .setParameter("dayId", dayId).setParameter("mealId", mealId).executeUpdate() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok("Deleted successfully");
    }

    @Override
    @Transactional
    public Meal updateMeal(int id, Meal updatedMeal) {
        Meal meal = getMealById(id);
        updatedMeal.setProducts(meal.getProducts());
        entityManager.merge(updatedMeal);
        entityManager.flush();
        return updatedMeal;
    }

    @Override
    public List<Meal> getAllMeals() {
        return entityManager.createQuery("SELECT m FROM Meal m", Meal.class).getResultList();
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteMealById(int id) {
        deleteProductsIfNotUsed(id);
        if (entityManager.createQuery("DELETE FROM Meal WHERE id = :id").setParameter("id", id)
                .executeUpdate() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok("Deleted successfully");
    }

    @Transactional
    public int deleteProductByIdIfNotUsed(int id) {
        if (entityManager.createNativeQuery("SELECT meal_id FROM meal_product WHERE product_id = :id")
                .setParameter("id", id).getResultList().size() == 1) {
            entityManager.createQuery("DELETE FROM Product WHERE id = :id")
                    .setParameter("id", id)
                    .executeUpdate();
        }
        return id;
    }

    @Transactional
    public void deleteProductsIfNotUsed(int mealId) {
        List<?> results = entityManager
                .createNativeQuery("SELECT product_id FROM meal_product WHERE meal_id = :mealId")
                .setParameter("mealId", mealId).getResultList();
        if (results.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        List<Integer> productIds = results.stream().map(x -> ((Number) x).intValue()).toList();
        productIds.stream().forEach(this::deleteProductByIdIfNotUsed);
    }
}
