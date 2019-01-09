package edu.monash.infotech.owl2metrics.model;

import edu.monash.infotech.owl2metrics.profiles.ProfileReporter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static edu.monash.infotech.owl2metrics.model.OntMetrics.MetricName.CID;
import static edu.monash.infotech.owl2metrics.model.OntMetrics.MetricName.COD;
import static edu.monash.infotech.owl2metrics.model.OntMetrics.MetricName.DIT;
import static edu.monash.infotech.owl2metrics.model.OntMetrics.MetricName.NOC;
import static edu.monash.infotech.owl2metrics.model.OntMetrics.MetricName.NOP;

/**
 * @author Yuan-Fang Li
 * @version $Id: OntMetrics.java 163 2015-11-18 02:33:22Z yli $
 */
public class OntMetrics {
    private String ontName;

    private long calculationTime;

    public long getCalculationTime() {
        return calculationTime;
    }

    public void setCalculationTime(long calculationTime) {
        this.calculationTime = calculationTime;
    }

    public DLCMetrics getDlcMetrics() {
        return dlcMetrics;
    }

    public void setDlcMetrics(DLCMetrics dlcMetrics) {
        this.dlcMetrics = dlcMetrics;
    }

    public PropertyMetrics getPropertyMetrics() {
        return propertyMetrics;
    }

    public void setPropertyMetrics(PropertyMetrics propertyMetrics) {
        this.propertyMetrics = propertyMetrics;
    }

    public double getRch() {
        return rch;
    }

    public void setRch(double rch) {
        this.rch = rch;
    }

    public int getCyc() {
        return cyc;
    }

    public void setCyc(int cyc) {
        this.cyc = cyc;
    }

    public String getOntName() {
        return ontName;
    }

    public void setOntName(String ontName) {
        this.ontName = ontName;
    }

    public double getSze() {
        return sze;
    }

    public void setSze(double sze) {
        this.sze = sze;
    }

    public Map<String, Integer> getNop() {
        return nop;
    }

    public void setNop(Map<String, Integer> nop) {
        this.nop = nop;
    }

    public String getExpressivity() {
        return expressivity;
    }

    public void setExpressivity(String expressivity) {
        this.expressivity = expressivity;
    }

    public int getGci() {
        return gci;
    }

    public void setGci(int gci) {
        this.gci = gci;
    }

    public int getHgci() {
        return hgci;
    }

    public void setHgci(int hgci) {
        this.hgci = hgci;
    }

    public Map<ProfileReporter.OWL2Profiles, Boolean> getProfiles() {
        return profiles;
    }

    public void setProfiles(Map<ProfileReporter.OWL2Profiles, Boolean> profiles) {
        this.profiles = profiles;
    }

    public int getInd() {
        return ind;
    }

    public void setInd(int ind) {
        this.ind = ind;
    }

    public double getELclass_prt() {
        return ELclass_prt;
    }

    public void setELclass_prt(double ELclass_prt) {
        this.ELclass_prt = ELclass_prt;
    }

    public double getELaxiom_prt() {
        return ELaxiom_prt;
    }

    public void setELaxiom_prt(double ELaxiom_prt) {
        this.ELaxiom_prt = ELaxiom_prt;
    }

    public int getHlc() {
        return hlc;
    }

    public void setHlc(int hlc) {
        this.hlc = hlc;
    }

    public double getRhlc() {
        return rhlc;
    }

    public void setRHhlc(double rhlc) {
        this.rhlc = rhlc;
    }

    public Map<String, Integer> getHlc_c() {
        return hlc_c;
    }

    public void setHlc_c(Map<String, Integer> hlc_c) {
        this.hlc_c = hlc_c;
    }

    public int getIhr() {
        return ihr;
    }

    public void setIhr(int ihr) {
        this.ihr = ihr;
    }

    public int getIir() {
        return iir;
    }

    public void setIir(int iir) {
        this.iir = iir;
    }

    public Map<String, Integer> getIhr_r() {
        return ihr_r;
    }

    public void setIhr_r(Map<String, Integer> ihr_r) {
        this.ihr_r = ihr_r;
    }

    public Map<String, Integer> getIir_r() {
        return iir_r;
    }

    public void setIir_r(Map<String, Integer> iir_r) {
        this.iir_r = iir_r;
    }

