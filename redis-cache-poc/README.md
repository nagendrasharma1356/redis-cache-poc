# Redis Cache POC (Spring Boot 3 + Redis + H2)

A small product-catalogue API that shows Redis caching with Spring's cache annotations.
Data lives in an in-memory **H2** database; hot reads are served from **Redis**.

## Stack
- Java 21, Spring Boot 3.3.4, Maven
- Spring Data JPA + H2 (in-memory)
- Spring Cache + Spring Data Redis (Lettuce), JSON values
- Docker Compose for Redis (plus optional Redis Commander UI)

## 1. Start Redis
```
docker compose up -d
```
Redis: `localhost:6379`  |  Redis Commander UI: http://localhost:8081

No Docker? Run the app with the in-memory cache instead (see "Run without Redis" below).

## 2. Run the app
Import the folder into IntelliJ IDEA as a Maven project (File > Open > select `pom.xml`) and run
`RedisCacheApplication`, or:
```
mvn spring-boot:run
```
App: http://localhost:8080  |  H2 console: http://localhost:8080/h2-console
(JDBC URL `jdbc:h2:mem:redisdb`, user `sa`, empty password)

## 3. Demo: see the cache working
The DB call is slowed down by 2 seconds on purpose (`app.simulated-latency-ms`), and every GET returns an
`X-Response-Time-Ms` header.

| Step | Request | Expected |
|---|---|---|
| 1 | `GET /api/products/1` | ~2000 ms, log shows `DB HIT` |
| 2 | `GET /api/products/1` again | a few ms, no `DB HIT` log |
| 3 | `GET /api/cache/keys` | shows `poc:products:1` with its TTL |
| 4 | `GET /api/cache/value?key=poc:products:1` | JSON stored in Redis |
| 5 | `PUT /api/products/1` (change price) | cache entry refreshed (`@CachePut`) |
| 6 | `GET /api/products/1` | fast, shows the new price |
| 7 | `POST /api/products` | list caches evicted |
| 8 | `GET /api/products` | slow again, then fast on repeat |
| 9 | `DELETE /api/cache/clear` | all caches emptied |

Import `postman/redis-cache-poc.postman_collection.json` into Postman to run these.

## Endpoints
| Method | Path | Cache behaviour |
|---|---|---|
| GET | `/api/products` | `@Cacheable` (`productList::all`) |
| GET | `/api/products/{id}` | `@Cacheable` (`products::{id}`) |
| GET | `/api/products/category/{category}` | `@Cacheable` (`productList::category:{name}`) |
| POST | `/api/products` | `@CacheEvict` all lists |
| PUT | `/api/products/{id}` | `@CachePut` product + evict lists |
| DELETE | `/api/products/{id}` | `@CacheEvict` product + lists |
| GET | `/api/cache/keys` | list Redis keys and TTLs |
| GET | `/api/cache/value?key=` | raw cached JSON |
| DELETE | `/api/cache/clear` | clear all caches |

## Cache design
- Key format: `poc:<cacheName>:<key>`, for example `poc:products:1`
- TTL: `products` 10 min, `productList` 2 min (configurable in `application.properties`)
- Values stored as JSON, nulls are not cached
- If Redis is unreachable, `LoggingCacheErrorHandler` logs the problem and the API falls back to the database

## Inspect Redis directly
```
docker exec -it redis-cache-poc redis-cli
KEYS poc:*
GET poc:products:1
TTL poc:products:1
```

## Run without Redis
```
mvn spring-boot:run -Dspring-boot.run.profiles=nocache
```
This swaps Redis for Spring's simple in-memory cache. The unit test also uses this profile,
so `mvn package` works without Redis running.

## Project layout
```
src/main/java/com/example/rediscache
  RedisCacheApplication.java      @EnableCaching
  config/CacheConfig.java         RedisCacheManager, TTLs, JSON serializer, error handler
  model/Product.java              JPA entity
  repository/ProductRepository    Spring Data JPA
  service/ProductService.java     @Cacheable / @CachePut / @CacheEvict
  controller/ProductController    REST API with X-Response-Time-Ms header
  controller/CacheController      inspect / clear Redis
  exception/                      404 and validation handling
```
