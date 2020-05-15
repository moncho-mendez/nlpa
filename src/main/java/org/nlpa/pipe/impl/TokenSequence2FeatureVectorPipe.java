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
import org.bdp4j.pipe.AbstractPipe;
import org.bdp4j.pipe.PipeParameter;
import org.bdp4j.types.Instance;
import org.nlpa.types.FeatureVector;
import org.nlpa.types.TokenSequence;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bdp4j.pipe.Pipe;
import org.bdp4j.pipe.TransformationPipe;
import org.nlpa.types.SequenceGroupingStrategy;

/**
 * A pipe to transform a TokenSequence wich contains a list of tokens included
 * in a message into a FeatureVector which compile togeher duplicated features
 * and assign a score for each feature according with a groupingStrategy. The
 * groupStrategy is one of the following: <ul>
 * <li>SequenceGroupingStrategy.COUNT: indicates the number of times that a
 * token is observed in the content (ex. 5)</li>
 * <li>SequenceGroupingStrategy.BOOLEAN: Indicates if the token is observed in
 * the content (1) or not (0) (ex. 0)</li>
 * <li>SequenceGroupingStrategy.FREQUENCY: Indicates the frequency of the token
 * in the text that is the count of times that the token is observed divided by
 * the whole amount of tokens.</li>
 * </ul>
 *
 * @author María Novo
 */
@AutoService(Pipe.class)
@TransformationPipe()
public class TokenSequence2FeatureVectorPipe extends AbstractPipe {

    /**
     * For logging purposes
     */
    private static final Logger logger = LogManager.getLogger(TokenSequence2FeatureVectorPipe.class);

    /**
     * Indicates the default grouping strategy
     */
    public static final String DEFAULT_GROUPING_STRATEGY = "COUNT";

    /**
     * Indicates the group strategy to create the featureVector
     */
    private SequenceGroupingStrategy groupStrategy = SequenceGroupingStrategy.COUNT;

    /**
     * Return the input type included the data attribute of a Instance
     *
     * @return the input type for the data attribute of the Instances processed
     */
    @Override
    public Class<?> getInputType() {
        return TokenSequence.class;
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
        return FeatureVector.class;
    }

    /**
     * Changes the grouping strategy
     *
     * @param groupStrategy The new grouping strategy
     */
    @PipeParameter(name = "groupStrategy", description = "Indicates the group strategy to create the featureVector", defaultValue = DEFAULT_GROUPING_STRATEGY)
    public void setGroupStrategy(String groupStrategy) {
        this.groupStrategy = SequenceGroupingStrategy.valueOf(groupStrategy);
    }

    /**
     * Retrieves the current grouping strategy
     *
     * @return The current grouping strategy
     */
    public SequenceGroupingStrategy getGroupStrategy() {
        return this.groupStrategy;
    }

    /**
     * Default constructor. Build a TokenSequence2FeatureVectorPipe pipe with
     * the default configuration values
     */
    public TokenSequence2FeatureVectorPipe() {
        this(SequenceGroupingStrategy.valueOf(DEFAULT_GROUPING_STRATEGY));
    }

    /**
     * Creates a TokenSequence2FeatureVectorPipe using an specific grouping
     * strategy
     *
     * @param groupStrategy The selected grouping strategy
     */
    public TokenSequence2FeatureVectorPipe(SequenceGroupingStrategy groupStrategy) {
        super(new Class<?>[]{StringBuffer2TokenSequencePipe.class}, new Class<?>[0]);
        this.groupStrategy = groupStrategy;
    }

    /**
     * Converts a tokenSequence in a FeatureVector, with the token and the
     * number of times that token appears in tokenSequence
     *
     * @param tokenSequence
     * @return A featureVector with the token and the number of times that a
     * token appears in tokenSequence
     */
    private FeatureVector countMatches(TokenSequence tokenSequence) {
        Map<String, Double> featureVector = new HashMap<>();
        try {
            for (int i = 0; i < tokenSequence.size(); i++) {
                String token = tokenSequence.getToken(i);
                if (featureVector.get(token) == null) {
                    featureVector.put(token, 1d);
                } else {
                    Double appearanceNumber = featureVector.get(token);
                    featureVector.put(token, appearanceNumber + 1d);
                }
            }
        } catch (Exception e) {
            logger.warn("[COUNT MATCHES] " + e.getMessage());
        }
        return new FeatureVector(featureVector);
    }

    /**
     * Process an Instance. This method takes an input Instance, destructively
     * transforming data from TokenSequence to FeatureVector. This is the method by which all
     * pipes are eventually run.
     *
     * @param carrier Instance to be processed.
     * @return The processed instance
     */
    @Override
    public Instance pipe(Instance carrier) {
        Map<String, Double> featureVector;
        FeatureVector featureVectorCountMatches;

        try {
            TokenSequence tokenSequence = (TokenSequence) carrier.getData();

            switch (groupStrategy) {
                case COUNT:
                    // Generate a featureVector with text and text appearance number in tokenSequence
                    featureVectorCountMatches = countMatches(tokenSequence);
                    carrier.setData(featureVectorCountMatches);

                    break;
                case BOOLEAN:
                    // Generate a featureVector with text and 0/1 if this text is or not in tokenSequence
                    featureVector = new HashMap<>();
                    for (int i = 0; i < tokenSequence.size(); i++) {
                        String token = tokenSequence.getToken(i);
                        if (featureVector.get(token) == null) {
                            featureVector.put(token, 1d);
                        }
                    }
                    carrier.setData(new FeatureVector(featureVector));
                    break;
                case FREQUENCY:
                    // Generate a featureVector with text and text appearance frequency in tokenSequence
                    featureVectorCountMatches = countMatches(tokenSequence);
                    Map<String, Double> tokens = featureVectorCountMatches.getFeatures();
                    int countTokens = tokens.size();
                    tokens.entrySet().forEach((entry) -> {
                        Double frequency = entry.getValue() / countTokens;
                        tokens.put(entry.getKey(), frequency);
                    });
                    carrier.setData(featureVectorCountMatches);

                    break;
            }

        } catch (Exception e) {
            logger.warn("[PIPE] " + e.getMessage());
        }
        return carrier;
    }
}
