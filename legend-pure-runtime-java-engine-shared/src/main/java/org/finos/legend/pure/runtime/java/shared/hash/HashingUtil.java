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

package org.finos.legend.pure.runtime.java.shared.hash;

import org.apache.commons.codec.digest.DigestUtils;

public class HashingUtil
{
    private HashingUtil()
    {

    }

    public static String hash(String text, HashType hashType) throws RuntimeException
    {
        if (hashType == HashType.MD5)
        {
            return md5Hash(text);
        }
        if (hashType == HashType.SHA1)
        {
            return sha1Hash(text);
        }
        if (hashType == HashType.SHA256)
        {
            return sha256Hash(text);
        }
        throw new RuntimeException("Unhandled hashType: " + hashType.name());
    }

    private static String md5Hash(String text)
    {
        return DigestUtils.md5Hex(text);
    }

    private static String sha1Hash(String text)
    {
        return DigestUtils.sha1Hex(text);
    }

    private static String sha256Hash(String text)
    {
        return DigestUtils.sha256Hex(text);
    }
}
