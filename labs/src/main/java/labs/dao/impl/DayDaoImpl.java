package labs.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.rmi.NoSuchObjectException;
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
import labs.dao.ProductDao;
import labs.model.Day;
import labs.model.Meal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Repository
public class DayDaoImpl implements DayDao {
//    @PersistenceContext
//    private EntityManager entityManager;
    private final DayRepository dayRepository;
    private final MealDao mealDao;
    private final ProductDao productDao;
    private final Cache cache;

    @Autowired
    public DayDaoImpl(DayRepository dayRepository, @Lazy MealDao mealDao, @Lazy ProductDao productDao, Cache cache) {
        this.dayRepository = dayRepository;
        this.mealDao = mealDao;
        this.productDao = productDao;
        this.cache = cache;
    }

    @Override
    public Day getDayById(int id) {
//        return entityManager.find(Day.class, id);
//        long startTime = System.currentTimeMillis();
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
            day.setMeals(meals.stream().map(mealDao::setRealWeightAndCaloriesForAllProducts).collect(Collectors.toList()));
            log.info("Day (id = " + day.getId() + ") was added to cache");
            cache.addObject("Day" + day.getId(), new CacheItem(day));
            log.info("Get day (id = " + day.getId() + ") from DB. Time elapsed = " +
                    (System.nanoTime() - startTime) / 1000000.0 + "ms");
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
//            List<Meal> meals = day.getMeals();
//            day.setMeals(meals.stream().map(mealDao::setWeightAndCaloriesForAllProducts).toList());
            day = setWeightsForAllProductsByDay(day);
            day.getMeals().stream()
                    .forEach(meal -> meal.getProducts()
                            .stream()
                            .forEach(product -> productDao
                                    .saveProductWeightToMealProductTable(product.getWeight(),
                                            meal.getId(), product.getId())));
        }
        dayRepository.save(day);
//        entityManager.persist(day);
        return day.getId();
    }

    public Day setWeightsForAllProductsByDay(Day day) {
        List<Meal> meals = day.getMeals();
        if (!meals.isEmpty()) {
            day.setMeals(meals.stream().map(mealDao::setWeightAndCaloriesForAllProducts).collect(Collectors.toList()));
        }
        return day;
    }

    public Day setRealWeightsForAllProductsByDay(Day day) {
        List<Meal> meals = day.getMeals();
        if (!meals.isEmpty()) {
            day.setMeals(meals.stream().map(mealDao::setRealWeightAndCaloriesForAllProducts).collect(Collectors.toList()));
        }
        return day;
    }

