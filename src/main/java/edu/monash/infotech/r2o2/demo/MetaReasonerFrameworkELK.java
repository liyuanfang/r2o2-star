package edu.monash.infotech.r2o2.demo;

import com.csvreader.CsvReader;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import edu.monash.infotech.r2o2.evaluation.EvaluatedReasoner;
import edu.monash.infotech.r2o2.prediction.Model;
import edu.monash.infotech.r2o2.prediction.RegressionModel;
import edu.monash.infotech.r2o2.prediction.StackRegressionModel;
import edu.monash.infotech.r2o2.prediction.StackXGBoostModel;
import edu.monash.infotech.r2o2.prediction.XGBoostModel;
import fantail.algorithms.AbstractRanker;
import fantail.core.Tools;
import org.apache.commons.math3.util.Pair;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

@SuppressWarnings("Duplicates")

public class MetaReasonerFrameworkELK {

    private static final double TIMEOUT = 1800000;

    public enum reasoner_time_disc_enum {
        A, B, C, D
    };


    private void write_eval_result(String output_file, StringBuilder sb) throws Exception {
        BufferedWriter xgbEvalWriter = new BufferedWriter(new FileWriter(output_file));
        xgbEvalWriter.write(sb.toString());
        xgbEvalWriter.close();
    }


    private HashMap<String, Object> loadPredictionModels(String dir, String predictionModelExtension, int chosen_max_depth_xgb) throws Exception {

        String stack_reg_model_endwith = ".arff.stack_reg.model";
        String stack_xgb_model_endwith = ".arff.stack_xgb.model";
        String reg_model_endwith = ".arff.reg.model";
        String xgb_model_endwith = ".arff.xgb_" + chosen_max_depth_xgb + ".model";

        // load prediction models
        File files[] = new File(dir).listFiles();

        HashMap<String, Object> predictionModels = new LinkedHashMap<>();

        for (File file : files) {
            if (file.getName().startsWith("r.") && file.getName().endsWith(predictionModelExtension)) {

                String reasoner_name = file.getName().split("\\.")[1];

                if (predictionModelExtension.contains(stack_reg_model_endwith)) {
                    String modelFile = dir + "/r." + reasoner_name + stack_reg_model_endwith;
                    StackRegressionModel m = new StackRegressionModel();
                    m = m.loadModel(modelFile);
                    predictionModels.put(reasoner_name, m);

                } else if (predictionModelExtension.contains(stack_xgb_model_endwith)) {
                    String modelFile = dir + "/r." + reasoner_name + stack_xgb_model_endwith;
                    StackXGBoostModel m = new StackXGBoostModel();
                    m = m.loadModel(modelFile);
                    predictionModels.put(reasoner_name, m);

                } else if (predictionModelExtension.contains(reg_model_endwith)) {
                    String modelFile = dir + "/r." + reasoner_name + reg_model_endwith;
                    RegressionModel m = new RegressionModel();
                    m = m.loadModel(modelFile);
                    predictionModels.put(reasoner_name, m);

                } else if (predictionModelExtension.contains(xgb_model_endwith)) {
                    String modelFile = dir + "/r." + reasoner_name + xgb_model_endwith;
                    XGBoostModel m = new XGBoostModel();
                    m = m.loadModel(modelFile);
                    predictionModels.put(reasoner_name, m);
                }
            }
        }

        return predictionModels;
    }


    private ArrayList<Attribute> create_attributes_for_stack_pm() {

        ArrayList<Attribute> atts = new ArrayList<>();

        // define attributes
        atts.add(new Attribute("rf"));
        atts.add(new Attribute("xgb"));
        atts.add(new Attribute("reasoning_time"));

        return atts;
    }

    private ArrayList<Attribute> create_attributes_for_stack_final(int n_model, List<String> reasoner_names) {

        ArrayList<Attribute> atts = new ArrayList<>();

        // define attributes
        for (int i = 0; i < n_model; i++) {
            atts.add(new Attribute("m_" + i, reasoner_names));
        }
        atts.add(new Attribute("reasoner", reasoner_names));

        return atts;
    }

    private AbstractRanker load_ranker(String file_path) throws Exception {

        FileInputStream fis = new FileInputStream(new File(file_path));
        ObjectInputStream ois = new ObjectInputStream(fis);
        AbstractRanker ranker = (AbstractRanker) ois.readObject();
        ois.close();
        return ranker;
    }

