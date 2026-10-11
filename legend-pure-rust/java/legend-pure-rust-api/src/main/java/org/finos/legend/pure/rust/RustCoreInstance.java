// Copyright 2026 Goldman Sachs
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

package org.finos.legend.pure.rust;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.M3Properties;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation._package._Package;
import org.finos.legend.pure.m4.ModelRepository;
import org.finos.legend.pure.m4.coreinstance.AbstractCoreInstance;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileState;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;
import org.finos.legend.pure.m4.coreinstance.indexing.IndexSpecification;
import org.finos.legend.pure.m4.coreinstance.primitive.StringCoreInstance;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

/**
 * A Rust heap object seen from Java as a read-only {@link CoreInstance},
 * read through the {@link RustHeap} of the evaluator that owns it.
 * One proxy exists per Rust handle; equality is identity. The classifier
 * is the Java graph node for the Rust classifier path.
 */
public final class RustCoreInstance extends AbstractCoreInstance implements Closeable
{
    private static final String ANONYMOUS_PREFIX = "@_";
    private static final int ANONYMOUS_DIGITS = Long.toUnsignedString(-1L, 32).length();

    private final RustHeap owner;
    private final long handle;
    private final String classifierPath;
    private CoreInstance classifier;
    private String name;

    public RustCoreInstance(RustHeap owner, long handle, String classifierPath)
    {
        this.owner = owner;
        this.handle = handle;
        this.classifierPath = classifierPath;
    }

    long handle()
    {
        return this.handle;
    }

    /**
     * The Rust classifier path ({@code my::pkg::Person}).
     *
     * @return the classifier path as the Rust side spells it
     */
    public String getClassifierPath()
    {
        return this.classifierPath;
    }

    @Override
    public CoreInstance getClassifier()
    {
        if (this.classifier == null)
        {
            CoreInstance resolved = _Package.getByUserPath(this.classifierPath, this.owner.getProcessorSupport());
            if (resolved == null)
            {
                throw PureRustEvaluationException.unsupported("classifier " + this.classifierPath + " is not in the Java graph");
            }
            this.classifier = resolved;
        }
        return this.classifier;
    }

    /**
     * As a Java instance's: a model element (an instance of a subtype of
     * {@code ModelElement}) is named by its {@code name}; any other instance
     * has an anonymous name, in the repository's anonymous form so
     * {@code ModelRepository.isAnonymousInstanceName} recognises it, unique
     * per Rust object. Never null: {@code id()} and every printer read it.
     */
    @Override
    public String getName()
    {
        if (this.name == null)
        {
            this.name = isModelElement() ? modelElementName() : anonymousName(this.handle);
        }
        return this.name;
    }

    private boolean isModelElement()
    {
        ProcessorSupport processorSupport = this.owner.getProcessorSupport();
        CoreInstance modelElement = processorSupport.package_getByUserPath(M3Paths.ModelElement);
        return (modelElement != null) && processorSupport.type_subTypeOf(getClassifier(), modelElement);
    }

    private String modelElementName()
    {
        ListIterable<? extends CoreInstance> values = getValueForMetaPropertyToMany(M3Properties.name);
        return ((values.size() == 1) && (values.get(0) instanceof StringCoreInstance)) ? values.get(0).getName() : anonymousName(this.handle);
    }

    private static String anonymousName(long handle)
    {
        String digits = Long.toUnsignedString(handle, 32);
        StringBuilder name = new StringBuilder(ANONYMOUS_PREFIX);
        for (int i = digits.length(); i < ANONYMOUS_DIGITS; i++)
        {
            name.append('0');
        }
        return name.append(digits).toString();
    }

    @Override
    public ModelRepository getRepository()
    {
        return null;
    }

    @Override
    public int getSyntheticId()
    {
        return -1;
    }

    @Override
    public SourceInformation getSourceInformation()
    {
        return null;
    }

    @Override
    public boolean isPersistent()
    {
        return false;
    }

    @Override
    public RichIterable<String> getKeys()
    {
        return this.owner.propertyNames(this.handle);
    }

