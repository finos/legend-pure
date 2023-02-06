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

package org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.predicate.Predicate;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;
import org.finos.legend.pure.m3.coreinstance.helper.PropertyTypeHelper;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.serialization.runtime.ParserService;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.generation.JavaPackageAndImportBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.ProcessorContext;

public class ClassProcessor
{
    private static final MutableSet<String> PLATFORM_FILES = new ParserService().parsers().flatCollect(c -> c.getCoreInstanceFactoriesRegistry().flatCollect(CoreInstanceFactoryRegistry::allManagedTypes)).toSet().withAll(new ParserService().inlineDSLs().flatCollect(c -> c.getCoreInstanceFactoriesRegistry().flatCollect(CoreInstanceFactoryRegistry::allManagedTypes)).toSet());

    public static boolean isPlatformClass(CoreInstance _class)
    {
        return PLATFORM_FILES.contains(PackageableElement.getUserPathForPackageableElement(_class));
    }
    public static final Predicate<CoreInstance> IS_PLATFORM_CLASS = ClassProcessor::isPlatformClass;

    public static RichIterable<CoreInstance> processClass(CoreInstance classGenericType, ProcessorContext processorContext, boolean addJavaSerializationSupport, String pureExternalPackage)
    {
        ProcessorSupport processorSupport = processorContext.getSupport();
        MutableList<StringJavaSource> classes = processorContext.getClasses();
        MutableSet<CoreInstance> processedClasses = processorContext.getProcessedClasses(ClassProcessor.class);
        CoreInstance _class = Instance.getValueForMetaPropertyToOneResolved(classGenericType, M3Properties.rawType, processorSupport);

        String _package = JavaPackageAndImportBuilder.buildPackageForPackageableElement(_class);
        String imports = JavaPackageAndImportBuilder.buildImports(_class);

        if (!processedClasses.contains(_class) && !processorSupport.instance_instanceOf(_class, M3Paths.DataType))
        {
            processedClasses.add(_class);

            boolean useJavaInheritance = _class.getValueForMetaPropertyToMany(M3Properties.generalizations).size() == 1;

            if (!isPlatformClass(_class))
            {
                classes.add(ClassInterfaceProcessor.buildInterface(_package, imports, classGenericType, processorContext, processorSupport, useJavaInheritance));
            }
            if (requiresCompilationImpl(processorContext.getSupport(), _class))
            {
                classes.add(ClassImplIncrementalCompilationProcessor.buildImplementation(_package, imports, classGenericType, processorContext, processorSupport));
            }
            classes.add(ClassImplProcessor.buildImplementation(_package, imports, classGenericType, processorContext, processorSupport, useJavaInheritance, addJavaSerializationSupport, pureExternalPackage));
            if (isLazy(_class))
            {
                classes.add(ClassLazyImplProcessor.buildImplementation(_package, imports, classGenericType, processorContext, processorSupport));
            }
        }

        return processedClasses;
    }

    public static boolean requiresCompilationImpl(ProcessorSupport processorSupport, CoreInstance _class)
    {
        return isPlatformClass(_class) && !M3Paths.List.equals(PackageableElement.getUserPathForPackageableElement(_class))
                && (!_Class.getQualifiedProperties(_class, processorSupport).isEmpty() || !_Class.getEqualityKeyProperties(_class, processorSupport).isEmpty());
    }

    static CoreInstance getPropertyUnresolvedReturnType(CoreInstance property, ProcessorSupport processorSupport)
    {
        CoreInstance functionType = processorSupport.function_getFunctionType(property);
        return Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, processorSupport);
    }

    static CoreInstance getPropertyResolvedReturnType(CoreInstance classGenericType, CoreInstance property, ProcessorSupport processorSupport)
    {
        return PropertyTypeHelper.getPropertyResolvedReturnType(classGenericType, property, processorSupport);
    }

    static String typeParameters(CoreInstance _class)
    {
        return _class.getValueForMetaPropertyToMany(M3Properties.typeParameters).collect(new Function<CoreInstance, String>()
        {
            @Override
            public String valueOf(CoreInstance coreInstance)
            {
                return coreInstance.getValueForMetaPropertyToOne(M3Properties.name).getName();
            }
        }).makeString(",");
    }

    static boolean isLazy(CoreInstance _class)
    {
        SourceInformation sourceInfo = _class.getSourceInformation();
        String sourceId = (sourceInfo == null) ? null : sourceInfo.getSourceId();
        return (sourceId != null) && sourceId.startsWith("/platform");
    }
}
