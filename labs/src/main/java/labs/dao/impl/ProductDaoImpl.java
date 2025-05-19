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
import org.springframework.context.annotation.Lazy;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@LogExecution
public class ProductDaoImpl implements ProductDao {
    @PersistenceContext
    private EntityManager entityManager;

    private final MealDao mealDao;
    private final MealRepository mealRepository;
    private final ProductRepository productRepository;
    private final SessionCache cache;
    private final DayRepository dayRepository;

    @Autowired
    public ProductDaoImpl(@Lazy MealDao mealDao, MealRepository mealRepository,
                          ProductRepository productRepository, SessionCache cache,
                          DayRepository dayRepository) {
        this.mealDao = mealDao;
        this.mealRepository = mealRepository;
        this.productRepository = productRepository;
        this.cache = cache;
        this.dayRepository = dayRepository;
    }

    @Override
    public Product getProductById(int id) {
        if (cache.exists("Product" + id)) {
            return (Product) cache.getObject("Product" + id);
        }
        Product product = productRepository.findById(id).orElseThrow();
        cache.addObject("Product" + id, product);
        return product;
    }

    public void updatedProductsInMealsInCache(Product product) {
        List<Integer> mealIds = mealRepository.getMealIdsByProductName(product.getName());
        Product productCopy;
        for (int id : mealIds) {
            productCopy = mealDao.setRealWeightAndCaloriesForProduct(id, product);
            if (cache.exists("Meal" + id)) {
                ((Meal) cache.getObject("Meal" + id)).getProducts().remove(productCopy);
                ((Meal) cache.getObject("Meal" + id)).getProducts().add(productCopy);
            }
        }
    }

