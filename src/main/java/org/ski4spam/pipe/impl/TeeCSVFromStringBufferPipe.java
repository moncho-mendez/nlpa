package org.ski4spam.pipe.impl;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bdp4j.ia.types.Instance;
import org.bdp4j.pipe.Pipe;
import org.bdp4j.pipe.TeePipe;
import static org.ski4spam.util.CSVUtils.getCSVSep;
import static org.ski4spam.util.CSVUtils.escapeCSV;

/**
 * This pipe parses Instances to csv format. It can be for showing it on
 * terminal or exporting it to .csv file. The resulting CSV could be readed in R
 * using out &lt;- read.csv("output.csv",header = TRUE, sep=";", encoding =
 * "UTF-8", skipNul = TRUE, stringsAsFactors = FALSE )
 *
 * @author Yeray Lage Freitas
 * @author María Novo
 */
@TeePipe()
public class TeeCSVFromStringBufferPipe extends Pipe {

    private static final Logger logger = LogManager.getLogger(TeeCSVFromStringBufferPipe.class);
    
    private String output;
    private FileWriter outputFile;
    private boolean saveData;
    private boolean isFirst;

    public TeeCSVFromStringBufferPipe() {
        this(null, false);
    }

    public TeeCSVFromStringBufferPipe(String output) {
        this(output, false);
    }

    public TeeCSVFromStringBufferPipe(String output, boolean saveData) {
        this.setOutput(output);
        this.setSaveData(saveData);
        this.isFirst = true;
    }

    @Override
    public Class getInputType() {
        return StringBuffer.class;
    }

    @Override
    public Class getOutputType() {
        return StringBuffer.class;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getOutput() {
        return this.output;
    }
    
    public void setSaveData(boolean saveData) {
        this.saveData = saveData;
    }

    public boolean getSaveData() {
        return this.saveData;
    }

    /**
     * Computes the CSV header for the instance
	  * @param withData Indicates whether include or not the data of the instance in the output
	  * @param propertyList a list of properties to be included in the CSV header
	  * @return The CSV header
     */
    public static String getCSVHeader(boolean withData, Set<String> propertyList) {
        StringBuilder builder = new StringBuilder();
        
        builder.append("id").append(getCSVSep());
        
        if (withData) builder.append("data").append(getCSVSep());
        
        for (String key : propertyList) {
            builder.append(key).append(getCSVSep());
        }
        
        builder.append("target");
        
        return builder.toString();
    }

    /**
     * Converts this instance toCSV string representation
	  * @param withData Indicates whether include or not the data of the instance in the output
	  * @param carrier Indicates the instance to be represented in the CSV line
	  * @return The string representation o the instance carrier
     */
    public static String toCSV(boolean withData, Instance carrier) {
        StringBuilder builder = new StringBuilder();
        Object name = carrier.getName();
        Object data = carrier.getData();
        Object target = carrier.getTarget();
        
        builder.append(name).append(getCSVSep());
        if (withData) builder.append(escapeCSV(data.toString()));
        
        for (Object value: carrier.getValueList()){
            builder.append(value).append(getCSVSep());
        }
        builder.append(target.toString());
        
        return builder.toString();
    }

    @Override
    public Instance pipe(Instance carrier) {
        try {
            if (isFirst) {
                outputFile = new FileWriter(output);
                this.outputFile.write(getCSVHeader(saveData, carrier.getPropertyList()));
                isFirst = false;
            }
            outputFile.write(toCSV(saveData, carrier) + "\n");
            if (isLast()){
                outputFile.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return carrier;
    }
}
