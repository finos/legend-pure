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

package org.finos.legend.pure.m3;

import org.finos.legend.pure.m3.navigation.M3Properties;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MapIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.block.factory.Procedures;
import org.eclipse.collections.impl.block.factory.Procedures2;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation._class._Class;
import org.finos.legend.pure.m3.navigation.generictype.GenericType;
import org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.navigation.property.Property;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.junit.Assert;

import java.util.Formatter;

public class CompiledStateIntegrityTestTools
{
    /**
     * Test the given instance for all available integrity violations.
     *
     * @param instance         instance to test
     * @param processorSupport processor support
     */
    public static void testInstanceIntegrity(CoreInstance instance, ProcessorSupport processorSupport)
    {
        testInstanceIntegrity(Lists.immutable.with(instance), processorSupport);
    }

    /**
     * Test the given instance for all available integrity violations.
     *
     * @param instances        instances to test
     * @param processorSupport processor support
     */
    public static void testInstanceIntegrity(Iterable<? extends CoreInstance> instances, ProcessorSupport processorSupport)
    {
        runIntegrityTest(instances, new IntegrityTest()
        {
            @Override
            protected void forEachViolation(CoreInstance instance, Procedure<? super String> violationProcedure, ProcessorSupport processorSupport)
            {
                forEachClassifierGenericTypeViolation(instance, violationProcedure, processorSupport);
                forEachPropertyClassifierViolation(instance, violationProcedure, processorSupport);
                forEachPropertyValueTypeViolation(instance, violationProcedure, processorSupport);
                forEachPropertyValueMultiplicityViolation(instance, violationProcedure, processorSupport);
            }

            @Override
            protected String getViolationDescription()
            {
                return "instance integrity";
            }
        }, processorSupport);
    }

    /**
     * Test the classifierGenericType of an instance. This checks that the
     * rawType of the classifierGenericType matches the instance's classifier
     * and that the number of type and multplicity arguments are appropriate.
     *
     * @param instance         instance to test
     * @param processorSupport processor support
     */
    public static void testInstanceClassifierGenericType(CoreInstance instance, ProcessorSupport processorSupport)
    {
        testInstanceClassifierGenericType(Lists.immutable.with(instance), processorSupport);
    }

    /**
     * Test the classifierGenericType of the given instances. For each instance,
     * this checks that the rawType of the classifierGenericType matches the
     * instance's classifier and that the number of type and multplicity arguments
     * are appropriate.
     *
     * @param instances        instances to test
     * @param processorSupport processor support
     */
    public static void testInstanceClassifierGenericType(Iterable<? extends CoreInstance> instances, ProcessorSupport processorSupport)
    {
        runIntegrityTest(instances, new IntegrityTest()
        {
            @Override
            protected void forEachViolation(CoreInstance instance, Procedure<? super String> violationProcedure, ProcessorSupport processorSupport)
            {
                forEachClassifierGenericTypeViolation(instance, violationProcedure, processorSupport);
            }

            @Override
            protected String getViolationDescription()
            {
                return "instance classifierGenericType integrity";
            }
        }, processorSupport);
    }

    /**
     * Test the property integrity of the given instance. This checks that all
     * properties are appropriate for the instance's classifier and that all
     * property values satisfy the properties' types and multiplicities.
     *
     * @param instance         instance to test
     * @param processorSupport processor support
     */
    public static void testPropertyIntegrity(CoreInstance instance, ProcessorSupport processorSupport)
    {
        testPropertyIntegrity(Lists.immutable.with(instance), processorSupport);
    }

    /**
     * Test the property integrity of the given instances. For each instance,
     * this checks that all properties are appropriate for the instance's
     * classifier and that all property values satisfy the properties' types
     * and multiplicities.
     *
     * @param instances        instances to test
     * @param processorSupport processor support
     */
    public static void testPropertyIntegrity(Iterable<? extends CoreInstance> instances, ProcessorSupport processorSupport)
    {
        runIntegrityTest(instances, new IntegrityTest()
        {
            @Override
            protected void forEachViolation(CoreInstance instance, Procedure<? super String> violationProcedure, ProcessorSupport processorSupport)
            {
                forEachPropertyClassifierViolation(instance, violationProcedure, processorSupport);
                forEachPropertyValueTypeViolation(instance, violationProcedure, processorSupport);
                forEachPropertyValueMultiplicityViolation(instance, violationProcedure, processorSupport);
            }

            @Override
            protected String getViolationDescription()
            {
                return "property integrity";
            }
        }, processorSupport);
    }

