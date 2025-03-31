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
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Repository
public class ProductDaoImpl implements ProductDao {
    @PersistenceContext
    private EntityManager entityManager;

    private final MealDao mealDao;
    private final MealRepository mealRepository;
    private final ProductRepository productRepository;
    private final Cache cache;
    private final DayRepository dayRepository;

    @Autowired
    public ProductDaoImpl(@Lazy MealDao mealDao, MealRepository mealRepository,
                          ProductRepository productRepository, Cache cache, DayRepository dayRepository) {
        this.mealDao = mealDao;
        this.mealRepository = mealRepository;
        this.productRepository = productRepository;
        this.cache = cache;
        this.dayRepository = dayRepository;
    }

    @Override
    public Product getProductById(int id) {
        try {
            long startTime = System.nanoTime();
            if (cache.exists("Product" + id)) {
                log.info("Get product (id = " + id + ") from cache. Time elapsed = " +
                        (System.nanoTime() - startTime) / 1000000.0 + "ms");
                return (Product) cache.getObject("Product" + id);
            }
            Product product = productRepository.findById(id).orElseThrow();
            cache.addObject("Product" + id, new CacheItem(product));
            log.info("Get product (id = " + id + ") from DB. Time elapsed = " +
                    (System.nanoTime() - startTime) / 1000000.0 + "ms");
            log.info("Product (id = " + id + ") was added to cache");
            return product;
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    public void updatedProductsInMealsInCache(Product product) {
        List<Integer> mealIds = mealRepository.getMealIdsByProductName(product.getName());
        Product productCopy;
        for (int id : mealIds) {
            productCopy = mealDao.setRealWeightAndCaloriesForProduct(id, product);
            if (cache.exists("Meal" + id)) {
                log.info("Meal (id = " + id + ") was updated in cache");
                ((Meal) cache.getObject("Meal" + id)).getProducts().remove(productCopy);
                ((Meal) cache.getObject("Meal" + id)).getProducts().add(productCopy);
            }
        }
    }

    @Override
    @Transactional
    public int addProductByMealId(int mealId, Product product) {
        entityManager.flush();
        Meal mealById;
        Product productCopy;
        try {
            if (cache.exists("Meal" + mealId)) {
                mealById = (Meal) cache.getObject("Meal" + mealId);
            } else {
                mealById = mealRepository.findById(mealId).orElseThrow();
            }
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Product productFromDb;
        if (cache.exists("Product" + productRepository.getIdByName(product.getName()))) {
            productFromDb = (Product) cache
                    .getObject("Product" + productRepository.getIdByName(product.getName()));
        } else {
            productFromDb = productRepository.findByName(product.getName());
        }
        float productWeight = product.getWeight();
        if (productFromDb != null) {
            entityManager.detach(productFromDb);
            List<Meal> mealsOfProductFromDb = productFromDb.getMeals();
            if (!mealsOfProductFromDb.contains(mealById)) {
                mealsOfProductFromDb.add(mealById);
                productFromDb.setMeals(mealsOfProductFromDb);
            }
            productRepository.saveProductToMealProductTable(mealId, productFromDb.getId(), productWeight);
            productRepository.saveProductWeightToMealProductTable(
                    productWeight, mealId, productFromDb.getId());
            entityManager.flush();
            List<Integer> mealIds = mealRepository.getMealIdsByProductName(productFromDb.getName());
            updatedProductsInMealsInCache(productFromDb);
            for (int id : mealIds) {
                productCopy = mealDao.setRealWeightAndCaloriesForProduct(id, productFromDb);
                Day day = dayRepository.findDayByMealId(id);
                if (cache.exists("Day" + day.getId())) {
                    Day dayFromCache = (Day) cache.getObject("Day" + day.getId());
                    Meal mealFromDayInCache = dayFromCache.getMeals().stream()
                            .filter(meal -> meal.getId() == id).collect(Collectors.toList()).get(0);
                    if (!mealFromDayInCache.getProducts().isEmpty()) {
                        mealFromDayInCache.getProducts().remove(productCopy);
                    } else {
                        mealFromDayInCache.setProducts(new ArrayList<>());
                    }
                    mealFromDayInCache.getProducts().add(productCopy);
                    log.info("Day (id = " + dayFromCache.getId() + ") was updated in cache");
                }
            }
            return productFromDb.getId();
        }
        List<Meal> meals = new ArrayList<>();
        meals.add(mealById);
        product.setMeals(meals);
        product.setWeight(100);
        productRepository.save(product);
        productRepository.saveProductWeightToMealProductTable(productWeight, mealId, product.getId());
        entityManager.flush();
        entityManager.detach(product);
        updatedProductsInMealsInCache(product);
        productCopy = mealDao.setRealWeightAndCaloriesForProduct(mealId, product);
        Day day = dayRepository.findDayByMealId(mealId);
        if (cache.exists("Day" + day.getId())) {
            Day dayFromCache = (Day) cache.getObject("Day" + day.getId());
            Meal mealFromDayInCache = dayFromCache.getMeals().get(dayFromCache.getMeals().indexOf(mealById));
            if (!mealFromDayInCache.getProducts().isEmpty()) {
                mealFromDayInCache.getProducts().remove(productCopy);
            } else {
                mealFromDayInCache.setProducts(new ArrayList<>());
            }
            mealFromDayInCache.getProducts().add(productCopy);
            log.info("Day (id = " + dayFromCache.getId() + ") was updated in cache");
        }
        return product.getId();
    }

    public List<Product> getProductsByIds(List<Integer> ids, int mealId, boolean withRealWeight) {
        long startTime = System.nanoTime();
        List<Product> products = new ArrayList<>();
        List<Integer> idsOfProductsNotFoundInCache = new ArrayList<>();
        Product product;
        long startTimeForEach;
        for (int id : ids) {
            startTimeForEach = System.nanoTime();
            if (cache.exists("Product" + id)) {
                if (!withRealWeight) {
                    products.add((Product) cache.getObject("Product" + id));
                } else {
                    products.add(mealDao.setRealWeightAndCaloriesForProduct(mealId,
                            (Product) cache.getObject("Product" + id)));
                }
                log.info("Get product (id = " + id + ") from cache. Time elapsed = " +
                        (System.nanoTime() - startTimeForEach) / 1000000.0 + "ms");
            } else {
                idsOfProductsNotFoundInCache.add(id);
            }
        }
        if (!idsOfProductsNotFoundInCache.isEmpty()) {
            for (int id : idsOfProductsNotFoundInCache) {
                startTimeForEach = System.nanoTime();
                product = productRepository.findById(id).orElseThrow();
                if (withRealWeight) {
                    product = mealDao.setRealWeightAndCaloriesForProduct(mealId, product);
                }
                cache.addObject("Product" + id, new CacheItem(product));
                products.add(product);
                log.info("Get product (id = " + id + ") from DB. Time elapsed = " +
                        (System.nanoTime() - startTimeForEach) / 1000000.0 + "ms");
                log.info("Product (id = " + id + ") was added to cache");
            }
        }
        log.info("Time elapsed for getting all products = " +
                (System.nanoTime() - startTime) / 1000000.0 + "ms");
        return products;
    }

    @Override
    public List<Product> getAllProductsByMealId(int mealId) {
        List<Integer> productIds = productRepository.getProductsIdsByMealId(mealId);
        if (productIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return getProductsByIds(productIds, mealId, true);
    }

    @Override
    public List<Product> getAllProducts() {
        List<Integer> productIds = productRepository.getAllProductsIds();
        return getProductsByIds(productIds, 0, false);
    }

    @Transactional
    public ResponseEntity<String> deleteProductById(int id) {
        if (!productRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        List<Integer> mealIds = mealRepository.findMealIdsByProductId(id);
        productRepository.deleteById(id);
        if (cache.exists("Product" + id)) {
            if (cache.exists("Product" + id)) {
                cache.removeObject("Product" + id);
                log.info("Product (id = " + id + ") was deleted from cache");
            }
        }
        for (int mealId : mealIds) {
            if (cache.exists("Meal" + mealId)) {
                Meal mealFromCache = (Meal) cache.getObject("Meal" + mealId);
                mealFromCache.getProducts().removeIf(product -> product.getId() == id);
                log.info("Product (id = " + id + ") was removed from meal (id = " + mealId + ") in cache");
            }
            Day day = dayRepository.findDayByMealId(mealId);
            if (cache.exists("Day" + day.getId())) {
                Day dayFromCache = (Day) cache.getObject("Day" + day.getId());
                dayFromCache.getMeals()
                        .forEach(meal -> meal.getProducts()
                                .removeIf(product -> product.getId() == id));
                log.info("Product (id = " + id + ") was deleted from meal (id = " +
                        mealId + ") in day (id = " + dayFromCache.getId() + ") in cache");
            }
        }
        return ResponseEntity.ok("Deleted successfully");
    }

    @Override
    @Transactional
    public int deleteProductByIdIfNotUsed(int id) {
        if (mealRepository.findMealIdsByProductId(id).size() == 1) {
            deleteProductById(id);
            return id;
        }
        return 0;
    }

    @Transactional
    public void deleteProductIfNotUsedOrDeleteFromMeal(int mealId, int productId) {
        if (deleteProductByIdIfNotUsed(productId) == 0) {
            final List<Integer> mealIds = mealRepository.findMealIdsByProductId(productId);
            productRepository.deleteProductFromMealProductTable(mealId, productId);
            entityManager.flush();
            if (cache.exists("Product" + productId)) {
                if (cache.exists("Product" + productId)) {
                    ((Product) cache.getObject("Product" + productId))
                            .getMeals().removeIf(meal -> meal.getId() == mealId);
                    log.info("Product (id = " + productId + ") was deleted from cache");
                }
            }
            if (cache.exists("Meal" + mealId)) {
                Meal meal = (Meal) cache.getObject("Meal" + mealId);
                meal.getProducts().removeIf(product -> product.getId() == productId);
                log.info("Product (id = " + productId +
                        ") was deleted from meal (id = " + mealId + ") in cache");
            }
            for (int id : mealIds) {
                if (cache.exists("Meal" + id)) {
                    Meal meal = (Meal) cache.getObject("Meal" + id);
                    meal.getProducts().forEach(product -> product.getMeals()
                                    .removeIf(mealFromList -> mealFromList.getId() == mealId));
                    log.info("Product (id = " + productId + ") was deleted from cache");
                }
                Day day = dayRepository.findDayByMealId(id);
                Meal meal = mealDao.getMealById(id);
                if (cache.exists("Day" + day.getId())) {
                    ((Day) cache.getObject("Day" + day.getId())).getMeals()
                            .remove(meal);
                    ((Day) cache.getObject("Day" + day.getId())).getMeals()
                            .add(meal);
                    log.info("Product (id = " + productId + ") was deleted from meal (id = " + mealId +
                            ") in day (id = " + day.getId() + ") in cache");
                }
            }
        }
    }

    @Override
    @Transactional
    public void deleteProductsIfNotUsed(int mealId) {
        List<Integer> productIds = productRepository.getProductsIdsByMealId(mealId);
        if (!productIds.isEmpty()) {
            productIds.stream().forEach(this::deleteProductByIdIfNotUsed);
        }
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteProductsByMealId(int mealId) {
        if (!mealRepository.existsById(mealId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        List<Integer> productIds = productRepository.getProductsIdsByMealId(mealId);
        productIds.stream().forEach(id -> deleteProductIfNotUsedOrDeleteFromMeal(mealId, id));
        return ResponseEntity.ok("Deleted successfully");
    }

    @Override
    @Transactional
    public Product updateProduct(int id, Product updatedProduct) {
        updatedProduct.setMeals(getProductById(id).getMeals());
        productRepository.save(updatedProduct);
        entityManager.flush();
        if (cache.exists("Product" + id)) {
            log.info("Product (id = " + id + ") was updated in cache");
            cache.updateObject("Product" + id, new CacheItem(updatedProduct));
        } else {
            log.info("Product (id = " + id + ") was added to cache");
            cache.addObject("Product" + id, new CacheItem(updatedProduct));
        }
        List<Integer> mealIds = mealRepository.findMealIdsByProductId(id);
        for (int mealId : mealIds) {
            if (cache.exists("Meal" + mealId)) {
                Meal mealFromCache = (Meal) cache.getObject("Meal" + mealId);
                if (mealFromCache.getProducts().removeIf(product -> product.getId() == id)) {
                    mealFromCache.getProducts()
                            .add(mealDao.setRealWeightAndCaloriesForProduct(mealId, updatedProduct));
                }
                log.info("Product (id = " + id + ") was updated in meal (id = " + mealId + ") in cache");
            }
            Day day = dayRepository.findDayByMealId(mealId);
            if (cache.exists("Day" + day.getId())) {
                Day dayFromCache = (Day) cache.getObject("Day" + day.getId());
                for (Meal mealFromDay : dayFromCache.getMeals()) {
                    if (mealFromDay.getProducts().removeIf(product -> product.getId() == id)) {
                        mealFromDay.getProducts().add(updatedProduct);
                    }
                }
                log.info("Product (id = " + id + ") was updated in meal (id = " +
                        mealId + ") in day (id = " + dayFromCache.getId() + ") in cache");
            }
        }
        return updatedProduct;
    }

    @Override
    public float getProductWeightFromTable(int mealId, int productId) {
        return productRepository.getProductWeightFromMealProductTable(mealId, productId);
    }

    @Override
    public void saveProductWeightToMealProductTable(float weight, int mealId, int productId) {
        productRepository.saveProductWeightToMealProductTable(weight, mealId, productId);
    }

    @Override
    public void updateProductsInCache(List<Integer> productIds) {
        if (productIds != null) {
            for (int productId : productIds) {
                if (cache.exists("Product" + productId)) {
                    cache.updateObject("Product" + productId,
                            new CacheItem(productRepository.findById(productId).orElseThrow()));
                    log.info("Product (id = " + productId + ") was updated in cache");
                }
            }
        }
    }
}
