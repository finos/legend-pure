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

package org.finos.legend.pure.m3.navigation;

import org.finos.legend.pure.m4.ModelRepository;

/**
 * Paths for standard M3 model elements.
 */
public class M3Paths
{
    private M3Paths()
    {
    }

    // M3.pure
    public static final String AbstractProperty = "meta::pure::metamodel::function::property::AbstractProperty";
    public static final String AggregationKind = "meta::pure::metamodel::function::property::AggregationKind";
    public static final String AnnotatedElement = "meta::pure::metamodel::extension::AnnotatedElement";
    public static final String Annotation = "meta::pure::metamodel::extension::Annotation";
    public static final String Any = "meta::pure::metamodel::type::Any";
    public static final String Association = "meta::pure::metamodel::relationship::Association";
    public static final String AssociationProjection = "meta::pure::metamodel::relationship::AssociationProjection";
    public static final String Binary = ModelRepository.BINARY_TYPE_NAME;
    public static final String Boolean = ModelRepository.BOOLEAN_TYPE_NAME;
    public static final String ByteStream = ModelRepository.BYTE_STREAM_TYPE_NAME;
    public static final String Class = "meta::pure::metamodel::type::Class";
    public static final String ClassConstraintValueSpecificationContext = "meta::pure::metamodel::valuespecification::ClassConstraintValueSpecificationContext";
    public static final String ClassProjection = "meta::pure::metamodel::type::ClassProjection";
    public static final String ConcreteFunctionDefinition = "meta::pure::metamodel::function::ConcreteFunctionDefinition";
    public static final String Constraint = "meta::pure::metamodel::constraint::Constraint";
    public static final String ConstraintsOverride = "meta::pure::metamodel::type::ConstraintsOverride";
    public static final String ConstraintsGetterOverride = "meta::pure::metamodel::type::ConstraintsGetterOverride";
    public static final String coreImport = "system::imports::coreImport";
    public static final String DataType = "meta::pure::metamodel::type::DataType";
    public static final String Date = ModelRepository.DATE_TYPE_NAME;
    public static final String DateTime = ModelRepository.DATETIME_TYPE_NAME;
    public static final String Decimal = ModelRepository.DECIMAL_TYPE_NAME;
    public static final String DefaultValue = "meta::pure::metamodel::function::property::DefaultValue";
    public static final String elementOverride = "meta::pure::metamodel::type::ElementOverride" ;
    public static final String ElementWithConstraints = "meta::pure::metamodel::extension::ElementWithConstraints";
    public static final String ElementWithStereotypes = "meta::pure::metamodel::extension::ElementWithStereotypes";
    public static final String ElementWithTaggedValues = "meta::pure::metamodel::extension::ElementWithTaggedValues";
    public static final String Enum = "meta::pure::metamodel::type::Enum";
    public static final String Enumeration = "meta::pure::metamodel::type::Enumeration";
    public static final String EnumStub = "meta::pure::metamodel::import::EnumStub";
    public static final String ExistingPropertyRouteNode = "meta::pure::metamodel::treepath::ExistingPropertyRouteNode";
    public static final String ExpressionSequenceValueSpecificationContext = "meta::pure::metamodel::valuespecification::ExpressionSequenceValueSpecificationContext";
    public static final String Float = ModelRepository.FLOAT_TYPE_NAME;
    public static final String Function = "meta::pure::metamodel::function::Function";
    public static final String FunctionDefinition = "meta::pure::metamodel::function::FunctionDefinition";
    public static final String FunctionExpression = "meta::pure::metamodel::valuespecification::FunctionExpression";
    public static final String FunctionType = "meta::pure::metamodel::type::FunctionType";
    public static final String Generalization = "meta::pure::metamodel::relationship::Generalization";
    public static final String GeneralizationView = "meta::pure::diagram::GeneralizationView";
    public static final String GenericType = "meta::pure::metamodel::type::generics::GenericType";
    public static final String GetterOverride = "meta::pure::metamodel::type::GetterOverride" ;
    public static final String GrammarInfoStub = "meta::pure::tools::GrammarInfoStub";
    public static final String ImportGroup = "meta::pure::metamodel::import::ImportGroup";
    public static final String ImportStub = "meta::pure::metamodel::import::ImportStub";
    public static final String InferredGenericType = "meta::pure::metamodel::type::generics::InferredGenericType";
    public static final String Integer = ModelRepository.INTEGER_TYPE_NAME;
    public static final String InstanceValue = "meta::pure::metamodel::valuespecification::InstanceValue";
    public static final String InstanceValueSpecificationContext = "meta::pure::metamodel::valuespecification::InstanceValueSpecificationContext";
    public static final String KeyExpression = "meta::pure::functions::lang::KeyExpression";
    public static final String KeyValueValueSpecificationContext = "meta::pure::metamodel::valuespecification::KeyValueValueSpecificationContext";
    public static final String LambdaFunction = "meta::pure::metamodel::function::LambdaFunction";
    public static final String LatestDate = ModelRepository.LATEST_DATE_TYPE_NAME;
    public static final String Measure = "meta::pure::metamodel::type::Measure";
    public static final String ModelElement = "meta::pure::metamodel::ModelElement";
    public static final String Multiplicity = "meta::pure::metamodel::multiplicity::Multiplicity";
    public static final String MultiplicityValue = "meta::pure::metamodel::multiplicity::MultiplicityValue";
    public static final String NativeFunction = "meta::pure::metamodel::function::NativeFunction";
    public static final String Nil = "meta::pure::metamodel::type::Nil";
    public static final String NewPropertyRouteNode = "meta::pure::metamodel::treepath::NewPropertyRouteNode";
    public static final String NewPropertyRouteNodeFunctionDefinition = "meta::pure::metamodel::treepath::NewPropertyRouteNodeFunctionDefinition";
    public static final String NonExecutableValueSpecification = "meta::pure::metamodel::valuespecification::NonExecutableValueSpecification";
    public static final String Number = "Number";
    public static final String OneMany = "meta::pure::metamodel::multiplicity::OneMany";
    public static final String Package = "Package";
    public static final String PackageableElement = "meta::pure::metamodel::PackageableElement";
    public static final String PackageableMultiplicity = "meta::pure::metamodel::multiplicity::PackageableMultiplicity";
    public static final String ParameterValueSpecificationContext = "meta::pure::metamodel::valuespecification::ParameterValueSpecificationContext";
    public static final String PrimitiveType = "meta::pure::metamodel::type::PrimitiveType";
    public static final String Profile = "meta::pure::metamodel::extension::Profile";
    public static final String PropertyRouteNode = "meta::pure::metamodel::treepath::PropertyRouteNode";
    public static final String Property = "meta::pure::metamodel::function::property::Property";
    public static final String PropertyOwner = "meta::pure::metamodel::PropertyOwner";
    public static final String PropertyStub = "meta::pure::metamodel::import::PropertyStub";
    public static final String PureOne = "meta::pure::metamodel::multiplicity::PureOne";
    public static final String PureZero = "meta::pure::metamodel::multiplicity::PureZero";
    public static final String QualifiedProperty = "meta::pure::metamodel::function::property::QualifiedProperty";
    public static final String ReferenceUsage = "meta::pure::metamodel::ReferenceUsage";
    public static final String Root = "Root";
    public static final String RootRouteNode = "meta::pure::metamodel::treepath::RootRouteNode";
    public static final String RouteNodePropertyStub = "meta::pure::metamodel::treepath::RouteNodePropertyStub";
    public static final String SimpleFunctionExpression = "meta::pure::metamodel::valuespecification::SimpleFunctionExpression";
    public static final String SourceInformation = "meta::pure::functions::meta::SourceInformation";
    public static final String Stereotype = "meta::pure::metamodel::extension::Stereotype";
    public static final String StrictDate = ModelRepository.STRICT_DATE_TYPE_NAME;
    public static final String StrictTime = ModelRepository.STRICT_TIME_TYPE_NAME;
    public static final String String = ModelRepository.STRING_TYPE_NAME;
    public static final String Tag = "meta::pure::metamodel::extension::Tag";
    public static final String TaggedValue = "meta::pure::metamodel::extension::TaggedValue";
    public static final String Type = "meta::pure::metamodel::type::Type";
    public static final String TypeParameter = "meta::pure::metamodel::type::generics::TypeParameter";
    public static final String Unit = "meta::pure::metamodel::type::Unit";
    public static final String ValueSpecification = "meta::pure::metamodel::valuespecification::ValueSpecification";
    public static final String VariableExpression = "meta::pure::metamodel::valuespecification::VariableExpression";
    public static final String ZeroMany = "meta::pure::metamodel::multiplicity::ZeroMany";
    public static final String ZeroOne = "meta::pure::metamodel::multiplicity::ZeroOne";

