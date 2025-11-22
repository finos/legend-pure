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

package org.finos.legend.pure.m3.tests.function.base.lang;

import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractTestRawEvalProperty extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void objectDoesntHavePropertyFail()
    {
        compileTestSource("fromString.pure",
                "Class Person\n" +
                "{\n" +
                "   name: String[1];\n" +
                "}\n" +
                "Class Alien\n" +
                "{\n" +
                "   species: String[1];\n" +
                "}\n" +
                "function test():Any[*]\n" +
                "{\n" +
                "   let person = ^Person(name = 'Obi Wan');\n" +
                "   let alien = ^Alien(species='Wookiee');\n" +
                "   let property = Person.properties->filter(p | $p.name == 'name')->toOne();\n" +
                "   $property->rawEvalProperty($alien);\n" +
                "}");
        PureExecutionException e = Assert.assertThrows(PureExecutionException.class, () -> execute("test():Any[*]"));
        assertPureException(PureExecutionException.class, "Can't find the property 'name' in the class Alien", "fromString.pure", 14, 15, e);
    }
}
