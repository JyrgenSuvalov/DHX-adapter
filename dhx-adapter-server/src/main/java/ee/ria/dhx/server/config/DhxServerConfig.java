package ee.ria.dhx.server.config;

import com.jcabi.aspects.Loggable;

import ee.ria.dhx.exception.DhxException;
import ee.ria.dhx.exception.DhxExceptionEnum;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Server config.
 * @author Aleksei Kokarev
 *
 */
@Configuration
@Slf4j
public class DhxServerConfig {

  @Value("${documents.folder}")
  @Setter
  String documentsFolder;

  @Value("${dhx.server-wsdl-file-v1:dhl_new.wsdl}")
  private String wsdlFilev1;
  
  @Value("${dhx.server-wsdl-file-v2:dhl_new_v2.wsdl}")
  private String wsdlFilev2;
  
  @Value("${dhx.server-wsdl-file-v3:dhl_new_v3.wsdl}")
  private String wsdlFilev3;
  
  @Value("${dhx.server-wsdl-file-v4:dhl_new_v4.wsdl}")
  private String wsdlFilev4;


/**
 * Creates document in configured folder.
 * @return created file
 * @throws DhxException thrown if error occurs
 */
  @Loggable
  public File createDocumentFile() throws DhxException {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("YYYY_MM_dd_HHmmss");
      Date date = new Date();
      String fileName = documentsFolder + "dhx_" + sdf.format(date) + UUID.randomUUID();
      log.debug("creating file: " + fileName);
      File file = new File(fileName);
      file.createNewFile();
      return file;
    } catch (IOException ex) {
      throw new DhxException(DhxExceptionEnum.FILE_ERROR,
          "Error occured while creating new file. " + ex.getMessage(), ex);
    }
  }

  /**
   * Returns document found in configured folder.
   * @param fileName name of the file to find
   * @return found file
   * @throws DhxException thrown if error occurs
   */
  @Loggable
  public File getDocumentFile(String fileName) throws DhxException {
    File file = new File(documentsFolder + fileName);
    if (file.exists()) {
      return file;
    } else {
      throw new DhxException(DhxExceptionEnum.FILE_ERROR,
          "File not found! file:" + documentsFolder + fileName);
    }
  }

  /**
   * Returns the wsdlFilev1.
   *
   * @return the wsdlFilev1
   */
  public String getWsdlFilev1() {
    return wsdlFilev1;
  }

  /**
   * Sets the wsdlFilev1.
   *
   * @param wsdlFilev1 the wsdlFilev1 to set
   */
  public void setWsdlFilev1(String wsdlFilev1) {
    this.wsdlFilev1 = wsdlFilev1;
  }

  /**
   * Returns the wsdlFilev2.
   *
   * @return the wsdlFilev2
   */
  public String getWsdlFilev2() {
    return wsdlFilev2;
  }

  /**
   * Sets the wsdlFilev2.
   *
   * @param wsdlFilev2 the wsdlFilev2 to set
   */
  public void setWsdlFilev2(String wsdlFilev2) {
    this.wsdlFilev2 = wsdlFilev2;
  }

  /**
   * Returns the wsdlFilev3.
   *
   * @return the wsdlFilev3
   */
  public String getWsdlFilev3() {
    return wsdlFilev3;
  }

  /**
   * Sets the wsdlFilev3.
   *
   * @param wsdlFilev3 the wsdlFilev3 to set
   */
  public void setWsdlFilev3(String wsdlFilev3) {
    this.wsdlFilev3 = wsdlFilev3;
  }

  /**
   * Returns the wsdlFilev4.
   *
   * @return the wsdlFilev4
   */
  public String getWsdlFilev4() {
    return wsdlFilev4;
  }

  /**
   * Sets the wsdlFilev4.
   *
   * @param wsdlFilev4 the wsdlFilev4 to set
   */
  public void setWsdlFilev4(String wsdlFilev4) {
    this.wsdlFilev4 = wsdlFilev4;
  }



}
