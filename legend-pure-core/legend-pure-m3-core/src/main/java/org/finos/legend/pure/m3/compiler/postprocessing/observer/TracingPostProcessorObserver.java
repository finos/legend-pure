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

package org.finos.legend.pure.m3.compiler.postprocessing.observer;

import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.tuple.primitive.ObjectLongPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.tools.SafeAppendable;

import java.util.Arrays;

public class TracingPostProcessorObserver implements PostProcessorObserver
{
    private final SafeAppendable appendable;
    private final MutableStack<ObjectLongPair<CoreInstance>> startTimeStack = Stacks.mutable.empty();

    private TracingPostProcessorObserver(Appendable appendable)
    {
        this.appendable = SafeAppendable.wrap(appendable);
    }

    @Override
    public void startProcessing(CoreInstance instance)
    {
        long start = System.nanoTime();
        this.startTimeStack.push(PrimitiveTuples.pair(instance, start));
        int level = this.startTimeStack.size() - 1;
        String indent = spaces(level);
        String levelLabel = (level + 1) + ": ";
        String moreIndent = spaces(levelLabel.length());
        print(indent).print(levelLabel).print("START ").printInstance(instance).printLineBreak();
        print(indent).print(moreIndent).print("classifier: ").printClassifier(instance.getClassifier()).printLineBreak();
        print(indent).print(moreIndent).print("source info: ").printSourceInfo(instance.getSourceInformation()).printLineBreak();
    }

    @Override
    public void finishProcessing(CoreInstance instance, Exception e)
    {
        long end = System.nanoTime();
        if (this.startTimeStack.isEmpty())
        {
            return;
        }

        ObjectLongPair<CoreInstance> previous = this.startTimeStack.pop();
        if (previous.getOne() != instance)
        {
            this.startTimeStack.push(previous);
            return;
        }

        long start = previous.getTwo();
        int level = this.startTimeStack.size();
        String indent = spaces(level);
        String levelLabel = (level + 1) + ": ";
        String moreIndent = spaces(levelLabel.length());
        print(indent).print(levelLabel).print("END ").printDuration(end - start).print("s ").printInstance(instance).printLineBreak();
        if (e != null)
        {
            print(indent).print(moreIndent).print("exception class: ").print(e.getClass().toString()).printLineBreak();
            print(indent).print(moreIndent).print("exception message: ").print(e.getMessage()).printLineBreak();
        }
    }

    private TracingPostProcessorObserver print(String string)
    {
        this.appendable.append(string);
        return this;
    }

    private void printLineBreak()
    {
        print(System.lineSeparator());
    }

    private TracingPostProcessorObserver printInstance(CoreInstance instance)
    {
        return (instance instanceof PackageableElement) ? printPackageableElementPath(instance) : print(instance.getName());
    }

    private TracingPostProcessorObserver printClassifier(CoreInstance classifier)
    {
        return (classifier == null) ? print("null") : printPackageableElementPath(classifier);
    }

    private TracingPostProcessorObserver printPackageableElementPath(CoreInstance instance)
    {
        org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(this.appendable, instance);
        return this;
    }

    private TracingPostProcessorObserver printSourceInfo(SourceInformation sourceInfo)
    {
        if (sourceInfo == null)
        {
            return print("null");
        }

        sourceInfo.appendMessage(this.appendable);
        return this;
    }

    private TracingPostProcessorObserver printDuration(long durationInNanos)
    {
        this.appendable.append(getDurationString(durationInNanos));
        return this;
    }

    private static String getDurationString(long durationInNanos)
    {
        if (durationInNanos == 0)
        {
            return "0.000000000";
        }

        boolean negative = durationInNanos < 0;
        String string = Long.toString(durationInNanos);
        int secondsCharCount = string.length() - (negative ? 10 : 9);
        if (secondsCharCount <= 0)
        {
            int expectedLen = negative ? 12 : 11;
            StringBuilder builder = new StringBuilder(expectedLen);
            builder.append(negative ? "-0." : "0.");
            for (int i = secondsCharCount; i < 0; i++)
            {
                builder.append('0');
            }
            if (negative)
            {
                builder.append(string, 1, string.length());
            }
            else
            {
                builder.append(string);
            }
            return builder.toString();
        }

        StringBuilder builder = new StringBuilder(string.length() + 1 + ((secondsCharCount - 1) / 3));
        int firstSeparatorIndex = ((secondsCharCount - 1) % 3) + 1 + (negative ? 1 : 0);
        builder.append(string, 0, firstSeparatorIndex);
        for (int i = firstSeparatorIndex; i < secondsCharCount; i += 3)
        {
            builder.append(',').append(string, i, i + 3);
        }
        return builder.append('.').append(string, string.length() - 9, string.length()).toString();
    }

    private String spaces(int len)
    {
        if (len == 0)
        {
            return "";
        }

        char[] chars = new char[len];
        Arrays.fill(chars, ' ');
        return new String(chars);
    }

    public static TracingPostProcessorObserver newObserver(Appendable appendable)
    {
        return new TracingPostProcessorObserver(appendable);
    }
}
