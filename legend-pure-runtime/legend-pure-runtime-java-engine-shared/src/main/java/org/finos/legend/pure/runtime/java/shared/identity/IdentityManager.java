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

package org.finos.legend.pure.runtime.java.shared.identity;

public class IdentityManager
{
    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();

    public static void setAuthenticatedUserId(String user)
    {
        currentUser.set(user);
    }

    public static String getAuthenticatedUserId()
    {
        return currentUser.get() == null ? "" : currentUser.get();
    }

    public static void clear()
    {
        currentUser.remove();
    }
}
