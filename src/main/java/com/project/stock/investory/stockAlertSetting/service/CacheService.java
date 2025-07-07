package com.project.stock.investory.stockAlertSetting.service;

import com.project.stock.investory.stockInfo.model.Stock;
import com.project.stock.investory.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    // 사용자 캐시 관련
    private static final String USER_CACHE_PREFIX = "user:";
    private static final String STOCK_CACHE_PREFIX = "stock:";
    
//    // 🔥 사용자 정보 캐시 조회 => 기존
//    @Cacheable(value = "users", key = "#userId", unless = "#result == null")
//    public User getUserFromCache(Long userId) {
//        log.debug("사용자 캐시 조회: userId={}", userId);
//        return null; // Spring Cache가 자동으로 처리
//    }

    @Cacheable(value = "users", key = "#userId", unless = "#result == null")
    public User getUserFromCache(Long userId) {
        try {
            log.debug("사용자 캐시 조회: userId={}", userId);
            return null;
        } catch (Exception e) {
            if (e.getCause() instanceof ClassCastException) {
                log.warn("클래스 로더 충돌 감지 - 캐시 자동 정리: userId={}", userId);
                evictUserCache(userId);
            }
            return null; // DB에서 조회하도록 fallback
        }
    }


    // 🔥 사용자 정보 캐시 저장
    @CachePut(value = "users", key = "#user.userId")
    public User putUserToCache(User user) {
        log.debug("사용자 캐시 저장: userId={}", user.getUserId());
        return user;
    }

    // 🔥 사용자 캐시 삭제
    @CacheEvict(value = "users", key = "#userId")
    public void evictUserCache(Long userId) {
        log.debug("사용자 캐시 삭제: userId={}", userId);
    }

    // 🔥 주식 정보 캐시 조회
    @Cacheable(value = "stocks", key = "#stockCode", unless = "#result == null")
    public Stock getStockFromCache(String stockCode) {
        log.debug("주식 캐시 조회: stockCode={}", stockCode);
        return null; // Spring Cache가 자동으로 처리
    }

    // 🔥 주식 정보 캐시 저장
    @CachePut(value = "stocks", key = "#stock.stockId")
    public Stock putStockToCache(Stock stock) {
        log.debug("주식 캐시 저장: stockCode={}", stock.getStockId());
        return stock;
    }

    // 🔥 주식 캐시 삭제
    @CacheEvict(value = "stocks", key = "#stockCode")
    public void evictStockCache(String stockCode) {
        log.debug("주식 캐시 삭제: stockCode={}", stockCode);
    }

    // 🔥 모든 사용자 캐시 삭제
    @CacheEvict(value = "users", allEntries = true)
    public void evictAllUserCache() {
        log.info("모든 사용자 캐시 삭제");
    }

    // 🔥 모든 주식 캐시 삭제
    @CacheEvict(value = "stocks", allEntries = true)
    public void evictAllStockCache() {
        log.info("모든 주식 캐시 삭제");
    }

    // 🔥 수동으로 Redis에서 값 저장/조회 (필요한 경우)
    public void setWithExpire(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}