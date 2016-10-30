# email-troller

The code takes an MS Outlook .pst file as input, for each email messages analyzes body and subject fields, and outputs any mentions of company names. The company name output in a contex, it is surrounded by the 80 preceeding and 80 succeeding characters.

Apache Tika framework parses the .pst file and email messages. A Tika plugin, I wrote, extracts relevant attributes of each email message. Actual email parsing is left up to Lib PST library. 

The code is efficient, I was able to complete anlysis of a 6GB .pst file on an 8 year old MacBook. 

Finally, I use Standford NLP library to process for Named Entity Recognition purposes. The library only seeks out organization names in the input text.

A pom.xml file simplifies compliling and running of the code.

This code needs a lot of improvements:

- loaction of input .pst file, parser settings etc... have all been hardcoded, so any change requires recompilation. A better way is to read these from properties file or command line arguments
