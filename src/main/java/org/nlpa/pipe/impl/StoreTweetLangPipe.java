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
import org.bdp4j.pipe.PropertyComputingPipe;
import org.bdp4j.types.Instance;
import org.nlpa.util.TwitterConfigurator;
import twitter4j.Status;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.bdp4j.pipe.Pipe;

/**
 * This pipe implements language guessing by using Twitter API
 *
 * @author Yeray Lage Freitas
 */
@AutoService(Pipe.class)
@PropertyComputingPipe()
public class StoreTweetLangPipe extends AbstractPipe {

    /**
     * For loging purposes
     */
    private static final Logger logger = LogManager.getLogger(StoreTweetLangPipe.class);

    /**
     * Default constructor. Creates a StooreTweetLangPipe Pipe
     */
    public StoreTweetLangPipe() {
        super(new Class<?>[0], new Class<?>[0]);
    }

    /**
     * Return the input type included the data attribute of a Instance
     *
     * @return the input type for the data attribute of the Instances processed
     */
    @Override
    public Class<?> getInputType() {
        return File.class;
    }

    /**
     * Indicates the datatype expected in the data attribute of a Instance after
     * processing
     *
     * @return the datatype expected in the data attribute of a Instance after
     * processing
     */
    @Override
    public Class<?> getOutputType() {
        return File.class;
    }

    /**
     * Process an Instance. This method takes an input Instance, adds language a
     * language-reliability properties and returns it. This is the method by
     * which all pipes are eventually run.
     *
     * @param carrier Instance to be processed.
     * @return Processed instance
     */
    @Override
    public Instance pipe(Instance carrier) {
        if (carrier.getData() instanceof File) {
            if (carrier.getProperty("extension") == "twtid") { // For using this just for tweets
                String tweetId;
                File file = (File) carrier.getData();
               //Achieving the tweet id from the given file.
                try (FileReader f = new FileReader(file);
                    BufferedReader b = new BufferedReader(f);){
                    tweetId = b.readLine();
                } catch (IOException e) {
                    logger.error("IO Exception caught / " + e.getMessage() + "Current tweet: " + file.getAbsolutePath());
                    return carrier;
                }

                //Extracting and returning the tweet status date or error if not available.
                Status status = TwitterConfigurator.getTwitterData().getStatus(tweetId);
                if (status != null) {
                    carrier.setProperty("language", status.getLang());
                    carrier.setProperty("language-reliability", 1.0);
                }
            }
        }
        return carrier;
    }
}
