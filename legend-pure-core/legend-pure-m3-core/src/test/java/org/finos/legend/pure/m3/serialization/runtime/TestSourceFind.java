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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.test.Verify;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.regex.Pattern;

public class TestSourceFind
{
    @Test
    public void testFindString()
    {
        Source source = new Source("testSource.pure", false, false, "the quick brown fox\njumped over the\nlazy dog\n\nthe\n quick\nbrown\nfox\njumped\nover\nthe\nlazy\ndog\n");
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates("testSource.pure", 1, 5, 1, 9), new SourceCoordinates("testSource.pure", 6, 2, 6, 6)), source.find("quick").toSet());
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates("testSource.pure", 2, 1, 2, 11)), source.find("jumped over").toSet());
        Assert.assertEquals(Sets.immutable.with(), source.find("jumped over the lazy dog").toSet());
        Assert.assertEquals(Sets.immutable.with(), source.find("$").toSet());

        // check preview text
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates.Preview("the ", "quick", " brown fox"), new SourceCoordinates.Preview("", "quick", "")), source.find("quick").collect(SourceCoordinates::getPreview).toSet());
    }

    @Test
    public void testFindStringCaseInsensitive()
    {
        Source source = new Source("testSource.pure", false, false, " The Quick Brown Fox \nJumped Over The\nLazy Dog\n\nTHE\nQUICK\nBROWN\nFOX\nJUMPED\nOVER\nTHE\nLAZY\nDOG\n");
        Assert.assertEquals(Sets.immutable.with(), source.find("quick", true).toSet());
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates("testSource.pure", 1, 6, 1, 10), new SourceCoordinates("testSource.pure", 6, 1, 6, 5)), source.find("quick", false).toSet());
        Assert.assertEquals(Sets.immutable.with(), source.find("JUMPED OVER", true).toSet());
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates("testSource.pure", 2, 1, 2, 11)), source.find("JUMPED OVER", false).toSet());

        // check preview text
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates.Preview("The ", "Quick", " Brown Fox")), source.find("Quick", true).collect(SourceCoordinates::getPreview).toSet());
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates.Preview("The ", "Quick", " Brown Fox"), new SourceCoordinates.Preview("", "QUICK", "")), source.find("quick", false).collect(SourceCoordinates::getPreview).toSet());
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
        Source source = new Source("testSource.pure", false, false, "the quick brown fox\n    jumped over the \nlazy dog\n\nthe\nquick\nbrown\nfox\njumped\nover\nthe\nlazy\ndog\n");
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates("testSource.pure", 1, 5, 1, 9), new SourceCoordinates("testSource.pure", 6, 1, 6, 5)), source.find(Pattern.compile("quic[\\w]+")).toSet());
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates("testSource.pure", 1, 4, 1, 10), new SourceCoordinates("testSource.pure", 3, 1, 3, 5), new SourceCoordinates("testSource.pure", 6, 1, 6, 5), new SourceCoordinates("testSource.pure", 12, 1, 12, 4)), source.find(Pattern.compile("\\s*((quick)|(lazy))\\s*")).toSet());
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates("testSource.pure", 2, 5, 2, 15)), source.find(Pattern.compile("jump(ed)?\\s+over")).toSet());
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates("testSource.pure", 2, 5, 2, 15)), source.find(Pattern.compile("jumped over")).toSet());
        Assert.assertEquals(Sets.immutable.with(), source.find(Pattern.compile("jumped\\s+over\\s+the\\s+lazy\\s+dog")).toSet());

        // check preview text
        Assert.assertEquals(Sets.immutable.with(new SourceCoordinates.Preview("", "jumped over", " the")), source.find(Pattern.compile("jumped over")).collect(SourceCoordinates::getPreview).toSet());
    }

    @Test
    public void testFindEmptyRegex()
    {
        Source source = new Source("testSource.pure", false, false, "the quick brown fox\njumped over the\nlazy dog\n\nthe\nquick\nbrown\nfox\njumped\nover\nthe\nlazy\ndog\n");
        Verify.assertEmpty(source.find(Pattern.compile("")));
        Verify.assertEmpty(source.find(Pattern.compile(".*")));
    }

    @Test
    public void testGetPreviewText()
    {
        Source source = new Source("testSource.pure", false, false, " the quick brown fox\njumped over the\nlazy dog \n\nthe\nquick\nbrown\nfox\njumped\nover\nthe\nlazy\ndog\n");
        Assert.assertEquals(new SourceCoordinates.Preview("the quic", "k brown fox\njumped over the\nlaz", "y dog"), source.getPreviewTextWithCoordinates(1, 10, 3, 3));
    }
}
