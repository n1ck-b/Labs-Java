package labs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import java.io.IOException;
import java.util.List;
import labs.dto.ProductDto;
import labs.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
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
    public ProductDto getProductById(@PathVariable int id) throws IOException {
        return productService.getProductById(id);
    }

    @GetMapping()
    public List<ProductDto> getProduct(@RequestParam(name = "query", required = false) String query)
            throws IOException {
        if (query != null) {
            return productService.getProductsByQuery(query).stream().map(ProductDto::toDto).toList();
        }
        return productService.getAllProducts();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProductById(@PathVariable int id) {
        return productService.deleteProductById(id);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ProductDto> updateProductById(@PathVariable int id, @RequestBody JsonPatch json)
            throws JsonPatchException, JsonProcessingException {
        return productService.updateProductById(id, json);
    }
}