    // GRAPH
    public static final String RootGraphFetchTree = "meta::pure::graphFetch::RootGraphFetchTree";

    // PATH
    public static final String CastPathElement = "meta::pure::metamodel::path::CastPathElement";
    public static final String Path = "meta::pure::metamodel::path::Path";
    public static final String PathElement = "meta::pure::metamodel::path::PathElement";
    public static final String PropertyPathElement = "meta::pure::metamodel::path::PropertyPathElement";

    // Milestoning
    public static final String Milestoning = "meta::pure::profiles::milestoning";

    // Equality
    public static final String equality = "meta::pure::profiles::equality";

    // Access
    public static final String access = "meta::pure::profiles::access";



    // -- To Move

    // Routing
    public static final String RoutedValueSpecification = "meta::pure::router::RoutedValueSpecification";
    public static final String ClusteredValueSpecification = "meta::pure::router::clustering::ClusteredValueSpecification";

    // Diagram
    public static final String Diagram = "meta::pure::diagram::Diagram";
    public static final String TypeView = "meta::pure::diagram::TypeView";
    public static final String PropertyView = "meta::pure::diagram::PropertyView";
    public static final String AssociationView = "meta::pure::diagram::AssociationView";

    // Service
    public static final String service = "meta::pure::service::service";
    public static final String ServiceResult = "meta::pure::service::ServiceResult";

    // Collections
    public static final String ValueHolder = "meta::pure::functions::collection::ValueHolder";
    public static final String TreeNode = "meta::pure::functions::collection::TreeNode";
    public static final String Pair = "meta::pure::functions::collection::Pair";
    public static final String List = "meta::pure::functions::collection::List";
    public static final String Map = "meta::pure::functions::collection::Map";
    public static final String MapStats = "meta::pure::functions::collection::MapStats";


}