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
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
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
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class IdBuilder
{
    private final String defaultIdPrefix;
    private final ProcessorSupport processorSupport;
    private final ImmutableMap<CoreInstance, Function<? super CoreInstance, String>> idBuilders;
    private final ConcurrentMutableMap<CoreInstance, Function<? super CoreInstance, String>> cache;

    private IdBuilder(String defaultIdPrefix, ProcessorSupport processorSupport, ImmutableMap<CoreInstance, Function<? super CoreInstance, String>> idBuilders)
    {
        this.defaultIdPrefix = defaultIdPrefix;
        this.processorSupport = processorSupport;
        this.idBuilders = idBuilders;
        this.cache = ConcurrentHashMap.newMap(this.idBuilders.size());
        this.idBuilders.forEachKeyValue(this.cache::put);
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
        return (this.defaultIdPrefix == null) ? Integer.toString(syntheticId) : (this.defaultIdPrefix + syntheticId);
    }

    // QualifiedProperty

    private static String buildIdForQualifiedProperty(CoreInstance property)
    {
        return PackageableElement.writeUserPathForPackageableElement(new StringBuilder(), property.getValueForMetaPropertyToOne(M3Properties.owner))
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

        StringBuilder builder = PackageableElement.writeUserPathForPackageableElement(new StringBuilder(), owner);
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
        return PackageableElement.writeUserPathForPackageableElement(new StringBuilder(), annotation.getValueForMetaPropertyToOne(M3Properties.profile))
                .append('.').append(annotation.getName())
                .toString();
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

    // Builder

    /**
     * Function to build ids for instances of the given classifier.
     */
    public interface IdBuilderFunction extends Function<CoreInstance, String>
    {
        String getClassifierPath();
    }

    public static class Builder
    {
        private final MutableMap<CoreInstance, Function<? super CoreInstance, String>> idBuilders = Maps.mutable.empty();
        private final ProcessorSupport processorSupport;
        private String defaultIdPrefix;

        public Builder(ProcessorSupport processorSupport)
        {
            this.processorSupport = Objects.requireNonNull(processorSupport, "processorSupport may not be null");
            addStandardIdBuilders();
        }

        /**
         * Set the optional default id prefix. If non-null, the default id function will use this as the prefix for
         * all ids it generates.
         *
         * @param prefix default id prefix
         */
        public void setDefaultIdPrefix(String prefix)
        {
            this.defaultIdPrefix = prefix;
        }

        /**
         * Set the optional default id prefix. If non-null, the default id function will use this as the prefix for
         * all ids it generates.
         *
         * @param prefix default id prefix
         */
        public Builder withDefaultIdPrefix(String prefix)
        {
            setDefaultIdPrefix(prefix);
            return this;
        }

        /**
         * Add a function to build ids for the given classifier. An exception will be thrown if there is already a
         * function registered for the classifier.
         *
         * @param classifierPath full path of the classifer
         * @param function       id builder function
         */
        public void addIdBuilder(String classifierPath, Function<? super CoreInstance, String> function)
        {
            CoreInstance type = this.processorSupport.package_getByUserPath(Objects.requireNonNull(classifierPath, "classifier path may not be null"));
            Function<? super CoreInstance, String> old = this.idBuilders.put(type, Objects.requireNonNull(function, "function may not be null"));
            if (old != null)
            {
                throw new RuntimeException("An id builder function is already registered for classifier: " + classifierPath);
            }
        }

        /**
         * Add a function to build ids for the given classifier. An exception will be thrown if there is already a
         * function registered for the classifier.
         *
         * @param classifierPath full path of the classifer
         * @param function       id builder function
         */
        public Builder withIdBuilder(String classifierPath, Function<? super CoreInstance, String> function)
        {
            addIdBuilder(classifierPath, function);
            return this;
        }

        /**
         * Add functions to build ids for the corresponding classifiers. An exception will be thrown if there is already
         * a function registered for any of the given classifiers. In case of an exception, some of the functions may
         * be registered.
         *
         * @param idBuilderFunctions id builder functions by classifier path
         */
        public Builder withIdBuilders(Map<String, ? extends Function<? super CoreInstance, String>> idBuilderFunctions)
        {
            idBuilderFunctions.forEach(this::addIdBuilder);
            return this;
        }

        /**
         * Add an id builder function. An exception will be thrown if there is already a function registered for the
         * classifier.
         *
         * @param idBuilderFunction id builder function
         */
        public void addIdBuilder(IdBuilderFunction idBuilderFunction)
        {
            addIdBuilder(idBuilderFunction.getClassifierPath(), idBuilderFunction);
        }

        /**
         * Add an id builder function. An exception will be thrown if there is already a function registered for the
         * classifier.
         *
         * @param idBuilderFunction id builder function
         */
        public Builder withIdBuilder(IdBuilderFunction idBuilderFunction)
        {
            addIdBuilder(idBuilderFunction.getClassifierPath(), idBuilderFunction);
            return this;
        }

        /**
         * Add functions to build ids for the corresponding classifiers. An exception will be thrown if there is already
         * a function registered for any of the given classifiers. In case of an exception, some of the functions may
         * be registered.
         *
         * @param idBuilderFunctions id builder functions by classifier path
         */
        public Builder withIdBuilders(IdBuilderFunction... idBuilderFunctions)
        {
            ArrayIterate.forEach(idBuilderFunctions, this::addIdBuilder);
            return this;
        }

        /**
         * Add functions to build ids for the corresponding classifiers. An exception will be thrown if there is already
         * a function registered for any of the given classifiers. In case of an exception, some of the functions may
         * be registered.
         *
         * @param idBuilderFunctions id builder functions by classifier path
         */
        public Builder withIdBuilders(Iterable<? extends IdBuilderFunction> idBuilderFunctions)
        {
            idBuilderFunctions.forEach(this::addIdBuilder);
            return this;
        }

        /**
         * Build the {@linkplain IdBuilder}.
         *
         * @return {@linkplain IdBuilder}
         */
        public IdBuilder build()
        {
            return new IdBuilder(this.defaultIdPrefix, this.processorSupport, this.idBuilders.toImmutable());
        }

        private void addStandardIdBuilders()
        {
            PrimitiveUtilities.getPrimitiveTypes(this.processorSupport).forEach(t -> this.idBuilders.put(t, CoreInstance::getName));
            addIdBuilder(M3Paths.Annotation, IdBuilder::buildIdForAnnotation);
            addIdBuilder(M3Paths.Enum, CoreInstance::getName);
            addIdBuilder(M3Paths.LambdaFunction, IdBuilder::buildIdForLambdaFunction);
            addIdBuilder(M3Paths.PackageableElement, IdBuilder::buildIdForPackageableElement);
            addIdBuilder(M3Paths.Property, IdBuilder::buildIdForProperty);
            addIdBuilder(M3Paths.QualifiedProperty, IdBuilder::buildIdForQualifiedProperty);
            CompiledExtensionLoader.extensions().flatCollect(x -> x.getExtraIdBuilders(this.processorSupport)).forEach(x -> addIdBuilder(x.getOne(), x.getTwo()));
        }
    }

    public static Builder builder(ProcessorSupport processorSupport)
    {
        return new Builder(processorSupport);
    }

    public static IdBuilder newIdBuilder(String defaultIdPrefix, ProcessorSupport processorSupport)
    {
        return builder(processorSupport).withDefaultIdPrefix(defaultIdPrefix).build();
    }

    public static IdBuilder newIdBuilder(String defaultIdPrefix, ProcessorSupport processorSupport, IdBuilderFunction... idBuilderFunctions)
    {
        return builder(processorSupport).withDefaultIdPrefix(defaultIdPrefix).withIdBuilders(idBuilderFunctions).build();
    }

    public static IdBuilder newIdBuilder(ProcessorSupport processorSupport)
    {
        return builder(processorSupport).build();
    }

    public static IdBuilder newIdBuilder(ProcessorSupport processorSupport, IdBuilderFunction... idBuilderFunctions)
    {
        return builder(processorSupport).withIdBuilders(idBuilderFunctions).build();
    }

    @Deprecated
    public static String buildId(CoreInstance coreInstance, ProcessorSupport processorSupport)
    {
        return builder(processorSupport).build().buildId(coreInstance);
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
}
