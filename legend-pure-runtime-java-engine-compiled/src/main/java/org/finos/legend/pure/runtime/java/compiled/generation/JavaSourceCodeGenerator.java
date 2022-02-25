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

package org.finos.legend.pure.runtime.java.compiled.generation;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m3.bootstrap.generator.M3ToJavaGenerator;
import org.finos.legend.pure.m3.compiler.visibility.AccessLevel;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.CodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.ClassJsonFactoryProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CoreExtensionCompiled;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.EnumProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassImplIncrementalCompilationProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassImplProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.measureUnit.MeasureProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.measureUnit.UnitProcessor;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class JavaSourceCodeGenerator
{
    public static final String EXTERNAL_FUNCTIONS_CLASS_NAME = "PureExternal";

    public static final String imports =
            "import org.eclipse.collections.api.LazyIterable;\n" +
                    "import org.eclipse.collections.api.block.function.Function0;\n" +
                    "import org.eclipse.collections.api.block.function.Function;\n" +
                    "import org.eclipse.collections.api.block.function.Function2;\n" +
                    "import org.eclipse.collections.api.block.predicate.Predicate;\n" +
                    "import org.eclipse.collections.api.block.procedure.Procedure;\n" +
                    "import org.eclipse.collections.api.map.ImmutableMap;\n" +
                    "import org.eclipse.collections.api.map.MutableMap;\n" +
                    "import org.eclipse.collections.api.map.MutableMapIterable;\n" +
                    "import org.eclipse.collections.api.map.MapIterable;\n" +
                    "import org.eclipse.collections.api.map.primitive.IntObjectMap;\n" +
                    "import org.eclipse.collections.api.set.MutableSet;\n" +
                    "import org.eclipse.collections.api.set.SetIterable;\n" +
                    "import org.eclipse.collections.api.list.MutableList;\n" +
                    "import org.eclipse.collections.api.list.ListIterable;\n" +
                    "import org.eclipse.collections.api.RichIterable;\n" +
                    "import org.eclipse.collections.api.tuple.Pair;\n" +
                    "import org.eclipse.collections.impl.factory.Lists;\n" +
                    "import org.eclipse.collections.impl.factory.Maps;\n" +
                    "import org.eclipse.collections.impl.map.mutable.UnifiedMap;\n" +
                    "import org.eclipse.collections.impl.map.strategy.mutable.UnifiedMapWithHashingStrategy;\n" +
                    "import org.eclipse.collections.impl.set.mutable.UnifiedSet;\n" +
                    "import org.eclipse.collections.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy;\n" +
                    "import org.eclipse.collections.impl.list.mutable.FastList;\n" +
                    "import org.eclipse.collections.impl.factory.Sets;\n" +
                    "import org.eclipse.collections.impl.block.function.checked.CheckedFunction0;\n" +
                    "import org.eclipse.collections.impl.utility.Iterate;\n" +
                    "import org.eclipse.collections.impl.utility.LazyIterate;\n" +
                    "import org.eclipse.collections.impl.utility.StringIterate;\n" +
                    "import org.finos.legend.pure.m3.navigation.generictype.GenericType;\n" +
                    "import org.finos.legend.pure.m3.navigation.ProcessorSupport;\n" +
                    "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n" +

                    "import org.junit.Test;\n" +
                    "import org.finos.legend.pure.m3.exception.PureExecutionException;\n" +
                    "import org.finos.legend.pure.m4.coreinstance.CoreInstance;\n" +
                    "import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;\n" +
                    "import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.*;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.*;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.metadata.*;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.*;\n" +

                    "import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.ChangeType;\n" +
                    "import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.ChangedPath;\n" +
                    "import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.Revision;\n" +
                    "import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;\n" +
                    "import org.finos.legend.pure.m3.tools.ListHelper;\n" +

                    "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.*;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.*;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.*;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.*;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.execution.*;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation.*;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.serialization.model.*;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.metadata.*;\n" +

                    "import java.lang.reflect.Method;\n" +
                    "import java.math.BigInteger;\n" +
                    "import java.sql.DatabaseMetaData;\n" +
                    "import java.sql.PreparedStatement;\n" +
                    "import java.sql.ResultSetMetaData;\n" +
                    "import java.util.Iterator;\n" +
                    "import java.util.Calendar;\n" +
                    "import java.util.Map;\n" +
                    "import java.util.ArrayDeque;\n" +
                    "import java.util.Deque;\n" +
                    "import org.json.simple.JSONObject;\n" +
                    "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;\n" +
                    "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;\n" +
                    "\n";

    private final ProcessorSupport processorSupport;
    private final IdBuilder idBuilder;
    private final CodeStorage codeStorage;
    private final boolean writeFilesToDisk;
    private final Path directoryToWriteFilesTo;
    private final String externalAPIPackage;

    private final boolean includePureStackTrace;
    private final MutableSet<CoreInstance> processedClasses = Sets.mutable.empty();
    private final MutableSet<CoreInstance> platformEnumerations = Sets.mutable.empty();
    private final MutableSet<CoreInstance> processedMeasures = Sets.mutable.empty();
    private final MutableSet<CoreInstance> processedUnits = Sets.mutable.empty();
    private final MutableSet<CoreInstance> javaSerializedClasses = Sets.mutable.empty();
    private final ListIterable<CompiledExtension> extensions;

    private final String name;

    public JavaSourceCodeGenerator(ProcessorSupport processorSupport, IdBuilder idBuilder, CodeStorage codeStorage, boolean writeFilesToDisk, Path directoryToWriteFilesTo, boolean includePureStackTrace, Iterable<? extends CompiledExtension> extensions, String name, String externalAPIPackage)
    {
        this.name = name;
        this.processorSupport = processorSupport;
        this.idBuilder = (idBuilder == null) ? IdBuilder.newIdBuilder(this.processorSupport) : idBuilder;
        this.codeStorage = codeStorage;
        this.writeFilesToDisk = writeFilesToDisk;
        this.directoryToWriteFilesTo = directoryToWriteFilesTo;
        this.includePureStackTrace = includePureStackTrace;
        this.extensions = Lists.mutable.with(CoreExtensionCompiled.extension()).withAll(extensions);
        this.externalAPIPackage = externalAPIPackage;
        if (this.directoryToWriteFilesTo != null)
        {
            this.javaClassesToDisk(this.extensions.flatCollect(CompiledExtension::getExtraJavaSources));
        }
    }

    public JavaSourceCodeGenerator(ProcessorSupport processorSupport, CodeStorage codeStorage, boolean writeFilesToDisk, Path directoryToWriteFilesTo, boolean includePureStackTrace, Iterable<? extends CompiledExtension> extensions, String name, String externalAPIPackage)
    {
        this(processorSupport, null, codeStorage, writeFilesToDisk, directoryToWriteFilesTo, includePureStackTrace, extensions, name, externalAPIPackage);
    }

    public ProcessorSupport getProcessorSupport()
    {
        return this.processorSupport;
    }

    ListIterable<StringJavaSource> generateCode(Source source)
    {
        if (source.getNewInstances() != null)
        {
            try
            {
                ProcessorContext processorContext = new ProcessorContext(this.processorSupport, this.extensions, this.idBuilder, this.includePureStackTrace);

                for (CoreInstance coreInstance : source.getNewInstances())
                {
                    if (!Instance.instanceOf(coreInstance, M3Paths.Package, this.processorSupport))
                    {
                        this.toJava(coreInstance, processorContext);
                    }
                }

                MutableList<StringJavaSource> javaClasses = this.buildJavaClasses(processorContext);
                if (this.writeFilesToDisk)
                {
                    this.javaClassesToDisk(javaClasses);
                }

                return javaClasses;
            }
            catch (Throwable t)
            {
                throw new RuntimeException("Error generating Java code for " + source.getId(), t);
            }
        }
        return Lists.fixedSize.empty();
    }

    ListIterable<StringJavaSource> generateCode()
    {
        CoreInstance root = this.processorSupport.package_getByUserPath("::");

        ProcessorContext processorContext = new ProcessorContext(this.processorSupport, this.extensions, this.idBuilder, this.includePureStackTrace);
        toJava(this.processorSupport.repository_getTopLevel(M3Paths.Package), processorContext);
        toJava(root, processorContext);

        MutableList<StringJavaSource> javaClasses = Lists.mutable.empty();
        javaClasses.addAll(this.buildJavaClasses(processorContext));
        if (this.writeFilesToDisk)
        {
            this.javaClassesToDisk(javaClasses);
        }

        javaClasses.addAllIterable(this.generatePureCoreHelperClasses(processorContext));
        return javaClasses;
    }

    ListIterable<StringJavaSource> generatePureCoreHelperClasses(ProcessorContext processorContext)
    {
        String platform = "import " + JavaPackageAndImportBuilder.platformJavaPackage() + ".*;\n";
        MutableList<StringJavaSource> coreJavaSources = Lists.mutable.with(
                StringJavaSource.newStringJavaSource(JavaPackageAndImportBuilder.platformJavaPackage(), "PureCompiledLambda", imports + platform + this.buildPureCompiledLambda(processorContext)),
                StringJavaSource.newStringJavaSource(JavaPackageAndImportBuilder.platformJavaPackage(), "LambdaZero", imports + platform + this.buildLambdaZero()),
                StringJavaSource.newStringJavaSource(JavaPackageAndImportBuilder.platformJavaPackage(), getFactoryRegistryName(), this.buildFactoryRegistry()),
                EnumProcessor.processEnum(),
                EnumProcessor.processEnumLazy());
        if (this.writeFilesToDisk)
        {
            this.javaClassesToDisk(coreJavaSources);
        }
        return coreJavaSources;
    }

    void collectClassesToSerialize()
    {
        CoreInstance externalizableStereotype = AccessLevel.EXTERNALIZABLE.getStereotype(this.processorSupport);
        ProcessorContext processorContext = new ProcessorContext(this.processorSupport, this.extensions, this.idBuilder, this.includePureStackTrace);
        ProcessorSupport processorSupport = processorContext.getSupport();

        for (CoreInstance element : externalizableStereotype.getValueForMetaPropertyToMany(M3Properties.modelElements))
        {
            if (Instance.instanceOf(element, M3Paths.Function, this.processorSupport))
            {
                CoreInstance functionType = processorSupport.function_getFunctionType(element);
                ListIterable<? extends CoreInstance> parameters = functionType.getValueForMetaPropertyToMany(M3Properties.parameters);
                for (CoreInstance parameter : parameters)
                {
                    CoreInstance type = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.genericType, processorSupport);
                    CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(type, M3Properties.rawType, processorSupport);
                    if (M3Paths.Map.equals(PackageableElement.getUserPathForPackageableElement(rawType)))
                    {
                        CoreInstance mapKey = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(type, M3Properties.typeArguments, processorSupport).get(0), M3Properties.rawType, processorSupport);
                        if (Instance.instanceOf(mapKey, M3Paths.Class, this.processorSupport))
                        {
                            this.addClassToSerialize(mapKey, this.javaSerializedClasses);
                        }
                        CoreInstance mapValue = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(type, M3Properties.typeArguments, processorSupport).get(1), M3Properties.rawType, processorSupport);
                        if (Instance.instanceOf(mapValue, M3Paths.Class, this.processorSupport))
                        {
                            this.addClassToSerialize(mapValue, this.javaSerializedClasses);
                        }
                    }
                }
            }
        }
    }

    private void addClassToSerialize(CoreInstance coreInstanceClass, MutableSet<CoreInstance> set)
    {
        if (!set.add(coreInstanceClass))
        {
            return;
        }

        boolean isInherit = !coreInstanceClass.getValueForMetaPropertyToMany(M3Properties.generalizations).isEmpty();

        if (isInherit)
        {
            for (CoreInstance superClass : coreInstanceClass.getValueForMetaPropertyToMany(M3Properties.generalizations))
            {
                CoreInstance generalGenericType = Instance.getValueForMetaPropertyToOneResolved(superClass, M3Properties.general, this.processorSupport);
                CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(generalGenericType, M3Properties.rawType, this.processorSupport);
                if (rawType != null && Instance.instanceOf(rawType, M3Paths.Class, this.processorSupport))
                {
                    this.addClassToSerialize(rawType, set);
                }
            }
        }

        boolean isInherited = !coreInstanceClass.getValueForMetaPropertyToMany(M3Properties.specializations).isEmpty();
        if (isInherited)
        {
            for (CoreInstance subClass : coreInstanceClass.getValueForMetaPropertyToMany(M3Properties.specializations))
            {
                CoreInstance specificType = Instance.getValueForMetaPropertyToOneResolved(subClass, M3Properties.specific, this.processorSupport);
                if (Instance.instanceOf(specificType, M3Paths.Class, this.processorSupport))
                {
                    this.addClassToSerialize(specificType, set);
                }
            }
        }

        RichIterable<CoreInstance> simpleProperties = this.processorSupport.class_getSimpleProperties(coreInstanceClass);
        simpleProperties.each(coreInstance ->
        {
            CoreInstance type = Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.genericType, this.processorSupport);
            CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(type, M3Properties.rawType, this.processorSupport);
            if (rawType != null && Instance.instanceOf(rawType, M3Paths.Class, this.processorSupport))
            {
                addClassToSerialize(rawType, set);
            }
        });
    }

    ListIterable<StringJavaSource> generateExternalizableAPI(String pack)
    {
        CoreInstance externalizableStereotype = AccessLevel.EXTERNALIZABLE.getStereotype(this.processorSupport);

        MutableList<String> externalizableFunctionCode = Lists.mutable.empty();
        CoreInstance functionClass = this.processorSupport.package_getByUserPath(M3Paths.Function);
        ProcessorContext processorContext = new ProcessorContext(this.processorSupport, this.extensions, this.idBuilder, this.includePureStackTrace);
        for (CoreInstance element : externalizableStereotype.getValueForMetaPropertyToMany(M3Properties.modelElements))
        {
            if (Instance.instanceOf(element, functionClass, this.processorSupport))
            {
                externalizableFunctionCode.add(FunctionProcessor.buildExternalizableFunction(element, processorContext));
            }
        }
        String text = this.buildExternalizableFunctionClass(externalizableFunctionCode);
        return Lists.immutable.with(StringJavaSource.newStringJavaSource(pack, EXTERNAL_FUNCTIONS_CLASS_NAME, text));
    }

    private void javaClassesToDisk(Iterable<? extends StringJavaSource> javaClasses)
    {
        try
        {
            Files.createDirectories(this.directoryToWriteFilesTo);
            for (StringJavaSource source : javaClasses)
            {
                //String fileUri = "file://" + source.toUri().getPath();
                Path file = Paths.get(this.directoryToWriteFilesTo + source.toUri().getPath());
                Files.createDirectories(file.getParent());
                Files.write(file, source.getCode().getBytes(StandardCharsets.UTF_8));
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private MutableList<StringJavaSource> buildJavaClasses(ProcessorContext processorContext)
    {
        MutableList<StringJavaSource> javaClasses = Lists.mutable.empty();
        javaClasses.addAll(processorContext.getClasses());

        MutableSet<String> processedSources = Sets.mutable.empty();
        String functionImports = processorContext.getNativeFunctionProcessor().getImports().makeString("");
        for (String sourceId : processorContext.getSourcesWithFunctionDefinitions())
        {
            processedSources.add(sourceId);
            javaClasses.add(StringJavaSource.newStringJavaSource(this.getFunctionPackageName(sourceId), sourceId, functionImports +
                    this.buildFunctionClass(sourceId, processorContext.getFunctionDefinitionsForSource(sourceId), processorContext.getLambdaFunctionsForSource(sourceId), processorContext.getNativeLambdaFunctionsForSource(sourceId))));
        }

        for (String sourceId : processorContext.getSourcesWithLambdaFunctions())
        {
            if (processedSources.add(sourceId))
            {
                javaClasses.add(StringJavaSource.newStringJavaSource(this.getFunctionPackageName(sourceId), sourceId, functionImports +
                        this.buildFunctionClass(sourceId, null, processorContext.getLambdaFunctionsForSource(sourceId), processorContext.getNativeLambdaFunctionsForSource(sourceId))));
            }
        }

        for (String sourceId : processorContext.getSourcesWithNativeLambdaFunctions())
        {
            if (processedSources.add(sourceId))
            {
                javaClasses.add(StringJavaSource.newStringJavaSource(this.getFunctionPackageName(sourceId), sourceId, functionImports +
                        this.buildFunctionClass(sourceId, null, null, processorContext.getNativeLambdaFunctionsForSource(sourceId))));
            }
        }

        return javaClasses;
    }

    private String getFunctionPackageName(String sourceId)
    {
        return JavaPackageAndImportBuilder.rootPackage();
    }

    private void toJava(CoreInstance coreInstance, ProcessorContext processorContext) throws PureCompilationException
    {
        try
        {
            if (Instance.instanceOf(coreInstance, M3Paths.Package, this.processorSupport))
            {
                for (CoreInstance element : coreInstance.getValueForMetaPropertyToMany(M3Properties.children))
                {
                    this.toJava(element, processorContext);
                }
            }
            if (Instance.instanceOf(coreInstance, M3Paths.Class, this.processorSupport))
            {
                CoreInstance genericType = Type.wrapGenericType(coreInstance, null, this.processorSupport);
                Instance.addValueToProperty(genericType, M3Properties.typeArguments, coreInstance.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToOne(M3Properties.typeArguments).getValueForMetaPropertyToMany(M3Properties.typeArguments), this.processorSupport);
                Instance.addValueToProperty(genericType, M3Properties.multiplicityArguments, coreInstance.getValueForMetaPropertyToOne(M3Properties.classifierGenericType).getValueForMetaPropertyToOne(M3Properties.typeArguments).getValueForMetaPropertyToMany(M3Properties.multiplicityArguments), this.processorSupport);
                boolean addJavaSerializationSupport = this.javaSerializedClasses.contains(coreInstance);
                RichIterable<CoreInstance> processedClasses = ClassProcessor.processClass(genericType, processorContext, addJavaSerializationSupport, this.externalAPIPackage);
                this.processedClasses.addAllIterable(processedClasses);

                ClassJsonFactoryProcessor.processClass(genericType, processorContext);
            }
            if (Instance.instanceOf(coreInstance, M3Paths.Enumeration, this.processorSupport) && ClassProcessor.isPlatformClass(coreInstance))
            {
                this.platformEnumerations.add(coreInstance);
            }
            if (Instance.instanceOf(coreInstance, M3Paths.ConcreteFunctionDefinition, this.processorSupport))
            {
                FunctionProcessor.processFunctionDefinition(coreInstance, processorContext, this.processorSupport);
            }
            if (Instance.instanceOf(coreInstance, M3Paths.NativeFunction, this.processorSupport))
            {
                processorContext.getNativeFunctionProcessor().buildNativeLambdaFunction(coreInstance, processorContext);
            }
            if (Instance.instanceOf(coreInstance, M3Paths.Measure, this.processorSupport))
            {
                RichIterable<CoreInstance> processedMeasure = MeasureProcessor.processMeasure(coreInstance, processorContext);
                this.processedMeasures.addAllIterable(processedMeasure);
            }
            if (Instance.instanceOf(coreInstance, M3Paths.Unit, this.processorSupport))
            {
                RichIterable<CoreInstance> processedUnit = UnitProcessor.processUnit(coreInstance, processorContext);
                this.processedUnits.addAllIterable(processedUnit);
            }
            LazyIterate.flatCollect(this.extensions, CompiledExtension::getExtraPackageableElementProcessors).forEach(e -> e.value(coreInstance, this, processorContext));
        }
        catch (PureCompilationException e)
        {
            throw e;
        }
        catch (Throwable t)
        {
            PureException pe = PureException.findPureException(t);
            if (pe == null)
            {
                throw new PureCompilationException(coreInstance.getSourceInformation(), "Error generating Java code for " + coreInstance, t);
            }
            else
            {
                String info = pe.getInfo();
                throw new PureCompilationException(coreInstance.getSourceInformation(), info == null ? "Error generating Java code for " + coreInstance : info, pe);
            }
        }
    }

    private String buildFunctionClass(String name, RichIterable<String> functionDefinitions, MapIterable<String, String> lambdaFunctions, MapIterable<String, String> nativeFunctions)
    {
        return
                "public class " + name + "\n" +
                        "{\n" +
                        ((lambdaFunctions != null || nativeFunctions != null) ?
                        "    public static MutableMap<String, SharedPureFunction<?>> __functions = Maps.mutable.empty();\n" +
                        "    static\n" +
                        "    {\n" +
                        (lambdaFunctions == null ? "" : lambdaFunctions.keyValuesView().collect(keyValuePair -> "        __functions.put(\"" + keyValuePair.getOne() + "\", " + keyValuePair.getTwo() + ");\n").makeString("")) +
                        (nativeFunctions == null ? "" : nativeFunctions.keyValuesView().collect(keyValuePair -> "        __functions.put(\"" + keyValuePair.getOne() + "\", " + keyValuePair.getTwo() + ");\n").makeString("")) +
                        "    }\n" :
                        "    public static MutableMap<String, SharedPureFunction<?>> __functions = Maps.fixedSize.empty();\n"
                        ) +
                        (functionDefinitions == null || functionDefinitions.isEmpty() ? "" : functionDefinitions.makeString("\n", "\n\n", "\n")) +
                        "}";
    }

    private String buildExternalizableFunctionClass(RichIterable<String> functionDefinitions)
    {
        try
        {
            java.lang.Class<?> externalClass = Thread.currentThread().getContextClassLoader().loadClass("org.finos.legend.pure.runtime.java.compiled.generation.ExternalClassBuilder");
            Method method = externalClass.getMethod("buildExternalizableFunctionClass", RichIterable.class, String.class, RichIterable.class);
            return (String) method.invoke(null, functionDefinitions, EXTERNAL_FUNCTIONS_CLASS_NAME, this.codeStorage.getAllRepoNames());
        }
        catch (ReflectiveOperationException e)
        {
            throw new RuntimeException(e);
        }
    }

    private String buildPureCompiledLambda(ProcessorContext processorContext)
    {
        return "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.*;\n" +
                "import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstance;\n" +
                "public class PureCompiledLambda extends ReflectiveCoreInstance implements LambdaFunction<Object>, org.finos.legend.pure.runtime.java.compiled.generation.processors.support.LambdaCompiledExtended\n" +
                "{\n" +
                "     LambdaFunction lambdaFunction;\n" +
                "     public SharedPureFunction pureFunction;\n" +
                "\n" +
                "    public PureCompiledLambda()\n" +
                "    {\n" +
                "        super(\"Anonymous_Lambda\");\n" +
                "    }\n" +
                "\n" +
                "    public PureCompiledLambda(String id)\n" +
                "    {\n" +
                "        super(id);\n" +
                "    }\n" +
                "\n" +
                "     public PureCompiledLambda lambdaFunction(LambdaFunction lambdaFunction)\n" +
                "     {\n" +
                "         this.lambdaFunction = (LambdaFunction)lambdaFunction;\n" +
                "         return this;\n" +
                "     }\n" +
                "\n" +
                "     public PureCompiledLambda pureFunction(SharedPureFunction pureFunction)\n" +
                "     {\n" +
                "         this.pureFunction = pureFunction;\n" +
                "         return this;\n" +
                "     }\n" +
                "\n" +
                "    public SharedPureFunction pureFunction()" +
                "    {" +
                "       return this.pureFunction;" +
                "    }\n" +
                "    public String __id(){return this.lambdaFunction == null ? \"Anonymous_Lambda\" : this.lambdaFunction.getName();}\n" +
                "    public PureCompiledLambda copy()\n" +
                "    {\n" +
                "        PureCompiledLambda p =  new PureCompiledLambda();\n" +
                "        p.lambdaFunction = (LambdaFunction)((AbstractCoreInstance)this.lambdaFunction).copy();\n" +
                "        p.pureFunction = this.pureFunction;\n" +
                "        return p;\n" +
                "    }\n" +
                "\n" +
                "    public static SharedPureFunction getPureFunction(final org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> function, ExecutionSupport es)\n" +
                "    {\n" +
                "        if (function == null)\n" +
                "        {\n" +
                "            return null;\n" +
                "        }\n" +
                "        else\n" +
                "        {\n" +
                "            return CoreGen.getSharedPureFunction(function, es);\n" +
                "        }" +
                "    }\n" +
                "\n" +
                this.processorSupport.class_getSimpleProperties(this.processorSupport.package_getByUserPath(M3Paths.LambdaFunction)).collect(coreInstance ->
                {
                    CoreInstance functionType = this.processorSupport.function_getFunctionType(coreInstance);
                    CoreInstance unresolvedReturnType = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, this.processorSupport);
                    CoreInstance returnType = GenericType.isGenericTypeConcrete(unresolvedReturnType) ? unresolvedReturnType : Type.wrapGenericType(this.processorSupport.package_getByUserPath(M3Paths.Any), this.processorSupport);
                    String name = Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.name, this.processorSupport).getName();
                    CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(coreInstance, M3Properties.multiplicity, this.processorSupport);
                    return buildDelegationReadProperty(coreInstance, "LambdaFunction", "this.lambdaFunction", "", name, returnType, unresolvedReturnType, multiplicity, this.processorSupport, processorContext);

                }).makeString("", "\n", "\n") +
                "\n" +
                "    public String getFullSystemPath()\n" +
                "    {\n" +
                "        return \"Root::meta::pure::metamodel::function::LambdaFunction\";\n" +
                "    }\n" +
                "}";
    }

    public static String buildDelegationReadProperty(CoreInstance property, String className, String owner, String classOwnerFullId, String name, CoreInstance returnType, CoreInstance unresolvedReturnType, CoreInstance multiplicity, ProcessorSupport processorSupport, ProcessorContext processorContext)
    {
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(returnType, M3Properties.rawType, processorSupport);
        boolean isPrimitive = rawType != null && Instance.instanceOf(rawType, M3Paths.PrimitiveType, processorSupport);
        boolean makePrimitiveIfPossible = GenericType.isGenericTypeConcrete(unresolvedReturnType) && Multiplicity.isToOne(multiplicity, true);
        String typePrimitive = TypeProcessor.pureTypeToJava(returnType, true, makePrimitiveIfPossible, processorSupport);
        String typeObject = TypeProcessor.pureTypeToJava(returnType, true, false, processorSupport);

        if (Multiplicity.isToOne(multiplicity, false))
        {
            //always include the reverse setter - possible that the Association is defined for a super-class, and the association-end
            // was overridden at the concrete class level.  In this case, the reverse needs to exist due to the super-class interface,
            // it just won't be called.
            return (isPrimitive ? "" :
                    "\n" +
                            "    public void _reverse_" + name + "(" + typePrimitive + " val)\n" +
                            "    {\n" +
                            "        throw new RuntimeException(\"Not Supported !\");" +
                            "    }\n" +
                            "\n" +
                            "    public void _sever_reverse_" + name + "(" + typePrimitive + " val)\n" +
                            "    {\n" +
                            "        throw new RuntimeException(\"Not Supported!\");" +
                            "    }\n") +
                    "\n" +
                    "    public " + className + " _" + name + "(" + typePrimitive + " val)\n" +
                    "    {\n" +
                    "        " + owner + "._" + name + "(val);" +
                    "        return this;\n" +
                    "    }\n" +
                    "\n" +
                    "    public " + className + " _" + name + "Remove()\n" +
                    "    {\n" +
                    "        " + owner + "._" + name + "Remove();\n" +
                    "        return this;\n" +
                    "    }\n" +
                    "\n" +
                    "    public " + typePrimitive + " _" + name + "()\n" +
                    "    {\n" +
                    "        return " + owner + "._" + name + "();\n" +
                    "    }\n"
                    + ClassImplProcessor.buildPropertyToOneGetterCoreInstance(property, returnType, name, processorContext);
        }
        else
        {
            //always include the reverse setter - possible that the Association is defined for a super-class, and the association-end
            // was overridden at the concrete class level.  In this case, the reverse needs to exist due to the super-class interface,
            // it just won't be called.
            return (isPrimitive ? "" :
                    "\n" +
                            "    public void _reverse_" + name + "(" + typePrimitive + " val)\n" +
                            "    {\n" +
                            "        throw new RuntimeException(\"Not Supported in Lazy Mode!\");" +
                            "    }\n" +
                            "\n" +
                            "    public void _sever_reverse_" + name + "(" + typePrimitive + " val)\n" +
                            "    {\n" +
                            "        throw new RuntimeException(\"Not Supported in Lazy Mode!\");" +
                            "    }\n") +
                    "\n" +
                    "    public " + className + " _" + name + "(RichIterable<? extends " + typeObject + "> val)\n" +
                    "    {\n" +
                    "        " + owner + "._" + name + "(val);\n" +
                    "        return this;\n" +
                    "    }\n" +
                    "\n" +
                    "    public " + className + " _" + name + "Add(" + typeObject + " val)\n" +
                    "    {\n" +
                    "        " + owner + "._" + name + "Add(val);\n" +
                    "        return this;\n" +
                    "    }\n" +
                    "\n" +
                    "    public " + className + " _" + name + "AddAll(RichIterable<? extends " + typeObject + "> val)\n" +
                    "    {\n" +
                    "        " + owner + "._" + name + "AddAll(val);\n" +
                    "        return this;\n" +
                    "    }\n" +
                    "    public " + className + " _" + name + "Remove(" + typeObject + " val)\n" +
                    "    {\n" +
                    "        " + owner + "._" + name + "Remove(val);\n" +
                    "        return this;\n" +
                    "    }\n" +
                    "    public " + className + " _" + name + "Remove()\n" +
                    "    {\n" +
                    "        " + owner + "._" + name + "Remove();\n" +
                    "        return this;\n" +
                    "    }\n" +
                    "    public RichIterable<? extends " + typeObject + "> _" + name + "()\n" +
                    "    {\n" +
                    "        return " + owner + "._" + name + "();\n" +
                    "    }\n" +
                    ClassImplProcessor.buildPropertyToManyGetterCoreInstance(property, returnType, name, processorContext) +
                    ClassImplProcessor.buildPropertyToManySetterCoreInstance(className, name) +
                    ClassImplProcessor.buildPropertyToManyRemoveItemCoreInstance(className, name) +
                    ClassImplProcessor.buildPropertyToManyAddAllCoreInstance(name, owner, className) +
                    ClassImplProcessor.buildPropertyToManyAddCoreInstance(name, owner, className);
        }
    }


    private String buildLambdaZero()
    {
        return "public interface LambdaZero<T>\n" +
                "{\n" +
                "    T execute();\n" +
                "}\n";
    }

    private static String toFactoryRegistryEntry(CoreInstance _class, ProcessorSupport processorSupport)
    {
        String path = PackageableElement.getUserPathForPackageableElement(_class);
        String factory = ClassProcessor.requiresCompilationImpl(processorSupport, _class) ? JavaPackageAndImportBuilder.buildImplClassReferenceFromType(_class, ClassImplIncrementalCompilationProcessor.CLASS_IMPL_SUFFIX) :
                M3ToJavaGenerator.getFullyQualifiedM3ImplForCompiledModel(_class);
        String factoryInterface = M3ToJavaGenerator.getFullyQualifiedM3InterfaceForCompiledModel(_class);
        return "\t\tinterfaceByPath.put(\"" + path + "\", " + factoryInterface + ".class);\n" +
                "\t\ttypeFactoriesByPath.put(\"" + path + "\", " + factory + ".FACTORY);\n";
    }

    private static String toEnumFactoryRegistryEntry(CoreInstance _enum)
    {
        String path = PackageableElement.getUserPathForPackageableElement(_enum);
        return "\t\tinterfaceByPath.put(\"" + path + "\", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum.class);\n" +
                "\t\ttypeFactoriesByPath.put(\"" + path + "\", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.EnumInstance.FACTORY);\n";
    }

    private static String toMeasureFactoryRegistryEntry(CoreInstance measure)
    {
        String path = PackageableElement.getUserPathForPackageableElement(measure);
        return "\t\tinterfaceByPath.put(\"" + path + "\", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure.class);\n" +
                "\t\ttypeFactoriesByPath.put(\"" + path + "\", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.MeasureInstance.FACTORY);\n";
    }

    private static String toUnitFactoryRegistryEntry(CoreInstance unit)
    {
        String path = PackageableElement.getUserPathForPackageableElement(unit);
        return "\t\tinterfaceByPath.put(\"" + path + "\", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit.class);\n" +
                "\t\ttypeFactoriesByPath.put(\"" + path + "\",org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.UnitInstance.FACTORY);\n"; // TODO: org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Unit_Impl
    }

    private String getFactoryRegistryName()
    {
        return this.name + "JavaModelFactoryRegistry";
    }

    private String buildFactoryRegistry()
    {
        String className = getFactoryRegistryName();
        RichIterable<CoreInstance> platformClasses = this.processedClasses.select(ClassProcessor.IS_PLATFORM_CLASS);
        RichIterable<CoreInstance> processedMeasures = this.processedMeasures;
        RichIterable<CoreInstance> processedUnits = this.processedUnits;

        RichIterable<CoreInstance> m3PlatformEnums = this.platformEnumerations.select(each -> "/platform/pure/m3.pure".equals(each.getSourceInformation().getSourceId()));

        int count = platformClasses.size() + m3PlatformEnums.size();

        return "import org.eclipse.collections.api.map.MutableMap;\n" +
                "import org.eclipse.collections.impl.factory.primitive.IntObjectMaps;\n" +
                "import org.eclipse.collections.impl.map.mutable.UnifiedMap;\n" +
                "import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;\n" +
                "import org.finos.legend.pure.m4.coreinstance.factory.CoreInstanceFactory;\n" +
                "\n" +
                "public class " + className + " implements org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistry\n" +
                "{\n" +
                "    public static final CoreInstanceFactoryRegistry REGISTRY;\n" +
                "\n" +
                "    static\n" +
                "    {\n" +
                "        MutableMap<String, java.lang.Class> interfaceByPath = UnifiedMap.newMap(" + count + ");\n" +
                "        MutableMap<String, CoreInstanceFactory> typeFactoriesByPath = UnifiedMap.newMap(" + count + ");\n" +
                platformClasses.collectWith(JavaSourceCodeGenerator::toFactoryRegistryEntry, this.processorSupport).makeString("") +
                m3PlatformEnums.collect(JavaSourceCodeGenerator::toEnumFactoryRegistryEntry).makeString("") +
                processedMeasures.collect(JavaSourceCodeGenerator::toMeasureFactoryRegistryEntry).makeString("") +
                processedUnits.collect(JavaSourceCodeGenerator::toUnitFactoryRegistryEntry).makeString("") + // TODO: TO_FACTORY_REGISTRY_ENTRY
                "        REGISTRY = new CoreInstanceFactoryRegistry(IntObjectMaps.immutable.<CoreInstanceFactory>empty(), typeFactoriesByPath.toImmutable(), interfaceByPath.toImmutable());\n" +
                "    }\n" +
                "}\n";
    }

    public void addToProcessedClasses(RichIterable<CoreInstance> processedClasses)
    {
        this.processedClasses.addAllIterable(processedClasses);
    }

    public boolean hasJavaSerializationSupport(CoreInstance coreInstance)
    {
        return this.javaSerializedClasses.contains(coreInstance);
    }
}
