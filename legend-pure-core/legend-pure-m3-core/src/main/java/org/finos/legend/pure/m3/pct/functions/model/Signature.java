// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m3.pct.functions.model;

public class Signature
{
    public String simple;
    public String id;
    public String documentation;
    public String grammarDoc;
    public String grammarCharacter;
    public boolean platformOnly;

    public Signature()
    {
    }

    public Signature(String id, String simple, boolean platformOnly, String documentation, String grammarDoc, String grammarCharacter)
    {
        this.simple = simple;
        this.id = id;
        this.documentation = documentation;
        this.grammarDoc = grammarDoc;
        this.grammarCharacter = grammarCharacter;
        this.platformOnly = platformOnly;
    }
}
