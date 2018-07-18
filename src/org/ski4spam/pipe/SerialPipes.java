/* Copyright (C) 2002 Univ. of Massachusetts Amherst, Computer Science Dept.
   This file is part of "MALLET" (MAchine Learning for LanguagE Toolkit).
   http://www.cs.umass.edu/~mccallum/mallet
   This software is provided under the terms of the Common Public License,
   version 1.0, as published by http://www.opensource.org.  For further
   information, see the file `LICENSE' included with this distribution. */
package org.ski4spam.pipe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ski4spam.ia.types.Instance;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;

/**
 * Convert an instance through a sequence of pipes.
 *
 * @author Andrew McCallum <a href="mailto:mccallum@cs.umass.edu">mccallum@cs.umass.edu</a>
 */


public class SerialPipes extends Pipe implements Serializable {
    private String inputType = "";
    private String outputType = "";

    private static final Logger logger = LogManager.getLogger(SerialPipes.class);

    public String getInputType() {
        return inputType;
    }

    public String getOutputType() {
        return outputType;
    }

    /**
     * Serial version UID
     */
    private static final long serialVersionUID = 2530269803030314901L;

    /**
     * Pipes
     */
    private ArrayList<Pipe> pipes;

    public SerialPipes() {
        this.pipes = new ArrayList<Pipe>();
    }

    public SerialPipes(Object[] pipes) {
        this((Pipe[]) pipes);
    }

    public SerialPipes(Pipe[] pipes) {
        this.pipes = new ArrayList<Pipe>(pipes.length);

        //System.out.println ("SerialPipes init this = "+this);
        for (Pipe pipe : pipes) this.add(pipe);
    }


    public SerialPipes(ArrayList<Pipe> pipeList //added by Fuchun
    ) {
        this.pipes = new ArrayList<Pipe>(pipeList.size());

        for (Pipe aPipeList : pipeList) {
            this.add(aPipeList);
        }
    }

    public Pipe[] getPipes() {
        if (pipes == null) return new Pipe[0];
        Pipe[] returnValue = new Pipe[pipes.size()];
        return pipes.toArray(returnValue);
    }

    /**
     * Set the pipes used
     *
     * @param pipes pipes used
     */

    public void setPipes(Pipe[] pipes) {
        this.pipes = new ArrayList<Pipe>(pipes.length);

        //System.out.println ("SerialPipes init this = "+this);
        for (Pipe pipe : pipes) this.add(pipe);
    }

    /**
     * Returns the current Pipe
     *
     * @return current Pipe
     */

    public SerialPipes getPipe() {
        return this;
    }

    //public ArrayList<Pipe> getPipes() { //added by Fuchun
    //    return pipes;
    //}

    public void setTargetProcessing(boolean lookForAndProcessTarget) {
        super.setTargetProcessing(lookForAndProcessTarget);

        for (Pipe pipe : pipes) pipe.setTargetProcessing(lookForAndProcessTarget);
    }

    public void add(Pipe pipe) {
        if (!pipes.isEmpty()) {
            Pipe last = pipes.get(pipes.size() - 1);
            if (checkCompatibility(last, pipe)) {
                logger.info("[PIPE ADD] Good compatibility between Pipes.");
                pipe.setParent(this);
                pipes.add(pipe);

                if (inputType.equals("")) {
                    // If first Pipe hasn't inputType
                    inputType = getInputType(pipe.getClass().getDeclaredAnnotations()[0]);
                }

                if (pipe.getClass().getDeclaredAnnotations()[0].toString().contains("TargetAssigning") && pipes.size() > 1) {
                    Annotation a = pipes.get(pipes.size() - 2).getClass().getDeclaredAnnotations()[0];
                    outputType = getOutputType(a);
                } else {
                    if (pipe.getClass().getDeclaredAnnotations()[0].toString().contains("TransformationPipe")) {
                        outputType = getOutputType(pipe.getClass().getDeclaredAnnotations()[0]);
                    } else {
                        outputType = getInputType(pipe.getClass().getDeclaredAnnotations()[0]);
                    }
                }
            } else {
                logger.error("[PIPE ADD] BAD compatibility between Pipes.");
                System.exit(0);
            }
        } else {
            // If first Pipe
            pipe.setParent(this);
            pipes.add(pipe);
            if (!pipe.getClass().getDeclaredAnnotations()[0].toString().contains("TargetAssigning")) {
                inputType = getInputType(pipe.getClass().getDeclaredAnnotations()[0]);
            }
        }
    }

