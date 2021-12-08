package com.ciaranwood.tapestry.cache.services;

import com.ciaranwood.tapestry.cache.services.annotations.CacheKey;
import com.ciaranwood.tapestry.cache.services.annotations.CacheResult;
import com.ciaranwood.tapestry.cache.services.annotations.WriteThrough;

public class StringStoreDao implements StringDao {

    private final StringStore store;

    public StringStoreDao(StringStore store) {
        this.store = store;
    }

    @Override
    @CacheResult
    public String get(@CacheKey Integer key) {
        return store.get(key);
    }

    @Override
    @WriteThrough
    public void put(@CacheKey Integer key, String data) {
        store.put(key, data);
    }
}
