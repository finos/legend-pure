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

package org.finos.legend.pure.m3.serialization.runtime.pattern;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.TaggedValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.ConcreteFunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.Type;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpressionAccessor;
import org.finos.legend.pure.m3.navigation.Instance;
import org.finos.legend.pure.m3.navigation.M3Paths;
import org.finos.legend.pure.m3.navigation.PackageableElement.PackageableElement;
import org.finos.legend.pure.m3.navigation.PrimitiveUtilities;
import org.finos.legend.pure.m3.navigation.ProcessorSupport;
import org.finos.legend.pure.m3.navigation.importstub.ImportStub;
import org.finos.legend.pure.m3.navigation.profile.Profile;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.transaction.framework.Transaction;
import org.finos.legend.pure.m4.transaction.framework.TransactionManager;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class URLPatternLibrary
{
    static final Comparator<PurePattern> URLPatternComparator = URLPatternLibrary::comparePatterns;

    public static final Pattern PATTERN_PARSING_PATTERN = Pattern.compile("([^{]+)|\\{([a-zA-Z_0-9]+)(:(([^}]+)))?\\}");

    private State mainState = new State();
    private final URLPatternLibraryTransactionManager transactionManager = new URLPatternLibraryTransactionManager();

    public void possiblyRegister(CoreInstance instance, ProcessorSupport processorSupport)
    {
        // Check that this is a ConcreteFunctionDefinitions: only they can have service URLs
        if (!(instance instanceof ConcreteFunctionDefinition))
        {
            return;
        }

        // Check that we can find the profile and tag for defining service URLs
        CoreInstance serviceProfile = processorSupport.package_getByUserPath("meta::pure::service::service");
        if (serviceProfile == null)
        {
            return;
        }
        CoreInstance urlTag = Profile.findTag(serviceProfile, "url");
        if (urlTag == null)
        {
            return;
        }

        CoreInstance stringType = processorSupport.package_getByUserPath(M3Paths.String);
        FunctionType functionType = (FunctionType) processorSupport.function_getFunctionType(instance);

        for (TaggedValue taggedValue : ((ConcreteFunctionDefinition<?>) instance)._taggedValues())
        {
            if (urlTag == ImportStub.withImportStubByPass(taggedValue._tagCoreInstance(), processorSupport))
            {
                // validate the function return
                GenericType returnGenericType = functionType._returnType();
                Type returnType = (Type) ImportStub.withImportStubByPass(returnGenericType._rawTypeCoreInstance(), processorSupport);
                if (!org.finos.legend.pure.m3.navigation.type.Type.subTypeOf(returnType, processorSupport.package_getByUserPath(M3Paths.ServiceResult), processorSupport) && returnType != stringType)
                {
                    throw new PureCompilationException(returnGenericType.getSourceInformation(), "Return type issue. A service function has to return a 'String' or a subtype of 'ServiceResult'.");
                }
                Multiplicity returnMultiplicity = ((FunctionType) processorSupport.function_getFunctionType(instance))._returnMultiplicity();
                if (!org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isToOne(returnMultiplicity, true))
                {
                    throw new PureCompilationException(returnGenericType.getSourceInformation(), "Return multiplicity issue. A service function has to return one ([1]) element.");
                }

                register((ConcreteFunctionDefinition<?>) instance, functionType, taggedValue, processorSupport, taggedValue._value());
            }
        }
    }

    public void register(ConcreteFunctionDefinition<?> function, FunctionType functionType, CoreInstance taggedValue, ProcessorSupport processorSupport, String pattern)
    {
        Matcher matcher = PATTERN_PARSING_PATTERN.matcher(pattern);
        StringBuilder builder = new StringBuilder(pattern.length() + 8);
        MutableList<String> urlArguments = Lists.mutable.empty();
        String first = null;

        while (matcher.find())
        {
            String group1 = matcher.group(1);
            if (group1 != null)
            {
                if ((first == null) && !group1.isEmpty())
                {
                    first = group1;
                }
                builder.append(group1);
            }
            else
            {
                String group2 = matcher.group(2);
                urlArguments.add(group2);
                builder.append("(?<").append(group2);

                String group4 = matcher.group(4);
                if (group4 == null)
                {
                    builder.append(">.+)");
                }
                else
                {
                    try
                    {
                        Pattern.compile(group4);
                    }
                    catch (PatternSyntaxException e)
                    {
                        throw new PureCompilationException(new SourceInformation(function.getSourceInformation().getSourceId(), taggedValue.getSourceInformation().getLine(), taggedValue.getSourceInformation().getColumn() + 1 + matcher.start(4) + e.getIndex(), -1, -1), "Error in the user provided regexp: " + group4, e);
                    }
                    builder.append('>').append(group4).append(")");
                }
            }
        }
        if (first == null)
        {
            throw new PureCompilationException(taggedValue.getSourceInformation(), "Error in the user provided regexp: " + pattern);
        }

        SetIterable<CoreInstance> supportedTypes = PrimitiveUtilities.getPrimitiveTypes(processorSupport).toSet();

        //Validate Arguments
        for (VariableExpression var : functionType._parameters())
        {
            CoreInstance type = ImportStub.withImportStubByPass(var._genericType()._rawTypeCoreInstance(), processorSupport);
            final String name = var._name();
            Multiplicity returnMultiplicity = var._multiplicity();

            String urlArg = urlArguments.contains(name) ? urlArguments.get(urlArguments.indexOf(name)) : null;
            if (!(supportedTypes.contains(type) || Instance.instanceOf(type, M3Paths.Enumeration, processorSupport)))
            {
                throw new PureCompilationException(var._genericType().getSourceInformation(), "Parameter type issue. All parameters must be a primitive type or enum");
            }
            if (urlArg != null && !org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isToOne(returnMultiplicity, true))
            {
                throw new PureCompilationException(var._genericType().getSourceInformation(), "Parameter multiplicity issue. A service function parameter specified in the URI has to be String[1]");
            }
            if (urlArg == null && org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isToOne(returnMultiplicity, true))
            {
                throw new PureCompilationException(var._genericType().getSourceInformation(), "Parameter multiplicity issue. All parameters with multiplicity [1] must be a part of the service url");
            }
            if (urlArg == null && !(org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isZeroToOne(returnMultiplicity) || org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity.isZeroToMany(returnMultiplicity)))
            {
                throw new PureCompilationException(var._genericType().getSourceInformation(), "Parameter multiplicity issue. Parameters not part of the service url must have multiplicity [0..1] or [*]");
            }
        }

        // Validate key
        if (!first.startsWith("/"))
        {
            throw new PureCompilationException(taggedValue.getSourceInformation(), "The URL needs to start with '/' (" + first + ")");
        }
        if (urlArguments.notEmpty() && !first.endsWith("/"))
        {
            throw new PureCompilationException(taggedValue.getSourceInformation(), "The first part of the URL (" + first + ") needs to end with '/'");
        }

        // Validate
        ListIterable<String> params = ((FunctionType) processorSupport.function_getFunctionType(function))._parameters().collect(VariableExpressionAccessor::_name, Lists.mutable.empty());
        if (!params.containsAll(urlArguments))
        {
            throw new PureCompilationException(function.getSourceInformation(), "URL pattern names mismatch. Found " + urlArguments + " in the pattern where the function has " + params);
        }

        PurePattern newPattern = new PurePattern(first, pattern, Pattern.compile(builder.toString()), function, urlArguments);
        if (!getState().register(getFunctionId(function), newPattern))
        {
            throw new PureCompilationException(taggedValue.getSourceInformation(), "A function has already been registered with the key '" + first + "' (" + function.getName() + ")");
        }
    }

    public Pair<CoreInstance, Map<String, String[]>> tryExecution(String url, ProcessorSupport processorSupport, Map<String, String[]> params)
    {
        return getState().tryExecution(url, processorSupport, params);
    }

    public void deregister(ConcreteFunctionDefinition<?> functionDefinition)
    {
        String functionId = getFunctionId(functionDefinition);
        getState().deregister(functionId);
    }

    public ListIterable<PurePattern> getPatterns()
    {
        return getState().patterns.asUnmodifiable();
    }

    public void clear()
    {
        this.transactionManager.clear();
        getState().clear();
    }

    public URLPatternLibraryTransaction newTransaction(boolean committable)
    {
        return this.transactionManager.newTransaction(committable);
    }

    private State getState()
    {
        URLPatternLibraryTransaction transaction = this.transactionManager.getThreadLocalTransaction();
        return (transaction == null) ? this.mainState : transaction.state;
    }

    private String getFunctionId(ConcreteFunctionDefinition<?> functionDefinition)
    {
        return PackageableElement.writeUserPathForPackageableElement(new StringBuilder(), functionDefinition._package())
                .append(PackageableElement.DEFAULT_PATH_SEPARATOR)
                .append(functionDefinition.getName())
                .toString();
    }

    public class URLPatternLibraryTransaction extends Transaction
    {
        private final State state;

        private URLPatternLibraryTransaction(URLPatternLibraryTransactionManager transactionManager, boolean committable)
        {
            super(transactionManager, committable);
            this.state = URLPatternLibrary.this.mainState.copy();
        }

        @Override
        protected void doCommit()
        {
            URLPatternLibrary.this.mainState = this.state;
        }

        @Override
        protected void doRollback()
        {
            // Nothing to do
        }
    }

    private class URLPatternLibraryTransactionManager extends TransactionManager<URLPatternLibraryTransaction>
    {
        @Override
        protected URLPatternLibraryTransaction createTransaction(boolean committable)
        {
            return new URLPatternLibraryTransaction(this, committable);
        }
    }

    private static class State
    {
        private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

        private final MutableList<PurePattern> patterns = Lists.mutable.empty();
        private final MutableMap<String, PurePattern> patternsByKeys = Maps.mutable.empty();
        private final MutableMap<String, PurePattern> patternByFuncId = Maps.mutable.empty();

        private State()
        {
        }

        boolean register(String funcId, PurePattern pattern)
        {
            Lock lock = this.readWriteLock.writeLock();
            lock.lock();
            try
            {
                PurePattern old = this.patternsByKeys.put(pattern.getKey(), pattern);
                if (old != null)
                {
                    // If there was already a pattern with this key, revert and return false
                    this.patternsByKeys.put(old.getKey(), old);
                    return false;
                }
                this.patternByFuncId.put(funcId, pattern);
                this.patterns.add(pattern);
                this.patterns.sortThis(URLPatternComparator);
                return true;
            }
            finally
            {
                lock.unlock();
            }
        }

        void deregister(String funcId)
        {
            Lock lock = this.readWriteLock.writeLock();
            lock.lock();
            try
            {
                PurePattern pattern = this.patternByFuncId.get(funcId);
                if (pattern != null)
                {
                    this.patterns.remove(pattern);
                    this.patternsByKeys.remove(pattern.getKey());
                    this.patternByFuncId.remove(funcId);
                }
            }
            finally
            {
                lock.unlock();
            }
        }

        void clear()
        {
            Lock lock = this.readWriteLock.writeLock();
            lock.lock();
            try
            {
                this.patterns.clear();
                this.patternsByKeys.clear();
                this.patternByFuncId.clear();
            }
            finally
            {
                lock.unlock();
            }
        }

        Pair<CoreInstance, Map<String, String[]>> tryExecution(String url, ProcessorSupport processorSupport, Map<String, String[]> params)
        {
            Lock lock = this.readWriteLock.readLock();
            lock.lock();
            try
            {
                for (PurePattern pattern : this.patterns)
                {
                    Pair<CoreInstance, Map<String, String[]>> requestParams = pattern.execute(url, processorSupport, params);
                    if (requestParams != null)
                    {
                        return requestParams;
                    }
                }
                return null;
            }
            finally
            {
                lock.unlock();
            }
        }

        State copy()
        {
            Lock lock = this.readWriteLock.readLock();
            lock.lock();
            try
            {
                State copy = new State();
                copy.patterns.addAll(this.patterns);
                copy.patternsByKeys.putAll(this.patternsByKeys);
                copy.patternByFuncId.putAll(this.patternByFuncId);
                return copy;
            }
            finally
            {
                lock.unlock();
            }
        }
    }

    static int comparePatterns(PurePattern pattern1, PurePattern pattern2)
    {
        String str1 = pattern1.getSrcPattern();
        String str2 = pattern2.getSrcPattern();
        return (str2.length() + (str2.contains("{") ? 0 : 10000)) - (str1.length() + (str1.contains("{") ? 0 : 10000));
    }
}
