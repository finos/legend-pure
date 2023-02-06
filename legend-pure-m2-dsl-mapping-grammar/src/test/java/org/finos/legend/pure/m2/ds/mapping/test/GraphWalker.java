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

package org.finos.legend.pure.m2.ds.mapping.test;


import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingProperties;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class GraphWalker
{
    protected final PureRuntime runtime;
    private final ProcessorSupport processorSupport;

    public GraphWalker(PureRuntime runtime, ProcessorSupport processorSupport)
    {
        this.runtime = runtime;
        this.processorSupport = processorSupport;
    }

    public CoreInstance getOne(CoreInstance join, String property)
    {
        return Instance.getValueForMetaPropertyToOneResolved(join, property, this.processorSupport);
    }

    public ListIterable<? extends CoreInstance> getMany(CoreInstance join, String property)
    {
        return Instance.getValueForMetaPropertyToManyResolved(join, property, this.processorSupport);
    }

    public String getName(CoreInstance coreInstance)
    {
        return this.getOne(coreInstance, M3Properties.name).getName();
    }


    public String getClassifier(CoreInstance coreInstance)
    {
        return this.getName(coreInstance.getClassifier());
    }

    public ListIterable<? extends CoreInstance> getJoins(CoreInstance db)
    {
        return this.getMany(db, "joins");
    }

    public CoreInstance getMapping(String name)
    {
        return this.runtime.getCoreInstance(name);
    }

    public ListIterable<? extends CoreInstance> getClassMappings(CoreInstance mapping)
    {
        return this.getMany(mapping, M2MappingProperties.classMappings);
    }

    public ListIterable<? extends CoreInstance> getAssociationMappings(CoreInstance mapping)
    {
        return this.getMany(mapping, M2MappingProperties.associationMappings);
    }

    public CoreInstance getClassMapping(CoreInstance mapping, String clazzName)
    {
        final ListIterable<? extends CoreInstance> classMappings = this.getClassMappings(mapping);
        for (CoreInstance classMapping : classMappings)
        {
            final CoreInstance mappingClass = this.getOne(classMapping, M3Properties._class);
            if (this.isNamePropertyValueEqual(mappingClass, clazzName))
            {
                return classMapping;
            }
        }
        return null;
    }

    public CoreInstance getClassMappingById(CoreInstance mapping, String id)
    {
        final ListIterable<? extends CoreInstance> classMappings = this.getClassMappings(mapping);
        for (CoreInstance classMapping : classMappings)
        {
            final CoreInstance idInstance = this.getOne(classMapping, M3Properties.id);
            if (idInstance.getName().equals(id))
            {
                return classMapping;
            }
        }
        return null;
    }

    public ListIterable<? extends CoreInstance> getClassMappingImplementationPropertyMappings(CoreInstance classMappingImpl)
    {
        return this.getMany(classMappingImpl, M2MappingProperties.propertyMappings);
    }

    public ListIterable<? extends CoreInstance> getClassMappingImplementationOtherwisePropertyMapping(CoreInstance classMappingImpl)
    {
        return this.getMany(classMappingImpl, M2MappingProperties.otherwisePropertyMapping);
    }

    public CoreInstance getClassMappingImplementationPropertyMapping(CoreInstance classMappingImpl, String propertyName)
    {
        final ListIterable<? extends CoreInstance> classMappingImplPropertyMappings = this.getClassMappingImplementationPropertyMappings(classMappingImpl);

        for (CoreInstance classMapping : classMappingImplPropertyMappings)
        {
            final CoreInstance mappingClass = this.getOne(classMapping, "property");
            if (this.isNamePropertyValueEqual(mappingClass, propertyName))
            {
                return classMapping;
            }
        }
        return null;
    }


    private boolean isNamePropertyValueEqual(CoreInstance each, String columnName)
    {
        return this.comparePropertyValue(each, M3Properties.name, columnName);
    }

    private boolean comparePropertyValue(CoreInstance each, String property, String columnName)
    {
        return this.getOne(each, property).getName().equals(columnName);
    }

    protected Predicate2<CoreInstance, String> CoreInstanceNamePropertyValuePredicate = new Predicate2<CoreInstance, String>()
    {
        @Override
        public boolean accept(CoreInstance coreInstance, String value)
        {
            return  GraphWalker.this.isNamePropertyValueEqual(coreInstance, value);
        }
    };
}

