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

package org.finos.legend.pure.runtime.java.interpreted.testHelper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.multimap.set.MutableSetMultimap;
import org.eclipse.collections.impl.factory.Multimaps;
import org.finos.legend.pure.runtime.java.interpreted.FunctionExecutionInterpreted;

public class CodeCoverageGenerator
{
    private static final String HTML_TEMPLATE = "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <title>Green Overlapping Highlights</title>\n" +
            "    <style>\n" +
            "    pre {\n" +
            "      white-space: pre-wrap;\n" +
            "      font-family: monospace;\n" +
            "    }\n" +
            "    .highlight-1 { background-color: #b9f6ca; }\n" +
            "    .highlight-2 { background-color: #69f0ae; }\n" +
            "    .highlight-3 { background-color: #00e676; }\n" +
            "    .highlight-4 { background-color: #00c853; }\n" +
            "    .highlight-5 { background-color: #009624; }\n" +
            "    .highlight-6 { background-color: #006400; color: white; }\n" +
            "  </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "<pre id=\"textDisplay\"></pre>\n" +
            "\n" +
            "<script>\n" +
            "    const text = `%code%`;\n" +
            "\n" +
            "    const highlights = [%coverage%];\n" +
            "\n" +
            "    function applyOverlappingHighlights(text, highlights) {\n" +
            "      const lines = text.split('\\n');\n" +
            "      const lineCharDepth = lines.map(line => new Array(line.length).fill(0));\n" +
            "\n" +
            "      // Mark depth per character\n" +
            "      highlights.forEach(({ start, end }) => {\n" +
            "        for (let line = start.line; line <= end.line; line++) {\n" +
            "          const startChar = (line === start.line) ? start.char : 0;\n" +
            "          const endChar = (line === end.line) ? end.char : lines[line].length;\n" +
            "          for (let i = startChar; i < endChar; i++) {\n" +
            "            lineCharDepth[line][i]++;\n" +
            "          }\n" +
            "        }\n" +
            "      });\n" +
            "\n" +
            "      // Build HTML with spans based on depth changes\n" +
            "      const resultLines = lines.map((line, lineIndex) => {\n" +
            "        const chars = line.split('');\n" +
            "        let result = '';\n" +
            "        let currentDepth = 0;\n" +
            "\n" +
            "        for (let i = 0; i <= chars.length; i++) {\n" +
            "          const nextDepth = lineCharDepth[lineIndex][i] || 0;\n" +
            "\n" +
            "          if (nextDepth !== currentDepth) {\n" +
            "            if (currentDepth > 0) result += `</span>`;\n" +
            "            if (nextDepth > 0) {\n" +
            "              const depthClass = Math.min(nextDepth, 6);\n" +
            "              result += `<span class=\"highlight-${depthClass}\">`;\n" +
            "            }\n" +
            "            currentDepth = nextDepth;\n" +
            "          }\n" +
            "\n" +
            "          if (i < chars.length) result += chars[i];\n" +
            "        }\n" +
            "\n" +
            "        return result;\n" +
            "      });\n" +
            "\n" +
            "      return resultLines.join('\\n');\n" +
            "    }\n" +
            "\n" +
            "    document.getElementById('textDisplay').innerHTML = applyOverlappingHighlights(text, highlights);\n" +
            "  </script>\n" +
            "</body>\n" +
            "</html>\n";

    public static void main(String[] args) throws Exception
    {
//        generateInterpreted("./target/coverage/report/", "./target/coverage/data/");
        generateInterpreted(args[0], args[1]);
    }

    public static void generateInterpreted(String targetDir, String coverageDir) throws Exception
    {
        Path coverageDirPath = Paths.get(coverageDir);

        MutableSetMultimap<String, String> sourceToCoverage = Multimaps.mutable.set.<String, String>with().asSynchronized();;

        try (Stream<Path> pathStream = Files.find(coverageDirPath, Integer.MAX_VALUE, (p, x) -> p.getFileName().toString().endsWith(".purecov")))
        {
            Stream<String> coverageLines = pathStream.flatMap(x ->
            {
                try
                {
                    return Files.lines(x);
                }
                catch (IOException e)
                {
                    throw new UncheckedIOException(e);
                }
            });

            coverageLines.parallel().forEach(x ->
            {
                String[] parts = StringUtils.split(x);
                String sourceId = parts[0];
                String sLine = parts[1];
                String sColumn = parts[2];
                String eLine = parts[3];
                String eColumn = parts[4];

                sourceToCoverage.put(sourceId, String.format("{ start: { line: %s, char: %s }, end: { line: %s, char: %s } }", sLine, sColumn, eLine, eColumn));
            });
        }

        FunctionExecutionInterpreted functionExecutionInterpreted = PureTestBuilderInterpreted.getFunctionExecutionInterpreted();

        sourceToCoverage.keyMultiValuePairsView().toList().stream().parallel().forEach((pair) ->
        {
            String source = pair.getOne();
            RichIterable<String> cov = pair.getTwo();
            String content = functionExecutionInterpreted.getPureRuntime().getCodeStorage().getContentAsText(source);
            String allCov = String.join(",", cov);

            String covForSource = HTML_TEMPLATE.replace("%code%", content)
                    .replace("%coverage%", allCov);

            try
            {
                Path reportFile = Paths.get(targetDir + source + ".html").toAbsolutePath();
                Files.createDirectories(reportFile.getParent());
                Files.write(reportFile, covForSource.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        });

        String indexContent = sourceToCoverage.keysView()
                .toSortedList()
                .collect(x -> "<li><a href=\"" + Paths.get("./" + x + ".html") + "\" target=\"_blank\">" + x + "</a></li>")
                .makeString("\n");

        String reportIndex = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>File List</title>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>Files With Coverage</h1>\n" +
                "    <ul>\n" + indexContent + "</ul>\n" +
                "</body>\n" +
                "</html>\n";

        Path reportFile = Paths.get(targetDir + "/index.html").toAbsolutePath();
        Files.write(reportFile, reportIndex.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
}
