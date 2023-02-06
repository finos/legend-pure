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

import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningFunctions;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
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
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
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
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.EXTENDED_MAPPING_ID;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.EXTENDED_MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.EXTENDED_MODEL_ID;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.INLINE_EMBEDDED_MAPPING_CODE;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.INLINE_EMBEDDED_MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.MAPPING_CODE;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.MAPPING_ID;
import static org.finos.legend.pure.m2.relational.MilestoningPropertyMappingTestSourceCodes.MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED;
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
        this.runtime.createInMemorySource(MODEL_ID, PROCESSING_MILESTONING_MODEL_CODE);
        this.runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        this.runtime.createInMemorySource(MAPPING_ID, MAPPING_CODE);
        this.runtime.compile();

        Mapping mapping = (Mapping)this.runtime.getCoreInstance("milestoning::Amap");
        RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation = mapping._classMappings().selectInstancesOf(RootRelationalInstanceSetImplementation.class).getFirst();

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation)rootRelationalInstanceSetImplementation._propertyMappings().detectWith(MILESTONING_PROPERTY_MAPPING, this.processorSupport);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = (MutableList<RelationalPropertyMapping>)milestoningEmbeddedRelationalInstance._propertyMappings().toList();
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
        this.runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        this.runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        this.runtime.createInMemorySource(MAPPING_ID, MAPPING_CODE);
        this.runtime.compile();

        Mapping mapping = (Mapping)this.runtime.getCoreInstance("milestoning::Amap");
        RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation = mapping._classMappings().selectInstancesOf(RootRelationalInstanceSetImplementation.class).getFirst();

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation)rootRelationalInstanceSetImplementation._propertyMappings().detectWith(MILESTONING_PROPERTY_MAPPING, this.processorSupport);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = (MutableList<RelationalPropertyMapping>)milestoningEmbeddedRelationalInstance._propertyMappings().toList();
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
        this.runtime.createInMemorySource(MODEL_ID, BI_TEMPORAL_MILESTONING_MODEL_CODE);
        this.runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        this.runtime.createInMemorySource(MAPPING_ID, MAPPING_CODE);
        this.runtime.compile();

        Mapping mapping = (Mapping)this.runtime.getCoreInstance("milestoning::Amap");
        RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation = mapping._classMappings().selectInstancesOf(RootRelationalInstanceSetImplementation.class).getFirst();

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation)rootRelationalInstanceSetImplementation._propertyMappings().detectWith(MILESTONING_PROPERTY_MAPPING, this.processorSupport);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = (MutableList<RelationalPropertyMapping>)milestoningEmbeddedRelationalInstance._propertyMappings().toList();
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
        this.runtime.createInMemorySource(MODEL_ID, PROCESSING_MILESTONING_MODEL_CODE);
        this.runtime.createInMemorySource(STORE_ID, NON_MILESTONED_STORE_CODE);
        this.runtime.createInMemorySource(MAPPING_ID, MAPPING_CODE);
        this.runtime.compile();

        Mapping mapping = (Mapping)this.runtime.getCoreInstance("milestoning::Amap");
        RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation = mapping._classMappings().selectInstancesOf(RootRelationalInstanceSetImplementation.class).getFirst();

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation)rootRelationalInstanceSetImplementation._propertyMappings().detectWith(MILESTONING_PROPERTY_MAPPING, this.processorSupport);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = (MutableList<RelationalPropertyMapping>)milestoningEmbeddedRelationalInstance._propertyMappings().toList();
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
        this.runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        this.runtime.createInMemorySource(STORE_ID, NON_MILESTONED_STORE_CODE);
        this.runtime.createInMemorySource(MAPPING_ID, MAPPING_CODE);
        this.runtime.compile();

        Mapping mapping = (Mapping)this.runtime.getCoreInstance("milestoning::Amap");
        RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation = mapping._classMappings().selectInstancesOf(RootRelationalInstanceSetImplementation.class).getFirst();

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation)rootRelationalInstanceSetImplementation._propertyMappings().detectWith(MILESTONING_PROPERTY_MAPPING, this.processorSupport);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = (MutableList<RelationalPropertyMapping>)milestoningEmbeddedRelationalInstance._propertyMappings().toList();
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
        this.runtime.createInMemorySource(MODEL_ID, BI_TEMPORAL_MILESTONING_MODEL_CODE);
        this.runtime.createInMemorySource(STORE_ID, NON_MILESTONED_STORE_CODE);
        this.runtime.createInMemorySource(MAPPING_ID, MAPPING_CODE);
        this.runtime.compile();

        Mapping mapping = (Mapping)this.runtime.getCoreInstance("milestoning::Amap");
        RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation = mapping._classMappings().selectInstancesOf(RootRelationalInstanceSetImplementation.class).getFirst();

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation)rootRelationalInstanceSetImplementation._propertyMappings().detectWith(MILESTONING_PROPERTY_MAPPING, this.processorSupport);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = (MutableList<RelationalPropertyMapping>)milestoningEmbeddedRelationalInstance._propertyMappings().toList();
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
        this.runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        this.runtime.createInMemorySource(EXTENDED_MODEL_ID, BUSINESS_MILESTONING_EXTENDED_MODEL_CODE);
        this.runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        this.runtime.createInMemorySource(MAPPING_ID, MAPPING_CODE);
        this.runtime.createInMemorySource(EXTENDED_MAPPING_ID, EXTENDED_MAPPING_CODE);
        this.runtime.compile();

        Mapping mapping = (Mapping)this.runtime.getCoreInstance("milestoning::Amap");
        RootRelationalInstanceSetImplementation rootRelationalInstanceSetImplementation = mapping._classMappings().selectInstancesOf(RootRelationalInstanceSetImplementation.class).getFirst();

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation)rootRelationalInstanceSetImplementation._propertyMappings().detectWith(MILESTONING_PROPERTY_MAPPING, this.processorSupport);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = (MutableList<RelationalPropertyMapping>)milestoningEmbeddedRelationalInstance._propertyMappings().toList();
        Assert.assertEquals(2, relationalPropertyMappings.size());

        RelationalPropertyMapping fromPropertyMapping = relationalPropertyMappings.get(0);
        validateProperty(fromPropertyMapping._property(), "from", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(fromPropertyMapping._relationalOperationElement(), "from_z");

        RelationalPropertyMapping thruPropertyMapping = relationalPropertyMappings.get(1);
        validateProperty(thruPropertyMapping._property(), "thru", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(thruPropertyMapping._relationalOperationElement(), "thru_z");

        Mapping subMapping = (Mapping)this.runtime.getCoreInstance("milestoning::Bmap");
        RootRelationalInstanceSetImplementation subRootRelationalInstanceSetImplementation = subMapping._classMappings().selectInstancesOf(RootRelationalInstanceSetImplementation.class).getFirst();

        EmbeddedRelationalInstanceSetImplementation subMilestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation)subRootRelationalInstanceSetImplementation._propertyMappings().detectWith(MILESTONING_PROPERTY_MAPPING, this.processorSupport);
        Assert.assertNull(subMilestoningEmbeddedRelationalInstance);
    }

    @Test
    public void testBusinessMilestoningPropertyMappingForEmbedded()
    {
        this.runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        this.runtime.createInMemorySource(EMBEDDED_MODEL_ID, EMBEDDED_MODEL_CODE);
        this.runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        this.runtime.createInMemorySource(MAPPING_ID, EMBEDDED_MAPPING_CODE);
        this.runtime.compile();

        Mapping mapping = (Mapping)this.runtime.getCoreInstance("milestoning::Bmap");
        RootRelationalInstanceSetImplementation rootRelationalInstance = mapping._classMappings().selectInstancesOf(RootRelationalInstanceSetImplementation.class).getFirst();
        EmbeddedRelationalInstanceSetImplementation embeddedRelationalInstance = rootRelationalInstance._propertyMappings().selectInstancesOf(EmbeddedRelationalInstanceSetImplementation.class).getFirst();

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation)embeddedRelationalInstance._propertyMappings().detectWith(MILESTONING_PROPERTY_MAPPING, this.processorSupport);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = (MutableList<RelationalPropertyMapping>)milestoningEmbeddedRelationalInstance._propertyMappings().toList();
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
        this.runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        this.runtime.createInMemorySource(EMBEDDED_MODEL_ID, TEMPORAL_EMBEDDED_MODEL_CODE);
        this.runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        this.runtime.createInMemorySource(MAPPING_ID, EMBEDDED_MAPPING_CODE);
        this.runtime.compile();

        Mapping mapping = (Mapping)this.runtime.getCoreInstance("milestoning::Bmap");
        RootRelationalInstanceSetImplementation rootRelationalInstance = mapping._classMappings().selectInstancesOf(RootRelationalInstanceSetImplementation.class).getFirst();

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation)rootRelationalInstance._propertyMappings().detectWith(MILESTONING_PROPERTY_MAPPING, this.processorSupport);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::B");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = (MutableList<RelationalPropertyMapping>)milestoningEmbeddedRelationalInstance._propertyMappings().toList();
        Assert.assertEquals(2, relationalPropertyMappings.size());

        RelationalPropertyMapping fromPropertyMapping = relationalPropertyMappings.get(0);
        validateProperty(fromPropertyMapping._property(), "from", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(fromPropertyMapping._relationalOperationElement(), "from_z");

        RelationalPropertyMapping thruPropertyMapping = relationalPropertyMappings.get(1);
        validateProperty(thruPropertyMapping._property(), "thru", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(thruPropertyMapping._relationalOperationElement(), "thru_z");

        EmbeddedRelationalInstanceSetImplementation embeddedRelationalInstance = rootRelationalInstance._propertyMappings().selectInstancesOf(EmbeddedRelationalInstanceSetImplementation.class).getFirst();

        milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation)embeddedRelationalInstance._propertyMappings().detectWith(MILESTONING_PROPERTY_MAPPING, this.processorSupport);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        relationalPropertyMappings = (MutableList<RelationalPropertyMapping>)milestoningEmbeddedRelationalInstance._propertyMappings().toList();
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
        this.runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        this.runtime.createInMemorySource(EMBEDDED_MODEL_ID, EMBEDDED_MODEL_CODE);
        this.runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        this.runtime.createInMemorySource(MAPPING_ID, INLINE_EMBEDDED_MAPPING_CODE);
        this.runtime.compile();

        Mapping mapping = (Mapping)this.runtime.getCoreInstance("milestoning::Bmap");
        RootRelationalInstanceSetImplementation rootRelationalInstance = mapping._classMappings().selectInstancesOf(RootRelationalInstanceSetImplementation.class).getFirst();
        EmbeddedRelationalInstanceSetImplementation embeddedRelationalInstance = rootRelationalInstance._propertyMappings().selectInstancesOf(EmbeddedRelationalInstanceSetImplementation.class).getFirst();

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation)embeddedRelationalInstance._propertyMappings().detectWith(MILESTONING_PROPERTY_MAPPING, this.processorSupport);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = (MutableList<RelationalPropertyMapping>)milestoningEmbeddedRelationalInstance._propertyMappings().toList();
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
        this.runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        this.runtime.createInMemorySource(EMBEDDED_MODEL_ID, TEMPORAL_EMBEDDED_MODEL_CODE);
        this.runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        this.runtime.createInMemorySource(MAPPING_ID, INLINE_EMBEDDED_MAPPING_CODE);
        this.runtime.compile();

        Mapping mapping = (Mapping)this.runtime.getCoreInstance("milestoning::Bmap");
        RootRelationalInstanceSetImplementation rootRelationalInstance = mapping._classMappings().selectInstancesOf(RootRelationalInstanceSetImplementation.class).getFirst();

        EmbeddedRelationalInstanceSetImplementation milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation)rootRelationalInstance._propertyMappings().detectWith(MILESTONING_PROPERTY_MAPPING, this.processorSupport);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::B");

        MutableList<RelationalPropertyMapping> relationalPropertyMappings = (MutableList<RelationalPropertyMapping>)milestoningEmbeddedRelationalInstance._propertyMappings().toList();
        Assert.assertEquals(2, relationalPropertyMappings.size());

        RelationalPropertyMapping fromPropertyMapping = relationalPropertyMappings.get(0);
        validateProperty(fromPropertyMapping._property(), "from", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(fromPropertyMapping._relationalOperationElement(), "from_z");

        RelationalPropertyMapping thruPropertyMapping = relationalPropertyMappings.get(1);
        validateProperty(thruPropertyMapping._property(), "thru", "meta::pure::milestoning::BusinessDateMilestoning");
        validateTableAliasColumn(thruPropertyMapping._relationalOperationElement(), "thru_z");

        EmbeddedRelationalInstanceSetImplementation embeddedRelationalInstance = rootRelationalInstance._propertyMappings().selectInstancesOf(EmbeddedRelationalInstanceSetImplementation.class).getFirst();

        milestoningEmbeddedRelationalInstance = (EmbeddedRelationalInstanceSetImplementation)embeddedRelationalInstance._propertyMappings().detectWith(MILESTONING_PROPERTY_MAPPING, this.processorSupport);
        Assert.assertNotNull(milestoningEmbeddedRelationalInstance);
        validateProperty(milestoningEmbeddedRelationalInstance._property(), "milestoning", "milestoning::A");

        relationalPropertyMappings = (MutableList<RelationalPropertyMapping>)milestoningEmbeddedRelationalInstance._propertyMappings().toList();
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
        this.runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        this.runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        this.runtime.createInMemorySource(MAPPING_ID, MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED);
        try
        {
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureCompilationException.class, "Class : [milestoning::A] has temporal specification. Hence mapping of property : [milestoning] is reserved and should not be explicit in the mapping", MAPPING_ID, 9, 7, e);
        }
    }

    @Test
    public void testMilestoningPropertyMappingValidationOnExtendedMapping()
    {
        this.runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        this.runtime.createInMemorySource(EXTENDED_MODEL_ID, BUSINESS_MILESTONING_EXTENDED_MODEL_CODE);
        this.runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        this.runtime.createInMemorySource(MAPPING_ID, MAPPING_CODE);
        this.runtime.createInMemorySource(EXTENDED_MAPPING_ID, EXTENDED_MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED);
        try
        {
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureCompilationException.class, "Class : [milestoning::B] has temporal specification. Hence mapping of property : [milestoning] is reserved and should not be explicit in the mapping", EXTENDED_MAPPING_ID, 10, 7, e);
        }
    }

    @Test
    public void testMilestoningPropertyMappingValidationOnEmbeddedMapping()
    {
        this.runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        this.runtime.createInMemorySource(EMBEDDED_MODEL_ID, EMBEDDED_MODEL_CODE);
        this.runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        this.runtime.createInMemorySource(MAPPING_ID, EMBEDDED_MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED);
        try
        {
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureCompilationException.class, "Class : [milestoning::A] has temporal specification. Hence mapping of property : [milestoning] is reserved and should not be explicit in the mapping", MAPPING_ID, 11, 10, e);
        }
    }

    @Test
    public void testMilestoningPropertyMappingValidationOnInlineEmbeddedMapping()
    {
        this.runtime.createInMemorySource(MODEL_ID, BUSINESS_MILESTONING_MODEL_CODE);
        this.runtime.createInMemorySource(EMBEDDED_MODEL_ID, EMBEDDED_MODEL_CODE);
        this.runtime.createInMemorySource(STORE_ID, MILESTONED_STORE_CODE);
        this.runtime.createInMemorySource(MAPPING_ID, INLINE_EMBEDDED_MAPPING_CODE_WITH_MILESTONING_PROPERTY_EXPLICITLY_MAPPED);
        try
        {
            this.runtime.compile();
            Assert.fail();
        }
        catch (Exception e)
        {
            this.assertOriginatingPureException(PureCompilationException.class, "Class : [milestoning::A] has temporal specification. Hence mapping of property : [milestoning] is reserved and should not be explicit in the mapping", MAPPING_ID, 15, 7, e);
        }
    }

    private static void validateProperty(Property property, String propertyName, String owner)
    {
        Assert.assertNotNull(property);
        Assert.assertEquals(propertyName, property._name());
        Assert.assertEquals(owner, PackageableElement.getUserPathForPackageableElement(property._owner()));
    }

    private static void validateTableAliasColumn(RelationalOperationElement relationalOperationElement, String columnName)
    {
        Assert.assertTrue(relationalOperationElement instanceof TableAliasColumn);
        Column column = ((TableAliasColumn)relationalOperationElement)._column();
        Assert.assertNotNull(column);
        Assert.assertEquals(columnName, column._name());
        Assert.assertEquals("myTable", ((Table)column._owner())._name());
        Assert.assertEquals("default", ((Table)column._owner())._schema()._name());
        Assert.assertEquals("myDB", ((Table)column._owner())._schema()._database()._name());
    }

    private static void validateLiteral(RelationalOperationElement relationalOperationElement)
    {
        Assert.assertTrue(relationalOperationElement instanceof Literal);
        Assert.assertTrue(((Literal)relationalOperationElement)._value() instanceof SQLNull);
    }

    private static final Predicate2<PropertyMapping, ProcessorSupport> MILESTONING_PROPERTY_MAPPING = new Predicate2<PropertyMapping, ProcessorSupport>()
    {
        @Override
        public boolean accept(PropertyMapping propertyMapping, ProcessorSupport processorSupport)
        {
            return MilestoningFunctions.isAutoGeneratedMilestoningNamedDateProperty(propertyMapping._property(), processorSupport);
        }
    };
}
