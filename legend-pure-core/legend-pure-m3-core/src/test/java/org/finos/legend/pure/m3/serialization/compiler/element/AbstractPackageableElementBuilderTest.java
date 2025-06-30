// Copyright 2025 Goldman Sachs
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

package org.finos.legend.pure.m3.serialization.compiler.element;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.coreinstance.Package;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.ModelElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.PackageableElement;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.ReferenceUsage;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.constraint.Constraint;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.ElementWithConstraints;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Stereotype;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.Tag;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.TaggedValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.Property;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.property.QualifiedProperty;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Association;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relationship.Generalization;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Any;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enum;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Enumeration;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Measure;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.PrimitiveType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Unit;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.junit.Assert;

public abstract class AbstractPackageableElementBuilderTest extends AbstractElementBuilderTest<Package, PackageableElement, Any>
{
    @Override
    protected java.lang.Class<? extends Package> getExpectedVirtualPackageClass()
    {
        return loadJavaClass(getExpectedVirtualPackageClassName());
    }

    protected abstract String getExpectedVirtualPackageClassName();

    @Override
    protected java.lang.Class<? extends PackageableElement> getExpectedConcreteElementClass(String classifierPath)
    {
        return loadJavaClass(getExpectedConcreteElementClassName(classifierPath));
    }

    protected abstract String getExpectedConcreteElementClassName(String classifierPath);

    @Override
    protected java.lang.Class<? extends Any> getExpectedComponentInstanceClass(String classifierPath)
    {
        return loadJavaClass(getExpectedComponentInstanceClassName(classifierPath));
    }

    protected abstract String getExpectedComponentInstanceClassName(String classifierPath);

    @Override
    protected void testConcreteElement(String path, String classifierPath, PackageableElement element)
    {
        super.testConcreteElement(path, classifierPath, element);
        testElement(path, element);
    }

    @Override
    protected void testVirtualPackage(String path, Package element)
    {
        super.testVirtualPackage(path, element);
        testElement(path, element);
    }