    public static void testClassifierProperties(CoreInstance instance, ProcessorSupport processorSupport)
    {
        testClassifierProperties(Lists.immutable.with(instance), processorSupport);
    }

    public static void testClassifierProperties(Iterable<? extends CoreInstance> instances, ProcessorSupport processorSupport)
    {
        runIntegrityTest(instances, new IntegrityTest()
        {
            @Override
            protected void forEachViolation(CoreInstance instance, Procedure<? super String> violationProcedure, ProcessorSupport processorSupport)
            {
                forEachPropertyClassifierViolation(instance, violationProcedure, processorSupport);
            }

            @Override
            protected String getViolationDescription()
            {
                return "property classifier";
            }
        }, processorSupport);
    }

    public static void testPropertyValueMultiplicities(CoreInstance instance, ProcessorSupport processorSupport)
    {
        testPropertyValueMultiplicities(Lists.immutable.with(instance), processorSupport);
    }

    public static void testPropertyValueMultiplicities(Iterable<? extends CoreInstance> instances, ProcessorSupport processorSupport)
    {
        runIntegrityTest(instances, new IntegrityTest()
        {
            @Override
            protected void forEachViolation(CoreInstance instance, Procedure<? super String> violationProcedure, ProcessorSupport processorSupport)
            {
                forEachPropertyValueMultiplicityViolation(instance, violationProcedure, processorSupport);
            }

            @Override
            protected String getViolationDescription()
            {
                return "property value multiplicity";
            }
        }, processorSupport);
    }

    public static void testPropertyValueTypes(CoreInstance instance, ProcessorSupport processorSupport)
    {
        testPropertyValueTypes(Lists.immutable.with(instance), processorSupport);
    }

    public static void testPropertyValueTypes(Iterable<? extends CoreInstance> instances, ProcessorSupport processorSupport)
    {
        runIntegrityTest(instances, new IntegrityTest()
        {
            @Override
            protected void forEachViolation(CoreInstance instance, Procedure<? super String> violationProcedure, ProcessorSupport processorSupport)
            {
                forEachPropertyValueTypeViolation(instance, violationProcedure, processorSupport);
            }

            @Override
            protected String getViolationDescription()
            {
                return "property value type";
            }
        }, processorSupport);
    }

    private static void runIntegrityTest(Iterable<? extends CoreInstance> instances, IntegrityTest test, ProcessorSupport processorSupport)
    {
        MutableList<String> errorMessages = Lists.mutable.empty();
        Procedure<String> violationProcedure = Procedures.bind(Procedures2.<String>addToCollection(), errorMessages);
        Iterate.forEachWith(instances, test, Tuples.pair(violationProcedure, processorSupport));
        int errorCount = errorMessages.size();
        if (errorCount > 0)
        {
            StringBuilder message = new StringBuilder(errorCount * 128);
            new Formatter(message).format("There %s %,d %s violation", (errorCount == 1) ? "is" : "are", errorCount, test.getViolationDescription());
            if (errorCount != 1)
            {
                message.append('s');
            }
            message.append(":\n\t");
            errorMessages.appendString(message, "\n\t");
            Assert.fail(message.toString());
        }
    }

