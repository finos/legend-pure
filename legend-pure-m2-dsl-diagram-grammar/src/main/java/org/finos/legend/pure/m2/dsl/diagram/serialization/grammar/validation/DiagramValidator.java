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

package org.finos.legend.pure.m2.dsl.diagram.serialization.grammar.validation;

import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.impl.utility.LazyIterate;
import org.finos.legend.pure.m2.dsl.diagram.M2DiagramPaths;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.SourceMutation;
import org.finos.legend.pure.m3.compiler.Context;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.compiler.validation.ValidatorState;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.AssociationView;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.Diagram;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.GeneralizationView;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.PropertyView;
import org.finos.legend.pure.m3.coreinstance.meta.pure.diagram.TypeView;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PropertyOwner;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.AbstractProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.tools.GrammarInfoStub;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.tools.matcher.MatchRunner;
import org.finos.legend.pure.m3.tools.matcher.Matcher;
import org.finos.legend.pure.m3.tools.matcher.MatcherState;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.exception.PureCompilationException;

public class DiagramValidator implements MatchRunner<Diagram>
{
    @Override
    public String getClassName()
    {
        return M2DiagramPaths.Diagram;
    }

    @Override
    public void run(Diagram diagram, MatcherState state, Matcher matcher, ModelRepository modelRepository, Context context) throws PureCompilationException
    {
        ValidatorState validatorState = (ValidatorState)state;
        ProcessorSupport processorSupport = validatorState.getProcessorSupport();

        // Note: there is nothing to validate in type views.

        for (AssociationView associationView : LazyIterate.reject(diagram._associationViews(), SourceMutation.IS_MARKED_FOR_DELETION))
        {
            validateAssociationView(associationView, processorSupport);
        }

        for (PropertyView propertyView : LazyIterate.reject(diagram._propertyViews(), SourceMutation.IS_MARKED_FOR_DELETION))
        {
            validatePropertyView(propertyView, processorSupport);
        }

        for (GeneralizationView generalizationView : LazyIterate.reject(diagram._generalizationViews(), SourceMutation.IS_MARKED_FOR_DELETION))
        {
            validateGeneralizationView(generalizationView, processorSupport);
        }
    }

    private void validateAssociationView(AssociationView associationView, ProcessorSupport processorSupport)
    {
        Association association = (Association)ImportStub.withImportStubByPass(associationView._associationCoreInstance(), processorSupport);
        Type source = (Type)ImportStub.withImportStubByPass(((TypeView)((GrammarInfoStub)associationView._sourceCoreInstance())._value())._typeCoreInstance(), processorSupport);
        Type target = (Type)ImportStub.withImportStubByPass(((TypeView)((GrammarInfoStub)associationView._targetCoreInstance())._value())._typeCoreInstance(), processorSupport);

        ListIterable<? extends AbstractProperty<?>> properties = association._properties().toList();
        Type leftTarget = (Type)ImportStub.withImportStubByPass(((FunctionType)processorSupport.function_getFunctionType(properties.get(0)))._returnType()._rawTypeCoreInstance(), processorSupport);
        Type rightTarget = (Type)ImportStub.withImportStubByPass(((FunctionType)processorSupport.function_getFunctionType(properties.get(1)))._returnType()._rawTypeCoreInstance(), processorSupport);

        Type associationSource;
        Type associationTarget;
        if ((source == leftTarget) || (target == rightTarget))
        {
            associationSource = leftTarget;
            associationTarget = rightTarget;
        }
        else
        {
            associationSource = rightTarget;
            associationTarget = leftTarget;
        }


        // Check that source type matches property source type
        if (associationSource != source)
        {
            StringBuilder message = new StringBuilder("Source type for AssociationView ");
            message.append(associationView._id());
            message.append(" (");
            PackageableElement.writeUserPathForPackageableElement(message, source, "::");
            message.append(") does not match the source type of the association ");
            PackageableElement.writeUserPathForPackageableElement(message, association, "::");
            message.append(" (");
            PackageableElement.writeUserPathForPackageableElement(message, associationSource, "::");
            message.append(')');
            throw new PureCompilationException(associationView.getSourceInformation(), message.toString());
        }

        // Check that target type matches property target type
        if (associationTarget != target)
        {
            StringBuilder message = new StringBuilder("Target type for AssociationView ");
            message.append(associationView._id());
            message.append(" (");
            PackageableElement.writeUserPathForPackageableElement(message, target, "::");
            message.append(") does not match the target type of the association ");
            PackageableElement.writeUserPathForPackageableElement(message, association, "::");
            message.append(" (");
            PackageableElement.writeUserPathForPackageableElement(message, associationTarget, "::");
            message.append(')');
            throw new PureCompilationException(associationView.getSourceInformation(), message.toString());
        }
    }