    @SuppressWarnings("unchecked")
    private void testElement(String path, PackageableElement element)
    {
        PackageableElement srcElement = (PackageableElement) runtime.getCoreInstance(path);
        Assert.assertNotNull(path, srcElement);
        Assert.assertNotSame(path, srcElement, element);

        Assert.assertEquals(path, srcElement._name(), element._name());
        Assert.assertEquals(path, srcElement.getName(), element.getName());

        Package pkg = srcElement._package();
        if (pkg == null)
        {
            Assert.assertNull(path, element._package());
        }
        else
        {
            Assert.assertEquals(path, getUserPath(pkg), getUserPath(element._package()));
        }

        Assert.assertEquals(path, getStereotypeSpecs(srcElement._stereotypes()), getStereotypeSpecs(element._stereotypes()));
        assertComponentInstanceClass(path, M3Paths.Stereotype, element._stereotypes());
        Assert.assertEquals(path, getTaggedValueSpecs(srcElement._taggedValues()), getTaggedValueSpecs(element._taggedValues()));
        assertComponentInstanceClass(path, M3Paths.TaggedValue, element._taggedValues());

        Assert.assertEquals(path, getRefUsageSpecs(srcElement._referenceUsages()).sortThis(), getRefUsageSpecs(element._referenceUsages()).sortThis());
        assertComponentInstanceClass(path, M3Paths.ReferenceUsage, element._referenceUsages());

        if (srcElement instanceof ElementWithConstraints)
        {
            Assert.assertTrue(path, element instanceof ElementWithConstraints);
            Assert.assertEquals(path,
                    getConstraintSpecs(((ElementWithConstraints) srcElement)._constraints()),
                    getConstraintSpecs(((ElementWithConstraints) element)._constraints()));
            assertComponentInstanceClass(path, M3Paths.Constraint, ((ElementWithConstraints) element)._constraints());
        }
        else
        {
            Assert.assertFalse(path, element instanceof ElementWithConstraints);
        }

        if (srcElement instanceof Package)
        {
            Assert.assertTrue(path, element instanceof Package);
            Package srcPackage = (Package) srcElement;
            Package pkgElement = (Package) element;

            Assert.assertEquals(path,
                    srcPackage._children().collect(PackageableElement::_name, Lists.mutable.empty()).sortThis(),
                    pkgElement._children().collect(PackageableElement::_name, Lists.mutable.empty()).sortThis());
        }
        else
        {
            Assert.assertFalse(path, element instanceof Package);
        }

        if (srcElement instanceof Type)
        {
            Assert.assertTrue(path, element instanceof Type);
            Type srcType = (Type) srcElement;
            Type type = (Type) element;

            Assert.assertEquals(path, getGeneralizationSpecs(srcType._generalizations(), true), getGeneralizationSpecs(type._generalizations(), false));
            assertComponentInstanceClass(path, M3Paths.Generalization, type._generalizations());
            Assert.assertEquals(path, getSpecializationSpecs(srcType._specializations()).sortThis(), getSpecializationSpecs(type._specializations()).sortThis());
            assertComponentInstanceClass(path, M3Paths.Generalization, type._specializations());
        }
        else
        {
            Assert.assertFalse(path, element instanceof Type);
        }

        if (srcElement instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class)
        {
            Assert.assertTrue(path, element instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class);
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> srcClass = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) srcElement;
            org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?> cls = (org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class<?>) element;

            Assert.assertEquals(path, getPropertySpecs(srcClass._properties(), true), getPropertySpecs(cls._properties(), false));
            assertComponentInstanceClass(path, M3Paths.Property, cls._properties());
            Assert.assertEquals(path, getQualifiedPropertySpecs(srcClass._qualifiedProperties(), true), getQualifiedPropertySpecs(cls._qualifiedProperties(), false));
            assertComponentInstanceClass(path, M3Paths.QualifiedProperty, cls._qualifiedProperties());
            Assert.assertEquals(path, getPropertySpecs(srcClass._propertiesFromAssociations(), true), getPropertySpecs(cls._propertiesFromAssociations(), false));
            assertComponentInstanceClass(path, M3Paths.Property, cls._propertiesFromAssociations());
            Assert.assertEquals(path, getQualifiedPropertySpecs(srcClass._qualifiedPropertiesFromAssociations(), true), getQualifiedPropertySpecs(cls._qualifiedPropertiesFromAssociations(), false));
            assertComponentInstanceClass(path, M3Paths.QualifiedProperty, cls._qualifiedPropertiesFromAssociations());
            Assert.assertEquals(path, getVariableExpressionSpecs(srcClass._typeVariables(), true), getVariableExpressionSpecs(cls._typeVariables(), false));
            assertComponentInstanceClass(path, M3Paths.VariableExpression, cls._typeVariables());
        }
        else
        {
            Assert.assertFalse(path, element instanceof org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Class);
        }

        if (srcElement instanceof PrimitiveType)
        {
            Assert.assertTrue(path, element instanceof PrimitiveType);
            PrimitiveType srcPrimitiveType = (PrimitiveType) srcElement;
            PrimitiveType primitiveType = (PrimitiveType) element;

            Assert.assertEquals(path, srcPrimitiveType._extended(), primitiveType._extended());
            Assert.assertEquals(path, getVariableExpressionSpecs(srcPrimitiveType._typeVariables(), true), getVariableExpressionSpecs(primitiveType._typeVariables(), false));
            assertComponentInstanceClass(path, M3Paths.VariableExpression, primitiveType._typeVariables());
        }
        else
        {
            Assert.assertFalse(path, element instanceof PrimitiveType);
        }

        if (srcElement instanceof Enumeration)
        {
            Assert.assertTrue(path, element instanceof Enumeration);
            Enumeration<? extends Enum> srcEnumeration = (Enumeration<? extends Enum>) srcElement;
            Enumeration<? extends Enum> enumeration = (Enumeration<? extends Enum>) element;

            Assert.assertEquals(path, srcEnumeration._values().collect(Enum::_name, Lists.mutable.empty()), enumeration._values().collect(Enum::_name, Lists.mutable.empty()));
        }
        else
        {
            Assert.assertFalse(path, element instanceof Enumeration);
        }

