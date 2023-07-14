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

import edu.mit.jwi.NonNull;
import edu.mit.jwi.Nullable;
import edu.mit.jwi.item.POS;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Provides simple a simple pattern-based stemming facility based on the "Rules
 * of Detachment" as described in the {@code morphy} man page in the Wordnet
 * distribution, which can be found at <a
 * href="http://wordnet.princeton.edu/man/morphy.7WN.html">
 * http://wordnet.princeton.edu/man/morphy.7WN.html</a> It also attempts to
 * strip "ful" endings. It does not search Wordnet to see if stems actually
 * exist. In particular, quoting from that man page:
 * <h3>Rules of Detachment</h3>
 * <p>
 * The following table shows the rules of detachment used by Morphy. If a word
 * ends with one of the suffixes, it is stripped from the word and the
 * corresponding ending is added. ... No rules are applicable to adverbs.
 * <p>
 * POS Suffix Ending<br>
 * <ul>
 * <li>NOUN "s" ""
 * <li>NOUN "ses" "s"
 * <li>NOUN "xes" "x"
 * <li>NOUN "zes" "z"
 * <li>NOUN "ches" "ch"
 * <li>NOUN "shes" "sh"
 * <li>NOUN "men" "man"
 * <li>NOUN "ies" "y"
 * <li>VERB "s" ""
 * <li>VERB "ies" "y"
 * <li>VERB "es" "e"
 * <li>VERB "es" ""
 * <li>VERB "ed" "e"
 * <li>VERB "ed" ""
 * <li>VERB "ing" "e"
 * <li>VERB "ing" ""
 * <li>ADJ "er" ""
 * <li>ADJ "est" ""
 * <li>ADJ "er" "e"
 * <li>ADJ "est" "e"
 * </ul>
 * <h3>Special Processing for nouns ending with 'ful'</h3>
 * <p>
 * Morphy contains code that searches for nouns ending with ful and performs a
 * transformation on the substring preceding it. It then appends 'ful' back
 * onto the resulting string and returns it. For example, if passed the nouns
 * "boxesful", it will return "boxful".
 *
 * @author Mark A. Finlayson
 * @version 2.4.0
 * @since JWI 1.0
 */
public class SimpleStemmer implements IStemmer
{
    public static final String underscore = "_";
    final Pattern whitespace = Pattern.compile("\\s+");

    public static final String SUFFIX_ches = "ches";
    public static final String SUFFIX_ed = "ed";
    public static final String SUFFIX_es = "es";
    public static final String SUFFIX_est = "est";
    public static final String SUFFIX_er = "er";
    public static final String SUFFIX_ful = "ful";
    public static final String SUFFIX_ies = "ies";
    public static final String SUFFIX_ing = "ing";
    public static final String SUFFIX_men = "men";
    public static final String SUFFIX_s = "s";
    public static final String SUFFIX_ss = "ss";
    public static final String SUFFIX_ses = "ses";
    public static final String SUFFIX_shes = "shes";
    public static final String SUFFIX_xes = "xes";
    public static final String SUFFIX_zes = "zes";

    public static final String ENDING_null = "";
    public static final String ENDING_ch = "ch";
    public static final String ENDING_e = "e";
    public static final String ENDING_man = "man";
    public static final String ENDING_s = SUFFIX_s;
    public static final String ENDING_sh = "sh";
    public static final String ENDING_x = "x";
    public static final String ENDING_y = "y";
    public static final String ENDING_z = "z";

    @NonNull
    public static final Map<POS, List<StemmingRule>> ruleMap;

