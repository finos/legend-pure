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

package org.finos.legend.pure.m3.navigation.PackageableElement;

import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.SafeAppendable;

import java.util.Collection;
import java.util.function.Consumer;

public class PackageableElement
{
    public static final String DEFAULT_PATH_SEPARATOR = "::";

    public static final Function<CoreInstance, String> GET_USER_PATH = PackageableElement::getUserPathForPackageableElement;

    @Deprecated
    public static final Function<CoreInstance, String> GET_NAME_VALUE_WITH_USER_PATH = instance ->
    {
        StringBuilder buffer = new StringBuilder();
        CoreInstance pkg = instance.getValueForMetaPropertyToOne(M3Properties._package);
        if (pkg != null)
        {
            PackageableElement.writeUserPathForPackageableElement(buffer, pkg).append(PackageableElement.DEFAULT_PATH_SEPARATOR);
        }
        buffer.append(instance.getValueForMetaPropertyToOne(M3Properties.name).getName());
        return buffer.toString();
    };

    public static void forEachPackagePathElement(CoreInstance packageableElement, Consumer<? super CoreInstance> firstConsumer, Consumer<? super CoreInstance> restConsumer)
    {
        CoreInstance pkg = packageableElement.getValueForMetaPropertyToOne(M3Properties._package);
        if (pkg == null)
        {
            firstConsumer.accept(packageableElement);
        }
        else
        {
            forEachPackagePathElement(pkg, firstConsumer, restConsumer);
            restConsumer.accept(packageableElement);
        }
    }

    public static void forEachPackagePathElement(CoreInstance packageableElement, Consumer<? super CoreInstance> consumer)
    {
        CoreInstance pkg = packageableElement.getValueForMetaPropertyToOne(M3Properties._package);
        if (pkg != null)
        {
            forEachPackagePathElement(pkg, consumer);
        }
        consumer.accept(packageableElement);
    }

    public static MutableList<CoreInstance> getUserObjectPathForPackageableElement(CoreInstance packageableElement)
    {
        MutableList<CoreInstance> path = Lists.mutable.empty();
        forEachPackagePathElement(packageableElement, path::add);
        return path;
    }

    public static String getM4UserPathForPackageableElement(CoreInstance packageableElement)
    {
        return writeM4UserPathForPackageableElement(new StringBuilder(64), packageableElement).toString();
    }

    public static <T extends Appendable> T writeM4UserPathForPackageableElement(T appendable, CoreInstance packageableElement)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        forEachPackagePathElement(packageableElement,
                e -> safeAppendable.append(e.getName()),
                e -> safeAppendable.append(".children[").append(e.getName()).append(']'));
        return appendable;
    }

    public static void collectM4Path(CoreInstance packageableElement, Collection<? super String> target)
    {
        forEachPackagePathElement(packageableElement,
                e -> target.add(e.getName()),
                e ->
                {
                    target.add(M3Properties.children);
                    target.add(e.getName());
                });
    }

    public static void forEachUserPathElement(String userPath, Consumer<? super String> consumer)
    {
        forEachUserPathElement(userPath, DEFAULT_PATH_SEPARATOR, consumer);
    }

    public static void forEachUserPathElement(String userPath, String separator, Consumer<? super String> consumer)
    {
        int start = 0;
        int sepLen = separator.length();
        int end = userPath.indexOf(separator);

        // If the string is just the separator, nothing to do
        if ((end == 0) && (sepLen == userPath.length()))
        {
            return;
        }

        while (end != -1)
        {
            consumer.accept(userPath.substring(start, end));
            start = end + sepLen;
            end = userPath.indexOf(separator, start);
        }
        consumer.accept(userPath.substring(start));
    }

    public static ListIterable<String> splitUserPath(String userPath)
    {
        return splitUserPath(userPath, DEFAULT_PATH_SEPARATOR);
    }

    public static ListIterable<String> splitUserPath(String userPath, String separator)
    {
        MutableList<String> result = Lists.mutable.empty();
        forEachUserPathElement(userPath, separator, result::add);
        return result;
    }

    public static String getUserPathForPackageableElement(CoreInstance packageableElement)
    {
        return getUserPathForPackageableElement(packageableElement, null);
    }

    public static String getUserPathForPackageableElement(CoreInstance packageableElement, String sep)
    {
        return writeUserPathForPackageableElement(new StringBuilder(64), packageableElement, sep).toString();
    }

    @Deprecated
    public static MutableList<String> getUserObjectPathForPackageableElementAsList(CoreInstance packageableElement, boolean includeRoot)
    {
        MutableList<String> pkgPath = Lists.mutable.empty();
        forEachPackagePathElement(packageableElement,
                includeRoot ?
                        element -> pkgPath.add(element.getName()) :
                        element ->
                        {
                            if (!M3Paths.Root.equals(element.getName()))
                            {
                                pkgPath.add(element.getName());
                            }
                        },
                element -> pkgPath.add(element.getName()));
        return pkgPath;
    }

    public static <T extends Appendable> T writeUserPathForPackageableElement(T appendable, CoreInstance packageableElement)
    {
        return writeUserPathForPackageableElement(appendable, packageableElement, null);
    }

    public static <T extends Appendable> T writeUserPathForPackageableElement(T appendable, CoreInstance packageableElement, String sep)
    {
        writeUserPathForPackageableElement_Recursive(SafeAppendable.wrap(appendable), packageableElement, (sep == null) ? DEFAULT_PATH_SEPARATOR : sep);
        return appendable;
    }

    private static void writeUserPathForPackageableElement_Recursive(SafeAppendable appendable, CoreInstance packageableElement, String sep)
    {
        CoreInstance pkg = packageableElement.getValueForMetaPropertyToOne(M3Properties._package);
        if ((pkg == null) || M3Paths.Root.equals(pkg.getName()))
        {
            appendable.append(packageableElement.getName());
        }
        else
        {
            writeUserPathForPackageableElement_Recursive(appendable, pkg, sep);
            appendable.append(sep).append(packageableElement.getName());
        }
    }

    public static String getSystemPathForPackageableElement(CoreInstance packageableElement)
    {
        return getSystemPathForPackageableElement(packageableElement, DEFAULT_PATH_SEPARATOR);
    }

    public static String getSystemPathForPackageableElement(CoreInstance packageableElement, String sep)
    {
        return writeSystemPathForPackageableElement(new StringBuilder(64), packageableElement, sep).toString();
    }

    public static <T extends Appendable> T writeSystemPathForPackageableElement(T appendable, CoreInstance packageableElement, String sep)
    {
        SafeAppendable safeAppendable = SafeAppendable.wrap(appendable);
        String separator = (sep == null) ? DEFAULT_PATH_SEPARATOR : sep;
        forEachPackagePathElement(packageableElement,
                e -> safeAppendable.append(e.getName()),
                e -> safeAppendable.append(separator).append(e.getName()));
        return appendable;
    }

    public static CoreInstance findPackageableElement(String path, ModelRepository repository)
    {
        ListIterable<String> paths = PackageableElement.splitUserPath(path);

        if (paths.size() == 1 && _Package.SPECIAL_TYPES.contains(path))
        {
            return repository.getTopLevel(path);
        }

        CoreInstance parent = repository.getTopLevel(M3Paths.Root);
        if (parent == null)
        {
            throw new RuntimeException("Cannot find Root in model repository");
        }

        if (path.isEmpty())
        {
            return parent;
        }

        for (String childName : paths)
        {
            CoreInstance child = _Package.findInPackage(parent, childName);
            if (child == null)
            {
                throw new RuntimeException("Not Found: " + path);
            }
            parent = child;
        }
        return parent;
    }
}