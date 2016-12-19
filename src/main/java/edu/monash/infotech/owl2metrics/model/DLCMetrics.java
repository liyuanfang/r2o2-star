package edu.monash.infotech.owl2metrics.model;

import com.google.common.collect.Maps;
import org.semanticweb.owlapi.model.ClassExpressionType;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.semanticweb.owlapi.model.ClassExpressionType.*;

public class DLCMetrics {
    private Map<ClassExpressionType, Integer> map;

    private int clsCount;

    private int enumCount;
    private int negCount;
    private int conjCount;
    private int disjCount;

    private int ufCount;
    private int efCount;
    private int maxCardCount;
    private int minCardCount;
    private int cardCount;

    private int clsDescTotal;
    private double prtDescTotal;

    private double cls;

    private double enumC;
    private double neg;
    private double conj;
    private double disj;

    private double uf;
    private double ef;
    private double maxCard;
    private double minCard;
    private double card;

    public DLCMetrics() {
        this.map = Maps.newHashMap();
        for (ClassExpressionType t : values()) {
            map.put(t, 0);
        }
    }

    public void setMap(Map<ClassExpressionType, Integer> map) {
        this.map = map;
    }

    public Map<ClassExpressionType, Integer> getMap() {
        return map;
    }

    public int getClsCount() {
        return clsCount;
    }
    public void setClsCount(int clsCount) {
        this.clsCount = clsCount;
    }
    public int getEnumCount() {
        return enumCount;
    }
    public void setEnumCount(int enumCount) {
        this.enumCount = enumCount;
    }
    public int getNegCount() {
        return negCount;
    }
    public void setNegCount(int negCount) {
        this.negCount = negCount;
    }
    public int getConjCount() {
        return conjCount;
    }
    public void setConjCount(int conjCount) {
        this.conjCount = conjCount;
    }
    public int getDisjCount() {
        return disjCount;
    }
    public void setDisjCount(int disjCount) {
        this.disjCount = disjCount;
    }
    public int getUfCount() {
        return ufCount;
    }
    public void setUfCount(int ufCount) {
        this.ufCount = ufCount;
    }
    public int getEfCount() {
        return efCount;
    }
    public void setEfCount(int efCount) {
        this.efCount = efCount;
    }
    public int getMaxCardCount() {
        return maxCardCount;
    }
    public void setMaxCardCount(int maxCardCount) {
        this.maxCardCount = maxCardCount;
    }
    public int getMinCardCount() {
        return minCardCount;
    }
    public void setMinCardCount(int minCardCount) {
        this.minCardCount = minCardCount;
    }
    public int getCardCount() {
        return cardCount;
    }
    public void setCardCount(int cardCount) {
        this.cardCount = cardCount;
    }
    public double getClsDescTotal() {
        return clsDescTotal;
    }
    public void setClsDescTotal(int clsDescTotal) {
        this.clsDescTotal = clsDescTotal;
    }
    public double getPrtDescTotal() {
        return prtDescTotal;
    }
    public void setPrtDescTotal(double prtDescTotal) {
        this.prtDescTotal = prtDescTotal;
    }
    public double getNeg() {
        return neg;
    }
    public void setNeg(double neg) {
        this.neg = neg;
    }
    public double getConj() {
        return conj;
    }
    public void setConj(double conj) {
        this.conj = conj;
    }
    public double getDisj() {
        return disj;
    }
    public void setDisj(double disj) {
        this.disj = disj;
    }
    public double getUf() {
        return uf;
    }
    public void setUf(double uf) {
        this.uf = uf;
    }
    public double getEf() {
        return ef;
    }
    public void setEf(double ef) {
        this.ef = ef;
    }
    public double getMaxCard() {
        return maxCard;
    }
    public void setMaxCard(double maxCard) {
        this.maxCard = maxCard;
    }
    public double getMinCard() {
        return minCard;
    }
    public void setMinCard(double minCard) {
        this.minCard = minCard;
    }
    public double getCard() {
        return card;
    }
    public void setCard(double card) {
        this.card = card;
    }

    @Override
    public String toString() {
        String value = "DL Constructor-level metrics:\n";
        Map<String, Object> stringObjectMap = asMap();
        int turn = 0;
        for (String key : stringObjectMap.keySet()) {
            value += "\t" + key + "\t" + stringObjectMap.get(key);
            if (turn++ % 2 != 0) {
                value += "\n";
            }
        }

        return value;
    }
    public double getCls() {
        return cls;
    }
    public void setCls(double cls) {
        this.cls = cls;
    }
    public double getEnum() {
        return enumC;
    }
    public void setEnumC(double enumC) {
        this.enumC = enumC;
    }

    public Map<String, Object> asMap() {
        int size = getAnonCount();
        DecimalFormat df = new DecimalFormat ("0.0000");
        Map<String, Object> values = new LinkedHashMap<String, Object>();

        int value = map.get(OBJECT_ONE_OF);
        values.put("ENUM-C", value);
        values.put("ENUM-%", formatDoubleValue(size, df, value));
        value = map.get(OBJECT_COMPLEMENT_OF);
        values.put("COMP-C", value);
        values.put("COMP-%", formatDoubleValue(size, df, value));
        value = map.get(OBJECT_INTERSECTION_OF);
        values.put("INTER-C", value);
        values.put("INTER-%", formatDoubleValue(size, df, value));
        value = map.get(OBJECT_UNION_OF);
        values.put("UNION-C", value);
        values.put("UNION-%", formatDoubleValue(size, df, value));
        value = map.get(OBJECT_ALL_VALUES_FROM) + map.get(DATA_ALL_VALUES_FROM);
        values.put("UF-C", value);
        values.put("UF-%", formatDoubleValue(size, df, value));
        value = map.get(OBJECT_SOME_VALUES_FROM) + map.get(DATA_SOME_VALUES_FROM);
        values.put("EF-C", value);
        values.put("EF-%", formatDoubleValue(size, df, value));
        value = map.get(OBJECT_HAS_VALUE) + map.get(DATA_HAS_VALUE);
        values.put("VALUE-C", value);
        values.put("VALUE-%", formatDoubleValue(size, df, value));
        value = map.get(OBJECT_HAS_SELF);
        values.put("SELF-C", value);
        values.put("SELF-%", formatDoubleValue(size, df, value));
        value = map.get(OBJECT_MIN_CARDINALITY) + map.get(DATA_MIN_CARDINALITY);
        values.put("MNCAR-C", value);
        values.put("MNCAR-%", formatDoubleValue(size, df, value));
        value = map.get(OBJECT_MAX_CARDINALITY) + map.get(DATA_MAX_CARDINALITY);
        values.put("MXCAR-C", value);
        values.put("MXCAR-%", formatDoubleValue(size, df, value));
        value = map.get(OBJECT_EXACT_CARDINALITY) + map.get(DATA_EXACT_CARDINALITY);
        values.put("CAR-C", value);
        values.put("CAR-%", formatDoubleValue(size, df, value));

        return values;
    }

    private String formatDoubleValue(int size, DecimalFormat df, double value) {
        return (value == 0) ? df.format(0) : df.format(value / size);
    }

    public int getAnonCount() {
        int size = 0;
        for (Integer i : map.values()) {
            size += i;
        }
        return size - map.get(OWL_CLASS);
    }
}