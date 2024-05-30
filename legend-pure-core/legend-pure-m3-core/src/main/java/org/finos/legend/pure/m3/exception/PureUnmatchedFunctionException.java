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

package org.finos.legend.pure.m3.exception;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class PureUnmatchedFunctionException extends PureCompilationException
{
    public static final String NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE = "\nThese functions, in packages already imported, would match the function call if you changed the parameters.\n";
    public static final String EMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE = "\nNo functions, in packages already imported, match the function name.\n";
    public static final String NONEMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE = "\nThese functions, in packages not imported, match the function name. Add the package to imports so that the function is in scope.\n";
    public static final String EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE = "\nNo functions, in packages not imported, match the function name.\n";
    public static final String FUNCTION_UNMATCHED_MESSAGE = "The system can't find a match for the function: ";

    private final String functionSignature;
    private final String functionName;
    private final ImmutableList<CoreInstance> importCandidatesWithPackageNotImported;
    private final ImmutableList<CoreInstance> importCandidatesWithPackageImported;
    private final CoreInstance importGroup;

    public PureUnmatchedFunctionException(SourceInformation sourceInformation, String functionSignature, String functionName,
                                          Iterable<? extends CoreInstance> candidatesInCoreImportsWithPackageNotImported,
                                          Iterable<? extends CoreInstance> candidatesNotInCoreImportsWithPackageNotImported,
                                          Iterable<? extends CoreInstance> candidatesInCoreImportsWithPackageImported,
                                          Iterable<? extends CoreInstance> candidatesNotInCoreImportsWithPackageImported,
                                          CoreInstance importGroup, ProcessorSupport processorSupport)
    {
        super(
                sourceInformation,
                buildInfo(
                        functionSignature,
                        Lists.mutable.<CoreInstance>withAll(candidatesInCoreImportsWithPackageNotImported).withAll(candidatesNotInCoreImportsWithPackageNotImported),
                        Lists.mutable.<CoreInstance>withAll(candidatesInCoreImportsWithPackageImported).withAll(candidatesNotInCoreImportsWithPackageImported),
                        processorSupport
                )
        );
        this.functionSignature = functionSignature;
        this.functionName = functionName;
        this.importCandidatesWithPackageNotImported = Lists.immutable.withAll(candidatesNotInCoreImportsWithPackageNotImported);
        this.importCandidatesWithPackageImported = Lists.immutable.withAll(candidatesNotInCoreImportsWithPackageImported);
        this.importGroup = importGroup;
    }

    public String getFunctionSignature()
    {
        return this.functionSignature;
    }

    public String getFunctionName()
    {
        return this.functionName;
    }

    public ListIterable<CoreInstance> getImportCandidatesWithPackageNotImported()
    {
        return this.importCandidatesWithPackageNotImported;
    }

    public ListIterable<CoreInstance> getImportCandidatesWithPackageImported()
    {
        return this.importCandidatesWithPackageImported;
    }

    public CoreInstance getImportGroup()
    {
        return this.importGroup;
    }

    private static String buildInfo(String functionSignature, Iterable<? extends CoreInstance> candidatesWithPackageNotImported, Iterable<? extends CoreInstance> candidatesWithPackageImported, ProcessorSupport processorSupport)
    {
        StringBuilder builder = new StringBuilder();
        builder.append(FUNCTION_UNMATCHED_MESSAGE);
        builder.append(functionSignature);
        builder.append("\n");

        if (Iterate.notEmpty(candidatesWithPackageImported))
        {
            builder.append(NONEMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE);
            MutableList<String> functionSignatures = findFunctionSignatures(candidatesWithPackageImported, processorSupport);
            builder.append(functionSignatures.makeString("\t", "\n\t", "\n"));
        }
        else
        {
            builder.append(EMPTY_CANDIDATES_WITH_PACKAGE_IMPORTED_MESSAGE);
        }

        if (Iterate.notEmpty(candidatesWithPackageNotImported))
        {
            builder.append(NONEMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE);
            MutableList<String> functionSignatures = findFunctionSignatures(candidatesWithPackageNotImported, processorSupport);
            builder.append(functionSignatures.makeString("\t", "\n\t", "\n"));
        }
        else
        {
            builder.append(EMPTY_CANDIDATES_WITH_PACKAGE_NOT_IMPORTED_MESSAGE);
        }

        return builder.toString();
    }

    private static MutableList<String> findFunctionSignatures(Iterable<? extends CoreInstance> candidates, ProcessorSupport processorSupport)
    {
        MutableList<String> index = Lists.mutable.empty();
        for (CoreInstance candidate : candidates)
        {
            StringBuilder builder = new StringBuilder();
            try
            {
                Function.print(builder, candidate, processorSupport);
            }
            catch (Exception e)
            {
                builder.setLength(0);
                PackageableElement.writeUserPathForPackageableElement(builder, candidate);
                builder.append("(???):??");
            }
            index.add(builder.toString());
        }
        return index.sortThis();
    }
}
