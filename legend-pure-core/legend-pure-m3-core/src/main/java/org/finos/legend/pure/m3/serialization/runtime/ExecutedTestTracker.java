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

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.tuple.Tuples;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
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
    public enum TestState
    {
        Passed, Failed, NotYetExecuted
    }

    private MutableMap<String, TestState> tests;
    private final Path path;
    private final String activeUserDBDirectory;
    private final Object lock = new Object();

    public ExecutedTestTracker(Path path, String activeUserDBDirectory)
    {
        this.path = path;
        this.activeUserDBDirectory = activeUserDBDirectory;
        this.tests = readFromFile().getIfAbsent(activeUserDBDirectory, Maps.mutable::empty);
    }

    public MutableMap<String, TestState> getTests()
    {
        return this.tests;
    }

    private void noteTestsState(SetIterable<String> newTests, TestState state)
    {
        newTests.forEach(t -> this.tests.put(t, state));
    }

    /**
     * Store the state of a set of tests after it has been determined that they pass under the current set of changes
     */
    public void notePassingTests(SetIterable<String> passedTests)
    {
        noteTestsState(passedTests, TestState.Passed);
    }

    /**
     * Store the state of a set of tests after it has been determined that they fail under the current set of changes
     */
    public void noteFailingTests(SetIterable<String> failedTests)
    {
        noteTestsState(failedTests, TestState.Failed);
    }

    /**
     * Whenever a change is made to the set of changed files e.g. saving, reverting, deleting, etc the stored state of
     * executed tests must be invalidated as it cannot be known whether the prior state for any given test still stands
     */
    void invalidate()
    {
        this.tests = Maps.mutable.empty();
    }

    @SuppressWarnings("unchecked")
    private MutableMap<String, MutableMap<String, TestState>> readFromFile()
    {
        if (Files.exists(this.path))
        {
            try (BufferedReader reader = Files.newBufferedReader(this.path, StandardCharsets.UTF_8))
            {
                Map<? extends String, ? extends Map<? extends String, ? extends String>> json = (Map<? extends String, ? extends Map<? extends String, ? extends String>>) new JSONParser().parse(reader);
                MutableMap<String, MutableMap<String, TestState>> result = Maps.mutable.ofInitialCapacity(json.size());
                json.forEach((userDbName, data) ->
                {
                    MutableMap<String, TestState> transformed = Maps.mutable.ofInitialCapacity(data.size());
                    data.forEach((name, state) -> transformed.put(name, TestState.valueOf(state)));
                    result.put(userDbName, transformed);
                });
                return result;
            }
            catch (Exception ignore)
            {
                // ignore exception and return empty map
            }
        }
        return Maps.mutable.empty();
    }

    private void writeDataToFile(MutableMap<String, MutableMap<String, TestState>> data)
    {
        try
        {
            synchronized (this.lock)
            {
                Map<String, Map<String, String>> serialized = data.collectValues((n, d) -> d.collectValues((testName, state) -> state.name()));
                Files.createDirectories(this.path.getParent());
                try (BufferedWriter writer = Files.newBufferedWriter(this.path, StandardCharsets.UTF_8))
                {
                    JSONObject.writeJSONString(serialized, writer);
                }
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Exception while writing data to " + this.path, e);
        }
    }

    public void writeToFile()
    {
        MutableMap<String, MutableMap<String, TestState>> currentlyStored = this.readFromFile();
        currentlyStored.put(this.activeUserDBDirectory, this.tests);
        this.writeDataToFile(currentlyStored);
    }

    @SuppressWarnings("unchecked")
    public JSONObject summaryOfImpactedTests(MutableSet<String> impactedTests)
    {
        if (impactedTests.isEmpty())
        {
            JSONObject noImpacts = new JSONObject();
            noImpacts.put(TestState.Passed.name(), "n/a");
            noImpacts.put(TestState.Failed.name(), "n/a");
            noImpacts.put(TestState.NotYetExecuted.name(), "n/a");
            return noImpacts;
        }

        MutableMap<TestState, Integer> count = impactedTests.toMap(t -> t, s -> this.tests.getIfAbsentValue(s, TestState.NotYetExecuted))
                .aggregateBy(t -> t, () -> 0, (count12, testState) -> count12 + 1);

        double totalCount = (double) count.getIfAbsentPut(TestState.Passed, 0)
                + count.getIfAbsentPut(TestState.Failed, 0)
                + count.getIfAbsentPut(TestState.NotYetExecuted, 0);

        return new JSONObject(count.collect((testState, count1) ->
        {
            NumberFormat df = DecimalFormat.getPercentInstance();
            return Tuples.pair(testState.name(), df.format(count1 / totalCount));
        }));
    }

    public boolean allImpactedTestsPass(MutableSet<String> impactedTests)
    {
        if (impactedTests.isEmpty())
        {
            return true;
        }

        MutableSet<TestState> impactedTestStates = impactedTests.collect(s -> this.tests.getIfAbsentValue(s, TestState.NotYetExecuted));
        return impactedTestStates.equals(Sets.fixedSize.with(TestState.Passed));
    }

    public void forgetTestsForUserDb(String userDbDirectory)
    {
        MutableMap<String, MutableMap<String, TestState>> currentlyStored = this.readFromFile();
        currentlyStored.remove(userDbDirectory);
        this.writeDataToFile(currentlyStored);
    }
}
