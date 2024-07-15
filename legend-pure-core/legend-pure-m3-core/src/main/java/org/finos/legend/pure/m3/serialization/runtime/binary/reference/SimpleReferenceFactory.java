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

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.math.BigInteger;

public class SimpleReferenceFactory implements ReferenceFactory
{
    private final BooleanReference trueReference = new BooleanReference(true);
    private final BooleanReference falseReference = new BooleanReference(false);

    @Override
    public Reference booleanReference(boolean value)
    {
        return value ? this.trueReference : this.falseReference;
    }

    @Override
    public Reference booleanReference(String name)
    {
        switch (name)
        {
            case ModelRepository.BOOLEAN_TRUE:
            {
                return this.trueReference;
            }
            case ModelRepository.BOOLEAN_FALSE:
            {
                return this.falseReference;
            }
            default:
            {
                throw new IllegalArgumentException("Unknown Boolean name: '" + name + "'");
            }
        }
    }

    @Override
    public Reference integerReference(int value)
    {
        return new IntIntegerReference(value);
    }

    @Override
    public Reference integerReference(long value)
    {
        return new LongIntegerReference(value);
    }

    @Override
    public Reference integerReference(BigInteger value)
    {
        return new BigIntegerReference(value);
    }

    @Override
    public Reference integerReference(String name)
    {
        return new IntegerByNameReference(name);
    }

    @Override
    public Reference floatReference(String name)
    {
        return new FloatReference(name);
    }

    @Override
    public Reference decimalReference(String name)
    {
        return new DecimalReference(name);
    }

    @Override
    public Reference dateReference(String name)
    {
        return new DateReference(name);
    }

    @Override
    public Reference dateTimeReference(String name)
    {
        return new DateTimeReference(name);
    }

    @Override
    public Reference strictDateReference(String name)
    {
        return new StrictDateReference(name);
    }

    @Override
    public Reference latestDateReference()
    {
        return new LatestDateReference();
    }

    @Override
    public Reference stringReference(String name)
    {
        return new StringReference(name);
    }

    @Override
    public Reference packageReference(String path)
    {
        return new PackageReference(path);
    }

    @Override
    public Reference packagedElementReference(String path)
    {
        return new PackagedElementReference(path);
    }

    private static class BooleanReference extends AbstractReference
    {
        private final boolean value;

        private BooleanReference(boolean value)
        {
            this.value = value;
        }

        @Override
        protected CoreInstance tryResolve(ModelRepository repository, ProcessorSupport processorSupport) throws UnresolvableReferenceException
        {
            return repository.newBooleanCoreInstance(this.value);
        }
    }

    private static class DateReference extends AbstractReference
    {
        private final String name;

        private DateReference(String name)
        {
            this.name = name;
        }

        @Override
        protected CoreInstance tryResolve(ModelRepository repository, ProcessorSupport processorSupport) throws UnresolvableReferenceException
        {
            return repository.newDateCoreInstance(this.name);
        }
    }

    private static class StrictDateReference extends AbstractReference
    {
        private final String name;

        private StrictDateReference(String name)
        {
            this.name = name;
        }

        @Override
        protected CoreInstance tryResolve(ModelRepository repository, ProcessorSupport processorSupport) throws UnresolvableReferenceException
        {
            return repository.newStrictDateCoreInstance(this.name);
        }
    }

    private static class DateTimeReference extends AbstractReference
    {
        private final String name;

        private DateTimeReference(String name)
        {
            this.name = name;
        }

        @Override
        protected CoreInstance tryResolve(ModelRepository repository, ProcessorSupport processorSupport) throws UnresolvableReferenceException
        {
            return repository.newDateTimeCoreInstance(this.name);
        }
    }

    private static class LatestDateReference extends AbstractReference
    {

        private LatestDateReference()
        {
        }

        @Override
        protected CoreInstance tryResolve(ModelRepository repository, ProcessorSupport processorSupport) throws UnresolvableReferenceException
        {
            return repository.newLatestDateCoreInstance();
        }
    }

    private static class FloatReference extends AbstractReference
    {
        private final String name;

        private FloatReference(String name)
        {
            this.name = name;
        }

        @Override
        protected CoreInstance tryResolve(ModelRepository repository, ProcessorSupport processorSupport) throws UnresolvableReferenceException
        {
            return repository.newFloatCoreInstance(this.name);
        }
    }

    private static class DecimalReference extends AbstractReference
    {
        private final String name;

        private DecimalReference(String name)
        {
            this.name = name;
        }