    private void validatePropertyView(PropertyView propertyView, ProcessorSupport processorSupport)
    {
        AbstractProperty property = (AbstractProperty)ImportStub.withImportStubByPass(propertyView._propertyCoreInstance(), processorSupport);
        FunctionType functionType = (FunctionType)processorSupport.function_getFunctionType(property);

        // Check that source type matches property source type
        Type source = (Type)ImportStub.withImportStubByPass(((TypeView)((GrammarInfoStub)propertyView._sourceCoreInstance())._value())._typeCoreInstance(), processorSupport);
        PropertyOwner propertySource = property._owner();
        if (propertySource != source)
        {
            StringBuilder message = new StringBuilder("Source type for PropertyView ");
            message.append(propertyView._id());
            message.append (" (");
            PackageableElement.writeUserPathForPackageableElement(message, source, "::");
            message.append (") does not match the owner of the property ");
            PackageableElement.writeUserPathForPackageableElement(message, propertySource, "::");
            message.append('.');
            message.append(Property.getPropertyName(property));
            message.append(" (");
            PackageableElement.writeUserPathForPackageableElement(message, propertySource, "::");
            message.append(')');
            throw new PureCompilationException(propertyView.getSourceInformation(), message.toString());
        }

        // Check that target type matches property target type
        Type target = (Type)ImportStub.withImportStubByPass(((TypeView)((GrammarInfoStub)propertyView._targetCoreInstance())._value())._typeCoreInstance(), processorSupport);
        Type propertyTarget = (Type)ImportStub.withImportStubByPass(functionType._returnType()._rawTypeCoreInstance(), processorSupport);
        if (propertyTarget != target)
        {
            StringBuilder message = new StringBuilder("Target type for PropertyView ");
            message.append(propertyView._id());
            message.append (" (");
            PackageableElement.writeUserPathForPackageableElement(message, target, "::");
            message.append (") does not match the target type of the property ");
            PackageableElement.writeUserPathForPackageableElement(message, propertySource, "::");
            message.append('.');
            message.append(Property.getPropertyName(property));
            message.append(" (");
            PackageableElement.writeUserPathForPackageableElement(message, propertyTarget, "::");
            message.append(')');
            throw new PureCompilationException(propertyView.getSourceInformation(), message.toString());
        }
    }

    private void validateGeneralizationView(GeneralizationView generalizationView, ProcessorSupport processorSupport)
    {
        Type source = (Type)ImportStub.withImportStubByPass(((TypeView)((GrammarInfoStub)generalizationView._sourceCoreInstance())._value())._typeCoreInstance(), processorSupport);
        Type target = (Type)ImportStub.withImportStubByPass(((TypeView)((GrammarInfoStub)generalizationView._targetCoreInstance())._value())._typeCoreInstance(), processorSupport);
        if (!org.finos.legend.pure.m3.navigation.type.Type.directSubTypeOf(source, target, processorSupport))
        {
            throw new PureCompilationException(generalizationView.getSourceInformation(), target.getName() + " is not a direct generalization of " + source.getName());
        }
    }
}
