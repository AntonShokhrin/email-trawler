package com;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.*;
import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.sequences.DocumentReaderAndWriter;
import edu.stanford.nlp.util.Triple;

import java.util.List;

public class StanfordNERWrapper {
    
    private static AbstractSequenceClassifier<CoreLabel> classifier;
    
    public void init () throws Exception {
        String serializedClassifier = "/Users/antonshokhrin/Development/NERemail/test/stanford-ner-2015-12-09/classifiers/english.all.3class.distsim.crf.ser.gz";
    
        classifier = CRFClassifier.getClassifier(serializedClassifier);
        if (classifier == null){
            System.out.println("classifier null");
        } else {
            System.out.println("classifier initialized successfuly");
        }
            
    }
    
    public void runNER(String inputText) {
        // This prints out all the details of what is stored for each token
        int i=0;
        for (List<CoreLabel> lcl : classifier.classify(inputText)){
            for (CoreLabel cl : lcl) {
                String word = cl.word();
                String category = cl.get(CoreAnnotations.AnswerAnnotation.class);
                if (category.equals("ORGANIZATION")){
                    System.out.print(word + ":");
              //      System.out.println(cl.toShorterString());
                    int startIndex = cl.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class);
                    int endIndex = cl.get(CoreAnnotations.CharacterOffsetEndAnnotation.class);
                    String unescapedString = inputText.substring((startIndex-80)>0?startIndex-80:0,(endIndex+80)>inputText.length()?inputText.length():startIndex+80);
                    unescapedString=unescapedString.replaceAll(":","");
                    unescapedString=unescapedString.replaceAll("\r\n","");
                    unescapedString=unescapedString.replaceAll("\n","");
                    System.out.println(unescapedString.replaceAll(":",""));
                }
            }
        }
    }
}