    /**
     * Apply the given procedure to each error message for a violation related the instance's
     * classifierGenericType. A violation occurs if the rawType of the classifierGenericType
     * does not match the instance's classifier or if the number of type and multiplicity
     * arguments differs from the required number.
     *
     * @param instance           instance to test
     * @param violationProcedure violation procedure
     * @param processorSupport   processor support
     */
    public static void forEachClassifierGenericTypeViolation(CoreInstance instance, Procedure<? super String> violationProcedure, ProcessorSupport processorSupport)
    {
        CoreInstance classifier = instance.getClassifier();
        ListIterable<? extends CoreInstance> typeParams = classifier.getValueForMetaPropertyToMany(M3Properties.typeParameters);
        ListIterable<? extends CoreInstance> multParams = classifier.getValueForMetaPropertyToMany(M3Properties.multiplicityParameters);

        CoreInstance classifierGenericType = instance.getValueForMetaPropertyToOne(M3Properties.classifierGenericType);
        if (classifierGenericType == null)
        {
            if (typeParams.notEmpty() || multParams.notEmpty())
            {
                StringBuilder message = new StringBuilder("Instance: ");
                message.append(instance);
                SourceInformation sourceInfo = instance.getSourceInformation();
                if (sourceInfo != null)
                {
                    message.append(" (");
                    sourceInfo.writeMessage(message);
                    message.append(")");
                }
                message.append("; classifierGenericType: null; problem: classifierGenericType required when there are type or multiplicity parameters: ");
                _Class.print(message, classifier, true);
                violationProcedure.value(message.toString());
            }
        }
        else
        {
            CoreInstance rawType = Instance.getValueForMetaPropertyToOneResolved(classifierGenericType, M3Properties.rawType, processorSupport);
            ListIterable<? extends CoreInstance> typeArgs = Instance.getValueForMetaPropertyToManyResolved(classifierGenericType, M3Properties.typeArguments, processorSupport);
            ListIterable<? extends CoreInstance> multArgs = Instance.getValueForMetaPropertyToManyResolved(classifierGenericType, M3Properties.multiplicityArguments, processorSupport);

            if (rawType != classifier)
            {
                StringBuilder message = new StringBuilder("Instance: ");
                message.append(instance);
                SourceInformation sourceInfo = instance.getSourceInformation();
                if (sourceInfo != null)
                {
                    message.append(" (");
                    sourceInfo.writeMessage(message);
                    message.append(")");
                }
                message.append("; classifierGenericType: ");
                GenericType.print(message, classifierGenericType, processorSupport);
                message.append("; problem: mismatch between rawType (");
                if (rawType == null)
                {
                    message.append("null");
                }
                else
                {
                    PackageableElement.writeUserPathForPackageableElement(message, rawType);
                }
                message.append(") and instance classifier (");
                PackageableElement.writeUserPathForPackageableElement(message, instance.getClassifier());
                message.append(")");
                violationProcedure.value(message.toString());
            }

            if (typeParams.size() != typeArgs.size())
            {
                StringBuilder message = new StringBuilder("Instance: ");
                message.append(instance);
                SourceInformation sourceInfo = instance.getSourceInformation();
                if (sourceInfo != null)
                {
                    message.append(" (");
                    sourceInfo.writeMessage(message);
                    message.append(")");
                }
                message.append("; classifierGenericType: ");
                GenericType.print(message, classifierGenericType, processorSupport);
                message.append("; classifier: ");
                _Class.print(message, rawType, true);
                message.append("; problem: mismatch between the number of type parameters (");
                message.append(typeParams.size());
                message.append(") and the number of type arguments (");
                message.append(typeArgs.size());
                message.append(")");
                violationProcedure.value(message.toString());
            }

            if (multParams.size() != multArgs.size())
            {
                StringBuilder message = new StringBuilder("Instance: ");
                message.append(instance);
                SourceInformation sourceInfo = instance.getSourceInformation();
                if (sourceInfo != null)
                {
                    message.append(" (");
                    sourceInfo.writeMessage(message);
                    message.append(")");
                }
                message.append("; classifierGenericType: ");
                GenericType.print(message, classifierGenericType, processorSupport);
                message.append("; classifier: ");
                _Class.print(message, rawType, true);
                message.append("; problem: mismatch between the number of multiplicity parameters (");
                message.append(multParams.size());
                message.append(") and the number of multiplicity arguments (");
                message.append(multArgs.size());
                message.append(")");
                violationProcedure.value(message.toString());
            }
        }
    }

