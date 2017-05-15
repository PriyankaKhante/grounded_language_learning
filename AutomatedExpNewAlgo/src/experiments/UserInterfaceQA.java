package experiments;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import java.util.Map.Entry;

import GroundedLanguage.Pair;
import GroundedLanguage.Triple;
import categorization.ClusterDB;
import categorization.GenericSimDB;
import categorization.ObjectClusterer;
import data.DataLoaderCY;
import data.InteractionTrial;
import evaluation.EvaluationJS;
import features.ClassLabelID;
import features.ContextInstancesCreator;
import features.FeatureDataLoader;
import features.IClassLabelFunction;
import weka.PolyKernelJS;
import weka.classifiers.Classifier;
import weka.classifiers.meta.AdaBoostM1;
import weka.classifiers.trees.J48;
import weka.clusterers.SimpleKMeans;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.Remove;

/* This class uses cluster data as input to facilitate automated question answering
 * as per the algorithm proposed by Priyanka. At each step the user is shown a cluster 
 * via an interface and he chooses to either label or skip the cluster and the classifier
 * is trained after every cluster that gets a label and the question count is recorded and 
 * written to an output file.
 * A provided grounded truth table will allow for lookups for a corresponding question
 * output is a table:
 | context | attribute | Label | object_id | Questions |
 _______________________________________________________
 where 'object_id' holds a list of object ids that are 'label' in 'attribute'
 Questions holds the number of questions required to obtain this pairing
 */

public class UserInterfaceQA{
	public static final String reqFilePath	= "C:\\Users\\Priyanka\\Desktop\\";
	public static final String responseName	= "groundedResponse.txt";
	public static final String requestName	= "groundedRequest.txt";
	
	//behaviour-modailities to use
	static String [] rc_behavior_modalities = {"look_color", "drop_audio", "push_audio",
			"press_haptics", "grasp_size", "shake_audio", "drop_haptics", "revolve_haptics"};  
	
	//attributes to be learned
	static String[] attributes = {"shape", "has_contents", "height", "deformable"};
	// Group A - {"color", "material", "weight", "size"}
	// Group B - {"shape", "has_contents", "height", "deformable"}
	
	// Modalities that have been taken out
	// grasp-audio, hold-audio, lift-audio, poke-audio, press-audio, squeeze-audio, drop-haptics,
	// poke-haptics, revolve-haptics, push-haptics, shake-haptics, grasp-haptics
			
	//Collections.shuffle(Arrays.asList(rc_behavior_modalities));

	static String modality;
	static String old_mod = "";
	static ArrayList<String> cur_cluster = new ArrayList<String>();
	static int clusterNum; //is this needed?'
	
	static Instances data;
	static HashMap<String, HashMap<String, String>> groundTruthTable = new HashMap<String, HashMap<String, String>>();
	static HashMap<String, Integer> questionCountPerContext = new HashMap<String, Integer>();
	static HashMap<Pair, ArrayList<Triple> > labelTable;
	static int questionCount = 0;
	static int questionCount2 = 0;
	
	// questions_per_label IS NOT USED FOR THIS EXPERIMENT. 
	// IT IS KEPT BECAUSE SOME CLASSES NEED IT AS A PARAMETER.
    static int questions_per_label = 0;
    
    static HashMap<String, ClusterDB> behavior_modality_clusters = new HashMap<String, ClusterDB>();
	static HashMap<ClusterDB, ObjectClusterer> objectClusterTable = new HashMap<ClusterDB, ObjectClusterer>();
    
    // To store the test and training objects for each modality
    //static ArrayList<String> test_objects;
	static ArrayList<String> objects;
	static boolean firstTime;
	static String global_attr = "";
	static Pair globalContextPair = null;

