// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.junit.Assert;
import org.junit.Test;

public class TestDistributedMetadataFiles
{
    @Test
    public void testGetMetadataPartitionBinFilePath()
    {
        Assert.assertEquals("metadata/0.bin", DistributedMetadataFiles.getMetadataPartitionBinFilePath(null, 0));
        Assert.assertEquals("metadata/1.bin", DistributedMetadataFiles.getMetadataPartitionBinFilePath(null, 1));
        Assert.assertEquals("metadata/5706003.bin", DistributedMetadataFiles.getMetadataPartitionBinFilePath(null, 5706003));
        Assert.assertEquals("metadata/123456789.bin", DistributedMetadataFiles.getMetadataPartitionBinFilePath(null, 123456789));

        Assert.assertEquals("metadata/xyz/0.bin", DistributedMetadataFiles.getMetadataPartitionBinFilePath("xyz", 0));
        Assert.assertEquals("metadata/abc/1.bin", DistributedMetadataFiles.getMetadataPartitionBinFilePath("abc", 1));
        Assert.assertEquals("metadata/_/5706003.bin", DistributedMetadataFiles.getMetadataPartitionBinFilePath("_", 5706003));
        Assert.assertEquals("metadata/platform/123456789.bin", DistributedMetadataFiles.getMetadataPartitionBinFilePath("platform", 123456789));
    }

    @Test
    public void testGetClassifierIndexFilePath()
    {
        Assert.assertEquals("metadata/classifiers/meta/pure/metamodel/type/Class.idx", DistributedMetadataFiles.getMetadataClassifierIndexFilePath(null, "meta::pure::metamodel::type::Class"));
        Assert.assertEquals("metadata/classifiers/meta/pure/metamodel/relationship/Association.idx", DistributedMetadataFiles.getMetadataClassifierIndexFilePath(null, "meta::pure::metamodel::relationship::Association"));
        Assert.assertEquals("metadata/classifiers/meta/pure/metamodel/type/Enumeration.idx", DistributedMetadataFiles.getMetadataClassifierIndexFilePath(null, "meta::pure::metamodel::type::Enumeration"));
        Assert.assertEquals("metadata/classifiers/meta/pure/metamodel/type/generics/GenericType.idx", DistributedMetadataFiles.getMetadataClassifierIndexFilePath(null, "meta::pure::metamodel::type::generics::GenericType"));

        Assert.assertEquals("metadata/12345/classifiers/meta/pure/metamodel/type/Class.idx", DistributedMetadataFiles.getMetadataClassifierIndexFilePath("12345", "meta::pure::metamodel::type::Class"));
        Assert.assertEquals("metadata/core/classifiers/meta/pure/metamodel/relationship/Association.idx", DistributedMetadataFiles.getMetadataClassifierIndexFilePath("core", "meta::pure::metamodel::relationship::Association"));
        Assert.assertEquals("metadata/__/classifiers/meta/pure/metamodel/type/Enumeration.idx", DistributedMetadataFiles.getMetadataClassifierIndexFilePath("__", "meta::pure::metamodel::type::Enumeration"));
        Assert.assertEquals("metadata/platform/classifiers/meta/pure/metamodel/type/generics/GenericType.idx", DistributedMetadataFiles.getMetadataClassifierIndexFilePath("platform", "meta::pure::metamodel::type::generics::GenericType"));
    }

    @Test
    public void testGetStringIndexFilePath()
    {
        Assert.assertEquals("metadata/strings/classifiers.idx", DistributedMetadataFiles.getClassifierIdStringsIndexFilePath(null));
        Assert.assertEquals("metadata/platform/strings/classifiers.idx", DistributedMetadataFiles.getClassifierIdStringsIndexFilePath("platform"));

        Assert.assertEquals("metadata/strings/other.idx", DistributedMetadataFiles.getOtherStringsIndexFilePath(null));
        Assert.assertEquals("metadata/platform/strings/other.idx", DistributedMetadataFiles.getOtherStringsIndexFilePath("platform"));

        Assert.assertEquals("metadata/strings/other-0.idx", DistributedMetadataFiles.getOtherStringsIndexPartitionFilePath(null, 0));
        Assert.assertEquals("metadata/strings/other-32768.idx", DistributedMetadataFiles.getOtherStringsIndexPartitionFilePath(null, 32768));
        Assert.assertEquals("metadata/strings/other-65536.idx", DistributedMetadataFiles.getOtherStringsIndexPartitionFilePath(null, 65536));
        Assert.assertEquals("metadata/strings/other-131072.idx", DistributedMetadataFiles.getOtherStringsIndexPartitionFilePath(null, 131072));
        Assert.assertEquals("metadata/strings/other-1638400.idx", DistributedMetadataFiles.getOtherStringsIndexPartitionFilePath(null, 1638400));

        Assert.assertEquals("metadata/core/strings/other-0.idx", DistributedMetadataFiles.getOtherStringsIndexPartitionFilePath("core", 0));
        Assert.assertEquals("metadata/platform/strings/other-32768.idx", DistributedMetadataFiles.getOtherStringsIndexPartitionFilePath("platform", 32768));
        Assert.assertEquals("metadata/_ABC_/strings/other-65536.idx", DistributedMetadataFiles.getOtherStringsIndexPartitionFilePath("_ABC_", 65536));
        Assert.assertEquals("metadata/null/strings/other-131072.idx", DistributedMetadataFiles.getOtherStringsIndexPartitionFilePath("null", 131072));
        Assert.assertEquals("metadata/_-_/strings/other-1638400.idx", DistributedMetadataFiles.getOtherStringsIndexPartitionFilePath("_-_", 1638400));
    }
}
