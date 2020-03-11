package org.nlpa.pipe.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bdp4j.pipe.AbstractPipe;
import org.bdp4j.pipe.PipeParameter;
import org.bdp4j.pipe.PropertyComputingPipe;
import org.bdp4j.types.Instance;
import org.bdp4j.util.Pair;
import static org.nlpa.pipe.impl.GuessLanguageFromStringBufferPipe.DEFAULT_LANG_PROPERTY;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 * This pipe detects the sentiment polarity of the text using lexicons and adds
 * the polarity as an instance property. The polarity score is within the range
 * [-1.0, 1.0]. The property that stores the language of text has to exist.
 * 
 * @author Patricia Martin Perez
 */
@PropertyComputingPipe
public class ComputePolarityFromLexiconPipe extends AbstractPipe {

	/**
	 * For logging purposes
	 */
	private static final Logger logger = LogManager.getLogger(ComputePolarityFromLexiconPipe.class);
	
	/**
	 * Number of words that are modified by the word of negation that precede them
	 */
	private static final int NEGATION_WORDS_COUNT = 3;

	/**
	 * A hashset of polarities in different languages. NOTE: All JSON files (listed
	 * below) containing the lexicon
	 *
	 */
	private static final HashMap<String, HashMap<String, double[]>> htPolarities = new HashMap<>();
	private static final HashMap<String, HashMap<String, List<String>>> htNgrams = new HashMap<>();
	private static final HashMap<String, List<String>> htNegativeWords = new HashMap<>();

	static {
		for (String i : new String[] { 
				"/lexicon-json/lexicon.de.json",
				"/lexicon-json/lexicon.en.json",
				"/lexicon-json/lexicon.es.json",
				"/lexicon-json/lexicon.fr.json",
				"/lexicon-json/lexicon.it.json",
				"/lexicon-json/lexicon.ru.json"
				
		}) {

			String lang = i.substring(22, 24).toUpperCase();
			try {
				InputStream is = ComputePolarityFromLexiconPipe.class.getResourceAsStream(i);		
				JsonReader jsonReader = Json.createReader(is);
				JsonObject jsonObject = jsonReader.readObject();
				jsonReader.close();
				
				HashMap<String, double[]> dict = new HashMap<>();
				HashMap<String, List<String>> ngramsDict = new HashMap<>();

				for (String word : jsonObject.keySet()) {
					if(isNgram(word)) {
						addNgramsDict(word, ngramsDict);
					}
					
					double[] doubleArray = new double[2];	
					JsonArray jsonArray = (JsonArray) jsonObject.get(word);
					JsonValue posValue = jsonArray.get(0);
					JsonValue negValue = jsonArray.get(1);
					doubleArray[0] = new Double(posValue.toString());
					doubleArray[1] = new Double(negValue.toString());
					dict.put(word, doubleArray);
				}

				htPolarities.put(lang, dict);
				htNgrams.put(lang, ngramsDict);
				sortNgramDictionary(ngramsDict);
				
			} catch (Exception e) {
				logger.error("Exception processing: " + i + " message " + e.getMessage());
			}

		}
		
		//Negative dictionaries
		for (String i : new String[] { 
				"/lexicon-json/negative-words/NegatingWordList.de.json",
				"/lexicon-json/negative-words/NegatingWordList.en.json",
				"/lexicon-json/negative-words/NegatingWordList.es.json",
				"/lexicon-json/negative-words/NegatingWordList.fr.json",
				"/lexicon-json/negative-words/NegatingWordList.it.json",
				"/lexicon-json/negative-words/NegatingWordList.ru.json"

				
		}) {

			String lang = i.substring(46, 48).toUpperCase();
			try {
				InputStream is = ComputePolarityFromLexiconPipe.class.getResourceAsStream(i);		
				JsonReader jsonReader = Json.createReader(is);
				JsonArray array = jsonReader.readArray();
				jsonReader.close();
				
				List<String> negativeWordsList = new ArrayList<String>();

				for (JsonValue v : array) {
					String a = ((JsonString) v).getString();
					negativeWordsList.add(a);
				}

				htNegativeWords.put(lang, negativeWordsList);
				
			} catch (Exception e) {
				logger.error("Exception processing: " + i + " message " + e.getMessage());
			}

		}	

	}
	
