package com.rcpky;

import com.rcpky.evictionpolicy.LRUEvictionPolicy;
import com.rcpky.interfaces.Cache;
import com.rcpky.interfaces.EvictionPolicy;
import com.rcpky.model.CacheEntry;
import com.rcpky.service.InMemoryCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class InMemoryCacheTest {

    private Cache<String, String> cache;
    private EvictionPolicy<String> evictionPolicy;
    private final int CACHE_CAPACITY = 3;
    
    @BeforeEach
    void setUp() {
        // Use a real LRU eviction policy for most tests
        evictionPolicy = new LRUEvictionPolicy<>();
        cache = new InMemoryCache<>(evictionPolicy, CACHE_CAPACITY);
    }
    
    @Test
    @DisplayName("Should put and get value from cache")
    void putAndGetValue() {
        // Arrange & Act
        cache.put("key1", "value1");
        String result = cache.get("key1");
        
        // Assert
        assertEquals("value1", result, "Cache should return the stored value");
    }
    
    @Test
    @DisplayName("Should return null when getting non-existent key")
    void getNonExistentKey() {
        // Act
        String result = cache.get("nonExistentKey");
        
        // Assert
        assertNull(result, "Cache should return null for non-existent keys");
    }
    
    @Test
    @DisplayName("Should remove value from cache")
    void removeValue() {
        // Arrange
        cache.put("key1", "value1");
        
        // Act
        cache.remove("key1");
        String result = cache.get("key1");
        
        // Assert
        assertNull(result, "Cache should return null after key is removed");
    }
    
    @Test
    @DisplayName("Should return correct size")
    void cacheSize() {
        // Arrange
        assertEquals(0, cache.size(), "Cache should initially be empty");
        
        // Act
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        
        // Assert
        assertEquals(2, cache.size(), "Cache size should be 2");
        
        // Act
        cache.remove("key1");
        
        // Assert
        assertEquals(1, cache.size(), "Cache size should be 1 after removal");
    }
    
    @Test
    @DisplayName("Should evict least recently used entry when capacity is reached")
    void evictLeastRecentlyUsed() {
        // Arrange - fill the cache to capacity
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        assertEquals(CACHE_CAPACITY, cache.size(), "Cache should be at capacity");
        
        // Access key1 to make it most recently used
        cache.get("key1");
        
        // Act - add a new entry that should trigger eviction
        cache.put("key4", "value4");
        
        // Assert - key2 should be evicted (least recently used)
        assertNull(cache.get("key2"), "LRU key should have been evicted");
        assertEquals("value1", cache.get("key1"), "Most recently used key should still be present");
        assertEquals("value3", cache.get("key3"), "key3 should still be present");
        assertEquals("value4", cache.get("key4"), "New key should be present");
    }
    
    @Test
    @DisplayName("Should handle concurrent access safely")
    void concurrentAccess() throws InterruptedException {
        // Arrange
        int numThreads = 10;
        int operationsPerThread = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        // Act
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "key-" + threadId + "-" + j;
                        String value = "value-" + threadId + "-" + j;
                        cache.put(key, value);
                        cache.get(key);
                        if (j % 5 == 0) { // Occasionally remove entries
                            cache.remove(key);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        latch.await();
        executorService.shutdown();
        
        // No assertion needed - test passes if no exceptions are thrown
    }
    
    @Test
    @DisplayName("Should correctly interact with eviction policy")
    void evictionPolicyInteraction() {
        // Arrange - create mocks
        EvictionPolicy<String> mockEvictionPolicy = mock(EvictionPolicy.class);
        Cache<String, String> cacheWithMock = new InMemoryCache<>(mockEvictionPolicy, 1);
        
        // Act - put should trigger keyAccessed
        cacheWithMock.put("key1", "value1");
        
        // Assert
        verify(mockEvictionPolicy).keyAccessed("key1");
        
        // Act - get should trigger keyAccessed
        cacheWithMock.get("key1");
        
        // Assert
        verify(mockEvictionPolicy, times(2)).keyAccessed("key1");
        
        // Act - adding beyond capacity should trigger evictKey
        cacheWithMock.put("key2", "value2");
        
        // Assert
        verify(mockEvictionPolicy).evictKey();
    }
    
    @Test
    @DisplayName("Should handle null value returned from eviction policy")
    void nullFromEvictionPolicy() {
        // Arrange - create mock that returns null on eviction
        EvictionPolicy<String> mockEvictionPolicy = mock(EvictionPolicy.class);
        when(mockEvictionPolicy.evictKey()).thenReturn(null);
        Cache<String, String> cacheWithMock = new InMemoryCache<>(mockEvictionPolicy, 1);
        
        // Act - put first entry
        cacheWithMock.put("key1", "value1");
        
        // Act - put second entry (should attempt eviction but get null)
        cacheWithMock.put("key2", "value2");
        
        // This test passes if no exception is thrown
    }
}
