// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.m2.relational.serialization.grammar.v1.antlr;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;

public class AsciiNodeBuilder
{
    public String text;
    private MutableList<AsciiNodeBuilder> children = FastList.newList();

    public AsciiNodeBuilder(String text)
    {
        this.text = text;
    }

    public void add(AsciiNodeBuilder child)
    {
        this.children.add(child);
    }

    public String build()
    {
        return this.text+", childrenData =["+children.collect(new Function<AsciiNodeBuilder, String>()
        {
            @Override
            public String valueOf(AsciiNodeBuilder asciiNodeBuilder)
            {
                return asciiNodeBuilder.build();
            }
        }).makeString(",")+"])";
    }
}
