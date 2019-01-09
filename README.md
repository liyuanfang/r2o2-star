# R2O2\*
This document contains information about the input data and instructions on how to the R2O2\* meta-reasoners for OWL as well as how to generate ontology metrics. 

The source code expects Java 1.8. It is structured as a Maven project, therefore dependencies should be automatically resolved by Maven. 

## The meta-reasoner
The following three java files are the entry points to the R2O2\* meta-reasoner.


### Data generation: `r2o2/demo/DataGeneration.java`

This class is responsible for generating training and testing instances for our meta-reasoning framework R2O2\*. The main function in the class is able to generate the following two datasets:


  * **ErrorsRemoved**: this dataset excludes ontologies that produced runtime (reasoning time) errors by any reasoner: hermit, pellet, more, fact, jfact, and konclude.

  * **ErrorsReplaced**: this dataset includes the above error ontologies. The reasoning time of these ontologies is the pre-defined timeout threshold, which is 30 mins = 1800000 milli-seconds.

For more information about these datasets, please refer to our paper. <!-- (paper url is given here!). -->

For more instructions on how to generate these datasets, please refer to the above java file. The detailed instructions are given as comments in the java file. Overall, it generates n-fold cross-validation datasets for R2O2\*, where n is given as an input. For example, if n = 10, then we generate 0 ~ 9 different directories under the given top directory (i.e. given as an input). For each of these directories, we can see:


  * The **`elk/`** directory: it contains a file `r.elk.arff` that will be used to build a prediction model for ELK.
    
  * The **`pm/`** directory: it contains a list of files with names like `r.reasoner_name.arff`, where `reasoner_name` is the name of a reasoner (e.g., fact, hermit, etc.). In our demonstration, we can see 6 different reasoner names.

  * The **`test_mm/`** directory: it contains the following arff files:
    
    * `input-pred.arff`: It is an arff file for test ontologies, where reasoning time is not given, i.e., all reasoning time is -1 (unknown). This file will be used to evaluate R2O2\*<sub>pt</sub>. The actual reasoning time of different reasoners on these ontologies are found in 'meta-time-actual.arff'.
    
    * `input-pred-name.csv`: This file contains information about the indices of test ontologies (in order) and their actual names. It shows which ontologies are tested in the current fold.
        
    * `meta-rank-actual.arff`: This file is used to evaluate R2O2\*<sub>rk</sub>, where each instance shows ontologies' metric values and reasoners' rankings in terms of their efficiency. A lower value indicates a higher rank.
        
    * `meta-time-actual.arff`: This file is used to evaluate R2O2\*<sub>rk</sub>, where each instance shows ontologies' metric values and actual reasoning time of the reasoners.
        
    * `pm-rank.arff`: This file is used to evaluate R2O2\*<sub>mc</sub>. 
    
  * The **`train_mm/`** directory: It has the same names of the files as `test_mm` but the files contain training data.


### Meta-reasoner without ELK: `r2o2/demo/MetaReasonerFramework.java`

This class provides functionality for building our 4 different meta-reasoners in the meta-reasoning framework R2O2\* (without ELK as one of the component reasoners). For detailed instructions, please see the `main()` method where detailed comments are provided in the code.

### Meta-reasoner with ELK: `r2o2/demo/MetaReasonerFrameworkELK.java¶`

This class provides functionality for evaluating the proposed 4 meta-reasoners with ELK. To evaluate these models, `MetaReasonerFramework.java` needs to be run first to build key components in each meta-reasoner.

<!-- For more information on how to use the code, please see demo code at `src/main/java/edu/monash/infotech/r2o2/demo/R2O2EvaluationDemo.java`. -->

### Sample Data

In the given Java package, the **`data/`** directory contains 2 sample datasets (ORE 2015 datasets) used in our paper. The readers can generate training and testing data for cross validation using the code `DataGeneration.java`. Please see the above explanation under the heading `demo/DataGeneration.java`.

## Metrics calculation
The entry point to the metrics calculation component is `DirectoryProcessor.java` in the package `edu.monash.infotech.owl2metrics.metrics.jgrapht`. It takes a folder (of ontologies) as input and generates a .csv file containing metric values of these ontologies.

# Contact

Please feel free to contact us if you have questions about the code base and the paper. 

  * Yong-Bin Kang (ykang@swin.edu.au)
  * Yuan-Fang Li (yuanfang.li@monash.edu)
