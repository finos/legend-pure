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

package org.finos.legend.pure.m4.serialization.grammar.antlr;

import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.finos.legend.pure.m4.coreinstance.SourceInformation;


public class PureAntlrErrorStrategy extends DefaultErrorStrategy
{

    private final AntlrSourceInformation sourceInformation;

    public PureAntlrErrorStrategy(AntlrSourceInformation sourceInformation)
    {
        this.sourceInformation = sourceInformation;
    }

    @Override
    public void recover(Parser recognizer, RecognitionException e)
    {
        Token t = recognizer.getCurrentToken();
        SourceInformation sourceInformation = this.sourceInformation.getSourceInformationForUnknownErrorPosition(t.getLine(), t.getCharPositionInLine());
        throw new PureParserException(sourceInformation, e.getMessage(), e);
    }

    @Override
    public Token recoverInline(Parser recognizer) throws RecognitionException
    {
        Token t = recognizer.getCurrentToken();
        String tokenName = getTokenErrorDisplay(t);
        IntervalSet expecting = getExpectedTokens(recognizer);
        String msg = getParserErrorMsg(recognizer, tokenName, expecting);
        SourceInformation sourceInformation = this.sourceInformation.getSourceInformationForOffendingToken(t.getLine(), t.getCharPositionInLine(), t);
        throw new PureParserException(sourceInformation, msg, new InputMismatchException(recognizer));
    }

    @Override
    public void sync(Parser recognizer) throws RecognitionException
    {
        super.sync(recognizer);
    }

    @Override
    protected void reportUnwantedToken(Parser recognizer)
    {
        if (inErrorRecoveryMode(recognizer))
        {
            return;
        }

        beginErrorCondition(recognizer);

        this.notifyWithPureErrorMessage(recognizer);
    }

    @Override
    protected void reportNoViableAlternative(Parser recognizer, NoViableAltException e)
    {
        this.notifyWithPureErrorMessage(recognizer, e);
    }

    @Override
    protected void reportInputMismatch(Parser recognizer, InputMismatchException e)
    {
        notifyWithPureErrorMessage(recognizer, e);
    }

    private String getParserErrorMsg(Parser recognizer, String tokenName, IntervalSet expecting)
    {
        String expectedTokenStr = expecting.toString(recognizer.getVocabulary());
        if (expectedTokenStr.contains("VALID_STRING"))
        {
            return "expected: a valid identifier text" + "; found: " + tokenName;
        }
        return "expected:" + (expecting.size() > 1 ? " one of " : " ") + expectedTokenStr + " found: " + tokenName;
    }

    private void notifyWithPureErrorMessage(Parser recognizer)
    {
        Token t = recognizer.getCurrentToken();
        String tokenName = getTokenErrorDisplay(t);
        IntervalSet expecting = getExpectedTokens(recognizer);
        String msg = getParserErrorMsg(recognizer, tokenName, expecting);
        recognizer.notifyErrorListeners(t, msg, null);
    }

    private void notifyWithPureErrorMessage(Parser recognizer, RecognitionException e)
    {
        String tokenName = this.getTokenErrorDisplay(e.getOffendingToken());
        IntervalSet expecting = this.getExpectedTokens(recognizer);
        String msg = this.getParserErrorMsg(recognizer, tokenName, expecting);
        recognizer.notifyErrorListeners(e.getOffendingToken(), msg, e);
    }
}
