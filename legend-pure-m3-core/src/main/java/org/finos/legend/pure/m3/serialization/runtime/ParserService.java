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

import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.serialization.grammar.Parser;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Service Loader to provide parsers from the classpath
 */
public class ParserService
{
    private final ServiceLoader<Parser> loader;
    private final ServiceLoader<InlineDSL> dslsLoader;

    public ParserService()
    {
        this.loader = ServiceLoader.load(Parser.class);
        this.dslsLoader = ServiceLoader.load(InlineDSL.class);
    }

    public ListIterable<Parser> parsers()
    {
        MutableList<Parser> parsers = Lists.mutable.of();
        Iterator<Parser> it = this.loader.iterator();
        while (it.hasNext())
        {
            try
            {
                Parser parser = it.next();
                parsers.add(parser);
            }
            catch (Throwable e)
            {
                // Needs to be silent ... during the build process
            }
        }
        return parsers;
    }

    public ListIterable<InlineDSL> inlineDSLs()
    {
        MutableList<InlineDSL> inlineDSLS = Lists.mutable.of();
        Iterator<InlineDSL> it = this.dslsLoader.iterator();
        while (it.hasNext())
        {
            try
            {
                InlineDSL dsl = it.next();
                inlineDSLS.add(dsl);
            }
            catch (Throwable e)
            {
                // Needs to be silent ... during the build process
            }
        }
        return inlineDSLS;
    }
}
