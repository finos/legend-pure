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

package org.finos.legend.pure.m3.serialization;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.multimap.list.ListMultimap;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.validation.ValidationType;
import org.finos.legend.pure.m3.compiler.validation.Validator;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.grammar.Parser;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3AntlrParser;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m3.serialization.grammar.top.TopParser;
import org.finos.legend.pure.m3.serialization.runtime.IncrementalCompiler;
import org.finos.legend.pure.m3.statelistener.M3M4StateListener;
import org.finos.legend.pure.m3.statelistener.M3StateListener;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;

public class Loader
{
    private static void updateStateFullContextCache(ListIterable<CoreInstance> parsedResults, Context context) throws PureCompilationException
    {
        context.registerInstancesByClassifier(parsedResults);
        context.registerFunctionsByName(parsedResults);
    }

    public static void parseM3(String code, ModelRepository repository, ValidationType validationType, M3M4StateListener stateListener, Context context)
    {
        parseM3(code, repository, new ParserLibrary(Lists.immutable.with(new M3AntlrParser(null))), validationType, stateListener, context);
    }

    public static void parseM3(String code, ModelRepository repository, ParserLibrary library, ValidationType validationType, M3M4StateListener stateListener, Context context)
    {
        try
        {
            ListMultimap<Parser, CoreInstance> newInstancesByParser = new TopParser().parse(code, "fromString.pure", repository, library, stateListener, context, null);
            ListIterable<CoreInstance> newInstances = newInstancesByParser.valuesView().toList();

            updateStateFullContextCache(newInstances, context);

            ProcessorSupport processorSupport = new M3ProcessorSupport(context, repository);
            PostProcessor.process(newInstances, repository, library, new InlineDSLLibrary(), null, context, processorSupport, null, null);

            processExcludes(repository, processorSupport, stateListener);

            if (validationType == ValidationType.DEEP)
            {
                repository.validate(stateListener);
            }

            // Validate M3
            stateListener.startValidation();
            Validator.validateM3(newInstances, validationType, library, new InlineDSLLibrary(), new PureCodeStorage(null), repository, context, processorSupport);
            stateListener.finishedValidation();
        }
        catch (PureException e)
        {
            throw new LoaderException(e, code);
        }
    }


    private static void processExcludes(ModelRepository repository, ProcessorSupport processorSupport, M3StateListener listener) throws PureCompilationException
    {
        listener.startProcessingIncludes();
        SetIterable<CoreInstance> excludes = IncrementalCompiler.rebuildExclusionSet(repository, processorSupport);
        listener.finishedProcessingIncludes(excludes);
    }

    public static class LoaderException extends RuntimeException
    {
        private LoaderException(PureException e, String code)
        {
            super(generateMessage(e, code), e);
        }

        @Override
        public PureException getCause()
        {
            return (PureException) super.getCause();
        }

        private static String generateMessage(PureException e, String code)
        {
            String space = "      ";
            return e + " in\n" + space + code.replace("\n", "\n" + space);
        }
    }
}
