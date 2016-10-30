# email-tawler

Synopsis: A Named Entity Recognition on content of MS Outlook PST files

The code takes an MS Outlook .pst file as input, for each email messages it analyzes body and subject fields, and outputs any mentions of company names. The company name appears in a contex, it is surrounded by the 80 preceeding and 80 succeeding characters.

Apache Tika framework parses the .pst file and email messages. A Tika plugin, I wrote, extracts relevant attributes of each email message. Actual email parsing is left up to Lib PST library. 
 
Finally, I use Stanford NLP library for Named Entity Recognition purposes. Only organization extracting capability of the Stanford NLP library are being used. Recognizing dates, currency and other etitites supported by the library is possible by passing appropirate setting in the StanfordNERWrapper.java file.

A pom.xml file simplifies compliling and running of the code.

The code is efficient, I was able to complete anlysis of a 6GB .pst file on an 8 year old MacBook with 4GB of memory. Having said that, it is a proof of concept and requires a LOT of improvements:

- loaction of input .pst file, parser settings etc... have all been hardcoded, so any change requires recompilation. A better way is to read these from properties file or command line arguments
