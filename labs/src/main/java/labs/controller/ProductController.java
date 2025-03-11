package labs.controller;

import java.io.IOException;
import java.util.List;
import labs.Product;
import labs.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{id}")
    public Product getMacronutrientsByProductName(@PathVariable int id) throws IOException {
        return productService.getProductByName(id);
    }

    @GetMapping()
    public List<Product> getMacronutrientsByQuery(@RequestParam(name = "query") String query)
            throws IOException {
        return productService.getProductByQuery(query);
    }
}