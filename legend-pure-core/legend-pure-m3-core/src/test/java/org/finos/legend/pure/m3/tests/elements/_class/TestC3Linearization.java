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

package org.finos.legend.pure.m3.tests.elements._class;

import org.finos.legend.pure.m3.navigation.linearization.C3Linearization;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.multimap.list.MutableListMultimap;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.multimap.list.FastListMultimap;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.M3PropertyPaths;
import org.finos.legend.pure.m3.navigation.M3ProcessorSupport;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstance;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class TestC3Linearization
{
    private ProcessorSupport processorSupport;

    @Before
    public void setUp()
    {
        this.processorSupport = new M3ProcessorSupport(null);
    }

    // Valid hierarchies

    @Test
    public void testNoGeneralizations()
    {
        // A
        CoreInstance typeA = newType("A");
        assertTypeLinearization(Lists.immutable.with(typeA), typeA);
    }

    @Test
    public void testSingleGeneralization()
    {
        // B
        // |
        // A
        CoreInstance typeA = newType("A");
        CoreInstance typeB = newType("B");
        addGeneralization(typeA, typeB);
        assertTypeLinearization(Lists.immutable.with(typeA, typeB), typeA);
        assertTypeLinearization(Lists.immutable.with(typeB), typeB);
    }

    @Test
    public void testLinearGeneralizations()
    {
        // C
        // |
        // B
        // |
        // A
        CoreInstance typeA = newType("A");
        CoreInstance typeB = newType("B");
        CoreInstance typeC = newType("C");
        addGeneralization(typeA, typeB);
        addGeneralization(typeB, typeC);
        assertTypeLinearization(Lists.immutable.with(typeA, typeB, typeC), typeA);
        assertTypeLinearization(Lists.immutable.with(typeB, typeC), typeB);
        assertTypeLinearization(Lists.immutable.with(typeC), typeC);
    }

    @Test
    public void testSimpleGeneralizationTree()
    {
        //    C
        //  /  \
        // A   B
        CoreInstance typeA = newType("A");
        CoreInstance typeB = newType("B");
        CoreInstance typeC = newType("C");
        addGeneralization(typeA, typeC);
        addGeneralization(typeB, typeC);
        assertTypeLinearization(Lists.immutable.with(typeA, typeC), typeA);
        assertTypeLinearization(Lists.immutable.with(typeB, typeC), typeB);
        assertTypeLinearization(Lists.immutable.with(typeC), typeC);
    }

    @Test
    public void testGeneralizationDiamond1()
    {
        //    D
        //  /  \
        // B    C
        //  \  /
        //   A
        CoreInstance typeA = newType("A");
        CoreInstance typeB = newType("B");
        CoreInstance typeC = newType("C");
        CoreInstance typeD = newType("D");
        addGeneralization(typeA, typeB);
        addGeneralization(typeA, typeC);
        addGeneralization(typeB, typeD);
        addGeneralization(typeC, typeD);
        assertTypeLinearization(Lists.immutable.with(typeA, typeB, typeC, typeD), typeA);
        assertTypeLinearization(Lists.immutable.with(typeB, typeD), typeB);
        assertTypeLinearization(Lists.immutable.with(typeC, typeD), typeC);
        assertTypeLinearization(Lists.immutable.with(typeD), typeD);
    }

    @Test
    public void testGeneralizationDiamond2()
    {
        //    D
        //  /  \
        // C    B
        //  \  /
        //   A
        CoreInstance typeA = newType("A");
        CoreInstance typeB = newType("B");
        CoreInstance typeC = newType("C");
        CoreInstance typeD = newType("D");
        addGeneralization(typeA, typeC);
        addGeneralization(typeA, typeB);
        addGeneralization(typeB, typeD);
        addGeneralization(typeC, typeD);
        assertTypeLinearization(Lists.immutable.with(typeA, typeC, typeB, typeD), typeA);
        assertTypeLinearization(Lists.immutable.with(typeB, typeD), typeB);
        assertTypeLinearization(Lists.immutable.with(typeC, typeD), typeC);
        assertTypeLinearization(Lists.immutable.with(typeD), typeD);
    }

    @Test
    public void testComplexGeneralization1()
    {
        //    E
        //  /   \
        // C     D
        //  \  / |
        //   B  /
        //   | /
        //   A
        CoreInstance typeA = newType("A");
        CoreInstance typeB = newType("B");
        CoreInstance typeC = newType("C");
        CoreInstance typeD = newType("D");
        CoreInstance typeE = newType("E");
        addGeneralization(typeA, typeB);
        addGeneralization(typeA, typeD);
        addGeneralization(typeB, typeC);
        addGeneralization(typeB, typeD);
        addGeneralization(typeC, typeE);
        addGeneralization(typeD, typeE);
        assertTypeLinearization(Lists.immutable.with(typeA, typeB, typeC, typeD, typeE), typeA);
        assertTypeLinearization(Lists.immutable.with(typeB, typeC, typeD, typeE), typeB);
        assertTypeLinearization(Lists.immutable.with(typeC, typeE), typeC);
        assertTypeLinearization(Lists.immutable.with(typeD, typeE), typeD);
        assertTypeLinearization(Lists.immutable.with(typeE), typeE);
    }

    @Test
    public void testComplexGeneralization2()
    {
        //    E
        //  /   \
        // C     D
        //  \  / |
        //   B  /
        //   | /
        //   A
        // (but B->D precedes B->C)
        CoreInstance typeA = newType("A");
        CoreInstance typeB = newType("B");
        CoreInstance typeC = newType("C");
        CoreInstance typeD = newType("D");
        CoreInstance typeE = newType("E");
        addGeneralization(typeA, typeB);
        addGeneralization(typeA, typeD);
        addGeneralization(typeB, typeD);
        addGeneralization(typeB, typeC);
        addGeneralization(typeC, typeE);
        addGeneralization(typeD, typeE);
        assertTypeLinearization(Lists.immutable.with(typeA, typeB, typeD, typeC, typeE), typeA);
        assertTypeLinearization(Lists.immutable.with(typeB, typeD, typeC, typeE), typeB);
        assertTypeLinearization(Lists.immutable.with(typeC, typeE), typeC);
        assertTypeLinearization(Lists.immutable.with(typeD, typeE), typeD);
        assertTypeLinearization(Lists.immutable.with(typeE), typeE);
    }

    // Invalid hierarchies

    @Test
    public void testSimpleLoop()
    {
        // A
        // |
        // A
        CoreInstance typeA = newType("A");
        addGeneralization(typeA, typeA);
        assertCompilationException(typeA);
    }

    @Test
    public void testIndirectLoop()
    {
        // A
        // |
        // B
        // |
        // A
        CoreInstance typeA = newType("A");
        CoreInstance typeB = newType("B");
        addGeneralization(typeA, typeB);
        addGeneralization(typeB, typeA);
        assertCompilationException(typeA);
    }

    @Test
    public void testVeryIndirectLoop()
    {
        // B
        // |
        // C
        // |
        // B
        // |
        // A
        CoreInstance typeA = newType("A");
        CoreInstance typeB = newType("B");
        CoreInstance typeC = newType("C");
        addGeneralization(typeA, typeB);
        addGeneralization(typeB, typeC);
        addGeneralization(typeC, typeB);
        assertCompilationException(typeA);
    }

    @Test
    public void testInvalidComplexGeneralization1() throws PureCompilationException
    {
        //    E
        //  /   \
        // C     D
        //  \  / |
        //   B  /
        //   | /
        //   A
        // (but A->D precedes A->B)
        //
        // This is invalid because D must precede B in the linearization since it
        // precedes it in A's generalizations, but B must precede D since D is a
        // generalization of it.
        CoreInstance typeA = newType("A");
        CoreInstance typeB = newType("B");
        CoreInstance typeC = newType("C");
        CoreInstance typeD = newType("D");
        CoreInstance typeE = newType("E");
        addGeneralization(typeA, typeD);
        addGeneralization(typeA, typeB);
        addGeneralization(typeB, typeC);
        addGeneralization(typeB, typeD);
        addGeneralization(typeC, typeE);
        addGeneralization(typeD, typeE);
        assertCompilationException(typeA);
    }

    @Test
    public void testInvalidComplexGeneralization2() throws PureCompilationException
    {
        //   ________
        //  /        \
        // |    F     |
        // |  /   \   |
        // |_D     E  |
        //   \  /  | /
        //    B    C
        //    | /
        //    A
        //
        // This is invalid because D must precede E in the linearization since it
        // precedes it in B's generalizations, but E must precede D in the
        // linearization since it precedes it in C's generalizations.
        CoreInstance typeA = newType("A");
        CoreInstance typeB = newType("B");
        CoreInstance typeC = newType("C");
        CoreInstance typeD = newType("D");
        CoreInstance typeE = newType("E");
        CoreInstance typeF = newType("F");
        addGeneralization(typeA, typeB);
        addGeneralization(typeA, typeC);
        addGeneralization(typeB, typeD);
        addGeneralization(typeB, typeE);
        addGeneralization(typeC, typeE);
        addGeneralization(typeC, typeD);
        addGeneralization(typeD, typeF);
        addGeneralization(typeE, typeF);
        assertCompilationException(typeA);
    }

    private void assertTypeLinearization(ImmutableList<CoreInstance> expected, CoreInstance type)
    {
        Assert.assertEquals(expected, C3Linearization.getTypeGeneralizationLinearization(type, this.processorSupport));
    }

    private void assertCompilationException(CoreInstance type)
    {
        try
        {
            ListIterable<CoreInstance> result = C3Linearization.getTypeGeneralizationLinearization(type, this.processorSupport);
            Assert.fail("Expected compilation error for generalization linearization of " + type + ", got: " + result.makeString("[", ", ", "]"));
        }
        catch (PureCompilationException e)
        {
            Assert.assertTrue(e.getInfo().startsWith("Inconsistent generalization hierarchy for " + type));
        }
    }

    private CoreInstance newType(String name)
    {
        return newType(name, Lists.immutable.<String>empty());
    }

    private CoreInstance newType(String name, ListIterable<String> typeParameters)
    {
        return newType(name, typeParameters, Lists.immutable.<String>empty());
    }

    private CoreInstance newType(String name, ListIterable<String> typeParameters, ListIterable<String> multiplicityParameters)
    {
        CoreInstance type = new StubCoreInstance(name);
        for (String typeParamName : typeParameters)
        {
            type.addKeyValue(M3PropertyPaths.typeParameters, newTypeParameter(typeParamName));
        }
        for (String multParamName : multiplicityParameters)
        {
            type.addKeyValue(M3PropertyPaths.multiplicityParameters, newString(multParamName));
        }
        return type;
    }

    private CoreInstance newGenericType(String typeParameter)
    {
        CoreInstance genericType = new StubCoreInstance("GenericType " + typeParameter);
        genericType.addKeyValue(M3PropertyPaths.typeParameter, newTypeParameter(typeParameter));
        return genericType;
    }

    private CoreInstance newGenericType(CoreInstance type)
    {
        return newGenericType(type, Lists.immutable.<CoreInstance>empty());
    }

    private CoreInstance newGenericType(CoreInstance type, ListIterable<CoreInstance> typeArguments)
    {
        return newGenericType(type, typeArguments, Lists.immutable.<CoreInstance>empty());
    }

    private CoreInstance newGenericType(CoreInstance type, ListIterable<CoreInstance> typeArguments, ListIterable<CoreInstance> multiplicityArguments)
    {
        CoreInstance genericType = new StubCoreInstance(type.getName() + " GenericType");
        genericType.addKeyValue(M3PropertyPaths.rawType, type);
        if (typeArguments.notEmpty())
        {
            genericType.setKeyValues(M3PropertyPaths.typeArguments, typeArguments);
        }
        if (multiplicityArguments.notEmpty())
        {
            genericType.setKeyValues(M3PropertyPaths.multiplicityArguments, multiplicityArguments);
        }
        return genericType;
    }

    private CoreInstance newTypeParameter(String name)
    {
        CoreInstance typeParameter = new StubCoreInstance("TypeParameter " + name);
        typeParameter.addKeyValue(Lists.immutable.with(M3Properties.name), newString(name));
        return typeParameter;
    }

    private CoreInstance newString(String value)
    {
        return new StubCoreInstance(value);
    }

    private void addGeneralization(CoreInstance spec, CoreInstance genl)
    {
        addGeneralization(spec, genl, newGenericType(genl));
    }

    private void addGeneralization(CoreInstance spec, CoreInstance genl, CoreInstance genlGenericType)
    {
        CoreInstance generalization = new StubCoreInstance(spec.getName() + "->" + genl.getName());
        generalization.addKeyValue(M3PropertyPaths.specific, spec);
        generalization.addKeyValue(M3PropertyPaths.general, genlGenericType);

        spec.addKeyValue(M3PropertyPaths.generalizations, generalization);
        genl.addKeyValue(M3PropertyPaths.specializations, generalization);
    }

    private static class StubCoreInstance extends AbstractCoreInstance
    {
        private final String name;
        private final MutableListMultimap<String, CoreInstance> properties = FastListMultimap.newMultimap();

        private StubCoreInstance(String name)
        {
            this.name = name;
        }

        @Override
        public ModelRepository getRepository()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getSyntheticId()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getName()
        {
            return this.name;
        }

        @Override
        public void setName(String name)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public CoreInstance getClassifier()
        {
            return new StubCoreInstance("Type");
        }

        @Override
        public void setClassifier(CoreInstance classifier)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public SourceInformation getSourceInformation()
        {
            return null;
        }

        @Override
        public void setSourceInformation(SourceInformation sourceInformation)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isPersistent()
        {
            return true;
        }

        @Override
        public void addKeyWithEmptyList(ListIterable<String> key)
        {
            this.properties.removeAll(key.getLast());
        }

        @Override
        public void modifyValueForToManyMetaProperty(String key, int offset, CoreInstance value)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeProperty(String keyName)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public CoreInstance getKeyByName(String name)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public CoreInstance getValueForMetaPropertyToOne(String propertyName)
        {
            return getValueForMetaPropertyToMany(propertyName).getFirst();
        }

        @Override
        public CoreInstance getValueForMetaPropertyToOne(CoreInstance property)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public MutableList<CoreInstance> getValueForMetaPropertyToMany(String keyName)
        {
            return this.properties.get(keyName);
        }

        @Override
        public MutableList<CoreInstance> getValueForMetaPropertyToMany(CoreInstance key)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isValueDefinedForKey(String keyName)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeValueForMetaPropertyToMany(String keyName, CoreInstance coreInstance)
        {
            this.properties.remove(keyName, coreInstance);
        }

        @Override
        public RichIterable<String> getKeys()
        {
            return this.properties.keysView();
        }

        @Override
        public ListIterable<String> getRealKeyByName(String name)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void validate(MutableSet<CoreInstance> doneList) throws PureCompilationException
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public CoreInstance copy()
        {
            throw new UnsupportedOperationException("Not Supported");
        }

        @Override
        public void printFull(Appendable appendable, String tab)
        {
            try
            {
                appendable.append(this.name);
                appendable.append("\n");
                for (String key : this.properties.keysView())
                {
                    appendable.append(tab);
                    appendable.append(key);
                    appendable.append(": ");
                    this.properties.get(key).collect(GET_NAME).appendString(appendable, "[", ", ", "]\n");
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void print(Appendable appendable, String tab)
        {
            printFull(appendable, tab);
        }

        @Override
        public void print(Appendable appendable, String tab, int max)
        {
            print(appendable, tab);
        }

        @Override
        public void printWithoutDebug(Appendable appendable, String tab)
        {
            print(appendable, tab);
        }

        @Override
        public void printWithoutDebug(Appendable appendable, String tab, int max)
        {
            print(appendable, tab);
        }

        @Override
        public void setKeyValues(ListIterable<String> key, ListIterable<? extends CoreInstance> value)
        {
            this.properties.replaceValues(key.getLast(), value);
        }

        @Override
        public void addKeyValue(ListIterable<String> key, CoreInstance value)
        {
            this.properties.put(key.getLast(), value);
        }

        @Override
        public <K> CoreInstance getValueInValueForMetaPropertyToManyByIDIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public <K> ListIterable<CoreInstance> getValueInValueForMetaPropertyToManyByIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void commit(ModelRepositoryTransaction transaction)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void rollback(ModelRepositoryTransaction transaction)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addCompileState(CompileState state)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeCompileState(CompileState state)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasCompileState(CompileState state)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompileStateSet getCompileStates()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setCompileStatesFrom(CompileStateSet states)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString()
        {
            return getName();
        }
    }
}