	public static boolean isNgram(String word) {
		if(word.contains(" ")) {
			return true;
		}
		return false;
	}
	
	public static void addNgramsDict(String ngramWord, HashMap<String, List<String>> ngramsDict) {
		Set<String> ngramsKey = ngramsDict.keySet();
		String[] words = ngramWord.split(" ");
		String firstWord = words[0]; // From 'high quality' only saves 'high'
		
		if(ngramsKey.contains(firstWord)) { // 'high' exists in the ngram dictionary
			List<String> ngramsList = ngramsDict.get(firstWord);
			
			if(ngramsList != null) {  // Already found ngrams for the word 'high'
				ngramsList.add(ngramWord);
			}else { // Didn't find ngrams for the owrd 'high'
				List<String> listNgrams = new ArrayList<String>();
				listNgrams.add(ngramWord);
				ngramsDict.put(firstWord, listNgrams);
			}
		}else { // Found 'high quality' before 'high' in the dictionary
			List<String> listNgrams = new ArrayList<String>();
			listNgrams.add(ngramWord);
			ngramsDict.put(firstWord, listNgrams);
		}
	}
	
	public static void sortNgramDictionary(HashMap<String, List<String>> ngramsDict) {
		Set<String> ngramsKey = ngramsDict.keySet();
		Iterator<String> it = ngramsKey.iterator();
		while(it.hasNext()) {
			List<String> ngramList = ngramsDict.get(it.next());
			ngramList.sort(Comparator.comparing( String::length ).reversed());
		}
		
	}
	


	/**
	 * The default property name where the polarity will be stored
	 */
	public static final String DEFAULT_POLARITY_PROPERTY = "polarity";

	/**
	 * The name of the property where the language is stored
	 */
	private String langProp = DEFAULT_LANG_PROPERTY;
	/**
	 * The name of the property where the polarity is stored
	 */
	private String polarityProp = DEFAULT_POLARITY_PROPERTY;

