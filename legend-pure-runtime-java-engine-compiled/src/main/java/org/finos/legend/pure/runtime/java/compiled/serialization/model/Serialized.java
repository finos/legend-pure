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

package org.finos.legend.pure.runtime.java.compiled.serialization.model;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.tuple.Pair;

public class Serialized
{
    private final ListIterable<Obj> objects;
    private final ListIterable<Pair<Obj, Obj>> packageLinks;

    public Serialized(ListIterable<Obj> objects, ListIterable<Pair<Obj, Obj>> packageLinks)
    {
        this.objects = objects;
        this.packageLinks = packageLinks;
    }

    public ListIterable<Obj> getObjects()
    {
        return this.objects;
    }

    public ListIterable<Pair<Obj, Obj>> getPackageLinks()
    {
        return this.packageLinks;
    }
}