    /**
     * Apply the given procedure to each error message related to a property classifier
     * violation. A property classifier violation is a case where the instance has a
     * property not defined for its classifier.
     *
     * @param instance           instance to test
     * @param violationProcedure violation procedure
     * @param processorSupport   processor support
     */
    public static void forEachPropertyClassifierViolation(CoreInstance instance, Procedure<? super String> violationProcedure, ProcessorSupport processorSupport)
    {
        MapIterable<String, CoreInstance> propertiesByName = processorSupport.class_getSimplePropertiesByName(instance.getClassifier());
        MutableList<String> invalidProperties = Lists.mutable.empty();
        for (String key : instance.getKeys())
        {
            if (!propertiesByName.containsKey(key))
            {
                invalidProperties.add(key);
            }
        }
        if (invalidProperties.notEmpty())
        {
            StringBuilder message = new StringBuilder("Instance: ");
            message.append(instance);
            SourceInformation sourceInfo = instance.getSourceInformation();
            if (sourceInfo != null)
            {
                message.append(" (");
                sourceInfo.writeMessage(message);
                message.append(")");
            }
            message.append("; invalid ");
            message.append((invalidProperties.size() == 1) ? "property" : "properties");
            invalidProperties.sortThis().appendString(message, ": ", ", ", "");
            violationProcedure.value(message.toString());
        }
    }

    /**
     * Apply the given procedure to each error message related to a property multiplicity
     * violation. A property multiplicity violation is a case where the number of values
     * for a property is inconsistent with its multiplicity.
     *
     * @param instance           instance to test
     * @param violationProcedure violation procedure
     * @param processorSupport   processor support
     */
    public static void forEachPropertyValueMultiplicityViolation(CoreInstance instance, Procedure<? super String> violationProcedure, ProcessorSupport processorSupport)
    {
        MapIterable<String, CoreInstance> propertiesByName = processorSupport.class_getSimplePropertiesByName(instance.getClassifier());
        for (Pair<String, CoreInstance> pair : propertiesByName.keyValuesView())
        {
            String propertyName = pair.getOne();
            ListIterable<? extends CoreInstance> values = instance.getValueForMetaPropertyToMany(propertyName);

            CoreInstance property = pair.getTwo();
            CoreInstance multiplicity = Property.resolveInstancePropertyReturnMultiplicity(instance, property, processorSupport);

            if (Multiplicity.isMultiplicityConcrete(multiplicity))
            {
                int count = values.size();
                if (!Multiplicity.isValid(multiplicity, count))
                {
                    StringBuilder message = new StringBuilder("Instance: ");
                    message.append(instance);
                    SourceInformation sourceInfo = instance.getSourceInformation();
                    if (sourceInfo != null)
                    {
                        message.append(" (");
                        sourceInfo.writeMessage(message);
                        message.append(")");
                    }
                    message.append("; property: ");
                    message.append(propertyName);
                    message.append("; multiplicity: ");
                    Multiplicity.print(message, multiplicity, false);
                    message.append("; count: ");
                    message.append(count);
                    violationProcedure.value(message.toString());
                }
            }
            else
            {
                StringBuilder message = new StringBuilder("Instance: ");
                message.append(instance);
                SourceInformation sourceInfo = instance.getSourceInformation();
                if (sourceInfo != null)
                {
                    message.append(" (");
                    sourceInfo.writeMessage(message);
                    message.append(")");
                }
                message.append("; property: ");
                message.append(propertyName);
                message.append("; multiplicity: ");
                Multiplicity.print(message, multiplicity, false);
                message.append("; problem: non-concrete");
                violationProcedure.value(message.toString());
            }
        }
    }

