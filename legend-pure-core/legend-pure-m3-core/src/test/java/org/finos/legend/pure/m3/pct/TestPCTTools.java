// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m3.pct;

import org.finos.legend.pure.m3.pct.shared.PCTTools;
import org.junit.Assert;
import org.junit.Test;

public class TestPCTTools
{
    @Test
    public void messageCleaned()
    {
        String msg = PCTTools.getMessageFromError(new Exception("Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), \"\n" +
                "expected: '#TDS\\n   p,o,i,newCol\\n   300,2,20,30\\n   300,1,10,30\\n   200,3,30,80\\n   200,3,30,80\\n   200,1,10,80\\n   200,1,10,80\\n   100,3,30,60\\n   100,2,20,60\\n   100,1,10,60\\n   0,1,10,20\\n   0,1,10,20\\n#'\n" +
                "actual:   '#TDS\\n   p,o,i,newCol\\n   300,2,20,30\\n   300,1,10,30\\n   200,3,30,110\\n   200,3,30,110\\n   200,1,10,110\\n   200,1,10,110\\n   100,3,30,110\\n   100,2,20,110\\n   100,1,10,110\\n   0,1,10,50\\n   0,1,10,50\\n#'\""));

        Assert.assertEquals("Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), \"\n" +
                "expected: '#TDS\n   p,o,i,newCol\n   300,2,20,30\n   300,1,10,30\n   200,3,30,80\n   200,3,30,80\n   200,1,10,80\n   200,1,10,80\n   100,3,30,60\n   100,2,20,60\n   100,1,10,60\n   0,1,10,20\n   0,1,10,20\n#'\n" +
                "actual:   '#TDS\n   p,o,i,newCol\n   300,2,20,30\n   300,1,10,30\n   200,3,30,110\n   200,3,30,110\n   200,1,10,110\n   200,1,10,110\n   100,3,30,110\n   100,2,20,110\n   100,1,10,110\n   0,1,10,50\n   0,1,10,50\n#'\"", msg);
    }
}
