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
import java.io.File;
import org.bdp4j.pipe.AbstractPipe;
import org.bdp4j.pipe.PipeParameter;
import org.bdp4j.pipe.TeePipe;
import org.bdp4j.types.Instance;
import org.bdp4j.util.EBoolean;

import org.bdp4j.pipe.Pipe;
import org.bdp4j.util.CSVDatasetWriter;
import org.bdp4j.util.Configurator;

/**
 * This pipe parses Instances to csv format. It can be for showing it on
 * terminal or exporting it to .csv file. The resulting CSV could be readed in R
 * using out &lt;- read.csv("output.csv",header = TRUE, sep=";", encoding =
 * "UTF-8", skipNul = TRUE, stringsAsFactors = FALSE )
 *
 * @author Yeray Lage Freitas
 * @author María Novo
 */
@AutoService(Pipe.class)
@TeePipe()
public class TeeCSVFromStringBufferPipe extends AbstractPipe {

    /**
     * Indicates the output filename/path for CSV storing
     */
    private String output;

    /**
     * Indicates if the data should be saved
     */
    private boolean saveData;

    /**
     * The default value for save Data
     */
    public static final String DEFAULT_SAVEDATA = "yes";

    /**
     * The default value for the output file
     */
    public static final String DEFAULT_OUTPUT_FILE = "output.csv";

    /**
     * Default constructor. Build a TeeCSVFromStringBufferPipe pipe with the default configuration
     * values
     */
    public TeeCSVFromStringBufferPipe() {
        this(DEFAULT_OUTPUT_FILE, true);
    }

    /**
     * Build a TeeCSVFromStringBufferPipe using the specified output directory
     * and the default value for saveData
     *
     * @param output The filename/path for the output file
     */
    public TeeCSVFromStringBufferPipe(String output) {
        super(new Class<?>[0], new Class<?>[0]);

        this.output = Configurator.getLastUsed().getProp(Configurator.OUTPUT_FOLDER) + System.getProperty("file.separator")+ output;
        File f = new File(this.output);
//        if (f.exists()) {
//            f.delete();
//        }
        this.dataset = new CSVDatasetWriter(this.output);
    }

    /**
     * Build a TeeCSVFromStringBufferPipe using the specified output directory
     * and the value for saveData
     *
     * @param output The filename/path for the output file
     * @param saveData tells if the data should be also saved in CSV
     */
    public TeeCSVFromStringBufferPipe(String output, boolean saveData) {
        super(new Class<?>[0], new Class<?>[0]);

        this.output = Configurator.getLastUsed().getProp(Configurator.OUTPUT_FOLDER) + System.getProperty("file.separator")+ output;
        File f = new File(this.output);
//        if (f.exists()) {
//            f.delete();
//        }
        this.dataset = new CSVDatasetWriter(this.output);
        this.setSaveData(saveData);
    }

    /**
     * Csv Dataset to store data
     */
    CSVDatasetWriter dataset = null;

    /**
     * Number of properties
     */
    int nprops = 0;

    /**
     * Length of the dictionary
     */
    int dictLength = 0;

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

    private static boolean contains(String[] arr, String targetValue) {
        for (String s : arr) {
            if (s.equals(targetValue)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Set the output fileName to store the CSV contents
     *
     * @param output The filename/filepath to store the CSV contents
     */
    @PipeParameter(name = "output", description = "Indicates the output filename/path for saving CSV", defaultValue = DEFAULT_OUTPUT_FILE)
    public void setOutput(String output) {
        this.dataset.flushAndClose();
        this.output = Configurator.getLastUsed().getProp(Configurator.OUTPUT_FOLDER) + System.getProperty("file.separator")+ output;
        File f = new File(this.output);
//        if (f.exists()) {
//            f.delete();
//        }
        this.dataset.flushAndClose();
        this.dataset = new CSVDatasetWriter(this.output);
    }

    /**
     * Returns the filename where the CSV contents will be stored
     *
     * @return the filename/filepath where the CSV contents will be stored
     */
    public String getOutput() {
        return this.output;
    }

    /**
     * Indicates if the data of the instance should be also saved in the CSV
     * file
     *
     * @param saveData True if the data should be also saved in the CSV
     */
    public void setSaveData(boolean saveData) {
        this.saveData = saveData;
    }

    /**
     * Indicates if the data of the instance should be also saved in the CSV
     * file (but from string)
     *
     * @param saveData "true" if the data should be also saved in the CSV
     */
    @PipeParameter(name = "saveData", description = "Indicates if the data should be saved or not", defaultValue = DEFAULT_SAVEDATA)
    public void setSaveData(String saveData) {
        this.saveData = EBoolean.parseBoolean(saveData);
    }

    /**
     * Checks whether the data should be saved to the CSV file or not
     *
     * @return true if the data should be also saved in CSV
     */
    public boolean getSaveData() {
        return this.saveData;
    }

    /**
     * Process an Instance. This method takes an input Instance, destructively
     * modifies it in some way, and returns it. This is the method by which all
     * pipes are eventually run.
     *
     * @param carrier Instance to be processed.
     * @return The processed instance
     */
    @Override
    public Instance pipe(Instance carrier) {
        //Ensure the columns of the dataset fits with the instance
        if (dataset.getColumnCount() == 0) {
            String columnsToAdd[] = new String[3 + carrier.getPropertyList().size()];
            Object defaultValues[] = new Object[3 + carrier.getPropertyList().size()];
            columnsToAdd[0] = "id";
            defaultValues[0] = "0";
            columnsToAdd[1] = "data";
            defaultValues[0] = "";
            int j = 2;

            for (String i : carrier.getPropertyList()) {
                columnsToAdd[j] = i;
                defaultValues[j] = "";
                j++;
            }

            columnsToAdd[j] = "target";
            defaultValues[j] = "";
            dataset.addColumns(columnsToAdd, defaultValues);
        } else if (dataset.getColumnCount() != (carrier.getPropertyList().size() + 3)) {
            String currentProps[] = dataset.getColumnNames();
            carrier.getPropertyList().stream().filter((prop) -> (!contains(currentProps, prop))).forEachOrdered((prop) -> {
                dataset.insertColumnAt(prop, "0", dataset.getColumnCount() - 1);
            });
        }

        //Create and add the new row
        Object newRow[] = new Object[carrier.getPropertyList().size() + 3];
        newRow[0] = carrier.getName();
        newRow[1] = (StringBuffer) carrier.getData();
        int i = 2;
        for (Object current : carrier.getValueList()) {
            newRow[i] = current;
            i++;
        }
        newRow[newRow.length - 1] = carrier.getTarget();
        dataset.addRow(newRow);

        //If isLast on the current burst close the dataset
        if (isLast()) {
            dataset.flushAndClose();
        }
        
        return carrier;

    }
}
