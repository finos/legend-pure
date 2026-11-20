// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.runtime.java.extension.store.relational.shared;

import org.apache.commons.csv.CSVRecord;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Stacks;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class TestCsvReader
{
    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testFileTooLarge() throws IOException
    {
        String filePath = writeCsvFile();
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> CsvReader.readCsv(null, null, filePath, 0, null, Stacks.mutable.empty()));
        assertContains("File is too large, file was 0.00 Mb, limit is 0 Mb", e.getInfo());
    }

    @Test
    public void testFileTooLargeWithGermanDefaultLocale() throws IOException
    {
        // With a default locale such as de-DE, String.format writes a comma as the decimal separator
        String filePath = writeCsvFile();
        Locale defaultLocale = Locale.getDefault();
        Locale displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
        Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.forLanguageTag("de-DE"));
        PureExecutionException e;
        try
        {
            e = Assert.assertThrows(PureExecutionException.class, () -> CsvReader.readCsv(null, null, filePath, 0, null, Stacks.mutable.empty()));
        }
        finally
        {
            Locale.setDefault(defaultLocale);
            Locale.setDefault(Locale.Category.DISPLAY, displayLocale);
            Locale.setDefault(Locale.Category.FORMAT, formatLocale);
        }
        assertContains("File is too large, file was 0.00 Mb, limit is 0 Mb", e.getInfo());
    }

    @Test
    public void testFileIsReadAsUtf8() throws IOException
    {
        // Greek letters are in neither windows-1252 nor ISO-8859-1, default charsets of JVMs before Java 18
        Path file = this.tmp.newFile("data.csv").toPath();
        Files.write(file, "name\r\n\u03B1\u03B2\u03B3\r\n".getBytes(StandardCharsets.UTF_8));

        Iterable<CSVRecord> records = CsvReader.readCsv(null, null, file.toString(), null, Stacks.mutable.empty());
        Assert.assertEquals(Lists.mutable.with(Lists.mutable.with("name"), Lists.mutable.with("\u03B1\u03B2\u03B3")), toLists(records));
    }

    private String writeCsvFile() throws IOException
    {
        Path file = this.tmp.newFile("data.csv").toPath();
        Files.write(file, "id,name\n1,a\n".getBytes(StandardCharsets.UTF_8));
        return file.toString();
    }

    private static void assertContains(String expected, String actual)
    {
        if ((actual == null) || !actual.contains(expected))
        {
            Assert.fail("Expected to find \"" + expected + "\" in: " + actual);
        }
    }

    private static MutableList<MutableList<String>> toLists(Iterable<CSVRecord> records)
    {
        MutableList<MutableList<String>> result = Lists.mutable.empty();
        for (CSVRecord record : records)
        {
            MutableList<String> values = Lists.mutable.empty();
            record.forEach(values::add);
            result.add(values);
        }
        return result;
    }
}
