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

package org.finos.legend.pure.m4;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;
import org.finos.legend.pure.m4.coreinstance.compileState.CompileStateSet;
import org.finos.legend.pure.m4.coreinstance.factory.CoreInstanceFactory;
import org.finos.legend.pure.m4.coreinstance.factory.MultipassCoreInstanceFactory;
import org.finos.legend.pure.m4.coreinstance.primitive.BinaryCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.BooleanCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.DateCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.DecimalCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.FloatCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.IntegerCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.PrimitiveCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StrictTimeCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.StringCoreInstance;
import org.finos.legend.pure.m4.coreinstance.primitive.date.DateFunctions;
import org.finos.legend.pure.m4.coreinstance.primitive.date.LatestDate;
import org.finos.legend.pure.m4.coreinstance.primitive.date.PureDate;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.PureStrictTime;
import org.finos.legend.pure.m4.coreinstance.primitive.strictTime.StrictTimeFunctions;
import org.finos.legend.pure.m4.coreinstance.simple.SimpleCoreInstance;
import org.finos.legend.pure.m4.coreinstance.simple.SimpleCoreInstanceFactory;
import org.finos.legend.pure.m4.exception.PureCompilationException;
import org.finos.legend.pure.m4.serialization.binary.BinaryRepositorySerializer;
import org.finos.legend.pure.m4.serialization.grammar.NameSpace;
import org.finos.legend.pure.m4.statelistener.M4StateListener;
import org.finos.legend.pure.m4.transaction.ModelRepositoryTransaction;
import org.finos.legend.pure.m4.transaction.TransactionObserver;
import org.finos.legend.pure.m4.transaction.VoidTransactionObserver;
import org.finos.legend.pure.m4.transaction.framework.TransactionManager;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

public class ModelRepository
{
    public static final String BOOLEAN_TYPE_NAME = "Boolean";
    public static final String BINARY_TYPE_NAME = "Binary";
    public static final String BYTE_STREAM_TYPE_NAME = "ByteStream";
    public static final String DATE_TYPE_NAME = "Date";
    public static final String STRICT_DATE_TYPE_NAME = "StrictDate";
    public static final String DATETIME_TYPE_NAME = "DateTime";
    public static final String LATEST_DATE_TYPE_NAME = "LatestDate";
    public static final String STRICT_TIME_TYPE_NAME = "StrictTime";
    public static final String FLOAT_TYPE_NAME = "Float";
    public static final String DECIMAL_TYPE_NAME = "Decimal";
    public static final String INTEGER_TYPE_NAME = "Integer";
    public static final String STRING_TYPE_NAME = "String";
    public static final ImmutableSet<String> PRIMITIVE_TYPE_NAMES = Sets.immutable.with(BOOLEAN_TYPE_NAME, BYTE_STREAM_TYPE_NAME, DATE_TYPE_NAME, STRICT_DATE_TYPE_NAME, DATETIME_TYPE_NAME, LATEST_DATE_TYPE_NAME, STRICT_TIME_TYPE_NAME, FLOAT_TYPE_NAME, DECIMAL_TYPE_NAME, INTEGER_TYPE_NAME, STRING_TYPE_NAME, BINARY_TYPE_NAME);


    public static final String BOOLEAN_TRUE = "true";
    public static final String BOOLEAN_FALSE = "false";

    private static final String ANONYMOUS_NAME_PREFIX = "@_";
    private static final int ANONYMOUS_PADDING_LENGTH = Integer.toString(Integer.MAX_VALUE, 32).length();
    private static final int ANONYMOUS_PADDING_TOTAL_LENGTH = ANONYMOUS_NAME_PREFIX.length() + ANONYMOUS_PADDING_LENGTH;

    private final ConcurrentMutableMap<String, CoreInstance> topLevelMap = ConcurrentHashMap.newMap();
    private ImmutableSet<CoreInstance> exclusionSet = Sets.immutable.with();
    private final AtomicInteger idCounter = new AtomicInteger(0);
    private final AtomicInteger anonymousIdCounter = new AtomicInteger(0);

    private final ModelRepositoryTransactionManager transactionManager = new ModelRepositoryTransactionManager();

