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
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eclipse.collections.impl.factory.primitive.LongObjectMaps;
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
                        "\uD801\uDE2C9omt6n1epopqk",
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
                        "\uD801\uDE2C9omt6n1epopqk",
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
                                          "dqjpvsg9pco1k" :
                                  "ClassWithVeryVeryVeryVery" +
                                          "\uD801\uDE00\uD801\uDE01\uD801\uDE02\uD801\uDE03\uD801\uDE04\uD801\uDE05\uD801\uDE06\uD801\uDE07\uD801\uDE08\uD801\uDE09\uD801\uDE0A" +
                                          "\uD801\uDE0B\uD801\uDE0C\uD801\uDE0D\uD801\uDE0E\uD801\uDE0F\uD801\uDE10\uD801\uDE11\uD801\uDE12\uD801\uDE13\uD801\uDE14\uD801\uDE15" +
                                          "\uD801\uDE16\uD801\uDE17\uD801\uDE18\uD801\uDE19\uD801\uDE1A\uD801\uDE1B\uD801\uDE1C\uD801\uDE1D\uD801\uDE1E\uD801\uDE1F\uD801\uDE20" +
                                          "\uD801\uDE21\uD801\uDE22\uD801\uDE23\uD801\uDE24\uD801\uDE25\uD801\uDE26\uD801\uDE27\uD801\uDE28" +
                                          "8pt6245a4uu9d";
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
                        "_-_-_-_-_cnev18gbg99dp",
                FilePathTools.getFilePathName(minLongName));
        Assert.assertEquals(
                "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                        "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                        "_-_-_-_-_cnev18gbg99dp",
                FilePathTools.getFilePathName(minLongName, null));
        EXTENSIONS.forEach(ext ->
        {
            String expected = ((ext.length() == 4) ?
                               ("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                                       "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                                       "_-_-_alljlteah664k") :
                               ("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                                       "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                                       "_-_-ae9nvaovemk65")) + ext;
            Assert.assertEquals(expected, FilePathTools.getFilePathName(minLongName, ext));
        });

        // Short enough without extension, but extension pushes it over
        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery\uD808\uDC00\uD808\uDC17ryVeryVeryLongName",
                FilePathTools.getFilePathName("ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery\uD808\uDC00\uD808\uDC17ryVeryVeryLongName")
        );
        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVer\uD808\uDC001jcboh5eh2gau.pelt",
                FilePathTools.getFilePathName("ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVer\uD808\uDC00\uD808\uDC17ryVeryVeryLongName", ".pelt")
        );
        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVer\uD808\uDC00\uD808\uDC17eryVeryVeryLongName",
                FilePathTools.getFilePathName("ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVer\uD808\uDC00\uD808\uDC17eryVeryVeryLongName")
        );
        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVe\uD808\uDC00\uD808\uDC17b01rqqtfc6vnk.pelt",
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
                        "\uD802\uDF2C\uD802\uDF2D\uD802\uDF2E\uD802\uDF2F\uD802\uDF30\uD802\uDF314fk1pd2tvv63j.pmf",
                FilePathTools.getFilePathName("123very_\uD802\uDF00\uD802\uDF01\uD802\uDF02\uD802\uDF03\uD802\uDF04\uD802\uDF05\uD802\uDF06\uD802\uDF07\uD802\uDF08\uD802\uDF09\uD802\uDF0A" +
                        "\uD802\uDF0B\uD802\uDF0C\uD802\uDF0D\uD802\uDF0E\uD802\uDF0F\uD802\uDF10\uD802\uDF11\uD802\uDF12\uD802\uDF13\uD802\uDF14\uD802\uDF15" +
                        "\uD802\uDF16\uD802\uDF17\uD802\uDF18\uD802\uDF19\uD802\uDF1A\uD802\uDF1B\uD802\uDF1C\uD802\uDF1D\uD802\uDF1E\uD802\uDF1F\uD802\uDF20" +
                        "\uD802\uDF21\uD802\uDF22\uD802\uDF23\uD802\uDF24\uD802\uDF25\uD802\uDF26\uD802\uDF27\uD802\uDF28\uD802\uDF29\uD802\uDF2A\uD802\uDF2B" +
                        "\uD802\uDF2C\uD802\uDF2D\uD802\uDF2E\uD802\uDF2F\uD802\uDF30\uD802\uDF31\uD802\uDF32\uD802\uDF33\uD802\uDF34\uD802\uDF35_long_name", ".pmf")
        );

        // Simply too long
        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVer45ebbkborcu02",
                FilePathTools.getFilePathName("ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName")
        );
        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVereacja58dbiaqf",
                FilePathTools.getFilePathName("ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery" +
                        "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery" +
                        "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName")
        );
        Assert.assertEquals(
                "packageWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVb01rqqtfc6vnk",
                FilePathTools.getFilePathName("packageWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName")
        );
        Assert.assertEquals(
                "packageWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVe6ctlvu95lq3l",
                FilePathTools.getFilePathName("packageWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery" +
                        "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery" +
                        "VeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryLongName")
        );

        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVe\uD808\uDC00\uD808\uDC17V1cgdra7e45opn",
                FilePathTools.getFilePathName("ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVe\uD808\uDC00\uD808\uDC17VeryVeryLongName")
        );
        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery\uD808\uDC001jcboh5eh2gau",
                FilePathTools.getFilePathName("ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVery\uD808\uDC00\uD808\uDC17ryVeryVeryLongName")
        );
        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryV\uD808\uDC001jcboh5eh2gau",
                FilePathTools.getFilePathName("ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryV\uD808\uDC00\uD808\uDC17ryVeryVeryLongName")
        );
        Assert.assertEquals(
                "ClassWithAVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVeryVefdu2cebkdi0hq",
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

    @Test
    public void testOverageHashingZeroToThreeCharStrings()
    {
        char minChar = 32;
        char maxChar = 126;
        MutableLongObjectMap<MutableList<String>> hashToStrings = LongObjectMaps.mutable.empty();
        hashToStrings.getIfAbsentPut(FilePathTools.hashOverage("", 0, 0), Lists.mutable::empty).add("");
        for (char c1 = minChar; c1 <= maxChar; c1++)
        {
            String s1 = String.valueOf(c1);
            hashToStrings.getIfAbsentPut(FilePathTools.hashOverage(s1, 0, 1), Lists.mutable::empty).add(s1);
            for (char c2 = minChar; c2 <= maxChar; c2++)
            {
                String s2 = s1 + c2;
                hashToStrings.getIfAbsentPut(FilePathTools.hashOverage(s2, 0, 2), Lists.mutable::empty).add(s2);
                for (char c3 = minChar; c3 < maxChar; c3++)
                {
                    String s3 = s2 + c3;
                    hashToStrings.getIfAbsentPut(FilePathTools.hashOverage(s3, 0, 3), Lists.mutable::empty).add(s3);
                }
            }
        }
        MutableList<String> violations = Lists.mutable.empty();
        hashToStrings.forEachKeyValue((hash, strings) ->
        {
            if (strings.size() > 1)
            {
                violations.add("hash: " + hash + "; " + strings.size() + " strings: " + strings.makeString("'", "', '", "'"));
            }
        });
        if (violations.notEmpty())
        {
            Assert.fail(violations.makeString(violations.size() + " collisions\n", "\n", ""));
        }
    }
}
