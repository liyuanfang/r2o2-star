package edu.monash.infotech.r2o2.demo;

import edu.monash.infotech.r2o2.evaluation.EvaluatedReasoner;
import edu.monash.infotech.r2o2.prediction.Model;
import edu.monash.infotech.r2o2.prediction.RegressionModel;
import edu.monash.infotech.r2o2.prediction.StackRegressionModel;
import edu.monash.infotech.r2o2.prediction.StackXGBoostModel;
import edu.monash.infotech.r2o2.prediction.XGBoostModel;
import fantail.algorithms.AbstractRanker;
import fantail.core.Tools;
import org.apache.commons.math3.stat.ranking.NaturalRanking;
import org.apache.commons.math3.stat.ranking.TiesStrategy;
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
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

@SuppressWarnings("Duplicates")

public class MetaReasonerFramework {

    public enum reasoner_time_disc_enum {
        A, B, C, D
    };

    private Instances create_stack_ensemble_instances(RegressionModel rm, XGBoostModel xgb, Instances original_instances) throws Exception {

        String rel = "instances_for_stacking_prediction_model";
        ArrayList<Attribute> atts = new ArrayList<>();
        atts.add(new Attribute("rf"));
        atts.add(new Attribute("xgb"));
        atts.add(new Attribute("reasoning_time"));
        Instances newInstances = new Instances(rel, atts, 0);

        for (int i = 0; i < original_instances.numInstances(); i++) {
            Instance instance = original_instances.instance(i);
            double vals[] = new double[atts.size()];
            vals[0] = rm.classifyInstance(instance);
            vals[1] = xgb.classifyInstance(instance);
            vals[2] = instance.value(instance.classIndex());
            newInstances.add(new DenseInstance(1, vals));
        }

        newInstances.setClassIndex(newInstances.numAttributes() - 1);
        return newInstances;
    }

    /**
     * Building prediction models for all reasoners except ELK. Prediction models will be built by 4 different ways: RF, XGBoost, meta-RF, meta-XGBoost
     */
    public void buildPredictionModel(String reasoningTimeDataDir, String train_dir_name, int nFold, int chosen_max_depth, boolean needEvaluation) throws Exception {
        System.out.println("\n# Generate prediction models:");

        for (int fold = 0; fold < nFold; fold++) {
            URL predDataURL = this.getClass().getClassLoader().getResource(reasoningTimeDataDir + "/" + fold + "/" + train_dir_name);
            File predDataDir = new File(predDataURL.getPath());
//            File predDataDir = new File(reasoningTimeDataDir + "/" + fold + "/" + train_dir_name);
            File files[] = predDataDir.listFiles();
            System.out.println("\tfold=" + fold);

            StringBuilder rmEval = new StringBuilder();
            StringBuilder xgbEvals = new StringBuilder();

            // evaluation writers (for Stack Random Forest & Stack XGBoost)
            StringBuilder stackRegEval = new StringBuilder();
            StringBuilder stackXgbEval = new StringBuilder();

            for (File file : files) {
                if (file.getName().startsWith("r.") && file.getName().endsWith("arff") &&
                        file.getName().split("\\.").length == 3) {

                    String reasonerName = file.getName().split("\\.")[1];
                    String file_path = file.getAbsolutePath();
                    Instances data = loadInstances(file_path);

                    // --- Regression
                    String rf_model_path = file.getAbsolutePath() + ".reg.model";
                    RegressionModel rm = new RegressionModel();
                    rm.buildClassifier(data);
                    rm.saveModel(rf_model_path);
                    if (needEvaluation) {
                        double regPerf = rm.evaluate(data);
                        System.out.println("\t\tRF:" + reasonerName + ", r2=" + regPerf);
                        rmEval.append(reasonerName + "," + regPerf + "\n");
                    }

                    // --- XGBoost
                    String objective = "";
                    String xgb_model_path = file_path + ".xgb_";
                    XGBoostModel xgb = new XGBoostModel();
                    xgb.buildClassifier(data, chosen_max_depth, objective);
                    xgb.saveModel(xgb_model_path + chosen_max_depth + ".model");
                    if (needEvaluation) {
                        double xgbPerf = xgb.evaluate(data, chosen_max_depth);
                        System.out.println("\t\tXGB_" + chosen_max_depth + ":" + reasonerName + ", r2=" + xgbPerf);
                        xgbEvals.append(reasonerName + "," + xgbPerf + "\n");
                    }

                    // --- create stack instances
                    String new_arff_file = file_path + ".stack.arff";
                    Instances stackInstances = create_stack_ensemble_instances(rm, xgb, data);
                    createArffFile(new_arff_file, stackInstances);

                    // --- Regression stack model
                    StackRegressionModel stack_rf = new StackRegressionModel();
                    stack_rf.addClassifier("rf", rm);
                    stack_rf.addClassifier("xgb", xgb);
                    stack_rf.buildClassifier(stackInstances);
                    stack_rf.saveData(new_arff_file, stackInstances);
                    stack_rf.saveModel(file_path + ".stack_reg.model");
                    if (needEvaluation) {
                        double stack_rf_perf = stack_rf.evaluate(data);
                        System.out.println("\t\tStack RF:" + reasonerName + ", r2=" + stack_rf_perf);
                        stackRegEval.append(reasonerName + "," + stack_rf_perf + "\n");
                    }

                    // --- XGB stack model
                    StackXGBoostModel stack_xgb = new StackXGBoostModel();
                    stack_xgb.addClassifier("rf", rm);
                    stack_xgb.addClassifier("xgb", xgb);
                    stack_xgb.buildClassifier(stackInstances, chosen_max_depth);
                    stack_xgb.saveModel(file_path + ".stack_xgb.model");
                    if (needEvaluation) {
                        double stack_xgb_perf = stack_xgb.evaluate(data, chosen_max_depth);
                        System.out.println("\t\tStack XGB:" + reasonerName + ", r2=" + stack_xgb_perf);
                        stackXgbEval.append(reasonerName + "," + stack_xgb_perf + "\n");
                    }
                }
            }

            if (needEvaluation) {
                // The performance of evaluated models is saved under the directory ~/train_mm/*. As presented in the paper,
                // the performance is measured by R^2 (coefficient of determination).
                String output_file = reasoningTimeDataDir + "/" + fold + "/" + train_dir_name + "/reg_eval.csv";
                write_eval_result(output_file, rmEval);

                output_file = reasoningTimeDataDir + "/" + fold + "/" + train_dir_name + "/xgb_eval_" + chosen_max_depth + ".csv";
                write_eval_result(output_file, xgbEvals);

                output_file = reasoningTimeDataDir + "/" + fold + "/" + train_dir_name + "/stack_reg_rf_xgb_" + chosen_max_depth + "_eval.csv";
                write_eval_result(output_file, stackRegEval);

                output_file = reasoningTimeDataDir + "/" + fold + "/" + train_dir_name + "/stack_xgb_rf_xgb_" + chosen_max_depth + "_eval.csv";
                write_eval_result(output_file, stackXgbEval);
            }
        }
    }

