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

package org.finos.legend.pure.m3.tests;


import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Functions;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.Sets;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.m3.execution.VoidFunctionExecution;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.tools.GraphStatistics;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.junit.Assert;

public class RuntimeVerifier
{

    private static final int DEFAULT_NUMBER_OF_ITERATIONS = 3;

    private RuntimeVerifier()
    {
    }

    public static void deleteCompileAndReloadMultipleTimesIsStable(PureRuntime runtime, FunctionExecution functionExecution,
                                                                   ListIterable<Pair<String, String>> sourceToDelete,
                                                                   ListIterable<? extends FunctionExecutionStateVerifier> additionalVerifiers)
    {
        deleteCompileAndReloadMultipleTimesIsStable(runtime, functionExecution, sourceToDelete, additionalVerifiers, new PureRuntimeContextStateVerifier(), null, null, 0, 0);
    }

    private static void deleteCompileAndReloadMultipleTimesIsStable(PureRuntime runtime, FunctionExecution functionExecution,
                                                                    ListIterable<Pair<String, String>> sourceToDelete,
                                                                    ListIterable<? extends FunctionExecutionStateVerifier> additionalVerifiers,
                                                                    RuntimeStateVerifier contextVerifier,
                                                                    String compileErrorMessage, String sourceId,
                                                                    int row, int column)
    {
        MapIterable<String, String> sourcesToDeleteMap = sourceToDelete.toMap(Functions.<String>firstOfPair(), Functions.<String>secondOfPair());
        verifyOperationIsStable(new RuntimeTestScriptBuilder(), new RuntimeTestScriptBuilder()
                        .deleteSources(sourceToDelete.collect(Functions.<String>firstOfPair()))
                        .compileWithExpectedCompileFailure(compileErrorMessage, sourceId, row, column)
                        .createInMemorySources(sourcesToDeleteMap)
                        .compile(),
                runtime, functionExecution, contextVerifier, additionalVerifiers, false, DEFAULT_NUMBER_OF_ITERATIONS);
    }

    public static void deleteCompileAndReloadMultipleTimesIsStable(PureRuntime runtime, FunctionExecution functionExecution,
                                                                   ListIterable<Pair<String, String>> sourceToDelete,
                                                                   String compileErrorMessage, String sourceId,
                                                                   int row, int column)
    {
        deleteCompileAndReloadMultipleTimesIsStable(runtime, functionExecution, sourceToDelete,
                Lists.fixedSize.<FunctionExecutionStateVerifier>of(), new PureRuntimeContextStateVerifier(), compileErrorMessage, sourceId, row, column);
    }

    public static void replaceWithCompileErrorCompileAndReloadMultipleTimesIsStable(PureRuntime runtime,
                                                                                    ListIterable<Pair<String, String>> sourcesToReplace,
                                                                                    String compileErrorMessage, String sourceIdWithCompileError,
                                                                                    int row, int column)
    {
        replaceWithCompileErrorCompileAndReloadMultipleTimesIsStable(runtime, sourcesToReplace, new PureRuntimeContextStateVerifier(),
                compileErrorMessage, sourceIdWithCompileError, row, column);
    }


    private static void replaceWithCompileErrorCompileAndReloadMultipleTimesIsStable(PureRuntime runtime,
                                                                                     ListIterable<Pair<String, String>> sourcesToReplace, RuntimeStateVerifier contextVerifier,
                                                                                     String compileErrorMessage, String sourceIdWithCompileError,
                                                                                     int row, int column)
    {

        for (Pair<String, String> sourceToReplace : sourcesToReplace)
        {
            String sourceId = sourceToReplace.getOne();
            String originalSource = runtime.getSourceRegistry().getSource(sourceId).getContent();
            String replacementSource = sourceToReplace.getTwo();

            verifyOperationIsStable(new RuntimeTestScriptBuilder(),
                    new RuntimeTestScriptBuilder()
                            .deleteSource(sourceId)
                            .createInMemorySource(sourceId, replacementSource)
                            .compileWithExpectedCompileFailure(compileErrorMessage, sourceIdWithCompileError, row, column)
                            .updateSource(sourceId, originalSource)
                            .compile(),
                    runtime, VoidFunctionExecution.VOID_FUNCTION_EXECUTION, contextVerifier, Lists.fixedSize.<FunctionExecutionStateVerifier>of(), false, DEFAULT_NUMBER_OF_ITERATIONS);
        }

    }

