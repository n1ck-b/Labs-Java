package labs.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import labs.dao.Cache;
import labs.dao.CacheItem;
import labs.dao.DayDao;
import labs.dao.DayRepository;
import labs.dao.MealDao;
import labs.dao.MealRepository;
import labs.dao.ProductDao;
import labs.dao.ProductRepository;
import labs.model.Day;
import labs.model.Meal;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Repository
public class DayDaoImpl implements DayDao {
    @PersistenceContext
    EntityManager entityManager;
    private final DayRepository dayRepository;
    private final MealDao mealDao;
    private final ProductDao productDao;
    private final Cache cache;
    private final MealRepository mealRepository;
    private final ProductRepository productRepository;

    @Autowired
    public DayDaoImpl(DayRepository dayRepository,
                      @Lazy MealDao mealDao, @Lazy ProductDao productDao, Cache cache,
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
        long startTime = System.nanoTime();
        Day day = (Day) cache.getObject("Day" + id);
        if (day != null) {
            log.info("Get day (id = " + id + ") from cache. Time elapsed = " +
                    (System.nanoTime() - startTime) / 1000000.0 + "ms");
            return day;
        }
        try {
            day = dayRepository.findById(id).orElseThrow();
            List<Meal> meals = day.getMeals();
            day.setMeals(meals.stream()
                    .map(mealDao::setRealWeightAndCaloriesForAllProducts).collect(Collectors.toList()));
            cache.addObject("Day" + day.getId(), new CacheItem(day));
            log.info("Get day (id = " + day.getId() + ") from DB. Time elapsed = " +
                    (System.nanoTime() - startTime) / 1000000.0 + "ms");
            log.info("Day (id = " + day.getId() + ") was added to cache");
            return day;
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
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
        long startTime = System.nanoTime();
        List<Day> days = new ArrayList<>();
        List<Integer> idsOfDaysNotFoundInCache = new ArrayList<>();
        Day day;
        long startTimeForEach;
        for (int id : ids) {
            startTimeForEach = System.nanoTime();
            day = (Day) cache.getObject("Day" + id);
            if (day != null) {
                days.add(day);
                log.info("Get day (id = " + id + ") from cache. Time elapsed = " +
                        (System.nanoTime() - startTimeForEach) / 1000000.0 + "ms");
            } else {
                idsOfDaysNotFoundInCache.add(id);
            }
        }
        List<Meal> meals;
        if (!idsOfDaysNotFoundInCache.isEmpty()) {
            for (int id : idsOfDaysNotFoundInCache) {
                startTimeForEach = System.nanoTime();
                day = dayRepository.findById(id).orElseThrow();
                meals = day.getMeals();
                day.setMeals(meals.stream()
                        .map(mealDao::setRealWeightAndCaloriesForAllProducts).collect(Collectors.toList()));
                cache.addObject("Day" + id, new CacheItem(day));
                days.add(day);
                log.info("Get day (id = " + id + ") from DB. Time elapsed = " +
                        (System.nanoTime() - startTimeForEach) / 1000000.0 + "ms");
                log.info("Day (id = " + id + ") was added to cache");
            }
        }
        log.info("Time elapsed for getting all days = " + (System.nanoTime() - startTime) / 1000000.0 + "ms");
        return days;
    }

    @Override
    public List<Day> getAllDays() {
        List<Integer> dayIds = dayRepository.findAllDaysIds();
        return getDaysByIds(dayIds);
    }

    @Transactional
    @Override
    public ResponseEntity<String> deleteDayById(int id) {
        if (!dayRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (cache.exists("Day" + id)) {
            log.info("Day (id = " + id + ") was deleted from cache");
            cache.removeObject("Day" + id);
        }
        List<Integer> mealIds = mealRepository.findAllMealIdByDayId(id);
        List<Integer> productIds = new ArrayList<>();
        for (int mealId : mealIds) {
            if (cache.exists("Meal" + mealId)) {
                log.info("Meal (id = " + mealId + ") was deleted from cache");
                cache.removeObject("Meal" + mealId);
                productDao.deleteProductsIfNotUsed(mealId);
                entityManager.flush();
                productIds.addAll(productRepository.getProductsIdsByMealId(mealId));
            }
        }
        dayRepository.deleteById(id);
        entityManager.flush();
        productDao.updateProductsInCache(productIds);
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
        if (cache.exists("Day" + id)) {
            log.info("Day (id = " + id + ") was updated in cache");
            cache.updateObject("Day" + id, new CacheItem(updatedDay));
        } else {
            log.info("Day (id = " + id + ") was added to cache");
            cache.addObject("Day" + id, new CacheItem(updatedDay));
        }
        return updatedDay;
    }

    @Override
    public List<Day> getDayByDate(LocalDate date) {
        List<Integer> dayIds = dayRepository.findDaysIdsByDate(date);
        if (dayIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return getDaysByIds(dayIds);
    }
}
