package edu.monash.infotech.owl2metrics.metrics.writer;

import edu.monash.infotech.owl2metrics.model.AggregratedClassMetric;
import edu.monash.infotech.owl2metrics.model.DLCMetrics;
import edu.monash.infotech.owl2metrics.model.OntMetrics;
import edu.monash.infotech.owl2metrics.model.PropertyMetrics;
import edu.monash.infotech.owl2metrics.profiles.ProfileReporter;

import java.util.Arrays;
import java.util.Map;

import static java.lang.String.valueOf;

/**
 * @author Yuan-Fang Li
 * @version $Id: $
 */
public class MetricsFormatter {
    private boolean measureExpressivity;

    public MetricsFormatter(boolean measureExpressivity) {
        this.measureExpressivity = measureExpressivity;
    }

    public static String[] getHeader(boolean measureExpressivity) {
        String[] commonPart = {"Ontology",
                "SIZE (KB)",
                "SOV", "ENR", "TIP", "EOG", "RCH", "CYC", "GCI", "HGCI","ESUB", "DSUB", "CSUB", "SUPECHN", "SUBECHN", "SUBCCHN",
                "SUPDCHN", "DSUPECHN", "DSUBECHN", "DSUBCCHN", "DSUPDCHN","ELCLSPRT","ELAXPRT","HLC","RHLC","IHR","IIR","ITR", "IND",
                "aNOC", "mNOC", "tNOC", "aCID", "mCID", "tCID", "aCOD", "mCOD", "tCOD", "aDIT", "mDIT", "tDIT",  "aNOP", "mNOP", "tNOP",
                "ENUM", "ENUMP", "NEG", "NEGP", "CONJ", "CONJP", "DISJ", "DISJP", "UF", "UFP", "EF", "EFP", "VALUE", "VALUEP", "SELF", "SELFP", "MNCAR", "MNCARP", "MXCAR", "MXCARP", "CAR", "CARP",
                "OBP", "OBPP", "DTP", "DTPP", "FUN", "FUNP", "SYM", "SYMP", "TRN", "TRNP", "IFUN", "IFUNP", "ASYM", "ASYMP", "REFLE", "REFLEP", "IRREF", "IRREFP",
                "SUBP", "EQVP", "DISP", "INV", "DOMN", "RANG", "CHN", "CHNP","ELPROP"
        };

        String[] array;
        if (measureExpressivity) {
            array = concat(commonPart, new String[] {"EXPRESSIVITY", "EL", "QL", "RL", "DL", "FULL"});
        } else {
            array = commonPart;
        }

        // include calculation time
        array = concat(array, new String[]{"CAL_TIME"});
        return array;

    }

    public String[] formatMetrics(String ontologyName, OntMetrics metrics) {
        // name
        String[] array = {ontologyName};
        // ont metrics
        String[] clsArray = {valueOf(metrics.getSze()),
                valueOf(metrics.getSov()), valueOf(metrics.getEnr()), valueOf(metrics.getTip()),
                valueOf(metrics.getEog()), valueOf(metrics.getRch()), valueOf(metrics.getCyc()),
                valueOf(metrics.getGci()), valueOf(metrics.getHgci()), valueOf(metrics.getEsub()),
                valueOf(metrics.getDsub()), valueOf(metrics.getCsub()), valueOf(metrics.getSupechn()),
                valueOf(metrics.getSubechn()), valueOf(metrics.getSubcchn()), valueOf(metrics.getDsupdchn())
                , valueOf(metrics.getDsupechn()), valueOf(metrics.getDsubechn()), valueOf(metrics.getDsubcchn()),
                valueOf(metrics.getDsupdchn()),valueOf(metrics.getELclass_prt()),valueOf(metrics.getELaxiom_prt()),
                valueOf(metrics.getHlc()),valueOf(metrics.getRhlc()),valueOf(metrics.getIhr()),valueOf(metrics.getIir()),
                valueOf(metrics.getItr()),valueOf(metrics.getInd())};
        array = concat(array, clsArray);
        // class metrics
        array = concat(array, getStringFromClsMetrics(metrics.getClasStats(OntMetrics.MetricName.NOC)));
        array = concat(array, getStringFromClsMetrics(metrics.getClasStats(OntMetrics.MetricName.CID)));
        array = concat(array, getStringFromClsMetrics(metrics.getClasStats(OntMetrics.MetricName.COD)));
        array = concat(array, getStringFromClsMetrics(metrics.getClasStats(OntMetrics.MetricName.DIT)));
        array = concat(array, getStringFromClsMetrics(metrics.getClasStats(OntMetrics.MetricName.NOP)));
        // dl metrics
        DLCMetrics dlcMetrics = metrics.getDlcMetrics();
        array = copyDLMetrics(array, dlcMetrics);
        // property metrics
        PropertyMetrics propertyMetrics = metrics.getPropertyMetrics();
        array = copyPropertyMetrics(array, propertyMetrics);


        if (measureExpressivity) {
            // Expressivity metrics
            String expressivity = metrics.getExpressivity();
            array = concat(array, new String[]{expressivity});

            // profiles
            String[] profiles = ProfileReporter.OWL2Profiles.getProfiles(metrics.getProfiles());
            array = concat(array, profiles);
        }

        // include calculation time
        array = concat(array, new String[]{Long.toString(metrics.getCalculationTime())});

        return array;
    }

    private String[] copyPropertyMetrics(String[] array, PropertyMetrics metrics) {
        Map<String, Object> map = metrics.asMap();
        String[] propArray = new String[map.size()];
        int i = 0;
        for (String key : map.keySet()) {
            propArray[i++] = map.get(key).toString();
        }
        array = concat(array, propArray);
        return array;
    }

    private String[] copyDLMetrics(String[] array, DLCMetrics dlcMetrics) {
        Map<String,Object> map = dlcMetrics.asMap();
        String[] dl = new String[map.size()];
        int i = 0;
        for (String key : map.keySet()) {
            dl[i++] = map.get(key).toString();
        }
        array = concat(array, dl);
        return array;
    }

    private String[] getStringFromClsMetrics(AggregratedClassMetric metrics) {
        String[] array = new String[3];

        array[0] = valueOf(metrics.getAvg());
        array[1] = valueOf(metrics.getMax());
        array[2] = valueOf(metrics.getTotal());

        return array;
    }

    public static <T> T[] concat(T[] first, T[] second) {
        T[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }
}
