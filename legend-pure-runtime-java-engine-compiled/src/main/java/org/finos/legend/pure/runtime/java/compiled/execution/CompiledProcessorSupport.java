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

package org.finos.legend.pure.runtime.java.compiled.execution;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.SetIterable;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.BaseCoreInstance;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionAccessor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecificationCoreInstanceWrapper;
import org.finos.legend.pure.m3.exception.PureExecutionException;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ReflectiveCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.ValCoreInstance;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.FullJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.MetadataJavaPaths;
import org.finos.legend.pure.runtime.java.compiled.metadata.ClassCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataAccessor;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataHolder;

import java.lang.reflect.Method;

public class CompiledProcessorSupport implements ProcessorSupport
{
    private final ClassLoader globalClassLoader;
    private final Context context = new Context();
    private final MetadataAccessor metadataAccessor;
    private final ClassCache classCache;
    private final Metadata metadata;
    private final SetIterable<String> extraSupportedTypes;

    public CompiledProcessorSupport(ClassLoader globalClassLoader, Metadata metadata, SetIterable<String> extraSupportedTypes)
    {
        this.globalClassLoader = globalClassLoader;
        this.classCache = new ClassCache(this.globalClassLoader);
        this.metadata = metadata;
        this.metadataAccessor = new MetadataHolder(metadata);
        this.extraSupportedTypes = extraSupportedTypes;
    }

    @Override
    public boolean instance_instanceOf(CoreInstance object, String typeName)
    {
        if (object instanceof ReflectiveCoreInstance)
        {
            if (PrimitiveUtilities.isPrimitiveTypeName(typeName))
            {
                return false;
            }
            Class<?> cl;
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type type = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type) this.package_getByUserPath(typeName);
            if (type == null)
            {
                String javaInterfaceName = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(typeName, this.extraSupportedTypes);
                try
                {
                    cl = object.getClass().getClassLoader().loadClass(javaInterfaceName);
                }
                catch (ClassNotFoundException e)
                {
                    throw new RuntimeException("Could not find Java interface for " + typeName + " (" + javaInterfaceName + ")", e);
                }
            }
            else
            {
                cl = this.classCache.getIfAbsentPutInterfaceForType(type);
            }
            return cl.isInstance(object);
        }

        if (object instanceof ValCoreInstance)
        {
            String valType = ((ValCoreInstance) object).getType();
            return typeName.equals(valType) ||
                    (M3Paths.Date.equals(typeName) && (valType.equals(M3Paths.DateTime) || valType.equals(M3Paths.StrictDate) || valType.equals(M3Paths.LatestDate)));
        }