    private Pair<List<String>, HashMap<String, Double>> pm_stack_predict(HashMap<String, Object> pm_models,
                                                                         Instance test,
                                                                         ArrayList<Attribute> attrs_stack_pm,
                                                                         Instances instances_stack_pm) throws Exception {

        HashMap<String, Double> preds = new HashMap<>();

        double min_pred_time = Double.MAX_VALUE;
        for (String reasoner_name : pm_models.keySet()) {
            // load each reasoner's stacking model
            StackXGBoostModel model = (StackXGBoostModel) pm_models.get(reasoner_name);
            HashMap<String, Object> bases = model.getClassifiers();

            // load base classifiers of the model
            RegressionModel rm = (RegressionModel) bases.get("rf");
            XGBoostModel xgb = (XGBoostModel) bases.get("xgb");

            double vals[] = new double[bases.size()];
            vals[0] = rm.classifyInstance(test);
            vals[1] = xgb.classifyInstance(test);

            Instance t = new DenseInstance(attrs_stack_pm.size());
            t.setDataset(instances_stack_pm);
            t.setValue(0, vals[0]);
            t.setValue(1, vals[1]);
            t.setValue(2, -1);
            double pred = model.classifyInstance(t);
            preds.put(reasoner_name, pred);

            if (pred < min_pred_time) {
                min_pred_time = pred;
            }
        }

        List<String> chosen_reasoners = new ArrayList<>();
        for (String reasoner_name : preds.keySet()) {
            if (min_pred_time == preds.get(reasoner_name)) {
                chosen_reasoners.add(reasoner_name);
            }
        }

        Pair<List<String>, HashMap<String, Double>> result = new Pair<List<String>, HashMap<String, Double>>(chosen_reasoners, preds);
        return result;
    }

    private double elk_stack_predict(HashMap<String, Object> pm_models,
                                     Instance test,
                                     ArrayList<Attribute> attrs_stack_pm,
                                     Instances instances_stack_pm) throws Exception {

        double pred = 0d;

        for (String reasoner_name : pm_models.keySet()) {
            // load each reasoner's stacking model
            StackXGBoostModel model = (StackXGBoostModel) pm_models.get(reasoner_name);
            HashMap<String, Object> bases = model.getClassifiers();

            // load base classifiers of the model
            RegressionModel rm = (RegressionModel) bases.get("rf");
            XGBoostModel xgb = (XGBoostModel) bases.get("xgb");

            double vals[] = new double[bases.size()];
            vals[0] = rm.classifyInstance(test);
            vals[1] = xgb.classifyInstance(test);

            Instance t = new DenseInstance(attrs_stack_pm.size());
            t.setDataset(instances_stack_pm);
            t.setValue(0, vals[0]);
            t.setValue(1, vals[1]);
            t.setValue(2, -1);
            pred = model.classifyInstance(t);
        }

        return pred;
    }

    public Instances loadInstances(String arff_file) throws Exception {
        ArffLoader.ArffReader arff = new ArffLoader.ArffReader(new BufferedReader(new FileReader(arff_file)));
        Instances data = arff.getData();
        data.setClassIndex(data.numAttributes() - 1);
        return data;
    }

    public void createArffFile(String fileName, Instances data) throws Exception {
        ArffSaver writer = new ArffSaver();
        writer.setInstances(data);
        writer.setFile(new File(fileName));
        writer.writeBatch();
    }

    private String final_stack_predict(Model model, Instances test_instances, HashMap<String, Double> reasoner_average_rank_map) throws Exception {

        String final_pred_label = "";

        HashMap<String, Integer> winners = new HashMap<>();

        // Step 1: Find how many reasoners are ranked the highest
        for (Instance test : test_instances) {
            double pred_index = model.classifyInstance(test);
            String pred_label = test.classAttribute().value((int) pred_index);

            if (winners.containsKey(pred_label))
                winners.put(pred_label, (winners.get(pred_label) + 1));
            else
                winners.put(pred_label, 0);
        }

        // Step 2: If there are multiple reasoners ranked the top, we find the best reasoner considering their average rank.
        if (winners.size() == 1) {
            for (String pred_label : winners.keySet()) {
                final_pred_label = pred_label;
            }
        } else {
            double min_average_rank = Double.MAX_VALUE;
            for (String winner : winners.keySet()) {
                double average_rank = reasoner_average_rank_map.get(winner);
                if (average_rank < min_average_rank) {
                    final_pred_label = winner;
                }
            }
        }

        return final_pred_label;
    }

    private String find_best_reasoner(List<String> reasoners, HashMap<String, Double> reasoner_average_rank_map) throws Exception {

        String final_pred_label = "";

        if (reasoners.size() == 1) {
            final_pred_label = reasoners.get(0);
        } else {
            double min_average_rank = Double.MAX_VALUE;
            for (String winner : reasoners) {
                double average_rank = reasoner_average_rank_map.get(winner);
                if (average_rank < min_average_rank) {
                    min_average_rank = average_rank;
                    final_pred_label = winner;
                }
            }
        }

        return final_pred_label;
    }

