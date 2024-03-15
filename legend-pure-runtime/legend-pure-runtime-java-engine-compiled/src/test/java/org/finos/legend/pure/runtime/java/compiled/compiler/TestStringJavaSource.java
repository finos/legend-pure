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

package org.finos.legend.pure.runtime.java.compiled.compiler;

import org.junit.Assert;
import org.junit.Test;

public class TestStringJavaSource
{
    @Test
    public void testHasPackageDeclaration()
    {
        Assert.assertTrue(StringJavaSource.hasPackageDeclaration("package a.b.c.d;\n\npublic class MyClass {}\n"));
        Assert.assertTrue(StringJavaSource.hasPackageDeclaration("\n\n\n\npackage valid.dec.line;\n\npublic class MyClass {}\n"));
        Assert.assertTrue(StringJavaSource.hasPackageDeclaration("// comment line 1\n// comment line 2\npackage e.f.g.h.i.j;\nclass MyClass {}\n"));
        Assert.assertTrue(StringJavaSource.hasPackageDeclaration("\r\n\r\n   \t package    \t\ta.b.c.d \t;\t\t\t\r\n"));
        Assert.assertTrue(StringJavaSource.hasPackageDeclaration("package    \t\ta.b.c.d \t;more stuff"));

        Assert.assertFalse(StringJavaSource.hasPackageDeclaration(""));
        Assert.assertFalse(StringJavaSource.hasPackageDeclaration("not even Java code"));
        Assert.assertFalse(StringJavaSource.hasPackageDeclaration("invalid package declaration.line;\n\npublic class MyClass {}\n"));
        Assert.assertFalse(StringJavaSource.hasPackageDeclaration("// package declaration.line;\n\npublic class MyClass {}\n"));
        Assert.assertFalse(StringJavaSource.hasPackageDeclaration("package\fa.b.c.d;\n"));
        Assert.assertFalse(StringJavaSource.hasPackageDeclaration("packagea.b.c.d;\n"));
        Assert.assertFalse(StringJavaSource.hasPackageDeclaration("\r\n\r\npackage    \r\n       a.b.c.d;\n"));
        Assert.assertFalse(StringJavaSource.hasPackageDeclaration("import a.b.c.d;\nimport e.f.g.h;\n\npublic class MyClass {}\n"));
    }
}
