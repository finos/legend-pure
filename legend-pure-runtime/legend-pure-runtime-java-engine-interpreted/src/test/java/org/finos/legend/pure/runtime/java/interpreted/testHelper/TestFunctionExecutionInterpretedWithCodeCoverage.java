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

package org.finos.legend.pure.runtime.java.interpreted.testHelper;

import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TestFunctionExecutionInterpretedWithCodeCoverage extends AbstractPureTestWithCoreCompiled
{
    private static final Pattern COVERAGE_LINE = Pattern.compile("\\S+( -?[0-9]+){4}");

    @ClassRule
    public static final TemporaryFolder TMP = new TemporaryFolder();

    private static Path coverageDirectory;

    @BeforeClass
    public static void setUp() throws IOException
    {
        coverageDirectory = TMP.newFolder("coverage").toPath();
        setUpRuntime(new FunctionExecutionInterpretedWithCodeCoverage(coverageDirectory));
    }

    @After
    public void cleanRuntime()
    {
        runtime.delete("fromString.pure");
        runtime.compile();
    }

    @Test
    public void testCoverageWithArabicDefaultLocale() throws IOException
    {
        // With a default locale such as ar-EG, String.format writes Arabic-Indic digits, which readers of coverage
        // files do not expect
        compileTestSource("fromString.pure", "function test::coverage::answer():Integer[1]\n{\n   42\n}\n");
        Locale defaultLocale = Locale.getDefault();
        Locale displayLocale = Locale.getDefault(Locale.Category.DISPLAY);
        Locale formatLocale = Locale.getDefault(Locale.Category.FORMAT);
        Locale.setDefault(Locale.forLanguageTag("ar-EG"));
        try
        {
            execute("test::coverage::answer():Integer[1]");
        }
        finally
        {
            Locale.setDefault(defaultLocale);
            Locale.setDefault(Locale.Category.DISPLAY, displayLocale);
            Locale.setDefault(Locale.Category.FORMAT, formatLocale);
        }

        List<String> lines = readCoverageLines();
        Assert.assertTrue("Should cover fromString.pure, found " + lines, lines.stream().anyMatch(l -> l.startsWith("fromString.pure ")));
        List<String> malformed = lines.stream().filter(l -> !COVERAGE_LINE.matcher(l).matches()).collect(Collectors.toList());
        Assert.assertEquals(new ArrayList<>(), malformed);
    }

    private static List<String> readCoverageLines() throws IOException
    {
        List<Path> files;
        try (Stream<Path> paths = Files.list(coverageDirectory))
        {
            files = paths.filter(p -> p.getFileName().toString().endsWith(".purecov")).collect(Collectors.toList());
        }
        List<String> lines = new ArrayList<>();
        for (Path file : files)
        {
            lines.addAll(Files.readAllLines(file, StandardCharsets.UTF_8));
        }
        return lines;
    }
}
