// Copyright 2024 Goldman Sachs
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

package org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.processor;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingPaths;
import org.finos.legend.pure.m2.dsl.mapping.M2MappingProperties;
import org.finos.legend.pure.m2.dsl.mapping.serialization.grammar.v1.validator.RelationFunctionInstanceSetImplementationValidator;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.PostProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.ProcessorState;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.Processor;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.PropertyMappingsImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.EmbeddedRelationFunctionSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionInstanceSetImplementation;
import org.finos.legend.pure.m3.coreinstance.meta.pure.mapping.relation.RelationFunctionPropertyMapping;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.Column;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.tools.GrammarInfoStub;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;


public class RelationFunctionInstanceSetImplementationProcessor extends Processor<RelationFunctionInstanceSetImplementation>
{
    @Override
    public void process(RelationFunctionInstanceSetImplementation instance, ProcessorState state, Matcher matcher, ModelRepository repository, Context context, ProcessorSupport processorSupport)
    {
        // The relationFunction slot may hold either an ImportStub (for `~func`)
        // or an anonymous LambdaFunction (for `~src`).  withImportStubByPass
        // returns the underlying instance for both shapes.
        CoreInstance relationFunction = ImportStub.withImportStubByPass(instance._relationFunctionCoreInstance(), processorSupport);
        PostProcessor.processElement(matcher, relationFunction, state, processorSupport);
        RelationFunctionInstanceSetImplementationValidator.validateRelationFunction(relationFunction, processorSupport);

        // The relation function's last expression has generic type Relation<T>
        // where T is the structural row type (a RelationType<...>).  We bind
        // the valueFn `row` parameter to T so `$row.<col>` resolves against
        // the columns of the row, mirroring how `extend`/`filter` lambdas
        // type their row parameter.
        GenericType lastExprType = (GenericType) relationFunction
                .getValueForMetaPropertyToMany(M3Properties.expressionSequence)
                .getLast()
                .getValueForMetaPropertyToOne(M3Properties.genericType);
        org.eclipse.collections.api.list.ListIterable<? extends GenericType> typeArgs = lastExprType._typeArguments().toList();
        if (typeArgs.isEmpty())
        {
            throw new org.finos.legend.pure.m4.exception.PureCompilationException(instance.getSourceInformation(), "Relation function's last expression has no Relation type argument; cannot bind property mapping `$row` parameter.");
        }
        GenericType rowType = typeArgs.getFirst();
        resolvePrimaryKey(instance, relationFunction, rowType, processorSupport);

        processPropertyMappings(instance._propertyMappings(), instance, rowType, matcher, state, processorSupport);
    }

    private static void resolvePrimaryKey(RelationFunctionInstanceSetImplementation instance, CoreInstance relationFunction, GenericType rowType, ProcessorSupport processorSupport)
    {
        if (instance._primaryKey().isEmpty())
        {
            return;
        }
        CoreInstance rawType = ImportStub.withImportStubByPass(rowType._rawTypeCoreInstance(), processorSupport);
        if (!(rawType instanceof RelationType))
        {
            String functionName = relationFunction instanceof ConcreteFunctionDefinition ? ((ConcreteFunctionDefinition<?>) relationFunction)._functionName() : "~src";
            throw new PureCompilationException(instance.getSourceInformation(), "Cannot resolve primary key columns: relation function '" + functionName + "' does not return a concrete RelationType. Ensure the function body produces a typed relation (e.g., #>{db.table}#).");
        }
        RichIterable<? extends Column<?, ?>> relationColumns = ((RelationType<?>) rawType)._columns();
        MutableList<Column<?, ?>> resolved = Lists.mutable.empty();
        for (Column<?, ?> placeholder : instance._primaryKey())
        {
            Column<?, ?> column = relationColumns.detect(c -> placeholder._name().equals(c._name()));
            if (column == null)
            {
                String mappingPath = PackageableElement.getUserPathForPackageableElement(ImportStub.withImportStubByPass(instance._parentCoreInstance(), processorSupport));
                throw new PureCompilationException(placeholder.getSourceInformation(), "Primary key column '" + placeholder._name() + "' declared in class mapping '" + instance._id() + "' (mapping '" + mappingPath + "') is not part of the columns returned by the relation function."
                        + " Available columns: [" + relationColumns.collect(Column::_name).makeString(", ") + "]."
                        + " Use `~primaryKey: [col1, col2, ...]` referencing only columns from the relation function's output.");
            }
            GenericType columnType = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericType(_Column.getColumnType(column), processorSupport);
            Column<?, ?> primaryKeyColumn = _Column.getColumnInstance(column._name(), false, columnType, _Column.getColumnMultiplicity(column), placeholder.getSourceInformation(), processorSupport);
            GenericType anySource = (GenericType) processorSupport.newAnonymousCoreInstance(placeholder.getSourceInformation(), M3Paths.GenericType);
            anySource._rawTypeCoreInstance(processorSupport.package_getByUserPath(M3Paths.Any));
            resolved.add(_Column.updateSource(primaryKeyColumn, anySource));
        }
        instance._primaryKey(resolved);
    }

