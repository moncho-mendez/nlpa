/*-
 * #%L
 * NLPA
 * %%
 * Copyright (C) 2018 - 2019 SING Group (University of Vigo)
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package org.nlpa.pipe.impl;

import com.google.auto.service.AutoService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bdp4j.pipe.AbstractPipe;
import org.bdp4j.pipe.PipeParameter;
import org.bdp4j.pipe.TransformationPipe;
import org.bdp4j.types.Instance;
import org.bdp4j.util.Pair;
import org.nlpa.types.Dictionary;
import org.nlpa.types.SynsetSequence;
import org.nlpa.util.BabelUtils;
import org.nlpa.util.unmatchedtexthandler.ObfuscationHandler;
import org.nlpa.util.unmatchedtexthandler.TyposHandler;
import org.nlpa.util.unmatchedtexthandler.UnmatchedTextHandler;
import org.nlpa.util.unmatchedtexthandler.UrbanDictionaryHandler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.bdp4j.pipe.Pipe;
import org.bdp4j.pipe.SharedDataProducer;

import static org.nlpa.pipe.impl.GuessLanguageFromStringBufferPipe.DEFAULT_LANG_PROPERTY;
import static org.nlpa.pipe.impl.FindEmoticonInStringBufferPipe.DEFAULT_EMOTICON_PROPERTY;

/**
 * This pipe modifies the data of an Instance transforming it from StringBuffer
 * to SynsetSequence
 *
 * @author Iñaki Velez
 * @author Enaitz Ezpeleta
 * @author José Ramón Méndez
 */
@AutoService(Pipe.class)
@TransformationPipe()
public class StringBuffer2SynsetSequencePipe extends AbstractPipe implements SharedDataProducer {

    /**
     * For loggins purposes
     */
    private static final Logger logger = LogManager.getLogger(StringBuffer2SynsetSequencePipe.class);

    /**
     * An array of UnmatchedTextHandlers to fix incorrect text fragments
     */
    UnmatchedTextHandler[] vUTH = {new UrbanDictionaryHandler(), new TyposHandler(), new ObfuscationHandler()};

    /**
     * The name of the property where the language is stored
     */
    private String langProp = DEFAULT_LANG_PROPERTY;

    /**
     * The default value of replaceEmoticon property
     */
    private final static String DEFAULT_REPLACE_EMOTICON_VALUE = "true";

    /**
     * List of puntuation marks accepted on the beggining of a word
     */
    private static final String acceptedCharOnBeggining = "¿¡[(\"\'";
    private static Pattern acceptedCharOnBegginingPattern = Pattern.compile("^[¿¡\\[\\(\"\'][¿¡\\[\\(\"\']*");
    /**
     * List of puntuation marks accepted on the end of a word
     */
    private static final String acceptedCharOnEnd = ".,!?)];:\"\'";
    private static Pattern acceptedCharOnEndPattern = Pattern.compile("[\\.,!?\\)\\];:<>\"\'][\\.,!?\\)\\];:<>\"\']*$");

    /**
     * List of puntuation marks accepted on the middle of a word
     */
    private static final String acceptedCharOnMiddle = "/-.,;:";
    private static Pattern acceptedCharOnMiddlePattern = Pattern.compile("[\\/\\()\\)\\-\\.,;:<>][\\/\\-\\.,;:<>]*");

    /**
     * The property that indicates if emoticon property will be transform in a
     * synset
     */
    private static Boolean replaceEmoticon = Boolean.parseBoolean(DEFAULT_REPLACE_EMOTICON_VALUE);

    /**
     * A pattern to detect puntuation marks
     */
    private Pattern puntMarkPattern = Pattern.compile("\\p{Punct}");

    /**
     * List of synset-emoticon relationship
     */
    private final static Map<String, String> EMOTICON_SYNSETS = new HashMap<>();


    /* Load list of synset-emoticon relationship*/
    static {
        if (replaceEmoticon) {
            for (String i : new String[]{"/synsets-json/emoticons.json"}) {
                InputStream is = InterjectionFromStringBufferPipe.class.getResourceAsStream(i);
                JsonReader rdr = Json.createReader(is);
                JsonObject jsonObject = rdr.readObject();
                jsonObject.keySet().forEach((chars) -> {
                    EMOTICON_SYNSETS.put(chars, jsonObject.getString(chars));
                });
            }
        }
    }

    /**
     * Default constructor. Creates a StringBuffer2SynsetSequencePipe Pipe
     */
    public StringBuffer2SynsetSequencePipe() {
        super(new Class<?>[]{GuessLanguageFromStringBufferPipe.class}, new Class<?>[0]);
    }

    public StringBuffer2SynsetSequencePipe(Boolean replaceEmoticonProp) {
        super(new Class<?>[]{GuessLanguageFromStringBufferPipe.class}, new Class<?>[0]);
        replaceEmoticon = replaceEmoticonProp;
    }

    /**
     * Return the input type included the data attribute of an Instance
     *
     * @return the input type for the data attribute of the Instance processed
     */
    @Override
    public Class<?> getInputType() {
        return StringBuffer.class;
    }

