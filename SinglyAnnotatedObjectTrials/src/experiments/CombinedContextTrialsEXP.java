package experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

import data.DataLoaderCY;
import data.InteractionTrial;
import evaluation.EvaluationJS;
import features.ClassLabelID;
import features.ContextInstancesCreator;
import features.FeatureDataLoader;
import features.IClassLabelFunction;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.meta.FilteredClassifier;
import weka.classifiers.trees.J48;
import weka.clusterers.FilteredClusterer;
import weka.core.Instances;
import weka.core.Utils;
import weka.filters.unsupervised.attribute.Remove;

public class CombinedContextTrialsEXP {
	static Instances data;
	static HashMap<String, HashMap<String, String>> groundTruthTable = new HashMap<String, HashMap<String, String>>();
	public static void main(String[] args) {
		//behaviour-modailities to use
		String [] rc_behavior_modalities = {"drop_audio", "revolve_audio","push_audio", "hold_haptics", "lift_haptics",
				"press_haptics","squeeze_haptics","grasp_size", "shake_audio", "look_color","look_shape", "grasp_audio",
				"hold_audio", "lift_audio", "poke_audio", "press_audio", "squeeze_audio", "drop_haptics", "poke_haptics", 
				"revolve_haptics", "push_haptics", "shake_haptics", "grasp_haptics"};   
		
		//attributes to be learned
		String[] attributes = {"size", "material", "has_contents", "height", "deformable", "color", "shape"};
				//"weight","deformable_from_top", "size", "material", "has_contents", "height",
				
		// Modalities that have been taken out
		// grasp-audio, hold-audio, lift-audio, poke-audio, press-audio, squeeze-audio, drop-haptics,
		// poke-haptics, revolve-haptics, push-haptics, shake-haptics, grasp-haptics
		
		//Collections.shuffle(Arrays.asList(rc_behavior_modalities));
		
		loadGroundTruthTable();
		
		try{
			//commenceExperiments(rc_behavior_modalities, attributes);
			System.out.println("Done outputting all results!");
			System.out.println("Now computing the ultimate max");
			System.out.println("Producing points to plot");
			createCSVFile(attributes);
			System.out.println("DONE!");
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/*
	 * Outputs a convoluted table in hashmaps after the input of the grounded truth.
	 * Maps: obj_name -> attribute_name (eg color) -> value
	 * The following attributes have numerical output: height, weight and width
	 */
	public static void loadGroundTruthTable(){
		try{
			BufferedReader in = new BufferedReader(new FileReader("/home/priyanka/Documents/attributesGroundTruthTable.csv"));
			String attributes[] = new String[9];
			//grab all the attribute names. Note that these should match that is output
			//by the modified weka program
			String line;
			if((line = in.readLine()) != null){
				StringTokenizer tokenizer = new StringTokenizer(line,",");
				int columnNum = 0;
				
				// Skip the first token as its "object_names"
				tokenizer.nextToken();
				
				while (tokenizer.hasMoreTokens()){
					//System.out.println("Token: " + tokenizer.nextToken());
					attributes[columnNum] = tokenizer.nextToken();
					columnNum++;
				}
			}

			//build the rest of the table
			while((line = in.readLine()) != null){
				StringTokenizer tokenizer = new StringTokenizer(line,","); 
				int columnNum = 0;
				HashMap<String,String> attrTruthTable = new HashMap<String,String>();

				String name = tokenizer.nextToken();
				
				while (tokenizer.hasMoreTokens()){ 
					attrTruthTable.put(attributes[columnNum], tokenizer.nextToken());
					columnNum++;
				}
	
				groundTruthTable.put(name,attrTruthTable);
			}

			in.close();
		} catch(IOException e){
 		 e.printStackTrace();
		}
	}

	public static void commenceExperiments(String[] rc_behavior_modalities, String[] attributes) throws Exception{
		// Get the object list
		DataLoaderCY DL = new DataLoaderCY();
		ArrayList<String> objects_list = DL.getObjectList();
	
		// Train and test sets are created in this way -> generate a set a 10 seeds first
		int[] seeds_array = randomGenerator(78, 1000);
			
		// File path to store the results in 
		String results_path = "/home/priyanka/Documents/grounded_language_learning/SinglyAnnotatedObjectTrials/CombinedContextTrialResults/";
		
		FeatureDataLoader FDL = new FeatureDataLoader();
		//load pre-computed features
		FDL.loadContextsDataWithLabels(rc_behavior_modalities);
		
		for(int m=0;m<attributes.length;m++){
			System.out.println("******** Learning the attribute: " + attributes[m] + " ********");
			
			//String modality_result_path = results_path + rc_behavior_modalities[g];
			// For every modality create a folder to store the results in
			Files.createDirectories(Paths.get(results_path + "/" + attributes[m]));
			
			// Load the labelled data
			loadLabelledData(rc_behavior_modalities, attributes[m]);
		
			// Get the labels of all the objects to compare our classifiers against
			HashMap<String, String> object_labels = new HashMap<String, String>();
			for(int l=0;l<objects_list.size();l++){
				HashMap<String, String> combination = groundTruthTable.get(objects_list.get(l)); 
				String label = combination.get(attributes[m]);
				object_labels.put(objects_list.get(l), label);	
			}
			
			//labeling function labels trials by labels
			IClassLabelFunction LF = new ClassLabelID(objects_list, object_labels);
			ArrayList<String> allClassValues = LF.getValueSet(objects_list);      // This one has each of the class values including duplicates
			ArrayList<String> class_values = LF.getUniqueValueSet(objects_list);   // This one does not have any duplicates
			String [] classVals = new String[class_values.size()];
			for (int f = 0; f < class_values.size(); f++)
				classVals[f]=class_values.get(f);
		
			for(int i=0;i<seeds_array.length;i++){
				// temp variable to store object list each time
				ArrayList<String> object_list = DL.getObjectList();
			
				// Generate the test and training object sets for each seed
				ArrayList<String> test_objects = DL.getRandomTestObjectSet(object_list, seeds_array[i]);
				ArrayList<String> objects = DL.getRandomTrainObjectSet(object_list, test_objects);
			
				// Print out the test objects
				/*for(int o=0; o<test_objects.size();o++)
					System.out.println("Test objects: " + test_objects.get(o));*/
				
				
				// Get the labels of all the test objects to compare our classifiers against
				/*HashMap<String, String> test_object_labels = new HashMap<String, String>();
				for(int l=0;l<test_objects.size();l++){
					HashMap<String, String> combination = groundTruthTable.get(test_objects.get(l)); 
					String label = combination.get(attr);
					System.out.println("Test Labels: " + label);
					test_object_labels.put(test_objects.get(l), label);	
				}*/
					
				//shuffle only the training set 10 times and generate more such trials. 
				for(int j=0;j<10;j++){    
					//System.out.println(" *************** SHUFFLING THE TRAINING OBJECTS *****************" + "\n\n");
					
					// For every round of shuffling create a new file to store the results
					PrintWriter writer = new PrintWriter(results_path + "/" + attributes[m] + "/" + i + "_" + j + ".txt", "UTF-8");
					
					Collections.shuffle(objects);
					
					// Setup evaluations for each test-train split combination
					EvaluationJS[] EV = new EvaluationJS[rc_behavior_modalities.length];
					/*for (int bm=0; bm < rc_behavior_modalities.length; bm++){
						EV[bm] = new EvaluationJS(LF.getNumClasses(), classVals, null);
					}*/
					
					//setup array to store temp classifier for multiple behavior testing
					Classifier[] C_array = new Classifier[rc_behavior_modalities.length];
					
					// After the shuffled data is generated, build classifiers after each object is added and test on the test set
					// To build a classifier -> 1) get the label for the object from the ground truth table
											//  2) Create instances of all these chosen objects
											//  3) Build the classifier and test on the test objects					
	 				for(int k=2;k<=objects.size();k++){
	 					EvaluationJS EV_full = new EvaluationJS(LF.getNumClasses(), classVals, null);
						// Another loop to go till the number of chosen objects
	 					//HashMap<String, String> train_object_labels = new HashMap<String, String>();
	 					ArrayList<String> train_objects = new ArrayList<String>();
	 					
	 					// In this loop add the subset of training objects you are going to use for this round
	 					for(int l=0;l<k;l++){
	 						train_objects.add(objects.get(l));
	 						/*HashMap<String, String> combination = groundTruthTable.get(train_objects.get(l)); 
	 						String label = combination.get(attr);
	 						train_object_labels.put(train_objects.get(l), label);	*/
	 					}
		 					
	 					//System.out.println("Train objects size: " + train_object_labels.size());
	 					
	 					// Create instances for all the objects including the one you just got the label for
	 					//labeling function labels trials by object ID
	 					/*IClassLabelFunction LF = new ClassLabelID(train_objects);
	 					ArrayList<String> class_values = LF.getValueSet();
	 					String [] classVals = new String[class_values.size()];
	 					for (int f = 0; f < class_values.size(); f++)
	 						classVals[i]=class_values.get(f);*/
	 						
	 					for(int g=0;g<rc_behavior_modalities.length;g++){
	 						// Declare the classifier
	 						Classifier C = new J48();
	 						Classifier C_boost = new AdaBoostM1();
	 						((AdaBoostM1)C_boost).setClassifier(C);
	 						
							//System.out.println("Behaviour Modality:  " + rc_behavior_modalities[g]);
	 						
	 						ContextInstancesCreator IC = new ContextInstancesCreator();
	 	 					IC.setLabelFunction(LF);
		 					IC.setData(FDL.getData(rc_behavior_modalities[g]));
		 					int success = IC.generateHeader(train_objects, objects_list);
	 						if(success == 0){
		 						break;
		 					}
	 						
		 					//full set of trials for training
		 					ArrayList<InteractionTrial> train_trials = DL.generateTrials(train_objects, 6);
							Instances train = IC.generateFullSet(train_trials);
		 						
		 					// Get a Filtered Classifier to use attributes without the name of the object
		 					//FilteredClassifier fc = new FilteredClassifier();
		 					
		 					/*String[] options = new String[1];
		 					options[0] = "-U";            // unpruned tree
								
	 						J48 tree = new J48();         // new instance of tree
		 					tree.setOptions(options);     // set the options
		 					*/
		 						
		 					// Set up a remove filter to remove the nominal attributes before classifying
		 					Remove remove = new Remove();
		 					remove.setAttributeIndices(Integer.toString(train.numAttributes()-1));
		 					remove.setInvertSelection(false);
		 					remove.setInputFormat(train);
		 						
		 					Instances training = weka.filters.Filter.useFilter(train, remove);
		 					//System.out.println("Training objects: " + train_objects.toString());
		 					//System.out.println("Training instances: " + training.toString());
		 						
		 					// The following removes all String attributes and doesn't use them while classifying
		 					//fc.setFilter(new weka.filters.unsupervised.attribute.RemoveType());
		 					//fc.setClassifier(tree);
							//fc.buildClassifier(training_subset);   
		 					
		 					// build classifier	
		 					//tree.buildClassifier(train);
		 				    
		 				    // Build Classifier only out of these chosen instances 
		 				    // try different ones - decision trees, KNN and SVM
		 				    // Following is the implementation of decision trees - J48
		 					EV[g] = new EvaluationJS(training);
		 						
		 					//train - boosted decision trees
		 					Classifier C_mb = Classifier.makeCopy(C_boost);
		 					C_mb.buildClassifier(training);
		 					C_array[g] = C_mb;
		 					
		 					IC.generateHeaderForTest(test_objects, objects_list);
		 												
							// Test on the test objects and store the accuracy
		 					ArrayList<InteractionTrial> test_trials = DL.generateTrials(test_objects, 6);		 						
		 					Instances test = IC.generateFullSet(test_trials);
		 					
		 					// Remove from test instances as well - Not sure if it causes a difference
		 					Remove remove1 = new Remove();
		 					remove1.setAttributeIndices(Integer.toString(test.numAttributes()-1));
		 					remove1.setInvertSelection(false);
		 					remove1.setInputFormat(test);
		 						
		 					Instances testing = weka.filters.Filter.useFilter(test, remove1);
		 					//System.out.println("Test objects: " + test_objects.toString());
		 					//System.out.println("Test instances: " + testing.toString());
		 					
		 					for(int t=0;t<testing.numInstances(); t++){
		 						//System.out.println("BEFORE Test instance at " + t + ": " + test.instance(t));
		 						EV[g].updateStatsForClassifier(C_mb.distributionForInstance(testing.instance(t)), testing.instance(t));
		 						//System.out.println("Test distribution for instance: " + Arrays.toString(C_mb.distributionForInstance(test.instance(t))));
		 					}
		 						
		 					// Write out the results to the file
		 					//writer.println(EV.toSummaryString());
		 					//writer.println(EV.toClassDetailsString());
		 					//writer.println("Kappa Statistics: " + EV.kappa());
		 					//writer.println();
		 					//System.out.println(EV.toMatrixString());
		 						
		 				   /*Evaluation eval = new Evaluation(train);
		 				   eval.evaluateModel(tree, test);*/
						}
	 					// evaluate from all contexts
	 					for(int o=0; o<test_objects.size()*6; o++){
	 						double [] final_distr = new double[LF.getNumClasses()];
	 						for (int d = 0; d <final_distr.length; d++)
	 							final_distr[d]=0.0;
	 						
 							for (int g = 0; g <rc_behavior_modalities.length; g++){
 								ContextInstancesCreator IC = new ContextInstancesCreator();
 								IC.setLabelFunction(LF);
 								IC.setData(FDL.getData(rc_behavior_modalities[g]));
 								IC.generateHeaderForTest(test_objects, objects_list);
									
 								// Test on the test objects and store the accuracy
 			 					ArrayList<InteractionTrial> test_trials = DL.generateTrials(test_objects, 6);		 						
 			 					Instances test = IC.generateFullSet(test_trials);
 			 					
 			 					// Remove from test instances as well - Not sure if it causes a difference
 			 					Remove remove1 = new Remove();
 			 					remove1.setAttributeIndices(Integer.toString(test.numAttributes()-1));
 			 					remove1.setInvertSelection(false);
 			 					remove1.setInputFormat(test);
 			 						
 			 					Instances testing = weka.filters.Filter.useFilter(test, remove1);
 								
 								//System.out.println("Test instance at " + o + ": " + test.instance(o));
 								double contextReliability = loadContextReliability(rc_behavior_modalities[g], attributes[m]);
 		 						//System.out.println("Context reliability of " + rc_behavior_modalities[g] + " is: " + contextReliability);
 		 						
 		 						// Convert all negative kappas to zero so as to normalize between 0-1     ====> Still up for discussion. Is negative Kappa worse than 0 Kappa?
 			 					if(contextReliability < 0)
 			 						contextReliability = 0;
 			 					
 								double[] distr_bm = C_array[g].distributionForInstance(testing.instance(o));
 								//System.out.println("Distribution for test instance: " + Arrays.toString(distr_bm));
 								for (int d = 0; d<final_distr.length; d++)
									final_distr[d]+=contextReliability*distr_bm[d];         // Adding up probabilities for each class label
 							}
 							
 							Utils.normalize(final_distr);
 							//System.out.println("Normalized Final distribution: " + Arrays.toString(final_distr));
 							//System.out.println("Getting the index of: " + test_objects.get(o/6));
 							int object_index = DL.getObjectList().indexOf(test_objects.get(o/6));
 							//System.out.println("Object list: " + objects_list.size());
 							int actual_class = LF.getUniqueClassIndex(allClassValues.get(object_index));
 							//System.out.println("Actual class later: " + actual_class);
 							EV_full.updateStatsForClassifier(final_distr, actual_class);
	 					}
	 					
	 					//final output
	 					double [] acc_rates = new double[rc_behavior_modalities.length];
	 					int c = 0;
	 					
	 					for (int b = 0; b <rc_behavior_modalities.length; b++){
 							System.out.println("Output for "+rc_behavior_modalities[b]+ " context.");
 							System.out.println(EV[b].toSummaryString());
 							
 							acc_rates[c]=EV[b].pctCorrect();
 							c++;
	 					}
	 					
	 					double avg_acc_single_behavior = Utils.mean(acc_rates);
	 					double std_avg_acc = Math.sqrt(Utils.variance(acc_rates));
	 					/*System.out.println("Mean and variance of single behavior acc:");
	 					System.out.println(avg_acc_single_behavior+"\t"+std_avg_acc);
	 					
	 					System.out.println(EV_full.toClassDetailsString());
	 					System.out.println(EV_full.toSummaryString());*/
	 					
	 					writer.println("\t\tNEW ITERATION: Train on "+ train_objects.size() + " and test on "+ test_objects.size() + " objects");
	 					writer.println("Train objects: " + train_objects);
	 					writer.println("Test objects: " + test_objects);
	 					writer.println("Mean and variance of single behavior acc:" + avg_acc_single_behavior + "\t" + std_avg_acc);
	 					writer.println(EV_full.toClassDetailsString());
	 					writer.println(EV_full.toSummaryString());
	 					writer.println();
					}
					writer.close();
				}
			}
		}
	}
	
	public static double loadContextReliability(String rc_behavior_modality, String attr){
		// Variable to store the reliability of the context (mean kappa from context-attr mapping experiment)
		double contextReliability = -100000;
		String readFilePath = "/home/priyanka/Documents/grounded_language_learning/AutomatedExpNewAlgo/ContextAttrMappingResults/" + rc_behavior_modality + "/" + attr + "/ContextAttrQC&Kappas.csv";
		
		try{
			FileReader fileReader = new FileReader(readFilePath);
		    BufferedReader bufferedReader = new BufferedReader(fileReader);
		    
		    String currentLine = "";
		    String lastLine = "";
		    
			while((currentLine = bufferedReader.readLine()) != null) {
				lastLine = currentLine;
			}
			
			List<String> params = Arrays.asList(lastLine.split(","));
			
			contextReliability = Double.parseDouble(params.get(1).trim());
			//System.out.println("Context reliability of " + rc_behavior_modality + " for attribute " + attr + " is: " + contextReliability);
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return contextReliability;
	}
		
	public static void loadLabelledData(String[] rc_behavior_modalities, String global_attr){
		DataLoaderCY DL = new DataLoaderCY();
		ArrayList<String> objects_list = DL.getObjectList();
		
		FeatureDataLoader FDL = new FeatureDataLoader();
		
		// Create the files which have the object data along with the labels for all modalities
		for(int g=0;g<rc_behavior_modalities.length;g++)
			FDL.createContextDataWithLabels(groundTruthTable, global_attr, rc_behavior_modalities[g], objects_list);
	}
	
	// Method to generate the 10 seeds for trials
	public static int[] randomGenerator(int seed, int max) {
		int[] seeds_array = new int[10];
	    Random generator = new Random(seed);
	    
	    for(int i=0;i<10;i++){
	    	int num = generator.nextInt();
	    	seeds_array[i] = num;
	    }
	    return seeds_array;
	}
	
	public static String readResultFiles(String readFilePath){	
		File readFile = new File(readFilePath);
		int max_correct = 0;
		String maxResult = "";
		
		try{
			FileReader fileReader = new FileReader(readFile);
		    BufferedReader bufferedReader = new BufferedReader(fileReader);
		    
		    String line = "";
		    String prev_line = "";
		    int count = 0;
		    boolean store = false;
		   
			while((line = bufferedReader.readLine()) != null) {
			    if (line.contains("NEW ITERATION")){
			    	count = 1;
			    	prev_line = line;
			    	store = false;
			    }
			    
			    if(line.contains("Correctly Classified Instances")){
			    	String[] tokens = line.split("          ");
			    	if(Integer.parseInt(tokens[1].trim()) > max_correct){
			    		maxResult = "";
			    		max_correct = Integer.parseInt(tokens[1].trim());
			    		store = true;
			    	}
			    }
			    
			    if(count <= 18 && store == true){
				      if(count == 3)
				     	maxResult = prev_line + "\n";
				      maxResult = maxResult + line + "\n ";
		    	}
			    
		    	count++;
			}
			bufferedReader.close();
		}
		
		catch(Exception e){
			e.printStackTrace();
		}
		
		return maxResult;
	}
	
	public static void writeMaxResultFile(String writeFilePath, String maxResult){
		try{
			PrintWriter writer = new PrintWriter(writeFilePath, "UTF-8");
			writer.println(maxResult);
			writer.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static String readMaxResultFiles(String readFilePath, String maxResult){
		File readFile = new File(readFilePath);
		int max_correct = 0;
		
		// First get the current max number of correct instances
		if(!maxResult.equals("")){
			String[] lines = maxResult.split("\n");
			for(int i=0;i<lines.length; i++){
				if(lines[i].contains("Correctly Classified Instances")){
					String[] tokens = lines[i].split("          ");
			    	max_correct = Integer.parseInt(tokens[1].trim());
				}
			}
		}

		try{
			FileReader fileReader = new FileReader(readFile);
		    BufferedReader bufferedReader = new BufferedReader(fileReader);
		    
		    String line = "";
		    String prev_line = "";
		    int count = 0;
		    boolean store = false;
		   
			while((line = bufferedReader.readLine()) != null) {
				if (line.contains("NEW ITERATION")){
			    	count = 1;
			    	prev_line = line;
			    	store = false;
			    }
			
				 if(line.contains("Correctly Classified Instances")){
				    	String[] tokens = line.split("          ");
				    	if(Integer.parseInt(tokens[1].trim()) > max_correct){
				    		maxResult = "";
				    		max_correct = Integer.parseInt(tokens[1].trim());
				    		store = true;
				    	}
				    }
				    
				    if(count <= 18 && store == true){
					      if(count == 2)
					     	maxResult = prev_line + "\n";
					      maxResult = maxResult + line + "\n ";
			    	}
				    
			    count++;
			}
			bufferedReader.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		return maxResult;
	}
	
	public static String readMinResultFiles(String readFilePath, String minResult){
		File readFile = new File(readFilePath);
		int min_correct = 100000;
		
		// First get the current min number of correct instances
		if(!minResult.equals("")){
			String[] lines = minResult.split("\n");
			for(int i=0;i<lines.length; i++){
				if(lines[i].contains("Correctly Classified Instances")){
					String[] tokens = lines[i].split("          ");
					min_correct = Integer.parseInt(tokens[1].trim());
				}
			}
		}

		try{
			FileReader fileReader = new FileReader(readFile);
		    BufferedReader bufferedReader = new BufferedReader(fileReader);
		    
		    String line = "";
		    String prev_line = "";
		    int count = 0;
		    boolean store = false;
		   
			while((line = bufferedReader.readLine()) != null) {
				if (line.contains("NEW ITERATION")){
			    	count = 1;
			    	prev_line = line;
			    	store = false;
			    }
			
				 if(line.contains("Correctly Classified Instances")){
				    	String[] tokens = line.split("          ");
				    	if(Integer.parseInt(tokens[1].trim()) < min_correct){
				    		minResult = "";
				    		min_correct = Integer.parseInt(tokens[1].trim());
				    		store = true;
				    	}
				    }
				    
				    if(count <= 18 && store == true){
					      if(count == 2)
					     	minResult = prev_line + "\n";
					      minResult = minResult + line + "\n ";
			    	}
				    
			    count++;
			}
			bufferedReader.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		return minResult;
	}
	
	// Method to create a .csv file per context with all the data points to plot a graph
	public static void createCSVFile(String[] attributes) throws Exception{
		// File path to store the results in 
		String results_path = "/home/priyanka/Documents/grounded_language_learning/SinglyAnnotatedObjectTrials/CombinedContextTrialResults/";
						
		for(int g=0;g<attributes.length;g++){
			String writeFilePath = results_path + attributes[g] + "/PointsToPlot.csv";
			File folder = new File(results_path + attributes[g]);
			File[] listOfFiles = folder.listFiles();
			
			try{
				PrintWriter writer = new PrintWriter(writeFilePath, "UTF-8");
				for(int i=0;i<listOfFiles.length;i++){
					String fileName = listOfFiles[i].getName();
				
					if(!(listOfFiles[i].isDirectory())){
						if(!(fileName.equals("MaxNumOfInstances.txt")) && !(fileName.equals("MinNumOfInstances.txt")) && !(fileName.equals("PointsToPlot.csv"))){
							FileReader fileReader = new FileReader(results_path + attributes[g] + "/" + fileName);
						    BufferedReader bufferedReader = new BufferedReader(fileReader);
		
						    String line = "";
						    int numOfInstTrainedOn = 0;
						    double mean = 0;
						    double std_dev = 0;
						    double kappa = 0;
						   
							while((line = bufferedReader.readLine()) != null) {
								if (line.contains("NEW ITERATION")){
									String[] tokens = line.split(" ");
									numOfInstTrainedOn = Integer.parseInt(tokens[4]);
									writer.print(numOfInstTrainedOn + ",");
							    }
								
								if(line.contains("Mean and variance of single behavior acc:")){
									String[] tokens = line.split(":");
									String[] tokens2 = tokens[1].trim().split("	");
									mean = Double.parseDouble(tokens2[0].trim());
									std_dev = Double.parseDouble(tokens2[1].trim());
									writer.print(mean + "," + std_dev + ",");
								}
								 
								if (line.contains("Kappa statistic")){
									String[] tokens = line.split("                        ");
									kappa = Double.parseDouble(tokens[1].trim());
									writer.print(kappa + "\n");
								}
							}
						}
					}
				}
				writer.close();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	}
}