    private boolean checkCompatibility(Pipe p1, Pipe p2) {
        Class<?> obj1 = p1.getClass();
        Annotation[] annotation1 = obj1.getDeclaredAnnotations();
        Class<?> obj2 = p2.getClass();
        Annotation[] annotation2 = obj2.getDeclaredAnnotations();

        if (annotation1[0].toString().contains("TargetAssigning") && pipes.size() > 1) {
            // If target assigning is the last on pipes array, we take the before one for Data type evaluation
            obj1 = pipes.get(pipes.size() - 2).getClass();
            annotation1 = obj1.getDeclaredAnnotations();
        }

        if (annotation2[0].toString().contains("TargetAssigning")) {
            // If target assigning it doesn't matter the before Data type
            return true;
        } else if (annotation1[0].toString().contains("TargetAssigning") && pipes.size() == 1) {
            // If target assigning is the first on pipes array
            return true;
        } else if (annotation1[0].toString().contains("TransformationPipe")) {
            // Compare p1 output with p2 input
            TransformationPipe tp1 = obj1.getAnnotation(TransformationPipe.class);
            return tp1.outputType().equals(getInputType(annotation2[0]));
        } else if (annotation1[0].toString().contains("TeePipe")) {
            // Compare p1 input with p2 input because type is not modified
            TeePipe tp1 = obj1.getAnnotation(TeePipe.class);
            return tp1.inputType().equals(getInputType(annotation2[0]));
        } else if (annotation1[0].toString().contains("PropertyComputingPipe")) {
            // Compare p1 input with p2 input because type is not modified
            PropertyComputingPipe tp1 = obj1.getAnnotation(PropertyComputingPipe.class);
            return tp1.inputType().equals(getInputType(annotation2[0]));
        }

        return false;
    }

    private String getInputType(Annotation a) {
        return a.toString().split("inputType=")[1].split(",")[0].replace(")", "");
    }

    private String getOutputType(Annotation a) {
        return a.toString().split("outputType=")[1].split(",")[0].replace(")", "");
    }

    public Instance pipe(Instance carrier, int startingIndex) {
        carrier = getInstance(carrier, startingIndex);

        return carrier;
    }

    private Instance getInstance(Instance carrier, int startingIndex) {
        for (int i = startingIndex; i < pipes.size(); i++) {

            Pipe p = pipes.get(i);

            if (p == null) {
                logger.fatal("Pipe " + i + " is null");
                System.exit(0);
            } else {

                try {
                    if (carrier.isValid()) {
                        carrier = p.pipe(carrier);
                    } else {
                        logger.info("Skipping invalid instance " + carrier.toString());
                    }
                } catch (Exception e) {
                    logger.fatal("Exception caught on pipe " + i + " (" + p.getClass().getName() + "). " + e.getMessage() + " while processing " + carrier.toString());
                    e.printStackTrace(System.err);
                    System.exit(0);
                }
            }
        }
        return carrier;
    }

    // Call this version when you are not training and don't want conjunctions to mess up the decoding.
    public Instance pipe(Instance carrier, int startingIndex,
                         boolean growAlphabet) {
        System.out.print("*");
        carrier = getInstance(carrier, startingIndex);
        return carrier;
    }

    public void removePipe(int index) {

        try {
            pipes.remove(index);
        } catch (Exception e) {
            System.err.println(
                    "Error removing pipe. Index = " + index + ".  " +
                            e.getMessage());
        }
    }

    //added by Fuchun Jan.30, 2004
    public void replacePipe(int index, Pipe p) {

        try {
            pipes.set(index, p);
        } catch (Exception e) {
            System.err.println(
                    "Error replacing pipe. Index = " + index + ".  " +
                            e.getMessage());
        }
    }

    public int size() {

        return pipes.size();
    }

    public Pipe getPipe(int index) {
        Pipe retPipe = null;

        try {
            retPipe = pipes.get(index);
        } catch (Exception e) {
            System.err.println(
                    "Error getting pipe. Index = " + index + ".  " +
                            e.getMessage());
        }

        return retPipe;
    }

    public Instance pipe(Instance carrier) {
        return pipe(carrier, 0);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (Pipe pipe : pipes) sb.append(pipe.toString()).append(",");

        return sb.toString();
    }

}
