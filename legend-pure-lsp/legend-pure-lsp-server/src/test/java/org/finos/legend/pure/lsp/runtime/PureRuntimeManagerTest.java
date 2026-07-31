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

package org.finos.legend.pure.lsp.runtime;

import org.junit.Assert;
import org.junit.Test;

public class PureRuntimeManagerTest
{
    @Test
    public void describeFailure_withMessage_prependsSimpleClassName()
    {
        Assert.assertEquals("IllegalStateException: boom",
                PureRuntimeManager.describeFailure(new IllegalStateException("boom")));
    }

    @Test
    public void describeFailure_nullMessage_usesSimpleClassNameOnly()
    {
        Assert.assertEquals("NullPointerException",
                PureRuntimeManager.describeFailure(new NullPointerException()));
    }

    @Test
    public void describeFailure_emptyMessage_usesSimpleClassNameOnly()
    {
        Assert.assertEquals("RuntimeException",
                PureRuntimeManager.describeFailure(new RuntimeException("")));
    }

    @Test
    public void describeFailure_error_isDescribedNotJustExceptions()
    {
        // Errors (e.g. a classpath LinkageError) must still be turned into a status message -
        // this is why the catch clauses widened from Exception to Throwable.
        Assert.assertEquals("NoClassDefFoundError: com/foo/Bar",
                PureRuntimeManager.describeFailure(new NoClassDefFoundError("com/foo/Bar")));
    }
}
