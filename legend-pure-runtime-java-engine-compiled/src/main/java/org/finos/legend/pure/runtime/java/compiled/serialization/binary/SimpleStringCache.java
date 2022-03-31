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

package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m4.serialization.Writer;

import java.util.Objects;

class SimpleStringCache extends StringCache
{
    private SimpleStringCache(ListIterable<String> classifierIds, ListIterable<String> otherStrings)
    {
        super(classifierIds, otherStrings);
    }

    public void write(Writer writer)
    {
        writer.writeStringArray(getClassifierStringArray());
        writer.writeStringArray(getOtherStringsArray());
    }

    public static Builder<SimpleStringCache> newBuilder()
    {
        return new SimpleStringCacheBuilder();
    }

    private static class SimpleStringCacheBuilder extends Builder<SimpleStringCache>
    {
        private final MutableSet<String> classifierIds = Sets.mutable.empty();
        private final MutableSet<String> otherStrings = Sets.mutable.empty();

        @Override
        public SimpleStringCache build()
        {
            MutableList<String> classifierIds = this.classifierIds.toSortedList();
            MutableList<String> otherStrings = this.otherStrings.asLazy().reject(this.classifierIds::contains).toSortedList();
            return new SimpleStringCache(classifierIds, otherStrings);
        }

        @Override
        protected void collectObj(String classifierId, String identifier, String name)
        {
            addClassifierId(classifierId);
            addOtherString(identifier);
            addOtherString(name);
        }

        @Override
        protected void collectSourceId(String sourceId)
        {
            addOtherString(sourceId);
        }

        @Override
        protected void collectProperty(String property)
        {
            addOtherString(property);
        }

        @Override
        protected void collectRef(String classifierId, String identifier)
        {
            addClassifierId(classifierId);
            addOtherString(identifier);
        }

        @Override
        protected void collectPrimitiveString(String string)
        {
            addOtherString(string);
        }

        private void addClassifierId(String string)
        {
            this.classifierIds.add(Objects.requireNonNull(string));
        }

        private void addOtherString(String string)
        {
            if (string != null)
            {
                this.otherStrings.add(string);
            }
        }
    }
}
