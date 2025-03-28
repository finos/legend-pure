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

package org.finos.legend.pure.m3.compiler.postprocessing.processor.projection;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Automap;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.ClassProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.AnnotatedElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.ExistingPropertyRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.NewPropertyRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.PropertyRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RootRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RouteNodePropertyStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassProjection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.DataType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class ClassProjectionProcessor extends Processor<ClassProjection<?>>
{
    @Override
    public String getClassName()
    {
        return M3Paths.ClassProjection;
    }

    @Override
    public void process(ClassProjection<?> classProjection, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        RootRouteNode projectionSpec = classProjection._projectionSpecification();
        Type projectedRawType = projectionSpec._type() == null ? null : (Type) ImportStub.withImportStubByPass(projectionSpec._type()._rawTypeCoreInstance(), processorSupport);

        PostProcessor.processElement(matcher, projectionSpec, state, processorSupport);

        processDerivedProperties(classProjection, projectionSpec, repository, context, processorSupport);
        processSimpleProperties(classProjection, state, repository, context, processorSupport);

        ProjectionUtil.copyAnnotations((AnnotatedElement) projectedRawType, classProjection, true, processorSupport);
        ProjectionUtil.copyAnnotations(projectionSpec, classProjection, true, processorSupport);

        context.update(classProjection);
        processClass(classProjection, state, matcher, processorSupport);
    }

    private static void processClass(ClassProjection<?> cls, ProcessorState state, Matcher matcher, ProcessorSupport processorSupport)
    {
        state.newTypeInferenceContext(cls);

        MutableList<CoreInstance> propertiesProperties = Lists.mutable.empty();

        // Simple properties
        propertiesProperties.addAllIterable(cls._properties());
        propertiesProperties.addAllIterable(cls._propertiesFromAssociations());

        // Qualified properties
        propertiesProperties.addAllIterable(cls._qualifiedProperties());
        propertiesProperties.addAllIterable(cls._qualifiedPropertiesFromAssociations());

        // Original milestoned properties
        propertiesProperties.addAllIterable(cls._originalMilestonedProperties());

        propertiesProperties.forEach(property ->
        {
            try
            {
                PostProcessor.processElement(matcher, property, state, processorSupport);
            }
            catch (PureCompilationException e)
            {
                StringBuilder builder = PackageableElement.writeUserPathForPackageableElement(new StringBuilder("Error compiling projection '"), cls)
                        .append("'. Property '").append(org.finos.legend.pure.m3.navigation.property.Property.getPropertyName(property))
                        .append("' cannot be resolved due to underlying cause: ")
                        .append(e.getInfo());
                throw new PureCompilationException(e.getSourceInformation(), builder.toString(), e);
            }
        });

        state.deleteTypeInferenceContext();
    }


    @Override
    public void populateReferenceUsages(ClassProjection<?> cls, ModelRepository repository, ProcessorSupport processorSupport)
    {
        ClassProcessor.processClassReferenceUsages(cls, repository, processorSupport);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void processDerivedProperties(ClassProjection classProjection, RootRouteNode projectionSpec, ModelRepository modelRepository, Context context, ProcessorSupport processorSupport)
    {
        validateDerivedProperties(projectionSpec, processorSupport);
        MutableList<Property<?, ?>> newSimpleProperties = projectionSpec._children().collect(instance ->
        {
            ListIterable<? extends ValueSpecification> valueSpecifications = ((NewPropertyRouteNode) instance)._specifications().toList();
            CoreInstance autoMapExpressionSequence = Automap.getAutoMapExpressionSequence(valueSpecifications.get(0));
            Property<?, ?> property = (Property<?, ?>) ImportStub.withImportStubByPass(autoMapExpressionSequence != null ? ((FunctionExpression) autoMapExpressionSequence)._funcCoreInstance() : ((FunctionExpression) valueSpecifications.get(0))._funcCoreInstance(), processorSupport);
            Property<?, ?> propertyCopy = ProjectionUtil.deepCopyAndBindSimpleProperty(property, classProjection, modelRepository, context, processorSupport);
            renameDerivedProperty(instance, propertyCopy);
            return propertyCopy;
        }, Lists.mutable.empty());
        copyPropertyAnnotations(projectionSpec, newSimpleProperties, processorSupport);
        classProjection._propertiesAddAll(newSimpleProperties);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void processSimpleProperties(ClassProjection classProjection, ProcessorState state, ModelRepository modelRepository, Context context, ProcessorSupport processorSupport)
    {
        RootRouteNode projectionSpec = classProjection._projectionSpecification();
        MutableList<Property<?, ?>> simplePropertyCopies = Lists.mutable.empty();
        MutableList<QualifiedProperty<?>> qualifiedPropertyCopies = Lists.mutable.empty();
        projectionSpec._resolvedProperties().forEach(prop ->
        {
            if (prop instanceof Property<?, ?>)
            {
                simplePropertyCopies.add(ProjectionUtil.deepCopyAndBindSimpleProperty((Property<?, ?>) prop, classProjection, modelRepository, context, processorSupport));
            }
            else
            {
                qualifiedPropertyCopies.add(ProjectionUtil.deepCopyAndBindQualifiedProperty((QualifiedProperty<?>) prop, classProjection, state, modelRepository, context, processorSupport));
            }
        });
        if (simplePropertyCopies.notEmpty())
        {
            copyPropertyAnnotations(projectionSpec, simplePropertyCopies, processorSupport);
            classProjection._propertiesAddAll(simplePropertyCopies);
        }
        if (qualifiedPropertyCopies.notEmpty())
        {
            copyPropertyAnnotations(projectionSpec, qualifiedPropertyCopies, processorSupport);
            classProjection._qualifiedPropertiesAddAll(qualifiedPropertyCopies);
        }
    }

    private static void copyPropertyAnnotations(RootRouteNode projectionSpec, RichIterable<? extends AbstractProperty<?>> resolvedProperties, ProcessorSupport processorSupport)
    {
        if (resolvedProperties.notEmpty())
        {
            RichIterable<? extends RouteNodePropertyStub> includedPropertyStubs = projectionSpec._included();
            if (includedPropertyStubs.notEmpty())
            {
                MutableMap<String, AbstractProperty<?>> resolvedPropertiesById = Maps.mutable.ofInitialCapacity(resolvedProperties.size());
                resolvedProperties.forEach(resolvedProperty ->
                {
                    String resolvedPropertyId = org.finos.legend.pure.m3.navigation.property.Property.getPropertyId(resolvedProperty, processorSupport);
                    resolvedPropertiesById.put(resolvedPropertyId, resolvedProperty);
                });

                includedPropertyStubs.forEach(includedPropertyStub ->
                {
                    CoreInstance includedProperty = ImportStub.withImportStubByPass(includedPropertyStub._propertyCoreInstance().toList().getFirst(), processorSupport);
                    String includedPropertyId = org.finos.legend.pure.m3.navigation.property.Property.getPropertyId(includedProperty, processorSupport);
                    AbstractProperty<?> derivedProperty = resolvedPropertiesById.get(includedPropertyId);
                    if (derivedProperty != null)
                    {
                        ProjectionUtil.copyAnnotations(includedPropertyStub, derivedProperty, false, processorSupport);
                    }
                });
            }
        }
    }

    private static void validateDerivedProperties(RootRouteNode projectionSpec, ProcessorSupport processorSupport)
    {
        //we can only support derived properties which flatten relations to a primitive type. Qualified Properties are not supported.
        for (PropertyRouteNode derivedProperty : projectionSpec._children())
        {
            if (derivedProperty instanceof ExistingPropertyRouteNode)
            {
                throw new PureCompilationException(derivedProperty.getSourceInformation(), String.format("Invalid projection specification. Found complex property '%s', only simple properties are allowed in a class projection.", derivedProperty._propertyName()));
            }
            CoreInstance derivedPropertyType = derivedProperty._type() == null ? null : ImportStub.withImportStubByPass(derivedProperty._type()._rawTypeCoreInstance(), processorSupport);
            if (!(derivedPropertyType instanceof DataType))
            {
                throw new PureCompilationException(derivedProperty.getSourceInformation(), String.format("Invalid projection specification. Derived property '%s' should be of PrimitiveType.", derivedProperty._propertyName()));
            }
            ListIterable<? extends ValueSpecification> valueSpecifications = derivedProperty instanceof NewPropertyRouteNode ?
                                                                             ((NewPropertyRouteNode) derivedProperty)._specifications().toList() : Lists.immutable.empty();
            if (valueSpecifications.size() != 1)
            {
                throw new PureCompilationException(derivedProperty.getSourceInformation(), "Invalid projection specification: derived property '" + derivedProperty._propertyName() + "' should have exactly 1 value specification, found " + valueSpecifications.size());
            }
            if (valueSpecifications.getFirst() instanceof FunctionExpression)
            {
                CoreInstance func = ImportStub.withImportStubByPass(((FunctionExpression) valueSpecifications.getFirst())._funcCoreInstance(), processorSupport);
                if (func != null && !(func instanceof Property) && Automap.getAutoMapExpressionSequence(valueSpecifications.getFirst()) == null)
                {
                    throw new PureCompilationException(derivedProperty.getSourceInformation(), String.format("Invalid projection specification. Derived property '%s' should be a simple property.", derivedProperty._propertyName()));
                }
            }
        }
    }

    private static void renameDerivedProperty(RouteNode instance, Property<?, ?> propertyCopy)
    {
        String derivedPropertyName = instance._name();
        propertyCopy._name(derivedPropertyName);
        propertyCopy.setName(derivedPropertyName);
    }
}
