package labs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.io.IOException;
import java.util.List;
import labs.aspect.CountVisits;
import labs.aspect.LogExecution;
import labs.dto.ProductDto;
import labs.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CountVisits
@Validated
@RestController
@LogExecution
@RequestMapping("/products")
@Tag(name = "Product controller", description = "API for CRUD operations with products")
@CrossOrigin(origins = "http://localhost:3000")
public class ProductController {
    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product was successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter"),
            @ApiResponse(responseCode = "404", description = "Product with specified ID wasn't found")
    })
    public ProductDto getProductById(@PathVariable @Positive
            @Parameter(description = "ID to get the day by") int id) {
        return productService.getProductById(id);
    }

    @GetMapping()
    @Operation(summary = "Get products",
            description = "Returns all products if parameter 'query' is ignored. " +
                    "Otherwise, returns products specified in query")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products were successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter"),
            @ApiResponse(responseCode = "404", description = "Products weren't found for specified query")
    })
    public List<ProductDto> getProduct(@RequestParam(name = "query", required = false)
            @Parameter(description = "Optional path parameter, query to get product/products by. " +
            "This query should contain name of the product and may contain its weight",
            example = "120g of rice and 200g of chicken") String query) throws IOException {
        if (query != null) {
            return productService.getProductsByQuery(query).stream().map(ProductDto::toDto).toList();
        }
        return productService.getAllProducts();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product was successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter"),
            @ApiResponse(responseCode = "404", description = "Product with specified ID wasn't found")
    })
    public ResponseEntity<String> deleteProductById(@PathVariable @Positive
            @Parameter(description = "ID to delete product by") int id) {
        return productService.deleteProductById(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update product by ID", description = "Update existing product")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product was successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter or request body"),
            @ApiResponse(responseCode = "404", description = "Product with specified ID wasn't found")
    })
    public ResponseEntity<ProductDto> updateProductById(@PathVariable @Positive
            @Parameter(description = "ID of the product being updated") int id,
            @RequestBody JsonPatch json) throws JsonPatchException, JsonProcessingException {
        return productService.updateProductById(id, json);
    }
}
