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

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.factory.Maps;
import org.eclipse.collections.impl.test.Verify;
import org.finos.legend.pure.m3.AbstractPureTestWithCoreCompiled;
import org.finos.legend.pure.m3.execution.FunctionExecution;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiled;
import org.finos.legend.pure.runtime.java.compiled.execution.FunctionExecutionCompiledBuilder;
import org.junit.Assert;
import org.junit.Test;

import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

public class TestMemoryFileManager extends AbstractPureTestWithCoreCompiled
{
    @Test
    public void testLoadClassesFromZipInputStream() throws IOException
    {
        compileTestSource("Class Person\n" +
                          "{\n" +
                          "   lastName:String[1];\n" +
                          "}\n" +
                          "function testPrint():Nil[0]\n" +
                          "{\n" +
                          "    print(Person,3);\n" +
                          "}\n");

        PureJavaCompiler compiler = ((FunctionExecutionCompiled)this.functionExecution).getJavaCompiler();
        Function<ClassJavaSource, URI> getURI = new Function<ClassJavaSource, URI>()
        {
            @Override
            public URI valueOf(ClassJavaSource source)
            {
                return source.toUri();
            }
        };
        MutableMap<URI, ClassJavaSource> expectedSourcesByURI = compiler.getFileManager().getAllClassJavaSources(true).groupByUniqueKey(getURI, Maps.mutable.<URI, ClassJavaSource>empty());
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

        MutableMap<URI, ClassJavaSource> actualSourcesByURI = fileManager.getAllClassJavaSources(true).groupByUniqueKey(getURI, Maps.mutable.<URI, ClassJavaSource>empty());
        Verify.assertSetsEqual(expectedSourcesByURI.keySet(), actualSourcesByURI.keySet());
        for (URI uri : expectedSourcesByURI.keysView())
        {
            ClassJavaSource expectedSource = expectedSourcesByURI.get(uri);
            ClassJavaSource actualSource = actualSourcesByURI.get(uri);
            Assert.assertArrayEquals(uri.toString(), expectedSource.getBytes(), actualSource.getBytes());
        }
    }

    @Override
    protected FunctionExecution getFunctionExecution()
    {
        return new FunctionExecutionCompiledBuilder().build();
    }
}