    public Instances loadInstances(String arff_file) throws Exception {
        File arff_f = new File(this.getClass().getClassLoader().getResource(arff_file).getPath());
        ArffLoader.ArffReader arff = new ArffLoader.ArffReader(new BufferedReader(new FileReader(arff_f)));
        Instances data = arff.getData();
        data.setClassIndex(data.numAttributes() - 1);
        return data;
    }

    public void createArffFile(String fileName, Instances data) throws Exception {
        File f = new File(this.getClass().getClassLoader().getResource(fileName).getPath());
        ArffSaver writer = new ArffSaver();
        writer.setInstances(data);
        writer.setFile(f);
        writer.writeBatch();
    }

    public void buildRankPredictionModel(String reasoningTimeDataDir, int nFold, int xgb_depth, boolean needEvaluation) throws Exception {

        System.out.println("\n# Generate rank prediction models:");

        for (int fold = 0; fold < nFold; fold++) {
            System.out.println("\tfold=" + fold);

            StringBuilder rmEval = new StringBuilder();
            StringBuilder xgbEval = new StringBuilder();

            File predDataDir = new File(this.getClass().getClassLoader().getResource(reasoningTimeDataDir + "/" + fold + "/train_mm/").getPath());
            File train_file = new File(predDataDir.getAbsolutePath() + "/pm-rank.arff");
            Instances data = loadInstances(train_file.getAbsolutePath());

            // --- Regression
            String output_file = train_file.getAbsolutePath() + ".reg.model";
            RegressionModel regressionModel = new RegressionModel();
            regressionModel.buildClassifier(data);
            regressionModel.saveModel(output_file);
            if (needEvaluation) {
                double regPerf = regressionModel.evaluateClassification(data);
                rmEval.append("RF," + regPerf + "\n");
            }

            // --- XGBoost
            String objective = "multi:softmax";
            output_file = train_file.getAbsolutePath() + ".xgb_" + xgb_depth + ".model";
            XGBoostModel xgb = new XGBoostModel();
            xgb.buildClassifier(train_file.getAbsolutePath(), xgb_depth, objective);
            xgb.saveModel(output_file);
            if (needEvaluation) {
                double xgbPerf = xgb.evaluateClassification(data, xgb_depth, objective);
                xgbEval.append("XGB_" + xgb_depth + "," + xgbPerf + "\n");
            }

            if (needEvaluation) {
                String eval_file = predDataDir.getAbsoluteFile() + "/rank_pm_reg_eval.csv";
                write_eval_result(eval_file, rmEval);
                eval_file = predDataDir.getAbsoluteFile() + "/rank_pm_xgb_eval_" + xgb_depth + ".csv";
                write_eval_result(eval_file, xgbEval);
            }
        }
    }


