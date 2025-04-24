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
import labs.aspect.LogExecution;
import labs.dto.MealDto;
import labs.dto.ProductDto;
import labs.service.MealService;
import labs.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@LogExecution
@RequestMapping("/meals")
@Tag(name = "Meal controller", description = "API for CRUD operations with meals")
public class MealController {
    private final MealService mealService;
    private final ProductService productService;

    @Autowired
    public MealController(MealService mealService, ProductService productService) {
        this.mealService = mealService;
        this.productService = productService;
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update meal by ID", description = "Update existing meal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Meal was successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter or request body"),
            @ApiResponse(responseCode = "404", description = "Meal with specified ID wasn't found")
    })
    public ResponseEntity<MealDto> updateMealById(@PathVariable @Positive
            @Parameter(description = "ID of the meal being updated") int id, @RequestBody JsonPatch json)
            throws JsonPatchException, JsonProcessingException {
        return mealService.updateMealById(json, id);
    }

    @GetMapping
    @Operation(summary = "Get meals",
            description = "If parameter 'product-name' is specified, " +
                    "than returns all meals that include this product. Otherwise returns all meals")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Meals were successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter"),
            @ApiResponse(responseCode = "404", description = "Meals with specified product weren't found")
    })
    public List<MealDto> getMeals(@RequestParam(name = "product-name", required = false)
            @Parameter(description = "Optional path parameter, name of the product to get meals by",
                    example = "chicken") String productName) {
        if (productName == null) {
            return mealService.getAllMeals();
        }
        return mealService.getMealsByProductName(productName);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get meal by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Meal was successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter"),
            @ApiResponse(responseCode = "404", description = "Meal with specified ID wasn't found")
    })
    public MealDto getMealById(@PathVariable @Positive @Parameter(description = "ID to get meal by") int id) {
        return mealService.getMealById(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete meal by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Meal was successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter"),
            @ApiResponse(responseCode = "404", description = "Meal with specified ID wasn't found")
    })
    public ResponseEntity<String> deleteMealById(@PathVariable @Positive
            @Parameter(description = "ID to delete meal by") int id) {
        return mealService.deleteMealById(id);
    }

    @PostMapping("/{mealId}/products")
    @Operation(summary = "Add product by meal ID",
            description = "If the product does not exist yet, than creates new product " +
                    "and adds it to specified meal, otherwise just adds it to meal. " +
                    "Product can be passed in request body or 'q' parameter can be used " +
                    "to pass product name and weight")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Product was successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Meal with specified ID wasn't found")
    })
    public List<Integer> addProductByMealId(@RequestParam(name = "q", required = false)
            @Parameter(description = "Optional path parameter, query to add product/products by. " +
                    "This query should contain name of the product and may contain its weight",
                    example = "120g of rice and 200g of chicken") String query,
            @RequestBody(required = false) ProductDto productDto,
            @PathVariable @Positive @Parameter(
                    description = "ID of the meal to add the product to") int mealId) throws IOException {
        if (query == null) {
            return productService.addProductByMealId(mealId, productDto);
        }
        return productService.addProductsByQueryAndMealId(mealId, query);
    }

    @GetMapping("/{mealId}/products")
    @Operation(summary = "Get all products by meal ID",
            description = "Returns all products, that belong to specified meal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products were successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter"),
            @ApiResponse(responseCode = "404", description = "Products weren't found for specified meal")
    })
    public List<ProductDto> getAllProductsByMealId(@PathVariable @Positive
            @Parameter(description = "ID of the meal to get all product by") int mealId) {
        return productService.getAllProductsByMealId(mealId);
    }

    @DeleteMapping("/{mealId}/products")
    @Operation(summary = "Delete products by meal ID",
            description = "Deletes all products from specified meal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Products were successfully removed from meal"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter"),
            @ApiResponse(responseCode = "404", description = "Meal with specified ID wasn't found")
    })
    public ResponseEntity<String> deleteProductsByMealId(@PathVariable @Positive
            @Parameter(description = "ID of the meal, from which products should be removed") int mealId) {
        return productService.deleteProductsByMealId(mealId);
    }
}
