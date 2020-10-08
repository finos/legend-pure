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
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.coreinstance.BaseCoreInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecificationCoreInstanceWrapper;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m4.ModelRepository;
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
import org.finos.legend.pure.runtime.java.compiled.metadata.FunctionCache;
import org.finos.legend.pure.runtime.java.compiled.metadata.Metadata;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataAccessor;
import org.finos.legend.pure.runtime.java.compiled.metadata.MetadataHolder;

import java.lang.reflect.Method;

public class CompiledProcessorSupport implements ProcessorSupport
{
    private final ClassLoader globalClassLoader;
    private final Context context = new Context();
    private final MetadataAccessor metadataAccessor;
    private final FunctionCache functionCache = new FunctionCache();
    private final ClassCache classCache = new ClassCache();
    private final Metadata metadata;
    private final MutableSet<String> extraSupportedTypes;

    public CompiledProcessorSupport(ClassLoader globalClassLoader, Metadata metadata, MutableSet<String> extraSupportedTypes)
    {
        this.globalClassLoader = globalClassLoader;
        this.metadata = metadata;
        this.metadataAccessor = new MetadataHolder(metadata);
        this.extraSupportedTypes = extraSupportedTypes;
    }

    @Override
    public boolean instance_instanceOf(CoreInstance object, String typeName)
    {
        try
        {
            if (object instanceof ReflectiveCoreInstance)
            {
                if (ModelRepository.PRIMITIVE_TYPE_NAMES.contains(typeName))
                {
                    return false;
                }
                Class cl = null;
                org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type type = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type)this.package_getByUserPath(typeName);
                if (type != null)
                {
                    cl = this.classCache.getIfAbsentPutInterfaceForType(type, this.globalClassLoader);
                }

                if (cl == null)
                {
                    cl = object.getClass().getClassLoader().loadClass(JavaPackageAndImportBuilder.buildInterfaceReferenceFromUserPath(typeName, this.extraSupportedTypes));
                }
                return cl.isInstance(object);
            }
            else if (object instanceof ValCoreInstance)
            {
                String valType = ((ValCoreInstance)object).getType();
                return typeName.equals(valType) ||
                        (M3Paths.Date.equals(typeName) && (valType.equals(M3Paths.DateTime) || valType.equals(M3Paths.StrictDate) || valType.equals(M3Paths.LatestDate)));
            }
            else
            {
                return Instance.instanceOf(object, typeName, this);
                //todo: enable this once we fix ExecutionManager
                //throw new UnsupportedOperationException("Unable to calculate instance of. Unexpected Core Instance type" + object.getClass().getName());
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean type_isPrimitiveType(CoreInstance type)
    {
        try
        {

            Class cl = type.getClass().getClassLoader().loadClass(FullJavaPaths.PrimitiveType);
            return cl.isInstance(type);
        }
        catch (Exception e)
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
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private Class pureClassToJavaClass(String fullName)
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
        String pack = pureClass.getName();
        while (parent != null && !"Root".equals(parent.getName()))
        {
            pack = parent.getName()+ "::" +pack;
            parent = parent.getValueForMetaPropertyToOne(M3Properties._package);
        }
        return pack;
    }


    @Override
    public CoreInstance type_wrapGenericType(CoreInstance aClass)
    {
        try
        {
            ClassLoader cl = aClass.getClass().getClassLoader();
            Class genericType = cl.loadClass(FullJavaPaths.GenericType_Impl);
            Class rawType = cl.loadClass(FullJavaPaths.Type);
            Object result = genericType.getConstructor(String.class).newInstance("id");
            Method m = genericType.getMethod("_rawType", rawType);
            m.invoke(result, aClass);
            return (CoreInstance)result;
        }
        catch (Exception e)
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
        try
        {
            return (CoreInstance)this.globalClassLoader.loadClass(
                    inferred ? FullJavaPaths.InferredGenericType_Impl:
                            FullJavaPaths.GenericType_Impl)
                    .getConstructor(String.class).newInstance("id");
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CoreInstance package_getByUserPath(final String path)
    {
        CoreInstance element = null;
        try
        {
            if (ModelRepository.PRIMITIVE_TYPE_NAMES.contains(path) || M3Paths.Number.equals(path))
            {
                element = this.metadataAccessor.getPrimitiveType(path);
            }
            else
            {
                String fullSystemPath = M3Paths.Package.equals(path) ? path : "Root::" + path;
                element = this.metadataAccessor.getClass(fullSystemPath);
            }

            //todo check for other enumerations and other packageable elements
        }
        catch (Throwable t)
        {
            //todo - change metadata to not throw
            //Ignore
        }

        if (element == null)
        {
            //todo - should we cache this in compiled ?
            element = _Package.getByUserPath(path, this);
        }

        return element;
    }

    @Override
    public CoreInstance repository_getTopLevel(String root)
    {
        if (M3Paths.Root.equals(root))
        {
            return this.metadataAccessor.getPackage(M3Paths.Root);
        }
        if (M3Paths.Package.equals(root))
        {
            return this.metadataAccessor.getClass(M3Paths.Package);
        }
        if (ModelRepository.PRIMITIVE_TYPE_NAMES.contains(root) || M3Paths.Number.equals(root))
        {
            return this.metadataAccessor.getPrimitiveType(root);
        }
        throw new RuntimeException("Could not find top level element: " + root);
    }

    @Override
    public CoreInstance newEphemeralAnonymousCoreInstance(String type)
    {
        try
        {
            if (ModelRepository.PRIMITIVE_TYPE_NAMES.contains(type))
            {
                return new ValCoreInstance(null, type);
            }
            else
            {
                String className = JavaPackageAndImportBuilder.buildPackageFromUserPath(type) + "." + "Root_" + type.replace("::", "_") + "_Impl";
                return (CoreInstance)this.globalClassLoader.loadClass(className).getConstructor(String.class).newInstance("NO_ID");
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CoreInstance newCoreInstance(String name, String typeName, SourceInformation sourceInformation)
    {
        try
        {
            if (ModelRepository.PRIMITIVE_TYPE_NAMES.contains(typeName))
            {
                return new ValCoreInstance(name, typeName);
            }
            //When invoked from newCoreInstance(name, classifier, sourceInformation, repository), typeName already begins with Root
            String className = (typeName.startsWith("Root") ? JavaPackageAndImportBuilder.buildPackageFromSystemPath(typeName) + "." + typeName + "_Impl" :
                    JavaPackageAndImportBuilder.buildPackageFromUserPath(typeName) + "." + "Root_" + typeName.replace("::","_") + "_Impl");
            return (CoreInstance)this.globalClassLoader.loadClass(className).getConstructor(String.class).newInstance(name);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CoreInstance newCoreInstance(String name, CoreInstance classifier, SourceInformation sourceInformation)
    {
        return newCoreInstance(name, fullName(classifier), sourceInformation);
    }


    private CoreInstance getPrimitiveType(String type)
    {
        try
        {
            return this.metadataAccessor.getPrimitiveType(type);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CoreInstance newAnonymousCoreInstance(SourceInformation sourceInformation, String classifier)
    {
        return this.newCoreInstance("NO_ID", classifier, sourceInformation);
    }

    @Override
    public SetIterable<CoreInstance> function_getFunctionsForName(final String functionName)
    {
        try
        {
            final MutableSet<CoreInstance> functions = UnifiedSet.newSet();

            //TODO Iterate over ConcreteFunctionDefinition and FunctionDefinition along with NativeFunction.
            //Get the map for NativeFunction
            MutableMap fnMap = (MutableMap)this.metadata.getMetadata(MetadataJavaPaths.NativeFunction);
            fnMap.putAll((MutableMap)this.metadata.getMetadata(MetadataJavaPaths.ConcreteFunctionDefinition));

            fnMap.forEachValue(new Procedure<ReflectiveCoreInstance>()
            {
                @Override
                public void value(ReflectiveCoreInstance val)
                {
                    try
                    {
                        Object fName = val.getClass().getMethod("_functionName").invoke(val);
                        if ((fName != null) && fName.equals(functionName))
                        {
                            functions.add(val);
                        }
                    }
                    catch (Exception e)
                    {
                        // Ignore
                    }
                }
            });
            return functions;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
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
        ListIterable<? extends CoreInstance> properties = Instance.getValueForMetaPropertyToManyResolved(classifier,M3Properties.properties,this);
        CoreInstance result = null ;
        for(int index=0 ; index < properties.size(); index++)
        {
            CoreInstance property = properties.get(index);
            if (Instance.getValueForMetaPropertyToOneResolved(property,M3Properties.name,this).getName().equals(propertyName) )
            {
                result = property;
                break;
            }
        }
        return result!=null ? result :_Class.computePropertiesByName(classifier, _Class.SIMPLE_PROPERTIES_PROPERTIES, this).get(propertyName);
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
        final ProcessorSupport processorSupport = this;
        return this.context.getIfAbsentPutPropertiesByName(classifier, new org.eclipse.collections.api.block.function.Function<CoreInstance, ImmutableMap<String, CoreInstance>>()
                    {
                        @Override
                        public ImmutableMap<String, CoreInstance> valueOf(CoreInstance cls)
                        {
                            return _Class.computePropertiesByName(cls, _Class.SIMPLE_PROPERTIES_PROPERTIES, processorSupport).toImmutable();
                        }
                    });
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
            return this.metadataAccessor.getPrimitiveType(((ValCoreInstance)instance).getType());
        }

        //todo: clean this up, seem to have interpreted style core instances in compiled
        if (instance instanceof PrimitiveCoreInstance || instance instanceof BaseCoreInstance)
        {
            return instance.getClassifier();
        }

        if (instance instanceof Any)
        {
            Any any = (Any)instance;
            GenericType genericType = any._classifierGenericType();
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type type = genericType != null ? genericType._rawType() : null;
            return type == null ? CompiledSupport.getType((Any)instance, this.metadataAccessor) : type;
        }

        try
        {
            Class pure = this.globalClassLoader.loadClass(JavaPackageAndImportBuilder.rootPackage() + "." + "Pure");
            Method m = pure.getMethod("safeGetGenericType", Object.class, MetadataAccessor.class, ProcessorSupport.class);
            GenericType genericType = (GenericType)m.invoke(null, instance, this.metadataAccessor, this);
            return genericType._rawType();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
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
