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
package org.nlpa.util.textextractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nlpa.util.TwitterConfigurator;

/**
 * A TextExtractor to extract text from tweets. The files that handle this
 * TextExtractor should contain only the tweet id of the desired twitter. Please
 * remenber that storing tweets is not legal
 *
 * @author Yeray Lage
 */
public class TWTIDTextExtractor extends TextExtractor {

    /**
     * For loging purposes
     */
    private static final Logger logger = LogManager.getLogger(TWTIDTextExtractor.class);

    /**
     * A static instance of the TexTextractor to implement a singleton pattern
     */
    private static TextExtractor instance = null;

    /**
     * Private default constructor
     */
    private TWTIDTextExtractor() {

    }

    /**
     * Retrieve the extensions that can process this TextExtractor
     *
     * @return An array of Strings containing the extensions of files that this
     * TextExtractor can handle
     */
    public static String[] getExtensions() {
        return new String[]{"twtid"};
    }

    /**
     * Return an instance of this TextExtractor
     *
     * @return an instance of this TextExtractor
     */
    public static TextExtractor getInstance() {
        if (instance == null) {
            instance = new TWTIDTextExtractor();
        }
        return instance;
    }

    /**
     * Extracts text from a given file
     *
     * @param file The file where the text is included
     * @return A StringBuffer with the extracted text
     */
    @Override
    public StringBuffer extractText(File file) {
        //Achieving the tweet id from the given file.
        String tweetId;
        try {
            FileReader f = new FileReader(file);
            BufferedReader b = new BufferedReader(f);
            tweetId = b.readLine();
            b.close();
        } catch (IOException e) {
            logger.error("IO Exception caught / " + e.getMessage() + "Current tweet: " + file.getAbsolutePath());
            return null; //Return a null will cause a fuerther invalidation of the instance
        }

        //Extracting and returning the tweet status text or error if not available.
        try {
            return new StringBuffer(TwitterConfigurator.getTwitterData().getStatus(tweetId).getText());
        } catch (NullPointerException e) {
            return null;
        }
    }
}