    public int getItr() {
        return itr;
    }

    public void setItr(int itr) {
        this.itr = itr;
    }

    public Map<String, Integer> getItr_r() {
        return itr_r;
    }

    public void setItr_r(Map<String, Integer> itr_r) {
        this.itr_r = itr_r;
    }

    public enum MetricName {
        SZE("sze"),
        NOC("noc"),
        NOP("nop"),
        CID("cid"),
        COD("cod"),
        DIT("dit"),
        GCI("gci"),
        HGCI("hgci"),
        ESUB("esub"),
        DSUB("dsub"),
        CSUB("dsub"),
        SUPECHN("supechn"),
        SUBECHN("subechn"),
        SUBCCHN("subcchn"),
        SUPDCHN("supdchn"),
        ELCLSPRT("ELclass_prt"),
        ELAXPRT("ELaxiom_prt"),
        HLC("hlc"),
        RHLC("rhlc");


        private final String name;

        private MetricName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private double sze;
    private int sov;
    private double enr;
    private int tip;
    private double eog;
    private double rch;
    private int cyc;
    private int gci;
    private int hgci;
    private int ind;

    private double esub;
    private double dsub;
    private double csub;

    private int supechn;
    private int subechn;
    private int subcchn;
    private int supdchn;

    private int dsupechn;
    private int dsubechn;
    private int dsubcchn;
    private int dsupdchn;

    private double ELclass_prt;
    private double ELaxiom_prt;

    private int hlc;
    private double rhlc;

    private int ihr;
    private int iir;
    private int itr;

    private String expressivity;

    private Map<String, Integer> noc;
    private Map<String, Integer> dit;
    private Map<String, Integer> cid;
    private Map<String, Integer> cod;
    private Map<String, Integer> nop;
    private Map<String, Integer> hlc_c;
    private Map<String, Integer> ihr_r;
    private Map<String, Integer> iir_r;
    private Map<String, Integer> itr_r;

    private Map<MetricName, AggregratedClassMetric> clsStats;

    private DLCMetrics dlcMetrics;
    private PropertyMetrics propertyMetrics;

    private Map<ProfileReporter.OWL2Profiles, Boolean> profiles;

    public OntMetrics(String ontName) {
        this.ontName = ontName;

        sze = 0;
        sov = 0;
        enr = 0;
        tip = 0;
        eog = 0;
        rch = 0;
        cyc = 0;
        gci = 0;
        hgci = 0;
        ind = 0;
        supechn = 0;
        subechn = 0;
        subcchn = 0;
        supdchn = 0;

        dsupechn = 0;
        dsubechn = 0;
        dsubcchn = 0;
        dsupdchn = 0;

        ELclass_prt = 0;
        ELaxiom_prt = 0;

        hlc = 0;
        rhlc = 0;

        ihr = 0;
        iir = 0;
        itr = 0;


        noc = new HashMap<String, Integer>();
        dit = new HashMap<String, Integer>();
        cid = new HashMap<String, Integer>();
        cod = new HashMap<String, Integer>();
        nop = new HashMap<String, Integer>();
        hlc_c = new HashMap<String, Integer>();
        ihr_r = new HashMap<String, Integer>();
        iir_r = new HashMap<String, Integer>();
        itr_r = new HashMap<String, Integer>();

        clsStats = new HashMap<MetricName, AggregratedClassMetric>();
    }

    public int getSov() {
        return sov;
    }

    public void setSov(int sov) {
        this.sov = sov;
    }

    public double getEnr() {
        return enr;
    }

    public void setEnr(double enr) {
        this.enr = enr;
    }

    public int getTip() {
        return tip;
    }

    public void setTip(int tip) {
        this.tip = tip;
    }

    public double getEog() {
        return eog;
    }

    public void setEog(double eog) {
        this.eog = eog;
    }

    public AggregratedClassMetric getClasStats (MetricName metricName) {
        return clsStats.get(metricName);
    }

    public void setClaStats(MetricName metricName, AggregratedClassMetric metrics) {
        clsStats.put(metricName, metrics);
    }

    public Map<String, Integer> getCod() {
        return cod;
    }

    public void setCod(Map<String, Integer> cod) {
        this.cod = cod;
    }

    public Map<String, Integer> getNoc() {
        return noc;
    }

    public void setNoc(Map<String, Integer> noc) {
        this.noc = noc;
    }

