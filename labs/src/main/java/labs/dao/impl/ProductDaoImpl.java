package labs.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import labs.dao.Cache;
import labs.dao.CacheItem;
import labs.dao.MealDao;
import labs.dao.MealRepository;
import labs.dao.ProductDao;
import labs.dao.ProductRepository;
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

    @Autowired
    public ProductDaoImpl(@Lazy MealDao mealDao, MealRepository mealRepository,
                          ProductRepository productRepository, Cache cache) {
        this.mealDao = mealDao;
        this.mealRepository = mealRepository;
        this.productRepository = productRepository;
        this.cache = cache;
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

    @Override
    @Transactional
    public int addProductByMealId(int mealId, Product product) {
        Meal mealById;
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
            if (cache.exists("Meal" + mealId)) {
                ((Meal) cache.getObject("Meal" + mealId)).getProducts().add(productFromDb);
            }
            return productFromDb.getId();
        }
        List<Meal> meals = new ArrayList<>();
        meals.add(mealById);
        product.setMeals(meals);
        product.setWeight(100);
        productRepository.save(product);
        productRepository.saveProductWeightToMealProductTable(productWeight, mealId, product.getId());
        if (cache.exists("Meal" + mealId)) {
            ((Meal) cache.getObject("Meal" + mealId)).getProducts().add(product);
        }
        return product.getId();
    }

    public List<Product> getProductsByIds(List<Integer> ids, int mealId, boolean withRealWeight) {
        String keyName = "Product";
        long startTime = System.nanoTime();
        List<Product> products = new ArrayList<>();
        List<Integer> idsOfProductsNotFoundInCache = new ArrayList<>();
        Product product;
        long startTimeForEach;
        for (int id : ids) {
            startTimeForEach = System.nanoTime();
            if (withRealWeight) {
                keyName = "RealProduct" + getProductWeightFromTable(mealId, id);
            }
            if (cache.exists(keyName + id)) {
                products.add((Product) cache.getObject(keyName + id));
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
                    keyName = "RealProduct" + getProductWeightFromTable(mealId, id);
                } else {
                    keyName = "Product";
                }
                cache.addObject(keyName + id, new CacheItem(product));
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
        productRepository.deleteById(id);
        if (cache.exists("Product" + id) || cache.exists("RealProduct" + id)) {
            log.info("Product (id = " + id + ") was deleted from cache");
            if (cache.exists("Product" + id)) {
                cache.removeObject("Product" + id);
            }
            if (cache.exists("RealProduct" + id)) {
                cache.removeObject("RealProduct" + id);
            }
        }
        return ResponseEntity.ok("Deleted successfully");
    }

    @Override
    @Transactional
    public int deleteProductByIdIfNotUsed(int id) {
        if (mealRepository.findMealIdsByProductId(id).size() == 1) {
            productRepository.deleteById(id);
            if (cache.exists("Product" + id) || cache.exists("RealProduct" + id)) {
                log.info("Product (id = " + id + ") was deleted from cache");
                if (cache.exists("Product" + id)) {
                    cache.removeObject("Product" + id);
                }
                if (cache.exists("RealProduct" + id)) {
                    cache.removeObject("RealProduct" + id);
                }
            }
            return id;
        }
        return 0;
    }

    @Transactional
    public void deleteProductIfNotUsedOrDeleteFromMeal(int mealId, int productId) {
        if (deleteProductByIdIfNotUsed(productId) == 0) {
            productRepository.deleteProductFromMealProductTable(mealId, productId);
            if (cache.exists("Meal" + mealId)) {
                Meal meal = (Meal) cache.getObject("Meal" + mealId);
                meal.getProducts().remove(productRepository.findById(productId).orElseThrow());
                log.info("Product (id = " + productId + ") was deleted from cache");
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
        if (cache.exists("Product" + id)) {
            log.info("Product (id = " + id + ") was updated in cache");
            cache.updateObject("Product" + id, new CacheItem(updatedProduct));
        } else {
            log.info("Product (id = " + id + ") was added to cache");
            cache.addObject("Product" + id, new CacheItem(updatedProduct));
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
//        List<Integer> productIds;
//        for (int id : mealIds) {
//            productIds = productRepository.getProductsIdsByMealId(id);
        if (productIds != null) {
            for (int productId : productIds) {
                if (cache.exists("Product" + productId)) {
                    cache.updateObject("Product" + productId,
                            new CacheItem(productRepository.findById(productId).orElseThrow()));
                    log.info("Product (id = " + productId + ") was updated in cache");
                }
            }
//        }
        }
    }
}
