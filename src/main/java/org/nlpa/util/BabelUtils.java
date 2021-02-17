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
package org.nlpa.util;

import org.bdp4j.util.Pair;
import org.nlpa.util.babelnet.KnowledgeBase;
import org.nlpa.util.babelnet.KnowledgeGraph;
import org.nlpa.util.babelnet.KnowledgeGraphFactory;
import org.nlpa.util.babelnet.KnowledgeGraphScorer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.uniroma1.lcl.babelfy.core.Babelfy;
import it.uniroma1.lcl.babelfy.commons.annotation.SemanticAnnotation;
import it.uniroma1.lcl.jlt.util.Language;

import it.uniroma1.lcl.babelnet.BabelSynset;
import it.uniroma1.lcl.babelnet.BabelNet;
import it.uniroma1.lcl.babelnet.BabelNetQuery;
import it.uniroma1.lcl.babelnet.BabelSynsetID;
import it.uniroma1.lcl.babelnet.BabelSynsetRelation;
import it.uniroma1.lcl.babelnet.data.BabelPointer;

import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.io.IOException;

import it.uniroma1.lcl.jlt.ling.Word;
import it.uniroma1.lcl.jlt.util.ScoredItem;


/**
 * This class encapsulates all required information to support Babelfy and
 * Babelnet queryies
 *
 * @author Iñaki Velez de Mendizabal
 */
public class BabelUtils {

    public static int babelfyQueries = 0;

    /**
     * For logging purpopses
     */
    private static final Logger logger = LogManager.getLogger(BabelUtils.class);

    /**
     * A instance of this class to implement a singleton pattern
     */
    private static BabelUtils bu = null;

    /**
     * An instance of Babelfy object required to query Babelfy
     */
    private Babelfy bfy;

    /**
     * An instance of BabelNet object required to query BabelNet
     */
    private BabelNet bn;

    /**
     * A stop synset to navigate in Babelnet hierarchy. The synset means entity
     */
    private static String stopSynset = "bn:00031027n";

    /**
     * String limit for babelfy queries
     */
    public static final int MAX_BABELFY_QUERY = 3000;

    /**
     * The private default constructor for this class
     */
    private BabelUtils() {
        bfy = new Babelfy();
        bn = BabelNet.getInstance();
    }

    /**
     * Achieves the default instance of BabelUtils
     *
     * @return a instance of this class
     */
    public static BabelUtils getDefault() {
        if (bu == null) {
            bu = new BabelUtils();
        }
        return bu;
    }

    /**
     * Determines whether a term is included in Babelnet or not
     *
     * @param term The term to check
     * @param lang The language in which the term is written
     * @return true if the term is included in Babelnet ontological dictionary
     */
    public boolean isTermInBabelNet(String term, String lang) {
        if (lang.trim().equalsIgnoreCase("UND")) {
            logger.error("Unable to query Babelnet because language is not found.");
            return false;
        }
        int resultNo = 0;
        try {
            BabelNetQuery query = new BabelNetQuery.Builder(term).from(Language.valueOf(lang)).build();
            List<BabelSynset> byl = bn.getSynsets(query);
            resultNo = byl.size();
        } catch (Exception e) {
            logger.error("Unable to query Babelnet: " + e.getMessage());
        }
        return (resultNo > 0);
    }

