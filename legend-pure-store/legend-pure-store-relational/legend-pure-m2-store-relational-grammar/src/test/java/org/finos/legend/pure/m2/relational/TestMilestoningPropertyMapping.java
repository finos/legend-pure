// Copyright 2021 Goldman Sachs
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

package org.finos.legend.pure.m2.relational;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.Mapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.EmbeddedRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RelationalPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.relational.mapping.RootRelationalInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Column;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.Literal;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.RelationalOperationElement;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.SQLNull;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.TableAliasColumn;
import org.finos.legend.pure.m3.coreinstance.meta.relational.metamodel.relation.Table;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.junit.Assert;
import org.junit.Test;

import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.BI_TEMPORAL_MILESTONING_MODEL_CODE;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_EXTENDED_MODEL_CODE;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.BUSINESS_MILESTONING_MODEL_CODE;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MAPPING_CODE;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MODEL_CODE;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.EMBEDDED_MODEL_ID;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.EXTENDED_MAPPING_CODE;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.EXTENDED_MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.EXTENDED_MAPPING_ID;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.EXTENDED_MODEL_ID;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.INLINE_EMBEDDED_MAPPING_CODE;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.INLINE_EMBEDDED_MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.MAPPING_CODE;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.MAPPING_ID;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.MILESTONED_STORE_CODE;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.MODEL_ID;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.NON_MILESTONED_STORE_CODE;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.PROCESSING_MILESTONING_MODEL_CODE;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.STORE_ID;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.TEMPORAL_EMBEDDED_MODEL_CODE;

public class TestMilestoningPropertyMapping extends AbstractPureRelationalTestWithCoreCompiled
{
    @Test
    public void testProcessingMilestoningPropertyMapping()
    {
        runtime.createInMemorySource(MODEL_ID, PROCESSING_MILESTONING_MODEL_CODE);
        runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        runtime.createInMemorySource(MAPPING_ID, MAPPING_CODE);
        runtime.compile();

        Mapping mapping = (Mapping) runtime.getCoreInstance("milestoning::Amap");
        RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation = (RootRelationalInstanceSetImplementation) mapping._classMappings().detect(RootRelationalInstanceSetImplementation.class::isInstance);

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) rootRelationalInstanceSetImplementation._propertyMappings().detect(TestMilestoningPropertyMapping::isMilestoningPropertyMapping);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        ListIterable<RelationalPropertyMapping> relationalPropertyMappings = milestoningEmbeddedRelationalInstance._propertyMappings().collect(RelationalPropertyMapping.class::cast, Lists.mutable.empty());
        Assert.assertEquals(2, relationalPropertyMappings.size());

        RelationalPropertyMapping inPropertyMapping = relationalPropertyMappings.get(0);
        validateProperty(inPropertyMapping._property(), "in", "meta::pure::milestoning::ProcessingDateMilestoning");
        validateTableAliasColumn(inPropertyMapping._relationalOperationElement(), "in_z");

