package ee.ria.dhx.server.persistence.service;

import ee.ria.dhx.exception.DhxException;
import ee.ria.dhx.exception.DhxExceptionEnum;
import ee.ria.dhx.server.persistence.entity.Document;
import ee.ria.dhx.server.persistence.entity.Folder;
import ee.ria.dhx.server.persistence.entity.Organisation;
import ee.ria.dhx.server.persistence.entity.Recipient;
import ee.ria.dhx.server.persistence.entity.Sender;
import ee.ria.dhx.server.persistence.entity.Transport;
import ee.ria.dhx.server.persistence.enumeration.RecipientStatusEnum;
import ee.ria.dhx.server.persistence.enumeration.StatusEnum;
import ee.ria.dhx.server.persistence.repository.OrganisationRepository;
import ee.ria.dhx.server.service.util.WsUtil;
import ee.ria.dhx.server.types.ee.riik.xrd.dhl.producers.producer.dhl.DocumentsArrayType;
import ee.ria.dhx.types.CapsuleAdressee;
import ee.ria.dhx.types.DhxOrganisation;
import ee.ria.dhx.types.IncomingDhxPackage;
import ee.ria.dhx.types.InternalXroadMember;
import ee.ria.dhx.types.ee.riik.schemas.deccontainer.vers_2_1.DecContainer;
import ee.ria.dhx.types.ee.riik.schemas.deccontainer.vers_2_1.DecContainer.Transport.DecRecipient;
import ee.ria.dhx.types.ee.riik.schemas.deccontainer.vers_2_1.ObjectFactory;
import ee.ria.dhx.util.CapsuleVersionEnum;
import ee.ria.dhx.util.ConversionUtil;
import ee.ria.dhx.util.FileUtil;
import ee.ria.dhx.ws.DhxOrganisationFactory;
import ee.ria.dhx.ws.config.CapsuleConfig;
import ee.ria.dhx.ws.config.DhxConfig;
import ee.ria.dhx.ws.service.DhxMarshallerService;
import lombok.Cleanup;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.activation.DataHandler;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * Class is intended to perform actions on capsule, e.g. create capsule from
 * persisted object and backwards. Class is aware of capsule versions, and
 * designed to be easily modified to support other capsule versions aswell.
 * Other classes in that module are unaware of capsule versions and only use
 * that class to get or set data to capsule.
 * 
 * @author Aleksei Kokarev
 *
 */

@Service
@Slf4j
public class CapsuleService {

	@Autowired
	@Setter
	CapsuleConfig capsuleConfig;

	@Value("${dhx.server.treat-cantainer-as-string}")
	@Setter
	Boolean treatContainerAsString;

	@Autowired
	@Setter
	DhxMarshallerService dhxMarshallerService;

	@Autowired
	@Setter
	OrganisationRepository organisationRepository;

	@Autowired
	@Setter
	PersistenceService persistenceService;

	@Autowired
	@Setter
	DhxConfig config;