	public static void main(String[] args) {
		//load object IDs into ground truth table. Leave attribute and attr label empty for now. 
		DataLoaderCY DL = new DataLoaderCY();
		ArrayList<String> object_list = DL.getObjectList();
		
		for(int i=0;i<object_list.size();i++){
			HashMap<String, String> tuple = new HashMap<String, String>();
			groundTruthTable.put(object_list.get(i), tuple);
		}
		
		try{
			// Check if the first request is for a new cluster
			File req = new File("C:\\Users\\Priyanka\\Desktop\\groundedRequest.txt");
			//File req = new File("/home/users/pkhante/Desktop/groundedRequest.txt");
			while(!req.exists()){
				//System.out.println("Going to sleep as first file does not exist!");
				Thread.sleep(2000); 
			}
			//System.out.println("Waking up as first request.txt now exists!");
			FileReader fr = new FileReader(req);
			BufferedReader br = new BufferedReader(fr);
			boolean validReq = false;
			String nl = "";
			while((nl = br.readLine()) != null) {
				if(nl.equals("1")){
					validReq = true;
				    break;
				}
				else{
				    System.out.println("First request is not valid! Exiting the program");
				    System.exit(0);
				}
			}
			br.close();
			req.delete();
					
			// If the first request is valid, then only proceed
			if(validReq){
				// WE WANT TO HAVE 10 TRIALS WITH DIFFERENT TEST SETS
				// Train and test sets are created in this way -> generate a set a 10 seeds first
				int seed = randomGenerator();
				System.out.println("Seed:" + seed);
				
				for(int m=0;m<attributes.length; m++){
					global_attr = attributes[m];
					System.out.println("******** Learning the attribute: " + global_attr + " ********");
					modality = chooseContextToClusterFrom(global_attr);
					System.out.println("*************** Picking clusters from modality: " + modality + " *******************");
					
					// Have a writer for results
					String[] rc_behavior_modality = new String[1];
					rc_behavior_modality[0] = modality;
					
					// Set up the result file path
					File results_filepath = new File("C:\\Users\\Priyanka\\Documents\\grounded_language_learning\\AutomatedExpNewAlgo\\GLLUserStudyResults\\User18\\" + global_attr + "\\" + modality);
					results_filepath.mkdirs();
				
					firstTime = true;
					int random = (int)Math.random();
					PrintWriter writer1 = new PrintWriter(results_filepath + "\\qs_result.txt", "UTF-8");
					
					// HACK - Compute similarity matrix and the clustering tree for each modality separately
					// Do spectral clustering and get the clusters to start the experiments
					// Compute similarity matrices for each modality
					computePairwiseSimilarityMatrices(rc_behavior_modality, seed);
					
					// Get clusters starting from depth = 2
					ArrayList<ObjectClusterer> result = getClustersAtDepth(modality, 2);
					
					Collections.shuffle(result);
					
					// Initialize the label table
					labelTable = new HashMap<Pair, ArrayList<Triple> >();
					
					// Get clusterNumber
					ClusterDB DB = behavior_modality_clusters.get(modality);
					
					//firstTime = true;
					// Do the following for each cluster starting at depth = 2
					for(int i=0; i<result.size(); i++){
						//System.out.println("Sending clusters to display: " + result.get(i).getIDs());
						
						// The following is used to select clusters with 5 or less objects only
						selectClustersToDisplay(modality, DB, result.get(i), writer1);
					}
					
					while(DB.checkForOutliers()){
						// Have a hashmap to store the outlier objects and which clusters do they belong to
						HashMap<Integer, ArrayList<String>> outliersWithClusters = new HashMap<Integer, ArrayList<String>>();
						
						// Send out all outliers to ask user for the number of categories
						int outlier_categories = createResponseFileForKMeans(DB.getOutlierObjects(), modality);
						
						System.out.println("The outlier have " + outlier_categories + " categories");
						
						// Do KNN with outliers and get labels for them as well
						performKMeansWithOutliers(outlier_categories, DB.getOutlierObjects(), outliersWithClusters);
						
						// Reset the outlier object list in DB to zero for future purposes
						DB.clearOutlierObjectsList();
						
						//System.out.println("Number of outlier clusters: " + outliersWithClusters.size());
						
						// Show the clusters of outlier objects and get labels for them
						for(int clusterNum : outliersWithClusters.keySet()){
							System.out.println("Number of outlier sets" + outliersWithClusters.keySet().size());
							// Add outlier clusters to cluster Table and get each one a cluster number
							// NOTE: OUTLIER CLUSTERS DO NOT HAVE OBJECT CLUSTERER OBJECT
							//System.out.println("ClusterNum: " + clusterNum);
							int currentCluster = DB.addClusterToClusterTable(outliersWithClusters.get(clusterNum));
							//System.out.println("Current cluster: " + currentCluster);
							//System.out.println("Current cluster: " + outliersWithClusters.get(clusterNum).toString());
							//System.out.println("Current modality: " + rc_behavior_modalities[j]);
							createResponseFileForOutliers(modality, DB, outliersWithClusters.get(clusterNum), currentCluster, writer1);
						}
					}
					// Write label table to file
					writeLabelTableToFile(labelTable, results_filepath);
					// Reset everything
					result.clear();
					writer1.close();
					
					questionCount = 0;
					questionCount2 = 0;
					DB.clearOutlierObjectsList();
					
					// Print out ground truth table
					Iterator it = groundTruthTable.entrySet().iterator();
					while (it.hasNext()) {
				        Map.Entry pair = (Map.Entry)it.next();
			        	HashMap<String, String> tuple = (HashMap<String, String>) pair.getValue();
			        	Iterator it2 = tuple.entrySet().iterator();
			        	while(it2.hasNext()){
			        		Map.Entry pair2 = (Map.Entry)it2.next();
			        		System.out.println(pair.getKey() + " ---> " + pair2.getKey() + " ---> " + pair2.getValue());
			        	}
					}
				}
				System.out.println("Done with all modalities");
				createEndFile();
				
				//Write out the ground truth table to a file
				//writeGroundTruthTable();
				
				// Find the max of all results and also get all the points and put it into a .csv file
				System.out.println("Producing points to plot");
				//createCSVFile(rc_behavior_modalities);
				System.out.println("DONE!");
				System.out.println("Changing modalities");
		}
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	// Method to write out the ground truth table to a file
	public static void writeGroundTruthTable(){
		// Filepath to store ground truth table in
		String filePath = "C:\\Users\\Priyanka\\Documents\\grounded_language_learning\\AutomatedExpNewAlgo\\UserInterfaceResults1\\groundTruthTable.csv";
		
		try{
			PrintWriter writer = new PrintWriter(filePath, "UTF-8");
			writer.println("object_names, color, shape, material, has_contents, height, weight, deformable, size");
			
			Iterator it = groundTruthTable.entrySet().iterator();
			while (it.hasNext()) {
		        Map.Entry pair = (Map.Entry)it.next();
	        	HashMap<String, String> tuple = (HashMap<String, String>) pair.getValue();
	        	Iterator it2 = tuple.entrySet().iterator();
	        	String[] values = new String[8];
	        	while(it2.hasNext()){
	        		Map.Entry pair2 = (Map.Entry)it2.next();
	        		if(pair2.getKey().equals("color"))
	        			values[0] = (String)pair2.getValue();
	        		if(pair2.getKey().equals("shape"))
	        			values[1] = (String)pair2.getValue();
	        		if(pair2.getKey().equals("material"))
	        			values[2] = (String)pair2.getValue();
	        		if(pair2.getKey().equals("has_contents"))
	        			values[3] = (String)pair2.getValue();
	        		if(pair2.getKey().equals("height"))
	        			values[4] = (String)pair2.getValue();
	        		if(pair2.getKey().equals("weight"))
	        			values[5] = (String)pair2.getValue();
	        		if(pair2.getKey().equals("deformable"))
	        			values[6] = (String)pair2.getValue();
	        		if(pair2.getKey().equals("size"))
	        			values[7] = (String)pair2.getValue();
	        	}
	        	writer.print(pair.getKey() + ",");
	        	for(int j=0;j<values.length;j++){
	        		if(j != values.length-1)
	        			writer.print(values[j] + ",");
	        		else
	        			writer.print(values[j]);
	        	}
	        	writer.println();
			}
			writer.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	// Method which gives the context-attr mapping deduced from ContextAttrMappingEXP
	public static String chooseContextToClusterFrom(String attribute){
		String rc_modality = "";
		if(attribute == "size")
			rc_modality = "grasp_size";
		if(attribute == "material")
			rc_modality = "drop_audio";
		if(attribute == "has_contents")
			rc_modality = "shake_audio";	
		if(attribute == "weight")
			rc_modality = "drop_haptics";
		if(attribute == "height")
			rc_modality = "press_haptics";
		if(attribute == "deformable")
			rc_modality = "revolve_haptics";
		if(attribute == "color")
			rc_modality = "look_color";
		if(attribute == "shape")
			rc_modality = "push_audio";
		
		return rc_modality;
	}
	
	// Do KNN with outliers and get labels for them as well
	public static void performKMeansWithOutliers(int outlier_categories, ArrayList<String> outliers, HashMap<Integer, ArrayList<String>> outliersWithClusters){
		ArrayList<String> outlier_objects = outliers;
		
		// Have a temp hashmap of outliers and clusters
		HashMap<String, Integer> temp_outlier_map = new HashMap<String, Integer>();
		
		// get instances of outlier objects
		FeatureDataLoader FDL = new FeatureDataLoader();
		
		//load pre-computed features
		FDL.loadContextsData(rc_behavior_modalities);
		
		// Create instances 
		DataLoaderCY DL = new DataLoaderCY();
		ArrayList<String> objects_list = DL.getObjectList();
		//HashMap<String, String> object_labels = new HashMap<String, String>();
		
		System.out.println("Modality at this point: " + modality);
		
		String attr = getAttributeForModality(modality);
		
		// Get the labels of all the objects to compare our classifiers against
		/*for(int l=0;l<objects_list.size();l++){
			HashMap<String, String> combination = groundTruthTable.get(objects_list.get(l)); 
			String label = combination.get(attr);
			object_labels.put(objects_list.get(l), label);	 
		}*/
		
		// Create a redundant Hashmap of object names to object names as its required by ClassLabelID
		HashMap<String, String> object_object = new HashMap<String, String>();
		for(int l=0;l<outlier_objects.size();l++){
			object_object.put(outlier_objects.get(l), outlier_objects.get(l));
		}
		
		//labeling function labels trials by labels
		IClassLabelFunction LF = new ClassLabelID(outlier_objects, object_object);
		ArrayList<String> class_values = LF.getUniqueValueSet(outlier_objects);
		String [] classVals = new String[class_values.size()];
		for (int f = 0; f < class_values.size(); f++)
			classVals[f]=class_values.get(f);
					
		//setup instances in order to setup kernel
		ContextInstancesCreator IC = new ContextInstancesCreator();
		IC.setLabelFunction(LF);
		IC.setData(FDL.getData(modality));
		IC.generateHeader(outlier_objects, objects_list);
				
		//full set of trials for training	
		// THE FOLLOWING CODE IS TO CLASSIFY EACH INSTANCE SEPARATELY
		/*ArrayList<InteractionTrial> train_trials = DL.generateTrials(outlier_objects, 6);
		
		Instances train = IC.generateFullSet(train_trials);
		System.out.println("Training instances: " + train.numInstances());*/
		
		Instances train = IC.generateAveragedFullSet(outlier_objects, 6);

		
		try{
			// Set up a remove filter to remove the nominal attributes before classifying
			Remove remove = new Remove();
			remove.setAttributeIndices(Integer.toString(train.numAttributes()-1));
			remove.setAttributeIndices(Integer.toString(train.numAttributes()));
			remove.setInvertSelection(false);
			remove.setInputFormat(train);
						
			Instances training = weka.filters.Filter.useFilter(train, remove);
			
			// Do K-means clustering
			SimpleKMeans kmeans = new SimpleKMeans();
			kmeans.setSeed(randomGenerator()); //10
			
			kmeans.setPreserveInstancesOrder(true);
			kmeans.setNumClusters(outlier_categories);
			
			kmeans.buildClusterer(training);
			
			// This array returns the cluster number (starting with 0) for each instance
			// The array has as many elements as the number of instances
			int[] assignments = kmeans.getAssignments();
			
			// Print out the clusters
		    // As we preserved the order of the instances, we can get which object they belong to
			// THE FOLLOWING CODE IS TO CLASSIFY EACH INSTANCE SEPARATELY
			/*for(int objectNum = 0; objectNum<outlier_objects.size(); objectNum++) {
				String objectName = outlier_objects.get(objectNum);
				HashMap<Integer, Integer> clusterNumCounts = new HashMap<Integer, Integer>();
				for(int clusterNum = objectNum*6; clusterNum<objectNum*6+6; clusterNum++){
					if(!clusterNumCounts.containsKey(assignments[clusterNum])){
						clusterNumCounts.put(assignments[clusterNum], 1);
					}
					else{
						int count = clusterNumCounts.get(assignments[clusterNum]);
						clusterNumCounts.remove(assignments[clusterNum]);
						clusterNumCounts.put(assignments[clusterNum], count+1);
					}
					
					System.out.printf("Instance %s -> Cluster %d \n", train.instance(clusterNum), assignments[clusterNum]);
				}
				
				// get the max cluster number for each object and that is the cluster it belongs to
				int max = Integer.MIN_VALUE;
				int clusterNum = Integer.MIN_VALUE;
				for(int freq : clusterNumCounts.keySet()){
					if(clusterNumCounts.get(freq) > max){
						max = clusterNumCounts.get(freq);
						clusterNum = freq;
					}
				}
				
			    System.out.printf("Instance %s -> Cluster %d \n", outlier_objects.get(objectNum), clusterNum);
			    temp_outlier_map.put(outlier_objects.get(objectNum), clusterNum);
			}*/
			
			System.out.println("Outlier objects size: " + outlier_objects.size());
			
			for(int objectNum = 0; objectNum<outlier_objects.size(); objectNum++) {
				String objectName = outlier_objects.get(objectNum);
				System.out.println("Outlier object in KMeans: " + objectName);
				HashMap<Integer, Integer> clusterNumCounts = new HashMap<Integer, Integer>();
				if(!clusterNumCounts.containsKey(assignments[objectNum])){
					clusterNumCounts.put(assignments[objectNum], 1);
				}
				else{
					int count = clusterNumCounts.get(assignments[objectNum]);
					clusterNumCounts.remove(assignments[objectNum]);
					clusterNumCounts.put(assignments[objectNum], count+1);
				}
					
					//System.out.printf("Instance %s -> Cluster %d \n", train.instance(objectNum), assignments[objectNum]);
				
				// get the max cluster number for each object and that is the cluster it belongs to
				int max = Integer.MIN_VALUE;
				int clusterNum = Integer.MIN_VALUE;
				for(int freq : clusterNumCounts.keySet()){
					if(clusterNumCounts.get(freq) > max){
						max = clusterNumCounts.get(freq);
						clusterNum = freq;
					}
				}
				
			    System.out.printf("Outlier: %s -> Cluster %d \n", outlier_objects.get(objectNum), clusterNum);
			    temp_outlier_map.put(outlier_objects.get(objectNum), clusterNum);
			}
			
			 // Add them to the hashmap of outliers and clusters
			for(String entry : temp_outlier_map.keySet()){
				if(!outliersWithClusters.containsKey(temp_outlier_map.get(entry))){
					ArrayList<String> objs = new ArrayList<String> ();
					objs.add(entry);
					outliersWithClusters.put(temp_outlier_map.get(entry), objs);
				}
				else{
					ArrayList<String> objs = outliersWithClusters.get(temp_outlier_map.get(entry));
					objs.add(entry);
					int clusterNum = temp_outlier_map.get(entry);
					outliersWithClusters.remove(temp_outlier_map.get(entry));
					outliersWithClusters.put(temp_outlier_map.get(entry), objs);
				}
			}
		    
		} catch(Exception e){
			e.printStackTrace();
		}
			
	}
	
	public static int createResponseFileForKMeans(ArrayList<String> outlier_objects, String modality){
		System.out.println("Creating a response file for KMeans");
		try{
			// Request text file code
			PrintWriter writer = new PrintWriter("C:\\Users\\Priyanka\\Desktop\\groundedResponse.txt", "UTF-8");
			// As its asking the categories in outliers, print 0 at the top to differentiate from other response files
			writer.println("0");
			writer.println(modality);
			writer.println("-10000");
			//System.out.println("Sending clusters to display: " + IDs.toString());
			for(int g=0; g<outlier_objects.size(); g++){
				writer.println(outlier_objects.get(g));
			}
			writer.close();
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Call sequence() as the response.txt file needs to be processed
		//sequence(DB, OBC, resultWriter);

		// Wait for the request.txt file to exist
		return checkIfRequestFileForKMeansExists();
	}
	
	public static int checkIfRequestFileForKMeansExists(){
		System.out.println("Checking if a Request File for K means");
		try{
			//System.out.println("Checking if outlier request file exists");
			// Check if request.txt file exists and sleep till it does
			File request = new File("C:\\Users\\Priyanka\\Desktop\\groundedRequest.txt");
			//File request = new File("/home/users/pkhante/Desktop/groundedRequest.txt");
			while(!request.exists()){
			  //System.out.println("Going to sleep as file does not exist!");
			  Thread.sleep(2000); 
			}
			//System.out.println("Waking up as request.txt now exists!");
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Read the contents of the request.txt file
		return readRequestFileForKMeans();
	}
	
	public static int readRequestFileForKMeans(){
		int outlier_categories = -10000;
		
		try{
			questionCount++;
			questionCount2++;
			System.out.println("Reading the request file for KMeans");
			File request = new File("C:\\Users\\Priyanka\\Desktop\\groundedRequest.txt");
			//File request = new File("/home/users/pkhante/Desktop/groundedRequest.txt");
			FileReader fileReader = new FileReader(request);
	        BufferedReader bufferedReader = new BufferedReader(fileReader);
	        String line = "";
	        ArrayList<String> params = new ArrayList<String>();
	     	
	     	// Read the contents of the request.txt file and then delete the file
	        while((line = bufferedReader.readLine()) != null) {
	        	//System.out.println(line);
	        	 params.add(line);
	        }
	        bufferedReader.close();  
	        // Delete the request.txt file
	        request.delete();
	        
	        outlier_categories = Integer.parseInt(params.get(0));
		}catch(Exception e){
			e.printStackTrace();
		}
		return outlier_categories;
	}
	
	public static void createEndFile(){
		//System.out.println("Creating an end program file");
		try{
			//PrintWriter writer = new PrintWriter("/home/users/pkhante/Desktop/groundedResponse.txt", "UTF-8");
			PrintWriter writer = new PrintWriter("C:\\Users\\Priyanka\\Desktop\\groundedResponse.txt", "UTF-8");
			writer.println("EndOfAllModalities");
			writer.close();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// root is at depth 1
	public static ArrayList<ObjectClusterer> getClustersAtDepth(String behavior_modality, int depth){
		ClusterDB CDB = behavior_modality_clusters.get(behavior_modality);
		ObjectClusterer OC = objectClusterTable.get(CDB);
		int clevel = 1;    //current level
		ArrayList<ObjectClusterer> result = new ArrayList<ObjectClusterer>(); 
		result = getClustersAtDepth(OC, clevel, depth, result);
		
		return result;
	}
	
	public static ArrayList<ObjectClusterer> getClustersAtDepth(ObjectClusterer OC, int clevel, int depth, ArrayList<ObjectClusterer> result){
		if(clevel == depth){
			result.add(OC);
		}
		else{
			ArrayList<ObjectClusterer> children = new ArrayList<ObjectClusterer>();
			children = OC.getChildren();
			if(children.size() != 0){
				for (int i = 0; i < children.size(); i ++){
					getClustersAtDepth(children.get(i), clevel+1, depth, result);
				}
			}	
		}
		return result;
	}
	
	public static void selectClustersToDisplay(String rc_behavior_modality, ClusterDB DB, ObjectClusterer result, PrintWriter writer){
		if(result.getIDs().size() <= 6){
			HashMap<Integer, ObjectClusterer> clusterNumAndOCTable = DB.getClusterNumAndOCTable();
			for (Map.Entry<Integer, ObjectClusterer> e : clusterNumAndOCTable.entrySet()) {
				ArrayList<String> tempClusterIds = e.getValue().getIDs();
					
				if(tempClusterIds.equals(result.getIDs())){
					createResponseFile(rc_behavior_modality, DB, result, (Integer)e.getKey(), writer);
					break;
				}
			}
		}
		else{
			if(result.getChildren().size() != 0){
				for(int m=0; m<result.getChildren().size(); m++){
					ObjectClusterer OCC = result.getChildren().get(m);
					if(OCC.getIDs().size() <= 6)
						selectClustersToDisplay(rc_behavior_modality, DB, OCC, writer);
					else{
						selectClustersToDisplay(rc_behavior_modality, DB, OCC, writer);
					}
				}
			}
		}
	}

	public static void computePairwiseSimilarityMatrices(String[] rc_behavior_modalities, int seed) throws Exception{
		DataLoaderCY DL = new DataLoaderCY();
		ArrayList<String> object_list = DL.getObjectList();
		
		// After you get the object
		
		// Generate the test and training object sets if its the first time
		if(firstTime == true){
			ArrayList<String> test_objects = DL.getRandomTestObjectSet(object_list, seed);
			objects = DL.getRandomTrainObjectSet(object_list, test_objects);
			//objects = object_list;
		}
		
		/*for(int i=0; i<test_objects.size(); i++){
			System.out.println("Test Objects: "+test_objects.get(i));
		}
		
		for(int i=0; i<objects.size(); i++){
			System.out.println("Train Objects: "+objects.get(i));
		}*/
		
		//load pre-computed features
		FeatureDataLoader FDL = new FeatureDataLoader();
		FDL.loadContextsDataWithLabels(rc_behavior_modalities);
		
		for (int b = 0; b<rc_behavior_modalities.length; b++){
			//System.out.print("Computing similarity for context:\t["+rc_behavior_modalities[b]+"]\n\t");
			
			String attr = getAttributeForModality(rc_behavior_modalities[b]);
			
			// Get the labels of all the objects to compare our classifiers against. This will not be used in this part but needed.
			/*for(int l=0;l<objects.size();l++){
				HashMap<String, String> combination = groundTruthTable.get(objects.get(l)); 
				String label = combination.get(attr);
				object_labels.put(objects.get(l), label);	
			}*/
			
			// Create a redundant Hashmap of object names to object names as its required by ClassLabelID
			HashMap<String, String> object_object = new HashMap<String, String>();
			for(int l=0;l<objects.size();l++){
				object_object.put(objects.get(l), objects.get(l));
			}
			
			//labeling function labels trials by labels
			IClassLabelFunction LF = new ClassLabelID(objects, object_object);
			ArrayList<String> class_values = LF.getUniqueValueSet(objects);
			String [] classVals = new String[class_values.size()];
			for (int f = 0; f < class_values.size(); f++)
				classVals[f]=class_values.get(f);
				
			//setup instances in order to setup kernel
			ContextInstancesCreator IC = new ContextInstancesCreator();
			IC.setLabelFunction(LF);
			IC.setData(FDL.getData(rc_behavior_modalities[b]));
			IC.generateHeader(objects, objects);
			
			//full set of trials for training
			ArrayList<InteractionTrial> train_trials = DL.generateTrials(objects, 6);
			Instances data_mb = IC.generateFullSet(train_trials);
			
			//System.out.println("number of attrs: " + data_mb.numAttributes());
			
			// Set up a remove filter to remove the nominal attributes before classifying
			Remove remove = new Remove();
			remove.setAttributeIndices(Integer.toString(data_mb.numAttributes()-1));
			remove.setInvertSelection(false);
			remove.setInputFormat(data_mb);
						
			Instances training = weka.filters.Filter.useFilter(data_mb, remove);
		
			//System.out.println(data_mb.instance(59));
			
			//kernel
			PolyKernelJS K = new PolyKernelJS(training,250007,1.0,false);
			
			//generic sim db
			GenericSimDB R_mb = new GenericSimDB(objects);
	
			//for each pair of objects
			for (int i = 0; i < objects.size();i++){
				Instances data_i_temp = IC.getDataForObject(objects.get(i), 6);
				// Remove the class Attribute before comparison
				Remove remove1 = new Remove();
				remove1.setAttributeIndices(Integer.toString(data_i_temp.numAttributes()));
				remove1.setInvertSelection(false);
				remove1.setInputFormat(data_i_temp);
							
				Instances data_i = weka.filters.Filter.useFilter(data_i_temp, remove1);
				
				for (int j = i; j < objects.size(); j++){
					Instances data_j_temp = IC.getDataForObject(objects.get(j), 6);
					// Remove the class Attribute before comparison
					Remove remove2 = new Remove();
					remove2.setAttributeIndices(Integer.toString(data_j_temp.numAttributes()));
					remove2.setInvertSelection(false);
					remove2.setInputFormat(data_i_temp);
								
					Instances data_j = weka.filters.Filter.useFilter(data_j_temp, remove2);
					
					double v = 0;
					int c = 0;
					
					//for each instance pair, compute similarity
					for (int a = 0; a < data_i.numInstances();a++){
						for (int z= 0; z < data_j.numInstances();z++){		
							double k_az = K.evaluate(data_i.instance(a), data_j.instance(z));
							v+=k_az;
							c++;
						}
					}
					
					v=v/(double)c;
		
					R_mb.setEntry(i, j, v);
					R_mb.setEntry(j, i, v);
				}
			}
			spectralClustering(R_mb, objects, rc_behavior_modalities[b]);
		}
	}
	
	public static void spectralClustering(GenericSimDB R_mb, ArrayList<String> objects, String rc_behavior_modality){
		try {
			R_mb.makeSymetric();
			R_mb.setDiagonal(1.0);      
			
			ObjectClusterer OC = new ObjectClusterer();
			OC.setAlpha(1.0);
			OC.setIDs(objects);
			OC.setNodeID("root");
			OC.setSDB(R_mb);
			OC.buildClustering();
			
			ClusterDB CDB = new ClusterDB(OC);
			CDB.resetClusterNumbers();
			CDB.giveIDsForClusters();
			CDB.createInvertedClusterTable();
			
			objectClusterTable.put(CDB, OC);
			
			//CDB.printClusterIDs();
			
			behavior_modality_clusters.put(rc_behavior_modality, CDB);
			
			System.out.println("\n\nObject Clustering:\n");
			OC.printClustering(2);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void createResponseFile(String rc_behavior_modality, ClusterDB DB, ObjectClusterer OBC, int clusterNum, PrintWriter resultWriter){
		System.out.println("Creating a response file");
		try{
			// Request text file code
			PrintWriter writer = new PrintWriter("C:\\Users\\Priyanka\\Desktop\\groundedResponse.txt", "UTF-8");
			writer.println(rc_behavior_modality);
			writer.println(clusterNum);
			// Get the cluster IDs
			ArrayList<String> IDs = OBC.getIDs();
			System.out.println("Sending clusters to display: " + IDs.toString());
			cur_cluster.clear();
			for(int g=0; g<IDs.size(); g++){
				cur_cluster.add(IDs.get(g));
				writer.println(IDs.get(g));
			}
			writer.close();
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Wait for the request.txt file to exist
		checkIfRequestFileExists(rc_behavior_modality, DB, OBC, clusterNum, resultWriter);
	}
	
	public static void createResponseFileForOutliers(String rc_behavior_modality, ClusterDB DB, ArrayList<String> outlier_objs, int currentCluster, PrintWriter resultWriter1){
		System.out.println("Creating a response file for outliers");
		try{
			// Request text file code
			//PrintWriter writer = new PrintWriter("/home/users/pkhante/Desktop/groundedResponse.txt", "UTF-8");
			PrintWriter writer = new PrintWriter("C:\\Users\\Priyanka\\Desktop\\groundedResponse.txt", "UTF-8");
			writer.println(rc_behavior_modality);
			writer.println(currentCluster);
			// Get the cluster IDs
			System.out.println("Sending clusters to display (outliers): " + outlier_objs.toString());
			cur_cluster.clear();
			for(int g=0; g<outlier_objs.size(); g++){
				cur_cluster.add(outlier_objs.get(g));
				writer.println(outlier_objs.get(g)); 
			}
			writer.close();
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Call sequenceForOutliers() as the response.txt file needs to be processed
		//sequenceForOutliers(DB, resultWriter);
		
		// Wait for the request.txt file to exist
		checkIfRequestFileExistsForOutliers(rc_behavior_modality, DB, currentCluster, resultWriter1);
	}
	
	public static void checkIfRequestFileExistsForOutliers(String rc_behavior_modality, ClusterDB DB, int currentCluster, PrintWriter writer){
		try{
			//System.out.println("Checking if outlier request file exists");
			// Check if request.txt file exists and sleep till it does
			File request = new File("C:\\Users\\Priyanka\\Desktop\\groundedRequest.txt");
			//File request = new File("/home/users/pkhante/Desktop/groundedRequest.txt");
			while(!request.exists()){
			  //System.out.println("Going to sleep as file does not exist!");
			  Thread.sleep(2000); 
			}
			//System.out.println("Waking up as request.txt now exists!");
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Read the contents of the request.txt file
		readRequestFileForOutliers(rc_behavior_modality, DB, currentCluster, writer);
	}
	
	public static void readRequestFileForOutliers(String rc_behavior_modality, ClusterDB DB, int currentCluster, PrintWriter writer){
		try{
			//System.out.println("Reading the outlier request file");
			File request = new File("C:\\Users\\Priyanka\\Desktop\\groundedRequest.txt");
			//File request = new File("/home/users/pkhante/Desktop/groundedRequest.txt");
			FileReader fileReader = new FileReader(request);
	        BufferedReader bufferedReader = new BufferedReader(fileReader);
	        String line = "";
	        ArrayList<String> params = new ArrayList<String>();
	     	
	     	// Read the contents of the request.txt file and then delete the file
	        while((line = bufferedReader.readLine()) != null) {
	        	//System.out.println(line);
	        	 params.add(line);
	        }
	        bufferedReader.close();  
	        // Delete the request.txt file
	        request.delete();
	        
	        String object_to_delete = "";
	     	String object_label = "";
	     	String previousClusterNum = "";
	     	String cluster_label = "";
	        
	     	// Delete object
        	if(params.get(0).equals("0")){      
        		// Last line is definitely the label
        		cluster_label = params.get(params.size()-1);
        		
        		// All lines in the middle are outliers
        		for(int i = 1; i<params.size()-1; i++){
        			object_to_delete = params.get(i);
        			cur_cluster.remove(object_to_delete);
	 				DB.setOutlierObject(object_to_delete);
	 				System.out.println("Outlier object set: " + object_to_delete);
	     			DB.deleteIDFromAllClusters(object_to_delete);
	     			DB.deleteFromClusterTable(currentCluster, object_to_delete);
	     			//System.out.println("ClusterNum: " + clusterNum);
	     			DB.clearInvertedClusterTable();
	     			DB.createInvertedClusterTable();
        		}
        		
        		questionCount++;
        		questionCount2++;
        		
        		if(!cluster_label.equals(""))
        			sequenceForOutliers(DB, writer, cluster_label);
        	}
        	
        	// Send new cluster
        	if(params.get(0).equals("1")){
        		//System.out.println("Size of params:" + params.size());
        		previousClusterNum = params.get(1);
        		cluster_label = params.get(2);
        		//System.out.println("Previous cluster label: " + cluster_label);
        		if(previousClusterNum.equals(Integer.toString(clusterNum)))
 					DB.setLabelForCluster(clusterNum, cluster_label);
        		
        		questionCount++;
        		questionCount2++;
        		
        		if(!cluster_label.equals(""))
        			sequenceForOutliers(DB, writer, cluster_label);
        	}
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void checkIfRequestFileExists(String rc_behavior_modality, ClusterDB DB, ObjectClusterer OBC, int clusterNum, PrintWriter writer){
		try{
			//System.out.println("Checking if request file exists");
			// Check if request.txt file exists and sleep till it does
			File request = new File("C:\\Users\\Priyanka\\Desktop\\groundedRequest.txt");
			//File request = new File("/home/users/pkhante/Desktop/groundedRequest.txt");
			while(!request.exists()){
			  //System.out.println("Going to sleep as file does not exist!");
			  Thread.sleep(2000); 
			}
			//System.out.println("Waking up as request.txt now exists!");
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Read the contents of the request.txt file
		readRequestFile(rc_behavior_modality, DB, OBC, clusterNum, writer);
	}
	

	/*
	 * Returns whether or not response file exists
	 */
	public static boolean responseFileExists(){
		String fullPath = reqFilePath + responseName;
		File f = new File(fullPath);
		return (f.exists() && !f.isDirectory());
	}

		
	public static void readRequestFile(String rc_behavior_modality, ClusterDB DB, ObjectClusterer OBC, int clusterNum, PrintWriter writer){
		try{
			System.out.println("Reading the request file");
			File request = new File("C:\\Users\\Priyanka\\Desktop\\groundedRequest.txt");
			//File request = new File("/home/users/pkhante/Desktop/groundedRequest.txt");
			FileReader fileReader = new FileReader(request);
	        BufferedReader bufferedReader = new BufferedReader(fileReader);
	        String line = "";
	        ArrayList<String> params = new ArrayList<String>();
	     	
	     	// Read the contents of the request.txt file and then delete the file
	        while((line = bufferedReader.readLine()) != null) {
	        	//System.out.println(line);
	        	 params.add(line);
	        }
	        bufferedReader.close();  
	        // Delete the request.txt file
	        request.delete();
	        
	        String object_to_delete = "";
	     	String object_label = "";
	     	String previousClusterNum = "";
	     	String cluster_label = "";
	     	
	     	System.out.println("Current cluster in readRequest: " + cur_cluster);
	        
	     	// Delete object
        	if(params.get(0).equals("0")){      
        		// Last line is definitely the label
        		cluster_label = params.get(params.size()-1);
        		System.out.println("Cluster label sent to sequence: " + cluster_label);
        		
        		// All lines in the middle are outliers
        		for(int i = 1; i<params.size()-1; i++){
        			object_to_delete = params.get(i);
        			System.out.println("Outlier object set: " + object_to_delete);
        			cur_cluster.remove(object_to_delete);
	 				DB.setOutlierObject(object_to_delete);
	     			DB.deleteIDFromAllClusters(object_to_delete);
	     			DB.deleteFromClusterTable(clusterNum, object_to_delete);
	     			//System.out.println("ClusterNum: " + clusterNum);
	     			DB.clearInvertedClusterTable();
	     			DB.createInvertedClusterTable();
        		}
        		
        		questionCount++;
        		questionCount2++;
        		
        		if(!cluster_label.equals(""))
        			sequence(DB, OBC, writer, cluster_label);
        	}
        	
        	// Send new cluster
        	if(params.get(0).equals("1")){
        		//System.out.println("Size of params:" + params.size());
        		previousClusterNum = params.get(1);
        		cluster_label = params.get(2);
        		System.out.println("Cluster label sent to sequence: " + cluster_label);
        		//System.out.println("Previous cluster label: " + cluster_label);
        		if(previousClusterNum.equals(Integer.toString(clusterNum)))
 					DB.setLabelForCluster(clusterNum, cluster_label);
        		
        		questionCount++;
        		questionCount2++;
        		
        		if(!cluster_label.equals(""))
        			sequence(DB, OBC, writer, cluster_label);
        	}
        	
        	// Recluster
        	if(params.get(0).equals("2")){
        		questionCount++;
        		HashMap<Integer, ObjectClusterer> clusterNumAndOCTable = DB.getClusterNumAndOCTable();
     			//ObjectClusterer OBC = clusterNumAndOCTable.get(clusterNum);
     			//ArrayList<String> tempClusterIds;
     			if(OBC.getChildren().size() != 0){
         			for(int m=0; m<OBC.getChildren().size(); m++){
         				ObjectClusterer OCC = OBC.getChildren().get(m);
         				for (Map.Entry<Integer, ObjectClusterer> e : clusterNumAndOCTable.entrySet()) {
         					ArrayList<String> tempClusterIds = e.getValue().getIDs();
         					
         					if(tempClusterIds.equals(OCC.getIDs())){
         						// Send the modified clusters to be displayed
         						createResponseFile(rc_behavior_modality, DB, OCC, (Integer)e.getKey(), writer);
         						break;
         					}
         				}
         			}
     			}
     			else{
     				System.out.println("You have reached the leaves! No children remaining to recluster!");
     			}
        	}
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * Breaks input string delimited by ' ' (spaces) and stuffs a vector
	 */
	public static ArrayList<String> splitString(String input){
		ArrayList<String> vec = new ArrayList<String>();
	    StringTokenizer st = new StringTokenizer(input);
	    while(st.hasMoreTokens())
	    	vec.add(st.nextToken());
		return vec;
	}
	
	/*
	 * Question answering methods. The type of question asked determines the search method.
	 * Generally the questions will be assumed to be exact, ie super hardcoded so any changes to questions must also be reflected
	 * here.
	 */
	public static String ask_free_resp(String question, String modality){
		//System.out.println("%%%%%% IN ASK_FREE_RESP %%%%%%%");
		//Increment the questions asked by 1 every time this method is called
		System.out.println("Modality in ask_free_resp: " + modality);
		if(question.equals("For the forthcoming clusters, what feature/attribute of the object ")){
			if(modality.equals("drop_audio"))
				return "material";
			if(modality.equals("shake_audio"))
				return "has_contents";
			if(modality.equals("drop_haptics"))
				return "weight";
			if(modality.equals("press_haptics"))
				return "height";
			if(modality.equals("revolve_haptics"))
				return "deformable";
			if(modality.equals("grasp_size"))
				return "size";
			if(modality.equals("look_color"))
				return "color";
			if(modality.equals("push_audio"))
				return "shape";
		}
		return null;
	}
	
	// The following method is only called for outlier clusters
	public static void sequenceForOutliers(ClusterDB DB, PrintWriter writer, String cluster_label){
		System.out.println("In sequence for outliers");
		
		try{
			boolean extant = false;
			//store answer as label
			ArrayList<Triple> values = labelTable.get(globalContextPair); //overwriting here?
			//System.out.println("Values " + values.size());
			for(int j = 0; j < values.size() && extant == false; j++){
				if(values.get(j).getLabel().equalsIgnoreCase(cluster_label)){ //label exists in table. Add it to list of objects
					Triple tempo = values.remove(j);					//must remove element, to update it, then add it back
					ArrayList<String> objects = tempo.getObjects();
					//System.out.println("Objects before updating a label: " + objects.size());
					// Add current cluster minus the outlier objects
					for(int i=0; i < cur_cluster.size(); i++)
						objects.add(cur_cluster.get(i));
					Triple update = new Triple(tempo.getLabel(),objects,tempo.getQuestionNum()+questions_per_label);
					values.add(update);								//finally add back the updated triple
					//System.out.println("Objects after updating a label: " + objects.size());
					extant = true;
				}
				
				// Add to the ground truth table
				for(int t=0;t<cur_cluster.size();t++){
					//Find the Object ID in the ground truth table
					Iterator it = groundTruthTable.entrySet().iterator();
					while (it.hasNext()) {
				        Map.Entry pair = (Map.Entry)it.next();
				        if(pair.getKey().equals(cur_cluster.get(t))){
				        	HashMap<String, String> existing_pairs = (HashMap<String, String>) pair.getValue();
				        	existing_pairs.put(global_attr, cluster_label);
				        	groundTruthTable.put((String)pair.getKey(), existing_pairs);
				        }
					}
				}
			}
			//labelTable.put(globalContextPair, values);
			
			if(extant == false){											//label doesn't exist in table. Create it
				ArrayList<String> temp_cur_cluster = (ArrayList<String>)cur_cluster.clone();
				Triple labelTriple = new Triple(cluster_label, temp_cur_cluster, questions_per_label);
				//add to list or put in table??
				values.add(labelTriple);
				labelTable.put(globalContextPair, values);
				//System.out.println("Values after adding a new label: " + values.size());
				extant = true;
				
				// Add to the ground truth table
				for(int t=0;t<cur_cluster.size();t++){
					//Find the Object ID in the ground truth table
					Iterator it = groundTruthTable.entrySet().iterator();
					while (it.hasNext()) {
				        Map.Entry pair = (Map.Entry)it.next();
				        if(pair.getKey().equals(cur_cluster.get(t))){
				        	HashMap<String, String> existing_pairs = (HashMap<String, String>) pair.getValue();
				        	existing_pairs.put(global_attr, cluster_label);
				        	groundTruthTable.put((String)pair.getKey(), existing_pairs);
				        }
					}
				}
			}
			
			// Print out the label table after adding or updating anything
			System.out.println("Label table after an update: ");
			for(int k=0; k<values.size();k++){
				System.out.println("\t " + values.get(k).getLabel() + " -----> " + values.get(k).getObjects());
			}
						
			//System.out.println("Cur_Cluster when all labels are same: " + cur_cluster.toString() + "with label: " + att_from_above);
			//System.out.println("Label table size: " + labelTable.get(globalContextPair).size());
			
			// Build classifier with labels obtained and test on test objects 
			buildClassifierAndTest(globalContextPair, modality, writer);
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	public static void sequence(ClusterDB DB, ObjectClusterer OBC, PrintWriter writer, String cluster_label){
		System.out.println("In sequence");
		ArrayList<Triple> value = null;
		
		try{
			if(firstTime){
				System.out.println("In sequence, first time!");
				firstTime = false;
				String modality_copy = modality;
				String resp = ask_free_resp("For the forthcoming clusters, what feature/attribute of the object ", modality);
				ArrayList<String> feature_vec = splitString(resp);
				global_attr = feature_vec.get(0);

				/*
				 *got modality and attribute. make a pair and begin gathering labels.
				 */
				globalContextPair = new Pair(modality,feature_vec.get(0));
				System.out.println("Current context pair: " + modality + " ---> "+  feature_vec.get(0));
				value = new ArrayList<Triple>();
				labelTable.put(globalContextPair, value);
				System.out.println("Size of label table at the beginning: " + labelTable.size());
			}
			
			boolean extant = false;
			//store answer as label
			ArrayList<Triple> values = labelTable.get(globalContextPair); //overwriting here?
			System.out.println("Values " + values.size());
			// Print out the label table after adding or updating anything
			System.out.println("Label table BEFORE: ");
			for(int k=0; k<values.size();k++){
				System.out.println("\t " + values.get(k).getLabel() + " -----> " + values.get(k).getObjects());
			}
			for(int j = 0; j < values.size() && extant == false; j++){
				if(values.get(j).getLabel().equalsIgnoreCase(cluster_label)){ //label exists in table. Add it to list of objects
					System.out.println("Matched: " + values.get(j).getLabel() + " with " + cluster_label);
					Triple tempo = values.remove(j);					//must remove element, to update it, then add it back
					ArrayList<String> labelled_objects = tempo.getObjects();
					System.out.println("Objects before updating label: " + labelled_objects.size() + " and they are " + labelled_objects);
					System.out.println("Current cluster: " + cur_cluster);
					// Add current cluster minus the outlier objects
					for(int i=0; i < cur_cluster.size(); i++)
						labelled_objects.add(cur_cluster.get(i));
					Triple update = new Triple(tempo.getLabel(),labelled_objects,tempo.getQuestionNum()+questions_per_label);
					values.add(update);								//finally add back the updated triple
					System.out.println("Objects after updating a label: " + labelled_objects.size());
					extant = true;
					
					// Add to the ground truth table
					for(int t=0;t<cur_cluster.size();t++){
						//Find the Object ID in the ground truth table
						Iterator it = groundTruthTable.entrySet().iterator();
						while (it.hasNext()) {
					        Map.Entry pair = (Map.Entry)it.next();
					        if(pair.getKey().equals(cur_cluster.get(t))){
					        	HashMap<String, String> existing_pairs = (HashMap<String, String>) pair.getValue();
					        	existing_pairs.put(global_attr, cluster_label);
					        	groundTruthTable.put((String)pair.getKey(), existing_pairs);
					        }
						}
					}
				}
			}
			
			//labelTable.put(globalContextPair, values);
			
			if(extant == false){											//label doesn't exist in table. Create it
				ArrayList<String> temp_cur_cluster = (ArrayList<String>)cur_cluster.clone();
				Triple labelTriple = new Triple(cluster_label, temp_cur_cluster, questions_per_label);
				System.out.println("Current cluster before adding to label table: " + cur_cluster); 
				//add to list or put in table??
				values.add(labelTriple);
				labelTable.put(globalContextPair, values);
				System.out.println("Values after adding a new label: " + values.size() + " after adding the label " + cluster_label);
				extant = true;
				
				// Add to the ground truth table
				for(int t=0;t<cur_cluster.size();t++){
					//Find the Object ID in the ground truth table
					Iterator it = groundTruthTable.entrySet().iterator();
					while (it.hasNext()) {
				        Map.Entry pair = (Map.Entry)it.next();
				        if(pair.getKey().equals(cur_cluster.get(t))){
				        	HashMap<String, String> existing_pairs = (HashMap<String, String>) pair.getValue();
				        	existing_pairs.put(global_attr, cluster_label);
				        	groundTruthTable.put((String)pair.getKey(), existing_pairs);
				        }
					}
				}
			}
			
			// Print out the label table after adding or updating anything
			System.out.println("Label table after an update: ");
			for(int k=0; k<values.size();k++){
				System.out.println("\t " + values.get(k).getLabel() + " -----> " + values.get(k).getObjects());
			}
			
			
			// Build classifier with labels obtained and test on test objects 
			buildClassifierAndTest(globalContextPair, modality, writer);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	// Method to build classifiers with the clusters labeled so far and test them on test objects
	// Obtain the labels so far from the label_table
	// This method adds labels to the instances -> create files with data with labels
	// And then build classifiers
	// Create decision trees and test on test object and output accuracy to a file. Don't create a new file unless its a different modality.
	public static void buildClassifierAndTest(Pair curContextPair, String modality, PrintWriter writer){
		System.out.println("In build classifier and test");
		
		ArrayList<Triple> labelTriple = labelTable.get(curContextPair);
		// new Triple(att_from_above, cur_cluster, questions_per_label);
		try{
			// To have a mapping between each object and its label
			//HashMap<String, String> object_labels = new HashMap<String, String>();
			//HashMap<String, String> training_object_labels = new HashMap<String, String>();
			ArrayList<String> training_objs = new ArrayList<String>();
			//DataLoaderCY DL = new DataLoaderCY();
			
			//FeatureDataLoader FDL = new FeatureDataLoader();
			
			//load pre-computed features
			//FDL.loadContextsData(rc_behavior_modalities);
			
			for(int i = 0; i<labelTriple.size(); i++){
				// Get the objects in the cluster and its label
				//ArrayList<String> cluster_objs = new ArrayList<String> ();
				//String cluster_label = labelTriple.get(i).getLabel();
				for(int j=0; j<labelTriple.get(i).getObjects().size(); j++){
					//cluster_objs.add(labelTriple.get(i).getObjects().get(j));
					training_objs.add(labelTriple.get(i).getObjects().get(j));
				}
				
				// Get the labels of all the training objects to compare our classifiers against
				/*for(int l=0;l<cluster_objs.size();l++){
					//HashMap<String, String> combination = groundTruthTable.get(cluster_objs.get(l)); 
					training_object_labels.put(cluster_objs.get(l), cluster_label);	
				}*/
			}

			// Create instances 
			/*ArrayList<String> objects_list = DL.getObjectList();
			
			String attr = getAttributeForModality(modality);
			
			// Get the labels of all the objects to compare our classifiers against
			// Add the training objects
			for(int l=0;l<training_objs.size();l++){
				HashMap<String, String> combination = groundTruthTable.get(training_objs.get(l)); 
				String label = combination.get(attr);
				object_labels.put(training_objs.get(l), label);	
			}
			
			//labeling function labels trials by labels
			IClassLabelFunction LF = new ClassLabelID(training_objs, training_object_labels);
			ArrayList<String> allClassValues = LF.getValueSet(training_objs);      // This one has each of the class values including duplicates
			ArrayList<String> class_values = LF.getUniqueValueSet(training_objs);   // This one does not have any duplicates
			//System.out.println("Class values in build classifier: " + class_values.toString());
			String [] classVals = new String[class_values.size()];
			for (int f = 0; f < class_values.size(); f++)
				classVals[f]=class_values.get(f);
						
			//setup instances in order to setup kernel
			ContextInstancesCreator IC = new ContextInstancesCreator();
			IC.setLabelFunction(LF);
			IC.setData(FDL.getData(modality));
			IC.generateHeader(training_objs, objects_list);
					
			//full set of trials for training			
			//System.out.println("Training objects size: " + training_objs.size());
			ArrayList<InteractionTrial> train_trials = DL.generateTrials(training_objs, 6);
			
			Instances train = IC.generateFullSet(train_trials);

			// Set up a remove filter to remove the nominal attributes before classifying
			Remove remove = new Remove();
			remove.setAttributeIndices(Integer.toString(train.numAttributes()-1));
			remove.setInvertSelection(false);
			remove.setInputFormat(train);
						
			Instances training = weka.filters.Filter.useFilter(train, remove);
				
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
			Instances test = IC.generateFullSet(test_trials);*/
			
			// Print out the output to console
			//System.out.println("Training objects: " + training_objs.toString());
			//System.out.println("Test objects: " + test_objects.toString());
			//System.out.println("\t\tNEW ITERATION: Train on "+train.numInstances()+" and test on "+ test.numInstances());
			
			// Write output to file
			writer.println("\t\tNEW ITERATION: Train on "+training_objs.size() + " objects");
			writer.println("Objects: " + training_objs.toString());
			//writer.println("Test objects: " + test_objects.toString());
			writer.println("Question Count (Not including Skip questions): " + questionCount2);
			writer.println("Question Count: " + questionCount);
						
			//EV.evaluateModel(C_boost, test);
			
			// Write out the results to the file
			//System.out.println(EV.toSummaryString());
			//System.out.println(EV.toClassDetailsString());
			//System.out.println("Kappa Statistics: " + EV.kappa());
			//writer.println(EV.toSummaryString());			
			//writer.println(EV.toClassDetailsString());
			//writer.println("Kappa Statistics: " + EV.kappa());
			writer.println();
				
		}catch(Exception e){
			e.printStackTrace();
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
	
	public static void writeLabelTableToFile(HashMap<Pair, ArrayList<Triple> > labelTable, File filePath){
		System.out.println(""); 
		try{
			 PrintWriter writer = new PrintWriter(filePath.getPath() + "\\LabelTable.csv", "UTF-8");
		     writer.println("Context,GeneralAttribute,Label,Objects");
			 for (Entry<Pair, ArrayList<Triple>> entry : labelTable.entrySet()){
		         String context = entry.getKey().getContext();
		         String attr = entry.getKey().getAttribute();   
		         System.out.println("Entry.getValue().size(): "+entry.getValue().size()); 
		         for(int i=0;i<entry.getValue().size();i++){
		        	 StringWriter output = new StringWriter();
		        	 output.append(context);
		        	 output.append("," + attr);
		        	 String label = entry.getValue().get(i).getLabel();
		        	 System.out.println("Label: " + label);
		        	 output.append("," + label + ",\"");
		        	 ArrayList<String> objects = entry.getValue().get(i).getObjects(); 
		        	 for(int j=0;j<objects.size();j++){
		        		 System.out.println("Object: " + objects.get(j));
		        		 if(j == objects.size() - 1)
		        			 output.append(objects.get(j) + "\"");
		        		 else
		        			 output.append(objects.get(j) + ",");
		        	 }
		        	 //String questionCount = Integer.toString(entry.getValue().get(i).getQuestionNum());
		        	 //System.out.println("QuestionCount: " + questionCount + "\n");
		        	 //output.append(questionCount);
		        	 writer.println(output);
		          }
		     }
			 writer.close();
		}
		catch(IOException e){
			 e.printStackTrace();
		}
	}
	
	public static void writeQuestionCountTableToFile(){
		System.out.println("QuestionCountTable Size: " + questionCountPerContext.size());
		 try {
			 PrintWriter writer = new PrintWriter("C:\\Users\\Priyanka\\Documents\\grounded_language_learning\\AutoQuestionAnswer\\src\\etc\\QuestionCountTable.csv", "UTF-8");
		     writer.println("Context,TotalQuestionCount");
		     for (Entry<String, Integer> entry : questionCountPerContext.entrySet()){
		    	 StringWriter output = new StringWriter();
		         String context = entry.getKey();
		         int questionCount = entry.getValue();
		         output.append(context);
		         output.append("," + Integer.toString(questionCount));
		         writer.println(output);
		     }
		     writer.close();
		 }
		 catch(IOException e){
			 e.printStackTrace();
		 } 
	}
	
	// Method to generate the seed for trials
	public static int randomGenerator() {
	    Random generator = new Random();
	    int num = generator.nextInt();
	    
	    return num;
	}
	
	// Method to find the maximum of the correct instances for each file
	// The output is written to a file in each folder
	public static void findMaxInstances(String [] rc_behavior_modalities) throws Exception{
		// File path to store the results in 
		// USUAL PATH
		//String results_path = "/home/priyanka/Documents/grounded_language_learning/AutomatedExpNewAlgo/results_exp2/";
		
		// PATH FOR EXTRA QUESTION COUNT
		String results_path = "C:\\Users\\Priyanka\\Documents\\grounded_language_learning\\AutomatedExpNewAlgo\\results_exp3QC\\";
		
		for(int g=0;g<rc_behavior_modalities.length;g++){
			File folder = new File(results_path + rc_behavior_modalities[g]);
			File[] listOfFiles = folder.listFiles();
			for(int i=0;i<listOfFiles.length;i++){
				String fileName = listOfFiles[i].getName();
				if(!(listOfFiles[i].isDirectory())){
					if(!fileName.equals("MaxNumOfInstances.txt")){
						String maxResult = readResultFiles(results_path + rc_behavior_modalities[g] + "\\" + fileName);
						File maxResultFilePath = new File(results_path + rc_behavior_modalities[g] + "\\MaxResults");
						maxResultFilePath.mkdirs();
						writeMaxResultFile(results_path + rc_behavior_modalities[g] + "\\MaxResults\\Max" + fileName, maxResult);
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
	
	// Method to find the maximum of the correct instances for each modality
	// The output is written to a file in each folder
	public static void findUltimateMax(String [] rc_behavior_modalities) throws Exception{
		// File path to store the results in 
		// USUAL PATH
		//String results_path = "/home/priyanka/Documents/grounded_language_learning/AutomatedExpNewAlgo/results_exp2/";
		
		// PATH FOR EXTRA QUESTION COUNT
		String results_path = "C:\\Users\\Priyanka\\Documents\\grounded_language_learning\\AutomatedExpNewAlgo\\results_exp3QC\\";
				
		for(int g=0;g<rc_behavior_modalities.length;g++){
			File folder = new File(results_path + rc_behavior_modalities[g] + "\\MaxResults");
			File[] listOfFiles = folder.listFiles();
			String maxResult = "";
			String minResult = "";
			String maxMinResult = "";
			for(int i=0;i<listOfFiles.length;i++){
				String fileName = listOfFiles[i].getName();
				maxResult = readMaxResultFiles(results_path + rc_behavior_modalities[g] + "\\MaxResults\\" + fileName, maxResult);
				minResult = readMinResultFiles(results_path + rc_behavior_modalities[g] + "\\MaxResults\\" + fileName, minResult);
				writeMaxResultFile(results_path + rc_behavior_modalities[g] + "\\MaxNumOfInstances.txt", maxResult);
				writeMaxResultFile(results_path + rc_behavior_modalities[g] + "\\MinNumOfInstances.txt", minResult);
			}
		}
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
	public static void createCSVFile(String [] rc_behavior_modalities) throws Exception{
		// File path to store the results in 
		// USUAL PATH
		//String results_path = "/home/priyanka/Documents/grounded_language_learning/AutomatedExpNewAlgo/results_exp2/";
		
		// PATH FOR EXTRA QUESTION COUNT - COMMENT OUT FOR NORMAL ALGO
		String results_path = "C:\\Users\\Priyanka\\Documents\\grounded_language_learning\\AutomatedExpNewAlgo\\results_exp3QC\\";
						
		for(int g=0;g<rc_behavior_modalities.length;g++){
			String writeFilePath = results_path + rc_behavior_modalities[g] + "\\PointsToPlot.csv";
			File folder = new File(results_path + rc_behavior_modalities[g]);
			File[] listOfFiles = folder.listFiles();
			
			try{
				PrintWriter writer = new PrintWriter(writeFilePath, "UTF-8");
				for(int i=0;i<listOfFiles.length;i++){
					String fileName = listOfFiles[i].getName();
				
					if(!(listOfFiles[i].isDirectory())){
						if(!(fileName.equals("MaxNumOfInstances.txt")) && !(fileName.equals("MinNumOfInstances.txt")) && !(fileName.equals("PointsToPlot.csv"))){
							FileReader fileReader = new FileReader(results_path + rc_behavior_modalities[g] + "\\" + fileName);
						    BufferedReader bufferedReader = new BufferedReader(fileReader);
		
						    String line = "";
						   
						    // Write out QuestionCount, Accuracy, Kappa and Number of Training Objects
							while((line = bufferedReader.readLine()) != null) {
								if (line.contains("Question Count (Not")){
									String[] tokens = line.split(":");
									int questionCount2 = Integer.parseInt(tokens[1].trim());
									writer.print(questionCount2 + ",");
							    }
								
								if (line.contains("Question Count:") ){
									String[] tokens = line.split(":");
									int questionCount = Integer.parseInt(tokens[1].trim());
									writer.print(questionCount + ",");
							    }
								
								if (line.contains("NEW ITERATION")){
									String[] tokens = line.split(" ");
									int numOfInstTrainedOn = Integer.parseInt(tokens[4]);
									writer.print(numOfInstTrainedOn/6 + ",");
							    }
							
								if(line.contains("Correctly Classified Instances")){
								    String[] tokens = line.split("              ");
								    float correctAccuracy = Float.parseFloat(tokens[1].split("%")[0].trim());  
								    writer.print(correctAccuracy + ",");
								}
								
								if (line.contains("Kappa Statistics")){
									String[] tokens = line.split(":");
									double kappa = Double.parseDouble(tokens[1].trim());
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
