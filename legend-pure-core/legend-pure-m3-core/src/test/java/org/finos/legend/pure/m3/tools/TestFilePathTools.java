// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.tools;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

public class TestFilePathTools
{
    private static final ImmutableList<String> EXTENSIONS = Lists.immutable.with(".txt", ".json", ".pure", ".pelt", ".pmf");

    @Test
    public void testToFilePath()
    {
        Assert.assertEquals("test/model/MyClass", FilePathTools.toFilePath("test::model::MyClass", "::", "/", null));
        Assert.assertEquals("test\\model\\MyClass", FilePathTools.toFilePath("test::model::MyClass", "::", "\\", null));

        EXTENSIONS.forEach(ext ->
        {
            Assert.assertEquals("test/model/MyClass" + ext, FilePathTools.toFilePath("test::model::MyClass", "::", "/", ext));
            Assert.assertEquals("test\\model\\MyClass" + ext, FilePathTools.toFilePath("test::model::MyClass", "::", "\\", ext));
        });

        Assert.assertEquals(
                "test/model/ClassWithAVeryVeryVery" +
                        "\uD801\uDE00\uD801\uDE01\uD801\uDE02\uD801\uDE03\uD801\uDE04\uD801\uDE05\uD801\uDE06\uD801\uDE07\uD801\uDE08\uD801\uDE09\uD801\uDE0A" +
                        "\uD801\uDE0B\uD801\uDE0C\uD801\uDE0D\uD801\uDE0E\uD801\uDE0F\uD801\uDE10\uD801\uDE11\uD801\uDE12\uD801\uDE13\uD801\uDE14\uD801\uDE15" +
                        "\uD801\uDE16\uD801\uDE17\uD801\uDE18\uD801\uDE19\uD801\uDE1A\uD801\uDE1B\uD801\uDE1C\uD801\uDE1D\uD801\uDE1E\uD801\uDE1F\uD801\uDE20" +
                        "\uD801\uDE21\uD801\uDE22\uD801\uDE23\uD801\uDE24\uD801\uDE25\uD801\uDE26\uD801\uDE27\uD801\uDE28\uD801\uDE29\uD801\uDE2A\uD801\uDE2B" +
                        "\uD801\uDE2C86gc6bm461hib",
                FilePathTools.toFilePath("test::model::ClassWithAVeryVeryVery" +
                        "\uD801\uDE00\uD801\uDE01\uD801\uDE02\uD801\uDE03\uD801\uDE04\uD801\uDE05\uD801\uDE06\uD801\uDE07\uD801\uDE08\uD801\uDE09\uD801\uDE0A" +
                        "\uD801\uDE0B\uD801\uDE0C\uD801\uDE0D\uD801\uDE0E\uD801\uDE0F\uD801\uDE10\uD801\uDE11\uD801\uDE12\uD801\uDE13\uD801\uDE14\uD801\uDE15" +
                        "\uD801\uDE16\uD801\uDE17\uD801\uDE18\uD801\uDE19\uD801\uDE1A\uD801\uDE1B\uD801\uDE1C\uD801\uDE1D\uD801\uDE1E\uD801\uDE1F\uD801\uDE20" +
                        "\uD801\uDE21\uD801\uDE22\uD801\uDE23\uD801\uDE24\uD801\uDE25\uD801\uDE26\uD801\uDE27\uD801\uDE28\uD801\uDE29\uD801\uDE2A\uD801\uDE2B" +
                        "\uD801\uDE2C\uD801\uDE2D\uD801\uDE2E\uD801\uDE2F" +
                        "VeryLongName", "::", "/", null));
        Assert.assertEquals(
                "test\\model\\ClassWithAVeryVeryVery" +
                        "\uD801\uDE00\uD801\uDE01\uD801\uDE02\uD801\uDE03\uD801\uDE04\uD801\uDE05\uD801\uDE06\uD801\uDE07\uD801\uDE08\uD801\uDE09\uD801\uDE0A" +
                        "\uD801\uDE0B\uD801\uDE0C\uD801\uDE0D\uD801\uDE0E\uD801\uDE0F\uD801\uDE10\uD801\uDE11\uD801\uDE12\uD801\uDE13\uD801\uDE14\uD801\uDE15" +
                        "\uD801\uDE16\uD801\uDE17\uD801\uDE18\uD801\uDE19\uD801\uDE1A\uD801\uDE1B\uD801\uDE1C\uD801\uDE1D\uD801\uDE1E\uD801\uDE1F\uD801\uDE20" +
                        "\uD801\uDE21\uD801\uDE22\uD801\uDE23\uD801\uDE24\uD801\uDE25\uD801\uDE26\uD801\uDE27\uD801\uDE28\uD801\uDE29\uD801\uDE2A\uD801\uDE2B" +
                        "\uD801\uDE2C86gc6bm461hib",
                FilePathTools.toFilePath("test::model::ClassWithAVeryVeryVery" +
                        "\uD801\uDE00\uD801\uDE01\uD801\uDE02\uD801\uDE03\uD801\uDE04\uD801\uDE05\uD801\uDE06\uD801\uDE07\uD801\uDE08\uD801\uDE09\uD801\uDE0A" +
                        "\uD801\uDE0B\uD801\uDE0C\uD801\uDE0D\uD801\uDE0E\uD801\uDE0F\uD801\uDE10\uD801\uDE11\uD801\uDE12\uD801\uDE13\uD801\uDE14\uD801\uDE15" +
                        "\uD801\uDE16\uD801\uDE17\uD801\uDE18\uD801\uDE19\uD801\uDE1A\uD801\uDE1B\uD801\uDE1C\uD801\uDE1D\uD801\uDE1E\uD801\uDE1F\uD801\uDE20" +
                        "\uD801\uDE21\uD801\uDE22\uD801\uDE23\uD801\uDE24\uD801\uDE25\uD801\uDE26\uD801\uDE27\uD801\uDE28\uD801\uDE29\uD801\uDE2A\uD801\uDE2B" +
                        "\uD801\uDE2C\uD801\uDE2D\uD801\uDE2E\uD801\uDE2F" +
                        "VeryLongName", "::", "\\", null));
        EXTENSIONS.forEach(ext ->
        {
            String expectedName = (ext.length() == 4) ?
                                  "ClassWithVeryVeryVeryVery" +
                                          "\uD801\uDE00\uD801\uDE01\uD801\uDE02\uD801\uDE03\uD801\uDE04\uD801\uDE05\uD801\uDE06\uD801\uDE07\uD801\uDE08\uD801\uDE09\uD801\uDE0A" +
                                          "\uD801\uDE0B\uD801\uDE0C\uD801\uDE0D\uD801\uDE0E\uD801\uDE0F\uD801\uDE10\uD801\uDE11\uD801\uDE12\uD801\uDE13\uD801\uDE14\uD801\uDE15" +
                                          "\uD801\uDE16\uD801\uDE17\uD801\uDE18\uD801\uDE19\uD801\uDE1A\uD801\uDE1B\uD801\uDE1C\uD801\uDE1D\uD801\uDE1E\uD801\uDE1F\uD801\uDE20" +
                                          "\uD801\uDE21\uD801\uDE22\uD801\uDE23\uD801\uDE24\uD801\uDE25\uD801\uDE26\uD801\uDE27\uD801\uDE28\uD801\uDE29" +
                                          "3424vqq4pqhf0" :
                                  "ClassWithVeryVeryVeryVery" +
                                          "\uD801\uDE00\uD801\uDE01\uD801\uDE02\uD801\uDE03\uD801\uDE04\uD801\uDE05\uD801\uDE06\uD801\uDE07\uD801\uDE08\uD801\uDE09\uD801\uDE0A" +
                                          "\uD801\uDE0B\uD801\uDE0C\uD801\uDE0D\uD801\uDE0E\uD801\uDE0F\uD801\uDE10\uD801\uDE11\uD801\uDE12\uD801\uDE13\uD801\uDE14\uD801\uDE15" +
                                          "\uD801\uDE16\uD801\uDE17\uD801\uDE18\uD801\uDE19\uD801\uDE1A\uD801\uDE1B\uD801\uDE1C\uD801\uDE1D\uD801\uDE1E\uD801\uDE1F\uD801\uDE20" +
                                          "\uD801\uDE21\uD801\uDE22\uD801\uDE23\uD801\uDE24\uD801\uDE25\uD801\uDE26\uD801\uDE27\uD801\uDE28" +
                                          "3tueqg7cv4su9";
            Assert.assertEquals(
                    "test/model/" + expectedName + ext,
                    FilePathTools.toFilePath("test::model::ClassWithVeryVeryVeryVery" +
                            "\uD801\uDE00\uD801\uDE01\uD801\uDE02\uD801\uDE03\uD801\uDE04\uD801\uDE05\uD801\uDE06\uD801\uDE07\uD801\uDE08\uD801\uDE09\uD801\uDE0A" +
                            "\uD801\uDE0B\uD801\uDE0C\uD801\uDE0D\uD801\uDE0E\uD801\uDE0F\uD801\uDE10\uD801\uDE11\uD801\uDE12\uD801\uDE13\uD801\uDE14\uD801\uDE15" +
                            "\uD801\uDE16\uD801\uDE17\uD801\uDE18\uD801\uDE19\uD801\uDE1A\uD801\uDE1B\uD801\uDE1C\uD801\uDE1D\uD801\uDE1E\uD801\uDE1F\uD801\uDE20" +
                            "\uD801\uDE21\uD801\uDE22\uD801\uDE23\uD801\uDE24\uD801\uDE25\uD801\uDE26\uD801\uDE27\uD801\uDE28\uD801\uDE29\uD801\uDE2A\uD801\uDE2B" +
                            "\uD801\uDE2C\uD801\uDE2D\uD801\uDE2E\uD801\uDE2F" +
                            "VeryLongName", "::", "/", ext));
            Assert.assertEquals(
                    "test\\model\\" + expectedName + ext,
                    FilePathTools.toFilePath("test::model::ClassWithVeryVeryVeryVery" +
                            "\uD801\uDE00\uD801\uDE01\uD801\uDE02\uD801\uDE03\uD801\uDE04\uD801\uDE05\uD801\uDE06\uD801\uDE07\uD801\uDE08\uD801\uDE09\uD801\uDE0A" +
                            "\uD801\uDE0B\uD801\uDE0C\uD801\uDE0D\uD801\uDE0E\uD801\uDE0F\uD801\uDE10\uD801\uDE11\uD801\uDE12\uD801\uDE13\uD801\uDE14\uD801\uDE15" +
                            "\uD801\uDE16\uD801\uDE17\uD801\uDE18\uD801\uDE19\uD801\uDE1A\uD801\uDE1B\uD801\uDE1C\uD801\uDE1D\uD801\uDE1E\uD801\uDE1F\uD801\uDE20" +
                            "\uD801\uDE21\uD801\uDE22\uD801\uDE23\uD801\uDE24\uD801\uDE25\uD801\uDE26\uD801\uDE27\uD801\uDE28\uD801\uDE29\uD801\uDE2A\uD801\uDE2B" +
                            "\uD801\uDE2C\uD801\uDE2D\uD801\uDE2E\uD801\uDE2F" +
                            "VeryLongName", "::", "\\", ext));
        });
    }