    private final AtomicReference<BooleanCoreInstance> cachedTrue = new AtomicReference<>();
    private final AtomicReference<BooleanCoreInstance> cachedFalse = new AtomicReference<>();
    private final int integerCacheMin = -1;
    private final int integerCacheMax = 1000;
    private final AtomicReferenceArray<IntegerCoreInstance> integerCache = new AtomicReferenceArray<>((this.integerCacheMax - this.integerCacheMin) + 1);
    private final ConcurrentMutableMap<String, StringCoreInstance> stringCache = ConcurrentHashMap.newMap();

    private final MultipassCoreInstanceFactory coreInstanceFactory;

    private TransactionObserver transactionObserver = VoidTransactionObserver.VOID_TRANSACTION_OBSERVER;

    public ModelRepository(MultipassCoreInstanceFactory factory)
    {
        this.coreInstanceFactory = factory;
    }

    public ModelRepository()
    {
        this(new SimpleCoreInstanceFactory());
    }

    public ModelRepository(int idCounter)
    {
        this();
        this.idCounter.set(idCounter);
    }

    public CoreInstance getTopLevel(String name)
    {
        ModelRepositoryTransaction transaction = getTransaction();
        return ((transaction != null) && transaction.isOpen()) ? transaction.getTopLevel(name) : this.topLevelMap.get(name);
    }

    public void addTopLevel(CoreInstance topLevel)
    {
        CoreInstance current = getOrAddTopLevel(topLevel);
        if (current != topLevel)
        {
            throw new RuntimeException("A top level element already exists with the name \"" + topLevel.getName() + "\"");
        }
    }

    public CoreInstance getOrAddTopLevel(CoreInstance topLevel)
    {
        ModelRepositoryTransaction transaction = getTransaction();
        return ((transaction != null) && transaction.isOpen()) ? transaction.getOrAddTopLevel(topLevel) : this.topLevelMap.getIfAbsentPut(topLevel.getName(), topLevel);
    }

    public RichIterable<CoreInstance> getTopLevels()
    {
        ModelRepositoryTransaction transaction = getTransaction();
        return ((transaction != null) && transaction.isOpen()) ? transaction.getTopLevels() : this.topLevelMap.valuesView();
    }

    public int getIdCounter()
    {
        return this.idCounter.get();
    }

    public int getAnonymousIdCounter()
    {
        return this.anonymousIdCounter.get();
    }

    public void clear()
    {
        this.transactionManager.clear();
        this.topLevelMap.clear();
        this.exclusionSet = Sets.immutable.empty();
        this.idCounter.set(0);
        this.anonymousIdCounter.set(0);
        clearCaches();
    }

    private void clearCaches()
    {
        this.cachedTrue.set(null);
        this.cachedFalse.set(null);
        this.stringCache.clear();
        for (int i = 0, size = this.integerCache.length(); i < size; i++)
        {
            this.integerCache.set(i, null);
        }
    }

    public CoreInstance getOrCreateTopLevel(String classifierName, SourceInformation sourceInformation)
    {
        // Classifier
        CoreInstance classifierType = getTopLevel(classifierName);
        if (classifierType == null)
        {
            CoreInstance newClassifierType = this.coreInstanceFactory.createCoreInstance(classifierName, nextId(), sourceInformation, null, this, true);
            classifierType = getOrAddTopLevel(newClassifierType);
            if (newClassifierType == classifierType)
            {
                registerNewInstanceInTransaction(classifierType);
            }
        }
        // For top level primitives that were instantiated before being parsed...
        if (sourceInformation != null)
        {
            classifierType.setSourceInformation(sourceInformation);
        }
        return classifierType;
    }

    //todo: these are only used in serialization, do this differently
    public CoreInstance newCoreInstanceMultiPass(String name, String classifierPath, SourceInformation sourceInfo)
    {
        return newCoreInstanceMultiPass(name, classifierPath, null, sourceInfo, 0);
    }

    public CoreInstance newCoreInstanceMultiPass(String name, String classifierPath, String typeInfo, SourceInformation sourceInfo, int compileStateBitSet)
    {
        int id = nextId();
        if ((name == null) || name.isEmpty())
        {
            name = nextAnonymousInstanceName();
        }
        CoreInstance result = this.coreInstanceFactory.createCoreInstance(name, id, sourceInfo, classifierPath, typeInfo, this, true);
        initializeCoreInstanceMultiPass(result, compileStateBitSet);
        return result;
    }

