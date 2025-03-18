package labs.dao.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import labs.Meal;
import labs.Product;
import labs.dao.MealDao;
import labs.dao.ProductDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class ProductDaoImpl implements ProductDao {
    @PersistenceContext
    private EntityManager entityManager;

    private final MealDao mealDao;

    @Autowired
    public ProductDaoImpl(MealDao mealDao) {
        this.mealDao = mealDao;
    }

    @Override
    public Product getProductById(int id) {
        return entityManager.find(Product.class, id);
    }

    @Override
    @Transactional
    public int addProduct(int mealId, Product product) {
        Meal mealById = mealDao.getMealById(mealId);
        if (mealById == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        List<Product> productsFromDb = entityManager
                .createQuery("SELECT p FROM Product p WHERE name = :name AND weight = :weight", Product.class)
                .setParameter("name", product.getName())
                .setParameter("weight", product.getWeight())
                .getResultList();
        if (!productsFromDb.isEmpty()) {
            Product productFromDb = productsFromDb.get(0);
            List<Meal> mealsOfProductFromDb = productFromDb.getMeals();
            if (!mealsOfProductFromDb.contains(mealById)) {
                mealsOfProductFromDb.add(mealById);
            }
            entityManager.flush();
            return productFromDb.getId();
        }
        List<Meal> meal = new ArrayList<>();
        meal.add(mealById);
        product.setMeals(meal);
        entityManager.persist(product);
        entityManager.flush();
        return product.getId();
    }

    @Override
    public List<Product> getAllProductsByMealId(int mealId) {
        List<?> results = entityManager
                .createNativeQuery("SELECT product_id FROM meal_product WHERE meal_id = :mealId")
                .setParameter("mealId", mealId)
                .getResultList();
        List<Integer> productIds = results.stream().map(x -> ((Number) x).intValue()).toList();
        return productIds.stream().map(id -> entityManager.find(Product.class, id)).toList();
    }

    @Override
    public List<Product> getAllProducts() {
        return entityManager.createQuery("SELECT p FROM Product p", Product.class).getResultList();
    }

    @Transactional
    public ResponseEntity<String> deleteProductById(int id) {
        if (entityManager.createQuery("DELETE FROM Product WHERE id = :id")
                .setParameter("id", id).executeUpdate() == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok("Deleted successfully");
    }

    @Override
    @Transactional
    // СДЕЛАТЬ ТАК, ЧТО ЕСЛИ ЭТИ ПРОДУКТЫ ЕЩЕ ГДЕ-ТО ЕСТЬ, ТО НЕ УДАЛЯТЬ ИХ
    public ResponseEntity<String> deleteProductsByMealId(int mealId) {
        List<?> results = entityManager
                .createNativeQuery("SELECT product_id FROM meal_product WHERE meal_id = :mealId")
                .setParameter("mealId", mealId)
                .getResultList();
        if (results.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        List<Integer> productIds = results.stream().map(x -> ((Number) x).intValue()).toList();
        productIds.stream().forEach(id -> entityManager.createQuery("DELETE FROM Product WHERE id = :id")
                .setParameter("id", id).executeUpdate());
        return ResponseEntity.ok("Deleted successfully");
    }

    @Override
    @Transactional
    public Product updateProduct(int id, Product updatedProduct) {
        updatedProduct.setMeals(getProductById(id).getMeals());
        entityManager.merge(updatedProduct);
        entityManager.flush();
        return updatedProduct;
    }
}