        @Override
        protected CoreInstance tryResolve(ModelRepository repository, ProcessorSupport processorSupport) throws UnresolvableReferenceException
        {
            return repository.newDecimalCoreInstance(this.name);
        }
    }

    private static class IntIntegerReference extends AbstractReference
    {
        private final int value;

        private IntIntegerReference(int value)
        {
            this.value = value;
        }

        @Override
        protected CoreInstance tryResolve(ModelRepository repository, ProcessorSupport processorSupport) throws UnresolvableReferenceException
        {
            return repository.newIntegerCoreInstance(this.value);
        }
    }

    private static class LongIntegerReference extends AbstractReference
    {
        private final long value;

        private LongIntegerReference(long value)
        {
            this.value = value;
        }

        @Override
        protected CoreInstance tryResolve(ModelRepository repository, ProcessorSupport processorSupport) throws UnresolvableReferenceException
        {
            return repository.newIntegerCoreInstance(this.value);
        }
    }

    private static class BigIntegerReference extends AbstractReference
    {
        private final BigInteger value;

        private BigIntegerReference(BigInteger value)
        {
            this.value = value;
        }

        @Override
        protected CoreInstance tryResolve(ModelRepository repository, ProcessorSupport processorSupport) throws UnresolvableReferenceException
        {
            return repository.newIntegerCoreInstance(this.value);
        }
    }

    private static class IntegerByNameReference extends AbstractReference
    {
        private final String name;

        private IntegerByNameReference(String name)
        {
            this.name = name;
        }

        @Override
        protected CoreInstance tryResolve(ModelRepository repository, ProcessorSupport processorSupport) throws UnresolvableReferenceException
        {
            return repository.newIntegerCoreInstance(this.name);
        }
    }

    private static class StringReference extends AbstractReference
    {
        private final String value;

        private StringReference(String value)
        {
            this.value = value;
        }

        @Override
        protected CoreInstance tryResolve(ModelRepository repository, ProcessorSupport processorSupport) throws UnresolvableReferenceException
        {
            return repository.newStringCoreInstance_cached(this.value);
        }
    }

    private static class PackagedElementReference extends AbstractReference
    {
        private final String path;

        private PackagedElementReference(String path)
        {
            this.path = path;
        }

        @Override
        protected CoreInstance tryResolve(ModelRepository repository, ProcessorSupport processorSupport) throws UnresolvableReferenceException
        {
            CoreInstance instance = processorSupport.package_getByUserPath(this.path);
            if (instance == null)
            {
                throw new UnresolvableReferenceException("Could not find " + this.path);
            }
            return instance;
        }
    }

    private static class PackageReference extends AbstractReference
    {
        private final String path;

        private PackageReference(String path)
        {
            this.path = path;
        }

        @Override
        protected CoreInstance tryResolve(ModelRepository repository, ProcessorSupport processorSupport) throws UnresolvableReferenceException
        {
            // TODO should we create Root if it doesn't exist?
            try
            {
                return findOrCreatePackage(repository);
            }
            catch (Exception e)
            {
                throw new UnresolvableReferenceException("Cannot resolve package: " + this.path, e);
            }
        }

        private CoreInstance findOrCreatePackage(ModelRepository repository)
        {
            ListIterable<String> path = PackageableElement.splitUserPath(this.path);

            CoreInstance parent = repository.getTopLevel(M3Paths.Root);
            if (parent == null)
            {
                throw new RuntimeException("Cannot find Root in model repository");
            }

            if ((path.size() == 1) && M3Paths.Root.equals(path.get(0)))
            {
                return parent;
            }

            CoreInstance packageClass = null;
            for (String name : path)
            {
                synchronized (parent)
                {
                    CoreInstance child = _Package.findInPackage(parent, name);
                    if (child == null)
                    {
                        if (packageClass == null)
                        {
                            packageClass = repository.getTopLevel(M3Paths.Package);
                            if (packageClass == null)
                            {
                                throw new RuntimeException("Cannot find class " + M3Paths.Package);
                            }
                        }
                        child = repository.newCoreInstanceMultiPass(name, M3Paths.Package, null);
                        child.setClassifier(packageClass);
                        child.addKeyValue(M3PropertyPaths._package, parent);
                        child.setKeyValues(M3PropertyPaths.children, Lists.immutable.<CoreInstance>empty());
                        child.addKeyValue(M3PropertyPaths.name, repository.newStringCoreInstance_cached(name));
                        if (parent.hasBeenValidated())
                        {
                            child.markValidated();
                        }
                        parent.addKeyValue(M3PropertyPaths.children, child);
                    }
                    parent = child;
                }
            }
            return parent;
        }
    }
}