    public Map<String, Integer> getDit() {
        return dit;
    }

    public void setDit(Map<String, Integer> dit) {
        this.dit = dit;
    }

    public Map<String, Integer> getCid() {
        return cid;
    }

    public void setCid(Map<String, Integer> cid) {
        this.cid = cid;
    }

    public double getEsub() {
        return esub;
    }

    public void setEsub(double esub) {
        this.esub = esub;
    }

    public double getDsub() {
        return dsub;
    }

    public void setDsub(double dsub) {
        this.dsub = dsub;
    }

    public double getCsub() {
        return csub;
    }

    public void setCsub(double csub) {
        this.csub = csub;
    }

    public int getSupechn() {
        return supechn;
    }

    public void setSupechn(int supechn) {
        this.supechn = supechn;
    }

    public int getSubechn() {
        return subechn;
    }

    public void setSubechn(int subechn) {
        this.subechn = subechn;
    }

    public int getSubcchn() {
        return subcchn;
    }

    public void setSubcchn(int subcchn) {
        this.subcchn = subcchn;
    }

    public int getSupdchn() {
        return supdchn;
    }

    public void setSupdchn(int supdchn) {
        this.supdchn = supdchn;
    }

    public int getDsupechn() {
        return dsupechn;
    }

    public void setDsupechn(int dsupechn) {
        this.dsupechn = dsupechn;
    }

    public int getDsubechn() {
        return dsubechn;
    }

    public void setDsubechn(int dsubechn) {
        this.dsubechn = dsubechn;
    }

    public int getDsubcchn() {
        return dsubcchn;
    }

    public void setDsubcchn(int dsubcchn) {
        this.dsubcchn = dsubcchn;
    }

    public int getDsupdchn() {
        return dsupdchn;
    }

    public void setDsupdchn(int dsupdchn) {
        this.dsupdchn = dsupdchn;
    }

    @Override
    public String toString() {
        String value = "Metrics for ontology: " + ontName + "\n";
        value += "Ontology-level metrics:\n";
        value += "\tMetric\tValue\n";
        value += "\tSZE" + "\t" + sze + "\n";
        value += "\tSOV" + "\t" + sov + "\n";
        value += "\tENR" + "\t" + enr + "\n";
        value += "\tTIP" + "\t" + tip + "\n";
        value += "\tEOG" + "\t" + eog + "\n";
        value += "\tRCH" + "\t" + rch + "\n";
        value += "\tCYC" + "\t" + cyc + "\n";
        value += "\tGCI" + "\t" + gci + "\n";
        value += "\tHGCI" + "\t" + hgci + "\n";
        value += "\tESUB" + "\t" + esub + "\n";
        value += "\tDSUB" + "\t" + dsub + "\n";
        value += "\tCSUB" + "\t" + csub + "\n";
        value += "\tSUPECHN" + "\t" + supechn + "\n";
        value += "\tSUBECHN" + "\t" + subechn + "\n";
        value += "\tSUBCCHN" + "\t" + subcchn + "\n";
        value += "\tSUPDCHN" + "\t" + supdchn + "\n";
        value += "\tDSUPECHN" + "\t" + dsupechn + "\n";
        value += "\tDSUBECHN" + "\t" + dsubechn + "\n";
        value += "\tDSUBCCHN" + "\t" + dsubcchn + "\n";
        value += "\tDSUPDCHN" + "\t" + dsupdchn + "\n";
        value += "\tELCLSPRT" + "\t" + ELclass_prt + "\n";
        value += "\tELAXPRT" + "\t" + ELaxiom_prt + "\n";
        value += "\tHLC" + "\t" + hlc + "\n";
        value += "\tRHLC" + "\t" + rhlc + "\n";
        value += "\tIHR" + "\t" + ihr + "\n";
        value += "\tIIR" + "\t" + iir + "\n";
        value += "\tITR" + "\t" + itr + "\n";
        value += "\tIND" + "\t" + ind + "\n";

        if (null != profiles.get(ProfileReporter.OWL2Profiles.EL)) {
            value += "\tEL" + "\t" + profiles.get(ProfileReporter.OWL2Profiles.EL) + "\n";
            value += "\tQL" + "\t" + profiles.get(ProfileReporter.OWL2Profiles.QL) + "\n";
            value += "\tRL" + "\t" + profiles.get(ProfileReporter.OWL2Profiles.RL) + "\n";
            value += "\tDL" + "\t" + profiles.get(ProfileReporter.OWL2Profiles.DL) + "\n";
            value += "\tFULL" + "\t" + profiles.get(ProfileReporter.OWL2Profiles.FULL) + "\n";
        }
        if (null != expressivity) {
            value += "\tEXP" + "\t" + expressivity;
        }

        value += "\nClass-level metrics\n";
        value += "\tMetric\tMax\tTotal\n";
        value += "\t" + NOC.toString() + "\t" + clsStats.get(NOC).getMax() + "\t" + clsStats.get(NOC).getTotal() + "\n";
        value += "\t" + CID.toString() + "\t" + clsStats.get(CID).getMax() + "\t" + clsStats.get(CID).getTotal() + "\n";
        value += "\t" + COD.toString() + "\t" + clsStats.get(COD).getMax() + "\t" + clsStats.get(COD).getTotal() + "\n";
        value += "\t" + DIT.toString() + "\t" + clsStats.get(DIT).getMax() + "\t" + clsStats.get(DIT).getTotal() + "\n";
        value += "\t" + NOP.toString() + "\t" + clsStats.get(NOP).getMax() + "\t" + clsStats.get(NOP).getTotal() + "\n";

        value += "\n" + dlcMetrics.toString() + "\n";
        value += "\n" + propertyMetrics.toString() + "\n";

        value += "\t" + "CAL-TIME" + "\t" + Long.toString(calculationTime) + " (ms) \n";
        value += "\n";

        return value;
    }

