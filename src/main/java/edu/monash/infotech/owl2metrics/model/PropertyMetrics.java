package edu.monash.infotech.owl2metrics.model;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.Map;

public class PropertyMetrics {

    private int sumPrtCount;

    private int data_prt_cnt;
    private int obj_prt_cnt;

    private int func_prt_cnt;
    private int sym_prt_cnt;
    private int trans_prt_cnt;
    private int inv_func_prt_cnt;

    private int asym_prt_cnt;
    private int refle_prt_cnt;
    private int irrefle_prt_cnt;

    private double data_prt;
    private double obj_prt;

    private double func_prt;
    private double sym_prt;
    private double ann_prt;
    private double trans_prt;
    private double inv_func_prt;

    //private double equiv_prt;
    //private double invOf;
    //private double disjoint;

    private double asym_prt;
    private double refle_prt;
    private double irrefle_prt;


    private int sub_prt_cnt;
    private int equiv_prt_cnt;
    private int invOf_cnt;
    private int disjoint_cnt;
    private int domain_cnt;
    private int range_cnt;
    private double el_prop_prt;
    private int chain_prt_cnt;
    private double chain_prt;

    public int getSumPrtCount() {
        return sumPrtCount;
    }


    public void setSumPrtCount(int sumPrtCount) {
        this.sumPrtCount = sumPrtCount;
    }


    public int getData_prt_cnt() {
        return data_prt_cnt;
    }


    public void setData_prt_cnt(int data_prt_cnt) {
        this.data_prt_cnt = data_prt_cnt;
    }


    public int getObj_prt_cnt() {
        return obj_prt_cnt;
    }


    public void setObj_prt_cnt(int obj_prt_cnt) {
        this.obj_prt_cnt = obj_prt_cnt;
    }


    public int getFunc_prt_cnt() {
        return func_prt_cnt;
    }


    public void setFunc_prt_cnt(int func_prt_cnt) {
        this.func_prt_cnt = func_prt_cnt;
    }


    public int getSym_prt_cnt() {
        return sym_prt_cnt;
    }


    public void setSym_prt_cnt(int sym_prt_cnt) {
        this.sym_prt_cnt = sym_prt_cnt;
    }


    public int getTrans_prt_cnt() {
        return trans_prt_cnt;
    }


    public void setTrans_prt_cnt(int trans_prt_cnt) {
        this.trans_prt_cnt = trans_prt_cnt;
    }


    public int getInv_func_prt_cnt() {
        return inv_func_prt_cnt;
    }


    public void setInv_func_prt_cnt(int inv_func_prt_cnt) {
        this.inv_func_prt_cnt = inv_func_prt_cnt;
    }


    public int getEquiv_prt_cnt() {
        return equiv_prt_cnt;
    }


    public void setEquiv_prt_cnt(int equiv_prt_cnt) {
        this.equiv_prt_cnt = equiv_prt_cnt;
    }


    public int getInvOf_cnt() {
        return invOf_cnt;
    }


    public void setInvOf_cnt(int invOf_cnt) {
        this.invOf_cnt = invOf_cnt;
    }


    public double getData_prt() {
        return data_prt;
    }


    public void setData_prt(double data_prt) {
        this.data_prt = data_prt;
    }


    public double getObj_prt() {
        return obj_prt;
    }


    public void setObj_prt(double obj_prt) {
        this.obj_prt = obj_prt;
    }


    public double getFunc_prt() {
        return func_prt;
    }


    public void setFunc_prt(double func_prt) {
        this.func_prt = func_prt;
    }


    public double getSym_prt() {
        return sym_prt;
    }


    public void setSym_prt(double sym_prt) {
        this.sym_prt = sym_prt;
    }


    public double getAnn_prt() {
        return ann_prt;
    }


    public void setAnn_prt(double ann_prt) {
        this.ann_prt = ann_prt;
    }


    public double getTrans_prt() {
        return trans_prt;
    }


    public void setTrans_prt(double trans_prt) {
        this.trans_prt = trans_prt;
    }


    public double getInv_func_prt() {
        return inv_func_prt;
    }


    public void setInv_func_prt(double inv_func_prt) {
        this.inv_func_prt = inv_func_prt;
    }


    @Override
    public String toString() {
        Map<String, Object> map = asMap();
        int turn = 0;

        String value = "DL-Property metrics:\n";
        for (String key : map.keySet()) {
            value += "\t" + key + "\t" + map.get(key).toString();
            if (turn++ % 2 != 0) {
                value += "\n";
            }
        }

        return value;
    }

