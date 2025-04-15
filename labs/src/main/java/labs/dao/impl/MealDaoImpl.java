package labs.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import labs.aspect.LogExecution;
import labs.dao.DayRepository;
import labs.dao.MealDao;
import labs.dao.MealRepository;
import labs.dao.ProductDao;
import labs.dao.ProductRepository;
import labs.dao.SessionCache;
import labs.exception.ExceptionMessages;
import labs.exception.NotFoundException;
import labs.model.Day;
import labs.model.Meal;
import labs.model.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@LogExecution
public class MealDaoImpl implements MealDao {
    @PersistenceContext
    private EntityManager entityManager;
    private final MealRepository mealRepository;
    private final DayRepository dayRepository;
    private final ProductDao productDao;
    private final SessionCache cache;
    private final ProductRepository productRepository;
    private static final String DELETED_MESSAGE = "Deleted successfully";

    @Autowired
    public MealDaoImpl(MealRepository mealRepository, ProductDao productDao,
                       DayRepository dayRepository, SessionCache cache, ProductRepository productRepository) {
        this.mealRepository = mealRepository;
        this.productDao = productDao;
        this.dayRepository = dayRepository;
        this.cache = cache;
        this.productRepository = productRepository;
    }

    @Override
    public Meal getMealById(int id) {
        if (cache.exists("Meal" + id)) {
            return (Meal) cache.getObject("Meal" + id);
        }
        Meal meal = mealRepository.findById(id).orElseThrow();
        List<Product> products = meal.getProducts();
        meal.setProducts(products.stream()
                .map(product -> setRealWeightAndCaloriesForProduct(meal.getId(), product))
                .collect(Collectors.toList()));
        cache.addObject("Meal" + id, meal);
        return meal;
    }

    public List<Meal> getMealsByIds(List<Integer> ids) {
        List<Meal> meals = new ArrayList<>();
        List<Integer> idsOfMealsNotFoundInCache = new ArrayList<>();
        Meal meal;
        for (int id : ids) {
            if (cache.exists("Meal" + id)) {
                meals.add((Meal) cache.getObject("Meal" + id));
            } else {
                idsOfMealsNotFoundInCache.add(id);
            }
        }
        if (!idsOfMealsNotFoundInCache.isEmpty()) {
            for (int id : idsOfMealsNotFoundInCache) {
                meal = mealRepository.findById(id).orElseThrow();
                meal = setRealWeightAndCaloriesForAllProducts(meal);
                cache.addObject("Meal" + id, meal);
                meals.add(meal);
            }
        }
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
            cache.removeObject("Day" + dayId);
            Day day = dayRepository.findById(dayId).orElseThrow();
            List<Meal> meals = day.getMeals();
            day.setMeals(meals.stream()
                    .map(this::setRealWeightAndCaloriesForAllProducts).collect(Collectors.toList()));
            cache.addObject("Day" + dayId, day);
        }
    }

    @Override
    @Transactional
    public int addMeal(int dayId, Meal meal) {
        meal.setDay(dayRepository.findById(dayId).orElseThrow());
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
        List<Integer> mealIds = mealRepository.findAllMealIdByDayId(dayId);
        if (mealIds.isEmpty()) {
            throw new NotFoundException(String.format(ExceptionMessages.MEALS_NOT_FOUND_BY_DAY, dayId));
        }
        mealIds.stream().forEach(productDao::deleteProductsIfNotUsed);
        for (int id : mealIds) {
            if (cache.exists("Meal" + id)) {
                cache.removeObject("Meal" + id);
            }
        }
        if (cache.exists("Day" + dayId)) {
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
        return ResponseEntity.ok(DELETED_MESSAGE);
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteMealsByDayIdAndMealId(int dayId, int mealId) {
        productDao.deleteProductsIfNotUsed(mealId);
        Meal meal = mealRepository.findById(mealId).orElseThrow();

        if (cache.exists("Meal" + mealId)) {
            cache.removeObject("Meal" + mealId);
        }
        if (cache.exists("Day" + dayId)) {
            Day day = (Day) cache.getObject("Day" + dayId);
            day.getMeals().remove(meal);
        }
        entityManager.flush();
        List<Integer> productIds = productRepository.getProductsIdsByMealId(mealId);
        mealRepository.deleteMealByIdAndDayId(mealId, dayId);
        entityManager.flush();
        productDao.updateProductsInCache(productIds);
        return ResponseEntity.ok(DELETED_MESSAGE);
    }

    @Override
    @Transactional
    public Meal updateMeal(int id, Meal updatedMeal) {
        Meal meal = getMealById(id);
        updatedMeal.setProducts(meal.getProducts());
        mealRepository.save(updatedMeal);
        cache.addObject("Meal" + id, updatedMeal);
        if (cache.exists("Day" + meal.getDay().getId())) {
            Day day = (Day) cache.getObject("Day" + meal.getDay().getId());
            day.getMeals().remove(updatedMeal);
            day.getMeals().add(updatedMeal);
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
    public Product setRealWeightAndCaloriesForProduct(int mealId, final Product product) {
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
        if (mealIds.isEmpty()) {
            return new ArrayList<>();
        }
        return getMealsByIds(mealIds);
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteMealById(int id) {
        Meal meal = mealRepository.findById(id).orElseThrow();
        if (cache.exists("Day" + meal.getDay().getId())) {
            Day day = (Day) cache.getObject("Day" + meal.getDay().getId());
            day.getMeals().remove(meal);
        }
        productDao.deleteProductsIfNotUsed(id);
        if (cache.exists("Meal" + id)) {
            cache.removeObject("Meal" + id);
        }
        entityManager.flush();
        List<Integer> productIds = productRepository.getProductsIdsByMealId(id);
        mealRepository.deleteById(id);
        entityManager.flush();
        productDao.updateProductsInCache(productIds);
        return ResponseEntity.ok(DELETED_MESSAGE);
    }

    @Override
    public List<Meal> getMealsByProductName(String productName) {
        List<Integer> mealIds = mealRepository.getMealIdsByProductName(productName);
        if (mealIds.isEmpty()) {
            return new ArrayList<>();
        }
        return getMealsByIds(mealIds);
    }

    @Override
    public boolean existsById(int id) {
        return mealRepository.existsById(id);
    }
}
