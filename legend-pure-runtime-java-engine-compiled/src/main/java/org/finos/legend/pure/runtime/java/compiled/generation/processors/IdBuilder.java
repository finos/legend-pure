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

package org.finos.legend.pure.runtime.java.compiled.generation.processors;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorageTools;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.function.Function;

public class IdBuilder
{
    private final String defaultIdPrefix;
    private final ProcessorSupport processorSupport;
    private final MapIterable<CoreInstance, Function<? super CoreInstance, String>> idBuilders;

    private IdBuilder(String defaultIdPrefix, ProcessorSupport processorSupport)
    {
        this.defaultIdPrefix = defaultIdPrefix;
        this.processorSupport = processorSupport;
        this.idBuilders = registerIdBuilderFunctions(processorSupport);
    }

    public String buildId(CoreInstance instance)
    {
        Function<? super CoreInstance, String> function = findBuilderFunction(this.processorSupport.getClassifier(instance));
        return (function == null) ? buildDefaultId(instance) : applyBuilderFunction(function, instance);
    }

    public Function<CoreInstance, String> getIdBuilderForClassifier(CoreInstance classifier)
    {
        Function<? super CoreInstance, String> function = findBuilderFunction(classifier);
        return (function == null) ? this::buildDefaultId : i -> applyBuilderFunction(function, i);
    }

    private Function<? super CoreInstance, String> findBuilderFunction(CoreInstance classifier)
    {
        Function<? super CoreInstance, String> function = this.idBuilders.get(classifier);
        if (function != null)
        {
            return function;
        }

        ListIterable<CoreInstance> genls = Type.getGeneralizationResolutionOrder(classifier, this.processorSupport);
        // We can skip the first element, as it's always the classifier itself (which we already checked)
        for (CoreInstance genl : genls.subList(1, genls.size()))
        {
            function = this.idBuilders.get(genl);
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
        return (this.defaultIdPrefix == null) ? Integer.toString(syntheticId) : (this.defaultIdPrefix + syntheticId);
    }

    // Primitive values

    private static String buildIdForPrimitiveValue(CoreInstance instance)
    {
        return instance.getName();
    }

    // Enum value

    private static String buildIdForEnumValue(CoreInstance instance)
    {
        return instance.getName();
    }

    // AbstractProperty

    private static String buildIdForAbstractProperty(CoreInstance property)
    {
        StringBuilder builder = new StringBuilder();
        PackageableElement.writeUserPathForPackageableElement(builder, property.getValueForMetaPropertyToOne(M3Properties.owner));
        return builder.append('.').append(property.getName()).toString();
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
        StringBuilder builder = new StringBuilder();
        PackageableElement.writeUserPathForPackageableElement(builder, annotation.getValueForMetaPropertyToOne(M3Properties.profile));
        return builder.append('.').append(annotation.getName()).toString();
    }

    // PackageableElement

    private static String buildIdForPackageableElement(CoreInstance instance)
    {
        String id = PackageableElement.getSystemPathForPackageableElement(instance);
        if (ModelRepository.isAnonymousInstanceName(id) && (id.indexOf(':') == -1))
        {
            // don't return anonymous ids
            return null;
        }
        return id;
    }

    // Factory methods

    public static IdBuilder newIdBuilder(String defaultIdPrefix, ProcessorSupport processorSupport)
    {
        return new IdBuilder(defaultIdPrefix, processorSupport);
    }

    public static IdBuilder newIdBuilder(ProcessorSupport processorSupport)
    {
        return newIdBuilder(null, processorSupport);
    }

    @Deprecated
    public static String buildId(CoreInstance coreInstance, ProcessorSupport processorSupport)
    {
        return newIdBuilder(null, processorSupport).buildId(coreInstance);
    }

    public static String sourceToId(SourceInformation sourceInformation)
    {
        String sourceId = sourceInformation.getSourceId();
        if (Source.isInMemory(sourceId))
        {
            return CodeStorageTools.hasPureFileExtension(sourceId) ? sourceId.substring(0, sourceId.length() - CodeStorage.PURE_FILE_EXTENSION.length()) : sourceId;
        }

        int endIndex = CodeStorageTools.hasPureFileExtension(sourceId) ? (sourceId.length() - CodeStorage.PURE_FILE_EXTENSION.length()) : sourceId.length();
        return sourceId.substring(1, endIndex).replace('/', '_');
    }

    private static MapIterable<CoreInstance, Function<? super CoreInstance, String>> registerIdBuilderFunctions(ProcessorSupport processorSupport)
    {
        MutableMap<CoreInstance, Function<? super CoreInstance, String>> map = Maps.mutable.empty();
        registerStandardIdBuilderFunctions(map, processorSupport);
        return map;
    }

    private static void registerStandardIdBuilderFunctions(MutableMap<CoreInstance, Function<? super CoreInstance, String>> map, ProcessorSupport processorSupport)
    {
        PrimitiveUtilities.getPrimitiveTypes(processorSupport).forEach(t -> map.put(t, IdBuilder::buildIdForPrimitiveValue));
        registerIdBuilderFunction(map, M3Paths.AbstractProperty, IdBuilder::buildIdForAbstractProperty, processorSupport);
        registerIdBuilderFunction(map, M3Paths.Annotation, IdBuilder::buildIdForAnnotation, processorSupport);
        registerIdBuilderFunction(map, M3Paths.Enum, IdBuilder::buildIdForEnumValue, processorSupport);
        registerIdBuilderFunction(map, M3Paths.LambdaFunction, IdBuilder::buildIdForLambdaFunction, processorSupport);
        registerIdBuilderFunction(map, M3Paths.PackageableElement, IdBuilder::buildIdForPackageableElement, processorSupport);
    }

    private static void registerIdBuilderFunction(MutableMap<CoreInstance, Function<? super CoreInstance, String>> map, String classifierPath, Function<? super CoreInstance, String> function, ProcessorSupport processorSupport)
    {
        CoreInstance type = processorSupport.package_getByUserPath(classifierPath);
        Function<? super CoreInstance, String> old = map.put(type, function);
        if (old != null)
        {
            throw new RuntimeException("An id builder function is already registered for classifier: " + classifierPath);
        }
    }
}