    /**
     * Determines whether a synset is included in Babelnet or not
     *
     * @param synsetToCheck The Synset to check
     * @param textToLink The word which corresponds with the Synset in Babelfy.
     * This word is provided only to create a log report.
     * @return true if is possible to obtain information about synset in
     * Babelnet, so the synset is included in Babelnet ontological dictionary.
     */
    public boolean checkSynsetInBabelnet(String synsetToCheck, String textToLink) {
        try {
            //Tray to obtain some information about the synset. If is not possible it generates a exception
            bn.getSynset(new BabelSynsetID(synsetToCheck)).toString();
            return true;

        } catch (Exception e) {
            logger.error("The text [" + textToLink + "] obtained in Babelfy as [" + synsetToCheck + "] does not exists in Babelnet. " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets an hypernym of a synset that is n levels above
     *
     * @param synsetToScale The Synset to search its hypernym.
     * @param levels The number of levels to be scaled.
     * @return a string with the hypernym synset ID
     */
    public String getSynsetHypernymFromLevel(String synsetToScale, int levels) {
        String tmpHypernym = synsetToScale;
        try {
            List<BabelSynsetRelation> elementsInAnyHypernymPointer, elementsInHypernymPointer;
            BabelSynset by;

            for (int l = 0; l < levels; l++) {
                by = bn.getSynset(new BabelSynsetID(tmpHypernym));
                elementsInAnyHypernymPointer = by.getOutgoingEdges(BabelPointer.ANY_HYPERNYM);
                elementsInHypernymPointer = by.getOutgoingEdges(BabelPointer.HYPERNYM);
                // If HYPERNYM returns values, it takes first synset and add to tmpHypernym
                if (elementsInHypernymPointer.size() >= 1) {
                    tmpHypernym = elementsInHypernymPointer.get(0).getBabelSynsetIDTarget().toString();
                } // else if ANY_HYPERNYM returns values, it takes first synset and add to tmpHypernym
                else if (elementsInAnyHypernymPointer.size() >= 1) {
                    tmpHypernym = elementsInAnyHypernymPointer.get(0).getBabelSynsetIDTarget().toString();
                }
            }
        } catch (Exception e) {
            logger.error("Hypernym search problem. The synset " + synsetToScale + " does not exists in Babelnet." + e.getMessage());
        }
        return tmpHypernym;
    }

    /**
     * Returns a list with all the hypernyms of a Synset until "entity" element.
     * If synset does not hypernyms, the list only returns the original synset.
     *
     * @param synsetToScale The Synset to search its hypernyms.
     * @return the list with the synset and all of hypernyms
     */
    public List<String> getAllHypernyms(String synsetToScale) {
        List<String> allHypernymsList = new ArrayList<>();
        try {
            List<BabelSynsetRelation> elementsInAnyHypernymPointer, elementsInHypernymPointer;
            BabelSynset by;
            do {
                //Meto o no meto su hiperonimo si solo tiene uno?
                allHypernymsList.add(synsetToScale);
                by = bn.getSynset(new BabelSynsetID(synsetToScale));
                elementsInAnyHypernymPointer = by.getOutgoingEdges(BabelPointer.ANY_HYPERNYM);
                elementsInHypernymPointer = by.getOutgoingEdges(BabelPointer.HYPERNYM);
                // If HYPERNYM returns values, it takes first synset and add to tmpHypernym
                if (elementsInHypernymPointer.size() >= 1) {
                    synsetToScale = elementsInHypernymPointer.get(0).getBabelSynsetIDTarget().toString();
                } // else if ANY_HYPERNYM returns values, it takes first synset and add to tmpHypernym
                else if (elementsInAnyHypernymPointer.size() >= 1) {
                    synsetToScale = elementsInAnyHypernymPointer.get(0).getBabelSynsetIDTarget().toString();
                }

            } while (!synsetToScale.equals(stopSynset) && !allHypernymsList.contains(synsetToScale));
        } catch (Exception e) {
            logger.error("Hypernym search problem. The synset " + synsetToScale + " does not exists in Babelnet." + e.getMessage());
        }
        return allHypernymsList;
    }

    /**
     * Returns a Map with Synsets and their first hypernym from BabelNet. Only
     * builds a pair if synset hypernym exists.
     *
     * @param originalSynsetList The synsets list to obtain hypernyms
     * @return A Map with pairs of synset as key and hypernym as value
     */
    public Map<String, String> getHypernymsFromBabelnet(List<String> originalSynsetList) {
        List<BabelSynsetRelation> elementsInAnyHypernymPointer, elementsInHypernymPointer;
        String hypernym;
        Map<String, String> synsetHypernymMap = new HashMap<>();

        for (String synsetListElement : originalSynsetList) {
            try {
                BabelSynset by = bn.getSynset(new BabelSynsetID(synsetListElement));
                elementsInAnyHypernymPointer = by.getOutgoingEdges(BabelPointer.ANY_HYPERNYM);
                elementsInHypernymPointer = by.getOutgoingEdges(BabelPointer.HYPERNYM);
                // If HYPERNYM returns values, it takes first and add to synsetHypernymMap
                if (elementsInHypernymPointer.size() >= 1) {
                    hypernym = elementsInHypernymPointer.get(0).getBabelSynsetIDTarget().toString();
                    synsetHypernymMap.put(synsetListElement, hypernym);
                } // else if ANY_HYPERNYM returns values, it takes first and add to synsetHypernymMap
                else if (elementsInAnyHypernymPointer.size() >= 1) {
                    hypernym = elementsInAnyHypernymPointer.get(0).getBabelSynsetIDTarget().toString();
                    synsetHypernymMap.put(synsetListElement, hypernym);
                }
            } catch (Exception e) {
                logger.error("Hypernym search problem. The synset " + synsetListElement + " does not exists in Babelnet." + e.getMessage());
            }
        }
        return synsetHypernymMap;
    }

    /**
     * Returns true if synsetOnTop if hypernym of synsetToCheck.
     *
     * @param synsetToCheck The Synset to be scaled to try to reach the
     * hypernym.
     * @param synsetOnTop The hypernym
     * @return True if synsetOnTop if hypernym of synsetToCheck.
     */
    public boolean isSynsetHypernymOf(String synsetToCheck, String synsetOnTop) {
        String scaledSynsetToCheck;

        if (synsetToCheck.equals(synsetOnTop)) {
            return false;
        } else {
            boolean isHypernym = false;
            do {
                scaledSynsetToCheck = bu.getSynsetHypernymFromLevel(synsetToCheck, 1);
                if (scaledSynsetToCheck.equals(synsetToCheck)) {
                    return false;
                } else {
                    if (scaledSynsetToCheck.equals(synsetOnTop)) {
                        return true;
                    } else {
                        synsetToCheck = scaledSynsetToCheck;
                    }
                }
            } while (!isHypernym && !synsetToCheck.equals(stopSynset));
            return isHypernym;
        }
    }

    /**
     * Build a list of sysntets from a text
     *
     * @param fixedText The text to be transformed into synsets
     * @param lang The language to identify the synsets
     * @return A vector of synsets. Each synset is represented in a pair (S,T)
     * where S stands for the synset ID and T for the text that matches this
     * synset ID
     */
    public ArrayList<Pair<String, String>> buildSynsetSequence(String fixedText, String lang) {
        // This is an arraylist of entries to check for duplicate results and nGrams
        ArrayList<BabelfyEntry> nGrams = new ArrayList<>();
        List<SemanticAnnotation> bfyAnnotations = new ArrayList<>();
        // boolean solved = false;
        String subtexts[] = new String[0];

        // Split text in 3500 (MAX_BABELFY_QUERY) characters string for querying
        String remain = new String(fixedText);
        List<String> parts = new ArrayList<>();
        while (remain.length() > MAX_BABELFY_QUERY) {
            int splitPos = remain.lastIndexOf('.', MAX_BABELFY_QUERY - 1); // Try to keep phrases in the same part
            if (splitPos == -1) {
                splitPos = remain.lastIndexOf(' ', MAX_BABELFY_QUERY - 1); // but at least try to keep words
            }
            if (splitPos == -1) {
                splitPos = MAX_BABELFY_QUERY - 1; // if this is imposible lets with the max length
            }
            parts.add(remain.substring(0, splitPos + 1));
            remain = remain.substring(splitPos + 1);
        }
        parts.add(remain);
        subtexts = parts.toArray(subtexts);

        // Text is not splitted
        int currentSubtext = 0;
        while (currentSubtext < subtexts.length) {
            try {
                logger.info("We are going to query LANG: " + lang + " Number of previous queries: " + babelfyQueries);

                //Cambio para que todas las consultas se hagan en Inglés restaurar antes del commit
                bfyAnnotations.addAll(bfy.babelfy(subtexts[currentSubtext], Language.valueOf(lang))); 
                // bfyAnnotations.addAll(bfy.babelfy(subtexts[currentSubtext], Language.valueOf("EN")));
                //Fin del cambio
                babelfyQueries++;
                currentSubtext++;
            } catch (RuntimeException e) {
                //Comentar antes de hacer commit
//                System.out.println(e.getMessage());
//                e.printStackTrace();
                //Fin de comentar
                if (e.getMessage().equals("Your key is not valid or the daily requests limit has been reached. Please visit http://babelfy.org.")) {
                    // Wait until 01:01:01 of the next day (just after midnigth)
                    Calendar c = Calendar.getInstance();
                    c.add(Calendar.DAY_OF_MONTH, 1);
                    c.set(Calendar.HOUR_OF_DAY, 1); // Wait for an hour and a minute for the actualization of babelcoins
                    c.set(Calendar.MINUTE, 1);
                    c.set(Calendar.SECOND, 1);
                    long midnight = c.getTimeInMillis();

                    long now = System.currentTimeMillis();

                    long millis = midnight - now;
                    long hours = millis / (1000 * 60 * 60);
                    long minutes = (millis % (1000 * 60 * 60)) / (1000 * 60);
                    logger.info(
                            "Your key is not valid or the daily requests limit has been reached. The application will pause for "
                            + hours + "h " + minutes + "m.");
                    try {
                        Thread.sleep(millis);
                    } catch (InterruptedException ie) {
                        logger.error("Unable to sleep " + millis + ". " + ie.getMessage());
                    }
                    babelfyQueries = 0;
                } else if (e.getMessage().equals("Your are not allowed on the requested languages. Please visit http://babelfy.org.")) {
                    logger.info("Mark instance for invalidating because caugth Babelfy error: Your are not allowed on the requested languages. Please visit http://babelfy.org.");
                    
                    return null;
                } else {
                    currentSubtext++;
                }
            }
        }

        for (SemanticAnnotation annotation : bfyAnnotations) {
            int start = annotation.getCharOffsetFragment().getStart();
            int end = annotation.getCharOffsetFragment().getEnd();
            double score = annotation.getGlobalScore();
            String synsetId = annotation.getBabelSynsetID();
            String text = fixedText.substring(start, end + 1);

            if (nGrams.size() == 0) { // If this anotation is the first i have ever received
                nGrams.add(new BabelfyEntry(start, end, score, synsetId, text));
                continue;
            }

            // This is a sequential search to find previous synsets that are connected with
            // the current one
            int pos = 0;
            BabelfyEntry prevAnot = nGrams.get(pos);
            //System.out.println("start: "+start+" end: "+end);

            while ((!(start >= prevAnot.getStartIdx() && end <= prevAnot.getEndIdx()))
                    && // The current anotation is
                    // included in other previous
                    // one
                    (!(prevAnot.getStartIdx() >= start && prevAnot.getEndIdx() <= end))
                    && // A previous anotation is
                    // included in the current
                    // one
                    pos < (nGrams.size()-1))
                {
                    pos++;
                    prevAnot = nGrams.get(pos);
                    //System.out.println("current-> start:"+prevAnot.getStartIdx()+"current->end:"+prevAnot.getEndIdx());
                }

            //System.out.println("pos: "+pos+"/"+(nGrams.size()-1));
            if (start == prevAnot.getStartIdx() && end == prevAnot.getEndIdx() && score > prevAnot.getScore()) {
                //System.out.println("same text+-> other "+prevAnot.getText()+" /current: "+text);
                nGrams.set(pos, new BabelfyEntry(start, end, score, synsetId, text));
            } else if (start >= prevAnot.getStartIdx() && end <= prevAnot.getEndIdx()) { // The current anotation is included
                                                                                        // in other previous one  
                //System.out.println("currentAnotation is included in other previous one+-> other: "+prevAnot.getText()+" /current: "+text);
                              
            } else if (prevAnot.getStartIdx() >= start && prevAnot.getEndIdx() <= end) { // A previous anotation is
                                                                                        // included in the current
                                                                                        // one
                //System.out.println("other previous one includes current one+-> other: "+prevAnot.getText()+" /current: "+text);
                nGrams.set(pos, new BabelfyEntry(start, end, score, synsetId, text));
            } else {
                //System.out.println("current anotation is unique-< current: "+text);
                nGrams.add(new BabelfyEntry(start, end, score, synsetId, text)); // it it not related to nothing
                // previous
            }
        }

        // The value that will be returned
        ArrayList<Pair<String, String>> returnValue = new ArrayList<>();
        for (BabelfyEntry entry : nGrams) {
            if (checkSynsetInBabelnet(entry.getSynsetId(), entry.getText())) {
                returnValue.add(new Pair<>(entry.getSynsetId(), entry.getText()));
            }

        }
        return returnValue;
    }

    /**
     * Build a list of sysntets from a text
     *
     * @param fixedText The text to be transformed into synsets
     * @param lang The language to identify the synsets
     * @return A vector of synsets. Each synset is represented in a pair (S,T)
     * where S stands for the synset ID and T for the text that matches this
     * synset ID
     */
    public ArrayList<Pair<String, String>> buildSynsetSequence2(String fixedText, String lang) {
        //http://www.smo.uhi.ac.uk/~oduibhin/oideasra/interfaces/winbabelnet.htm
        List<Word> words=new ArrayList<>();
        StringTokenizer st=new StringTokenizer(fixedText);
        while(st.hasMoreTokens())
            words.add(new Word(st.nextToken()));

        //KnowledgeConfiguration kbConfig=KnowledgeConfiguration.getInstance();
        try{
            KnowledgeGraphFactory factory = KnowledgeGraphFactory.getInstance(KnowledgeBase.BABELNET);
            KnowledgeGraph kGraph = factory.getKnowledgeGraph(words);
            KnowledgeGraphScorer scorer=KnowledgeGraphScorer.DEGREE;
            
            Map<String, Double> scores = scorer.score(kGraph);
            for (String concept : scores.keySet()) {
                double score = scores.get(concept);
                for (Word word : kGraph.wordsForConcept(concept))
                    word.addLabel(concept, score);
            }
            for (Word word : words) {
                System.out.println("\n\t" + word.getWord() + " -- ID " + word.getId() +" => SENSE DISTRIBUTION: ");
                for (ScoredItem<String> label : word.getLabels()) {
                    System.out.println("\t [" + label.getItem() + "]:" +/*String.format(*/label.getScore()/*)*/);
                }
            }
            return null;
        }catch(IOException e){
            return null;
        }
    }

	public static void main(String args[]){
		ArrayList<Pair<String, String>> result=BabelUtils.getDefault().buildSynsetSequence2("This is an example of text", "EN");

        for (Pair<String,String> current: result){
            System.out.println(""+current.getObj1()+"->"+current.getObj2());
        }
	}

}
