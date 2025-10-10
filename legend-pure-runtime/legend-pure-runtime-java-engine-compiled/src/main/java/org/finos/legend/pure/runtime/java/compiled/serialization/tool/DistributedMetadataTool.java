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

import java.nio.charset.Charset;
import org.eclipse.collections.impl.utility.ArrayIterate;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.util.zip.ZipFile;

public class DistributedMetadataTool
{
    private final DistributedBinaryGraphDeserializer deserializer;
    private final BufferedReader in;
    private final PrintStream out;

    private String prefix = "";

    public static void main(String[] args) throws IOException
    {
        if (args.length < 1)
        {
            throw new RuntimeException("Expected at least 1 argument, got " + args.length);
        }
        String zipPath = args[0];
        try (ZipFile zipFile = new ZipFile(new File(zipPath)))
        {
            DistributedBinaryGraphDeserializer.Builder builder = DistributedBinaryGraphDeserializer.newBuilder(zipFile).withoutObjValidation();
            if (args.length > 1)
            {
                ArrayIterate.forEach(args, 1, args.length, builder::withMetadataNames);
            }
            DistributedBinaryGraphDeserializer deserializer = builder.build();
            new DistributedMetadataTool(deserializer).repl();
        }
    }

    private DistributedMetadataTool(DistributedBinaryGraphDeserializer deserializer)
    {
        this.deserializer = deserializer;
        this.in = new BufferedReader(new InputStreamReader(System.in, Charset.defaultCharset()));
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
        this.out.println(this.prefix + line);
    }

    private void printf(String format, Object... args)
    {
        this.out.printf(this.prefix + format + "%n", args);
    }

    private void classifiers()
    {
        this.deserializer.getClassifiers().toSortedList().forEach(this.out::println);
    }

    private void instance(String classifier, String id)
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

    private class ObjPrinter implements PropertyValueVisitor<Void>, RValueVisitor<Void>
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
        public Void visit(PropertyValueMany many)
        {
            indent();
            print(many.getProperty());
            indent();
            many.getValues().forEach(v -> v.visit(this));
            outdent();
            outdent();
            return null;
        }

        @Override
        public Void visit(PropertyValueOne one)
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
        public Void visit(Primitive primitive)
        {
            print(primitive.toString());
            return null;
        }

        @Override
        public Void visit(ObjRef objRef)
        {
            printf("instance %s %s", objRef.getClassifierId(), objRef.getId());
            return null;
        }

        @Override
        public Void visit(EnumRef enumRef)
        {
            printf("enum %s %s", enumRef.getEnumerationId(), enumRef.getEnumerationId());
            return null;
        }
    }
}
