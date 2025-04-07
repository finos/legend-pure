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

package org.finos.legend.pure.runtime.java.compiled.metadata;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ReflectiveCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.MetadataJavaPaths;

import java.util.Objects;

public final class MetadataEager implements Metadata
{
    private final MetamodelByClassifier metamodelByClassifier = new MetamodelByClassifier();

    private final ThreadLocal<MetamodelByClassifier> added = new ThreadLocal<>();

    @Override
    public void startTransaction()
    {
        this.added.set(new MetamodelByClassifier());
    }

    @Override
    public void commitTransaction()
    {
        MetamodelByClassifier trans = this.added.get();
        if (trans != null)
        {
            this.added.remove();
            this.metamodelByClassifier.commitChanges(trans);
        }
    }

    @Override
    public void rollbackTransaction()
    {
        this.added.remove();
    }

    public void clear()
    {
        this.metamodelByClassifier.clear();
        this.added.remove();
    }

    public void addChild(String packageClassifier, String packageId, String objectClassifier, String instanceId)
    {
        if (this.isInTransaction())
        {
            if (this.added.get().getMetadata(packageClassifier, packageId) == null)
            {
                CoreInstance packageInstance = Objects.requireNonNull(this.metamodelByClassifier.getMetadata(packageClassifier, packageId));
                CoreInstance copy = ((ReflectiveCoreInstance) packageInstance).copy();

                this.added.get().add(packageClassifier, packageId, copy);
            }
        }

        this.getMetamodel().addChild(packageClassifier, packageId, objectClassifier, instanceId);
    }

    @Override
    public CoreInstance getEnum(String enumerationName, String enumName)
    {
        return getMetadata(enumerationName, enumerationName + "." + M3Properties.values + "['" + enumName + "']");
    }

    @Deprecated
    public void invalidateCoreInstances(RichIterable<? extends CoreInstance> instances, ProcessorSupport processorSupport)
    {
        IdBuilder idBuilder = IdBuilder.newIdBuilder(processorSupport);
        for (CoreInstance coreInstance : instances)
        {
            String identifier = idBuilder.buildId(coreInstance);
            String classifier = MetadataJavaPaths.buildMetadataKeyFromType(coreInstance.getClassifier()).intern();

            CoreInstance pack = coreInstance.getValueForMetaPropertyToOne(M3Properties._package);
            if (pack != null)
            {
                String packIdentifier = idBuilder.buildId(pack);
                String packClassifier = MetadataJavaPaths.buildMetadataKeyFromType(pack.getClassifier()).intern();

                this.getMetamodel().remove(classifier, identifier, packClassifier, packIdentifier);
            }
        }
    }

    public void add(String classifier, String id, CoreInstance instance)
    {
        this.getMetamodel().add(classifier, id, instance);
    }


    @Override
    public CoreInstance getMetadata(String classifier, String id)
    {
        // for backward compatibility
        if (id.startsWith("Root::"))
        {
            id = id.substring(6);
        }
        CoreInstance coreInstance = this.metamodelByClassifier.getMetadata(classifier, id);
        if (coreInstance == null && this.isInTransaction())
        {
            coreInstance = this.added.get().getMetadata(classifier, id);
        }

        if (coreInstance == null)
        {
            throw new PureExecutionException("Element " + id + " of type " + classifier + " does not exist");
        }

        return coreInstance;
    }

    @Override
    public MapIterable<String, CoreInstance> getMetadata(String classifier)
    {
        MapIterable<String, CoreInstance> instances = this.metamodelByClassifier.getMetadata(classifier);
        if (isInTransaction())
        {
            MapIterable<String, CoreInstance> addedInstances = this.added.get().getMetadata(classifier);
            if (addedInstances.notEmpty())
            {
                MutableMap<String, CoreInstance> allInstances = Maps.mutable.ofInitialCapacity(instances.size());
                instances.forEachKeyValue(allInstances::put);
                addedInstances.forEachKeyValue(allInstances::put);
                return allInstances;
            }
        }
        return instances;
    }

    public int getSize()
    {
        return this.getMetamodel().metadata.valuesView().size();
    }

    private boolean isInTransaction()
    {
        return this.added.get() != null;
    }


    private MetamodelByClassifier getMetamodel()
    {
        return this.isInTransaction() ? this.added.get() : this.metamodelByClassifier;
    }

    private static class MetamodelByClassifier
    {
        private final MutableMap<String, MutableMap<String, CoreInstance>> metadata = Maps.mutable.empty();

        private void clear()
        {
            this.metadata.clear();
        }

        private void add(String classifier, String id, CoreInstance coreInstance)
        {
            this.metadata.getIfAbsentPut(classifier, Maps.mutable::empty).put(id, coreInstance);
        }

        private void commitChanges(MetamodelByClassifier toBeAdded)
        {
            toBeAdded.metadata.forEachKeyValue((classifier, instancesById) -> this.metadata.getIfAbsentPut(classifier, Maps.mutable::empty).putAll(instancesById));
        }

        private CoreInstance getMetadata(String classifier, String id)
        {
            MutableMap<String, CoreInstance> instancesForClassifier = this.metadata.get(classifier);
            return instancesForClassifier == null ? null : instancesForClassifier.get(id);
        }

        private MapIterable<String, CoreInstance> getMetadata(String classifier)
        {
            return this.metadata.getIfAbsentValue(classifier, Maps.fixedSize.empty());
        }

        private void addChild(String packageClassifier, String packageId, String objectClassifier, String instanceId)
        {
            Package _package = (Package) Objects.requireNonNull(getMetadata(packageClassifier, packageId));
            PackageableElement child = (PackageableElement) Objects.requireNonNull(getMetadata(objectClassifier, instanceId));
            _package._childrenAdd(child);
        }

        private void remove(String classifier, String identifier, String packClassifier, String packIdentifier)
        {
            MutableMap<String, CoreInstance> classifierMetaData = this.metadata.get(classifier);
            if (classifierMetaData != null)
            {
                CoreInstance o = classifierMetaData.remove(identifier);
                if (o instanceof PackageableElement)
                {
                    Package _package = (Package) Objects.requireNonNull(this.getMetadata(packClassifier, packIdentifier));
                    _package._childrenRemove((PackageableElement) o);
                }
            }
        }
    }
}
