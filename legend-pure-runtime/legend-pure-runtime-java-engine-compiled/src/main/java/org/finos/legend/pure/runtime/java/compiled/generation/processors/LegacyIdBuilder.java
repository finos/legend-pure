// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.generation.processors;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.measure.Measure;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;

import java.util.Objects;
import java.util.function.Function;

@Deprecated
class LegacyIdBuilder extends IdBuilder
{
    private final String defaultIdPrefix;
    private final ProcessorSupport processorSupport;
    private final ImmutableMap<CoreInstance, Function<? super CoreInstance, String>> idBuilders;
    private final ConcurrentMutableMap<CoreInstance, Function<? super CoreInstance, String>> cache;

    LegacyIdBuilder(String defaultIdPrefix, ProcessorSupport processorSupport)
    {
        this.defaultIdPrefix = defaultIdPrefix;
        this.processorSupport = processorSupport;
        this.idBuilders = buildIdBuildersMap(processorSupport);
        this.cache = ConcurrentHashMap.newMap(this.idBuilders.size());
        this.idBuilders.forEachKeyValue(this.cache::put);
    }

    public String buildId(CoreInstance instance)
    {
        Function<? super CoreInstance, String> function = findBuilderFunction(this.processorSupport.getClassifier(instance));
        return (function == null) ? buildDefaultId(instance) : applyBuilderFunction(function, instance);
    }

    private Function<? super CoreInstance, String> findBuilderFunction(CoreInstance classifier)
    {
        return this.cache.getIfAbsentPutWithKey(classifier, this::computeBuilderFunction);
    }

    private Function<? super CoreInstance, String> computeBuilderFunction(CoreInstance classifier)
    {
        for (CoreInstance genl : Type.getGeneralizationResolutionOrder(classifier, this.processorSupport))
        {
            Function<? super CoreInstance, String> function = this.idBuilders.get(genl);
            if (function != null)
            {
                return function;
            }
        }
        return null;
    }

    private String applyBuilderFunction(Function<? super CoreInstance, String> function, CoreInstance instance)
    {
        String id = function.apply(instance);
        return (id == null) ? buildDefaultId(instance) : id;
    }

    private String buildDefaultId(CoreInstance instance)
    {
        int syntheticId = instance.getSyntheticId();
        return (this.defaultIdPrefix == null) ? Integer.toUnsignedString(syntheticId, 32) : (this.defaultIdPrefix + Integer.toUnsignedString(syntheticId, 32));
    }

    // QualifiedProperty

    private static String buildIdForQualifiedProperty(CoreInstance property)
    {
        return PackageableElement.writeUserPathForPackageableElement(new StringBuilder(64), property.getValueForMetaPropertyToOne(M3Properties.owner))
                .append('.').append(property.getName())
                .toString();
    }

    // Property

    private static String buildIdForProperty(CoreInstance property)
    {
        CoreInstance owner = property.getValueForMetaPropertyToOne(M3Properties.owner);
        String propertyProperty;
        int index = owner.getValueForMetaPropertyToMany(M3Properties.properties).indexOf(property);
        if (index == -1)
        {
            index = owner.getValueForMetaPropertyToMany(M3Properties.originalMilestonedProperties).indexOf(property);
            if (index == -1)
            {
                StringBuilder builder = new StringBuilder("Error generating id for property '").append(property.getName()).append("' owned by ");
                PackageableElement.writeUserPathForPackageableElement(builder, owner);
                builder.append(": could not find it in either '").append(M3Properties.properties).append("' or '").append(M3Properties.originalMilestonedProperties).append("'");
                throw new IllegalStateException(builder.toString());
            }
            propertyProperty = M3Properties.originalMilestonedProperties;
        }
        else
        {
            propertyProperty = M3Properties.properties;
        }

        StringBuilder builder = PackageableElement.writeUserPathForPackageableElement(new StringBuilder(64), owner);
        builder.append('.').append(propertyProperty);
        builder.append('.').append(property.getName());
        if (owner instanceof Association)
        {
            // associations can have multiple properties with the same name
            builder.append('_').append(index);
        }
        return builder.toString();
    }

    // LambdaFunction

    private static String buildIdForLambdaFunction(CoreInstance lambda)
    {
        String name = lambda.getName();
        return ModelRepository.isAnonymousInstanceName(name) ? null : name;
    }

    // Annotation

    private static String buildIdForAnnotation(CoreInstance annotation)
    {
        return PackageableElement.writeUserPathForPackageableElement(new StringBuilder(64), annotation.getValueForMetaPropertyToOne(M3Properties.profile))
                .append('.').append(annotation.getName())
                .toString();
    }

    // Unit

    private static String buildIdForUnit(CoreInstance unit)
    {
        return Measure.getUserPathForUnit(unit);
    }

    // PackageableElement

    private static String buildIdForPackageableElement(CoreInstance instance)
    {
        String id = PackageableElement.getUserPathForPackageableElement(instance);
        if (ModelRepository.isAnonymousInstanceName(id) && (id.indexOf(':') == -1))
        {
            // don't return anonymous ids
            return null;
        }
        return id;
    }

    private static ImmutableMap<CoreInstance, Function<? super CoreInstance, String>> buildIdBuildersMap(ProcessorSupport processorSupport)
    {
        MutableMap<CoreInstance, Function<? super CoreInstance, String>> idBuilders = Maps.mutable.empty();
        PrimitiveUtilities.getPrimitiveTypes(processorSupport).forEach(t -> idBuilders.put(t, CoreInstance::getName));
        addIdBuilder(idBuilders, processorSupport, M3Paths.Annotation, LegacyIdBuilder::buildIdForAnnotation);
        addIdBuilder(idBuilders, processorSupport, M3Paths.Enum, CoreInstance::getName);
        addIdBuilder(idBuilders, processorSupport, M3Paths.LambdaFunction, LegacyIdBuilder::buildIdForLambdaFunction);
        addIdBuilder(idBuilders, processorSupport, M3Paths.PackageableElement, LegacyIdBuilder::buildIdForPackageableElement);
        addIdBuilder(idBuilders, processorSupport, M3Paths.Property, LegacyIdBuilder::buildIdForProperty);
        addIdBuilder(idBuilders, processorSupport, M3Paths.QualifiedProperty, LegacyIdBuilder::buildIdForQualifiedProperty);
        addIdBuilder(idBuilders, processorSupport, M3Paths.Unit, LegacyIdBuilder::buildIdForUnit);
        CompiledExtensionLoader.extensions().flatCollect(x -> x.getExtraIdBuilders(processorSupport)).forEach(x -> addIdBuilder(idBuilders, processorSupport, x.getOne(), x.getTwo()));
        return idBuilders.toImmutable();
    }

    private static void addIdBuilder(MutableMap<CoreInstance, Function<? super CoreInstance, String>> idBuilders, ProcessorSupport processorSupport, String classifierPath, Function<? super CoreInstance, String> function)
    {
        CoreInstance type = processorSupport.package_getByUserPath(Objects.requireNonNull(classifierPath, "classifier path may not be null"));
        Function<? super CoreInstance, String> old = idBuilders.put(type, Objects.requireNonNull(function, "function may not be null"));
        if (old != null)
        {
            throw new RuntimeException("An id builder function is already registered for classifier: " + classifierPath);
        }
    }
}
