//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference
// Implementation, v2.2.11
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2016.11.10 at 01:13:12 PM EET
//


package ee.ria.dhx.types.eu.x_road.xsd.identifiers;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>
 * Java class for XRoadObjectType.
 * 
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;simpleType name="XRoadObjectType"&gt;
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string"&gt;
 *     &lt;enumeration value="MEMBER"/&gt;
 *     &lt;enumeration value="SUBSYSTEM"/&gt;
 *     &lt;enumeration value="SERVER"/&gt;
 *     &lt;enumeration value="GLOBALGROUP"/&gt;
 *     &lt;enumeration value="LOCALGROUP"/&gt;
 *     &lt;enumeration value="SECURITYCATEGORY"/&gt;
 *     &lt;enumeration value="SERVICE"/&gt;
 *     &lt;enumeration value="CENTRALSERVICE"/&gt;
 *   &lt;/restriction&gt;
 * &lt;/simpleType&gt;
 * </pre>
 * 
 */
@XmlType(name = "XRoadObjectType")
@XmlEnum
public enum XRoadObjectType {

  MEMBER, SUBSYSTEM, SERVER, GLOBALGROUP, LOCALGROUP, SECURITYCATEGORY, SERVICE, CENTRALSERVICE;

  public String value() {
    return name();
  }

  public static XRoadObjectType fromValue(String v) {
    return valueOf(v);
  }

}
