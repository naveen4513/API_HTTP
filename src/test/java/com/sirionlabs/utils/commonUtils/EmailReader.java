package com.sirionlabs.utils.commonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.MimeBodyPart;
import javax.mail.search.*;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

;

/**
 * Created by shivashish on 220/6/17.
 */
public class EmailReader {


	private final static Logger logger = LoggerFactory.getLogger(EmailReader.class);
	FileUtils fileUtils;
	private String host;
	private String user;
	private String password;
	private String portNumber;
	private Boolean is_ssl;
	private Properties properties;
	private Store store;
	private Date currentDate;
	private SimpleDateFormat sdfDate;

	public EmailReader(String host, String user, String password, Boolean is_ssl, String portNumber) {
		this.host = host;
		this.user = user;
		this.password = password;
		this.portNumber = portNumber;
		this.is_ssl = is_ssl;
		this.fileUtils = new FileUtils();
		this.currentDate = new Date();
		this.sdfDate = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");

		connect(host, user, password, is_ssl, portNumber);
	}

	void connect(String host, String user, String password, Boolean is_ssl, String portNumber) {

		try {
			properties = new Properties();
			properties.put("mail.imaps.starttls.enable", "true");
			properties.put("mail.smtp.socketFactory.fallback", "true");
			Session emailSession = Session.getDefaultInstance(properties);
			//emailSession.setDebug(true);

			// create the IMAP3 store object and connect with the pop server
			if (is_ssl) {
				properties.put("mail.imaps.port", portNumber);
				store = emailSession.getStore("imaps");
			} else {
				properties.put("mail.imaps.port", portNumber);
				store = emailSession.getStore("imap");
			}

			store.connect(host, user, password);

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// this function will messsages based on folder and searchTerm
	public Message[] searchBySearchTermAndFolder(Folder folder, SearchTerm searchTerm) throws MessagingException, IOException {
		folder.open(Folder.READ_ONLY);
		Message[] msgs = {};
		try {
			msgs = folder.search(searchTerm);
			logger.info("MAILS: " + msgs.length);

			if (msgs.length == 0) {
				return null;
			}


			for (Message message : msgs) {
				try {
					logger.info("------------------------------------------");
					logger.info("DATE: " + message.getSentDate().toString());
					logger.info("FROM: " + message.getFrom()[0].toString());
					logger.info("TO: " + message.getAllRecipients()[0].toString());
					logger.info("SUBJECT: " + message.getSubject().toString());
					//logger.info("CONTENT: " + message.getContent().toString());
					logger.info("------------------------------------------");
				} catch (Exception e) {
					logger.info("No Information");
				}
			}
		} catch (MessagingException e) {
			logger.info(e.toString());
		}
		return msgs;

	}

	// this function will return all the unread messages in Inbox and limited by count
	public Message[] showUnReadMails(int count, String path) throws MessagingException {
		Folder emailFolder = store.getFolder(path);
		emailFolder.open(Folder.READ_ONLY);
		FlagTerm ft = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
		Message[] msgs = {}; // all unreadMessages
		Message[] nMsgs = new Message[count];// Top count number of unreadMessage
		try {
			msgs = emailFolder.search(ft);
			logger.info("MAILS: " + msgs.length);
			if (msgs.length > count) {
				for (int i = 0; i < count; i++) {
					nMsgs[i] = msgs[i];
				}

			} else {
				for (int i = 0; i < msgs.length; i++) {
					nMsgs[i] = msgs[i];
				}
			}

			for (Message message : nMsgs) {
				try {
					logger.info("------------------------------------------");
					logger.info("DATE: " + message.getSentDate().toString());
					logger.info("FROM: " + message.getFrom()[0].toString());
					logger.info("TO: " + message.getAllRecipients()[0].toString());
					logger.info("SUBJECT: " + message.getSubject().toString());
					logger.info("CONTENT: " + message.getContent().toString());
					logger.info("------------------------------------------");
				} catch (Exception e) {
					logger.info("No Information");
				}
			}
		} catch (MessagingException e) {
			logger.info(e.toString());
		}

		return nMsgs;
	}

	// this function will return all the mails for specified userEmail and/or specified subject
	public Message[] showMailsByUserAndSubject(String userEmail, String subject, Boolean or) throws MessagingException, IOException {
		Folder inbox = store.getFolder("INBOX");
		SearchTerm term = null;

		if (subject != null)
			term = new SubjectTerm(subject);
		if (userEmail != null) {
			RecipientStringTerm recipientTerm = new RecipientStringTerm(Message.RecipientType.TO, userEmail);
			if (term != null) {
				if (or)
					term = new OrTerm(term, recipientTerm);
				else
					term = new AndTerm(term, recipientTerm);
			} else
				term = recipientTerm;
		}

		return searchBySearchTermAndFolder(inbox, term);
	}

	// this function will return all the mails for specified userEmail and between 2 dates
	public Message[] showMailsByUserAndDate(String userEmail, Date pastDate, Date futureDate) throws MessagingException, IOException {

		logger.info("search condition : {},{},{}", userEmail, pastDate, futureDate);
		Folder inbox = store.getFolder("INBOX");
		SearchTerm term = null;
		SearchTerm olderThan = new ReceivedDateTerm(ComparisonTerm.LT, futureDate);
		SearchTerm newerThan = new ReceivedDateTerm(ComparisonTerm.GT, pastDate);
		term = new AndTerm(olderThan, newerThan);

		if (userEmail != null) {
			RecipientStringTerm recipientTerm = new RecipientStringTerm(Message.RecipientType.TO, userEmail);
			if (term != null) {
				term = new AndTerm(term, recipientTerm);
			} else
				term = recipientTerm;
		}

		return searchBySearchTermAndFolder(inbox, term);
	}

	// this function will return all the mails for specified userEmail and not older than numDays Days
	public Message[] showMailsByUserAndDays(String userEmail, int numDays) throws MessagingException, IOException {

		logger.info("search condition is {},{}", userEmail, numDays);
		Folder inbox = store.getFolder("INBOX");
		SearchTerm term = null;
		long DAY_IN_MS = 1000 * 60 * 60 * 24;
		SearchTerm olderThan = new ReceivedDateTerm(ComparisonTerm.LT, new Date());
		SearchTerm newerThan = new ReceivedDateTerm(ComparisonTerm.GT, new Date(System.currentTimeMillis() - (numDays * DAY_IN_MS))
		);
		term = new AndTerm(olderThan, newerThan);

		if (userEmail != null) {
			RecipientStringTerm recipientTerm = new RecipientStringTerm(Message.RecipientType.TO, userEmail);
			if (term != null) {
				term = new AndTerm(term, recipientTerm);
			} else
				term = recipientTerm;
		}

		return searchBySearchTermAndFolder(inbox, term);
	}

	// this function will return all the mails for specified userEmail and not older than numDays Days
	public Message[] showMailsByUserAndDaysAndSubject(String userEmail, int numDays, String subject, String emailDownloadDirectory) throws MessagingException, IOException {
		return showMailsByUserAndDaysAndSubject(userEmail, numDays, subject, false, emailDownloadDirectory, false);
	}


	// this function will return all the mails for specified userEmail and not older than numDays Days
	public Message[] showMailsByUserAndDaysAndSubject(String userEmail, int numDays, String subject, Boolean is_download, String downloadDirectory, Boolean is_downloadAttachments) throws MessagingException, IOException {

		logger.info("search condition : To->{},TimeWindow->{} days,Subject-> {}", userEmail, numDays, subject);
		Folder inbox = store.getFolder("INBOX");
		SearchTerm term = null;
		SearchTerm subjectTerm = null;
		if (subject != null)
			subjectTerm = new SubjectTerm(subject);
		long DAY_IN_MS = 1000 * 60 * 60 * 24;
		SearchTerm olderThan = new ReceivedDateTerm(ComparisonTerm.LT, new Date());
		SearchTerm newerThan = new ReceivedDateTerm(ComparisonTerm.GT, new Date(System.currentTimeMillis() - (numDays * DAY_IN_MS))
		);
		term = new AndTerm(olderThan, newerThan);

		if (userEmail != null) {
			RecipientStringTerm recipientTerm = new RecipientStringTerm(Message.RecipientType.TO, userEmail);
			if (term != null) {
				term = new AndTerm(term, recipientTerm);
			} else
				term = recipientTerm;
		}
		term = new AndTerm(term, subjectTerm);

		Message[] filtersMsgs = searchBySearchTermAndFolder(inbox, term);
		if (is_download && filtersMsgs != null && downloadDirectory != null && !downloadDirectory.isEmpty()) {
			// calling the saveMesssagesFunction
			logger.info("subject is : {}, emailDownloadDirectory is : {} ", subject, downloadDirectory);
			saveMessages(filtersMsgs, subject, downloadDirectory);
		}

		if (is_downloadAttachments && filtersMsgs != null && downloadDirectory != null && !downloadDirectory.isEmpty()) {
			// calling the downloadAttachmentsFunctions
			logger.info("Downloading the attachments of Subject :{} , And AttachmentsDownloadDirectory is : {} ", subject, downloadDirectory);
			downloadAttachments(filtersMsgs, downloadDirectory);
		}
		return filtersMsgs;
	}

	// this function will save the array of filtered messages in particular location
	public void saveMessages(Message[] msgs, String filterName, String emailDownloadDirectory) throws MessagingException, IOException {
		logger.info("Saving Messages at {} location based of Subject , timestamp and Recipient Name", emailDownloadDirectory);
		int i = 1;
		String PATH = emailDownloadDirectory;
		logger.info("Created the directory : {}  in saveMessage Function ", PATH);
		fileUtils.createDirIfNotExist(PATH);
		logger.info("Created the directory : {}  in saveMessage Function 1", PATH);

		String directoryName = PATH.concat(msgs[0].getAllRecipients()[0].toString().split("@")[0] + "_" + sdfDate.format(currentDate) + "//");
		directoryName = directoryName.replaceAll(":", "_");
		directoryName = directoryName.replaceAll("-", "_");
		logger.info("directoryName {}", directoryName);

		if (fileUtils.createDirIfNotExist(directoryName)) {

			for (Message message : msgs) {
				String strDate = sdfDate.format(message.getReceivedDate());
				String recipientFirstName = message.getAllRecipients()[0].toString().split("@")[0];
				String fileName = recipientFirstName + "_" + filterName + "_" + strDate;
				fileName = fileName.replaceAll(":", "_");
				fileName = fileName.replaceAll("-", "_");
				File file = new File(directoryName + "//" + fileName + ".html");
				if (file.exists()) {
					file = new File(directoryName + "//" + fileName + "(" + i++ + ")" + ".html");
				} else {
					file = new File(directoryName + "//" + fileName + ".html");
				}

				if (!fileUtils.saveMessageContentInFile(file, message))
					logger.error("Error Occured in saving the content of Message in Html file : {} ", file.getAbsoluteFile());
			}
		} else {
			logger.error("Not Being able to Create Directory : {} ", directoryName);
		}


	}


	// this function will save the filtered messages in particular location
	public void downloadAttachments(Message[] msgs, String downloadDirectory) throws MessagingException, IOException {
		String PATH = downloadDirectory;
		logger.info("Created the directory : {}  in downloadAttachments Function ", PATH);
		fileUtils.createDirIfNotExist(PATH);
		logger.info("Created the directory : {}  in downloadAttachments Function 1 ", PATH);


		for (Message message : msgs) {
			if (message.getContentType().toString().contains("multipart")) {
				Multipart multiPart = (Multipart) message.getContent();
				String directoryName = PATH.concat(message.getAllRecipients()[0].toString().split("@")[0] + "_" + sdfDate.format(currentDate) + "_" + "Attachments" + "//");
				directoryName = directoryName.replaceAll(":", "_");
				directoryName = directoryName.replaceAll("-", "_");
				if (fileUtils.createDirIfNotExist(directoryName)) {

					logger.debug("MultiPart Count is : {}", multiPart.getCount());
					for (int i = 0; i < multiPart.getCount(); i++) {
						MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(i);
						String strDate = sdfDate.format(message.getReceivedDate());
						String recipientFirstName = message.getAllRecipients()[0].toString().split("@")[0];
						String fileName = recipientFirstName + "_" + message.getSubject().toString().replace(" ", "_") + "_" + strDate;
						fileName = fileName.replaceAll(":", "_");
						fileName = fileName.replaceAll("-", "_");
						if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
							part.saveFile(directoryName + "//" + fileName + "_" + part.getFileName().replace(" ", "_"));
						}
					}
				} else
					logger.error("Not Being able to Create Directory : {} ", directoryName);
			} else
				continue;

		}
	}


}

