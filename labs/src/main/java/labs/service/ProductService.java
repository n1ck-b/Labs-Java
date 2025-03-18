package labs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import java.io.IOException;
import java.util.List;
import labs.Product;
import labs.dto.ProductDto;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public interface ProductService {
    ProductDto getProductById(int id) throws IOException;

    List<Product> getProductByQuery(String query) throws IOException;

    List<Integer> addProduct(int mealId, ProductDto productDto);

    List<Integer> addProductsByQuery(int mealId, String query) throws IOException;

    List<ProductDto> getAllProductsByMealId(int mealId);

    List<ProductDto> getAllProducts();

    ResponseEntity<String> deleteProductsByMealId(int mealId);

    ResponseEntity<String> deleteProductById(int id);

    ResponseEntity<ProductDto> updateProductById(int id, JsonPatch json)
            throws JsonPatchException, JsonProcessingException;
}
