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

package org.finos.legend.pure.runtime.java.compiled.metadata;

import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.map.MapIterable;
import org.finos.legend.pure.m4.coreinstance.CoreInstance;

public interface Metadata
{
    void startTransaction();

    void commitTransaction();

    void rollbackTransaction();

    CoreInstance getMetadata(String classifier, String id);

    MapIterable<String, CoreInstance> getMetadata(String classifier);

    default RichIterable<CoreInstance> getClassifierInstances(String classifier)
    {
        return getMetadata(classifier).valuesView();
    }

    CoreInstance getEnum(String enumerationName, String enumName);

    /**
     * <p>Return whether the metadata supports accessing an element by its package path.</p>
     *
     * <p>If true, then {@link #hasElement} and {@link #getElementByPath} can be used to access elements by their
     * package path. Otherwise, those methods will throw {@link UnsupportedOperationException}.</p>
     * 
     * @return whether element by path is supported
     * @see #hasElement
     * @see #getElementByPath
     */
    default boolean supportsElementByPath()
    {
        return false;
    }

    /**
     * <p>Return whether the metadata has an element with the given package path.</p>
     *
     * <p>For a given path, {@link #getElementByPath} will return the element if and only if this method returns true.
     * Otherwise, it will return null.</p>
     *
     * <p>Use {@link #supportsElementByPath} to check if the method is supported on this metadata instance. If not,
     * an {@link UnsupportedOperationException} will be thrown.</p>
     *
     * @param path package path
     * @return whether there is an element with the given path
     * @throws UnsupportedOperationException if element by path access is not supported
     * @see #supportsElementByPath
     * @see #getElementByPath
     */
    default boolean hasElement(String path)
    {
        return getElementByPath(path) != null;
    }

    /**
     * <p>Get an element by its package path. Returns null if there is no such element</p>
     *
     * <p>This method will return the element if and only if {@link #hasElement} returns true. Otherwise, it will return
     * null.</p>
     *
     * <p>Use {@link #supportsElementByPath} to check if the method is supported on this metadata instance. If not,
     * an {@link UnsupportedOperationException} will be thrown.</p>
     *
     * @param path package path of the element
     * @return element with the given path, or null if there is no such element
     * @throws UnsupportedOperationException if element by path access is not supported
     * @see #supportsElementByPath
     * @see #hasElement
     */
    default CoreInstance getElementByPath(String path)
    {
        throw new UnsupportedOperationException(getClass().getName() + " does not support element by path");
    }
}