	/**
	 * Methods creates Document object from IncomingDhxPackage. Created object
	 * is not saved in database. If document senders's organisation is not
	 * found, it is created and saved.
	 * 
	 * @param pckg
	 *            - IncomingDhxPackage to create Document for
	 * @param version
	 *            - version of the capsule being received
	 * @return - Document created from IncomingDhxPackage
	 * @throws DhxException
	 */
	public Document getDocumentFromIncomingContainer(IncomingDhxPackage pckg, CapsuleVersionEnum version)
			throws DhxException {
		if (pckg.getParsedContainerVersion() != null && version != null
				&& !version.equals(pckg.getParsedContainerVersion())) {
			throw new DhxException(DhxExceptionEnum.TECHNICAL_ERROR,
					"Parsed container version and version in input are different!");
		}
		if (version == null && pckg.getParsedContainerVersion() != null) {
			version = pckg.getParsedContainerVersion();
		}
		if (version == null) {
			throw new DhxException(DhxExceptionEnum.TECHNICAL_ERROR, "Version of the capsule is not provided!");
		}
		// InputStream schemaStream = null;
		InputStream capsuleStream = null;
		// InputStream stringStream = null;
		String folderName = null;
		if (pckg.getDocumentFile() == null) {
			throw new DhxException(DhxExceptionEnum.TECHNICAL_ERROR, "Empty attachment!");
		}
		try {
			Object container = null;
			log.debug("creating container for incoming document");
			capsuleStream = pckg.getDocumentFile().getInputStream();
			if (pckg.getParsedContainer() != null) {
				container = pckg.getParsedContainer();
			} else {
				container = dhxMarshallerService.unmarshall(capsuleStream);
			}
			folderName = getFolderNameFromCapsule(container);
			Document document = new Document();
			document.setCapsuleVersion(version.toString());
			DhxOrganisation dhxSenderOrg = DhxOrganisationFactory.createDhxOrganisation(pckg.getClient());
			Organisation senderOrg = organisationRepository.findByRegistrationCodeAndSubSystem(dhxSenderOrg.getCode(),
					dhxSenderOrg.getSystem());
			if (senderOrg == null) {
				if (pckg.getClient().getRepresentee() != null) {
					Organisation representor = persistenceService
							.getOrganisationFromInternalXroadMemberAndSave(pckg.getClient(), true, false);
				}
				senderOrg = persistenceService.getOrganisationFromInternalXroadMemberAndSave(pckg.getClient(), false,
						false);
				// throw new DhxException(DhxExceptionEnum.WRONG_SENDER,
				// "Unable to find senders organisation");
			}
			document.setOrganisation(senderOrg);
			Integer inprocessStatusId = StatusEnum.IN_PROCESS.getClassificatorId();
			Transport transport = new Transport();
			transport.setStatusId(inprocessStatusId);
			transport.setSendingStart(new Timestamp(new Date().getTime()));
			document.addTransport(transport);
			Sender sender = new Sender();
			transport.addSender(sender);
			sender.setOrganisation(senderOrg);
			sender.setTransport(transport);
			CapsuleAdressee capsuleSender = capsuleConfig.getSenderFromContainer(container);
			sender.setPersonalCode(capsuleSender.getPersonalCode());
			sender.setStructuralUnit(capsuleSender.getStructuralUnit());
			Folder folder = persistenceService.getFolderByNameOrDefaultFolder(folderName);
			document.setFolder(folder);
			document.setOutgoingDocument(false);
			validateAndSetContainerToDocument(container, document);
			Recipient recipient = new Recipient();
			recipient.setTransport(transport);
			recipient.setStatusId(inprocessStatusId);
			recipient.setDhxExternalConsignmentId(pckg.getExternalConsignmentId());
			recipient.setSendingStart(new Timestamp(new Date().getTime()));
			recipient.setStatusChangeDate(new Timestamp(new Date().getTime()));
			recipient.setRecipientStatusId(RecipientStatusEnum.ACCEPTED.getClassificatorId());
			DhxOrganisation dhxRecipientOrg = DhxOrganisationFactory.createDhxOrganisation(pckg.getService());
			Organisation recipientOrg = organisationRepository
					.findByRegistrationCodeAndSubSystem(dhxRecipientOrg.getCode(), dhxRecipientOrg.getSystem());
			if (recipientOrg == null) {
				throw new DhxException(DhxExceptionEnum.WRONG_SENDER, "Unable to find recipients organisation");
			}
			recipient.setOrganisation(recipientOrg);
			for (CapsuleAdressee containerRecipient : capsuleConfig.getAdresseesFromContainer(container)) {
				if (dhxRecipientOrg.equalsToCapsuleOrganisation(containerRecipient.getAdresseeCode())) {
					recipient.setPersonalcode(containerRecipient.getPersonalCode());
					recipient.setStructuralUnit(containerRecipient.getStructuralUnit());
					break;
				}
			}
			transport.addRecipient(recipient);
			return document;
		} catch (IOException ex) {
			throw new DhxException(DhxExceptionEnum.TECHNICAL_ERROR,
					"Error occured while getting or unpacking attachment");
		} finally {
			FileUtil.safeCloseStream(capsuleStream);
		}
	}

