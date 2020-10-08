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

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.list.mutable.FastList;

public class Serialized
{
    private MutableList<Obj> objects;
    private MutableList<Pair<Obj, Obj>> packageLinks;

    public Serialized()
    {
        this.objects = FastList.newList();
        this.packageLinks = FastList.newList();
    }

    public Serialized(MutableList<Obj> objects,  MutableList<Pair<Obj, Obj>> packageLinks)
    {
        this.objects = objects;
        this.packageLinks = packageLinks;
    }

    public MutableList<Obj> getObjects()
    {
        return this.objects;
    }

    public MutableList<Pair<Obj, Obj>> getPackageLinks()
    {
        return this.packageLinks;
    }

    public void addAll(Serialized serialized)
    {
        this.objects.addAll(serialized.objects);
        this.packageLinks.addAll(serialized.packageLinks);
    }
}
