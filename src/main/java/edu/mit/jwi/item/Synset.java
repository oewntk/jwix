/* ******************************************************************************
 * Java Wordnet Interface Library (JWI) v2.4.0
 * Copyright (c) 2007-2015 Mark A. Finlayson
 *
 * JWI is distributed under the terms of the Creative Commons Attribution 4.0
 * International Public License, which means it may be freely used for all
 * purposes, as long as proper acknowledgment is made.  See the license file
 * included with this distribution for more details.
 *******************************************************************************/

package edu.mit.jwi.item;

import edu.mit.jwi.NonNull;
import edu.mit.jwi.Nullable;

import java.util.*;
import java.util.Map.Entry;

/**
 * Default implementation of the {@code ISynset} interface.
 *
 * @author Mark A. Finlayson
 * @version 2.4.0
 * @since JWI 1.0
 */
public class Synset implements ISynset
{
    /**
     * This serial version UID identifies the last version of JWI whose
     * serialized instances of the Synset class are compatible with this
     * implementation.
     *
     * @since JWI 2.4.0
     */
    private static final long serialVersionUID = 240;

    @Nullable
    private final ISynsetID id;
    @Nullable
    private final String gloss;
    @Nullable
    private final ILexFile lexFile;
    @NonNull
    private final List<IWord> words;
    private final boolean isAdjSat;
    private final boolean isAdjHead;
    @NonNull
    private final List<ISynsetID> related;
    @NonNull
    private final Map<IPointer, List<ISynsetID>> relatedMap;