    public Map<String, Object> asMap() {
        Map<String, Object> values = new LinkedHashMap<String, Object>();

        //values.put("NAME", ontName);
        values.put("SZE", sze);
        values.put("SOV", sov);
        values.put("ENR", enr);
        values.put("TIP", tip);
        values.put("EOG", eog);
        values.put("RCH", rch);
        values.put("CYC", cyc);
        values.put("GCI", gci);
        values.put("HGCI", hgci);
        values.put("ESUB", esub);
        values.put("DSUB", dsub);
        values.put("CSUB", csub);
        values.put("SUPECHN",supechn);
        values.put("SUBECHN",subechn);
        values.put("SUBCCHN",subcchn);
        values.put("SUPDCHN",supdchn);
        values.put("DSUPECHN",dsupechn);
        values.put("DSUBECHN",dsubechn);
        values.put("DSUBCCHN",dsubcchn);
        values.put("DSUPDCHN",dsupdchn);
        values.put("ELCLSPRT",ELclass_prt);
        values.put("ELAXPRT",ELaxiom_prt);
        values.put("HLC",hlc);
        values.put("RHLC",rhlc);
        values.put("IHR",ihr);
        values.put("IIR",iir);
        values.put("ITR",itr);
        values.put("IND", ind);
        //values.put("EL", profiles.get(ProfileReporter.OWL2Profiles.EL));
        //values.put("QL", profiles.get(ProfileReporter.OWL2Profiles.QL));
        //values.put("RL", profiles.get(ProfileReporter.OWL2Profiles.RL));
        //values.put("DL", profiles.get(ProfileReporter.OWL2Profiles.DL));
        //values.put("FULL", profiles.get(ProfileReporter.OWL2Profiles.FULL));
        //values.put("EXP", expressivity);
        values.put("NOC-MAX", clsStats.get(NOC).getMax());
        values.put("NOC-TTL", clsStats.get(NOC).getTotal());
        values.put("CID-MAX", clsStats.get(CID).getMax());
        values.put("CID-TTL", clsStats.get(CID).getTotal());
        values.put("COD-MAX", clsStats.get(COD).getMax());
        values.put("COD-TTL", clsStats.get(COD).getTotal());
        values.put("DIT-MAX", clsStats.get(DIT).getMax());
        values.put("DIT-TTL", clsStats.get(DIT).getTotal());
        values.put("NOP-MAX", clsStats.get(NOP).getMax());
        values.put("NOP-TTL", clsStats.get(NOP).getTotal());

        values.putAll(dlcMetrics.asMap());
        values.putAll(propertyMetrics.asMap());

        values.put("CAL-TIME", Long.toString(calculationTime));
        return values;
    }
}
