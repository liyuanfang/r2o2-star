package edu.monash.infotech.r2o2.demo;

import edu.monash.infotech.r2o2.evaluation.ReasonerComponent;
import edu.monash.infotech.r2o2.reasoner.R2O2;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

import java.io.File;

/**
 * This r2o2 shows how to use R2O2 in training and testing phases with sample data provided.
 */
public class R202Demo {

    public static void main(String args[]) throws Exception {

        ClassLoader classLoader = R202Demo.class.getClassLoader();

        File pmPerfFile = new File(classLoader.getResource("r2o2/pm/pm.performance.csv").getFile());
        File avgRankPerfFile = new File(classLoader.getResource("r2o2/train_mm/ranker.summary.avgRank").getFile());
        File rankPerfFile = new File(classLoader.getResource("r2o2/train_mm/ranker.summary").getFile());
        File trainMetricPropertyFile = new File(classLoader.getResource("r2o2/train_mm/ore14_all.csv.filter.csv.properties").getFile());

        File testOntMatrixFile = new File(classLoader.getResource("r2o2/test_mm/test_onts_metrics.csv").getFile());
        File testOntFile = new File(classLoader.getResource("r2o2/test_mm/test_onts.arff").getFile());

        /**
         * Step 1: Create a meta-reasoner by loading trained rankers
         */
        System.out.println("Loading rankers .....");
        R2O2 metaReasoner = new R2O2(classLoader.getResource("r2o2/train_mm").getFile(), "ranker");

        /**
         * Step 2: load prediction models
         */
        System.out.println("Loading prediction models .....");
        metaReasoner.loadPredictionModels(pmPerfFile.getParent(), "reg");

        /**
         * Step 3: read the ontology metrics of the testing ontologies and transform them as we did on the training data (followed methods in our AAAI paper)
         * Instance format: ontology, M1, ... M_k, where k is the number of chosen metrics after preprocessing
         */
        Instances testRawInstances = metaReasoner.transformTestInstances(testOntMatrixFile.getAbsolutePath(), trainMetricPropertyFile.getAbsolutePath());
        //System.out.println(testRawInstances);

        /**
         * Step 4: read testing ontology file locations
         * Instance format: ontology path, pred_time, actual_time
         */
        Instances testOntInstances = metaReasoner.readOntology(testOntFile.getAbsolutePath());


        /**
         * Step 5: generate (1) an instance set for the use in the prediction models, and (2) a rank matrix for the used in rankers.
         */
        Instances testPMInstances = metaReasoner.generatePMInstances(testRawInstances);
        testPMInstances.setClassIndex(testPMInstances.numAttributes()-1);
        //System.out.println(testPMInstances);

        String reasonerNames[] = metaReasoner.getReasonerNames(pmPerfFile.getAbsolutePath());
        Instances testMetaInstances = metaReasoner.generateMetaInstances(testRawInstances, reasonerNames);
        //System.out.println(testRankingInstances);

        // read the ranking matrix profile, performance of the reasoners, performance of rankers
        System.out.println("Reading meta data properties .....");
        metaReasoner.readMetaDataProfile(reasonerNames, pmPerfFile.getAbsolutePath(), avgRankPerfFile.getAbsolutePath(), rankPerfFile.getAbsolutePath());

        /**
         * Step 2: apply R202
         */
        Instances resultInstances = new Instances(testOntInstances, 0);
        for (int i = 0; i < testPMInstances.numInstances(); i++) {
            Instance instancePM = testPMInstances.instance(i);
            Instance instanceMeta = testMetaInstances.instance(i);

            // predict the ranking of reasoner components
            ReasonerComponent[] predRankingResult = metaReasoner.recommendRanking(instanceMeta);

            // choose the most efficient reasoner determined by R202
            ReasonerComponent metaR = metaReasoner.findBestReasoner(predRankingResult);
            System.out.println("meta reasoner:" + metaR);

            // predict the reasoning time
            double predRTime = metaReasoner.predict(metaR, instancePM);

            // run the meta-reasoner
            Instance ontInstance = testOntInstances.instance(i);
            double actualRTime = metaReasoner.run(metaR, classLoader.getResource("r2o2/test_mm").getPath(), ontInstance);
            System.out.println("pellet: pred time:" + predRTime + ", actual time:" + actualRTime);

            // save the result instance
            //Instance resultInstance = metaReasoner.generateResultInstance(ontInstance, predRTime, actualRTime);
            //resultInstances.add(resultInstance);
        }

        // write the final result
        ArffSaver writer = new ArffSaver();
        writer.setInstances(resultInstances);
        writer.setFile(new File(testOntFile.getAbsolutePath() + ".res.arff"));
        writer.writeBatch();
    }
}