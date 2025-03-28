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
import labs.dao.DayDao;
import labs.dao.DayRepository;
import labs.dao.MealDao;
import labs.dao.MealRepository;
import labs.dao.ProductDao;
import labs.dto.MealDto;
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
//    @PersistenceContext
//    EntityManager entityManager;

    private final DayDao dayDao;
    private final MealRepository mealRepository;
    private final DayRepository dayRepository;
    private final ProductDao productDao;
    private final Cache cache;

    @Autowired
    public MealDaoImpl(DayDao dayDao, MealRepository mealRepository, ProductDao productDao, DayRepository dayRepository, Cache cache) {
        this.dayDao = dayDao;
        this.mealRepository = mealRepository;
        this.productDao = productDao;
        this.dayRepository = dayRepository;
        this.cache = cache;
    }

    @Override
    public Meal getMealById(int id) {
        //return entityManager.find(Meal.class, id);
        try {
            long startTime = System.nanoTime();
            if (cache.exists("Meal" + id)) {
                log.info("Get meal (id = " + id + ") from cache. Time elapsed = " +
                        (System.nanoTime() - startTime) / 1000000.0 + "ms");
                return (Meal) cache.getObject("Meal" + id);
            }
            Meal meal = mealRepository.findById(id).orElseThrow();
            List<Product> products = meal.getProducts();
            meal.setProducts(products.stream()
                    .map(product -> setRealWeightAndCaloriesForProduct(meal.getId(), product)).collect(Collectors.toList()));
            cache.addObject("Meal" + id, new CacheItem(meal));
            log.info("Get meal (id = " + id + ") from DB. Time elapsed = " +
                    (System.nanoTime() - startTime) / 1000000.0 + "ms");
            log.info("Meal (id = " + id + ") was added to cache");
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
                log.info("Get meal (id = " + id + ") from cache. Time elapsed = " +
                        (System.nanoTime() - startTimeForEach) / 1000000.0 + "ms");
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
                log.info("Get meal (id = " + id + ") from DB. Time elapsed = " +
                        (System.nanoTime() - startTimeForEach) / 1000000.0 + "ms");
                log.info("Meal (id = " + id + ") was added to cache");
            }
        }
        log.info("Time elapsed for getting all meals = " + (System.nanoTime() - startTime) / 1000000.0 + "ms");
        return meals;
    }

    @Override
    public List<Meal> getMealsByDayId(int dayId) {
//        return entityManager.createQuery("SELECT m FROM Meal m WHERE day.id = :id", Meal.class)
//                .setParameter("id", dayId).getResultList();
//        List<Meal> meals = mealRepository.findAllByDay_Id(dayId);
//        return meals.stream().map(this::setRealWeightAndCaloriesForAllProducts).toList();
        List<Integer> mealIds = mealRepository.findAllMealIdByDayId(dayId);
        return getMealsByIds(mealIds);
    }

    @Override
    @Transactional
    public int addMeal(int dayId, Meal meal) {
        //meal.setDay(dayDao.getDayById(dayId));
        try {
            meal.setDay(dayRepository.findById(dayId).orElseThrow());
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        //entityManager.persist(meal);
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
        return meal.getId();
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteMealsByDayId(int dayId) {
//        List<Integer> mealIds = entityManager
//                .createQuery("SELECT m.id FROM Meal m WHERE day.id = :id", Integer.class)
//                .setParameter("id", dayId)
//                .getResultList();
//        if (dayDao.getDayById(dayId) == null) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
//        }
        if (!dayRepository.existsById(dayId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        List<Integer> mealIds = mealRepository.findAllMealIdByDayId(dayId);
        if (mealIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        mealIds.stream().forEach(productDao::deleteProductsIfNotUsed);
//        if (entityManager.createQuery("DELETE FROM Meal WHERE day.id = :id").setParameter("id", dayId)
//                .executeUpdate() == 0) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
//        }
        for (int id : mealIds) {
            if (cache.exists("Meal" + id)) {
                log.info("Meal (id = " + id + ") was deleted from cache");
                cache.removeObject("Meal" + id);
            }
        }
        mealRepository.deleteAllByDay_Id(dayId);
        return ResponseEntity.ok("Deleted successfully");
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteMealsByDayIdAndMealId(int dayId, int mealId) {
//        if (entityManager.createQuery("DELETE FROM Meal WHERE day.id = :dayId AND id = :mealId")
//                        .setParameter("dayId", dayId).setParameter("mealId", mealId).executeUpdate() == 0) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
//        }
        if (!mealRepository.existsById(mealId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
//        if (dayDao.getDayById(dayId) == null) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
//        }
        if (!dayRepository.existsById(dayId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        productDao.deleteProductsIfNotUsed(mealId);
        mealRepository.deleteMealByIdAndDayId(mealId, dayId);
        if (cache.exists("Meal" + mealId)) {
            log.info("Meal (id = " + mealId + ") was deleted from cache");
            cache.removeObject("Meal" + mealId);
        }
        return ResponseEntity.ok("Deleted successfully");
    }

    @Override
    @Transactional
    public Meal updateMeal(int id, Meal updatedMeal) {
        Meal meal = getMealById(id);
        updatedMeal.setProducts(meal.getProducts());
//        entityManager.merge(updatedMeal);
//        entityManager.flush();
        mealRepository.save(updatedMeal);
        if (cache.exists("Meal" + id)) {
            log.info("Meal (id = " + id + ") was updated in cache");
            cache.updateObject("Meal" + id, new CacheItem(updatedMeal));
        } else {
            log.info("Meal (id = " + id + ") was added to cache");
            cache.addObject("Meal" + id, new CacheItem(updatedMeal));
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
//        meal.getProducts().stream().forEach(product -> productDao.);
//        log.info("mealId = " + mealId);
//        log.info("productId = " + product.getId());
        Product productCopy = new Product(product.getId(),
                product.getName(),
                product.getWeight(),
                product.getCalories(),
                product.getProteins(),
                product.getCarbs(),
                product.getFats(),
                product.getMeals());
        productCopy.setWeight(productDao.getProductWeightFromTable(mealId, productCopy.getId()));
        //log.info("weight = " + productCopy.getWeight());
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
                    .map(product -> setRealWeightAndCaloriesForProduct(meal.getId(), product)).collect(Collectors.toList()));
        }
//        log.info("mealId = " + meal.getId());
        //log.info("products = " + meal.getProducts());
        //meal.getProducts().stream().forEach(product -> log.info("productId = " + product.getId() + " weight = " + product.getWeight()));
        return meal;
    }

    @Override
    public List<Meal> getAllMeals() {
//        return entityManager.createQuery("SELECT m FROM Meal m", Meal.class).getResultList();
//        List<Meal> meals = mealRepository.findAll();
//        meals.forEach(meal -> entityManager.detach(meal));
        List<Integer> mealIds = mealRepository.findAllMealsIds();
        return getMealsByIds(mealIds);
//        return meals.stream().map(this::setRealWeightAndCaloriesForAllProducts).toList();
//        log.info("LIST\n");
//        for (int i = 0; i < updatedMeals.size(); i++) {
//            log.info("mealId" + updatedMeals.get(i).getId());
//            for (int j = 0; j < updatedMeals.get(i).getProducts().size(); j++) {
//                log.info("productId = " + updatedMeals.get(i).getProducts().get(j).getId() + " weight = " + updatedMeals.get(i).getProducts().get(j).getWeight());
//            }
//        }
        //return mealRepository.findAll();
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteMealById(int id) {
        if (!mealRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        productDao.deleteProductsIfNotUsed(id);
//        if (entityManager.createQuery("DELETE FROM Meal WHERE id = :id").setParameter("id", id)
//                .executeUpdate() == 0) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
//        }
        mealRepository.deleteById(id);
        if (cache.exists("Meal" + id)) {
            log.info("Meal (id = " + id + ") was deleted from cache");
            cache.removeObject("Meal" + id);
        }
        return ResponseEntity.ok("Deleted successfully");
    }

    @Override
    public List<Meal> getMealsByProductName(String productName) {
//        List<Meal> meals = mealRepository.getMealsByProductName(productName);
//        if (meals.isEmpty()) {
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
//        }
//        return meals;

//        List<Meal> meals = mealRepository.getMealsByProductName(productName);
        List<Integer> mealIds = mealRepository.getMealIdsByProductName(productName);
//        return meals.stream().map(this::setRealWeightAndCaloriesForAllProducts).collect(Collectors.toList());
        return getMealsByIds(mealIds);
    }
}
