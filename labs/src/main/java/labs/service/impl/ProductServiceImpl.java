package labs.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import labs.dao.ProductDao;
import labs.dto.ProductDto;
import labs.model.Product;
import labs.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {
    private final ProductDao productDao;

    @Autowired
    public ProductServiceImpl(ProductDao productDao) {
        this.productDao = productDao;
    }

    @Override
    public ProductDto getProductById(int id) {
        Product product = productDao.getProductById(id);
        return ProductDto.toDto(product);
    }

    @Override
    public List<Product> getProductsByQuery(String query) throws IOException {
        long startTime = System.currentTimeMillis();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = ProductServiceImpl.getJsonFromExternalApi(query);
        log.info("Time elapsed for product by query = " + (System.currentTimeMillis() - startTime) + "ms");
        return mapper.treeToValue(node.get("items"), new TypeReference<List<Product>>() {});
    }

    private static JsonNode getJsonFromExternalApi(String query) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request requestForExternalApi = new Request.Builder().url("https://api.calorieninjas.com/v1/nutrition?query=" +
                query).addHeader("X-Api-Key", "").build();
        long startTime = System.currentTimeMillis();
        Response responseFromExternalApi = client.newCall(requestForExternalApi).execute();
        log.info("Time elapsed for API request = " + (System.currentTimeMillis() - startTime) + "ms");
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(responseFromExternalApi.body().string());
    }

    @Override
    public List<Integer> addProductByMealId(int mealId, ProductDto productDto) {
        if (productDto.getId() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        List<Integer> ids = new ArrayList<>();
        if (productDto.getWeight() != 100) {
            productDto.setCalories((productDto.getCalories() * 100) / productDto.getWeight());
            productDto.setProteins((productDto.getProteins() * 100) / productDto.getWeight());
            productDto.setCarbs((productDto.getCarbs() * 100) / productDto.getWeight());
            productDto.setFats((productDto.getFats() * 100) / productDto.getWeight());
        }
        ids.add(productDao.addProductByMealId(mealId, productDto.fromDto()));
        return ids;
    }

    public Product setWeightAndCalories(Product product) {
        if (product.getWeight() != 100) {
            product.setCalories((product.getCalories() * 100) / product.getWeight());
            product.setProteins((product.getProteins() * 100) / product.getWeight());
            product.setCarbs((product.getCarbs() * 100) / product.getWeight());
            product.setFats((product.getFats() * 100) / product.getWeight());
        }
        return product;
    }

    @Override
    public List<Integer> addProductsByQueryAndMealId(int mealId, String query) throws IOException {
        List<Product> products = getProductsByQuery(query);
        List<Product> updatedProducts = products.stream().map(this::setWeightAndCalories).toList();
        return updatedProducts.stream()
                .map(product -> productDao.addProductByMealId(mealId, product)).toList();
    }

    @Override
    public List<ProductDto> getAllProductsByMealId(int mealId) {
        List<Product> products = productDao.getAllProductsByMealId(mealId);
        return products.stream().map(ProductDto::toDto).toList();
    }

    @Override
    public List<ProductDto> getAllProducts() {
        return productDao.getAllProducts().stream().map(ProductDto::toDto).toList();
    }

    @Override
    public ResponseEntity<String> deleteProductsByMealId(int mealId) {
        return productDao.deleteProductsByMealId(mealId);
    }

    @Override
    public ResponseEntity<String> deleteProductById(int id) {
        return productDao.deleteProductById(id);
    }

    @Override
    public ResponseEntity<ProductDto> updateProductById(int id, JsonPatch json)
            throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Product product = productDao.getProductById(id);
        if (json.toString().contains("id") || json.toString().contains("meals") ||
                json.toString().contains("weight")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        JsonNode node;
        try {
            node = json.apply(objectMapper.convertValue(product, JsonNode.class));
        } catch (JsonPatchException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        product = objectMapper.treeToValue(node, Product.class);
        Product updatedProduct = productDao.updateProduct(id, product);
        return ResponseEntity.ok(ProductDto.toDto(updatedProduct));
    }
}
