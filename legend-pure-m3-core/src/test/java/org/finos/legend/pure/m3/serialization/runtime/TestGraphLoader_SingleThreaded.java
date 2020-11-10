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

package org.finos.legend.pure.m3.serialization.runtime;

import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.serialization.grammar.ParserLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m3.serialization.runtime.binary.PureRepositoryJarLibrary;
import org.finos.legend.pure.m3.serialization.runtime.binary.SimplePureRepositoryJarLibrary;
import org.finos.legend.pure.m3.serialization.runtime.pattern.URLPatternLibrary;
import org.finos.legend.pure.m4.ModelRepository;
import org.junit.After;
import org.junit.BeforeClass;

public class TestGraphLoader_SingleThreaded extends TestGraphLoader
{
    @BeforeClass
    public static void setUpAll() {
        setUp();
        IncrementalCompiler compiler = runtime2.getIncrementalCompiler();
        loader = buildGraphLoader(repository2, context2, compiler.getParserLibrary(), compiler.getDslLibrary(), runtime2.getSourceRegistry(), runtime2.getURLPatternLibrary(), SimplePureRepositoryJarLibrary.newLibrary(jars));
    }

    protected static GraphLoader buildGraphLoader(ModelRepository repository, Context context, ParserLibrary parserLibrary, InlineDSLLibrary dslLibrary, SourceRegistry sourceRegistry, URLPatternLibrary patternLibrary, PureRepositoryJarLibrary jarLibrary)
    {
        return new GraphLoader(repository, context, parserLibrary, dslLibrary, sourceRegistry, patternLibrary, jarLibrary);
    }
}