        RelationalPropertyMapping outPropertyMapping = relationalPropertyMappings.get(1);
        validateProperty(outPropertyMapping._property(), "out", "meta::pure::milestoning::ProcessingDateMilestoning");
        validateTableAliasColumn(outPropertyMapping._relationalOperationElement(), "out_z");
    }

    @Test
    public void testBusinessMilestoningPropertyMapping()
    {
        runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        runtime.createInMemorySource(MAPPING_ID, MAPPING_CODE);
        runtime.compile();

        Mapping mapping = (Mapping) runtime.getCoreInstance("milestoning::Amap");
        RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation = (RootRelationalInstanceSetImplementation) mapping._classMappings().detect(RootRelationalInstanceSetImplementation.class::isInstance);

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) rootRelationalInstanceSetImplementation._propertyMappings().detect(TestMilestoningPropertyMapping::isMilestoningPropertyMapping);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = milestoningEmbeddedRelationalInstance._propertyMappings().collect(RelationalPropertyMapping.class::cast, Lists.mutable.empty());
        Assert.assertEquals(2, relationalPropertyMappings.size());

        RelationalPropertyMapping fromPropertyMapping = relationalPropertyMappings.get(0);
        validateProperty(fromPropertyMapping._property(), "from", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(fromPropertyMapping._relationalOperationElement(), "from_z");

        RelationalPropertyMapping thruPropertyMapping = relationalPropertyMappings.get(1);
        validateProperty(thruPropertyMapping._property(), "thru", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(thruPropertyMapping._relationalOperationElement(), "thru_z");
    }

    @Test
    public void testBiTemporalMilestoningPropertyMapping()
    {
        runtime.createInMemorySource(MODEL_ID, BI_TEMPORAL_MILESTONING_MODEL_CODE);
        runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        runtime.createInMemorySource(MAPPING_ID, MAPPING_CODE);
        runtime.compile();

        Mapping mapping = (Mapping) runtime.getCoreInstance("milestoning::Amap");
        RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation = (RootRelationalInstanceSetImplementation) mapping._classMappings().detect(RootRelationalInstanceSetImplementation.class::isInstance);

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) rootRelationalInstanceSetImplementation._propertyMappings().detect(TestMilestoningPropertyMapping::isMilestoningPropertyMapping);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = milestoningEmbeddedRelationalInstance._propertyMappings().collect(RelationalPropertyMapping.class::cast, Lists.mutable.empty());
        Assert.assertEquals(4, relationalPropertyMappings.size());

        RelationalPropertyMapping inPropertyMapping = relationalPropertyMappings.get(0);
        validateProperty(inPropertyMapping._property(), "in", "meta::pure::milestoning::ProcessingDateMilestoning");
        validateTableAliasColumn(inPropertyMapping._relationalOperationElement(), "in_z");

        RelationalPropertyMapping outPropertyMapping = relationalPropertyMappings.get(1);
        validateProperty(outPropertyMapping._property(), "out", "meta::pure::milestoning::ProcessingDateMilestoning");
        validateTableAliasColumn(outPropertyMapping._relationalOperationElement(), "out_z");

        RelationalPropertyMapping fromPropertyMapping = relationalPropertyMappings.get(2);
        validateProperty(fromPropertyMapping._property(), "from", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(fromPropertyMapping._relationalOperationElement(), "from_z");

        RelationalPropertyMapping thruPropertyMapping = relationalPropertyMappings.get(3);
        validateProperty(thruPropertyMapping._property(), "thru", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(thruPropertyMapping._relationalOperationElement(), "thru_z");
    }

    @Test
    public void testProcessingMilestoningPropertyMappingWithNonMilestonedTable()
    {
        runtime.createInMemorySource(MODEL_ID, PROCESSING_MILESTONING_MODEL_CODE);
        runtime.createInMemorySource(STORE_ID, NON_MILESTONED_STORE_CODE);
        runtime.createInMemorySource(MAPPING_ID, MAPPING_CODE);
        runtime.compile();

        Mapping mapping = (Mapping) runtime.getCoreInstance("milestoning::Amap");
        RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation = (RootRelationalInstanceSetImplementation) mapping._classMappings().detect(RootRelationalInstanceSetImplementation.class::isInstance);

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) rootRelationalInstanceSetImplementation._propertyMappings().detect(TestMilestoningPropertyMapping::isMilestoningPropertyMapping);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = milestoningEmbeddedRelationalInstance._propertyMappings().collect(RelationalPropertyMapping.class::cast, Lists.mutable.empty());
        Assert.assertEquals(2, relationalPropertyMappings.size());

        RelationalPropertyMapping inPropertyMapping = relationalPropertyMappings.get(0);
        validateProperty(inPropertyMapping._property(), "in", "meta::pure::milestoning::ProcessingDateMilestoning");
        validateLiteral(inPropertyMapping._relationalOperationElement());

        RelationalPropertyMapping outPropertyMapping = relationalPropertyMappings.get(1);
        validateProperty(outPropertyMapping._property(), "out", "meta::pure::milestoning::ProcessingDateMilestoning");
        validateLiteral(outPropertyMapping._relationalOperationElement());
    }

    @Test
    public void testBusinessMilestoningPropertyMappingWithNonMilestonedTable()
    {
        runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        runtime.createInMemorySource(STORE_ID, NON_MILESTONED_STORE_CODE);
        runtime.createInMemorySource(MAPPING_ID, MAPPING_CODE);
        runtime.compile();

        Mapping mapping = (Mapping) runtime.getCoreInstance("milestoning::Amap");
        RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation = (RootRelationalInstanceSetImplementation) mapping._classMappings().detect(RootRelationalInstanceSetImplementation.class::isInstance);

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) rootRelationalInstanceSetImplementation._propertyMappings().detect(TestMilestoningPropertyMapping::isMilestoningPropertyMapping);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = milestoningEmbeddedRelationalInstance._propertyMappings().collect(RelationalPropertyMapping.class::cast, Lists.mutable.empty());
        Assert.assertEquals(2, relationalPropertyMappings.size());

        RelationalPropertyMapping fromPropertyMapping = relationalPropertyMappings.get(0);
        validateProperty(fromPropertyMapping._property(), "from", "meta::pure::milestoning::BusinessDateMilestoning");
        validateLiteral(fromPropertyMapping._relationalOperationElement());

        RelationalPropertyMapping thruPropertyMapping = relationalPropertyMappings.get(1);
        validateProperty(thruPropertyMapping._property(), "thru", "meta::pure::milestoning::BusinessDateMilestoning");
        validateLiteral(thruPropertyMapping._relationalOperationElement());
    }

    @Test
    public void testBiTemporalMilestoningPropertyMappingWithNonMilestonedTable()
    {
        runtime.createInMemorySource(MODEL_ID, BI_TEMPORAL_MILESTONING_MODEL_CODE);
        runtime.createInMemorySource(STORE_ID, NON_MILESTONED_STORE_CODE);
        runtime.createInMemorySource(MAPPING_ID, MAPPING_CODE);
        runtime.compile();

        Mapping mapping = (Mapping) runtime.getCoreInstance("milestoning::Amap");
        RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation = (RootRelationalInstanceSetImplementation) mapping._classMappings().detect(RootRelationalInstanceSetImplementation.class::isInstance);

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) rootRelationalInstanceSetImplementation._propertyMappings().detect(TestMilestoningPropertyMapping::isMilestoningPropertyMapping);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = milestoningEmbeddedRelationalInstance._propertyMappings().collect(RelationalPropertyMapping.class::cast, Lists.mutable.empty());
        Assert.assertEquals(4, relationalPropertyMappings.size());

        RelationalPropertyMapping inPropertyMapping = relationalPropertyMappings.get(0);
        validateProperty(inPropertyMapping._property(), "in", "meta::pure::milestoning::ProcessingDateMilestoning");
        validateLiteral(inPropertyMapping._relationalOperationElement());

        RelationalPropertyMapping outPropertyMapping = relationalPropertyMappings.get(1);
        validateProperty(outPropertyMapping._property(), "out", "meta::pure::milestoning::ProcessingDateMilestoning");
        validateLiteral(outPropertyMapping._relationalOperationElement());

        RelationalPropertyMapping fromPropertyMapping = relationalPropertyMappings.get(2);
        validateProperty(fromPropertyMapping._property(), "from", "meta::pure::milestoning::BusinessDateMilestoning");
        validateLiteral(fromPropertyMapping._relationalOperationElement());

        RelationalPropertyMapping thruPropertyMapping = relationalPropertyMappings.get(3);
        validateProperty(thruPropertyMapping._property(), "thru", "meta::pure::milestoning::BusinessDateMilestoning");
        validateLiteral(thruPropertyMapping._relationalOperationElement());
    }

    @Test
    public void testBusinessMilestoningPropertyMappingWithMappingExtends()
    {
        runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        runtime.createInMemorySource(EXTENDED_MODEL_ID, BUSINESS_MILESTONING_EXTENDED_MODEL_CODE);
        runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        runtime.createInMemorySource(MAPPING_ID, MAPPING_CODE);
        runtime.createInMemorySource(EXTENDED_MAPPING_ID, EXTENDED_MAPPING_CODE);
        runtime.compile();

        Mapping mapping = (Mapping) runtime.getCoreInstance("milestoning::Amap");
        RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation = (RootRelationalInstanceSetImplementation) mapping._classMappings().detect(RootRelationalInstanceSetImplementation.class::isInstance);

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) rootRelationalInstanceSetImplementation._propertyMappings().detect(TestMilestoningPropertyMapping::isMilestoningPropertyMapping);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = milestoningEmbeddedRelationalInstance._propertyMappings().collect(RelationalPropertyMapping.class::cast, Lists.mutable.empty());
        Assert.assertEquals(2, relationalPropertyMappings.size());

        RelationalPropertyMapping fromPropertyMapping = relationalPropertyMappings.get(0);
        validateProperty(fromPropertyMapping._property(), "from", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(fromPropertyMapping._relationalOperationElement(), "from_z");

        RelationalPropertyMapping thruPropertyMapping = relationalPropertyMappings.get(1);
        validateProperty(thruPropertyMapping._property(), "thru", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(thruPropertyMapping._relationalOperationElement(), "thru_z");

        Mapping subMapping = (Mapping) runtime.getCoreInstance("milestoning::Bmap");
        RootRelationalInstanceSetImplementation subRootRelationalInstanceSetImplementation = (RootRelationalInstanceSetImplementation) subMapping._classMappings().detect(RootRelationalInstanceSetImplementation.class::isInstance);

        EmbeddedRelationalInstanceSetImplementation subMilestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) subRootRelationalInstanceSetImplementation._propertyMappings().detect(TestMilestoningPropertyMapping::isMilestoningPropertyMapping);
        Assert.assertNull(subMilestoningEmbeddedRelationalInstance);
    }

    @Test
    public void testBusinessMilestoningPropertyMappingForEmbedded()
    {
        runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        runtime.createInMemorySource(EMBEDDED_MODEL_ID, EMBEDDED_MODEL_CODE);
        runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        runtime.createInMemorySource(MAPPING_ID, EMBEDDED_MAPPING_CODE);
        runtime.compile();

        Mapping mapping = (Mapping) runtime.getCoreInstance("milestoning::Bmap");
        RootRelationalInstanceSetImplementation rootRelationalInstance = (RootRelationalInstanceSetImplementation) mapping._classMappings().detect(RootRelationalInstanceSetImplementation.class::isInstance);
        EmbeddedRelationalInstanceSetImplementation embeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) rootRelationalInstance._propertyMappings().detect(EmbeddedRelationalInstanceSetImplementation.class::isInstance);

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) embeddedRelationalInstance._propertyMappings().detect(TestMilestoningPropertyMapping::isMilestoningPropertyMapping);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = milestoningEmbeddedRelationalInstance._propertyMappings().collect(RelationalPropertyMapping.class::cast, Lists.mutable.empty());
        Assert.assertEquals(2, relationalPropertyMappings.size());

        RelationalPropertyMapping fromPropertyMapping = relationalPropertyMappings.get(0);
        validateProperty(fromPropertyMapping._property(), "from", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(fromPropertyMapping._relationalOperationElement(), "from_z");

        RelationalPropertyMapping thruPropertyMapping = relationalPropertyMappings.get(1);
        validateProperty(thruPropertyMapping._property(), "thru", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(thruPropertyMapping._relationalOperationElement(), "thru_z");
    }

    @Test
    public void testBusinessMilestoningPropertyMappingForTemporalEmbedded()
    {
        runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        runtime.createInMemorySource(EMBEDDED_MODEL_ID, TEMPORAL_EMBEDDED_MODEL_CODE);
        runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        runtime.createInMemorySource(MAPPING_ID, EMBEDDED_MAPPING_CODE);
        runtime.compile();

        Mapping mapping = (Mapping) runtime.getCoreInstance("milestoning::Bmap");
        RootRelationalInstanceSetImplementation rootRelationalInstance = (RootRelationalInstanceSetImplementation) mapping._classMappings().detect(RootRelationalInstanceSetImplementation.class::isInstance);

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) rootRelationalInstance._propertyMappings().detect(TestMilestoningPropertyMapping::isMilestoningPropertyMapping);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::B");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = milestoningEmbeddedRelationalInstance._propertyMappings().collect(RelationalPropertyMapping.class::cast, Lists.mutable.empty());
        Assert.assertEquals(2, relationalPropertyMappings.size());

        RelationalPropertyMapping fromPropertyMapping = relationalPropertyMappings.get(0);
        validateProperty(fromPropertyMapping._property(), "from", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(fromPropertyMapping._relationalOperationElement(), "from_z");

        RelationalPropertyMapping thruPropertyMapping = relationalPropertyMappings.get(1);
        validateProperty(thruPropertyMapping._property(), "thru", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(thruPropertyMapping._relationalOperationElement(), "thru_z");

        EmbeddedRelationalInstanceSetImplementation embeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) rootRelationalInstance._propertyMappings().detect(EmbeddedRelationalInstanceSetImplementation.class::isInstance);

        milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) embeddedRelationalInstance._propertyMappings().detect(TestMilestoningPropertyMapping::isMilestoningPropertyMapping);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        relationalPropertyMappings = milestoningEmbeddedRelationalInstance._propertyMappings().collect(RelationalPropertyMapping.class::cast, Lists.mutable.empty());
        Assert.assertEquals(2, relationalPropertyMappings.size());

        fromPropertyMapping = relationalPropertyMappings.get(0);
        validateProperty(fromPropertyMapping._property(), "from", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(fromPropertyMapping._relationalOperationElement(), "from_z");

        thruPropertyMapping = relationalPropertyMappings.get(1);
        validateProperty(thruPropertyMapping._property(), "thru", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(thruPropertyMapping._relationalOperationElement(), "thru_z");
    }

    @Test
    public void testBusinessMilestoningPropertyMappingForInlineEmbedded()
    {
        runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        runtime.createInMemorySource(EMBEDDED_MODEL_ID, EMBEDDED_MODEL_CODE);
        runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        runtime.createInMemorySource(MAPPING_ID, INLINE_EMBEDDED_MAPPING_CODE);
        runtime.compile();

        Mapping mapping = (Mapping) runtime.getCoreInstance("milestoning::Bmap");
        RootRelationalInstanceSetImplementation rootRelationalInstance = (RootRelationalInstanceSetImplementation) mapping._classMappings().detect(RootRelationalInstanceSetImplementation.class::isInstance);
        EmbeddedRelationalInstanceSetImplementation embeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) rootRelationalInstance._propertyMappings().detect(EmbeddedRelationalInstanceSetImplementation.class::isInstance);

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) embeddedRelationalInstance._propertyMappings().detect(TestMilestoningPropertyMapping::isMilestoningPropertyMapping);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = milestoningEmbeddedRelationalInstance._propertyMappings().collect(RelationalPropertyMapping.class::cast, Lists.mutable.empty());
        Assert.assertEquals(2, relationalPropertyMappings.size());

        RelationalPropertyMapping fromPropertyMapping = relationalPropertyMappings.get(0);
        validateProperty(fromPropertyMapping._property(), "from", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(fromPropertyMapping._relationalOperationElement(), "from_z");

        RelationalPropertyMapping thruPropertyMapping = relationalPropertyMappings.get(1);
        validateProperty(thruPropertyMapping._property(), "thru", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(thruPropertyMapping._relationalOperationElement(), "thru_z");
    }

    @Test
    public void testBusinessMilestoningPropertyMappingForTemporalInlineEmbedded()
    {
        runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        runtime.createInMemorySource(EMBEDDED_MODEL_ID, TEMPORAL_EMBEDDED_MODEL_CODE);
        runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        runtime.createInMemorySource(MAPPING_ID, INLINE_EMBEDDED_MAPPING_CODE);
        runtime.compile();

        Mapping mapping = (Mapping) runtime.getCoreInstance("milestoning::Bmap");
        RootRelationalInstanceSetImplementation rootRelationalInstance = (RootRelationalInstanceSetImplementation) mapping._classMappings().detect(RootRelationalInstanceSetImplementation.class::isInstance);

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) rootRelationalInstance._propertyMappings().detect(TestMilestoningPropertyMapping::isMilestoningPropertyMapping);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::B");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = milestoningEmbeddedRelationalInstance._propertyMappings().collect(RelationalPropertyMapping.class::cast, Lists.mutable.empty());
        Assert.assertEquals(2, relationalPropertyMappings.size());

        RelationalPropertyMapping fromPropertyMapping = relationalPropertyMappings.get(0);
        validateProperty(fromPropertyMapping._property(), "from", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(fromPropertyMapping._relationalOperationElement(), "from_z");

        RelationalPropertyMapping thruPropertyMapping = relationalPropertyMappings.get(1);
        validateProperty(thruPropertyMapping._property(), "thru", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(thruPropertyMapping._relationalOperationElement(), "thru_z");

        EmbeddedRelationalInstanceSetImplementation embeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) rootRelationalInstance._propertyMappings().detect(EmbeddedRelationalInstanceSetImplementation.class::isInstance);

        milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation) embeddedRelationalInstance._propertyMappings().detect(TestMilestoningPropertyMapping::isMilestoningPropertyMapping);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        relationalPropertyMappings = milestoningEmbeddedRelationalInstance._propertyMappings().collect(RelationalPropertyMapping.class::cast, Lists.mutable.empty());
        Assert.assertEquals(2, relationalPropertyMappings.size());

        fromPropertyMapping = relationalPropertyMappings.get(0);
        validateProperty(fromPropertyMapping._property(), "from", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(fromPropertyMapping._relationalOperationElement(), "from_z");

        thruPropertyMapping = relationalPropertyMappings.get(1);
        validateProperty(thruPropertyMapping._property(), "thru", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(thruPropertyMapping._relationalOperationElement(), "thru_z");
    }

    @Test
    public void testMilestoningPropertyMappingValidation()
    {
        runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        runtime.createInMemorySource(MAPPING_ID, MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED);
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "Class : [milestoning::A] has temporal specification. Hence mapping of property : [milestoning] is reserved and should not be explicit in the mapping", MAPPING_ID, 9, 7, e);
    }

    @Test
    public void testMilestoningPropertyMappingValidationOnExtendedMapping()
    {
        runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        runtime.createInMemorySource(EXTENDED_MODEL_ID, BUSINESS_MILESTONING_EXTENDED_MODEL_CODE);
        runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        runtime.createInMemorySource(MAPPING_ID, MAPPING_CODE);
        runtime.createInMemorySource(EXTENDED_MAPPING_ID, EXTENDED_MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED);
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "Class : [milestoning::B] has temporal specification. Hence mapping of property : [milestoning] is reserved and should not be explicit in the mapping", EXTENDED_MAPPING_ID, 10, 7, e);
    }

    @Test
    public void testMilestoningPropertyMappingValidationOnEmbeddedMapping()
    {
        runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        runtime.createInMemorySource(EMBEDDED_MODEL_ID, EMBEDDED_MODEL_CODE);
        runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        runtime.createInMemorySource(MAPPING_ID, EMBEDDED_MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED);
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "Class : [milestoning::A] has temporal specification. Hence mapping of property : [milestoning] is reserved and should not be explicit in the mapping", MAPPING_ID, 11, 10, e);
    }

    @Test
    public void testMilestoningPropertyMappingValidationOnInlineEmbeddedMapping()
    {
        runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        runtime.createInMemorySource(EMBEDDED_MODEL_ID, EMBEDDED_MODEL_CODE);
        runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        runtime.createInMemorySource(MAPPING_ID, INLINE_EMBEDDED_MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED);
        PureCompilationException e = Assert.assertThrows(PureCompilationException.class, runtime::compile);
        assertPureException(PureCompilationException.class, "Class : [milestoning::A] has temporal specification. Hence mapping of property : [milestoning] is reserved and should not be explicit in the mapping", MAPPING_ID, 15, 7, e);
    }

    private static void validateProperty(Property<?, ?> property, String propertyName, String owner)
    {
        Assert.assertNotNull(property);
        Assert.assertEquals(propertyName, property._name());
        Assert.assertEquals(owner, PackageableElement.getUserPathForPackageableElement(property._owner()));
    }

    private static void validateTableAliasColumn(RelationalOperationElement relationalOperationElement, String columnName)
    {
        Assert.assertTrue(relationalOperationElement instanceof TableAliasColumn);
        Column column = ((TableAliasColumn) relationalOperationElement)._column();
        Assert.assertNotNull(column);
        Assert.assertEquals(columnName, column._name());
        Assert.assertEquals("myTable", ((Table) column._owner())._name());
        Assert.assertEquals("default", ((Table) column._owner())._schema()._name());
        Assert.assertEquals("myDB", ((Table) column._owner())._schema()._database()._name());
    }

    private static void validateLiteral(RelationalOperationElement relationalOperationElement)
    {
        Assert.assertTrue(relationalOperationElement instanceof Literal);
        Assert.assertTrue(((Literal) relationalOperationElement)._value() instanceof SQLNull);
    }

    private static boolean isMilestoningPropertyMapping(PropertyMapping propertyMapping)
    {
        return MilestoningFunctions.isAutoGeneratedMilestoningNamedDateProperty(propertyMapping._property(), processorSupport);
    }
}