    public Map<String, Object> asMap() {
        DecimalFormat df = new DecimalFormat("0.0000");

        Map<String, Object> values = new LinkedHashMap<String, Object>();

        values.put("OBJE-C", obj_prt_cnt);
        values.put("OBJE-%", df.format(obj_prt));
        values.put("DATA-C", data_prt_cnt);
        values.put("DATA-%", df.format(data_prt));
        values.put("FUNC-C", func_prt_cnt);
        values.put("FUNC-%", df.format(func_prt));
        values.put("SYMM-C", sym_prt_cnt);
        values.put("SYMM-%", df.format(sym_prt));
        values.put("TRAN-C", trans_prt_cnt);
        values.put("TRAN-%", df.format(trans_prt));
        values.put("INVF-C", inv_func_prt_cnt);
        values.put("INVF-%", df.format(inv_func_prt));
        values.put("ASYM-C", asym_prt_cnt);
        values.put("ASYM-%", df.format(asym_prt));
        values.put("REF-C", refle_prt_cnt);
        values.put("REF-%", df.format(refle_prt));
        values.put("IRREF-C", irrefle_prt_cnt);
        values.put("IRREF-%", df.format(irrefle_prt));

        values.put("SUBP-C", sub_prt_cnt);
        values.put("EQUI-C", equiv_prt_cnt);
        values.put("DISJ-C", disjoint_cnt);
        values.put("INV-C", invOf_cnt);
        values.put("DOMN-C", domain_cnt);
        values.put("RANG-C", range_cnt);

        values.put("CHN-C", chain_prt_cnt);
        values.put("CHN-%", chain_prt);
        values.put("ELPROP-%", el_prop_prt);

        return values;
    }

    public int getAsym_prt_cnt() {
        return asym_prt_cnt;
    }

    public void setAsym_prt_cnt(int asym_prt_cnt) {
        this.asym_prt_cnt = asym_prt_cnt;
    }

    public int getRefle_prt_cnt() {
        return refle_prt_cnt;
    }

    public void setRefle_prt_cnt(int refle_prt_cnt) {
        this.refle_prt_cnt = refle_prt_cnt;
    }

    public int getIrrefle_prt_cnt() {
        return irrefle_prt_cnt;
    }

    public void setIrrefle_prt_cnt(int irrefle_prt_cnt) {
        this.irrefle_prt_cnt = irrefle_prt_cnt;
    }

    public double getAsym_prt() {
        return asym_prt;
    }

    public void setAsym_prt(double asym_prt) {
        this.asym_prt = asym_prt;
    }

    public double getRefle_prt() {
        return refle_prt;
    }

    public void setRefle_prt(double refle_prt) {
        this.refle_prt = refle_prt;
    }

    public double getIrrefle_prt() {
        return irrefle_prt;
    }

    public void setIrrefle_prt(double irrefle_prt) {
        this.irrefle_prt = irrefle_prt;
    }

    public int getDisjoint_cnt() {
        return disjoint_cnt;
    }

    public void setDisjoint_cnt(int disjoint_cnt) {
        this.disjoint_cnt = disjoint_cnt;
    }

    public int getSub_prt_cnt() {
        return sub_prt_cnt;
    }

    public void setSub_prt_cnt(int sub_prt_cnt) {
        this.sub_prt_cnt = sub_prt_cnt;
    }

    public int getDomain_cnt() {
        return domain_cnt;
    }

    public void setDomain_cnt(int domain_cnt) {
        this.domain_cnt = domain_cnt;
    }

    public int getRange_cnt() {
        return range_cnt;
    }

    public void setRange_cnt(int range_cnt) {
        this.range_cnt = range_cnt;
    }

    public void setEl_prop_prt(double el_prop_prt) {
        this.el_prop_prt = el_prop_prt;
    }

    public double getEl_prop_prt() {
        return el_prop_prt;
    }

    public int getChain_prt_cnt() {
        return chain_prt_cnt;
    }

    public void setChain_prt_cnt(int chain_prt_cnt) {
        this.chain_prt_cnt = chain_prt_cnt;
    }

    public void setChain_prt(double chain_prt) {
        this.chain_prt = chain_prt;
    }

    public double calculateTotal_prt_cnt() {
        return func_prt_cnt + sym_prt_cnt + trans_prt_cnt + inv_func_prt_cnt + asym_prt_cnt + refle_prt_cnt +
                irrefle_prt_cnt + sub_prt_cnt + equiv_prt_cnt + equiv_prt_cnt + invOf_cnt + disjoint_cnt +
                domain_cnt + range_cnt + chain_prt_cnt;

    }
}
