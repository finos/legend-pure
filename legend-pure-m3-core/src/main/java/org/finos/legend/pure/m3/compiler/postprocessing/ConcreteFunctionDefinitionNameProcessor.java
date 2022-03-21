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
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class ConcreteFunctionDefinitionNameProcessor
{
    public static void process(Function<?> function, ModelRepository repository, ProcessorSupport processorSupport) throws PureCompilationException
    {
        Package parent = function._package();
        if (parent != null)
        {
            //Make sure we have a unique name for overloaded functions.
            String signature = getSignatureAndResolveImports(function, repository, processorSupport);
            parent._childrenRemove(function);
            function.setName(signature);
            parent._childrenAdd(function);

            if (function._name() == null)
            {
                function._name(signature);
            }
            if (parent._children().count(c -> signature.equals(c.getName())) > 1)
            {
                ListIterable<SourceInformation> sourceInfos = parent._children().collectIf(c -> signature.equals(c.getName()), CoreInstance::getSourceInformation, Lists.mutable.empty()).sortThis();
                String pkg = PackageableElement.getUserPathForPackageableElement(parent);
                if (M3Paths.Root.equals(pkg))
                {
                    pkg = "::";
                }

                StringBuilder message = new StringBuilder("The function '").append(signature).append("' is defined more than once in the package '").append(pkg).append("' at: ");
                boolean first = true;
                for (SourceInformation sourceInfo : sourceInfos)
                {
                    if (first)
                    {
                        first = false;
                    }
                    else
                    {
                        message.append(", ");
                    }
                    message.append(sourceInfo.getSourceId()).append(" (line:").append(sourceInfo.getLine()).append(" column:").append(sourceInfo.getColumn()).append(')');
                }
                throw new PureCompilationException(function.getSourceInformation(), message.toString());
            }
        }
    }

    private static String getSignatureAndResolveImports(Function<?> function, ModelRepository repository, ProcessorSupport processorSupport) throws PureCompilationException
    {
        String functionName = function._functionName();
        FunctionType functionType = (FunctionType)processorSupport.function_getFunctionType(function);
        GenericType returnTypeGeneric = functionType._returnType();
        org.finos.legend.pure.m3.navigation.generictype.GenericType.resolveGenericTypeUsingImports(returnTypeGeneric, repository, processorSupport);
        String returnType = ImportStub.withImportStubByPass(returnTypeGeneric._rawTypeCoreInstance(), processorSupport) == null ? org.finos.legend.pure.m3.navigation.generictype.GenericType.getTypeParameterName(returnTypeGeneric, processorSupport) : ImportStub.withImportStubByPass(returnTypeGeneric._rawTypeCoreInstance(), processorSupport).getName();
        String multiplicity = Multiplicity.multiplicityToSignatureString(functionType._returnMultiplicity());
        MutableList<String> vars = functionType._parameters().collect(v -> functionSignatureVariableToString(v, repository, processorSupport), Lists.mutable.empty());
        return functionName + "_" + vars.makeString("_") + "_" + returnType + multiplicity;
    }

    private static String functionSignatureVariableToString(VariableExpression variable, ModelRepository repository, ProcessorSupport processorSupport) throws PureCompilationException
    {
        GenericType variableTypeGeneric = variable._genericType();
        org.finos.legend.pure.m3.navigation.generictype.GenericType.resolveGenericTypeUsingImports(variableTypeGeneric, repository, processorSupport);
        String type;
        if (ImportStub.withImportStubByPass(variableTypeGeneric._rawTypeCoreInstance(), processorSupport) == null)
        {
            type = org.finos.legend.pure.m3.navigation.generictype.GenericType.getTypeParameterName(variableTypeGeneric, processorSupport);
        }
        else
        {
            Type theType = (Type)ImportStub.withImportStubByPass(variableTypeGeneric._rawTypeCoreInstance(), processorSupport);
            if ("FunctionType".equals(theType.getClassifier().getName()))
            {
                type = "FunctionTypeTODO";
            }
            else
            {
                type = ImportStub.withImportStubByPass(variableTypeGeneric._rawTypeCoreInstance(), processorSupport).getName();
            }
        }
        String multiplicity = Multiplicity.multiplicityToSignatureString(variable._multiplicity());
        return type + multiplicity;
    }

}
