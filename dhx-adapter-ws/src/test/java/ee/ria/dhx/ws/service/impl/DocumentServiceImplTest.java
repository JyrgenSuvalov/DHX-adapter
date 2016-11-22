package ee.ria.dhx.ws.service.impl;

import ee.ria.dhx.exception.DhxException;

import ee.ria.dhx.types.CapsuleAdressee;
import ee.ria.dhx.types.DhxPackage;
import ee.ria.dhx.types.InternalXroadMember;
import ee.ria.dhx.types.OutgoingDhxPackage;
import ee.ria.dhx.util.CapsuleVersionEnum;
import ee.ria.dhx.ws.config.CapsuleConfig;
import ee.ria.dhx.ws.config.DhxConfig;
import ee.ria.dhx.ws.config.SoapConfig;
import ee.ria.dhx.ws.service.AddressService;
import ee.ria.dhx.ws.service.DhxImplementationSpecificService;
import ee.ria.dhx.ws.service.DhxMarshallerService;
import ee.ria.dhx.ws.service.DhxPackageService;
import ee.ria.dhx.ws.service.impl.DhxGateway;
import ee.ria.dhx.ws.service.impl.DhxPackageServiceImpl;
import ee.ria.dhx.types.ee.riik.schemas.deccontainer.vers_2_1.DecContainer;

