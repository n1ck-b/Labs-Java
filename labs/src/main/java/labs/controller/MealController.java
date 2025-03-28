package labs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import java.io.IOException;
import java.util.List;
import labs.dto.MealDto;
import labs.dto.ProductDto;
import labs.service.MealService;
import labs.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/meals")
public class MealController {
    private final MealService mealService;
    private final ProductService productService;

    @Autowired
    public MealController(MealService mealService, ProductService productService) {
        this.mealService = mealService;
        this.productService = productService;
    }

    @PatchMapping("/{id}")
    public ResponseEntity<MealDto> updateMealById(@PathVariable int id, @RequestBody JsonPatch json)
            throws JsonPatchException, JsonProcessingException {
        return mealService.updateMealById(json, id);
    }

    @GetMapping
    public List<MealDto> getMeals(@RequestParam(name = "product-name", required = false) String productName) {
        if (productName == null) {
            return mealService.getAllMeals();
        }
        return mealService.getMealsByProductName(productName);
    }

    @GetMapping("/{id}")
    public MealDto getMealById(@PathVariable int id) {
        return mealService.getMealById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteMealById(@PathVariable int id) {
        return mealService.deleteMealById(id);
    }

    @PostMapping("/{mealId}/products")
    public List<Integer> addProductByMealId(@RequestParam(name = "q", required = false) String query,
                                  @RequestBody(required = false) ProductDto productDto,
                                  @PathVariable int mealId) throws IOException {
        if (query == null) {
            return productService.addProductByMealId(mealId, productDto);
        }
        return productService.addProductsByQueryAndMealId(mealId, query);
    }

    @GetMapping("/{mealId}/products")
    public List<ProductDto> getAllProductsByMealId(@PathVariable int mealId) {
        return productService.getAllProductsByMealId(mealId);
    }

    @DeleteMapping("/{mealId}/products")
    public ResponseEntity<String> deleteProductsByMealId(@PathVariable int mealId) {
        return productService.deleteProductsByMealId(mealId);
    }

//    @GetMapping
//    public List<MealDto> getMealsByProductName(@RequestParam(name = "product-name") String productName) {
//        return mealService.getMealsByProductName(productName);
//    }
}
