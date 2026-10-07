package com.example.rediscache.controller;

import com.example.rediscache.model.Product;
import com.example.rediscache.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Every GET returns an "X-Response-Time-Ms" header so you can see the cache effect:
 * first call ~2000 ms (DB + simulated latency), repeat calls a few ms (Redis).
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final String TIME_HEADER = "X-Response-Time-Ms";

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAll() {
        long start = System.nanoTime();
        List<Product> products = service.getAll();
        return ResponseEntity.ok().header(TIME_HEADER, elapsedMs(start)).body(products);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        long start = System.nanoTime();
        Product product = service.getById(id);
        return ResponseEntity.ok().header(TIME_HEADER, elapsedMs(start)).body(product);
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getByCategory(@PathVariable String category) {
        long start = System.nanoTime();
        List<Product> products = service.getByCategory(category);
        return ResponseEntity.ok().header(TIME_HEADER, elapsedMs(start)).body(products);
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody Product product) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(product));
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody Product product) {
        return service.update(id, product);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private String elapsedMs(long startNanos) {
        return String.valueOf((System.nanoTime() - startNanos) / 1_000_000);
    }
}
