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
    private final FileReader fileReader;
    private final String[] otherStrings;

    private LazyStringIndex(String[] classifierStrings, FileReader fileReader, int otherStringCount)
    {
        super(classifierStrings);
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
        String filePath = DistributedStringCache.getOtherStringIndexPartitionFilePath(partitionStart);
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

    static LazyStringIndex fromFileReader(FileReader fileReader)
    {
        String[] classifierIds = readClassifierIds(fileReader);
        int otherStringCount = readOtherStringCount(fileReader);
        return new LazyStringIndex(classifierIds, fileReader, otherStringCount);
    }

    private static String[] readClassifierIds(FileReader fileReader)
    {
        try (Reader reader = fileReader.getReader(DistributedStringCache.CLASSIFIER_ID_INDEX_FILE_PATH))
        {
            return readClassifierIds(reader);
        }
    }

    private static int readOtherStringCount(FileReader fileReader)
    {
        try (Reader reader = fileReader.getReader(DistributedStringCache.OTHER_STRING_INDEX_METADATA_FILE_PATH))
        {
            return reader.readInt();
        }
    }
}
