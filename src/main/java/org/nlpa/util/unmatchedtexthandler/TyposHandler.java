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
package org.nlpa.util.unmatchedtexthandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.languagetool.JLanguageTool;
import org.languagetool.language.Russian;
import org.languagetool.Language;
import org.languagetool.language.*;
import org.languagetool.rules.RuleMatch;
import org.nlpa.Main;
import org.bdp4j.util.Pair;
import pt.tumba.spell.SpellChecker;

/**
 * A class to get the match with the specific word from two different tools:
 * <ul>
 * <li>JLanguageTool: A tool that offers spell and grammar checking.</li>
 * <li>JaSpell: This tool is a Java spelling checking package.</li>
 * </ul>
 *
 * @author María Novo
 */
public class TyposHandler extends UnmatchedTextHandler {

    private static final Logger logger = LogManager.getLogger(TyposHandler.class);
    private final static Map<String, Class<? extends Language>> LANGUAGE_CLASSES = new HashMap<>();

    /* Define the list of possible languages to use*/
    static {

        LANGUAGE_CLASSES.put("es", Spanish.class);
        LANGUAGE_CLASSES.put("en", BritishEnglish.class);
//        LANGUAGE_CLASSES.put("en", AmericanEnglish.class);
//        LANGUAGE_CLASSES.put("en", CanadianEnglish.class);
//        LANGUAGE_CLASSES.put("en", AustralianEnglish.class);
//        LANGUAGE_CLASSES.put("en", NewZealandEnglish.class);
//        LANGUAGE_CLASSES.put("en", SouthAfricanEnglish.class);
        LANGUAGE_CLASSES.put("zh", Chinese.class);
        LANGUAGE_CLASSES.put("nl", Dutch.class);
        LANGUAGE_CLASSES.put("ja", Japanese.class);
        LANGUAGE_CLASSES.put("de", GermanyGerman.class);
        LANGUAGE_CLASSES.put("it", Italian.class);
        LANGUAGE_CLASSES.put("pt", PortugalPortuguese.class);
        LANGUAGE_CLASSES.put("ru", Russian.class);
        LANGUAGE_CLASSES.put("bs", BosnianSerbian.class);
        LANGUAGE_CLASSES.put("br", Breton.class);
        LANGUAGE_CLASSES.put("ca", Catalan.class);
        LANGUAGE_CLASSES.put("sr", CroatianSerbian.class);
        LANGUAGE_CLASSES.put("eo", Esperanto.class);
        LANGUAGE_CLASSES.put("gl", Galician.class);
        LANGUAGE_CLASSES.put("el", Greek.class);
        LANGUAGE_CLASSES.put("sr", JekavianSerbian.class);
        LANGUAGE_CLASSES.put("sr", MontenegrinSerbian.class);
        LANGUAGE_CLASSES.put("fa", Persian.class);
        LANGUAGE_CLASSES.put("pl", Polish.class);
        LANGUAGE_CLASSES.put("ro", Romanian.class);
        LANGUAGE_CLASSES.put("sr", Serbian.class);
        LANGUAGE_CLASSES.put("sk", Slovak.class);
        LANGUAGE_CLASSES.put("uk", Ukrainian.class);
        LANGUAGE_CLASSES.put("ca", ValencianCatalan.class);
    }

    /**
     * This method get instance of the language class corresponding with the
     * lang parameter.
     *
     * @param lang Specifies the language of instance to get
     * @return the Language instance of the corresponding language. If the
     * language doesn't exists, return null.
     */
    private static Language getLanguage(String lang) {
        try {
            Class<? extends Language> languageClass = LANGUAGE_CLASSES.get(lang);
            return languageClass.getConstructor().newInstance();
        } catch (Exception ex) {
            logger.warn("The language " + lang + "doesn't exists in JlanguageTool. " + ex.getMessage());
            return null;
        }
    }

    /**
     * Check if the lang exists in this api.
     *
     * @param lang
     * @return True if the language exists, otherwise false.
     */
    private static boolean hasLanguage(String lang) {
        return LANGUAGE_CLASSES.containsKey(lang);
    }

    /**
     * This method search matches with two diferents tools: JLanguageTool and
     * JSpellMatcher
     *
     * @param lang The language of the original string
     * @param text Is a pair with the original text and the match with this
     * text. If the second one doesn't exists, the value is null
     *
     */
    @Override
    public void handle(Pair<String, String> text, String lang) {
        String originalString = "";
        try {

            originalString = text.getObj1().toLowerCase();
            originalString = originalString.replaceAll("\\p{Punct}", "");

            String replacementString = text.getObj2();
            lang = lang.toLowerCase();
            try {
                if (originalString != null && !originalString.equals("") && replacementString == null) {
                    String matchedString = getJLanguageToolMatch(originalString, lang);
                    if (matchedString != null) {
                        text.setObj2(matchedString);
                    } else {
                        matchedString = getJaSpellMatch(originalString, lang);
                        if (matchedString != null) {
                            text.setObj2(matchedString);
                        }
                    }
                }
            } catch (NullPointerException ex) {
                logger.error("ERROR" + Main.class.getName() + ". originalString = " + originalString + ":" + ex.getMessage());
            }

        } catch (Exception ex) {
            logger.error("ERROR" + Main.class.getName() + ". originalString = " + originalString + ":" + ex.getMessage());
        }
    }

    /**
     * Return match with the parameter, using JLanguageTool
     *
     * @param originalString Word to get match in resources files
     * @param lang Specifies the language of the original string
     * @return String who contains , if exists, the match with the original
     * string. In other case, return null.
     */
    private String getJLanguageToolMatch(String originalString, String lang) {
        try {
            if (hasLanguage(lang)) {
                JLanguageTool langTool = new JLanguageTool(getLanguage(lang));
                try {
                    List<RuleMatch> matches = langTool.check(originalString, false, JLanguageTool.ParagraphHandling.NORMAL);
                    if (!matches.isEmpty()) {
                        List<String> matchesList = matches.get(0).getSuggestedReplacements();
                        if (!matchesList.isEmpty()) {
                            return matchesList.get(0).toString().toLowerCase();
                        }
                    } else {
                        return originalString;
                    }

                } catch (IOException ex) {
                    logger.error(Main.class.getName() + ". " + ex.getMessage());
                }
            }
            return null;

        } catch (ExceptionInInitializerError | NoClassDefFoundError ex) {
            logger.error("ERROR " + Main.class.getName() + ". getJLanguageToolMatch: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Return the word that matches with the parameter, using JaSpell
     *
     * @param originalString Word to get match in resources files
     * @param lang Specifies the language of the original string
     * @return String who contains , if exists, the match with word. In other
     * case, return null.
     */
    private String getJaSpellMatch(String originalString, String lang) throws Exception {
        try (Reader dictionaryReader = new InputStreamReader(Main.class.getResourceAsStream("/dict/" + lang + ".txt"))) {
            SpellChecker spellCheck = new SpellChecker();
            spellCheck.initialize(dictionaryReader);
            return spellCheck.findMostSimilar(originalString);

        } catch (Exception ex) {
            if (ex.getClass().getName().equals("java.lang.NullPointerException")) {
                logger.info("ERROR getJaSpellMatch - The lang " + lang.toUpperCase() + " doesn't exist.");
            } else {
                logger.error("ERROR " + Main.class.getName() + ". getJaSpellMatch: " + ex.getMessage());
            }
            return null;
        }
    }
}