    private List<String> get_best_reasoners_from_ranking(List<String> reasoner_names, double ranker_ranking[]) {

        // Find the lowerst rank from ranker ranking
        double min_ranker_ranking = Double.MAX_VALUE;
        for (int k = 0; k < ranker_ranking.length; k++) {
            if (ranker_ranking[k] < min_ranker_ranking) {
                min_ranker_ranking = ranker_ranking[k];
            }
        }
        // Find the reasoners having the lowest rank
        List<String> ranker_pred_reasoners = new ArrayList<>();
        for (int k = 0; k < ranker_ranking.length; k++) {
            if (ranker_ranking[k] == min_ranker_ranking) {
                ranker_pred_reasoners.add(reasoner_names.get(k));
            }
        }

        return ranker_pred_reasoners;
    }

    private Instances create_final_stack_instances(List<String> predicted_reasoners_1,
                                                   List<String> predicted_reasoners_2,
                                                   List<String> predicted_reasoners_3,
                                                   Instances target_instances,
                                                   List<String> true_reasoners) {

        Instances instances = new Instances(target_instances, 0);

        for (int k = 0; k < predicted_reasoners_1.size(); k++) {
            for (int l = 0; l < predicted_reasoners_2.size(); l++) {
                for (int m = 0; m < predicted_reasoners_3.size(); m++) {
                    for (String r : true_reasoners) {

                        double vals[] = new double[instances.numAttributes()];
                        for (int i = 0; i < vals.length; i++) {
                            Attribute att = target_instances.attribute(i);
                            if (i == 0)
                                vals[i] = att.indexOfValue(predicted_reasoners_1.get(k)); // stacking prediction model
                            else if (i == 1)
                                vals[i] = att.indexOfValue(predicted_reasoners_2.get(l)); // ranker
                            else if (i == 2)
                                vals[i] = att.indexOfValue(predicted_reasoners_3.get(m)); // rank prediction model
                            else if (i == 3)
                                vals[i] = att.indexOfValue(r); // true reasoner
                        }
                        instances.add(new DenseInstance(1, vals));
                    }
                }
            }
        }

        return instances;
    }

    private Instance create_rank_pm_instance(Instances template, Instance rank_instance) {

        Instances instances = new Instances(template, 0);

        double vals[] = new double[instances.numAttributes()];
        for (int i = 0; i < instances.numAttributes() - 2; i++) {
            vals[i] = rank_instance.value(i);
        }
        vals[vals.length - 1] = template.firstInstance().classValue(); // default value;
        instances.add(new DenseInstance(1, vals));

        return instances.firstInstance();
    }


    private Model load_pm_model(String model_name, String model_path) throws Exception {

        Model model = null;
        if (model_name.contains("reg")) {
            model = new RegressionModel();
            model = (RegressionModel) model.loadModel(model_path);
        } else if (model_name.contains("xgb")) {
            model = new XGBoostModel();
            model = (XGBoostModel) model.loadModel(model_path);
        }

        return model;
    }

    private HashMap<String, Double> read_reasoner_average_rank(String average_rank_file) throws Exception {

        HashMap<String, Double> performance = new LinkedHashMap();

        File file = new File(average_rank_file);
        if (file.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(average_rank_file));
            String line = "";
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith("#")) continue;
                if (line.length() == 0) continue;

