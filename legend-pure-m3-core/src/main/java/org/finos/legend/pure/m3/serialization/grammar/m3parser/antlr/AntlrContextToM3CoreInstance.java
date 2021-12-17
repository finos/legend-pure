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

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringEscapeUtils;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function0;
import org.eclipse.collections.api.block.predicate.Predicate2;
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
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.compiler.postprocessing.processor.milestoning.MilestoningClassProcessor;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.PackageInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.functions.lang.KeyExpressionInstance;
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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.AssociationInstance;
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
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.ClassProjectionInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.EnumerationInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionTypeInstance;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.MeasureInstance;
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
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
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
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.FunctionDefinitionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.FunctionExpressionParametersContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.FunctionTypePureTypeContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.FunctionTypeSignatureContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.FunctionVariableExpressionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.IdentifierContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.Import_statementContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ImportsContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.InstanceAtomicRightSideContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.InstanceContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.InstanceLiteralContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.InstanceLiteralTokenContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.InstancePropertyAssignmentContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.InstanceReferenceContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.InstanceRightSideContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.LambdaParamTypeContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.LambdaPipeContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.LetExpressionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MappingContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MappingLineContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MultiplicityArgumentContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MultiplicityArgumentsContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.NativeFunctionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.NotExpressionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.PackagePathContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ProfileContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ProgramLineContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.PropertiesContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.PropertyContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.PropertyExpressionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.PropertyOrFunctionExpressionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.PropertyRefContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.QualifiedNameContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.QualifiedPropertyContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.SignedExpressionContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.SimplePropertyContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.SourceAndTargetMappingIdContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.StereotypeContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.StereotypeDefinitionsContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.StereotypesContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TagDefinitionsContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TaggedValueContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TaggedValuesContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TreePathClassBodyContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TreePathContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TreePathPropertyParameterTypeContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TypeAndMultiplicityParametersContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TypeArgumentsContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TypeContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TypeParameterContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.TypeParametersWithContravarianceAndMultiplicityParametersContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.VariableContext;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSL;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLLibrary;
import org.finos.legend.pure.m3.serialization.grammar.m3parser.inlinedsl.InlineDSLTextContent;
import org.finos.legend.pure.m3.serialization.runtime.Source;
import org.finos.legend.pure.m3.serialization.runtime.SourceState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;
import org.finos.legend.pure.m4.serialization.grammar.antlr.AntlrSourceInformation;
import org.finos.legend.pure.m4.serialization.grammar.antlr.PureParserException;

import java.util.List;

public class AntlrContextToM3CoreInstance
{
    private final String tab = "    ";
    private final String tilde = "~";
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

    private boolean hasImportChanged;

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
        CoreInstance result = null;
        ImportGroup importId = null;
        if (ctx.imports() != null)
        {
            importId = this.imports(ctx.imports());

            if (this.oldState == null)
            {
                this.hasImportChanged = true;
            }
            else
            {
                String importGroupID = importId._name();
                RichIterable<? extends PackageableElement> oldImportGroups = this.oldState.getImportGroups().select(e -> importGroupID.equals(e._name()));

                if (oldImportGroups.size() == 1)
                {
                    ImportGroup oldImportGroup = (ImportGroup) oldImportGroups.toList().get(0);

                    MutableSet<String> oldPaths = oldImportGroup._imports().collect(Import::_path, Sets.mutable.empty());
                    MutableSet<String> newPaths = importId._imports().collect(Import::_path, Sets.mutable.empty());

                    if (oldPaths.equals(newPaths))
                    {
                        this.hasImportChanged = false;
                        PackageInstance parent = (PackageInstance) this.processorSupport.package_getByUserPath("system::imports");
                        parent._children(parent._children().reject(e -> importGroupID.equals(e._name())));
                        parent._childrenAdd(oldImportGroup);
                        oldImportGroup._package(parent);
                        oldImportGroup.setSourceInformation(importId.getSourceInformation());
                        oldImportGroup._imports(importId._imports());
                        importId = oldImportGroup;
                    }
                    else
                    {
                        this.hasImportChanged = true;
                    }
                }
                else
                {
                    this.hasImportChanged = true;
                }
            }
        }
        if (ctx.profile() != null)
        {
            for (ProfileContext pCtx : ctx.profile())
            {
                if (this.hasImportChanged)
                {
                    result = this.profile(pCtx);
                }
                else
                {
                    String importGroupID = importId._name();
                    String newContent = Lists.mutable.with(pCtx.start.getInputStream().getText(new Interval(pCtx.start.getStartIndex(), pCtx.stop.getStopIndex())).split("\\r?\\n")).makeString("", System.lineSeparator(), System.lineSeparator());
                    MutableList<CoreInstance> oldInstances = this.oldInstances.select(i -> (this.oldState != null) && this.oldState.instanceImportGroupInSourceEqualsNewImportGroup(i, importGroupID) && this.oldState.instanceContentInSourceEqualsNewContent(i, newContent), Lists.mutable.empty());

                    if (oldInstances.size() == 1)
                    {
                        CoreInstance thisInstance = oldInstances.get(0);
                        this.oldInstances.remove(thisInstance);
                        SourceInformation newSourceInfo = this.sourceInformation.getPureSourceInformation(pCtx.getStart(), pCtx.qualifiedName().getStop(), pCtx.getStop());
                        if (thisInstance.getSourceInformation().getStartColumn() == newSourceInfo.getStartColumn())
                        {
                            this.offsetSourceInformationForInstanceAndChildren(thisInstance, newSourceInfo.getStartLine() - thisInstance.getSourceInformation().getStartLine());
                            PackageInstance packageInstance = this.buildPackage(pCtx.qualifiedName().packagePath());
                            ((PackageableElement) thisInstance)._package(packageInstance);
                            packageInstance._childrenAdd((PackageableElement) thisInstance);
                            result = thisInstance;
                        }
                        else
                        {
                            result = this.profile(pCtx);
                        }
                    }
                    else
                    {
                        result = this.profile(pCtx);
                    }
                }
                this.coreInstancesResult.add(result);
            }
        }
        if (ctx.classDefinition() != null)
        {
            for (ClassDefinitionContext dCtx : ctx.classDefinition())
            {
                if (this.hasImportChanged)
                {
                    result = this.classParser(dCtx, importId, this.addLines);
                }
                else
                {
                    String importGroupID = importId._name();
                    String newContent = Lists.mutable.with(dCtx.start.getInputStream().getText(new Interval(dCtx.start.getStartIndex(), dCtx.stop.getStopIndex())).split("\\r?\\n")).makeString("", System.lineSeparator(), System.lineSeparator());
                    List<CoreInstance> oldInstances = this.oldInstances.select(i -> (this.oldState != null) && this.oldState.instanceImportGroupInSourceEqualsNewImportGroup(i, importGroupID) && this.oldState.instanceContentInSourceEqualsNewContent(i, newContent), Lists.mutable.empty());

                    if (oldInstances.size() == 1)
                    {
                        CoreInstance thisInstance = oldInstances.get(0);
                        this.oldInstances.remove(thisInstance);
                        SourceInformation newSourceInfo = this.sourceInformation.getPureSourceInformation(dCtx.getStart(), dCtx.qualifiedName().identifier().getStart(), dCtx.getStop());
                        if (thisInstance.getSourceInformation().getStartColumn() == newSourceInfo.getStartColumn())
                        {
                            this.offsetSourceInformationForInstanceAndChildren(thisInstance, newSourceInfo.getStartLine() - thisInstance.getSourceInformation().getStartLine());
                            PackageInstance packageInstance = this.buildPackage(dCtx.qualifiedName().packagePath());
                            ((PackageableElement) thisInstance)._package(packageInstance);
                            packageInstance._childrenAdd((PackageableElement) thisInstance);
                            result = thisInstance;
                        }
                        else
                        {
                            result = this.classParser(dCtx, importId, this.addLines);
                        }
                    }
                    else
                    {
                        result = this.classParser(dCtx, importId, this.addLines);
                    }
                }
                this.coreInstancesResult.add(result);
            }
        }
        if (ctx.measureDefinition() != null)
        {
            for (org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MeasureDefinitionContext dCtx : ctx.measureDefinition())
            {
                if (this.hasImportChanged)
                {
                    result = this.measureParser(dCtx, importId, this.addLines);
                }
                else
                {
                    String importGroupID = importId._name();
                    String newContent = Lists.mutable.with(dCtx.start.getInputStream().getText(new Interval(dCtx.start.getStartIndex(), dCtx.stop.getStopIndex())).split("\\r?\\n")).makeString("", System.lineSeparator(), System.lineSeparator());
                    List<CoreInstance> oldInstances = this.oldInstances.select(i -> (this.oldState != null) && this.oldState.instanceImportGroupInSourceEqualsNewImportGroup(i, importGroupID) && this.oldState.instanceContentInSourceEqualsNewContent(i, newContent), Lists.mutable.empty());

                    Predicate2<CoreInstance, SourceInformation> matchesOldInstance = (oldMeasureInstance, newSourceInfo) ->
                    {
                        if (newSourceInfo == null || oldMeasureInstance.getSourceInformation().getStartColumn() == newSourceInfo.getStartColumn())
                        {
                            PackageInstance packageInstance;
                            if (newSourceInfo != null)
                            {
                                offsetSourceInformationForInstanceAndChildren(oldMeasureInstance, newSourceInfo.getStartLine() - oldMeasureInstance.getSourceInformation().getStartLine());
                            }
                            Function<String, IdentifierContext> newIdentifierContext = s -> new IdentifierContext(null, 1)
                            {
                                @Override
                                public String getText()
                                {
                                    return s;
                                }
                            };
                            packageInstance = buildPackage(Lists.mutable.with(newIdentifierContext.valueOf(oldMeasureInstance.getValueForMetaPropertyToOne(M3Properties._package).getName())));
                            ((PackageableElement) oldMeasureInstance)._package(packageInstance);
                            packageInstance._childrenAdd((PackageableElement) oldMeasureInstance);
                            return true;
                        }
                        return false;
                    };

                    CoreInstance oldMeasureInstance = ListIterate.detect(oldInstances, i -> Instance.instanceOf(i, M3Paths.Measure, this.processorSupport));

                    if (oldMeasureInstance != null)
                    {
                        this.oldInstances.remove(oldMeasureInstance);
                        SourceInformation newSourceInfo = this.sourceInformation.getPureSourceInformation(dCtx.getStart(), dCtx.qualifiedName().identifier().getStart(), dCtx.getStop());
                        if (matchesOldInstance.accept(oldMeasureInstance, newSourceInfo))
                        {
                            MeasureInstance mI = (MeasureInstance) oldMeasureInstance;
                            result = mI;

                            MutableList<Unit> units = Lists.mutable.with(mI._canonicalUnit()).withAll(mI._nonCanonicalUnits());
                            for (Unit unit : units)
                            {
                                PackageInstance packageInstance = buildPackage(Lists.mutable.withAll(dCtx.qualifiedName().packagePath() == null ? Lists.mutable.empty() : ListAdapter.adapt(dCtx.qualifiedName().packagePath().identifier())));
                                unit._package(packageInstance);
                                packageInstance._childrenAdd(unit);
                            }
                        }
                        else
                        {
                            result = this.measureParser(dCtx, importId, this.addLines);
                        }
                    }
                    else
                    {
                        result = this.measureParser(dCtx, importId, this.addLines);
                    }
                }
                MeasureInstance mI = (MeasureInstance) result;
                List<Unit> allMeasureUnitInstances = Lists.mutable.<Unit>withAll(mI._nonCanonicalUnits()).with(mI._canonicalUnit()).toList();
                this.coreInstancesResult.add(result);
                this.coreInstancesResult.addAll(allMeasureUnitInstances);
            }
        }
        if (ctx.association() != null)
        {
            for (AssociationContext pCtx : ctx.association())
            {

                if (this.hasImportChanged)
                {
                    result = this.associationParser(pCtx, importId);
                }
                else
                {
                    String importGroupID = importId._name();
                    String newContent = Lists.mutable.with(pCtx.start.getInputStream().getText(new Interval(pCtx.start.getStartIndex(), pCtx.stop.getStopIndex())).split("\\r?\\n")).makeString("", System.lineSeparator(), System.lineSeparator());
                    List<CoreInstance> oldInstances = this.oldInstances.select(i -> (this.oldState != null) && this.oldState.instanceImportGroupInSourceEqualsNewImportGroup(i, importGroupID) && this.oldState.instanceContentInSourceEqualsNewContent(i, newContent), Lists.mutable.empty());

                    if (oldInstances.size() == 1)
                    {
                        CoreInstance thisInstance = oldInstances.get(0);
                        this.oldInstances.remove(thisInstance);
                        SourceInformation newSourceInfo = this.sourceInformation.getPureSourceInformation(pCtx.ASSOCIATION().getSymbol(), pCtx.qualifiedName().identifier().getStart(), pCtx.getStop());
                        if (thisInstance.getSourceInformation().getStartColumn() == newSourceInfo.getStartColumn())
                        {
                            this.offsetSourceInformationForInstanceAndChildren(thisInstance, newSourceInfo.getStartLine() - thisInstance.getSourceInformation().getStartLine());
                            PackageInstance packageInstance = this.buildPackage(pCtx.qualifiedName().packagePath());
                            ((PackageableElement) thisInstance)._package(packageInstance);
                            packageInstance._childrenAdd((PackageableElement) thisInstance);
                            result = thisInstance;
                        }
                        else
                        {
                            result = this.associationParser(pCtx, importId);
                        }
                    }
                    else
                    {
                        result = this.associationParser(pCtx, importId);
                    }
                }
                this.coreInstancesResult.add(result);
            }
        }
        if (ctx.enumDefinition() != null)
        {
            for (EnumDefinitionContext pCtx : ctx.enumDefinition())
            {
                if (this.hasImportChanged)
                {
                    result = this.enumParser(pCtx, importId);
                }
                else
                {
                    String importGroupID = importId._name();
                    String newContent = Lists.mutable.with(pCtx.start.getInputStream().getText(new Interval(pCtx.start.getStartIndex(), pCtx.stop.getStopIndex())).split("\\r?\\n")).makeString("", System.lineSeparator(), System.lineSeparator());
                    List<CoreInstance> oldInstances = this.oldInstances.select(i -> (this.oldState != null) && this.oldState.instanceImportGroupInSourceEqualsNewImportGroup(i, importGroupID) && this.oldState.instanceContentInSourceEqualsNewContent(i, newContent), Lists.mutable.empty());

                    if (oldInstances.size() == 1)
                    {
                        CoreInstance thisInstance = oldInstances.get(0);
                        this.oldInstances.remove(thisInstance);
                        SourceInformation newSourceInfo = this.sourceInformation.getPureSourceInformation(pCtx.getStart(), pCtx.qualifiedName().identifier().getStart(), pCtx.getStop());
                        if (thisInstance.getSourceInformation().getStartColumn() == newSourceInfo.getStartColumn())
                        {
                            this.offsetSourceInformationForInstanceAndChildren(thisInstance, newSourceInfo.getStartLine() - thisInstance.getSourceInformation().getStartLine());
                            PackageInstance packageInstance = this.buildPackage(pCtx.qualifiedName().packagePath());
                            ((PackageableElement) thisInstance)._package(packageInstance);
                            packageInstance._childrenAdd((PackageableElement) thisInstance);
                            result = thisInstance;
                        }
                        else
                        {
                            result = this.enumParser(pCtx, importId);
                        }
                    }
                    else
                    {
                        result = this.enumParser(pCtx, importId);
                    }
                }
                this.coreInstancesResult.add(result);
            }
        }
        if (ctx.nativeFunction() != null)
        {
            for (NativeFunctionContext pCtx : ctx.nativeFunction())
            {
                if (this.hasImportChanged)
                {
                    this.nativeFunction(pCtx, importId, "", this.coreInstancesResult);
                }
                else
                {
                    String importGroupID = importId._name();
                    String newContent = Lists.mutable.with(pCtx.start.getInputStream().getText(new Interval(pCtx.start.getStartIndex(), pCtx.stop.getStopIndex())).split("\\r?\\n")).makeString("", System.lineSeparator(), System.lineSeparator());
                    List<CoreInstance> oldInstances = this.oldInstances.select(i -> (this.oldState != null) && this.oldState.instanceImportGroupInSourceEqualsNewImportGroup(i, importGroupID) && this.oldState.instanceContentInSourceEqualsNewContent(i, newContent), Lists.mutable.empty());

                    if (oldInstances.size() == 1)
                    {
                        CoreInstance thisInstance = oldInstances.get(0);
                        this.oldInstances.remove(thisInstance);
                        SourceInformation newSourceInfo = this.sourceInformation.getPureSourceInformation(pCtx.NATIVE().getSymbol(), pCtx.qualifiedName().identifier().getStart(), pCtx.END_LINE().getSymbol());
                        if (thisInstance.getSourceInformation().getStartColumn() == newSourceInfo.getStartColumn())
                        {
                            this.functionCounter++;
                            this.offsetSourceInformationForInstanceAndChildren(thisInstance, newSourceInfo.getStartLine() - thisInstance.getSourceInformation().getStartLine());
                            PackageInstance packageInstance = this.buildPackage(pCtx.qualifiedName().packagePath());
                            ((PackageableElement) thisInstance)._package(packageInstance);
                            packageInstance._childrenAdd((PackageableElement) thisInstance);
                            this.coreInstancesResult.add(thisInstance);
                        }
                        else
                        {
                            this.nativeFunction(pCtx, importId, "", this.coreInstancesResult);
                        }
                    }
                    else
                    {
                        this.nativeFunction(pCtx, importId, "", this.coreInstancesResult);
                    }
                }
            }
        }
        if (ctx.functionDefinition() != null)
        {
            for (FunctionDefinitionContext pCtx : ctx.functionDefinition())
            {
                if (this.hasImportChanged)
                {
                    result = this.concreteFunctionDefinition(pCtx, importId, true, "", this.coreInstancesResult);
                }
                else
                {
                    String importGroupID = importId._name();
                    String newContent = Lists.mutable.with(pCtx.start.getInputStream().getText(new Interval(pCtx.start.getStartIndex(), pCtx.stop.getStopIndex())).split("\\r?\\n")).makeString("", System.lineSeparator(), System.lineSeparator());
                    List<CoreInstance> oldInstances = this.oldInstances.select(i -> (this.oldState != null) && this.oldState.instanceImportGroupInSourceEqualsNewImportGroup(i, importGroupID) && this.oldState.instanceContentInSourceEqualsNewContent(i, newContent), Lists.mutable.empty());

                    if (oldInstances.size() == 1)
                    {
                        CoreInstance thisInstance = oldInstances.get(0);
                        this.oldInstances.remove(thisInstance);
                        SourceInformation newSourceInfo = this.sourceInformation.getPureSourceInformation(pCtx.FUNCTION().getSymbol(), pCtx.qualifiedName().identifier().getStart(), pCtx.getStop());
                        if (thisInstance.getSourceInformation().getStartColumn() == newSourceInfo.getStartColumn())
                        {
                            this.functionCounter++;
                            this.offsetSourceInformationForInstanceAndChildren(thisInstance, newSourceInfo.getStartLine() - thisInstance.getSourceInformation().getStartLine());
                            PackageInstance packageInstance = this.buildPackage(pCtx.qualifiedName().packagePath());
                            ((PackageableElement) thisInstance)._package(packageInstance);
                            packageInstance._childrenAdd((PackageableElement) thisInstance);
                            this.coreInstancesResult.add(thisInstance);
                            result = thisInstance;
                        }
                        else
                        {
                            result = this.concreteFunctionDefinition(pCtx, importId, true, "", this.coreInstancesResult);
                        }
                    }
                    else
                    {
                        result = this.concreteFunctionDefinition(pCtx, importId, true, "", this.coreInstancesResult);
                    }
                }
            }
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

    private void offsetSourceInformationForInstanceAndChildren(CoreInstance instance, int offset)
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
                else if (oldSourceInfo.getSourceId().equals(sourceInfo.getSourceId()) && oldSourceInfo.contains(sourceInfo))
                {
                    this.newSourceInfoMap.add(Tuples.pair(next, new SourceInformation(sourceInfo.getSourceId(), sourceInfo.getStartLine() + offset, sourceInfo.getStartColumn(), sourceInfo.getLine() + offset, sourceInfo.getColumn(), sourceInfo.getEndLine() + offset, sourceInfo.getEndColumn())));
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
        MutableList<Import> imports = Lists.mutable.empty();
        int importGroupStartLine = -1;
        int importGroupStartColumn = -1;
        int importGroupEndLine = -1;
        int importGroupEndColumn = -1;
        for (Import_statementContext isCtx : ctx.import_statement())
        {
            Import _import = ImportInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(isCtx.getStart(), isCtx.packagePath().getStart(), isCtx.STAR().getSymbol()), this.packageToString(isCtx.packagePath()));

            imports.add(_import);
            SourceInformation sourceInfo = _import.getSourceInformation();
            if (importGroupStartLine == -1)
            {
                importGroupStartLine = sourceInfo.getStartLine();
                importGroupStartColumn = sourceInfo.getStartColumn();
                importGroupEndLine = sourceInfo.getEndLine();
                importGroupEndColumn = sourceInfo.getEndColumn();
            }
            if (importGroupStartLine > sourceInfo.getStartLine())
            {
                importGroupStartLine = sourceInfo.getStartLine();
                importGroupStartColumn = sourceInfo.getStartColumn();
            }
            if (importGroupEndLine < sourceInfo.getEndLine())
            {
                importGroupEndLine = sourceInfo.getEndLine();
                importGroupEndColumn = sourceInfo.getEndColumn();
            }
        }
        if (importGroupStartLine == -1)
        {
            importGroupStartLine = 1 + this.sourceInformation.getOffsetLine();
            importGroupStartColumn = 0;
            importGroupEndLine = 1 + this.sourceInformation.getOffsetLine();
            importGroupEndColumn = 0;
        }
        return buildImportGroupFromImport(this.sourceInformation.getSourceName(), this.count, imports, new SourceInformation(this.sourceInformation.getSourceName(), importGroupStartLine, importGroupStartColumn, importGroupEndLine, importGroupEndColumn));
    }

