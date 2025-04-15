package labs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import java.io.IOException;
import java.util.List;
import labs.dto.ProductDto;
import labs.model.Product;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public interface ProductService {
    ProductDto getProductById(int id);

    List<Product> getProductsByQuery(String query) throws IOException;

    List<Integer> addProductByMealId(int mealId, ProductDto productDto);

    List<Integer> addProductsByQueryAndMealId(int mealId, String query) throws IOException;

    List<ProductDto> getAllProductsByMealId(int mealId);

    List<ProductDto> getAllProducts();

    ResponseEntity<String> deleteProductsByMealId(int mealId);

    ResponseEntity<String> deleteProductById(int id);

    ResponseEntity<ProductDto> updateProductById(int id, JsonPatch json)
            throws JsonPatchException, JsonProcessingException;
}
