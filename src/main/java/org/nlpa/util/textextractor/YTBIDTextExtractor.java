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
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.nlpa.util.Configuration;
import org.nlpa.util.YouTubeConfigurator;

/**
 * A TextExtractor used to extract text from youtube comments identifier Files
 * using this TextExtractor should contain only a identifier of a youtube
 * comment
 *
 * @author Rosalía Laza
 * @author Reyes Pavón
 */
public class YTBIDTextExtractor extends TextExtractor {

    /**
     * For logging purposes
     */
    private static final Logger logger = LogManager.getLogger(YTBIDTextExtractor.class);

    /**
     * A static instance of the TexTextractor to implement a singleton pattern
     */
    static TextExtractor instance = null;

    /**
     * Private default constructor
     */
    private YTBIDTextExtractor() {

    }

    /**
     * Retrieve the extensions that can process this TextExtractor
     *
     * @return An array of Strings containing the extensions of files that this
     * TextExtractor can handle
     */
    public static String[] getExtensions() {
        return new String[]{"ytbid"};
    }

    /**
     * Return an instance of this TextExtractor
     *
     * @return an instance of this TextExtractor
     */
    public static TextExtractor getInstance() {
        if (instance == null) {
            instance = new YTBIDTextExtractor();
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
        //Achieving the youtube id from the given file.
        String youtubeId;
        StringBuffer sbResult = new StringBuffer();
        String text = null;
        try {
            FileReader f = new FileReader(file);
            BufferedReader b = new BufferedReader(f);
            youtubeId = b.readLine();
            b.close();
        } catch (IOException e) {
            logger.error("IO Exception caught / " + e.getMessage() + "Current youtube: " + file.getAbsolutePath());
            return null; //Return a null will cause a fuerther invalidation of the instance
        }

        String apiKey = Configuration.getSystemConfig().getConfigOption("youtube", "APIKey");

        //Extracting and returning the youtube text or error if not available.
        try {
            YouTubeConfigurator youTubeConfigurator = YouTubeConfigurator.getYouTubeData();
            if (youTubeConfigurator.size() == 0) {
                youTubeConfigurator.retrieveYoutubeCache();
            }
            boolean existsData = YouTubeConfigurator.getYouTubeData().isIncluded(youtubeId);
            if (!existsData) {
                URL url = new URL("https://www.googleapis.com/youtube/v3/comments?part=snippet&id=" + youtubeId + "&textFormat=html&key=" + apiKey);
                InputStream is = url.openStream();
                JsonReader rdr = Json.createReader(is);
                JsonObject obj = rdr.readObject();
                JsonArray arr = obj.getJsonArray("items");
                if (arr.isEmpty()) {
                    logger.error("empty array while processing " + file.getAbsolutePath());
                    youTubeConfigurator.add(youtubeId, null, null);
                    youTubeConfigurator.saveYoutubeCache();
                    return null;
                } else {
                    // Get text
                    text = arr.getJsonObject(0).getJsonObject("snippet").getString("textOriginal");
                    //detecting charset with library
                    byte[] rawData = text.getBytes();
                    CharsetDetector detector = new CharsetDetector();
                    detector.setText(rawData);
                    CharsetMatch cm = detector.detect();
                    logger.warn("Charset guesed: " + cm.getName() + " [confidence=" + cm.getConfidence() + "/100]for " + file.getAbsolutePath() + " Content type: " + text);
                    sbResult.append(new String(rawData, Charset.forName(cm.getName())));

                    // Get date
                    String youTubeDate = arr.getJsonObject(0).getJsonObject("snippet").getString("publishedAt");
                    SimpleDateFormat sdf = new SimpleDateFormat();
                    sdf.applyPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                    sdf.setTimeZone(TimeZone.getTimeZone("GMT+1"));
                    Date dateResult = sdf.parse(youTubeDate, new ParsePosition(0));

                    youTubeConfigurator.add(youtubeId, new String(rawData, Charset.forName(cm.getName())), dateResult);
                    youTubeConfigurator.saveYoutubeCache();
                    return sbResult;
                }
            } else {
                String youTubeText = YouTubeConfigurator.getYouTubeData().isIncludedText(youtubeId);
                if (youTubeText != null) {
                    CharsetDetector detector = new CharsetDetector();
                    detector.setText(youTubeText.getBytes());
                    CharsetMatch cm = detector.detect();
                    logger.warn("Charset guesed: " + cm.getName() + " [confidence=" + cm.getConfidence() + "/100]for " + file.getAbsolutePath() + " Content type: " + text);
                    sbResult.append(new String(youTubeText.getBytes(), Charset.forName(cm.getName())));
                    return sbResult;
                } else {
                    return null;
                }
            }

        } catch (MalformedURLException e) {
            logger.error(e.getMessage() + " while processing " + file.getAbsolutePath());
            return null;
        } catch (IOException e) {
            logger.error(e.getMessage() + " while processing " + file.getAbsolutePath());
            return null;
        }
    }
}