	/**
	 * Because sendDocuments service accepts many containers in attachment and
	 * those containers are not wrapped with wrapper element, unmarshalling does
	 * not work.
	 * 
	 * @param handler
	 *            - handler containing list of containers
	 * @param version
	 *            - version of the containers to parse
	 * @return
	 */
	public List<Object> getContainersList(DataHandler handler, CapsuleVersionEnum version) throws DhxException {
		if (handler == null) {
			throw new DhxException(DhxExceptionEnum.TECHNICAL_ERROR, "Empty attachment!");
		}
		switch (version) {
		case V21:
			File tempFile = null;
			FileOutputStream fos = null;
			try {
				log.debug("containers2: " + WsUtil.readInput(WsUtil.base64decodeAndUnzip(handler.getInputStream())));

				DecContainer container = dhxMarshallerService
						.unmarshall(WsUtil.base64decodeAndUnzip(handler.getInputStream()));
				ArrayList<Object> containers = new ArrayList<Object>();
				containers.add(container);
				return containers;
			} catch (Exception ex) {
				log.info("Got error while unmarshalling capsule. Maybe there are many capsules in reqeust. continue."
						+ ex.getMessage(), ex);
			}
			try {
				// first create wrapper so then we can unmarshall
				tempFile = FileUtil.createPipelineFile();
				fos = new FileOutputStream(tempFile);
				FileUtil.writeToFile("<DocWrapper xmlns=\"http://producers.dhl.xrd.riik.ee/producer/dhl\"><docs>", fos);
				FileUtil.writeToFile(handler.getInputStream(), fos);
				FileUtil.writeToFile("</docs></DocWrapper>", fos);
				DocumentsArrayType docs = dhxMarshallerService.unmarshall(tempFile);
				List<DecContainer> containers = docs.getDecContainer();
				List<Object> objects = new ArrayList<Object>();
				objects.addAll(containers);
				return objects;
			} catch (IOException ex) {
				throw new DhxException(DhxExceptionEnum.TECHNICAL_ERROR, "Error occured while parsing attachment.");
			} finally {
				if (tempFile != null) {
					FileUtil.safeCloseStream(fos);
					tempFile.delete();
				}
			}
		default:
			throw new DhxException(DhxExceptionEnum.TECHNICAL_ERROR,
					"Unable to find adressees for given verion. version:" + version.toString());
		}
	}

