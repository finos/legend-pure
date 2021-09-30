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

package org.finos.legend.pure.runtime.java.compiled.serialization.binary;

import org.finos.legend.pure.m4.serialization.Reader;

class LazyStringIndex extends AbstractStringIndex
{
    private final String metadataName;
    private final FileReader fileReader;
    private final String[] otherStrings;

    private LazyStringIndex(String[] classifierStrings, String metadataName, FileReader fileReader, int otherStringCount)
    {
        super(classifierStrings);
        this.metadataName = metadataName;
        this.fileReader = fileReader;
        this.otherStrings = new String[otherStringCount];
    }

    @Override
    protected String getOtherString(int index)
    {
        String string = this.otherStrings[index];
        if (string == null)
        {
            loadPartition(index);
            string = this.otherStrings[index];
        }
        return string;
    }

    private void loadPartition(int index)
    {
        int partitionStart = DistributedStringCache.getStartOfPartition(index);
        String filePath = DistributedMetadataFiles.getOtherStringsIndexPartitionFilePath(this.metadataName, partitionStart);
        String[] partition;
        try (Reader reader = this.fileReader.getReader(filePath))
        {
            partition = reader.readStringArray();
        }
        synchronized (this)
        {
            if (this.otherStrings[partitionStart] == null)
            {
                System.arraycopy(partition, 0, this.otherStrings, partitionStart, partition.length);
            }
        }
    }

    static LazyStringIndex fromFileReader(String metadataName, FileReader fileReader)
    {
        String[] classifierIds = readClassifierIds(metadataName, fileReader);
        int otherStringCount = readOtherStringCount(metadataName, fileReader);
        return new LazyStringIndex(classifierIds, metadataName, fileReader, otherStringCount);
    }

    private static String[] readClassifierIds(String metadataName, FileReader fileReader)
    {
        try (Reader reader = fileReader.getReader(DistributedMetadataFiles.getClassifierIdStringsIndexFilePath(metadataName)))
        {
            return readClassifierIds(reader);
        }
    }

    private static int readOtherStringCount(String metadataName, FileReader fileReader)
    {
        try (Reader reader = fileReader.getReader(DistributedMetadataFiles.getOtherStringsIndexFilePath(metadataName)))
        {
            return reader.readInt();
        }
    }
}
