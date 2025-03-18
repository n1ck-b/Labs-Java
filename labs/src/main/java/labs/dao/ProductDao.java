package labs.dao;

import java.util.List;
import labs.Product;
import org.springframework.http.ResponseEntity;

public interface ProductDao {
    Product getProductById(int id);

    int addProduct(int mealId, Product product);

    List<Product> getAllProductsByMealId(int mealId);

    List<Product> getAllProducts();

    ResponseEntity<String> deleteProductsByMealId(int mealId);

    ResponseEntity<String> deleteProductById(int id);

    Product updateProduct(int id, Product updatedProduct);
}
