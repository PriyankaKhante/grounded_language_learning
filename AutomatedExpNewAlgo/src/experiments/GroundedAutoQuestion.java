package experiments;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystems;
import java.io.IOException;
import GroundedLanguage.*;
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
 * A provided grounded truth table will allow for lookups for a corresponding question
 *
 * output is a table:
 | context | attribute | Label | object_id | Questions |
 _______________________________________________________
 where 'object_id' holds a list of object ids that are 'label' in 'attribute'
 Questions holds the number of questions required to obtain this pairing
 */

public class GroundedAutoQuestion {
	public static final String filePath = "/home/users/pkhante/Pictures/grounded_learning_images/";
	public static final String reqFilePath	= "/home/priyanka/Desktop/";
	public static final String responseName	= "groundedResponse.txt";
	public static final String requestName	= "groundedRequest.txt";
	
	//behaviour-modailities to use
	static String [] rc_behavior_modalities = {"drop_audio"};
		//{"drop_audio", "revolve_audio","push_audio", "hold_haptics","lift_haptics",
			//			"press_haptics","squeeze_haptics","grasp_size", "shake_audio", "look_color","look_shape"};  
					
	// Modalities that have been taken out
	// grasp-audio, hold-audio, lift-audio, poke-audio, press-audio, squeeze-audio, drop-haptics,
	// poke-haptics, revolve-haptics, push-haptics, shake-haptics, grasp-haptics
			
	//Collections.shuffle(Arrays.asList(rc_behavior_modalities));

	static String modality;
	static String old_mod = "";
	static ArrayList<String> cur_cluster;
	static String clusterAttribute;
	static int clusterNum; //is this needed?'
	
	static Instances data;
	static HashMap<String, HashMap<String, String>> groundTruthTable = new HashMap<String, HashMap<String, String>>();
	static HashMap<String, Integer> questionCountPerContext = new HashMap<String, Integer>();
	static HashMap<Pair, ArrayList<Triple> > labelTable = new HashMap<Pair, ArrayList<Triple> >();
	static int questionCount = 0;
    static int questions_per_label = 0;
    
    static HashMap<String, ClusterDB> behavior_modality_clusters = new HashMap<String, ClusterDB>();
	static HashMap<ClusterDB, ObjectClusterer> objectClusterTable = new HashMap<ClusterDB, ObjectClusterer>();
    
    // To store the test and training objects for each modality
    static ArrayList<String> test_objects, objects;
	static boolean firstTime;
	static String global_attr = "";
	static Pair globalContextPair = null;
	
	// Constructor
	public GroundedAutoQuestion(){
		cur_cluster = new ArrayList<String>();
	}

