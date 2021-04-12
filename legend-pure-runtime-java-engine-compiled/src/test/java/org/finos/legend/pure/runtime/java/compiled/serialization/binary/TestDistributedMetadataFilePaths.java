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

public class TestDistributedMetadataFilePaths
{
    @Test
    public void testGetMetadataPartitionFilePath()
    {
        Assert.assertEquals("metadata/0.bin", DistributedBinaryGraphSerializer.getMetadataBinFilePath("0"));
        Assert.assertEquals("metadata/1.bin", DistributedBinaryGraphSerializer.getMetadataBinFilePath("1"));
        Assert.assertEquals("metadata/5706003.bin", DistributedBinaryGraphSerializer.getMetadataBinFilePath("5706003"));
        Assert.assertEquals("metadata/123456789.bin", DistributedBinaryGraphSerializer.getMetadataBinFilePath("123456789"));
    }

    @Test
    public void testGetClassifierIndexFilePath()
    {
        Assert.assertEquals("metadata/meta_pure_metamodel_type_Class.idx", DistributedBinaryGraphSerializer.getMetadataIndexFilePath("meta::pure::metamodel::type::Class"));
        Assert.assertEquals("metadata/meta_pure_metamodel_relationship_Association.idx", DistributedBinaryGraphSerializer.getMetadataIndexFilePath("meta::pure::metamodel::relationship::Association"));
        Assert.assertEquals("metadata/meta_pure_metamodel_type_Enumeration.idx", DistributedBinaryGraphSerializer.getMetadataIndexFilePath("meta::pure::metamodel::type::Enumeration"));
        Assert.assertEquals("metadata/meta_pure_metamodel_type_generics_GenericType.idx", DistributedBinaryGraphSerializer.getMetadataIndexFilePath("meta::pure::metamodel::type::generics::GenericType"));
    }

    @Test
    public void testGetStringIndexFilePath()
    {
        Assert.assertEquals("metadata/strings/classifiers.idx", DistributedBinaryGraphSerializer.getMetadataIndexFilePath("strings", "classifiers"));
        Assert.assertEquals("metadata/strings/other.idx", DistributedBinaryGraphSerializer.getMetadataIndexFilePath("strings", "other"));
        Assert.assertEquals("metadata/strings/other-0.idx", DistributedBinaryGraphSerializer.getMetadataIndexFilePath("strings", "other-0"));
        Assert.assertEquals("metadata/strings/other-32768.idx", DistributedBinaryGraphSerializer.getMetadataIndexFilePath("strings", "other-32768"));
        Assert.assertEquals("metadata/strings/other-65536.idx", DistributedBinaryGraphSerializer.getMetadataIndexFilePath("strings", "other-65536"));
        Assert.assertEquals("metadata/strings/other-131072.idx", DistributedBinaryGraphSerializer.getMetadataIndexFilePath("strings", "other-131072"));
        Assert.assertEquals("metadata/strings/other-1638400.idx", DistributedBinaryGraphSerializer.getMetadataIndexFilePath("strings", "other-1638400"));
    }
}
