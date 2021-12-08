package com.ciaranwood.tapestry.cache.services.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.tapestry5.ioc.services.PlasticProxyFactory;
import org.apache.tapestry5.plastic.ClassInstantiator;
import org.apache.tapestry5.plastic.ConstructorCallback;
import org.apache.tapestry5.plastic.InstanceContext;
import org.apache.tapestry5.plastic.InstructionBuilder;
import org.apache.tapestry5.plastic.InstructionBuilderCallback;
import org.apache.tapestry5.plastic.PlasticField;
import org.apache.tapestry5.plastic.PlasticUtils;

import com.ciaranwood.tapestry.cache.services.CacheWriterClassFactory;

import lombok.extern.slf4j.Slf4j;
import net.sf.ehcache.Ehcache;

@Slf4j
public class CacheWriterClassFactoryImpl implements CacheWriterClassFactory {
    private final PlasticProxyFactory classFactory;
    private final Map<String, CacheWriterBridges> bridgesToCacheName = new HashMap<>();

    public CacheWriterClassFactoryImpl(PlasticProxyFactory classFactory) {
        this.classFactory = classFactory;
    }

    @Override
    public <T> void add(Ehcache cache, final Class<T> serviceInterface, T instance, Method write, String methodKey) {
        ClassInstantiator<CacheWriterBridge> classInstantialor = classFactory.createProxy(CacheWriterBridge.class, plasticClass -> {
                final String fieldName = "instance";
                PlasticField field = plasticClass.introduceField(serviceInterface, fieldName);
                plasticClass.onConstruct(new ConstructorCallback() {
                    @Override
                    public void onConstruct(Object methodInstance, InstanceContext context) {
                        try {
                            Field reflectionField = methodInstance.getClass().getDeclaredField(fieldName);
                            reflectionField.setAccessible(true);
                            reflectionField.set(methodInstance, instance);
                        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                });
                plasticClass.introduceMethod(
                        PlasticUtils.getMethodDescription(CacheWriterBridge.class, "write", java.lang.Object.class, java.lang.Object.class),
                        new InstructionBuilderCallback() {
                    @Override
                    public void doBuild(InstructionBuilder builder) {
                        builder
                            .loadThis().getField(field)
                            .loadArgument(0).castOrUnbox(PlasticUtils.toTypeName(write.getParameterTypes()[0]))
                            .loadArgument(1).castOrUnbox(PlasticUtils.toTypeName(write.getParameterTypes()[1]))
                            .invoke(write);
                        builder.returnResult();
                    }
                });
            });
        try {
            CacheWriterBridge bridge = classInstantialor.newInstance();
            putBridge(cache.getName(), methodKey, bridge);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CacheWriterBridges getCacheWriterBridges(Ehcache cache) {
        return bridgesToCacheName.get(cache.getName());
    }

    private void putBridge(String cacheName, String methodKey, CacheWriterBridge bridge) {
        CacheWriterBridges bridges = bridgesToCacheName.get(cacheName);

        if(bridges == null) {
            bridges = new CacheWriterBridges();
            bridgesToCacheName.put(cacheName, bridges);
        }

        bridges.put(methodKey, bridge);
    }
}
