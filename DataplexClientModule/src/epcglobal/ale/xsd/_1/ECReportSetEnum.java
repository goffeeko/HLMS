//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.06.28 at 03:34:44 �U�� CST 
//


package epcglobal.ale.xsd._1;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ECReportSetEnum.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="ECReportSetEnum">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}NCName">
 *     &lt;enumeration value="CURRENT"/>
 *     &lt;enumeration value="ADDITIONS"/>
 *     &lt;enumeration value="DELETIONS"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "ECReportSetEnum")
@XmlEnum
public enum ECReportSetEnum {

    CURRENT,
    ADDITIONS,
    DELETIONS;

    public String value() {
        return name();
    }

    public static ECReportSetEnum fromValue(String v) {
        return valueOf(v);
    }

}