    //todo: these are only used in serialization, do this differently
    public CoreInstance newCoreInstanceMultiPass(int id, String name, int classifierSyntheticId, SourceInformation sourceInformation, int compileStateBitSet)
    {
        CoreInstance result = this.coreInstanceFactory.createCoreInstance(name, id, sourceInformation, classifierSyntheticId, this, true);
        initializeCoreInstanceMultiPass(result, compileStateBitSet);
        return result;
    }

    //todo: these are only used in serialization, do this differently
    private void initializeCoreInstanceMultiPass(CoreInstance instance, int compileStateBitSet)
    {
        instance.setCompileStatesFrom(CompileStateSet.fromBitSet(CompileStateSet.removeExtraCompileStates(compileStateBitSet)));
        registerNewInstanceInTransaction(instance);
    }

    public CoreInstance newCoreInstance(String name, CoreInstance classifier, SourceInformation sourceInformation, boolean persistent)
    {
        if (classifier == null)
        {
            throw new RuntimeException("Please provide a classifier for the creation of the CoreInstance '" + name + "'");
        }
        if (classifier.equals(getTopLevel(BOOLEAN_TYPE_NAME)))
        {
            if (sourceInformation != null)
            {
                throw new IllegalArgumentException("Instances of " + BOOLEAN_TYPE_NAME + " may not have source information");
            }
            return newBooleanCoreInstance(name);
        }
        if (classifier.equals(getTopLevel(DATE_TYPE_NAME)))
        {
            if (sourceInformation != null)
            {
                throw new IllegalArgumentException("Instances of " + DATE_TYPE_NAME + " may not have source information");
            }
            return newDateCoreInstance(name);
        }
        if (classifier.equals(getTopLevel(STRICT_DATE_TYPE_NAME)))
        {
            if (sourceInformation != null)
            {
                throw new IllegalArgumentException("Instances of " + STRICT_DATE_TYPE_NAME + " may not have source information");
            }
            return newStrictDateCoreInstance(name);
        }
        if (classifier.equals(getTopLevel(DATETIME_TYPE_NAME)))
        {
            if (sourceInformation != null)
            {
                throw new IllegalArgumentException("Instances of " + DATETIME_TYPE_NAME + " may not have source information");
            }
            return newDateCoreInstance(name);
        }
        if (classifier.equals(getTopLevel(STRICT_TIME_TYPE_NAME)))
        {
            if (sourceInformation != null)
            {
                throw new IllegalArgumentException("Instances of " + STRICT_TIME_TYPE_NAME + " may not have source information");
            }
            return newStrictTimeCoreInstance(name);
        }
        if (classifier.equals(getTopLevel(FLOAT_TYPE_NAME)))
        {
            if (sourceInformation != null)
            {
                throw new IllegalArgumentException("Instances of " + FLOAT_TYPE_NAME + " may not have source information");
            }
            return newFloatCoreInstance(name);
        }
        if (classifier.equals(getTopLevel(DECIMAL_TYPE_NAME)))
        {
            if (sourceInformation != null)
            {
                throw new IllegalArgumentException("Instances of " + DECIMAL_TYPE_NAME + " may not have source information");
            }
            return newDecimalCoreInstance(name);
        }
        if (classifier.equals(getTopLevel(INTEGER_TYPE_NAME)))
        {
            if (sourceInformation != null)
            {
                throw new IllegalArgumentException("Instances of " + INTEGER_TYPE_NAME + " may not have source information");
            }
            return newIntegerCoreInstance(name);
        }
        if (classifier.equals(getTopLevel(STRING_TYPE_NAME)))
        {
            if (sourceInformation != null)
            {
                throw new IllegalArgumentException("Instances of " + STRING_TYPE_NAME + " may not have source information");
            }
            return persistent ? newStringCoreInstance_cached(name) : newStringCoreInstance(name);
        }
        CoreInstance result = createCoreInstance(name, sourceInformation, classifier, persistent);
        registerNewInstanceInTransaction(result);
        return result;
    }

    public CoreInstance newCoreInstance(String name, CoreInstance classifier, SourceInformation sourceInformation)
    {
        return newCoreInstance(name, classifier, sourceInformation, true);
    }

    public CoreInstance newCoreInstance(String name, CoreInstance classifier, SourceInformation sourceInformation, CoreInstanceFactory factory)
    {
        CoreInstance result = factory.createCoreInstance(name, this.nextId(), sourceInformation, classifier, this, true);
        this.registerNewInstanceInTransaction(result);
        return result;
    }