                String em[] = line.split(",");
                performance.put(em[0], Double.parseDouble(em[1]));
            }
            reader.close();
        } else {
            throw new Exception("There is no rank performance file for reasoners!");
        }

        return performance;
    }

    public <T> String get_metrics_str (Instance instance) throws Exception {
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < instance.numAttributes()-1; i++) {
            key.append(instance.value(i));
        }
        return Hashing.md5().hashString(key.toString().trim().toLowerCase(), Charsets.UTF_8).toString();
    }

    /**
     * Evaluate R2O2* using ELK
     */
    public void evaluate_mr_with_elk(String target_models[], String reasoningTimeDataDir,
                                     int nFold, String result_file_name,
                                     String elk_info_file) throws Exception {


        HashMap<String, OntologyELStat> el_map = new LinkedHashMap<>();

        // Read reasoning data of the ELK reasoner
        CsvReader el_reader = new CsvReader(new File(elk_info_file).getAbsolutePath());
        while (el_reader.readRecord()) {
            String data[] = el_reader.getValues();
            OntologyELStat el_stat = new OntologyELStat();
            el_stat.set_name(data[0].trim().toLowerCase());
            el_stat.setEL(Boolean.parseBoolean(data[1]));
            el_stat.set_reasoning_time(Double.parseDouble(data[2]));
            //el_stat.set_metrics_str(data[3]);

            String key = Hashing.md5().hashString(data[3].trim().toLowerCase(), Charsets.UTF_8).toString();
            el_map.put(key, el_stat);
        }
        el_reader.close();

        // 10-fold cross-validation
        for (int fold = 0; fold < nFold; fold++) {

            System.out.print("\tfold:" + fold + ",");

            // load datasets required
            String test_dir = reasoningTimeDataDir + "/" + fold + "/test_mm/";
            String train_dir = reasoningTimeDataDir + "/" + fold + "/train_mm/";
            String pm_dir = reasoningTimeDataDir + "/" + fold + "/pm/";
            String elk_dir = reasoningTimeDataDir + "/" + fold+ "/elk/";

            // load average ranks of reasoners found in the training data
            String avg_rank_performance_file = train_dir + "/ranker.summary.avgRank";
            HashMap<String, Double> reasoner_average_rank_map = read_reasoner_average_rank(avg_rank_performance_file);

            // load test instances: (1) rank, (2) time ==> for comparison purposes
            Instances test_rank_instances = Tools.loadFantailARFFInstances(test_dir + "/meta-rank-actual.arff");
            Instances test_time_instances = Tools.loadFantailARFFInstances(test_dir + "/meta-time-actual.arff");

            Instances test_pm_instances = loadInstances(test_dir + "/input-pred.arff");
            Instances test_rank_pm_instances = loadInstances(test_dir + "/pm-rank.arff");

            // read reasoner names
            List<String> reasoner_names = Arrays.asList(Tools.getTargetNames(test_rank_instances.firstInstance()));

            // create attributes for stacking models
            ArrayList<Attribute> atts_stack_pm = create_attributes_for_stack_pm();

            // set default structure of the instances for stacking prediction model
            String rel = "instances_for_stacking_prediction_model";
            Instances instances_stack_pm = new Instances(rel, atts_stack_pm, 0);
            instances_stack_pm.setClassIndex(instances_stack_pm.numAttributes() - 1);

            // load stacking prediction models
            HashMap<String, Object> pm_stack_models = new HashMap<>();
            pm_stack_models = loadPredictionModels(pm_dir, target_models[0], 10);

            // load ranker
            AbstractRanker ranker = load_ranker(train_dir + target_models[1]);

            // load rank prediction model
            Model rank_pm = load_pm_model(target_models[2], train_dir + target_models[2]);

            // load final stack model
            Model final_stack_model = load_pm_model(target_models[3], train_dir + target_models[3]);

            // load elk prediction models
            HashMap<String, Object> elk_stack_models = new HashMap<>();
            elk_stack_models = loadPredictionModels(elk_dir, target_models[0], 10);

            // create final instances for our final stacking model
            ArrayList<Attribute> stack_final_atts = create_attributes_for_stack_final(target_models.length - 1, reasoner_names);
            Instances stack_final_instances = new Instances(rel, stack_final_atts, 0);
            stack_final_instances.setClassIndex(stack_final_instances.numAttributes() - 1);

            // iterate each instance and evaluate its prediction quality
            List<String> pm_stack_predictions = new ArrayList<>();
            List<String> ranker_predictions = new ArrayList<>();
            List<String> pm_rank_predictions = new ArrayList<>();
            List<String> final_stack_predictions = new ArrayList<>();

            HashMap<Integer, Double> elk_time_map = new LinkedHashMap<>();
            List<Integer> elk_ontset = new ArrayList<>();

            for (int i = 0; i < test_rank_instances.numInstances(); i++) {

                Instance test_rank_instance = test_rank_instances.instance(i);
                Instance test_pm_instance = test_pm_instances.instance(i);

                // check the given ontology is eligible to input to elk or not.
                String metrics_str = get_metrics_str(test_pm_instance).toLowerCase();
                OntologyELStat el_stat = el_map.get(metrics_str);

                // perform stacking prediction model
                Pair<List<String>, HashMap<String, Double>> pm_stack_prediction_pair =
                        pm_stack_predict(pm_stack_models, test_pm_instance, atts_stack_pm, instances_stack_pm);
                List<String> pm_stack_prediction_set = pm_stack_prediction_pair.getKey();
                HashMap<String, Double> pm_stack_prediction_score_set = pm_stack_prediction_pair.getValue();

                String pm_stack_prediction_winner = find_best_reasoner(pm_stack_prediction_set, reasoner_average_rank_map);

                // perform ranker
                double[] ranker_ranking = ranker.recommendRanking(test_rank_instance);
                List<String> ranker_prediction_set = get_best_reasoners_from_ranking(reasoner_names, ranker_ranking);
                String ranker_prediction_winner = find_best_reasoner(ranker_prediction_set, reasoner_average_rank_map);

                // perform rank prediction model
                Instance target_rp_instance = create_rank_pm_instance(test_rank_pm_instances, test_rank_instance);
                double class_index = rank_pm.classifyInstance(target_rp_instance);
                String rm_prediction_winner = target_rp_instance.classAttribute().value((int) class_index);
                List<String> pm_rank_prediction_set = new ArrayList<>();
                pm_rank_prediction_set.add(rm_prediction_winner);

                // -------- perform the stacking model integrating the above 3 models
                // Find true reasoners (mostly the # of the true reasoners = 1, but sometimes more than 1)
                double[] true_ranking = Tools.getTargetVector(test_rank_instance);
                List<String> true_reasoners = get_best_reasoners_from_ranking(reasoner_names, true_ranking);

                // create final stack instances
                Instances meta_instances = create_final_stack_instances(pm_stack_prediction_set, ranker_prediction_set, pm_rank_prediction_set, stack_final_instances, true_reasoners);
                String final_stack_winner = final_stack_predict(final_stack_model, meta_instances, reasoner_average_rank_map);

                // get the predicted reasoning time of ELK for this given ontology

                if (el_stat == null || !el_stat.isEL()){
                    pm_stack_predictions.add(pm_stack_prediction_winner);
                    ranker_predictions.add(ranker_prediction_winner);
                    pm_rank_predictions.add(rm_prediction_winner);
                    final_stack_predictions.add(final_stack_winner);
                } else {

                    // add the current index of the test ontology into elk_ontset
                    elk_ontset.add(i);

                    double elk_stack_prediction_score = elk_stack_predict(elk_stack_models, test_pm_instance, atts_stack_pm, instances_stack_pm);

                    if (elk_stack_prediction_score > pm_stack_prediction_score_set.get(pm_stack_prediction_winner)) {
                        pm_stack_predictions.add(pm_stack_prediction_winner);
                    } else {
                        pm_stack_predictions.add("elk");
                    }
                    if (elk_stack_prediction_score > pm_stack_prediction_score_set.get(ranker_prediction_winner)) {
                        ranker_predictions.add(ranker_prediction_winner);
                    } else {
                        ranker_predictions.add("elk");
                    }
                    if (elk_stack_prediction_score > pm_stack_prediction_score_set.get(rm_prediction_winner)) {
                        pm_rank_predictions.add(rm_prediction_winner);
                    } else {
                        pm_rank_predictions.add("elk");
                    }
                    if (elk_stack_prediction_score > pm_stack_prediction_score_set.get(final_stack_winner)) {
                        final_stack_predictions.add(final_stack_winner);
                    } else {
                        final_stack_predictions.add("elk");
                    }
                    elk_time_map.put(i, el_stat.get_reasoning_time());
                }
            }
            System.out.println("\t# of elk onts:" + elk_ontset.size());

            // read actual reasoning time data for the testing data for evaluation
            HashMap<String, EvaluatedReasoner> e_models = evaluate_models_predictions(
                    test_rank_instances, test_time_instances,
                    pm_stack_predictions, ranker_predictions,
                    pm_rank_predictions, final_stack_predictions,
                    elk_time_map, elk_ontset, test_dir, fold);
            write_evaluated_result(e_models, test_dir + result_file_name);
        }
    }

    private void write_evaluated_result(HashMap<String, EvaluatedReasoner> e_models, String result_file_name) throws Exception {

        StringBuilder sb = new StringBuilder();
        sb.append("model,p,p_A,p_B,p_C,p_D,t,t_A,t_B,t_C,t_D,td,td_A,td_B,td_C,td_D,instance_num\n");

        for (String r_name : e_models.keySet()) {
            EvaluatedReasoner m = e_models.get(r_name);

            // write precisions
            sb.append(r_name + "," + String.format("%.2f", ((double) m.sum_precisions)) + ",");
            for (int i = 0; i < m.n_bins; i++) {
                sb.append(String.format("%.2f", (double) m.precisions_per_bins[i]) + ",");
            }

            // write reasoning times
            sb.append(String.format("%.2f", (m.sum_reasoning_times)) + ",");
            for (int i = 0; i < m.n_bins; i++) {
                sb.append(String.format("%.2f", m.reasoning_times_per_bins[i]) + ",");
            }

            // write reasoning time differences between each reasoner and virtual best reasoner
            sb.append(String.format("%.2f", (m.sum_additive_times)) + ",");
            for (int i = 0; i < m.n_bins; i++) {
                sb.append(String.format("%.2f", m.additive_times_per_bins[i]) + ",");
            }
            sb.append(m.n_instances + "\n");
        }

        write_eval_result(result_file_name, sb);
    }

    private List<Integer> find_best_reasoners_indexes(double actual_ranking[]) {

        List<Integer> actual_winners = new ArrayList<>();
        for (int i = 0; i < actual_ranking.length; i++) {
            if (actual_ranking[i] == 1) {
                actual_winners.add(i);
            }
        }
        return actual_winners;
    }

    private int evaluate_ranking(List<Integer> actual_winners, int reasoner_index) {
        for (Integer winner_index : actual_winners) {
            if (reasoner_index == winner_index)
                return 1;
        }
        return 0;
    }

    private String eval_model(EvaluatedReasoner m, boolean isEL, int t_index, List<Integer> actual_winners, double actual_times[],
                              HashMap<Integer, Double> elk_time_map, HashMap<String, Integer> reasoner_name_to_index_map) {



        // compare the actual reasoning time with elk's reasoning time.
        // note that the actual reasoning time has been decided excluding elk
        double winner_time = actual_times[actual_winners.get(0)];

        double elk_time = 0;
        boolean is_elk_winner = false;
        if (isEL) {
            elk_time = elk_time_map.get(t_index);
            if (elk_time < actual_times[actual_winners.get(0)]) {
                winner_time = elk_time;
                is_elk_winner = true;
            }
        }

        if (m.model_name.equalsIgnoreCase("m5")) {
            // virtual best reasoner
            if (is_elk_winner) {
                m.winners[t_index] = "elk";
                m.winners_indexes[t_index] = reasoner_name_to_index_map.get("elk");
            }
        }

        List<Integer> winners = null;
        if (is_elk_winner) {
            winners = Arrays.asList(reasoner_name_to_index_map.get("elk"));
        } else {
            winners = actual_winners;
        }

        reasoner_time_disc_enum time_label = discretize_time(winner_time);

        if (m.winners[t_index].equalsIgnoreCase("ELK")) {
            m.reasoning_times[t_index] = Math.expm1(elk_time);
            m.precisions[t_index] = evaluate_ranking(winners, m.winners_indexes[t_index]);
            m.sum_precisions += m.precisions[t_index];
            m.precisions_per_bins[time_label.ordinal()] += m.precisions[t_index];

            m.sum_reasoning_times += m.reasoning_times[t_index];
            m.reasoning_times_per_bins[time_label.ordinal()] += m.reasoning_times[t_index];

            m.additive_times[t_index] = Math.expm1(elk_time) - Math.expm1(winner_time);
            m.sum_additive_times += m.additive_times[t_index];
            m.additive_times_per_bins[time_label.ordinal()] += m.additive_times[t_index];

        } else {
            m.precisions[t_index] = evaluate_ranking(winners, m.winners_indexes[t_index]);
            m.sum_precisions += m.precisions[t_index];
            m.precisions_per_bins[time_label.ordinal()] += m.precisions[t_index];

            m.reasoning_times[t_index] = Math.expm1(actual_times[m.winners_indexes[t_index]]);
            m.sum_reasoning_times += m.reasoning_times[t_index];
            m.reasoning_times_per_bins[time_label.ordinal()] += m.reasoning_times[t_index];

            m.additive_times[t_index] = Math.expm1(actual_times[m.winners_indexes[t_index]]) - Math.expm1(winner_time);
            m.sum_additive_times += m.additive_times[t_index];
            m.additive_times_per_bins[time_label.ordinal()] += m.additive_times[t_index];
        }
        return m.winners[t_index];

    }

    private reasoner_time_disc_enum discretize_time(double log_time) {
        double actual_time = Math.expm1(log_time); // ms

        if (actual_time <= 1000) {
            return reasoner_time_disc_enum.A;
        } else if ((actual_time > 1000) && (actual_time <= 10000)) {
            return reasoner_time_disc_enum.B;
        } else if ((actual_time > 10000) && (actual_time <= 100000)) {
            return reasoner_time_disc_enum.C;
        } else {
            return reasoner_time_disc_enum.D;
        }
    }

    private HashMap<String, EvaluatedReasoner> evaluate_models_predictions(Instances test_rank_instances,
                                                                           Instances test_time_instances,
                                                                           List<String> pm_stack_predictions,
                                                                           List<String> ranker_predictions,
                                                                           List<String> pm_rank_predictions,
                                                                           List<String> final_stack_predictions,
                                                                           HashMap<Integer, Double> elk_time_map,
                                                                           List<Integer> elk_ontset,
                                                                           String test_dir,
                                                                           int fold) throws Exception {

        int n_bins = 4;
        int n_instances = test_rank_instances.numInstances();

        // read reasoners' names
        List<String> reasoner_names = new ArrayList(Arrays.asList(Tools.getTargetNames(test_rank_instances.firstInstance())));
        reasoner_names.add("elk");

        // create evaluated reasoners
        HashMap<String, EvaluatedReasoner> e_models = new LinkedHashMap<>(); // # of reasoners + our 4 models + virtual best reasoner
        HashMap<Integer, String> reasoner_index_to_name_map = new LinkedHashMap<>();
        HashMap<String, Integer> reasoner_name_to_index_map = new LinkedHashMap<>();

        List<String> model_names = Arrays.asList("m1", "m2", "m3", "m4", "m5");

        for (int i = 0; i < reasoner_names.size(); i++) {
            EvaluatedReasoner m = new EvaluatedReasoner(n_instances, n_bins);
            m.model_name = reasoner_names.get(i);
            m.model_index = i;
            e_models.put(m.model_name, m);
            reasoner_index_to_name_map.put(m.model_index, m.model_name);
            reasoner_name_to_index_map.put(m.model_name, m.model_index);

        }
        for (int i = 0; i < model_names.size(); i++) {
            EvaluatedReasoner m = new EvaluatedReasoner(n_instances, n_bins);
            m.model_name = model_names.get(i);
            m.model_index = i + reasoner_names.size();
            e_models.put(m.model_name, m);
            reasoner_index_to_name_map.put(m.model_index, m.model_name);
            reasoner_name_to_index_map.put(m.model_name, m.model_index);
        }

        StringBuilder sb = new StringBuilder();

        // evaluate prediction results of various approaches
        for (int i = 0; i < n_instances; i++) {

            Instance r_rank = test_rank_instances.instance(i);
            Instance t_rank = test_time_instances.instance(i);

            // read actual ranking and times of the reasoners
            double actual_ranking[] = Tools.getTargetVector(r_rank);
            double actual_times[] = Tools.getTargetVector(t_rank);

            List<Integer> actual_winners = find_best_reasoners_indexes(actual_ranking);

            // compare the true and predicted rankings

            // *** evaluate individual reasoners
            for (String model_name : e_models.keySet()) {

                EvaluatedReasoner m = e_models.get(model_name);
                if (reasoner_names.contains(model_name)) {
                    m.winners[i] = model_name;
                } else if (model_name.equalsIgnoreCase("m1")) {
                    m.winners[i] = pm_stack_predictions.get(i);
                } else if (model_name.equalsIgnoreCase("m2")) {
                    m.winners[i] = ranker_predictions.get(i);
                } else if (model_name.equalsIgnoreCase("m3")) {
                    m.winners[i] = pm_rank_predictions.get(i);
                } else if (model_name.equalsIgnoreCase("m4")) {
                    m.winners[i] = final_stack_predictions.get(i);
                } else if (model_name.equalsIgnoreCase("m5")) {
                    m.winners[i] = reasoner_index_to_name_map.get(actual_winners.get(0));
                } else {
                    throw new Exception("Model name: '" + model_name + "' doesn't exist!");
                }

                m.winners_indexes[i] = reasoner_name_to_index_map.get(m.winners[i]);

                String winner = eval_model(m, elk_ontset.contains(i), i, actual_winners, actual_times, elk_time_map, reasoner_name_to_index_map);
                if (model_name.equalsIgnoreCase("m4")) {
                    sb.append(i + "," + winner + "\n");
                }
                e_models.put(model_name, m);
            }
        }

        write_eval_result(test_dir + "/prediction_elk_" + fold + ".csv", sb);
        return e_models;
    }

    private double handle_err_ont (double rt, int how_to_handle_err_onts) {

        double new_rt = -1;

        // handling error ontologies
        if (rt == -1) {
            if (how_to_handle_err_onts == 0) {
                new_rt = rt;
            } else if (how_to_handle_err_onts == 1) {
                new_rt = TIMEOUT; // timeout
            } else if (how_to_handle_err_onts == 2) {
                new_rt = TIMEOUT*2; // penalty
            }
        } else {
            new_rt = rt;
        }
        return new_rt;
    }

    /**
     * Read which ontologies can be reasoned by ELK.
     */
    public HashMap<String, OntologyELStat> get_EL_stats_and_reasoning_data(String elk_info_file_path, String elk_rt_file_path,
                                                                                                                   String ont_names_metrics_path, int how_to_handle_err_onts) throws Exception {

        HashMap<String, OntologyELStat> result = new LinkedHashMap<>();

        // Read ontology names and their metrics string
        HashMap<String, String> metrics_map = new LinkedHashMap<>();
        CsvReader metrics_reader = new CsvReader(new File(ont_names_metrics_path).getAbsolutePath());
        metrics_reader.readHeaders();
        while (metrics_reader.readRecord()) {
            String ontName = metrics_reader.get("name").toLowerCase();
            String metrics =  metrics_reader.get("metrics").toLowerCase();
            metrics_map.put(ontName, metrics);
        }
        metrics_reader.close();

        // Read reasoning data of the ELK reasoner
        HashMap<String, Double> rt_map = new LinkedHashMap<>();
        CsvReader rt_reader = new CsvReader(new File(elk_rt_file_path).getAbsolutePath());
        rt_reader.readHeaders();
        while (rt_reader.readRecord()) {
            String ontName = rt_reader.get("Ont name").toLowerCase();
            double rt = Double.parseDouble(rt_reader.get("Classification time (ms)"));
            if (rt == -1)
                rt = handle_err_ont(rt, how_to_handle_err_onts);

            Double rtDouble = Math.log1p(rt);
            rt_map.put(ontName, rtDouble);
        }
        rt_reader.close();

        // Read ontology status information about EL
        CsvReader el_reader = new CsvReader(elk_info_file_path);
        el_reader.readHeaders();
        while (el_reader.readRecord()) {
            String ontName = el_reader.get("Ontology").toLowerCase();
            boolean isEL = Boolean.parseBoolean(el_reader.get("EL").toLowerCase());

            OntologyELStat ont_el_stat = new OntologyELStat();
            ont_el_stat.set_name(ontName);
            ont_el_stat.set_reasoning_time(rt_map.get(ontName));
            ont_el_stat.setEL(isEL);
            ont_el_stat.set_metrics_str(metrics_map.get(ontName));
            result.put(ontName, ont_el_stat);
        }
        el_reader.close();


        String outFilename = elk_rt_file_path + ".stat.csv";
        BufferedWriter w = new BufferedWriter(new FileWriter(outFilename));
        for (String ontName : result.keySet()) {
            if (result.get(ontName).get_metrics_str() != null)
                w.write(result.get(ontName).toString() + "\n");
        }
        w.close();

        return result;
    }

    /**
     * Run and Evaluate the meta-reasoning framework R2O2* using ELK
     * @param args N/A
     */
    public static void main(String args[]) throws Exception {

        MetaReasonerFrameworkELK mr = new MetaReasonerFrameworkELK();

        // N-fold cross-valudation
        int nFold = 10;
        // If the dataset is ErrorsRemoved, this set to be 0, otherwise 1
        int how_to_handle_err_ont = 0;

        String reasoningTimeDataDir = MetaReasonerFrameworkELK.class.getClassLoader().getResource("data/ErrorsRemoved").getPath(); // or "./data/ErrorsReplaced

        // This file shows whether each ontology is reasoned by ELK. It has more extended information than ore2015.csv.
        // If a value of an ontology under the 'EL' column is true, then this ontology can be successfully reasoned by ELK
        String elk_info_file_path = reasoningTimeDataDir + "/ore2015_extended.csv";

        // For evaluation purposes, we keep the original reasoning time of ELK
        String elk_rt_file_path = reasoningTimeDataDir + "/ELK_reasoning_time.csv";

        // This file indicates what metrics values are given to each ontology. It will be used for evaluation.
        String ont_names_metrics_path = reasoningTimeDataDir + "/ont_names_metrics.csv";

        // This file will be created through runnig the function: get_EL_stats_and_reasoning_data() that shows which ontology can be successfully reasoned by
        // ELK and what are its metric values
        String elk_info_file = elk_rt_file_path + ".stat.csv";
        mr.get_EL_stats_and_reasoning_data(elk_info_file_path, elk_rt_file_path, ont_names_metrics_path, how_to_handle_err_ont);

        // The evaluation result file to be written
        String resultFileName = "final_eval_elk_using_elkpm_using_xgb.csv";

        // The key components in each meta-reasoner model; 1) R2O2*_pt, 2) R2O2*_rk, 3) R2O2*_mc, 4) R2O2*_all
        String target_models[] = {".arff.stack_xgb.model", "ARTForests.ranker", "pm-rank.arff.xgb_10.model", "meta-stack-final.arff.xgb.model"};
        mr.evaluate_mr_with_elk(target_models, reasoningTimeDataDir, nFold, resultFileName, elk_info_file);
    }
}