        return Instance.instanceOf(object, typeName, this);
        //todo: enable this once we fix ExecutionManager
        //throw new UnsupportedOperationException("Unable to calculate instance of. Unexpected Core Instance type" + object.getClass().getName());
    }

    @Override
    public boolean type_isPrimitiveType(CoreInstance type)
    {
        try
        {
            Class<?> cl = type.getClass().getClassLoader().loadClass(FullJavaPaths.PrimitiveType);
            return cl.isInstance(type);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean valueSpecification_instanceOf(CoreInstance valueSpecification, String type)
    {
        try
        {
            ValueSpecification valueSpec = ValueSpecificationCoreInstanceWrapper.toValueSpecification(valueSpecification);
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type rawType = valueSpec._genericType()._rawType();

            Class<?> valueSpecType = (rawType instanceof Enumeration) ? this.globalClassLoader.loadClass(FullJavaPaths.Enum) : this.pureClassToJavaClass(fullName(rawType));
            Class<?> typeCl = this.pureClassToJavaClass(type);
            return typeCl.isAssignableFrom(valueSpecType);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Class<?> pureClassToJavaClass(String fullName)
    {
        if (fullName == null)
        {
            throw new IllegalArgumentException("Pure type path cannot be null");
        }
        switch (fullName)
        {
            case M3Paths.String:
            {
                return String.class;
            }
            case M3Paths.Boolean:
            {
                return Boolean.class;
            }
            case M3Paths.Integer:
            {
                return Long.class;
            }
            case M3Paths.Float:
            {
                return Double.class;
            }
            case M3Paths.Number:
            {
                return Number.class;
            }
            case M3Paths.Date:
            case M3Paths.StrictDate:
            case M3Paths.DateTime:
            case M3Paths.LatestDate:
            {
                return PureDate.class;
            }
            default:
            {
                try
                {
                    String javaPath = JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(fullName, this.extraSupportedTypes);
                    return this.globalClassLoader.loadClass(javaPath);
                }
                catch (Exception e)
                {
                    throw new RuntimeException("Error converting Pure class to Java: " + fullName, e);
                }
            }
        }
    }

    private String fullName(CoreInstance pureClass)
    {
        CoreInstance parent = pureClass.getValueForMetaPropertyToOne(M3Properties._package);
        MutableList<String> names = Lists.mutable.with(pureClass.getName());
        while (parent != null && !M3Paths.Root.equals(parent.getName()))
        {
            names.add(parent.getName());
            parent = parent.getValueForMetaPropertyToOne(M3Properties._package);
        }
        return names.asReversed().makeString("::");
    }


    @Override
    public CoreInstance type_wrapGenericType(CoreInstance aClass)
    {
        try
        {
            ClassLoader cl = aClass.getClass().getClassLoader();
            Class<?> genericType = cl.loadClass(FullJavaPaths.GenericType_Impl);
            Class<?> rawType = cl.loadClass(FullJavaPaths.Type);
            Object result = genericType.getConstructor(String.class).newInstance("id");
            Method m = genericType.getMethod("_rawType", rawType);
            m.invoke(result, aClass);
            return (CoreInstance) result;
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CoreInstance function_getFunctionType(CoreInstance function)
    {
        return Function.getFunctionType(function, this.context, this);
    }

    @Override
    public CoreInstance newGenericType(SourceInformation sourceInformation, CoreInstance source, boolean inferred)
    {
        String className = inferred ? FullJavaPaths.InferredGenericType_Impl : FullJavaPaths.GenericType_Impl;
        try
        {
            return (CoreInstance) this.globalClassLoader.loadClass(className).getConstructor(String.class).newInstance("id");
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CoreInstance package_getByUserPath(String path)
    {
        // Check top level elements
        if (M3Paths.Root.equals(path) || "::".equals(path))
        {
            return this.metadataAccessor.getPackage(M3Paths.Root);
        }
        if (M3Paths.Package.equals(path))
        {
            return this.metadataAccessor.getClass(M3Paths.Package);
        }
        if (PrimitiveUtilities.isPrimitiveTypeName(path))
        {
            return this.metadataAccessor.getPrimitiveType(path);
        }

        int lastColon = path.lastIndexOf(':');
        if (lastColon == -1)
        {
            // An element in Root - probably a package
            try
            {
                CoreInstance element = this.metadataAccessor.getPackage(M3Paths.Root + "::" + path);
                if (element != null)
                {
                    return element;
                }
            }
            catch (Exception ignore)
            {
                // Perhaps it's not a package? Fall back to general method
            }

            // Get the Root package, then search its children
            Package pkg = this.metadataAccessor.getPackage(M3Paths.Root);
            return pkg._children().detect(c -> path.equals(c.getName()));
        }

        // Perhaps the element is a class?
        try
        {
            CoreInstance element = this.metadataAccessor.getClass(M3Paths.Root + "::" + path);
            if (element != null)
            {
                return element;
            }
        }
        catch (Exception ignore)
        {
            // Perhaps it's not a class? Fall back to general method
        }

        // Get the element's package, then search in the package
        Package pkg;
        try
        {
            pkg = this.metadataAccessor.getPackage(M3Paths.Root + "::" + path.substring(0, lastColon - 1));
        }
        catch (Exception ignore)
        {
            pkg = null;
        }
        if (pkg == null)
        {
            // Package doesn't exist, so the element
            return null;
        }

        // Search the children of the package
        String name = path.substring(lastColon + 1);
        return pkg._children().detect(c -> name.equals(c.getName()));
    }

    @Override
    public CoreInstance repository_getTopLevel(String name)
    {
        if (M3Paths.Root.equals(name))
        {
            return this.metadataAccessor.getPackage(M3Paths.Root);
        }
        if (M3Paths.Package.equals(name))
        {
            return this.metadataAccessor.getClass(M3Paths.Package);
        }
        if (PrimitiveUtilities.isPrimitiveTypeName(name))
        {
            return this.metadataAccessor.getPrimitiveType(name);
        }
        throw new RuntimeException("Could not find top level element: " + name);
    }

    @Override
    public CoreInstance newEphemeralAnonymousCoreInstance(String type)
    {
        if (PrimitiveUtilities.isPrimitiveTypeName(type))
        {
            return new ValCoreInstance(null, type);
        }
        try
        {
            String className = JavaPackageAndImportBuilder.buildPackageFromUserPath(type) + "." + "Root_" + type.replace("::", "_") + "_Impl";
            return (CoreInstance) this.globalClassLoader.loadClass(className).getConstructor(String.class).newInstance("NO_ID");
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CoreInstance newCoreInstance(String name, String typeName, SourceInformation sourceInformation)
    {
        if (PrimitiveUtilities.isPrimitiveTypeName(typeName))
        {
            return new ValCoreInstance(name, typeName);
        }
        try
        {
            //When invoked from newCoreInstance(name, classifier, sourceInformation, repository), typeName already begins with Root
            String className = (typeName.startsWith("Root") ? JavaPackageAndImportBuilder.buildPackageFromSystemPath(typeName) + "." + typeName + "_Impl" :
                    JavaPackageAndImportBuilder.buildPackageFromUserPath(typeName) + "." + "Root_" + typeName.replace("::", "_") + "_Impl");
            return (CoreInstance) this.globalClassLoader.loadClass(className).getConstructor(String.class).newInstance(name);
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CoreInstance newCoreInstance(String name, CoreInstance classifier, SourceInformation sourceInformation)
    {
        return newCoreInstance(name, fullName(classifier), sourceInformation);
    }

    @Override
    public CoreInstance newAnonymousCoreInstance(SourceInformation sourceInformation, String classifier)
    {
        return this.newCoreInstance("NO_ID", classifier, sourceInformation);
    }

    @Override
    public SetIterable<CoreInstance> function_getFunctionsForName(String functionName)
    {
        //TODO Iterate over ConcreteFunctionDefinition and FunctionDefinition along with NativeFunction.
        return this.metadata.getMetadata(MetadataJavaPaths.NativeFunction).valuesView().asLazy()
                .concatenate(this.metadata.getMetadata(MetadataJavaPaths.ConcreteFunctionDefinition).valuesView())
                .select(f -> (f instanceof FunctionAccessor) && functionName.equals(((FunctionAccessor<?>) f)._functionName()), Sets.mutable.empty());
    }

    @Override
    public ImmutableList<CoreInstance> type_getTypeGeneralizations(CoreInstance type, org.eclipse.collections.api.block.function.Function<? super CoreInstance, ? extends ImmutableList<CoreInstance>> generator)
    {
        //todo: compiled mode specific version of this
        return this.context.getIfAbsentPutGeneralizations(type, generator);
    }

    @Override
    public CoreInstance class_findPropertyUsingGeneralization(CoreInstance classifier, final String propertyName)
    {
        CoreInstance result = Instance.getValueForMetaPropertyToManyResolved(classifier, M3Properties.properties, this)
                .detect(property -> propertyName.equals(property.getValueForMetaPropertyToOne(M3Properties.name).getName()));
        return (result == null) ? _Class.computePropertiesByName(classifier, _Class.SIMPLE_PROPERTIES_PROPERTIES, this).get(propertyName) : result;
    }

    @Override
    public CoreInstance class_findPropertyOrQualifiedPropertyUsingGeneralization(CoreInstance classifier, String propertyName)
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public RichIterable<CoreInstance> class_getSimpleProperties(CoreInstance classifier)
    {
        return this.class_getSimplePropertiesByName(classifier).valuesView();
    }

    @Override
    public MapIterable<String, CoreInstance> class_getSimplePropertiesByName(CoreInstance classifier)
    {
        return this.context.getIfAbsentPutPropertiesByName(classifier, cls -> _Class.computePropertiesByName(cls, _Class.SIMPLE_PROPERTIES_PROPERTIES, this).toImmutable());
    }

    @Override
    public RichIterable<CoreInstance> class_getQualifiedProperties(CoreInstance classifier)
    {
        return this.class_getQualifiedPropertiesByName(classifier).valuesView();
    }

    @Override
    public MapIterable<String, CoreInstance> class_getQualifiedPropertiesByName(CoreInstance classifier)
    {
        return _Class.getQualifiedPropertiesByName(classifier, this);
    }

    @Override
    public ListIterable<String> property_getPath(CoreInstance property)
    {
        return Property.calculatePropertyPath(property, this);
    }

    @Override
    public CoreInstance getClassifier(CoreInstance instance)
    {
        if (instance instanceof ValCoreInstance)
        {
            return this.metadataAccessor.getPrimitiveType(((ValCoreInstance) instance).getType());
        }

        //todo: clean this up, seem to have interpreted style core instances in compiled
        if (instance instanceof PrimitiveCoreInstance || instance instanceof BaseCoreInstance)
        {
            return instance.getClassifier();
        }

        if (instance instanceof Any)
        {
            Any any = (Any) instance;
            GenericType genericType = any._classifierGenericType();
            if (genericType != null)
            {
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type type = genericType._rawType();
                if (type != null)
                {
                    return type;
                }
            }
            return CompiledSupport.getType(any, this.metadataAccessor);
        }

        throw new PureExecutionException("ERROR unhandled type for value: " + instance + " (instance of " + instance.getClass() + ")");
    }

    @Override
    public boolean type_subTypeOf(CoreInstance type, CoreInstance possibleSuperType)
    {
        //todo - optimize
        return Type.subTypeOf(type, possibleSuperType, this);
    }

    @Override
    public CoreInstance type_BottomType()
    {
        return this.metadataAccessor.getBottomType();
    }

    @Override
    public CoreInstance type_TopType()
    {
        return this.metadataAccessor.getTopType();
    }

    public Metadata getMetadata()
    {
        return this.metadata;
    }
}