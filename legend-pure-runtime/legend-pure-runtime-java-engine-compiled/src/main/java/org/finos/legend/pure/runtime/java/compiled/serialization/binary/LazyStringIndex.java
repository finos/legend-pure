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

import java.util.concurrent.atomic.AtomicReferenceArray;

public class LazyStringIndex extends StringIndex
{
    private final String metadataName;
    private final FileReader fileReader;
    private final AtomicReferenceArray<String> otherStrings;

    private LazyStringIndex(String[] classifierStrings, String metadataName, FileReader fileReader, int otherStringCount)
    {
        super(classifierStrings);
        this.metadataName = metadataName;
        this.fileReader = fileReader;
        this.otherStrings = new AtomicReferenceArray<>(otherStringCount);
    }

    @Override
    protected String getOtherString(int index)
    {
        String string = this.otherStrings.get(index);
        if (string != null)
        {
            return string;
        }
        loadPartition(index);
        return this.otherStrings.get(index);
    }

    private void loadPartition(int index)
    {
        int partitionStart = DistributedStringCache.getStartOfPartition(index);
        String filePath = DistributedMetadataHelper.getOtherStringsIndexPartitionFilePath(this.metadataName, partitionStart);
        try (Reader reader = this.fileReader.getReader(filePath))
        {
            int partitionLength = reader.readInt();
            for (int i = partitionStart, end = partitionStart + partitionLength; i < end; i++)
            {
                String string = reader.readString();
                if (!this.otherStrings.compareAndSet(i, null, string) && ((i >= index) || (this.otherStrings.get(index) != null)))
                {
                    // If the compareAndSet fails, it means one or more other threads are already loading this partition.
                    // If the index we want has already been loaded, we can stop and leave the loading of the rest of the
                    // partition to the other threads.
                    return;
                }
            }
        }
    }

    public static LazyStringIndex fromFileReader(String metadataName, FileReader fileReader)
    {
        String[] classifierIds = readClassifierIds(metadataName, fileReader);
        int otherStringCount = readOtherStringCount(metadataName, fileReader);
        return new LazyStringIndex(classifierIds, metadataName, fileReader, otherStringCount);
    }

    private static String[] readClassifierIds(String metadataName, FileReader fileReader)
    {
        try (Reader reader = fileReader.getReader(DistributedMetadataHelper.getClassifierIdStringsIndexFilePath(metadataName)))
        {
            return readClassifierIds(reader);
        }
    }

    private static int readOtherStringCount(String metadataName, FileReader fileReader)
    {
        try (Reader reader = fileReader.getReader(DistributedMetadataHelper.getOtherStringsIndexFilePath(metadataName)))
        {
            return reader.readInt();
        }
    }
}