import ee.ria.dhx.types.eu.x_road.dhx.producer.SendDocumentResponse;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.stream.FileImageInputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class DocumentServiceImplTest {
  
  @InjectMocks
  DhxPackageServiceImpl documentService;
  
  @Mock
  AddressService addressService;
  
  @Mock
  DhxGateway dhxGateway;
  
  @Mock
  DhxConfig config;
  
  @Mock
  SoapConfig soapConfig;
  
  @Mock
  DhxMarshallerService dhxMarshallerService;

  @Mock
  DhxImplementationSpecificService dhxImplementationSpecificService;
  
  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();
  
  @Rule
  public final ExpectedException exception = ExpectedException.none();
  
  @Mock
  CapsuleConfig xsdConfig;
  
  @Before
  public void init () throws DhxException{
    documentService = new DhxPackageServiceImpl();
    MockitoAnnotations.initMocks(this);
    config = new DhxConfig();  
    config.setParseCapsule(true);
    when(xsdConfig.getCurrentCapsuleVersion())
    .thenReturn(CapsuleVersionEnum.V21);
    when(soapConfig.getDefaultClient())
    .thenReturn(new InternalXroadMember("", "", "", "", "", null));
    
    when(xsdConfig.getXsdForVersion(CapsuleVersionEnum.V21))
    .thenReturn("jar://Dvk_kapsel_vers_2_1_eng_est.xsd");
    documentService.setConfig(config);
  }
  /*

  @Test
  public void sendDocumentFileToRecipient() throws DhxException, IOException{
    String recipient = "11";
    InternalXroadMember member = new InternalXroadMember("ee", "GOV", "11", "DHX", null, null);
    SendDocumentResponse resp = new SendDocumentResponse();
    File file = new ClassPathResource("kapsel_21.xml").getFile();
    when(addressService.getClientForMemberCode(Mockito.eq(recipient), Mockito.anyString()))
    .thenReturn(member);
    when(dhxGateway.sendDocument(Mockito.any(OutgoingDhxPackage.class)))
    .thenReturn(resp);
    SendDocumentResponse responses = documentService.sendDocument(file, "rand", recipient, null);
    Mockito.verify(dhxGateway, times(1)).sendDocument(Mockito.any(OutgoingDhxPackage.class));
    assertEquals(null, responses.getFault());
  }
  
  @Test
  public void sendDocumentStreamToRecipient() throws DhxException, IOException{
    String recipient = "11";
    InternalXroadMember member = new InternalXroadMember("ee", "GOV", "11", "DHX", null, null);
    SendDocumentResponse resp = new SendDocumentResponse();
    File file = new ClassPathResource("kapsel_21.xml").getFile();
    when(addressService.getClientForMemberCode(Mockito.eq(recipient), Mockito.anyString()))
    .thenReturn(member);
    when(dhxGateway.sendDocument(Mockito.any(OutgoingDhxPackage.class)))
    .thenReturn(resp);
    List<SendDocumentResponse> responses = documentService.sendDocument(new FileInputStream(file), "rand", recipient, null);
    Mockito.verify(dhxGateway, times(1)).sendDocument(Mockito.any(OutgoingDhxPackage.class));
    assertEquals(1, responses.size());
  }
  
  @Test(expected = DhxException.class)
  public void sendDocumentStreamToEmptyRecipient() throws DhxException, IOException{
    String recipient = null;
    File file = new ClassPathResource("kapsel_21.xml").getFile();
    documentService.sendDocument(new FileInputStream(file), "rand", recipient, null);
    Mockito.verify(dhxGateway, times(1)).sendDocument(Mockito.any(OutgoingDhxPackage.class));
    exception.expectMessage("Recipient not defined");
  }
  
  @Test
  public void sendDocumentStream() throws DhxException, IOException{
    String recipient = "11";
    InternalXroadMember member = new InternalXroadMember("ee", "GOV", "11", "DHX", null, null);
    CapsuleAdressee adr = new CapsuleAdressee("10560025");
    List<CapsuleAdressee> adrs = new ArrayList<CapsuleAdressee>();
    DecContainer capsule = new DecContainer();
    adrs.add(adr);
    SendDocumentResponse resp = new SendDocumentResponse();
    File file = new ClassPathResource("kapsel_21.xml").getFile();
    when(addressService.getClientForMemberCode(Mockito.eq(recipient), Mockito.anyString()))
    .thenReturn(member);
    when(dhxGateway.sendDocument(Mockito.any(OutgoingDhxPackage.class)))
    .thenReturn(resp);
    when(xsdConfig.getAdresseesFromContainer(Mockito.any(DecContainer.class)))
    .thenReturn(adrs);
    when(dhxMarshallerService.unmarshallAndValidate(Mockito.any(InputStream.class), Mockito.any(InputStream.class)))
    .thenReturn(capsule);
    when(dhxMarshallerService.marshall(capsule))
    .thenReturn(file);
    List<SendDocumentResponse> responses = documentService.sendDocument(new FileInputStream(file), "rand");
    Mockito.verify(dhxGateway, times(1)).sendDocument(Mockito.any(OutgoingDhxPackage.class));
    assertEquals(1, responses.size());
  }
  
  @Test
  public void sendDocumentStreamVersion() throws DhxException, IOException{
    String recipient = "11";
    InternalXroadMember member = new InternalXroadMember("ee", "GOV", "11", "DHX", null, null);
    CapsuleAdressee adr = new CapsuleAdressee("10560025");
    List<CapsuleAdressee> adrs = new ArrayList<CapsuleAdressee>();
    DecContainer capsule = new DecContainer();
    adrs.add(adr);
    SendDocumentResponse resp = new SendDocumentResponse();
    File file = new ClassPathResource("kapsel_21.xml").getFile();
    when(addressService.getClientForMemberCode(Mockito.eq(recipient), Mockito.anyString()))
    .thenReturn(member);
    when(dhxGateway.sendDocument(Mockito.any(OutgoingDhxPackage.class)))
    .thenReturn(resp);
    when(xsdConfig.getAdresseesFromContainer(Mockito.any(DecContainer.class)))
    .thenReturn(adrs);
    when(dhxMarshallerService.unmarshallAndValidate(Mockito.any(InputStream.class), Mockito.any(InputStream.class)))
    .thenReturn(capsule);
    when(dhxMarshallerService.marshall(capsule))
    .thenReturn(file);
    List<SendDocumentResponse> responses = documentService.sendDocument(new FileInputStream(file), "rand", CapsuleVersionEnum.V21);
    Mockito.verify(dhxGateway, times(1)).sendDocument(Mockito.any(OutgoingDhxPackage.class));
    assertEquals(1, responses.size());
  }
  
  @Test
  public void sendDocumentFileVersion() throws DhxException, IOException{
    String recipient = "11";
    InternalXroadMember member = new InternalXroadMember("ee", "GOV", "11", "DHX", null, null);
    CapsuleAdressee adr = new CapsuleAdressee("10560025");
    List<CapsuleAdressee> adrs = new ArrayList<CapsuleAdressee>();
    DecContainer capsule = new DecContainer();
    adrs.add(adr);
    SendDocumentResponse resp = new SendDocumentResponse();
    File file = new ClassPathResource("kapsel_21.xml").getFile();
    when(addressService.getClientForMemberCode(Mockito.eq(recipient), Mockito.anyString()))
    .thenReturn(member);
    when(dhxGateway.sendDocument(Mockito.any(OutgoingDhxPackage.class)))
    .thenReturn(resp);
    when(xsdConfig.getAdresseesFromContainer(Mockito.any(DecContainer.class)))
    .thenReturn(adrs);
    when(dhxMarshallerService.unmarshallAndValidate(Mockito.any(InputStream.class), Mockito.any(InputStream.class)))
    .thenReturn(capsule);
    when(dhxMarshallerService.marshall(capsule))
    .thenReturn(file);
    List<SendDocumentResponse> responses = documentService.sendDocument(file, "rand", CapsuleVersionEnum.V21);
    Mockito.verify(dhxGateway, times(1)).sendDocument(Mockito.any(OutgoingDhxPackage.class));
    assertEquals(1, responses.size());
  }
  
  @Test
  public void sendDocumentFile() throws DhxException, IOException{
    String recipient = "11";
    InternalXroadMember member = new InternalXroadMember("ee", "GOV", "11", "DHX", null, null);
    CapsuleAdressee adr = new CapsuleAdressee("10560025");
    List<CapsuleAdressee> adrs = new ArrayList<CapsuleAdressee>();
    DecContainer capsule = new DecContainer();
    adrs.add(adr);
    SendDocumentResponse resp = new SendDocumentResponse();
    File file = new ClassPathResource("kapsel_21.xml").getFile();
    when(addressService.getClientForMemberCode(Mockito.eq(recipient), Mockito.anyString()))
    .thenReturn(member);
    when(dhxGateway.sendDocument(Mockito.any(OutgoingDhxPackage.class)))
    .thenReturn(resp);
    when(xsdConfig.getAdresseesFromContainer(Mockito.any(DecContainer.class)))
    .thenReturn(adrs);
    when(dhxMarshallerService.unmarshallAndValidate(Mockito.any(InputStream.class), Mockito.any(InputStream.class)))
    .thenReturn(capsule);
    when(dhxMarshallerService.marshall(capsule))
    .thenReturn(file);
    List<SendDocumentResponse> responses = documentService.sendDocument(file, "rand");
    Mockito.verify(dhxGateway, times(1)).sendDocument(Mockito.any(OutgoingDhxPackage.class));
    assertEquals(1, responses.size());
  }
  
  
  @Ignore
  @Test
  public void sendDocumentStreamNoCapsuleRecipient() throws DhxException, IOException{
    String recipient = "11";
    InternalXroadMember member = new InternalXroadMember("ee", "GOV", "11", "DHX", null, null);
    CapsuleAdressee adr = new CapsuleAdressee("10560025");
    List<CapsuleAdressee> adrs = new ArrayList<CapsuleAdressee>();
    DecContainer capsule = new DecContainer();
    adrs.add(adr);
    SendDocumentResponse resp = new SendDocumentResponse();
    File file = new ClassPathResource("kapsel_21.xml").getFile();
    when(addressService.getClientForMemberCode(Mockito.eq(recipient), Mockito.anyString()))
    .thenReturn(member);
    when(dhxGateway.sendDocument(Mockito.any(OutgoingDhxPackage.class)))
    .thenReturn(resp);
    when(xsdConfig.getAdresseesFromContainer(Mockito.any(DecContainer.class)))
    .thenReturn(null);
    when(dhxMarshallerService.unmarshallAndValidate(Mockito.any(InputStream.class), Mockito.any(InputStream.class)))
    .thenReturn(capsule);
    when(dhxMarshallerService.marshall(capsule))
    .thenReturn(file);
    List<SendDocumentResponse> responses = documentService.sendDocument(new FileInputStream(file), "rand");
    Mockito.verify(dhxGateway, times(1)).sendDocument(Mockito.any(OutgoingDhxPackage.class));
    assertEquals(1, responses.size());
  }
  
  @Ignore
  @Test
  public void sendDocumentStreamEmptyVersion() throws DhxException, IOException{
    CapsuleVersionEnum version = null;
    String recipient = "11";
    InternalXroadMember member = new InternalXroadMember("ee", "GOV", "11", "DHX", null, null);
    CapsuleAdressee adr = new CapsuleAdressee("10560025");
    List<CapsuleAdressee> adrs = new ArrayList<CapsuleAdressee>();
    DecContainer capsule = new DecContainer();
    adrs.add(adr);
    SendDocumentResponse resp = new SendDocumentResponse();
    File file = new ClassPathResource("kapsel_21.xml").getFile();
    when(addressService.getClientForMemberCode(Mockito.eq(recipient), Mockito.anyString()))
    .thenReturn(member);
    when(dhxGateway.sendDocument(Mockito.any(OutgoingDhxPackage.class)))
    .thenReturn(resp);
    when(xsdConfig.getAdresseesFromContainer(Mockito.any(DecContainer.class)))
    .thenReturn(adrs);
    when(dhxMarshallerService.unmarshallAndValidate(Mockito.any(InputStream.class), Mockito.any(InputStream.class)))
    .thenReturn(capsule);
    when(dhxMarshallerService.marshall(capsule))
    .thenReturn(file);
    List<SendDocumentResponse> responses = documentService.sendDocument(new FileInputStream(file), "rand", version);
    Mockito.verify(dhxGateway, times(1)).sendDocument(Mockito.any(OutgoingDhxPackage.class));
    assertEquals(1, responses.size());
  }

  */
  //TODO: test with multiple recipients, test recipient check
}