    /**
     * Indicates the datatype expected in the data attribute of an Instance
     * after processing
     *
     * @return the datatype expected in the data attribute of an Instance after
     * processing
     */
    @Override
    public Class<?> getOutputType() {
        return SynsetSequence.class;
    }

    /**
     * Stablish the name of the property where the language will be stored
     *
     * @param langProp The name of the property where the language is stored
     */
    @PipeParameter(name = "langpropname", description = "Indicates the property name to store the language", defaultValue = DEFAULT_LANG_PROPERTY)
    public void setLangProp(String langProp) {
        this.langProp = langProp;
    }

    /**
     * Returns the name of the property in which the language is stored
     *
     * @return the name of the property where the language is stored
     */
    public String getLangProp() {
        return this.langProp;
    }

    /**
     * Get the value of replaceEmoticon property
     *
     * @return The value of replaceEmoticon property
     */
    public Boolean isReplaceEmoticon() {
        return replaceEmoticon;
    }

    /**
     * Establish replaceEmoticon value
     *
     * @param replaceEmoticon True if you want to add the synsetID corresponding
     * with emoticon property
     */
    @PipeParameter(name = "replaceemoticonname", description = "Establish replaceEmoticon value", defaultValue = DEFAULT_REPLACE_EMOTICON_VALUE)
    public void setReplaceEmoticon(Boolean replaceEmoticonProp) {
        replaceEmoticon = replaceEmoticonProp;
    }

    /**
     * This method find fagments in text (str) thar are incorrect.
     *
     * @param str The original text
     * @param lang The language of the original text
     * @return A vector of pairs (T,R) where T is the incorrect fragment and R
     * will be the replacement (null now)
     */
    private ArrayList<Pair<String, String>> computeUnmatched(String str, String lang) {
        StringTokenizer st = new StringTokenizer(str, " \t\n\r\u000b\f");

        // The value that will be returned
        ArrayList<Pair<String, String>> returnValue = new ArrayList<Pair<String, String>>();

        while (st.hasMoreTokens()) {
            String current = st.nextToken().trim();

            Matcher matcher = puntMarkPattern.matcher(current);
            if (matcher.find()) { // We found a puntuation mark in the token
                // matcher.start() <- here is the index of the puntuation mark
                // We developed rules checking also the existence of term/terms in Babelnet

                // if do not fit the rules and/or not found in Babelnet
                // returnValue.add(new Pair<String,String>(current,null));
                // To check the exitence of the term in BabelNet, we will
                // create a class org.ski4spam.util.BabelNetUtils with
                // static methods.
                int indexOfPuntMark = matcher.start();
                if (indexOfPuntMark == 0) { // The puntuation symbol is at the beggining
                    if (acceptedCharOnBeggining.indexOf(current.charAt(indexOfPuntMark)) == -1) {
                        returnValue.add(new Pair<>(current, null));
                    } else {
                        Matcher innerMatcher = acceptedCharOnBegginingPattern.matcher(current);
                        if (!BabelUtils.getDefault().isTermInBabelNet(innerMatcher.replaceFirst(""), lang)) {
                            returnValue.add(new Pair<>(current, null));
                        }
                    }
                } else if (indexOfPuntMark == current.length() - 1) { // the puntuation symbol is at the end
                    if (acceptedCharOnEnd.indexOf(current.charAt(indexOfPuntMark)) == -1) {
                        returnValue.add(new Pair<>(current, null));
                    } else {
                        if (!BabelUtils.getDefault().isTermInBabelNet(current.substring(0, indexOfPuntMark), lang)) {
                            returnValue.add(new Pair<>(current, null));
                        }
                    }
                } else { // The puntuation symbol is in the middle
                    if (acceptedCharOnMiddle.indexOf(current.charAt(indexOfPuntMark)) == -1
                            && acceptedCharOnEnd.indexOf(current.charAt(indexOfPuntMark)) == -1) {
                        returnValue.add(new Pair<>(current, null));
                    } else {
                        Matcher innerMatcher = acceptedCharOnEndPattern.matcher(current);
                        if (innerMatcher.find(indexOfPuntMark)) {
                            if (!BabelUtils.getDefault().isTermInBabelNet(innerMatcher.replaceFirst(""), lang)) {
                                returnValue.add(new Pair<>(current, null));
                            }
                        } else {
                            innerMatcher = acceptedCharOnMiddlePattern.matcher(current);
                            if (innerMatcher.find()) {
                                String firstElement = current.substring(0, innerMatcher.start());
                                String lastElement = current.substring(innerMatcher.end());
                                if (!BabelUtils.getDefault().isTermInBabelNet(firstElement, lang)
                                        || (innerMatcher.end() < current.length() - 1
                                        && !BabelUtils.getDefault().isTermInBabelNet(lastElement, lang))) {
                                    returnValue.add(new Pair<>(current, null));
                                }
                            } else {
                                returnValue.add(new Pair<>(current, null));
                            }
                        }
                    }
                }
            } else {
                // We check if the term current exist in babelnet.
                // if current is not found in Babelnet
                if (!BabelUtils.getDefault().isTermInBabelNet(current, lang)) {
                    returnValue.add(new Pair<>(current, null));
                }
            }

        }
        return returnValue;
    }