        if (srcElement instanceof Association)
        {
            Assert.assertTrue(path, element instanceof Association);
            Association srcAssociation = (Association) srcElement;
            Association association = (Association) element;

            Assert.assertEquals(path, getPropertySpecs(srcAssociation._properties(), true), getPropertySpecs(association._properties(), false));
            assertComponentInstanceClass(path, M3Paths.Property, association._properties());
            Assert.assertEquals(path, getQualifiedPropertySpecs(srcAssociation._qualifiedProperties(), true), getQualifiedPropertySpecs(association._qualifiedProperties(), false));
            assertComponentInstanceClass(path, M3Paths.QualifiedProperty, association._qualifiedProperties());
        }
        else
        {
            Assert.assertFalse(path, element instanceof Association);
        }

        if (srcElement instanceof Measure)
        {
            Assert.assertTrue(path, element instanceof Measure);
            Measure srcMeasure = (Measure) srcElement;
            Measure measure = (Measure) element;

            Assert.assertEquals(srcMeasure._canonicalUnit()._name(), measure._canonicalUnit()._name());
            assertComponentInstanceClass(path, M3Paths.Unit, measure._canonicalUnit());
            Assert.assertEquals(srcMeasure._nonCanonicalUnits().collect(Unit::_name, Lists.mutable.empty()), measure._nonCanonicalUnits().collect(Unit::_name, Lists.mutable.empty()));
            assertComponentInstanceClass(path, M3Paths.Unit, measure._nonCanonicalUnits());
        }
        else
        {
            Assert.assertFalse(path, element instanceof Measure);
        }