	/**
	 * Methods creates Document object using data found from SOAP sender, SOAP
	 * recipient, container and folderName. Created object is not saved in
	 * database. If document senders's organisation is not found, it is created
	 * and saved. Method is meant to be used from NOT DHX services, means that
	 * document will be outgoing.
	 * 
	 * @param senderMember
	 *            - sender of the document(from SOAP header)
	 * @param recipientMember
	 *            - recipientMember of the document(from SOAP header)
	 * @param container
	 *            - container
	 * @param folderName
	 *            - name of the folder to save the document to
	 * @param version
	 *            - version of the capsule being sent
	 * @return - created Document object
	 * @throws DhxException
	 */
	public Document getDocumentFromOutgoingContainer(InternalXroadMember senderMember,
			InternalXroadMember recipientMember, Object container, String folderName, CapsuleVersionEnum version)
			throws DhxException {
		log.debug("creating container for outgoing document");
		if (folderName == null) {
			folderName = getFolderNameFromCapsule(container);
		}
		Document document = new Document();
		document.setCapsuleVersion(version.toString());
		DhxOrganisation dhxSenderOrg = DhxOrganisationFactory.createDhxOrganisation(senderMember);
		Organisation senderOrg = organisationRepository.findByRegistrationCodeAndSubSystem(dhxSenderOrg.getCode(),
				dhxSenderOrg.getSystem());
		// sender might not be member of DHX, means he is not in address list,
		// just sends the documents.
		if (senderOrg == null) {
			if (senderMember.getRepresentee() != null) {
				Organisation representor = persistenceService
						.getOrganisationFromInternalXroadMemberAndSave(senderMember, true, false);
			}
			senderOrg = persistenceService.getOrganisationFromInternalXroadMemberAndSave(senderMember, false, false);
		}
		document.setOrganisation(senderOrg);
		Integer inprocessStatusId = StatusEnum.IN_PROCESS.getClassificatorId();
		Transport transport = new Transport();
		transport.setStatusId(inprocessStatusId);
		transport.setSendingStart(new Timestamp(new Date().getTime()));
		document.addTransport(transport);
		Sender sender = new Sender();
		transport.addSender(sender);
		sender.setOrganisation(senderOrg);
		sender.setTransport(transport);
		CapsuleAdressee capsuleSender = capsuleConfig.getSenderFromContainer(container);
		Organisation capsuleSenderOrg = persistenceService.findOrg(capsuleSender.getAdresseeCode());
		if (!capsuleSenderOrg.equals(senderOrg)) {
			throw new DhxException(DhxExceptionEnum.TECHNICAL_ERROR,
					"Request sender and capsule sender do not match! ");
		}
		sender.setPersonalCode(capsuleSender.getPersonalCode());
		sender.setStructuralUnit(capsuleSender.getStructuralUnit());
		Folder folder = persistenceService.getFolderByNameOrDefaultFolder(folderName);
		document.setFolder(folder);
		document.setOutgoingDocument(true);
		for (CapsuleAdressee containerRecipient : capsuleConfig.getAdresseesFromContainer(container)) {
			Recipient recipient = new Recipient();
			recipient.setTransport(transport);
			recipient.setStructuralUnit(containerRecipient.getStructuralUnit());
			recipient.setStatusId(inprocessStatusId);
			recipient.setStatusChangeDate(new Timestamp(new Date().getTime()));
			recipient.setRecipientStatusId(RecipientStatusEnum.ACCEPTED.getClassificatorId());
			recipient.setPersonalcode(containerRecipient.getPersonalCode());
			recipient.setSendingStart(new Timestamp(new Date().getTime()));
			Organisation org = persistenceService.findOrg(containerRecipient.getAdresseeCode());

			if (org == null) {
				throw new DhxException(DhxExceptionEnum.WRONG_SENDER, "Unable to find recipients organisation");
			}
			log.debug("Found recipient organisation: " + org.getRegistrationCode() + "/" + org.getSubSystem());
			recipient.setOrganisation(org);
			transport.addRecipient(recipient);
		}
		// TODO: think of the alternative to reading into string
		validateAndSetContainerToDocument(container, document);
		return document;
	}

	private void validateAndSetContainerToDocument(Object container, Document document) throws DhxException {
		InputStream schemaStream = null;
		try {
			if (treatContainerAsString) {
				setDecMetadataFromDocument(container, document);
				if (config.getCapsuleValidate()) {
					schemaStream = FileUtil
							.getFileAsStream(capsuleConfig.getXsdForVersion(capsuleConfig.getCurrentCapsuleVersion()));
				}
				StringWriter writer = dhxMarshallerService.marshallToWriterAndValidate(container, schemaStream);
				document.setContent(writer.toString());
			} else {
				throw new DhxException(DhxExceptionEnum.TECHNICAL_ERROR, "Unimplemented!");
			}
		} finally {
			FileUtil.safeCloseStream(schemaStream);
		}
	}

	/**
	 * Method creates container object from Document object. Also it sets
	 * DecMetadata for capsule version 2.1.
	 * 
	 * @param doc
	 *            - Document to create DecContaner from
	 * @return - created container object
	 * @throws DhxException
	 */
	public Object getContainerFromDocument(Document doc) throws DhxException {
		InputStream schemaStream = null;
		InputStream capsuleStream = null;
		InputStream stringStream = null;
		Object container = null;
		try {
			if (treatContainerAsString) {
				stringStream = new ByteArrayInputStream(doc.getContent().getBytes("UTF-8"));
				container = dhxMarshallerService.unmarshallAndValidate(stringStream, schemaStream);
				setDecMetadataFromDocument(container, doc);
			} else {
				throw new DhxException(DhxExceptionEnum.TECHNICAL_ERROR, "UNIMPLEMENTED!");
			}
		} catch (IOException ex) {
			throw new DhxException(DhxExceptionEnum.TECHNICAL_ERROR,
					"Error occured while getting or unpacking attachment" + ex.getMessage(), ex);
		} finally {
			FileUtil.safeCloseStream(capsuleStream);
			FileUtil.safeCloseStream(schemaStream);
			FileUtil.safeCloseStream(stringStream);
		}
		return container;
	}

