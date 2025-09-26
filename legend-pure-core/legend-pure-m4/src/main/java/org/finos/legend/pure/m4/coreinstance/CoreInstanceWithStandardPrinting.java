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

package org.finos.legend.pure.m4.coreinstance;

public interface CoreInstanceWithStandardPrinting extends CoreInstance
{
    @Override
    default void printFull(Appendable appendable, String tab)
    {
        CoreInstanceStandardPrinter.printFull(appendable, this, tab);
    }

    @Override
    default void print(Appendable appendable, String tab, int max)
    {
        CoreInstanceStandardPrinter.print(appendable, this, tab, max);
    }

    @Override
    default void printWithoutDebug(Appendable appendable, String tab, int max)
    {
        CoreInstanceStandardPrinter.printWithoutDebug(appendable, this, tab, max);
    }
}