    static
    {
        Map<POS, List<StemmingRule>> ruleMapHidden = new TreeMap<>();

        List<StemmingRule> list;

        // nouns
        list = new ArrayList<>(8);
        list.add(new StemmingRule(SUFFIX_s, ENDING_null, POS.NOUN, SUFFIX_ss));
        list.add(new StemmingRule(SUFFIX_ses, ENDING_s, POS.NOUN, (String[]) null));
        list.add(new StemmingRule(SUFFIX_xes, ENDING_x, POS.NOUN, (String[]) null));
        list.add(new StemmingRule(SUFFIX_zes, ENDING_z, POS.NOUN, (String[]) null));
        list.add(new StemmingRule(SUFFIX_ches, ENDING_ch, POS.NOUN, (String[]) null));
        list.add(new StemmingRule(SUFFIX_shes, ENDING_sh, POS.NOUN, (String[]) null));
        list.add(new StemmingRule(SUFFIX_men, ENDING_man, POS.NOUN, (String[]) null));
        list.add(new StemmingRule(SUFFIX_ies, ENDING_y, POS.NOUN, (String[]) null));
        ruleMapHidden.put(POS.NOUN, Collections.unmodifiableList(list));

        // verbs
        list = new ArrayList<>(8);
        list.add(new StemmingRule(SUFFIX_s, ENDING_null, POS.VERB, (String[]) null));
        list.add(new StemmingRule(SUFFIX_ies, ENDING_y, POS.VERB, (String[]) null));
        list.add(new StemmingRule(SUFFIX_es, ENDING_e, POS.VERB, (String[]) null));
        list.add(new StemmingRule(SUFFIX_es, ENDING_null, POS.VERB, (String[]) null));
        list.add(new StemmingRule(SUFFIX_ed, ENDING_e, POS.VERB, (String[]) null));
        list.add(new StemmingRule(SUFFIX_ed, ENDING_null, POS.VERB, (String[]) null));
        list.add(new StemmingRule(SUFFIX_ing, ENDING_e, POS.VERB, (String[]) null));
        list.add(new StemmingRule(SUFFIX_ing, ENDING_null, POS.VERB, (String[]) null));
        ruleMapHidden.put(POS.VERB, Collections.unmodifiableList(list));

        // adjectives
        list = new ArrayList<>(4);
        list.add(new StemmingRule(SUFFIX_er, ENDING_e, POS.ADJECTIVE, (String[]) null));
        list.add(new StemmingRule(SUFFIX_er, ENDING_null, POS.ADJECTIVE, (String[]) null));
        list.add(new StemmingRule(SUFFIX_est, ENDING_e, POS.ADJECTIVE, (String[]) null));
        list.add(new StemmingRule(SUFFIX_est, ENDING_null, POS.ADJECTIVE, (String[]) null));
        ruleMapHidden.put(POS.ADJECTIVE, Collections.unmodifiableList(list));

        // adverbs
        ruleMapHidden.put(POS.ADVERB, Collections.emptyList());

        // assign
        ruleMap = Collections.unmodifiableMap(ruleMapHidden);
    }

