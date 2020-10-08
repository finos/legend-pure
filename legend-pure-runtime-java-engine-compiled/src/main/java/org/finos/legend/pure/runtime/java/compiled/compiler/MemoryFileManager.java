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

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.impl.block.factory.Predicates;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;
import org.finos.legend.pure.m3.serialization.runtime.Message;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MemoryFileManager extends ForwardingJavaFileManager<StandardJavaFileManager>
{
    private final MemoryFileManager parent;
    private final MutableMap<String, ClassJavaSource> codeByName = UnifiedMap.newMap();
    private final MutableListMultimap<String, ClassJavaSource> codeByPackage = FastListMultimap.newMultimap();

    private final Message message;
    private int count = 0;

    public MemoryFileManager(JavaCompiler compiler, MemoryFileManager parent, Message message)
    {
        super(compiler.getStandardFileManager(null, null, null));
        this.parent = parent;
        this.message = message;
    }

    public MemoryFileManager(JavaCompiler compiler, Message message)
    {
        this(compiler, null, message);
    }

    public MemoryFileManager(JavaCompiler compiler)
    {
        this(compiler, null, null);
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException
    {
        MutableList<JavaFileObject> result = FastList.newList(this.codeByName.size());
        collectFiles(result, location, packageName, kinds, recurse);
        return result;
    }

    private void collectFiles(final MutableCollection<JavaFileObject> target, Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException
    {
        if (this.parent != null)
        {
            this.parent.collectFiles(target, location, packageName, kinds, recurse);
        }
        target.addAllIterable(super.list(location, packageName, kinds, recurse));
        if ((location == StandardLocation.CLASS_PATH ) && kinds.contains(Kind.CLASS))
        {
            target.addAll(this.codeByPackage.get(packageName));
            if (recurse)
            {
                final String packagePrefix = packageName + '.';
                this.codeByPackage.forEachKeyMultiValues(new Procedure2<String, Iterable<ClassJavaSource>>()
                {
                    @Override
                    public void value(String pkg, Iterable<ClassJavaSource> files)
                    {
                        if (pkg.startsWith(packagePrefix))
                        {
                            target.addAllIterable(files);
                        }
                    }
                });
            }
        }
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file)
    {
        if (file instanceof ClassJavaSource)
        {
            return ((ClassJavaSource)file).inferBinaryName();
        }
        return super.inferBinaryName(location, file);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException
    {
        if (kind == Kind.CLASS)
        {
            return getClassJavaSourceForOutput(className);
        }
        return super.getJavaFileForOutput(location, className, kind, sibling);
    }

    private ClassJavaSource getClassJavaSourceForOutput(String className)
    {
        ClassJavaSource source = this.codeByName.get(className);
        if (source == null)
        {
            source = ClassJavaSource.fromClassName(className);
            this.codeByName.put(className, source);
            String pkg = getPackageFromClassName(className);
            this.codeByPackage.put(pkg, source);
        }
        if (this.message != null)
        {
            this.message.setMessage(String.format("Compiling Java classes (%,d)", this.count));
        }
        this.count++;
        return source;
    }

    ClassJavaSource getClassJavaSourceByName(String name)
    {
        return getClassJavaSourceByName(name, false);
    }

    ClassJavaSource getClassJavaSourceByName(String name, boolean searchParent)
    {
        ClassJavaSource source = this.codeByName.get(name);
        if ((source == null) && searchParent && (this.parent != null))
        {
            source = this.parent.getClassJavaSourceByName(name, true);
        }
        return source;
    }

    ClassJavaSource removeClassJavaSourceByName(String name)
    {
        ClassJavaSource source = this.codeByName.remove(name);
        if (source != null)
        {
            String pkg = getPackageFromClassName(name);
            this.codeByPackage.remove(pkg, source);
        }
        return source;
    }

    RichIterable<ClassJavaSource> getAllClassJavaSources(boolean includeClassesFromParent)
    {
        if (!includeClassesFromParent || (this.parent == null))
        {
            return this.codeByName.valuesView();
        }

        MutableList<ClassJavaSource> sources = FastList.newList(this.codeByName.size() + this.parent.codeByName.size());
        sources.addAll(this.codeByName.values());
        this.parent.collectAllClassJavaSourcesIf(Predicates.notIn(this.codeByName.keySet()), sources);
        return sources;
    }

    private void collectAllClassJavaSourcesIf(final Predicate<? super String> predicate, final MutableCollection<? super ClassJavaSource> target)
    {
        this.codeByName.forEachKeyValue(new Procedure2<String, ClassJavaSource>()
        {
            @Override
            public void value(String className, ClassJavaSource source)
            {
                if (predicate.accept(className))
                {
                    target.add(source);
                }
            }
        });
        if (this.parent != null)
        {
            this.parent.collectAllClassJavaSourcesIf(Predicates.and(predicate, Predicates.notIn(this.codeByName.keySet())), target);
        }
    }

    public void writeClassJavaSourcesToJar(JarOutputStream stream) throws IOException
    {
        writeClassJavaSourcesToJar(stream, false);
    }

    public void writeClassJavaSourcesToJar(JarOutputStream stream, boolean includeClassesFromParent) throws IOException
    {
        writeClassJavaSourcesToZip(stream, includeClassesFromParent);
    }

    public void writeClassJavaSourcesToZip(ZipOutputStream stream) throws IOException
    {
        writeClassJavaSourcesToZip(stream, false);
    }

    public void writeClassJavaSourcesToZip(ZipOutputStream stream, boolean includeClassesFromParent) throws IOException
    {
        for (ClassJavaSource source : getAllClassJavaSources(includeClassesFromParent))
        {
            ZipEntry entry = new ZipEntry(source.getName().substring(1));
            stream.putNextEntry(entry);
            stream.write(source.getBytes());
            stream.closeEntry();
        }
    }

    public void writeClassJavaSources(Path directory) throws IOException
    {
        writeClassJavaSources(directory, false);
    }

    public void writeClassJavaSources(Path directory, boolean includeClassesFromParent) throws IOException
    {
        for (ClassJavaSource source : getAllClassJavaSources(includeClassesFromParent))
        {
            Path path = directory.resolve(source.getName().substring(1));
            Files.createDirectories(path.getParent());
            Files.write(path, source.getBytes());
        }
    }

    public void loadClassesFromJarFile(Path jarFile) throws IOException
    {
        try (JarInputStream jarInputStream = new JarInputStream(new BufferedInputStream(Files.newInputStream(jarFile))))
        {
            loadClassesFromZipInputStream(jarInputStream);
        }
    }

    public void loadClassesFromZipFile(Path zipFile) throws IOException
    {
        try (ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipFile))))
        {
            loadClassesFromZipInputStream(zipInputStream);
        }
    }

    public void loadClassesFromZipInputStream(ZipInputStream zipStream) throws IOException
    {
        byte[] buffer = null;
        for (ZipEntry entry = zipStream.getNextEntry(); entry != null; entry = zipStream.getNextEntry())
        {
            String name = entry.getName();
            if (name.endsWith(Kind.CLASS.extension))
            {
                String className = zipEntryNameToClassName(name);
                ClassJavaSource source = getClassJavaSourceForOutput(className);
                if (buffer == null)
                {
                    buffer = new byte[8192];
                }
                long size = entry.getSize();
                try (OutputStream outStream = source.openOutputStream((size > 0L) ? (int)size : 8192))
                {
                    for (int read = zipStream.read(buffer); read != -1; read = zipStream.read(buffer))
                    {
                        outStream.write(buffer, 0, read);
                    }
                }
            }
        }
    }

    public void loadClassesFromDirectory(Path directory) throws IOException
    {
        if (!Files.isDirectory(directory))
        {
            throw new IllegalArgumentException("Not a directory: " + directory);
        }
        loadFromDirectory(directory, directory);
    }

    private void loadFromDirectory(Path directory, Path root) throws IOException
    {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory))
        {
            for (Path entry : dirStream)
            {
                if (Files.isDirectory(entry))
                {
                    loadFromDirectory(entry, root);
                }
                else
                {
                    loadFromFile(entry, root);
                }
            }
        }
    }

    private void loadFromFile(Path file, Path root) throws IOException
    {
        if (file.getFileName().toString().endsWith(Kind.CLASS.extension))
        {
            String className = classFilePathToClassName(root.relativize(file));
            byte[] fileBytes = Files.readAllBytes(file);
            ClassJavaSource source = getClassJavaSourceForOutput(className);
            source.setBytes(fileBytes);
        }
    }

    private static String zipEntryNameToClassName(String zipEntryName)
    {
        char separator = ((zipEntryName.indexOf('/') != -1) || (zipEntryName.indexOf('\\') == -1)) ? '/' : '\\';
        int startIndex = (zipEntryName.charAt(0) == separator) ? 1 : 0;
        int endIndex = zipEntryName.length() - Kind.CLASS.extension.length();
        return zipEntryName.substring(startIndex, endIndex).replace(separator, '.');
    }

    private static String classFilePathToClassName(Path classFilePath)
    {
        int nameCount = classFilePath.getNameCount();
        StringBuilder builder = new StringBuilder(nameCount * 8);
        int lastIndex = nameCount - 1;
        for (int i = 0; i < lastIndex; i++)
        {
            builder.append(classFilePath.getName(i));
            builder.append('.');
        }
        String lastName = classFilePath.getName(lastIndex).toString();
        builder.append(lastName, 0, lastName.length() - Kind.CLASS.extension.length());
        return builder.toString();
    }

    private static String getPackageFromClassName(String className)
    {
        int lastDotIndex = className.lastIndexOf('.');
        return (lastDotIndex == -1) ? "" : className.substring(0, lastDotIndex);
    }
}