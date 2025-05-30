package labs.dao;

import java.util.List;
import labs.model.Product;
import org.springframework.http.ResponseEntity;

public interface ProductDao {
    Product getProductById(int id);

    Product addProductByMealId(int mealId, Product product);

    List<Product> getAllProductsByMealId(int mealId);

    List<Product> getAllProducts();

    ResponseEntity<String> deleteProductsByMealId(int mealId);

    ResponseEntity<String> deleteProductById(int id);

    Product updateProduct(int id, Product updatedProduct);

    int deleteProductByIdIfNotUsed(int id);

    void deleteProductsIfNotUsed(int mealId);

    float getProductWeightFromTable(int mealId, int productId);

    void saveProductWeightToMealProductTable(float weight, int mealId, int productId);

    void saveProductsWeightToTable(List<Product> products, int mealId);

    void updateProductsInCache(List<Integer> mealIds);

    boolean existsById(int id);

    ResponseEntity<String> deleteProductByMealIdAndProductId(int mealId, int productId);
}