    /**
     * Constructs a new synset object with the specified parameters.
     *
     * @param id           the synset id; may not be <code>null</code>
     * @param lexFile      the lexical file for this synset; may not be <code>null</code>
     * @param isAdjSat     <code>true</code> if this object represents an adjective
     *                     satellite synset; <code>false</code> otherwise
     * @param isAdjHead    <code>true</code> if this object represents an adjective head
     *                     synset; <code>false</code> otherwise
     * @param gloss        the gloss for this synset; may not be <code>null</code>
     * @param wordBuilders the list of word builders for this synset; may not be
     *                     <code>null</code>
     * @param ids          a map of related synset lists, indexed by pointer; may be
     *                     <code>null</code>
     * @throws NullPointerException     if any of the id, lexical file, word list, or gloss are
     *                                  <code>null</code>, or the word list contains a
     *                                  <code>null</code>
     * @throws IllegalArgumentException if the word list is empty, or both the adjective satellite
     *                                  and adjective head flags are set
     * @throws IllegalArgumentException if either the adjective satellite and adjective head flags
     *                                  are set, and the lexical file number is not zero
     * @since JWI 1.0
     */
    public Synset(@Nullable ISynsetID id, @Nullable ILexFile lexFile, boolean isAdjSat, boolean isAdjHead, @Nullable String gloss,
                  @Nullable List<IWordBuilder> wordBuilders, @Nullable Map<IPointer, ? extends List<ISynsetID>> ids)
    {
        if (id == null)
        {
            throw new NullPointerException();
        }
        if (lexFile == null)
        {
            throw new NullPointerException();
        }
        if (gloss == null)
        {
            throw new NullPointerException();
        }
        if (wordBuilders == null)
        {
            throw new NullPointerException();
        }
        if (wordBuilders.isEmpty())
        {
            throw new IllegalArgumentException();
        }
        if (isAdjSat && isAdjHead)
        {
            throw new IllegalArgumentException();
        }
        if ((isAdjSat || isAdjHead) && lexFile.getNumber() != 0)
        {
            throw new IllegalArgumentException();
        }

        this.id = id;
        this.lexFile = lexFile;
        this.gloss = gloss;
        this.isAdjSat = isAdjSat;
        this.isAdjHead = isAdjHead;

        // words
        List<IWord> words = new ArrayList<>(wordBuilders.size());
        for (IWordBuilder wordBuilder : wordBuilders)
        {
            words.add(wordBuilder.toWord(this));
        }
        this.words = Collections.unmodifiableList(words);

        Set<ISynsetID> hiddenSet = null;
        Map<IPointer, List<ISynsetID>> hiddenMap = null;
        // fill synset map
        if (ids != null)
        {
            hiddenSet = new LinkedHashSet<>();
            hiddenMap = new HashMap<>(ids.size());
            for (Entry<IPointer, ? extends List<ISynsetID>> entry : ids.entrySet())
            {
                if (entry.getValue() == null || entry.getValue().isEmpty())
                {
                    continue;
                }
                hiddenMap.put(entry.getKey(), Collections.unmodifiableList(new ArrayList<>(entry.getValue())));
                hiddenSet.addAll(entry.getValue());
            }
        }
        this.related = (hiddenSet != null && !hiddenSet.isEmpty()) ? Collections.unmodifiableList(new ArrayList<>(hiddenSet)) : Collections.emptyList();
        this.relatedMap = (hiddenMap != null && !hiddenMap.isEmpty()) ? Collections.unmodifiableMap(hiddenMap) : Collections.emptyMap();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.IItem#getID()
     */
    @Nullable
    public ISynsetID getID()
    {
        return id;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISynset#getOffset()
     */
    public int getOffset()
    {
        assert id != null;
        return id.getOffset();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.IHasPOS#getPOS()
     */
    public POS getPOS()
    {
        assert id != null;
        return id.getPOS();
    }

    public int getType()
    {
        POS pos = getPOS();
        if (pos != POS.ADJECTIVE)
        {
            assert pos != null;
            return pos.getNumber();
        }
        return isAdjectiveSatellite() ? 5 : 3;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISynset#getGloss()
     */
    @Nullable
    public String getGloss()
    {
        return gloss;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISynset#getWords()
     */
    @NonNull
    public List<IWord> getWords()
    {
        return words;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISynset#getWord(int)
     */
    public IWord getWord(int wordNumber)
    {
        return words.get(wordNumber - 1);
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISynset#getLexicalFile()
     */
    @Nullable
    public ILexFile getLexicalFile()
    {
        return lexFile;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISynset#getRelatedMap()
     */
    @NonNull
    public Map<IPointer, List<ISynsetID>> getRelatedMap()
    {
        return relatedMap;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISynset#getRelatedSynsets(edu.edu.mit.jwi.item.IPointer)
     */
    @Nullable
    public List<ISynsetID> getRelatedSynsets(IPointer ptrType)
    {
        List<ISynsetID> result = relatedMap.get(ptrType);
        return result != null ? result : Collections.emptyList();
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISynset#getRelatedSynsets()
     */
    @NonNull
    public List<ISynsetID> getRelatedSynsets()
    {
        return related;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISynset#isAdjectiveSatellite()
     */
    public boolean isAdjectiveSatellite()
    {
        return isAdjSat;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.item.ISynset#isAdjectiveHead()
     */
    public boolean isAdjectiveHead()
    {
        return isAdjHead;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    public int hashCode()
    {
        final int PRIME = 31;
        int result = 1;
        assert gloss != null;
        result = PRIME * result + gloss.hashCode();
        result = PRIME * result + (isAdjSat ? 1231 : 1237);
        assert id != null;
        result = PRIME * result + id.hashCode();
        result = PRIME * result + words.hashCode();
        result = PRIME * result + relatedMap.hashCode();
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(@Nullable Object obj)
    {
        if (this == obj)
        {
            return true;
        }
        if (obj == null)
        {
            return false;
        }
        if (!(obj instanceof Synset))
        {
            return false;
        }
        final Synset other = (Synset) obj;
        assert id != null;
        if (!id.equals(other.getID()))
        {
            return false;
        }
        if (!words.equals(other.getWords()))
        {
            return false;
        }
        assert gloss != null;
        if (!gloss.equals(other.getGloss()))
        {
            return false;
        }
        if (isAdjSat != other.isAdjectiveSatellite())
        {
            return false;
        }
        return relatedMap.equals(other.getRelatedMap());
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @NonNull
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SYNSET{");
        assert id != null;
        sb.append(id);
        sb.append(" : Words[");
        for (IWord word : words)
        {
            sb.append(word.toString());
            sb.append(", ");
        }
        sb.replace(sb.length() - 2, sb.length(), "]}");
        return sb.toString();
    }

    /**
     * Takes an integer in the closed range [0,99999999] and converts it into an
     * eight decimal digit zero-filled string. E.g., "1" becomes "00000001",
     * "1234" becomes "00001234", and so on. This is used for the generation of
     * synset and word numbers.
     *
     * @param offset the offset to be converted
     * @return the zero-filled string representation of the offset
     * @throws IllegalArgumentException if the specified offset is not in the valid range of
     *                                  [0,99999999]
     * @since JWI 2.1.0
     */
    @NonNull
    public static String zeroFillOffset(int offset)
    {
        checkOffset(offset);
        StringBuilder sb = new StringBuilder(8);
        String offsetStr = Integer.toString(offset);
        int numZeros = 8 - offsetStr.length();
        for (int i = 0; i < numZeros; i++)
        {
            sb.append('0');
        }
        sb.append(offsetStr);
        return sb.toString();
    }

    /**
     * Throws an exception if the specified offset is not in the valid range of
     * [0,99999999].
     *
     * @param offset the offset to be checked
     * @return the checked offset
     * @throws IllegalArgumentException if the specified offset is not in the valid range of
     *                                  [0,99999999]
     * @since JWI 2.1.0
     */
    @SuppressWarnings("UnusedReturnValue")
    public static int checkOffset(int offset)
    {
        if (!isLegalOffset(offset))
        {
            throw new IllegalArgumentException("'" + offset + "' is not a valid offset; offsets must be in the closed range [0,99999999]");
        }
        return offset;
    }

    /**
     * Returns true an exception if the specified offset is not in the valid
     * range of [0,99999999].
     *
     * @param offset the offset to be checked
     * @return <code>true</code> if the specified offset is in the closed range
     * [0, 99999999]; <code>false</code> otherwise.
     * @since JWI 2.2.0
     */
    public static boolean isLegalOffset(int offset)
    {
        if (offset < 0)
        {
            return false;
        }
        return offset <= 99999999;
    }

    /**
     * A word builder used to construct word objects inside the synset object
     * constructor.
     *
     * @author Mark A. Finlayson
     * @version 2.4.0
     * @since JWI 2.2.0
     */
    public interface IWordBuilder
    {
        /**
         * Creates the word represented by this builder. If the builder
         * represents invalid values for a word, this method may throw an
         * exception.
         *
         * @param synset the synset to which this word should be attached
         * @return the created word
         * @since JWI 2.2.0
         */
        @NonNull
        IWord toWord(ISynset synset);

        /**
         * Adds the specified verb frame to this word.
         *
         * @param frame the frame to be added, may not be <code>null</code>
         * @throws NullPointerException if the specified frame is <code>null</code>
         * @since JWI 2.2.0
         */
        void addVerbFrame(IVerbFrame frame);

        /**
         * Adds a pointer from this word to another word with the specified id.
         *
         * @param ptrType the pointer type, may not be <code>null</code>
         * @param id      the word id, may not be <code>null</code>
         * @throws NullPointerException if either argument is <code>null</code>
         * @since JWI 2.2.0
         */
        void addRelatedWord(IPointer ptrType, IWordID id);
    }

    /**
     * Holds information about word objects before they are instantiated.
     *
     * @author Mark A. Finlayson
     * @version 2.4.0
     * @since JWI 1.0
     */
    public static class WordBuilder implements IWordBuilder
    {
        // instance fields
        private final int num;
        protected final int lexID;
        private final String lemma;
        private final AdjMarker marker;
        private final Map<IPointer, ArrayList<IWordID>> relatedWords = new HashMap<>();
        private final ArrayList<IVerbFrame> verbFrames = new ArrayList<>();

        /**
         * Constructs a new word builder object. The constructor does not check
         * its arguments - this is done when the word is created.
         *
         * @param num    the word number
         * @param lemma  the lemma
         * @param lexID  the id of the lexical file in which the word is listed
         * @param marker the adjective marker for the word
         * @since JWI 1.0
         */
        public WordBuilder(int num, String lemma, int lexID, AdjMarker marker)
        {
            this.num = num;
            this.lemma = lemma;
            this.lexID = lexID;
            this.marker = marker;
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.edu.mit.jwi.item.Synset.IWordBuilder#addRelatedWord(edu.edu.mit.jwi.item.IPointer, edu.edu.mit.jwi.item.IWordID)
         */
        public void addRelatedWord(@Nullable IPointer ptrType, @Nullable IWordID id)
        {
            if (ptrType == null)
            {
                throw new NullPointerException();
            }
            if (id == null)
            {
                throw new NullPointerException();
            }
            ArrayList<IWordID> words = relatedWords.computeIfAbsent(ptrType, k -> new ArrayList<>());
            words.add(id);
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.edu.mit.jwi.item.Synset.IWordBuilder#addVerbFrame(edu.edu.mit.jwi.item.IVerbFrame)
         */
        public void addVerbFrame(@Nullable IVerbFrame frame)
        {
            if (frame == null)
            {
                throw new NullPointerException();
            }
            verbFrames.add(frame);
        }

        /*
         * (non-Javadoc)
         *
         * @see edu.edu.mit.jwi.item.Synset.IWordBuilder#toWord(edu.edu.mit.jwi.item.ISynset)
         */
        @NonNull
        public IWord toWord(@NonNull ISynset synset)
        {
            return new Word(synset, num, lemma, lexID, marker, verbFrames, relatedWords);
        }
    }
}
