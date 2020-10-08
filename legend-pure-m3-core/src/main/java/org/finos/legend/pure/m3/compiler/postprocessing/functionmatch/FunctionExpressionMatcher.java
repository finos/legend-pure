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

package org.finos.legend.pure.m3.compiler.postprocessing.functionmatch;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Multimaps;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.imports.Imports;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

public class FunctionExpressionMatcher
{
    public static ListIterable<? extends CoreInstance> findMatchingFunctionsInTheRepository(FunctionExpression functionExpression, boolean lenient, ProcessorSupport processorSupport) throws PureCompilationException
    {
        RichIterable<? extends ValueSpecification> parametersValues = functionExpression._parametersValues();

        ListIterable<String> splitFunctionPath = PackageableElement.splitUserPath(functionExpression._functionName());
        int lastIndex = splitFunctionPath.size() - 1;
        String functionToFindName = splitFunctionPath.get(lastIndex);
        ListIterable<String> functionPkg = (lastIndex == 0) ? Lists.immutable.<String>with() : splitFunctionPath.take(lastIndex);

        RichIterable<Function> functionsToSearch = getFunctionsWithMatchingName(functionToFindName, functionPkg, functionExpression, processorSupport);

        SourceInformation sourceInformation = functionExpression.getSourceInformation();

        return getFunctionMatches(functionsToSearch, parametersValues, functionToFindName, sourceInformation, lenient, processorSupport);
    }

    public static ListIterable<? extends CoreInstance> getFunctionMatches(RichIterable<Function> functionsToSearch, RichIterable<? extends ValueSpecification> parametersValues, String functionToFindName, SourceInformation sourceInformation, boolean lenient, ProcessorSupport processorSupport) throws PureCompilationException
    {
        MutableListMultimap<FunctionMatch, CoreInstance> functionsByMatch = Multimaps.mutable.list.empty();
        try
        {
            for (Function function : functionsToSearch)
            {
                FunctionMatch match = FunctionMatch.newFunctionMatch(function, functionToFindName, parametersValues.toList(), lenient, processorSupport);
                if (match != null)
                {
                    functionsByMatch.put(match, function);
                }
            }
        }
        catch (RuntimeException e)
        {
            StringBuilder message = new StringBuilder("Error finding match for function '");
            message.append(functionToFindName);
            message.append('\'');
            if (e.getMessage() != null)
            {
                message.append(": ");
                message.append(e.getMessage());
            }
            throw new PureCompilationException(sourceInformation, message.toString(), e);
        }

        MutableList<CoreInstance> functionMatches = FastList.newList(functionsByMatch.sizeDistinct());
        for (FunctionMatch match : functionsByMatch.keysView().toSortedList())
        {
            functionMatches.addAllIterable(functionsByMatch.get(match));
        }
        return functionMatches;
    }

    public static CoreInstance getBestFunctionMatch(RichIterable<? extends Function> functionsToSearch, ListIterable<? extends ValueSpecification> parametersValues, String functionToFindName, SourceInformation sourceInformation, boolean lenient, ProcessorSupport processorSupport) throws PureCompilationException
    {
        FunctionMatch bestMatch = null;
        MutableList<Function> bestFunctions = null;

        try
        {
            for (Function function : functionsToSearch)
            {
                FunctionMatch match = FunctionMatch.newFunctionMatch(function, functionToFindName, parametersValues, lenient, processorSupport);
                if (match != null)
                {
                    if (bestMatch == null)
                    {
                        bestMatch = match;
                        bestFunctions = Lists.mutable.with(function);
                    }
                    else
                    {
                        int comparison = match.compareTo(bestMatch);
                        if (comparison == 0)
                        {
                            bestFunctions.add(function);
                        }
                        else if (comparison < 0)
                        {
                            bestMatch = match;
                            bestFunctions.clear();
                            bestFunctions.add(function);
                        }
                    }
                }
            }
        }
        catch (RuntimeException e)
        {
            StringBuilder message = new StringBuilder("Error finding match for function '");
            message.append(functionToFindName);
            message.append('\'');
            if (e.getMessage() != null)
            {
                message.append(": ");
                message.append(e.getMessage());
            }
            throw new PureCompilationException(sourceInformation, message.toString(), e);
        }

        if (bestFunctions == null)
        {
            return null;
        }

        if (bestFunctions.size() > 1)
        {
            StringBuilder message = new StringBuilder("Too many matches for ");
            org.finos.legend.pure.m3.navigation.functionexpression.FunctionExpression.printFunctionSignatureFromExpression(message, functionToFindName, parametersValues, processorSupport);
            MutableList<String> functionDescriptors = FastList.newList(bestFunctions.size());
            for (CoreInstance func : bestFunctions)
            {
                functionDescriptors.add(org.finos.legend.pure.m3.navigation.function.Function.print(func, processorSupport));
            }
            functionDescriptors.sortThis().appendString(message, ":\n\t", "\n\t", "");
            throw new PureCompilationException(sourceInformation, message.toString());
        }

        return bestFunctions.getFirst();
    }

    private static RichIterable<Function> getFunctionsWithMatchingName(String functionName, ListIterable<String> functionPackage, FunctionExpression functionExpression, ProcessorSupport processorSupport)
    {
        SetIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> packages = getValidPackages(functionPackage, functionExpression, processorSupport);
        MutableList<Function> functions = Lists.mutable.empty();
        for (CoreInstance function : processorSupport.function_getFunctionsForName(functionName))
        {
            if (packages.contains(((Function)function)._package()))
            {
                functions.add((Function)function);
            }
        }
        return functions;
    }

    private static SetIterable<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> getValidPackages(ListIterable<String> functionPackage, FunctionExpression functionExpression, ProcessorSupport processorSupport)
    {
        if (functionPackage.notEmpty())
        {
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement pkg = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement)_Package.getByUserPath(functionPackage, processorSupport);
            return (pkg == null) ? Sets.immutable.<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement>empty() : Sets.immutable.with(pkg);
        }

        MutableSet<org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement> packages = Imports.getImportGroupPackages(functionExpression._importGroup(), processorSupport).toSet();
        packages.add((org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement)processorSupport.repository_getTopLevel(M3Paths.Root));
        return packages;
    }
}
