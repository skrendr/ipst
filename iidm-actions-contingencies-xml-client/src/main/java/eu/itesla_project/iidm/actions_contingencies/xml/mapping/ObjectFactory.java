//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2015.06.19 at 10:45:35 AM CEST 
//


package eu.itesla_project.iidm.actions_contingencies.xml.mapping;

import java.math.BigInteger;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the eu.itesla_project.iidm.actions_contingencies.xml.mapping package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Num_QNAME = new QName("", "num");
    private final static QName _Name_QNAME = new QName("", "name");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: eu.itesla_project.iidm.actions_contingencies.xml.mapping
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Association }
     * 
     */
    public Association createAssociation() {
        return new Association();
    }

    /**
     * Create an instance of {@link Contingency }
     * 
     */
    public Contingency createContingency() {
        return new Contingency();
    }

    /**
     * Create an instance of {@link Zones }
     * 
     */
    public Zones createZones() {
        return new Zones();
    }

    /**
     * Create an instance of {@link Zone }
     * 
     */
    public Zone createZone() {
        return new Zone();
    }

    /**
     * Create an instance of {@link VoltageLevels }
     * 
     */
    public VoltageLevels createVoltageLevels() {
        return new VoltageLevels();
    }

    /**
     * Create an instance of {@link VoltageLevel }
     * 
     */
    public VoltageLevel createVoltageLevel() {
        return new VoltageLevel();
    }

    /**
     * Create an instance of {@link Equipments }
     * 
     */
    public Equipments createEquipments() {
        return new Equipments();
    }

    /**
     * Create an instance of {@link Equipment }
     * 
     */
    public Equipment createEquipment() {
        return new Equipment();
    }

    /**
     * Create an instance of {@link Constraint }
     * 
     */
    public Constraint createConstraint() {
        return new Constraint();
    }

    /**
     * Create an instance of {@link Action }
     * 
     */
    public Action createAction() {
        return new Action();
    }

    /**
     * Create an instance of {@link Description }
     * 
     */
    public Description createDescription() {
        return new Description();
    }

    /**
     * Create an instance of {@link GenerationOperation }
     * 
     */
    public GenerationOperation createGenerationOperation() {
        return new GenerationOperation();
    }

    /**
     * Create an instance of {@link And }
     * 
     */
    public And createAnd() {
        return new And();
    }

    /**
     * Create an instance of {@link Operand }
     * 
     */
    public Operand createOperand() {
        return new Operand();
    }

    /**
     * Create an instance of {@link Then }
     * 
     */
    public Then createThen() {
        return new Then();
    }

    /**
     * Create an instance of {@link Or }
     * 
     */
    public Or createOr() {
        return new Or();
    }

    /**
     * Create an instance of {@link ElementaryActions }
     * 
     */
    public ElementaryActions createElementaryActions() {
        return new ElementaryActions();
    }

    /**
     * Create an instance of {@link ElementaryAction }
     * 
     */
    public ElementaryAction createElementaryAction() {
        return new ElementaryAction();
    }

    /**
     * Create an instance of {@link LineOperation }
     * 
     */
    public LineOperation createLineOperation() {
        return new LineOperation();
    }

    /**
     * Create an instance of {@link SwitchOperation }
     * 
     */
    public SwitchOperation createSwitchOperation() {
        return new SwitchOperation();
    }

    /**
     * Create an instance of {@link PstOperation }
     * 
     */
    public PstOperation createPstOperation() {
        return new PstOperation();
    }

    /**
     * Create an instance of {@link Redispatching }
     * 
     */
    public Redispatching createRedispatching() {
        return new Redispatching();
    }

    /**
     * Create an instance of {@link ActionCtgAssociations }
     * 
     */
    public ActionCtgAssociations createActionCtgAssociations() {
        return new ActionCtgAssociations();
    }

    /**
     * Create an instance of {@link ActionPlans }
     * 
     */
    public ActionPlans createActionPlans() {
        return new ActionPlans();
    }

    /**
     * Create an instance of {@link ActionPlan }
     * 
     */
    public ActionPlan createActionPlan() {
        return new ActionPlan();
    }

    /**
     * Create an instance of {@link Option }
     * 
     */
    public Option createOption() {
        return new Option();
    }

    /**
     * Create an instance of {@link LogicalExpression }
     * 
     */
    public LogicalExpression createLogicalExpression() {
        return new LogicalExpression();
    }

    /**
     * Create an instance of {@link Contingencies }
     * 
     */
    public Contingencies createContingencies() {
        return new Contingencies();
    }

    /**
     * Create an instance of {@link ActionsContingencies }
     * 
     */
    public ActionsContingencies createActionsContingencies() {
        return new ActionsContingencies();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "num")
    public JAXBElement<BigInteger> createNum(BigInteger value) {
        return new JAXBElement<BigInteger>(_Num_QNAME, BigInteger.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "", name = "name")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    public JAXBElement<String> createName(String value) {
        return new JAXBElement<String>(_Name_QNAME, String.class, null, value);
    }

}