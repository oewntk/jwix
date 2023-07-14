/* ******************************************************************************
 * Java Wordnet Interface Library (JWI) v2.4.0
 * Copyright (c) 2007-2015 Mark A. Finlayson
 *
 * JWI is distributed under the terms of the Creative Commons Attribution 4.0
 * International Public License, which means it may be freely used for all
 * purposes, as long as proper acknowledgment is made.  See the license file
 * included with this distribution for more details.
 *******************************************************************************/

package edu.mit.jwi.data;

import edu.mit.jwi.NonNull;
import edu.mit.jwi.Nullable;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * Concrete implementation of a wordnet file data source. This particular
 * implementation is for files on disk, and directly accesses the appropriate
 * byte offset in the file to find requested lines. It is appropriate for
 * Wordnet data files.
 *
 * @param <T> the type of object represented in this data resource
 * @author Mark A. Finlayson
 * @version 2.4.0
 * @since JWI 2.0.0
 */
public class DirectAccessWordnetFile<T> extends WordnetFile<T>
{
    /**
     * Constructs a new direct access wordnet file, on the specified file with
     * the specified content type.
     *
     * @param file        the file which backs this wordnet file; may not be
     *                    <code>null</code>
     * @param contentType the content type for this file; may not be <code>null</code>
     * @throws NullPointerException {@link NullPointerException} if either the file or content type
     *                              is <code>null</code>
     * @since JWI 2.0.0
     */
    public DirectAccessWordnetFile(@NonNull File file, IContentType<T> contentType)
    {
        super(file, contentType);
    }

    private final Object bufferLock = new Object();

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IDataSource#getLine(java.lang.String)
     */
    @Nullable
    public String getLine(@NonNull String key)
    {
        ByteBuffer buffer = getBuffer();
        synchronized (bufferLock)
        {
            try
            {
                int byteOffset = Integer.parseInt(key);
                assert buffer != null;
                if (buffer.limit() <= byteOffset)
                {
                    return null;
                }
                buffer.position(byteOffset);
                assert getContentType() != null;
                String line = getLine(buffer, getContentType().getCharset());
                return line != null && line.startsWith(key) ? line : null;
            }
            catch (NumberFormatException e)
            {
                return null;
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.WordnetFile#makeIterator(java.nio.ByteBuffer, java.lang.String)
     */
    @NonNull
    public LineIterator makeIterator(@NonNull ByteBuffer buffer, String key)
    {
        return new DirectLineIterator(buffer, key);
    }

    /**
     * Used to iterate over lines in a file. It is a look-ahead iterator. Does
     * not support the {@link #remove()} method; if that method is called, it
     * will throw an {@link UnsupportedOperationException}.
     *
     * @author Mark A. Finlayson
     * @version 2.4.0
     * @since JWI 2.0.0
     */
    public class DirectLineIterator extends LineIterator
    {
        /**
         * Constructs a new line iterator over this buffer, starting at the
         * specified key.
         *
         * @param buffer the buffer over which the iterator should iterator; may
         *               not be <code>null</code>
         * @param key    the key of the line to start at; may be <code>null</code>
         * @throws NullPointerException if the specified buffer is <code>null</code>
         * @since JWI 2.0.0
         */
        public DirectLineIterator(@NonNull ByteBuffer buffer, String key)
        {
            super(buffer);
            init(key);
        }

        private final Object bufferLock = new Object();

        /*
         * (non-Javadoc)
         *
         * @see edu.edu.mit.jwi.data.WordnetFile.LineIterator#findFirstLine(java.lang.String)
         */
        protected void findFirstLine(@NonNull String key)
        {
            synchronized (bufferLock)
            {
                try
                {
                    int byteOffset = Integer.parseInt(key);
                    if (itrBuffer.limit() <= byteOffset)
                    {
                        return;
                    }
                    itrBuffer.position(byteOffset);
                    assert getContentType() != null;
                    next = getLine(itrBuffer, getContentType().getCharset());
                }
                catch (NumberFormatException e)
                {
                    // Ignore
                }
            }
        }
    }
}
