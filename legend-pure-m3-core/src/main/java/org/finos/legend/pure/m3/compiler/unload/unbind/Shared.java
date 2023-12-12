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

package org.finos.legend.pure.m3.compiler.unload.unbind;

import org.eclipse.collections.api.RichIterable;
import org.finos.legend.pure.m3.compiler.ReferenceUsage;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.EnumStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.PropertyStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.InferredGenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class Shared
{
    public static void cleanUpGenericType(GenericType genericType, UnbindState state, ProcessorSupport processorSupport)
    {
        state.freeValidated(genericType);
        genericType._referenceUsagesRemove();
        CoreInstance rawType = genericType._rawTypeCoreInstance();
        if (rawType != null)
        {
            cleanUpReferenceUsage(rawType, genericType, processorSupport);
            if (rawType instanceof FunctionType)
            {
                cleanUpFunctionType((FunctionType) rawType, state, processorSupport);
            }
            else
            {
                cleanImportStub(rawType, processorSupport);
            }
        }
        for (GenericType typeArgument : genericType._typeArguments())
        {
            cleanUpGenericType(typeArgument, state, processorSupport);
        }
    }

    public static void cleanImportStub(CoreInstance importStub, ProcessorSupport processorSupport)
    {
        if (importStub instanceof ImportStub)
        {
            ((ImportStub) importStub)._resolvedNodeRemove();
        }
    }

    public static void cleanPropertyStub(CoreInstance propertyStub, ProcessorSupport processorSupport)
    {
        if (propertyStub instanceof PropertyStub)
        {
            if (((PropertyStub) propertyStub)._ownerCoreInstance() != null)
            {
                cleanImportStub(((PropertyStub) propertyStub)._ownerCoreInstance(), processorSupport);
                ((PropertyStub) propertyStub)._resolvedPropertyRemove();
            }
        }
    }

    public static void cleanEnumStub(CoreInstance enumStub, ProcessorSupport processorSupport)
    {
        if (enumStub instanceof EnumStub)
        {
            if (((EnumStub) enumStub)._enumerationCoreInstance() != null)
            {
                cleanImportStub(((EnumStub) enumStub)._enumerationCoreInstance(), processorSupport);
                ((EnumStub) enumStub)._resolvedEnumRemove();
            }
        }
    }

    public static void cleanUpReferenceUsage(CoreInstance value, CoreInstance owner, ProcessorSupport processorSupport)
    {
        CoreInstance resolved;
        try
        {
            resolved = org.finos.legend.pure.m3.navigation.importstub.ImportStub.withImportStubByPass(value, processorSupport);
        }
        catch (PureCompilationException e)
        {
            // Exception is ok here ...
            resolved = null;
        }

        if (resolved != null)
        {
            ReferenceUsage.removeReferenceUsagesForUser(resolved, owner, processorSupport);
        }
    }

    private static void cleanUpFunctionType(FunctionType functionType, UnbindState state, ProcessorSupport processorSupport)
    {
        GenericType returnType = functionType._returnType();
        if (returnType != null)
        {
            returnType._referenceUsagesRemove();
            cleanUpGenericType(returnType, state, processorSupport);
        }

        for (VariableExpression variableExpression : functionType._parameters())
        {
            GenericType varGenericType = variableExpression._genericType();
            if (varGenericType != null)
            {
                cleanUpGenericType(varGenericType, state, processorSupport);
                if (varGenericType instanceof InferredGenericType)
                {
                    variableExpression._genericTypeRemove();
                    variableExpression._multiplicityRemove();
                }
            }
        }
    }

    public static void cleanImportStubs(RichIterable<? extends CoreInstance> importStubs, ProcessorSupport processorSupport)
    {
        for (CoreInstance importStub : importStubs)
        {
            cleanImportStub(importStub, processorSupport);
        }
    }

    public static void cleanEnumStubs(RichIterable<? extends CoreInstance> enumStubs, ProcessorSupport processorSupport)
    {
        for (CoreInstance enumStub : enumStubs)
        {
            cleanEnumStub(enumStub, processorSupport);
        }
    }

    public static void cleanUpReferenceUsages(RichIterable<? extends CoreInstance> values, CoreInstance owner, ProcessorSupport processorSupport)
    {
        for (CoreInstance value : values)
        {
            cleanUpReferenceUsage(value, owner, processorSupport);
        }
    }
}
