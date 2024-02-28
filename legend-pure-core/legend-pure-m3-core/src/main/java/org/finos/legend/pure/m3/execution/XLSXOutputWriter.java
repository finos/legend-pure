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

package org.finos.legend.pure.m3.execution;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class XLSXOutputWriter implements OutputWriter<Object>
{
    private static final String XLSX_TEMPLATE = "/xlsx_template.xlsx";
    private static final String SHEET_PATH = "xl/worksheets/sheet1.xml";

    private final OutputWriter writer;

    public XLSXOutputWriter(OutputWriter writer)
    {
        this.writer = writer;
    }

    @Override
    public void write(Object result, OutputStream outputStream) throws IOException
    {
        try (InputStream is = XLSXOutputWriter.class.getResourceAsStream(XLSX_TEMPLATE);
             ZipInputStream zis = new ZipInputStream(is);
             ZipOutputStream zos = new ZipOutputStream(outputStream))
        {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null)
            {
                zos.putNextEntry(new ZipEntry(zipEntry.getName()));
                IOUtils.copy(zis, zos);
                zos.closeEntry();
            }

            ZipEntry sheet1 = new ZipEntry(SHEET_PATH);
            zos.putNextEntry(sheet1);

            writer.write(result, zos);

            zos.closeEntry();
            zos.finish();
        }
    }
}