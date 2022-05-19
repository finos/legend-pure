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

package org.finos.legend.pure.m3.serialization.runtime.pattern;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.junit.Assert;
import org.junit.Test;

public class TestURLPatternLibraryOrder
{
    @Test
    public void testOrder()
    {
        MutableList<PurePattern> patterns = Lists.mutable.with(
                new PurePattern(null, "/pure/diagram/{diagramPath}", null, null, Lists.fixedSize.empty()),
                new PurePattern(null, "/pure/diagram/colors", null, null, Lists.fixedSize.empty()),
                new PurePattern(null, "/gggg/ppppppppp/current/account/{accountId}/{user}", null, null, Lists.fixedSize.empty()),
                new PurePattern(null, "/gggg/ppppppppp/current/{user}", null, null, Lists.fixedSize.empty()));

        MutableList<PurePattern> res = patterns.sortThis(URLPatternLibrary.URLPatternComparator);

        Assert.assertEquals(Lists.mutable.with("/pure/diagram/colors",
                "/gggg/ppppppppp/current/account/{accountId}/{user}",
                "/gggg/ppppppppp/current/{user}",
                "/pure/diagram/{diagramPath}"),
                res.collect(PurePattern::getSrcPattern));
    }
}