    public CoreInstance newEphemeralCoreInstance(String name, CoreInstance classifier, SourceInformation sourceInformation)
    {
        return newCoreInstance(name, classifier, sourceInformation, false);
    }

    public CoreInstance newAnonymousCoreInstance(SourceInformation sourceInformation, CoreInstance classifier, boolean persistent)
    {
        return newAnonymousCoreInstance(sourceInformation, classifier, persistent, this.coreInstanceFactory);
    }

    public CoreInstance newAnonymousCoreInstance(SourceInformation sourceInformation, CoreInstance classifier, boolean persistent, CoreInstanceFactory factory)
    {
        if (classifier == null)
        {
            throw new RuntimeException("Please provide a classifier for the creation of an Anonymous CoreInstance");
        }
        CoreInstance result = factory.createCoreInstance(nextAnonymousInstanceName(), this.nextId(), sourceInformation, classifier, this, persistent);
        registerNewInstanceInTransaction(result);
        return result;
    }

    public CoreInstance newAnonymousCoreInstance(SourceInformation sourceInformation, CoreInstance classifier)
    {
        return newAnonymousCoreInstance(sourceInformation, classifier, true);
    }

    public CoreInstance newEphemeralAnonymousCoreInstance(SourceInformation sourceInformation, CoreInstance classifier)
    {
        return newAnonymousCoreInstance(sourceInformation, classifier, false);
    }

    public CoreInstance newUnknownTypeCoreInstance(String name, SourceInformation sourceInformation, boolean persistent)
    {
        if (name == null)
        {
            name = nextAnonymousInstanceName();
        }
        CoreInstance instance = this.coreInstanceFactory.createCoreInstance(name, nextId(), sourceInformation, null, this, persistent);
        registerNewInstanceInTransaction(instance);
        return instance;
    }

    public CoreInstance newUnknownTypeCoreInstance(String name, SourceInformation sourceInformation)
    {
        return newUnknownTypeCoreInstance(name, sourceInformation, true);
    }

    private CoreInstance createCoreInstance(String name, SourceInformation sourceInformation, CoreInstance classifier, boolean persistent)
    {
        return this.coreInstanceFactory.createCoreInstance(name, nextId(), sourceInformation, classifier, this, persistent);
    }

    public CoreInstance newEphemeralUnknownTypeCoreInstance(String name, SourceInformation sourceInformation)
    {
        return newUnknownTypeCoreInstance(name, sourceInformation, false);
    }

    public BooleanCoreInstance newBooleanCoreInstance(String name)
    {
        return newBooleanCoreInstance(getBooleanValue(name));
    }

    // Should be used only by BinaryRepositorySerializer
    public BooleanCoreInstance newBooleanCoreInstance(String name, int internalSyntheticId)
    {
        return newBooleanCoreInstance(getBooleanValue(name), getOrCreateTopLevel(BOOLEAN_TYPE_NAME, null), internalSyntheticId);
    }

    public BooleanCoreInstance newBooleanCoreInstance(boolean value)
    {
        return newBooleanCoreInstance(value, getOrCreateTopLevel(BOOLEAN_TYPE_NAME, null), nextId());
    }

    private BooleanCoreInstance newBooleanCoreInstance(boolean value, CoreInstance classifier, int internalSyntheticId)
    {
        AtomicReference<BooleanCoreInstance> cache = value ? this.cachedTrue : this.cachedFalse;
        BooleanCoreInstance instance = cache.get();
        if (instance == null)
        {
            cache.compareAndSet(null, PrimitiveCoreInstance.newBooleanCoreInstance(value, classifier, internalSyntheticId));
            instance = cache.get();
        }
        return instance;
    }

    public static boolean getBooleanValue(String name)
    {
        if (BOOLEAN_TRUE.equalsIgnoreCase(name))
        {
            return true;
        }
        if (BOOLEAN_FALSE.equalsIgnoreCase(name))
        {
            return false;
        }
        throw new IllegalArgumentException("Invalid Pure Boolean: '" + name + "'");
    }

    public CoreInstance newLatestDateCoreInstance()
    {
        return newDateCoreInstance(LatestDate.instance, LATEST_DATE_TYPE_NAME);
    }

    public DateCoreInstance newDateCoreInstance(String name)
    {
        return newDateCoreInstance(getPureDate(name));
    }

    public DateCoreInstance newStrictDateCoreInstance(String name)
    {
        return newStrictDateCoreInstance(getPureDate(name));
    }