	public static void main(String[] args) {
		// Load the ground truth table as its needed to get the labels
		loadGroundTruthTable();
		
		try{
			// Create a first request file
			writeRequestFile(1,",");
			
			// Check if the first request is for a new cluster
			File req = new File("/home/priyanka/Desktop/groundedRequest.txt");
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
				// Do spectral clustering and get the clusters to start the experiments
				// Compute similarity matrices for each modality
				for (int j=0;j<rc_behavior_modalities.length; j++){
					// HACK - Compute similarity matrix and the clustering tree for each modality separately
					// Have a writer for results
					File results_filepath = new File("/home/priyanka/Documents/grounded_language_learning/AutomatedExpNewAlgo/results_exp2/" + rc_behavior_modalities[j]);
					results_filepath.mkdirs();
					PrintWriter writer = new PrintWriter(results_filepath + "/" + new java.util.Date() + ".txt", "UTF-8");
					firstTime = true;
					String[] rc_behavior_modality = new String[1];
					rc_behavior_modality[0] = rc_behavior_modalities[j];
					System.out.println("Computing for modality: " + rc_behavior_modality[0]);
					// After the clusters are computed, create the labelled data for all modalities
					loadLabelledData(rc_behavior_modalities);
					
					computePairwiseSimilarityMatrices(rc_behavior_modality);
					
					// Get clusters starting from depth = 2
					ArrayList<ObjectClusterer> result = getClustersAtDepth(rc_behavior_modalities[j], 2);
					
					// ************ TODO: LATER ON: HAVE TO DO MULTIPLE ROUNDS WITH SHUFFLING THE CLUSTERS ***********
					Collections.shuffle(result);
					// Get clusterNumber
					ClusterDB DB = behavior_modality_clusters.get(rc_behavior_modalities[j]);
					//System.out.println("Result size: "+result.size());
					// Do the following for each cluster starting at depth = 2
					//for(int i=0; i<result.size(); i++){
					for(int i=0; i<result.size(); i++){
						//System.out.println("Sending clusters to display: " + result.get(i).getIDs());
						
						// The following is used to select clusters with 5 or less objects only
						selectClustersToDisplay(rc_behavior_modalities[j], DB, result.get(i), writer);
					}
					
					while(DB.checkForOutliers()){
						// Have a hashmap to store the outlier objects and which clusters do they belong to
						HashMap<Integer, ArrayList<String>> outliersWithClusters = new HashMap<Integer, ArrayList<String>>();
						
						// As all the clusters are shown, now take care of the outliers
						int outlier_categories = deduceCategoriesOfOutliers(DB.getOutlierObjects(), rc_behavior_modalities[j]);
						
						// Do KNN with outliers and get labels for them as well
						performKMeansWithOutliers(outlier_categories, DB.getOutlierObjects(), outliersWithClusters);
						
						// Reset the outlier objects in DB to null for future purposes
						DB.clearOutlierObjectsList();
						
						System.out.println("Number of outlier clusters: " + outliersWithClusters.size());
						
						// Show the clusters of outlier objects and get labels for them
						for(int clusterNum : outliersWithClusters.keySet()){
							// Add outlier clusters to cluster Table and get each one a cluster number
							// NOTE: OUTLIER CLUSTERS DO NOT HAVE OBJECT CLUSTERER OBJECT
							int currentCluster = DB.addClusterToClusterTable(outliersWithClusters.get(clusterNum));
							createResponseFileForOutliers(rc_behavior_modality[j], DB, outliersWithClusters.get(clusterNum), currentCluster, writer);
						}
					}
					//System.out.println("Changing modalities");
					result.clear();
					writer.close();
					//DB.mergeOutlierObjectsWithClusters();
					//DB.mergeClustersWithSameLabels();
					//DB.printClustersWithObjectIDsAndLabels(rc_behavior_modalities[j]);
				}
				//System.out.println("Done with all modalities");

				createEndFile();
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}
	
	// Do KNN with outliers and get labels for them as well
	public static void performKMeansWithOutliers(int outlier_categories, ArrayList<String> outliers, HashMap<Integer, ArrayList<String>> outliersWithClusters){
		ArrayList<String> outlier_objects = outliers;
		
		// Have a temp hashmap of outliers and clusters
		HashMap<String, Integer> temp_outlier_map = new HashMap<String, Integer>();
		
		// get instances of outlier objects
		FeatureDataLoader FDL = new FeatureDataLoader();
		
		//load pre-computed features
		FDL.loadContextsDataWithLabels(rc_behavior_modalities);
		
		// Create instances 
		DataLoaderCY DL = new DataLoaderCY();
		ArrayList<String> objects_list = DL.getObjectList();
		HashMap<String, String> object_labels = new HashMap<String, String>();
		
		String attr = getAttributeForModality(modality);
		
		// Get the labels of all the objects to compare our classifiers against
		for(int l=0;l<objects_list.size();l++){
			HashMap<String, String> combination = groundTruthTable.get(objects_list.get(l)); 
			String label = combination.get(attr);
			object_labels.put(objects_list.get(l), label);	 
		}
		
		//labeling function labels trials by labels
		IClassLabelFunction LF = new ClassLabelID(objects_list, object_labels);
		ArrayList<String> class_values = LF.getValueSet();
		//System.out.println("Class values in build classifier: " + class_values.toString());
		String [] classVals = new String[class_values.size()];
		for (int f = 0; f < class_values.size(); f++)
			classVals[f]=class_values.get(f);
					
		//setup instances in order to setup kernel
		ContextInstancesCreator IC = new ContextInstancesCreator();
		IC.setLabelFunction(LF);
		IC.setData(FDL.getData(modality));
		IC.generateHeader();
				
		//full set of trials for training	
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
			kmeans.setSeed(10);
			
			kmeans.setPreserveInstancesOrder(true);
			kmeans.setNumClusters(outlier_categories);
			
			kmeans.buildClusterer(training);
			
			// This array returns the cluster number (starting with 0) for each instance
			// The array has as many elements as the number of instances
			int[] assignments = kmeans.getAssignments();
			
			// Print out the clusters
		    // As we preserved the order of the instances, we can get which object they belong to
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
			
			for(int objectNum = 0; objectNum<outlier_objects.size(); objectNum++) {
				String objectName = outlier_objects.get(objectNum);
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
	
	// Check the labels of outliers from groundtruth and return the number of categories within them
	public static int deduceCategoriesOfOutliers(ArrayList<String> outlier_objects, String modality){
		String attr = getAttributeForModality(modality);
		ArrayList<String> outlier_labels = new ArrayList<String> ();
		for(int i=0;i<outlier_objects.size();i++){
			HashMap<String, String> combination = groundTruthTable.get(outlier_objects.get(i)); 
			String label = combination.get(attr);
			if(!outlier_labels.contains(label)){
				outlier_labels.add(label);
			}
		}
		
		return outlier_labels.size();
	}
	
	public static void loadLabelledData(String[] rc_behavior_modalities){
		DataLoaderCY DL = new DataLoaderCY();
		ArrayList<String> objects_list = DL.getObjectList();
		
		FeatureDataLoader FDL = new FeatureDataLoader();
		
		// Create the files which have the object data along with the labels for all modalities
		for(int g=0;g<rc_behavior_modalities.length;g++)
			FDL.createContextDataWithLabels(groundTruthTable, getAttributeForModality(rc_behavior_modalities[g]) , rc_behavior_modalities[g], objects_list);
	}
	
	public static void createEndFile(){
		System.out.println("Creating an end program file");
		try{
			//PrintWriter writer = new PrintWriter("/home/users/pkhante/Desktop/groundedResponse.txt", "UTF-8");
			PrintWriter writer = new PrintWriter("/home/priyanka/Desktop/groundedResponse.txt", "UTF-8");
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
		
		/*for(int i=0;i<result.size(); i++){
			System.out.println("Size: "+ result.get(i).getIDs().size() + "ClusterIDs: " + result.get(i).getIDs());
		}*/
		
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
					//System.out.println("New cluster: " + (Integer)e.getKey());
					// Send the modified clusters to be displayed
					createResponseFile(rc_behavior_modality, DB, result, (Integer)e.getKey(), writer);
					break;
				}
			}
			// Get the clusterNumber
			//ArrayList<String> clusterids = result.getIDs();
			//String listString = "";
			//for(String clusterIDs : clusterids){
				//listString += clusterIDs + ",";
			//}
			//int clusterNum = DB.getClusterNumber(listString);
			
			// Send a command to create response.txt
			//createResponseFile(rc_behavior_modality, DB, result, clusterNum);
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

	/*
	 * Outputs a convoluted table in hashmaps after the input of the grounded truth.
	 * Maps: obj_name -> attribute_name (eg color) -> value
	 * The following attributes have numerical output: height weight and width
	 */
	static void loadGroundTruthTable(){
		try{
			BufferedReader in = new BufferedReader(new FileReader("/home/priyanka/Documents/grounded_language_learning/AutomatedExpNewAlgo/src/etc/attributesGroundTruthTable.csv"));
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
		//return groundTruthTable;
	}
	
	public static void computePairwiseSimilarityMatrices(String[] rc_behavior_modalities) throws Exception{
		DataLoaderCY DL = new DataLoaderCY();
		ArrayList<String> object_list = DL.getObjectList();
		
		// Generate the test and training object sets if its the first time
		if(firstTime == true){
			test_objects = DL.getRandomTestObjectSet(object_list, 45);
			objects = DL.getRandomTrainObjectSet(object_list, test_objects);
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
		
		//output path
		//String output_path = new String("/home/users/pkhante/extracted_feature_vectors/output_files");
		//File output_files = new File(output_path);
		//deleteDirectory(output_files);
		
		for (int b = 0; b<rc_behavior_modalities.length; b++){
			//System.out.print("Computing similarity for context:\t["+rc_behavior_modalities[b]+"]\n\t");
			
			String attr = getAttributeForModality(rc_behavior_modalities[b]);
			HashMap<String, String> object_labels = new HashMap<String, String>();
			
			// Get the labels of all the objects to compare our classifiers against. This will not be used in this part but needed.
			for(int l=0;l<objects.size();l++){
				HashMap<String, String> combination = groundTruthTable.get(objects.get(l)); 
				String label = combination.get(attr);
				object_labels.put(objects.get(l), label);	
			}
			
			//labeling function labels trials by labels
			IClassLabelFunction LF = new ClassLabelID(objects, object_labels);
			ArrayList<String> class_values = LF.getValueSet();
			String [] classVals = new String[class_values.size()];
			for (int f = 0; f < class_values.size(); f++)
				classVals[f]=class_values.get(f);
				
			//setup instances in order to setup kernel
			ContextInstancesCreator IC = new ContextInstancesCreator();
			IC.setLabelFunction(LF);
			IC.setData(FDL.getData(rc_behavior_modalities[b]));
			IC.generateHeader();
			
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
							
							//System.out.println("Object i: "+data_i.instance(a).toString());
							//System.out.println("Object j: "+data_j.instance(z).toString());
							//System.out.println("k_az:" + k_az);
							
							v+=k_az;
							c++;
						}
					}
					
					v=v/(double)c;
		
					R_mb.setEntry(i, j, v);
					R_mb.setEntry(j, i, v);
					
					//System.out.print(".");
				}
			}
			//System.out.println();
			
			//String filename = new String(output_path+"/"+rc_behavior_modalities[b]+"_sim.txt");
			//String filename_notags = new String(output_path+"/"+rc_behavior_modalities[b]+"_sim_notags.txt");
			//R_mb.writeToFile(filename);
			//R_mb.writeToFileNoTags(filename_notags);
			
			spectralClustering(R_mb, objects, rc_behavior_modalities[b]);
		}
	}
	
	public static void spectralClustering(GenericSimDB R_mb, ArrayList<String> objects, String rc_behavior_modality){
		try {
			R_mb.makeSymetric();
			R_mb.setDiagonal(1.0);      
			//System.out.println("\nSim Object Matrix as input to spectral clustering:");
			//R_mb.printDebug();
			
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
			
			//System.out.println("\n\nObject Clustering:\n");
			//OC.printClustering(2);
			
			//deleteIDFromAllClusters("tin_can", OC);
			//System.out.println("\n\nObject Clustering2:\n");
			//OC.printClustering(10);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void createResponseFile(String rc_behavior_modality, ClusterDB DB, ObjectClusterer OBC, int clusterNum, PrintWriter resultWriter){
		System.out.println("Creating a response file");
		try{
			// Request text file code
			//PrintWriter writer = new PrintWriter("/home/users/pkhante/Desktop/groundedResponse.txt", "UTF-8");
			PrintWriter writer = new PrintWriter("/home/priyanka/Desktop/groundedResponse.txt", "UTF-8");
			writer.println(rc_behavior_modality);
			writer.println(clusterNum);
			// Get the cluster IDs
			ArrayList<String> IDs = OBC.getIDs();
			System.out.println("Sending clusters to display: " + IDs.toString());
			for(int g=0; g<IDs.size(); g++){
				writer.println(IDs.get(g));
			}
			writer.close();
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Call sequence() as the response.txt file needs to be processed
		// TODO: NEED TO ADD A LOT OF PROCESSING FROM SEQUENCE
		// TODO: Get labels if perfect clusters. Build classifiers, etc.
		sequence(DB, OBC, resultWriter);

		// Wait for the request.txt file to exist
		checkIfRequestFileExists(rc_behavior_modality, DB, OBC, clusterNum, resultWriter);
	}
	
	public static void createResponseFileForOutliers(String rc_behavior_modality, ClusterDB DB, ArrayList<String> outlier_objs, int currentCluster, PrintWriter resultWriter){
		System.out.println("Creating a response file for outliers");
		try{
			// Request text file code
			//PrintWriter writer = new PrintWriter("/home/users/pkhante/Desktop/groundedResponse.txt", "UTF-8");
			PrintWriter writer = new PrintWriter("/home/priyanka/Desktop/groundedResponse.txt", "UTF-8");
			writer.println(rc_behavior_modality);
			writer.println(currentCluster);
			// Get the cluster IDs
			System.out.println("Sending clusters to display: " + outlier_objs.toString());
			for(int g=0; g<outlier_objs.size(); g++){
				writer.println(outlier_objs.get(g)); 
			}
			writer.close();
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Call sequenceForOutliers() as the response.txt file needs to be processed
		sequenceForOutliers(DB, resultWriter);
		
		// Wait for the request.txt file to exist
		checkIfRequestFileExistsForOutliers(rc_behavior_modality, DB, currentCluster);
	}
	
	public static void checkIfRequestFileExistsForOutliers(String rc_behavior_modality, ClusterDB DB, int currentCluster){
		try{
			System.out.println("Checking if outlier request file exists");
			// Check if request.txt file exists and sleep till it does
			File request = new File("/home/priyanka/Desktop/groundedRequest.txt");
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
		readRequestFileForOutliers(rc_behavior_modality, DB, currentCluster);
	}
	
	public static void readRequestFileForOutliers(String rc_behavior_modality, ClusterDB DB, int currentCluster){
		try{
			System.out.println("Reading the outlier request file");
			File request = new File("/home/priyanka/Desktop/groundedRequest.txt");
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
	 				DB.setOutlierObject(object_to_delete);
	 				System.out.println("Outlier object set: " + object_to_delete);
	     			//DB.deleteIDFromAllClusters(object_to_delete);
	     			DB.deleteFromClusterTable(currentCluster, object_to_delete);
	     			//System.out.println("ClusterNum: " + clusterNum);
	     			DB.clearInvertedClusterTable();
	     			DB.createInvertedClusterTable();
        		}
				// Get clusterNumber
				/*ArrayList<String> clusterids1 = OBC.getIDs();
				String listString1 = "";
				for(String clusterIDs : clusterids1){
					listString1 += clusterIDs + ",";
				}
				int clusterNum1 = DB.getClusterNumber(listString1);*/
			    //System.out.println("ClusterNum1: " + clusterNum1);
				// Ask for a new cluster to be displayed
				//createResponseFile(rc_behavior_modality, DB, OBC, clusterNum1);
				writeRequestFile(1, cluster_label);
        	}
        	
        	// Send new cluster
        	if(params.get(0).equals("1")){
        		//System.out.println("Size of params:" + params.size());
        		previousClusterNum = params.get(1);
        		cluster_label = params.get(2);
        		System.out.println("Previous cluster label: " + cluster_label);
        		if(previousClusterNum.equals(Integer.toString(clusterNum)))
 					DB.setLabelForCluster(clusterNum, cluster_label);
        		
        		// ADDING NEW STUFF FOR DYNAMIC SPECTRAL CLUSTERING
        		//deleteLabelledDataFromTree(OBC, rc_behavior_modality);
        	}
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void checkIfRequestFileExists(String rc_behavior_modality, ClusterDB DB, ObjectClusterer OBC, int clusterNum, PrintWriter writer){
		try{
			System.out.println("Checking if request file exists");
			// Check if request.txt file exists and sleep till it does
			File request = new File("/home/priyanka/Desktop/groundedRequest.txt");
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

	/*
	 * Writes request file
	 * ID: 0 = remove -> name of object, its label
	 		1 = get next cluster -> current cluster number, its label
	 		2 = recluster -> no other arguments

	 	Returns success

	 	Input is ID and a vector, which can be null if the ID specifies an action not related to object IDS
	 */
	public static boolean writeRequestFile(int ID, ArrayList<String> outlier_objs, String label){
		try{
			PrintWriter writer = new PrintWriter((reqFilePath + requestName), "UTF-8");
			if(writer != null){
				writer.println(ID);
				if(ID == 0){
					System.out.println("Requesting object removal");
					for(int i=0;i<outlier_objs.size(); i++){
						writer.println(outlier_objs.get(i));
						System.out.println("Object to be deleted: " + outlier_objs.get(i));
					}
					writer.println(label);
					System.out.println("Label of cluster: " + label);
				}
				else if(ID == 1){
					System.out.println("Requesting next cluster");
					writer.println(clusterNum);
					writer.println(label);
				}
				else
					System.out.println("Requesting to recluster");
				writer.close();
			}
		}
		catch(IOException e){
			  e.printStackTrace();
		}

		return true;
	}
	/*
	 * ONLY used for ID 1
	 */
	public static boolean writeRequestFile(int ID, String label){
		try{
			PrintWriter writer = new PrintWriter((reqFilePath + requestName), "UTF-8");

			if(writer != null){
				writer.println(ID);
				if(ID == 1){
					System.out.println("Requesting next cluster");
					writer.println(clusterNum);
					writer.println(label);
				}
				else
					System.out.println("Requesting a recluster");
				writer.close();
			}
			else {
				System.out.println("Unable to create file");
				return false;
			}
		}
		catch(IOException e){
			  e.printStackTrace();
		}
		return true;
	}
	
	public static void readRequestFile(String rc_behavior_modality, ClusterDB DB, ObjectClusterer OBC, int clusterNum, PrintWriter writer){
		try{
			System.out.println("Reading the request file");
			File request = new File("/home/priyanka/Desktop/groundedRequest.txt");
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
	 				DB.setOutlierObject(object_to_delete);
	 				System.out.println("Outlier object set: " + object_to_delete);
	     			//DB.deleteIDFromAllClusters(object_to_delete);
	     			DB.deleteFromClusterTable(clusterNum, object_to_delete);
	     			//System.out.println("ClusterNum: " + clusterNum);
	     			DB.clearInvertedClusterTable();
	     			DB.createInvertedClusterTable();
        		}
				// Get clusterNumber
				/*ArrayList<String> clusterids1 = OBC.getIDs();
				String listString1 = "";
				for(String clusterIDs : clusterids1){
					listString1 += clusterIDs + ",";
				}
				int clusterNum1 = DB.getClusterNumber(listString1);
			    //System.out.println("ClusterNum1: " + clusterNum1);*/
				// Ask for a new cluster to be displayed
				//createResponseFile(rc_behavior_modality, DB, OBC, clusterNum1);
				writeRequestFile(1, cluster_label);
        	}
        	
        	// Send new cluster
        	if(params.get(0).equals("1")){
        		//System.out.println("Size of params:" + params.size());
        		previousClusterNum = params.get(1);
        		cluster_label = params.get(2);
        		System.out.println("Previous cluster label: " + cluster_label);
        		if(previousClusterNum.equals(Integer.toString(clusterNum)))
 					DB.setLabelForCluster(clusterNum, cluster_label);
        		
        		// ADDING NEW STUFF FOR DYNAMIC SPECTRAL CLUSTERING
        		//deleteLabelledDataFromTree(OBC, rc_behavior_modality);
        	}
        	
        	// Recluster
        	if(params.get(0).equals("2")){
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
	
	// For Dynamic Spectral Clustering
	public static void deleteLabelledDataFromTree(ObjectClusterer OBC, String rc_behavior_modality){
		// Get the items in the cluster
		ArrayList<String> clusterids1 = OBC.getIDs();
		for(int i=0;i<clusterids1.size();i++){
			for(int j=0;j<objects.size();j++){
				if(objects.get(j).equals(clusterids1.get(i)))
					objects.remove(j);
			}
		}
			
		// Compute the spectral clustering again after clusters have been deleted
		String[] rc_behavior_modalities = new String[1];
		rc_behavior_modalities[0] = rc_behavior_modality;
			
		try{
			computePairwiseSimilarityMatrices(rc_behavior_modalities);
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * Reads the response file from external program
	 * IDs : N/A
	 * Expected format: 
	 	behavior_modality
	 	cluster_num
	 	object names
	 */

	public static boolean readResponseFile(){
		System.out.println("Reading the response file");
		String line;
		int lineNum = 0;
		int ID;
		ArrayList<String> objects = new ArrayList<String>();
		String fullPath = (reqFilePath + responseName);
		try{
			BufferedReader myfile = new BufferedReader(new FileReader("/home/priyanka/Desktop/groundedResponse.txt"));
			//BufferedReader myfile = new BufferedReader(new FileReader("/home/users/pkhante/Desktop/groundedResponse.txt"));
			if(myfile != null){
				while((line = myfile.readLine()) != null){
					if(!(line.equals("EndOfAllModalities"))){
						if(lineNum == 0){
							modality = line;
							//System.out.println("Modality: " + modality);
							int first = modality.compareTo(old_mod);
							//System.out.println("Old_mod: " + old_mod);
							//System.out.println("Value of first: " + first);
							if(first != 0){
								firstTime = true;
								//Store the previous questionCount and reset it for the next context
								if(questionCount != 0)
									questionCountPerContext.put(old_mod, questionCount);
								
								questionCount = 0;
								old_mod = modality;
							}
							else
								firstTime = false;
						}
						else if(lineNum == 1)
							clusterNum = Integer.parseInt(line);
						else{
							objects.add(line);
						}
						lineNum++;
					}
					else{
						// As all modalities are now done, exit after writing the labelTables to .csv files
						System.out.println("The program has ended");
						writeLabelTableToFile(labelTable);
						questionCountPerContext.put(modality, questionCount);
						writeQuestionCountTableToFile();	
						System.exit(0);
					}
				}
				myfile.close();
			}
			else{
				System.out.println("Unable to open response file. Looking for " + fullPath);
				return false;
			}
			cur_cluster = objects;

			try {
		        Path path = FileSystems.getDefault().getPath(reqFilePath, responseName);
			    Files.delete(path);
				System.out.println("Response file parsed and deleted successfully.");
			} catch (IOException x) {
			    System.err.format("Error deleting response file. %s: no such" + " file or directory%n", fullPath);
			}
		}
		catch(IOException e){
			  e.printStackTrace();
		}
		return true;
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
		System.out.println("%%%%%% IN ASK_FREE_RESP %%%%%%%");
		//Increment the questions asked by 1 every time this method is called
		questionCount++;
		//questions_per_label++;
		if(question.equals("For the forthcoming clusters, what feature/attribute of the object ")){
			System.out.println("Modality here" + modality);
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
		}
		/*
		* Here its being asked what label is held by this attribute (modality) by the group of objects
		* or asking what label is held by the single outlier. In both cases the first object in the group
		* has the desired label
		*/
		
		if(question.equals("What/how ") || question.equals("What is the ") || question.equals("What ")){
			HashMap<String, String> objectLabels = groundTruthTable.get(cur_cluster.get(0));
			System.out.println("**********Label***************: " + objectLabels.get(modality));
			return objectLabels.get(modality);
		}
		/*
		 * Here we give the outlier names. presupposes that the outliers list has been set by a previous question
		 */
		/*if(question.equals("Please specify the names of the outlier(s), separated by spaces")){
			String result = "";
			for(int i = 0; i < outliers.size(); i++){
				result += outliers.get(i);
				if( i < outliers.size() - 1)
					result += " ";
			}
			System.out.println("**********OUTLIERS***************: " +outliers.toString());
			return result;
		}*/
		return null;
	}
	/*
	 * Returns one of the options given. 
	 * op1 is always 'no' or '>2'.
	 * op2 is always 'yes' or '1 or 2'
	 */
	public static boolean ask_mult_choice(String question, String modality, String op1, String op2){
		System.out.println("%%%%%% IN MULT CHOICE %%%%%%%");
		//Increment the questions asked by 1 every time this method is called
		questionCount++;
		//questions_per_label++;
		/*if(question.equals("Are all of these objects similar in ")){
			//System.out.println("ask_multi_choice ----- ALL");
			//search through gt for all objects in cluster. if any labels differ, return 'no' ie op1 ie true
			String label = "";

			for(int i =0; i < cur_cluster.size(); i++){
				HashMap<String,String> objectEntry = groundTruthTable.get(cur_cluster.get(i));
				if(i == 0)
					label = objectEntry.get(modality);
				
				else
					if(!label.equals(objectEntry.get(modality)))
						return true;						
			}
			//System.out.println("All of them have the same label!");
		}*/
		if(question.equals("Are most of these objects similar in ")){
			//System.out.println("ask_multi_choice ----- MOST");
			//search through gtt. if more than half of the labels differ in 'modality' return 'yes' ie op2 ie false
			String label;
			HashMap<String, Integer> frequencyList = new HashMap<String,Integer>();
			for(int i =0; i < cur_cluster.size(); i++){
				HashMap<String,String> objectEntry = groundTruthTable.get(cur_cluster.get(i));
				label = objectEntry.get(modality);
				if(!frequencyList.containsKey(label))
					frequencyList.put(label, 1);
				else{
					int lastFreq = frequencyList.get(label);
					frequencyList.put(label, lastFreq+1);
				}
			}
			int max = Integer.MIN_VALUE;
			for(String freqLabel : frequencyList.keySet()){
				if(frequencyList.get(freqLabel) > max){
					max = frequencyList.get(freqLabel);
				}
			}
			if(max >= cur_cluster.size())
				return false;
			return true;
		}
		if(question.equals("How many objects don't fit the ")){
			//System.out.println("Asking for the number of outliers");
			//search through gtt. if more 2 objects don't fit, return 'no' ie op1 ie true
			//AND set up the outlier list to include these objects
			String label;
			HashMap<String, ArrayList<String> > frequencyList = new HashMap<String,ArrayList<String> >();
			for(int i = 0; i < cur_cluster.size(); i++){
				HashMap<String,String> objectEntry = groundTruthTable.get(cur_cluster.get(i));
				label = objectEntry.get(modality);

				//System.out.println("&&&&&Current cluster: " + cur_cluster.get(i));
				//System.out.println("label&&&&: "+label);

				if(!frequencyList.containsKey(label)){
					ArrayList<String> newObj = new ArrayList<String>();
					newObj.add(cur_cluster.get(i));
					frequencyList.put(label, newObj);
				}
				
				else{
					ArrayList<String> oldObjs = frequencyList.get(label);
					oldObjs.add(cur_cluster.get(i));
					frequencyList.put(label, oldObjs);
				}
			}
			
			/*for (Map.Entry<String, ArrayList<String> > name:frequencyList.entrySet()){
	            String key = name.getKey();
	            int value = name.getValue().size();  
	            System.out.println(key + " : " + value);  
			} */
			
			//Return true if more than 2 outliers --> If you have more than 3 labels, then have to recluster
			if(frequencyList.size() > 3)
				return true;
			else{      // Has only 2 outliers or less
				//find maximum labels, objects
				int max = Integer.MIN_VALUE;
				String max_label = ""; 					//tracks the location of the 'bulk' label
				
				// Get the maximum number of objects for that max_label
				for(Map.Entry<String, ArrayList<String> > freqLabel : frequencyList.entrySet()){
					if(freqLabel.getValue().size() > max){
						max = freqLabel.getValue().size();  
						max_label = freqLabel.getKey();
					}	
				}
				
				//System.out.println("Max_label: "+max_label);
				//finally, answer the question
				/*outliers.clear();
		
				for(Map.Entry<String, ArrayList<String> > freqLabel : frequencyList.entrySet()){
					if(!freqLabel.getKey().equals(max_label)){
						for(int k = 0; k < freqLabel.getValue().size(); k++)
							outliers.add(freqLabel.getValue().get(k));
					}
				}
				System.out.println("******Added outliers********: " + outliers.size());*/
			}
		}
		return false;
	}
	
	// A method which checks the cluster and returns if its perfect or imperfect (with outliers) 
	public static String checkForCommonLabel(String attr) {
		// Return answer - "All same", "Outliers", "Not same (Recluster)"
		
		HashMap<String, String> cluster_labels = new HashMap<String, String> ();
		HashMap<String, Integer> label_count = new HashMap<String, Integer> ();
		
		//System.out.println("Cur_cluster: (In check for common label)" + cur_cluster.size());
		for(int i = 0; i < cur_cluster.size(); i++){
			HashMap<String,String> objectEntry = groundTruthTable.get(cur_cluster.get(i));
			cluster_labels.put(cur_cluster.get(i), objectEntry.get(attr));
		}
		
		// Count the number of objects for each label
		for (Map.Entry<String, String> entry : cluster_labels.entrySet()) {
			if(!label_count.containsKey(entry.getValue()))
				label_count.put(entry.getValue(), 1);
			else{
				int count = label_count.get(entry.getValue());
				label_count.put(entry.getValue(), count + 1);
			}
		}
		
		//System.out.println("Label count: " + label_count.size());
		
		// If there are 3 labels means that there can be 2 or more outliers
		if(label_count.size() <= 3 && label_count.size() != 1){
			int outlier_counter = 0;
			int special_counter = 0;
			int double_outlier_counter = 0;
			ArrayList<String> outlier_labels = new ArrayList<String>();
			ArrayList<String> outlier_objs = new ArrayList<String>();
			
			// Has to have 2 outliers only or else discard
			for (Map.Entry<String, Integer> entry : label_count.entrySet())
			{
			    if(entry.getValue() == 1)
			    	outlier_counter++;
			}
			
			// if there are not one outliers of each label, there might be two outliers with the same label
			if(outlier_counter == 0 && label_count.size() == 2){
				for (Map.Entry<String, Integer> entry : label_count.entrySet())
				{
				    if(entry.getValue() == 2){
				    	double_outlier_counter++;
				    	break;
				    }
				}
			}
			
			if(label_count.size() == 2 && outlier_counter == 2)
				special_counter = 1;
			
			if(label_count.size() == 3 && outlier_counter == 3)
				special_counter = 2;
			
			if(outlier_counter == 2 && label_count.size() == 3 || special_counter == 2){
				String label = "";
				int num = 0;
				for (Map.Entry<String, Integer> entry : label_count.entrySet())
				{
					if(num != 2){
					    if(entry.getValue() == 1){
					    	outlier_labels.add(entry.getKey());
					    	num++;
					    }
					}
				}
				
				for (Map.Entry<String, String> entry : cluster_labels.entrySet())
				{
				    if(outlier_labels.contains(entry.getValue())){
				    	outlier_objs.add(entry.getKey());
				    }
				    else
				    	label = entry.getValue();
				}
				
				return "Outliers/" + label + "/" + outlier_objs.get(0) + "/" + outlier_objs.get(1);
			}
		
			// Has to have one outlier only or else discard
			if(outlier_counter == 1 && label_count.size() == 2 || special_counter == 1){
				String label = "";
				int num = 0;
				for (Map.Entry<String, Integer> entry : label_count.entrySet())
				{
					if(num != 1){
					    if(entry.getValue() == 1){
					    	outlier_labels.add(entry.getKey());
					    	num++;
					    }
					}
				}
				
				for (Map.Entry<String, String> entry : cluster_labels.entrySet())
				{
				    if(outlier_labels.contains(entry.getValue())){
				    	outlier_objs.add(entry.getKey());
				    }
				    // Hack - To get the label for the cluster as we know there is only one outlier
				    else
				    	label = entry.getValue();
				}
				
				return "Outliers/" + label + "/" + outlier_objs.get(0);
			}
			
			// Has two outliers with the same label
			if(label_count.size() == 2 && double_outlier_counter == 1){
				String label = "";
				int num = 0;
				for (Map.Entry<String, Integer> entry : label_count.entrySet())
				{
					if(num != 1){
					    if(entry.getValue() == 2){
					    	outlier_labels.add(entry.getKey());
					    	num++;
					    }
					}
				}
				
				for (Map.Entry<String, String> entry : cluster_labels.entrySet())
				{
				    if(outlier_labels.contains(entry.getValue())){
				    	outlier_objs.add(entry.getKey());
				    }
				    // Hack - To get the label for the cluster as we know there is only one outlier
				    else
				    	label = entry.getValue();
				}
				
				return "Outliers/" + label + "/" + outlier_objs.get(0) + "/" + outlier_objs.get(1);
			}
		}
		else if(label_count.size() == 1){
			// Get the max in the label count table and get its label
			Map.Entry<String, Integer> maxEntry = null;

			for (Map.Entry<String, Integer> entry : label_count.entrySet())
			{
			    if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
			    {
			        maxEntry = entry;
			    }
			}
			return "All same/" + maxEntry.getKey();
		}

		return "Recluster";
	}
	
	// The following method is only called for outlier clusters
	public static void sequenceForOutliers(ClusterDB DB, PrintWriter writer){
		readResponseFile();
		boolean req_sent = false;
		
		try{
			String att_from_above = "";
			
			String common_lab = checkForCommonLabel(global_attr);
			System.out.println("Common label : " + common_lab);
			if(common_lab.contains("All same")){ //user input 'Yes' to "Any attributes common to all objects?"
					//Split the string to get the label for the cluster
					att_from_above = common_lab.split("/")[1].trim();
					
					//if context and general attribute are not exclusive, should break it apart like below
					//otherwise there can only be one attribute that matches, which has to exist because its whats being analyzed in the current
					//cycle.
				    ArrayList<Triple> temp = labelTable.get(globalContextPair);
					
					//issue here. labelTriple is fine but labelTable expects a mapping to an arraylist of entries.
					//should probably just add labelTriple to an arraylist and add mapping once questions are over for this modality
					Triple labelTriple = new Triple(att_from_above, cur_cluster, questions_per_label);
					temp.add(labelTriple);
					labelTable.put(globalContextPair,temp);
					System.out.println("Cur_Cluster when all labels are same: " + cur_cluster.toString() + "with label: " + att_from_above);
			
					// Build classifier with labels obtained and test on test objects 
					if(labelTable.get(globalContextPair).size() > 1)
						buildClassifierAndTest(globalContextPair, modality, writer);
			}
			else if(common_lab.contains("Outliers")){
				ArrayList<String> outlier_objs = new ArrayList<String>();
				// Write the outlier objects to the request file and remove them from the cluster and use the rest as a perfect cluster with a label
				//Split the string to get the label for the cluster
				String[] tokens = common_lab.split("/");
				att_from_above = tokens[1].trim();
				
				// Remove outliers from the current cluster
				// Only one outlier
				if(tokens.length == 3){
					outlier_objs.add(tokens[2].trim());
					DB.deleteIDFromAllClusters(tokens[2].trim());
				}
				
				// Two outliers
				if(tokens.length == 4){
					DB.deleteIDFromAllClusters(tokens[2].trim());
					outlier_objs.add(tokens[2].trim());
					DB.deleteIDFromAllClusters(tokens[3].trim());
					outlier_objs.add(tokens[3].trim());
				}
				
				// Remove the outlier objects from the current cluster
				for(int i = 0; i < outlier_objs.size(); i++){
					cur_cluster.remove(outlier_objs.get(i));
				}
				
				System.out.println("Current cluster after removing outlier: " + cur_cluster.toString());
				
				boolean extant = false;
				//store answer as label
				ArrayList<Triple> values = labelTable.get(globalContextPair); //overwriting here?
				System.out.println("Values " + values.size());
				for(int j = 0; j < values.size() && extant == false; j++){
					if(values.get(j).getLabel().equals(att_from_above)){ //label exists in table. Add it to list of objects
						Triple tempo = values.remove(j);					//must remove element, to update it, then add it back
						ArrayList<String> objects = tempo.getObjects();
						System.out.println("Objects before updating a label: " + objects.size());
						// Add current cluster minus the outlier objects
						for(int i=0; i < cur_cluster.size(); i++)
							objects.add(cur_cluster.get(i));
						Triple update = new Triple(tempo.getLabel(),objects,tempo.getQuestionNum()+questions_per_label);
						values.add(update);								//finally add back the updated triple
						System.out.println("Objects after updating a label: " + objects.size());
						extant = true;
					}
				}
				labelTable.put(globalContextPair, values);
				
				if(extant == false){											//label doesn't exist in table. Create it
					Triple labelTriple = new Triple(att_from_above, cur_cluster, questions_per_label);
					//add to list or put in table??
					values.add(labelTriple);
					labelTable.put(globalContextPair, values);
					System.out.println("Values after adding a new label: " + values.size());
					extant = true;
				}
				
				System.out.println("Cur_Cluster when all labels are same: " + cur_cluster.toString() + "with label: " + att_from_above);
				System.out.println("Label table size: " + labelTable.get(globalContextPair).size());
				// Build classifier with labels obtained and test on test objects 
				if(labelTable.get(globalContextPair).size() > 1)
					buildClassifierAndTest(globalContextPair, modality, writer);

				writeRequestFile(0,outlier_objs,att_from_above);			//send request for deleting outliers
				req_sent = true;
				
				// Wait for the request.txt file to exist
				//checkIfRequestFileExists(modality, DB, OBC, clusterNum);
				
				//get next cluster
				if(!req_sent){
					writeRequestFile(1,att_from_above);
					req_sent = false;
				}
			}
			//get next cluster
			if(!req_sent){
				writeRequestFile(1,att_from_above);
				req_sent = false;
			}
		} catch(Exception e){
			e.printStackTrace();
		}
			
	}

	public static void sequence(ClusterDB DB, ObjectClusterer OBC, PrintWriter writer){
		// Following commented out - Because the following case cannot happen
		// as sequence() is called by createResponseFile()
		/*while(!responseFileExists()){		//wait for a response with updated cluster
			try {
				    Thread.sleep(100);
				} catch(InterruptedException ex) {
				    Thread.currentThread().interrupt();
				}
		}*/
		readResponseFile();
		String att_from_above = "";
		boolean req_sent = false;
		ArrayList<Triple> value = null;
		
		try{
			if(firstTime){
				//questions_per_label = 0;
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

				clusterAttribute = feature_vec.get(0);
				//System.out.println("*********CLUSTER ATTRIBUTE********** : " + clusterAttribute);
			}
			//No longer ask this question! Just check if the cluster has less than two outliers and then label it.
			//boolean common_att = ask_mult_choice("Are all of these objects similar in ", clusterAttribute, "No", "Yes");
			String common_lab = checkForCommonLabel(global_attr);
			System.out.println("Common label : " + common_lab);
			if(common_lab.contains("All same")){ //user input 'Yes' to "Any attributes common to all objects?"
					//Split the string to get the label for the cluster
					att_from_above = common_lab.split("/")[1].trim();
					
					//if context and general attribute are not exclusive, should break it apart like below
					//otherwise there can only be one attribute that matches, which has to exist because its whats being analyzed in the current
					//cycle.
				    ArrayList<Triple> temp = labelTable.get(globalContextPair);
					
					//issue here. labelTriple is fine but labelTable expects a mapping to an arraylist of entries.
					//should probably just add labelTriple to an arraylist and add mapping once questions are over for this modality
					Triple labelTriple = new Triple(att_from_above, cur_cluster, questions_per_label);
					temp.add(labelTriple);
					labelTable.put(globalContextPair,temp);
					System.out.println("Cur_Cluster when all labels are same: " + cur_cluster.toString() + "with label: " + att_from_above);
			
					// Build classifier with labels obtained and test on test objects 
					if(labelTable.get(globalContextPair).size() > 1)
						buildClassifierAndTest(globalContextPair, modality, writer);
			}
			else if(common_lab.contains("Outliers")){
				ArrayList<String> outlier_objs = new ArrayList<String>();
				// Write the outlier objects to the request file and remove them from the cluster and use the rest as a perfect cluster with a label
				//Split the string to get the label for the cluster
				String[] tokens = common_lab.split("/");
				att_from_above = tokens[1].trim();
				
				// Remove outliers from the current cluster
				// Only one outlier
				if(tokens.length == 3){
					outlier_objs.add(tokens[2].trim());
					DB.deleteIDFromAllClusters(tokens[2].trim());
				}
				
				// Two outliers
				if(tokens.length == 4){
					DB.deleteIDFromAllClusters(tokens[2].trim());
					outlier_objs.add(tokens[2].trim());
					DB.deleteIDFromAllClusters(tokens[3].trim());
					outlier_objs.add(tokens[3].trim());
				}
				
				// Remove the outlier objects from the current cluster
				for(int i = 0; i < outlier_objs.size(); i++){
					cur_cluster.remove(outlier_objs.get(i));
				}
				
				System.out.println("Current cluster after removing outlier: " + cur_cluster.toString());
				
				boolean extant = false;
				//store answer as label
				ArrayList<Triple> values = labelTable.get(globalContextPair); //overwriting here?
				System.out.println("Values " + values.size());
				for(int j = 0; j < values.size() && extant == false; j++){
					if(values.get(j).getLabel().equals(att_from_above)){ //label exists in table. Add it to list of objects
						Triple tempo = values.remove(j);					//must remove element, to update it, then add it back
						ArrayList<String> objects = tempo.getObjects();
						System.out.println("Objects before updating a label: " + objects.size());
						// Add current cluster minus the outlier objects
						for(int i=0; i < cur_cluster.size(); i++)
							objects.add(cur_cluster.get(i));
						Triple update = new Triple(tempo.getLabel(),objects,tempo.getQuestionNum()+questions_per_label);
						values.add(update);								//finally add back the updated triple
						System.out.println("Objects after updating a label: " + objects.size());
						extant = true;
					}
				}
				labelTable.put(globalContextPair, values);
				
				if(extant == false){											//label doesn't exist in table. Create it
					Triple labelTriple = new Triple(att_from_above, cur_cluster, questions_per_label);
					//add to list or put in table??
					values.add(labelTriple);
					labelTable.put(globalContextPair, values);
					System.out.println("Values after adding a new label: " + values.size());
					extant = true;
				}
				
				System.out.println("Cur_Cluster when all labels are same: " + cur_cluster.toString() + "with label: " + att_from_above);
				System.out.println("Label table size: " + labelTable.get(globalContextPair).size());
				// Build classifier with labels obtained and test on test objects 
				if(labelTable.get(globalContextPair).size() > 1)
					buildClassifierAndTest(globalContextPair, modality, writer);

				writeRequestFile(0,outlier_objs,att_from_above);			//send request for deleting outliers
				req_sent = true;
				
				// Wait for the request.txt file to exist
				//checkIfRequestFileExists(modality, DB, OBC, clusterNum);
			}
			else if(common_lab.equals("Recluster")){
				// HACK - Just create an array for the argument of writeRequestFile
				ArrayList<String> att = new ArrayList<String> ();
				att.add(att_from_above);
				
				writeRequestFile(2, att, att_from_above); 	//recluster
				req_sent = true;
				
				// Wait for the request.txt file to exist
				//checkIfRequestFileExists(modality, DB, OBC, clusterNum);
			}
			/*else{
				if(cur_cluster.size() <= 3 || ask_mult_choice("Are most of these objects similar in ", clusterAttribute, "No", "Yes")){
					boolean mult_choice;
					mult_choice = ask_mult_choice("How many objects don't fit the ", clusterAttribute, ">2", "1 or 2");
					if(!mult_choice){
						ArrayList<String> outliers = splitString(
								ask_free_resp("Please specify the names of the outlier(s), separated by spaces", ""));
						ArrayList<String> full_cluster = (ArrayList<String>) cur_cluster.clone();
						System.out.println("Full_cluster at beginning: " + full_cluster.toString());
						String outlier_name = null;
						
						for(int i = 0; i < outliers.size(); i++){
							outlier_name = outliers.get(i);
							full_cluster.remove(outlier_name);
							System.out.println("Full cluster after removing outlier: " + full_cluster.toString());
							cur_cluster.clear();
							cur_cluster.add(outlier_name);
							System.out.println("Full cluster later on: " + full_cluster);
							System.out.println("Current cluster later on: " + cur_cluster);
							
							String answer = ask_free_resp("What is the ", clusterAttribute);
							System.out.println("Label of the outlier: "+answer);
							boolean extant = false;
							//store answer as label
							ArrayList<Triple> values = labelTable.get(curContextPair); //overwriting here?
							
							for(int j = 0; j < values.size() && extant == false; j++){
								if(values.get(j).getLabel().equals(answer)){ //label exists in table. Add it to list of objects
									Triple temp = values.remove(j);					//must remove element, to update it, then add it back
									ArrayList<String> objects = temp.getObjects();
									objects.add(outlier_name);
									Triple update = new Triple(temp.getLabel(),objects,temp.getQuestionNum()+questions_per_label);
									values.add(update);								//finally add back the updated triple
									extant = true;
									System.out.println("Values after updating a label: " + values.size());
								}
							}
							labelTable.put(curContextPair, values);
							
							if(extant == false){											//label doesn't exist in table. Create it
								Triple labelTriple = new Triple(answer, cur_cluster, questions_per_label);
								//add to list or put in table??
								values.add(labelTriple);
								labelTable.put(curContextPair, values);
								System.out.println("Values after adding a new label: " + values.size());
								extant = true;
							}

							writeRequestFile(0,outlier_name,answer);			//send request to Java prog
							while(!responseFileExists()){						//wait for Java to respond with updated cluster
								try {
									    Thread.sleep(100);
									} catch(InterruptedException ex) {
									    Thread.currentThread().interrupt();
									}
							}
							readResponseFile();
							try {
								    Thread.sleep(1000);
								} catch(InterruptedException ex) {
								    Thread.currentThread().interrupt();
								} 											//waits for the cv window to update
						}*/
						/* Here I need to grab the label for the attribute. Need to check label table for:
						 * the existance of the attribute. If it exists, add the label to the vector of labels already seen
						 * Otherwise, add an entry pair <feature, vec:labels>
						 */
						
						/*cur_cluster = full_cluster; //make the current cluster the previous current cluster minus the outliers
						System.out.println("Full cluster after removing outliers: " + full_cluster.toString());
						att_from_above = ask_free_resp("What " , clusterAttribute);
						System.out.println("Att_From_above: " + att_from_above);
						boolean extant = false;
						//store answer as label
						ArrayList<Triple> values = labelTable.get(curContextPair); //overwriting here?
						for(int j = 0; j < values.size() && extant == false; j++){
							if(values.get(j).getLabel().equals(att_from_above)){ //label exists in table. Add it to list of objects
								Triple temp = values.remove(j);					//must remove element, to update it, then add it back
								System.out.println("There is a matching label");
								ArrayList<String> objects = temp.getObjects();
								for(int k = 0; k<cur_cluster.size(); k++){
									objects.add(cur_cluster.get(k));
								}
								Triple update = new Triple(temp.getLabel(),objects,temp.getQuestionNum()+questions_per_label);
								values.add(update);								//finally add back the updated triple
								extant = true;
							}
						}
						labelTable.put(curContextPair, values);
						
						if(extant == false){											//label doesn't exist in table. Create it
							System.out.println("There is NO matching label");
							Triple labelTriple = new Triple(att_from_above, cur_cluster, questions_per_label);
							//add to list or put in table??
							values.add(labelTriple);
							labelTable.put(curContextPair, values);
							extant = true;
						}
					}
					else{
						writeRequestFile(2, att_from_above, att_from_above); 	//recluster
						req_sent = true;
					}
				}
				else{
					writeRequestFile(2, att_from_above, att_from_above); 		//recluster
					req_sent = true;
				}
			}
			
			System.out.println("Waiting...");
*/
			//get next cluster
			if(!req_sent){
				writeRequestFile(1,att_from_above);
				req_sent = false;
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	// Method to build classifiers with the clusters labeled so far and test them on test objects
	// Obtain the labels so far from the label_table
	// TODO: This method should add labels to the instances -> create files with data with labels
	// And then build classifiers
	// Create decision trees and test on test object and output accuracy to a file. Don't create a new file unless its a different modality
	public static void buildClassifierAndTest(Pair curContextPair, String modality, PrintWriter writer){
		System.out.println("In build classifier and test");
		
		ArrayList<Triple> labelTriple = labelTable.get(curContextPair);
		// new Triple(att_from_above, cur_cluster, questions_per_label);
		try{
			// To have a mapping between each object and its label
			HashMap<String, String> object_labels = new HashMap<String, String>();
			HashMap<String, String> training_object_labels = new HashMap<String, String>();
			ArrayList<String> training_objs = new ArrayList<String>();
			DataLoaderCY DL = new DataLoaderCY();
			
			FeatureDataLoader FDL = new FeatureDataLoader();
			
			//load pre-computed features
			FDL.loadContextsDataWithLabels(rc_behavior_modalities);
			
			for(int i = 0; i<labelTriple.size(); i++){
				// Get the objects in the cluster and its label
				ArrayList<String> cluster_objs = new ArrayList<String> ();
				String cluster_label = labelTriple.get(i).getLabel();
				for(int j=0; j<labelTriple.get(i).getObjects().size(); j++){
					cluster_objs.add(labelTriple.get(i).getObjects().get(j));
					training_objs.add(labelTriple.get(i).getObjects().get(j));
				}
				
				// Get the labels of all the training objects to compare our classifiers against
				for(int l=0;l<cluster_objs.size();l++){
					HashMap<String, String> combination = groundTruthTable.get(cluster_objs.get(l)); 
					training_object_labels.put(cluster_objs.get(l), cluster_label);	
				}
			}

			// Create instances 
			ArrayList<String> objects_list = DL.getObjectList();
			
			String attr = getAttributeForModality(modality);
			
			// Get the labels of all the objects to compare our classifiers against
			for(int l=0;l<objects_list.size();l++){
				HashMap<String, String> combination = groundTruthTable.get(objects_list.get(l)); 
				String label = combination.get(attr);
				object_labels.put(objects_list.get(l), label);	
			}
			
			//labeling function labels trials by labels
			IClassLabelFunction LF = new ClassLabelID(objects_list, object_labels);
			ArrayList<String> class_values = LF.getValueSet();
			//System.out.println("Class values in build classifier: " + class_values.toString());
			String [] classVals = new String[class_values.size()];
			for (int f = 0; f < class_values.size(); f++)
				classVals[f]=class_values.get(f);
						
			//setup instances in order to setup kernel
			ContextInstancesCreator IC = new ContextInstancesCreator();
			IC.setLabelFunction(LF);
			IC.setData(FDL.getData(modality));
			IC.generateHeader();
					
			//full set of trials for training			
			System.out.println("Training objects size: " + training_objs.size());
			ArrayList<InteractionTrial> train_trials = DL.generateTrials(training_objs, 6);
			
			Instances train = IC.generateFullSet(train_trials);
			System.out.println("Training instances: " + train.numInstances());
				
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
			Instances test = IC.generateFullSet(test_trials);
			System.out.println("Training objects: " + training_objs.toString());
			System.out.println("Test objects: " + test_objects.toString());
			System.out.println("\t\tNEW ITERATION: Train on "+train.numInstances()+" and test on "+ test.numInstances());
			writer.println("\t\tNEW ITERATION: Train on "+train.numInstances()+" and test on "+ test.numInstances());
						
			EV.evaluateModel(C_boost, test);
			
			// Write out the results to the file
			System.out.println(EV.toSummaryString());
			System.out.println(EV.toClassDetailsString());
			writer.println(EV.toSummaryString());			
			writer.println(EV.toClassDetailsString());
				
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
	
	public static void writeLabelTableToFile(HashMap<Pair, ArrayList<Triple> > labelTable){
		System.out.println("The number of items in LabelTable: "+labelTable.size()); 
		try{
			 PrintWriter writer = new PrintWriter("/home/priyanka/Documents/grounded_language_learning/AutoQuestionAnswer/src/etc/LabelTable.csv", "UTF-8");
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
			 PrintWriter writer = new PrintWriter("/home/priyanka/Documents/grounded_language_learning/AutoQuestionAnswer/src/etc/QuestionCountTable.csv", "UTF-8");
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
}

