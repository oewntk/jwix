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
import java.util.Comparator;

/**
 * Concrete implementation of a wordnet file data source. This particular
 * implementation is for files on disk, and uses a binary search algorithm to
 * find requested lines. It is appropriate for alphabetically-ordered Wordnet
 * files.
 *
 * @param <T> the type of object represented in this data resource
 * @author Mark A. Finlayson
 * @version 2.4.0
 * @since JWI 2.0.0
 */
public class BinaryStartSearchWordnetFile<T> extends WordnetFile<T>
{
    // the comparator
    @SuppressWarnings("WeakerAccess")
    protected final Comparator<String> fComparator;

    /**
     * Constructs a new binary search wordnet file, on the specified file with
     * the specified content type.
     *
     * @param file        the file which backs this wordnet file; may not be
     *                    <code>null</code>
     * @param contentType the content type for this file; may not be <code>null</code>
     * @throws NullPointerException {@link NullPointerException} if either the file or content type
     *                              is <code>null</code>
     * @since JWI 2.0.0
     */
    public BinaryStartSearchWordnetFile(@NonNull File file, IContentType<T> contentType)
    {
        super(file, contentType);
        assert getContentType() != null;
        fComparator = getContentType().getLineComparator();
    }

    private final Object bufferLock = new Object();

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.IDataSource#getLine(java.lang.String)
     */
    @Nullable
    public String getLine(String key)
    {
        ByteBuffer buffer = getBuffer();

        synchronized (bufferLock)
        {
            int start = 0;
            int midpoint;
            assert buffer != null;
            int stop = buffer.limit();
            int cmp;
            String line;
            while (stop - start > 1)
            {
                // find the middle of the buffer
                midpoint = (start + stop) / 2;
                buffer.position(midpoint);

                // back up to the beginning of the line
                rewindToLineStart(buffer);

                // read line
                assert getContentType() != null;
                line = getLine(buffer, getContentType().getCharset());

                // if we get a null, we've reached the end of the file
                cmp = (line == null) ? 1 : fComparator.compare(line, key);

                // found our line
                if (cmp == 0)
                {
                    return line;
                }

                if (cmp > 0)
                {
                    // too far forward
                    stop = midpoint;
                }
                else
                {
                    // too far back
                    start = midpoint;
                }
            }
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.data.WordnetFile#makeIterator(java.nio.ByteBuffer, java.lang.String)
     */
    @NonNull
    public LineIterator makeIterator(@NonNull ByteBuffer buffer, String key)
    {
        return new BinarySearchLineIterator(buffer, key);
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
    public class BinarySearchLineIterator extends LineIterator
    {
        private final Object bufferLock;

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
        public BinarySearchLineIterator(@NonNull ByteBuffer buffer, String key)
        {
            super(buffer);
            bufferLock = new Object();
            init(key);
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.edu.mit.jwi.data.WordnetFile.LineIterator#findFirstLine(java.lang.String)
         */
        protected void findFirstLine(@NonNull String key)
        {
            synchronized (bufferLock)
            {
                int lastOffset = -1;
                int start = 0;
                int stop = itrBuffer.limit();
                int offset, midpoint;
                int compare;
                String line;
                while (start + 1 < stop)
                {
                    midpoint = (start + stop) / 2;
                    itrBuffer.position(midpoint);
                    assert getContentType() != null;
                    getLine(itrBuffer, getContentType().getCharset());
                    offset = itrBuffer.position();
                    line = getLine(itrBuffer, getContentType().getCharset());

                    // Fix for Bug009: If the line is null, we've reached
                    // the end of the file, so just advance to the first line
                    if (line == null)
                    {
                        itrBuffer.position(itrBuffer.limit());
                        return;
                    }

                    compare = fComparator.compare(line, key);
                    // if the key matches exactly, we know we have found
                    // the start of this pattern in the file
                    if (compare == 0)
                    {
                        next = line;
                        return;
                    }
                    else if (compare > 0)
                    {
                        stop = midpoint;
                    }
                    else
                    {
                        start = midpoint;
                        // remember last position before
                        lastOffset = offset;
                    }
                }

                // Getting here means that we didn't find an exact match
                // to the key, so we take the last line that started
                // with the pattern
                if (lastOffset > -1)
                {
                    itrBuffer.position(lastOffset);
                    next = getLine(itrBuffer, getContentType().getCharset());
                    return;
                }

                // If we didn't have any lines that matched the pattern
                // then just advance to the first non-comment
                itrBuffer.position(itrBuffer.limit());
            }
        }
    }
}