    public CoreInstance newDateTimeCoreInstance(String name)
    {
        return newDateTimeCoreInstance(getPureDate(name));
    }

    public CoreInstance newStrictTimeCoreInstance(String name)
    {
        return newStrictTimeCoreInstance(getPureStrictTime(name));
    }

    // Should be used only by BinaryRepositorySerializer
    public CoreInstance newDateCoreInstance(String name, int internalSyntheticId)
    {
        return newDateCoreInstance(getPureDate(name), getOrCreateTopLevel(DATE_TYPE_NAME, null), internalSyntheticId);
    }

    // Should be used only by BinaryRepositorySerializer
    public CoreInstance newStrictDateCoreInstance(String name, int internalSyntheticId)
    {
        return newDateCoreInstance(getPureDate(name), getOrCreateTopLevel(STRICT_DATE_TYPE_NAME, null), internalSyntheticId);
    }

    // Should be used only by BinaryRepositorySerializer
    public CoreInstance newDateTimeCoreInstance(String name, int internalSyntheticId)
    {
        return newDateCoreInstance(getPureDate(name), getOrCreateTopLevel(DATETIME_TYPE_NAME, null), internalSyntheticId);
    }

    // TODO [ByteStream] factory for core instance

    public DateCoreInstance newDateCoreInstance(PureDate value)
    {
        return newDateCoreInstance(value, DateFunctions.datePrimitiveType(value));
    }

    public DateCoreInstance newStrictDateCoreInstance(PureDate value)
    {
        if (!value.hasDay() || value.hasHour())
        {
            throw new PureCompilationException("StrictDate must be a calendar day, got: " + value);
        }
        return newDateCoreInstance(value, STRICT_DATE_TYPE_NAME);
    }

    public DateCoreInstance newDateTimeCoreInstance(PureDate value)
    {
        if (!value.hasHour())
        {
            throw new PureCompilationException("DateTime must include time information, got: " + value);
        }
        return newDateCoreInstance(value, DATETIME_TYPE_NAME);
    }

    public StrictTimeCoreInstance newStrictTimeCoreInstance(PureStrictTime value)
    {
        if (!value.hasMinute())
        {
            throw new PureCompilationException("StrictTime must include minute information, got: " + value);
        }
        return newStrictTimeCoreInstance(value, STRICT_TIME_TYPE_NAME);
    }

    private DateCoreInstance newDateCoreInstance(PureDate value, String typeName)
    {
        return newDateCoreInstance(value, getOrCreateTopLevel(typeName, null), nextId());
    }

    private DateCoreInstance newDateCoreInstance(PureDate value, CoreInstance classifier, int internalSyntheticId)
    {
        return PrimitiveCoreInstance.newDateCoreInstance(value, classifier, internalSyntheticId);
    }

    private StrictTimeCoreInstance newStrictTimeCoreInstance(PureStrictTime value, String typeName)
    {
        return newStrictTimeCoreInstance(value, getOrCreateTopLevel(typeName, null), nextId());
    }

    private StrictTimeCoreInstance newStrictTimeCoreInstance(PureStrictTime value, CoreInstance classifier, int internalSyntheticId)
    {
        return PrimitiveCoreInstance.newStrictTimeCoreInstance(value, classifier, internalSyntheticId);
    }

    private PureStrictTime getPureStrictTime(String name)
    {
        try
        {
            return StrictTimeFunctions.parsePureStrictTime(name);
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Invalid Pure StrictTime: '" + name + "'", e);
        }
    }

    private PureDate getPureDate(String name)
    {
        try
        {
            return DateFunctions.parsePureDate(name);
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("Invalid Pure Date: '" + name + "'", e);
        }
    }

    public FloatCoreInstance newFloatCoreInstance(String name)
    {
        return newFloatCoreInstance(new BigDecimal(name.endsWith("F") || name.endsWith("f") ? name.substring(0, name.length() - 1) : name));
    }

    // Should be used only by BinaryRepositorySerializer
    public FloatCoreInstance newFloatCoreInstance(String name, int internalSyntheticId)
    {
        return newFloatCoreInstance(new BigDecimal(name), getOrCreateTopLevel(FLOAT_TYPE_NAME, null), internalSyntheticId);
    }

    public FloatCoreInstance newFloatCoreInstance(BigDecimal value)
    {
        return newFloatCoreInstance(value, getOrCreateTopLevel(FLOAT_TYPE_NAME, null), nextId());
    }

