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

package org.finos.legend.pure.m3.compiler.postprocessing;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.AssociationProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningClassProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningPropertyProcessor;
import org.finos.legend.pure.m3.compiler.validation.validator.GenericTypeValidator;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.AssociationProjection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.pattern.URLPatternLibrary;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class PostProcessor
{
    public static SourceMutation process(Iterable<? extends CoreInstance> newInstancesConsolidated, ModelRepository modelRepository, ParserLibrary parserLibrary, InlineDSLLibrary inlineDSLLibrary, CodeStorage codeStorage, Context context, ProcessorSupport processorSupport, URLPatternLibrary URLPatternLibrary, Message message) throws PureCompilationException
    {
        CoreInstance concreteFunctionDefinition = processorSupport.package_getByUserPath(M3Paths.ConcreteFunctionDefinition);
        CoreInstance nativeFunction = processorSupport.package_getByUserPath(M3Paths.NativeFunction);
        MutableSet<CoreInstance> set = Sets.mutable.with(concreteFunctionDefinition, nativeFunction);

        validatePackages(newInstancesConsolidated, processorSupport);
        renameFunctions(newInstancesConsolidated, modelRepository, set, context, processorSupport);

        populateSpecializations(newInstancesConsolidated, processorSupport);

        populateTemporalMilestonedProperties(newInstancesConsolidated, modelRepository, context, processorSupport);

        MutableList<CoreInstance> allInstancesConsolidated = Lists.mutable.withAll(newInstancesConsolidated);
        allInstancesConsolidated.addAllIterable(populatePropertiesFromAssociations(newInstancesConsolidated, modelRepository, context, processorSupport));

        // Post Process
        Matcher matcher = new Matcher(modelRepository, context, processorSupport);

        addMatchersComingFromParsers(parserLibrary, matcher);
        inlineDSLLibrary.getInlineDSLs().forEach(dsl -> dsl.getProcessors().forEach(matcher::addMatchIfTypeIsKnown));

        ProcessorState state = new ProcessorState(VariableContext.newVariableContext(), parserLibrary, inlineDSLLibrary, processorSupport, URLPatternLibrary, codeStorage, message);
        allInstancesConsolidated.forEach(coreInstance ->
        {
            state.resetVariableContext();
            processElement(matcher, coreInstance, state, processorSupport);
        });

        state.getFunctionDefinitions().forEach(functionDef -> GenericTypeTraceability.addTraceForFunctionDefinition((FunctionDefinition<?>) functionDef, modelRepository, processorSupport));

        return state.getSourceMutation();
    }

    private static void addMatchersComingFromParsers(ParserLibrary parserLibrary, Matcher matcher)
    {
        parserLibrary.getParsers().forEach(parser -> parser.getProcessors().forEach(matcher::addMatchIfTypeIsKnown));
    }

    private static void validatePackages(Iterable<? extends CoreInstance> newInstancesConsolidated, ProcessorSupport processorSupport) throws PureCompilationException
    {
        newInstancesConsolidated.forEach(instance ->
        {
            CoreInstance pkg = Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties._package, processorSupport);
            if ((pkg != null) && !(pkg instanceof Package))
            {
                throw new PureCompilationException(instance.getSourceInformation(), "'" + PackageableElement.getUserPathForPackageableElement(pkg, "::") + "' is a " + pkg.getClassifier().getName() + ", should be a Package");
            }
        });
    }

    private static void populateSpecializations(Iterable<? extends CoreInstance> newInstancesConsolidated, ProcessorSupport processorSupport) throws PureCompilationException
    {
        // Specializations
        newInstancesConsolidated.forEach(coreInstance ->
        {
            if (coreInstance instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type)
            {
                SpecializationProcessor.process((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type) coreInstance, processorSupport);
            }
        });
    }

    private static Iterable<AbstractProperty<?>> populatePropertiesFromAssociations(Iterable<? extends CoreInstance> newInstancesConsolidated, ModelRepository modelRepository, Context context, final ProcessorSupport processorSupport) throws PureCompilationException
    {
        // Associations must be processed before AssociationProjections
        MutableList<Association> associations = Lists.mutable.empty();
        MutableList<Association> associationProjections = Lists.mutable.empty();
        newInstancesConsolidated.forEach(i ->
        {
            if (i instanceof AssociationProjection)
            {
                associationProjections.add((Association) i);
            }
            else if (i instanceof Association)
            {
                associations.add((Association) i);
            }
        });

        return associations.asLazy().concatenate(associationProjections).flatCollect(a -> AssociationProcessor.process(a, context, processorSupport, modelRepository), Lists.mutable.empty());
    }

    private static void populateTemporalMilestonedProperties(Iterable<? extends CoreInstance> newInstancesConsolidated, ModelRepository modelRepository, Context context, ProcessorSupport processorSupport) throws PureCompilationException
    {
        newInstancesConsolidated.forEach(instance ->
        {
            if (instance instanceof Class)
            {
                MilestoningClassProcessor.process((Class<?>) instance, context, processorSupport, modelRepository);
                MilestoningPropertyProcessor.process((Class<?>) instance, context, processorSupport, modelRepository);
            }
        });
    }

    private static void renameFunctions(Iterable<? extends CoreInstance> newInstancesConsolidated, ModelRepository modelRepository, MutableSet<CoreInstance> functionTypesButProperty, Context context, ProcessorSupport processorSupport) throws PureCompilationException
    {
        newInstancesConsolidated.forEach(coreInstance ->
        {
            if (functionTypesButProperty.contains(coreInstance.getClassifier()))
            {
                ConcreteFunctionDefinitionNameProcessor.process((Function<?>) coreInstance, modelRepository, processorSupport);
                context.update(coreInstance);
            }
        });
    }

    public static void processElement(Matcher matcher, CoreInstance instance, ProcessorState state, ProcessorSupport processorSupport) throws PureCompilationException
    {
        if (!instance.hasBeenProcessed())
        {
            state.incAndPushCount();
            instance.markProcessed();

            state.pushVariableContext();

            if (!matcher.match(instance, state))
            {
                instance.markNotProcessed();
            }

            state.popVariableContext();

            GenericTypeValidator.validateClassifierGenericTypeForInstance(instance, false, processorSupport);
        }
    }
}
