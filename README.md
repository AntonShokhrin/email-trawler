# email-troller

The code takes an MS Outlook .pst file as input, for each email messages analyizes body and subject, and outputs mentions of company names.

Apache Tika framework parses the .pst file and email messages. A Tika plugin, I wrote, extracts relevant attributes of each email message. Actual email parsing is left up to Lib PST library.

Finally, I use Standford NLP library to process for Named Entity Recognition purposes. The library only seeks out organization names in the input text.

A pom.xml file simplifies compliling and running of the code.

This code needs a lot of improvements:

- loaction of input .pst file, parser settings etc... have all been hardcoded, so any change requires recompilation. A better way is to read these from properties file or command line arguments