    public List<HashMap<String, Double>> findRankingFromStackPredModel(String dir, String model_extenstion) throws Exception {

        List<HashMap<String, Double>> sp_rankings = new ArrayList<>();

        // load stacking models
        HashMap<String, Object> stack_models = new LinkedHashMap<>();
        stack_models = loadPredictionModels(dir, model_extenstion, -1);
        HashMap<String, Instances> stack_instances = new HashMap<>();
        stack_instances = loadReasonerInstances(dir, ".stack.arff");

        // --- apply the prediction models on the given data
        String testFileName = dir + "/../train_mm/input-pred.arff";
        Instances testInstances = new Instances(new BufferedReader(new FileReader(testFileName)));
        testInstances.setClassIndex(testInstances.numAttributes() - 1);

        // predict the test instances using the final stack model
        for (int i = 0; i < testInstances.numInstances(); i++) {
            HashMap<String, Double> predictionMap = new LinkedHashMap<>();
            HashMap<String, Double> rank_map = new LinkedHashMap<>();
            List<String> reasoner_names =  new ArrayList();

            for (String reasoner : stack_models.keySet()) {
                Instance t_stack = stack_instances.get(reasoner).instance(i);
                if (model_extenstion.contains("reg")) {
                    StackRegressionModel model = (StackRegressionModel) stack_models.get(reasoner);
                    double pred = model.classifyInstance(t_stack);
                    predictionMap.put(reasoner, pred);
                } else {
                    StackXGBoostModel model = (StackXGBoostModel) stack_models.get(reasoner);
                    double pred = model.classifyInstance(t_stack);
                    predictionMap.put(reasoner, pred);
                }
                reasoner_names.add(reasoner);
            }

            // read ranking of the reasoners of this instance
            double predictions[] = summarizePrediction(predictionMap, reasoner_names);
            double predRanking[] = rank(predictions);

            for (int k = 0; k < reasoner_names.size(); k++) {
                rank_map.put(reasoner_names.get(k), predRanking[k]);
            }
            sp_rankings.add(rank_map);
        }

        return sp_rankings;
    }

