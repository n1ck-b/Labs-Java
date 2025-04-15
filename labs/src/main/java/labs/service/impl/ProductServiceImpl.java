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
import labs.aspect.LogExecution;
import labs.dao.MealDao;
import labs.dao.ProductDao;
import labs.dto.ProductDto;
import labs.exception.ExceptionMessages;
import labs.exception.NotFoundException;
import labs.exception.ValidationException;
import labs.model.Product;
import labs.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@LogExecution
public class ProductServiceImpl implements ProductService {
    private final ProductDao productDao;
    private final MealDao mealDao;

    @Autowired
    public ProductServiceImpl(ProductDao productDao, MealDao mealDao) {
        this.productDao = productDao;
        this.mealDao = mealDao;
    }

    @Override
    public ProductDto getProductById(int id) {
        if (!productDao.existsById(id)) {
            throw new NotFoundException(String.format(ExceptionMessages.PRODUCT_NOT_FOUND, id));
        }
        Product product = productDao.getProductById(id);
        return ProductDto.toDto(product);
    }

    @Override
    public List<Product> getProductsByQuery(String query) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = ProductServiceImpl.getJsonFromExternalApi(query);
        List<Product> products = mapper.treeToValue(node.get("items"), new TypeReference<List<Product>>() {});
        if (products.isEmpty()) {
            throw new NotFoundException(String.format(ExceptionMessages.PRODUCTS_NOT_FOUND_BY_QUERY, query));
        }
        return products;
    }

    public static JsonNode getJsonFromExternalApi(String query) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request requestForExternalApi = new Request.Builder().url("https://api.calorieninjas.com/v1/nutrition?query=" +
                query).addHeader("X-Api-Key", "").build();
        Response responseFromExternalApi = client.newCall(requestForExternalApi).execute();
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(responseFromExternalApi.body().string());
    }

    @Override
    public List<Integer> addProductByMealId(int mealId, ProductDto productDto) {
        if (!mealDao.existsById(mealId)) {
            throw new NotFoundException(String.format(ExceptionMessages.MEAL_NOT_FOUND, mealId));
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
        if (!mealDao.existsById(mealId)) {
            throw new NotFoundException(String.format(ExceptionMessages.MEAL_NOT_FOUND, mealId));
        }
        List<Product> products = getProductsByQuery(query);
        List<Product> updatedProducts = products.stream().map(this::setWeightAndCalories).toList();
        return updatedProducts.stream()
                .map(product -> productDao.addProductByMealId(mealId, product)).toList();
    }

    @Override
    public List<ProductDto> getAllProductsByMealId(int mealId) {
        if (!mealDao.existsById(mealId)) {
            throw new NotFoundException(String.format(ExceptionMessages.MEAL_NOT_FOUND, mealId));
        }
        List<Product> products = productDao.getAllProductsByMealId(mealId);
        if (products.isEmpty()) {
            throw new NotFoundException(String.format(ExceptionMessages.PRODUCTS_NOT_FOUND_BY_MEAL_ID,
                    mealId));
        }
        return products.stream().map(ProductDto::toDto).toList();
    }

    @Override
    public List<ProductDto> getAllProducts() {
        List<Product> products = productDao.getAllProducts();
        if (products.isEmpty()) {
            throw new NotFoundException(ExceptionMessages.PRODUCTS_NOT_FOUND);
        }
        return products.stream().map(ProductDto::toDto).toList();
    }

    @Override
    public ResponseEntity<String> deleteProductsByMealId(int mealId) {
        if (!mealDao.existsById(mealId)) {
            throw new NotFoundException(String.format(ExceptionMessages.MEAL_NOT_FOUND, mealId));
        }
        return productDao.deleteProductsByMealId(mealId);
    }

    @Override
    public ResponseEntity<String> deleteProductById(int id) {
        if (!productDao.existsById(id)) {
            throw new NotFoundException(String.format(ExceptionMessages.PRODUCT_NOT_FOUND, id));
        }
        return productDao.deleteProductById(id);
    }

    @Override
    public ResponseEntity<ProductDto> updateProductById(int id, JsonPatch json)
            throws JsonProcessingException, JsonPatchException {
        if (!productDao.existsById(id)) {
            throw new NotFoundException(String.format(ExceptionMessages.PRODUCT_NOT_FOUND, id));
        }
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Product product = productDao.getProductById(id);
        ProductDto productDto = ProductDto.toDto(product);
        if (json.toString().contains("id") || json.toString().contains("meals") ||
                json.toString().contains("weight")) {
            throw new ValidationException(String.format(ExceptionMessages.PATCH_VALIDATION_EXCEPTION,
                    "'id', 'meals' and 'weight'"));
        }
        JsonNode node;
        node = json.apply(objectMapper.convertValue(productDto, JsonNode.class));
        productDto = objectMapper.treeToValue(node, ProductDto.class);
        product = productDto.fromDto();
        return ResponseEntity.ok(ProductDto.toDto(productDao.updateProduct(id, product)));
    }
}
