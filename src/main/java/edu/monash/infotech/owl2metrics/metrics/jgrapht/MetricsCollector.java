package edu.monash.infotech.owl2metrics.metrics.jgrapht;

import com.google.common.collect.Sets;
import edu.monash.infotech.owl2metrics.model.AggregratedClassMetric;
import edu.monash.infotech.owl2metrics.model.DLCMetrics;
import edu.monash.infotech.owl2metrics.model.OntMetrics;
import edu.monash.infotech.owl2metrics.model.PropertyMetrics;
import edu.monash.infotech.owl2metrics.profiles.ProfileReporter;
import edu.monash.infotech.owl2metrics.translate.OWL2Graph;
import edu.monash.infotech.owl2metrics.translate.jgrapht.graph.NamedNode;
import edu.monash.infotech.owl2metrics.translate.jgrapht.graph.NamedParamEdge;
import org.apache.log4j.Logger;
import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.traverse.DepthFirstIterator;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.metrics.GCICount;
import org.semanticweb.owlapi.metrics.HiddenGCICount;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static java.lang.Boolean.valueOf;
import static java.lang.Math.max;
import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.*;

/**
 * @author Yuan-Fang Li
 * @version $Id $
 */
public class MetricsCollector {
    private Logger logger = Logger.getLogger(getClass());

    private DirectedGraph<NamedNode, NamedParamEdge> graph;
    private OWLOntology ontology;
    private boolean measureExrpessivity;

    private int noClasses, noSubCls;
    private int noProperties;
    private int noIndividuals;

    private int subclass_crt;

    private int cidMax, cidTotal;
    private int codMax, codTotal;
    private int nocMax, nocTotal;
    private int ditMax, ditTotal;
    private int nopMax, nopTotal;

    private DLCMetrics dlcMetrics;
    private PropertyMetrics propertyMetrics;

    private Set<OWLClass> tested = new HashSet<OWLClass>();
    private Set<OWLClass> calculatingDepthSet = new HashSet<OWLClass>();
    private Map<OWLClass, Map<ClassExpressionType, Integer>> cls_map = new HashMap<OWLClass, Map<ClassExpressionType, Integer>>();

    public MetricsCollector(DirectedGraph<NamedNode, NamedParamEdge> graph, OWLOntology ontology, boolean measureExpressivity) {
        this.graph = graph;
        this.ontology = ontology;

        noClasses = 0;
        noSubCls = 0;

        cidMax = 0;
        cidTotal = 0;
        codMax = 0;
        codTotal = 0;
        nocMax = 0;
        nocTotal = 0;
        ditMax = 0;
        ditTotal = 0;

        dlcMetrics = new DLCMetrics();
        propertyMetrics = new PropertyMetrics();

        this.measureExrpessivity = measureExpressivity;
    }

    public MetricsCollector(DirectedGraph<NamedNode, NamedParamEdge> graph, OWLOntology ontology) {
        this(graph, ontology, true);
    }

    public OntMetrics collectMetrics(String ontName) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        OntMetrics metrics = new OntMetrics(ontName);
        Set<NamedNode> allNodeHits = graph.vertexSet();
        Set<NamedParamEdge> allEdgeHits = graph.edgeSet();

        // EOG
        Map<Integer, Set<String>> edgeNodeMap = new HashMap<Integer, Set<String>>();

        int namedNodeCount = 0, totalCount = 0;

        for (NamedNode node : allNodeHits) {
            // SOV
            if (!valueOf(node.getProperties().get(OWL2Graph.NODE_ANON_TYPE_NAME).toString())) {
                namedNodeCount++;
            }

            edgeNodeMap = handleNode(metrics, edgeNodeMap, node);
            totalCount++;
            if (totalCount % 1000 == 0) {
                logger.info("Handled " + totalCount + " nodes.");
            }
        }
        logger.info("Handled total # nodes: " + totalCount);

        // SOV
        metrics.setSov(namedNodeCount);

        // ENR
        int nodeCount = allNodeHits.size();
        int edgeCount = allEdgeHits.size();
        metrics.setEnr((double) edgeCount / nodeCount);

        // CYC
        StrongConnectivityInspector<NamedNode, NamedParamEdge> ccInspector = new StrongConnectivityInspector<NamedNode, NamedParamEdge>(graph);
        List<Set<NamedNode>> cycList = ccInspector.stronglyConnectedSets();
        int noCC = 0;
        for (Set<NamedNode> set : cycList) {
            if (set.size() > 1) {
                noCC++;
            }
        }
        int cyclomatic = edgeCount - nodeCount + 2 * noCC;
        metrics.setCyc(max(0, cyclomatic));
        logger.info("CYC = " + max(0, cyclomatic));

        // EOG
        edgeNodeMap.remove(0);
        double eog = 0;
        int edgeOutcomeCount = 0;
        for (Set<String> nodes : edgeNodeMap.values()) {
            edgeOutcomeCount += nodes.size();
        }
        for (Set<String> nodes : edgeNodeMap.values()) {
            int eCount = nodes.size();
            double eRatio = (double) eCount / edgeOutcomeCount;
            eog -= eRatio * (Math.log(eRatio) / Math.log(2));
        }
        metrics.setEog(eog);

        // GCI
        OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
        ((OWLOntologyFactory.OWLOntologyCreationHandler) ontologyManager).ontologyCreated(ontology);
        GCICount gciCount = new GCICount(ontologyManager);
        gciCount.setImportsClosureUsed(true);
        gciCount.setOntology(ontology);
        metrics.setGci(gciCount.getValue());
        gciCount.dispose();
        ontologyManager.removeOntology(ontology);

        // Hidden GCI
        HiddenGCICount hiddenGCICount = new HiddenGCICount(ontologyManager);
        ((OWLOntologyFactory.OWLOntologyCreationHandler) ontologyManager).ontologyCreated(ontology);
        hiddenGCICount.setImportsClosureUsed(true);
        hiddenGCICount.setOntology(ontology);
        metrics.setHgci(hiddenGCICount.getValue());
        hiddenGCICount.dispose();
        ontologyManager.removeOntology(ontology);