    /**
     * Apply the given procedure to each error message related to a property type
     * violation. A property type violation is a case where the type of the values
     * for a property is inconsistent with the property's type.
     *
     * @param instance           instance to test
     * @param violationProcedure violation procedure
     * @param processorSupport   processor support
     */
    public static void forEachPropertyValueTypeViolation(CoreInstance instance, Procedure<? super String> violationProcedure, ProcessorSupport processorSupport)
    {
        MapIterable<String, CoreInstance> propertiesByName = processorSupport.class_getSimplePropertiesByName(instance.getClassifier());
        for (Pair<String, CoreInstance> pair : propertiesByName.keyValuesView())
        {
            String propertyName = pair.getOne();
            ListIterable<? extends CoreInstance> values = Instance.getValueForMetaPropertyToManyResolved(instance, propertyName, processorSupport);

            CoreInstance property = pair.getTwo();
            CoreInstance genericType = GenericType.resolvePropertyReturnType(Instance.extractGenericTypeFromInstance(instance, processorSupport), property, processorSupport);

            if (GenericType.isGenericTypeFullyConcrete(genericType, processorSupport))
            {
                int i = 0;
                for (CoreInstance value : values)
                {
                    CoreInstance valueGenericType = Instance.extractGenericTypeFromInstance(value, processorSupport);
                    if (!GenericType.isGenericTypeFullyConcrete(valueGenericType, processorSupport))
                    {
                        // TODO what should we do here?
//                            StringBuilder message = new StringBuilder("Instance: ");
//                            message.append(node);
//                            SourceInformation sourceInfo = node.getSourceInformation();
//                            if (sourceInfo != null)
//                            {
//                                message.append(" (");
//                                sourceInfo.writeMessage(message);
//                                message.append(")");
//                            }
//                            message.append("; property: ");
//                            message.append(propertyName);
//                            message.append("; value: ");
//                            message.append(value);
//                            message.append("; index: ");
//                            message.append(i);
//                            message.append("; value generic type: ");
//                            GenericType.print(message, valueGenericType, context, processorSupport);
//                            message.append("; problem: non-concrete value generic type");
//                            errorMessages.add(message.toString());
                    }
                    else if (!GenericType.isGenericCompatibleWith(valueGenericType, genericType, processorSupport))
                    {
                        StringBuilder message = new StringBuilder("Instance: ");
                        message.append(instance);
                        SourceInformation sourceInfo = instance.getSourceInformation();
                        if (sourceInfo != null)
                        {
                            message.append(" (");
                            sourceInfo.writeMessage(message);
                            message.append(")");
                        }
                        message.append("; property: ");
                        message.append(propertyName);
                        message.append("; value: ");
                        message.append(value);
                        message.append("; index: ");
                        message.append(i);
                        message.append("; value generic type: ");
                        GenericType.print(message, valueGenericType, processorSupport);
                        message.append("; property generic type: ");
                        GenericType.print(message, genericType, processorSupport);
                        violationProcedure.value(message.toString());
                    }
                    i++;
                }
            }
            else
            {
                // TODO what should we do here?
//                    StringBuilder message = new StringBuilder("Instance: ");
//                    message.append(node);
//                    SourceInformation sourceInfo = node.getSourceInformation();
//                    if (sourceInfo != null)
//                    {
//                        message.append(" (");
//                        sourceInfo.writeMessage(message);
//                        message.append(")");
//                    }
//                    message.append("; property: ");
//                    message.append(propertyName);
//                    message.append("; property generic type: ");
//                    GenericType.print(message, genericType, context, processorSupport);
//                    message.append("; problem: non-concrete property generic type");
//                    errorMessages.add(message.toString());
            }
        }
    }

    private static abstract class IntegrityTest implements Procedure2<CoreInstance, Pair<? extends Procedure<? super String>, ? extends ProcessorSupport>>
    {

        @Override
        public void value(CoreInstance instance, Pair<? extends Procedure<? super String>, ? extends ProcessorSupport> violationProcedureAndProcessorSupport)
        {
            forEachViolation(instance, violationProcedureAndProcessorSupport.getOne(), violationProcedureAndProcessorSupport.getTwo());
        }

        protected abstract void forEachViolation(CoreInstance instance, Procedure<? super String> violationProcedure, ProcessorSupport processorSupport);

        protected abstract String getViolationDescription();
    }
}