    public void buildFinalStackRankingModel(String reasoningTimeDataDir, int nFold, String ensemble[], int chosen_xgb_max_depth, boolean needEvaluation) throws Exception {

        System.out.println("\n# Generate the final stack model:");

        for (int fold = 0; fold < nFold; fold++) {
            System.out.println("\tfold=" + fold);

            // ---- get the prediction results from the stack prediction model: model 1

            File time_pred_dir = new File(this.getClass().getClassLoader().getResource(reasoningTimeDataDir + "/" + fold + "/pm/").getPath());
            List<HashMap<String, Double>> sp_rankings = findRankingFromStackPredModel(time_pred_dir.getAbsolutePath(), ensemble[0]);

            // ---- load the arff file to be used by ranker and its instances: model 2
            File ranker_dir = new File(this.getClass().getClassLoader().getResource(reasoningTimeDataDir + "/" + fold + "/train_mm/").getPath());
            AbstractRanker ranker = load_ranker(ranker_dir.getAbsolutePath() + "/" + ensemble[1]);
            Instances ranker_instances = loadInstances(ranker_dir.getAbsolutePath() + "/meta-rank-actual.arff");

            // ---- load the rank prediction model and its instances: model 3
            Object stack_rank_pred_model = loadRankPredictionModel(ranker_dir.getAbsolutePath(), ensemble[2]);
            Instances rp_instances = loadInstances(ranker_dir.getAbsolutePath() + "/pm-rank.arff");
            rp_instances.setClassIndex(rp_instances.numAttributes() - 1);
            // ---------------------------------------------------


            // ---- init stacking model instances
            String rel = "instances_for_stacking_final_model";
            List<String> reasoner_names = Arrays.asList(Tools.getTargetNames(ranker_instances.instance(0)));
            ArrayList<Attribute> atts = create_attributes_for_stack_final(ensemble.length, reasoner_names);

            // create the stacking instance structure
            Instances newInstances = new Instances(rel, atts, 0);
            newInstances.setClassIndex(newInstances.numAttributes() - 1);

            for (int i = 0; i < ranker_instances.numInstances(); i++) {
                Instance instance = ranker_instances.instance(i);
                double[] true_ranking = Tools.getTargetVector(instance);

                // find the most efficient reasoner(s) recommended by the stacking prediction model
                List<String> sp_pred_reasoners = get_best_reasoners_from_ranking(sp_rankings.get(i));

                // find the most efficient reasoner(s) recommended by the ranker
                double[] ranker_ranking = ranker.recommendRanking(instance);
                List<String> ranker_pred_reasoners = get_best_reasoners_from_ranking(reasoner_names, ranker_ranking);

                // find the most efficient reasoner(s) recommended by the rank prediction model
                Instance target_rp_instance = create_rank_pm_instance(rp_instances, instance);
                double rp_pred_index = 0;
                if (ensemble[2].contains("reg")) {
                    rp_pred_index = ((RegressionModel) stack_rank_pred_model).classifyInstance(target_rp_instance);
                } else if (ensemble[2].contains("xgb")) {
                    rp_pred_index = ((XGBoostModel) stack_rank_pred_model).classifyInstance(target_rp_instance);
                }
                String rp_class_label = target_rp_instance.classAttribute().value((int) rp_pred_index);
                List<String> rp_class_reasoners = new ArrayList<>();
                rp_class_reasoners.add(rp_class_label);

                // Find true reasoners (mostly the # of the true reasoners = 1, but sometimes more than 1)
                List<String> true_reasoners = get_best_reasoners_from_ranking(reasoner_names, true_ranking);

                // create final stack instances
                Instances instances = create_final_stack_instances(rp_class_reasoners, ranker_pred_reasoners, sp_pred_reasoners, newInstances, true_reasoners);
                newInstances.addAll(instances);
            }

            newInstances.setClassIndex(newInstances.numAttributes() - 1);
            String final_arff_file = ranker_dir + "/meta-stack-final.arff";
            createArffFile(final_arff_file, newInstances);

            // --- Regression stack model
            StringBuilder rm_eval = new StringBuilder();
            RegressionModel rm = new RegressionModel();
            rm.buildClassifier(newInstances);
            rm.saveModel(final_arff_file + ".reg.model");
            if (needEvaluation) {
                double rm_perf = rm.evaluateClassification(newInstances);
                rm_eval.append("final_stack_rf," + rm_perf + "\n");
                String output_file = final_arff_file + ".reg_eval.csv";
                write_eval_result(output_file, rm_eval);
            }

            // --- XGB stack model
            StringBuilder xgb_eval = new StringBuilder();
            XGBoostModel xgb = new XGBoostModel();
            xgb.buildClassifier(newInstances, chosen_xgb_max_depth, "multi:softmax");
            xgb.saveModel(final_arff_file + ".xgb.model");
            if (needEvaluation) {
                double xgb_perf = xgb.evaluateClassification(newInstances, chosen_xgb_max_depth, "multi:softmax");
                xgb_eval.append("final_stack_xgb," + xgb_perf + "\n");
                String output_file = final_arff_file + ".xgb_eval.csv";
                write_eval_result(output_file, xgb_eval);
            }
        }
    }

    private void write_eval_result(String output_file, StringBuilder sb) throws Exception {
        BufferedWriter xgbEvalWriter = new BufferedWriter(new FileWriter(this.getClass().getClassLoader().getResource(".").getPath() + output_file));
        xgbEvalWriter.write(sb.toString());
        xgbEvalWriter.close();
    }

    public void buildRankerOnAcutal(String reasoningTimeDataDir, int nFold, boolean needEvaluation) throws Exception {
        System.out.println("\n# Generate rankers on actual reasoning data:");

        for (int fold = 0; fold < nFold; fold++) {
            System.out.println("fold:" + fold);

            // Train rankers on the ranking matrix generated using "predicted" reasoning time
            File trainDir = new File(reasoningTimeDataDir + "/" + fold + "/train_mm");

            // Train rankers on the ranking matrix generated using "actual" reasoning time
            String output_file = trainDir.getAbsolutePath() + "/ranker.summary";
            R2O2 metaReasoner = new R2O2();
            String trainFile = trainDir + "/meta-rank-actual.arff";
            metaReasoner.buildModel(trainFile, "ranker", "ranker.summary", needEvaluation);
        }
    }

