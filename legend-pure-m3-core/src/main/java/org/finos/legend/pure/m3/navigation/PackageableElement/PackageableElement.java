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

import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

import java.io.IOException;
import java.util.Collection;

public class PackageableElement
{
    public static final String DEFAULT_PATH_SEPARATOR = "::";

    public static final Function<CoreInstance, String> GET_USER_PATH = new Function<CoreInstance, String>()
    {
        @Override
        public String valueOf(CoreInstance packageableElement)
        {
            return getUserPathForPackageableElement(packageableElement);
        }
    };

    public static final Function<CoreInstance, String> GET_NAME_VALUE_WITH_USER_PATH = new Function<CoreInstance, String>()
    {
        @Override
        public String valueOf(CoreInstance instance)
        {
            StringBuffer buffer = new StringBuffer();
            CoreInstance pkg = instance.getValueForMetaPropertyToOne(M3Properties._package);
            if (pkg != null)
            {
                PackageableElement.writeUserPathForPackageableElement(buffer, pkg);
                buffer.append(PackageableElement.DEFAULT_PATH_SEPARATOR);
            }
            buffer.append(instance.getValueForMetaPropertyToOne(M3Properties.name).getName());
            return buffer.toString();
        }
    };

    public static MutableList<CoreInstance> getUserObjectPathForPackageableElement(CoreInstance packageableElement)
    {
        return getUserObjectPathForPackageableElement_Recursive(packageableElement, 1);
    }

    private static final Function<CoreInstance, CoreInstance> DEFAULT_GET_PACKAGE_ACCESSOR = new Function<CoreInstance, CoreInstance>()
    {
        @Override
        public CoreInstance valueOf(CoreInstance packageableElement)
        {
            return packageableElement.getValueForMetaPropertyToOne(M3Properties._package);
        }
    };

    private static MutableList<CoreInstance> getUserObjectPathForPackageableElement_Recursive(CoreInstance packageableElement, int depth)
    {
        CoreInstance pkg = packageableElement.getValueForMetaPropertyToOne(M3Properties._package);
        if (pkg == null)
        {
            return FastList.<CoreInstance>newList(depth).with(packageableElement);
        }

        MutableList<CoreInstance> pkgPath = getUserObjectPathForPackageableElement_Recursive(pkg, depth + 1);
        pkgPath.add(packageableElement);
        return pkgPath;
    }

    public static String getM4UserPathForPackageableElement(CoreInstance packageableElement)
    {
        StringBuilder builder = new StringBuilder(64);
        writeM4UserPathForPackageableElement(builder, packageableElement);
        return builder.toString();
    }

    public static void writeM4UserPathForPackageableElement(Appendable appendable, CoreInstance packageableElement)
    {
        try
        {
            CoreInstance pkg = packageableElement.getValueForMetaPropertyToOne(M3Properties._package);
            if (pkg == null)
            {
                appendable.append(M3Paths.Root);
            }
            else
            {
                writeM4UserPathForPackageableElement(appendable, pkg);
                appendable.append(".children[");
                appendable.append(packageableElement.getName());
                appendable.append(']');
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public static void collectM4Path(CoreInstance packageableElement, Collection<? super String> target)
    {
        CoreInstance pkg = packageableElement.getValueForMetaPropertyToOne(M3Properties._package);
        if (pkg != null)
        {
            collectM4Path(pkg, target);
            target.add(M3Properties.children);
        }
        target.add(packageableElement.getName());
    }

    public static ListIterable<String> splitUserPath(String userPath)
    {
        return splitUserPath(userPath, DEFAULT_PATH_SEPARATOR);
    }

    public static ListIterable<String> splitUserPath(String userPath, String separator)
    {
        int start = 0;
        int sepLen = separator.length();
        int end = userPath.indexOf(separator);

        // If the string is just the separator, return an empty list
        if ((end == 0) && (sepLen == userPath.length()))
        {
            return Lists.immutable.empty();
        }

        MutableList<String> result = Lists.mutable.with();
        while (end != -1)
        {
            result.add(userPath.substring(start, end));
            start = end + sepLen;
            end = userPath.indexOf(separator, start);
        }
        result.add(userPath.substring(start));
        return result;
    }

    public static String getUserPathForPackageableElement(CoreInstance packageableElement)
    {
        return getUserPathForPackageableElement(packageableElement, null);
    }

    public static String getUserPathForPackageableElement(CoreInstance packageableElement, String sep)
    {
        StringBuilder builder = new StringBuilder(64);
        writeUserPathForPackageableElement(builder, packageableElement, sep);
        return builder.toString();
    }

    public static MutableList<String> getUserObjectPathForPackageableElementAsList(CoreInstance packageableElement, boolean includeRoot)
    {
        CoreInstance pkg = packageableElement.getValueForMetaPropertyToOne(M3Properties._package);
        if (pkg == null)
        {
            return (includeRoot || !"Root".equals(packageableElement.getName())) ? Lists.mutable.with(packageableElement.getName()) : Lists.mutable.<String>of();
        }
        else
        {
            MutableList<String> pkgPath = getUserObjectPathForPackageableElementAsList(pkg, includeRoot);
            pkgPath.add(packageableElement.getName());
            return pkgPath;
        }
    }

    public static void writeUserPathForPackageableElement(Appendable appendable, CoreInstance packageableElement)
    {
        writeUserPathForPackageableElement(appendable, packageableElement, null);
    }

    public static void writeUserPathForPackageableElement(Appendable appendable, CoreInstance packageableElement, String sep)
    {
        try
        {
            writeUserPathForPackageableElement_Recursive(appendable, packageableElement, (sep == null) ? DEFAULT_PATH_SEPARATOR : sep);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void writeUserPathForPackageableElement_Recursive(Appendable appendable, CoreInstance packageableElement, String sep) throws IOException
    {
        CoreInstance pkg = packageableElement.getValueForMetaPropertyToOne(M3Properties._package);
        if ((pkg == null) || M3Paths.Root.equals(pkg.getName()))
        {
            appendable.append(packageableElement.getName());
        }
        else
        {
            writeUserPathForPackageableElement_Recursive(appendable, pkg, sep);
            appendable.append(sep);
            appendable.append(packageableElement.getName());
        }
    }

    public static String getSystemPathForPackageableElement(CoreInstance packageableElement)
    {
        return getSystemPathForPackageableElement(packageableElement, DEFAULT_PATH_SEPARATOR);
    }

    public static String getSystemPathForPackageableElement(CoreInstance packageableElement, String sep)
    {
        StringBuilder builder = new StringBuilder(64);
        writeSystemPathForPackageableElement(builder, packageableElement, sep);
        return builder.toString();
    }

    public static void writeSystemPathForPackageableElement(Appendable appendable, CoreInstance packageableElement, String sep)
    {
        try
        {
            writeSystemPathForPackageableElement_Recursive(appendable, packageableElement, (sep == null) ? DEFAULT_PATH_SEPARATOR : sep);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void writeSystemPathForPackageableElement_Recursive(Appendable appendable, CoreInstance packageableElement, String sep) throws IOException
    {
        CoreInstance pkg = packageableElement.getValueForMetaPropertyToOne(M3Properties._package);
        if (pkg == null)
        {
            appendable.append(packageableElement.getName());
        }
        else
        {
            writeSystemPathForPackageableElement_Recursive(appendable, pkg, sep);
            appendable.append(sep);
            appendable.append(packageableElement.getName());
        }
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
