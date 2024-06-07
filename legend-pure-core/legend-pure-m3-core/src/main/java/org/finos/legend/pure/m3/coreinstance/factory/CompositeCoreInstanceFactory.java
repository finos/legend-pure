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

package org.finos.legend.pure.m3.coreinstance.factory;

import org.finos.legend.pure.m3.coreinstance.BaseM3CoreInstanceFactory;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.EnumInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.factory.CoreInstanceFactory;
import org.finos.legend.pure.m4.coreinstance.factory.MultipassCoreInstanceFactory;
import org.finos.legend.pure.m4.coreinstance.simple.SimpleCoreInstanceFactory;

public class CompositeCoreInstanceFactory extends BaseM3CoreInstanceFactory implements MultipassCoreInstanceFactory
{
    public static final String IS_ENUM_TYPE_INFO = "Enum";
    private static final CoreInstanceFactory DEFAULT_CORE_INSTANCE_FACTORY = new SimpleCoreInstanceFactory();
    private final CoreInstanceFactoryRegistry registry;

    public CompositeCoreInstanceFactory(CoreInstanceFactoryRegistry registry)
    {
        this.registry = registry;
    }

    @Override
    public boolean supports(CoreInstance classifier)
    {
        return true;
    }

    @Override
    public CoreInstance createCoreInstance(String name, int internalSyntheticId, SourceInformation sourceInformation, CoreInstance classifier, ModelRepository repository, boolean persistent)
    {
        CoreInstanceFactory factory = this.registry.getFactoryForPath(this.getClassifierPath(classifier));
        if (factory == null)
        {
            factory = classifier instanceof Enumeration ? EnumInstance.FACTORY : DEFAULT_CORE_INSTANCE_FACTORY;
        }
        return factory.createCoreInstance(name, internalSyntheticId, sourceInformation, classifier, repository, persistent);
    }

    @Override
    public boolean supports(String classifierPath)
    {
        return true;
    }

    @Override
    public CoreInstance createCoreInstance(String name, int internalSyntheticId, SourceInformation sourceInformation, String classifierPath, String typeInfo, ModelRepository repository, boolean persistent)
    {
        CoreInstanceFactory factory = this.registry.getFactoryForPath(classifierPath);
        if (factory == null)
        {
            factory = IS_ENUM_TYPE_INFO.equals(typeInfo) ? EnumInstance.FACTORY : DEFAULT_CORE_INSTANCE_FACTORY;
        }
        //Classifier is set as a second step
        return factory.createCoreInstance(name, internalSyntheticId, sourceInformation, null, repository, persistent);
    }

    @Override
    public boolean supports(int classifierSyntheticId)
    {
        return true;
    }

    @Override
    public CoreInstance createCoreInstance(String name, int internalSyntheticId, SourceInformation sourceInformation, int classifierSyntheticId, ModelRepository repository, boolean persistent)
    {
        CoreInstanceFactory factory = this.registry.getFactoryForId(classifierSyntheticId);
        if (factory == null)
        {
            factory = DEFAULT_CORE_INSTANCE_FACTORY;
        }
        //Classifier is set as a second step
        return factory.createCoreInstance(name, internalSyntheticId, sourceInformation, null, repository, persistent);
    }
}
