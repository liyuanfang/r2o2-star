package edu.monash.infotech.owl2metrics.profiles;

import org.semanticweb.owlapi.metrics.DLExpressivity;
import org.semanticweb.owlapi.metrics.OWLMetric;
import org.semanticweb.owlapi.metrics.OWLMetricManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.profiles.OWL2DLProfile;
import org.semanticweb.owlapi.profiles.OWL2ELProfile;
import org.semanticweb.owlapi.profiles.OWL2Profile;
import org.semanticweb.owlapi.profiles.OWL2QLProfile;
import org.semanticweb.owlapi.profiles.OWL2RLProfile;
import org.semanticweb.owlapi.profiles.OWLProfile;
import org.semanticweb.owlapi.profiles.OWLProfileReport;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Yuan-Fang Li
 * @version $Id: $
 */
public class ProfileReporter {
    public enum OWL2Profiles {
        EL(OWL2ELProfile.class),
        QL(OWL2QLProfile.class),
        RL(OWL2RLProfile.class),
        DL(OWL2DLProfile.class),
        FULL(OWL2Profile.class);

        private Class<? extends OWLProfile> profileClass;

        OWL2Profiles(Class<? extends OWLProfile> profileClass) {
            this.profileClass = profileClass;
        }

        public Class<? extends OWLProfile> getProfileClass() {
            return profileClass;
        }

        public static String[] getHeaders() {
            return new String[] {"EL", "QL", "RL", "DL", "FULL"};
        }

        public static String[] getProfiles(Map<OWL2Profiles, Boolean> map) {
            return new String[]{map.get(EL).toString(), map.get(QL).toString(), map.get(RL).toString(), map.get(DL).toString(), map.get(FULL).toString()};
        }
    }

    public  Map<OWL2Profiles, Boolean> reportProfile(OWLOntology ontology) throws IllegalAccessException, InstantiationException {
        Map<OWL2Profiles, Boolean> result = new LinkedHashMap<OWL2Profiles, Boolean>();

        for (OWL2Profiles p : OWL2Profiles.values()) {
            Class<? extends OWLProfile> profileClass = p.getProfileClass();
            OWLProfile owlProfile = profileClass.newInstance();
            OWLProfileReport report = owlProfile.checkOntology(ontology);
            result.put(p, report.isInProfile() && report.getViolations().isEmpty());
        }

        return result;
    }
    
    public String getMinProfile(Map<OWL2Profiles, Boolean> map) {
        String profile = "UNKNOWN";

        if (map.get(OWL2Profiles.EL)) {
            profile = OWL2Profiles.EL.name();
        } else if (map.get(OWL2Profiles.QL)) {
            profile = OWL2Profiles.QL.name();
        } else if (map.get(OWL2Profiles.RL)) {
            profile = OWL2Profiles.RL.name();
        } else if (map.get(OWL2Profiles.DL)) {
            profile = OWL2Profiles.DL.name();
        } else if (map.get(OWL2Profiles.FULL)) {
            profile = OWL2Profiles.FULL.name();
        }

        return profile;
    }

    public String getDLExpressivity(OWLOntology ontology) {
        DLExpressivity expressivity = new DLExpressivity(ontology.getOWLOntologyManager());
        OWLMetricManager metricManager = new OWLMetricManager(Collections.<OWLMetric<?>>singletonList(expressivity));
        metricManager.setOntology(ontology);
        return expressivity.recomputeMetric();
    }
}


