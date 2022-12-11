// Copyright 2022 Goldman Sachs
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

package org.finos.legend.pure.ide.light.api.concept;

import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.MutableList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RenameConceptUtility
{
    private static final String CONCRETE_FUNCTION_DEFINITION = "ConcreteFunctionDefinition";
    private static final Pattern IDENTIFIER_REGEX = Pattern.compile("^([a-zA-Z0-9_][a-zA-Z0-9_$]*)");
    private static final Pattern SIGNATURE_SUFFIX_REGEX = Pattern.compile("_([a-z]|[A-Z]|[0-9]|_|$)*_(.)*", Pattern.DOTALL);

    private RenameConceptUtility()
    {
    }

    public static MutableList<? extends AbstractRenameConceptEntry> removeInvalidReplaceConceptEntry(final String[] sourceCodeLines, MutableList<? extends AbstractRenameConceptEntry> entries)
    {
        return entries.select(new Predicate<AbstractRenameConceptEntry>()
        {
            @Override
            public boolean accept(AbstractRenameConceptEntry replaceEdit)
            {
                if (replaceEdit.getConceptColumnIndex() < 0 || replaceEdit.getConceptColumnIndex() >= sourceCodeLines[replaceEdit.getConceptLineIndex()].length())
                {
                    return false;
                }
                Matcher matcher = IDENTIFIER_REGEX.matcher(sourceCodeLines[replaceEdit.getConceptLineIndex()].substring(replaceEdit.getConceptColumnIndex()));
                if (matcher.find())
                {
                    String identifierAtThisPoint = matcher.group(1);
                    boolean isExactMatch = identifierAtThisPoint.equals(replaceEdit.getConceptName());
                    boolean isSignatureOfOriginal = CONCRETE_FUNCTION_DEFINITION.equals(replaceEdit.getConceptType()) && identifierAtThisPoint.startsWith(replaceEdit.getConceptName()) && SIGNATURE_SUFFIX_REGEX.matcher(identifierAtThisPoint.substring(replaceEdit.getConceptName().length())).matches();
                    return isExactMatch || isSignatureOfOriginal;
                }
                return false;
            }
        });
    }

    public static String replace(String[] sourceCodeLines, MutableList<? extends AbstractRenameConceptEntry> entries)
    {
        entries.sortThisBy(AbstractRenameConceptEntry::getReplaceColumnIndex).sortThisBy(AbstractRenameConceptEntry::getReplaceLineIndex);
        StringBuilder stringBuilder = new StringBuilder();
        int counter = 0;
        for (int lineIndex = 0; lineIndex < sourceCodeLines.length; ++lineIndex)
        {
            int columnIndex = 0;
            while (counter < entries.size() && entries.get(counter).getReplaceLineIndex() == lineIndex)
            {
                AbstractRenameConceptEntry replaceEdit = entries.get(counter);
                stringBuilder.append(sourceCodeLines[lineIndex], columnIndex, replaceEdit.getReplaceColumnIndex()).append(replaceEdit.getNewReplaceString());
                columnIndex = replaceEdit.getReplaceColumnIndex() + replaceEdit.getOriginalReplaceString().length();
                counter++;
            }
            stringBuilder.append(sourceCodeLines[lineIndex].substring(columnIndex)).append("\n");
        }
        return stringBuilder.substring(0, stringBuilder.length() - 1);
    }
}