	/**
	 * Construct a ComputePolarityFromLexicon instance
	 *
	 */
	public ComputePolarityFromLexiconPipe() {
		super(new Class<?>[] { GuessLanguageFromStringBufferPipe.class, ContractionsFromStringBufferPipe.class,
			AbbreviationFromStringBufferPipe.class, SlangFromStringBufferPipe.class, StringBufferToLowerCasePipe.class}, new Class<?>[0]);

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
	 * Indicates the datatype expected in the data attribute of an Instance after
	 * processing
	 *
	 * @return the datatype expected in the data attribute of an Instance after
	 *         processing
	 */
	@Override
	public Class<?> getOutputType() {

		return StringBuffer.class;
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

	@Override
	public Instance pipe(Instance carrier) {
		if (carrier.getData() instanceof StringBuffer) {

			String lang = (String) carrier.getProperty(langProp);
			StringBuffer data = (StringBuffer) carrier.getData();

			HashMap<String, double[]> dict = htPolarities.get(lang);
			HashMap<String, List<String>> ngramDict = htNgrams.get(lang);
			List<String> negativeWordsDict = htNegativeWords.get(lang);
			
			// When there is not a lexicon for the language
			if (dict == null) {
				carrier.setProperty(polarityProp, 0.0);
				return carrier;
			}

			double polarity = calculatePolarity(data.toString(), dict, ngramDict, negativeWordsDict);
			double polarityDecimalFormat = (double) Math.round(polarity * 100) / 100;
			carrier.setProperty(polarityProp, polarityDecimalFormat);

		} else {
			logger.error("Data should be an StringBuffer when processing " + carrier.getName() + " but is a "
					+ carrier.getData().getClass().getName());
		}
		
		return carrier;
	}
	
	/**
	 * Calculate the polarity of a text. For each sentence gets the polarity of the words.
	 *
	 * @param data the text to calculate the polarity
	 * @param dict the lexicon based on the language of the text
	 * 
	 * @return polarity of the text
	 */
	private double calculatePolarity(String data, HashMap<String, double[]> dict, HashMap<String,
			List<String>> ngramDict, List<String> negativeWordsDict) {
		double totalPolarity = 0.0d;
		double weightSentence = 0.0d;
		double totalWeightSentence = 0.0d;
		double polaritySentence = 0.0d;
		
	
		// Calculate the polarity for each sentence
		String[] sentences = {data};
		if (data.contains(".")) {
			sentences = data.split("\\.");
		} else {
			sentences[0] = data;
		}

		
		for (String sentence : sentences) {
			
			double polarityScore = 0.0d;
			double totalPolarityScore = 0.0d;
			int wordNum = 0;
			int negationWordNum = 0;
			boolean isNegation = false;
			String[] words = sentence.split(" ");

			for(int sentenceIndex=0; sentenceIndex<words.length; sentenceIndex++) {

				if (checkNegationWord(words[sentenceIndex], negativeWordsDict)) {
					isNegation = true;
					negationWordNum = 0;
				} else {
					String word = hasNgram(words, sentenceIndex, ngramDict);
					sentenceIndex+=(word.split(" ").length - 1);

					double[] polarity = dict.get(word);
					
					
					if (polarity != null) {
						
						double res = getScorePolarity(polarity[0], polarity[1]);
						if(res != 0) { 
							wordNum++; 
						}
						
						if (isNegation) {
							negationWordNum++;
							polarityScore += res;
							
							if (negationWordNum >= this.NEGATION_WORDS_COUNT || sentenceIndex == (words.length-1)) {
								totalPolarityScore += polarityScore * (-1);
								isNegation = false;
							}

						} else {
							totalPolarityScore += res;
							polarityScore = 0;
						}

					}
				}

			}
			
			if (wordNum > 0) { 
				
				weightSentence = wordNum / 5.0;
				totalWeightSentence += weightSentence;
				
				polaritySentence += (weightSentence * totalPolarityScore);
				
				totalPolarity = polaritySentence / totalWeightSentence;
			}

		}

		return totalPolarity;
	}
	
	
	/**
	 * Get the polarity of a word. Considering positive, negative and neutral 
	 * scores, the highest score is choosen.
	 *
	 * @param posScore positive score of the word
	 * @param negScore negative score of the word
	 * 
	 * @return polarity of the word
	 */
	private double getScorePolarity(double posScore, double negScore) {
		double polarityScore = 0;
		// The objectivity score
		double objScore = 1 - (posScore + negScore);

		if (objScore > (posScore + negScore)) {
			// Neutral
			polarityScore = 0;
		} else if (posScore > negScore) {
			// Positive
			polarityScore = posScore;
		} else {
			// Negative
			polarityScore = negScore * (-1);
		}

		return polarityScore;
	}
	
	
	/**
	 * Checks if the word is a negation word.
	 *
	 * @param word
	 * @param negativeWordsDict a dictionary with negative words of a specific language
	 * 
	 * @return true or false if the word is use for negation.
	 */
	private boolean checkNegationWord(String word, List<String> negativeWordsDict) {		
		if (negativeWordsDict != null && negativeWordsDict.contains(word)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Checks if there is a ngram in the sentence beginning with the given word.
	 *
	 * @param sentenceWords the words of the sentence
	 * @param sentenceIndex the current index of the sentence
	 * @param ngramsDict a dictionary with the ngrams of the given word
	 * 
	 * @return the ngram that contains the sentence. If there isn't any ngram, returns the given word
	 */
	public String hasNgram(String[] sentenceWords, int sentenceIndex, HashMap<String, List<String>> ngramsDict) {
		String word = sentenceWords[sentenceIndex]; // original
		
		if(ngramsDict != null) {
		List<String> ngramList = ngramsDict.get(sentenceWords[sentenceIndex]);	
			if(ngramList != null){
					for(String ngram : ngramList) {
						String[] ngramWords = ngram.split(" ");
						String[] wordsSlice = Arrays.copyOfRange(sentenceWords, sentenceIndex, sentenceIndex + ngramWords.length);
						if (Arrays.equals(ngramWords, wordsSlice)) {
							return ngram;
						}
				}
			}
		}
		return word;
	}
	
}
