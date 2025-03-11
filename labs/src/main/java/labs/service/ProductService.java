package labs.service;

import java.io.IOException;
import java.util.List;
import labs.Product;
import org.springframework.stereotype.Service;

@Service
public interface ProductService {
    Product getProductByName(int id) throws IOException;

    List<Product> getProductByQuery(String query) throws IOException;
}
