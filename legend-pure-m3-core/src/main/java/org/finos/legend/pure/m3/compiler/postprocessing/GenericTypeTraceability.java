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

import org.finos.legend.pure.m3.compiler.ReferenceUsage;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.NewPropertyRouteNodeFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public class GenericTypeTraceability
{
    public static void addTraceForFunctionDefinition(FunctionDefinition<?> functionDefinition, ModelRepository repository, ProcessorSupport processorSupport)
    {
        FunctionType functionType = (FunctionType) processorSupport.function_getFunctionType(functionDefinition);
        if (functionType._functionCoreInstance().isEmpty())
        {
            functionType._functionAdd(functionDefinition);
        }
        addTraceForFunctionType(functionType, repository, processorSupport);
    }

    public static void addTraceForFunctionType(FunctionType functionType, ModelRepository repository, ProcessorSupport processorSupport)
    {
        functionType._parameters().forEach(variableExpression ->
        {
            if (variableExpression._functionTypeOwner() == null)
            {
                variableExpression._functionTypeOwner(functionType);
            }
            addTraceForAllPossibleTypeArguments(variableExpression, M3Properties.genericType, 0, variableExpression._genericType(), repository, processorSupport);
        });
        addTraceForAllPossibleTypeArguments(functionType, M3Properties.returnType, 0, functionType._returnType(), repository, processorSupport);
    }

    public static void addTraceForProperty(Property<?, ?> property, ModelRepository repository, ProcessorSupport processorSupport)
    {
        GenericType classifierGenericType = property._classifierGenericType();
        addTraceForAllPossibleTypeArguments(property, M3Properties.classifierGenericType, 0, classifierGenericType, repository, processorSupport);
    }

    public static void addTraceForEmptyInstanceValueGenericType(CoreInstance instance, ModelRepository repository, ProcessorSupport processorSupport)
    {
        CoreInstance genericType = instance.getValueForMetaPropertyToOne(M3Properties.genericType);
        addTraceForAllPossibleTypeArguments(instance, M3Properties.genericType, 0, (GenericType) genericType, repository, processorSupport);
    }

    public static void addTraceForGeneralization(Generalization generalization, ModelRepository modelRepository, ProcessorSupport processorSupport)
    {
        GenericType genericGenericType = generalization._general();
        addTraceForAllPossibleTypeArguments(generalization, M3Properties.general, 0, genericGenericType, modelRepository, processorSupport);
    }

    public static void addTraceForEnum(FunctionExpression functionExpression, ModelRepository modelRepository, ProcessorSupport processorSupport)
    {
        ValueSpecification instanceValue = functionExpression._parametersValues().getFirst();
        addTraceForAllPossibleTypeArguments(instanceValue, M3Properties.genericType, 0, instanceValue._genericType(), modelRepository, processorSupport);
    }

    public static void addTraceForPath(CoreInstance instance, ModelRepository repository, ProcessorSupport processorSupport)
    {
        CoreInstance genericType = instance.getValueForMetaPropertyToOne(M3Properties.start);
        addTraceForAllPossibleTypeArguments(instance, M3Properties.start, 0, (GenericType) genericType, repository, processorSupport);
    }

    public static void addTraceForTreePath(CoreInstance instance, ModelRepository repository, ProcessorSupport processorSupport)
    {
        CoreInstance genericType = instance.getValueForMetaPropertyToOne(M3Properties.type);
        addTraceForAllPossibleTypeArguments(instance, M3Properties.type, 0, (GenericType) genericType, repository, processorSupport);
    }

    public static void addTraceForNewPropertyRouteNodeFunctionDefinition(NewPropertyRouteNodeFunctionDefinition<?, ?> instance, ModelRepository repository, ProcessorSupport processorSupport)
    {
        GenericType classifierGenericType = instance._classifierGenericType();
        addTraceForAllPossibleTypeArguments(instance, M3Properties.classifierGenericType, 0, classifierGenericType, repository, processorSupport);
    }

    private static void addTraceForAllPossibleTypeArguments(CoreInstance parent, String parentProperty, int parentOffset, GenericType genericType, ModelRepository repository, ProcessorSupport processorSupport)
    {
        ReferenceUsage.addReferenceUsage(genericType, parent, parentProperty, parentOffset, repository, processorSupport);

        Type rawType = (Type) ImportStub.withImportStubByPass(genericType._rawTypeCoreInstance(), processorSupport);
        if (rawType != null)
        {
            if (rawType instanceof FunctionType)
            {
                addTraceForFunctionType((FunctionType) rawType, repository, processorSupport);
            }
            ReferenceUsage.addReferenceUsage(rawType, genericType, M3Properties.rawType, 0, repository, processorSupport);

            int offset = 0;
            for (GenericType typeArgument : genericType._typeArguments())
            {
                addTraceForAllPossibleTypeArguments(genericType, M3Properties.typeArguments, offset++, typeArgument, repository, processorSupport);
            }
        }
    }
}