    private HashMap<String, Object> loadPredictionModels(String dir, String predictionModelExtension, int chosen_max_depth_xgb) throws Exception {

        String stack_reg_model_endwith = ".arff.stack_reg.model";
        String stack_xgb_model_endwith = ".arff.stack_xgb.model";
        String reg_model_endwith = ".arff.reg.model";
        String xgb_model_endwith = ".arff.xgb_" + chosen_max_depth_xgb + ".model";

        // load prediction models
        File dir_f = new File(this.getClass().getClassLoader().getResource(dir).getPath());
        File files[] = dir_f.listFiles();

        HashMap<String, Object> predictionModels = new LinkedHashMap<>();

        for (File file : files) {
            if (file.getName().startsWith("r.") && file.getName().endsWith(predictionModelExtension)) {

                String reasoner_name = file.getName().split("\\.")[1];

                if (predictionModelExtension.contains(stack_reg_model_endwith)) {
                    String modelFile = dir_f.getAbsolutePath() + "/r." + reasoner_name + stack_reg_model_endwith;
                    StackRegressionModel m = new StackRegressionModel();
                    m = m.loadModel(modelFile);
                    predictionModels.put(reasoner_name, m);

                } else if (predictionModelExtension.contains(stack_xgb_model_endwith)) {
                    String modelFile = dir_f.getAbsolutePath() + "/r." + reasoner_name + stack_xgb_model_endwith;
                    StackXGBoostModel m = new StackXGBoostModel();
                    m = m.loadModel(modelFile);
                    predictionModels.put(reasoner_name, m);

                } else if (predictionModelExtension.contains(reg_model_endwith)) {
                    String modelFile = dir_f.getAbsolutePath() + "/r." + reasoner_name + reg_model_endwith;
                    RegressionModel m = new RegressionModel();
                    m = m.loadModel(modelFile);
                    predictionModels.put(reasoner_name, m);

                } else if (predictionModelExtension.contains(xgb_model_endwith)) {
                    String modelFile = dir_f.getAbsolutePath() + "/r." + reasoner_name + xgb_model_endwith;
                    XGBoostModel m = new XGBoostModel();
                    m = m.loadModel(modelFile);
                    predictionModels.put(reasoner_name, m);
                }
            }
        }

        return predictionModels;
    }

    private HashMap<String, Instances> loadReasonerInstances(String dir, String arff_file_extension) throws Exception {

        // load prediction models
        File files[] = new File(dir).listFiles();

        HashMap<String, Instances> arff_sets = new LinkedHashMap<>();

        for (File file : files) {
            if (file.getName().startsWith("r.") && file.getName().endsWith(arff_file_extension)) {

                String reasoner_name = file.getName().split("\\.")[1];
                arff_sets.put(reasoner_name, loadInstances(file.getAbsolutePath()));
            }
        }

        return arff_sets;
    }

    private Object loadRankPredictionModel(String dir, String model_name) throws Exception {

        Object model = null;
        String modelFile = dir + "/" + model_name;

        if (model_name.contains("reg")) {
            model = new RegressionModel();
            model = ((RegressionModel) model).loadModel(modelFile);
        } else if (model_name.contains("xgb")) {
            model = new XGBoostModel();
            model = ((XGBoostModel) model).loadModel(modelFile);
        }

        return model;
    }


    public double[] rank(double values[]) {
        return new NaturalRanking(TiesStrategy.MINIMUM).rank(values);
    }

    public double[] summarizePrediction (HashMap<String, Double> preds, List<String> reasoner_names) {
        double predictions[] = new double[reasoner_names.size()];
        for (int i = 0; i < reasoner_names.size(); i++) {
            predictions[i] = preds.get(reasoner_names.get(i));
        }
        return predictions;
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


        FileInputStream fis = new FileInputStream(new File(this.getClass().getClassLoader().getResource(file_path).getPath()));
        ObjectInputStream ois = new ObjectInputStream(fis);
        AbstractRanker ranker = (AbstractRanker) ois.readObject();
        ois.close();
        return ranker;
    }