    private FloatCoreInstance newFloatCoreInstance(BigDecimal value, CoreInstance classifier, int internalSyntheticId)
    {
        return PrimitiveCoreInstance.newFloatCoreInstance(value, classifier, internalSyntheticId);
    }

    public DecimalCoreInstance newDecimalCoreInstance(String name)
    {
        return newDecimalCoreInstance(new BigDecimal(name.endsWith("D") || name.endsWith("d") ? name.substring(0, name.length() - 1) : name));
    }

    // Should be used only by BinaryRepositorySerializer
    public DecimalCoreInstance newDecimalCoreInstance(String name, int internalSyntheticId)
    {
        return newDecimalCoreInstance(new BigDecimal(name), getOrCreateTopLevel(DECIMAL_TYPE_NAME, null), internalSyntheticId);
    }

    public DecimalCoreInstance newDecimalCoreInstance(BigDecimal value)
    {
        return newDecimalCoreInstance(value, getOrCreateTopLevel(DECIMAL_TYPE_NAME, null), nextId());
    }

    private DecimalCoreInstance newDecimalCoreInstance(BigDecimal value, CoreInstance classifier, int internalSyntheticId)
    {
        return PrimitiveCoreInstance.newDecimalCoreInstance(value, classifier, internalSyntheticId);
    }

    public IntegerCoreInstance newIntegerCoreInstance(String name)
    {
        return newIntegerCoreInstance(name, getOrCreateTopLevel(INTEGER_TYPE_NAME, null), nextId());
    }

    // Should be used only by BinaryRepositorySerializer
    public CoreInstance newIntegerCoreInstance(String name, int internalSyntheticId)
    {
        return newIntegerCoreInstance(name, getOrCreateTopLevel(INTEGER_TYPE_NAME, null), internalSyntheticId);
    }

    private IntegerCoreInstance newIntegerCoreInstance(String name, CoreInstance classifier, int internalSyntheticId)
    {
        try
        {
            int intValue = Integer.parseInt(name);
            return newIntegerCoreInstance(intValue, classifier, internalSyntheticId);
        }
        catch (NumberFormatException e)
        {
            try
            {
                Long longValue = Long.valueOf(name);
                return PrimitiveCoreInstance.newIntegerCoreInstance(longValue, classifier, nextId());
            }
            catch (NumberFormatException e1)
            {
                BigInteger bigIntValue = new BigInteger(name);
                return PrimitiveCoreInstance.newIntegerCoreInstance(bigIntValue, classifier, nextId());
            }
        }
    }

    public IntegerCoreInstance newIntegerCoreInstance(int value)
    {
        return newIntegerCoreInstance(value, getOrCreateTopLevel(INTEGER_TYPE_NAME, null));
    }

    private IntegerCoreInstance newIntegerCoreInstance(int value, CoreInstance classifier)
    {
        return newIntegerCoreInstance(value, classifier, nextId());
    }

    private IntegerCoreInstance newIntegerCoreInstance(int value, CoreInstance classifier, int internalSyntheticId)
    {
        if ((this.integerCacheMin <= value) && (value <= this.integerCacheMax))
        {
            int index = value - this.integerCacheMin;
            IntegerCoreInstance instance = this.integerCache.get(index);
            if (instance == null)
            {
                instance = PrimitiveCoreInstance.newIntegerCoreInstance(value, classifier, internalSyntheticId);
                if (!this.integerCache.compareAndSet(index, null, instance))
                {
                    instance = this.integerCache.get(index);
                }
            }
            return instance;
        }
        return PrimitiveCoreInstance.newIntegerCoreInstance(value, classifier, internalSyntheticId);
    }

    public IntegerCoreInstance newIntegerCoreInstance(long value)
    {
        return newIntegerCoreInstance(value, getOrCreateTopLevel(INTEGER_TYPE_NAME, null));
    }

    private IntegerCoreInstance newIntegerCoreInstance(long value, CoreInstance classifier)
    {
        // Convert to an Integer if we can
        if ((Integer.MIN_VALUE <= value) && (value <= Integer.MAX_VALUE))
        {
            return newIntegerCoreInstance((int)value, classifier);
        }
        return PrimitiveCoreInstance.newIntegerCoreInstance(value, classifier, nextId());
    }

    public IntegerCoreInstance newIntegerCoreInstance(BigInteger value)
    {
        return newIntegerCoreInstance(value, getOrCreateTopLevel(INTEGER_TYPE_NAME, null));
    }

