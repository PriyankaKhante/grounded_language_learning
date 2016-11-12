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
import weka.filters.unsupervised.attribute.Remove;

public class SinglyAnnotatedObjectEXP {
	static Instances data;
	static HashMap<String, HashMap<String, String>> groundTruthTable = new HashMap<String, HashMap<String, String>>();
	public static void main(String[] args) {
		//behaviour-modailities to use
		String [] rc_behavior_modalities = {"drop_audio", "revolve_audio","push_audio", "hold_haptics","lift_haptics",
					"press_haptics","squeeze_haptics","grasp_size", "shake_audio", "look_color","look_shape"};  
				
		// Modalities that have been taken out
		// grasp-audio, hold-audio, lift-audio, poke-audio, press-audio, squeeze-audio, drop-haptics,
		// poke-haptics, revolve-haptics, push-haptics, shake-haptics, grasp-haptics
		
		//Collections.shuffle(Arrays.asList(rc_behavior_modalities));
		
		loadGroundTruthTable();
		
		try{
			commenceExperiments(rc_behavior_modalities);
			System.out.println("Done outputting all results!");
			System.out.println("Now computing the ultimate max");
			findMaxInstances(rc_behavior_modalities);
			findUltimateMax(rc_behavior_modalities);
			System.out.println("Producing points to plot");
			createCSVFile(rc_behavior_modalities);
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

	public static void commenceExperiments(String[] rc_behavior_modalities) throws Exception{
		// Get the object list
		DataLoaderCY DL = new DataLoaderCY();
		ArrayList<String> objects_list = DL.getObjectList();
	
		// Train and test sets are created in this way -> generate a set a 10 seeds first
		int[] seeds_array = randomGenerator(78, 1000);
			
		// File path to store the results in 
		String results_path = "/home/priyanka/Documents/grounded_language_learning/SinglyAnnotatedObjectTrials/SinglyAnnotatedResults/";
		
		FeatureDataLoader FDL = new FeatureDataLoader();
		
		// Create the files which have the object data along with the labels
		for(int g=0;g<rc_behavior_modalities.length;g++)
			FDL.createContextDataWithLabels(groundTruthTable, getAttributeForModality(rc_behavior_modalities[g]), rc_behavior_modalities[g], objects_list);
		
		for(int g=0;g<rc_behavior_modalities.length;g++){
			System.out.println("Behaviour Modality:  " + rc_behavior_modalities[g]);
			String modality_result_path = results_path + rc_behavior_modalities[g];
			// For every modality create a folder to store the results in
			Files.createDirectories(Paths.get(modality_result_path));
			
			String attr = getAttributeForModality(rc_behavior_modalities[g]);
			
			//load pre-computed features
			FDL.loadContextsDataWithLabels(rc_behavior_modalities);
		
			// Get the labels of all the objects to compare our classifiers against
			HashMap<String, String> object_labels = new HashMap<String, String>();
			for(int l=0;l<objects_list.size();l++){
				HashMap<String, String> combination = groundTruthTable.get(objects_list.get(l)); 
				String label = combination.get(attr);
				object_labels.put(objects_list.get(l), label);	
			}
			
			for(int i=0;i<seeds_array.length;i++){
				System.out.println("i: " + i);
				
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
					PrintWriter writer = new PrintWriter(modality_result_path + "/result" + i + "_" + j + ".txt", "UTF-8");
					
					Collections.shuffle(objects);
					
					// After the shuffled data is generated, build classifiers after each object is added and test on the test set
					// To build a classifier -> 1) get the label for the object from the ground truth table
											//  2) Create instances of all these chosen objects
											//  3) Build the classifier and test on the test objects 
					
	 				for(int k=2;k<=objects.size();k++){
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
	 						
	 					//labeling function labels trials by labels
	 					IClassLabelFunction LF = new ClassLabelID(object_list, object_labels);
	 					ArrayList<String> class_values = LF.getValueSet();
	 					String [] classVals = new String[class_values.size()];
	 					for (int f = 0; f < class_values.size(); f++)
	 						classVals[f]=class_values.get(f);
	 						
	 					//setup instances in order to setup kernel
	 					ContextInstancesCreator IC = new ContextInstancesCreator();
	 					IC.setLabelFunction(LF);
	 					IC.setData(FDL.getData(rc_behavior_modalities[g]));
	 					int success = IC.generateHeader();
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
	 						
	 					// The following removes all String attributes and doesn't use them while classifying
	 					//fc.setFilter(new weka.filters.unsupervised.attribute.RemoveType());
	 					//fc.setClassifier(tree);
						//fc.buildClassifier(training_subset);   
	 					
	 					// build classifier	
	 					//tree.buildClassifier(train);
	 				    
	 				    // Build Classifier only out of these chosen instances 
	 				    // try different ones - decision trees, KNN and SVM
	 				    // Following is the implementation of decision trees - J48
	 					EvaluationJS EV = new EvaluationJS(training);
	 						
	 					//train - boosted decision trees
	 					Classifier C = new J48();
	 					Classifier C_boost = new AdaBoostM1();
	 					((AdaBoostM1)C_boost).setClassifier(C);
	 						
	 					C_boost.buildClassifier(train);
	 												
						// Test on the test objects and store the accuracy
	 					ArrayList<InteractionTrial> test_trials = DL.generateTrials(test_objects, 6);		 						
	 					Instances test = IC.generateFullSet(test_trials);
	 						
	 					writer.println("\t\tNEW ITERATION: Train on "+train.numInstances()+" and test on "+ test.numInstances());
	 						
	 					EV.evaluateModel(C_boost, test);
	 			
	 					//EV.crossValidateModel(C_boost, training, 10, new Random(1));
	 						
	 					// Write out the results to the file
	 					writer.println(EV.toSummaryString());
	 					writer.println(EV.toClassDetailsString());
	 					writer.println("Kappa Statistics: " + EV.kappa());
	 					writer.println();
	 					//System.out.println(EV.toMatrixString());
	 						
	 				   /*Evaluation eval = new Evaluation(train);
	 				   eval.evaluateModel(tree, test);*/
					}
	 				writer.close();
				}
			}
		}
	}

	// Method to get the corresponding attribute for the modality
	public static String getAttributeForModality(String modality){
		if(modality.equals("drop_audio"))
			return "material";
		if(modality.equals("revolve_audio"))
			return "has_contents";
		if(modality.equals("push_audio"))
			return "material";
	    if(modality.equals("shake_audio"))
			return "has_contents";
		if(modality.equals("hold_haptics"))
			return "weight";
		if(modality.equals("lift_haptics"))
			return "weight";
		if(modality.equals("press_haptics"))
			return "height";
		if(modality.equals("squeeze_haptics"))
			return "deformable";
		if(modality.equals("grasp_size"))
			return "size";
		if(modality.equals("look_color"))
			return "color";
		if(modality.equals("look_shape"))
			return "shape";
		
		return "";
	}
	
	// Method to generate the 10 seeds for trials
	public static int[] randomGenerator(int seed, int max) {
		int[] seeds_array = new int[10];
	    Random generator = new Random(seed);
	    
	    for(int i=0;i<10;i++){
	    	int num = generator.nextInt();
	    	System.out.println("Seed: " + num);
	    	seeds_array[i] = num;
	    }
	    return seeds_array;
	}
	
	// Method to find the maximum of the correct instances for each file
	// The output is written to a file in each folder
	public static void findMaxInstances(String [] rc_behavior_modalities) throws Exception{
		// File path to store the results in 
		String results_path = "/home/priyanka/Documents/grounded_language_learning/SinglyAnnotatedObjectTrials/SinglyAnnotatedResults/";
		
		for(int g=0;g<rc_behavior_modalities.length;g++){
			File folder = new File(results_path + rc_behavior_modalities[g]);
			File[] listOfFiles = folder.listFiles();
			for(int i=0;i<listOfFiles.length;i++){
				String fileName = listOfFiles[i].getName();
				if(!(listOfFiles[i].isDirectory())){
					if(!fileName.equals("MaxNumOfInstances.txt")){
						String maxResult = readResultFiles(results_path + rc_behavior_modalities[g] + "/" + fileName);
						File maxResultFilePath = new File(results_path + rc_behavior_modalities[g] + "/MaxResults");
						maxResultFilePath.mkdirs();
						writeMaxResultFile(results_path + rc_behavior_modalities[g] + "/MaxResults/Max" + fileName, maxResult);
					}
				}
			}
		}
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
	
	// Method to find the maximum of the correct instances for each modality
	// The output is written to a file in each folder
	public static void findUltimateMax(String [] rc_behavior_modalities) throws Exception{
		// File path to store the results in 
		String results_path = "/home/priyanka/Documents/grounded_language_learning/SinglyAnnotatedObjectTrials/SinglyAnnotatedResults/";
				
		for(int g=0;g<rc_behavior_modalities.length;g++){
			File folder = new File(results_path + rc_behavior_modalities[g] + "/MaxResults");
			File[] listOfFiles = folder.listFiles();
			String maxResult = "";
			String minResult = "";
			String maxMinResult = "";
			for(int i=0;i<listOfFiles.length;i++){
				String fileName = listOfFiles[i].getName();
				maxResult = readMaxResultFiles(results_path + rc_behavior_modalities[g] + "/MaxResults/" + fileName, maxResult);
				minResult = readMinResultFiles(results_path + rc_behavior_modalities[g] + "/MaxResults/" + fileName, minResult);
				writeMaxResultFile(results_path + rc_behavior_modalities[g] + "/MaxNumOfInstances.txt", maxResult);
				writeMaxResultFile(results_path + rc_behavior_modalities[g] + "/MinNumOfInstances.txt", minResult);
			}
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
	public static void createCSVFile(String [] rc_behavior_modalities) throws Exception{
		// File path to store the results in 
		String results_path = "/home/priyanka/Documents/grounded_language_learning/SinglyAnnotatedObjectTrials/SinglyAnnotatedResults/";
						
		for(int g=0;g<rc_behavior_modalities.length;g++){
			String writeFilePath = results_path + rc_behavior_modalities[g] + "/PointsToPlot.csv";
			File folder = new File(results_path + rc_behavior_modalities[g]);
			File[] listOfFiles = folder.listFiles();
			
			try{
				PrintWriter writer = new PrintWriter(writeFilePath, "UTF-8");
				for(int i=0;i<listOfFiles.length;i++){
					String fileName = listOfFiles[i].getName();
				
					if(!(listOfFiles[i].isDirectory())){
						if(!(fileName.equals("MaxNumOfInstances.txt")) && !(fileName.equals("MinNumOfInstances.txt")) && !(fileName.equals("PointsToPlot.csv"))){
							FileReader fileReader = new FileReader(results_path + rc_behavior_modalities[g] + "/" + fileName);
						    BufferedReader bufferedReader = new BufferedReader(fileReader);
		
						    String line = "";
						    int numOfInstTrainedOn = 0;
						    float correctAccuracy = 0;
						    double kappa = 0;
						   
							while((line = bufferedReader.readLine()) != null) {
								if (line.contains("NEW ITERATION")){
									String[] tokens = line.split(" ");
									numOfInstTrainedOn = Integer.parseInt(tokens[4]);
									writer.print(numOfInstTrainedOn/6 + ",");
							    }
							
								if(line.contains("Correctly Classified Instances")){
								    String[] tokens = line.split("              ");
								    correctAccuracy = Float.parseFloat(tokens[1].split("%")[0].trim());  
								    writer.print(correctAccuracy + ",");
								}
								 
								if (line.contains("Kappa Statistics")){
									String[] tokens = line.split(":");
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
