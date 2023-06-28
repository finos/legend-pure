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

package org.finos.legend.pure.m2.dsl.mapping;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EmbeddedSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.EnumerationMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.MappingInclude;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.SetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.external.store.model.PureInstanceSetImplementation;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class Mapping
{
    private Mapping()
    {
        // Utility class
    }

    public static SetImplementation getClassMappingById(org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping, String id, ProcessorSupport processorSupport)
    {
        return (SetImplementation)getMappingEntityById(mapping, id, M2MappingProperties.classMappings, M3Properties.id, processorSupport);
    }

    public static EnumerationMapping<?> getEnumerationMappingByName(org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping, String name, ProcessorSupport processorSupport)
    {
        return (EnumerationMapping<?>)getMappingEntityById(mapping, name, M2MappingProperties.enumerationMappings, M3Properties.name, processorSupport);
    }

    public static MapIterable<String,SetImplementation> getClassMappingsById(org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping, ProcessorSupport processorSupport)
    {
        return (MapIterable<String, SetImplementation>)getMappingEntitiesById(mapping, "class", M2MappingProperties.classMappings, M3Properties.id, processorSupport);
    }

    public static MapIterable<String, SetImplementation> getClassMappingsByIdIncludeEmbedded(org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping, ProcessorSupport processorSupport)
    {
        MutableMap<String, SetImplementation> classMappingsById = (MutableMap<String, SetImplementation>)getMappingEntitiesById(mapping, "class", M2MappingProperties.classMappings, M3Properties.id, processorSupport);
        MutableMap<String, SetImplementation> embeddedClassMappingIds = getEmbeddedMappingsByIdForClassMapping(classMappingsById.valuesView(), processorSupport);
        embeddedClassMappingIds.putAll(classMappingsById);
        return embeddedClassMappingIds;
    }

    private static MutableMap<String, SetImplementation> getEmbeddedMappingsByIdForClassMapping(RichIterable<SetImplementation> classMappings, ProcessorSupport processorSupport)
    {
        MutableMap<String, SetImplementation> embeddedMappingsById = Maps.mutable.of();
        for (SetImplementation classMapping: classMappings)
        {
            getEmbeddedMappingsByIdForPropertiesMapping(classMapping, processorSupport, embeddedMappingsById);
        }
        return embeddedMappingsById;
    }

    private static MutableMap<String, SetImplementation> getEmbeddedMappingsByIdForPropertiesMapping(CoreInstance propertyMappingOwner, ProcessorSupport processorSupport, MutableMap<String, SetImplementation> embeddedMappingsById)
    {
        RichIterable<? extends PropertyMapping> propertyMappings = propertyMappingOwner instanceof PureInstanceSetImplementation ? ((PureInstanceSetImplementation)propertyMappingOwner)._propertyMappings() : Lists.fixedSize.<PropertyMapping>empty();

        for (PropertyMapping propertyMapping : propertyMappings)
        {
            if (propertyMapping instanceof EmbeddedSetImplementation)
            {
                embeddedMappingsById.put(((EmbeddedSetImplementation)propertyMapping)._id(), (EmbeddedSetImplementation)propertyMapping);
                getEmbeddedMappingsByIdForPropertiesMapping(propertyMapping, processorSupport, embeddedMappingsById);
            }
        }
        return embeddedMappingsById;
    }

    public static MapIterable<String, EnumerationMapping> getEnumerationMappingsByName(org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping, ProcessorSupport processorSupport)
    {
        return (MapIterable<String, EnumerationMapping>)getMappingEntitiesById(mapping, "enumeration", M2MappingProperties.enumerationMappings, M3Properties.name, processorSupport);
    }

    private static CoreInstance getMappingEntityById(org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping, String id, String entityProperty, String idProperty, ProcessorSupport processorSupport)
    {
        return getMappingEntityById(mapping, id, entityProperty, idProperty, Sets.mutable.<CoreInstance>empty(), processorSupport);
    }

    private static CoreInstance getMappingEntityById(org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping, String id, String entityProperty, String idProperty, MutableSet<CoreInstance> visited, ProcessorSupport processorSupport)
    {
        if (visited.add(mapping))
        {
            CoreInstance entity = mapping.getValueInValueForMetaPropertyToManyWithKey(entityProperty, idProperty, id);
            if (entity != null)
            {
                return entity;
            }
            for (MappingInclude include : mapping._includes())
            {
                org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping includedMapping = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping)ImportStub.withImportStubByPass(include._includedCoreInstance(), processorSupport);
                entity = getMappingEntityById(includedMapping, id, entityProperty, idProperty, visited, processorSupport);
                if (entity != null)
                {
                    return entity;
                }
            }
        }
        return null;
    }

    private static MutableMap<String, ? extends CoreInstance> getMappingEntitiesById(org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping mapping, String entityType, String entityProperty, String idProperty, ProcessorSupport processorSupport)
    {
        MutableMap<String, CoreInstance> map = Maps.mutable.empty();
        collectMappingEntitiesById(map, mapping, mapping, entityType, entityProperty, idProperty, Sets.mutable.<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping>empty(), processorSupport);
        return map;
    }

    private static void collectMappingEntitiesById(MutableMap<String, CoreInstance> map, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping currentMapping, org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping rootMapping, String entityType, String entityProperty, String idProperty, MutableSet<org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping> visited, ProcessorSupport processorSupport)
    {
        if (visited.add(currentMapping))
        {
            for (CoreInstance entity : entityProperty.equals(M2MappingProperties.classMappings) ? currentMapping._classMappings() : currentMapping._enumerationMappings())
            {
                String id = entity instanceof SetImplementation ? ((SetImplementation)entity)._id() : ((EnumerationMapping)entity)._name();
                if (id == null)
                {
                    StringBuilder message = new StringBuilder(entityType);
                    message.append(" mapping without an id in mapping ");
                    PackageableElement.writeUserPathForPackageableElement(message, currentMapping);
                    SourceInformation sourceInfo = entity.getSourceInformation();
                    if (sourceInfo == null)
                    {
                        sourceInfo = currentMapping.getSourceInformation();
                    }
                    throw new PureCompilationException(sourceInfo, message.toString());
                }
                CoreInstance old = map.put(id, entity);
                if (old != null)
                {
                    StringBuilder message = new StringBuilder("Multiple ");
                    message.append(entityType);
                    message.append(" mappings with ");
                    message.append(idProperty);
                    message.append(" '");
                    message.append(id);
                    message.append("' for mapping ");
                    PackageableElement.writeUserPathForPackageableElement(message, rootMapping);
                    throw new PureCompilationException(rootMapping.getSourceInformation(), message.toString());
                }
            }
            for (MappingInclude include : currentMapping._includes())
            {
                org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping includedMapping = (org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping)ImportStub.withImportStubByPass(include._includedCoreInstance(), processorSupport);
                collectMappingEntitiesById(map, includedMapping, rootMapping, entityType, entityProperty, idProperty, visited, processorSupport);
            }
        }
    }
}