        double numOfLogicalAxiom = 0;
        for (OWLOntology ont : ontology.getImportsClosure()) {
            numOfLogicalAxiom += ont.getLogicalAxiomCount();
        }
        Set<OWLSubClassOfAxiom> subclassSet = ontology.getAxioms(AxiomType.SUBCLASS_OF, true);
        subclass_crt = ontology.getAxiomCount(AxiomType.SUBCLASS_OF, true);
        //ESUB/ALLSUB - Variable
        int ESUB_crt = 0;
        boolean subflag_Eq = false;
        boolean supflag_Eq = false;
        //DISSUB/ALLSUB - Variable
        int DSUB_crt = 0;
        boolean subflag_D = false;
        boolean supflag_D = false;
        //CSUB/ALLSUB - Variable
        int CSUB_crt = 0;
        int CSUB_AnaClass_crt = 0;
        int temp = 0;
        //(SuperClass)Number of Chain of Existential, Depth of Chain of Existential - Variable
        int E_chn_crt = 0;
        int D_E_chn_crt = 0;
        int max_D_E_chn_crt = 0;
        //(SupClass)Number of Chain of Disjunction, Depth of Chain of Disjunction - Variable
        int D_chn_crt = 0;
        int D_D_chn_crt = 0;
        int max_D_D_chn_crt = 0;
        //(SubClass)Number of Chain of Existential, Depth of Chain of Existential - Variable
        int sE_chn_crt = 0;
        int D_sE_chn_crt = 0;
        int max_D_sE_chn_crt = 0;
        //(SubClass)Number of Chain of Conjunction, Depth of Chain of Conjunction - Variable
        int C_chn_crt = 0;
        int D_C_chn_crt = 0;
        int max_D_C_chn_crt = 0;
        //Transitive role, Inverse role, Role hierarchy and Disjunction  - Variable
        //Map<String,Map<String,ArrayList<Integer>>> ont_Map = new HashMap<String, Map<String, ArrayList<Integer>>>();
        Map<String, ArrayList<Integer>> InvokedMap = new HashMap<String, ArrayList<Integer>>();
        int HLC = 0;


        int countAxiom = 0;