        if (srcElement instanceof FunctionDefinition)
        {
            Assert.assertTrue(path, element instanceof FunctionDefinition);
            FunctionDefinition<?> srcFunc = (FunctionDefinition<?>) srcElement;
            FunctionDefinition<?> func = (FunctionDefinition<?>) element;

            FunctionType srcFuncType = (FunctionType) srcFunc._classifierGenericType()._typeArguments().getOnly()._rawType();
            FunctionType funcType = (FunctionType) func._classifierGenericType()._typeArguments().getOnly()._rawType();
            assertComponentInstanceClass(path, M3Paths.FunctionType, funcType);
            Assert.assertEquals(path, getVariableExpressionSpecs(srcFuncType._parameters(), true), getVariableExpressionSpecs(funcType._parameters(), false));
            assertComponentInstanceClass(path, M3Paths.VariableExpression, funcType._parameters());
            Assert.assertEquals(path, printGenericType(srcFuncType._returnType(), true), printGenericType(funcType._returnType(), false));
            assertComponentInstanceClass(path, M3Paths.GenericType, funcType._returnType());

            Assert.assertEquals(path, srcFunc._applications().size(), func._applications().size());
            Assert.assertEquals(path, srcFunc._expressionSequence().size(), func._expressionSequence().size());
        }
        else
        {
            Assert.assertFalse(path, element instanceof FunctionDefinition);
        }
    }

    private MutableList<String> getGeneralizationSpecs(RichIterable<? extends Generalization> generalizations, boolean sourceModel)
    {
        return generalizations.collect(g -> getGeneralizationSpec(g, sourceModel), Lists.mutable.ofInitialCapacity(generalizations.size()));
    }

    private String getGeneralizationSpec(Generalization generalization, boolean sourceModel)
    {
        return printGenericType(generalization._general(), sourceModel);
    }

    private MutableList<String> getSpecializationSpecs(RichIterable<? extends Generalization> specializations)
    {
        return specializations.collect(this::getSpecializationSpec, Lists.mutable.ofInitialCapacity(specializations.size()));
    }

    private String getSpecializationSpec(Generalization specialization)
    {
        Type specific = specialization._specific();
        if (specific instanceof PackageableElement)
        {
            return getUserPath(specific);
        }
        if (specific instanceof ModelElement)
        {
            return appendUserPath(new StringBuilder(((ModelElement) specific)._name()).append(" instance of "), specific.getClassifier()).toString();
        }
        return appendUserPath(new StringBuilder(specific.getName()).append(" instance of "), specific.getClassifier()).toString();
    }

    private MutableList<String> getPropertySpecs(RichIterable<? extends Property<?, ?>> properties, boolean sourceModel)
    {
        return properties.collect(p -> getPropertySpec(p, sourceModel), Lists.mutable.ofInitialCapacity(properties.size()));
    }

    private String getPropertySpec(Property<?, ?> property, boolean sourceModel)
    {
        StringBuilder builder = new StringBuilder(property._name());
        GenericType.print(builder.append(':'), property._genericType(), true, getProcessorSupport(sourceModel));
        Multiplicity.print(builder, property._multiplicity(), true);
        return builder.toString();
    }

    private MutableList<String> getQualifiedPropertySpecs(RichIterable<? extends QualifiedProperty<?>> qualifiedProperties, boolean sourceModel)
    {
        return qualifiedProperties.collect(qp -> getQualifiedPropertySpec(qp, sourceModel), Lists.mutable.ofInitialCapacity(qualifiedProperties.size()));
    }

    private String getQualifiedPropertySpec(QualifiedProperty<?> qualifiedProperty, boolean sourceModel)
    {
        StringBuilder builder = new StringBuilder(qualifiedProperty._functionName()).append('(');
        ((FunctionType) qualifiedProperty._classifierGenericType()._typeArguments().getOnly()._rawType())._parameters()
                .forEach(p -> appendVariableExpressionSpec(builder, p, sourceModel).append(", "));
        if (builder.charAt(builder.length() - 1) == ' ')
        {
            builder.setLength(builder.length() - 2);
        }
        GenericType.print(builder.append("):"), qualifiedProperty._genericType(), true, getProcessorSupport(sourceModel));
        Multiplicity.print(builder, qualifiedProperty._multiplicity(), true);
        return builder.toString();
    }

    private MutableList<String> getStereotypeSpecs(RichIterable<? extends Stereotype> stereotypes)
    {
        return stereotypes.collect(this::getStereotypeSpec, Lists.mutable.ofInitialCapacity(stereotypes.size()));
    }

    private String getStereotypeSpec(Stereotype stereotype)
    {
        return appendUserPath(new StringBuilder(), stereotype._profile()).append('.').append(stereotype._value()).toString();
    }

    private MutableList<String> getTaggedValueSpecs(RichIterable<? extends TaggedValue> taggedValues)
    {
        return taggedValues.collect(this::getTaggedValueSpec, Lists.mutable.ofInitialCapacity(taggedValues.size()));
    }

    private String getTaggedValueSpec(TaggedValue taggedValue)
    {
        Tag tag = taggedValue._tag();
        return appendUserPath(new StringBuilder(), tag._profile())
                .append('.')
                .append(tag._value())
                .append("='")
                .append(taggedValue._value())
                .append('\'')
                .toString();
    }

    private MutableList<String> getVariableExpressionSpecs(RichIterable<? extends VariableExpression> vars, boolean sourceModel)
    {
        return vars.collect(v -> getVariableExpressionSpec(v, sourceModel), Lists.mutable.ofInitialCapacity(vars.size()));
    }

    private String getVariableExpressionSpec(VariableExpression var, boolean sourceModel)
    {
        return appendVariableExpressionSpec(new StringBuilder(), var, sourceModel).toString();
    }

    private StringBuilder appendVariableExpressionSpec(StringBuilder builder, VariableExpression var, boolean sourceModel)
    {
        builder.append(var._name());
        GenericType.print(builder.append(':'), var._genericType(), true, getProcessorSupport(sourceModel));
        Multiplicity.print(builder, var._multiplicity(), true);
        return builder;
    }

    private MutableList<String> getRefUsageSpecs(RichIterable<? extends ReferenceUsage> refUsages)
    {
        return refUsages.collect(this::getRefUsageSpec, Lists.mutable.ofInitialCapacity(refUsages.size()));
    }

    private String getRefUsageSpec(ReferenceUsage refUsage)
    {
        // we don't use the owner here to avoid resolving the reference, which can be expensive when there are a large number
        return "ReferenceUsage{property=" + refUsage._propertyName() + " offset=" + refUsage._offset() + " }";
    }

    private MutableList<String> getConstraintSpecs(RichIterable<? extends Constraint> constraints)
    {
        return constraints.collect(this::getConstraintSpec, Lists.mutable.ofInitialCapacity(constraints.size()));
    }

    private String getConstraintSpec(Constraint constraint)
    {
        // TODO improve this
        return constraint._name();
    }

    @SuppressWarnings("unchecked")
    private static <T> java.lang.Class<T> loadJavaClass(String name)
    {
        try
        {
            return (java.lang.Class<T>) Thread.currentThread().getContextClassLoader().loadClass(name);
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }
}
