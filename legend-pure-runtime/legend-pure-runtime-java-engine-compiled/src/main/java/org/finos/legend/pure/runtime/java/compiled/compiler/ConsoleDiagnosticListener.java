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

package org.finos.legend.pure.runtime.java.compiled.compiler;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.util.Locale;

/**
 * Class to write DiagnosticInformation during inMemory compile to Standard out.
 */
public class ConsoleDiagnosticListener implements DiagnosticListener<JavaFileObject>
{
    public void report(Diagnostic<? extends JavaFileObject> diagnostic)
    {
        System.out.println("Line Number->" + diagnostic.getLineNumber());
        System.out.println("code->" + diagnostic.getCode());
        System.out.println("Message->"
                + diagnostic.getMessage(Locale.ENGLISH));
        System.out.println("Source->" + diagnostic.getSource());
        System.out.println(" ");
    }
}