        for (OWLSubClassOfAxiom a : subclassSet) {
            Set<OWLClassExpression> Subcls = a.getSubClass().getNestedClassExpressions();
            Set<OWLClassExpression> Supcls = a.getSuperClass().getNestedClassExpressions();
            countAxiom++;
            //System.out.println(countAxiom + ". " + a);
            //ESUB/ALLSUB - Existential quantification
            //System.out.println("ESUB/ALLSUB");
            for (OWLClassExpression c : Subcls) {
                if (c.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)) {
                    subflag_Eq = true;
                    break;
                }
            }
            for (OWLClassExpression c : Supcls) {
                if (c.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)) {
                    supflag_Eq = true;
                    break;
                }
            }

            if (subflag_Eq || supflag_Eq) {
                ESUB_crt++;
                subflag_Eq = false;
                supflag_Eq = false;
            }

            //DISSUB/ALLSUB - Disjunction
            //System.out.println("DISSUB/ALLSUB");
            for (OWLClassExpression c : Subcls) {
                if (c.getClassExpressionType().equals(ClassExpressionType.OBJECT_UNION_OF)) {
                    subflag_D = true;
                    break;
                }
            }
            for (OWLClassExpression c : Supcls) {
                if (c.getClassExpressionType().equals(ClassExpressionType.OBJECT_UNION_OF)) {
                    supflag_D = true;
                    break;
                }
            }

            if (subflag_D || supflag_D) {
                DSUB_crt++;
                subflag_D = false;
                supflag_D = false;
            }

            //CSUB/ALLSUB - Conjunction with anonymous in subclass
            //System.out.println("CSUB/ALLSUB");
            OWLClassExpression CSUB_Subcls = a.getSubClass();
            if (CSUB_Subcls.getClassExpressionType().equals(ClassExpressionType.OBJECT_INTERSECTION_OF)) {
                Set<OWLClassExpression> set_component = CSUB_Subcls.asConjunctSet();
                for (OWLClassExpression con : set_component) {
                    if (con.isAnonymous()) {
                        temp++;
                    }
                }
                if (temp > 0) {
                    CSUB_crt++;
                }
                CSUB_AnaClass_crt += temp;
                temp = 0;
            }

            //(SuperClass)Number of Chain of Existential, Depth of Chain of Existential
            //System.out.println("(SuperClass)Number of Chain of Existential");
            OWLClassExpression E_chn_supCls = a.getSuperClass();
            //if(supCls.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)){
            D_E_chn_crt = calculateDepth(E_chn_supCls, ClassExpressionType.OBJECT_SOME_VALUES_FROM);
            tested.clear();
            if (D_E_chn_crt > 1) {
                E_chn_crt++;
            }
            if (D_E_chn_crt > max_D_E_chn_crt) {
                max_D_E_chn_crt = D_E_chn_crt;
                D_E_chn_crt = 0;
            }
            //}

            //(SupClass)Number of Chain of Disjunction, Depth of Chain of Disjunction
            //System.out.println("(SupClass)Number of Chain of Disjunction");
            OWLClassExpression D_chn_supCls = a.getSuperClass();
            //if(supCls.getClassExpressionType().equals(ClassExpressionType.OBJECT_UNION_OF)){
            D_D_chn_crt = calculateDepth(D_chn_supCls, ClassExpressionType.OBJECT_UNION_OF);
            tested.clear();
            if (D_D_chn_crt > 1) {
                D_chn_crt++;
            }
            if (D_D_chn_crt > max_D_D_chn_crt) {
                max_D_D_chn_crt = D_D_chn_crt;
                D_D_chn_crt = 0;
            }
            //}

            //(SubClass)Number of Chain of Existential, Depth of Chain of Existential
            //System.out.println("(SubClass)Number of Chain of Existential");
            OWLClassExpression sE_chn_subCls = a.getSubClass();
            //if(subCls.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)){
            D_sE_chn_crt = calculateDepth(sE_chn_subCls, ClassExpressionType.OBJECT_SOME_VALUES_FROM);
            tested.clear();
            if (D_sE_chn_crt > 1) {
                sE_chn_crt++;
            }
            if (D_sE_chn_crt > max_D_sE_chn_crt) {
                max_D_sE_chn_crt = D_sE_chn_crt;
                D_sE_chn_crt = 0;
            }

            //}

            //(SubClass)Number of Chain of Conjunction, Depth of Chain of Conjunction
            //System.out.println("(SubClass)Number of Chain of Conjunction");
            OWLClassExpression C_chn_subCls = a.getSubClass();
            //if(subCls.getClassExpressionType().equals(ClassExpressionType.OBJECT_INTERSECTION_OF)){
            D_C_chn_crt = calculateDepth(C_chn_subCls, ClassExpressionType.OBJECT_INTERSECTION_OF);
            tested.clear();
            if (D_C_chn_crt > 1) {
                C_chn_crt++;
            }
            if (D_C_chn_crt > max_D_C_chn_crt) {
                max_D_C_chn_crt = D_C_chn_crt;
                D_C_chn_crt = 0;
            }
            //}

            //Transitive role, Inverse role, Role hierarchy and Disjunction
            //System.out.println("Calculating HLC");
            int totalInvoked = 0;
            OWLClassExpression HLC_subCls = a.getSubClass();
            String HLC_aCls = "";
            if (!HLC_subCls.isAnonymous()) {
                HLC_aCls = HLC_subCls.asOWLClass().toString();
                if (!InvokedMap.containsKey(HLC_aCls)) {
                    ArrayList<Integer> newAList = new ArrayList<Integer>();
                    InvokedMap.put(HLC_subCls.asOWLClass().toString(), newAList);
                }
                OWLClassExpression HLC_supCls = a.getSuperClass();
                if (HLC_supCls.isAnonymous()) {
                    int disjunctionInvoking = calculateDisjunctionInvoking(HLC_supCls);
                    if (disjunctionInvoking > 0) {
                        //System.out.println("Check Disjunction Invoking");
                        totalInvoked += disjunctionInvoking;
                        HLC += disjunctionInvoking;
                    }
                    Set<OWLObjectProperty> objectPropertiesInSignature = HLC_supCls.getObjectPropertiesInSignature();
                    int transitiveDiffRole = calculateDifficultRole(objectPropertiesInSignature, "transitive", ontology);
                    if (transitiveDiffRole > 0) {
                        //System.out.println("Check transitive role");
                        totalInvoked += transitiveDiffRole;
                        HLC += transitiveDiffRole;
                    }
                    int inverseDiffRole = calculateDifficultRole(objectPropertiesInSignature, "inverse", ontology);
                    if (inverseDiffRole > 0) {
                        //System.out.println("Check inverse role");
                        totalInvoked += inverseDiffRole;
                        HLC += inverseDiffRole;
                    }
                    int hierarchyDiffRole = calculateDifficultRole(objectPropertiesInSignature, "hierarchy", ontology);
                    if (hierarchyDiffRole > 0) {
                        //System.out.println("Check hierarchy role");
                        totalInvoked += hierarchyDiffRole;
                        HLC += hierarchyDiffRole;
                    }
                }

                InvokedMap.get(HLC_aCls).add(totalInvoked);
                metrics.getHlc_c().put(HLC_aCls, totalInvoked);
                //System.out.println("End calculating HLC");
            }

        }

        subflag_Eq = false;
        supflag_Eq = false;
        subflag_D = false;
        supflag_D = false;
        //System.out.println("Getting Chain results");
        if (subclass_crt != 0) {
            //ESUB/ALLSUB - Result
            metrics.setEsub((double) ESUB_crt / subclass_crt);
            //DISSUB/ALLSUB - Result
            metrics.setDsub((double) DSUB_crt / subclass_crt);
            //CSUB/ALLSUB - Result
            metrics.setCsub((double) CSUB_crt / subclass_crt);
        } else {
            metrics.setEsub(0);
            metrics.setDsub(0);
            metrics.setCsub(0);
        }
        //(SuperClass)Number of Chain of Existential, Depth of Chain of Existential - Result
        if (E_chn_crt < 1) max_D_E_chn_crt = 0;
        metrics.setSupechn(E_chn_crt);
        metrics.setDsupechn(max_D_E_chn_crt);
        //(SupClass)Number of Chain of Disjunction, Depth of Chain of Disjunction - Result
        if (D_chn_crt < 1) max_D_D_chn_crt = 0;
        metrics.setSupdchn(D_chn_crt);
        metrics.setDsupdchn(max_D_D_chn_crt);
        //(SubClass)Number of Chain of Existential, Depth of Chain of Existential - Result
        if (sE_chn_crt < 1) max_D_sE_chn_crt = 0;
        metrics.setSubechn(sE_chn_crt);
        metrics.setDsubechn(max_D_sE_chn_crt);
        //(SubClass)Number of Chain of Conjunction, Depth of Chain of Conjunction - Result
        if (C_chn_crt < 1) max_D_C_chn_crt = 0;
        metrics.setSubcchn(C_chn_crt);
        metrics.setDsubcchn(max_D_C_chn_crt);
        //Transitive role, Inverse role, Role hierarchy and Disjunction - Result
        //ont_Map.put(ontName,InvokedMap);
        metrics.setHlc(HLC);
        double RHLC = HLC / numOfLogicalAxiom;
        if (numOfLogicalAxiom != 0 && !Double.isNaN(RHLC)) {
            metrics.setRHhlc(RHLC);
        } else {
            metrics.setRHhlc(0);
        }


        //Get all Class Expressions in Ontology
        Set<OWLClassExpression> set = new HashSet<OWLClassExpression>();
        for (OWLOntology ont : ontology.getImportsClosure()) {
            set.addAll(ont.getNestedClassExpressions());
        }

        //% of EL Class Expression in Ontology
        //System.out.println("Start calculate % of EL Class Expression");
        double tNum = set.size();
        double countEL = 0;
        for (OWLClassExpression exp : set) {
            if (checkEL(exp)) {
                countEL++;
            }
        }
        double ELpercent = (countEL / tNum);
        if (!Double.isNaN(ELpercent)) {
            metrics.setELclass_prt(ELpercent);
        } else {
            metrics.setELclass_prt(0);
        }
        //System.out.println("End calculate % of EL Class Expression");


        //% of EL axiom in Ontology
        //System.out.println("Start calculate % of EL axiom");
        double ELaxiom = 0;
        boolean flag = false;
        Set<OWLAxiom> axiomSet = removePropertyAxiom(ontology.getTBoxAxioms(true));
        double taxiomnum = axiomSet.size();
        for (OWLAxiom ax : axiomSet) {
            if (ax.getAxiomType().equals(AxiomType.SUBCLASS_OF) || ax.getAxiomType().equals(AxiomType.EQUIVALENT_CLASSES)) {
                Set<OWLClassExpression> class_expSet = ax.getNestedClassExpressions();
                for (OWLClassExpression exp : class_expSet) {
                    if (!checkEL(exp)) {
                        flag = false;
                        break;
                    } else {
                        flag = true;
                    }
                }
                if (flag) {
                    ELaxiom++;
                }

            } else if (ax.getAxiomType().equals(AxiomType.DECLARATION)) {
                ELaxiom++;
            }
        }
        double ELaxiompercent = (ELaxiom / taxiomnum);
        if (!Double.isNaN(ELaxiompercent)) {
            metrics.setELaxiom_prt(ELaxiompercent);
        } else {
            metrics.setELaxiom_prt(0);
        }
        //System.out.println("End calculate % of EL axiom");

        //% of EL properties in Ontology
        //System.out.println("Start calculate % of EL properties");
        double Elprop_count = propertyMetrics.getFunc_prt_cnt() + propertyMetrics.getSub_prt_cnt() + propertyMetrics.getTrans_prt_cnt() + propertyMetrics.getChain_prt_cnt() + propertyMetrics.getRefle_prt_cnt() + propertyMetrics.getDomain_cnt() + propertyMetrics.getRange_cnt();
        double Elprop_percent = (Elprop_count / propertyMetrics.calculateTotal_prt_cnt());
        if (!Double.isNaN(Elprop_percent)) {
            propertyMetrics.setEl_prop_prt(Elprop_percent);
        } else {
            propertyMetrics.setEl_prop_prt(0);
        }
        //System.out.println("End calculate % of EL properties");


        //Number of Hierarchy role invoked
        //System.out.println("Start calculate Number of Hierarchy role");
        int IHR = 0;
        Set<OWLSubObjectPropertyOfAxiom> hierarchy_propertySet = ontology.getAxioms(AxiomType.SUB_OBJECT_PROPERTY,true);
        Set<OWLObjectProperty> subpropertySet = new HashSet<OWLObjectProperty>();
        for(OWLSubObjectPropertyOfAxiom axiom : hierarchy_propertySet){
            OWLObjectPropertyExpression subProperty = axiom.getSubProperty();
            if(!subProperty.isAnonymous() && !subpropertySet.contains(subProperty.asOWLObjectProperty())){
                subpropertySet.add(subProperty.asOWLObjectProperty());
            }
        }
        for(OWLObjectProperty property : subpropertySet){
            int total_invoking_role = 0;
            Set<OWLAxiom> axioms = property.getReferencingAxioms(ontology, true);
            Set<OWLAxiom> referencingAxioms = addInvokedAxiomsForProperty(axioms);
            total_invoking_role+=referencingAxioms.size();
            IHR+=referencingAxioms.size();
            metrics.getIhr_r().put(property.toString(),total_invoking_role);
        }
        metrics.setIhr(IHR);
        //System.out.println("End calculate Number of Hierarchy role");

        //Number of Inverse role invoked
        //System.out.println("Start calculate Number of Inverse role");
        int IIR = 0;
        Set<OWLInverseObjectPropertiesAxiom> inverse_property_axiom_Set = ontology.getAxioms(AxiomType.INVERSE_OBJECT_PROPERTIES,true);
        Set<OWLObjectProperty> inversepropertySet = new HashSet<OWLObjectProperty>();
        for(OWLInverseObjectPropertiesAxiom axiom : inverse_property_axiom_Set){
            OWLObjectPropertyExpression firstProperty = axiom.getFirstProperty();
            OWLObjectPropertyExpression secondProperty = axiom.getSecondProperty();
            if(!firstProperty.isAnonymous() && !inversepropertySet.contains(firstProperty.asOWLObjectProperty())){
                inversepropertySet.add(firstProperty.asOWLObjectProperty());
            }

            if(!secondProperty.isAnonymous() && !inversepropertySet.contains(secondProperty.asOWLObjectProperty())){
                inversepropertySet.add(secondProperty.asOWLObjectProperty());
            }
        }
        for(OWLObjectProperty property : inversepropertySet){
            int total_invoking_role = 0;
            Set<OWLAxiom> axioms = property.getReferencingAxioms(ontology, true);
            Set<OWLAxiom> referencingAxioms = addInvokedAxiomsForProperty(axioms);
            IIR+=referencingAxioms.size();
            metrics.getIir_r().put(property.toString(),total_invoking_role);
        }
        metrics.setIir(IIR);
        //System.out.println("End calculate Number of Inverse role");

        //Number of Transitive Role Invoked
        //System.out.println("Start calculate Number of Transitive role");
        int ITR = 0;
        Set<OWLTransitiveObjectPropertyAxiom> transitiveObjectPropertyAxiomSet = ontology.getAxioms(AxiomType.TRANSITIVE_OBJECT_PROPERTY,true);
        Set<OWLObjectProperty> transitivepropertySet = new HashSet<OWLObjectProperty>();
        for(OWLTransitiveObjectPropertyAxiom axiom : transitiveObjectPropertyAxiomSet){
            OWLObjectPropertyExpression propertyExpression = axiom.getProperty();
            if(!propertyExpression.isAnonymous() && !transitivepropertySet.contains(propertyExpression.asOWLObjectProperty())){
                transitivepropertySet.add(propertyExpression.asOWLObjectProperty());
            }
        }
        for(OWLObjectProperty property : transitivepropertySet){
            int total_invoking_role = 0;
            Set<OWLAxiom> axioms = property.getReferencingAxioms(ontology, true);
            Set<OWLAxiom> referencingAxioms = addInvokedAxiomsForProperty(axioms);
            total_invoking_role+=referencingAxioms.size();
            ITR+=referencingAxioms.size();
            metrics.getItr_r().put(property.toString(),total_invoking_role);
        }
        metrics.setItr(ITR);
        //System.out.println("End calculate Number of Transitive role");


        // Individuals
        metrics.setInd(noIndividuals);

        if (measureExrpessivity) {
            // Ontology profiles
            ProfileReporter reporter = new ProfileReporter();
            try {
                metrics.setProfiles(reporter.reportProfile(ontology));
            } catch (Exception e) {
                metrics.setProfiles(Collections.<ProfileReporter.OWL2Profiles, Boolean>emptyMap());
            }

            // DL expressivity
            metrics.setExpressivity(reporter.getDLExpressivity(ontology));
        }

        // NOC
        metrics.setClaStats(OntMetrics.MetricName.NOC, new AggregratedClassMetric(nocMax, nocTotal, (double) nocTotal / noClasses));

        // CID
        metrics.setClaStats(OntMetrics.MetricName.CID, new AggregratedClassMetric(cidMax, cidTotal, (double) cidTotal / noClasses));

        // COD
        metrics.setClaStats(OntMetrics.MetricName.COD, new AggregratedClassMetric(codMax, codTotal, (double) codTotal / noClasses));

        // DIT
        metrics.setClaStats(OntMetrics.MetricName.DIT, new AggregratedClassMetric(ditMax, ditTotal, (double) ditTotal / noClasses));

        // NOP
        metrics.setClaStats(OntMetrics.MetricName.NOP, new AggregratedClassMetric(nopMax, nopTotal, (double) nopTotal / noClasses));

        // TIP
        metrics.setTip(noSubCls - noClasses + 1);

        // DL constructors
        for (OWLClassExpression exp : set) {
            ClassExpressionType expressionType = exp.getClassExpressionType();
            dlcMetrics.getMap().put(expressionType, dlcMetrics.getMap().get(expressionType) + 1);
        }
        dlcMetrics.setClsCount(noClasses);
        metrics.setDlcMetrics(dlcMetrics);
        metrics.setRch((double) dlcMetrics.getAnonCount() / noClasses);

        // Prop constructors
        if (0 != noProperties) {
            propertyMetrics.setData_prt((double) propertyMetrics.getData_prt_cnt() / noProperties);
            propertyMetrics.setObj_prt((double) propertyMetrics.getObj_prt_cnt() / noProperties);
            propertyMetrics.setFunc_prt((double) propertyMetrics.getFunc_prt_cnt() / noProperties);
            propertyMetrics.setInv_func_prt((double) propertyMetrics.getInv_func_prt_cnt() / noProperties);
            propertyMetrics.setSym_prt((double) propertyMetrics.getSym_prt_cnt() / noProperties);
            propertyMetrics.setTrans_prt((double) propertyMetrics.getTrans_prt_cnt() / noProperties);
            //propertyMetrics.setEquiv_prt((double) propertyMetrics.getEquiv_prt_cnt() / noProperties);
            //propertyMetrics.setInvOf((double) propertyMetrics.getInvOf_cnt() / noProperties);
            //propertyMetrics.setDisjoint((double) propertyMetrics.getDisjoint() / noProperties);
            propertyMetrics.setChain_prt((double) propertyMetrics.getChain_prt_cnt() / noProperties);
        }
        metrics.setPropertyMetrics(propertyMetrics);

        /*
        StrongConnectivityInspector<NamedNode, NamedParamEdge> connectivityInspector = new StrongConnectivityInspector<NamedNode, NamedParamEdge>(graph);
        List<Set<NamedNode>> sets = connectivityInspector.stronglyConnectedSets();
        int sccCount = 0;
        Set<NamedNode> scc = Sets.newLinkedHashSet();
        for (Set<NamedNode> s : sets) {
            if (s.size() > 1) {
                sccCount++;
                scc.addAll(s);
            }
        }
        logger.info("Ont : " + ontName + " # scc = " + sccCount + ", # connected nodes = " + scc.size());
        */

        // add missing classes back to class-level metrics
        // NOC
        addBackMissingClasses(metrics, allNodeHits, "Noc");

        // NOP
        addBackMissingClasses(metrics, allNodeHits, "Nop");

        // CID
        addBackMissingClasses(metrics, allNodeHits, "Cid");

        // COD
        addBackMissingClasses(metrics, allNodeHits, "Cod");

        // DIT
        addBackMissingClasses(metrics, allNodeHits, "Dit");

        // HLC_C
        addBackMissingClasses(metrics, allNodeHits, "Hlc_c");

        // ihr_r
        addBackMissingClasses(metrics, allNodeHits, "Ihr_r");

        // iir_r
        addBackMissingClasses(metrics, allNodeHits, "Iir_r");

        // itr_r
        addBackMissingClasses(metrics, allNodeHits, "Itr_r");

        logger.info("All metrics calculated.");
        return metrics;
    }

    private Set<OWLAxiom> addInvokedAxiomsForProperty(Set<OWLAxiom> axioms) {
        Set<OWLAxiom> set = new HashSet<OWLAxiom>();
        for (OWLAxiom a : axioms) {
            if (a instanceof OWLClassAxiom || a instanceof OWLClassAssertionAxiom || a instanceof OWLPropertyAssertionAxiom) {
                set.add((a));
            }
        }

        return set;
    }

    @SuppressWarnings("unchecked")
    private void addBackMissingClasses(OntMetrics metrics, Set<NamedNode> allNodeHits, String metricName)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String getterMethod = "get" + metricName;
        Method m = metrics.getClass().getMethod(getterMethod);

        Set<String> allNames, curNocNames;
        allNames = Sets.newHashSet();
        for (NamedNode n : allNodeHits) {
            if (n.getTypes().contains(OWLRDFVocabulary.OWL_CLASS.toString())) {
                allNames.add(n.getName());
            }
        }

        Map<String, Integer> map = (Map<String, Integer>) m.invoke(metrics);
        curNocNames = map.keySet();
        if (allNames.removeAll(curNocNames)) {
            for (String n : allNames) {
                map.put(n, 0);
            }
        }
    }

    // Calculate Difficult Role
    private int calculateDifficultRole(Set<OWLObjectProperty> propertySet, String type, OWLOntology ont) {
        int count = 0;
        if (type.equals("transitive")) {
            for (OWLObjectProperty r : propertySet) {
                if (r.isTransitive(ont)) {
                    count++;
                }
            }
        } else if (type.equals("inverse")) {
            for (OWLObjectProperty r : propertySet) {
                Set<OWLObjectPropertyExpression> inverses = r.getInverses(ont);
                if (inverses.size() > 0) {
                    count++;
                }
            }
        } else if (type.equals("hierarchy")) {
            for (OWLObjectProperty r : propertySet) {
                if (r.getSubProperties(ont).size() > 0 || r.getSuperProperties(ont).size() > 0) {
                    count++;
                }
            }
        }
        return count;
    }

    // Calculate the invoking of a Class
    private int calculateDisjunctionInvoking(OWLClassExpression cls) {
        int totalInvoking = 0;
        int count = 0;
        if (cls.getClassExpressionType().equals(ClassExpressionType.OBJECT_UNION_OF)) {
            //Set<OWLClassExpression> cls_set = cls.asDisjunctSet();
            count = 1;
            totalInvoking += count;
        } else if (cls.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)) {
            OWLClassExpression filler = ((OWLObjectSomeValuesFrom) cls).getFiller();
            if (filler.isAnonymous()) {
                count = calculateDisjunctionInvoking(filler);
                totalInvoking += count;
            }
        }else if (cls.getClassExpressionType().equals(ClassExpressionType.OBJECT_ALL_VALUES_FROM)) {
            OWLClassExpression filler = ((OWLObjectAllValuesFrom) cls).getFiller();
            if(filler.isAnonymous()){
                count = calculateDisjunctionInvoking(filler);
                totalInvoking += count;
            }
        }else if (cls.getClassExpressionType().equals(ClassExpressionType.OBJECT_MAX_CARDINALITY)) {
            OWLClassExpression filler = ((OWLObjectMaxCardinality) cls).getFiller();
            if(filler.isAnonymous()){
                count = calculateDisjunctionInvoking(filler);
                totalInvoking += count;
            }
        }else if (cls.getClassExpressionType().equals(ClassExpressionType.OBJECT_MIN_CARDINALITY)) {
            OWLClassExpression filler = ((OWLObjectMinCardinality) cls).getFiller();
            if(filler.isAnonymous()){
                count = calculateDisjunctionInvoking(filler);
                totalInvoking += count;
            }
        } else if (cls.getClassExpressionType().equals(ClassExpressionType.OBJECT_INTERSECTION_OF)) {
            Set<OWLClassExpression> cls_set = cls.asConjunctSet();
            for (OWLClassExpression aCls : cls_set) {
                if(aCls.isAnonymous()) {
                    count = calculateDisjunctionInvoking(aCls);
                    totalInvoking += count;
                }
            }
        }

        return totalInvoking;
    }

    //Calculate the depth of chain of language constructors
    private int calculateDepth(OWLClassExpression cls, ClassExpressionType type) {
        int Depin = 0;
        int count = 0, max = 0;
        int max_size = 0, size = 0;
        OWLClassExpression target = null;

        if (cls.getClassExpressionType().equals(type)) {
            Depin++;
        }

        if (cls.getClassExpressionType().equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM)) {
            target = ((OWLObjectSomeValuesFrom) cls).getFiller();
            max = calculateDepth(target, type);
        } else if (cls.getClassExpressionType().equals(ClassExpressionType.OBJECT_INTERSECTION_OF) || cls.getClassExpressionType().equals(ClassExpressionType.OBJECT_UNION_OF)) {
            Set<OWLClassExpression> cls_set = cls.asConjunctSet();
            if (cls.getClassExpressionType().equals(ClassExpressionType.OBJECT_UNION_OF)) {
                cls_set = cls.asDisjunctSet();
            }
            for (OWLClassExpression onecls : cls_set) {
                count = calculateDepth(onecls, type);
                if (count > max) {
                    max = count;
                    count = 0;
                }
            }
        } else if (cls.getClassExpressionType().equals(ClassExpressionType.OWL_CLASS) && !tested.contains(cls.asOWLClass())) {
            tested.add(cls.asOWLClass());
            calculatingDepthSet.add(cls.asOWLClass());
            Set<OWLClassAxiom> cls_axiom = new HashSet<OWLClassAxiom>();
            for (OWLOntology ont : ontology.getImportsClosure()) {
                cls_axiom.addAll(ont.getAxioms(cls.asOWLClass()));
            }
            if (!cls_map.containsKey(cls.asOWLClass())) {
                for (OWLClassAxiom cc : cls_axiom) {
                    if (cc.getAxiomType().equals(AxiomType.SUBCLASS_OF)) {
                        OWLClassExpression inside_supCls = ((OWLSubClassOfAxiom) cc).getSuperClass();
                        count = calculateDepth(inside_supCls, type);
                        if (count > max) {
                            max = count;
                            count = 0;
                        }

                    }
                }
                Map<ClassExpressionType, Integer> map = new HashMap<ClassExpressionType, Integer>();
                map.put(type, max);
                cls_map.put(cls.asOWLClass(), map);
            } else if (!cls_map.get(cls.asOWLClass()).containsKey(type)) {
                for (OWLClassAxiom cc : cls_axiom) {
                    if (cc.getAxiomType().equals(AxiomType.SUBCLASS_OF)) {
                        OWLClassExpression inside_supCls = ((OWLSubClassOfAxiom) cc).getSuperClass();
                        count = calculateDepth(inside_supCls, type);
                        if (count > max) {
                            max = count;
                            count = 0;
                        }

                    }
                }
                cls_map.get(cls.asOWLClass()).put(type, max);
            } else {
                count = cls_map.get(cls.asOWLClass()).get(type);
                if (count > max) {
                    max = count;
                    count = 0;
                }
            }

        }

        Depin += max;
        ////System.out.println("Finish calculateDepth: " + cls);
        return Depin;
    }

    //Check EL
    private boolean checkEL(OWLClassExpression expression) {
        ClassExpressionType type = expression.getClassExpressionType();
        if (type.equals(ClassExpressionType.OBJECT_INTERSECTION_OF) || type.equals(ClassExpressionType.OBJECT_SOME_VALUES_FROM) || type.equals(ClassExpressionType.OWL_CLASS)) {
            return true;
        } else {
            return false;
        }
    }

    //Remove Property Axiom
    private Set<OWLAxiom> removePropertyAxiom(Set<OWLAxiom> axiomSet) {
        Set<OWLAxiom> newaxSet = new HashSet<OWLAxiom>();
        for (OWLAxiom ax : axiomSet) {
            if (!(ax.getAxiomType().toString().contains("Property")||ax.getAxiomType().toString().contains("Properties"))) {
                newaxSet.add(ax);
            }
        }
        return newaxSet;
    }


    // TODO: should we only handle named classes here?
    private Map<Integer, Set<String>> handleNode(OntMetrics metrics, Map<Integer, Set<String>> edgeNodeMap, NamedNode node) {

        // EOG
        Set<NamedParamEdge> query = graph.edgesOf(node);
        int size = query.size();
        edgeNodeMap = updateEdgeCount(edgeNodeMap, node, size);

        //Object type = node.getProperties().get(NODE_TYPE_NAME);
        SortedSet<String> types = node.getTypes();
        if (types.contains(OWL2Graph.OWL_CLASS_NAME)) {
            noClasses++;
            handleClassNode(metrics, node);
        } else if (types.contains(OWL_DATA_PROPERTY.toString())) {
            noProperties++;
            handleProperties(node, OWL_DATA_PROPERTY.toString());
        } else if (types.contains(OWL_OBJECT_PROPERTY.toString())) {
            noProperties++;
            handleProperties(node, OWL_OBJECT_PROPERTY.toString());
        } else if (types.contains(OWL_INDIVIDUAL.toString())) {
            noIndividuals++;
        }
        return edgeNodeMap;
    }

    private void handleProperties(NamedNode node, Object type) {
        if (type.equals(OWL_DATA_PROPERTY.toString())) {
            propertyMetrics.setData_prt_cnt(propertyMetrics.getData_prt_cnt() + 1);
        } else {
            propertyMetrics.setObj_prt_cnt(propertyMetrics.getObj_prt_cnt() + 1);
        }
        Set<String> kind = (Set<String>) node.getProperties().get(OWL2Graph.PROPERTY_KIND);
        if (kind == null) {
            kind = Collections.emptySet();
        }
        if (kind.contains(OWLRDFVocabulary.OWL_FUNCTIONAL_PROPERTY.toString())) {
            propertyMetrics.setFunc_prt_cnt(propertyMetrics.getFunc_prt_cnt() + 1);
        } else if (kind.contains(OWLRDFVocabulary.OWL_SYMMETRIC_PROPERTY.toString())) {
            propertyMetrics.setSym_prt_cnt(propertyMetrics.getSym_prt_cnt() + 1);
        } else if (kind.contains(OWLRDFVocabulary.OWL_TRANSITIVE_PROPERTY.toString())) {
            propertyMetrics.setTrans_prt_cnt(propertyMetrics.getTrans_prt_cnt() + 1);
        } else if (kind.contains(OWLRDFVocabulary.OWL_INVERSE_FUNCTIONAL_PROPERTY.toString())) {
            propertyMetrics.setInv_func_prt_cnt(propertyMetrics.getInv_func_prt_cnt() + 1);
        } else if (kind.contains(OWLRDFVocabulary.OWL_ASYMMETRIC_PROPERTY.toString())) {
            propertyMetrics.setAsym_prt_cnt(propertyMetrics.getAsym_prt_cnt() + 1);
        } else if (kind.contains(OWLRDFVocabulary.OWL_REFLEXIVE_PROPERTY.toString())) {
            propertyMetrics.setRefle_prt_cnt(propertyMetrics.getRefle_prt_cnt() + 1);
        } else if (kind.contains(OWLRDFVocabulary.OWL_IRREFLEXIVE_PROPERTY.toString())) {
            propertyMetrics.setIrrefle_prt_cnt(propertyMetrics.getIrrefle_prt_cnt() + 1);
        }
        for (NamedParamEdge e : graph.outgoingEdgesOf(node)) {
            if (e.getName().equals(RDFS_SUB_PROPERTY_OF.toString())) {
                propertyMetrics.setSub_prt_cnt(propertyMetrics.getSub_prt_cnt() + 1);
            } else if (e.getName().equals(OWL_EQUIVALENT_PROPERTY.toString())) {
                propertyMetrics.setEquiv_prt_cnt(propertyMetrics.getEquiv_prt_cnt() + 1);
            } else if (e.getName().equals(OWL_INVERSE_OF.toString())) {
                propertyMetrics.setInvOf_cnt(propertyMetrics.getInvOf_cnt() + 1);
            } else if (e.getName().equals(OWL_DISJOINT_WITH.toString())) {
                propertyMetrics.setDisjoint_cnt(propertyMetrics.getDisjoint_cnt() + 1);
            } else if (e.getName().equals(RDFS_DOMAIN.toString())) {
                propertyMetrics.setDomain_cnt(propertyMetrics.getDomain_cnt() + 1);
            } else if (e.getName().equals(RDFS_RANGE.toString())) {
                propertyMetrics.setRange_cnt(propertyMetrics.getRange_cnt() + 1);
            } else if (e.getName().equals(OWLRDFVocabulary.OWL_PROPERTY_CHAIN_AXIOM.toString())) {
                propertyMetrics.setChain_prt_cnt(propertyMetrics.getChain_prt_cnt() + 1);
            }

        }
    }

    private void handleClassNode(OntMetrics metrics, NamedNode node) {
        String nodeName = node.getName();

        // NOC
        int cNoc = 0;
        Set<NamedParamEdge> incomingEdges = graph.incomingEdgesOf(node);
        for (NamedParamEdge e : incomingEdges) {
            if (e.getName().equals(RDFS_SUBCLASS_OF.toString())) {
                cNoc++;
                noSubCls++;
            }
            metrics.getNoc().put(nodeName, cNoc);
        }
        if (nodeName.equals(OWL_THING.toString())) {
            return;
        }

        // NOC
        nocTotal += cNoc;
        nocMax = max(cNoc, nocMax);

        // CID
        int cCid = graph.inDegreeOf(node);
        metrics.getCid().put(nodeName, cCid);
        cidTotal += cCid;
        cidMax = max(cCid, cidMax);

        // COD
        int cCod = graph.outDegreeOf(node);
        metrics.getCod().put(nodeName, cCod);
        codTotal += cCod;
        codMax = max(cCod, codMax);

        /*
        // DIT
        int cDit = calculateDITForNode(node);
        metrics.getDit().put(nodeName, cDit);
        ditTotal += cDit;
        ditMax = max(cDit, ditMax);
        */

        // NOP
        int cNop = 0;
        Set<NamedParamEdge> outgoingEdges = graph.outgoingEdgesOf(node);
        for (NamedParamEdge e : outgoingEdges) {
            if (e.getName().equals(RDFS_SUBCLASS_OF.toString())) {
                cNop++;
            }
        }
        metrics.getNop().put(nodeName, cNop);
        nopTotal += cNop;
        nopMax = max(cNop, nopMax);

        /*
        if (Boolean.valueOf(node.getProperties().getProperty(NODE_ANON_TYPE_NAME))) {
            String anonType = node.getProperties().getProperty(ANON_CLASS_TYPE);
            if (OWL_ONE_OF.toString().equals(anonType)) {
                dlcMetrics.setEnumCount(dlcMetrics.getEnumCount() + 1);
            } else if (OWL_COMPLEMENT_OF.toString().equals(anonType)) {
                dlcMetrics.setNegCount(dlcMetrics.getNegCount() + 1);
            } else if (OWL_INTERSECTION_OF.toString().equals(anonType)) {
                dlcMetrics.setConjCount(dlcMetrics.getConjCount() + 1);
            } else if (OWL_UNION_OF.toString().endsWith(anonType)) {
                dlcMetrics.setDisjCount(dlcMetrics.getDisjCount() + 1);
            } else if (OWL_ALL_VALUES_FROM.toString().equals(anonType)) {
                dlcMetrics.setUfCount(dlcMetrics.getUfCount() + 1);
            } else if (OWL_SOME_VALUES_FROM.toString().equals(anonType)) {
                dlcMetrics.setEfCount(dlcMetrics.getEfCount() + 1);
            } else if (OWL_MAX_CARDINALITY.toString().equals(anonType)) {
                dlcMetrics.setMaxCardCount(dlcMetrics.getMaxCardCount() + 1);
            } else if (OWL_MIN_CARDINALITY.toString().equals(anonType)) {
                dlcMetrics.setMinCardCount(dlcMetrics.getMinCardCount() + 1);
            } else if (OWL_CARDINALITY.toString().equals(anonType)) {
                dlcMetrics.setCardCount(dlcMetrics.getCardCount() + 1);
            }
        }
        */
    }

    private int calculateDITForNode(NamedNode node) {
        int maxDIT = 0;
        boolean isolatedSub = true;
        for (NamedParamEdge e : graph.outgoingEdgesOf(node)) {
            if (e.getName().equals(RDFS_SUBCLASS_OF.toString())) {
                isolatedSub = false;
                break;
            }
        }
        if (isolatedSub) {
            return 0;
        }

        DepthFirstIterator<NamedNode, NamedParamEdge> iterator =
                new EdgeTypeSensitiveDepthFirstIterator(graph, node, RDFS_SUBCLASS_OF.toString());

        int curDit = 0;
        while (iterator.hasNext()) {
            NamedNode next = iterator.next();
            if (next.getName().equals(OWL_THING.toString())) {
                maxDIT = max(maxDIT, curDit);
                curDit = 0;
            } else {
                curDit++;
            }
        }
        return maxDIT;
    }

    private Map<Integer, Set<String>> updateEdgeCount(Map<Integer, Set<String>> edgeNodeMap, NamedNode node, int size) {

        Set<String> nodeNameSet = edgeNodeMap.get(size);
        if (null == nodeNameSet) {
            nodeNameSet = new HashSet<String>();
        }
        nodeNameSet.add(node.getName());
        edgeNodeMap.put(size, nodeNameSet);
        return edgeNodeMap;
    }
}
