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

package org.finos.legend.pure.m3.serialization.runtime.binary.reference;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;

import java.math.BigInteger;

public class CachedReferenceFactory implements ReferenceFactory
{
    private final Function<String, Reference> newStringReference = new Function<String, Reference>()
    {
        @Override
        public Reference valueOf(String name)
        {
            return CachedReferenceFactory.this.delegate.stringReference(name);
        }
    };

    private final Function<Integer, Reference> newIntIntegerReference = new Function<Integer, Reference>()
    {
        @Override
        public Reference valueOf(Integer value)
        {
            return CachedReferenceFactory.this.delegate.integerReference(value);
        }
    };

    private final Function<String, Reference> newPackageReference = new Function<String, Reference>()
    {
        @Override
        public Reference valueOf(String path)
        {
            return CachedReferenceFactory.this.delegate.packageReference(path);
        }
    };

    private final Function<String, Reference> newPackagedElementReference = new Function<String, Reference>()
    {
        @Override
        public Reference valueOf(String path)
        {
            return CachedReferenceFactory.this.delegate.packagedElementReference(path);
        }
    };

    private final ConcurrentMutableMap<String, Reference> stringReferenceCache = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<Integer, Reference> intReferenceCache = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<String, Reference> packageReferenceCache = ConcurrentHashMap.newMap();
    private final ConcurrentMutableMap<String, Reference> packagedElementReferenceCache = ConcurrentHashMap.newMap();
    private final ReferenceFactory delegate;
    private final Reference trueReference;
    private final Reference falseReference;
    private final Reference latestDateReference;

    private CachedReferenceFactory(ReferenceFactory delegate)
    {
        this.delegate = delegate;
        this.trueReference = this.delegate.booleanReference(true);
        this.falseReference = this.delegate.booleanReference(false);
        this.latestDateReference = this.delegate.latestDateReference();
    }

    @Override
    public Reference booleanReference(boolean value)
    {
        return value ? this.trueReference : this.falseReference;
    }

    @Override
    public Reference booleanReference(String name)
    {
        return this.delegate.booleanReference(name);
    }

    @Override
    public Reference integerReference(int value)
    {
        return this.intReferenceCache.getIfAbsentPutWithKey(value, this.newIntIntegerReference);
    }

    @Override
    public Reference integerReference(long value)
    {
        return this.delegate.integerReference(value);
    }

    @Override
    public Reference integerReference(BigInteger value)
    {
        return this.delegate.integerReference(value);
    }

    @Override
    public Reference integerReference(String name)
    {
        return this.delegate.integerReference(name);
    }

    @Override
    public Reference floatReference(String name)
    {
        return this.delegate.floatReference(name);
    }

    @Override
    public Reference decimalReference(String name)
    {
        return this.delegate.decimalReference(name);
    }

    @Override
    public Reference dateReference(String name)
    {
        return this.delegate.dateReference(name);
    }

    @Override
    public Reference dateTimeReference(String name)
    {
        return this.delegate.dateTimeReference(name);
    }

    @Override
    public Reference strictDateReference(String name)
    {
        return this.delegate.strictDateReference(name);
    }

    @Override
    public Reference latestDateReference()
    {
        return this.latestDateReference;
    }

    @Override
    public Reference stringReference(String name)
    {
        return this.stringReferenceCache.getIfAbsentPutWithKey(name, this.newStringReference);
    }

    @Override
    public Reference packageReference(String path)
    {
        return this.packageReferenceCache.getIfAbsentPutWithKey(path, this.newPackageReference);
    }

    @Override
    public Reference packagedElementReference(String path)
    {
        return this.packagedElementReferenceCache.getIfAbsentPutWithKey(path, this.newPackagedElementReference);
    }

    public static CachedReferenceFactory wrap(ReferenceFactory factory)
    {
        return new CachedReferenceFactory(factory);
    }
}
