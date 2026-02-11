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
import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.set.strategy.mutable.UnifiedSetWithHashingStrategy;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.pure.m3.bootstrap.generator.M3ToJavaGenerator;
import org.finos.legend.pure.m3.compiler.visibility.AccessLevel;
import org.finos.legend.pure.m3.execution.test.TestTools;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.function.Function;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.navigation.type.Type;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.RepositoryCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m3.tools.JavaTools;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.exception.PureException;
import org.finos.legend.pure.runtime.java.compiled.compiler.StringJavaSource;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtension;
import org.finos.legend.pure.runtime.java.compiled.extension.CompiledExtensionLoader;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.ClassJsonFactoryProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.FunctionProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.IdBuilder;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.EnumProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.ExtendedPrimitiveTypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.TypeProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassImplIncrementalCompilationProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassImplProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type._class.ClassProcessor;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.type.measureUnit.MeasureProcessor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;

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

                    "import org.finos.legend.pure.m4.coreinstance.CoreInstance;\n" +
                    "import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;\n" +
                    "import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;\n" +

                    "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;\n" +
                    "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;\n" +
                    "import org.finos.legend.pure.m3.exception.PureExecutionException;\n" +
                    "import org.finos.legend.pure.m3.execution.ExecutionSupport;\n" +
                    "import org.finos.legend.pure.m3.navigation.ProcessorSupport;\n" +
                    "import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;\n" +
                    "import org.finos.legend.pure.m3.navigation.generictype.GenericType;\n" +
                    "import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.ChangeType;\n" +
                    "import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.ChangedPath;\n" +
                    "import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.vcs.Revision;\n" +
                    "import org.finos.legend.pure.m3.tools.ListHelper;\n" +

                    "import org.finos.legend.pure.runtime.java.compiled.execution.*;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.execution.sourceInformation.*;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.*;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.coreinstance.*;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.*;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.function.defended.*;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.map.*;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.metadata.*;\n" +
                    "import org.finos.legend.pure.runtime.java.compiled.serialization.model.*;\n" +

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
                    "\n";

    private final ProcessorSupport processorSupport;
    private final IdBuilder idBuilder;
    private final RepositoryCodeStorage codeStorage;
    private final boolean writeFilesToDisk;
    private final Path directoryToWriteFilesTo;
    private final String externalAPIPackage;

    private final boolean includePureStackTrace;
    private final MutableSet<CoreInstance> processedClasses = Sets.mutable.empty();
    private final MutableSet<CoreInstance> platformEnumerations = Sets.mutable.empty();
    private final MutableSet<CoreInstance> javaSerializedClasses = Sets.mutable.empty();
    private final ListIterable<CompiledExtension> extensions;

    private final String name;

    public JavaSourceCodeGenerator(ProcessorSupport processorSupport, IdBuilder idBuilder, RepositoryCodeStorage codeStorage, boolean writeFilesToDisk, Path directoryToWriteFilesTo, boolean includePureStackTrace, Iterable<? extends CompiledExtension> providedExtensions, String name, String externalAPIPackage, boolean generateCompilerExtensionCode)
    {
        this.name = name;
        this.processorSupport = processorSupport;
        this.idBuilder = (idBuilder == null) ? IdBuilder.newIdBuilder(this.processorSupport) : idBuilder;
        this.codeStorage = codeStorage;
        this.writeFilesToDisk = writeFilesToDisk;
        this.directoryToWriteFilesTo = directoryToWriteFilesTo;
        this.includePureStackTrace = includePureStackTrace;
        this.externalAPIPackage = externalAPIPackage;
        this.extensions = new UnifiedSetWithHashingStrategy<>(new HashingStrategy<CompiledExtension>()
        {
            @Override
            public int computeHashCode(CompiledExtension extension)
            {
                return extension.getClass().hashCode();
            }

            @Override
            public boolean equals(CompiledExtension extension1, CompiledExtension extension2)
            {
                return extension1.getClass() == extension2.getClass();
            }
        }).withAll(CompiledExtensionLoader.extensions()).withAll(providedExtensions).toList();
    }

    public JavaSourceCodeGenerator(ProcessorSupport processorSupport, RepositoryCodeStorage codeStorage, boolean writeFilesToDisk, Path directoryToWriteFilesTo, boolean includePureStackTrace, Iterable<? extends CompiledExtension> extensions, String name, String externalAPIPackage, boolean generateCompilerExtensionCode)
    {
        this(processorSupport, null, codeStorage, writeFilesToDisk, directoryToWriteFilesTo, includePureStackTrace, extensions, name, externalAPIPackage, generateCompilerExtensionCode);
    }

    public ProcessorSupport getProcessorSupport()
    {
        return this.processorSupport;
    }


    public ProcessorContext getProcessorContext()
    {
        return new ProcessorContext(this.processorSupport, this.extensions, this.idBuilder, this.includePureStackTrace);
    }

    ListIterable<StringJavaSource> generateCode(Source source, CodeRepository codeRepository)
    {
       return generateCode(source, codeRepository, null, true);
    }


    ListIterable<StringJavaSource> generateCode(Source source, CodeRepository codeRepository, String compileGroup, boolean generatePureTests)
    {
        if (source.getNewInstances() == null)
        {
            return Lists.fixedSize.empty();
        }
        try
        {
            ProcessorContext processorContext = getProcessorContext();

            source.getNewInstances().forEach(coreInstance ->
            {
                if (!Instance.instanceOf(coreInstance, M3Paths.Package, this.processorSupport) && (generatePureTests || compileGroup == null || compileGroup.startsWith("core") || !TestTools.hasAnyTestStereotype(coreInstance, processorSupport) || !Instance.instanceOf(coreInstance, M3Paths.FunctionDefinition, this.processorSupport) || coreInstance.getValueForMetaPropertyToMany(M3Properties.applications).notEmpty()))
                {
                    this.toJava(coreInstance, codeRepository, null, processorContext);
                }
            });

            MutableList<StringJavaSource> javaClasses = buildJavaClasses(processorContext);
            if (this.writeFilesToDisk)
            {
                this.javaClassesToDisk(javaClasses);
            }

            return javaClasses;
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error generating Java code for " + source.getId(), e);
        }
    }

    public ListIterable<StringJavaSource> generateCode(MutableSet<String> allTypes)
    {
        CoreInstance root = this.processorSupport.package_getByUserPath("::");

        ProcessorContext processorContext = new ProcessorContext(this.processorSupport, this.extensions, this.idBuilder, this.includePureStackTrace);


        toJava(this.processorSupport.repository_getTopLevel(M3Paths.Package), null, allTypes, processorContext);
        toJava(root, null, allTypes, processorContext);

        MutableList<StringJavaSource> javaClasses = Lists.mutable.empty();
        javaClasses.addAll(this.buildJavaClasses(processorContext));

        if (allTypes == null)
        {
            // Generate Helper Classes if we don't generate Factories
            javaClasses.addAllIterable(this.generatePureCoreHelperClasses(processorContext));
        }
        else
        {
            javaClasses.add(StringJavaSource.newStringJavaSource(JavaPackageAndImportBuilder.platformJavaPackage(), getFactoryRegistryName(), this.buildFactoryRegistry()));
        }

        if (this.writeFilesToDisk)
        {
            this.javaClassesToDisk(javaClasses);
        }

        return javaClasses;
    }

    void collectClassesToSerialize()
    {
        AccessLevel.EXTERNALIZABLE.getStereotype(this.processorSupport).getValueForMetaPropertyToMany(M3Properties.modelElements).forEach(element ->
        {
            if (Instance.instanceOf(element, M3Paths.Function, this.processorSupport))
            {
                CoreInstance functionType = this.processorSupport.function_getFunctionType(element);
                functionType.getValueForMetaPropertyToMany(M3Properties.parameters).forEach(parameter ->
                {
                    CoreInstance type = Instance.getValueForMetaPropertyToOneResolved(parameter, M3Properties.genericType, this.processorSupport);
                    CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(type, M3Properties.rawType, this.processorSupport);
                    if (M3Paths.Map.equals(PackageableElement.getUserPathForPackageableElement(rawType)))
                    {
                        CoreInstance mapKey = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(type, M3Properties.typeArguments, this.processorSupport).get(0), M3Properties.rawType, this.processorSupport);
                        if (Instance.instanceOf(mapKey, M3Paths.Class, this.processorSupport))
                        {
                            this.addClassToSerialize(mapKey, this.javaSerializedClasses);
                        }
                        CoreInstance mapValue = Instance.getValueForMetaPropertyToOneResolved(Instance.getValueForMetaPropertyToManyResolved(type, M3Properties.typeArguments, this.processorSupport).get(1), M3Properties.rawType, this.processorSupport);
                        if (Instance.instanceOf(mapValue, M3Paths.Class, this.processorSupport))
                        {
                            this.addClassToSerialize(mapValue, this.javaSerializedClasses);
                        }
                    }
                });
            }
        });
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
        CoreInstance functionClass = this.processorSupport.package_getByUserPath(M3Paths.Function);
        ProcessorContext processorContext = new ProcessorContext(this.processorSupport, this.extensions, this.idBuilder, this.includePureStackTrace);
        MutableList<String> externalizableFunctionCode = AccessLevel.EXTERNALIZABLE.getStereotype(this.processorSupport).getValueForMetaPropertyToMany(M3Properties.modelElements).collectIf(
                e -> (e instanceof Function) || Instance.instanceOf(e, functionClass, this.processorSupport),
                e -> FunctionProcessor.buildExternalizableFunction(e, processorContext),
                Lists.mutable.empty());
        String text = ExternalClassBuilder.buildExternalizableFunctionClass(pack, EXTERNAL_FUNCTIONS_CLASS_NAME, externalizableFunctionCode, this.codeStorage.getAllRepositories().collect(CodeRepository::getName));
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
        MutableList<StringJavaSource> javaClasses = Lists.mutable.withAll(processorContext.getClasses());

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

    private void toJava(CoreInstance coreInstance, CodeRepository codeRepository, MutableSet<String> allTypes, ProcessorContext processorContext) throws PureCompilationException
    {
        if (Instance.instanceOf(coreInstance, M3Paths.Package, this.processorSupport))
        {
            coreInstance.getValueForMetaPropertyToMany(M3Properties.children).forEach(e -> toJava(e, codeRepository, allTypes, processorContext));
        }
        if (codeRepository == null || (coreInstance.getSourceInformation() != null && coreInstance.getSourceInformation().getSourceId().startsWith("/" + codeRepository.getName())))
        {
            if (allTypes == null || allTypes.contains(PackageableElement.getUserPathForPackageableElement(coreInstance)))
            {
                try
                {
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
                    if (Type.isExtendedPrimitiveType(coreInstance, processorSupport))
                    {
                        ExtendedPrimitiveTypeProcessor.processExtendedPrimitiveType(coreInstance, processorContext);
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
                        MeasureProcessor.processMeasure(coreInstance, processorContext);
                    }
                    // We only want to execute if the related repository is available.
                    // Otherwise the type are not in the model and the instanceOf code will fail later.
                    this.extensions.select(c -> this.codeStorage.getRepository(c.getRelatedRepository()) != null).flatCollect(CompiledExtension::getExtraPackageableElementProcessors).forEach(e -> e.value(coreInstance, this, processorContext));

                }
                catch (PureCompilationException e)
                {
                    throw e;
                }
                catch (Exception e)
                {
                    PureException pe = PureException.findPureException(e);
                    if (pe == null)
                    {
                        throw new PureCompilationException(coreInstance.getSourceInformation(), "Error generating Java code for " + coreInstance, e);
                    }

                    String info = pe.getInfo();
                    throw new PureCompilationException(coreInstance.getSourceInformation(), info == null ? "Error generating Java code for " + coreInstance : info, pe);
                }
            }
        }
    }

    private String buildFunctionClass(String name, RichIterable<String> functionDefinitions, MapIterable<String, String> lambdaFunctions, MapIterable<String, String> nativeFunctions)
    {
        StringBuilder staticBlockContent = new StringBuilder();
        StringBuilder helperMethodsContent = new StringBuilder();
        MutableList<Pair<String, String>> allFunctions = Lists.mutable.empty();

        if (lambdaFunctions != null)
        {
            lambdaFunctions.forEachKeyValue((key, value) -> allFunctions.add(Tuples.pair(key, value)));
        }
        if (nativeFunctions != null)
        {
            nativeFunctions.forEachKeyValue((key, value) -> allFunctions.add(Tuples.pair(key, value)));
        }

        allFunctions.sort(Comparator.comparing(Pair::getOne));

        if (!allFunctions.isEmpty())
        {
            staticBlockContent.append("    public static MutableMap<String, SharedPureFunction<?>> __functions = Maps.mutable.empty();\n");
            staticBlockContent.append("    static\n");
            staticBlockContent.append("    {\n");

            allFunctions.forEach(pair ->
            {
                String key = pair.getOne();
                String methodName = JavaTools.makeValidJavaIdentifier(key);
                staticBlockContent.append("        __functions.put(\"").append(key).append("\", ").append(methodName).append("());\n");
            });

            staticBlockContent.append("    }\n");
        }
        else
        {
            staticBlockContent.append("    public static MutableMap<String, SharedPureFunction<?>> __functions = Maps.fixedSize.empty();\n");
        }

        if (!allFunctions.isEmpty())
        {
            allFunctions.forEach(pair ->
            {
                String key = pair.getOne();
                String value = pair.getTwo();
                String methodName = JavaTools.makeValidJavaIdentifier(key);
                helperMethodsContent.append("\n");
                helperMethodsContent.append("    private static SharedPureFunction<?> ").append(methodName).append("()\n");
                helperMethodsContent.append("    {\n");
                helperMethodsContent.append("        return ").append(value).append(";\n");
                helperMethodsContent.append("    }\n");
            });
        }

        return
                "public class " + name + "\n" +
                        "{\n" +
                        staticBlockContent.toString() +
                        (functionDefinitions == null || functionDefinitions.isEmpty() ? "" : functionDefinitions.makeString("\n", "\n\n", "\n")) +
                        helperMethodsContent.toString() +
                        "}";
    }

    private String buildPureCompiledLambda(ProcessorContext processorContext)
    {
        return "import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.*;\n" +
                "\n" +
                "public class PureCompiledLambda extends AbstractPureCompiledLambda<Object>\n" +
                "{\n" +
                "    public PureCompiledLambda(LambdaFunction lambdaFunction, SharedPureFunction pureFunction)\n" +
                "    {\n" +
                "        super(lambdaFunction, pureFunction);\n" +
                "    }\n" +
                "\n" +
                "    public PureCompiledLambda(ExecutionSupport executionSupport, String lambdaId, SharedPureFunction pureFunction)\n" +
                "    {\n" +
                "        super(executionSupport, lambdaId, pureFunction);\n" +
                "    }\n" +
                "\n" +
                "    @Override\n" +
                "    public PureCompiledLambda copy()\n" +
                "    {\n" +
                "        LambdaFunction<Object> lambda = lambdaFunction();\n" +
                "        return new PureCompiledLambda((lambda == null) ? null : lambda.copy(), pureFunction());\n" +
                "    }\n" +
                this.processorSupport.class_getSimpleProperties(this.processorSupport.package_getByUserPath(M3Paths.LambdaFunction)).toSortedListBy(CoreInstance::getName).collect(prop ->
                {
                    CoreInstance functionType = this.processorSupport.function_getFunctionType(prop);
                    CoreInstance unresolvedReturnType = Instance.getValueForMetaPropertyToOneResolved(functionType, M3Properties.returnType, this.processorSupport);
                    CoreInstance returnType = GenericType.isGenericTypeConcrete(unresolvedReturnType) ? unresolvedReturnType : Type.wrapGenericType(this.processorSupport.package_getByUserPath(M3Paths.Any), this.processorSupport);
                    CoreInstance multiplicity = Instance.getValueForMetaPropertyToOneResolved(prop, M3Properties.multiplicity, this.processorSupport);
                    return buildDelegationReadProperty(prop, "LambdaFunction", "lambdaFunction()", true, "", Property.getPropertyName(prop), returnType, unresolvedReturnType, multiplicity, this.processorSupport, processorContext);
                }).makeString("", "\n", "\n") +
                "    public static SharedPureFunction getPureFunction(org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> function, ExecutionSupport es)\n" +
                "    {\n" +
                "        return (function == null) ? null : CoreGen.getSharedPureFunction(function, es);\n" +
                "    }\n" +
                "}\n";
    }

    public static String buildDelegationReadProperty(CoreInstance property, String className, String owner, String classOwnerFullId, String name, CoreInstance returnType,
                                                     CoreInstance unresolvedReturnType, CoreInstance multiplicity, ProcessorSupport processorSupport, ProcessorContext processorContext)
    {
        return buildDelegationReadProperty(property, className, owner, false, classOwnerFullId, name, returnType, unresolvedReturnType, multiplicity, processorSupport, processorContext);
    }

    public static String buildDelegationReadProperty(CoreInstance property, String className, String owner, boolean returnOwnerOnModify, String classOwnerFullId, String name, CoreInstance returnType, CoreInstance unresolvedReturnType, CoreInstance multiplicity, ProcessorSupport processorSupport, ProcessorContext processorContext)
    {
        CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(returnType, M3Properties.rawType, processorSupport);
        boolean isPrimitive = rawType != null && Instance.instanceOf(rawType, M3Paths.PrimitiveType, processorSupport);
        boolean makePrimitiveIfPossible = GenericType.isGenericTypeConcrete(unresolvedReturnType) && Multiplicity.isToOne(multiplicity, true);
        String typePrimitive = TypeProcessor.pureTypeToJava(returnType, true, makePrimitiveIfPossible, processorSupport);
        String typeObject = TypeProcessor.pureTypeToJava(returnType, true, false, processorSupport);
        String returnRef = returnOwnerOnModify ? owner : "this";

        if (Multiplicity.isToOne(multiplicity, false))
        {
            //always include the reverse setter - possible that the Association is defined for a super-class, and the association-end
            // was overridden at the concrete class level.  In this case, the reverse needs to exist due to the super-class interface,
            // it just won't be called.
            return (isPrimitive ? "" :
                    "\n" +
                    "    public void _reverse_" + name + "(" + typePrimitive + " val)\n" +
                    "    {\n" +
                    "        throw new RuntimeException(\"Not Supported !\");\n" +
                    "    }\n" +
                    "\n" +
                    "    public void _sever_reverse_" + name + "(" + typePrimitive + " val)\n" +
                    "    {\n" +
                    "        throw new RuntimeException(\"Not Supported!\");\n" +
                    "    }\n") +
                    "\n" +
                    "    public " + className + " _" + name + "(" + typePrimitive + " val)\n" +
                    "    {\n" +
                    "        " + owner + "._" + name + "(val);\n" +
                    "        return " + returnRef + ";\n" +
                    "    }\n" +
                    "\n" +
                    "    public " + className + " _" + name + "Remove()\n" +
                    "    {\n" +
                    "        " + owner + "._" + name + "Remove();\n" +
                    "        return " + returnRef + ";\n" +
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
                    "        return " + returnRef + ";\n" +
                    "    }\n" +
                    "\n" +
                    "    public " + className + " _" + name + "Add(" + typeObject + " val)\n" +
                    "    {\n" +
                    "        " + owner + "._" + name + "Add(val);\n" +
                    "        return " + returnRef + ";\n" +
                    "    }\n" +
                    "\n" +
                    "    public " + className + " _" + name + "AddAll(RichIterable<? extends " + typeObject + "> val)\n" +
                    "    {\n" +
                    "        " + owner + "._" + name + "AddAll(val);\n" +
                    "        return " + returnRef + ";\n" +
                    "    }\n" +
                    "    public " + className + " _" + name + "Remove(" + typeObject + " val)\n" +
                    "    {\n" +
                    "        " + owner + "._" + name + "Remove(val);\n" +
                    "        return " + returnRef + ";\n" +
                    "    }\n" +
                    "    public " + className + " _" + name + "Remove()\n" +
                    "    {\n" +
                    "        " + owner + "._" + name + "Remove();\n" +
                    "        return " + returnRef + ";\n" +
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

    private String toFactoryRegistryEntry(String path, CoreInstance _class)
    {
        String factory = ClassProcessor.requiresCompilationImpl(this.processorSupport, _class) ?
                         JavaPackageAndImportBuilder.buildImplClassReferenceFromType(_class, ClassImplIncrementalCompilationProcessor.CLASS_IMPL_SUFFIX, this.processorSupport) :
                         M3ToJavaGenerator.getFullyQualifiedM3ImplForCompiledModel(_class);
        String factoryInterface = M3ToJavaGenerator.getFullyQualifiedM3InterfaceForCompiledModel(_class);
        return "            .withType(\"" + path + "\", " + factory + ".FACTORY, " + factoryInterface + ".class)\n";
    }

    private String toEnumFactoryRegistryEntry(String path)
    {
        return "            .withType(\"" + path + "\", org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.EnumInstance.FACTORY, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum.class)\n";
    }

    private String getFactoryRegistryName()
    {
        return this.name + "JavaModelFactoryRegistry";
    }

    private String buildFactoryRegistry()
    {
        String className = getFactoryRegistryName();
        MutableList<Pair<String, CoreInstance>> platformClasses = this.processedClasses.collectIf(
                ClassProcessor.IS_PLATFORM_CLASS,
                c -> Tuples.pair(PackageableElement.getUserPathForPackageableElement(c), c),
                Lists.mutable.empty()).sortThisBy(Pair::getOne);
        MutableList<String> m3PlatformEnums = this.platformEnumerations.collectIf(
                each -> "/platform/pure/grammar/m3.pure".equals(each.getSourceInformation().getSourceId()),
                PackageableElement::getUserPathForPackageableElement,
                Lists.mutable.empty()).sortThis();

        int count = platformClasses.size() + m3PlatformEnums.size();
        return "\n" +
                "import org.finos.legend.pure.m3.coreinstance.CoreInstanceFactoryRegistry;\n" +
                "\n" +
                "public class " + className + " implements org.finos.legend.pure.runtime.java.compiled.factory.JavaModelFactoryRegistry\n" +
                "{\n" +
                "    public static final CoreInstanceFactoryRegistry REGISTRY = CoreInstanceFactoryRegistry.builder(" + count + ")\n" +
                platformClasses.asLazy().collect(pair -> toFactoryRegistryEntry(pair.getOne(), pair.getTwo())).makeString("") +
                m3PlatformEnums.asLazy().collect(this::toEnumFactoryRegistryEntry).makeString("") +
                "            .build();\n" +
                "\n" +
                "    @Override\n" +
                "    public CoreInstanceFactoryRegistry getRegistry()\n" +
                "    {\n" +
                "        return REGISTRY;\n" +
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

    public ListIterable<StringJavaSource> generatePureCoreHelperClasses(ProcessorContext processorContext)
    {
        String platform = "import " + JavaPackageAndImportBuilder.platformJavaPackage() + ".*;\n";

        MutableList<StringJavaSource> coreJavaSources = Lists.mutable.with(
                StringJavaSource.newStringJavaSource(JavaPackageAndImportBuilder.platformJavaPackage(), "PureCompiledLambda", imports + platform + this.buildPureCompiledLambda(processorContext)),
                StringJavaSource.newStringJavaSource(JavaPackageAndImportBuilder.platformJavaPackage(), "LambdaZero", imports + platform + this.buildLambdaZero()),
                EnumProcessor.processEnum(),
                EnumProcessor.processEnumLazy());

        if (this.writeFilesToDisk)
        {
            this.javaClassesToDisk(coreJavaSources);
        }

        return coreJavaSources;
    }

    public Collection<StringJavaSource> generateExtensionsCode(String compileGroup)
    {
        MutableList<StringJavaSource> extraJavaSources = Lists.mutable.empty();
        if (compileGroup != null)
        {
            this.extensions.forEach(ext ->
            {
                if (compileGroup.equals(ext.getRelatedRepository()))
                {
                    extraJavaSources.addAll(ext.getExtraJavaSources());
                }
            });
        }
        if (this.writeFilesToDisk)
        {
            javaClassesToDisk(extraJavaSources);
        }
        return extraJavaSources;
    }
}
