package org.nlpa.pipe.impl;

import com.google.auto.service.AutoService;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.neural.rnn.RNNCoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.CoreMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bdp4j.pipe.AbstractPipe;
import org.bdp4j.pipe.PipeParameter;
import org.bdp4j.pipe.PropertyComputingPipe;
import org.bdp4j.types.Instance;

import java.util.Properties;
import org.bdp4j.pipe.Pipe;

/**
 * This pipe adds the polarity of the text as instance property. Possible resuts
 * are included in a 5-levels Likert scale:
 * <ul>
 * <li> 0: "Very Negative"</li>
 * <li> 1: "Negative" </li>
 * <li> 2: "Neutral" </li>
 * <li> 3: "Positive" </li>
 * <li> 4: "Very Positive" </li>
 * </ul>
 * 
 * To compute the polarity we take advantage of a webservice implementing a REST API.
 * The webservice is implemented python and takes advantae of textblob library
 * to compute polarity.
 * 
 * To execute this task, the textblob web service should be executed. The service
 * can be started using  adocker container included in the /scripts directory.
 * A starting script (run-textblob-ws-sh) is provided to facilitate launching.
 * The text-blob service has been entirelly developed by Enaitz Ezpeleta 
 * (Mondragon Unibertsitatea)
 * 
 * If the service is being executed in other server, SSH tunneling (-L) can be used
 * to connect the pipe with the service. As an example the following command can be used
 * 
 * sudo ssh -L 80:textblob_ws:80 moncho@ski.4spam.group
 * 
 * @author José Ramón Méndez
 * @author Enaitz Ezpeleta
 */
@AutoService(Pipe.class)
@PropertyComputingPipe()
public class ComputePolarityFromStringBufferPipe extends AbstractPipe {
  /**
   * For logging purposes
   */
  private static final Logger logger = LogManager.getLogger(ComputePolarityFromStringBufferPipe.class);

    /**
     * Initing a StandfordCoreNLP pipeline
     */
    static StanfordCoreNLP pipeline;
    static Properties props;

    static {
        props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,parse,sentiment");
        props.setProperty("parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
        pipeline = new StanfordCoreNLP(props);
    }

    /**
     * Return the input type included the data attribute of a Instance
     *
     * @return the input type for the data attribute of the Instances processed
     */
    @Override
    public Class<?> getInputType() {
        return StringBuffer.class;
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
        return StringBuffer.class;
    }

    /**
     * The default property name to store the polarity
     */
    public static final String DEFAULT_POLARITY_PROPERTY = "polarity";

    /**
     * The property name to store the polarity
     */
    private String polProp = DEFAULT_POLARITY_PROPERTY;

    /**
     * Sets the property where the polarity will be stored
     *
     * @param polProp the name of the property for the polarity
     */
    @PipeParameter(name = "polpropname", description = "Indicates the property name to store the polarity", defaultValue = DEFAULT_POLARITY_PROPERTY)
    public void setExtensionProp(String polProp) {
        this.polProp = polProp;
    }

    /**
     * Retrieves the property name for storing the file polarity
     *
     * @return String containing the property name for storing the file polarity
     */
    public String getPolarityProp() {
        return this.polProp;
    }

    /**
     * Default constructor
     */
    public ComputePolarityFromStringBufferPipe() {
        this (DEFAULT_POLARITY_PROPERTY);
    }

    /**
     * Build a StoreFileExtensionPipe that stores the polarity of the file in
     * the property polProp
     *
     * @param polProp The name of the property to extore the file polarity
     */
    public ComputePolarityFromStringBufferPipe(String polProp) {
        super(new Class<?>[0],new Class<?>[0]);

        this.polProp = polProp;
    }

    /**
     * Process an Instance. This method takes an input Instance, destructively
     * modifies it in some way, and returns it. This is the method by which all
     * pipes are eventually run.
     *
     * @param carrier Instance to be processed.
     * @return Instancia procesada
     */
    @Override
    public Instance pipe(Instance carrier) {
        //System.out.print("Processing: " + carrier.getName() + " ... ");
        int mainSentiment = 0;
        if (carrier.getData() instanceof StringBuffer) {

            String text = carrier.getData().toString().replaceAll("[^\\p{Space}\\p{Print}]", "");
            //System.out.print("(size: " + text.length() + ") ");
            if (text != null && text.length() > 0) {
                int longest = 0;
                Annotation annotation = pipeline.process(text);
                for (CoreMap sentence : annotation
                        .get(CoreAnnotations.SentencesAnnotation.class)) {
                    Tree tree = sentence
                            .get(SentimentCoreAnnotations.SentimentAnnotatedTree.class);
                    int sentiment = RNNCoreAnnotations.getPredictedClass(tree);
                    String partText = sentence.toString();
                    if (partText.length() > longest) {
                        mainSentiment = sentiment;
                        longest = partText.length();
                    }

                }
            }
        }else{
          logger.error("Data should be an StrinBuffer when processing "+carrier.getName()+" but is a "+carrier.getData().getClass().getName());
        }
        carrier.setProperty(polProp, mainSentiment);
        //System.out.println("done: polarity=" + mainSentiment + ".");
        return carrier;
    }
}
