package labs.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import labs.dao.Cache;
import labs.dao.CacheItem;
import labs.dao.DayRepository;
import labs.dao.MealDao;
import labs.dao.MealRepository;
import labs.dao.ProductDao;
import labs.dao.ProductRepository;
import labs.model.Day;
import labs.model.Meal;
import labs.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Repository
public class MealDaoImpl implements MealDao {
    @PersistenceContext
    private EntityManager entityManager;
    private final MealRepository mealRepository;
    private final DayRepository dayRepository;
    private final ProductDao productDao;
    private final Cache cache;
    private final ProductRepository productRepository;
    private static final String GETMEALLOG = "Get meal (id = %d) from %s. Time elapsed = %fms";
    private static final String MEALLOG = "Meal (id = %d) was %s cache";
    private static final String DELETEDMESSAGE = "Deleted successfully";
    private static final String MEALDAYLOG = "Meal (id = %d) was %s day (id = %d) in cache";

    @Autowired
    public MealDaoImpl(MealRepository mealRepository, ProductDao productDao,
                       DayRepository dayRepository, Cache cache, ProductRepository productRepository) {
        this.mealRepository = mealRepository;
        this.productDao = productDao;
        this.dayRepository = dayRepository;
        this.cache = cache;
        this.productRepository = productRepository;
    }

