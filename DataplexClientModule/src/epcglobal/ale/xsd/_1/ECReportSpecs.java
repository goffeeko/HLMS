//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 in JDK 6 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2012.06.28 at 03:34:44 �U�� CST 
//


package epcglobal.ale.xsd._1;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ECReportSpecs complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ECReportSpecs">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="reportSpec" type="{urn:epcglobal:ale:xsd:1}ECReportSpec" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ECReportSpecs", propOrder = {
    "reportSpec"
})
public class ECReportSpecs {

    @XmlElement(required = true)
    protected List<ECReportSpec> reportSpec;

    /**
     * Gets the value of the reportSpec property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the reportSpec property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getReportSpec().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ECReportSpec }
     * 
     * 
     */
    public List<ECReportSpec> getReportSpec() {
        if (reportSpec == null) {
            reportSpec = new ArrayList<ECReportSpec>();
        }
        return this.reportSpec;
    }

}
