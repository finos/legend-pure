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

package org.finos.legend.pure.m3.bootstrap.generator;

import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.factory.Sets;

public class StubDef
{
    private final String className;
    private final String stubType;
    private final SetIterable<String> owners;

    private StubDef(String className, String stubType, SetIterable<String> owners)
    {
        this.className = className;
        this.stubType = stubType;
        this.owners = owners;
    }

    public static StubDef build(String className, String stubType)
    {
        return new StubDef(className, stubType, Sets.immutable.<String>empty());
    }

    static StubDef build(String className, String stubType, SetIterable<String> owners)
    {
        return new StubDef(className, stubType, owners);
    }

    public String getClassName()
    {
        return this.className;
    }

    String getStubType()
    {
        return this.stubType;
    }

    boolean isOwner(String className)
    {
        return this.owners.contains(className);
    }
}
