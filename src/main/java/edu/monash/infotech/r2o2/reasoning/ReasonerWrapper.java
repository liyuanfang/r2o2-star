package edu.monash.infotech.r2o2.reasoning;

import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import eu.trowl.owlapi3.rel.reasoner.dl.RELReasonerFactory;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.more.MOReReasonerFactory;
import org.semanticweb.owlapi.owllink.OWLlinkHTTPXMLReasonerFactory;
import org.semanticweb.owlapi.owllink.OWLlinkReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.SimpleConfiguration;
import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory;
import uk.ac.manchester.cs.jfact.JFactFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.semanticweb.more.OWL2ReasonerManager.HERMIT;

/**
 * @author Yuan-Fang Li
 * @version $Id: $
 */
public class ReasonerWrapper {
    public static final ReasonerWrapper REASONER_WRAPPER;
    static {
        try {
            REASONER_WRAPPER = new ReasonerWrapper();
        } catch(MalformedURLException e) {
            System.err.println("Error initialising Konclude: " + e);
            throw new RuntimeException(e);
        }
    }

    public class ReasonerBundle {
        public final OWLReasonerFactory factory;
        public final OWLReasonerConfiguration config;

        public ReasonerBundle(OWLReasonerFactory factory, OWLReasonerConfiguration config) {
            this.factory = factory;
            this.config = config;
        }
    }

    public static enum REASONER_ID {
        CHAINSAW(0),
        FACT(1),
        HERMIT(2),
        JFACT(3),
        KONCLUDE(4),
        MORE(5),
        PELLET(6),
        TROWL(7),
        ELEPHANT(8),
        RACER(9),
        ELK(10);

        private int code;
        private static Map<Integer, REASONER_ID> map = new HashMap<Integer, REASONER_ID>(REASONER_ID.values().length);
        static {
            map.put(CHAINSAW.code, CHAINSAW);
            map.put(FACT.code, FACT);
            map.put(HERMIT.code, HERMIT);
            map.put(JFACT.code, JFACT);
            map.put(KONCLUDE.code, KONCLUDE);
            map.put(MORE.code, MORE);
            map.put(PELLET.code, PELLET);
            map.put(TROWL.code, TROWL);
            map.put(ELEPHANT.code, ELEPHANT);
            map.put(RACER.code, RACER);
            map.put(ELK.code, ELK);
        }

        private REASONER_ID(int code) {
            this.code = code;
        }

        public static REASONER_ID getEnum(int code) {
            return map.get(code);
        }
    };
    private final Map<REASONER_ID, OWLReasonerFactory> factories = new HashMap<REASONER_ID, OWLReasonerFactory>(REASONER_ID.values().length);;
    private final Map<REASONER_ID, OWLReasonerConfiguration> configs = new HashMap<REASONER_ID, OWLReasonerConfiguration>(REASONER_ID.values().length);;

    public ReasonerBundle getBundle(REASONER_ID reasonerIdx) {
        return new ReasonerBundle(factories.get(reasonerIdx), configs.get(reasonerIdx));
    }

    public ReasonerWrapper(String koncludeServerUrl) throws MalformedURLException {
        factories.put(REASONER_ID.CHAINSAW, new FaCTPlusPlusReasonerFactory());
        factories.put(REASONER_ID.FACT, new FaCTPlusPlusReasonerFactory());
        factories.put(REASONER_ID.HERMIT, new Reasoner.ReasonerFactory());
        factories.put(REASONER_ID.JFACT, new JFactFactory());
        factories.put(REASONER_ID.KONCLUDE, new OWLlinkHTTPXMLReasonerFactory());
        factories.put(REASONER_ID.MORE, new MOReReasonerFactory(HERMIT));
        factories.put(REASONER_ID.PELLET, PelletReasonerFactory.getInstance());
        factories.put(REASONER_ID.TROWL, new RELReasonerFactory());
        factories.put(REASONER_ID.ELK, new ElkReasonerFactory());
        factories.put(REASONER_ID.RACER, new RELReasonerFactory());

        configs.put(REASONER_ID.CHAINSAW, new SimpleConfiguration());
        configs.put(REASONER_ID.FACT, new SimpleConfiguration());
        configs.put(REASONER_ID.HERMIT, new SimpleConfiguration());
        configs.put(REASONER_ID.JFACT, new SimpleConfiguration());
        configs.put(REASONER_ID.MORE, new SimpleConfiguration());
        configs.put(REASONER_ID.KONCLUDE, new OWLlinkReasonerConfiguration(new URL(koncludeServerUrl)));
        configs.put(REASONER_ID.PELLET, new SimpleConfiguration());
        configs.put(REASONER_ID.TROWL, new SimpleConfiguration());
        configs.put(REASONER_ID.ELK, new SimpleConfiguration());
        configs.put(REASONER_ID.RACER, new SimpleConfiguration());
    }

    public ReasonerWrapper() throws MalformedURLException {
        this("http://localhost:8081");
    }
}
