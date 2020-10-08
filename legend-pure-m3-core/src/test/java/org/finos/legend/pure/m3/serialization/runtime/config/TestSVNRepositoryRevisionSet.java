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

package org.finos.legend.pure.m3.serialization.runtime.config;

import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.factory.primitive.ObjectLongMaps;
import org.eclipse.collections.impl.test.Verify;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.Test;

public class TestSVNRepositoryRevisionSet
{
    @Test
    public void testNew()
    {
        SVNRepositoryRevisionSet repoRevs1 = SVNRepositoryRevisionSet.newWith("repo1", 10, "repo2", 9, "repo3", 8);
        Verify.assertSetsEqual(Sets.mutable.with("repo1", "repo2", "repo3"), repoRevs1.getRepositories().toSet());
        Assert.assertEquals(10, repoRevs1.getRevision("repo1"));
        Assert.assertEquals(9, repoRevs1.getRevision("repo2"));
        Assert.assertEquals(8, repoRevs1.getRevision("repo3"));
        Assert.assertEquals(-1, repoRevs1.getRevision("not a repo"));

        try
        {
            SVNRepositoryRevisionSet.newWith("repo1", 10, "repo2", -1, "repo3", 8);
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Invalid repository revision set: [repo1:10, repo2:-1, repo3:8]", e.getMessage());
        }
    }

    @Test
    public void testIsMoreRecentThan()
    {
        SVNRepositoryRevisionSet repoRevs1 = SVNRepositoryRevisionSet.newWith("repo1", 10, "repo2", 10, "repo3", 10);
        Assert.assertFalse(repoRevs1.isMoreRecentThan(repoRevs1));

        SVNRepositoryRevisionSet repoRevs2 = SVNRepositoryRevisionSet.newWith("repo1", 10, "repo2", 9, "repo3", 10);
        Assert.assertTrue(repoRevs1.isMoreRecentThan(repoRevs2));
        Assert.assertFalse(repoRevs2.isMoreRecentThan(repoRevs1));

        SVNRepositoryRevisionSet repoRevs3 = SVNRepositoryRevisionSet.newWith("repo1", 10, "repo2", 9, "repo3", 11);
        Assert.assertFalse(repoRevs1.isMoreRecentThan(repoRevs3));
        Assert.assertFalse(repoRevs3.isMoreRecentThan(repoRevs1));
        Assert.assertFalse(repoRevs2.isMoreRecentThan(repoRevs3));
        Assert.assertTrue(repoRevs3.isMoreRecentThan(repoRevs2));

        SVNRepositoryRevisionSet repoRevs4 = SVNRepositoryRevisionSet.newWith("repo1", 10, "repo2", 10);
        Assert.assertFalse(repoRevs1.isMoreRecentThan(repoRevs4));
        Assert.assertFalse(repoRevs4.isMoreRecentThan(repoRevs1));
        Assert.assertFalse(repoRevs2.isMoreRecentThan(repoRevs4));
        Assert.assertFalse(repoRevs4.isMoreRecentThan(repoRevs2));
        Assert.assertFalse(repoRevs3.isMoreRecentThan(repoRevs4));
        Assert.assertFalse(repoRevs4.isMoreRecentThan(repoRevs3));
    }

    @Test
    public void testToJSON() throws ParseException
    {
        assertJSONEquals("{}", SVNRepositoryRevisionSet.newWith(ObjectLongMaps.immutable.<String>with()).toJSON());

        SVNRepositoryRevisionSet repoRevs1 = SVNRepositoryRevisionSet.newWith("repo1", 10, "repo2", 10, "repo3", 10);
        assertJSONEquals("{\"repo1\":10,\"repo2\":10,\"repo3\":10}", repoRevs1.toJSON());

        SVNRepositoryRevisionSet repoRevs2 = SVNRepositoryRevisionSet.newWith("repo1", 10, "repo2", 9, "repo3", 10);
        assertJSONEquals("{\"repo1\":10,\"repo2\":9,\"repo3\":10}", repoRevs2.toJSON());

        SVNRepositoryRevisionSet repoRevs3 = SVNRepositoryRevisionSet.newWith("repo1", 10, "repo2", 9, "repo3", 11);
        assertJSONEquals("{\"repo1\":10,\"repo2\":9,\"repo3\":11}", repoRevs3.toJSON());

        SVNRepositoryRevisionSet repoRevs4 = SVNRepositoryRevisionSet.newWith("repo1", 10, "repo2", 10);
        assertJSONEquals("{\"repo1\":10,\"repo2\":10}", repoRevs4.toJSON());
    }

    @Test
    public void testFromJSON()
    {
        Assert.assertEquals(SVNRepositoryRevisionSet.newWith(ObjectLongMaps.immutable.<String>with()), SVNRepositoryRevisionSet.fromJSON("{}"));

        SVNRepositoryRevisionSet repoRevs1 = SVNRepositoryRevisionSet.newWith("repo1", 10, "repo2", 10, "repo3", 10);
        Assert.assertEquals(repoRevs1, SVNRepositoryRevisionSet.fromJSON("{\"repo1\":10,\"repo2\":10,\"repo3\":10}"));

        SVNRepositoryRevisionSet repoRevs2 = SVNRepositoryRevisionSet.newWith("repo1", 10, "repo2", 9, "repo3", 10);
        Assert.assertEquals(repoRevs2, SVNRepositoryRevisionSet.fromJSON("{\"repo1\":10,\"repo2\":9,\"repo3\":10}"));

        SVNRepositoryRevisionSet repoRevs3 = SVNRepositoryRevisionSet.newWith("repo1", 10, "repo2", 9, "repo3", 11);
        Assert.assertEquals(repoRevs3, SVNRepositoryRevisionSet.fromJSON("{\"repo1\":10,\"repo2\":9,\"repo3\":11}"));

        SVNRepositoryRevisionSet repoRevs4 = SVNRepositoryRevisionSet.newWith("repo1", 10, "repo2", 10);
        Assert.assertEquals(repoRevs4, SVNRepositoryRevisionSet.fromJSON("{\"repo1\":10,\"repo2\":10}"));

        try
        {
            SVNRepositoryRevisionSet.fromJSON("<the quick brown fox>");
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Invalid JSON: <the quick brown fox>", e.getMessage());
        }

        try
        {
            SVNRepositoryRevisionSet.fromJSON("5");
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Invalid repository revision set JSON: 5", e.getMessage());
        }

        try
        {
            SVNRepositoryRevisionSet.fromJSON("{\"repo1\":\"a\",\"repo2\":15}");
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
            String java7runtime = "Invalid repository revision set JSON: {\"repo1\":\"a\",\"repo2\":15}";
            String java11runtime = "Invalid repository revision set JSON: {\"repo2\":15,\"repo1\":\"a\"}";
            Assert.assertTrue(java7runtime.equals(e.getMessage()) || java11runtime.equals(e.getMessage()));
        }

        try
        {
            SVNRepositoryRevisionSet.fromJSON("{\"repo1\":10,\"repo2\":-1}");
            Assert.fail();
        }
        catch (IllegalArgumentException e)
        {
            Assert.assertEquals("Invalid repository revision set: [repo1:10, repo2:-1]", e.getMessage());
        }
    }

    private void assertJSONEquals(String json1, String json2) throws ParseException
    {
        Assert.assertEquals(JSONValue.parseWithException(json1), JSONValue.parseWithException(json2));
    }
}
