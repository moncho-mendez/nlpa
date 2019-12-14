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

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.InputStream;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bdp4j.pipe.Pipe;

import static org.nlpa.pipe.impl.GuessLanguageFromStringBufferPipe.DEFAULT_LANG_PROPERTY;

/**
 * This pipe detects abbreviations in text and replaces them by its expansion using dictionaries (Json files). 
 * The property that stores the language of text has to exist.
 *
 * @author Reyes Pavón Rial
 * @author Rosalía Laza Fidalgo
 */
@AutoService(Pipe.class)
@TransformationPipe()
public class AbbreviationFromStringBufferPipe extends AbstractPipe {

    /**
     * For logging purposes
     */
    private static final Logger logger = LogManager.getLogger(AbbreviationFromStringBufferPipe.class);

    /**
     * A hashset of abbreviations in different languages. NOTE: All JSON files
     * (listed below) containing abbreviations
     *
     */
    private static final HashMap<String, HashMap<String, Pair<Pattern, String>>> htAbbreviations = new HashMap<>();

    static {
        for (String i : new String[]{"/abbreviations-json/abbrev.es.json", 
                                     "/abbreviations-json/abbrev.en.json",
                                     "/abbreviations-json/abbrev.fr.json",
                                     "/abbreviations-json/abbrev.gl.json",
                                     "/abbreviations-json/abbrev.eu.json",
                                     "/abbreviations-json/abbrev.ru.json",
                                     "/abbreviations-json/abbrev.de.json",
                                     "/abbreviations-json/abbrev.pt.json"
        }) {
 
            String lang = i.substring(27, 29).toUpperCase();
            try {
                InputStream is = AbbreviationFromStringBufferPipe.class.getResourceAsStream(i);
                JsonReader rdr = Json.createReader(is);
                JsonObject jsonObject = rdr.readObject();
                rdr.close();
                HashMap<String, Pair<Pattern, String>> dict = new HashMap<>();
                for (String abbrev : jsonObject.keySet()) {
                    dict.put(abbrev, new Pair<>(
                            Pattern.compile("(?:[\\p{Space}]|[\"><¡?¿!;:,.'-]|^)(" + Pattern.quote(abbrev) + ")[;:?\"!,.'>-]?(?=(?:[\\p{Space}]|$|>))"),
                            jsonObject.getString(abbrev))); 
                }
                htAbbreviations.put(lang, dict);
            } catch (Exception e) {
                logger.error("Exception processing: " + i + " message " + e.getMessage());
            }

        }

    }

    /**
     * The name of the property where the language is stored
     */
    private String langProp = DEFAULT_LANG_PROPERTY;

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
     * Indicates the datatype expected in the data attribute of an Instance after
     * processing
     *
     * @return the datatype expected in the data attribute of an Instance after
     * processing
     */
    @Override
    public Class<?> getOutputType() {
        return StringBuffer.class;
    }

    /**
     * Default constructor. Construct a AbbreviationFromStringBuffer instance with default configuration
     */
    public AbbreviationFromStringBufferPipe() {
        this(DEFAULT_LANG_PROPERTY);
    }

    /**
     * Construct a AbbreviationFromStringBuffer instance given a language
     * property
     *
     * @param langProp The property that stores the language of text
     */
    public AbbreviationFromStringBufferPipe(String langProp) {
        super(new Class<?>[]{GuessLanguageFromStringBufferPipe.class},new Class<?>[0]);

        this.langProp = langProp;
    }

    /**
     * Establish the name of the property where the language will be stored
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
     * Process an Instance. This method takes an input Instance, modifies it
     * extending abbreviations, and returns it. This is the method by which all
     * pipes are eventually run.
     *
     * @param carrier Instance to be processed.
     * @return Instance processed
     */
    @Override
    public Instance pipe(Instance carrier) {
        if (carrier.getData() instanceof StringBuffer) {

             String lang = (String) carrier.getProperty(langProp);
             StringBuffer sb = (StringBuffer)carrier.getData();

             HashMap<String,Pair<Pattern,String>> dict = htAbbreviations.get(lang);
             if (dict==null) return carrier; //When there is not a dictionary for the language

             for(String abbrev:dict.keySet()){
                  Pattern p=dict.get(abbrev).getObj1();
                  Matcher m = p.matcher(sb);
                  int last = 0;
                  while (m.find(last)){
                     last = m.start(1);
                     sb = sb.replace(m.start(1), m.end(1), dict.get(abbrev).getObj2());
                  }
             }
        }else{
          logger.error("Data should be an StrinBuffer when processing "+carrier.getName()+" but is a "+carrier.getData().getClass().getName());
        }
        return carrier;
    }
}