    public static void verifyOperationIsStable(RuntimeActionRunner initialState, RuntimeActionRunner scriptToRun,
                                               PureRuntime runtime, FunctionExecution functionExecution,
                                               ListIterable<? extends FunctionExecutionStateVerifier> additionalVerifiers)
    {
        verifyOperationIsStable(initialState, scriptToRun, runtime, functionExecution, new PureRuntimeContextStateVerifier(), additionalVerifiers, false, DEFAULT_NUMBER_OF_ITERATIONS);
    }

    public static void verifyOperationIsStable(RuntimeActionRunner initialState, RuntimeActionRunner scriptToRun,
                                               PureRuntime runtime, FunctionExecution functionExecution,
                                               ListIterable<? extends FunctionExecutionStateVerifier> additionalVerifiers, boolean debug, int numberOfIterations)
    {
        verifyOperationIsStable(initialState, scriptToRun, runtime, functionExecution, new PureRuntimeContextStateVerifier(), additionalVerifiers, debug, numberOfIterations);
    }

    public static void verifyOperationIsStable(RuntimeActionRunner scriptToRun,
                                               PureRuntime runtime, FunctionExecution functionExecution,
                                               ListIterable<? extends FunctionExecutionStateVerifier> additionalVerifiers, boolean debug, int numberOfIterations)
    {
        verifyOperationIsStable(new RuntimeActionRunner(), scriptToRun, runtime, functionExecution, new PureRuntimeContextStateVerifier(), additionalVerifiers, debug, numberOfIterations);
    }

    private static void verifyOperationIsStable(RuntimeActionRunner initialState, RuntimeActionRunner scriptToRun,
                                                PureRuntime runtime, FunctionExecution functionExecution, RuntimeStateVerifier contextVerifier,
                                                ListIterable<? extends FunctionExecutionStateVerifier> additionalVerifiers, boolean debug, int numberOfIterations)
    {
        TrackingTransactionObserver observer = null;
        byte[] before = null;
        MapIterable<String,Integer> instanceByClassifierCountsBefore = null;
        initialState.run(runtime, functionExecution);

        if (debug)
        {
            observer = new TrackingTransactionObserver(runtime.getModelRepository());
            runtime.getModelRepository().setTransactionObserver(observer);
            before = runtime.getModelRepository().serialize();
            instanceByClassifierCountsBefore = runtime.getContext().countInstancesByClassifier();
        }

        PureRuntimeStateVerifier runtimeStateVerifier = new PureRuntimeStateVerifier(contextVerifier);

        runtimeStateVerifier.snapshotState(runtime);


        for (FunctionExecutionStateVerifier verifier : additionalVerifiers)
        {
            verifier.snapshotState(functionExecution);
        }


        for (int i = 0; i < numberOfIterations; i++)
        {
            scriptToRun.run(runtime, functionExecution);

            if (debug)
            {
                MapIterable<String,Integer> instanceByClassifierCountsAfter = runtime.getContext().countInstancesByClassifier();
                for (String key : instanceByClassifierCountsAfter.keysView())
                {
                    Integer beforeC = instanceByClassifierCountsBefore.get(key);
                    Integer afterC = instanceByClassifierCountsAfter.get(key);
                    Assert.assertEquals(key, beforeC, afterC);
                }

                //PLEASE NOTE - these lines need to be commented out if running a delete operation. Deletes are non-transactional
                observer.compareToModifiedBefore(runtime.getModelRepository());
                observer.compareToAdded(runtime.getModelRepository());

                byte[] after = runtime.getModelRepository().serialize();
                TrackingTransactionObserver.compareBytes(before, after);
            }

            runtimeStateVerifier.assertStateSame(runtime);
            for (FunctionExecutionStateVerifier additionalVerifier : additionalVerifiers)
            {
                additionalVerifier.assertStateSame(functionExecution);
            }
        }
    }

