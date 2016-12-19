package edu.monash.infotech.owl2metrics.translate.jgrapht;

import com.google.common.collect.Maps;
import edu.monash.infotech.owl2metrics.translate.AbstractOWL2GraphImpl;
import edu.monash.infotech.owl2metrics.translate.OWL2Graph;
import edu.monash.infotech.owl2metrics.translate.all.ClassExpNodeAdder;
import edu.monash.infotech.owl2metrics.translate.all.NodeRelAddingVisitor;
import edu.monash.infotech.owl2metrics.translate.jgrapht.graph.IndexedDirectedGraph;
import edu.monash.infotech.owl2metrics.translate.jgrapht.graph.IndexedDirectedGraphImpl;
import edu.monash.infotech.owl2metrics.translate.jgrapht.graph.NamedNode;
import edu.monash.infotech.owl2metrics.translate.jgrapht.graph.NamedParamEdge;
import org.jgrapht.DirectedGraph;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.io.OWLOntologyDocumentSource;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.SimpleRenderer;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyIRIMapperImpl;

import java.util.Map;
import java.util.Set;

import static org.semanticweb.owlapi.vocab.OWLRDFVocabulary.*;

/**
 * @author Yuan-Fang Li
 * @version $Id: $
 */
public class OWL2GraphJGraphTImpl extends AbstractOWL2GraphImpl<DirectedGraph<NamedNode, NamedParamEdge>, NamedNode, NamedParamEdge>
        implements OWL2Graph<DirectedGraph<NamedNode, NamedParamEdge>, NamedNode, NamedParamEdge> {

    protected IndexedDirectedGraph<NamedNode, NamedParamEdge> graph;

    public OWL2GraphJGraphTImpl() {
        super();
    }

    public DirectedGraph<NamedNode, NamedParamEdge> loadOWLOntology(OWLOntology ontology, boolean includeImports) {
        int axiomCount = 0;
        this.ontology = ontology;

        ceAdder = new ClassExpNodeAdder<NamedNode, NamedParamEdge>(this);
        createNodeRelAdder();
        graph = new IndexedDirectedGraphImpl(NamedParamEdge.class, ontology.getSignature(true).size());

        thingNode = findOrCreateClass(OWL_THING.toStringID(), OWL2Graph.OWL_CLASS_NAME, false);

        for (OWLClass cls : ontology.getClassesInSignature(true)) {
            NamedNode clsNode = findOrCreateNode(cls.toString(), OWLRDFVocabulary.OWL_CLASS.toString(), cls.isAnonymous());
            if (ontology.getSubClassAxiomsForSubClass(cls).isEmpty()) {
                //createRelationship(clsNode, thingNode, DEPENDS_ON);
                addSubclassRel(clsNode, thingNode);
            }
        }

        for (OWLDataProperty prp : ontology.getDataPropertiesInSignature(true)) {
            findOrCreateNode(prp.toString(), OWL_DATA_PROPERTY.toString(), prp.isAnonymous());
        }
        for (OWLObjectProperty prp : ontology.getObjectPropertiesInSignature(true)) {
            findOrCreateNode(prp.toString(), OWL_OBJECT_PROPERTY.toString(), prp.isAnonymous());
        }
        for (OWLIndividual ind : ontology.getIndividualsInSignature(true)) {
            findOrCreateNode(ind.toString(), OWL_INDIVIDUAL.toString(), ind.isAnonymous());
        }
        for (OWLDatatype type : ontology.getDatatypesInSignature(true)) {
            findOrCreateNode(type.toString(), OWL_DATATYPE.toString(), false);
        }

        logger.info("Total (non-imported) logical axioms # = " + ontology.getLogicalAxiomCount());
        for (AxiomType<?> type : AxiomType.AXIOM_TYPES) {
            Set<? extends OWLAxiom> axioms = ontology.getAxioms(type, includeImports);
            for (OWLAxiom axiom : axioms) {
                if (axiom.isLogicalAxiom()) {
                    if (++axiomCount % 10000 == 0) {
                        logger.debug("Processed axioms # = " + axiomCount);
                    }
                    axiom.accept(adder);
                }
            }
        }
        logger.info("Finished all logical axioms, # = " + axiomCount);
        return graph;
    }

    protected void createNodeRelAdder() {

        adder = new NodeRelAddingVisitor<NamedNode, NamedParamEdge>(this);
    }

    public DirectedGraph<NamedNode, NamedParamEdge> loadOWLOntology(OWLOntologyDocumentSource source, boolean includeImports, Map<String, String> iriMapping) throws OWLOntologyCreationException {
        return loadOWLOntology(loadOntology(source, iriMapping), includeImports);
    }

    public DirectedGraph<NamedNode, NamedParamEdge> loadOWLOntology(OWLOntologyDocumentSource source, boolean includeImports) throws OWLOntologyCreationException {
        return loadOWLOntology(source, includeImports, Maps.<String, String>newHashMap());
    }

    private OWLOntology loadOntology(OWLOntologyDocumentSource source, Map<String, String> iriMapping) throws OWLOntologyCreationException {
        OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();
        if (!iriMapping.isEmpty()) {
            OWLOntologyIRIMapperImpl mapper = new OWLOntologyIRIMapperImpl();
            for (String key : iriMapping.keySet()) {
                mapper.addMapping(IRI.create(key), IRI.create(iriMapping.get(key)));
            }
            owlOntologyManager.addIRIMapper(mapper);
        }
        OWLObjectRenderer renderer = new SimpleRenderer();
        ToStringRenderer.getInstance().setRenderer(renderer);
        return owlOntologyManager.loadOntologyFromOntologyDocument(source);
    }

    public DirectedGraph<NamedNode, NamedParamEdge> getGraph() {
        return graph;
    }

    public NamedNode findOrCreateNode(String name, String type, boolean isAnonymous) {
        NamedNode vertex;
        NamedNode node = makeNode(name, type, isAnonymous);
        if (graph.containsVertex(node)) {
            vertex = graph.getVertex(name);
        } else {
            vertex = node;
            graph.addVertex(vertex);
        }
        return vertex;
    }

    public NamedNode createNode(String name, String type, boolean isAnonymous) {
        NamedNode node = makeNode(name, type, isAnonymous);
        graph.addVertex(node);
        return node;
    }

    private NamedNode makeNode(String name, String type, boolean isAnonymous) {
        NamedNode node = new NamedNode(name);
        node.getTypes().add(type);
        node.setProperty(NODE_ANON_TYPE_NAME, Boolean.toString(isAnonymous));
        return node;
    }

    public NamedParamEdge createRelationship(NamedNode source, NamedNode target, String typeAndName) {
        if (!graph.containsEdge(source, target, typeAndName)) {
            NamedParamEdge edge = graph.addEdge(source, target);
            edge.setName(typeAndName);
            graph.addEdgeToSet(edge);
            return edge;
        } else {
            return null;
        }
    }

    public NamedParamEdge findRelationship(NamedNode source, NamedNode target, String relationshipName) {
        Set<NamedParamEdge> allEdges = graph.getAllEdges(source, target);
        if (null != allEdges) {
            for (NamedParamEdge e : allEdges) {
                if (e.getName().equals(relationshipName)) {
                    return e;
                }
            }
        }
        return null;
    }

    public <T extends OWLPropertyExpression> NamedParamEdge createRelationship(NamedNode source, NamedNode target, T property) {
        NamedParamEdge relationship = createRelationship(source, target, property.toString());
        String type;
        if (property.isObjectPropertyExpression()) {
            type = OWL_OBJECT_PROPERTY.toString();
        } else if (property.isDataPropertyExpression()) {
            type = OWL_DATA_PROPERTY.toString();
        } else {
            type = RDF_PROPERTY.toString();
        }
        NamedNode propNode = findOrCreateNode(property.toString(), type, property.isAnonymous());
        propNode.setProperty(PROP_NODE_BUILDIN, Boolean.toString(false));
        return relationship;
    }

    public void shutdown() {
        this.graph = new IndexedDirectedGraphImpl(NamedParamEdge.class, 0);
    }

    public NamedNode createNode(String name) {
        return new NamedNode(name);
    }

    public NamedNode setProperty(NamedNode namedNode, String key, String value) {
        namedNode.setProperty(key, value);
        return namedNode;
    }
}