    @Override
    public Meal getMealById(int id) {
        try {
            long startTime = System.nanoTime();
            if (cache.exists("Meal" + id)) {
                log.info(String.format(GETMEALLOG, id, "cache", (System.nanoTime() - startTime) / 1000000.0));
                return (Meal) cache.getObject("Meal" + id);
            }
            Meal meal = mealRepository.findById(id).orElseThrow();
            List<Product> products = meal.getProducts();
            meal.setProducts(products.stream()
                    .map(product -> setRealWeightAndCaloriesForProduct(meal.getId(), product))
                    .collect(Collectors.toList()));
            cache.addObject("Meal" + id, new CacheItem(meal));
            log.info(String.format(GETMEALLOG, id, "DB", (System.nanoTime() - startTime) / 1000000.0));
            log.info(String.format(MEALLOG, id, "added to"));
            return meal;
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    public List<Meal> getMealsByIds(List<Integer> ids) {
        long startTime = System.nanoTime();
        List<Meal> meals = new ArrayList<>();
        List<Integer> idsOfMealsNotFoundInCache = new ArrayList<>();
        Meal meal;
        long startTimeForEach;
        for (int id : ids) {
            startTimeForEach = System.nanoTime();
            if (cache.exists("Meal" + id)) {
                meals.add((Meal) cache.getObject("Meal" + id));
                log.info(String.format(GETMEALLOG, id, "cache",
                        (System.nanoTime() - startTimeForEach) / 1000000.0));
            } else {
                idsOfMealsNotFoundInCache.add(id);
            }
        }
        if (!idsOfMealsNotFoundInCache.isEmpty()) {
            for (int id : idsOfMealsNotFoundInCache) {
                startTimeForEach = System.nanoTime();
                meal = mealRepository.findById(id).orElseThrow();
                meal = setRealWeightAndCaloriesForAllProducts(meal);
                cache.addObject("Meal" + id, new CacheItem(meal));
                meals.add(meal);
                log.info(String.format(GETMEALLOG, id, "DB",
                        (System.nanoTime() - startTimeForEach) / 1000000.0));
                log.info(String.format(MEALLOG, id, "added to"));
            }
        }
        log.info("Time elapsed for getting all meals = " +
                (System.nanoTime() - startTime) / 1000000.0 + "ms");
        return meals;
    }

    @Override
    public List<Meal> getMealsByDayId(int dayId) {
        List<Integer> mealIds = mealRepository.findAllMealIdByDayId(dayId);
        return getMealsByIds(mealIds);
    }

    @Override
    public void updateDayInCache(int dayId) {
        if (cache.exists("Day" + dayId)) {
            log.info("Day (id = " + dayId + ") was updated in cache");
            cache.removeObject("Day" + dayId);
            Day day = dayRepository.findById(dayId).orElseThrow();
            List<Meal> meals = day.getMeals();
            day.setMeals(meals.stream()
                    .map(this::setRealWeightAndCaloriesForAllProducts).collect(Collectors.toList()));
            cache.addObject("Day" + dayId, new CacheItem(day));
        }
    }

    @Override
    @Transactional
    public int addMeal(int dayId, Meal meal) {
        try {
            meal.setDay(dayRepository.findById(dayId).orElseThrow());
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!meal.getProducts().isEmpty()) {
            List<Product> products = meal.getProducts();
            meal.setProducts(products.stream()
                    .map(this::setWeightAndCaloriesForProduct).collect(Collectors.toList()));
        }
        mealRepository.save(meal);
        meal.getProducts().stream()
                .forEach(product -> productDao
                        .saveProductWeightToMealProductTable(product.getWeight(),
                                meal.getId(), product.getId()));
        if (cache.exists("Day" + dayId)) {
            Day day = (Day) cache.getObject("Day" + dayId);
            day.getMeals().add(meal);
        }
        return meal.getId();
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteMealsByDayId(int dayId) {
        if (!dayRepository.existsById(dayId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        List<Integer> mealIds = mealRepository.findAllMealIdByDayId(dayId);
        if (mealIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        mealIds.stream().forEach(productDao::deleteProductsIfNotUsed);
        for (int id : mealIds) {
            if (cache.exists("Meal" + id)) {
                log.info(String.format(MEALLOG, id, "deleted from"));
                cache.removeObject("Meal" + id);
            }
        }
        if (cache.exists("Day" + dayId)) {
            log.info("Meals were deleted from day (id = " + dayId + ") in cache");
            Day day = (Day) cache.getObject("Day" + dayId);
            day.getMeals().clear();
        }
        entityManager.flush();
        List<Integer> productIds = new ArrayList<>();
        for (int mealId : mealIds) {
            productIds.addAll(productRepository.getProductsIdsByMealId(mealId));
        }
        mealRepository.deleteAllByDay_Id(dayId);
        entityManager.flush();
        productDao.updateProductsInCache(productIds);
        return ResponseEntity.ok(DELETEDMESSAGE);
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteMealsByDayIdAndMealId(int dayId, int mealId) {
        if (!mealRepository.existsById(mealId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!dayRepository.existsById(dayId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        productDao.deleteProductsIfNotUsed(mealId);
        Meal meal = mealRepository.findById(mealId).orElseThrow();

        if (cache.exists("Meal" + mealId)) {
            log.info(String.format(MEALLOG, mealId, "deleted from"));
            cache.removeObject("Meal" + mealId);
        }
        if (cache.exists("Day" + dayId)) {
            Day day = (Day) cache.getObject("Day" + dayId);
            day.getMeals().remove(meal);
            log.info(String.format(MEALDAYLOG, mealId, "deleted from", dayId));
        }
        entityManager.flush();
        List<Integer> productIds = productRepository.getProductsIdsByMealId(mealId);
        mealRepository.deleteMealByIdAndDayId(mealId, dayId);
        entityManager.flush();
        productDao.updateProductsInCache(productIds);
        return ResponseEntity.ok(DELETEDMESSAGE);
    }

    @Override
    @Transactional
    public Meal updateMeal(int id, Meal updatedMeal) {
        Meal meal = getMealById(id);
        updatedMeal.setProducts(meal.getProducts());
        mealRepository.save(updatedMeal);
        if (cache.exists("Meal" + id)) {
            log.info(String.format(MEALLOG, id, "updated in"));
            cache.updateObject("Meal" + id, new CacheItem(updatedMeal));
        } else {
            log.info(String.format(MEALLOG, id, "added to"));
            cache.addObject("Meal" + id, new CacheItem(updatedMeal));
        }
        if (cache.exists("Day" + meal.getDay().getId())) {
            Day day = (Day) cache.getObject("Day" + meal.getDay().getId());
            day.getMeals().remove(updatedMeal);
            day.getMeals().add(updatedMeal);
            log.info(String.format(MEALDAYLOG, id, "updated in", day.getId()));
        }
        return updatedMeal;
    }

    @Override
    public Meal setWeightAndCaloriesForAllProducts(Meal meal) {
        List<Product> products = meal.getProducts();
        if (!products.isEmpty()) {
            meal.setProducts(products.stream()
                    .map(this::setWeightAndCaloriesForProduct).collect(Collectors.toList()));
        }
        return meal;
    }

    @Override
    public Product setWeightAndCaloriesForProduct(Product product) {
        if (product.getWeight() != 100) {
            product.setCalories((product.getCalories() * 100) / product.getWeight());
            product.setProteins((product.getProteins() * 100) / product.getWeight());
            product.setCarbs((product.getCarbs() * 100) / product.getWeight());
            product.setFats((product.getFats() * 100) / product.getWeight());
            product.setWeight(100);
        }
        return product;
    }

    @Override
    public Product setRealWeightAndCaloriesForProduct(int mealId, Product product) {
        Product productCopy = new Product(product.getId(),
                product.getName(),
                product.getWeight(),
                product.getCalories(),
                product.getProteins(),
                product.getCarbs(),
                product.getFats(),
                product.getMeals());
        productCopy.setWeight(productDao.getProductWeightFromTable(mealId, productCopy.getId()));
        productCopy.setCalories((productCopy.getCalories() * productCopy.getWeight()) / 100);
        productCopy.setProteins((productCopy.getProteins() * productCopy.getWeight()) / 100);
        productCopy.setCarbs((productCopy.getCarbs() * productCopy.getWeight()) / 100);
        productCopy.setFats((productCopy.getFats() * productCopy.getWeight()) / 100);
        return productCopy;
    }

    @Override
    public Meal setRealWeightAndCaloriesForAllProducts(Meal meal) {
        List<Product> products = meal.getProducts();
        if (!products.isEmpty()) {
            meal.setProducts(products.stream()
                    .map(product -> setRealWeightAndCaloriesForProduct(meal.getId(), product))
                    .collect(Collectors.toList()));
        }
        return meal;
    }

    @Override
    public List<Meal> getAllMeals() {
        List<Integer> mealIds = mealRepository.findAllMealsIds();
        return getMealsByIds(mealIds);
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteMealById(int id) {
        if (!mealRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Meal meal = mealRepository.findById(id).orElseThrow();
        if (cache.exists("Day" + meal.getDay().getId())) {
            Day day = (Day) cache.getObject("Day" + meal.getDay().getId());
            day.getMeals().remove(meal);
            log.info(String.format(MEALDAYLOG, id, "deleted from", meal.getDay().getId()));
        }
        productDao.deleteProductsIfNotUsed(id);
        if (cache.exists("Meal" + id)) {
            log.info(String.format(MEALLOG, id, "deleted from"));
            cache.removeObject("Meal" + id);
        }
        entityManager.flush();
        List<Integer> productIds = productRepository.getProductsIdsByMealId(id);
        mealRepository.deleteById(id);
        entityManager.flush();
        productDao.updateProductsInCache(productIds);
        return ResponseEntity.ok(DELETEDMESSAGE);
    }

    @Override
    public List<Meal> getMealsByProductName(String productName) {
        // List<Meal> meals = mealRepository.getMealsByProductName(productName);
        List<Integer> mealIds = mealRepository.getMealIdsByProductName(productName);
        //        for (Meal meal : meals) {
        //            if (!cache.exists("Meal" + meal.getId())) {
        //                cache.addObject("Meal" + meal.getId(), new CacheItem(meal));
        //                log.info("Meal (id = " + meal.getId() + ") was added to cache");
        //            }
        //        }
        // return meals.stream().map(this::setRealWeightAndCaloriesForAllProducts)
        // .collect(Collectors.toList());
        return getMealsByIds(mealIds);
    }
}
