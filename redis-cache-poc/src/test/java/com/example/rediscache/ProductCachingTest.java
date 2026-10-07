package com.example.rediscache;

import com.example.rediscache.repository.ProductRepository;
import com.example.rediscache.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Runs with the "nocache" profile (in-memory cache), so no Redis is needed to build the project.
 * Proves the @Cacheable wiring: two calls, one database hit.
 */
@SpringBootTest
@ActiveProfiles("nocache")
@TestPropertySource(properties = "app.simulated-latency-ms=0")
class ProductCachingTest {

    @Autowired
    private ProductService productService;

    @SpyBean
    private ProductRepository productRepository;

    @Test
    void secondCallIsServedFromCache() {
        productService.getById(1L);
        productService.getById(1L);

        verify(productRepository, times(1)).findById(1L);
    }
}
