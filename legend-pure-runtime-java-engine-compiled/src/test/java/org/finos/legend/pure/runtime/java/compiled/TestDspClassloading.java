// Copyright 2020 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.pure.runtime.java.compiled;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.util.concurrent.TimeUnit;

public class TestDspClassloading
{
    public static final String PURE_MAP_CLASS = "org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureMap";
    public static final String PURE_CACHE_MAP_CLASS = "org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.PureCacheMap";
    public static final String JAVA_MAP_CLASS = "java.util.Map";

    @Test
    public void testDspClassloadingWithoutGSCollections() throws Exception
    {
        Class<?> pureMapClass = getClass().getClassLoader().loadClass(PURE_MAP_CLASS);
        Class<?> pureCacheMapClass = getClass().getClassLoader().loadClass(PURE_CACHE_MAP_CLASS);
        Class<?> gsCollectionsMutableMapClass = getClass().getClassLoader().loadClass(JAVA_MAP_CLASS);
        Constructor<?> pureCacheMapConstructor = pureCacheMapClass.getConstructor(int.class, long.class, TimeUnit.class);
        Constructor<?> pureMapConstructor = pureMapClass.getConstructor(gsCollectionsMutableMapClass);
        Object pureCacheMapInstance = pureCacheMapConstructor.newInstance(1, 2, TimeUnit.SECONDS);
        Object pureMapInstance = pureMapConstructor.newInstance(pureCacheMapInstance);
        Assert.assertNotNull(pureMapInstance);
    }
}
