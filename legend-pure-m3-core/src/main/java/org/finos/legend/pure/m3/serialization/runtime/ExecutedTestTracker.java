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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.block.factory.Functions0;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.eclipse.collections.impl.tuple.Tuples;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Map;

/**
 * For keeping track of the tests a user has executed during their session, to ensure that all impacted tests pass before
 * they may commit their changes.
 */
public class ExecutedTestTracker
{
    public enum TestState {Passed, Failed, NotYetExecuted}

    private MutableMap<String, TestState> tests;
    private final Path path;
    private final String activeUserDBDirectory;
    private final Object lock = new Object();

    public ExecutedTestTracker(Path path, String activeUserDBDirectory)
    {
        this.path = path;
        this.activeUserDBDirectory = activeUserDBDirectory;
        this.tests = this.readFromFile().getIfAbsent(activeUserDBDirectory, Functions0.<String, TestState>newUnifiedMap());
    }

    public MutableMap<String, TestState> getTests()
    {
        return this.tests;
    }

    private void noteTestsState(UnifiedSet<String> newTests, TestState state)
    {
        this.tests.putAll(newTests.toMap(Functions.<String>identity(), Functions.getFixedValue(state)));
    }

    /**
     * Store the state of a set of tests after it has been determined that they pass under the current set of changes
     */
    public void notePassingTests(UnifiedSet<String> passedTests)
    {
        this.noteTestsState(passedTests, TestState.Passed);
    }

    /**
     * Store the state of a set of tests after it has been determined that they fail under the current set of changes
     */
    public void noteFailingTests(UnifiedSet<String> failedTests)
    {
        this.noteTestsState(failedTests, TestState.Failed);
    }

    /**
     * Whenever a change is made to the set of changed files e.g. saving, reverting, deleting, etc the stored state of
     * executed tests must be invalidated as it cannot be known whether the prior state for any given test still stands
     */
    void invalidate()
    {
        this.tests = new UnifiedMap<>();
    }

    private static final Function2<String, String, TestState> testDeserializer = new Function2<String, String, TestState>()
    {
        @Override
        public TestState value(String testName, String state)
        {
            return TestState.valueOf(state);
        }
    };

    private static final Function2<String, Map<? extends String, ? extends String>, MutableMap<String, TestState>> userDbsTestsDeserializer = new Function2<String, Map<? extends String, ? extends String>, MutableMap<String, TestState>>()
    {
        @Override
        public MutableMap<String, TestState> value(String usedDbName, Map<? extends String, ? extends String> data)
        {
            UnifiedMap<? extends String, ? extends String> map = new UnifiedMap<>(data);
            return (MutableMap<String, TestState>)map.collectValues(testDeserializer);
        }
    };

    private static final Function2<String, TestState, String> testSerializer = new Function2<String, TestState, String>()
    {
        @Override
        public String value(String testName, TestState state)
        {
            return state.name();
        }
    };

    private static final Function2<String, MutableMap<String, TestState>, MutableMap<String, String>> userDbsTestsSerializer = new Function2<String, MutableMap<String, TestState>, MutableMap<String, String>>()
    {
        @Override
        public MutableMap<String, String> value(String usedDbName, MutableMap<String, TestState> data)
        {
            return new UnifiedMap<>(data).collectValues(testSerializer);
        }
    };

    private MutableMap<String, MutableMap<String, TestState>> readFromFile()
    {
        try
        {
            if (Files.exists(this.path))
            {
                try (BufferedReader reader = Files.newBufferedReader(this.path, Charset.defaultCharset()))
                {
                    return (MutableMap<String, MutableMap<String, TestState>>)new UnifiedMap<>((Map<? extends String, ? extends Map<? extends String, ? extends String>>)new JSONParser().parse(reader)).collectValues(userDbsTestsDeserializer);
                }
            }
            else
            {
                return new UnifiedMap<>();
            }
        }
        catch (Exception e)
        {
            return new UnifiedMap<>();
        }
    }

    private void writeDataToFile(MutableMap<String, MutableMap<String, TestState>> data)
    {
        try
        {
            if (Files.notExists(this.path))
            {
                Files.createDirectories(this.path.getParent());
                Files.createFile(this.path);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Exception while loading " + this.path, e);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(this.path, Charset.defaultCharset()))
        {
            synchronized (this.lock)
            {
                new JSONObject(data.collectValues(userDbsTestsSerializer)).writeJSONString(writer);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Exception while loading " + this.path, e);
        }
    }

    public void writeToFile()
    {
        MutableMap<String, MutableMap<String, TestState>> currentlyStored = this.readFromFile();
        currentlyStored.put(this.activeUserDBDirectory, this.tests);
        this.writeDataToFile(currentlyStored);
    }

    public JSONObject summaryOfImpactedTests(MutableSet<String> impactedTests)
    {
        if(impactedTests.isEmpty())
        {
            JSONObject noImpacts = new JSONObject();
            noImpacts.put(TestState.Passed.name(), "n/a");
            noImpacts.put(TestState.Failed.name(), "n/a");
            noImpacts.put(TestState.NotYetExecuted.name(), "n/a");
            return noImpacts;
        }

        final MutableMap<String, TestState> tests = this.tests;

        MutableMap<TestState, Integer> count = impactedTests.toMap(Functions.<String>identity(), new Function<String, TestState>()
        {
            @Override
            public TestState valueOf(String s)
            {
                return tests.getIfAbsent(s, Functions0.value(TestState.NotYetExecuted));
            }
        }).aggregateBy(Functions.<TestState>identity(), Functions0.value(0), new Function2<Integer, TestState, Integer>()
        {
            @Override
            public Integer value(Integer count, TestState testState)
            {
                return count + 1;
            }
        });

        final Double totalCount = (double) count.getIfAbsentPut(TestState.Passed, 0)
                + count.getIfAbsentPut(TestState.Failed, 0)
                + count.getIfAbsentPut(TestState.NotYetExecuted, 0);

        return new JSONObject(count.collect(new Function2<TestState, Integer, Pair<String, String>>()
        {
            @Override
            public Pair<String, String> value(TestState testState, Integer count)
            {
                NumberFormat df = DecimalFormat.getPercentInstance();
                return Tuples.pair(testState.name(), df.format(count / (double) totalCount));
            }
        }));
    }

    public boolean allImpactedTestsPass(MutableSet<String> impactedTests)
    {
        if(impactedTests.isEmpty())
        {
            return true;
        }

        final MutableMap<String, TestState> tests = this.tests;

        MutableSet<TestState> impactedTestStates = impactedTests.collect(new Function<String, TestState>()
        {
            @Override
            public TestState valueOf(String s)
            {
                return tests.getIfAbsent(s, Functions0.value(TestState.NotYetExecuted));
            }
        });

        MutableSet<TestState> passes = new UnifiedSet<TestState>().with(TestState.Passed);
        return impactedTestStates.equals(passes);
    }

    public void forgetTestsForUserDb(String userDbDirectory)
    {
        MutableMap<String, MutableMap<String, TestState>> currentlyStored = this.readFromFile();
        currentlyStored.remove(userDbDirectory);
        this.writeDataToFile(currentlyStored);
    }
}