    private IntegerCoreInstance newIntegerCoreInstance(BigInteger value, CoreInstance classifier)
    {
        // Convert to an Integer if we can
        int intValue = value.intValue();
        if (value.equals(BigInteger.valueOf(intValue)))
        {
            return newIntegerCoreInstance(intValue, classifier);
        }
        // If we can't convert to an Integer, try to convert to a Long
        long longValue = value.longValue();
        if (value.equals(BigInteger.valueOf(longValue)))
        {
            return newIntegerCoreInstance(longValue, classifier);
        }
        return PrimitiveCoreInstance.newIntegerCoreInstance(value, classifier, nextId());
    }

    public StringCoreInstance newStringCoreInstance(String value)
    {
        StringCoreInstance instance = this.stringCache.get(value);
        return (instance == null) ? newStringInstance(value) : instance;
    }

    public StringCoreInstance newStringCoreInstance_cached(String value)
    {
        return this.stringCache.getIfAbsentPutWithKey(value, this::newStringInstance);
    }

    // Should be used only by BinaryRepositorySerializer
    public StringCoreInstance newStringCoreInstance_cached(String value, int internalSyntheticId)
    {
        return this.stringCache.getIfAbsentPut(value, () -> newStringInstance(value, internalSyntheticId));
    }

    private StringCoreInstance newStringInstance(String value)
    {
        return newStringInstance(value, getOrCreateTopLevel(STRING_TYPE_NAME, null));
    }

    private StringCoreInstance newStringInstance(String value, CoreInstance classifier)
    {
        return newStringInstance(value, classifier, nextId());
    }

    private StringCoreInstance newStringInstance(String value, int internalSyntheticId)
    {
        return newStringInstance(value, getOrCreateTopLevel(STRING_TYPE_NAME, null), internalSyntheticId);
    }

    private StringCoreInstance newStringInstance(String value, CoreInstance classifier, int internalSyntheticId)
    {
        return PrimitiveCoreInstance.newStringCoreInstance(value, classifier, internalSyntheticId);
    }

    public BinaryCoreInstance newBinaryCoreInstance(byte[] value)
    {
        return newBinaryInstance(value);
    }

    private BinaryCoreInstance newBinaryInstance(byte[] value)
    {
        return newBinaryInstance(value, getOrCreateTopLevel(BINARY_TYPE_NAME, null));
    }

    private BinaryCoreInstance newBinaryInstance(byte[] value, CoreInstance classifier)
    {
        return newBinaryInstance(value, classifier, nextId());
    }

    private BinaryCoreInstance newBinaryInstance(byte[] value, CoreInstance classifier, int internalSyntheticId)
    {
        return PrimitiveCoreInstance.newBinaryCoreInstance(value, classifier, internalSyntheticId);
    }

    private void registerNewInstanceInTransaction(CoreInstance newInstance)
    {
        ModelRepositoryTransaction transaction = getTransaction();
        if (transaction != null)
        {
            transaction.registerNew(newInstance);
        }
    }

    public void validate(M4StateListener listener) throws PureCompilationException
    {
        MutableSet<CoreInstance> doneList = Sets.mutable.empty();
        listener.startRepositorySimpleValidation();
        for (CoreInstance instance : getTopLevels())
        {
            instance.validate(doneList);
        }
        listener.finishedRepositorySimpleValidation(doneList);
    }

    public void setExclusionSet(Iterable<? extends CoreInstance> exclusionSet)
    {
        this.exclusionSet = Sets.immutable.withAll(exclusionSet);
    }

    public SetIterable<CoreInstance> getExclusionSet()
    {
        return this.exclusionSet;
    }

    public CoreInstance instantiate(CoreInstance classifierType, String instanceName, SourceInformation sourceInformation, CoreInstance owner, NameSpace possibleNameSpace, boolean deep)
    {
        if (possibleNameSpace == null)
        {
            if (deep)
            {
                // Primitives
                return this.newCoreInstance(instanceName, classifierType, sourceInformation);
            }
            else
            {
                CoreInstance instance = this.getOrCreateTopLevel(instanceName, sourceInformation);
                instance.setClassifier(classifierType);
                return instance;
            }
        }
        else
        {
            // TODO add a reference from the new instance back to the name space
            SimpleCoreInstance nameSpaceNode = (SimpleCoreInstance)possibleNameSpace.getNode();
            CoreInstance instance = nameSpaceNode.getOrCreateUnknownTypeNode(possibleNameSpace.getKey(), ((instanceName == null) ? nextAnonymousInstanceName() : instanceName), this);
            instance.setClassifier(classifierType);
            instance.setSourceInformation(sourceInformation);
            return instance;
        }
    }