	/**
	 * Sometimes DHX addressee(incoming document) and DVK addresse(outgoing
	 * document might be different. In DHX there must be always registration
	 * code, in DVK there might be system also. That method changes recipient
	 * and sender in capsule accordingly.
	 * 
	 * @param container container Object to do changes in 
	 * @param sender sender organisation
	 * @param recipient recipient organisation
	 * @param outgoingContainer defines wether it is incoming or outgoing container.
	 */
	public void formatCapsuleRecipientAndSender(Object containerObject, Organisation sender, Organisation recipient, Boolean outgoingContainer) throws DhxException{
		CapsuleVersionEnum version = CapsuleVersionEnum.forClass(containerObject.getClass());
		switch (version) {
		case V21:
			DecContainer container = (DecContainer) containerObject;
			if (container != null) {
				String senderOraganisationCode = null;
				String recipientOrganisationCode = null;
				String recipientOrganisationCodeToFind = null;
				if(outgoingContainer) {
					senderOraganisationCode = sender.getRegistrationCode();
					recipientOrganisationCode = recipient.getRegistrationCode();
					recipientOrganisationCodeToFind = persistenceService.toDvkCapsuleAddressee(recipient.getRegistrationCode(), recipient.getSubSystem());
				} else {
					senderOraganisationCode = persistenceService.toDvkCapsuleAddressee(sender.getRegistrationCode(), sender.getSubSystem());
					recipientOrganisationCode = persistenceService.toDvkCapsuleAddressee(recipient.getRegistrationCode(), recipient.getSubSystem());
					recipientOrganisationCodeToFind = recipient.getRegistrationCode();
				}
				container.getTransport().getDecSender().setOrganisationCode(senderOraganisationCode);
				for(DecRecipient decRecipient : container.getTransport().getDecRecipient()) {
					if(decRecipient.getOrganisationCode().equals(recipientOrganisationCodeToFind)) {
						decRecipient.setOrganisationCode(recipientOrganisationCode);
						break;
					}
				}
			}
			break;
		default:
			throw new DhxException(DhxExceptionEnum.TECHNICAL_ERROR,
					"Unable to find adressees for given verion. version:" + version.toString());
		}
	}
	
	

	private String getFolderNameFromCapsule(Object containerObject) throws DhxException {
		if(containerObject == null) {
			return null;
		}
		CapsuleVersionEnum version = CapsuleVersionEnum.forClass(containerObject.getClass());
		switch (version) {
		case V21:
			DecContainer container = (DecContainer) containerObject;
			if (container != null && container.getDecMetadata() != null
					&& container.getDecMetadata().getDecFolder() != null) {
				return container.getDecMetadata().getDecFolder();
			}
			return null;
		default:
			throw new DhxException(DhxExceptionEnum.TECHNICAL_ERROR,
					"Unable to find adressees for given verion. version:" + version.toString());
		}
	}

	private void setDecMetadataFromDocument(Object containerObject, Document doc) throws DhxException {
		CapsuleVersionEnum version = CapsuleVersionEnum.forClass(containerObject.getClass());
		switch (version) {
		case V21:
			DecContainer container = (DecContainer) containerObject;
			if (container.getDecMetadata() == null) {
				ObjectFactory factory = new ObjectFactory();
				container.setDecMetadata(factory.createDecContainerDecMetadata());
			}
			if (doc.getDocumentId() != null) {
				container.getDecMetadata().setDecId(BigInteger.valueOf(doc.getDocumentId()));
			} else if (container.getDecMetadata().getDecId() == null) {
				// set random just to validate
				container.getDecMetadata().setDecId(BigInteger.valueOf(99999));
			}
			if (doc.getFolder() != null) {
				container.getDecMetadata().setDecFolder(doc.getFolder().getName());
			}
			XMLGregorianCalendar date = ConversionUtil.toGregorianCalendar(new Date());
			container.getDecMetadata().setDecReceiptDate(date);
			break;
		default:
			throw new DhxException(DhxExceptionEnum.TECHNICAL_ERROR,
					"Unable to find adressees for given verion. version:" + version.toString());
		}
	}




}
