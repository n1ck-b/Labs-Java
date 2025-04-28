package service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import java.io.IOException;
import java.util.List;
import labs.dao.MealDao;
import labs.dao.ProductDao;
import labs.dto.ProductDto;
import labs.exception.NotFoundException;
import labs.exception.ValidationException;
import labs.model.Product;
import labs.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class ProductServiceImplTest {
    private static final int ID1 = 1;
    private static final int ID2 = 2;
    private static final String QUERY = "bread";
    private Product product1;
    private Product product2;
    private ProductDto productDtoWithDifferentWeight;
    private Product productWithDifferentWeight;
    private ProductDto productDto;
    private ResponseEntity<String> responseEntityForDeletion;
    private JsonPatch jsonPatch;
    private JsonPatch jsonPatchWithError;
    private Product updatedProduct;

    @Mock
    private ProductDao productDao;

    @Mock
    private MealDao mealDao;

    @InjectMocks
    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() throws IOException {
        product1 = new Product(ID1, "bread", 100, 261.6f, 8.8f, 50.2f, 3.4f, List.of());
        product2 = new Product(ID2, "rice", 100, 140, 0.9f, 45, 7, List.of());
        productDto = new ProductDto(ID1, "bread", List.of(), 100.0f, 261.6f, 8.8f, 50.2f, 3.4f);
        productDtoWithDifferentWeight = new ProductDto(ID1, "bread", List.of(), 200.0f,
                523.2f, 17.6f, 100.4f, 6.8f);
        productWithDifferentWeight = new Product(ID1, "bread", 200.0f, 523.2f,
                17.6f, 100.4f, 6.8f, List.of());
        responseEntityForDeletion = new ResponseEntity<>("Deleted successfully", HttpStatus.OK);
        updatedProduct = new Product(ID1, "bread", 100, 261.6f, 8.7f, 50.2f, 3.4f, List.of());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        String patch = """
                [
                    {
                        "op": "replace",
                        "path": "/proteins",
                        "value": "8.7"
                    }
                ]""";
        String patchWithError = """
                [
                    {
                        "op": "replace",
                        "path": "/id",
                        "value": "8"
                    }
                ]""";
        jsonPatch = JsonPatch.fromJson(mapper.readTree(patch));
        jsonPatchWithError = JsonPatch.fromJson(mapper.readTree(patchWithError));
    }

    @Test
    void testGetProductById_WhenExists() {
        Mockito.when(productDao.existsById(ID1)).thenReturn(true);
        Mockito.when(productDao.getProductById(ID1)).thenReturn(product1);

        ProductDto result = productService.getProductById(ID1);

        assertEquals(ProductDto.toDto(product1), result);
        Mockito.verify(productDao, Mockito.times(1)).getProductById(ID1);
    }

    @Test
    void testGetProductById_WhenNotExists() {
        Mockito.when(productDao.existsById(ID1)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> productService.getProductById(ID1));
        Mockito.verify(productDao, Mockito.times(0)).getProductById(ID1);
    }

    @Test
    void testGetProductByQuery_WhenExists() throws IOException {
        List<Product> result = productService.getProductsByQuery(QUERY);

        assertEquals(List.of(new Product(0, "bread", 100, 261.6f, 8.8f, 50.2f, 3.4f, List.of())), result);
    }

    @Test
    void testGetProductByQuery_WhenNotExists() {
        assertThrows(NotFoundException.class, () -> productService.getProductsByQuery("null"));
    }

    @Test
    void testAddProductByMealId_WhenExists() {
        Mockito.when(mealDao.existsById(ID1)).thenReturn(true);
        Mockito.when(productDao.addProductByMealId(ID1, productDto.fromDto())).thenReturn(ID1);

        List<Integer> result = productService.addProductByMealId(ID1, productDto);

        assertEquals(List.of(ID1), result);
        Mockito.verify(productDao, Mockito.times(1)).addProductByMealId(ID1, productDto.fromDto());
    }

    @Test
    void testAddProductByMealId_WhenNotExists() {
        Mockito.when(mealDao.existsById(ID1)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> productService.addProductByMealId(ID1, productDto));
        Mockito.verify(productDao, Mockito.times(0)).addProductByMealId(ID1, productDto.fromDto());
    }

    @Test
    void testAddProductByMealId_WhenProductHasDifferentWeight() {
        Product product = new Product(ID1, "bread", 200, 261.6f, 8.8f, 50.2f, 3.4f, List.of());
        Mockito.when(mealDao.existsById(ID1)).thenReturn(true);
        Mockito.when(productDao.addProductByMealId(ID1, product)).thenReturn(ID1);

        List<Integer> result = productService.addProductByMealId(ID1, productDtoWithDifferentWeight);

        assertEquals(List.of(ID1), result);
        Mockito.verify(productDao, Mockito.times(1)).addProductByMealId(ID1, product);
    }

    @Test
    void testSetProductWeight_WhenWeightIsDifferent() {
        Product result = productService.setWeightAndCalories(productWithDifferentWeight);

        assertEquals(new Product(ID1, "bread", 200, 261.6f, 8.8f, 50.2f, 3.4f, List.of()), result);
    }

    @Test
    void testAddProductsByQueryAndMealId_WhenNotExists() {
        Mockito.when(mealDao.existsById(ID1)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> productService.addProductsByQueryAndMealId(ID1, QUERY));
        Mockito.verify(productDao, Mockito.times(0)).addProductByMealId(ID1, product1);
    }

    @Test
    void testGetAllProducts_WhenExist() {
        Mockito.when(productDao.getAllProducts()).thenReturn(List.of(product1, product2));

        List<ProductDto> result = productService.getAllProducts();

        assertEquals(List.of(product1, product2).stream().map(ProductDto::toDto).toList(), result);
        Mockito.verify(productDao, Mockito.times(1)).getAllProducts();
    }

    @Test
    void testGetAllProducts_WhenNotExist() {
        Mockito.when(productDao.getAllProducts()).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> productService.getAllProducts());
        Mockito.verify(productDao, Mockito.times(1)).getAllProducts();
    }

    @Test
    void testDeleteProductsByMealId_WhenExists() {
        Mockito.when(mealDao.existsById(ID1)).thenReturn(true);
        Mockito.when(productDao.deleteProductsByMealId(ID1)).thenReturn(responseEntityForDeletion);

        ResponseEntity<String> result = productService.deleteProductsByMealId(ID1);

        assertEquals(responseEntityForDeletion, result);
        Mockito.verify(productDao, Mockito.times(1)).deleteProductsByMealId(ID1);
    }

    @Test
    void testDeleteProductsByMealId_WhenNotExists() {
        Mockito.when(mealDao.existsById(ID1)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> productService.deleteProductsByMealId(ID1));
        Mockito.verify(productDao, Mockito.times(0)).deleteProductsByMealId(ID1);
    }

    @Test
    void testDeleteProductId_WhenExists() {
        Mockito.when(productDao.existsById(ID1)).thenReturn(true);
        Mockito.when(productDao.deleteProductById(ID1)).thenReturn(responseEntityForDeletion);

        ResponseEntity<String> result = productService.deleteProductById(ID1);

        assertEquals(responseEntityForDeletion, result);
        Mockito.verify(productDao, Mockito.times(1)).deleteProductById(ID1);
    }

    @Test
    void testDeleteProductId_WhenNotExists() {
        Mockito.when(productDao.existsById(ID1)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> productService.deleteProductById(ID1));
        Mockito.verify(productDao, Mockito.times(0)).deleteProductById(ID1);
    }

    @Test
    void testUpdateProductById_WhenExists() throws JsonPatchException, JsonProcessingException {
        Mockito.when(productDao.existsById(ID1)).thenReturn(true);
        Mockito.when(productDao.getProductById(ID1)).thenReturn(product1);
        Mockito.when(productDao.updateProduct(ID1, updatedProduct)).thenReturn(updatedProduct);

        ResponseEntity<ProductDto> result = productService.updateProductById(ID1, jsonPatch);

        assertEquals(new ResponseEntity<>(ProductDto.toDto(updatedProduct), HttpStatus.OK), result);
        Mockito.verify(productDao, Mockito.times(1)).updateProduct(ID1, updatedProduct);
    }

    @Test
    void testUpdateProductById_WhenNotExists() {
        Mockito.when(productDao.existsById(ID1)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> productService.updateProductById(ID1, jsonPatch));
        Mockito.verify(productDao, Mockito.times(0)).updateProduct(ID1, updatedProduct);
    }

    @Test
    void testUpdateProductById_WhenJsonContainsRestrictedField() {
        Mockito.when(productDao.existsById(ID1)).thenReturn(true);
        Mockito.when(productDao.getProductById(ID1)).thenReturn(product1);

        assertThrows(ValidationException.class,
                () -> productService.updateProductById(ID1, jsonPatchWithError));
        Mockito.verify(productDao, Mockito.times(0)).updateProduct(ID1, updatedProduct);
    }

    @Test
    void testGetAllProductsByMealId_WhenExists() {
        Mockito.when(mealDao.existsById(ID1)).thenReturn(true);
        Mockito.when(productDao.getAllProductsByMealId(ID1)).thenReturn(List.of(product1, product2));

        List<ProductDto> result = productService.getAllProductsByMealId(ID1);

        assertEquals(List.of(product1, product2).stream().map(ProductDto::toDto).toList(), result);
        Mockito.verify(productDao, Mockito.times(1)).getAllProductsByMealId(ID1);
    }

    @Test
    void testGetAllProductsByMealId_WhenMealNotExists() {
        Mockito.when(mealDao.existsById(ID1)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> productService.getAllProductsByMealId(ID1));
        Mockito.verify(productDao, Mockito.times(0)).getAllProductsByMealId(ID1);
    }

    @Test
    void testGetAllProductsByMealId_WhenProductsNotExist() {
        Mockito.when(mealDao.existsById(ID1)).thenReturn(true);
        Mockito.when(productDao.getAllProductsByMealId(ID1)).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> productService.getAllProductsByMealId(ID1));
        Mockito.verify(productDao, Mockito.times(1)).getAllProductsByMealId(ID1);
    }
}
