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

package org.finos.legend.pure.runtime.java.extension.store.relational;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.bouncycastle.util.Arrays;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class CsvReader
{
    protected static final int ONE_MB_SIZE_LIMIT = 1;
    protected static final long MEGA_BYTE = 1024 * 1024L;

    public static Iterable<CSVRecord> readCsv(CodeStorage codeStorage, SourceInformation sourceForError, String filePath,
                                              Integer rowLimit)
    {
        return readCsv(codeStorage, sourceForError, filePath, ONE_MB_SIZE_LIMIT, rowLimit);
    }

    public static Iterable<CSVRecord> readCsv(CodeStorage codeStorage, SourceInformation sourceForError, String filePath,
                                              int sizeLimitMegabytes, Integer rowLimit)
    {
        try
        {
            long sizeLimitBytes = sizeLimitMegabytes * MEGA_BYTE;
            CSVParser csvParser;
            if (codeStorage != null && codeStorage.exists(filePath))
            {
                String file = codeStorage.getContentAsText(filePath);
                if (file.getBytes(StandardCharsets.UTF_8).length > sizeLimitBytes)
                {
                    throw new PureExecutionException("File is too large, file was " + String.format("%.2f", file.length() * 1.0 / MEGA_BYTE) + " Mb, limit is " + sizeLimitMegabytes + " Mb");
                }
                csvParser = CSVParser.parse(file, CSVFormat.EXCEL);
            }
            else
            {
                File file = new File(filePath);
                if (file.exists())
                {
                    if (file.length() > sizeLimitBytes)
                    {
                        throw new PureExecutionException("File is too large, file was " + String.format("%.2f", file.length() * 1.0 / MEGA_BYTE) + " Mb, limit is " + sizeLimitMegabytes + " Mb");
                    }
                    csvParser = CSVParser.parse(file, Charset.defaultCharset(), CSVFormat.EXCEL);
                }
                else
                {
                    throw new PureExecutionException(sourceForError, "No CSV file found with path '" + filePath + "'");
                }
            }

            return rowLimit == null ? csvParser : LazyIterate.take(csvParser, rowLimit);

        }
        catch (Exception e)
        {
            throw new PureExecutionException(sourceForError, "Unable to read the CSV file '" + filePath + "' " + e.getMessage(), e);
        }
    }

    public static CSVParser readCsv(Reader csvReader, CSVFormat format, long skipStartLines) throws IOException
    {
        skipStartLines(csvReader, format.getRecordSeparator(), skipStartLines);
        return format.parse(csvReader);
    }

    public static CSVParser readCsv(String csvString, SourceInformation sourceForError, CSVFormat csvFormat, long skipStartLines)
    {
        try
        {
            StringReader csvReader = new StringReader(csvString);
            skipStartLines(csvReader, csvFormat.getRecordSeparator(), skipStartLines);
            return csvFormat.parse(csvReader);
        }
        catch (IOException e)
        {
            throw new PureExecutionException(sourceForError, "Unable to read the CSV string " + e.getMessage(), e);
        }
    }

    private static void skipStartLines(Reader csvReader, String recordSeparator, long skipStartLines) throws IOException
    {
        byte[] separatorBytes = recordSeparator.getBytes();
        for (int line = 0; line < skipStartLines; line++)
        {
            byte[] buffer = new byte[separatorBytes.length];
            for (int i = 0; i < buffer.length; i++)
            {
                buffer[i] = readByte(csvReader);
            }
            while (!Arrays.areEqual(separatorBytes, buffer))
            {
                System.arraycopy(buffer, 1, buffer, 0, buffer.length - 1);
                buffer[buffer.length - 1] = readByte(csvReader);
            }
        }
    }

    private static byte readByte(Reader csvReader) throws IOException
    {
        int ch = csvReader.read();
        if (ch == -1)
        {
            throw new IOException("End of file encountered while skipping start lines");
        }
        return (byte)ch;
    }
}