    @Override
    public ListIterable<String> getRealKeyByName(String name)
    {
        return null;
    }

    @Override
    public CoreInstance getKeyByName(String name)
    {
        return null;
    }

    @Override
    public ListIterable<? extends CoreInstance> getValueForMetaPropertyToMany(String keyName)
    {
        return this.owner.propertyValues(this.handle, keyName);
    }

    @Override
    public CoreInstance getValueForMetaPropertyToOne(String propertyName)
    {
        ListIterable<? extends CoreInstance> values = getValueForMetaPropertyToMany(propertyName);
        return values.isEmpty() ? null : values.get(0);
    }

    @Override
    public boolean isValueDefinedForKey(String keyName)
    {
        return getValueForMetaPropertyToMany(keyName).notEmpty();
    }

    @Override
    public <K> CoreInstance getValueInValueForMetaPropertyToManyByIDIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return getValueForMetaPropertyToMany(keyName).detect(v -> Objects.equals(keyInIndex, indexSpec.getIndexKey(v)));
    }

    @Override
    public <K> ListIterable<? extends CoreInstance> getValueInValueForMetaPropertyToManyByIndex(String keyName, IndexSpecification<K> indexSpec, K keyInIndex)
    {
        return getValueForMetaPropertyToMany(keyName).select(v -> Objects.equals(keyInIndex, indexSpec.getIndexKey(v)));
    }

    @Override
    public void print(Appendable appendable, String tab, int max)
    {
        try
        {
            appendable.append(tab).append(ModelRepository.possiblyReplaceAnonymousId(getName())).append(" instance ").append(this.classifierPath);
            if (max > 0)
            {
                for (String key : getKeys())
                {
                    appendable.append('\n').append(tab).append("    ").append(key).append(':');
                    for (CoreInstance value : getValueForMetaPropertyToMany(key))
                    {
                        appendable.append(' ').append(String.valueOf(value.getName()));
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        print(builder, "", 0);
        return builder.toString();
    }

    @Override
    public CoreInstance copy()
    {
        throw new UnsupportedOperationException("Rust-backed instances cannot be copied from Java");
    }

    @Override
    public void setName(String name)
    {
        throw readOnly();
    }

    @Override
    public void setClassifier(CoreInstance classifier)
    {
        throw readOnly();
    }

    @Override
    public void setSourceInformation(SourceInformation sourceInformation)
    {
        throw readOnly();
    }

    @Override
    public void addKeyWithEmptyList(ListIterable<String> key)
    {
        throw readOnly();
    }

    @Override
    public void modifyValueForToManyMetaProperty(String key, int offset, CoreInstance value)
    {
        throw readOnly();
    }

    @Override
    public void removeProperty(String propertyNameKey)
    {
        throw readOnly();
    }

    @Override
    public void removeValueForMetaPropertyToMany(String keyName, CoreInstance coreInstance)
    {
        throw readOnly();
    }

    @Override
    public void setKeyValues(ListIterable<String> key, ListIterable<? extends CoreInstance> value)
    {
        throw readOnly();
    }

    @Override
    public void addKeyValue(ListIterable<String> key, CoreInstance value)
    {
        throw readOnly();
    }

    @Override
    public void commit(ModelRepositoryTransaction transaction)
    {
        throw readOnly();
    }

    @Override
    public void addCompileState(CompileState state)
    {
    }

    @Override
    public void removeCompileState(CompileState state)
    {
    }

    @Override
    public boolean hasCompileState(CompileState state)
    {
        return CompileStateSet.PROCESSED_VALIDATED.contains(state);
    }

    @Override
    public CompileStateSet getCompileStates()
    {
        return CompileStateSet.PROCESSED_VALIDATED;
    }

    @Override
    public void setCompileStatesFrom(CompileStateSet states)
    {
    }

    @Override
    public void validate(MutableSet<CoreInstance> doneList)
    {
    }

    @Override
    public void close()
    {
        this.owner.release(this.handle);
    }

    private static UnsupportedOperationException readOnly()
    {
        return new UnsupportedOperationException("Rust-backed instances are read-only from Java");
    }
}
