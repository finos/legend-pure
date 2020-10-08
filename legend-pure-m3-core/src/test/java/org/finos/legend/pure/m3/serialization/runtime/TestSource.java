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

import java.util.regex.Pattern;

import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.test.Verify;
import org.junit.Assert;
import org.junit.Test;

public class TestSource
{
    @Test
    public void testFindString()
    {
        Source source = new Source("testSource.pure", false, false, "the quick brown fox\njumped over the\nlazy dog\n\nthe\nquick\nbrown\nfox\njumped\nover\nthe\nlazy\ndog\n");
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates("testSource.pure", 1, 5, 1, 10), new SourceCoordinates("testSource.pure", 6, 1, 6, 6)), source.find("quick").toSet());
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates("testSource.pure", 2, 1, 2, 12)), source.find("jumped over").toSet());
        Assert.assertEquals(Sets.immutable.with(), source.find("jumped over the lazy dog").toSet());
        Assert.assertEquals(Sets.immutable.with(), source.find("$").toSet());
    }

    @Test
    public void testFindStringCaseInsensitive()
    {
        Source source = new Source("testSource.pure", false, false, "The Quick Brown Fox\nJumped Over The\nLazy Dog\n\nTHE\nQUICK\nBROWN\nFOX\nJUMPED\nOVER\nTHE\nLAZY\nDOG\n");
        Assert.assertEquals(Sets.immutable.with(), source.find("quick", true).toSet());
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates("testSource.pure", 1, 5, 1, 10), new SourceCoordinates("testSource.pure", 6, 1, 6, 6)), source.find("quick", false).toSet());
        Assert.assertEquals(Sets.immutable.with(), source.find("JUMPED OVER", true).toSet());
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates("testSource.pure", 2, 1, 2, 12)), source.find("JUMPED OVER", false).toSet());
    }

    @Test
    public void testFindEmptyString()
    {
        Source source = new Source("testSource.pure", false, false, "the quick brown fox\njumped over the\nlazy dog\n\nthe\nquick\nbrown\nfox\njumped\nover\nthe\nlazy\ndog\n");
        Verify.assertEmpty(source.find("", true));
        Verify.assertEmpty(source.find("", false));
    }

    @Test
    public void testFindRegex()
    {
        Source source = new Source("testSource.pure", false, false, "the quick brown fox\njumped over the\nlazy dog\n\nthe\nquick\nbrown\nfox\njumped\nover\nthe\nlazy\ndog\n");
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates("testSource.pure", 1, 5, 1, 10), new SourceCoordinates("testSource.pure", 6, 1, 6, 6)), source.find(Pattern.compile("quic[\\w]+")).toSet());
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates("testSource.pure", 1, 4, 1, 11), new SourceCoordinates("testSource.pure", 3, 1, 3, 6), new SourceCoordinates("testSource.pure", 6, 1, 6, 6), new SourceCoordinates("testSource.pure", 12, 1, 12, 5)), source.find(Pattern.compile("\\s*((quick)|(lazy))\\s*")).toSet());
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates("testSource.pure", 2, 1, 2, 12)), source.find(Pattern.compile("jump(ed)?\\s+over")).toSet());
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates("testSource.pure", 2, 1, 2, 12)), source.find(Pattern.compile("jumped over")).toSet());
        Assert.assertEquals(Sets.immutable.with(), source.find(Pattern.compile("jumped\\s+over\\s+the\\s+lazy\\s+dog")).toSet());
    }

    @Test
    public void testFindEmptyRegex()
    {
        Source source = new Source("testSource.pure", false, false, "the quick brown fox\njumped over the\nlazy dog\n\nthe\nquick\nbrown\nfox\njumped\nover\nthe\nlazy\ndog\n");
        Verify.assertEmpty(source.find(Pattern.compile("")));
        Verify.assertEmpty(source.find(Pattern.compile(".*")));
    }
}