    @Override
    @Transactional
    public Product addProductByMealId(int mealId, Product product) {
        entityManager.flush();
        Meal mealById;
        Product productCopy;
        if (cache.exists("Meal" + mealId)) {
            mealById = (Meal) cache.getObject("Meal" + mealId);
        } else {
            mealById = mealRepository.findById(mealId).orElseThrow();
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
            } else {
                return productFromDb;
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
                }
            }
            return productFromDb;
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
        }
        return product;
    }

    public List<Product> getProductsByIds(List<Integer> ids, int mealId, boolean withRealWeight) {
        List<Product> products = new ArrayList<>();
        List<Integer> idsOfProductsNotFoundInCache = new ArrayList<>();
        Product product;
        for (int id : ids) {
            if (cache.exists("Product" + id)) {
                if (!withRealWeight) {
                    products.add((Product) cache.getObject("Product" + id));
                } else {
                    products.add(mealDao.setRealWeightAndCaloriesForProduct(mealId,
                            (Product) cache.getObject("Product" + id)));
                }
            } else {
                idsOfProductsNotFoundInCache.add(id);
            }
        }
        if (!idsOfProductsNotFoundInCache.isEmpty()) {
            for (int id : idsOfProductsNotFoundInCache) {
                product = productRepository.findById(id).orElseThrow();
                if (withRealWeight) {
                    product = mealDao.setRealWeightAndCaloriesForProduct(mealId, product);
                }
                cache.addObject("Product" + id, product);
                products.add(product);
            }
        }
        return products;
    }

    @Override
    public List<Product> getAllProductsByMealId(int mealId) {
        List<Integer> productIds = productRepository.getProductsIdsByMealId(mealId);
        if (productIds.isEmpty()) {
            return new ArrayList<>();
        }
        return getProductsByIds(productIds, mealId, true);
    }

    @Override
    public List<Product> getAllProducts() {
        List<Integer> productIds = productRepository.getAllProductsIds();
        if (productIds.isEmpty()) {
            return new ArrayList<>();
        }
        return getProductsByIds(productIds, 0, false);
    }

    @Transactional
    public ResponseEntity<String> deleteProductById(int id) {
        List<Integer> mealIds = mealRepository.findMealIdsByProductId(id);
        productRepository.deleteById(id);
        if (cache.exists("Product" + id)) {
            cache.removeObject("Product" + id);
        }
        for (int mealId : mealIds) {
            if (cache.exists("Meal" + mealId)) {
                Meal mealFromCache = (Meal) cache.getObject("Meal" + mealId);
                mealFromCache.getProducts().removeIf(product -> product.getId() == id);
            }
            Day day = dayRepository.findDayByMealId(mealId);
            if (cache.exists("Day" + day.getId())) {
                Day dayFromCache = (Day) cache.getObject("Day" + day.getId());
                dayFromCache.getMeals()
                        .forEach(meal -> meal.getProducts()
                                .removeIf(product -> product.getId() == id));
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

    public void updateMealAndDaysInCache(List<Integer> mealIds, int mealId) {
        for (int id : mealIds) {
            if (cache.exists("Meal" + id)) {
                Meal meal = (Meal) cache.getObject("Meal" + id);
                meal.getProducts().forEach(product -> product.getMeals()
                        .removeIf(mealFromList -> mealFromList.getId() == mealId));
            }
            Day day = dayRepository.findDayByMealId(id);
            if (cache.exists("Day" + day.getId())) {
                cache.removeObject("Day" + day.getId());
            }
        }
    }

    @Transactional
    public void deleteProductIfNotUsedOrDeleteFromMeal(int mealId, int productId) {
        if (deleteProductByIdIfNotUsed(productId) == 0) {
            final List<Integer> mealIds = mealRepository.findMealIdsByProductId(productId);
            List<Meal> meals = mealIds
                    .stream()
                    .map(id -> mealRepository.findById(id).orElseThrow())
                    .collect(Collectors.toList());
            for (Meal meal : meals) {
                mealDao.setRealWeightAndCaloriesForAllProducts(meal);
            }
            productRepository.deleteProductFromMealProductTable(mealId, productId);
            entityManager.flush();
            if (cache.exists("Product" + productId)) {
                Product product = (Product) cache.getObject("Product" + productId);
                product.getMeals().removeIf(meal -> meal.getId() == mealId);
            }
            if (cache.exists("Meal" + mealId)) {
                Meal meal = (Meal) cache.getObject("Meal" + mealId);
                meal.getProducts().removeIf(product -> product.getId() == productId);
            }
            int dayId = mealRepository.findById(mealId).orElseThrow().getDay().getId();
            if (cache.exists("Day" + dayId)) {
                Day day = (Day) cache.getObject("Day" + dayId);
                day.getMeals().stream().forEach(meal -> {
                    if (meal.getId() == mealId) {
                        meal.getProducts().removeIf(product -> product.getId() == productId);
                    }
                });
            }
            updateMealAndDaysInCache(mealIds, mealId);
            for (Meal meal : meals) {
                if (meal.getId() != mealId) {
                    saveProductsWeightToTable(meal.getProducts(), meal.getId());
                }
                if (meal.getId() == mealId && !meal.getProducts().isEmpty()) {
                    saveProductsWeightToTable(meal.getProducts(), meal.getId());
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
        List<Integer> productIds = productRepository.getProductsIdsByMealId(mealId);
        if (productIds.isEmpty()) {
            throw new NotFoundException(String.format(
                    ExceptionMessages.PRODUCTS_NOT_FOUND_BY_MEAL_ID, mealId));
        }
        productIds.stream().forEach(id -> deleteProductIfNotUsedOrDeleteFromMeal(mealId, id));
        return ResponseEntity.ok("Deleted successfully");
    }

    @Override
    @Transactional
    public Product updateProduct(int id, Product updatedProduct) {
        List<Meal> meals = getProductById(id).getMeals();
        updatedProduct.setMeals(meals);
        productRepository.save(updatedProduct);
        entityManager.flush();
        cache.addObject("Product" + id, updatedProduct);
        List<Integer> mealIds = mealRepository.findMealIdsByProductId(id);
        for (int mealId : mealIds) {
            Day day = dayRepository.findDayByMealId(mealId);
            if (cache.exists("Day" + day.getId())) {
                cache.removeObject("Day" + day.getId());
            }
        }
        return updatedProduct;
    }

    @Override
    public float getProductWeightFromTable(int mealId, int productId) {
        return productRepository.getProductWeightFromMealProductTable(mealId, productId);
    }

    @Transactional
    @Override
    public void saveProductWeightToMealProductTable(float weight, int mealId, int productId) {
        productRepository.saveProductWeightToMealProductTable(weight, mealId, productId);
    }

    @Transactional
    @Override
    public void saveProductsWeightToTable(List<Product> products, int mealId) {
        products.stream().forEach(product -> saveProductWeightToMealProductTable(product.getWeight(),
                                mealId, product.getId()));
    }

    @Override
    public void updateProductsInCache(List<Integer> productIds) {
        if (productIds != null) {
            for (int productId : productIds) {
                if (cache.exists("Product" + productId)) {
                    Product product = productRepository.findById(productId).orElseThrow();
                    cache.addObject("Product" + productId, product);
                }
            }
        }
    }

    @Override
    public boolean existsById(int id) {
        return productRepository.existsById(id);
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteProductByMealIdAndProductId(int mealId, int productId) {
        deleteProductIfNotUsedOrDeleteFromMeal(mealId, productId);
        return ResponseEntity.ok("Deleted successfully");
    }
}
