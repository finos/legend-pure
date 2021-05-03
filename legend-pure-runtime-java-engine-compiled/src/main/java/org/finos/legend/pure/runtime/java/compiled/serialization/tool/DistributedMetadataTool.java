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

package org.finos.legend.pure.runtime.java.compiled.serialization.tool;

import org.finos.legend.pure.runtime.java.compiled.serialization.binary.DistributedBinaryGraphDeserializer;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.EnumRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Obj;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.ObjRef;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.Primitive;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueMany;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueOne;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.PropertyValueVisitor;
import org.finos.legend.pure.runtime.java.compiled.serialization.model.RValueVisitor;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.zip.ZipFile;

public class DistributedMetadataTool implements Closeable
{
    private final ZipFile zipFile;
    private final DistributedBinaryGraphDeserializer deserializer;
    private final BufferedReader in;
    private final PrintStream out;

    private String prefix = "";


    public static void main(String[] args) throws IOException
    {
        String zipPath = args[0];
        try (DistributedMetadataTool tool = new DistributedMetadataTool(zipPath))
        {
            tool.repl();
        }
    }

    private DistributedMetadataTool(String zipPath)
    {
        try
        {
            this.zipFile = new ZipFile(new File(zipPath));
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not open zip file at: " + zipPath, e);
        }
        this.deserializer = DistributedBinaryGraphDeserializer.fromZip(this.zipFile);
        this.in = new BufferedReader(new InputStreamReader(System.in));
        this.out = System.out;
    }

    private void repl()
    {
        while (true)
        {
            print("How can I help:");
            String line = readLine();
            if (line.equals("quit"))
            {
                return;
            }

            if (line.equals("classifiers"))
            {
                classifiers();
            }
            else if (line.startsWith("instance"))
            {
                String[] parts = line.split(" ");
                instance(parts[1], parts[2]);
            }
            print("");
        }
    }

    private String readLine()
    {
        try
        {
            return this.in.readLine().trim();
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private void indent()
    {
        this.prefix += "   ";
    }

    private void outdent()
    {
        this.prefix = this.prefix.substring(0, this.prefix.length() - 3);
    }

    private void print(String line)
    {
        this.out.println(this.prefix+line);
    }

    private void printf(String format, Object... args)
    {
        this.out.printf(this.prefix+format+'\n', args);
    }

    private void classifiers()
    {
        this.deserializer.getClassifiers().toList().sortThis().forEach(this.out::println);
    }

    private void instance(String classifier, String id)
    {
        try
        {
            if (this.deserializer.hasInstance(classifier, id))
            {
                Obj instance = this.deserializer.getInstance(classifier, id);
                new ObjPrinter(instance).printObj();
            }
            else
            {
                this.out.println("Instance not found");
            }
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() throws IOException
    {
        this.zipFile.close();
    }

    private class ObjPrinter implements PropertyValueVisitor, RValueVisitor
    {
        private final Obj obj;

        ObjPrinter(Obj obj)
        {
            this.obj = obj;
        }

        void printObj()
        {
            printf("%s (%s)", this.obj.getName(), this.obj.getClassifier());
            indent();
            if (this.obj.getSourceInformation() != null)
            {
                printf("sourceInfo = %s", this.obj.getSourceInformation().toString());
            }
            printf("identifier = %s", this.obj.getIdentifier());
            printf("properties =");
            indent();
            this.obj.getPropertyValues().forEach(v -> v.visit(this));
            outdent();
            outdent();
        }

        @Override
        public Object accept(PropertyValueMany many)
        {
            indent();
            print(many.getProperty());
            indent();
            many.getValues().forEach(v->v.visit(this));
            outdent();
            outdent();
            return null;
        }

        @Override
        public Object accept(PropertyValueOne one)
        {
            indent();
            print(one.getProperty());
            indent();
            one.getValue().visit(this);
            outdent();
            outdent();
            return null;
        }

        @Override
        public Object accept(Primitive primitive)
        {
            print(primitive.toString());
            return null;
        }

        @Override
        public Object accept(ObjRef objRef)
        {
            printf("instance %s %s", objRef.getClassifierId(), objRef.getId());
            return null;
        }

        @Override
        public Object accept(EnumRef enumRef)
        {
            printf("enum %s %s", enumRef.getEnumerationId(), enumRef.getEnumerationId());
            return null;
        }
    }
}