    @Test
    public void testGetFilePathName()
    {
        // Basic short names
        Lists.mutable.with("", "a", "abc", "the quick brown fox").forEach(name ->
        {
            Assert.assertSame(name, FilePathTools.getFilePathName(name));
            Assert.assertSame(name, FilePathTools.getFilePathName(name, null));
            EXTENSIONS.forEach(ext ->
                    Assert.assertEquals(name + ext, FilePathTools.getFilePathName(name, ext)));
        });

        // Short names with unicode
        Lists.mutable.with("My\uD808\uDC3BClass",
                "\uD808\uDC00\uD808\uDC17",
                "\uD801\uDE00\uD801\uDE01\uD801\uDE02",
                "\uD801\uDE03\uD801\uDE04\uD801\uDE05\uD801\uDE06",
                "\uD801\uDE07\uD801\uDE08\uD801\uDE09\uD801\uDE0A",
                "\uD801\uDE0B\uD801\uDE0C\uD801\uDE0D\uD801\uDE0E\uD801\uDE0F\uD801\uDE10\uD801\uDE11\uD801\uDE12\uD801\uDE13\uD801\uDE14\uD801\uDE15",
                "\uD801\uDE16\uD801\uDE17\uD801\uDE18\uD801\uDE19\uD801\uDE1A\uD801\uDE1B\uD801\uDE1C\uD801\uDE1D\uD801\uDE1E\uD801\uDE1F\uD801\uDE20",
                "\uD801\uDE21\uD801\uDE22\uD801\uDE23\uD801\uDE24\uD801\uDE25\uD801\uDE26\uD801\uDE27\uD801\uDE28\uD801\uDE29\uD801\uDE2A\uD801\uDE2B",
                "\uD801\uDE2C\uD801\uDE2D\uD801\uDE2E\uD801\uDE2F",
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery\uD808\uDC00\uD808\uDC17ryLongName",
                "ClassWithAVeryVeryVery" +
                        "\uD801\uDE00\uD801\uDE01\uD801\uDE02\uD801\uDE03\uD801\uDE04\uD801\uDE05\uD801\uDE06\uD801\uDE07\uD801\uDE08\uD801\uDE09\uD801\uDE0A" +
                        "\uD801\uDE0B\uD801\uDE0C\uD801\uDE0D\uD801\uDE0E\uD801\uDE0F\uD801\uDE10\uD801\uDE11\uD801\uDE12\uD801\uDE13\uD801\uDE14\uD801\uDE15" +
                        "\uD801\uDE16\uD801\uDE17\uD801\uDE18\uD801\uDE19\uD801\uDE1A\uD801\uDE1B\uD801\uDE1C\uD801\uDE1D\uD801\uDE1E\uD801\uDE1F\uD801\uDE20" +
                        "\uD801\uDE21\uD801\uDE22\uD801\uDE23\uD801\uDE24\uD801\uDE25\uD801\uDE26\uD801\uDE27\uD801\uDE28\uD801\uDE29\uD801\uDE2A\uD801\uDE2B" +
                        "\uD801\uDE2C\uD801\uDE2D\uD801\uDE2E\uD801\uDE2F",
                "mod_\uD802\uDF00\uD802\uDF01\uD802\uDF02\uD802\uDF03_\uD802\uDF04\uD802\uDF05\uD802\uDF06\uD802\uDF07_\uD802\uDF08\uD802\uDF09\uD802\uDF0A_mod",
                "\uD802\uDF0B\uD802\uDF0C\uD802\uDF0D_\uD802\uDF0E\uD802\uDF0F\uD802\uDF10\uD802\uDF11\uD802\uDF12\uD802\uDF13_\uD802\uDF14\uD802\uDF15",
                "\uD802\uDF16\uD802\uDF17\uD802\uDF18\uD802\uDF19\uD802\uDF1A\uD802\uDF1B\uD802\uDF1C\uD802\uDF1D\uD802\uDF1E\uD802\uDF1F\uD802\uDF20",
                "\uD802\uDF21\uD802\uDF22\uD802\uDF23\uD802\uDF24\uD802\uDF25\uD802\uDF26\uD802\uDF27\uD802\uDF28\uD802\uDF29\uD802\uDF2A\uD802\uDF2B",
                "a\uD802\uDF2C\uD802\uDF2D\uD802\uDF2E\uD802\uDF2F\uD802\uDF30\uD802\uDF31\uD802\uDF32\uD802\uDF33\uD802\uDF34\uD802\uDF35z"
        ).forEach(name ->
        {
            Assert.assertSame(name, FilePathTools.getFilePathName(name));
            Assert.assertSame(name, FilePathTools.getFilePathName(name, null));
            EXTENSIONS.forEach(ext ->
                    Assert.assertEquals(name + ext, FilePathTools.getFilePathName(name, ext)));
        });

        // Max short name
        String maxShortName = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                "_-_-_-_-_-_-_-_-_-_-_-";
        Assert.assertEquals(126, maxShortName.length());
        Assert.assertSame(maxShortName, FilePathTools.getFilePathName(maxShortName));
        Assert.assertSame(maxShortName, FilePathTools.getFilePathName(maxShortName, null));
        EXTENSIONS.forEach(ext ->
        {
            String subName = maxShortName.substring(0, maxShortName.length() - ext.length());
            Assert.assertEquals(subName + ext, FilePathTools.getFilePathName(subName, ext));
        });

        // Min long name
        String minLongName = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                "_-_-_-_-_-_-_-_-_-_-_-_";
        Assert.assertEquals(127, minLongName.length());
        Assert.assertEquals(
                "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                        "_-_-_-_-_b0bivi7v23ohu",
                FilePathTools.getFilePathName(minLongName));
        Assert.assertEquals(
                "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                        "_-_-_-_-_b0bivi7v23ohu",
                FilePathTools.getFilePathName(minLongName, null));
        EXTENSIONS.forEach(ext ->
        {
            String expected = ((ext.length() == 4) ?
                               ("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                                       "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                                       "_-_-_2ns1c81su0sj2") :
                               ("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                                       "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                                       "_-_-3nqcidr61le81")) + ext;
            Assert.assertEquals(expected, FilePathTools.getFilePathName(minLongName, ext));
        });

        // Short enough without extension, but extension pushes it over
        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery\uD808\uDC00\uD808\uDC17ryVeryVeryLongName",
                FilePathTools.getFilePathName("ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery\uD808\uDC00\uD808\uDC17ryVeryVeryLongName")
        );
        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVer\uD808\uDC009d7r6s8av6fsh.pelt",
                FilePathTools.getFilePathName("ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVer\uD808\uDC00\uD808\uDC17ryVeryVeryLongName", ".pelt")
        );
        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVer\uD808\uDC00\uD808\uDC17eryVeryVeryLongName",
                FilePathTools.getFilePathName("ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVer\uD808\uDC00\uD808\uDC17eryVeryVeryLongName")
        );
        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVe\uD808\uDC00\uD808\uDC175tklick8u7q2v.pelt",
                FilePathTools.getFilePathName("ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVe\uD808\uDC00\uD808\uDC17eryVeryVeryLongName", ".pelt")
        );

        Assert.assertEquals(
                "very_\uD802\uDF00\uD802\uDF01\uD802\uDF02\uD802\uDF03\uD802\uDF04\uD802\uDF05\uD802\uDF06\uD802\uDF07\uD802\uDF08\uD802\uDF09\uD802\uDF0A" +
                        "\uD802\uDF0B\uD802\uDF0C\uD802\uDF0D\uD802\uDF0E\uD802\uDF0F\uD802\uDF10\uD802\uDF11\uD802\uDF12\uD802\uDF13\uD802\uDF14\uD802\uDF15" +
                        "\uD802\uDF16\uD802\uDF17\uD802\uDF18\uD802\uDF19\uD802\uDF1A\uD802\uDF1B\uD802\uDF1C\uD802\uDF1D\uD802\uDF1E\uD802\uDF1F\uD802\uDF20" +
                        "\uD802\uDF21\uD802\uDF22\uD802\uDF23\uD802\uDF24\uD802\uDF25\uD802\uDF26\uD802\uDF27\uD802\uDF28\uD802\uDF29\uD802\uDF2A\uD802\uDF2B" +
                        "\uD802\uDF2C\uD802\uDF2D\uD802\uDF2E\uD802\uDF2F\uD802\uDF30\uD802\uDF31\uD802\uDF32\uD802\uDF33\uD802\uDF34\uD802\uDF35_long_name",
                FilePathTools.getFilePathName("very_\uD802\uDF00\uD802\uDF01\uD802\uDF02\uD802\uDF03\uD802\uDF04\uD802\uDF05\uD802\uDF06\uD802\uDF07\uD802\uDF08\uD802\uDF09\uD802\uDF0A" +
                        "\uD802\uDF0B\uD802\uDF0C\uD802\uDF0D\uD802\uDF0E\uD802\uDF0F\uD802\uDF10\uD802\uDF11\uD802\uDF12\uD802\uDF13\uD802\uDF14\uD802\uDF15" +
                        "\uD802\uDF16\uD802\uDF17\uD802\uDF18\uD802\uDF19\uD802\uDF1A\uD802\uDF1B\uD802\uDF1C\uD802\uDF1D\uD802\uDF1E\uD802\uDF1F\uD802\uDF20" +
                        "\uD802\uDF21\uD802\uDF22\uD802\uDF23\uD802\uDF24\uD802\uDF25\uD802\uDF26\uD802\uDF27\uD802\uDF28\uD802\uDF29\uD802\uDF2A\uD802\uDF2B" +
                        "\uD802\uDF2C\uD802\uDF2D\uD802\uDF2E\uD802\uDF2F\uD802\uDF30\uD802\uDF31\uD802\uDF32\uD802\uDF33\uD802\uDF34\uD802\uDF35_long_name")
        );
        Assert.assertEquals(
                "123very_\uD802\uDF00\uD802\uDF01\uD802\uDF02\uD802\uDF03\uD802\uDF04\uD802\uDF05\uD802\uDF06\uD802\uDF07\uD802\uDF08\uD802\uDF09\uD802\uDF0A" +
                        "\uD802\uDF0B\uD802\uDF0C\uD802\uDF0D\uD802\uDF0E\uD802\uDF0F\uD802\uDF10\uD802\uDF11\uD802\uDF12\uD802\uDF13\uD802\uDF14\uD802\uDF15" +
                        "\uD802\uDF16\uD802\uDF17\uD802\uDF18\uD802\uDF19\uD802\uDF1A\uD802\uDF1B\uD802\uDF1C\uD802\uDF1D\uD802\uDF1E\uD802\uDF1F\uD802\uDF20" +
                        "\uD802\uDF21\uD802\uDF22\uD802\uDF23\uD802\uDF24\uD802\uDF25\uD802\uDF26\uD802\uDF27\uD802\uDF28\uD802\uDF29\uD802\uDF2A\uD802\uDF2B" +
                        "\uD802\uDF2C\uD802\uDF2D\uD802\uDF2E\uD802\uDF2F\uD802\uDF30\uD802\uDF313lgtrt1tbc6nh.pmf",
                FilePathTools.getFilePathName("123very_\uD802\uDF00\uD802\uDF01\uD802\uDF02\uD802\uDF03\uD802\uDF04\uD802\uDF05\uD802\uDF06\uD802\uDF07\uD802\uDF08\uD802\uDF09\uD802\uDF0A" +
                        "\uD802\uDF0B\uD802\uDF0C\uD802\uDF0D\uD802\uDF0E\uD802\uDF0F\uD802\uDF10\uD802\uDF11\uD802\uDF12\uD802\uDF13\uD802\uDF14\uD802\uDF15" +
                        "\uD802\uDF16\uD802\uDF17\uD802\uDF18\uD802\uDF19\uD802\uDF1A\uD802\uDF1B\uD802\uDF1C\uD802\uDF1D\uD802\uDF1E\uD802\uDF1F\uD802\uDF20" +
                        "\uD802\uDF21\uD802\uDF22\uD802\uDF23\uD802\uDF24\uD802\uDF25\uD802\uDF26\uD802\uDF27\uD802\uDF28\uD802\uDF29\uD802\uDF2A\uD802\uDF2B" +
                        "\uD802\uDF2C\uD802\uDF2D\uD802\uDF2E\uD802\uDF2F\uD802\uDF30\uD802\uDF31\uD802\uDF32\uD802\uDF33\uD802\uDF34\uD802\uDF35_long_name", ".pmf")
        );

        // Simply too long
        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVer5pnih0hib99bc",
                FilePathTools.getFilePathName("ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName")
        );
        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVercbspuomqa9s8a",
                FilePathTools.getFilePathName("ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery" +
                        "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery" +
                        "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName")
        );
        Assert.assertEquals(
                "packageWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryV5tklick8u7q2v",
                FilePathTools.getFilePathName("packageWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName")
        );
        Assert.assertEquals(
                "packageWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryV3t40o2vma703t",
                FilePathTools.getFilePathName("packageWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery" +
                        "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery" +
                        "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName")
        );

        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVe\uD808\uDC00\uD808\uDC17V9vkl2m1umkbg9",
                FilePathTools.getFilePathName("ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVe\uD808\uDC00\uD808\uDC17VeryVeryLongName")
        );
        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery\uD808\uDC009d7r6s8av6fsh",
                FilePathTools.getFilePathName("ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery\uD808\uDC00\uD808\uDC17ryVeryVeryLongName")
        );
        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryV\uD808\uDC009d7r6s8av6fsh",
                FilePathTools.getFilePathName("ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryV\uD808\uDC00\uD808\uDC17ryVeryVeryLongName")
        );
        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeedqdbf0jhs7sh",
                FilePathTools.getFilePathName("ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVe\uD808\uDC00\uD808\uDC17ryVeryVeryLongName")
        );
    }

    @Test
    public void testGetFilePathNameWithTooLongExtension()
    {
        String longExt = ".__a_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_very_long_extension";
        Assert.assertEquals(114, longExt.length());

        // file name plus extension are under the limit
        Assert.assertEquals("file_name" + longExt, FilePathTools.getFilePathName("file_name", longExt));

        // file name plus extension are over the limit
        IllegalArgumentException e = Assert.assertThrows(IllegalArgumentException.class, () -> FilePathTools.getFilePathName("not_terribly_long_file_name", longExt));
        Assert.assertEquals("File extension exceeds the limit of 113 characters: " + longExt, e.getMessage());
    }
}