    public CoreInstance resolve(Iterable<String> path)
    {
        Iterator<String> pathIterator = path.iterator();
        if (!pathIterator.hasNext())
        {
            throw new RuntimeException("Cannot resolve empty path");
        }
        CoreInstance node = getTopLevel(pathIterator.next());
        while (pathIterator.hasNext())
        {
            String key = pathIterator.next();
            if (!pathIterator.hasNext())
            {
                throw new RuntimeException(Iterate.makeString(path, "Invalid path: [", ", ", "]"));
            }
            String value = pathIterator.next();
            CoreInstance newNode = node.getValueInValueForMetaPropertyToMany(key, value);
            if (newNode == null)
            {
                throw new RuntimeException("Error resolving path " + Iterate.makeString(path, "[", ", ", "]") + ": '" + value + "' is unknown for the key '" + key + "' in '" + node.getName() + "'");
            }
            node = newNode;
        }
        return node;
    }

    public byte[] serialize()
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        serialize(stream);
        return stream.toByteArray();
    }

    public void serialize(OutputStream stream)
    {
        new BinaryRepositorySerializer(stream).serialize(this);
    }

    public ModelRepositoryTransaction newTransaction(boolean committable)
    {
        return this.transactionManager.newTransaction(committable);
    }

    public ModelRepositoryTransaction getTransaction()
    {
        return this.transactionManager.getThreadLocalTransaction();
    }

    public void commitTransactionTopLevels(ModelRepositoryTransaction transaction)
    {
        if (transaction == null)
        {
            throw new IllegalArgumentException("transaction may not be null");
        }
        if (transaction.getModelRepository() != this)
        {
            throw new IllegalArgumentException("transaction is for a different model repository");
        }
        for (CoreInstance topLevel : transaction.getTopLevels())
        {
            CoreInstance current = this.topLevelMap.getIfAbsentPut(topLevel.getName(), topLevel);
            if (current != topLevel)
            {
                throw new RuntimeException("A top level element already exists with the name \"" + topLevel.getName() + "\"");
            }
        }
    }

    public void setCounters(int idCounter, int anonymousIdCounter)
    {
        this.idCounter.set(idCounter);
        this.anonymousIdCounter.set(anonymousIdCounter);
    }

    private int nextId()
    {
        return this.idCounter.getAndIncrement();
    }

    private int nextAnonymousId()
    {
        return this.anonymousIdCounter.getAndIncrement();
    }

    private String nextAnonymousInstanceName()
    {
        String id = Integer.toString(nextAnonymousId(), 32);
        if (id.length() >= ANONYMOUS_PADDING_LENGTH)
        {
            return ANONYMOUS_NAME_PREFIX.concat(id);
        }

        StringBuilder builder = new StringBuilder(ANONYMOUS_PADDING_TOTAL_LENGTH).append(ANONYMOUS_NAME_PREFIX);
        for (int i = id.length(); i < ANONYMOUS_PADDING_LENGTH; i++)
        {
            builder.append('0');
        }
        return builder.append(id).toString();
    }

    public void setTransactionObserver(TransactionObserver transactionObserver)
    {
        this.transactionObserver = transactionObserver;
    }

    public static boolean isAnonymousInstanceName(String name)
    {
        return (name != null) &&
                (name.length() >= ANONYMOUS_PADDING_TOTAL_LENGTH) &&
                name.startsWith(ANONYMOUS_NAME_PREFIX);
    }

    /**
     * Useful for printing and testing, so that the ID numbers don't change in the prints
     * @param id instance id
     * @return the original ID or "Anonymous_StripedId"
     */
    public static String possiblyReplaceAnonymousId(String id)
    {
        return isAnonymousInstanceName(id) ? "Anonymous_StripedId" : id;
    }

    private class ModelRepositoryTransactionManager extends TransactionManager<ModelRepositoryTransaction>
    {
        @Override
        protected ModelRepositoryTransaction createTransaction(boolean committable)
        {
            return ModelRepositoryTransaction.newTransaction(this, committable, ModelRepository.this, ModelRepository.this.transactionObserver);
        }
    }
}