package org.finos.legend.pure.runtime.java.compiled.generation.processors;

import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.factory.primitive.ObjectIntMaps;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.serialization.filesystem.PureCodeStorage;
import org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepository;
import org.finos.legend.pure.m3.serialization.filesystem.usercodestorage.MutableCodeStorage;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntime;
import org.finos.legend.pure.m3.serialization.runtime.PureRuntimeBuilder;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.tools.GraphNodeIterable;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestIdBuilder
{
    private static PureRuntime runtime;
    private static ProcessorSupport processorSupport;

    @BeforeClass
    public static void setUp()
    {
        MutableCodeStorage codeStorage = PureCodeStorage.createCodeStorage(null, Lists.immutable.with(CodeRepository.newPlatformCodeRepository()));
        runtime = new PureRuntimeBuilder(codeStorage).setTransactionalByDefault(false).buildAndInitialize();
        processorSupport = runtime.getProcessorSupport();
    }

    @Test
    public void testIdUniqueness()
    {
        testIdUniqueness(IdBuilder.newIdBuilder(processorSupport));
    }

    @Test
    public void testIdUniquenessWithPrefix()
    {
        testIdUniqueness(IdBuilder.newIdBuilder("$core$", processorSupport));
    }

    private void testIdUniqueness(IdBuilder idBuilder)
    {
        MutableSet<CoreInstance> excludedClassifiers = PrimitiveUtilities.getPrimitiveTypes(processorSupport).toSet();
        MutableMap<CoreInstance, MutableSet<String>> classifierIds = Maps.mutable.empty();
        MutableMap<CoreInstance, MutableObjectIntMap<String>> idConflicts = Maps.mutable.empty();
        GraphNodeIterable.fromModelRepository(runtime.getModelRepository())
                .forEach(instance ->
                {
                    CoreInstance classifier = instance.getClassifier();
                    if (!excludedClassifiers.contains(classifier))
                    {
                        String id = idBuilder.buildId(instance);
                        if (!classifierIds.getIfAbsentPut(classifier, Sets.mutable::empty).add(id))
                        {
                            idConflicts.getIfAbsentPut(classifier, ObjectIntMaps.mutable::empty).updateValue(id, 1, n -> n + 1);
                        }
                    }
                });
        if (idConflicts.notEmpty())
        {
            MutableMap<CoreInstance, String> classifierPaths = idConflicts.keysView().toMap(c -> c, PackageableElement::getUserPathForPackageableElement);
            StringBuilder builder = new StringBuilder("The following ids have conflicts:");
            idConflicts.keysView().toSortedListBy(classifierPaths::get).forEach(classifier ->
            {
                builder.append("\n\t").append(classifierPaths.get(classifier));
                ObjectIntMap<String> classifierIdConflicts = idConflicts.get(classifier);
                classifierIdConflicts.forEachKeyValue((id, count) -> builder.append("\n\t\t").append(id).append(": ").append(count).append(" instances"));
            });
            Assert.fail(builder.toString());
        }
    }
}
