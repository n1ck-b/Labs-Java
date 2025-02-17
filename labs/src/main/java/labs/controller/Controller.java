package labs.controller;

import java.io.IOException;
import java.util.List;
import labs.Product;
import labs.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class Controller {
    private ProductService productService;

    @Autowired
    public Controller(ProductService productService) {
        this.productService = productService;
    }
    
    @GetMapping("/{productName}")
    public Product getMacronutrientsByProductName(@PathVariable String productName) throws IOException {
        return productService.getProductByName(productName);
    }

    @GetMapping("/")
    public List<Product> getMacronutrientsByQuery(@RequestParam(name = "query") String query) throws IOException {
        return productService.getProductByQuery(query);
    }
}
