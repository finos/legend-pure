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

package org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.factory.Stacks;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.stack.MutableStack;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningClassProcessor;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.valuespecification.InstanceValueProcessor;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.PackageInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.KeyExpressionInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.EnumStubInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.Import;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroup;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportGroupInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.ImportStubInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.PropertyStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel._import.PropertyStubInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.constraint.Constraint;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.constraint.ConstraintInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Profile;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.ProfileInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.StereotypeInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.TagInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.TaggedValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.TaggedValueInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinitionInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunctionInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.NativeFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.NativeFunctionInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.DefaultValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.DefaultValueInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.PropertyInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedPropertyInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.MultiplicityInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.MultiplicityValueInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.GenericTypeOperationInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.AssociationInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.AssociationProjection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.AssociationProjectionInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.GeneralizationInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.ExistingPropertyRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.ExistingPropertyRouteNodeInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.NewPropertyRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.NewPropertyRouteNodeFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.NewPropertyRouteNodeFunctionDefinitionInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.NewPropertyRouteNodeInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.PropertyRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RootRouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RootRouteNodeInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RouteNode;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RouteNodePropertyStub;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.treepath.RouteNodePropertyStubInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassProjection;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassProjectionInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.EnumerationInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionTypeInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.MeasureInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveTypeInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.UnitInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericTypeInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.TypeParameter;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.TypeParameterInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValueInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.SimpleFunctionExpressionInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpressionInstance;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ValueSpecificationBootstrap;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m3.navigation.relation._Column;
import org.finos.legend.pure.m3.navigation.relation._RelationType;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.AllOrFunctionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ArithmeticPartContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.AssociationContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.AssociationProjectionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.AtomicExpressionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.BooleanPartContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.BuildMilestoningVariableExpressionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ClassDefinitionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.CodeBlockContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.CombinedArithmeticOnlyContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.CombinedExpressionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ComplexConstraintContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ComplexPropertyContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ConstraintContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ConstraintsContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ContravarianceTypeParameterContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.DefaultValueContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.DefaultValueExpressionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.DefinitionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.DerivedPropertyContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.DslContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.EnumDefinitionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.EnumValueContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.EqualNotEqualContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ExpressionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ExpressionInstanceAtomicRightSideContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ExpressionInstanceContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ExpressionInstanceParserPropertyAssignmentContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ExpressionInstanceRightSideContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ExpressionOrExpressionGroupContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ExpressionPartContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ExtraFunctionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.FunctionDefinitionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.FunctionExpressionParametersContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.FunctionTypePureTypeContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.FunctionTypeSignatureContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.FunctionVariableExpressionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.IdentifierContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ImportsContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.InstanceAtomicRightSideContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.InstanceContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.InstanceLiteralContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.InstanceLiteralTokenContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.InstancePropertyAssignmentContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.InstanceReferenceContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.InstanceRightSideContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.LambdaFunctionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.LambdaParamTypeContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.LambdaPipeContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.LetExpressionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MappingContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MappingLineContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MeasureDefinitionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MultiplicityArgumentContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MultiplicityArgumentsContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.NativeFunctionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.NonConvertibleUnitExprContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.NotExpressionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.PackagePathContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ProfileContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ProgramLineContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.PropertiesContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.PropertyContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.PropertyExpressionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.PropertyNameContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.PropertyOrFunctionExpressionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.PropertyRefContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.QualifiedNameContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.QualifiedPropertyContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.SignedExpressionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.SimpleConstraintContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.SimplePropertyContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.SourceAndTargetMappingIdContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.StereotypeContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.StereotypeDefinitionsContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.StereotypesContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.SubsetTypeContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TagDefinitionsContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TaggedValueContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TaggedValuesContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TreePathClassBodyContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TreePathContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TreePathPropertyParameterTypeContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TypeAddSubOperationContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TypeAndMultiplicityParametersContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TypeArgumentsContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TypeContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TypeParameterContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TypeParametersWithContravarianceAndMultiplicityParametersContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TypeWithOperationContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.UnitExprContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.UnitInstanceContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.UnitInstanceLiteralContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.UnitNameContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.VariableContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m3.serialization.runtime.SourceState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.grammar.StringEscape;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

import java.util.List;

public class AntlrContextToM3CoreInstance
{
    private final String tab = "    ";
    private int functionCounter;
    private final ModelRepository repository;
    private final Context context;
    private final ProcessorSupport processorSupport;
    private final AntlrSourceInformation sourceInformation;
    private final InlineDSLLibrary inlineDSLLibrary;
    private final MutableList<CoreInstance> coreInstancesResult;
    private Multiplicity pureOne;
    private Multiplicity pureZero;
    private Multiplicity zeroMany;
    private Multiplicity zeroOne;
    private Multiplicity oneMany;
    private final int count;
    private final boolean addLines;
    private final SourceState oldState;
    private final MutableMap<CoreInstance, SourceInformation> newSourceInfoMap;
    private final MutableSet<CoreInstance> oldInstances;

    public AntlrContextToM3CoreInstance(Context context, ModelRepository repository, ProcessorSupport processorSupport, AntlrSourceInformation sourceInformation, InlineDSLLibrary inlineDSLLibrary, MutableList<CoreInstance> coreInstancesResult, int count, boolean addLines, SourceState oldState)
    {
        this.context = context;
        this.repository = repository;
        this.processorSupport = processorSupport;
        this.sourceInformation = sourceInformation;
        this.inlineDSLLibrary = inlineDSLLibrary;
        this.coreInstancesResult = coreInstancesResult;
        this.count = count;
        this.addLines = addLines;
        this.oldState = oldState;
        this.newSourceInfoMap = Maps.mutable.empty();
        this.oldInstances = this.oldState == null ? Sets.mutable.empty() : this.oldState.getInstances().toSet();
    }

    public CoreInstance definition(DefinitionContext ctx, boolean useImportStubsInInstanceParser) throws PureParserException
    {
        if (ctx.imports() == null)
        {
            throw new PureParserException(this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.getStart(), ctx.getStop()), "Null imports!");
        }
        boolean hasImportChanged = true;
        ImportGroup _importId = imports(ctx.imports());
        if (this.oldState != null)
        {
            String importGroupID = _importId._name();
            MutableList<? extends PackageableElement> oldImportGroups = this.oldState.getImportGroups().select(e -> importGroupID.equals(e._name()), Lists.mutable.empty());
            if (oldImportGroups.size() == 1)
            {
                ImportGroup oldImportGroup = (ImportGroup) oldImportGroups.get(0);
                MutableSet<String> oldPaths = oldImportGroup._imports().collect(Import::_path, Sets.mutable.empty());
                MutableSet<String> newPaths = _importId._imports().collect(Import::_path, Sets.mutable.empty());
                if (oldPaths.equals(newPaths))
                {
                    hasImportChanged = false;
                    Package systemImports = (PackageInstance) this.processorSupport.package_getByUserPath("system::imports");
                    systemImports._children(systemImports._children().reject(e -> importGroupID.equals(e._name())));
                    systemImports._childrenAdd(oldImportGroup);
                    oldImportGroup._package(systemImports);
                    oldImportGroup.setSourceInformation(_importId.getSourceInformation());
                    oldImportGroup._imports(_importId._imports());
                    _importId = oldImportGroup;
                }
            }
        }