    private void processPropertyMappings(Iterable<? extends PropertyMapping> propertyMappings, RelationFunctionInstanceSetImplementation owner, GenericType rowType, Matcher matcher, ProcessorState state, ProcessorSupport processorSupport)
    {
        for (PropertyMapping propertyMapping : propertyMappings)
        {
            if (propertyMapping instanceof RelationFunctionPropertyMapping)
            {
                processRelationFunctionPropertyMapping((RelationFunctionPropertyMapping) propertyMapping, rowType, matcher, state, processorSupport);
            }
            else if (propertyMapping instanceof EmbeddedRelationFunctionSetImplementation)
            {
                EmbeddedRelationFunctionSetImplementation embeddedSet = (EmbeddedRelationFunctionSetImplementation) propertyMapping;
                Property<?, ?> property = (Property<?, ?>) ImportStub.withImportStubByPass(embeddedSet._propertyCoreInstance(), processorSupport);
                CoreInstance targetClass = property._classifierGenericType() == null ||
                        property._classifierGenericType()._typeArguments() == null ||
                        property._classifierGenericType()._typeArguments().size() < 2 ||
                        property._classifierGenericType()._typeArguments().toList().get(1) == null ? null : ImportStub.withImportStubByPass(property._classifierGenericType()._typeArguments().toList().get(1)._rawTypeCoreInstance(), processorSupport);
                embeddedSet._classCoreInstance(targetClass);
                embeddedSet._relationFunctionCoreInstance(owner._relationFunctionCoreInstance());

                processPropertyMappings(embeddedSet._propertyMappings(), embeddedSet, rowType, matcher, state, processorSupport);
            }
        }
    }

    private void processRelationFunctionPropertyMapping(RelationFunctionPropertyMapping pm, GenericType rowType, Matcher matcher, ProcessorState state, ProcessorSupport processorSupport)
    {
        if (pm._transformerCoreInstance() != null)
        {
            GrammarInfoStub transformerStub = (GrammarInfoStub) pm._transformerCoreInstance();
            EnumerationMappingProcessor.processsEnumerationTransformer(transformerStub, pm, processorSupport);
        }

        LambdaFunction<?> valueFn = pm._valueFn();
        if (valueFn == null)
        {
            return;
        }

        try (ProcessorState.VariableContextScope ignore = state.withNewVariableContext())
        {
            // Inject the `row` parameter typed at the relation function's
            // last-expression row type (the first type-arg of Relation<T>) so
            // `$row.<col>` resolves against the columns of the row.
            FunctionType ft = getFunctionType(valueFn, processorSupport);
            ft._parametersAdd(createRowParameter(rowType, ft.getSourceInformation(), pm.getSourceInformation(), processorSupport));

            matcher.fullMatch(valueFn, state);
        }
    }

    private VariableExpression createRowParameter(GenericType rowType, SourceInformation genericTypeSourceInfo, SourceInformation parameterSourceInformation, ProcessorSupport processorSupport)
    {
        GenericType copy = (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericType(rowType, processorSupport);
        VariableExpression rowParam = (VariableExpression) processorSupport.newAnonymousCoreInstance(parameterSourceInformation, M3Paths.VariableExpression);
        return rowParam._name("row")
                ._genericType(copy)
                ._multiplicity((Multiplicity) processorSupport.package_getByUserPath(M3Paths.PureOne));
    }

    private static FunctionType getFunctionType(LambdaFunction<?> lambdaFunction, ProcessorSupport processorSupport)
    {
        GenericType typeArgument = lambdaFunction._classifierGenericType()._typeArguments().getAny();
        return (FunctionType) ImportStub.withImportStubByPass(typeArgument._rawTypeCoreInstance(), processorSupport);
    }

    @Override
    public void populateReferenceUsages(RelationFunctionInstanceSetImplementation instance, ModelRepository repository, ProcessorSupport processorSupport)
    {
        // Only emit the reference-usage when relationFunction is an ImportStub
        // (i.e. `~func` form).  For `~src`, the LambdaFunction is owned by this
        // mapping and not referenced from elsewhere.
        CoreInstance rfRaw = instance._relationFunctionCoreInstance();
        if (rfRaw != null && processorSupport.instance_instanceOf(rfRaw, M3Paths.ImportStub))
        {
            addReferenceUsageForToOneProperty(instance, rfRaw, M2MappingProperties.relationFunction, repository, processorSupport);
        }
    }

    @Override
    public String getClassName()
    {
        return M2MappingPaths.RelationFunctionInstanceSetImplementation;
    }
}