//    private List<Day> getDaysFromCache(List<Integer> ids) {
//        List<Day> days = new ArrayList<>();
//        Day day;
//        for (int id : ids) {
//            day = (Day) cache.getObject("Day" + id);
//            if (day != null) {
//                days.add(day);
//                ids.remove(id);
//            }
//        }
//        return days;
//    }

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
                day.setMeals(meals.stream().map(mealDao::setRealWeightAndCaloriesForAllProducts).collect(Collectors.toList()));
                log.info("Day (id = " + id + ") was added to cache");
                cache.addObject("Day" + id, new CacheItem(day));
                days.add(day);
                log.info("Get day (id = " + id + ") from DB. Time elapsed = " +
                        (System.nanoTime() - startTimeForEach) / 1000000.0 + "ms");
            }
        }
        log.info("Time elapsed for getting all days = " + (System.nanoTime() - startTime) / 1000000.0 + "ms");
        return days;
    }

    @Override
    public List<Day> getAllDays() {
//        return entityManager.createQuery("SELECT d FROM Day d", Day.class).getResultList();
        //long startTime = System.nanoTime();
        List<Integer> dayIds = dayRepository.findAllDaysIds();
//        List<Day> days = new ArrayList<>();
//        List<Integer> idsOfDaysNotFoundInCache = new ArrayList<>();
//        Day day;
//        long startTimeForEach;
//        for (int id : dayIds) {
//            startTimeForEach = System.nanoTime();
//            day = (Day) cache.getObject("Day" + id);
//            if (day != null) {
//                days.add(day);
//                log.info("Get day (id = " + day.getId() + ") from cache. Time elapsed = " +
//                        (System.nanoTime() - startTimeForEach) / 1000000.0 + "ms");
//            } else {
//                idsOfDaysNotFoundInCache.add(id);
//            }
//        }
//        List<Meal> meals;
//        if (!idsOfDaysNotFoundInCache.isEmpty()) {
//            for (int id : idsOfDaysNotFoundInCache) {
//                startTimeForEach = System.nanoTime();
//                day = dayRepository.findById(id).orElseThrow();
//                meals = day.getMeals();
//                day.setMeals(meals.stream().map(mealDao::setRealWeightAndCaloriesForAllProducts).collect(Collectors.toList()));
//                cache.addObject("Day" + id, new CacheItem(day));
//                days.add(day);
//                log.info("Get day (id = " + day.getId() + ") from DB. Time elapsed = " +
//                        (System.nanoTime() - startTimeForEach) / 1000000.0 + "ms");
//            }
//        }
//        //days = dayRepository.findAll();
//        log.info("Time elapsed for getting all days = " + (System.nanoTime() - startTime) / 1000000.0 + "ms");
        //return days.stream().map(this::setRealWeightsForAllProductsByDay).toList();
        return getDaysByIds(dayIds);
    }

    @Transactional
    @Override
    public ResponseEntity<String> deleteDayById(int id) {
//        if (entityManager.createQuery("DELETE FROM Day WHERE id = :id").setParameter("id", id)
//                .executeUpdate() == 0) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
//        }
        if (!dayRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
//        mealDao.deleteMealById(dayRepository.findById(id).orElseThrow().getId());
        dayRepository.deleteById(id);
        if (cache.exists("Day" + id)) {
            log.info("Day (id = " + id + ") was deleted from cache");
            cache.removeObject("Day" + id);
        }
//        dayRepository.delete(dayRepository.findById(id).orElseThrow());
        return ResponseEntity.ok("Deleted successfully");
    }

    @Override
    @Transactional
    public Day updateDayById(int id, Day updatedDay) {
        Day day = getDayById(id);
        if (!day.getMeals().isEmpty()) {
            updatedDay.setMeals(day.getMeals());
        }
//        updatedDay.setMeals(getDayById(id).getMeals());
//        entityManager.merge(updatedDay);
//        entityManager.flush();
//        if(updatedDay.getMeals() == null)
//        {
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
//        }
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
//        return entityManager.createQuery("SELECT d FROM Day d WHERE date = :date", Day.class)
//                .setParameter("date", date).getResultList();
        List<Integer> dayIds = dayRepository.findDaysIdsByDate(date);
        if (dayIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
//        long startTime = System.nanoTime();
//        List<Day> days = new ArrayList<>();
//        List<Integer> idsOfDaysNotFoundInCache = new ArrayList<>();
//        Day day;
//        long startTimeForEach;
//        for (int id : dayIds) {
//            startTimeForEach = System.nanoTime();
//            day = (Day) cache.getObject("Day" + id);
//            if (day != null) {
//                days.add(day);
//                log.info("Get day (id = " + day.getId() + ") from cache. Time elapsed = " +
//                        (System.nanoTime() - startTimeForEach) / 1000000.0 + "ms");
//            } else {
//                idsOfDaysNotFoundInCache.add(id);
//            }
//        }
//        List<Meal> meals;
//        if (!idsOfDaysNotFoundInCache.isEmpty()) {
//            for (int id : idsOfDaysNotFoundInCache) {
//                startTimeForEach = System.nanoTime();
//                day = dayRepository.findById(id).orElseThrow();
//                meals = day.getMeals();
//                day.setMeals(meals.stream().map(mealDao::setRealWeightAndCaloriesForAllProducts).collect(Collectors.toList()));
//                cache.addObject("Day" + id, new CacheItem(day));
//                days.add(day);
//                log.info("Get day (id = " + day.getId() + ") from DB. Time elapsed = " +
//                        (System.nanoTime() - startTimeForEach) / 1000000.0 + "ms");
//            }
//        }
//        log.info("Time elapsed for getting all days = " + (System.nanoTime() - startTime) / 1000000.0 + "ms");
//        days = dayRepository.findDayByDate(date);
//        return days.stream().map(this::setRealWeightsForAllProductsByDay).toList();
        return getDaysByIds(dayIds);
    }
}
