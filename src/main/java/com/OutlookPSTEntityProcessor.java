package com;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pff.PSTAttachment;
import com.pff.PSTFile;
import com.pff.PSTFolder;
import com.pff.PSTMessage;
import com.pff.PSTObject;
import com.pff.PSTException;
import com.pff.PSTRecipient;

import java.util.Vector;
import java.util.Map;
import java.util.Locale;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;
import java.util.Date;
import java.util.TimeZone;

import java.text.SimpleDateFormat;

public class OutlookPSTEntityProcessor {
    
    private static final Logger LOG = LoggerFactory.getLogger(OutlookPSTEntityProcessor.class);
    private String parser;
    static final String DEFAULT_PARSER = "org.apache.tika.parser.mbox.OutlookPSTParserr";
    private String filePath;
    private PSTFile theFile;
    private Vector<Long> flatEmails;
    private Iterator<Long> emailIDiter;
    
    private static final String MESSAGE_ID = "messageId";
    private static final String SUBJECT = "subject";
    private static final String FROM_NAME = "fromName";
    private static final String FROM_EMAIL = "fromEmail";
    private static final String SENT_DATE = "sentDate";
    private static final String TO_NAME = "toName";
    private static final String TO_EMAIL = "toEmail";
    private static final String CC_NAME = "ccName";
    private static final String CC_EMAIL = "ccEmail";
    private static final String BCC_NAME = "bccName";
    private static final String BCC_EMAIL = "bccEmail";
    private static final String CONTENT = "content";
    
    public void init(String pstFileLocation) {

        /* /Users/antonshokhrin/Development/solr/input_data/test_file_mini.pst */
        flatEmails = new Vector<>();
        filePath=pstFileLocation;
        try {
            theFile = new PSTFile(filePath);
            processFolder(theFile.getRootFolder());
        } catch (Exception e) {
            String errMsg = String.format(Locale.ENGLISH,
                                          "Failed to load file: %s", filePath);
            LOG.error(errMsg, e);
        } /* This needs to relocate to distructor 
           finally {
            if (theFile != null && theFile.getFileHandle() != null) {
                try{
                    theFile.getFileHandle().close();
                } catch (IOException e) {
                    //swallow closing exception
                }
            }
        }*/
        emailIDiter=flatEmails.iterator();
    }
    
    private void processFolder(PSTFolder folder){

        // go through the folders...
        if (folder.hasSubfolders()) {
            Vector<PSTFolder> childFolders = null;
            try {
                childFolders = folder.getSubFolders();
            } catch (Exception e) {
                LOG.error("Failed to extract message", e);
            }
            
            for (PSTFolder childFolder : childFolders) {
                processFolder(childFolder);
            }
        }
        
        // and now the emails for this folder
        if (folder.getContentCount() > 0) {
            
            PSTMessage email = null;
            
            try{
                email = (PSTMessage)folder.getNextChild();
            } catch (Exception e){
                LOG.error("Failed to extract message", e);
            }
            
            while (email != null) {
                //For the time being only interested in email messages
                if (email.getDescriptorNode().itemType==2){
                    flatEmails.add(email.getDescriptorNodeId());
                } else {
                    LOG.info("Parsed message '{}', of type '{}'", email.getDescriptorNodeId(), email.getDescriptorNode().itemType);
                }
                try{
                    email = (PSTMessage)folder.getNextChild();
                } catch (Exception e){
                    LOG.error("Failed to extract message", e);
                }
            }
        }
    }
    
    public Map<String,Object> nextRow() {
        Map<String,Object> row = null;
        PSTObject eMessage = null;
        if (emailIDiter.hasNext()){
            try{
                eMessage = PSTObject.detectAndLoadPSTObject(theFile, emailIDiter.next());
                row = new HashMap<>();
                copyMessageDetails(row,eMessage);
            } catch(Exception e){
                LOG.error("Failed to return a row", e);
            }
        }
        return row;
    }
    
