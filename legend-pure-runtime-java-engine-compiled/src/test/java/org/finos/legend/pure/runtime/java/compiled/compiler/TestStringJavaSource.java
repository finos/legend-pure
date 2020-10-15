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

        Assert.assertFalse(StringJavaSource.hasPackageDeclaration(""));
        Assert.assertFalse(StringJavaSource.hasPackageDeclaration("not even Java code"));
        Assert.assertFalse(StringJavaSource.hasPackageDeclaration("invalid package declaration.line;\n\npublic class MyClass {}\n"));
        Assert.assertFalse(StringJavaSource.hasPackageDeclaration("// package declaration.line;\n\npublic class MyClass {}\n"));
        Assert.assertFalse(StringJavaSource.hasPackageDeclaration("package\fa.b.c.d;\n"));
        Assert.assertFalse(StringJavaSource.hasPackageDeclaration("\r\n\r\npackage    \r\n       a.b.c.d;\n"));
        Assert.assertFalse(StringJavaSource.hasPackageDeclaration("import a.b.c.d;\nimport e.f.g.h;\n\npublic class MyClass {}\n"));
    }
}
