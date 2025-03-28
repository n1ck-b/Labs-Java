package labs.dao;

import java.util.List;
import labs.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    @Modifying
    @Query(nativeQuery = true,
            value = "DELETE FROM meal_product WHERE meal_id = :mealId AND product_id = :productId")
    void deleteProductFromMealProductTable(int mealId, int productId);

    @Query(nativeQuery = true, value = "SELECT product_id FROM meal_product WHERE meal_id = :mealId")
    List<Integer> getProductsIdsByMealId(int mealId);

    Product findByName(String name);

    @Modifying
    @Query(nativeQuery = true, value =
            "UPDATE meal_product SET product_weight = ?1 WHERE meal_id = ?2 AND product_id = ?3")
    void saveProductWeightToMealProductTable(float weight, int mealId, int productId);

    @Query(nativeQuery = true, value =
            "SELECT product_weight FROM meal_product WHERE meal_id = :mealId AND product_id = :productId")
    float getProductWeightFromMealProductTable(int mealId, int productId);

    @Modifying
    @Query(nativeQuery = true, value =
            "INSERT INTO meal_product (meal_id, product_id, product_weight) VALUES (?1, ?2, ?3)")
    void saveProductToMealProductTable(int mealId, int productId, float weight);

    @Query("SELECT id FROM Product WHERE name = :name")
    Integer getIdByName(String name);

    @Query("SELECT id FROM Product")
    List<Integer> getAllProductsIds();
}
