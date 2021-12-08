package com.ciaranwood.tapestry.cache.services;

import java.lang.reflect.Method;

import com.ciaranwood.tapestry.cache.services.impl.CacheWriterBridges;

import net.sf.ehcache.Ehcache;

public interface CacheWriterClassFactory {
    
    <T> void add(Ehcache cache, Class<T> serviceInterface, T instance, Method write, String methodKey);

    CacheWriterBridges getCacheWriterBridges(Ehcache cache);
}
