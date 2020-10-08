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

package org.finos.legend.pure.m3.tools.kerberos;

import org.eclipse.collections.impl.block.function.checked.CheckedFunction0;

import javax.security.auth.Subject;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

public class SubjectGetter extends CheckedFunction0<Subject>
{
    private static final long SUBJECT_LIFE_MILLIS = 600000; // 10 minutes

    private final Configuration loginConfiguration;
    private Subject subject = null;
    private long timestamp = 0L;

    public SubjectGetter(Configuration loginConfiguration)
    {
        this.loginConfiguration = loginConfiguration;
    }

    @Override
    public Subject safeValue() throws LoginException
    {
        long now = System.currentTimeMillis();
        if ((this.subject == null) || ((now - this.timestamp) > SUBJECT_LIFE_MILLIS))
        {
            LoginContext context = new LoginContext("", null, null, this.loginConfiguration);
            context.login();
            this.subject = context.getSubject();
            this.timestamp = now;
        }
        return this.subject;
    }
}
