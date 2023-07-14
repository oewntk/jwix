/* ******************************************************************************
 * Java Wordnet Interface Library (JWI) v2.4.0
 * Copyright (c) 2007-2015 Mark A. Finlayson
 *
 * JWI is distributed under the terms of the Creative Commons Attribution 4.0
 * International Public License, which means it may be freely used for all
 * purposes, as long as proper acknowledgment is made.  See the license file
 * included with this distribution for more details.
 *******************************************************************************/

package edu.mit.jwi.morph;

import edu.mit.jwi.IDictionary;
import edu.mit.jwi.Nullable;
import edu.mit.jwi.item.IExceptionEntry;
import edu.mit.jwi.item.POS;

import java.util.*;

/**
 * This stemmer adds functionality to the simple pattern-based stemmer
 * {@code SimpleStemmer} by checking to see if possible stems are actually
 * contained in Wordnet. If any stems are found, only these stems are returned.
 * If no prospective stems are found, the word is considered unknown, and the
 * result returned is the same as that of the {@code SimpleStemmer} class.
 *
 * @author Mark A. Finlayson
 * @version 2.4.0
 * @since JWI 1.0
 */
public class WordnetStemmer extends SimpleStemmer
{
    @Nullable
    private final IDictionary dict;

    /**
     * Constructs a WordnetStemmer that, naturally, requires a Wordnet
     * dictionary.
     *
     * @param dict the dictionary to use; may not be <code>null</code>
     * @throws NullPointerException if the specified dictionary is <code>null</code>
     * @since JWI 1.0
     */
    public WordnetStemmer(@Nullable IDictionary dict)
    {
        if (dict == null)
        {
            throw new NullPointerException();
        }
        this.dict = dict;
    }

    /**
     * Returns the dictionary in use by the stemmer; will not return <code>null</code>
     *
     * @return the dictionary in use by this stemmer
     * @since JWI 2.2.0
     */
    @Nullable
    public IDictionary getDictionary()
    {
        return dict;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.morph.SimpleStemmer#findStems(java.lang.String, edu.edu.mit.jwi.item.POS)
     */
    public List<String> findStems(String word, @Nullable POS pos)
    {
        word = normalize(word);

        if (pos == null)
        {
            return super.findStems(word, null);
        }

        Set<String> result = new LinkedHashSet<>();

        // first look for the word in the exception lists
        IExceptionEntry excEntry = dict.getExceptionEntry(word, pos);
        if (excEntry != null)
        {
            result.addAll(excEntry.getRootForms());
        }

        // then look and see if it's in Wordnet; if so, the form itself is a stem
        if (dict.getIndexWord(word, pos) != null)
        {
            result.add(word);
        }

        if (excEntry != null)
        {
            return new ArrayList<>(result);
        }

        // go to the simple stemmer and check and see if any of those stems are in WordNet
        List<String> possibles = super.findStems(word, pos);

        // Fix for Bug015: don't allow empty strings to go to the dictionary
        possibles.removeIf(s -> s.trim().length() == 0);

        // check each algorithmically obtained root to see if it's in WordNet
        for (String possible : possibles)
        {
            if (dict.getIndexWord(possible, pos) != null)
            {
                result.add(possible);
            }
        }

        if (result.isEmpty())
        {
            if (possibles.isEmpty())
            {
                return Collections.emptyList();
            }
            else
            {
                return new ArrayList<>(possibles);
            }
        }
        return new ArrayList<>(result);
    }
}