    private void copyMessageDetails(Map<String,Object> destination, PSTObject source){
        //Information I am interseted in indexing:
        // 1. Message body
        // 2. Subject
        // 3. Sender's name and email
        // 4. "To" recipients names and email
        // 5. Date
        // 6. Unique Message ID
        int countr = -1;
        try {
            countr = ((PSTMessage)source).getNumberOfRecipients();
        } catch (Exception e){
            LOG.error("Failed to retrieve recipients count", e);
        }
        
        destination.put(MESSAGE_ID, source.getDescriptorNodeId());
        List<String> TOEmailList = new ArrayList<>();
        List<String> TONameList = new ArrayList<>();
        List<String> CCEmailList = new ArrayList<>();
        List<String> CCNameList = new ArrayList<>();
        List<String> BCCEmailList = new ArrayList<>();
        List<String> BCCNameList = new ArrayList<>();
        
        for (int i = 0; i<countr; i++){
            try{
                PSTRecipient reciep = ((PSTMessage)source).getRecipient(i);
                
                if(reciep.getRecipientType()==PSTRecipient.MAPI_TO){
                    TOEmailList.add(reciep.getEmailAddress());
                    TONameList.add(reciep.getDisplayName());
                } else if (reciep.getRecipientType()==PSTRecipient.MAPI_CC){
                    CCEmailList.add(reciep.getEmailAddress());
                    CCNameList.add(reciep.getDisplayName());
                } else {
                    BCCEmailList.add(reciep.getEmailAddress());
                    BCCNameList.add(reciep.getDisplayName());
                }
                /*
                 LOG.info("Recipient '{}' DisplayName '{}'",i, reciep.getDisplayName());
                 LOG.info("Recipient '{}' RecipientType '{}'",i, reciep.getRecipientType());
                 LOG.info("Recipient '{}' EmailAddressType '{}'",i, reciep.getEmailAddressType());
                 LOG.info("Recipient '{}' EmailAddress '{}'",i, reciep.getEmailAddress());
                 LOG.info("Recipient '{}' RecipientFlags '{}'",i, reciep.getRecipientFlags());
                 LOG.info("Recipient '{}' RecipientOrder '{}'",i, reciep.getRecipientOrder());
                 LOG.info("Recipient '{}' SmtpAddress '{}'",i, reciep.getSmtpAddress());*/
            }catch (Exception e){
                LOG.error("Failed to retrieve recipient", e);
            }
        }
        
        if (TOEmailList.size()>0){
            destination.put(TO_EMAIL, TOEmailList);
            destination.put(TO_NAME, TONameList);
        }
        if (CCEmailList.size()>0){
            destination.put(CC_EMAIL, CCEmailList);
            destination.put(CC_NAME, CCNameList);
        }
        if (BCCEmailList.size()>0){
            destination.put(BCC_EMAIL, BCCEmailList);
            destination.put(BCC_NAME, BCCNameList);
        }
        if (((PSTMessage)source).getSentRepresentingEmailAddress()!=null){
            destination.put(FROM_NAME,((PSTMessage)source).getSentRepresentingName());
            destination.put(FROM_EMAIL,((PSTMessage)source).getSentRepresentingEmailAddress());
            //LOG.info("From Email '{}'",((PSTMessage)source).getSentRepresentingEmailAddress());
        }
        
        destination.put(SENT_DATE,getUTCDate(((PSTMessage)source).getMessageDeliveryTime()));
        destination.put(SUBJECT,((PSTMessage)source).getSubject());
        
        destination.put(CONTENT,((PSTMessage)source).getBody());
        /*
        LOG.info("MessageDeliveryTime '{}'",((PSTMessage)source).getMessageDeliveryTime());
        LOG.info("Subject '{}'",((PSTMessage)source).getSubject());
        LOG.info("MessageClass '{}'",((PSTMessage)source).getMessageClass());
        LOG.info("SentRepresentingName '{}'",((PSTMessage)source).getSentRepresentingName());
        LOG.info("SentRepresentingAddress '{}'",((PSTMessage)source).getSentRepresentingAddressType());
        LOG.info("SentRepresentingEmailAddress '{}'",((PSTMessage)source).getSentRepresentingEmailAddress());
        LOG.info("ReceivedByAddress '{}'",((PSTMessage)source).getReceivedByAddress());
        LOG.info("RcvdRepresentingName '{}'",((PSTMessage)source).getRcvdRepresentingName());
        LOG.info("OriginalSubject '{}'",((PSTMessage)source).getOriginalSubject());
        LOG.info("MessageToMe '{}'",((PSTMessage)source).getMessageToMe());
        LOG.info("MessageCcMe '{}'",((PSTMessage)source).getMessageCcMe());
        LOG.info("SentRepresentingAddrtype '{}'",((PSTMessage)source).getSentRepresentingAddrtype());
        LOG.info("OriginalDisplayBcc '{}'",((PSTMessage)source).getOriginalDisplayBcc());
        LOG.info("OriginalDisplayCc '{}'",((PSTMessage)source).getOriginalDisplayCc());
        LOG.info("OriginalDisplayTo '{}'",((PSTMessage)source).getOriginalDisplayTo());
        LOG.info("RcvdRepresentingAddrtype '{}'",((PSTMessage)source).getRcvdRepresentingAddrtype());
        LOG.info("RcvdRepresentingEmailAddress '{}'",((PSTMessage)source).getRcvdRepresentingEmailAddress());
        LOG.info("SenderEmailAddress '{}'",((PSTMessage)source).getSenderEmailAddress());
        LOG.info("DisplayBCC '{}'",((PSTMessage)source).getDisplayBCC());
        LOG.info("DisplayCC '{}'",((PSTMessage)source).getDisplayCC());
        LOG.info("DisplayTo '{}'",((PSTMessage)source).getDisplayTo());
        */
        
    }
    
    private String getUTCDate (Date in){
        final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        final SimpleDateFormat sdf = new SimpleDateFormat(ISO_FORMAT);
        final TimeZone utc = TimeZone.getTimeZone("UTC");
        sdf.setTimeZone(utc);
        return sdf.format(in);
    }
    // My observations
    // For email message the informaiton
    public static void main(String [] args)
    {
        OutlookPSTEntityProcessor mainProcess = new OutlookPSTEntityProcessor();
        mainProcess.init("/Users/antonshokhrin/Downloads/batch2.pst");
        //mainProcess.init("/Users/antonshokhrin/Development/solr/input_data/test_file_mini.pst");
        try {
            StanfordNERWrapper stWrapper = new StanfordNERWrapper();
            stWrapper.init();
            Map<String,Object> thisMessage = mainProcess.nextRow();
            while (thisMessage!=null){
                stWrapper.runNER(thisMessage.get("content").toString());
                thisMessage = mainProcess.nextRow();
            }
        } catch (Exception eee){
            LOG.error("Failed to NER the text", eee);
        }
    }
    
}