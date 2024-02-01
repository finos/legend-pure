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

package org.finos.legend.pure.m3.serialization.grammar;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.runtime.navigation.NavigationHandler;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.util.Comparator;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class ParserLibrary
{
    private static final Comparator<Parser> PARSER_COMPARATOR = new Comparator<Parser>()
    {
        @Override
        public int compare(Parser parser1, Parser parser2)
        {
            if (parser1 == parser2)
            {
                return 0;
            }

            String name1 = parser1.getName();
            String name2 = parser2.getName();
            if (name1.equals(name2))
            {
                return 0;
            }

            // TODO maybe check transitively
            SetIterable<String> requiredParsers1 = parser1.getRequiredParsers();
            SetIterable<String> requiredParsers2 = parser2.getRequiredParsers();
            if (requiredParsers2.contains(name1))
            {
                return -1;
            }
            else if (requiredParsers1.contains(name2))
            {
                return 1;
            }
            else
            {
                return name1.compareTo(name2);
            }
        }
    };

    private final ImmutableMap<String, Parser> parsers;
    private final ImmutableMap<String, NavigationHandler> navigationHandlers;

    public ParserLibrary(Iterable<? extends Parser> parsers)
    {
        this.parsers = indexParsersByName(parsers);
        this.navigationHandlers = indexNavigationHandlers(this.parsers.valuesView());
    }

    public ParserLibrary()
    {
        this.parsers = Maps.immutable.empty();
        this.navigationHandlers = Maps.immutable.empty();
    }

    public RichIterable<Parser> getParsers()
    {
        return this.parsers.valuesView();
    }

    public Parser getParser(String name)
    {
        Parser parser = this.parsers.get(name);
        if (parser == null)
        {
            throw new RuntimeException("'" + name + "' is not a known parser. Known parsers are: " + this.parsers.keysView().toSortedList().makeString(", "));
        }
        return this.parsers.get(name).newInstance(this);
    }

    /**
     * Validate the parser library.
     *
     * @throws InvalidParserLibraryException if the parser library is invalid
     */
    public void validate() throws InvalidParserLibraryException
    {
        MutableSet<String> errorMessages = Sets.mutable.with();
        for (Parser parser : this.parsers)
        {
            for (String requiredParserName : parser.getRequiredParsers())
            {
                if (!this.parsers.containsKey(requiredParserName))
                {
                    errorMessages.add("Parser '" + requiredParserName + "' is required by parser '" + parser.getName() + "' but is not present.");
                }
            }
        }

        if (errorMessages.notEmpty())
        {
            throw new InvalidParserLibraryException(errorMessages);
        }
    }

    public ListIterable<String> getRequiredFiles()
    {
        MutableList<String> files = Lists.mutable.with();
        for (Parser parser : this.parsers.valuesView().toSortedList(PARSER_COMPARATOR))
        {
            for (String file : parser.getRequiredFiles())
            {
                if (!files.contains(file))
                {
                    files.add(file);
                }
            }
        }
        return files;
    }

    public Pair<Object, RelationType> resolveRelationElementAccessor(PackageableElement element, MutableList<? extends String> path, SourceInformation sourceInformation, ProcessorSupport processorSupport)
    {
        MutableList<Pair<Object, RelationType>> res = this.parsers.valuesView().collect(c -> c.resolveRelationElementAccessor(element, path, sourceInformation, processorSupport)).select(Objects::nonNull).toList();
        assertEquals("Found 0 ore more than one (" + res.size() + ") handlers for resolving the element Accessor " + element.getName() + " " + path.makeString("."), 1, res.size());
        return res.get(0);
    }

    public class InvalidParserLibraryException extends Exception
    {
        private final RichIterable<String> messages;

        private InvalidParserLibraryException(RichIterable<String> messages)
        {
            this.messages = messages;
        }

        @Override
        public String getMessage()
        {
            return "Invalid parser library." + this.messages.makeString("\n\t", "\n\t", "");
        }
    }

    public NavigationHandler getNavigationHandler(String typePath)
    {
        return this.navigationHandlers.get(typePath);
    }

    private static ImmutableMap<String, Parser> indexParsersByName(Iterable<? extends Parser> parsers)
    {
        MutableMap<String, Parser> index = Maps.mutable.empty();
        for (Parser parser : parsers)
        {
            Parser old = index.put(parser.getName(), parser);
            if ((old != null) && (parser != old))
            {
                throw new IllegalArgumentException("Duplicate parsers with name: " + parser.getName());
            }
        }
        return index.toImmutable();
    }

    private static ImmutableMap<String, NavigationHandler> indexNavigationHandlers(RichIterable<Parser> parsers)
    {
        if (parsers.isEmpty())
        {
            return Maps.immutable.empty();
        }

        MutableMap<String, NavigationHandler> index = Maps.mutable.empty();
        for (Parser parser : parsers)
        {
            for (NavigationHandler handler : parser.getNavigationHandlers())
            {
                NavigationHandler old = index.put(handler.getClassName(), handler);
                if ((old != null) && (old != handler))
                {
                    throw new RuntimeException("Multiple navigation handlers for " + handler.getClassName());
                }
            }
        }
        return index.toImmutable();
    }
}