    /**
     * Returns a set of stemming rules used by this stemmer. Will not return a
     * null map, but it may be empty. The lists in the map will also not be
     * null, but may be empty.
     *
     * @return the rule map for this stemmer
     * @since JWI 3.5.1
     */
    @NonNull
    @SuppressWarnings("SameReturnValue")
    public Map<POS, List<StemmingRule>> getRuleMap()
    {
        return ruleMap;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.edu.mit.jwi.morph.IStemmer#findStems(java.lang.String, edu.edu.mit.jwi.item.POS)
     */
    public List<String> findStems(String word, @Nullable POS pos)
    {
        word = normalize(word);

        // if pos is null, do all
        if (pos == null)
        {
            Set<String> result = new LinkedHashSet<>();
            for (POS p : POS.values())
            {
                result.addAll(findStems(word, p));
            }
            if (result.isEmpty())
            {
                return Collections.emptyList();
            }
            return new ArrayList<>(result);
        }

        boolean isCollocation = word.contains(underscore);

        switch (pos)
        {
            case NOUN:
                return isCollocation ? getNounCollocationRoots(word) : stripNounSuffix(word);
            case VERB:
                // BUG006: here we check for composites
                return isCollocation ? getVerbCollocationRoots(word) : stripVerbSuffix(word);
            case ADJECTIVE:
                return stripAdjectiveSuffix(word);
            case ADVERB:
                // nothing for adverbs
                return Collections.emptyList();
        }

        throw new IllegalArgumentException("This should not happen");
    }

    /**
     * Converts all whitespace runs to single underscores. Tests first to see if
     * there is any whitespace before converting.
     *
     * @param word the string to be normalized
     * @return a normalized string
     * @throws NullPointerException     if the specified string is <code>null</code>
     * @throws IllegalArgumentException if the specified string is empty or all whitespace
     * @since JWI 2.1.1
     */
    protected String normalize(String word)
    {
        // make lowercase
        word = word.toLowerCase();

        // replace all underscores with spaces
        word = word.replace('_', ' ');

        // trim off extra whitespace
        word = word.trim();
        if (word.length() == 0)
        {
            throw new IllegalArgumentException();
        }

        // replace all whitespace with underscores
        word = whitespace.matcher(word).replaceAll(underscore);

        // return normalized word
        return word;
    }

    /**
     * Strips suffixes from the specified word according to the noun rules.
     *
     * @param noun the word to be modified
     * @return a list of modified forms that were constructed, or the empty list
     * if none
     * @throws NullPointerException if the specified word is <code>null</code>
     * @since JWI 1.0
     */
    @NonNull
    protected List<String> stripNounSuffix(@NonNull final String noun)
    {
        if (noun.length() <= 2)
        {
            return Collections.emptyList();
        }

        // strip off "ful"
        String word = noun;
        String suffix = null;
        if (noun.endsWith(SUFFIX_ful))
        {
            word = noun.substring(0, noun.length() - SUFFIX_ful.length());
            suffix = SUFFIX_ful;
        }

        // we will return this to the caller
        Set<String> result = new LinkedHashSet<>();

        // apply the rules
        String root;
        List<StemmingRule> rules = getRuleMap().get(POS.NOUN);
        assert rules != null;
        for (StemmingRule rule : rules)
        {
            root = rule.apply(word, suffix);
            if (root != null && root.length() > 0)
            {
                result.add(root);
            }
        }
        return result.isEmpty() ? Collections.emptyList() : new ArrayList<>(result);
    }

    /**
     * Handles stemming noun collocations.
     *
     * @param composite the word to be modified
     * @return a list of modified forms that were constructed, or the empty list
     * if none
     * @throws NullPointerException if the specified word is <code>null</code>
     * @since JWI 1.1.1
     */
    @NonNull
    protected List<String> getNounCollocationRoots(@NonNull String composite)
    {
        // split into parts
        String[] parts = composite.split(underscore);
        if (parts.length < 2)
        {
            return Collections.emptyList();
        }

        // stem each part
        List<List<String>> rootSets = new ArrayList<>(parts.length);
        for (String part : parts)
        {
            rootSets.add(findStems(part, POS.NOUN));
        }

        // reassemble all combinations
        Set<StringBuffer> poss = new HashSet<>();

        // seed the set
        List<String> rootSet = rootSets.get(0);
        if (rootSet == null)
        {
            poss.add(new StringBuffer(parts[0]));
        }
        else
        {
            for (String root : rootSet)
            {
                poss.add(new StringBuffer(root));
            }
        }

        // make all combinations
        StringBuffer newBuf;
        Set<StringBuffer> replace;
        for (int i = 1; i < rootSets.size(); i++)
        {
            rootSet = rootSets.get(i);
            if (rootSet.isEmpty())
            {
                for (StringBuffer p : poss)
                {
                    p.append("_");
                    p.append(parts[i]);
                }
            }
            else
            {
                replace = new HashSet<>();
                for (StringBuffer p : poss)
                {
                    for (Object root : rootSet)
                    {
                        newBuf = new StringBuffer();
                        newBuf.append(p.toString());
                        newBuf.append("_");
                        newBuf.append(root);
                        replace.add(newBuf);
                    }
                }
                poss.clear();
                poss.addAll(replace);
            }
        }

        if (poss.isEmpty())
        {
            return Collections.emptyList();
        }

        // make sure to remove empties
        Set<String> result = new LinkedHashSet<>();
        String root;
        for (StringBuffer p : poss)
        {
            root = p.toString().trim();
            if (root.length() != 0)
            {
                result.add(root);
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * Strips suffixes from the specified word according to the verb rules.
     *
     * @param verb the word to be modified
     * @return a list of modified forms that were constructed, or the empty list
     * if none
     * @throws NullPointerException if the specified word is <code>null</code>
     * @since JWI 1.0
     */
    @NonNull
    protected List<String> stripVerbSuffix(@NonNull final String verb)
    {
        if (verb.length() <= 2)
        {
            return Collections.emptyList();
        }

        // we will return this to the caller
        Set<String> result = new LinkedHashSet<>();

        // apply the rules
        String root;
        List<StemmingRule> rules = getRuleMap().get(POS.VERB);
        assert rules != null;
        for (StemmingRule rule : rules)
        {
            root = rule.apply(verb);
            if (root != null && root.length() > 0)
            {
                result.add(root);
            }
        }
        return result.isEmpty() ? Collections.emptyList() : new ArrayList<>(result);
    }

    /**
     * Handles stemming verb collocations.
     *
     * @param composite the word to be modified
     * @return a list of modified forms that were constructed, or an empty list
     * if none
     * @throws NullPointerException if the specified word is <code>null</code>
     * @since JWI 1.1.1
     */
    @NonNull
    protected List<String> getVerbCollocationRoots(@NonNull String composite)
    {
        // split into parts
        String[] parts = composite.split(underscore);
        if (parts.length < 2)
        {
            return Collections.emptyList();
        }

        // find the stems of each parts
        List<List<String>> rootSets = new ArrayList<>(parts.length);
        for (String part : parts)
        {
            rootSets.add(findStems(part, POS.VERB));
        }

        Set<String> result = new LinkedHashSet<>();

        // form all combinations
        StringBuilder rootBuffer = new StringBuilder();
        for (int i = 0; i < parts.length; i++)
        {
            if (rootSets.get(i) == null)
            {
                continue;
            }
            for (String partRoot : rootSets.get(i))
            {
                rootBuffer.replace(0, rootBuffer.length(), "");

                for (int j = 0; j < parts.length; j++)
                {
                    if (j == i)
                    {
                        rootBuffer.append(partRoot);
                    }
                    else
                    {
                        rootBuffer.append(parts[j]);
                    }
                    if (j < parts.length - 1)
                    {
                        rootBuffer.append(underscore);
                    }
                }
                result.add(rootBuffer.toString());
            }
        }

        // remove any empties
        result.removeIf(s -> s.length() == 0);
        return result.isEmpty() ? Collections.emptyList() : new ArrayList<>(result);
    }

    /**
     * Strips suffixes from the specified word according to the adjective rules.
     *
     * @param adj the word to be modified
     * @return a list of modified forms that were constructed, or an empty list
     * if none
     * @throws NullPointerException if the specified word is <code>null</code>
     * @since JWI 1.0
     */
    @NonNull
    protected List<String> stripAdjectiveSuffix(@NonNull final String adj)
    {
        // we will return this to the caller
        Set<String> result = new LinkedHashSet<>();

        // apply the rules
        String root;
        List<StemmingRule> rules = getRuleMap().get(POS.ADJECTIVE);
        assert rules != null;
        for (StemmingRule rule : rules)
        {
            root = rule.apply(adj);
            if (root != null && root.length() > 0)
            {
                result.add(root);
            }
        }
        return result.isEmpty() ? Collections.emptyList() : new ArrayList<>(result);
    }
}
