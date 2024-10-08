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

package org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;

public class InlineDSLLibrary
{
    private final MutableMap<String, InlineDSL> inlineDSLs = Maps.mutable.with();

    public InlineDSLLibrary()
    {
    }

    public InlineDSLLibrary(Iterable<? extends InlineDSL> parsers)
    {
        parsers.forEach(this::registerInLineDSL);
    }

    public void registerInLineDSL(InlineDSL inlineDSL)
    {
        this.inlineDSLs.put(inlineDSL.getName(), inlineDSL);
    }

    public RichIterable<InlineDSL> getInlineDSLs()
    {
        return this.inlineDSLs.valuesView();
    }

    public RichIterable<String> getInlineDSLNames()
    {
        return this.inlineDSLs.keysView();
    }
}
