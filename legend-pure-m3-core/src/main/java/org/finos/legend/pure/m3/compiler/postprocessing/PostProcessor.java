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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.AssociationProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningClassProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningPropertyProcessor;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.compiler.validation.validator.GenericTypeValidator;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.AssociationProjection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m3.serialization.runtime.Message;
import org.finos.legend.pure.m3.serialization.runtime.pattern.URLPatternLibrary;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

import java.util.Comparator;

public class PostProcessor
{
    public static SourceMutation process(Iterable<? extends CoreInstance> newInstancesConsolidated, ModelRepository modelRepository, ParserLibrary parserLibrary, InlineDSLLibrary inlineDSLLibrary, CodeStorage codeStorage, Context context, ProcessorSupport processorSupport, URLPatternLibrary URLPatternLibrary, Message message) throws PureCompilationException
    {
        CoreInstance concreteFunctionDefinition = processorSupport.package_getByUserPath(M3Paths.ConcreteFunctionDefinition);
        CoreInstance nativeFunction = processorSupport.package_getByUserPath(M3Paths.NativeFunction);
        MutableSet<CoreInstance> set = UnifiedSet.newSetWith(concreteFunctionDefinition, nativeFunction);

        validatePackages(newInstancesConsolidated, processorSupport);
        renameFunctions(newInstancesConsolidated, modelRepository, set, context, processorSupport);

        populateSpecializations(newInstancesConsolidated, processorSupport);

        populateTemporalMilestonedProperties(newInstancesConsolidated, modelRepository, context, processorSupport);

        Iterable<CoreInstance> instancesFromAssociations = populatePropertiesFromAssociations(newInstancesConsolidated, modelRepository, context, processorSupport);
        Iterable<? extends CoreInstance> allInstancesConsolidated = ((MutableList<CoreInstance>)Lists.mutable.withAll(newInstancesConsolidated)).withAll(instancesFromAssociations);

        // Post Process
        Matcher matcher = new Matcher(modelRepository, context, processorSupport);

        addMatchersComingFromParsers(parserLibrary, matcher);
        for (InlineDSL dsl : inlineDSLLibrary.getInlineDSLs())
        {
            for (MatchRunner dslProcessor : dsl.getProcessors())
            {
                matcher.addMatchIfTypeIsKnown(dslProcessor);
            }
        }

        ProcessorState state = new ProcessorState(VariableContext.newVariableContext(), parserLibrary, inlineDSLLibrary, processorSupport, URLPatternLibrary, codeStorage, message);
        for (CoreInstance coreInstance : allInstancesConsolidated)
        {
            state.resetVariableContext();
            processElement(matcher, coreInstance, state, processorSupport);
        }

        for (CoreInstance coreInstance : state.getFunctionDefinitions())
        {
            GenericTypeTraceability.addTraceForFunctionDefinition((FunctionDefinition)coreInstance, modelRepository, processorSupport);
        }

        return state.getSourceMutation();
    }

    private static void addMatchersComingFromParsers(ParserLibrary parserLibrary, Matcher matcher)
    {
        for (Parser parser : parserLibrary.getParsers())
        {
            for (MatchRunner runner : parser.getProcessors())
            {
                matcher.addMatchIfTypeIsKnown(runner);
            }
        }
    }

    private static void validatePackages(Iterable<? extends CoreInstance> newInstancesConsolidated, ProcessorSupport processorSupport) throws PureCompilationException
    {
        for (CoreInstance instance : newInstancesConsolidated)
        {
            CoreInstance pkg = Instance.getValueForMetaPropertyToOneResolved(instance, M3Properties._package, processorSupport);
            if ((pkg != null) && !(pkg instanceof Package))
            {
                throw new PureCompilationException(instance.getSourceInformation(), "'" + PackageableElement.getUserPathForPackageableElement(pkg, "::") + "' is a " + pkg.getClassifier().getName() + ", should be a Package");
            }
        }
    }

    private static void populateSpecializations(Iterable<? extends CoreInstance> newInstancesConsolidated, ProcessorSupport processorSupport) throws PureCompilationException
    {
        // Specializations
        CoreInstance type = processorSupport.package_getByUserPath(M3Paths.Type);
        for (CoreInstance coreInstance : newInstancesConsolidated)
        {
            if (Type.subTypeOf(coreInstance.getClassifier(), type, processorSupport))
            {
                SpecializationProcessor.process((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type)coreInstance, processorSupport);
            }
        }
    }

    private static Iterable<CoreInstance> populatePropertiesFromAssociations(Iterable<? extends CoreInstance> newInstancesConsolidated, ModelRepository modelRepository, Context context, final ProcessorSupport processorSupport) throws PureCompilationException
    {
        CoreInstance associationType = processorSupport.package_getByUserPath(M3Paths.Association);
        MutableList<CoreInstance> associations = FastList.newList();
        for (CoreInstance instance : newInstancesConsolidated)
        {
            if (Type.subTypeOf(instance.getClassifier(), associationType, processorSupport))
            {

                associations.add(instance);
            }
        }
        //Associations need to be sorted to process Associations before AssociationProjections
        associations.sortThis(new Comparator<CoreInstance>()
        {
            @Override
            public int compare(CoreInstance o1, CoreInstance o2)
            {
                return o1 instanceof AssociationProjection ? (o2 instanceof AssociationProjection ? 0 : 1) : (o2 instanceof AssociationProjection ? -1 : 0);
            }
        });
        MutableList<CoreInstance> instancesFromAssociations = FastList.newList();
        for (CoreInstance instance : associations)
        {
            if (Type.subTypeOf(instance.getClassifier(), associationType, processorSupport))
            {
                instancesFromAssociations.addAllIterable(AssociationProcessor.process((Association)instance, context, processorSupport, modelRepository));
            }
        }
        return instancesFromAssociations;
    }

    private static void populateTemporalMilestonedProperties(Iterable<? extends CoreInstance> newInstancesConsolidated, ModelRepository modelRepository, Context context, ProcessorSupport processorSupport) throws PureCompilationException
    {
        Class classType = (Class)processorSupport.package_getByUserPath(M3Paths.Class);
        for (CoreInstance instance : newInstancesConsolidated)
        {
            if (Type.subTypeOf(instance.getClassifier(), classType, processorSupport))
            {
                MilestoningClassProcessor.process((Class)instance, context, processorSupport, modelRepository);
                MilestoningPropertyProcessor.process((Class)instance, context, processorSupport, modelRepository);
            }
        }
    }

    private static void renameFunctions(Iterable<? extends CoreInstance> newInstancesConsolidated, ModelRepository modelRepository, MutableSet<CoreInstance> functionTypesButProperty, Context context, ProcessorSupport processorSupport) throws PureCompilationException
    {
        for (CoreInstance coreInstance : newInstancesConsolidated)
        {
            if (functionTypesButProperty.contains(coreInstance.getClassifier()))
            {
                ConcreteFunctionDefinitionNameProcessor.process((Function)coreInstance, modelRepository, processorSupport);
                context.update(coreInstance);
            }
        }
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