    /**
     * Try to fix terms that are incorrectly written (and are not found in
     * Wordnet) The original text should be fixed according with the
     * replacements made
     *
     * @param originalText The originalText to fix
     * @param unmatched A list of text fragments that should be tryed to fix.
     * The text fragments are in the form of a pair (T,R) where T is the
     * original fragment ant R the replacement (null originally). This method
     * should fill R with the suggested replacement
     * @return A string containing the original text fixed
     */
    private String handleUnmatched(String originalText, List<Pair<String, String>> unmatched, String lang) {
        // Implement the UnmatchedTextHandler interface and three specific
        // implementations that are:
        // + UrbanDictionaryHandler
        // + TyposHandler
        // + ObfuscationHandler
        String returnValue = originalText;

        // The replacement should be done here
        // DONE develop these things (Moncho)
        for (Pair<String, String> current : unmatched) {
            for (int i = 0; current.getObj2() == null && i < vUTH.length; i++) {
                vUTH[i].handle(current, lang);
                // System.out.println(current.getObj1() + " (" + lang + ");");
                // writeToFile(current.getObj1() + " (" + lang + ");\r\n");
            }
            if (current.getObj2() != null) {
                returnValue.replace(current.getObj1(), current.getObj2());
            }
        }

        return returnValue;
    }

    /**
     * Create a synsetVector from text
     *
     * @param fixedText The text to transform into a synset vector
     * @param lang The language in which the original text is written
     * @return A vector of synsets. Each synset is represented in a pair (S,T)
     * where S stands for the synset ID and T for the text that matches this
     * synset ID
     */
    private ArrayList<Pair<String, String>> buildSynsetSequence(String fixedText, String lang) {
        // Call Babelfy api to transform the string into a vector of sysnsets.
        // The fisrt string in the pair is the synsetID from babelnet
        // The second string is the matched text
        // The dictionary (dict) should be updated by adding each detected synset in
        // texts.

        // Query Babelnet
        ArrayList<Pair<String, String>> returnValue = BabelUtils.getDefault().buildSynsetSequence(fixedText, lang);
        if (returnValue == null) {
            return null;
        }
        // Update dictionaries
        for (Pair<String, String> current : returnValue) {
            Dictionary.getDictionary().add(current.getObj1());
        }

        return returnValue;
    }

    @Override
    /**
     * Compute synsets from text. This method get data from StringBuffer and
     * process instances:
     * <li>Invalidate instance if the language is not present</li>
     * <li>Get the list of unmatched texts</li>
     * <li>Process this texts to get matches</li>
     * <li>Build a synset vector</li>
     */
    public Instance pipe(Instance carrier) {
        SynsetSequence sv = new SynsetSequence((StringBuffer) carrier.getData());

        // Invalidate the instance if the language is not present
        // We cannot correctly represent the instance if the language is not present
        if (carrier.getProperty(langProp) == null || ((String) carrier.getProperty(langProp)).equalsIgnoreCase("UND")) {
            logger.error("Instance " + carrier.getName()
                    + " cannot be transformed into a SynsetVector because language could not be determined. It has been invalidated.");
            carrier.invalidate();
            return carrier;
        }

        sv.setUnmatchedTexts(
                computeUnmatched(sv.getOriginalText(), ((String) carrier.getProperty(langProp)).toUpperCase()));

        if (sv.getUnmatchedTexts().size() > 0) {
            sv.setFixedText(handleUnmatched(sv.getOriginalText(), sv.getUnmatchedTexts(),
                    ((String) carrier.getProperty(langProp)).toUpperCase()));
        } else {
            sv.setFixedText(sv.getOriginalText());
        }
        ArrayList<Pair<String, String>> syns = buildSynsetSequence(sv.getFixedText(),
                ((String) carrier.getProperty(langProp)).toUpperCase());

        // If there is emoticon property, replace with the corresponding synsetID
        if (replaceEmoticon) {

            String emoticon = carrier.getProperty(DEFAULT_EMOTICON_PROPERTY).toString();
            String synsetID;
            if (!emoticon.equals("")) {
                emoticon = emoticon.trim();
                synsetID = EMOTICON_SYNSETS.get(emoticon);
                if (!synsetID.equals("")) {
                    syns.add(new Pair<>(synsetID, emoticon));
                    Dictionary.getDictionary().add(synsetID);
                }
            }
        }

        if (syns == null) {
            logger.info("Invalidating instance because was unable to find synsets");
            carrier.setData(null);
            carrier.invalidate();
            return carrier;
        }

        sv.setSynsets(syns);

        carrier.setData(sv);

        logger.info("Instance processed: " + carrier.getName());
        
        return carrier;
    }

    /**
     * Save data to a file
     *
     * @param dir Directory name where the data is saved
     */
    @Override
    public void writeToDisk(String dir) {
        Dictionary.getDictionary().writeToDisk(dir + System.getProperty("file.separator") + "Dictionary.ser");
    }
}
