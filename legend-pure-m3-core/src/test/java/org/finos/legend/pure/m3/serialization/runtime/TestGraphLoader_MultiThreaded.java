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
import org.finos.legend.pure.m3.serialization.runtime.pattern.URLPatternLibrary;
import org.finos.legend.pure.m4.ModelRepository;

import java.util.concurrent.ForkJoinPool;

public class TestGraphLoader_MultiThreaded extends TestGraphLoader
{
    @Override
    protected GraphLoader buildGraphLoader(ModelRepository repository, Context context, ParserLibrary parserLibrary, InlineDSLLibrary dslLibrary, SourceRegistry sourceRegistry, URLPatternLibrary patternLibrary, PureRepositoryJarLibrary jarLibrary)
    {
        return new GraphLoader(repository, context, parserLibrary, dslLibrary, sourceRegistry, patternLibrary, jarLibrary, new ForkJoinPool());
    }
}
