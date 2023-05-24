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

package org.finos.legend.pure.m3.serialization.grammar;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.serialization.runtime.SourceState;
import org.finos.legend.pure.m3.serialization.runtime.binary.reference.ExternalReferenceSerializer;
import org.finos.legend.pure.m3.serialization.runtime.navigation.NavigationHandler;
import org.finos.legend.pure.m3.statelistener.M3M4StateListener;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

public interface Parser extends CoreInstanceFactoriesRegistry
{
    Function<Parser, String> GET_NAME = Parser::getName;
    Function<Parser, RichIterable<MatchRunner>> GET_PROCESSORS = Parser::getProcessors;
    Function<Parser, RichIterable<MatchRunner>> GET_UNLOAD_WALKERS = Parser::getUnLoadWalkers;
    Function<Parser, RichIterable<MatchRunner>> GET_UNLOAD_UNBINDERS = Parser::getUnLoadUnbinders;
    Function<Parser, RichIterable<ExternalReferenceSerializer>> GET_REFERENCE_SERIALIZERS = Parser::getExternalReferenceSerializers;

    /**
     * Parser name.  No two parsers may have the same name.
     *
     * @return parser name
     */
    String getName();

    void parse(String string, String sourceName, boolean addLines, int offset, ModelRepository repository, MutableList<CoreInstance> coreInstancesResult, M3M4StateListener listener, Context context, int count, SourceState oldState) throws PureParserException;

    String parseMapping(String content, String id, String extendsId, String setSourceInfo, boolean root, String classPath, String classSourceInfo, String mappingPath, String sourceName, int offset, String importId, ModelRepository repository, Context context) throws PureParserException;

    /**
     * Return a new instance of the parser within the given
     * parser library.
     *
     * @return new parser instance
     */
    Parser newInstance(ParserLibrary library);

    RichIterable<MatchRunner> getProcessors();

    RichIterable<MatchRunner> getUnLoadWalkers();

    RichIterable<MatchRunner> getUnLoadUnbinders();

    RichIterable<MatchRunner> getValidators();

    RichIterable<NavigationHandler> getNavigationHandlers();

    RichIterable<ExternalReferenceSerializer> getExternalReferenceSerializers();

    /**
     * Return the names of the parsers that are required for this
     * parser.  That is, the parsers which must be present in order
     * for this parser to work.
     *
     * @return required parsers
     */
    SetIterable<String> getRequiredParsers();

    /**
     * Return the Pure files that are required for this parser
     * to work.  That is, the files which must be compiled prior
     * to parsing with this parser.  If there are any order
     * dependencies between these files, then they should be
     * returned in that order.  Only files that are prerequisites
     * specifically for this parser (as opposed to its prerequisite
     * parsers) should be returned.
     *
     * @return required files
     */
    ListIterable<String> getRequiredFiles();
}