        final ImportGroup importId = _importId;
        CoreInstance result = null;
        if (ctx.profile() != null)
        {
            result = processPackageableElement(ctx, hasImportChanged, importId, DefinitionContext::profile, this::profile, ProfileContext::qualifiedName, repository, sourceInformation, oldState, oldInstances, coreInstancesResult, newSourceInfoMap);
        }
        if (ctx.classDefinition() != null)
        {
            result = processPackageableElement(ctx, hasImportChanged, importId, DefinitionContext::classDefinition, z -> classParser(z, importId, addLines), ClassDefinitionContext::qualifiedName, repository, sourceInformation, oldState, oldInstances, coreInstancesResult, newSourceInfoMap);
        }
        if (ctx.primitiveDefinition() != null)
        {
            result = processPackageableElement(ctx, hasImportChanged, importId, DefinitionContext::primitiveDefinition, z -> primitiveParser(z, importId), M3Parser.PrimitiveDefinitionContext::qualifiedName, repository, sourceInformation, oldState, oldInstances, coreInstancesResult, newSourceInfoMap);
        }
        if (ctx.measureDefinition() != null)
        {
            result = processPackageableElement(ctx, hasImportChanged, importId, DefinitionContext::measureDefinition, z -> measureParser(z, importId), MeasureDefinitionContext::qualifiedName, repository, sourceInformation, oldState, oldInstances, coreInstancesResult, newSourceInfoMap);
        }
        if (ctx.association() != null)
        {
            result = processPackageableElement(ctx, hasImportChanged, importId, DefinitionContext::association, z -> associationParser(z, importId), AssociationContext::qualifiedName, repository, sourceInformation, oldState, oldInstances, coreInstancesResult, newSourceInfoMap);
        }
        if (ctx.enumDefinition() != null)
        {
            result = processPackageableElement(ctx, hasImportChanged, importId, DefinitionContext::enumDefinition, z -> enumParser(z, importId), EnumDefinitionContext::qualifiedName, repository, sourceInformation, oldState, oldInstances, coreInstancesResult, newSourceInfoMap);
        }
        if (ctx.nativeFunction() != null)
        {
            result = processPackageableElement(ctx, hasImportChanged, importId, DefinitionContext::nativeFunction, z -> nativeFunction(z, importId, "", coreInstancesResult), NativeFunctionContext::qualifiedName, repository, sourceInformation, oldState, oldInstances, coreInstancesResult, newSourceInfoMap);
        }
        if (ctx.functionDefinition() != null)
        {
            result = processPackageableElement(ctx, hasImportChanged, importId, DefinitionContext::functionDefinition, z -> concreteFunctionDefinition(z, importId, true, "", coreInstancesResult), FunctionDefinitionContext::qualifiedName, repository, sourceInformation, oldState, oldInstances, coreInstancesResult, newSourceInfoMap);
        }
        if (ctx.instance() != null)
        {
            for (InstanceContext pCtx : ctx.instance())
            {
                result = this.instanceParser(pCtx, true, importId, this.addLines, "", true, useImportStubsInInstanceParser);
                this.coreInstancesResult.add(result);
            }
        }
        this.newSourceInfoMap.forEachKeyValue(CoreInstance::setSourceInformation);
        return result;
    }

    private static <T extends ParserRuleContext> CoreInstance processPackageableElement(
            DefinitionContext ctx,
            boolean hasImportChanged,
            ImportGroup importId,
            Function<DefinitionContext, List<T>> getValues,
            Function<T, CoreInstance> process,
            Function<T, QualifiedNameContext> getQualifiedName,
            ModelRepository repository,
            AntlrSourceInformation sourceInformation,
            SourceState oldState,
            MutableSet<CoreInstance> oldInstances,
            MutableList<CoreInstance> coreInstancesResult,
            MutableMap<CoreInstance, SourceInformation> newSourceInfoMap
    )
    {
        CoreInstance result = null;
        for (T pCtx : getValues.apply(ctx))
        {
            if (hasImportChanged)
            {
                result = process.apply(pCtx);
            }
            else
            {
                String importGroupID = importId._name();
                String newContent = normalizeContent(pCtx.start.getInputStream().getText(new Interval(pCtx.start.getStartIndex(), pCtx.stop.getStopIndex())));
                MutableList<CoreInstance> foundOldInstances = oldInstances.select(i -> oldState.instanceImportGroupInSourceEqualsNewImportGroup(i, importGroupID) && oldState.instanceContentInSourceEqualsNewContent(i, newContent), Lists.mutable.empty());

                if (foundOldInstances.size() == 1)
                {
                    CoreInstance thisInstance = foundOldInstances.get(0);
                    oldInstances.remove(thisInstance);
                    SourceInformation newSourceInfo = sourceInformation.getPureSourceInformation(pCtx.getStart(), getQualifiedName.apply(pCtx).getStop(), pCtx.getStop());
                    if (thisInstance.getSourceInformation().getStartColumn() == newSourceInfo.getStartColumn())
                    {
                        offsetSourceInformationForInstanceAndChildren(thisInstance, newSourceInfo.getStartLine() - thisInstance.getSourceInformation().getStartLine(), newSourceInfoMap);
                        buildAndSetPackage((PackageableElement) thisInstance, getQualifiedName.apply(pCtx).packagePath(), repository, sourceInformation);
                        result = thisInstance;
                    }
                    else
                    {
                        result = process.apply(pCtx);
                    }
                }
                else
                {
                    result = process.apply(pCtx);
                }
            }
            coreInstancesResult.add(result);
        }
        return result;
    }

    private static String normalizeContent(String content)
    {
        return ArrayIterate.makeString(content.split("\\R"), "", System.lineSeparator(), System.lineSeparator());
    }

    private static void offsetSourceInformationForInstanceAndChildren(CoreInstance instance, int offset, MutableMap<CoreInstance, SourceInformation> newSourceInfoMap)
    {
        SourceInformation oldSourceInfo = instance.getSourceInformation();
        MutableSet<CoreInstance> visited = Sets.mutable.empty();
        MutableStack<CoreInstance> searchStack = Stacks.mutable.with(instance);
        while (!searchStack.isEmpty())
        {
            CoreInstance next = searchStack.pop();
            if (visited.add(next))
            {
                boolean searchPropertyValues = false;
                SourceInformation sourceInfo = next.getSourceInformation();
                if (sourceInfo == null)
                {
                    searchPropertyValues = !(next instanceof Package);
                }
                else if (oldSourceInfo.subsumes(sourceInfo))
                {
                    newSourceInfoMap.add(Tuples.pair(next, new SourceInformation(sourceInfo.getSourceId(), sourceInfo.getStartLine() + offset, sourceInfo.getStartColumn(), sourceInfo.getLine() + offset, sourceInfo.getColumn(), sourceInfo.getEndLine() + offset, sourceInfo.getEndColumn())));
                    searchPropertyValues = true;
                }
                if (searchPropertyValues)
                {
                    for (String key : next.getKeys())
                    {
                        for (CoreInstance value : next.getValueForMetaPropertyToMany(key))
                        {
                            searchStack.push(value);
                        }
                    }
                }
            }
        }
    }

    private ImportGroup imports(ImportsContext ctx)
    {
        MutableList<Import> imports = ListIterate.collect(
                ctx.import_statement(),
                isCtx -> ImportInstance.createPersistent(
                        this.repository,
                        this.sourceInformation.getPureSourceInformation(isCtx.getStart(), isCtx.packagePath().getStart(), isCtx.STAR().getSymbol()),
                        packageToString(isCtx.packagePath())));

        String name = createImportGroupId(this.sourceInformation.getSourceName(), this.count);

        Package systemImports = (Package) this.processorSupport.package_getByUserPath("system::imports");
        CoreInstance child = _Package.findInPackage(systemImports, name);

        // If the ImportGroup does not exist, create a new one
        if (child == null)
        {
            ImportGroup importGroup = ImportGroupInstance.createPersistent(this.repository, name, getImportGroupSourceInfo(imports))
                    ._name(name)
                    ._imports(imports);
            setPackage(importGroup, systemImports);
            return importGroup;
        }

        // If a child with the given name exists but is not an ImportGroup, throw an exception
        if (!(child instanceof ImportGroup))
        {
            StringBuilder builder = new StringBuilder().append("system::imports::").append(name).append(" is not an ImportGroup");
            CoreInstance classifier;
            try
            {
                classifier = this.processorSupport.getClassifier(child);
            }
            catch (Exception ignore)
            {
                classifier = null;
            }
            if (classifier != null)
            {
                org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(builder.append(": found "), classifier);
            }
            throw new RuntimeException(builder.toString());
        }

        // If the ImportGroup already exists, check source information and add imports
        ImportGroup importGroup = (ImportGroup) child;
        SourceInformation igSourceInfo = importGroup.getSourceInformation();
        if (!this.sourceInformation.getSourceName().equals(igSourceInfo.getSourceId()))
        {
            StringBuilder builder = new StringBuilder("ImportGroup system::imports::").append(name);
            igSourceInfo.appendMessage(builder.append(" (")).append(") cannot be used for source ").append(this.sourceInformation.getSourceName());
            throw new RuntimeException(builder.toString());
        }
        if (imports.notEmpty())
        {
            StringBuilder builder = new StringBuilder("Cannot add imports to already existing ImportGroup system::imports::").append(name);
            igSourceInfo.appendMessage(builder.append(" (")).append(")");
            throw new RuntimeException(builder.toString());
        }
        return importGroup;
    }

    private SourceInformation getImportGroupSourceInfo(Iterable<? extends Import> imports)
    {
        int[] bounds = Iterate.injectInto(null, imports, (result, _import) ->
        {
            SourceInformation sourceInfo = _import.getSourceInformation();
            if (sourceInfo != null)
            {
                if (result == null)
                {
                    return new int[]{sourceInfo.getStartLine(), sourceInfo.getStartColumn(), sourceInfo.getEndLine(), sourceInfo.getEndColumn()};
                }

                if (SourceInformation.isBefore(sourceInfo.getStartLine(), sourceInfo.getStartColumn(), result[0], result[1]))
                {
                    result[0] = sourceInfo.getStartLine();
                    result[1] = sourceInfo.getStartColumn();
                }
                if (SourceInformation.isAfter(sourceInfo.getEndLine(), sourceInfo.getEndColumn(), result[2], result[3]))
                {
                    result[2] = sourceInfo.getEndLine();
                    result[3] = sourceInfo.getEndColumn();
                }
            }
            return result;
        });
        return (bounds == null) ?
                getDefaultImportGroupSourceInfo() :
                new SourceInformation(this.sourceInformation.getSourceName(), bounds[0], bounds[1], bounds[2], bounds[3]);
    }

    private SourceInformation getDefaultImportGroupSourceInfo()
    {
        int line = this.sourceInformation.getOffsetLine() + 1;
        return new SourceInformation(this.sourceInformation.getSourceName(), line, 0, line, 0);
    }

    public CoreInstance combinedExpression(CombinedExpressionContext ctx, String exprName, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        CoreInstance result = this.expressionOrExpressionGroup(ctx.expressionOrExpressionGroup(), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
        CoreInstance boolResult = result;
        CoreInstance arithResult = result;

        if (ctx.expressionPart() != null)
        {
            MutableList<ArithmeticPartContext> arth = Lists.mutable.empty();
            MutableList<BooleanPartContext> bool = Lists.mutable.empty();

            // Invariant: arth.isEmpty() || bool.isEmpty ie they can't both contains elements at the same time
            for (ExpressionPartContext epCtx : ctx.expressionPart())
            {
                if (epCtx.arithmeticPart() != null)
                {
                    if (!bool.isEmpty())
                    {
                        boolResult = this.booleanPart(bool, (ValueSpecification) arithResult, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
                        bool.clear();
                    }
                    arth.add(epCtx.arithmeticPart());
                }
                else if (epCtx.booleanPart() != null)
                {
                    if (!arth.isEmpty())
                    {
                        arithResult = this.arithmeticPart(arth, boolResult, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
                        arth.clear();
                    }
                    bool.add(epCtx.booleanPart());
                }
            }

            // Invariant allows us to make the choice here - either we still have arth to process or bool to process but not both 
            if (!arth.isEmpty())
            {
                result = this.arithmeticPart(arth, boolResult, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            else if (!bool.isEmpty())
            {
                result = this.booleanPart(bool, (ValueSpecification) arithResult, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
        }
        return result;
    }

    private CoreInstance expressionOrExpressionGroup(ExpressionOrExpressionGroupContext ctx, String exprName, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        return this.expression(ctx.expression(), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
    }

    private enum BoolOp
    {
        AND, OR
    }

    private boolean isLowerPrecedenceBoolean(String boolOp1, String boolOp2)
    {
        return "or".equals(boolOp1) && "and".equals(boolOp2);
    }

    private SimpleFunctionExpression buildBoolean(BooleanPartContext ctx, BoolOp op, CoreInstance initialValue, String exprName, MutableList<String> typeParamtersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        TerminalNode terminalNode;
        switch (op)
        {
            case AND:
            {
                terminalNode = ctx.AND();
                break;
            }
            case OR:
            {
                terminalNode = ctx.OR();
                break;
            }
            default:
            {
                throw new IllegalArgumentException("Unexpected boolean operation in buildBoolean" + op);
            }
        }

        CoreInstance other = this.expression(ctx.expression(), exprName, typeParamtersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
        SimpleFunctionExpression sfe = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(terminalNode.getSymbol()), null, null, importId, null);
        sfe._functionName(op.name().toLowerCase());
        sfe._parametersValues(Lists.mutable.of((ValueSpecification) initialValue, (ValueSpecification) other));
        return sfe;
    }

    private SimpleFunctionExpression processBooleanOp(SimpleFunctionExpression sfe, BooleanPartContext ctx, BoolOp op, CoreInstance initialValue, String exprName, MutableList<String> typeParamtersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        if (sfe == null)
        {
            return buildBoolean(ctx, op, initialValue, exprName, typeParamtersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
        }

        if (isLowerPrecedenceBoolean(sfe.getValueForMetaPropertyToOne("functionName").getName(), op.name().toLowerCase()))
        {
            ListIterable<? extends CoreInstance> params = sfe.getValueForMetaPropertyToMany("parametersValues");
            SimpleFunctionExpression newSfe = buildBoolean(ctx, op, params.getLast(), exprName, typeParamtersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
            MutableList<CoreInstance> l = Lists.mutable.withAll(params.subList(0, params.size() - 1));
            sfe._parametersValues(Lists.mutable.of((ValueSpecification) l.get(0), newSfe));
            return sfe;
        }

        return buildBoolean(ctx, op, sfe, exprName, typeParamtersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
    }

    private SimpleFunctionExpression booleanPart(List<BooleanPartContext> bList, ValueSpecification input, String exprName, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        SimpleFunctionExpression sfe = null;
        for (BooleanPartContext ctx : bList)
        {
            if (ctx.AND() != null)
            {
                sfe = processBooleanOp(sfe, ctx, BoolOp.AND, input, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            else if (ctx.OR() != null)
            {
                sfe = processBooleanOp(sfe, ctx, BoolOp.OR, input, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            else
            {
                sfe = this.equalNotEqual(ctx.equalNotEqual(), sfe == null ? input : sfe, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
        }
        return sfe;
    }

    private CoreInstance nonArrowOrEqual(M3Parser.NonArrowOrEqualExpressionContext ctx, String exprName, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        if (ctx.combinedExpression() != null)
        {
            return combinedExpression(ctx.combinedExpression(), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
        }

        if (ctx.atomicExpression() != null)
        {
            return atomicExpression(ctx.atomicExpression(), typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
        }
        if (ctx.notExpression() != null)
        {
            return notExpression(ctx.notExpression(), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, importId, addLines);
        }
        if (ctx.signedExpression() != null)
        {
            return signedExpression(ctx.signedExpression(), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, importId, addLines);
        }
        if (ctx.expressionsArray() != null)
        {
            MutableList<CoreInstance> expressions = ListIterate.collect(ctx.expressionsArray().expression(), eCtx -> expression(eCtx, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, false, importId, addLines));
            return doWrap(expressions, ctx.expressionsArray().getStart().getLine(), ctx.expressionsArray().getStart().getCharPositionInLine() + 1, ctx.getStop().getLine(), ctx.getStop().getCharPositionInLine() + 1);
        }

        CoreInstance start;
        CoreInstance end;
        CoreInstance step;
        MutableList<CoreInstance> expressions = Lists.mutable.of();
        switch (ctx.sliceExpression().expression().size())
        {
            case 1: //:end
            {
                start = null;
                end = this.expression(ctx.sliceExpression().expression(0), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
                step = null;
                break;
            }
            case 2: //start:end
            {
                start = this.expression(ctx.sliceExpression().expression(0), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
                expressions.add(start);
                end = this.expression(ctx.sliceExpression().expression(1), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
                step = null;
                break;
            }
            case 3: //start:end:step
            {
                start = this.expression(ctx.sliceExpression().expression(0), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
                expressions.add(start);
                end = this.expression(ctx.sliceExpression().expression(1), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
                step = this.expression(ctx.sliceExpression().expression(2), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
                break;
            }
            default:
            {
                //Not reachable. coded just for comment
                start = null;
                end = null;
                step = null;
                break;
            }
        }
        MutableList<ValueSpecification> params = Lists.mutable.empty();
        if (start != null)
        {
            params.add(this.doWrap(Lists.mutable.of(start)));
        }
        params.add(this.doWrap(Lists.mutable.of(end)));
        if (step != null)
        {
            params.add(this.doWrap(Lists.mutable.of(step)));
        }
        return SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.getStart()), null, null, importId, null)
                ._functionName("range")
                ._parametersValues(params);
    }

    private CoreInstance expression(ExpressionContext ctx, String exprName, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        CoreInstance result;

        MutableList<ValueSpecification> parameters = Lists.mutable.empty();

        result = nonArrowOrEqual(ctx.nonArrowOrEqualExpression(), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
        if (ctx.propertyOrFunctionExpression() != null)
        {
            for (PropertyOrFunctionExpressionContext pfCtx : ctx.propertyOrFunctionExpression())
            {
                if (pfCtx.propertyExpression() != null)
                {
                    result = propertyExpression(pfCtx.propertyExpression(), result, parameters, typeParametersNames, multiplicityParameterNames, lambdaContext, space, importId);
                }
                else
                {
                    for (int i = 0; i < pfCtx.functionExpression().qualifiedName().size(); i++)
                    {
                        parameters = this.functionExpressionParameters(pfCtx.functionExpression().functionExpressionParameters(i), typeParametersNames, multiplicityParameterNames, importId, lambdaContext, addLines, spacePlusTabs(space, 4));
                        parameters.add(0, (ValueSpecification) result);
                        result = this.functionExpression(pfCtx.functionExpression().qualifiedName(i), parameters, importId);
                    }
                }
            }
        }


        if (ctx.equalNotEqual() != null)
        {
            result = this.equalNotEqual(ctx.equalNotEqual(), (ValueSpecification) result, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
        }
        return result;
    }

    private CoreInstance propertyExpression(PropertyExpressionContext ctx, CoreInstance result, MutableList<ValueSpecification> parameters, MutableList<String> multiplicityParameterNames, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, ImportGroup importId)
    {
        parameters.clear();
        boolean function = false;
        PropertyNameContext property = ctx.propertyName();
        CoreInstance parameter;
        if (ctx.functionExpressionParameters() != null)
        {
            function = true;
            FunctionExpressionParametersContext fepCtx = ctx.functionExpressionParameters();
            if (fepCtx.combinedExpression() != null)
            {
                for (CombinedExpressionContext ceCtx : fepCtx.combinedExpression())
                {
                    parameter = this.combinedExpression(ceCtx, "param", typeParametersNames, multiplicityParameterNames, lambdaContext, spacePlusTabs(space, 4), true, importId, addLines);
                    parameters.add((ValueSpecification) parameter);
                }
            }
        }
        else if (ctx.functionExpressionLatestMilestoningDateParameter() != null)
        {
            function = true;
            ListIterate.collect(ctx.functionExpressionLatestMilestoningDateParameter().LATEST_DATE(), terminalNode -> InstanceValueInstance.createPersistent(repository, sourceInformation.getPureSourceInformation(terminalNode.getSymbol()), null, null)._values(Lists.immutable.with(repository.newLatestDateCoreInstance())), parameters);
        }
        if (!function)
        {
            SimpleFunctionExpressionInstance sfe = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(property.getStart()), null, null, importId, null);
            InstanceValue instanceValue = this.doWrap(property);
            //Going to become an auto-map lambda so set a name to be used for the lambda
            instanceValue.setName(lambdaContext.getLambdaFunctionUniqueName());
            instanceValue._multiplicity(this.getPureOne());
            GenericTypeInstance gt = GenericTypeInstance.createPersistent(this.repository);
            Type ti = (Type) this.processorSupport.package_getByUserPath("String");
            gt._rawTypeCoreInstance(ti);
            instanceValue._genericType(gt);
            sfe._propertyName(instanceValue);
            sfe._parametersValues(Lists.mutable.of((ValueSpecification) result));
            result = sfe;
        }
        else
        {
            SimpleFunctionExpressionInstance sfe = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(property.getStart()), null, null, importId, null);
            InstanceValue instanceValue = this.doWrap(property);
            //Going to become an auto-map lambda so set a name to be used for the lambda
            instanceValue.setName(lambdaContext.getLambdaFunctionUniqueName());
            instanceValue._multiplicity(this.getPureOne());
            GenericTypeInstance gt = GenericTypeInstance.createPersistent(this.repository);
            Type ti = (Type) this.processorSupport.package_getByUserPath("String");
            gt._rawTypeCoreInstance(ti);
            instanceValue._genericType(gt);
            sfe._qualifiedPropertyName(instanceValue);
            sfe._parametersValues(Lists.mutable.of((ValueSpecification) result).withAll(parameters));
            result = sfe;
        }
        return result;
    }

    private SimpleFunctionExpression signedExpression(SignedExpressionContext ctx, String exprName, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, ImportGroup importId, boolean addLines)
    {
        CoreInstance number;
        SimpleFunctionExpression result;
        if (ctx.MINUS() != null)
        {
            number = this.expression(ctx.expression(), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, true, importId, addLines);
            result = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.MINUS().getSymbol()), null, null, importId, null);
            result._functionName("minus");
            result._parametersValues(Lists.mutable.of((ValueSpecification) number));
        }
        else
        {
            number = this.expression(ctx.expression(), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, true, importId, addLines);
            result = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.PLUS().getSymbol()), null, null, importId, null);
            result._functionName("plus");
            result._parametersValues(Lists.mutable.of((ValueSpecification) number));
        }
        return result;
    }

    private SimpleFunctionExpression notExpression(NotExpressionContext ctx, String exprName, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, ImportGroup importId, boolean addLines)
    {
        CoreInstance negated;
        SimpleFunctionExpression result;
        negated = this.expression(ctx.expression(), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, true, importId, addLines);
        result = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.NOT().getSymbol()), null, null, importId, null);
        result._functionName("not");
        result._parametersValues(Lists.mutable.of((ValueSpecification) negated));
        return result;
    }

    private CoreInstance instanceLiteralToken(InstanceLiteralTokenContext ctx, boolean wrapFlag)
    {
        CoreInstance result = null;
        try
        {
            if (ctx.STRING() != null)
            {
                String withQuote = StringEscape.unescape(ctx.getText());
                result = this.repository.newStringCoreInstance_cached(withQuote.substring(1, withQuote.length() - 1));
            }
            else if (ctx.INTEGER() != null)
            {
                result = this.repository.newIntegerCoreInstance(ctx.getText());
            }
            else if (ctx.FLOAT() != null)
            {
                result = this.repository.newFloatCoreInstance(ctx.getText());
            }
            else if (ctx.DECIMAL() != null)
            {
                result = this.repository.newDecimalCoreInstance(ctx.getText());
            }
            else if (ctx.DATE() != null)
            {
                result = this.repository.newDateCoreInstance(ctx.getText());
            }
            else if (ctx.STRICTTIME() != null)
            {
                result = this.repository.newStrictTimeCoreInstance(ctx.getText());
            }
            else if (ctx.BOOLEAN() != null)
            {
                result = this.repository.newBooleanCoreInstance(ctx.getText());
            }
        }
        catch (Exception e)
        {
            SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.getStart(), ctx.getStop());
            throw new PureParserException(sourceInfo, e.getMessage(), e);
        }
        return wrapFlag ? this.doWrap(result, ctx.getStart()) : result;
    }

    private CoreInstance atomicExpression(AtomicExpressionContext ctx, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        CoreInstance result;
        ListIterable<CoreInstance> dsl;

        MutableList<VariableExpression> expressions = Lists.mutable.of();
        if (ctx.instanceLiteralToken() != null)
        {
            result = this.instanceLiteralToken(ctx.instanceLiteralToken(), wrapFlag);
        }
        else if (ctx.columnBuilders() != null)
        {
            SourceInformation src = this.sourceInformation.getPureSourceInformation(ctx.getStart());

            // Create the RelationType
            MutableList<CoreInstance> lambdas = Lists.mutable.empty();
            MutableList<CoreInstance> columnNames = Lists.mutable.empty();
            MutableList<CoreInstance> columnInstances = Lists.mutable.empty();
            MutableList<CoreInstance> extraFunction = Lists.mutable.empty();
            ListIterate.forEach(ctx.columnBuilders().oneColSpec(), oneColSpec ->
            {
                M3Parser.ColumnNameContext colNameCtx = oneColSpec.columnName();
                String colName = StringEscape.unescape(removeQuotes(colNameCtx.getText()));
                columnNames.add(this.repository.newStringCoreInstance(colName));
                GenericType returnType = null;
                Multiplicity multiplicity = (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.newMultiplicity(0, 1, processorSupport);
                if (oneColSpec.anyLambda() != null)
                {
                    lambdas.add(processLambda(oneColSpec.anyLambda(), Lists.mutable.empty(), Lists.mutable.empty(), lambdaContext, importId, space, addLines, false, Lists.mutable.empty()));
                    if (oneColSpec.extraFunction() != null)
                    {
                        ExtraFunctionContext extraFunctionContext = oneColSpec.extraFunction();
                        extraFunction.add(processLambda(extraFunctionContext.anyLambda(), Lists.mutable.empty(), Lists.mutable.empty(), lambdaContext, importId, space, addLines, false, Lists.mutable.empty()));
                    }
                }
                else if (oneColSpec.type() != null)
                {
                    returnType = type(oneColSpec.type(), typeParametersNames, "", importId, addLines);
                }
                if (oneColSpec.multiplicity() != null)
                {
                    multiplicity = this.buildMultiplicity(oneColSpec.multiplicity().multiplicityArgument());
                }
                if (returnType == null)
                {
                    returnType = (GenericType) processorSupport.newAnonymousCoreInstance(src, M3Paths.GenericType);
                    returnType._rawType(null);
                }
                columnInstances.add(_Column.getColumnInstance(colName, false, returnType, multiplicity, src, processorSupport));
            });
            RelationType<?> relationType = _RelationType.build(columnInstances, this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.getStart(), ctx.getStop()), processorSupport);
            GenericType relationTypeGenericType = (GenericType) processorSupport.type_wrapGenericType(relationType);

            // Build the function
            CoreInstance replacementFunction = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.getStart()), null, null, importId, null);

            // FunctionName
            boolean isArray = ctx.columnBuilders().BRACKET_OPEN() != null;
            List<Boolean> nonFunctions = ListIterate.collect(ctx.columnBuilders().oneColSpec(), x -> x.type() != null | x.COLON() == null).distinct();
            if (isArray && nonFunctions.size() > 1)
            {
                throw new PureParserException(this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.getStart(), ctx.getStop()), "Can't mix column types");
            }
            boolean nonFunction = nonFunctions.get(0);

            if (isArray && !nonFunction)
            {
                if (!extraFunction.isEmpty())
                {
                    MutableList<CoreInstance> allColSpecs = Lists.mutable.empty();
                    for (int i = 0; i < lambdas.size(); i++)
                    {
                        GenericType localColumnType = GenericTypeInstance.createPersistent(this.repository);
                        localColumnType._rawTypeCoreInstance(_RelationType.build(Lists.immutable.with(_Column.getColumnInstance(columnNames.get(i).getName(), false, (String) null, (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.newMultiplicity(0, 1, processorSupport), src, processorSupport)), this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.getStart(), ctx.getStop()), processorSupport));

                        CoreInstance aggColFunc = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.getStart()), null, null, importId, null);
                        aggColFunc.setKeyValues(Lists.immutable.with("functionName"), Lists.mutable.with(this.repository.newStringCoreInstance(mayShiftTo2kindFunction((LambdaFunction<?>) lambdas.get(i), "aggColSpec"))));
                        MutableList<CoreInstance> parameters = Lists.mutable.empty();
                        parameters.add(ValueSpecificationBootstrap.wrapValueSpecification(lambdas.get(i), true, processorSupport));
                        parameters.add(ValueSpecificationBootstrap.wrapValueSpecification(extraFunction.get(i), true, processorSupport));
                        parameters.add(ValueSpecificationBootstrap.wrapValueSpecification(columnNames.get(i), true, processorSupport));
                        parameters.add(InstanceValueInstance.createPersistent(this.repository, "", localColumnType, this.getPureOne()));
                        aggColFunc.setKeyValues(Lists.immutable.with("parametersValues"), parameters);
                        allColSpecs.add(aggColFunc);
                    }
                    replacementFunction.setKeyValues(Lists.immutable.with("functionName"), Lists.mutable.with(this.repository.newStringCoreInstance(mayShiftTo2kindFunction(ctx, lambdas, "aggColSpecArray"))));
                    MutableList<CoreInstance> parameters = Lists.mutable.empty();
                    parameters.add(InstanceValueInstance.createPersistent(this.repository, null, null)._values(allColSpecs));
                    parameters.add(InstanceValueInstance.createPersistent(this.repository, "", relationTypeGenericType, this.getPureOne()));
                    replacementFunction.setKeyValues(Lists.immutable.with("parametersValues"), parameters);
                    result = replacementFunction;
                }
                else
                {
                    MutableList<CoreInstance> allColSpecs = Lists.mutable.empty();
                    for (int i = 0; i < lambdas.size(); i++)
                    {
                        GenericType localColumnType = GenericTypeInstance.createPersistent(this.repository);
                        localColumnType._rawTypeCoreInstance(_RelationType.build(Lists.immutable.with(_Column.getColumnInstance(columnNames.get(i).getName(), false, (String) null, (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.newMultiplicity(0, 1, processorSupport), src, processorSupport)), this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.getStart(), ctx.getStop()), processorSupport));

                        CoreInstance colFunc = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.getStart()), null, null, importId, null);
                        colFunc.setKeyValues(Lists.immutable.with("functionName"), Lists.mutable.with(this.repository.newStringCoreInstance(mayShiftTo2kindFunction((LambdaFunction<?>) lambdas.get(i), "funcColSpec"))));
                        MutableList<CoreInstance> parameters = Lists.mutable.empty();
                        parameters.add(ValueSpecificationBootstrap.wrapValueSpecification(lambdas.get(i), true, processorSupport));
                        parameters.add(ValueSpecificationBootstrap.wrapValueSpecification(columnNames.get(i), true, processorSupport));
                        parameters.add(InstanceValueInstance.createPersistent(this.repository, "", localColumnType, this.getPureOne()));
                        colFunc.setKeyValues(Lists.immutable.with("parametersValues"), parameters);
                        allColSpecs.add(colFunc);
                    }
                    replacementFunction.setKeyValues(Lists.immutable.with("functionName"), Lists.mutable.with(this.repository.newStringCoreInstance(mayShiftTo2kindFunction(ctx, lambdas, "funcColSpecArray"))));
                    MutableList<CoreInstance> parameters = Lists.mutable.empty();
                    parameters.add(InstanceValueInstance.createPersistent(this.repository, null, null)._values(allColSpecs));
                    parameters.add(InstanceValueInstance.createPersistent(this.repository, "", relationTypeGenericType, this.getPureOne()));
                    replacementFunction.setKeyValues(Lists.immutable.with("parametersValues"), parameters);
                    result = replacementFunction;
                }
            }
            else
            {
                String functionName = !extraFunction.isEmpty() ? "aggColSpec" : nonFunction ? isArray ? "colSpecArray" : "colSpec" : isArray ? "funcColSpecArray" : "funcColSpec";
                functionName = nonFunction ? functionName : mayShiftTo2kindFunction((LambdaFunction<?>) lambdas.get(0), functionName);

                replacementFunction.setKeyValues(Lists.mutable.with("functionName"), Lists.mutable.with(this.repository.newStringCoreInstance(functionName)));

                // Function Parameters
                MutableList<CoreInstance> parameters = Lists.mutable.empty();
                if (!nonFunction)
                {
                    parameters.add(ValueSpecificationBootstrap.wrapValueSpecification(lambdas, true, processorSupport));
                }
                if (!extraFunction.isEmpty())
                {
                    parameters.add(ValueSpecificationBootstrap.wrapValueSpecification(extraFunction.getFirst(), true, processorSupport));
                }
                parameters.add(ValueSpecificationBootstrap.wrapValueSpecification(columnNames, true, processorSupport));
                parameters.add(InstanceValueInstance.createPersistent(this.repository, "", relationTypeGenericType, this.getPureOne()));

                replacementFunction.setKeyValues(Lists.mutable.with("parametersValues"), parameters);

                result = replacementFunction;
            }
        }
        else if (ctx.dsl() != null)
        {
            dsl = this.dsl(ctx.dsl(), importId);
            if (dsl.size() > 1)
            {
                throw new RuntimeException("Unexpected");
            }
            result = wrapFlag ? this.doWrap(dsl.getFirst()) : dsl.getFirst();
        }
        else if (ctx.expressionInstance() != null)
        {
            result = this.expressionInstanceParser(ctx.expressionInstance(), typeParametersNames, lambdaContext, importId, addLines, spacePlusTabs(space, 4));
        }
        else if (ctx.unitInstance() != null)
        {
            result = this.unitInstanceParser(ctx.unitInstance(), importId);
        }
        else if (ctx.variable() != null)
        {
            result = this.variable(ctx.variable());
        }
        else if (ctx.type() != null)
        {
            GenericType genericType = this.type(ctx.type(), typeParametersNames, "", importId, addLines);
            result = InstanceValueInstance.createPersistent(this.repository, genericType, this.getPureOne());
        }
        else if (ctx.multiplicity() != null)
        {
            Multiplicity multiplicity = this.buildMultiplicity(ctx.multiplicity().multiplicityArgument());
            if (multiplicity._multiplicityParameter() != null)
            {
                if (!multiplicityParameterNames.contains(multiplicity._multiplicityParameter()))
                {
                    throw new PureCompilationException(this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.getStart(), ctx.getStop()), "The multiplicity parameter " + multiplicity._multiplicityParameter() + " is unknown!");
                }
            }
            GenericType genericType = GenericTypeInstance.createPersistent(this.repository);
            CoreInstance type = this.repository.getTopLevel("Any");
            genericType._rawTypeCoreInstance(type);
            result = InstanceValueInstance.createPersistent(this.repository, genericType, multiplicity);
        }
        else if (ctx.anyLambda() != null)
        {
            result = this.processLambda(ctx.anyLambda(), typeParametersNames, multiplicityParameterNames, lambdaContext, importId, space, addLines, wrapFlag, expressions);
        }
        else
        {
            result = this.instanceReference(ctx.instanceReference(), typeParametersNames, multiplicityParameterNames, lambdaContext, importId, space, addLines);
        }

        return result;
    }

    private String mayShiftTo2kindFunction(AtomicExpressionContext ctx, MutableList<CoreInstance> lambdas, String functionName)
    {
        MutableList<Boolean> res = lambdas.collect(lambda -> (((FunctionType) ((LambdaFunction<?>) lambda)._classifierGenericType()._typeArguments().getFirst()._rawType())._parameters().size() == 3)).distinct();
        if (res.size() == 1)
        {
            return res.getFirst() ? functionName + "2" : functionName;
        }
        throw new PureParserException(this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.getStart(), ctx.getStop()), "All functions used in the col array should be of the same type.");
    }

    private static String mayShiftTo2kindFunction(LambdaFunction<?> lambda, String functionName)
    {
        if (((FunctionType) lambda._classifierGenericType()._typeArguments().getFirst()._rawType())._parameters().size() == 3)
        {
            functionName = functionName + "2";
        }
        return functionName;
    }


    private CoreInstance processLambda(M3Parser.AnyLambdaContext lambda, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, ImportGroup importId, String space, boolean addLines, boolean wrapFlag, MutableList<VariableExpression> expressions)
    {
        if (lambda.lambdaFunction() != null)
        {
            return processMultiParamLambda(lambda.lambdaFunction(), typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines, expressions);
        }
        else if (lambda.lambdaParam() != null && lambda.lambdaPipe() != null)
        {
            return processSingleParamLambda(lambda.lambdaParam(), lambda.lambdaPipe(), typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines, expressions);
        }
        else
        {
            //lambdaPipe
            return this.lambdaPipe(lambda.lambdaPipe(), null, expressions, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
        }
    }

    private CoreInstance processMultiParamLambda(LambdaFunctionContext ctx, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines, MutableList<VariableExpression> expressions)
    {
        boolean hasLambdaParams = (ctx.lambdaParam() != null) && !ctx.lambdaParam().isEmpty();
        if (hasLambdaParams)
        {
            Iterate.collect(ctx.lambdaParam(), p -> lambdaParam(p.lambdaParamType(), p.identifier(), typeParametersNames, space, importId), expressions);
        }
        return this.lambdaPipe(ctx.lambdaPipe(), hasLambdaParams ? ctx.lambdaParam(0).getStart() : null, expressions, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
    }

    private CoreInstance processSingleParamLambda(M3Parser.LambdaParamContext lambdaCtxt, M3Parser.LambdaPipeContext pipeContext, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines, MutableList<VariableExpression> expressions)
    {
        expressions.add(lambdaParam(lambdaCtxt.lambdaParamType(), lambdaCtxt.identifier(), typeParametersNames, space, importId));
        return lambdaPipe(pipeContext, lambdaCtxt.getStart(), expressions, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
    }

    private CoreInstance instanceReference(InstanceReferenceContext ctx, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, ImportGroup importId, String space, boolean addLines)
    {
        ImportStubInstance is;
        InstanceValueInstance instanceVal;
        if (ctx.qualifiedName() != null)
        {
            is = ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.qualifiedName().identifier().getStart()), this.getQualifiedNameString(ctx.qualifiedName()), importId);
            instanceVal = InstanceValueInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.qualifiedName().identifier().getStart()), null, null);
        }
        else if (ctx.unitName() != null)
        {
            is = ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.unitName().identifier().getStart()), this.getUnitNameWithMeasure(ctx.unitName()), importId);
            instanceVal = InstanceValueInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.unitName().identifier().getStart()), null, null);
        }
        else
        {
            is = ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.getStart()), ctx.PATH_SEPARATOR().getText(), importId);
            instanceVal = InstanceValueInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.getStart()), null, null);
        }

        instanceVal._values(Lists.mutable.<CoreInstance>of(is));
        CoreInstance result = instanceVal;
        if (ctx.allOrFunction() != null)
        {
            result = this.allOrFunction(ctx.allOrFunction(), Lists.mutable.of(instanceVal), ctx.qualifiedName(), typeParametersNames, multiplicityParameterNames, lambdaContext, space, importId, addLines);
        }
        return result;
    }

    private ListIterable<CoreInstance> dsl(DslContext ctx, ImportGroup importId)
    {
        String fullText = ctx.getText();
        String dslText = fullText.substring(fullText.indexOf('#') + 1, fullText.lastIndexOf('#'));
        //TODO temporary hack till we move treepath completely away from dsl
        String trimmedText = dslText.trim();
        if (trimmedText.startsWith("/") || trimmedText.startsWith("{") || trimmedText.startsWith("TDS") || trimmedText.startsWith(">"))
        {
            MutableList<InlineDSL> results = this.inlineDSLLibrary.getInlineDSLs().select(dsl -> dsl.match(dslText), Lists.mutable.empty());
            if (results.size() == 1)
            {
                return Lists.mutable.with(results.get(0).parse(dslText, importId, this.sourceInformation.getSourceName(), ctx.getStart().getCharPositionInLine() + 2, ctx.getStart().getLine() + this.sourceInformation.getOffsetLine(), this.repository, this.context));
            }

            SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.getStart(), ctx.getStop());
            StringBuilder builder = new StringBuilder();
            if (results.isEmpty())
            {
                builder.append("Can't find a parser for '");
                StringEscape.escape(builder, fullText).append("'");
                MutableList<String> knownDSLs = this.inlineDSLLibrary.getInlineDSLNames().toSortedList();
                if (knownDSLs.isEmpty())
                {
                    builder.append(" (no known inline DSL parsers)");
                }
                else
                {
                    knownDSLs.appendString(builder, " (known parsers: ", ", ", ")");
                }
            }
            else
            {
                builder.append("Found ").append(results.size()).append(" parsers (");
                results.collect(InlineDSL::getName).sortThis().appendString(builder, ", ");
                builder.append(") for '");
                StringEscape.escape(builder, fullText).append("'");
            }
            throw new PureParserException(sourceInfo, builder.toString());
        }
        return Lists.mutable.of(new M3AntlrParser(this.inlineDSLLibrary).parseTreePath(dslText, this.sourceInformation.getSourceName(), ctx.getStart().getLine() + this.sourceInformation.getOffsetLine(), ctx.getStart().getCharPositionInLine() + 2, importId, this.repository, this.context));
    }

    public CoreInstance instanceParser(InstanceContext ctx, boolean wrapFlag, ImportGroup importId, boolean addLines, String space, boolean topLevel, boolean useImportStubsInInstanceParser) throws PureParserException
    {
        Token start = ctx.NEW_SYMBOL().getSymbol();
        Token end = ctx.getStop();
        MutableMap<String, ListIterable<CoreInstance>> propertyValues = Maps.mutable.of();
        Token file = null;
        Token startLine = null;
        Token startColumn = null;
        Token line = null;
        Token column = null;
        Token endLine = null;
        Token endColumn = null;
        ListIterable<GenericType> renderedTypeArguments = ctx.typeArguments() == null ? null : this.typeArguments(ctx.typeArguments(), Lists.mutable.empty(), importId, addLines);
        ListIterable<Multiplicity> renderedMultiplicityArguments = ctx.multiplicityArguments() == null ? null : this.multiplicityArguments(ctx.multiplicityArguments());
        if (ctx.FILE_NAME() != null)
        {
            file = ctx.FILE_NAME().getSymbol();
            startLine = ctx.INTEGER(0).getSymbol();
            startColumn = ctx.INTEGER(1).getSymbol();
            line = ctx.INTEGER(2).getSymbol();
            column = ctx.INTEGER(3).getSymbol();
            endLine = ctx.INTEGER(4).getSymbol();
            endColumn = ctx.INTEGER(5).getSymbol();
        }
        SourceInformation sourceInfo = null;
        if (file != null)
        {
            if (addLines)
            {
                throw new RuntimeException("Adding debugging info should only be done by the system (when m3 is generated...)");
            }
            sourceInfo = new SourceInformation(file.getText().substring(2), Integer.parseInt(startLine.getText()), Integer.parseInt(startColumn.getText()), Integer.parseInt(line.getText()), Integer.parseInt(column.getText()), Integer.parseInt(endLine.getText()), Integer.parseInt(endColumn.getText()));
        }
        if (ctx.identifier() != null)
        {
            this.checkExists(ctx.qualifiedName().size() == 1 ? null : ctx.qualifiedName(1), ctx.identifier(), sourceInfo);
        }

        CoreInstance classifier = this.processorSupport.package_getByUserPath(this.getQualifiedNameString(ctx.qualifiedName(0)));
        CoreInstance instance = ctx.identifier() == null ? this.repository.newAnonymousCoreInstance(sourceInfo, classifier) : this.repository.newCoreInstance(ctx.identifier().getText(), classifier, sourceInfo);

        if (topLevel)
        {
            MutableList<IdentifierContext> packagePath = Lists.mutable.empty();
            if (ctx.qualifiedName().size() > 1)
            {
                if (ctx.qualifiedName(1).packagePath() != null)
                {
                    packagePath.addAll(ctx.qualifiedName(1).packagePath().identifier());
                }
                packagePath.add(ctx.qualifiedName(1).identifier());
            }
            Instance.addValueToProperty(buildPackage(packagePath, this.repository, this.sourceInformation), M3Properties.children, instance, this.processorSupport);
        }
        if (ctx.instancePropertyAssignment() != null)
        {
            for (InstancePropertyAssignmentContext ipaCtx : ctx.instancePropertyAssignment())
            {
                this.instanceParserPropertyAssignment(ipaCtx, propertyValues, wrapFlag, importId, addLines, space, useImportStubsInInstanceParser);
            }
        }
        if (file == null && addLines)
        {
            sourceInfo = this.sourceInformation.getPureSourceInformation(start, ctx.qualifiedName(0).identifier().getStart(), end);
            instance.setSourceInformation(sourceInfo);
        }

        if (!Iterate.isEmpty(renderedTypeArguments) || !Iterate.isEmpty(renderedMultiplicityArguments))
        {
            GenericTypeInstance genericTypeInstance = GenericTypeInstance.createPersistent(this.repository, sourceInfo);
            if (classifier instanceof Type)
            {
                genericTypeInstance._rawTypeCoreInstance(classifier);
            }
            else
            {
                ImportStubInstance is = ImportStubInstance.createPersistent(this.repository, this.getQualifiedNameString(ctx.qualifiedName(0)), importId);
                genericTypeInstance._rawTypeCoreInstance(is);
            }
            genericTypeInstance._typeArguments(renderedTypeArguments);
            genericTypeInstance._multiplicityArguments(renderedMultiplicityArguments);
            Instance.setValueForProperty(instance, "classifierGenericType", genericTypeInstance, this.processorSupport);
        }

        for (String propertyName : propertyValues.keysView())
        {
            Instance.setValuesForProperty(instance, propertyName, propertyValues.get(propertyName), this.processorSupport);
        }
        return instance;
    }

    private void instanceParserPropertyAssignment(InstancePropertyAssignmentContext ctx, MutableMap<String, ListIterable<CoreInstance>> propertyValues, boolean wrapFlag, ImportGroup importId, boolean addLines, String space, boolean useImportStubsInInstanceParser) throws PureParserException
    {
        ListIterable<CoreInstance> values = this.instanceParserRightSide(ctx.instanceRightSide(), wrapFlag, importId, addLines, space, useImportStubsInInstanceParser);
        propertyValues.put(ctx.propertyName().getText(), values);
    }

    private ListIterable<CoreInstance> instanceParserRightSide(InstanceRightSideContext ctx, boolean wrapFlag, ImportGroup importId, boolean addLines, String space, boolean useImportStubsInInstanceParser) throws PureParserException
    {
        MutableList<CoreInstance> values = Lists.mutable.of();
        if (ctx.instanceAtomicRightSideScalar() != null)
        {
            values.add(this.instanceParserAtomicRightSide(ctx.instanceAtomicRightSideScalar().instanceAtomicRightSide(), wrapFlag, importId, addLines, space, useImportStubsInInstanceParser));
        }
        else
        {
            for (InstanceAtomicRightSideContext iARSCtx : ctx.instanceAtomicRightSideVector().instanceAtomicRightSide())
            {
                values.add(this.instanceParserAtomicRightSide(iARSCtx, wrapFlag, importId, addLines, space, useImportStubsInInstanceParser));
            }
        }
        return values;
    }

    private CoreInstance instanceParserAtomicRightSide(InstanceAtomicRightSideContext ctx, boolean wrapFlag, ImportGroup importId, boolean addLines, String space, boolean useImportStubsInInstanceParser) throws PureParserException
    {
        if (ctx.instanceLiteral() != null)
        {
            return this.instanceLiteral(ctx.instanceLiteral());
        }
        if (ctx.LATEST_DATE() != null)
        {
            return this.repository.newLatestDateCoreInstance();
        }
        if (ctx.instance() != null)
        {
            return this.instanceParser(ctx.instance(), wrapFlag, importId, addLines, space, false, useImportStubsInInstanceParser);
        }
        if (ctx.qualifiedName() != null)
        {
            return useImportStubsInInstanceParser ?
                    ImportStubInstance.createPersistent(this.repository, this.getQualifiedNameString(ctx.qualifiedName()), importId) :
                    org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.findPackageableElement(this.getQualifiedNameString(ctx.qualifiedName()), this.repository);
        }
        if (ctx.enumReference() != null)
        {
            String enumeration = this.getQualifiedNameString(ctx.enumReference().qualifiedName());
            String enumValue = ctx.enumReference().identifier().getText();
            return useImportStubsInInstanceParser ?
                    EnumStubInstance.createPersistent(this.repository, enumValue, ImportStubInstance.createPersistent(this.repository, enumeration, importId)) :
                    findEnum(enumeration, enumValue, this.repository);
        }
        if (ctx.stereotypeReference() != null)
        {
            return ImportStubInstance.createPersistent(this.repository, this.getQualifiedNameString(ctx.stereotypeReference().qualifiedName()) + "@" + ctx.stereotypeReference().identifier().getText(), importId);
        }
        if (ctx.tagReference() != null)
        {
            return ImportStubInstance.createPersistent(this.repository, this.getQualifiedNameString(ctx.tagReference().qualifiedName()) + "%" + ctx.tagReference().identifier().getText(), importId);
        }
        //instanceReference
        return this.repository.newStringCoreInstance_cached(ctx.identifier().getText());
    }

    private CoreInstance instanceLiteral(InstanceLiteralContext ctx)
    {
        if (ctx.instanceLiteralToken() != null)
        {
            return this.instanceLiteralToken(ctx.instanceLiteralToken(), false);
        }
        String sign = (ctx.MINUS() == null) ? "+" : "-";
        if (ctx.INTEGER() != null)
        {
            return this.repository.newIntegerCoreInstance(sign + ctx.INTEGER().getText());
        }
        if (ctx.DECIMAL() != null)
        {
            return this.repository.newDecimalCoreInstance(sign + ctx.DECIMAL().getText());
        }
        return this.repository.newFloatCoreInstance(sign + ctx.FLOAT().getText());
    }

    private Any lambdaPipe(LambdaPipeContext ctx, Token firstToken, ListIterable<VariableExpression> params, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        Token lambdaStartToken = firstToken != null ? firstToken : ctx.PIPE().getSymbol();
        ListIterable<ValueSpecification> block = codeBlock(ctx.codeBlock(), typeParametersNames, multiplicityParameterNames, importId, lambdaContext, addLines, spacePlusTabs(space, 6));

        SourceInformation typeSourceInfo = this.sourceInformation.getPureSourceInformation(lambdaStartToken);
        FunctionType signature = FunctionTypeInstance.createPersistent(this.repository, typeSourceInfo, null, null);
        if (Iterate.notEmpty(params))
        {
            signature._parameters(params);
        }
        // Note: we cannot set the function of the signature FunctionType, as this can cause stack overflow if serializing to M4

        GenericType genericTypeInstance = GenericTypeInstance.createPersistent(this.repository, typeSourceInfo)
                ._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.LambdaFunction))
                ._typeArguments(Lists.mutable.with(GenericTypeInstance.createPersistent(this.repository, typeSourceInfo)._rawType(signature)));

        LambdaFunction<?> lambdaFunction = LambdaFunctionInstance.createPersistent(this.repository, lambdaContext.getLambdaFunctionUniqueName(), this.sourceInformation.getPureSourceInformation(lambdaStartToken, lambdaStartToken, ctx.getStop()))
                ._classifierGenericType(genericTypeInstance);
        lambdaFunction._expressionSequence(block);

        return wrapFlag ? this.doWrap(lambdaFunction, lambdaStartToken) : lambdaFunction;
    }

    private ListIterable<ValueSpecification> codeBlock(CodeBlockContext ctx, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, ImportGroup importId, LambdaContext lambdaContext, boolean addLines, String space)
    {
        String newSpace = space + "  ";
        return ListIterate.collect(ctx.programLine(), plCtx -> programLine(plCtx, M3Properties.line, typeParametersNames, multiplicityParameterNames, lambdaContext, importId, addLines, newSpace));
    }

    private ValueSpecification programLine(ProgramLineContext ctx, String exprName, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, ImportGroup importId, boolean addLines, String space)
    {
        if (ctx.combinedExpression() != null)
        {
            return (ValueSpecification) this.combinedExpression(ctx.combinedExpression(), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, true, importId, addLines);
        }
        else
        {
            return this.letExpression(ctx.letExpression(), typeParametersNames, multiplicityParameterNames, importId, lambdaContext, addLines, space);
        }
    }

    private SimpleFunctionExpression letExpression(LetExpressionContext ctx, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, ImportGroup importId, LambdaContext lambdaContext, boolean addLines, String space)
    {
        CoreInstance result = this.combinedExpression(ctx.combinedExpression(), "", typeParametersNames, multiplicityParameterNames, lambdaContext, spacePlusTabs(space, 4), true, importId, addLines);
        SourceInformation instanceValueSourceInfo = this.sourceInformation.getPureSourceInformation(ctx.identifier().getStart());
        InstanceValue iv = InstanceValueInstance.createPersistent(this.repository, instanceValueSourceInfo, null, null);

        iv._values(Lists.mutable.of(this.repository.newStringCoreInstance_cached(ctx.identifier().getText())));
        GenericTypeInstance gt = GenericTypeInstance.createPersistent(this.repository, instanceValueSourceInfo);
        Type ti = (Type) this.processorSupport.package_getByUserPath("String");
        gt._rawTypeCoreInstance(ti);
        iv._genericType(gt);
        iv._multiplicity(this.getPureOne());
        SimpleFunctionExpression sfe = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.LET().getSymbol()), null, null, importId, null);
        sfe._functionName("letFunction");
        sfe._parametersValues(Lists.mutable.of(iv, (ValueSpecification) result));
        return sfe;
    }

    private VariableExpression lambdaParam(LambdaParamTypeContext ctx, IdentifierContext var, MutableList<String> typeParametersNames, String space, ImportGroup importId)
    {
        GenericType type = null;
        Multiplicity multiplicity = null;
        if (ctx != null)
        {
            type = this.type(ctx.type(), typeParametersNames, spacePlusTabs(space, 3), importId, false);
            multiplicity = this.buildMultiplicity(ctx.multiplicity().multiplicityArgument());
        }
        return VariableExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(var.getStart()), type, multiplicity, var.getText());
    }

    private SimpleFunctionExpression allOrFunction(AllOrFunctionContext ctx, MutableList<? extends ValueSpecification> params, QualifiedNameContext funcName, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, ImportGroup importId, boolean addLines)
    {
        if (ctx.allFunction() != null)
        {
            return SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.allFunction().getStart()), null, null, importId, null)
                    ._functionName("getAll")
                    ._parametersValues(params);
        }

        if (ctx.allVersionsFunction() != null)
        {
            return SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.allVersionsFunction().getStart()), null, null, importId, null)
                    ._functionName("getAllVersions")
                    ._parametersValues(params);
        }

        if (ctx.allVersionsInRangeFunction() != null)
        {
            MutableList<ValueSpecification> milestoningParams = this.buildMilestoningVariableExpression(ctx.allVersionsInRangeFunction().buildMilestoningVariableExpression());
            return SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.allVersionsInRangeFunction().getStart()), null, null, importId, null)
                    ._functionName("getAllVersionsInRange")
                    ._parametersValues(Lists.mutable.<ValueSpecification>withAll(params).withAll(milestoningParams));
        }

        if (ctx.allFunctionWithMilestoning() != null)
        {
            MutableList<ValueSpecification> milestoningParams = this.buildMilestoningVariableExpression(ctx.allFunctionWithMilestoning().buildMilestoningVariableExpression());
            return SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.allFunctionWithMilestoning().getStart()), null, null, importId, null)
                    ._functionName("getAll")
                    ._parametersValues(Lists.mutable.<ValueSpecification>withAll(params).withAll(milestoningParams));
        }

        MutableList<ValueSpecification> parameters = this.functionExpressionParameters(ctx.functionExpressionParameters(), typeParametersNames, multiplicityParameterNames, importId, lambdaContext, addLines, space);
        return this.functionExpression(funcName, parameters, importId);
    }

    private MutableList<ValueSpecification> buildMilestoningVariableExpression(List<BuildMilestoningVariableExpressionContext> ctxs)
    {
        return ListIterate.collect(ctxs, ctx ->
        {
            if (ctx.DATE() != null)
            {
                return InstanceValueInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.DATE().getSymbol()), null, null)
                        ._values(Lists.mutable.of(this.repository.newDateCoreInstance(ctx.DATE().getText())));
            }
            if (ctx.LATEST_DATE() != null)
            {
                return InstanceValueInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.LATEST_DATE().getSymbol()), null, null)
                        ._values(Lists.mutable.of(this.repository.newLatestDateCoreInstance()));
            }
            return variable(ctx.variable());
        });
    }

    private VariableExpression variable(VariableContext ctx)
    {
        return VariableExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.identifier().getStart()), null, null, ctx.identifier().getText());
    }

    private MutableList<ValueSpecification> functionExpressionParameters(FunctionExpressionParametersContext ctx, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, ImportGroup importId, LambdaContext lambdaContext, boolean addLines, String space)
    {
        MutableList<ValueSpecification> parameters = Lists.mutable.empty();
        for (CombinedExpressionContext ceCtx : ctx.combinedExpression())
        {
            parameters.add((ValueSpecification) this.combinedExpression(ceCtx, "param", typeParametersNames, multiplicityParameterNames, lambdaContext, spacePlusTabs(space, 4), true, importId, addLines));
        }
        return parameters;
    }

    /**
     * Parse the instantiation of an instance of UNIT.
     */
    private InstanceValue unitInstanceParser(UnitInstanceContext ctx, ImportGroup importId)
    {
        UnitInstanceLiteralContext uctx = ctx.unitInstanceLiteral();
        boolean negative = uctx.MINUS() != null;
        PrimitiveCoreInstance<?> value;
        if (uctx.INTEGER() != null)
        {
            value = this.repository.newIntegerCoreInstance((negative ? "-" : "") + uctx.INTEGER().getText());
        }
        else if (uctx.DECIMAL() != null)
        {
            value = this.repository.newDecimalCoreInstance((negative ? "-" : "") + uctx.DECIMAL().getText());
        }
        else
        {
            value = this.repository.newFloatCoreInstance((negative ? "-" : "") + uctx.FLOAT().getText());
        }

        SourceInformation typeSourceInfo = this.sourceInformation.getPureSourceInformation(ctx.unitName().identifier().getStart());
        GenericType genericType = GenericTypeInstance.createPersistent(this.repository, typeSourceInfo)
                ._rawTypeCoreInstance(ImportStubInstance.createPersistent(this.repository, typeSourceInfo, getUnitNameWithMeasure(ctx.unitName()), importId));

        SourceInformation instanceValueSourceInfo = this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.getStart(), ctx.getStop(), true);
        InstanceValue iv = InstanceValueInstance.createPersistent(this.repository, instanceValueSourceInfo, genericType, getPureOne())
                ._genericType(genericType)
                ._multiplicity(getPureOne())
                ._values(Lists.immutable.with(value));
        return InstanceValueInstance.createPersistent(this.repository, instanceValueSourceInfo, genericType, getPureOne())
                ._genericType(genericType)
                ._multiplicity(getPureOne())
                ._values(Lists.immutable.with(iv));
    }

    private String getUnitNameWithMeasure(UnitNameContext ctx)
    {
        QualifiedNameContext measureQualifiedName = ctx.qualifiedName();
        StringBuilder builder = new StringBuilder();
        if (measureQualifiedName.packagePath() != null)
        {
            appendPackage(builder, measureQualifiedName.packagePath()).append("::");
        }
        builder.append(measureQualifiedName.identifier().getText()).append(org.finos.legend.pure.m3.navigation.importstub.ImportStub.UNIT_STUB_DELIM).append(ctx.identifier().getText());
        return builder.toString();
    }

    private SimpleFunctionExpression expressionInstanceParser(ExpressionInstanceContext ctx, MutableList<String> typeParametersNames, LambdaContext lambdaContext, ImportGroup importId, boolean addLines, String space)
    {
        ListIterable<GenericType> renderedTypeArguments = null;
        ListIterable<Multiplicity> renderedMultiplicityArguments = null;
        ListIterable<InstanceValue> renderedTypeVariableValues = null;
        MutableList<CoreInstance> keyExpressions = Lists.mutable.empty();
        Token end = ctx.getStop();
        if (ctx.expressionInstanceParserPropertyAssignment() != null)
        {
            for (ExpressionInstanceParserPropertyAssignmentContext propCtx : ctx.expressionInstanceParserPropertyAssignment())
            {
                keyExpressions.add(this.expressionInstanceParserPropertyAssignment(propCtx, typeParametersNames, importId, addLines, spacePlusTabs(space, 8), lambdaContext));
            }
        }
        renderedTypeVariableValues = this.processTypeVariableValues(ctx.typeVariableValues());
        if (ctx.typeArguments() != null)
        {
            renderedTypeArguments = this.typeArguments(ctx.typeArguments(), typeParametersNames, importId, addLines);
        }
        if (ctx.multiplicityArguments() != null)
        {
            renderedMultiplicityArguments = this.multiplicityArguments(ctx.multiplicityArguments());
        }
        SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(ctx.NEW_SYMBOL().getSymbol(), ctx.identifier() == null ? ctx.NEW_SYMBOL().getSymbol() : ctx.identifier().getStart(), end);
        SimpleFunctionExpressionInstance sfei = SimpleFunctionExpressionInstance.createPersistent(this.repository, sourceInfo, null, null, importId, null);
        sfei._functionName(ctx.variable() == null ? "new" : "copy");
        MutableList<ValueSpecification> paramValues = Lists.mutable.of();

        if (ctx.variable() == null)
        {
            GenericTypeInstance genericTypeInstance = GenericTypeInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.NEW_SYMBOL().getSymbol()));
            Type type = (Type) this.processorSupport.package_getByUserPath(M3Paths.Class);
            genericTypeInstance._rawTypeCoreInstance(type);
            GenericTypeInstance ta = GenericTypeInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.qualifiedName().getStart(), ctx.qualifiedName().getStop(), ctx.qualifiedName().getStop()));
            ImportStubInstance is = ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.qualifiedName().getStart(), ctx.qualifiedName().getStop(), ctx.qualifiedName().getStop()), this.getQualifiedNameString(ctx.qualifiedName()), importId);
            ta._rawTypeCoreInstance(is);

            if (renderedTypeArguments != null)
            {
                ta._typeArguments(renderedTypeArguments);
            }
            if (renderedTypeVariableValues != null)
            {
                ta._typeVariableValues(renderedTypeVariableValues);
            }
            if (renderedMultiplicityArguments != null)
            {
                ta._multiplicityArguments(renderedMultiplicityArguments);
            }

            genericTypeInstance._typeArguments(Lists.mutable.<GenericType>of(ta));
            InstanceValueInstance iv = InstanceValueInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.qualifiedName().identifier().getStart()), genericTypeInstance, this.getPureOne());
            //todo: shouldn't be necessary
            iv._values(Lists.fixedSize.<CoreInstance>empty());
            paramValues.add(iv);
        }
        else
        {
            VariableExpressionInstance vei = VariableExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.variable().getStart()), null, null, ctx.variable().identifier().getText());
            paramValues.add(vei);
        }

        InstanceValue nameCi = ctx.identifier() == null ? this.doWrap(Lists.mutable.of(this.repository.newStringCoreInstance_cached(""))) : this.doWrap(ctx.identifier(), false);
        paramValues.add(nameCi);

        if (!keyExpressions.isEmpty())
        {
            InstanceValue keyExpr = this.doWrap(keyExpressions);
            paramValues.add(keyExpr);
        }

        sfei._parametersValues(paramValues);
        return sfei;
    }

    private CoreInstance expressionInstanceParserPropertyAssignment(ExpressionInstanceParserPropertyAssignmentContext ctx, MutableList<String> typeParametersNames, ImportGroup importId, boolean addLines, String space, LambdaContext lambdaContext)
    {
        MutableList<PropertyNameContext> properties = Lists.mutable.withAll(ctx.propertyName());
        CoreInstance result = this.expressionInstanceParserRightSide(ctx.expressionInstanceRightSide(), typeParametersNames, importId, lambdaContext, addLines, space);
        InstanceValue instanceVal = properties.size() == 1 ? this.doWrap(properties.getFirst()) : this.doWrap(properties);
        return KeyExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.EQUAL().getSymbol()), (ValueSpecification) result, instanceVal)
                ._add(ctx.PLUS() != null);
    }

    private CoreInstance expressionInstanceParserAtomicRightSide(ExpressionInstanceAtomicRightSideContext ctx, MutableList<String> typeParametersNames, ImportGroup importId, LambdaContext lambdaContext, boolean addLines, String space)
    {
        if (ctx.combinedExpression() != null)
        {
            return this.combinedExpression(ctx.combinedExpression(), "", Lists.mutable.empty(), Lists.mutable.empty(), lambdaContext, spacePlusTabs(space, 4), true, importId, addLines);
        }
        if (ctx.expressionInstance() != null)
        {
            return this.expressionInstanceParser(ctx.expressionInstance(), typeParametersNames, lambdaContext, importId, addLines, space);
        }
        return this.processorSupport.package_getByUserPath(this.getQualifiedNameString(ctx.qualifiedName()));
    }

    private CoreInstance expressionInstanceParserRightSide(ExpressionInstanceRightSideContext ctx, MutableList<String> typeParametersNames, ImportGroup importId, LambdaContext lambdaContext, boolean addLines, String space)
    {
        return this.expressionInstanceParserAtomicRightSide(ctx.expressionInstanceAtomicRightSide(), typeParametersNames, importId, lambdaContext, addLines, space);
    }

    private SimpleFunctionExpression equalNotEqual(EqualNotEqualContext ctx, ValueSpecification input, String exprName, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        SimpleFunctionExpression result = null;
        CoreInstance other;
        if (ctx.TEST_EQUAL() != null)
        {
            other = this.combinedArithmeticOnly(ctx.combinedArithmeticOnly(), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
            result = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.TEST_EQUAL().getSymbol()), null, null, importId, null);
            result._functionName("equal");
            result._parametersValues(Lists.mutable.of(input, (ValueSpecification) other));
        }
        else if (ctx.TEST_NOT_EQUAL() != null)
        {
            other = this.combinedArithmeticOnly(ctx.combinedArithmeticOnly(), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
            SimpleFunctionExpressionInstance inner = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.TEST_NOT_EQUAL().getSymbol()), null, null, importId, null);
            inner._functionName("equal");
            inner._parametersValues(Lists.mutable.of(input, (ValueSpecification) other));

            result = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.TEST_NOT_EQUAL().getSymbol()), null, null, importId, null);
            result._functionName("not");
            result._parametersValues(Lists.mutable.<ValueSpecification>of(inner));
        }
        return result;
    }

    private CoreInstance combinedArithmeticOnly(CombinedArithmeticOnlyContext ctx, String exprName, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        CoreInstance result = this.expressionOrExpressionGroup(ctx.expressionOrExpressionGroup(), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
        if (Iterate.notEmpty(ctx.arithmeticPart()))
        {
            return this.arithmeticPart(ctx.arithmeticPart(), result, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
        }

        return result;
    }

    enum ArithOp
    {
        PLUS,
        MINUS,
        TIMES,
        DIVIDE,
        LESSTHAN,
        LESSTHANEQUAL,
        GREATERTHAN,
        GREATERTHANEQUAL
    }

    // Build arithmetic op which can handle list of params (like lisp eg (+ u v z y z))
    private SimpleFunctionExpression buildArithmeticWithListParam(ArithmeticPartContext ctx, ArithOp op, CoreInstance initialValue, String exprName, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        MutableList<CoreInstance> others = Lists.mutable.empty();

        Function0<List<TerminalNode>> getTokens;
        Function<Integer, TerminalNode> getToken;

        switch (op)
        {
            case PLUS:
                getTokens = ctx::PLUS;
                getToken = ctx::PLUS;
                break;
            case TIMES:
                getTokens = ctx::STAR;
                getToken = ctx::STAR;
                break;
            case MINUS:
                getTokens = ctx::MINUS;
                getToken = ctx::MINUS;
                break;
            default:
                throw new IllegalStateException("Unexpected arithmetic operation for buildArithmeticWithListParam: " + op);
        }

        String op_str = op.toString().toLowerCase();

        for (ExpressionContext eCtx : ctx.expression())
        {
            others.add(this.expression(eCtx, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines));
        }
        TerminalNode newOp = getToken.apply(getTokens.value().size() - 1);
        SimpleFunctionExpression sfe = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(newOp.getSymbol()), null, null, importId, null);
        sfe._functionName(op_str);
        sfe._parametersValues(Lists.mutable.<ValueSpecification>of(this.doWrap(Lists.mutable.with(initialValue).withAll(others))));
        return sfe;
    }

    // Handles divide, since dive is built up in a tree: eg x / y /z is div(x, div(y,z))
    private SimpleFunctionExpression buildArithmeticDivide(ArithmeticPartContext ctx, ArithOp op, CoreInstance initialValue, String exprName, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        MutableList<CoreInstance> others = Lists.mutable.empty();

        for (ExpressionContext eCtx : ctx.expression())
        {
            others.add(this.expression(eCtx, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines));
        }
        SimpleFunctionExpression sfe = null;
        for (CoreInstance other : others)
        {
            sfe = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.DIVIDE(ctx.DIVIDE().size() - 1).getSymbol()), null, null, importId, null);
            sfe._functionName("divide");
            sfe._parametersValues(Lists.mutable.of((ValueSpecification) initialValue, (ValueSpecification) other));
            initialValue = sfe;
        }
        return sfe;
    }

    private SimpleFunctionExpression buildComparisonOp(ArithmeticPartContext ctx, ArithOp op, CoreInstance initialValue, String exprName, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        String op_str;

        Function0<TerminalNode> getToken;

        switch (op)
        {
            case LESSTHAN:
                getToken = ctx::LESSTHAN;
                op_str = "lessThan";
                break;
            case LESSTHANEQUAL:
                getToken = ctx::LESSTHANEQUAL;
                op_str = "lessThanEqual";
                break;
            case GREATERTHAN:
                getToken = ctx::GREATERTHAN;
                op_str = "greaterThan";
                break;
            case GREATERTHANEQUAL:
                getToken = ctx::GREATERTHANEQUAL;
                op_str = "greaterThanEqual";
                break;

            default:
                throw new IllegalStateException("Unexpected arithmetic operation for buildComparisonOp: " + op);
        }

        CoreInstance other = this.expression(ctx.expression(0), exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
        SimpleFunctionExpression sfe = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(getToken.value().getSymbol()), null, null, importId, null);
        sfe._functionName(op_str);
        sfe._parametersValues(Lists.mutable.of((ValueSpecification) initialValue, (ValueSpecification) other));
        return sfe;
    }

    private boolean isAdditiveOp(String op)
    {
        return op.equals("plus") || op.equals("minus");
    }

    private boolean isProductOp(String op)
    {
        return op.equals("times") || op.equals("star") || op.equals("divide");
    }

    private boolean isRelationalComparison(String operator)
    {
        return "lessThan".equals(operator) || "lessThanEqual".equals(operator) || "greaterThan".equals(operator) || "greaterThanEqual".equals(operator);
    }

    private boolean isStrictlyLowerPrecendence(String operator1, String operator2)
    {
        return (isRelationalComparison(operator1) && (isAdditiveOp(operator2) || isProductOp(operator2)))
                || (isAdditiveOp(operator1) && isProductOp(operator2));
    }

    private ListIterable<? extends CoreInstance> getParams(SimpleFunctionExpression sfe)
    {
        boolean isValueSpecificationFunc = "divide".equals(sfe.getValueForMetaPropertyToOne("functionName").getName()) || this.isRelationalComparison(sfe.getValueForMetaPropertyToOne("functionName").getName());
        if (isValueSpecificationFunc)
        {
            return sfe.getValueForMetaPropertyToMany("parametersValues");
        }
        else
        {
            return sfe.getValueForMetaPropertyToOne("parametersValues").getValueForMetaPropertyToMany("values");
        }
    }

    private interface BuildArithmeticExpression
    {
        SimpleFunctionExpression build(ArithmeticPartContext ctx, ArithOp op, CoreInstance initialValue, String exprName, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines);
    }

    // Antlr grammar as currently defined does not handle precedence for arithmetic ops
    // We take care of precedence here. 
    // Intuition: if we are processing an expression and the previous expression is of lower precedence, we 'snatch' the last argument from the previous expression and make it part of the current one
    // For example: 1 + 2 * 4. The grammar will have led us to build plus(1,2). When looking at the multiplication, the expression should snatch 2, and replace it with mult(2,4), 
    // so we end up with plus(1, mult(2,4))
    private SimpleFunctionExpression processOp(BuildArithmeticExpression builder, SimpleFunctionExpression sfe, ArithmeticPartContext ctx, ArithOp op, CoreInstance initialValue, String exprName, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameters, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        String opStr = op.name().toLowerCase();
        // Case where we are building from scratch
        if (sfe == null)
        {
            return builder.build(ctx, op, initialValue, exprName, typeParametersNames, multiplicityParameters, lambdaContext, space, wrapFlag, importId, addLines);
        }
        //Case where we are in the middle of an expression, and currently looking at something of higher precedence than previous expression
        //Some processing to replace the last argument of the previous expression with the current expression (where current expression
        //has the last param as it's initial parameter).
        if (isStrictlyLowerPrecendence(sfe.getValueForMetaPropertyToOne("functionName").getName(), opStr))
        {
            ListIterable<? extends CoreInstance> params = getParams(sfe);
            SimpleFunctionExpression newSfe = builder.build(ctx, op, params.getLast(), exprName, typeParametersNames, multiplicityParameters, lambdaContext, space, wrapFlag, importId, addLines);
            MutableList<CoreInstance> l = Lists.mutable.withAll(params.subList(0, params.size() - 1));
            // division and relational ops handle parameter values differently.
            if ("divide".equals(sfe._functionName()) || isRelationalComparison(sfe._functionName()))
            {
                sfe._parametersValues(Lists.mutable.of((ValueSpecification) l.get(0), newSfe));
            }
            else
            {
                sfe._parametersValues(Lists.mutable.<ValueSpecification>of(this.doWrap(l.with(newSfe))));
            }
            return sfe;
        }
        // Case where are in the middle of an expression, but currently looking at something of lower or equal precedence
        // Add the previously processed expression as the initial argument to this expression 
        return builder.build(ctx, op, sfe, exprName, typeParametersNames, multiplicityParameters, lambdaContext, space, wrapFlag, importId, addLines);
    }

    private SimpleFunctionExpression arithmeticPart(List<ArithmeticPartContext> aList, CoreInstance result, String exprName, MutableList<String> typeParametersNames, MutableList<String> multiplicityParameterNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        SimpleFunctionExpression sfe = null;
        for (ArithmeticPartContext ctx : aList)
        {
            if (Iterate.notEmpty(ctx.PLUS()))
            {
                sfe = processOp(this::buildArithmeticWithListParam, sfe, ctx, ArithOp.PLUS, result, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            else if (Iterate.notEmpty(ctx.STAR()))
            {
                sfe = processOp(this::buildArithmeticWithListParam, sfe, ctx, ArithOp.TIMES, result, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            else if (Iterate.notEmpty(ctx.MINUS()))
            {
                sfe = processOp(this::buildArithmeticWithListParam, sfe, ctx, ArithOp.MINUS, result, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            else if (Iterate.notEmpty(ctx.DIVIDE()))
            {
                sfe = processOp(this::buildArithmeticDivide, sfe, ctx, ArithOp.DIVIDE, result, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            // Relational comparison ops are of the lowest precedence, so no need to see if the expression needs to 'snatch' the last argument from the previous expression
            else if (ctx.LESSTHAN() != null)
            {
                sfe = buildComparisonOp(ctx, ArithOp.LESSTHAN, sfe == null ? result : sfe, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            else if (ctx.LESSTHANEQUAL() != null)
            {
                sfe = buildComparisonOp(ctx, ArithOp.LESSTHANEQUAL, sfe == null ? result : sfe, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            else if (ctx.GREATERTHAN() != null)
            {
                sfe = buildComparisonOp(ctx, ArithOp.GREATERTHAN, sfe == null ? result : sfe, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            else if (ctx.GREATERTHANEQUAL() != null)
            {
                sfe = buildComparisonOp(ctx, ArithOp.GREATERTHANEQUAL, sfe == null ? result : sfe, exprName, typeParametersNames, multiplicityParameterNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
        }
        return sfe;
    }


    public ListIterable<InstanceValue> processTypeVariableValues(M3Parser.TypeVariableValuesContext ctx)
    {
        return ctx == null ? Lists.mutable.empty() : ListIterate.collect(ctx.instanceLiteral(), x ->
        {
            InstanceValue iv = doWrap(instanceLiteral(x), x.getStart());
            InstanceValueProcessor.updateInstanceValue(iv, processorSupport);
            return iv;
        });
    }

    public GenericType type(TypeContext ctx, MutableList<String> typeParametersNames, String space, ImportGroup importId, boolean addLines)
    {
        if (ctx.qualifiedName() != null)
        {
            ListIterable<GenericType> renderedTypeArguments = this.typeArguments(ctx.typeArguments(), typeParametersNames, importId, addLines);
            ListIterable<Multiplicity> renderedMultiplicityArguments = this.multiplicityArguments(ctx.multiplicityArguments());
            ListIterable<InstanceValue> renderedTypeVariableValues = this.processTypeVariableValues(ctx.typeVariableValues());
            return this.processType(ctx.qualifiedName(), typeParametersNames, renderedTypeArguments, renderedMultiplicityArguments, renderedTypeVariableValues, importId);
        }
        if (ctx.unitName() != null)
        {
            return this.processUnitType(ctx.unitName(), importId);
        }
        if (ctx.CURLY_BRACKET_OPEN() != null)
        {
            GenericType returnType = this.type(ctx.type(), typeParametersNames, spacePlusTabs(space, 5), importId, addLines);
            Multiplicity returnMultiplicity = this.buildMultiplicity(ctx.multiplicity().multiplicityArgument());
            SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.getStart(), ctx.getStop());
            FunctionTypeInstance functionTypeInstance = FunctionTypeInstance.createPersistent(this.repository, sourceInfo, returnMultiplicity, returnType);

            MutableList<VariableExpression> params = ListIterate.collect(ctx.functionTypePureType(), fCtx -> typeFunctionTypePureType(fCtx, typeParametersNames, space, importId, addLines));
            if (params.notEmpty())
            {
                functionTypeInstance._parameters(params);
            }
            GenericTypeInstance genericTypeInstance = GenericTypeInstance.createPersistent(this.repository);
            genericTypeInstance._rawTypeCoreInstance(functionTypeInstance);
            return genericTypeInstance;
        }
        if (ctx.GROUP_OPEN() != null)
        {
            GenericTypeInstance genericTypeInstance = GenericTypeInstance.createPersistent(this.repository);
            SourceInformation srcInfo = this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.getStart(), ctx.getStop());
            genericTypeInstance._rawTypeCoreInstance(
                    _RelationType.build(
                            ListIterate.collect(
                                    ctx.columnType(),
                                    c ->
                                    {
                                        M3Parser.ColumnNameContext colNameCtx = c.mayColumnName().columnName();
                                        String colName = colNameCtx != null ? removeQuotes(colNameCtx.getText()) : "";
                                        return _Column.getColumnInstance(
                                                c.mayColumnName().QUESTION() != null ? "" : colName,
                                                c.mayColumnName().QUESTION() != null,
                                                c.mayColumnType().QUESTION() != null ? GenericTypeInstance.createPersistent(this.repository) : this.type(c.mayColumnType().type(), typeParametersNames, spacePlusTabs(space, 5), importId, addLines),
                                                c.multiplicity() == null ?
                                                        (Multiplicity) org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.newMultiplicity(0, 1, processorSupport) :
                                                        this.buildMultiplicity(c.multiplicity().multiplicityArgument()),
                                                srcInfo,
                                                processorSupport
                                        );
                                    }
                            ), srcInfo, processorSupport
                    )
            );
            return genericTypeInstance;
        }
        throw new PureParserException(this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.getStart(), ctx.getStop()), "Type not understood");
    }

    private SimpleFunctionExpression functionExpression(QualifiedNameContext funcName, MutableList<ValueSpecification> parameters, ImportGroup importId)
    {
        SimpleFunctionExpressionInstance result = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(funcName.identifier().getStart()), null, null, importId, null);
        result._functionName(this.getQualifiedNameString(funcName));
        result._parametersValues(parameters);
        return result;
    }

    private CoreInstance enumValue(EnumValueContext ctx, Enumeration<?> enumeration, ImportGroup importId)
    {
        CoreInstance enumValue = this.repository.newCoreInstance(ctx.identifier().getText(), enumeration, this.sourceInformation.getPureSourceInformation(ctx.identifier().getStart()), true);
        enumValue.addKeyValue(M3PropertyPaths.name_Enum, this.repository.newStringCoreInstance_cached(ctx.identifier().getText()));

        ListIterable<CoreInstance> stereotypes = (ctx.stereotypes() == null) ? Lists.immutable.empty() : stereotypes(ctx.stereotypes(), importId);
        if (stereotypes.notEmpty())
        {
            enumValue.setKeyValues(M3PropertyPaths.stereotypes, stereotypes);
        }
        ListIterable<TaggedValue> taggedValues = (ctx.taggedValues() == null) ? Lists.immutable.empty() : taggedValues(ctx.taggedValues(), importId);
        if (taggedValues.notEmpty())
        {
            enumValue.setKeyValues(M3PropertyPaths.taggedValues, taggedValues);
        }
        return enumValue;
    }

    private Enumeration<?> enumParser(EnumDefinitionContext ctx, ImportGroup importId)
    {
        checkExists(ctx.qualifiedName().packagePath(), ctx.qualifiedName().identifier(), null);

        SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.qualifiedName().identifier().getStart(), ctx.getStop());

        EnumerationInstance enumeration = EnumerationInstance.createPersistent(this.repository, ctx.qualifiedName().identifier().getText(), sourceInfo);
        enumeration._name(ctx.qualifiedName().identifier().getText());

        buildAndSetPackage(enumeration, ctx.qualifiedName().packagePath(), this.repository, this.sourceInformation);

        enumeration._classifierGenericType(GenericTypeInstance.createPersistent(this.repository, sourceInfo)
                ._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.Enumeration))
                ._typeArguments(Lists.immutable.with(GenericTypeInstance.createPersistent(this.repository, sourceInfo)._rawType(enumeration))));

        ListIterable<TaggedValue> taggedValues = (ctx.taggedValues() == null) ? Lists.immutable.empty() : taggedValues(ctx.taggedValues(), importId);
        if (taggedValues.notEmpty())
        {
            enumeration._taggedValues(taggedValues);
        }
        ListIterable<CoreInstance> stereotypes = (ctx.stereotypes() == null) ? Lists.immutable.empty() : stereotypes(ctx.stereotypes(), importId);
        if (stereotypes.notEmpty())
        {
            enumeration._stereotypesCoreInstance(stereotypes);
        }

        enumeration._generalizations(Lists.immutable.with(GeneralizationInstance.createPersistent(this.repository, sourceInfo, GenericTypeInstance.createPersistent(this.repository, sourceInfo)._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.Enum)), enumeration)));

        enumeration._values(ListIterate.collect(ctx.enumValue(), evCtx -> enumValue(evCtx, enumeration, importId)));
        return enumeration;
    }

    /**
     * Parses the measure given its definition context.
     * Returns the parsed measure as a CoreInstance.
     */
    private Measure measureParser(MeasureDefinitionContext ctx, ImportGroup importId) throws PureParserException
    {
        checkExists(ctx.qualifiedName().packagePath(), ctx.qualifiedName().identifier(), null);

        SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.qualifiedName().identifier().getStart(), ctx.getStop());
        String measureName = ctx.qualifiedName().identifier().getText();
        Measure measure = MeasureInstance.createPersistent(this.repository, measureName, sourceInfo)
                ._name(measureName)
                ._classifierGenericType(GenericTypeInstance.createPersistent(this.repository, sourceInfo)._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.Measure)));

        buildAndSetPackage(measure, ctx.qualifiedName().packagePath(), this.repository, this.sourceInformation);

        ListIterable<CoreInstance> stereotypes = (ctx.stereotypes() == null) ? Lists.immutable.empty() : stereotypes(ctx.stereotypes(), importId);
        if (stereotypes.notEmpty())
        {
            measure._stereotypesCoreInstance(stereotypes);
        }
        ListIterable<TaggedValue> taggedValues = (ctx.taggedValues() == null) ? Lists.immutable.empty() : taggedValues(ctx.taggedValues(), importId);
        if (taggedValues.notEmpty())
        {
            measure._taggedValues(taggedValues);
        }

        measure._generalizations(Lists.immutable.with(GeneralizationInstance.createPersistent(this.repository, sourceInfo, GenericTypeInstance.createPersistent(this.repository, sourceInfo)._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.DataType)), measure)));

        M3Parser.MeasureBodyContext measureBodyCtx = ctx.measureBody();
        if (measureBodyCtx.canonicalUnitExpr() != null)
        {
            // traditional canonical unit pattern
            measure._canonicalUnit(unitParser(measureBodyCtx.canonicalUnitExpr().unitExpr(), importId, measure));

            MutableList<Unit> nonCanonicalUnits = ListIterate.collect(measureBodyCtx.unitExpr(), unitCtx -> unitParser(unitCtx, importId, measure));
            if (nonCanonicalUnits.notEmpty())
            {
                measure._nonCanonicalUnits(nonCanonicalUnits);
            }
        }
        else
        {
            // non-convertible unit pattern
            MutableList<Unit> nonConvertibleUnits = ListIterate.collect(measureBodyCtx.nonConvertibleUnitExpr(), unitCtx -> nonConvertibleUnitParser(unitCtx, measure));
            measure._canonicalUnit(nonConvertibleUnits.get(0));
            if (nonConvertibleUnits.size() > 1)
            {
                measure._nonCanonicalUnits(Lists.immutable.withAll(nonConvertibleUnits.subList(1, nonConvertibleUnits.size())));
            }
        }
        return measure;
    }

    /**
     * Helps build the unitInstance for any canonical and non-canonical units and returns the parsed unitInstance.
     */
    @SuppressWarnings("unchecked")
    private Unit unitParser(UnitExprContext ctx, ImportGroup importId, Measure measure)
    {
        SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.identifier().getStart(), ctx.getStop());
        String unitName = ctx.identifier().getText();
        Unit unit = UnitInstance.createPersistent(this.repository, unitName, sourceInfo, measure)
                ._name(unitName)
                ._classifierGenericType(GenericTypeInstance.createPersistent(this.repository, sourceInfo)._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.Unit)));

        // set unit super type to be its measure (Kilogram -> Mass)
        Generalization generalization = GeneralizationInstance.createPersistent(this.repository, sourceInfo, GenericTypeInstance.createPersistent(this.repository, sourceInfo)._rawType(measure), unit);
        unit._generalizations(Lists.immutable.with(generalization));
        measure._specializationsAdd(generalization);

        // prepare lambda instance for the conversion function

        LambdaContext lambdaContext = new LambdaContext(org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(new StringBuilder(), measure, "_").append('~').append(unitName).toString());
        MutableList<String> typeParametersNames = Lists.mutable.empty();
        FunctionType conversionFuncType = FunctionTypeInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.unitConversionExpr().getStart(), ctx.unitConversionExpr().getStart(), ctx.unitConversionExpr().getStop()), null, null);

        // prepare params

        VariableExpression expr = lambdaParam(null, ctx.unitConversionExpr().identifier(), typeParametersNames, "", importId)
                ._multiplicity(getPureOne())
                ._functionTypeOwner(conversionFuncType)
                ._genericType(GenericTypeInstance.createPersistent(this.repository)._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.Number)));
        conversionFuncType._parameters(Lists.immutable.with(expr));

        GenericType conversionFuncClassifierGT = GenericTypeInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.unitConversionExpr().ARROW().getSymbol()))
                ._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.LambdaFunction))
                ._typeArguments(Lists.immutable.with(GenericTypeInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.unitConversionExpr().ARROW().getSymbol()))._rawType(conversionFuncType)));
        LambdaFunction<?> conversionFunc = LambdaFunctionInstance.createPersistent(this.repository, lambdaContext.getLambdaFunctionUniqueName(), this.sourceInformation.getPureSourceInformation(ctx.unitConversionExpr().ARROW().getSymbol()))
                ._classifierGenericType(conversionFuncClassifierGT)
                ._expressionSequence(codeBlock(ctx.unitConversionExpr().codeBlock(), typeParametersNames, Lists.mutable.empty(), importId, lambdaContext, this.addLines, tabs(6)));
        conversionFuncType._functionAdd(conversionFunc);

        unit._conversionFunction(conversionFunc);

        return unit;
    }

    private Unit nonConvertibleUnitParser(NonConvertibleUnitExprContext ctx, Measure measure)
    {
        SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.identifier().getStart(), ctx.getStop());
        String unitName = ctx.identifier().getText();
        Unit unitInstance = UnitInstance.createPersistent(this.repository, unitName, sourceInfo, measure)
                ._name(unitName)
                ._classifierGenericType(GenericTypeInstance.createPersistent(this.repository, sourceInfo)._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.Unit)));
        Generalization generalization = GeneralizationInstance.createPersistent(this.repository, sourceInfo, GenericTypeInstance.createPersistent(this.repository, sourceInfo)._rawType(measure), unitInstance);
        unitInstance._generalizations(Lists.immutable.with(generalization));
        measure._specializationsAdd(generalization);
        return unitInstance;
    }

    private CoreInstance primitiveParser(M3Parser.PrimitiveDefinitionContext ctx, ImportGroup importId)
    {
        this.checkExists(ctx.qualifiedName().packagePath(), ctx.qualifiedName().identifier(), null);

        String typeName = ctx.qualifiedName().identifier().getText();
        String fullName = this.getQualifiedNameString(ctx.qualifiedName());

        SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.qualifiedName().identifier().getStart(), ctx.getStop(), true);
        PrimitiveType primitiveType = PrimitiveTypeInstance.createPersistent(this.repository, typeName, sourceInfo)
                ._name(typeName)
                ._classifierGenericType(GenericTypeInstance.createPersistent(this.repository, sourceInfo)._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.PrimitiveType)))
                ._extended(true);

        buildAndSetPackage(primitiveType, ctx.qualifiedName().packagePath(), this.repository, this.sourceInformation);

        ListIterable<VariableExpression> typeVariables = (ctx.typeVariableParameters() == null) ?
                Lists.immutable.empty() :
                ListIterate.collect(ctx.typeVariableParameters().functionVariableExpression(), fveCtx -> functionVariableExpression(fveCtx, Lists.mutable.empty(), importId, spacePlusTabs("", 4)));
        if (typeVariables.notEmpty())
        {
            primitiveType._typeVariables(typeVariables);
        }

        GenericType superType = type(ctx.type(), Lists.mutable.empty(), "", importId, this.addLines);
        primitiveType._generalizationsAdd(GeneralizationInstance.createPersistent(this.repository, superType.getSourceInformation(), superType, primitiveType));
        ListIterable<CoreInstance> stereotypes = (ctx.stereotypes() == null) ? Lists.immutable.empty() : stereotypes(ctx.stereotypes(), importId);
        if (stereotypes.notEmpty())
        {
            primitiveType._stereotypesCoreInstance(stereotypes);
        }
        ListIterable<TaggedValue> taggedValues = (ctx.taggedValues() == null) ? Lists.immutable.empty() : taggedValues(ctx.taggedValues(), importId);
        if (taggedValues.notEmpty())
        {
            primitiveType._taggedValues(taggedValues);
        }

        LambdaContext lambdaContext = new LambdaContext(fullName.replace("::", "_"));
        MutableList<Constraint> constraints = constraints(primitiveType, ctx.constraints(), Lists.mutable.empty(), Lists.mutable.empty(), typeVariables, importId, lambdaContext, this.addLines);
        if (constraints.notEmpty())
        {
            primitiveType._constraints(constraints);
        }

        return primitiveType;
    }

    private CoreInstance classParser(ClassDefinitionContext ctx, ImportGroup importId, boolean addLines) throws PureParserException
    {
        checkExists(ctx.qualifiedName().packagePath(), ctx.qualifiedName().identifier(), null);

        SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.qualifiedName().identifier().getStart(), ctx.getStop());

        MutableList<String> typeParameterNames = Lists.mutable.empty();
        MutableList<Boolean> contravariants = Lists.mutable.empty();
        MutableList<String> multiplicityParameterNames = Lists.mutable.empty();
        if (ctx.typeParametersWithContravarianceAndMultiplicityParameters() != null)
        {
            this.typeParametersWithContravarianceAndMultiplicityParameters(ctx.typeParametersWithContravarianceAndMultiplicityParameters(), typeParameterNames, contravariants, multiplicityParameterNames);
        }

        ListIterable<CoreInstance> stereotypes = (ctx.stereotypes() == null) ? Lists.immutable.empty() : stereotypes(ctx.stereotypes(), importId);
        ListIterable<TaggedValue> taggedValues = (ctx.taggedValues() == null) ? Lists.immutable.empty() : taggedValues(ctx.taggedValues(), importId);
        if (ctx.projection() != null)
        {
            return projectionParser(ctx, importId, addLines, stereotypes, taggedValues);
        }

        String className = ctx.qualifiedName().identifier().getText();
        ClassInstance classInstance = ClassInstance.createPersistent(this.repository, className, sourceInfo);
        classInstance._name(className);

        ListIterable<VariableExpression> typeVariables = (ctx.typeVariableParameters() == null) ?
                Lists.immutable.empty() :
                ListIterate.collect(ctx.typeVariableParameters().functionVariableExpression(), fveCtx -> functionVariableExpression(fveCtx, Lists.mutable.empty(), importId, spacePlusTabs("", 4)));
        classInstance._typeVariables(typeVariables);

        buildAndSetPackage(classInstance, ctx.qualifiedName().packagePath(), this.repository, this.sourceInformation);

        String fullName = this.getQualifiedNameString(ctx.qualifiedName());
        ImportStub ownerType = ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.qualifiedName().identifier().getStart()), fullName, importId);

        LambdaContext lambdaContext = new LambdaContext(fullName.replace("::", "_"));
        MutableList<Constraint> constraints = constraints(classInstance, ctx.constraints(), typeParameterNames, multiplicityParameterNames, typeVariables, importId, lambdaContext, addLines);

        MutableList<Property<? extends CoreInstance, ?>> properties = Lists.mutable.empty();
        MutableList<QualifiedProperty<? extends CoreInstance>> qualifiedProperties = Lists.mutable.empty();
        propertyParser(classInstance, ctx.classBody().properties(), properties, qualifiedProperties, typeParameterNames, multiplicityParameterNames, typeVariables, ownerType, importId, 0);

        GenericType classifierGT = GenericTypeInstance.createPersistent(this.repository, sourceInfo)
                ._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.Class));
        GenericType classifierGTTA = GenericTypeInstance.createPersistent(this.repository)
                ._rawTypeCoreInstance(classInstance);
        if (typeVariables.notEmpty())
        {
            classifierGTTA._typeVariableValues(typeVariables);
        }

        if (typeParameterNames.notEmpty())
        {
            MutableList<TypeParameter> typeParameters = Lists.mutable.of();
            MutableList<Pair<String, Boolean>> tps = typeParameterNames.zip(contravariants);
            for (Pair<String, Boolean> typeParam : tps)
            {
                TypeParameterInstance tp = TypeParameterInstance.createPersistent(this.repository, typeParam.getOne());
                tp._contravariant(typeParam.getTwo());
                typeParameters.add(tp);
            }
            classInstance._typeParameters(typeParameters);

            MutableList<GenericType> typeArgs = Lists.mutable.of();
            for (String typeParamName : typeParameterNames)
            {
                TypeParameterInstance tp = TypeParameterInstance.createPersistent(this.repository, typeParamName);
                GenericTypeInstance gt = GenericTypeInstance.createPersistent(this.repository);
                gt._typeParameter(tp);
                typeArgs.add(gt);
            }

            classifierGTTA._typeArguments(typeArgs);
        }

        if (multiplicityParameterNames.notEmpty())
        {
            MutableList<Multiplicity> multParameters = Lists.mutable.of();

            for (String multiplicityParam : multiplicityParameterNames)
            {
                MultiplicityInstance mult = MultiplicityInstance.createPersistent(this.repository, null, null);
                mult._multiplicityParameter(multiplicityParam);
                multParameters.add(mult);

            }
            classInstance._multiplicityParameters(this.multParamsToInstanceValues(multiplicityParameterNames));
            classifierGTTA._multiplicityArguments(multParameters);
        }

        classifierGT._typeArgumentsAdd(classifierGTTA);

        classInstance._classifierGenericType(classifierGT);

        if (properties.notEmpty())
        {
            classInstance._properties(properties);
        }
        if (qualifiedProperties.notEmpty())
        {
            classInstance._qualifiedProperties(qualifiedProperties);
        }
        if (stereotypes.notEmpty())
        {
            classInstance._stereotypesCoreInstance(stereotypes);
        }
        if (taggedValues.notEmpty())
        {
            classInstance._taggedValues(taggedValues);
        }
        if (constraints.notEmpty())
        {
            classInstance._constraints(constraints);
        }
        if (ctx.EXTENDS() == null)
        {
            GenericType superType = GenericTypeInstance.createPersistent(this.repository, sourceInfo)._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.Any));
            classInstance._generalizationsAdd(GeneralizationInstance.createPersistent(this.repository, sourceInfo, superType, classInstance));
            MilestoningClassProcessor.addMilestoningProperty(classInstance, this.context, this.processorSupport, this.repository);
        }
        else
        {
            classInstance._generalizations(ListIterate.collect(ctx.type(), typeCtx ->
            {
                GenericType superType = type(typeCtx, typeParameterNames, "", importId, addLines);
                return GeneralizationInstance.createPersistent(this.repository, superType.getSourceInformation(), superType, classInstance);
            }));
        }
        return classInstance;
    }


    private MutableList<Constraint> constraints(CoreInstance owner, ConstraintsContext ctx, MutableList<String> typeParameterNames, MutableList<String> multParameterNames, ListIterable<VariableExpression> typeVariables, ImportGroup importId, LambdaContext lambdaContext, boolean addLines)
    {
        MutableList<Constraint> constraints = Lists.mutable.empty();
        if (ctx != null)
        {
            ListIterate.forEachWithIndex(ctx.constraint(), (cCtx, i) -> constraints.add(constraint(owner, cCtx, i, typeParameterNames, multParameterNames, typeVariables, importId, lambdaContext, addLines, false)));
        }
        return constraints;
    }

    private Constraint constraint(CoreInstance owner, ConstraintContext ctx, int position, MutableList<String> typeParameterNames, MutableList<String> multiplicityParameterNames, ListIterable<VariableExpression> typeVariables, ImportGroup importId, LambdaContext lambdaContext, boolean addLines, boolean postConstraint)
    {
        String constraintName;
        String constraintOwner = null;
        String constraintExternalId = null;
        CoreInstance constraintFunctionDefinition;
        String constraintLevel = null;
        LambdaFunction<?> constraintMessageFunction = null;
        SourceInformation constraintSourceInformation;

        if (ctx.simpleConstraint() != null)
        {
            SimpleConstraintContext simpleConstraintContext = ctx.simpleConstraint();
            constraintName = simpleConstraintContext.constraintId() != null ? simpleConstraintContext.constraintId().VALID_STRING().getText() : String.valueOf(position);
            constraintFunctionDefinition = this.combinedExpression(simpleConstraintContext.combinedExpression(), "", Lists.mutable.empty(), Lists.mutable.empty(), lambdaContext, "", true, importId, addLines);
            constraintSourceInformation = this.sourceInformation.getPureSourceInformation(simpleConstraintContext.getStart(), simpleConstraintContext.getStart(), simpleConstraintContext.getStop());
        }
        else
        {
            ComplexConstraintContext complexConstraintContext = ctx.complexConstraint();
            constraintSourceInformation = this.sourceInformation.getPureSourceInformation(complexConstraintContext.getStart(), complexConstraintContext.VALID_STRING().getSymbol(), complexConstraintContext.getStop());
            if (this.processorSupport.instance_instanceOf(owner, M3Paths.Type))
            {
                constraintName = complexConstraintContext.VALID_STRING().getText();

                if (complexConstraintContext.constraintOwner() != null)
                {
                    constraintOwner = complexConstraintContext.constraintOwner().VALID_STRING().getText();
                }

                if (complexConstraintContext.constraintExternalId() != null)
                {
                    constraintExternalId = StringEscape.unescape(removeQuotes(complexConstraintContext.constraintExternalId().STRING()));
                }

                constraintFunctionDefinition = this.combinedExpression(complexConstraintContext.constraintFunction().combinedExpression(), "", Lists.mutable.empty(), Lists.mutable.empty(), lambdaContext, "", true, importId, addLines);

                if (complexConstraintContext.constraintEnforcementLevel() != null)
                {
                    constraintLevel = complexConstraintContext.constraintEnforcementLevel().ENFORCEMENT_LEVEL().getText();
                }

                if (complexConstraintContext.constraintMessage() != null)
                {
                    CoreInstance messageFunction = this.combinedExpression(complexConstraintContext.constraintMessage().combinedExpression(), "", Lists.mutable.empty(), Lists.mutable.empty(), lambdaContext, "", true, importId, addLines);
                    SourceInformation messageSourceInformation = messageFunction.getSourceInformation();

                    CoreInstance messageFunctionType = this.repository.newAnonymousCoreInstance(messageSourceInformation, this.processorSupport.package_getByUserPath(M3Paths.FunctionType), true);
                    GenericType thisParamType = (GenericType) org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(owner, this.processorSupport);
                    if (typeParameterNames.notEmpty())
                    {
                        MutableList<TypeParameter> typeParameters = typeParameterNames.collect(n -> TypeParameterInstance.createPersistent(this.repository, n));
                        Instance.setValuesForProperty(messageFunctionType, M3Properties.typeParameters, typeParameters, this.processorSupport);
                        MutableList<GenericType> typeArgs = typeParameters.collect(tp -> GenericTypeInstance.createPersistent(this.repository)._typeParameter(tp));
                        thisParamType._typeArguments(typeArgs);
                    }
                    if (multiplicityParameterNames.notEmpty())
                    {
                        Instance.setValuesForProperty(messageFunctionType, M3Properties.multiplicityParameters, multParamsToInstanceValues(multiplicityParameterNames), this.processorSupport);
                        MutableList<Multiplicity> multArgs = multiplicityParameterNames.collect(n -> MultiplicityInstance.createPersistent(this.repository, null, null)._multiplicityParameter(n));
                        thisParamType._multiplicityArguments(multArgs);
                    }
                    if (typeVariables.notEmpty())
                    {
                        thisParamType._typeVariableValues(typeVariables);
                    }

                    CoreInstance param = VariableExpressionInstance.createPersistent(this.repository, messageSourceInformation, thisParamType, this.pureOne, "this");
                    Instance.addValueToProperty(messageFunctionType, M3Properties.parameters, owner.getValueForMetaPropertyToMany(M3Properties.typeVariables), this.processorSupport);
                    Instance.addValueToProperty(messageFunctionType, M3Properties.parameters, param, this.processorSupport);
                    Instance.setValueForProperty(messageFunctionType, M3Properties.returnMultiplicity, this.getPureOne(), this.processorSupport);
                    Instance.setValueForProperty(messageFunctionType, M3Properties.returnType, org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(this.processorSupport.package_getByUserPath(M3Paths.String), this.processorSupport), this.processorSupport);

                    CoreInstance messageFunctionTypeGt = this.repository.newAnonymousCoreInstance(messageSourceInformation, this.processorSupport.package_getByUserPath(M3Paths.GenericType), true);
                    Instance.setValueForProperty(messageFunctionTypeGt, M3Properties.rawType, messageFunctionType, this.processorSupport);

                    CoreInstance lambdaFunctionClass = this.processorSupport.package_getByUserPath(M3Paths.LambdaFunction);
                    CoreInstance lambdaGenericType = org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(lambdaFunctionClass, this.processorSupport);
                    Instance.setValueForProperty(lambdaGenericType, M3Properties.typeArguments, messageFunctionTypeGt, this.processorSupport);
                    constraintMessageFunction = LambdaFunctionInstance.createPersistent(this.repository, lambdaContext.getLambdaFunctionUniqueName(), messageSourceInformation);
                    Instance.setValueForProperty(constraintMessageFunction, M3Properties.expressionSequence, messageFunction, this.processorSupport);
                    Instance.setValueForProperty(constraintMessageFunction, M3Properties.classifierGenericType, lambdaGenericType, this.processorSupport);
                    Instance.setValueForProperty(messageFunctionType, M3Properties.function, constraintMessageFunction, this.processorSupport);
                }
            }
            else
            {
                throw new PureParserException(constraintSourceInformation, "Complex constraint specifications are supported only for class definitions");
            }
        }

        SourceInformation functionSourceInformation = constraintFunctionDefinition.getSourceInformation();
        CoreInstance functionType = this.repository.newAnonymousCoreInstance(functionSourceInformation, this.processorSupport.package_getByUserPath(M3Paths.FunctionType), true);
        if (this.processorSupport.instance_instanceOf(owner, M3Paths.ElementWithConstraints))
        {
            GenericType thisParamType;
            if (org.finos.legend.pure.m3.navigation.type.Type.isExtendedPrimitiveType(owner, processorSupport))
            {
                thisParamType = (GenericType) org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(org.finos.legend.pure.m3.navigation.type.Type.findPrimitiveTypeFromExtendedPrimitiveType(owner, processorSupport), this.processorSupport);
            }
            else
            {
                thisParamType = (GenericType) org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(owner, this.processorSupport);
                if (typeParameterNames.notEmpty())
                {
                    MutableList<TypeParameter> typeParameters = typeParameterNames.collect(n -> TypeParameterInstance.createPersistent(this.repository, n));
                    Instance.setValuesForProperty(functionType, M3Properties.typeParameters, typeParameters, this.processorSupport);
                    MutableList<GenericType> typeArgs = typeParameters.collect(tp -> GenericTypeInstance.createPersistent(this.repository)._typeParameter(tp));
                    thisParamType._typeArguments(typeArgs);
                }
                if (multiplicityParameterNames.notEmpty())
                {
                    Instance.setValuesForProperty(functionType, M3Properties.multiplicityParameters, multParamsToInstanceValues(multiplicityParameterNames), this.processorSupport);
                    MutableList<Multiplicity> multParameters = multiplicityParameterNames.collect(n -> MultiplicityInstance.createPersistent(this.repository, null, null)._multiplicityParameter(n));
                    thisParamType._multiplicityArguments(multParameters);
                }
                if (typeVariables.notEmpty())
                {
                    thisParamType._typeVariableValues(typeVariables);
                }
            }
            CoreInstance param = VariableExpressionInstance.createPersistent(this.repository, functionSourceInformation, thisParamType, getPureOne(), "this");
            if (this.processorSupport.instance_instanceOf(owner, M3Paths.Type))
            {
                Instance.addValueToProperty(functionType, M3Properties.parameters, owner.getValueForMetaPropertyToMany(M3Properties.typeVariables), this.processorSupport);
            }
            Instance.addValueToProperty(functionType, M3Properties.parameters, param, this.processorSupport);
        }

        if (this.processorSupport.instance_instanceOf(owner, M3Paths.FunctionDefinition))
        {
            FunctionType ft = (FunctionType) this.processorSupport.function_getFunctionType(owner);
            MutableList<CoreInstance> params = Lists.mutable.empty();
            params.addAllIterable(Instance.getValueForMetaPropertyToManyResolved(ft, M3Properties.parameters, this.processorSupport));
            if (postConstraint)
            {
                CoreInstance returnParam = VariableExpressionInstance.createPersistent(this.repository, functionSourceInformation, (GenericType) ft.getValueForMetaPropertyToOne(M3Properties.returnType), (Multiplicity) ft.getValueForMetaPropertyToOne(M3Properties.returnMultiplicity), "return");
                params.add(returnParam);
            }
            Instance.addValueToProperty(functionType, M3Properties.parameters, params, this.processorSupport);
        }


        Instance.setValueForProperty(functionType, M3Properties.returnMultiplicity, this.getPureOne(), this.processorSupport);
        Instance.setValueForProperty(functionType, M3Properties.returnType, org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(this.processorSupport.package_getByUserPath(M3Paths.Boolean), this.processorSupport), this.processorSupport);
        CoreInstance functionTypeGt = this.repository.newAnonymousCoreInstance(functionSourceInformation, this.processorSupport.package_getByUserPath(M3Paths.GenericType), true);
        Instance.setValueForProperty(functionTypeGt, M3Properties.rawType, functionType, this.processorSupport);

        CoreInstance lambdaFunctionClass = this.processorSupport.package_getByUserPath(M3Paths.LambdaFunction);
        CoreInstance lambdaGenericType = org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(lambdaFunctionClass, this.processorSupport);
        Instance.setValueForProperty(lambdaGenericType, M3Properties.typeArguments, functionTypeGt, this.processorSupport);


        LambdaFunction<?> constraintFunctionLambda = LambdaFunctionInstance.createPersistent(this.repository, lambdaContext.getLambdaFunctionUniqueName(), functionSourceInformation);
        Instance.setValueForProperty(constraintFunctionLambda, M3Properties.expressionSequence, constraintFunctionDefinition, this.processorSupport);
        Instance.setValueForProperty(constraintFunctionLambda, M3Properties.classifierGenericType, lambdaGenericType, this.processorSupport);
        Instance.setValueForProperty(functionType, M3Properties.function, constraintFunctionLambda, this.processorSupport);

        Constraint constraint = ConstraintInstance.createPersistent(this.repository, null, null);
        constraint.setSourceInformation(constraintSourceInformation);
        constraint._name(constraintName);
        if (constraintOwner != null)
        {
            constraint._owner(constraintOwner);
        }
        if (constraintExternalId != null)
        {
            constraint._externalId(constraintExternalId);
        }
        constraint._functionDefinition(constraintFunctionLambda);
        if (constraintLevel != null)
        {
            constraint._enforcementLevel(constraintLevel);
        }
        if (constraintMessageFunction != null)
        {
            constraint._messageFunction(constraintMessageFunction);
        }

        return constraint;
    }

    private CoreInstance projectionParser(ClassDefinitionContext ctx, ImportGroup importId, boolean addLines, ListIterable<CoreInstance> stereotypes, ListIterable<TaggedValue> taggedValues) throws PureParserException
    {
        checkExists(ctx.qualifiedName().packagePath(), ctx.qualifiedName().identifier(), null);

        RootRouteNode rootNode;
        SourceInformation sourceInfo;
        if (ctx.projection().dsl() == null)
        {
            rootNode = treePath(ctx.projection().treePath(), importId);
            sourceInfo = this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.qualifiedName().identifier().getStart(), ctx.getStop(), true);
        }
        else
        {
            rootNode = (RootRouteNode) dsl(ctx.projection().dsl(), importId).get(0);
            // HACK we get the start and main line/column from the main context, but the end line/column from the parsed root node
            // this is because the main context does not have the correct end line/col
            SourceInformation tmp = this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.qualifiedName().identifier().getStart(), ctx.getStop());
            sourceInfo = new SourceInformation(tmp.getSourceId(), tmp.getStartLine(), tmp.getStartColumn(), tmp.getLine(), tmp.getColumn(), rootNode.getSourceInformation().getEndLine(), rootNode.getSourceInformation().getEndColumn());
        }

        String name = ctx.qualifiedName().identifier().getText();
        ClassProjection<?> projection = ClassProjectionInstance.createPersistent(this.repository, name, sourceInfo, rootNode)._name(name);
        rootNode._owner(projection);

        buildAndSetPackage(projection, ctx.qualifiedName().packagePath(), this.repository, this.sourceInformation);

        GenericType classifierGT = GenericTypeInstance.createPersistent(this.repository, sourceInfo)
                ._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.ClassProjection))
                ._typeArguments(Lists.mutable.with(GenericTypeInstance.createPersistent(this.repository)._rawType(projection)));
        projection._classifierGenericType(classifierGT);

        GenericType superType = GenericTypeInstance.createPersistent(this.repository, sourceInfo)
                ._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.Any));
        projection._generalizations(Lists.mutable.with(GeneralizationInstance.createPersistent(this.repository, sourceInfo, superType, projection)));
        String fullName = this.getQualifiedNameString(ctx.qualifiedName());
        MutableList<Constraint> constraints = constraints(projection, ctx.constraints(), Lists.fixedSize.empty(), Lists.fixedSize.empty(), Lists.fixedSize.empty(), importId, new LambdaContext(fullName.replace("::", "_")), addLines);

        if (Iterate.notEmpty(stereotypes))
        {
            projection._stereotypesCoreInstance(stereotypes);
        }
        if (Iterate.notEmpty(taggedValues))
        {
            projection._taggedValues(taggedValues);
        }
        if (Iterate.notEmpty(constraints))
        {
            projection._constraints(constraints);
        }
        return projection;
    }

    private CoreInstance associationParser(AssociationContext ctx, ImportGroup importId) throws PureParserException
    {
        checkExists(ctx.qualifiedName().packagePath(), ctx.qualifiedName().identifier(), null);

        String associationName = ctx.qualifiedName().identifier().getText();
        ListIterable<CoreInstance> stereotypes = (ctx.stereotypes() == null) ? Lists.immutable.empty() : stereotypes(ctx.stereotypes(), importId);
        ListIterable<TaggedValue> taggedValues = (ctx.taggedValues() == null) ? Lists.immutable.empty() : taggedValues(ctx.taggedValues(), importId);

        if (ctx.associationProjection() != null)
        {
            AssociationProjectionContext apCtx = ctx.associationProjection();
            AssociationProjection projection = AssociationProjectionInstance.createPersistent(this.repository, associationName, this.sourceInformation.getPureSourceInformation(ctx.ASSOCIATION().getSymbol(), ctx.qualifiedName().identifier().getStart(), ctx.associationProjection().getStop()), null)
                    ._name(associationName);
            buildAndSetPackage(projection, ctx.qualifiedName().packagePath(), this.repository, this.sourceInformation);

            ImportStub projectedAssociation = ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(apCtx.qualifiedName(0).getStart()), this.getQualifiedNameString(ctx.associationProjection().qualifiedName(0)), importId);
            ImportStub projectionOne = ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(apCtx.qualifiedName(1).getStart()), this.getQualifiedNameString(apCtx.qualifiedName(1)), importId);
            ImportStub projectionTwo = ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(apCtx.qualifiedName(2).getStart()), this.getQualifiedNameString(apCtx.qualifiedName(2)), importId);
            projection._projectedAssociationCoreInstance(projectedAssociation);
            projection._projectionsCoreInstance(Lists.immutable.<CoreInstance>with(projectionOne, projectionTwo));

            if (stereotypes.notEmpty())
            {
                projection._stereotypesCoreInstance(stereotypes);
            }
            if (taggedValues.notEmpty())
            {
                projection._taggedValues(taggedValues);
            }
            return projection;
        }

        SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(ctx.ASSOCIATION().getSymbol(), ctx.qualifiedName().identifier().getStart(), ctx.getStop());
        Association association = AssociationInstance.createPersistent(this.repository, associationName, sourceInfo)
                ._name(associationName);
        buildAndSetPackage(association, ctx.qualifiedName().packagePath(), this.repository, this.sourceInformation);

        association._classifierGenericType(GenericTypeInstance.createPersistent(this.repository, sourceInfo)._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.Association)));

        if (stereotypes.notEmpty())
        {
            association._stereotypesCoreInstance(stereotypes);
        }
        if (taggedValues.notEmpty())
        {
            association._taggedValues(taggedValues);
        }

        ImportStub is = ImportStubInstance.createPersistent(this.repository, this.getQualifiedNameString(ctx.qualifiedName()), importId);
        MutableList<Property<? extends CoreInstance, ?>> properties = Lists.mutable.empty();
        MutableList<QualifiedProperty<? extends CoreInstance>> qualifiedProperties = Lists.mutable.empty();
        propertyParser(null, ctx.associationBody().properties(), properties, qualifiedProperties, Lists.fixedSize.empty(), Lists.fixedSize.empty(), Lists.mutable.empty(), is, importId, 0);
        association._properties(properties);
        if (qualifiedProperties.notEmpty())
        {
            association._qualifiedProperties(qualifiedProperties);
        }
        return association;
    }

    public void propertyParser(Class<?> classInstance, PropertiesContext ctx, MutableList<Property<? extends CoreInstance, ?>> properties, MutableList<QualifiedProperty<? extends CoreInstance>> qualifiedProperties, MutableList<String> typeParameterNames,
                               MutableList<String> multiplicityParameterNames, ListIterable<VariableExpression> typeVariables, ImportStub isOwner, ImportGroup importId, int startingQualifiedPropertyIndex)
    {
        if (ctx.property() != null)
        {
            ctx.property().forEach(pCtx -> simpleProperty(pCtx, properties, typeParameterNames, multiplicityParameterNames, typeVariables, isOwner, importId, this.addLines));
        }
        if (ctx.qualifiedProperty() != null)
        {
            Iterate.forEachWithIndex(ctx.qualifiedProperty(), (pCtx, i) -> qualifiedProperty(classInstance, pCtx, qualifiedProperties, typeParameterNames, multiplicityParameterNames, typeVariables, isOwner, importId, this.addLines, i + startingQualifiedPropertyIndex));
        }
    }

    private void simpleProperty(PropertyContext ctx, MutableList<Property<? extends CoreInstance, ?>> properties, MutableList<String> typeParameterNames,
                                MutableList<String> multiplicityParameterNames, ListIterable<VariableExpression> typeVariables, ImportStub isOwner, ImportGroup importId, boolean addLines)
    {
        String propertyName = removeQuotes(ctx.propertyName().getText());

        String aggregation;
        if (ctx.aggregation() != null)
        {
            if ("(composite)".equals(ctx.aggregation().getText()))
            {
                aggregation = "Composite";
            }
            else if ("(shared)".equals(ctx.aggregation().getText()))
            {
                aggregation = "Shared";
            }
            else
            {
                aggregation = "None";
            }
        }
        else
        {
            aggregation = "None";
        }
        GenericType genericType = type(ctx.propertyReturnType().type(), typeParameterNames, "", importId, addLines);
        Multiplicity multiplicity = buildMultiplicity(ctx.propertyReturnType().multiplicity().multiplicityArgument());

        Enumeration<?> agg = (Enumeration<?>) this.processorSupport.package_getByUserPath(M3Paths.AggregationKind);
        Enum aggKind = (Enum) agg._values().detect(v -> aggregation.equals(((Enum) v).getName()));
        SourceInformation propertySourceInfo = this.sourceInformation.getPureSourceInformation(ctx.propertyName().getStart(), ctx.propertyName().getStart(), ctx.getStop());
        Property<? extends CoreInstance, ?> propertyInstance = PropertyInstance.createPersistent(this.repository, propertyName, propertySourceInfo, aggKind, genericType, multiplicity, null);
        propertyInstance._name(propertyName);

        if (ctx.stereotypes() != null)
        {
            ListIterable<CoreInstance> stereotypes = this.stereotypes(ctx.stereotypes(), importId);
            propertyInstance._stereotypesCoreInstance(stereotypes);
        }
        if (ctx.taggedValues() != null)
        {
            ListIterable<TaggedValue> tags = taggedValues(ctx.taggedValues(), importId);
            propertyInstance._taggedValues(tags);
        }
        if (ctx.defaultValue() != null)
        {
            DefaultValue defaultValue = defaultValue(ctx.defaultValue(), isOwner, importId, propertyName);
            propertyInstance._defaultValue(defaultValue);
        }

        GenericType classifierGT = GenericTypeInstance.createPersistent(this.repository, propertySourceInfo)
                ._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.Property))
                ._multiplicityArguments(Lists.mutable.of(multiplicity));

        GenericType classifierGTTA = GenericTypeInstance.createPersistent(this.repository, propertySourceInfo)._rawTypeCoreInstance(isOwner);
        if (typeParameterNames.notEmpty())
        {
            classifierGTTA._typeArguments(typeParameterNames.collect(tp -> GenericTypeInstance.createPersistent(this.repository)._typeParameter(TypeParameterInstance.createPersistent(this.repository, tp))));
        }
        if (multiplicityParameterNames.notEmpty())
        {
            classifierGTTA._multiplicityArguments(multiplicityParameterNames.collect(p -> MultiplicityInstance.createPersistent(this.repository)._multiplicityParameter(p)));
        }
        if (typeVariables.notEmpty())
        {
            classifierGTTA._typeVariableValues(typeVariables);
        }

        //Clone generic type
        //TODO - do we need a deep clone?
        GenericTypeInstance ngt = GenericTypeInstance.createPersistent(this.repository, genericType.getSourceInformation());
        CoreInstance rawType = genericType._rawTypeCoreInstance();
        if (rawType != null)
        {
            if (rawType instanceof ImportStub)
            {
                ImportStub gtis = (ImportStub) rawType;
                ImportStubInstance is = ImportStubInstance.createPersistent(this.repository, gtis.getSourceInformation(), gtis._idOrPath(), gtis._importGroup());
//                ImportStubInstance is = ImportStubInstance.createPersistent(this.repository, gtis.getSourceInformation(), ((ImportStubInstance)gtis)._idOrPathAsCoreInstance().getName(), (ImportGroup)gtis._importGroup());
                ngt._rawTypeCoreInstance(is);
            }
            else
            {
                ngt._rawTypeCoreInstance(rawType);
            }
        }

        if (genericType._typeArguments().notEmpty())
        {
            ngt._typeArguments(genericType._typeArguments());
        }
        ngt._typeParameter(genericType._typeParameter());
        if (genericType._multiplicityArguments().notEmpty())
        {
            ngt._multiplicityArguments(genericType._multiplicityArguments());
        }
        if (genericType._typeVariableValues().notEmpty())
        {
            ngt._typeVariableValues(genericType._typeVariableValues());
        }

        classifierGT._typeArguments(Lists.mutable.of(classifierGTTA, ngt));
        propertyInstance._classifierGenericType(classifierGT);
        properties.add(propertyInstance);
    }

    private void qualifiedProperty(Class<?> classInstance, QualifiedPropertyContext ctx, MutableList<QualifiedProperty<? extends CoreInstance>> qualifiedProperties, MutableList<String> typeParameterNames, MutableList<String> multiplicityParameterNames, ListIterable<VariableExpression> typeVariables, ImportStub isOwner, ImportGroup importId, boolean addLines, int qualifiedPropertyIndex)
    {
        SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(ctx.identifier().getStart(), ctx.identifier().getStart(), ctx.getStop());

        MutableList<VariableExpression> vars = Lists.mutable.of();
        ListIterable<ValueSpecification> code = Lists.fixedSize.empty();
        GenericType genericType = this.type(ctx.propertyReturnType().type(), typeParameterNames, "", importId, addLines);
        Multiplicity multiplicity = this.buildMultiplicity(ctx.propertyReturnType().multiplicity().multiplicityArgument());

        String propertyName = ctx.identifier().getText();
        String qualifiedPropertyName = propertyName + "_" + qualifiedPropertyIndex;

        if (ctx.qualifiedPropertyBody() != null)
        {
            if (ctx.qualifiedPropertyBody().functionVariableExpression() != null)
            {
                ListIterate.collect(ctx.qualifiedPropertyBody().functionVariableExpression(), fveCtx -> functionVariableExpression(fveCtx, typeParameterNames, importId, ""), vars);
            }
            if (ctx.qualifiedPropertyBody().codeBlock() != null)
            {
                LambdaContext lambdaContext = new LambdaContext(isOwner._idOrPath().replace("::", "_") + "_" + qualifiedPropertyName);
                code = this.codeBlock(ctx.qualifiedPropertyBody().codeBlock(), typeParameterNames, multiplicityParameterNames, importId, lambdaContext, addLines, "");
            }
        }

        GenericType thisParamGT = GenericTypeInstance.createPersistent(this.repository, sourceInfo)._rawTypeCoreInstance(isOwner);
        if (typeParameterNames.notEmpty())
        {
            MutableList<GenericType> typeArgs = typeParameterNames.collect(n -> GenericTypeInstance.createPersistent(this.repository)._typeParameter(TypeParameterInstance.createPersistent(this.repository, n)));
            thisParamGT._typeArguments(typeArgs);
        }
        if (multiplicityParameterNames.notEmpty())
        {
            MutableList<Multiplicity> multParameters = multiplicityParameterNames.collect(n -> MultiplicityInstance.createPersistent(this.repository, null, null)._multiplicityParameter(n));
            thisParamGT._multiplicityArguments(multParameters);
        }
        if (typeVariables.notEmpty())
        {
            thisParamGT._typeVariableValues(typeVariables);
        }

        VariableExpression thisParam = VariableExpressionInstance.createPersistent(this.repository, thisParamGT, this.getPureOne(), "this");

        GenericType ngt = GenericTypeInstance.createPersistent(this.repository, genericType.getSourceInformation());

        CoreInstance rawType = genericType._rawTypeCoreInstance();
        if (rawType instanceof ImportStub)
        {
            ImportStub gtis = (ImportStub) rawType;
            ImportStubInstance is = ImportStubInstance.createPersistent(this.repository, gtis.getSourceInformation(), gtis._idOrPath(), gtis._importGroup());
//                ImportStubInstance is = ImportStubInstance.createPersistent(this.repository, gtis.getSourceInformation(), ((ImportStubInstance)gtis)._idOrPathAsCoreInstance().getName(), (ImportGroup)gtis._importGroup());
            ngt._rawTypeCoreInstance(is);
        }
        else if (rawType != null)
        {
            ngt._rawTypeCoreInstance(rawType);
        }
        if (!genericType._typeArguments().isEmpty())
        {
            ngt._typeArguments(genericType._typeArguments());
        }
        ngt._typeParameter(genericType._typeParameter());
        if (!genericType._multiplicityArguments().isEmpty())
        {
            ngt._multiplicityArguments(genericType._multiplicityArguments());
        }

        QualifiedPropertyInstance qpi = QualifiedPropertyInstance.createPersistent(this.repository, qualifiedPropertyName, sourceInfo, genericType, null, multiplicity, null);
        qpi._name(propertyName);
        qpi._functionName(propertyName);
        qpi._expressionSequence(code);

        ListIterable<CoreInstance> stereotypes = (ctx.stereotypes() == null) ? Lists.immutable.empty() : stereotypes(ctx.stereotypes(), importId);
        if (stereotypes.notEmpty())
        {
            qpi._stereotypesCoreInstance(stereotypes);
        }
        ListIterable<TaggedValue> taggedValues = (ctx.taggedValues() == null) ? Lists.immutable.empty() : taggedValues(ctx.taggedValues(), importId);
        if (taggedValues.notEmpty())
        {
            qpi._taggedValues(taggedValues);
        }

        FunctionType ft = FunctionTypeInstance.createPersistent(this.repository, sourceInfo, multiplicity, ngt);
        if (typeParameterNames.notEmpty())
        {
            ft._typeParameters(this.processTypeParametersInstance(this.repository, typeParameterNames));
        }
        MutableList<VariableExpression> ftParams = Lists.mutable.with(thisParam);
        if (classInstance != null)
        {
            classInstance._typeVariables().collect(c -> VariableExpressionInstance.createPersistent(this.repository, (GenericType) org.finos.legend.pure.m3.navigation.generictype.GenericType.copyGenericType(c._genericType(), processorSupport), this.getPureOne(), c._name()), ftParams);
        }
        ft._parameters(ftParams.withAll(vars));

        GenericType classifierGT = GenericTypeInstance.createPersistent(this.repository, sourceInfo)
                ._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.QualifiedProperty))
                ._typeArguments(Lists.immutable.with(GenericTypeInstance.createPersistent(this.repository, sourceInfo)._rawType(ft)));
        qpi._classifierGenericType(classifierGT);
        qualifiedProperties.add(qpi);
    }

    private CoreInstance nativeFunction(NativeFunctionContext ctx, ImportGroup importId, String space, MutableList<CoreInstance> coreInstancesResult)
    {
        this.functionCounter++;
        SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(ctx.NATIVE().getSymbol(), ctx.qualifiedName().identifier().getStart(), ctx.END_LINE().getSymbol());
        NativeFunction<?> function = NativeFunctionInstance.createPersistent(this.repository, ctx.qualifiedName().identifier().getText() + this.functionCounter, sourceInfo)
                ._functionName(ctx.qualifiedName().identifier().getText());
        buildAndSetPackage(function, ctx.qualifiedName().packagePath(), this.repository, this.sourceInformation);

        MutableList<String> typeParametersNames = Lists.mutable.empty();
        MutableList<String> multiplicityParametersNames = Lists.mutable.empty();
        if (ctx.typeAndMultiplicityParameters() != null)
        {
            this.typeParametersAndMultiplicityParameters(ctx.typeAndMultiplicityParameters(), typeParametersNames, multiplicityParametersNames);
        }

        ListIterable<CoreInstance> stereotypes = (ctx.stereotypes() == null) ? Lists.immutable.empty() : stereotypes(ctx.stereotypes(), importId);
        if (stereotypes.notEmpty())
        {
            function._stereotypesCoreInstance(stereotypes);
        }
        ListIterable<TaggedValue> taggedValues = (ctx.taggedValues() == null) ? Lists.immutable.empty() : taggedValues(ctx.taggedValues(), importId);
        if (taggedValues.notEmpty())
        {
            function._taggedValues(taggedValues);
        }

        FunctionType signature = functionTypeSignature(ctx.functionTypeSignature(), function, typeParametersNames, multiplicityParametersNames, importId, spacePlusTabs(space, 1));
        GenericType classifierGT = GenericTypeInstance.createPersistent(this.repository, sourceInfo)
                ._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.NativeFunction))
                ._typeArguments(Lists.immutable.of(GenericTypeInstance.createPersistent(this.repository, sourceInfo)._rawType(signature)));
        function._classifierGenericType(classifierGT);
        return function;
    }

    public CoreInstance concreteFunctionDefinition(FunctionDefinitionContext ctx, ImportGroup importId, boolean addLines, String space, MutableList<CoreInstance> coreInstancesResult)
    {
        this.functionCounter++;
        ConcreteFunctionDefinitionInstance functionDefinition = ConcreteFunctionDefinitionInstance.createPersistent(this.repository, ctx.qualifiedName().identifier().getText() + importId.getName() + this.functionCounter, this.sourceInformation.getPureSourceInformation(ctx.FUNCTION().getSymbol(), ctx.qualifiedName().identifier().getStart(), ctx.getStop()));

        ListIterable<CoreInstance> stereotypes = (ctx.stereotypes() == null) ? null : stereotypes(ctx.stereotypes(), importId);
        ListIterable<TaggedValue> tags = (ctx.taggedValues() == null) ? null : taggedValues(ctx.taggedValues(), importId);

        MutableList<String> typeParametersNames = Lists.mutable.empty();
        MutableList<String> multiplicityParameterNames = Lists.mutable.empty();
        if (ctx.typeAndMultiplicityParameters() != null)
        {
            this.typeParametersAndMultiplicityParameters(ctx.typeAndMultiplicityParameters(), typeParametersNames, multiplicityParameterNames);
        }

        FunctionType signature = this.functionTypeSignature(ctx.functionTypeSignature(), functionDefinition, typeParametersNames, multiplicityParameterNames, importId, spacePlusTabs(space, 1));

        //Reset the lambda function counter - we count within the Concrete definition
        LambdaContext lambdaContext = new LambdaContext(getFunctionUniqueId(ctx.qualifiedName(), this.functionCounter, importId));

        ListIterable<ValueSpecification> block = this.codeBlock(ctx.codeBlock(), typeParametersNames, multiplicityParameterNames, importId, lambdaContext, addLines, spacePlusTabs(space, 2));

        functionDefinition._stereotypesCoreInstance(stereotypes);
        functionDefinition._taggedValues(tags);
        functionDefinition._functionName(ctx.qualifiedName().identifier().getText());
        buildAndSetPackage(functionDefinition, ctx.qualifiedName().packagePath(), this.repository, this.sourceInformation);
        GenericType classifierGT = GenericTypeInstance.createPersistent(this.repository, signature.getSourceInformation())
                ._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.ConcreteFunctionDefinition))
                ._typeArguments(Lists.immutable.of(GenericTypeInstance.createPersistent(this.repository, signature.getSourceInformation())._rawType(signature)));
        functionDefinition._classifierGenericType(classifierGT);

        MutableList<Constraint> preConstraints = Lists.mutable.empty();
        MutableList<Constraint> postConstraints = Lists.mutable.empty();
        if (ctx.constraints() != null)
        {
            ListIterate.forEachWithIndex(ctx.constraints().constraint(), (cCtx, i) ->
            {
                if (cCtx.simpleConstraint() == null)
                {
                    throw new PureParserException(this.sourceInformation.getPureSourceInformation(cCtx.getStart()), "Complex constraint specifications are supported only for class definitions");
                }
                if (cCtx.simpleConstraint().combinedExpression().getText().contains("$return"))
                {
                    postConstraints.add(this.constraint(functionDefinition, cCtx, i, Lists.fixedSize.empty(), Lists.fixedSize.empty(), Lists.fixedSize.empty(), importId, lambdaContext, addLines, true));
                }
                else
                {
                    preConstraints.add(this.constraint(functionDefinition, cCtx, i, Lists.fixedSize.empty(), Lists.fixedSize.empty(), Lists.fixedSize.empty(), importId, lambdaContext, addLines, false));
                }
            });
        }
        if (preConstraints.notEmpty())
        {
            functionDefinition._preConstraints(preConstraints);
        }
        if (postConstraints.notEmpty())
        {
            functionDefinition._postConstraints(postConstraints);
        }

        functionDefinition._expressionSequence(block);

        return functionDefinition;
    }

    private void typeParametersAndMultiplicityParameters(TypeAndMultiplicityParametersContext ctx, MutableList<String> typeParameterNames, MutableList<String> multiplicityParameterNames)
    {
        if (ctx.typeParameters() != null)
        {
            for (TypeParameterContext tpCtx : ctx.typeParameters().typeParameter())
            {
                typeParameterNames.add(tpCtx.identifier().getText());
            }
        }
        if (ctx.multiplictyParameters() != null)
        {
            for (IdentifierContext mpCtx : ctx.multiplictyParameters().identifier())
            {
                multiplicityParameterNames.add(mpCtx.getText());
            }
        }
    }

    private void typeParametersWithContravarianceAndMultiplicityParameters(TypeParametersWithContravarianceAndMultiplicityParametersContext ctx, MutableList<String> typeParameterNames, MutableList<Boolean> contravariants, MutableList<String> multiplicityParameterNames)
    {
        if (ctx.contravarianceTypeParameters() != null)
        {
            for (ContravarianceTypeParameterContext tpCtx : ctx.contravarianceTypeParameters().contravarianceTypeParameter())
            {
                contravariants.add(tpCtx.MINUS() != null);
                typeParameterNames.add(tpCtx.identifier().getText());
            }
        }
        if (ctx.multiplictyParameters() != null)
        {
            for (IdentifierContext mpCtx : ctx.multiplictyParameters().identifier())
            {
                multiplicityParameterNames.add(mpCtx.getText());
            }
        }
    }

    private FunctionType functionTypeSignature(FunctionTypeSignatureContext ctx, org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function<?> function, MutableList<String> typeParametersNames, MutableList<String> multiplicityParametersNames, ImportGroup importId, String space)
    {
        ListIterable<VariableExpression> vars = (ctx.functionVariableExpression() == null) ?
                Lists.immutable.empty() :
                ListIterate.collect(ctx.functionVariableExpression(), fveCtx -> functionVariableExpression(fveCtx, typeParametersNames, importId, spacePlusTabs(space, 4)));
        GenericType returnType = this.type(ctx.type(), typeParametersNames, spacePlusTabs(space, 3), importId, false);
        Multiplicity multiplicity = this.buildMultiplicity(ctx.multiplicity().multiplicityArgument());

        SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.getStart(), ctx.getStop());
        FunctionType ft = FunctionTypeInstance.createPersistent(this.repository, sourceInfo, multiplicity, returnType);
        ft._functionAdd(function);
        if (typeParametersNames.notEmpty())
        {
            ft._typeParameters(this.processTypeParametersInstance(this.repository, typeParametersNames));
        }
        if (multiplicityParametersNames.notEmpty())
        {
            ft._multiplicityParameters(this.multParamsToInstanceValues(multiplicityParametersNames));
        }
        if (vars.notEmpty())
        {
            ft._parameters(vars);
        }
        return ft;
    }

    private ListIterable<InstanceValue> multParamsToInstanceValues(ListIterable<String> multParameters)
    {
        Type stringType = (Type) this.processorSupport.package_getByUserPath(M3Paths.String);
        Multiplicity pureOne = getPureOne();
        return multParameters.collect(multParameter -> InstanceValueInstance.createPersistent(this.repository, GenericTypeInstance.createPersistent(this.repository)._rawTypeCoreInstance(stringType), pureOne)._values(Lists.mutable.of(this.repository.newStringCoreInstance_cached(multParameter))));
    }

    private ListIterable<TypeParameter> processTypeParametersInstance(ModelRepository repository, MutableList<String> typeParameters)
    {
        return typeParameters.collect(tp -> TypeParameterInstance.createPersistent(repository, tp));
    }

    private VariableExpression functionVariableExpression(FunctionVariableExpressionContext ctx, MutableList<String> typeParametersNames, ImportGroup importId, String space)
    {
        GenericType type;
        Multiplicity multiplicity;
        type = this.type(ctx.type(), typeParametersNames, spacePlusTabs(space, 3), importId, true);
        multiplicity = this.buildMultiplicity(ctx.multiplicity().multiplicityArgument());
        return VariableExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.identifier().getStart()), type, multiplicity, ctx.identifier().getText());
    }

    private VariableExpression typeFunctionTypePureType(FunctionTypePureTypeContext ctx, MutableList<String> typeParametersNames, String space, ImportGroup importId, boolean addLines)
    {
        GenericType type = this.type(ctx.type(), typeParametersNames, spacePlusTabs(space, 3), importId, addLines);
        Multiplicity multiplicity = this.buildMultiplicity(ctx.multiplicity().multiplicityArgument());
        return VariableExpressionInstance.createPersistent(this.repository, type, multiplicity, "");
    }

    private ListIterable<Multiplicity> multiplicityArguments(MultiplicityArgumentsContext ctx)
    {
        MutableList<Multiplicity> results = Lists.mutable.empty();
        if (ctx != null && ctx.multiplicityArgument() != null)
        {
            ListIterate.collect(ctx.multiplicityArgument(), this::buildMultiplicity, results);
        }
        return results;
    }

    private ListIterable<GenericType> typeArguments(TypeArgumentsContext ctx, MutableList<String> typeParametersNames, ImportGroup importId, boolean addLines)
    {
        MutableList<GenericType> result = Lists.mutable.empty();
        if (ctx != null && ctx.typeWithOperation() != null)
        {
            ListIterate.collect(ctx.typeWithOperation(), typeCtx -> typeWithOperation(typeCtx, typeParametersNames, "", importId, addLines), result);
        }
        return result;
    }

    private GenericType typeWithOperation(TypeWithOperationContext typeCtx, MutableList<String> typeParametersNames, String space, ImportGroup importId, boolean addLines)
    {
        GenericType left = type(typeCtx.type(), typeParametersNames, space, importId, addLines);

        if (typeCtx.equalType() != null)
        {
            GenericType newLeft = type(typeCtx.equalType().type(), typeParametersNames, space, importId, addLines);
            GenericType right = buildSubset(buildAddSub(newLeft, typeCtx.typeAddSubOperation(), typeParametersNames, space, importId, addLines), typeCtx.subsetType(), typeParametersNames, space, importId, addLines);
            return GenericTypeOperationInstance.createPersistent(repository, left, right, (Enum) findEnum(M3Paths.GenericTypeOperationType, "Equal", repository));
        }

        if (typeCtx.subsetType() != null)
        {
            return buildSubset(
                    buildAddSub(left, typeCtx.typeAddSubOperation(), typeParametersNames, space, importId, addLines),
                    typeCtx.subsetType(),
                    typeParametersNames, space, importId, addLines);
        }

        return buildAddSub(left, typeCtx.typeAddSubOperation(), typeParametersNames, space, importId, addLines);
    }

    private GenericType buildSubset(GenericType left, SubsetTypeContext subsetTypeContext, MutableList<String> typeParametersNames, String space, ImportGroup importId, boolean addLines)
    {
        if (subsetTypeContext != null)
        {
            return GenericTypeOperationInstance.createPersistent(repository, left, type(subsetTypeContext.type(), typeParametersNames, space, importId, addLines), (Enum) findEnum(M3Paths.GenericTypeOperationType, "Subset", repository));
        }
        return left;
    }

    public GenericType buildAddSub(GenericType start, List<TypeAddSubOperationContext> addSub, MutableList<String> typeParametersNames, String space, ImportGroup importId, boolean addLines)
    {
        if (!addSub.isEmpty())
        {
            return ListIterate.injectInto(
                    start,
                    addSub,
                    (genericType, typeOperationContext) ->
                    {
                        GenericType right = type(
                                typeOperationContext.addType() != null ?
                                        typeOperationContext.addType().type() : typeOperationContext.subType().type(),
                                typeParametersNames, space, importId, addLines);
                        String type = typeOperationContext.addType() != null ? "Union" : "Difference";
                        return GenericTypeOperationInstance.createPersistent(repository, genericType, right, (Enum) findEnum(M3Paths.GenericTypeOperationType, type, repository));
                    });
        }
        return start;
    }

    private GenericType processType(QualifiedNameContext classParserPath, MutableList<String> typeParametersNames, ListIterable<GenericType> possibleTypeArguments, ListIterable<Multiplicity> possibleMultiplicityArguments, ListIterable<InstanceValue> possibleTypeVariableValues, ImportGroup importId)
    {
        SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(classParserPath.identifier().getStart());
        GenericTypeInstance result = GenericTypeInstance.createPersistent(this.repository, sourceInfo);

        MutableList<String> packagePaths = this.qualifiedNameToList(classParserPath);
        if (typeParametersNames.contains(packagePaths.getFirst()))
        {
            TypeParameterInstance tp = TypeParameterInstance.createPersistent(this.repository, packagePaths.getFirst());
            result._typeParameter(tp);
        }
        else
        {
            String idOrPath = this.getQualifiedNameString(classParserPath);

            if (_Package.SPECIAL_TYPES.contains(idOrPath))
            {
                CoreInstance type = this.repository.getTopLevel(idOrPath);
                if (type == null)
                {
                    throw new RuntimeException("Failed to find type");
                }
                result._rawTypeCoreInstance(type);
            }
            else
            {
                ImportStubInstance is = ImportStubInstance.createPersistent(this.repository, sourceInfo, idOrPath, importId);
                result._rawTypeCoreInstance(is);
            }
        }
        if (!Iterate.isEmpty(possibleTypeArguments))
        {
            result._typeArguments(possibleTypeArguments);
        }
        if (!Iterate.isEmpty(possibleMultiplicityArguments))
        {
            result._multiplicityArguments(possibleMultiplicityArguments);
        }
        if (!Iterate.isEmpty(possibleTypeVariableValues))
        {
            result._typeVariableValuesAddAll(possibleTypeVariableValues);
        }
        return result;
    }

    private GenericType processUnitType(UnitNameContext unitNameContext, ImportGroup importId)
    {
        SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(unitNameContext.identifier().getStart());
        String idOrPath = getUnitNameWithMeasure(unitNameContext);
        return GenericTypeInstance.createPersistent(this.repository, sourceInfo)
                ._rawTypeCoreInstance(ImportStubInstance.createPersistent(this.repository, sourceInfo, idOrPath, importId));
    }

    private ListIterable<CoreInstance> stereotypes(StereotypesContext ctx, ImportGroup importId)
    {
        return ListIterate.collect(ctx.stereotype(), stereotypeContext -> stereotype(importId, stereotypeContext));
    }

    private ImportStub stereotype(ImportGroup importId, StereotypeContext ctx)
    {
        return ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.qualifiedName().getStart(), ctx.identifier().getStart(), ctx.identifier().getStop()), this.getQualifiedNameString(ctx.qualifiedName()) + "@" + ctx.identifier().getText(), importId);
    }

    private ListIterable<TaggedValue> taggedValues(TaggedValuesContext ctx, ImportGroup importId)
    {
        return ListIterate.collect(ctx.taggedValue(), tvContext -> taggedValue(importId, tvContext));
    }

    private TaggedValue taggedValue(ImportGroup importId, TaggedValueContext ctx)
    {
        ImportStubInstance importStubInstance = ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.qualifiedName().getStart(), ctx.identifier().getStart(), ctx.identifier().getStop()), this.getQualifiedNameString(ctx.qualifiedName()) + "%" + ctx.identifier().getText(), importId);
        return TaggedValueInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.STRING().get(0).getSymbol(), ctx.STRING().get(ctx.STRING().size() - 1).getSymbol()), importStubInstance, Lists.mutable.withAll(ctx.STRING()).collect(AntlrContextToM3CoreInstance::removeQuotes).makeString());
    }

    private DefaultValue defaultValue(DefaultValueContext ctx, ImportStub isOwner, ImportGroup importId, String propertyName)
    {
        DefaultValueInstance defaultValueInstance = DefaultValueInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.EQUAL().getSymbol(), ctx.getStop()));

        LambdaContext lambdaContext = new LambdaContext("defaultValue$" + isOwner._idOrPath().replace("::", "_") + "$" + propertyName);
        CoreInstance defaultValueExpression = defaultValueExpression(ctx.defaultValueExpression(), importId, lambdaContext);
        if (defaultValueExpression != null)
        {
            SourceInformation source = defaultValueExpression.getSourceInformation();
            LambdaFunction<?> defaultValueFunctionLambda = LambdaFunctionInstance.createPersistent(this.repository, lambdaContext.getLambdaFunctionUniqueName(), source);
            CoreInstance functionType = this.repository.newAnonymousCoreInstance(source, this.processorSupport.package_getByUserPath(M3Paths.FunctionType), true);

            CoreInstance functionTypeGt = this.repository.newAnonymousCoreInstance(source, this.processorSupport.package_getByUserPath(M3Paths.GenericType), true);
            Instance.setValueForProperty(functionTypeGt, M3Properties.rawType, functionType, this.processorSupport);

            CoreInstance lambdaFunctionClass = this.processorSupport.package_getByUserPath(M3Paths.LambdaFunction);
            CoreInstance lambdaGenericType = org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(lambdaFunctionClass, this.processorSupport);
            Instance.setValueForProperty(lambdaGenericType, M3Properties.typeArguments, functionTypeGt, this.processorSupport);

            Instance.setValueForProperty(defaultValueFunctionLambda, M3Properties.expressionSequence, defaultValueExpression, this.processorSupport);
            Instance.setValueForProperty(defaultValueFunctionLambda, M3Properties.classifierGenericType, lambdaGenericType, this.processorSupport);
            Instance.setValueForProperty(functionType, M3Properties.function, defaultValueFunctionLambda, this.processorSupport);

            defaultValueInstance._functionDefinition(defaultValueFunctionLambda);
        }

        return defaultValueInstance;
    }

    private CoreInstance defaultValueExpression(DefaultValueExpressionContext ctx, ImportGroup importId, LambdaContext lambdaContext)
    {
        CoreInstance result = null;

        if (ctx.instanceLiteral() != null)
        {
            result = doWrap(instanceLiteral(ctx.instanceLiteral()), ctx.instanceLiteral().getStart());
        }
        else if (ctx.instanceReference() != null)
        {
            result = instanceReference(ctx.instanceReference(), Lists.mutable.empty(), Lists.mutable.empty(), lambdaContext, importId, this.tabs(4), addLines);
        }
        else if (ctx.expressionInstance() != null)
        {
            result = this.expressionInstanceParser(ctx.expressionInstance(), Lists.mutable.empty(), lambdaContext, importId, addLines, this.tabs(4));
        }
        else if (ctx.defaultValueExpressionsArray() != null)
        {
            MutableList<CoreInstance> expressions = Lists.mutable.of();
            for (DefaultValueExpressionContext eCtx : ctx.defaultValueExpressionsArray().defaultValueExpression())
            {
                expressions.add(this.defaultValueExpression(eCtx, importId, lambdaContext));
            }
            result = this.doWrap(expressions, ctx.defaultValueExpressionsArray().getStart().getLine(), ctx.defaultValueExpressionsArray().getStart().getCharPositionInLine() + 1, ctx.getStop().getLine(), ctx.getStop().getCharPositionInLine() + 1);
        }

        if (ctx.propertyExpression() != null)
        {
            result = propertyExpression(ctx.propertyExpression(), result, Lists.mutable.empty(), Lists.mutable.empty(), Lists.mutable.empty(), lambdaContext, this.tabs(4), importId);
        }
        return result;
    }

    public static String removeQuotes(TerminalNode stringNode)
    {
        return removeQuotes(stringNode.getText());
    }

    private String packageToString(PackagePathContext ctx)
    {
        return this.packageToList(ctx).makeString("::");
    }

    private StringBuilder appendPackage(StringBuilder builder, PackagePathContext ctx)
    {
        if (ctx != null)
        {
            LazyIterate.collect(ctx.identifier(), RuleContext::getText).appendString(builder, org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.DEFAULT_PATH_SEPARATOR);
        }
        return builder;
    }

    private String getQualifiedNameString(QualifiedNameContext ctx)
    {
        return ctx.packagePath() != null ? this.packageToString(ctx.packagePath()).concat("::").concat(ctx.identifier().getText()) : ctx.identifier().getText();
    }


    private String getFunctionUniqueId(QualifiedNameContext qualifiedNameCtx, int functionCounter, ImportGroup importId)
    {
        StringBuilder builder = new StringBuilder();
        if (qualifiedNameCtx.packagePath() != null)
        {
            qualifiedNameCtx.packagePath().identifier().forEach(i -> builder.append(i.getText()).append('$'));
        }
        builder.append(qualifiedNameCtx.identifier().getText()).append('$').append(functionCounter).append('$');
        org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(builder, importId, "$");
        return builder.toString();
    }

    private Profile profile(ProfileContext ctx)
    {
        String profileName = ctx.qualifiedName().identifier().getText();
        SourceInformation sourceInfo = this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.qualifiedName().getStop(), ctx.getStop());
        Profile profile = ProfileInstance.createPersistent(this.repository, profileName, sourceInfo);
        buildAndSetPackage(profile, ctx.qualifiedName().packagePath(), this.repository, this.sourceInformation);
        return profile._name(profileName)
                ._classifierGenericType(GenericTypeInstance.createPersistent(this.repository, sourceInfo)._rawType((Type) this.processorSupport.package_getByUserPath(M3Paths.Profile)))
                ._p_stereotypes(buildStereoTypes(ctx.stereotypeDefinitions(), profile))
                ._p_tags(buildTags(ctx.tagDefinitions(), profile));
    }

    private ListIterable<Stereotype> buildStereoTypes(StereotypeDefinitionsContext ctx, Profile profile)
    {
        if (ctx == null)
        {
            return Lists.fixedSize.empty();
        }
        MutableList<Stereotype> stereotypes = Lists.mutable.empty();
        for (IdentifierContext identifierContext : ctx.identifier())
        {
            stereotypes.add(StereotypeInstance.createPersistent(this.repository, identifierContext.getText(), this.sourceInformation.getPureSourceInformation(identifierContext.getStart()), profile, identifierContext.getText()));
        }
        return stereotypes;
    }

    private ListIterable<Tag> buildTags(TagDefinitionsContext ctx, Profile profile)
    {
        if (ctx == null)
        {
            return Lists.fixedSize.empty();
        }
        MutableList<Tag> tags = Lists.mutable.empty();
        for (IdentifierContext identifierContext : ctx.identifier())
        {
            tags.add(TagInstance.createPersistent(this.repository, identifierContext.getText(), this.sourceInformation.getPureSourceInformation(identifierContext.getStart()), profile, identifierContext.getText()));
        }
        return tags;
    }

    public RootRouteNode treePath(TreePathContext ctx, ImportGroup importId)
    {
        GenericType treePathGT = this.type(ctx.type(), Lists.mutable.empty(), "", importId, true);
        if (Iterate.notEmpty(treePathGT._typeArguments()))
        {
            throw new PureParserException(treePathGT.getSourceInformation(), "TreePath doesn't support GenericTypes");
        }
        boolean includeAll = ctx.treePathClassBody() != null && ctx.treePathClassBody().simplePropertyFilter() != null && ctx.treePathClassBody().simplePropertyFilter().STAR() != null;
        String rootRouteNodeName = ctx.alias() != null ? ctx.alias().identifier().getText() : ctx.type().qualifiedName().identifier().getText();
        RootRouteNode rootRouteNode = RootRouteNodeInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.getStart(), ctx.getStop()), String.valueOf(includeAll), rootRouteNodeName, treePathGT);
        if (ctx.stereotypes() != null)
        {
            rootRouteNode._stereotypesCoreInstance(this.stereotypes(ctx.stereotypes(), importId));
        }
        if (ctx.taggedValues() != null)
        {
            rootRouteNode._taggedValues(this.taggedValues(ctx.taggedValues(), importId));
        }
        if (ctx.treePathClassBody() != null)
        {
            this.treepathBody(ctx.treePathClassBody(), rootRouteNode, importId, new LambdaContext(importId.getName() + rootRouteNodeName), "");
        }
        return rootRouteNode;
    }

    private void treepathBody(TreePathClassBodyContext ctx, RouteNode routeNode, ImportGroup importId, LambdaContext lambdaContext, String space)
    {
        MutableList<PropertyRouteNode> complexProperties = Lists.mutable.empty();

        if (ctx.simplePropertyFilter() != null)
        {
            MutableList<RouteNodePropertyStub> propertyStubs = Lists.mutable.empty();
            if (ctx.simplePropertyFilter().PLUS() != null || ctx.simplePropertyFilter().MINUS() != null)
            {
                for (SimplePropertyContext spCtx : ctx.simplePropertyFilter().simpleProperty())
                {
                    propertyStubs.add(this.treePathSimpleProperty(spCtx, importId, space, routeNode));
                }
                if (ctx.simplePropertyFilter().PLUS() != null)
                {
                    routeNode._included(propertyStubs);
                }
                else
                {
                    routeNode._excluded(propertyStubs);
                }
            }
        }
        if (ctx.complexProperty() != null)
        {
            for (ComplexPropertyContext cpCtx : ctx.complexProperty())
            {
                complexProperties.add(this.treePathExistingComplexProperty(cpCtx, importId, space, routeNode, lambdaContext));
            }
        }
        if (ctx.derivedProperty() != null)
        {
            for (DerivedPropertyContext dpCtx : ctx.derivedProperty())
            {
                complexProperties.add(this.treePathDerivedComplexProperty(dpCtx, lambdaContext, importId, space));
            }
        }
        if (!complexProperties.isEmpty())
        {
            routeNode._children(complexProperties);
        }
    }

    private RouteNodePropertyStub treePathSimpleProperty(SimplePropertyContext ctx, ImportGroup importId, String space, RouteNode routeNode)
    {
        RouteNodePropertyStub routeNodePropertyStub = this.propertyRef(ctx.propertyRef(), importId, space, routeNode);
        if (ctx.taggedValues() != null)
        {
            routeNodePropertyStub._taggedValues(this.taggedValues(ctx.taggedValues(), importId));
        }
        if (ctx.stereotypes() != null)
        {
            routeNodePropertyStub._stereotypesCoreInstance(this.stereotypes(ctx.stereotypes(), importId));
        }
        return routeNodePropertyStub;
    }


    private ExistingPropertyRouteNode treePathExistingComplexProperty(ComplexPropertyContext ctx, ImportGroup importId, String space, RouteNode routeNode, LambdaContext lambdaContext)
    {
        RouteNodePropertyStub routeNodePropertyStub = this.propertyRef(ctx.propertyRef(), importId, space, routeNode);
        String complexPropertyName = ctx.alias() != null ? ctx.alias().identifier().getText() : ctx.propertyRef().identifier().getText();
        boolean includeAll = ctx.treePathClassBody() != null && ctx.treePathClassBody().simplePropertyFilter() != null && ctx.treePathClassBody().simplePropertyFilter().STAR() != null;
        ExistingPropertyRouteNode epRouteNode = ExistingPropertyRouteNodeInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.getStart(), ctx.getStop()), String.valueOf(includeAll), complexPropertyName, routeNodePropertyStub, ctx.propertyRef().identifier().getText(), null, null);
        if (ctx.treePathClassBody() != null)
        {
            this.treepathBody(ctx.treePathClassBody(), epRouteNode, importId, lambdaContext, space);
        }
        if (ctx.taggedValues() != null)
        {
            epRouteNode._taggedValues(this.taggedValues(ctx.taggedValues(), importId));
        }
        if (ctx.stereotypes() != null)
        {
            epRouteNode._stereotypesCoreInstance(this.stereotypes(ctx.stereotypes(), importId));
        }
        return epRouteNode;
    }

    private NewPropertyRouteNode treePathDerivedComplexProperty(DerivedPropertyContext ctx, LambdaContext lambdaContext, ImportGroup importId, String space)
    {
        String propertyName = ctx.alias() != null ? ctx.alias().identifier().getText() : ctx.propertyRef().identifier().getText();
        boolean includeAll = ctx.treePathClassBody() != null && ctx.treePathClassBody().simplePropertyFilter() != null && ctx.treePathClassBody().simplePropertyFilter().STAR() != null;
        NewPropertyRouteNode dpRouteNode = NewPropertyRouteNodeInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.propertyRef().getStart(), ctx.getStop()), null, String.valueOf(includeAll), propertyName, ctx.propertyRef().identifier().getText(), null, null);
        ListIterable<ValueSpecification> codeSpecifications = this.codeBlock(ctx.codeBlock(), Lists.mutable.empty(), Lists.mutable.empty(), importId, lambdaContext, true, space);
        dpRouteNode._specifications(codeSpecifications);
        NewPropertyRouteNodeFunctionDefinition<?, ?> functionDefinition = NewPropertyRouteNodeFunctionDefinitionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.codeBlock().getStart()), dpRouteNode);
        functionDefinition._expressionSequence(codeSpecifications);
        dpRouteNode._functionDefinition(functionDefinition);
        if (ctx.treePathClassBody() != null)
        {
            this.treepathBody(ctx.treePathClassBody(), dpRouteNode, importId, lambdaContext, space);
        }
        if (ctx.taggedValues() != null)
        {
            dpRouteNode._taggedValues(this.taggedValues(ctx.taggedValues(), importId));
        }
        if (ctx.stereotypes() != null)
        {
            dpRouteNode._stereotypesCoreInstance(this.stereotypes(ctx.stereotypes(), importId));
        }
        return dpRouteNode;
    }

    private RouteNodePropertyStub propertyRef(PropertyRefContext ctx, ImportGroup importId, String space, RouteNode routeNode)
    {
        RouteNodePropertyStub routeNodePropertyStub = RouteNodePropertyStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.identifier().getStart(), ctx.getStop()), routeNode);

        String propertyName = ctx.identifier().getText();
        PropertyStub propertyStub = PropertyStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.identifier().getStart()), null, propertyName);
        routeNodePropertyStub._propertyCoreInstance(Lists.mutable.<CoreInstance>with(propertyStub));
        if (ctx.treePathPropertyParameterType() != null)
        {
            MutableList<InstanceValue> params = Lists.mutable.of();
            for (TreePathPropertyParameterTypeContext fCtx : ctx.treePathPropertyParameterType())
            {
                GenericType type = this.type(fCtx.type(), Lists.mutable.empty(), spacePlusTabs(space, 3), importId, true);
                Multiplicity multiplicity = this.buildMultiplicity(fCtx.multiplicity().multiplicityArgument());
                InstanceValue iv = InstanceValueInstance.createPersistent(this.repository, type, multiplicity);
                params.add(iv);
            }
            routeNodePropertyStub._parameters(params);
        }
        return routeNodePropertyStub;
    }

    private static void setPackage(PackageableElement element, Package pkg)
    {
        element._package(pkg);
        pkg._childrenAdd(element);
    }

    private static void buildAndSetPackage(PackageableElement element, PackagePathContext paths, ModelRepository repository, AntlrSourceInformation sourceInformation)
    {
        setPackage(element, buildPackage(paths, repository, sourceInformation));
    }

    private static Package buildPackage(PackagePathContext paths, ModelRepository repository, AntlrSourceInformation sourceInformation)
    {
        return buildPackage(paths == null ? Lists.immutable.empty() : paths.identifier(), repository, sourceInformation);
    }

    private static Package buildPackage(Iterable<? extends IdentifierContext> paths, ModelRepository repository, AntlrSourceInformation sourceInformation)
    {
        Package parent = (Package) repository.getTopLevel(M3Paths.Root);
        if (parent == null)
        {
            throw new RuntimeException("Cannot find Root in model repository");
        }

        if (paths != null)
        {
            for (IdentifierContext childToken : paths)
            {
                String childName = childToken.getText();
                synchronized (parent)
                {
                    CoreInstance child = _Package.findInPackage(parent, childName);
                    if (child == null)
                    {
                        Package newPackage = PackageInstance.createPersistent(repository, childName, null)
                                ._name(childName)
                                ._package(parent)
                                ._children(Lists.immutable.empty());
                        parent._childrenAdd(newPackage);
                        parent = newPackage;
                    }
                    else if (!(child instanceof Package))
                    {
                        StringBuilder builder = org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.writeUserPathForPackageableElement(new StringBuilder("'"), child)
                                .append("' is a ").append(child.getClassifier().getName()).append(", should be a Package");
                        throw new PureParserException(sourceInformation.getSourceName(), childToken.getStart().getLine(), childToken.getStart().getCharPositionInLine(), builder.toString(), LazyIterate.collect(paths, RuleContext::getText).makeString("::"));
                    }
                    else
                    {
                        parent = (Package) child;
                    }
                }
            }
        }
        return parent;
    }

    public TemporaryPurePropertyMapping mappingLine(MappingLineContext ctx, LambdaContext lambdaContext, String cl, ImportGroup importId)
    {
        String sourceMappingId;
        String targetMappingId;
        if (ctx.sourceAndTargetMappingId() != null)
        {
            SourceAndTargetMappingIdContext sctx = ctx.sourceAndTargetMappingId();
            if (sctx.targetId() != null)
            {
                sourceMappingId = sctx.sourceId().qualifiedName().getText();
                targetMappingId = sctx.targetId().qualifiedName().getText();
            }
            else
            {
                sourceMappingId = null;
                targetMappingId = sctx.sourceId().qualifiedName().getText();
            }
        }
        else
        {
            sourceMappingId = null;
            targetMappingId = null;
        }

        Pair<String, SourceInformation> enumerationMappingInformation;
        if (ctx.ENUMERATION_MAPPING() != null)
        {
            IdentifierContext identifier = ctx.identifier();
            String enumerationMappingName = identifier.getText();
            SourceInformation enumerationMappingReferenceSourceInformation = this.sourceInformation.getPureSourceInformation(identifier.getStart(), identifier.getStart(), identifier.getStop());
            enumerationMappingInformation = Tuples.pair(enumerationMappingName, enumerationMappingReferenceSourceInformation);
        }
        else
        {
            enumerationMappingInformation = null;
        }

        return TemporaryPurePropertyMapping.build(
                this.sourceInformation.getPureSourceInformation(ctx.qualifiedName().getStart()),
                ctx.PLUS() != null,
                ctx.type() == null ? null : this.type(ctx.type(), Lists.mutable.empty(), "", importId, true),
                ctx.multiplicity() == null ? null : this.buildMultiplicity(ctx.multiplicity().multiplicityArgument()),
                ctx.qualifiedName().getText(),
                this.combinedExpression(ctx.combinedExpression(), "", Lists.mutable.empty(), Lists.mutable.empty(), lambdaContext, "", true, importId, true),
                sourceMappingId,
                targetMappingId,
                ctx.STAR() != null,
                enumerationMappingInformation
        );
    }

    public TemporaryPureSetImplementation mapping(MappingContext ctx, String cl, LambdaContext lambdaContext, ImportGroup importId)
    {
        ImportStub src = (ctx.qualifiedName() == null) ?
                null :
                ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.qualifiedName().getStart()), getQualifiedNameString(ctx.qualifiedName()), importId);

        CoreInstance filter = (ctx.combinedExpression() == null) ?
                null :
                combinedExpression(ctx.combinedExpression(), "", Lists.mutable.empty(), Lists.mutable.empty(), lambdaContext, "", true, importId, true);

        MutableList<TemporaryPurePropertyMapping> propertyMappings = ListIterate.collect(ctx.mappingLine(), mlc -> mappingLine(mlc, lambdaContext, cl, importId));
        return TemporaryPureSetImplementation.build(src, filter, propertyMappings);
    }


    public TemporaryPureAggregateSpecification aggregateSpecification(org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.AggregateSpecificationContext aggSpecCtx, ImportGroup importId, LambdaContext lambdaContext, int index)
    {
        boolean canAggregate = "true".equals(aggSpecCtx.BOOLEAN().getText());

        MutableList<TemporaryPureGroupByFunctionSpecification> groupByFunctionSpecifications = Lists.mutable.with();
        MutableList<TemporaryPureAggregationFunctionSpecification> aggregationFunctionSpecifications = Lists.mutable.with();

        org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.GroupByFunctionSpecificationsContext groupByFunctionSpecificationsContext = aggSpecCtx.groupByFunctionSpecifications();
        if (groupByFunctionSpecificationsContext != null)
        {
            for (org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.GroupByFunctionSpecificationContext groupByFunctionSpecificationContext : groupByFunctionSpecificationsContext.groupByFunctionSpecification())
            {
                CoreInstance groupByExpression = this.combinedExpression(groupByFunctionSpecificationContext.combinedExpression(), "", Lists.mutable.empty(), Lists.mutable.empty(), lambdaContext, "", true, importId, true);
                SourceInformation sourceInformation = this.sourceInformation.getPureSourceInformation(groupByFunctionSpecificationContext.getStart(), groupByFunctionSpecificationContext.getStart(), groupByFunctionSpecificationContext.getStop());

                groupByFunctionSpecifications.add(TemporaryPureGroupByFunctionSpecification.build(sourceInformation, groupByExpression));
            }
        }

        org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.AggregationFunctionSpecificationsContext aggregationFunctionSpecificationsContext = aggSpecCtx.aggregationFunctionSpecifications();
        if (aggregationFunctionSpecificationsContext != null)
        {
            for (org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.AggregationFunctionSpecificationContext aggregationFunctionSpecificationContext : aggregationFunctionSpecificationsContext.aggregationFunctionSpecification())
            {
                CoreInstance mapExpression = this.combinedExpression(aggregationFunctionSpecificationContext.combinedExpression(0), "", Lists.mutable.empty(), Lists.mutable.empty(), lambdaContext, "", true, importId, true);
                CoreInstance aggregateExpression = this.combinedExpression(aggregationFunctionSpecificationContext.combinedExpression(1), "", Lists.mutable.empty(), Lists.mutable.empty(), lambdaContext, "", true, importId, true);
                SourceInformation sourceInformation = this.sourceInformation.getPureSourceInformation(aggregationFunctionSpecificationContext.getStart(), aggregationFunctionSpecificationContext.getStart(), aggregationFunctionSpecificationContext.getStop());

                aggregationFunctionSpecifications.add(TemporaryPureAggregationFunctionSpecification.build(sourceInformation, mapExpression, aggregateExpression));
            }
        }

        SourceInformation sourceInformation = this.sourceInformation.getPureSourceInformation(aggSpecCtx.getStart(), aggSpecCtx.getStart(), aggSpecCtx.getStop());
        return TemporaryPureAggregateSpecification.build(sourceInformation, index, canAggregate, groupByFunctionSpecifications, aggregationFunctionSpecifications);
    }

    public TemporaryPurePropertyMapping combinedExpression(CombinedExpressionContext ctx, String property, LambdaContext lambdaContext, ImportGroup importId)
    {
        CoreInstance expression = this.combinedExpression(ctx, "", Lists.mutable.empty(), Lists.mutable.empty(), lambdaContext, "", true, importId, true);
        return TemporaryPurePropertyMapping.build(
                this.sourceInformation.getPureSourceInformation(ctx.getStart()),
                false,
                null,
                null,
                property,
                expression,
                null,
                null,
                false,
                null);
    }

    public TemporaryPureMergeOperationFunctionSpecification mergeOperationSpecification(CombinedExpressionContext ctx, LambdaContext lambdaContext, ImportGroup importId)
    {
        CoreInstance expression = this.combinedExpression(ctx, "", Lists.mutable.empty(), Lists.mutable.empty(), lambdaContext, "", true, importId, true);
        return TemporaryPureMergeOperationFunctionSpecification.build(
                this.sourceInformation.getPureSourceInformation(ctx.getStart()),
                expression);
    }

    private Multiplicity buildMultiplicity(MultiplicityArgumentContext ctx)
    {
        if (ctx.identifier() == null)
        {
            if ((ctx.fromMultiplicity() == null || "1".equals(ctx.fromMultiplicity().getText())) && "1".equals(ctx.toMultiplicity().getText()))
            {
                if (this.pureOne == null)
                {
                    this.pureOne = (Multiplicity) this.processorSupport.package_getByUserPath(M3Paths.PureOne);
                }
                return this.pureOne;
            }
            else if ((ctx.fromMultiplicity() == null || "0".equals(ctx.fromMultiplicity().getText())) && "*".equals(ctx.toMultiplicity().getText()))
            {
                if (this.zeroMany == null)
                {
                    this.zeroMany = (Multiplicity) this.processorSupport.package_getByUserPath(M3Paths.ZeroMany);
                }
                return this.zeroMany;
            }
            else if ((ctx.fromMultiplicity() == null || "0".equals(ctx.fromMultiplicity().getText())) && "0".equals(ctx.toMultiplicity().getText()))
            {
                if (this.pureZero == null)
                {
                    this.pureZero = (Multiplicity) this.processorSupport.package_getByUserPath(M3Paths.PureZero);
                }
                return this.pureZero;
            }
            else if (ctx.fromMultiplicity() != null && "0".equals(ctx.fromMultiplicity().getText()) && "1".equals(ctx.toMultiplicity().getText()))
            {
                if (this.zeroOne == null)
                {
                    this.zeroOne = (Multiplicity) this.processorSupport.package_getByUserPath(M3Paths.ZeroOne);
                }
                return this.zeroOne;
            }
            else if (ctx.fromMultiplicity() != null && "1".equals(ctx.fromMultiplicity().getText()) && "*".equals(ctx.toMultiplicity().getText()))
            {
                if (this.oneMany == null)
                {
                    this.oneMany = (Multiplicity) this.processorSupport.package_getByUserPath(M3Paths.OneMany);
                }
                return this.oneMany;
            }
            else
            {
                String f = ctx.fromMultiplicity() == null ? "*".equals(ctx.toMultiplicity().getText()) ? "0" : ctx.toMultiplicity().getText() : ctx.fromMultiplicity().getText();
                MultiplicityInstance instance = MultiplicityInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.toMultiplicity().getStart()));
                MultiplicityValueInstance lower = MultiplicityValueInstance.createPersistent(this.repository);
                lower._value(Long.parseLong(f));
                MultiplicityValueInstance upper = MultiplicityValueInstance.createPersistent(this.repository);
                if (!"*".equals(ctx.toMultiplicity().getText()))
                {
                    upper._value(Long.parseLong(ctx.toMultiplicity().getText()));
                }
                instance._lowerBound(lower);
                instance._upperBound(upper);
                return instance;
            }
        }
        else
        {
            Token src = ctx.toMultiplicity() == null ? ctx.identifier().getStart() : ctx.toMultiplicity().getStart();
            MultiplicityInstance instance = MultiplicityInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(src));
            instance._multiplicityParameter(ctx.identifier().getText());
            return instance;
        }
    }

    private MutableList<String> packageToList(PackagePathContext pkgCtx)
    {
        return (pkgCtx == null) ? Lists.mutable.empty() : ListIterate.collect(pkgCtx.identifier(), RuleContext::getText);
    }

    private MutableList<String> qualifiedNameToList(QualifiedNameContext qualifiedName)
    {
        return (qualifiedName == null) ? Lists.mutable.empty() : packageToList(qualifiedName.packagePath()).with(qualifiedName.identifier().getText());
    }

    private void checkExists(PackagePathContext pkgCtx, IdentifierContext identifierCtx, SourceInformation sourceInfo) throws PureParserException
    {
        boolean exists;
        MutableList<String> allPaths = Lists.mutable.empty();
        MutableList<String> packagePaths = this.packageToList(pkgCtx);
        allPaths.addAll(packagePaths);
        allPaths.add(identifierCtx.getText());
        try
        {
            exists = this.userPathDefined(allPaths);
        }
        catch (Exception e)
        {
            if (sourceInfo == null)
            {
                sourceInfo = this.sourceInformation.getPureSourceInformation(identifierCtx.getStart(), identifierCtx.getStart(), identifierCtx.getStop());
            }
            throw new PureParserException(sourceInfo, allPaths.makeString("Error determining whether the path ", "::", " exists"), e);
        }
        if (exists)
        {
            if (sourceInfo == null)
            {
                sourceInfo = this.sourceInformation.getPureSourceInformation(identifierCtx.getStart(), identifierCtx.getStart(), identifierCtx.getStop());
            }
            throw new PureParserException(sourceInfo, "The element '" + identifierCtx.getText() + "' already exists in the package '" + (pkgCtx == null ? "::" : this.packageToString(pkgCtx)) + "'");
        }
    }

    private void checkExists(QualifiedNameContext pkgCtx, IdentifierContext identifierCtx, SourceInformation sourceInfo) throws PureParserException
    {
        boolean exists;
        MutableList<String> allPaths = Lists.mutable.empty();
        if (pkgCtx != null)
        {
            MutableList<String> packagePaths = this.packageToList(pkgCtx.packagePath());
            allPaths.addAll(packagePaths);
            allPaths.add(pkgCtx.identifier().getText());
        }
        allPaths.add(identifierCtx.getText());
        try
        {
            exists = this.userPathDefined(allPaths);
        }
        catch (Exception e)
        {
            if (sourceInfo == null)
            {
                sourceInfo = this.sourceInformation.getPureSourceInformation(identifierCtx.getStart(), identifierCtx.getStart(), identifierCtx.getStop());
            }
            throw new PureParserException(sourceInfo, allPaths.makeString("Error determining whether the path ", "::", " exists"), e);
        }
        if (exists)
        {
            if (sourceInfo == null)
            {
                sourceInfo = this.sourceInformation.getPureSourceInformation(identifierCtx.getStart(), identifierCtx.getStart(), identifierCtx.getStop());
            }
            throw new PureParserException(sourceInfo, "The element '" + identifierCtx.getText() + "' already exists in the package '" + (pkgCtx == null ? "::" : this.getQualifiedNameString(pkgCtx)) + "'");
        }
    }

    private void checkExists(Package pkg, String name, SourceInformation sourceInfo)
    {
        if (pkg._children().anySatisfy(c -> name.equals(c.getName())))
        {
            throw new PureParserException(sourceInfo, "The element '" + name + "' already exists in the package '" + org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(pkg) + "'");
        }
    }

    @Deprecated
    public ImportGroupInstance buildImportGroupFromImport(String fileName, int count, ListIterable<? extends Import> imports, SourceInformation sourceInfo)
    {
        String id = createImportGroupId(fileName, count);
        ImportGroupInstance ig = ImportGroupInstance.createPersistent(this.repository, id, sourceInfo);
        ig._imports(imports);

        Package parent = (Package) this.processorSupport.package_getByUserPath("system::imports");
        parent._childrenAdd(ig);
        ig._package(parent);
        ig._name(id);
        return ig;
    }

    public static String createImportGroupId(String fileName, int count)
    {
        // TODO fix this to ensure ids are unique
        return Source.importForSourceName(fileName) + "_" + count;
    }

    private static CoreInstance findEnum(String enumerationFullPath, final String enumName, ModelRepository repository)
    {
        EnumerationInstance enumerationInstance = (EnumerationInstance) org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.findPackageableElement(enumerationFullPath, repository);
        return enumerationInstance._values().detect(each -> enumName.equals(each.getName()));
    }

    private InstanceValue doWrap(ListIterable<? extends CoreInstance> content, int beginLine, int beginColumn, int endLine, int endColumn)
    {
        return InstanceValueInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(beginLine, beginColumn, endLine, endColumn), null, null)
                ._values(content);
    }

    private InstanceValue doWrap(ListIterable<? extends CoreInstance> content)
    {
        return InstanceValueInstance.createPersistent(this.repository, null, null)
                ._values(content);
    }

    private InstanceValue doWrap(CoreInstance content)
    {
        return InstanceValueInstance.createPersistent(this.repository, null, null)
                ._values(Lists.fixedSize.of(content));
    }

    private InstanceValue doWrap(CoreInstance content, Token token)
    {
        return this.doWrap(Lists.fixedSize.of(content), token);
    }

    private InstanceValue doWrap(ListIterable<? extends CoreInstance> content, Token token)
    {
        return InstanceValueInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(token), null, null)
                ._values(content);
    }

    private InstanceValue doWrap(IdentifierContext content, boolean addQuote)
    {
        CoreInstance stringInstance = this.repository.newStringCoreInstance_cached(addQuote ? "'" + content.getText() + "'" : content.getText());
        return this.doWrap(Lists.mutable.with(stringInstance), content.getStart());
    }

    private InstanceValue doWrap(PropertyNameContext content)
    {
        CoreInstance stringInstance = this.repository.newStringCoreInstance_cached(removeQuotes(content.getText()));
        return this.doWrap(Lists.mutable.with(stringInstance), content.getStart());
    }

    public static String removeQuotes(String name)
    {
        name = name.trim();
        return name.startsWith("'") ? name.substring(1, name.length() - 1) : name;
    }

    private InstanceValue doWrap(MutableList<PropertyNameContext> content)
    {
        ListIterable<CoreInstance> values = content.collect(val -> this.repository.newStringCoreInstance_cached(removeQuotes(val.getText())));
        return InstanceValueInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(content.getFirst().getStart(), content.getFirst().getStart(), content.getLast().getStart()), null, null)
                ._values(values);
    }

    private boolean userPathDefined(RichIterable<String> paths)
    {
        if (paths.isEmpty())
        {
            return false;
        }
        if (paths.size() == 1)
        {
            // Check Primitives
            if (this.repository.getTopLevel(paths.getFirst()) != null)
            {
                return true;
            }
        }
        CoreInstance instance = this.repository.getTopLevel(M3Paths.Root);
        for (String path : paths)
        {
            if (instance == null || instance.getClassifier() == null)
            {
                return false;
            }
            instance = instance.getValueInValueForMetaPropertyToMany(M3Properties.children, path);
        }
        return instance != null && instance.getClassifier() != null;
    }

    private String tabs(int count)
    {
        switch (count)
        {
            case 0:
            {
                return "";
            }
            case 1:
            {
                return this.tab;
            }
            default:
            {
                return tabs(new StringBuilder(count * this.tab.length()), count).toString();
            }
        }
    }

    private String spacePlusTabs(String space, int tabs)
    {
        switch (tabs)
        {
            case 0:
            {
                return space;
            }
            case 1:
            {
                return space.concat(this.tab);
            }
            default:
            {
                return tabs(new StringBuilder(space.length() + (tabs * this.tab.length())).append(space), tabs).toString();
            }
        }
    }

    private StringBuilder tabs(StringBuilder builder, int count)
    {
        builder.ensureCapacity(builder.length() + (count * this.tab.length()));
        for (int i = 0; i < count; i++)
        {
            builder.append(this.tab);
        }
        return builder;
    }

    private Multiplicity getPureOne()
    {
        if (this.pureOne == null)
        {
            this.pureOne = (Multiplicity) this.processorSupport.package_getByUserPath(M3Paths.PureOne);
        }
        return this.pureOne;
    }

    private Multiplicity getOneMany()
    {
        if (this.oneMany == null)
        {
            this.oneMany = (Multiplicity) this.processorSupport.package_getByUserPath(M3Paths.OneMany);
        }
        return this.oneMany;
    }

    public static final class LambdaContext
    {
        private int lambdaFunctionCounter = 0;
        private final String lambdaFunctionOwnerId;

        public LambdaContext(String lambdaFunctionOwnerId)
        {
            this.lambdaFunctionOwnerId = lambdaFunctionOwnerId;
        }

        public String getLambdaFunctionUniqueName()
        {
            return this.lambdaFunctionOwnerId + '$' + this.lambdaFunctionCounter++;
        }
    }
}
