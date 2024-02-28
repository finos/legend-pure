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

package org.finos.legend.pure.m3.tools;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

public class IdUtils
{
    private static final SecureRandom RANDOM = new SecureRandom();

    public static class IdComponents
    {
        private final long timeInMillis;
        private final byte[] hostAddressBytes;
        private final byte[] randomBytes;

        public IdComponents(long timeInMillis, byte[] hostAddressBytes, byte[] randomBytes)
        {
            this.timeInMillis = timeInMillis;
            this.hostAddressBytes = hostAddressBytes;
            this.randomBytes = randomBytes;
        }

        public long getTimeInMillis()
        {
            return this.timeInMillis;
        }

        public byte[] getHostAddressBytes()
        {
            return this.hostAddressBytes;
        }

        public byte[] getRandomBytes()
        {
            return this.randomBytes;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (!(o instanceof IdComponents))
            {
                return false;
            }

            IdComponents that = (IdComponents) o;

            if (this.timeInMillis != that.timeInMillis)
            {
                return false;
            }
            if (!Arrays.equals(this.hostAddressBytes, that.hostAddressBytes))
            {
                return false;
            }
            if (!Arrays.equals(this.randomBytes, that.randomBytes))
            {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = (int) (this.timeInMillis ^ timeInMillis >>> 32);
            result = 31 * result + (this.hostAddressBytes != null ? Arrays.hashCode(this.hostAddressBytes) : 0);
            result = 31 * result + (this.randomBytes != null ? Arrays.hashCode(this.randomBytes) : 0);
            return result;
        }
    }


    public static String generateUniqueId(long timeInMillis)
    {
        byte[] hostAddressBytes = getHostAddressBytes();
        if (hostAddressBytes == null)
        {
            hostAddressBytes = new byte[4];
            RANDOM.nextBytes(hostAddressBytes);
        }

        byte[] random = new byte[8];
        RANDOM.nextBytes(random);

        return constructId(new IdComponents(timeInMillis, hostAddressBytes, random));
    }

    public static String constructId(IdComponents components)
    {
        StringBuilder builder = new StringBuilder(40);
        builder.append(Hex.encodeHex(components.getHostAddressBytes())); //size unknown

        // 64 bits from system time
        builder.append(Hex.encodeHex(getTimeAsBytes(components.getTimeInMillis()))); // 8 bytes

        builder.append(Hex.encodeHex(components.getRandomBytes())); //8 bytes

        return builder.toString();
    }

    public static IdComponents deconstructId(String id)
    {
        byte[] decodedId;
        try
        {
            decodedId = Hex.decodeHex(id.toCharArray());
        }
        catch (DecoderException e)
        {
            throw new RuntimeException("Invalid id: \"" + id + "\"", e);
        }

        byte[] hostAddressAsBytes = Arrays.copyOfRange(decodedId, 0, (decodedId.length - 16));

        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(decodedId, decodedId.length - 16, 8);
        buffer.flip();
        long timeInMillis = buffer.getLong();

        byte[] randomBytes = Arrays.copyOfRange(decodedId, decodedId.length - 8, decodedId.length);

        return new IdComponents(timeInMillis, hostAddressAsBytes, randomBytes);
    }

    public static byte[] getHostAddressBytes()
    {
        try
        {
            return InetAddress.getLocalHost().getAddress();
        }
        catch (UnknownHostException e)
        {
            return null;
        }
    }

    public static byte[] getTimeAsBytes(long timeInMillis)
    {
        byte[] bytes = new byte[8];
        bytes[0] = (byte) (timeInMillis >> 56);
        bytes[1] = (byte) (timeInMillis >> 48);
        bytes[2] = (byte) (timeInMillis >> 40);
        bytes[3] = (byte) (timeInMillis >> 32);
        bytes[4] = (byte) (timeInMillis >> 24);
        bytes[5] = (byte) (timeInMillis >> 16);
        bytes[6] = (byte) (timeInMillis >> 8);
        bytes[7] = (byte) timeInMillis;
        return bytes;
    }

}
