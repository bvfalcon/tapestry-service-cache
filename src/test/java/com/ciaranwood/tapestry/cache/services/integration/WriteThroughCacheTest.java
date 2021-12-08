package com.ciaranwood.tapestry.cache.services.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Random;

import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.junit.Before;
import org.junit.Test;

import com.ciaranwood.tapestry.cache.services.AlternateCacheKeyIndex;
import com.ciaranwood.tapestry.cache.services.CacheFactory;
import com.ciaranwood.tapestry.cache.services.CacheLocator;
import com.ciaranwood.tapestry.cache.services.IntegerStore;
import com.ciaranwood.tapestry.cache.services.MissingCacheResultForWriteThrough;
import com.ciaranwood.tapestry.cache.services.MultipleDao;
import com.ciaranwood.tapestry.cache.services.ServiceCacheModule;
import com.ciaranwood.tapestry.cache.services.StringDao;
import com.ciaranwood.tapestry.cache.services.StringStore;
import com.ciaranwood.tapestry.cache.services.WriteThroughModule;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

public class WriteThroughCacheTest {

    private Registry registry;
    private static final Integer ID = -1;
    private static final String STRING_DATA = "Data";
    private static final Integer INTEGER_DATA = new Random().nextInt(Integer.MAX_VALUE);

    @Before
    public void startRegistry() {
        RegistryBuilder builder = new RegistryBuilder();
        builder.add(ServiceCacheModule.class, WriteThroughModule.class);
        registry = builder.build();
        registry.performRegistryStartup();
    }

    @Test
    public void testWriteThroughCache() {
        StringDao stringDao = registry.getService(StringDao.class);
        Ehcache cache = getCache("StringDao");
        StringStore store = registry.getService(StringStore.class);

        assertNull(cache.get(ID));

        stringDao.put(ID, STRING_DATA);

        Element element = cache.get(new CacheLocator("get", ID));
        assertNotNull(element);
        assertEquals(STRING_DATA, element.getObjectValue());
        assertEquals(STRING_DATA, store.get(ID));
        assertEquals(1, cache.getKeys().size());
    }

    @Test
    public void testHitCache() {
        StringDao stringDao = registry.getService(StringDao.class);
        Ehcache cache = getCache("StringDao");

        stringDao.put(ID, STRING_DATA);

        String result = stringDao.get(ID);

        assertEquals(STRING_DATA, result);
        assertEquals(1, cache.getStatistics().cacheHitCount());
    }

    @Test
    public void testMultipleWriteThroughCachesInOneService() {
        MultipleDao multipleDao = registry.getService(MultipleDao.class);
        Ehcache cache = getCache("MultipleDao");
        StringStore stringStore = registry.getService(StringStore.class);
        IntegerStore integerStore = registry.getService(IntegerStore.class);

        assertNull(cache.get(ID));

        multipleDao.putString(ID, STRING_DATA);
        multipleDao.putInteger(ID, INTEGER_DATA);

        Element stringElement = cache.get(new CacheLocator("StringStore", ID));
        assertNotNull(stringElement);
        assertEquals(STRING_DATA, stringElement.getObjectValue());
        assertEquals(STRING_DATA, stringStore.get(ID));

        Element integerElement = cache.get(new CacheLocator("IntegerStore", ID));
        assertNotNull(integerElement);
        assertEquals(INTEGER_DATA, integerElement.getObjectValue());
        assertEquals(INTEGER_DATA, integerStore.get(ID));

        assertEquals(2, cache.getKeys().size());
    }

    @Test
    public void testAlternateCacheKeyIndex() {
        AlternateCacheKeyIndex service = registry.getService(AlternateCacheKeyIndex.class);

        service.put(INTEGER_DATA, ID);

        Integer result = service.get(ID);

        assertEquals(INTEGER_DATA, result);

        Ehcache cache = getCache("AlternateCacheKeyIndex");
        Element element = cache.get(new CacheLocator("get", ID));

        assertEquals(INTEGER_DATA, element.getObjectValue());
    }

    @Test
    public void allowWriteThroughWithoutMatchingCacheResult() {
        MissingCacheResultForWriteThrough service = registry.getService(MissingCacheResultForWriteThrough.class);

        service.put(ID, STRING_DATA);

        Ehcache cache = getCache("MissingCacheResultForWriteThrough");
        Element element = cache.get(new CacheLocator("put", ID));

        assertEquals(STRING_DATA, element.getObjectValue());
    }


    private Ehcache getCache(String serviceId) {
        CacheFactory cacheFactory = registry.getService(CacheFactory.class);
        return cacheFactory.getCache(serviceId);
    }
}
