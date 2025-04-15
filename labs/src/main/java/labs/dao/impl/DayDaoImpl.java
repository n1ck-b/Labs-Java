package labs.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import labs.aspect.LogExecution;
import labs.dao.DayDao;
import labs.dao.DayRepository;
import labs.dao.MealDao;
import labs.dao.MealRepository;
import labs.dao.ProductDao;
import labs.dao.ProductRepository;
import labs.dao.SessionCache;
import labs.model.Day;
import labs.model.Meal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@LogExecution
public class DayDaoImpl implements DayDao {
    @PersistenceContext
    EntityManager entityManager;
    private final DayRepository dayRepository;
    private final MealDao mealDao;
    private final ProductDao productDao;
    private final SessionCache cache;
    private final MealRepository mealRepository;
    private final ProductRepository productRepository;

    @Autowired
    public DayDaoImpl(DayRepository dayRepository,
                      @Lazy MealDao mealDao, @Lazy ProductDao productDao, SessionCache cache,
                      MealRepository mealRepository, ProductRepository productRepository) {
        this.dayRepository = dayRepository;
        this.mealDao = mealDao;
        this.productDao = productDao;
        this.cache = cache;
        this.mealRepository = mealRepository;
        this.productRepository = productRepository;
    }

    @Override
    public Day getDayById(int id) {
        Day day = (Day) cache.getObject("Day" + id);
        if (day != null) {
            return day;
        }
        day = dayRepository.findById(id).orElseThrow();
        List<Meal> meals = day.getMeals();
        day.setMeals(meals.stream()
                .map(mealDao::setRealWeightAndCaloriesForAllProducts).collect(Collectors.toList()));
        cache.addObject("Day" + day.getId(), day);
        return day;
    }

    @Transactional
    @Override
    public int addDay(Day day) {
        if (!day.getMeals().isEmpty()) {
            Day dayCopy = day;
            day.getMeals().stream().forEach(meal -> meal.setDay(dayCopy));
            day = setWeightsForAllProductsByDay(day);
            day.getMeals().stream()
                    .forEach(meal -> meal.getProducts()
                            .stream()
                            .forEach(product -> productDao
                                    .saveProductWeightToMealProductTable(product.getWeight(),
                                            meal.getId(), product.getId())));
        }
        dayRepository.save(day);
        return day.getId();
    }

    public Day setWeightsForAllProductsByDay(Day day) {
        List<Meal> meals = day.getMeals();
        if (!meals.isEmpty()) {
            day.setMeals(meals.stream()
                    .map(mealDao::setWeightAndCaloriesForAllProducts).collect(Collectors.toList()));
        }
        return day;
    }

    public Day setRealWeightsForAllProductsByDay(Day day) {
        List<Meal> meals = day.getMeals();
        if (!meals.isEmpty()) {
            day.setMeals(meals.stream()
                    .map(mealDao::setRealWeightAndCaloriesForAllProducts).collect(Collectors.toList()));
        }
        return day;
    }

    public List<Day> getDaysByIds(List<Integer> ids) {
        List<Day> days = new ArrayList<>();
        List<Integer> idsOfDaysNotFoundInCache = new ArrayList<>();
        Day day;
        for (int id : ids) {
            day = (Day) cache.getObject("Day" + id);
            if (day != null) {
                days.add(day);
            } else {
                idsOfDaysNotFoundInCache.add(id);
            }
        }
        List<Meal> meals;
        if (!idsOfDaysNotFoundInCache.isEmpty()) {
            for (int id : idsOfDaysNotFoundInCache) {
                day = dayRepository.findById(id).orElseThrow();
                meals = day.getMeals();
                day.setMeals(meals.stream()
                        .map(mealDao::setRealWeightAndCaloriesForAllProducts).collect(Collectors.toList()));
                cache.addObject("Day" + id, day);
                days.add(day);
            }
        }
        return days;
    }

    @Override
    public List<Day> getAllDays() {
        List<Integer> dayIds = dayRepository.findAllDaysIds();
        if (dayIds.isEmpty()) {
            return new ArrayList<>();
        }
        return getDaysByIds(dayIds);
    }

    @Transactional
    @Override
    public ResponseEntity<String> deleteDayById(int id) {
        if (cache.exists("Day" + id)) {
            cache.removeObject("Day" + id);
        }
        List<Integer> mealIds = mealRepository.findAllMealIdByDayId(id);
        List<Integer> productIds = new ArrayList<>();
        for (int mealId : mealIds) {
            if (cache.exists("Meal" + mealId)) {
                cache.removeObject("Meal" + mealId);
            }
            productDao.deleteProductsIfNotUsed(mealId);
            entityManager.flush();
            productIds.addAll(productRepository.getProductsIdsByMealId(mealId));
        }
        dayRepository.deleteById(id);
        entityManager.flush();
        if (!productIds.isEmpty()) {
            productDao.updateProductsInCache(productIds);
        }
        return ResponseEntity.ok("Deleted successfully");
    }

    @Override
    @Transactional
    public Day updateDayById(int id, Day updatedDay) {
        Day day = getDayById(id);
        if (!day.getMeals().isEmpty()) {
            updatedDay.setMeals(day.getMeals());
        }
        dayRepository.save(updatedDay);
        cache.addObject("Day" + id, updatedDay);
        return updatedDay;
    }

    @Override
    public List<Day> getDayByDate(LocalDate date) {
        List<Integer> dayIds = dayRepository.findDaysIdsByDate(date);
        if (dayIds.isEmpty()) {
            return new ArrayList<>();
        }
        return getDaysByIds(dayIds);
    }

    @Override
    public boolean existsById(int id) {
        return dayRepository.existsById(id);
    }
}