    public static void compareCoreInstanceMap(SetIterable<CoreInstance> expected, SetIterable<CoreInstance> actual)
    {
        MutableSet<CoreInstance> actualNotPartOfExpected = Sets.mutable.of();

        for (CoreInstance instance : actual)
        {
            if (!expected.contains(instance))
            {
                actualNotPartOfExpected.add(instance);
            }
        }

        MutableSet<CoreInstance> expectedNotPartOfActual = Sets.mutable.of();

        for (CoreInstance originalInstance : expected)
        {
            if (!actual.contains(originalInstance))
            {
                expectedNotPartOfActual.add(originalInstance);
            }
        }

        if (expectedNotPartOfActual.size() != actualNotPartOfExpected.size())
        {
            System.out.println("Expected:");
            for (CoreInstance instance : expectedNotPartOfActual)
            {
                System.out.println("Core Instance:" + instance.getSyntheticId() + ":" + instance.getName() + ":" + instance.getClassifier().getName());
            }
            System.out.println("Actual:");
            for (CoreInstance instance : actualNotPartOfExpected)
            {
                System.out.println("Core Instance:" + instance.getSyntheticId() + ":" + instance.getName() + ":" + instance.getClassifier().getName());
            }
        }

        Assert.assertEquals(expectedNotPartOfActual.size(), actualNotPartOfExpected.size());
    }

    public interface RuntimeStateVerifier
    {
        void snapshotState(PureRuntime pureRuntime);

        void assertStateSame(PureRuntime pureRuntime);
    }

    public interface FunctionExecutionStateVerifier
    {
        void snapshotState(FunctionExecution functionExecution);

        void assertStateSame(FunctionExecution functionExecution);
    }

    public static class PureRuntimeStateVerifier implements RuntimeStateVerifier
    {
        private ObjectIntMap<String> instanceCountByClassifier;
        private int repositorySize;
        private int sourceCount;
        private final RuntimeStateVerifier contextStateVerifier;

        public PureRuntimeStateVerifier()
        {
            this(new PureRuntimeContextStateVerifier());
        }


        PureRuntimeStateVerifier(RuntimeStateVerifier contextStateVerifier)
        {
            this.contextStateVerifier = contextStateVerifier;
        }


        @Override
        public void snapshotState(PureRuntime pureRuntime)
        {
            this.instanceCountByClassifier = GraphStatistics.instanceCountByClassifierPathAsMap(pureRuntime.getModelRepository());
            this.repositorySize = pureRuntime.getModelRepository().serialize().length;
            this.sourceCount = pureRuntime.getSourceRegistry().getSources().size();
            this.contextStateVerifier.snapshotState(pureRuntime);
        }

        @Override
        public void assertStateSame(PureRuntime pureRuntime)
        {
            ObjectIntMap<String> instanceCountByClassifierAfter = GraphStatistics.instanceCountByClassifierPathAsMap(pureRuntime.getModelRepository());
            if (!this.instanceCountByClassifier.equals(instanceCountByClassifierAfter))
            {
                StringBuilder message = new StringBuilder("Mismatch in instance counts by classifier:\n");
                GraphStatistics.writeInstanceCountsByClassifierPathDeltas(message, "\t", "before", this.instanceCountByClassifier, "after", instanceCountByClassifierAfter);
                Assert.fail(message.toString());
            }
            int sizeAfter = pureRuntime.getModelRepository().serialize().length;
            Assert.assertEquals("Memory Before (bytes):" + this.repositorySize + " Memory After (bytes):" + sizeAfter + " Delta:" + (sizeAfter - this.repositorySize), this.repositorySize, sizeAfter);
            Assert.assertEquals("Source is not registered", this.sourceCount, pureRuntime.getSourceRegistry().getSources().size());

            this.contextStateVerifier.assertStateSame(pureRuntime);
        }
    }

    static class PureRuntimeContextStateVerifier implements RuntimeStateVerifier
    {
        private int instancesByClassifier;

        @Override
        public void snapshotState(PureRuntime pureRuntime)
        {
            this.instancesByClassifier = pureRuntime.getContext().getAllInstances().size();
        }

        @Override
        public void assertStateSame(PureRuntime pureRuntime)
        {
            Assert.assertEquals(this.instancesByClassifier, pureRuntime.getContext().getAllInstances().size());
        }
    }

    static class DoNothingVerifier implements RuntimeStateVerifier
    {
        @Override
        public void snapshotState(PureRuntime pureRuntime)
        {
        }

        @Override
        public void assertStateSame(PureRuntime pureRuntime)
        {
        }
    }
}
