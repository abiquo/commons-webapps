/**
 * Copyright (C) 2008 - Abiquo Holdings S.L. All rights reserved.
 *
 * Please see /opt/abiquo/tomcat/webapps/legal/ on Abiquo server
 * or contact contact@abiquo.com for licensing information.
 */
package com.abiquo.commons.crypto;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static java.lang.Boolean.getBoolean;
import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.crypto.encrypt.Encryptors;

import com.google.common.base.Throwables;
import com.google.common.io.CharSource;
import com.google.common.io.Files;

/**
 * Utility class for encoding/decoding passwords at Abiquo.
 * 
 * @author <a href="mailto:serafin.sedano@abiquo.com">Serafin Sedano</a>
 */
public class Crypto
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Crypto.class);

    private static final Crypto INSTANCE = new Crypto();

    public static Crypto instance()
    {
        return INSTANCE;
    }

    private final StringBuilder pwd;

    private final byte[] salt;

    private Crypto()
    {
        try
        {
            StringBuilder tmp = new StringBuilder();
            if (getBoolean("abiquo.security.encrypt"))
            {
                CharSource source =
                    Files.asCharSource(new File("/etc/abiquo/.store"), StandardCharsets.UTF_8);

                source.copyTo(tmp);
            }
            else
            {
                tmp.append("no-password");
            }
            pwd = tmp;

            salt = StandardCharsets.UTF_8.encode(CharBuffer.wrap(tmp)).array();
        }
        catch (FileNotFoundException n)
        {
            LOGGER.error("File not found, make sure to configure Abiquo properly");
            throw Throwables.propagate(n);
        }
        catch (Exception e)
        {
            throw Throwables.propagate(e);
        }
    }

    /**
     * String since it is already encrypted.
     */
    public String encode(final String password)
    {
        requireNonNull(password, "password");
        return Encryptors.queryableText(pwd, new String(Hex.encode(salt))).encrypt(password);
    }

    /**
     * If need a String: <code>new String(chars)</code>
     */
    public char[] decode(final String encoded)
    {
        requireNonNull(encoded, "encoded");
        // queryableText since we need to check for duplicated in DB
        return Encryptors.queryableText(pwd, new String(Hex.encode(salt))).decrypt(encoded)
            .toCharArray();
    }

    /** Matches <code>password</code> with <code>encoded</code>. True if success, false otherwise. */
    public boolean matches(final String password, final String encoded)
    {
        if (isNullOrEmpty(password) || isNullOrEmpty(encoded))
        {
            return false;
        }
        return nullToEmpty(password).equals(new String(decode(nullToEmpty(encoded))));
    }
}