    private List<String> pm_stack_predict(HashMap<String, Object> pm_models,
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

        return chosen_reasoners;
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

    private List<String> get_best_reasoners_from_ranking(HashMap<String, Double> ranking_map) {

        // Find the lowerest rank from ranker ranking
        double min_ranker_ranking = Double.MAX_VALUE;
        List<String> ranker_pred_reasoners = new ArrayList<>();

        List<String> reasoners = new ArrayList(ranking_map.keySet());
        for (String r: reasoners) {
            if (ranking_map.get(r) < min_ranker_ranking) {
                min_ranker_ranking = ranking_map.get(r) ;
            }
        }

        // Find the reasoners having the lowest rank
        for (String r: reasoners) {
            if (ranking_map.get(r) == min_ranker_ranking) {
                ranker_pred_reasoners.add(r);
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

        File file = new File(this.getClass().getClassLoader().getResource(average_rank_file).getPath());
        if (file.exists()) {
            BufferedReader reader = new BufferedReader(new FileReader(file));
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

    /**
     * Evaluate the meta-reasoning framework R2O2*
     */

    public void evaluateMR(String target_models[], String reasoningTimeDataDir,
                           int cvFoldTest, String result_file_name) throws Exception {

        for (int fold = 0; fold < cvFoldTest; fold++) {

            System.out.println("fold:" + fold);

            // load datasets required
            String test_dir = reasoningTimeDataDir + "/" + fold + "/test_mm/";
            String train_dir = reasoningTimeDataDir + "/" + fold + "/train_mm/";
            String pm_dir = reasoningTimeDataDir + "/" + fold + "/pm/";

            // load average ranks of reasoners found in the training data
            String avg_rank_performance_file = train_dir + "ranker.summary.avgRank";
            HashMap<String, Double> reasoner_average_rank_map = read_reasoner_average_rank(avg_rank_performance_file);

            // load different test instances
            Instances test_rank_instances = Tools.loadFantailARFFInstances(test_dir + "meta-rank-actual.arff");
            Instances test_time_instances = Tools.loadFantailARFFInstances(test_dir + "meta-time-actual.arff");

            Instances test_pm_instances = loadInstances(test_dir + "input-pred.arff");
            Instances test_rank_pm_instances = loadInstances(test_dir + "pm-rank.arff");
            test_pm_instances.setClassIndex(test_pm_instances.numAttributes() - 1);
            test_rank_pm_instances.setClassIndex(test_rank_pm_instances.numAttributes() - 1);

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
            pm_stack_models = loadPredictionModels(pm_dir, target_models[0], -1);

            // load ranker
            AbstractRanker ranker = load_ranker(train_dir + target_models[1]);

            // load rank prediction model
            Model rank_pm = load_pm_model(target_models[2], train_dir + target_models[2]);

            // load final stack model
            Model final_stack_model = load_pm_model(target_models[3], train_dir + target_models[3]);

            // create final instances for our final stacking model
            ArrayList<Attribute> stack_final_atts = create_attributes_for_stack_final(target_models.length - 1, reasoner_names);
            Instances stack_final_instances = new Instances(rel, stack_final_atts, 0);
            stack_final_instances.setClassIndex(stack_final_instances.numAttributes() - 1);

            // iterate each instance and evaluate its prediction quality
            List<String> pm_stack_predictions = new ArrayList<>();
            List<String> ranker_predictions = new ArrayList<>();
            List<String> pm_rank_predictions = new ArrayList<>();
            List<String> final_stack_predictions = new ArrayList<>();

            for (int i = 0; i < test_rank_instances.numInstances(); i++) {

                Instance test_rank_instance = test_rank_instances.instance(i);
                Instance test_pm_instance = test_pm_instances.instance(i);

                // perform stacking prediction model
                List<String> pm_stack_prediction_set = pm_stack_predict(pm_stack_models, test_pm_instance, atts_stack_pm, instances_stack_pm);
                String pm_stack_prediction_winner = find_best_reasoner(pm_stack_prediction_set, reasoner_average_rank_map);
                pm_stack_predictions.add(pm_stack_prediction_winner);

                // perform ranker
                double[] ranker_ranking = ranker.recommendRanking(test_rank_instance);
                List<String> ranker_prediction_set = get_best_reasoners_from_ranking(reasoner_names, ranker_ranking);
                String ranker_prediction_winner = find_best_reasoner(ranker_prediction_set, reasoner_average_rank_map);
                ranker_predictions.add(ranker_prediction_winner);

                // perform rank prediction model
                Instance target_rp_instance = create_rank_pm_instance(test_rank_pm_instances, test_rank_instance);
                double class_index = rank_pm.classifyInstance(target_rp_instance);
                String class_label = target_rp_instance.classAttribute().value((int) class_index);
                List<String> pm_rank_prediction_set = new ArrayList<>();
                pm_rank_prediction_set.add(class_label);
                pm_rank_predictions.add(class_label);

                // -------- perform the stacking model integrating the above 3 models
                // Find true reasoners (mostly the # of the true reasoners = 1, but sometimes more than 1)
                double[] true_ranking = Tools.getTargetVector(test_rank_instance);
                List<String> true_reasoners = get_best_reasoners_from_ranking(reasoner_names, true_ranking);

                // create final stack instances
                Instances meta_instances = create_final_stack_instances(pm_stack_prediction_set, ranker_prediction_set, pm_rank_prediction_set, stack_final_instances, true_reasoners);
                String final_stack_winner = final_stack_predict(final_stack_model, meta_instances, reasoner_average_rank_map);
                final_stack_predictions.add(final_stack_winner);

            }

            // read actual reasoning time data for the testing data for evaluation
            HashMap<String, EvaluatedReasoner> e_models = evaluate_models_predictions(
                    test_rank_instances, test_time_instances,
                    pm_stack_predictions, ranker_predictions,
                    pm_rank_predictions, final_stack_predictions, fold, test_dir);

            write_evaluated_result(e_models, test_dir + result_file_name);
        }
    }

    /**
     * Write the evaluation results into a file
     */
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

    private double evaluate_time_diff(List<Integer> actual_winners, double actual_times[], int reasoner_index) {
        return Math.expm1(actual_times[reasoner_index]) - Math.expm1(actual_times[actual_winners.get(0)]);
    }

    private void eval_model(EvaluatedReasoner m, int t_index, List<Integer> actual_winners, double actual_times[]) {


        reasoner_time_disc_enum time_label = discretize_time(actual_times[actual_winners.get(0)]);

        m.precisions[t_index] = evaluate_ranking(actual_winners, m.winners_indexes[t_index]);
        m.sum_precisions += m.precisions[t_index];
        m.precisions_per_bins[time_label.ordinal()] += m.precisions[t_index];

        m.reasoning_times[t_index] = Math.expm1(actual_times[m.winners_indexes[t_index]]);
        m.sum_reasoning_times += m.reasoning_times[t_index];
        m.reasoning_times_per_bins[time_label.ordinal()] += m.reasoning_times[t_index];

        m.additive_times[t_index] = evaluate_time_diff(actual_winners, actual_times, m.winners_indexes[t_index]);
        m.sum_additive_times += m.additive_times[t_index];
        m.additive_times_per_bins[time_label.ordinal()] += m.additive_times[t_index];
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

    /**
     * Evaluated all the meta-reasoners on the testing sets.
     */
    private HashMap<String, EvaluatedReasoner> evaluate_models_predictions(Instances test_rank_instances,
                                                                           Instances test_time_instances,
                                                                           List<String> pm_stack_predictions,
                                                                           List<String> ranker_predictions,
                                                                           List<String> pm_rank_predictions,
                                                                           List<String> final_stack_predictions,
                                                                           int fold,
                                                                           String test_dir) throws Exception {

        int n_bins = 4;
        int n_instances = test_rank_instances.numInstances();

        // read reasoners' names
        List<String> reasoner_names = Arrays.asList(Tools.getTargetNames(test_rank_instances.firstInstance()));

        // create evaluated reasoners
        HashMap<String, EvaluatedReasoner> e_models = new LinkedHashMap<>(); // # of reasoners + our 4 models + virtual best reasoner
        HashMap<Integer, String> reasoner_index_to_name_map = new HashMap<>();
        HashMap<String, Integer> reasoner_name_to_index_map = new HashMap<>();

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
        for (int i = 0; i < test_rank_instances.numInstances(); i++) {

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
                    sb.append(i + "," + final_stack_predictions.get(i) + "\n");
                } else if (model_name.equalsIgnoreCase("m5")) {
                    m.winners[i] = reasoner_index_to_name_map.get(actual_winners.get(0));
                } else {
                    throw new Exception("Model name: '" + model_name + "' doesn't exist!");
                }

                m.winners_indexes[i] = reasoner_name_to_index_map.get(m.winners[i]);

                eval_model(m, i, actual_winners, actual_times);
                e_models.put(model_name, m);
            }

        }
        write_eval_result(test_dir + "/prediction_" + fold + ".csv", sb);

        return e_models;
    }


    /**
     * Run and Evaluate the meta-reasoning framework R2O2*
     * @param args N/A
     */
    public static void main(String args[]) throws Exception {

        // To evalaute the key components in R2O2* in the training data, you need to set it to be true.
        // For example, if you want to
        //  - (1) measure the performance of different predictions models (implemeneted by RF, XGBoost, meta-RF, and meta-XGBoost) for each reasoner, where those models were used in R2O2*_pt,
        //  - (2) measure the performance of different rankers (implemented by 5 rankers - please see our paper), where a single best ranker was used in R2O2*_rk,
        //  - (3) measure the performance of different prediction models for multi-class classification, used in R2O2*_mc, then
        // you need to set it to be true. The performance will be measured via 10-fold cross-validation on the training data.
        boolean needEvaluation = false;

        // If you want to generate prediction models for R2O2*_pt, set it to be true. Note that once the prediction models are built, these models will be saved under ~/pm/*.
        // Then, we can use the built models later on. So once you first build the models, you can set it to be false when evaluating R2O2*.
        boolean genPredictionModel = false;

        // If you want to generate rankers for R2O2*_rk, set it to be true. Note that once the single best ranker is built, it will be saved under ~/train_mm/. In
        // our case, ARTForests.ranker will be chosen.
        // Then, we can use this model later on. So once you first build the best ranker, you can set it to be false when evaluating R2O2*.
        boolean genRankers = false;

        // If you want to generate the prediction models (implemented by RF and XGBoost) for R2O2*_mc, set it to be true. Note that once the prediction models are built, these models will be saved under ~/train_mm/.
        // In our experiments, pm-rank.arff.reg.model is the RF model, and pm-rank.arff.xgb_10.model is the XGBoost model.
        // Then, we can use the built models later on. So once you first build the models, you can set it to be false when evaluating R2O2*.
        boolean genRankPred = false;

        // If you want to build R2O2*_all (stacking model), you can set it to be true. The RF model is meta-stack-final.arff.reg.model, and the XGBoost model is  meta-stack-final.arff.xgb.model.
        // In the paper, the XGBoost model is used.
        boolean genFinalStackRankingModel = false;

        // If we want evaluate all the meta-reasoners in R2O2*, set it as true.
        boolean evalMode = true;

        MetaReasonerFramework metaReasonerFramework = new MetaReasonerFramework();

        // Input the dataset to be used for building R2O2* and tested by R2O2*
        String reasoningTimeDataDir = "data/ErrorsRemoved";

        // If you've generated N-fold cross-validation datasets, set it to be N.
        int nFold = 10; // 10-fold cross-validation

        // In our XGBoost implementation, we consistently used its max-depth as 10.
        int chosen_xgb_max_depth = 10;

        // create prediction models of reasoners and evaluatePredictedRanking their performance (for R2O2*_pt)
        if (genPredictionModel) {
            metaReasonerFramework.buildPredictionModel(reasoningTimeDataDir, "pm", nFold, chosen_xgb_max_depth, needEvaluation);
            // train elk
            metaReasonerFramework.buildPredictionModel(reasoningTimeDataDir, "elk", nFold, chosen_xgb_max_depth, needEvaluation);
        }

        // build rankers on the ranking matrix (for R2O2*_rk)
        if (genRankers) {
            metaReasonerFramework.buildRankerOnAcutal(reasoningTimeDataDir, nFold, needEvaluation);
        }

        // build multi-class classification model using RF and XGBoost (chosen eventually) for (for R2O2*_mc)
        if (genRankPred) {
            metaReasonerFramework.buildRankPredictionModel(reasoningTimeDataDir, nFold, chosen_xgb_max_depth, needEvaluation);
        }

        // build R2O2*_all model (stacking model of the above three different meta-reasoners, that is, R2O2*_pt, R2O2*_rk, and R2O2*_mc.
        if (genFinalStackRankingModel) {
            String ensemble[] = {".arff.stack_xgb.model", "ARTForests.ranker", "pm-rank.arff.xgb_10.model"}; // the built model names of the meta-reasoners.
            metaReasonerFramework.buildFinalStackRankingModel(reasoningTimeDataDir, nFold, ensemble, chosen_xgb_max_depth, needEvaluation);
        }

        // We use the XGBoost stacking model finally.
        if (evalMode) {
            // The evaluation result will be stored in the following file
            String xgb_result_fname = "final_eval_using_xgb.csv";

            // In our evaluation, we compare 4 meta-reasoners. The indices of the following list are the meta-reasoners, R2O2*_pt, R2O2*_rk, R2O2*_mc, and R2O2*_all in order.
            String xgb_models[] = {".arff.stack_xgb.model", "ARTForests.ranker", "pm-rank.arff.xgb_10.model", "meta-stack-final.arff.xgb.model"};

            // Evaluate R2O2*
            metaReasonerFramework.evaluateMR(xgb_models, reasoningTimeDataDir, nFold, xgb_result_fname);
        }
    }
}