    public CoreInstance combinedExpression(CombinedExpressionContext ctx, String exprName, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        CoreInstance result = this.expressionOrExpressionGroup(ctx.expressionOrExpressionGroup(), exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
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
                        boolResult = this.booleanPart(bool, (ValueSpecification) arithResult, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
                        bool.clear();
                    }
                    arth.add(epCtx.arithmeticPart());
                }
                else if (epCtx.booleanPart() != null)
                {
                    if (!arth.isEmpty())
                    {
                        arithResult = this.arithmeticPart(arth, boolResult, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
                        arth.clear();
                    }
                    bool.add(epCtx.booleanPart());
                }
            }

            // Invariant allows us to make the choice here - either we still have arth to process or bool to process but not both 
            if (!arth.isEmpty())
            {
                result = this.arithmeticPart(arth, boolResult, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            else if (!bool.isEmpty())
            {
                result = this.booleanPart(bool, (ValueSpecification) arithResult, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
        }
        return result;
    }

    private CoreInstance expressionOrExpressionGroup(ExpressionOrExpressionGroupContext ctx, String exprName, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        return this.expression(ctx.expression(), exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
    }

    private enum BoolOp
    {
        AND, OR
    }

    private boolean isLowerPrecedenceBoolean(String boolOp1, String boolOp2)
    {
        return "or".equals(boolOp1) && "and".equals(boolOp2);
    }

    private SimpleFunctionExpression buildBoolean(BooleanPartContext ctx, BoolOp op, CoreInstance initialValue, String exprName, MutableList<String> typeParamtersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
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

        CoreInstance other = this.expression(ctx.expression(), exprName, typeParamtersNames, lambdaContext, space, wrapFlag, importId, addLines);
        SimpleFunctionExpression sfe = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(terminalNode.getSymbol()), null, null, importId, null);
        sfe._functionName(op.name().toLowerCase());
        sfe._parametersValues(Lists.mutable.of((ValueSpecification) initialValue, (ValueSpecification) other));
        return sfe;
    }

    private SimpleFunctionExpression processBooleanOp(SimpleFunctionExpression sfe, BooleanPartContext ctx, BoolOp op, CoreInstance initialValue, String exprName, MutableList<String> typeParamtersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        if (sfe == null)
        {
            return buildBoolean(ctx, op, initialValue, exprName, typeParamtersNames, lambdaContext, space, wrapFlag, importId, addLines);
        }

        if (isLowerPrecedenceBoolean(sfe.getValueForMetaPropertyToOne("functionName").getName(), op.name().toLowerCase()))
        {
            ListIterable<? extends CoreInstance> params = sfe.getValueForMetaPropertyToMany("parametersValues");
            SimpleFunctionExpression newSfe = buildBoolean(ctx, op, params.getLast(), exprName, typeParamtersNames, lambdaContext, space, wrapFlag, importId, addLines);
            MutableList<CoreInstance> l = Lists.mutable.withAll(params.subList(0, params.size() - 1));
            sfe._parametersValues(Lists.mutable.of((ValueSpecification) l.get(0), newSfe));
            return sfe;
        }

        return buildBoolean(ctx, op, sfe, exprName, typeParamtersNames, lambdaContext, space, wrapFlag, importId, addLines);
    }

    private SimpleFunctionExpression booleanPart(List<BooleanPartContext> bList, ValueSpecification input, String exprName, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        SimpleFunctionExpression sfe = null;
        for (BooleanPartContext ctx : bList)
        {
            if (ctx.AND() != null)
            {
                sfe = processBooleanOp(sfe, ctx, BoolOp.AND, input, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            else if (ctx.OR() != null)
            {
                sfe = processBooleanOp(sfe, ctx, BoolOp.OR, input, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            else
            {
                sfe = this.equalNotEqual(ctx.equalNotEqual(), sfe == null ? input : sfe, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
        }
        return sfe;
    }

    private CoreInstance expression(ExpressionContext ctx, String exprName, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        CoreInstance result = null;
        CoreInstance end = null;
        CoreInstance step = null;
        MutableList<CoreInstance> expressions = Lists.mutable.of();
        MutableList<ValueSpecification> parameters = Lists.mutable.empty();
        if (ctx.combinedExpression() != null)
        {
            return this.combinedExpression(ctx.combinedExpression(), exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
        }

        if (ctx.atomicExpression() != null)
        {
            result = this.atomicExpression(ctx.atomicExpression(), typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
        }
        else if (ctx.notExpression() != null)
        {
            result = this.notExpression(ctx.notExpression(), exprName, typeParametersNames, lambdaContext, space, importId, addLines);
        }
        else if (ctx.signedExpression() != null)
        {
            result = this.signedExpression(ctx.signedExpression(), exprName, typeParametersNames, lambdaContext, space, importId, addLines);
        }
        else if (ctx.expressionsArray() != null)
        {
            for (ExpressionContext eCtx : ctx.expressionsArray().expression())
            {
                expressions.add(this.expression(eCtx, exprName, typeParametersNames, lambdaContext, space, false, importId, addLines));
            }
            result = this.doWrap(expressions, ctx.expressionsArray().getStart().getLine(), ctx.expressionsArray().getStart().getCharPositionInLine(), ctx.getStop().getLine(), ctx.getStop().getCharPositionInLine());
        }
        else
        {
            switch (ctx.sliceExpression().expression().size())
            {
                case 1: //:end
                {
                    end = this.expression(ctx.sliceExpression().expression(0), exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
                    break;
                }
                case 2: //start:end
                {
                    result = this.expression(ctx.sliceExpression().expression(0), exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
                    expressions.add(result);
                    end = this.expression(ctx.sliceExpression().expression(1), exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
                    break;
                }
                case 3: //start:end:step
                {
                    result = this.expression(ctx.sliceExpression().expression(0), exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
                    expressions.add(result);
                    end = this.expression(ctx.sliceExpression().expression(1), exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
                    step = this.expression(ctx.sliceExpression().expression(2), exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
                    break;
                }
                default:
                {
                    //Not reachable. coded just for comment
                    break;
                }
            }
            MutableList<ValueSpecification> params = Lists.mutable.empty();
            if (result != null)
            {
                params.add(this.doWrap(Lists.mutable.of(result)));
            }
            params.add(this.doWrap(Lists.mutable.of(end)));
            if (step != null)
            {
                params.add(this.doWrap(Lists.mutable.of(step)));
            }
            SimpleFunctionExpressionInstance sfe = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.getStart()), null, null, importId, null);
            sfe._functionName("range");
            sfe._parametersValues(params);
            result = sfe;
        }
        if (ctx.propertyOrFunctionExpression() != null)
        {
            for (PropertyOrFunctionExpressionContext pfCtx : ctx.propertyOrFunctionExpression())
            {
                if (pfCtx.propertyExpression() != null)
                {
                    result = propertyExpression(pfCtx.propertyExpression(), result, parameters, typeParametersNames, lambdaContext, space, importId);
                }
                else
                {
                    for (int i = 0; i < pfCtx.functionExpression().qualifiedName().size(); i++)
                    {
                        parameters = this.functionExpressionParameters(pfCtx.functionExpression().functionExpressionParameters(i), typeParametersNames, importId, lambdaContext, addLines, spacePlusTabs(space, 4));
                        parameters.add(0, (ValueSpecification) result);
                        result = this.functionExpression(pfCtx.functionExpression().qualifiedName(i), parameters, importId);
                    }
                }
            }
        }


        if (ctx.equalNotEqual() != null)
        {
            result = this.equalNotEqual(ctx.equalNotEqual(), (ValueSpecification) result, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
        }
        return result;
    }

    private CoreInstance propertyExpression(PropertyExpressionContext ctx, CoreInstance result, MutableList<ValueSpecification> parameters, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, ImportGroup importId)
    {
        parameters.clear();
        boolean function = false;
        IdentifierContext property = ctx.identifier();
        CoreInstance parameter;
        if (ctx.functionExpressionParameters() != null)
        {
            function = true;
            FunctionExpressionParametersContext fepCtx = ctx.functionExpressionParameters();
            if (fepCtx.combinedExpression() != null)
            {
                for (CombinedExpressionContext ceCtx : fepCtx.combinedExpression())
                {
                    parameter = this.combinedExpression(ceCtx, "param", typeParametersNames, lambdaContext, spacePlusTabs(space, 4), true, importId, addLines);
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
            InstanceValue instanceValue = this.doWrap(property, false);
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
            InstanceValue instanceValue = this.doWrap(property, false);
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

    private SimpleFunctionExpression signedExpression(SignedExpressionContext ctx, String exprName, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, ImportGroup importId, boolean addLines)
    {
        CoreInstance number;
        SimpleFunctionExpression result;
        if (ctx.MINUS() != null)
        {
            number = this.expression(ctx.expression(), exprName, typeParametersNames, lambdaContext, space, true, importId, addLines);
            result = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.MINUS().getSymbol()), null, null, importId, null);
            result._functionName("minus");
            result._parametersValues(Lists.mutable.of((ValueSpecification) number));
        }
        else
        {
            number = this.expression(ctx.expression(), exprName, typeParametersNames, lambdaContext, space, true, importId, addLines);
            result = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.PLUS().getSymbol()), null, null, importId, null);
            result._functionName("plus");
            result._parametersValues(Lists.mutable.of((ValueSpecification) number));

        }
        return result;
    }

    private SimpleFunctionExpression notExpression(NotExpressionContext ctx, String exprName, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, ImportGroup importId, boolean addLines)
    {
        CoreInstance negated;
        SimpleFunctionExpression result;
        negated = this.expression(ctx.expression(), exprName, typeParametersNames, lambdaContext, space, true, importId, addLines);
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
                String withQuote = StringEscapeUtils.unescapeJava(ctx.getText());
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

    private CoreInstance atomicExpression(AtomicExpressionContext ctx, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        CoreInstance result;
        ListIterable<CoreInstance> dsl;
        GenericType genericType;
        VariableExpression expr;
        MutableList<VariableExpression> expressions = Lists.mutable.of();
        if (ctx.instanceLiteralToken() != null)
        {
            result = this.instanceLiteralToken(ctx.instanceLiteralToken(), wrapFlag);
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
            genericType = this.type(ctx.type(), typeParametersNames, "", importId, addLines);
            result = InstanceValueInstance.createPersistent(this.repository, genericType, this.getPureOne());
        }
        else if (ctx.lambdaFunction() != null)
        {
            boolean hasLambdaParams = false;
            if (ctx.lambdaFunction().lambdaParam() != null)
            {
                for (int i = 0; i < ctx.lambdaFunction().lambdaParam().size(); i++)
                {
                    hasLambdaParams = true;
                    IdentifierContext idCtx = ctx.lambdaFunction().lambdaParam(i).identifier();
                    expr = this.lambdaParam(ctx.lambdaFunction().lambdaParam(i).lambdaParamType(), idCtx, typeParametersNames, space, importId);
                    expressions.add(expr);
                }
            }
            result = this.lambdaPipe(ctx.lambdaFunction().lambdaPipe(), hasLambdaParams ? ctx.lambdaFunction().lambdaParam(0).getStart() : null, expressions, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
        }
        else if (ctx.lambdaParam() != null && ctx.lambdaPipe() != null)
        {
            expr = this.lambdaParam(ctx.lambdaParam().lambdaParamType(), ctx.lambdaParam().identifier(), typeParametersNames, space, importId);
            expressions.add(expr);
            result = this.lambdaPipe(ctx.lambdaPipe(), ctx.lambdaParam().getStart(), expressions, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
        }
        else if (ctx.instanceReference() != null)
        {
            result = this.instanceReference(ctx.instanceReference(), typeParametersNames, lambdaContext, importId, space, addLines);
        }
        else
        {
            //lambdaPipe
            result = this.lambdaPipe(ctx.lambdaPipe(), null, expressions, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
        }
        return result;
    }

    private CoreInstance instanceReference(InstanceReferenceContext ctx, MutableList<String> typeParametersNames, LambdaContext lambdaContext, ImportGroup importId, String space, boolean addLines)
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
            result = this.allOrFunction(ctx.allOrFunction(), Lists.mutable.of(instanceVal), ctx.qualifiedName(), typeParametersNames, lambdaContext, space, importId, addLines);
        }
        return result;
    }

    private ListIterable<CoreInstance> dsl(DslContext ctx, ImportGroup importId)
    {
        String res = ctx.getText();
        res = res.substring(res.indexOf('#') + 1, res.lastIndexOf('#'));
        if (res.trim().startsWith("/") || res.trim().startsWith("{"))
        {
            //TODO temporary hack till we move treepath completely away from dsl
            InlineDSLTextContent dslTextContext = new InlineDSLTextContent(res, ctx.getStart().getLine() + this.sourceInformation.getOffsetLine(), ctx.getStart().getCharPositionInLine() + 2);
            CoreInstance instance = this.processWithParser(dslTextContext, importId);
            return Lists.mutable.of(instance);
        }
        else
        {
            return Lists.mutable.of(new M3AntlrParser(this.inlineDSLLibrary).parseTreePath(res, this.sourceInformation.getSourceName(), ctx.getStart().getLine() + this.sourceInformation.getOffsetLine(), ctx.getStart().getCharPositionInLine() + 2, importId, this.repository, this.context));
        }
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
            PackageInstance packageInstance = this.buildPackage(packagePath);
            Instance.addValueToProperty(packageInstance, "children", instance, this.processorSupport);
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
        propertyValues.put(ctx.identifier().getText(), values);
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

    private CoreInstance processWithParser(InlineDSLTextContent code, ImportGroup importId)
    {
        MutableList<InlineDSL> results = this.inlineDSLLibrary.getInlineDSLs().select(dsl -> dsl.match(code.getText()), Lists.mutable.empty());

        if (results.isEmpty())
        {
            throw new RuntimeException("Can't find a parser for the String '" + code.getText() + "'");
        }
        if (results.size() > 1)
        {
            throw new RuntimeException("Found " + results.size() + " parsers (" + results.collect(InlineDSL::getName).makeString(", ") + ") for the String '" + code + "'");
        }
        return results.get(0).parse(code.getText(), importId, this.sourceInformation.getSourceName(), code.getColumn(), code.getLine(), this.repository, this.context);
    }

    private Any lambdaPipe(LambdaPipeContext ctx, Token firstToken, ListIterable<VariableExpression> params, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        Token lambdaStartToken = firstToken != null ? firstToken : ctx.PIPE().getSymbol();
        ListIterable<ValueSpecification> block = codeBlock(ctx.codeBlock(), typeParametersNames, importId, lambdaContext, addLines, spacePlusTabs(space, 6));

        FunctionTypeInstance signature = FunctionTypeInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(lambdaStartToken), null, null);
        if (Iterate.notEmpty(params))
        {
            signature._parameters(params);
        }
        // Note: we cannot set the function of the signature FunctionType, as this can cause stack overflow if serializing to M4

        GenericTypeInstance genericTypeInstance = GenericTypeInstance.createPersistent(this.repository);
        Type type = (Type) this.processorSupport.package_getByUserPath(M3Paths.LambdaFunction);
        genericTypeInstance._rawTypeCoreInstance(type);
        GenericTypeInstance genericTypeInstanceTa = GenericTypeInstance.createPersistent(this.repository);
        genericTypeInstanceTa._rawTypeCoreInstance(signature);
        genericTypeInstance._typeArguments(Lists.mutable.<GenericType>of(genericTypeInstanceTa));

        LambdaFunctionInstance lambdaFunction = LambdaFunctionInstance.createPersistent(this.repository, lambdaContext.getLambdaFunctionUniqueName(), this.sourceInformation.getPureSourceInformation(lambdaStartToken));
        lambdaFunction._classifierGenericType(genericTypeInstance);
        lambdaFunction._expressionSequence(block);

        return wrapFlag ? this.doWrap(lambdaFunction, lambdaStartToken) : lambdaFunction;
    }

    private ListIterable<ValueSpecification> codeBlock(CodeBlockContext ctx, MutableList<String> typeParametersNames, ImportGroup importId, LambdaContext lambdaContext, boolean addLines, String space)
    {
        String newSpace = space + "  ";
        return ListIterate.collect(ctx.programLine(), plCtx -> programLine(plCtx, M3Properties.line, typeParametersNames, lambdaContext, importId, addLines, newSpace));
    }

    private ValueSpecification programLine(ProgramLineContext ctx, String exprName, MutableList<String> typeParametersNames, LambdaContext lambdaContext, ImportGroup importId, boolean addLines, String space)
    {
        if (ctx.combinedExpression() != null)
        {
            return (ValueSpecification) this.combinedExpression(ctx.combinedExpression(), exprName, typeParametersNames, lambdaContext, space, true, importId, addLines);
        }
        else
        {
            return this.letExpression(ctx.letExpression(), typeParametersNames, importId, lambdaContext, addLines, space);
        }
    }

    private SimpleFunctionExpression letExpression(LetExpressionContext ctx, MutableList<String> typeParametersNames, ImportGroup importId, LambdaContext lambdaContext, boolean addLines, String space)
    {
        CoreInstance result = this.combinedExpression(ctx.combinedExpression(), "", typeParametersNames, lambdaContext, spacePlusTabs(space, 4), true, importId, addLines);
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

    private SimpleFunctionExpression allOrFunction(AllOrFunctionContext ctx, MutableList<? extends ValueSpecification> params, QualifiedNameContext funcName, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, ImportGroup importId, boolean addLines)
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

        MutableList<ValueSpecification> parameters = this.functionExpressionParameters(ctx.functionExpressionParameters(), typeParametersNames, importId, lambdaContext, addLines, space);
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

    private MutableList<ValueSpecification> functionExpressionParameters(FunctionExpressionParametersContext ctx, MutableList<String> typeParametersNames, ImportGroup importId, LambdaContext lambdaContext, boolean addLines, String space)
    {
        MutableList<ValueSpecification> parameters = Lists.mutable.empty();
        for (CombinedExpressionContext ceCtx : ctx.combinedExpression())
        {
            parameters.add((ValueSpecification) this.combinedExpression(ceCtx, "param", typeParametersNames, lambdaContext, spacePlusTabs(space, 4), true, importId, addLines));
        }
        return parameters;
    }

    /**
     * Parse the instantiation of an instance of UNIT.
     */
    private InstanceValueInstance unitInstanceParser(org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.UnitInstanceContext ctx, ImportGroup importId)
    {
        org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.UnitInstanceLiteralContext uctx = ctx.unitInstanceLiteral();
        GenericTypeInstance genericTypeInstance = GenericTypeInstance.createPersistent(this.repository);
        CoreInstance typeImportStub = ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.unitName().identifier().getStart()), this.getUnitNameWithMeasure(ctx.unitName()), importId);
        genericTypeInstance._rawTypeCoreInstance(typeImportStub);
        InstanceValueInstance iv = InstanceValueInstance.createPersistent(this.repository, genericTypeInstance, this.getPureOne());

        PrimitiveCoreInstance<?> result;
        if (uctx.MINUS() != null)
        {
            if (uctx.INTEGER() != null)
            {
                result = this.repository.newIntegerCoreInstance("-" + uctx.INTEGER().getText());
            }
            else if (uctx.DECIMAL() != null)
            {
                result = this.repository.newDecimalCoreInstance("-" + uctx.DECIMAL().getText());
            }
            else
            {
                result = this.repository.newFloatCoreInstance("-" + uctx.FLOAT().getText());
            }
        }
        else
        {
            if (uctx.INTEGER() != null)
            {
                result = this.repository.newIntegerCoreInstance("+" + uctx.INTEGER().getText());
            }
            else if (uctx.DECIMAL() != null)
            {
                result = this.repository.newDecimalCoreInstance("+" + uctx.DECIMAL().getText());
            }
            else
            {
                result = this.repository.newFloatCoreInstance("+" + uctx.FLOAT().getText());
            }
        }

        iv._genericType(genericTypeInstance);
        iv._multiplicity(this.getPureOne());
        iv._values(Lists.mutable.with(result.getValue()));

        InstanceValueInstance wrapperIv = InstanceValueInstance.createPersistent(this.repository, genericTypeInstance, this.getPureOne());
        wrapperIv._genericType(genericTypeInstance);
        wrapperIv._multiplicity(this.getPureOne());
        wrapperIv._values(Lists.mutable.with(iv));

        return wrapperIv;
    }

    private String getUnitNameWithMeasure(org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.UnitNameContext ctx)
    {
        QualifiedNameContext measureQualifiedName = ctx.qualifiedName();
        String nameWithoutPath = measureQualifiedName.identifier().getText().concat(this.tilde).concat(ctx.identifier().getText());
        return measureQualifiedName.packagePath() != null ? this.packageToString(measureQualifiedName.packagePath()).concat("::").concat(nameWithoutPath) : nameWithoutPath;
    }

    private SimpleFunctionExpression expressionInstanceParser(ExpressionInstanceContext ctx, MutableList<String> typeParametersNames, LambdaContext lambdaContext, ImportGroup importId, boolean addLines, String space)
    {
        ListIterable<GenericType> renderedTypeArguments = null;
        ListIterable<Multiplicity> renderedMultiplicityArguments = null;
        MutableList<CoreInstance> keyExpressions = Lists.mutable.empty();
        Token end = ctx.getStop();
        if (ctx.expressionInstanceParserPropertyAssignment() != null)
        {
            for (ExpressionInstanceParserPropertyAssignmentContext propCtx : ctx.expressionInstanceParserPropertyAssignment())
            {
                keyExpressions.add(this.expressionInstanceParserPropertyAssignment(propCtx, typeParametersNames, importId, addLines, spacePlusTabs(space, 8), lambdaContext));
            }
        }
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
        MutableList<IdentifierContext> properties = Lists.mutable.withAll(ctx.identifier());
        CoreInstance result = this.expressionInstanceParserRightSide(ctx.expressionInstanceRightSide(), typeParametersNames, importId, lambdaContext, addLines, space);
        InstanceValue instanceVal = properties.size() == 1 ? this.doWrap(properties.getFirst(), false) : this.doWrap(properties, false);
        return KeyExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.EQUAL().getSymbol()), (ValueSpecification) result, instanceVal)
                ._add(ctx.PLUS() != null);
    }

    private CoreInstance expressionInstanceParserAtomicRightSide(ExpressionInstanceAtomicRightSideContext ctx, MutableList<String> typeParametersNames, ImportGroup importId, LambdaContext lambdaContext, boolean addLines, String space)
    {
        if (ctx.combinedExpression() != null)
        {
            return this.combinedExpression(ctx.combinedExpression(), "", Lists.mutable.empty(), lambdaContext, spacePlusTabs(space, 4), true, importId, addLines);
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

    private SimpleFunctionExpression equalNotEqual(EqualNotEqualContext ctx, ValueSpecification input, String exprName, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        SimpleFunctionExpression result = null;
        CoreInstance other;
        if (ctx.TEST_EQUAL() != null)
        {
            other = this.combinedArithmeticOnly(ctx.combinedArithmeticOnly(), exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
            result = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.TEST_EQUAL().getSymbol()), null, null, importId, null);
            result._functionName("equal");
            result._parametersValues(Lists.mutable.of(input, (ValueSpecification) other));
        }
        else if (ctx.TEST_NOT_EQUAL() != null)
        {
            other = this.combinedArithmeticOnly(ctx.combinedArithmeticOnly(), exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
            SimpleFunctionExpressionInstance inner = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.TEST_NOT_EQUAL().getSymbol()), null, null, importId, null);
            inner._functionName("equal");
            inner._parametersValues(Lists.mutable.of(input, (ValueSpecification) other));

            result = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.TEST_NOT_EQUAL().getSymbol()), null, null, importId, null);
            result._functionName("not");
            result._parametersValues(Lists.mutable.<ValueSpecification>of(inner));
        }
        return result;
    }

    private CoreInstance combinedArithmeticOnly(CombinedArithmeticOnlyContext ctx, String exprName, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        CoreInstance result = this.expressionOrExpressionGroup(ctx.expressionOrExpressionGroup(), exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
        if (Iterate.notEmpty(ctx.arithmeticPart()))
        {
            return this.arithmeticPart(ctx.arithmeticPart(), result, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
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
    private SimpleFunctionExpression buildArithmeticWithListParam(ArithmeticPartContext ctx, ArithOp op, CoreInstance initialValue, String exprName, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
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
            others.add(this.expression(eCtx, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines));
        }
        TerminalNode newOp = getToken.apply(getTokens.value().size() - 1);
        SimpleFunctionExpression sfe = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(newOp.getSymbol()), null, null, importId, null);
        sfe._functionName(op_str);
        sfe._parametersValues(Lists.mutable.<ValueSpecification>of(this.doWrap(Lists.mutable.with(initialValue).withAll(others))));
        return sfe;
    }

    // Handles divide, since dive is built up in a tree: eg x / y /z is div(x, div(y,z))
    private SimpleFunctionExpression buildArithmeticDivide(ArithmeticPartContext ctx, ArithOp op, CoreInstance initialValue, String exprName, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        MutableList<CoreInstance> others = Lists.mutable.empty();

        for (ExpressionContext eCtx : ctx.expression())
        {
            others.add(this.expression(eCtx, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines));
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

    private SimpleFunctionExpression buildComparisonOp(ArithmeticPartContext ctx, ArithOp op, CoreInstance initialValue, String exprName, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
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

        CoreInstance other = this.expression(ctx.expression(0), exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
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
        SimpleFunctionExpression build(ArithmeticPartContext ctx, ArithOp op, CoreInstance initialValue, String exprName, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines);
    }

    // Antlr grammar as currently defined does not handle precedence for arithmetic ops
    // We take care of precedence here. 
    // Intuition: if we are processing an expression and the previous expression is of lower precedence, we 'snatch' the last argument from the previous expression and make it part of the current one
    // For example: 1 + 2 * 4. The grammar will have led us to build plus(1,2). When looking at the multiplication, the expression should snatch 2, and replace it with mult(2,4), 
    // so we end up with plus(1, mult(2,4))
    private SimpleFunctionExpression processOp(BuildArithmeticExpression builder, SimpleFunctionExpression sfe, ArithmeticPartContext ctx, ArithOp op, CoreInstance initialValue, String exprName, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        String opStr = op.name().toLowerCase();
        // Case where we are building from scratch
        if (sfe == null)
        {
            return builder.build(ctx, op, initialValue, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
        }
        //Case where we are in the middle of an expression, and currently looking at something of higher precedence than previous expression
        //Some processing to replace the last argument of the previous expression with the current expression (where current expression
        //has the last param as it's initial parameter).
        if (isStrictlyLowerPrecendence(sfe.getValueForMetaPropertyToOne("functionName").getName(), opStr))
        {
            ListIterable<? extends CoreInstance> params = getParams(sfe);
            SimpleFunctionExpression newSfe = builder.build(ctx, op, params.getLast(), exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
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
        return builder.build(ctx, op, sfe, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
    }

    private SimpleFunctionExpression arithmeticPart(List<ArithmeticPartContext> aList, CoreInstance result, String exprName, MutableList<String> typeParametersNames, LambdaContext lambdaContext, String space, boolean wrapFlag, ImportGroup importId, boolean addLines)
    {
        SimpleFunctionExpression sfe = null;
        for (ArithmeticPartContext ctx : aList)
        {
            if (Iterate.notEmpty(ctx.PLUS()))
            {
                sfe = processOp(this::buildArithmeticWithListParam, sfe, ctx, ArithOp.PLUS, result, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            else if (Iterate.notEmpty(ctx.STAR()))
            {
                sfe = processOp(this::buildArithmeticWithListParam, sfe, ctx, ArithOp.TIMES, result, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            else if (Iterate.notEmpty(ctx.MINUS()))
            {
                sfe = processOp(this::buildArithmeticWithListParam, sfe, ctx, ArithOp.MINUS, result, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            else if (Iterate.notEmpty(ctx.DIVIDE()))
            {
                sfe = processOp(this::buildArithmeticDivide, sfe, ctx, ArithOp.DIVIDE, result, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            // Relational comparison ops are of the lowest precedence, so no need to see if the expression needs to 'snatch' the last argument from the previous expression
            else if (ctx.LESSTHAN() != null)
            {
                sfe = buildComparisonOp(ctx, ArithOp.LESSTHAN, sfe == null ? result : sfe, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            else if (ctx.LESSTHANEQUAL() != null)
            {
                sfe = buildComparisonOp(ctx, ArithOp.LESSTHANEQUAL, sfe == null ? result : sfe, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            else if (ctx.GREATERTHAN() != null)
            {
                sfe = buildComparisonOp(ctx, ArithOp.GREATERTHAN, sfe == null ? result : sfe, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
            else if (ctx.GREATERTHANEQUAL() != null)
            {
                sfe = buildComparisonOp(ctx, ArithOp.GREATERTHANEQUAL, sfe == null ? result : sfe, exprName, typeParametersNames, lambdaContext, space, wrapFlag, importId, addLines);
            }
        }
        return sfe;
    }


    public GenericType type(TypeContext ctx, MutableList<String> typeParametersNames, String space, ImportGroup importId, boolean addLines)
    {
        if (ctx.qualifiedName() != null)
        {
            ListIterable<GenericType> renderedTypeArguments = this.typeArguments(ctx.typeArguments(), typeParametersNames, importId, addLines);
            ListIterable<Multiplicity> renderedMultiplicityArguments = this.multiplicityArguments(ctx.multiplicityArguments());
            return this.processType(ctx.qualifiedName(), typeParametersNames, renderedTypeArguments, renderedMultiplicityArguments, importId);
        }
        if (ctx.unitName() != null)
        {
            return this.processUnitType(ctx.unitName(), importId);
        }

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

    private SimpleFunctionExpression functionExpression(QualifiedNameContext funcName, MutableList<ValueSpecification> parameters, ImportGroup importId)
    {
        SimpleFunctionExpressionInstance result = SimpleFunctionExpressionInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(funcName.identifier().getStart()), null, null, importId, null);
        result._functionName(this.getQualifiedNameString(funcName));
        result._parametersValues(parameters);
        return result;
    }

    private CoreInstance enumValue(EnumValueContext ctx, CoreInstance enumeration, ImportGroup importId)
    {
        ListIterable<CoreInstance> stereotypes = null;
        ListIterable<TaggedValue> tags = null;
        if (ctx.stereotypes() != null)
        {
            stereotypes = this.stereotypes(ctx.stereotypes(), importId);
        }
        if (ctx.taggedValues() != null)
        {
            tags = this.taggedValues(ctx.taggedValues(), importId);
        }
        CoreInstance enumValue = this.repository.newCoreInstance(ctx.identifier().getText(), enumeration, this.sourceInformation.getPureSourceInformation(ctx.identifier().getStart()), true);

        enumValue.addKeyValue(Lists.immutable.of("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "type", "children", "Enum", "properties", "name"), this.repository.newStringCoreInstance_cached(ctx.identifier().getText()));
        if (stereotypes != null)
        {
            enumValue.setKeyValues(Lists.immutable.of("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "extension", "children", "ElementWithStereotypes", "properties", "stereotypes"), Lists.mutable.withAll(stereotypes));
        }
        if (tags != null)
        {
            enumValue.setKeyValues(Lists.immutable.of("Root", "children", "meta", "children", "pure", "children", "metamodel", "children", "extension", "children", "ElementWithTaggedValues", "properties", "taggedValues"), Lists.mutable.<CoreInstance>withAll(tags));
        }
        return enumValue;
    }

    private Enumeration<?> enumParser(EnumDefinitionContext ctx, ImportGroup importId) throws PureParserException
    {
        EnumerationInstance enumerationInstance;
        CoreInstance value;
        MutableList<CoreInstance> values = Lists.mutable.empty();
        ListIterable<CoreInstance> stereotypes = Lists.mutable.empty();
        ListIterable<TaggedValue> tags = Lists.mutable.empty();

        if (ctx.stereotypes() != null)
        {
            stereotypes = this.stereotypes(ctx.stereotypes(), importId);
        }
        if (ctx.taggedValues() != null)
        {
            tags = this.taggedValues(ctx.taggedValues(), importId);
        }

        this.checkExists(ctx.qualifiedName().packagePath(), ctx.qualifiedName().identifier(), null);

        PackageInstance packageInstance = this.buildPackage(ctx.qualifiedName().packagePath());

        GenericTypeInstance genericTypeInstance = GenericTypeInstance.createPersistent(this.repository);
        ClassInstance enumerationType = (ClassInstance) this.processorSupport.package_getByUserPath(M3Paths.Enumeration);
        genericTypeInstance._rawTypeCoreInstance(enumerationType);

        enumerationInstance = EnumerationInstance.createPersistent(this.repository, ctx.qualifiedName().identifier().getText());
        enumerationInstance._name(ctx.qualifiedName().identifier().getText());
        enumerationInstance._package(packageInstance);
        GenericTypeInstance taGenericType = GenericTypeInstance.createPersistent(this.repository);
        taGenericType._rawTypeCoreInstance(enumerationInstance);
        genericTypeInstance._typeArguments(Lists.mutable.<GenericType>of(taGenericType));
        enumerationInstance._classifierGenericType(genericTypeInstance);
        packageInstance._childrenAdd(enumerationInstance);
        if (!tags.isEmpty())
        {
            enumerationInstance._taggedValues(tags);
        }
        if (!stereotypes.isEmpty())
        {
            enumerationInstance._stereotypesCoreInstance(stereotypes);
        }
        enumerationInstance._classifierGenericType(genericTypeInstance);

        GenericTypeInstance general = GenericTypeInstance.createPersistent(this.repository);
        ClassInstance enumType = (ClassInstance) this.processorSupport.package_getByUserPath(M3Paths.Enum);
        general._rawTypeCoreInstance(enumType);
        GeneralizationInstance gen = GeneralizationInstance.createPersistent(this.repository, general, enumerationInstance);
        enumerationInstance._generalizations(Lists.mutable.<Generalization>of(gen));

        for (EnumValueContext evCtx : ctx.enumValue())
        {
            value = this.enumValue(evCtx, enumerationInstance, importId);
            values.add(value);
        }
        enumerationInstance.setSourceInformation(this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.qualifiedName().identifier().getStart(), ctx.getStop()));
        enumerationInstance._values(values);
        return enumerationInstance;
    }

    /**
     * Parses the measure given its definition context.
     * Returns the parsed measure as a CoreInstance.
     */
    private CoreInstance measureParser(org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MeasureDefinitionContext ctx, ImportGroup importId, boolean addLines) throws PureParserException
    {
        UnitInstance canonicalUnit;
        MutableList<UnitInstance> nonCanonicalUnits;

        MutableList<GenericType> superTypesGenericTypes = Lists.mutable.empty();
        MutableList<String> typeParameterNames = Lists.mutable.empty();
        MutableList<Boolean> contravariants = Lists.mutable.empty();
        MutableList<String> multiplicityParameterNames = Lists.mutable.empty();
        ListIterable<CoreInstance> stereotypes = Lists.mutable.empty();
        ListIterable<TaggedValue> tags = Lists.mutable.empty();
        MeasureInstance measureInstance;

        String measureName = ctx.qualifiedName().identifier().getText();
        measureInstance = MeasureInstance.createPersistent(this.repository, measureName);
        PackageInstance packageInstance = this.buildPackage(ctx.qualifiedName().packagePath());
        measureInstance._package(packageInstance);
        packageInstance._childrenAdd(measureInstance);

        String fullName = this.getQualifiedNameString(ctx.qualifiedName());

        measureInstance.setSourceInformation(this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.qualifiedName().identifier().getStart(), ctx.getStop()));

        if (superTypesGenericTypes.isEmpty())
        {
            GenericTypeInstance genericTypeInstance = GenericTypeInstance.createPersistent(this.repository);
            genericTypeInstance._rawTypeCoreInstance(this.processorSupport.package_getByUserPath(M3Paths.Any));
            superTypesGenericTypes.add(genericTypeInstance);
        }

        GenericTypeInstance classifierGT = GenericTypeInstance.createPersistent(this.repository);
        ClassInstance measureType = (ClassInstance) this.processorSupport.package_getByUserPath(M3Paths.Measure);
        classifierGT._rawTypeCoreInstance(measureType);


        measureInstance._classifierGenericType(classifierGT);

        if (!typeParameterNames.isEmpty())
        {
            MutableList<TypeParameter> typeParameters = Lists.mutable.of();
            MutableList<Pair<String, Boolean>> tps = typeParameterNames.zip(contravariants);
            for (Pair<String, Boolean> typeParam : tps)
            {
                TypeParameterInstance tp = TypeParameterInstance.createPersistent(this.repository, typeParam.getOne());
                tp._contravariant(typeParam.getTwo());
                typeParameters.add(tp);
            }

            MutableList<GenericType> typeArgs = Lists.mutable.of();
            for (String typeParamName : typeParameterNames)
            {
                TypeParameterInstance tp = TypeParameterInstance.createPersistent(this.repository, typeParamName);
                GenericTypeInstance gt = GenericTypeInstance.createPersistent(this.repository);
                gt._typeParameter(tp);
                typeArgs.add(gt);
            }

        }

        if (!multiplicityParameterNames.isEmpty())
        {
            MutableList<Multiplicity> multParameters = Lists.mutable.of();

            for (String multiplicityParam : multiplicityParameterNames)
            {
                MultiplicityInstance mult = MultiplicityInstance.createPersistent(this.repository, null, null);
                mult._multiplicityParameter(multiplicityParam);
                multParameters.add(mult);
            }
        }

        measureInstance._name(ctx.qualifiedName().identifier().getText());
        if (!stereotypes.isEmpty())
        {
            measureInstance._stereotypesCoreInstance(stereotypes);
        }
        if (!tags.isEmpty())
        {
            measureInstance._taggedValues(tags);
        }

        MutableList<Generalization> generalizations = Lists.mutable.empty();
        for (GenericType superType : superTypesGenericTypes)
        {
            GeneralizationInstance generalizationInstance = GeneralizationInstance.createPersistent(this.repository, superType, measureInstance);
            generalizations.add(generalizationInstance);
        }
        measureInstance._generalizations(generalizations);

        if (null != ctx.measureBody().canonicalExpr())
        {
            // traditional canonical unit pattern
            canonicalUnit = this.canonicalUnitParser(ctx.measureBody().canonicalExpr(), importId, measureInstance, ctx);
            measureInstance._canonicalUnit(canonicalUnit);

            nonCanonicalUnits = this.nonCanonicalUnitsParser(ctx.measureBody().measureExpr(), importId, measureInstance, ctx);
            if (nonCanonicalUnits.notEmpty())
            {
                measureInstance._nonCanonicalUnits(nonCanonicalUnits);
            }
        }
        else
        {
            // non-convertible unit pattern
            MutableList<UnitInstance> nonConvertibleUnits = Lists.mutable.empty();
            for (org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.NonConvertibleMeasureExprContext ncctx : ctx.measureBody().nonConvertibleMeasureExpr())
            {
                UnitInstance currentUnit = this.nonConvertibleUnitParser(ncctx, importId, measureInstance, ctx);
                nonConvertibleUnits.add(currentUnit);
            }
            measureInstance._canonicalUnit(nonConvertibleUnits.get(0));
            if (nonConvertibleUnits.size() > 1)
            {
                measureInstance._nonCanonicalUnits(nonConvertibleUnits.subList(1, nonConvertibleUnits.size()));
            }
        }
        return measureInstance;
    }

    /**
     * Parse the canonical unit and returns it as a UnitInstance
     */
    private UnitInstance canonicalUnitParser(org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.CanonicalExprContext ctx, ImportGroup importId, MeasureInstance measureInstance, org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MeasureDefinitionContext mctx) throws PureParserException
    {
        return this.unitParser(ctx.measureExpr(), importId, measureInstance, mctx);
    }

    /**
     * Parses the non-canonical units in a measure and return a MutableList of UnitInstance's.
     */
    private MutableList<UnitInstance> nonCanonicalUnitsParser(List<org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MeasureExprContext> ctxList, ImportGroup importId, MeasureInstance measureInstance, org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MeasureDefinitionContext mctx) throws PureParserException
    {
        return ListIterate.collect(ctxList, ctx -> unitParser(ctx, importId, measureInstance, mctx));
    }

    /**
     * Helps build the unitInstance for any canonical and non-canonical units and returns the parsed unitInstance.
     */
    private UnitInstance unitParser(org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MeasureExprContext ctx, ImportGroup importId, MeasureInstance measureInstance, org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MeasureDefinitionContext mctx) throws PureParserException
    {
        UnitInstance unitInstance;

        MutableList<GenericType> superTypesGenericTypes = Lists.mutable.empty();

        this.checkExists(ctx.qualifiedName().packagePath(), ctx.qualifiedName().identifier(), null);

        String unitName = mctx.qualifiedName().identifier().getText().concat(this.tilde).concat(ctx.qualifiedName().identifier().getText());
        unitInstance = UnitInstance.createPersistent(this.repository, unitName);

        PackageInstance packageInstance = buildPackage(Lists.mutable.withAll(mctx.qualifiedName().packagePath() == null ? Lists.mutable.empty() : ListAdapter.adapt(mctx.qualifiedName().packagePath().identifier())));
        unitInstance._package(packageInstance);
        packageInstance._childrenAdd(unitInstance);

        unitInstance.setSourceInformation(this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.qualifiedName().identifier().getStart(), ctx.getStop()));

        GenericTypeInstance classifierGT = GenericTypeInstance.createPersistent(this.repository);
        ClassInstance unitType = (ClassInstance) this.processorSupport.package_getByUserPath(M3Paths.Unit);
        classifierGT._rawTypeCoreInstance(unitType);

        unitInstance._classifierGenericType(classifierGT);

        unitInstance._name(unitName);

        if (superTypesGenericTypes.isEmpty())
        {
            GenericTypeInstance genericTypeInstance = GenericTypeInstance.createPersistent(this.repository);
            genericTypeInstance._rawTypeCoreInstance(measureInstance); // set unit super type to be its measure (Kilogram -> Mass)
            superTypesGenericTypes.add(genericTypeInstance);
        }
        MutableList<Generalization> generalizations = Lists.mutable.empty();
        for (GenericType superType : superTypesGenericTypes)
        {
            GeneralizationInstance generalizationInstance = GeneralizationInstance.createPersistent(this.repository, superType, unitInstance);
            generalizations.add(generalizationInstance);
        }
        unitInstance._generalizations(generalizations);

        unitInstance._measure(measureInstance);

        // prepare lambda instance for the conversion function

        String fullName = this.getQualifiedNameString(ctx.qualifiedName());
        LambdaContext lambdaContext = new LambdaContext(fullName.replace("::", "_"));
        MutableList<String> typeParametersNames = Lists.mutable.empty();
        FunctionTypeInstance signature = FunctionTypeInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.unitExpr().getStart(), ctx.unitExpr().getStart(), ctx.unitExpr().getStop()), null, null);

        // prepare params

        VariableExpression expr = this.lambdaParam(null, ctx.unitExpr().identifier(), typeParametersNames, "", importId);
        expr._multiplicity(this.getPureOne());
        expr._functionTypeOwner(signature);
        GenericTypeInstance paramGenericType = GenericTypeInstance.createPersistent(this.repository);
        CoreInstance paramType = this.processorSupport.package_getByUserPath(M3Paths.Number);
        paramGenericType._rawTypeCoreInstance(paramType);
        expr._genericType(paramGenericType);
        signature._parameters(Lists.mutable.with(expr));

        GenericTypeInstance genericTypeInstance = GenericTypeInstance.createPersistent(this.repository);
        Type type = (Type) this.processorSupport.package_getByUserPath(M3Paths.LambdaFunction);
        genericTypeInstance._rawTypeCoreInstance(type);
        GenericTypeInstance genericTypeInstanceTa = GenericTypeInstance.createPersistent(this.repository);
        genericTypeInstanceTa._rawTypeCoreInstance(signature);
        genericTypeInstance._typeArguments(Lists.mutable.<GenericType>of(genericTypeInstanceTa));
        LambdaFunctionInstance lambdaFunctionInstance = LambdaFunctionInstance.createPersistent(this.repository, lambdaContext.getLambdaFunctionUniqueName(), this.sourceInformation.getPureSourceInformation(ctx.unitExpr().ARROW().getSymbol()));
        lambdaFunctionInstance._classifierGenericType(genericTypeInstance);
        signature._functionAdd(lambdaFunctionInstance);

        ListIterable<ValueSpecification> block = this.codeBlock(ctx.unitExpr().codeBlock(), typeParametersNames, importId, lambdaContext, addLines, tabs(6));
        lambdaFunctionInstance._expressionSequence(block);

        unitInstance._conversionFunction(lambdaFunctionInstance);

        return unitInstance;
    }

    private UnitInstance nonConvertibleUnitParser(org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.NonConvertibleMeasureExprContext ctx, ImportGroup importId, MeasureInstance measureInstance, org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.MeasureDefinitionContext mctx) throws PureParserException
    {
        UnitInstance unitInstance;
        MutableList<GenericType> superTypesGenericTypes = Lists.mutable.empty();

        this.checkExists(ctx.qualifiedName().packagePath(), ctx.qualifiedName().identifier(), null);

        String unitName = mctx.qualifiedName().identifier().getText().concat(this.tilde).concat(ctx.qualifiedName().identifier().getText());
        unitInstance = UnitInstance.createPersistent(this.repository, unitName);

        PackageInstance packageInstance = buildPackage(Lists.mutable.withAll(mctx.qualifiedName().packagePath() == null ? Lists.mutable.empty() : ListAdapter.adapt(mctx.qualifiedName().packagePath().identifier())));
        unitInstance._package(packageInstance);
        packageInstance._childrenAdd(unitInstance);

        unitInstance.setSourceInformation(this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.qualifiedName().identifier().getStart(), ctx.getStop()));

        GenericTypeInstance classifierGT = GenericTypeInstance.createPersistent(this.repository);
        ClassInstance unitType = (ClassInstance) this.processorSupport.package_getByUserPath(M3Paths.Unit);
        classifierGT._rawTypeCoreInstance(unitType);

        unitInstance._classifierGenericType(classifierGT);

        unitInstance._name(unitName);

        if (superTypesGenericTypes.isEmpty())
        {
            GenericTypeInstance genericTypeInstance = GenericTypeInstance.createPersistent(this.repository);
            genericTypeInstance._rawTypeCoreInstance(measureInstance);
            superTypesGenericTypes.add(genericTypeInstance);
        }
        unitInstance._generalizations(superTypesGenericTypes.collect(superType -> GeneralizationInstance.createPersistent(this.repository, superType, unitInstance)));

        unitInstance._measure(measureInstance);

        return unitInstance;
    }

    private CoreInstance classParser(ClassDefinitionContext ctx, ImportGroup importId, boolean addLines) throws PureParserException
    {
        MutableList<Property<? extends CoreInstance, ?>> properties = Lists.mutable.empty();
        MutableList<QualifiedProperty<? extends CoreInstance>> qualifiedProperties = Lists.mutable.empty();
        MutableList<GenericType> superTypesGenericTypes = Lists.mutable.empty();
        boolean isDirectSubTypeofAny = false;
        MutableList<String> typeParameterNames = Lists.mutable.empty();
        MutableList<Boolean> contravariants = Lists.mutable.empty();
        MutableList<String> multiplicityParameterNames = Lists.mutable.empty();
        ImportStubInstance ownerType;
        ListIterable<CoreInstance> stereotypes = null;
        ListIterable<TaggedValue> tags = null;
        ClassInstance classInstance;
        if (ctx.stereotypes() != null)
        {
            stereotypes = this.stereotypes(ctx.stereotypes(), importId);
        }
        if (ctx.taggedValues() != null)
        {
            tags = this.taggedValues(ctx.taggedValues(), importId);
        }
        this.checkExists(ctx.qualifiedName().packagePath(), ctx.qualifiedName().identifier(), null);
        if (ctx.typeParametersWithContravarianceAndMultiplicityParameters() != null)
        {
            this.typeParametersWithContravarianceAndMultiplicityParameters(ctx.typeParametersWithContravarianceAndMultiplicityParameters(), typeParameterNames, contravariants, multiplicityParameterNames);
        }
        if (ctx.projection() != null)
        {
            return this.projectionParser(ctx, importId, addLines, stereotypes, tags);
        }
        else
        {
            if (ctx.EXTENDS() != null)
            {
                for (TypeContext typeCtx : ctx.type())
                {
                    superTypesGenericTypes.add(this.type(typeCtx, typeParameterNames, "", importId, addLines));
                }
            }
            String className = ctx.qualifiedName().identifier().getText();
            classInstance = ClassInstance.createPersistent(this.repository, className);
            PackageInstance packageInstance = this.buildPackage(ctx.qualifiedName().packagePath());
            classInstance._package(packageInstance);
            packageInstance._childrenAdd(classInstance);

            String fullName = this.getQualifiedNameString(ctx.qualifiedName());
            ownerType = ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.qualifiedName().identifier().getStart()), fullName, importId);

            LambdaContext lambdaContext = new LambdaContext(fullName.replace("::", "_"));
            MutableList<Constraint> constraints = this.constraints(classInstance, ctx.constraints(), importId, lambdaContext, addLines);
            this.propertyParser(ctx.classBody().properties(), properties, qualifiedProperties, typeParameterNames, multiplicityParameterNames, ownerType, importId, 0);
            classInstance.setSourceInformation(this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.qualifiedName().identifier().getStart(), ctx.getStop()));

            if (superTypesGenericTypes.isEmpty())
            {
                isDirectSubTypeofAny = true;
                GenericTypeInstance genericTypeInstance = GenericTypeInstance.createPersistent(this.repository);
                genericTypeInstance._rawTypeCoreInstance(this.processorSupport.package_getByUserPath(M3Paths.Any));
                superTypesGenericTypes.add(genericTypeInstance);
            }

            GenericTypeInstance classifierGT = GenericTypeInstance.createPersistent(this.repository);
            ClassInstance classType = (ClassInstance) this.processorSupport.package_getByUserPath(M3Paths.Class);
            classifierGT._rawTypeCoreInstance(classType);
            GenericTypeInstance classifierGTTA = GenericTypeInstance.createPersistent(this.repository);
            classifierGTTA._rawTypeCoreInstance(classInstance);

            if (!typeParameterNames.isEmpty())
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

            if (!multiplicityParameterNames.isEmpty())
            {
                MutableList<Multiplicity> multParameters = Lists.mutable.of();

                for (String multiplicityParam : multiplicityParameterNames)
                {
                    MultiplicityInstance mult = MultiplicityInstance.createPersistent(this.repository, null, null);
                    mult._multiplicityParameter(multiplicityParam);
                    multParameters.add(mult);

                }
                classInstance._multiplicityParameters(this.processMultiplicityParametersInstance(multiplicityParameterNames));
                classifierGTTA._multiplicityArguments(multParameters);
            }

            classifierGT._typeArguments(Lists.mutable.<GenericType>of(classifierGTTA));
            classInstance._classifierGenericType(classifierGT);

            if (properties.notEmpty())
            {
                classInstance._properties(properties);
            }
            if (qualifiedProperties.notEmpty())
            {
                classInstance._qualifiedProperties(qualifiedProperties);
            }
            classInstance._name(ctx.qualifiedName().identifier().getText());
            if (stereotypes != null)
            {
                classInstance._stereotypesCoreInstance(stereotypes);
            }
            if (tags != null)
            {
                classInstance._taggedValues(tags);
            }
            if (constraints.notEmpty())
            {
                classInstance._constraints(constraints);
            }

            MutableList<Generalization> generalizations = Lists.mutable.empty();
            for (GenericType superType : superTypesGenericTypes)
            {
                GeneralizationInstance generalizationInstance = GeneralizationInstance.createPersistent(this.repository, superType, classInstance);
                generalizations.add(generalizationInstance);
            }
            classInstance._generalizations(generalizations);
            if (isDirectSubTypeofAny)
            {
                MilestoningClassProcessor.addMilestoningProperty(classInstance, this.context, this.processorSupport, this.repository);
            }
            return classInstance;
        }
    }


    private MutableList<Constraint> constraints(CoreInstance owner, ConstraintsContext ctx, ImportGroup importId, LambdaContext lambdaContext, boolean addLines)
    {
        MutableList<Constraint> constraints = Lists.mutable.empty();
        if (ctx != null)
        {
            int i = 0;

            for (ConstraintContext cCtx : ctx.constraint())
            {
                constraints.add(this.constraint(owner, cCtx, i, importId, lambdaContext, addLines, false));
                i++;
            }
        }
        return constraints;
    }

    private Constraint constraint(CoreInstance owner, ConstraintContext ctx, int position, ImportGroup importId, LambdaContext lambdaContext, boolean addLines, boolean postConstraint)
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
            org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.SimpleConstraintContext simpleConstraintContext = ctx.simpleConstraint();
            constraintName = simpleConstraintContext.constraintId() != null ? simpleConstraintContext.constraintId().VALID_STRING().getText() : String.valueOf(position);
            constraintFunctionDefinition = this.combinedExpression(simpleConstraintContext.combinedExpression(), "", Lists.mutable.empty(), lambdaContext, "", true, importId, addLines);
            constraintSourceInformation = this.sourceInformation.getPureSourceInformation(simpleConstraintContext.getStart(), simpleConstraintContext.getStart(), simpleConstraintContext.getStop());
        }
        else
        {
            org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.ComplexConstraintContext complexConstraintContext = ctx.complexConstraint();
            constraintSourceInformation = this.sourceInformation.getPureSourceInformation(complexConstraintContext.getStart(), complexConstraintContext.VALID_STRING().getSymbol(), complexConstraintContext.getStop());
            if (this.processorSupport.instance_instanceOf(owner, M3Paths.Class))
            {
                constraintName = complexConstraintContext.VALID_STRING().getText();

                if (complexConstraintContext.constraintOwner() != null)
                {
                    constraintOwner = complexConstraintContext.constraintOwner().VALID_STRING().getText();
                }

                if (complexConstraintContext.constraintExternalId() != null)
                {
                    constraintExternalId = this.removeQuotes(complexConstraintContext.constraintExternalId().STRING());
                }

                constraintFunctionDefinition = this.combinedExpression(complexConstraintContext.constraintFunction().combinedExpression(), "", Lists.mutable.empty(), lambdaContext, "", true, importId, addLines);

                if (complexConstraintContext.constraintEnforcementLevel() != null)
                {
                    constraintLevel = complexConstraintContext.constraintEnforcementLevel().ENFORCEMENT_LEVEL().getText();
                }

                if (complexConstraintContext.constraintMessage() != null)
                {
                    CoreInstance messageFunction = this.combinedExpression(complexConstraintContext.constraintMessage().combinedExpression(), "", Lists.mutable.empty(), lambdaContext, "", true, importId, addLines);
                    SourceInformation messageSourceInformation = messageFunction.getSourceInformation();

                    CoreInstance messageFunctionType = this.repository.newAnonymousCoreInstance(messageSourceInformation, this.processorSupport.package_getByUserPath(M3Paths.FunctionType), true);
                    CoreInstance param = VariableExpressionInstance.createPersistent(this.repository, messageSourceInformation, (GenericType) org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(owner, this.processorSupport), this.pureOne, "this");
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
            CoreInstance param = VariableExpressionInstance.createPersistent(this.repository, functionSourceInformation, (GenericType) org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(owner, this.processorSupport), this.pureOne, "this");
            Instance.addValueToProperty(functionType, M3Properties.parameters, param, this.processorSupport);
        }

        if (this.processorSupport.instance_instanceOf(owner, M3Paths.FunctionDefinition))
        {
            FunctionType ft = (FunctionType) this.processorSupport.function_getFunctionType(owner);
            MutableList<CoreInstance> params = Lists.mutable.empty();
            params.addAll(Instance.getValueForMetaPropertyToManyResolved(ft, M3Properties.parameters, this.processorSupport).toList());
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

        Constraint constraint = ConstraintInstance.createPersistent(this.repository, null, null, null);
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

    private CoreInstance projectionParser(ClassDefinitionContext ctx, ImportGroup importId, boolean addLines, ListIterable<CoreInstance> stereotypes, ListIterable<TaggedValue> tags) throws PureParserException
    {

        CoreInstance treePath = ctx.projection().dsl() != null ? this.dsl(ctx.projection().dsl(), importId).get(0) : this.treePath(ctx.projection().treePath(), importId);

        this.checkExists(ctx.qualifiedName().packagePath(), ctx.qualifiedName().identifier(), null);

        RootRouteNode rootNode = (RootRouteNode) treePath;

        String name = ctx.qualifiedName().identifier().getText();
        ClassProjectionInstance projection = ClassProjectionInstance.createPersistent(this.repository, name, this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.qualifiedName().identifier().getStart(), ctx.getStop()), rootNode);
        projection._name(name);
        rootNode._owner(projection);

        PackageInstance packageInstance = this.buildPackage(ctx.qualifiedName().packagePath());
        projection._package(packageInstance);
        packageInstance._childrenAdd(projection);

        GenericTypeInstance classifierGT = GenericTypeInstance.createPersistent(this.repository);
        ClassInstance classType = (ClassInstance) this.processorSupport.package_getByUserPath(M3Paths.ClassProjection);
        classifierGT._rawTypeCoreInstance(classType);
        GenericTypeInstance classifierGTTA = GenericTypeInstance.createPersistent(this.repository);
        classifierGTTA._rawTypeCoreInstance(projection);
        classifierGT._typeArguments(Lists.mutable.<GenericType>of(classifierGTTA));
        projection._classifierGenericType(classifierGT);

        GenericTypeInstance superType = GenericTypeInstance.createPersistent(this.repository);
        superType._rawTypeCoreInstance(this.processorSupport.package_getByUserPath(M3Paths.Any));
        GeneralizationInstance generalizationInstance = GeneralizationInstance.createPersistent(this.repository, superType, projection);
        projection._generalizations(Lists.mutable.<Generalization>of(generalizationInstance));
        String fullName = this.getQualifiedNameString(ctx.qualifiedName());
        MutableList<Constraint> constraints = this.constraints(projection, ctx.constraints(), importId, new LambdaContext(fullName.replace("::", "_")), addLines);

        if (Iterate.notEmpty(stereotypes))
        {
            projection._stereotypesCoreInstance(stereotypes);
        }
        if (Iterate.notEmpty(tags))
        {
            projection._taggedValues(tags);
        }
        if (Iterate.notEmpty(constraints))
        {
            projection._constraints(constraints);
        }
        return projection;
    }

    private CoreInstance associationParser(AssociationContext ctx, ImportGroup importId) throws PureParserException
    {
        MutableList<Property<? extends CoreInstance, ?>> properties = Lists.mutable.empty();
        MutableList<QualifiedProperty<? extends CoreInstance>> qualifiedProperties = Lists.mutable.empty();
        ListIterable<CoreInstance> stereotypes = null;
        ListIterable<TaggedValue> tags = null;
        AssociationInstance associationInstance;
        ImportStub is;
        if (ctx.stereotypes() != null)
        {
            stereotypes = this.stereotypes(ctx.stereotypes(), importId);
        }
        if (ctx.taggedValues() != null)
        {
            tags = this.taggedValues(ctx.taggedValues(), importId);
        }
        this.checkExists(ctx.qualifiedName().packagePath(), ctx.qualifiedName().identifier(), null);
        PackageInstance packageInstance = this.buildPackage(ctx.qualifiedName().packagePath());
        if (ctx.associationProjection() != null)
        {
            AssociationProjectionContext apCtx = ctx.associationProjection();
            ImportStubInstance projectedAssociation = ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(apCtx.qualifiedName(0).getStart()), this.getQualifiedNameString(ctx.associationProjection().qualifiedName(0)), importId);
            ImportStubInstance projectionOne = ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(apCtx.qualifiedName(1).getStart()), this.getQualifiedNameString(apCtx.qualifiedName(1)), importId);
            ImportStubInstance projectionTwo = ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(apCtx.qualifiedName(2).getStart()), this.getQualifiedNameString(apCtx.qualifiedName(2)), importId);
            AssociationProjectionInstance projection = AssociationProjectionInstance.createPersistent(this.repository, ctx.qualifiedName().identifier().getText(), this.sourceInformation.getPureSourceInformation(ctx.ASSOCIATION().getSymbol(), ctx.qualifiedName().identifier().getStart(), ctx.associationProjection().getStop()), null);
            projection._name(ctx.qualifiedName().identifier().getText());
            projection._package(packageInstance);
            packageInstance._childrenAdd(projection);

            projection._projectedAssociationCoreInstance(projectedAssociation);
            projection._projectionsCoreInstance(Lists.fixedSize.<CoreInstance>with(projectionOne, projectionTwo));

            if (!Iterate.isEmpty(stereotypes))
            {
                projection._stereotypesCoreInstance(stereotypes);
            }
            if (!Iterate.isEmpty(tags))
            {
                projection._taggedValues(tags);
            }
            return projection;
        }
        else
        {
            String associationName = ctx.qualifiedName().identifier().getText();
            associationInstance = AssociationInstance.createPersistent(this.repository, associationName);
            associationInstance._name(ctx.qualifiedName().identifier().getText());
            associationInstance._package(packageInstance);
            packageInstance._childrenAdd(associationInstance);

            GenericTypeInstance classifierGT = GenericTypeInstance.createPersistent(this.repository);
            ClassInstance assocationType = (ClassInstance) this.processorSupport.package_getByUserPath(M3Paths.Association);
            classifierGT._rawTypeCoreInstance(assocationType);
            associationInstance._classifierGenericType(classifierGT);

            associationInstance._stereotypesCoreInstance(stereotypes);
            associationInstance._taggedValues(tags);
            is = ImportStubInstance.createPersistent(this.repository, this.getQualifiedNameString(ctx.qualifiedName()), importId);
            this.propertyParser(ctx.associationBody().properties(), properties, qualifiedProperties, Lists.fixedSize.empty(), Lists.fixedSize.empty(), is, importId, 0);
            associationInstance.setSourceInformation(this.sourceInformation.getPureSourceInformation(ctx.ASSOCIATION().getSymbol(), ctx.qualifiedName().identifier().getStart(), ctx.getStop()));
            associationInstance._properties(properties);

            if (Iterate.notEmpty(qualifiedProperties))
            {
                associationInstance._qualifiedProperties(qualifiedProperties);
            }
            return associationInstance;
        }
    }

    public void propertyParser(PropertiesContext ctx, MutableList<Property<? extends CoreInstance, ?>> properties, MutableList<QualifiedProperty<? extends CoreInstance>> qualifiedProperties, MutableList<String> typeParameterNames,
                               MutableList<String> multiplicityParameterNames, ImportStub isOwner, ImportGroup importId, int startingQualifiedPropertyIndex)
    {
        int qualifiedPropertyIndex = startingQualifiedPropertyIndex;
        if (ctx.property() != null)
        {
            for (PropertyContext pCtx : ctx.property())
            {
                this.simpleProperty(pCtx, properties, typeParameterNames, multiplicityParameterNames, isOwner, importId, this.addLines);
            }
        }
        if (ctx.qualifiedProperty() != null)
        {
            for (QualifiedPropertyContext pCtx : ctx.qualifiedProperty())
            {
                this.qualifiedProperty(pCtx, qualifiedProperties, typeParameterNames, multiplicityParameterNames, isOwner, importId, this.addLines, qualifiedPropertyIndex);
                qualifiedPropertyIndex++;
            }
        }
    }

    private void simpleProperty(PropertyContext ctx, MutableList<Property<? extends CoreInstance, ?>> properties, MutableList<String> typeParameterNames,
                                MutableList<String> multiplicityParameterNames, ImportStub isOwner, ImportGroup importId, boolean addLines)
    {
        ListIterable<CoreInstance> stereotypes = null;
        ListIterable<TaggedValue> tags = null;
        DefaultValue defaultValue = null;
        GenericType genericType;
        Multiplicity multiplicity;
        String aggregation;
        String propertyName = ctx.identifier().getText();

        if (ctx.stereotypes() != null)
        {
            stereotypes = this.stereotypes(ctx.stereotypes(), importId);
        }
        if (ctx.taggedValues() != null)
        {
            tags = this.taggedValues(ctx.taggedValues(), importId);
        }
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
        if (ctx.defaultValue() != null)
        {
            defaultValue = defaultValue(ctx.defaultValue(), importId, propertyName);
        }
        genericType = this.type(ctx.propertyReturnType().type(), typeParameterNames, "", importId, addLines);
        multiplicity = this.buildMultiplicity(ctx.propertyReturnType().multiplicity().multiplicityArgument());

        Enumeration<?> agg = (Enumeration<?>) this.processorSupport.package_getByUserPath(M3Paths.AggregationKind);
        Enum aggKind = (Enum) agg._values().detect(v -> aggregation.equals(((Enum) v).getName()));
        SourceInformation propertySourceInfo = this.sourceInformation.getPureSourceInformation(ctx.identifier().getStart(), ctx.identifier().getStart(), ctx.getStop());
        PropertyInstance propertyInstance = PropertyInstance.createPersistent(this.repository, propertyName, propertySourceInfo, aggKind, genericType, multiplicity, null);
        propertyInstance._stereotypesCoreInstance(stereotypes);
        propertyInstance._taggedValues(tags);
        propertyInstance._name(propertyName);
        propertyInstance._defaultValue(defaultValue);

        GenericTypeInstance classifierGT = GenericTypeInstance.createPersistent(this.repository, propertySourceInfo);
        ClassInstance propertyType = (ClassInstance) this.processorSupport.package_getByUserPath(M3Paths.Property);
        classifierGT._rawTypeCoreInstance(propertyType);
        classifierGT._multiplicityArguments(Lists.mutable.of(multiplicity));
        GenericTypeInstance classifierGTTA = GenericTypeInstance.createPersistent(this.repository);
        classifierGTTA._rawTypeCoreInstance(isOwner);


        if (!typeParameterNames.isEmpty())
        {
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
        if (!multiplicityParameterNames.isEmpty())
        {
            MutableList<Multiplicity> multParameters = Lists.mutable.of();

            for (String multiplicityParam : multiplicityParameterNames)
            {
                MultiplicityInstance mult = MultiplicityInstance.createPersistent(this.repository, null, null);
                mult._multiplicityParameter(multiplicityParam);
                multParameters.add(mult);

            }
            classifierGTTA._multiplicityArguments(multParameters);
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

        if (!genericType._typeArguments().isEmpty())
        {
            ngt._typeArguments(genericType._typeArguments());
        }
        ngt._typeParameter(genericType._typeParameter());
        if (!genericType._multiplicityArguments().isEmpty())
        {
            ngt._multiplicityArguments(genericType._multiplicityArguments());
        }

        classifierGT._typeArguments(Lists.mutable.<GenericType>of(classifierGTTA, ngt));
        propertyInstance._classifierGenericType(classifierGT);
        properties.add(propertyInstance);
    }

    private void qualifiedProperty(QualifiedPropertyContext ctx, MutableList<QualifiedProperty<? extends CoreInstance>> qualifiedProperties, MutableList<String> typeParameterNames, MutableList<String> multiplicityParameterNames, ImportStub isOwner, ImportGroup importId, boolean addLines, int qualifiedPropertyIndex)
    {
        ListIterable<CoreInstance> stereotypes = (ctx.stereotypes() == null) ? null : stereotypes(ctx.stereotypes(), importId);
        ListIterable<TaggedValue> tags = (ctx.taggedValues() == null) ? null : taggedValues(ctx.taggedValues(), importId);
        MutableList<VariableExpression> vars = Lists.mutable.of();
        ListIterable<ValueSpecification> code = Lists.fixedSize.empty();
        GenericType genericType = this.type(ctx.propertyReturnType().type(), typeParameterNames, "", importId, addLines);
        Multiplicity multiplicity = this.buildMultiplicity(ctx.propertyReturnType().multiplicity().multiplicityArgument());

        String propertyName = ctx.identifier().getText();
        final String qualifiedPropertyName = propertyName + "_" + qualifiedPropertyIndex;

        if (ctx.qualifiedPropertyBody() != null)
        {
            if (ctx.qualifiedPropertyBody().functionVariableExpression() != null)
            {
                ListIterate.collect(ctx.qualifiedPropertyBody().functionVariableExpression(), fveCtx -> functionVariableExpression(fveCtx, typeParameterNames, importId, ""), vars);
            }
            if (ctx.qualifiedPropertyBody().codeBlock() != null)
            {
                LambdaContext lambdaContext = new LambdaContext(isOwner._idOrPath().replace("::", "_") + "_" + qualifiedPropertyName);
                code = this.codeBlock(ctx.qualifiedPropertyBody().codeBlock(), typeParameterNames, importId, lambdaContext, addLines, "");
            }
        }

        GenericTypeInstance variableGenericType = GenericTypeInstance.createPersistent(this.repository);
        variableGenericType._rawTypeCoreInstance(isOwner);

        if (typeParameterNames.notEmpty())
        {
            MutableList<GenericType> typeArgs = typeParameterNames.collect(n -> GenericTypeInstance.createPersistent(this.repository)._typeParameter(TypeParameterInstance.createPersistent(this.repository, n)));
            variableGenericType._typeArguments(typeArgs);
        }
        if (multiplicityParameterNames.notEmpty())
        {
            MutableList<Multiplicity> multParameters = multiplicityParameterNames.collect(n -> MultiplicityInstance.createPersistent(this.repository, null, null)._multiplicityParameter(n));
            variableGenericType._multiplicityArguments(multParameters);
        }

        VariableExpressionInstance vei = VariableExpressionInstance.createPersistent(this.repository, variableGenericType, this.getPureOne(), "this");

        GenericTypeInstance ngt = GenericTypeInstance.createPersistent(this.repository);

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
        if (!genericType._typeArguments().isEmpty())
        {
            ngt._typeArguments(genericType._typeArguments());
        }
        ngt._typeParameter(genericType._typeParameter());
        if (!genericType._multiplicityArguments().isEmpty())
        {
            ngt._multiplicityArguments(genericType._multiplicityArguments());
        }

        QualifiedPropertyInstance qpi = QualifiedPropertyInstance.createPersistent(this.repository, qualifiedPropertyName,
                this.sourceInformation.getPureSourceInformation(ctx.identifier().getStart(), ctx.identifier().getStart(), ctx.getStop()), genericType, multiplicity, null);
        qpi._name(propertyName);
        qpi._functionName(propertyName);
        qpi._expressionSequence(code);
        qpi._stereotypesCoreInstance(stereotypes);
        qpi._taggedValues(tags);

        FunctionTypeInstance ft = FunctionTypeInstance.createPersistent(this.repository, qpi.getSourceInformation(), multiplicity, ngt);
        if (!typeParameterNames.isEmpty())
        {
            ft._typeParameters(this.processTypeParametersInstance(this.repository, typeParameterNames));
        }
        ft._parameters(Lists.mutable.<VariableExpression>of(vei).withAll(vars));
        GenericTypeInstance ftGenericTypeInstance = GenericTypeInstance.createPersistent(this.repository);
        ftGenericTypeInstance._rawTypeCoreInstance(ft);

        GenericTypeInstance genericTypeInstance = GenericTypeInstance.createPersistent(this.repository);
        genericTypeInstance._rawTypeCoreInstance(this.processorSupport.package_getByUserPath(M3Paths.QualifiedProperty));
        genericTypeInstance._typeArguments(Lists.fixedSize.<GenericType>of(ftGenericTypeInstance));
        qpi._classifierGenericType(genericTypeInstance);
        qualifiedProperties.add(qpi);
    }

    private void nativeFunction(NativeFunctionContext ctx, ImportGroup importId, String space, MutableList<CoreInstance> coreInstancesResult)
    {
        this.functionCounter++;
        NativeFunctionInstance function = NativeFunctionInstance.createPersistent(this.repository, ctx.qualifiedName().identifier().getText() + this.functionCounter, this.sourceInformation.getPureSourceInformation(ctx.NATIVE().getSymbol(), ctx.qualifiedName().identifier().getStart(), ctx.END_LINE().getSymbol()));

        MutableList<String> typeParametersNames = Lists.mutable.empty();
        MutableList<String> multiplicityParametersNames = Lists.mutable.empty();
        if (ctx.typeAndMultiplicityParameters() != null)
        {
            this.typeParametersAndMultiplicityParameters(ctx.typeAndMultiplicityParameters(), typeParametersNames, multiplicityParametersNames);
        }
        FunctionType signature = functionTypeSignature(ctx.functionTypeSignature(), function, typeParametersNames, multiplicityParametersNames, importId, spacePlusTabs(space, 1));

        function._functionName(ctx.qualifiedName().identifier().getText());
        PackageInstance packageInstance = this.buildPackage(ctx.qualifiedName().packagePath());

        function._package(packageInstance);
        packageInstance._childrenAdd(function);
        GenericTypeInstance genericTypeInstance = GenericTypeInstance.createPersistent(this.repository);
        Type type = (Type) this.processorSupport.package_getByUserPath(M3Paths.NativeFunction);
        genericTypeInstance._rawTypeCoreInstance(type);
        GenericTypeInstance genericTypeInstanceTa = GenericTypeInstance.createPersistent(this.repository);
        genericTypeInstanceTa._rawTypeCoreInstance(signature);
        genericTypeInstance._typeArguments(Lists.mutable.<GenericType>of(genericTypeInstanceTa));
        function._classifierGenericType(genericTypeInstance);
        coreInstancesResult.add(function);
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

        ListIterable<ValueSpecification> block = this.codeBlock(ctx.codeBlock(), typeParametersNames, importId, lambdaContext, addLines, spacePlusTabs(space, 2));

        functionDefinition._stereotypesCoreInstance(stereotypes);
        functionDefinition._taggedValues(tags);
        functionDefinition._functionName(ctx.qualifiedName().identifier().getText());
        PackageInstance packageInstance = this.buildPackage(ctx.qualifiedName().packagePath());
        functionDefinition._package(packageInstance);
        packageInstance._childrenAdd(functionDefinition);
        GenericTypeInstance genericTypeInstance = GenericTypeInstance.createPersistent(this.repository);
        Type type = (Type) this.processorSupport.package_getByUserPath(M3Paths.ConcreteFunctionDefinition);
        genericTypeInstance._rawTypeCoreInstance(type);
        GenericTypeInstance genericTypeInstanceTa = GenericTypeInstance.createPersistent(this.repository);
        genericTypeInstanceTa._rawTypeCoreInstance(signature);
        genericTypeInstance._typeArguments(Lists.mutable.<GenericType>of(genericTypeInstanceTa));
        functionDefinition._classifierGenericType(genericTypeInstance);

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
                    postConstraints.add(this.constraint(functionDefinition, cCtx, i, importId, lambdaContext, addLines, true));
                }
                else
                {
                    preConstraints.add(this.constraint(functionDefinition, cCtx, i, importId, lambdaContext, addLines, false));
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

        coreInstancesResult.add(functionDefinition);

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
            ft._multiplicityParameters(this.processMultiplicityParametersInstance(multiplicityParametersNames));
        }
        if (vars.notEmpty())
        {
            ft._parameters(vars);
        }
        return ft;
    }

    private ListIterable<InstanceValue> processMultiplicityParametersInstance(MutableList<String> multParameters)
    {
        MutableList<InstanceValue> result = Lists.mutable.of();
        for (String multParameter : multParameters)
        {
            InstanceValueInstance iv = InstanceValueInstance.createPersistent(this.repository, null, null);
            iv._values(Lists.mutable.of(this.repository.newStringCoreInstance_cached(multParameter)));
            GenericTypeInstance gt = GenericTypeInstance.createPersistent(this.repository);
            Type ti = (Type) this.processorSupport.package_getByUserPath("String");
            gt._rawTypeCoreInstance(ti);
            iv._genericType(gt);
            iv._multiplicity(this.getPureOne());
            result.add(iv);
        }
        return result;
    }

    private ListIterable<TypeParameter> processTypeParametersInstance(final ModelRepository repository, MutableList<String> typeParameters)
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
        if (ctx != null && ctx.type() != null)
        {
            ListIterate.collect(ctx.type(), typeCtx -> type(typeCtx, typeParametersNames, "", importId, addLines), result);
        }
        return result;
    }

    private GenericType processType(QualifiedNameContext classParserPath, MutableList<String> typeParametersNames, ListIterable<GenericType> possibleTypeArguments, ListIterable<Multiplicity> possibleMultiplicityArguments, ImportGroup importId)
    {
        GenericTypeInstance result = GenericTypeInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(classParserPath.identifier().getStart()));

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
                ImportStubInstance is = ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(classParserPath.identifier().getStart()), idOrPath, importId);
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
        return result;
    }

    private GenericType processUnitType(org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.UnitNameContext unitNameContext, ImportGroup importId)
    {
        GenericTypeInstance result = GenericTypeInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(unitNameContext.identifier().getStart()));
        String idOrPath = getUnitNameWithMeasure(unitNameContext);

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
            ImportStubInstance is = ImportStubInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(unitNameContext.identifier().getStart()), idOrPath, importId);
            result._rawTypeCoreInstance(is);
        }
        return result;
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
        return TaggedValueInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.STRING().getSymbol(), ctx.STRING().getSymbol()), importStubInstance, this.removeQuotes(ctx.STRING()));
    }

    private DefaultValue defaultValue(DefaultValueContext ctx, ImportGroup importId, String propertyName)
    {
        LambdaFunction<?> defaultValueFunctionLambda = null;

        DefaultValueExpressionContext expression = ctx.defaultValueExpression();

        ++this.functionCounter;
        LambdaContext lambdaContext = new LambdaContext(propertyName + "_defaultValue_" + this.functionCounter);

        DefaultValueInstance defaultValueInstance = DefaultValueInstance.createPersistent(this.repository,
                this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.EQUAL().getSymbol(), ctx.getStop()));

        CoreInstance defaultValueExpression = this.defaultValueExpression(ctx.defaultValueExpression(), importId, lambdaContext);
        if (defaultValueExpression != null)
        {
            SourceInformation source = defaultValueExpression.getSourceInformation();
            defaultValueFunctionLambda = LambdaFunctionInstance.createPersistent(this.repository, lambdaContext.getLambdaFunctionUniqueName(), source);
            CoreInstance functionType = this.repository.newAnonymousCoreInstance(source, this.processorSupport.package_getByUserPath(M3Paths.FunctionType), true);

            CoreInstance functionTypeGt = this.repository.newAnonymousCoreInstance(source, this.processorSupport.package_getByUserPath(M3Paths.GenericType), true);
            Instance.setValueForProperty(functionTypeGt, M3Properties.rawType, functionType, this.processorSupport);

            CoreInstance lambdaFunctionClass = this.processorSupport.package_getByUserPath(M3Paths.LambdaFunction);
            CoreInstance lambdaGenericType = org.finos.legend.pure.m3.navigation.type.Type.wrapGenericType(lambdaFunctionClass, this.processorSupport);
            Instance.setValueForProperty(lambdaGenericType, M3Properties.typeArguments, functionTypeGt, this.processorSupport);

            Instance.setValueForProperty(defaultValueFunctionLambda, M3Properties.expressionSequence, defaultValueExpression, this.processorSupport);
            Instance.setValueForProperty(defaultValueFunctionLambda, M3Properties.classifierGenericType, lambdaGenericType, this.processorSupport);
            Instance.setValueForProperty(functionType, M3Properties.function, defaultValueFunctionLambda, this.processorSupport);
        }

        defaultValueInstance._functionDefinition(defaultValueFunctionLambda);

        return defaultValueInstance;
    }

    private CoreInstance defaultValueExpression(DefaultValueExpressionContext ctx, ImportGroup importId, LambdaContext lambdaContext)
    {
        CoreInstance result = null;

        if (ctx.instanceLiteralToken() != null)
        {
            result = this.instanceLiteralToken(ctx.instanceLiteralToken(), true);
        }
        else if (ctx.instanceReference() != null)
        {
            result = instanceReference(ctx.instanceReference(), Lists.mutable.empty(), lambdaContext, importId, this.tabs(4), addLines);
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
            result = this.doWrap(expressions, ctx.defaultValueExpressionsArray().getStart().getLine(), ctx.defaultValueExpressionsArray().getStart().getCharPositionInLine(), ctx.getStop().getLine(), ctx.getStop().getCharPositionInLine());
        }

        if (ctx.propertyExpression() != null)
        {
            result = propertyExpression(ctx.propertyExpression(), result, Lists.mutable.empty(), Lists.mutable.empty(), lambdaContext, this.tabs(4), importId);
        }
        return result;
    }

    private String removeQuotes(TerminalNode stringNode)
    {
        return stringNode.getText().substring(1, stringNode.getText().length() - 1);
    }

    private String packageToString(PackagePathContext ctx)
    {
        return this.packageToList(ctx).makeString("::");
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

    private ProfileInstance profile(ProfileContext ctx)
    {
        PackageInstance packageInstance = this.buildPackage(ctx.qualifiedName().packagePath());
        String profileName = ctx.qualifiedName().identifier().getText();
        ProfileInstance profileInstance = ProfileInstance.createPersistent(this.repository, profileName);
        profileInstance._package(packageInstance);
        packageInstance._childrenAdd(profileInstance);
        profileInstance._name(profileName);
        profileInstance.setSourceInformation(this.sourceInformation.getPureSourceInformation(ctx.getStart(), ctx.qualifiedName().getStop(), ctx.getStop()));
        profileInstance._p_stereotypes(this.buildStereoTypes(ctx.stereotypeDefinitions(), profileInstance));
        profileInstance._p_tags(this.buildTags(ctx.tagDefinitions(), profileInstance));
        return profileInstance;
    }

    private ListIterable<Stereotype> buildStereoTypes(StereotypeDefinitionsContext ctx, ProfileInstance profileInstance)
    {
        if (ctx == null)
        {
            return Lists.fixedSize.empty();
        }
        MutableList<Stereotype> stereotypes = Lists.mutable.empty();
        for (IdentifierContext identifierContext : ctx.identifier())
        {
            stereotypes.add(StereotypeInstance.createPersistent(this.repository, identifierContext.getText(), this.sourceInformation.getPureSourceInformation(identifierContext.getStart()), profileInstance, identifierContext.getText()));
        }
        return stereotypes;
    }

    private ListIterable<Tag> buildTags(TagDefinitionsContext ctx, ProfileInstance profileInstance)
    {
        if (ctx == null)
        {
            return Lists.fixedSize.empty();
        }
        MutableList<Tag> tags = Lists.mutable.empty();
        for (IdentifierContext identifierContext : ctx.identifier())
        {
            tags.add(TagInstance.createPersistent(this.repository, identifierContext.getText(), this.sourceInformation.getPureSourceInformation(identifierContext.getStart()), profileInstance, identifierContext.getText()));
        }
        return tags;
    }

    public CoreInstance treePath(TreePathContext ctx, ImportGroup importId)
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
        ListIterable<ValueSpecification> codeSpecifications = this.codeBlock(ctx.codeBlock(), Lists.mutable.empty(), importId, lambdaContext, true, space);
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


    private PackageInstance buildPackage(PackagePathContext paths)
    {
        return this.buildPackage(paths == null ? Lists.mutable.empty() : ListAdapter.adapt(paths.identifier()));
    }

    private PackageInstance buildPackage(MutableList<IdentifierContext> paths)
    {
        PackageInstance parent = (PackageInstance) this.repository.getTopLevel(M3Paths.Root);
        if (parent == null)
        {
            throw new RuntimeException("Cannot find Root in model repository");
        }

        if (paths == null || Iterate.isEmpty(paths))
        {
            return parent;
        }

        for (IdentifierContext childToken : paths)
        {
            String childName = childToken.getText();
            synchronized (parent)
            {
                CoreInstance child = _Package.findInPackage(parent, childName);
                if (child == null)
                {
                    PackageInstance package_ = PackageInstance.createPersistent(this.repository, childName, null);
                    package_._name(childName);
                    package_._package(parent);
                    package_._children(Lists.immutable.empty());
                    parent._childrenAdd(package_);
                    child = package_;
                }
                else if (!(child instanceof PackageInstance))
                {
                    throw new PureParserException(this.sourceInformation.getSourceName(), childToken.getStart().getLine(), childToken.getStart().getCharPositionInLine(), "'" + org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement.getUserPathForPackageableElement(child)
                            + "' is a " + child.getClassifier().getName() + ", should be a Package", paths.collect(RuleContext::getText).makeString("::"));
                }
                parent = (PackageInstance) child;
            }
        }
        return parent;
    }

    public TemporaryPurePropertyMapping mappingLine(MappingLineContext ctx, LambdaContext lambdaContext, String cl, ImportGroup importId)
    {
        String sourceMappingId = null;
        String targetMappingId = null;
        Pair<String, SourceInformation> enumerationMappingInformation = null;

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
                targetMappingId = sctx.sourceId().qualifiedName().getText();
            }
        }

        if (ctx.ENUMERATION_MAPPING() != null)
        {
            IdentifierContext identifier = ctx.identifier();
            String enumerationMappingName = identifier.getText();
            SourceInformation enumerationMappingReferenceSourceInformation = this.sourceInformation.getPureSourceInformation(identifier.getStart(), identifier.getStart(), identifier.getStop());
            enumerationMappingInformation = Tuples.pair(enumerationMappingName, enumerationMappingReferenceSourceInformation);
        }

        return TemporaryPurePropertyMapping.build(
                this.sourceInformation.getPureSourceInformation(ctx.qualifiedName().getStart()),
                ctx.PLUS() != null,
                ctx.type() == null ? null : this.type(ctx.type(), Lists.mutable.empty(), "", importId, true),
                ctx.multiplicity() == null ? null : this.buildMultiplicity(ctx.multiplicity().multiplicityArgument()),
                ctx.qualifiedName().getText(),
                this.combinedExpression(ctx.combinedExpression(), "", Lists.mutable.empty(), lambdaContext, "", true, importId, true),
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
                combinedExpression(ctx.combinedExpression(), "", Lists.mutable.empty(), lambdaContext, "", true, importId, true);

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
                CoreInstance groupByExpression = this.combinedExpression(groupByFunctionSpecificationContext.combinedExpression(), "", Lists.mutable.empty(), lambdaContext, "", true, importId, true);
                SourceInformation sourceInformation = this.sourceInformation.getPureSourceInformation(groupByFunctionSpecificationContext.getStart(), groupByFunctionSpecificationContext.getStart(), groupByFunctionSpecificationContext.getStop());

                groupByFunctionSpecifications.add(TemporaryPureGroupByFunctionSpecification.build(sourceInformation, groupByExpression));
            }
        }

        org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.AggregationFunctionSpecificationsContext aggregationFunctionSpecificationsContext = aggSpecCtx.aggregationFunctionSpecifications();
        if (aggregationFunctionSpecificationsContext != null)
        {
            for (org.finos.legend.pure.m3.serialization.grammar.m3parser.antlr.M3Parser.AggregationFunctionSpecificationContext aggregationFunctionSpecificationContext : aggregationFunctionSpecificationsContext.aggregationFunctionSpecification())
            {
                CoreInstance mapExpression = this.combinedExpression(aggregationFunctionSpecificationContext.combinedExpression(0), "", Lists.mutable.empty(), lambdaContext, "", true, importId, true);
                CoreInstance aggregateExpression = this.combinedExpression(aggregationFunctionSpecificationContext.combinedExpression(1), "", Lists.mutable.empty(), lambdaContext, "", true, importId, true);
                SourceInformation sourceInformation = this.sourceInformation.getPureSourceInformation(aggregationFunctionSpecificationContext.getStart(), aggregationFunctionSpecificationContext.getStart(), aggregationFunctionSpecificationContext.getStop());

                aggregationFunctionSpecifications.add(TemporaryPureAggregationFunctionSpecification.build(sourceInformation, mapExpression, aggregateExpression));
            }
        }

        SourceInformation sourceInformation = this.sourceInformation.getPureSourceInformation(aggSpecCtx.getStart(), aggSpecCtx.getStart(), aggSpecCtx.getStop());
        return TemporaryPureAggregateSpecification.build(sourceInformation, index, canAggregate, groupByFunctionSpecifications, aggregationFunctionSpecifications);
    }

    public TemporaryPurePropertyMapping combinedExpression(CombinedExpressionContext ctx, String property, LambdaContext lambdaContext, ImportGroup importId)
    {
        CoreInstance expression = this.combinedExpression(ctx, "", Lists.mutable.empty(), lambdaContext, "", true, importId, true);
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

    public ImportGroupInstance buildImportGroupFromImport(String fileName, int count, ListIterable<? extends Import> imports, SourceInformation sourceInfo)
    {
        String id = createImportGroupId(fileName, count);
        ImportGroupInstance ig = ImportGroupInstance.createPersistent(this.repository, id, sourceInfo);
        ig._imports(imports);

        PackageInstance parent = (PackageInstance) this.processorSupport.package_getByUserPath("system::imports");
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

    private InstanceValue doWrap(MutableList<IdentifierContext> content, boolean addQuote)
    {
        ListIterable<CoreInstance> values = content.collect(val -> this.repository.newStringCoreInstance_cached(addQuote ? "'" + val.getText() + "'" : val.getText()));
        return InstanceValueInstance.createPersistent(this.repository, this.sourceInformation.getPureSourceInformation(content.getFirst().getStart(), content.getFirst().getStart(), content.getLast().getStart()), null, null)
                ._values(values);
    }

    private boolean userPathDefined(RichIterable<String> paths)
    {
        if (paths.isEmpty())
        {
            return false;
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
