package com.example.rediscache.service;

import com.example.rediscache.config.CacheConfig;
import com.example.rediscache.exception.ResourceNotFoundException;
import com.example.rediscache.model.Product;
import com.example.rediscache.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository repository;
    private final long simulatedLatencyMs;

    public ProductService(ProductRepository repository,
                          @Value("${app.simulated-latency-ms:0}") long simulatedLatencyMs) {
        this.repository = repository;
        this.simulatedLatencyMs = simulatedLatencyMs;
    }

    /** READ: result is cached under  poc:products:{id}. Second call never reaches the DB. */
    @Cacheable(cacheNames = CacheConfig.PRODUCTS_CACHE, key = "#id")
    public Product getById(Long id) {
        log.info("DB HIT -> loading product {} from H2", id);
        simulateSlowQuery();
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
    }

    /** READ: whole list cached under  poc:productList:all */
    @Cacheable(cacheNames = CacheConfig.PRODUCT_LIST_CACHE, key = "'all'")
    public List<Product> getAll() {
        log.info("DB HIT -> loading all products from H2");
        simulateSlowQuery();
        return repository.findAll();
    }

    /** READ: cached per category, e.g.  poc:productList:category:electronics */
    @Cacheable(cacheNames = CacheConfig.PRODUCT_LIST_CACHE, key = "'category:' + #category.toLowerCase()")
    public List<Product> getByCategory(String category) {
        log.info("DB HIT -> loading products of category '{}' from H2", category);
        simulateSlowQuery();
        return repository.findByCategoryIgnoreCase(category);
    }

    /** CREATE: cached lists are now stale, so evict them all. */
    @CacheEvict(cacheNames = CacheConfig.PRODUCT_LIST_CACHE, allEntries = true)
    @Transactional
    public Product create(Product product) {
        product.setId(null);
        Product saved = repository.save(product);
        log.info("Created product {} -> evicted productList cache", saved.getId());
        return saved;
    }

    /** UPDATE: refresh the single-product entry (@CachePut) and evict stale lists. */
    @Caching(
            put = @CachePut(cacheNames = CacheConfig.PRODUCTS_CACHE, key = "#id"),
            evict = @CacheEvict(cacheNames = CacheConfig.PRODUCT_LIST_CACHE, allEntries = true)
    )
    @Transactional
    public Product update(Long id, Product updated) {
        Product existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id " + id));
        existing.setName(updated.getName());
        existing.setCategory(updated.getCategory());
        existing.setPrice(updated.getPrice());
        existing.setStock(updated.getStock());
        Product saved = repository.save(existing);
        log.info("Updated product {} -> refreshed products cache, evicted productList cache", id);
        return saved;
    }

    /** DELETE: remove the entry and the stale lists. */
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.PRODUCTS_CACHE, key = "#id"),
            @CacheEvict(cacheNames = CacheConfig.PRODUCT_LIST_CACHE, allEntries = true)
    })
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id " + id);
        }
        repository.deleteById(id);
        log.info("Deleted product {} -> evicted caches", id);
    }

    private void simulateSlowQuery() {
        if (simulatedLatencyMs <= 0) {
            return;
        }
        try {
            Thread.sleep(simulatedLatencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
