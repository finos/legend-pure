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

package org.finos.legend.pure.runtime.java.compiled.runtime.compiler;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.tests.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.runtime.java.compiled.compiler.ClassJavaSource;
import org.finos.legend.pure.runtime.java.compiled.compiler.MemoryClassLoader;
import org.finos.legend.pure.runtime.java.compiled.compiler.MemoryFileManager;
import org.finos.legend.pure.runtime.java.compiled.compiler.PureJavaCompiler;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiled;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistryLoader;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class TestMemoryFileManager extends AbstractPureTestWithCoreCompiled
{
    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder();

    @BeforeClass
    public static void setUp()
    {
        setUpRuntime(getFunctionExecution(), JavaModelFactoryRegistryLoader.loader());
    }

    @Test
    public void testLoadClassesFromZipInputStream() throws IOException
    {
        compileTestSource("Class Person\n" +
                "{\n" +
                "   lastName:String[1];\n" +
                "}\n" +
                "function testPrint():Nil[0]\n" +
                "{\n" +
                "    s(Person);\n" +
                "}\n" +
                "function s(a:Any[1]):Nil[0]" +
                "{" +
                "   []" +
                "}");

        PureJavaCompiler compiler = ((FunctionExecutionCompiled) functionExecution).getJavaCompiler();
        MutableMap<URI, ClassJavaSource> expectedSourcesByURI = compiler.getFileManager().getAllClassJavaSources(true).groupByUniqueKey(SimpleJavaFileObject::toUri, Maps.mutable.empty());
        Verify.assertNotEmpty(expectedSourcesByURI);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream jarStream = new JarOutputStream(bytes))
        {
            compiler.writeClassJavaSourcesToJar(jarStream);
        }

        MemoryFileManager fileManager = new MemoryFileManager(ToolProvider.getSystemJavaCompiler());
        try (JarInputStream jarStream = new JarInputStream(new ByteArrayInputStream(bytes.toByteArray())))
        {
            fileManager.loadClassesFromZipInputStream(jarStream);
        }

        MutableMap<URI, ClassJavaSource> actualSourcesByURI = fileManager.getAllClassJavaSources(true).groupByUniqueKey(SimpleJavaFileObject::toUri, Maps.mutable.empty());
        Verify.assertSetsEqual(expectedSourcesByURI.keySet(), actualSourcesByURI.keySet());
        for (URI uri : expectedSourcesByURI.keysView())
        {
            ClassJavaSource expectedSource = expectedSourcesByURI.get(uri);
            ClassJavaSource actualSource = actualSourcesByURI.get(uri);
            Assert.assertArrayEquals(uri.toString(), expectedSource.getBytes(), actualSource.getBytes());
        }
    }

    @Test
    public void testSourceOnClassPathIsReadAsUtf8() throws Exception
    {
        // With no source path, javac also looks for sources on the class path. Greek letters are in neither
        // windows-1252 nor ISO-8859-1, default charsets of JVMs before Java 18.
        Path classPath = this.tmp.newFolder().toPath();
        Path greeting = Files.createDirectories(classPath.resolve("test/charset")).resolve("Greeting.java");
        Files.write(greeting, ("package test.charset;\n" +
                "public class Greeting\n" +
                "{\n" +
                "    public static final String TEXT = \"\u03B1\u03B2\u03B3\";\n" +
                "}\n").getBytes(StandardCharsets.UTF_8));
        StringJavaSource user = StringJavaSource.newStringJavaSource("test.charset", "User", "package test.charset;\n" +
                "public class User\n" +
                "{\n" +
                "    public static String text()\n" +
                "    {\n" +
                "        return Greeting.TEXT;\n" +
                "    }\n" +
                "}\n", false);

        MemoryFileManager fileManager = new MemoryFileManager(ToolProvider.getSystemJavaCompiler());
        PureJavaCompiler.compile(Lists.immutable.with(user), fileManager, classPath.toString(), null);

        Class<?> userClass = new MemoryClassLoader(fileManager, TestMemoryFileManager.class.getClassLoader()).loadClass("test.charset.User");
        Assert.assertEquals("\u03B1\u03B2\u03B3", userClass.getMethod("text").invoke(null));
    }

    protected static FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}
