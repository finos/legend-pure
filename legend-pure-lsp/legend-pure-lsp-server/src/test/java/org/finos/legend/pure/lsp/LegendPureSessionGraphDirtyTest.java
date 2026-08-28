// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.lsp;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Assert;
import org.junit.Test;

/**
 * Verifies the graphDirty contract that lets ensureCompiled() skip the write lock entirely when
 * nothing is pending: dirty starts false, every successful SourceMutationService mutation (and the
 * immutable-source no-op branches) leaves it false again, and execute() never leaves it dirty behind
 * (so a run of executions never forces the next one back onto the slow/write-lock path).
 */
public class LegendPureSessionGraphDirtyTest
{
    @Test
    public void freshSessionIsNotDirtyAfterInitialize() throws Exception
    {
        LegendPureSession session = new LegendPureSession();
        session.initialize();
        Assert.assertFalse(readGraphDirty(session));
    }

    @Test
    public void successfulModifyAndCompile_leavesGraphNotDirty() throws Exception
    {
        LegendPureSession session = new LegendPureSession();
        session.initialize();

        LegendPureSession.CompileResult result = session.modifyAndCompile(
                "graphDirtyTest.pure", "function graphDirtyTestFn():Any[*]\n{\n  print('hi', 1);\n}\n");
        Assert.assertTrue("compile should succeed, got: "
                        + (result.getError() != null ? result.getError().getMessage() : ""),
                result.isSuccess());
        Assert.assertFalse("graph should be clean immediately after a successful compile", readGraphDirty(session));
    }

    @Test
    public void executeNeverLeavesGraphDirty() throws Exception
    {
        LegendPureSession session = new LegendPureSession();
        session.initialize();
        session.modifyAndCompile("graphDirtyTest2.pure", "function go():Any[*]\n{\n  print('go', 1);\n}\n");
        Assert.assertFalse(readGraphDirty(session));

        LegendPureSession.ExecuteResult result = session.executeGo();
        Assert.assertTrue("go() should execute, got: " + result.getError(), result.isSuccess());
        // ensureCompiled() runs before every execute() - confirm it never leaves dirty=true behind,
        // so a run of back-to-back executions keeps taking the fast (no-write-lock) path.
        Assert.assertFalse(readGraphDirty(session));
    }

    @Test
    public void markGraphDirtyAndMarkGraphCompiled_roundTrip()
    {
        LegendPureSession session = new LegendPureSession();
        session.initialize();
        Assert.assertFalse(readGraphDirty(session));

        session.markGraphDirty();
        Assert.assertTrue(readGraphDirty(session));

        session.markGraphCompiled();
        Assert.assertFalse(readGraphDirty(session));
    }

    private static boolean readGraphDirty(LegendPureSession session)
    {
        try
        {
            Field field = LegendPureSession.class.getDeclaredField("graphDirty");
            field.setAccessible(true);
            return ((AtomicBoolean) field.get(session)).get();
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }
}
