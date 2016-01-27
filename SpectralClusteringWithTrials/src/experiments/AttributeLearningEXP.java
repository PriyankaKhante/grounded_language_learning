package experiments;

import features.ClassLabelID;
import features.ContextInstancesCreatorForKMeans;
import features.IClassLabelFunction;
import features.ContextInstancesCreator;
import features.ContextFeatureData;
import features.FeatureDataLoader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;

import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.attribute.StringToNominal;
import weka.classifiers.functions.supportVector.NormalizedPolyKernel;
import weka.clusterers.FilteredClusterer;
import weka.clusterers.XMeans;
import weka.clusterers.SimpleKMeans;
import categorization.ClusterDB;
import categorization.GenericSimDB;
import categorization.ObjectClusterer;
import weka.PolyKernelJS;
import data.DataLoaderCY;
import data.DataLoaderOutliers;
import data.InteractionTrial;

public class AttributeLearningEXP{
	//static Instances data;
	static HashMap<String, ClusterDB> behavior_modality_clusters = new HashMap<String, ClusterDB>();
	static HashMap<ClusterDB, ObjectClusterer> objectClusterTable = new HashMap<ClusterDB, ObjectClusterer>();
	
	// To store the contexts with their specific general attributes
	static HashMap<String, String> contextAttributeTable = new HashMap<String, String>();
	
	// To store the ordering of the object list
	static HashMap<String, Integer> orderedObjectList = new HashMap<String, Integer>();
	
	// To store the outliers after all the perfect clusters have got their labels
	static ArrayList<String> outliers_list = new ArrayList<String>();
	
	public static void main(String[] args) throws Exception{
		// ********* FOR NOW POPULATE OUTLIERS HERE. LATER TAKE IT OUT!!!!!!! **********
		// Every time the user picks an outlier, it should be added to this list. 
		outliers_list.add("silver_cappuccino_maker");
		outliers_list.add("tin_can");
		outliers_list.add("tennis_container_split_peas");
		outliers_list.add("christmas_red_container");
		outliers_list.add("wooden_cylindrical_container");
		outliers_list.add("regular_creamcheese_box");
		outliers_list.add("blue_salt_can");
			
		//behaviour-modailities to use
		String [] rc_behavior_modalities = {"look_color"};
				//{"drop_audio","revolve_audio","push_audio","shake_audio", "hold_haptics","lift_haptics",
											//"press_haptics","squeeze_haptics","grasp_size","look_color","look_shape"};
		String[] rc_behavior_modality = new String[1];
		rc_behavior_modality[0] = rc_behavior_modalities[0];
		
		// Modalities that have been taken out
		// grasp-audio, hold-audio, lift-audio, poke-audio, press-audio, squeeze-audio, drop-haptics,
		// poke-haptics, revolve-haptics, push-haptics, shake-haptics, grasp-haptics
		
		Collections.shuffle(Arrays.asList(rc_behavior_modalities));
		
		try {
			computePairwiseSimilarityMatrices(rc_behavior_modalities);
			
			// Check if the first request is for a new cluster
			File req = new File("/Users/Priyanka/Desktop/groundedRequest.txt");
			//File req = new File("/home/users/pkhante/Desktop/groundedRequest.txt");
			while(!req.exists()){
			  //System.out.println("Going to sleep as first request.txt file does not exist!");
			  Thread.sleep(2000); 
			}
			//System.out.println("Waking up as first request.txt file now exists!");
			FileReader fr = new FileReader(req);
	        BufferedReader br = new BufferedReader(fr);
	        boolean validReq = false;
	        String nl = "";
	        while((nl = br.readLine()) != null) {
	        	if(nl.equals("1")){
	        		validReq = true;
	        		break;
	        	}
	        	else
	        		System.out.println("First request is not valid!");
	        }
	        br.close();
	        req.delete();
			
			if(validReq){
				// Do the following for each behaviour-modality
				for(int j=0; j < rc_behavior_modalities.length; j++){
					// Get clusters starting from depth = 2
					ArrayList<ObjectClusterer> result = getClustersAtDepth(rc_behavior_modalities[j], 2);
					Collections.shuffle(result);
					// Get clusterNumber
					ClusterDB DB = behavior_modality_clusters.get(rc_behavior_modalities[j]);
					//System.out.println("Result size: "+result.size());
					// Do the following for each cluster starting at depth = 2
					//for(int i=0; i<result.size(); i++){
					for(int i=0; i<result.size(); i++){
						//System.out.println("Sending clusters to display: " + result.get(i).getIDs());
						selectClustersToDisplay(rc_behavior_modalities[j], DB, result.get(i));
						// Get the clusterNumber
						/*ArrayList<String> clusterids = result.get(i).getIDs();
						String listString = "";
						for(String clusterIDs : clusterids){
							listString += clusterIDs + ",";
						}
						int clusterNum = DB.getClusterNumber(listString);
						
						// Send a command to create response.txt
						createResponseFile(rc_behavior_modalities[j], DB, result.get(i), clusterNum);*/
						
						// After all the clusters in spectral clustering get labelled, get the outlier clusters
						performSimpleKMeansClustering(rc_behavior_modality);
					}
					//System.out.println("Changing modalities");
					result.clear();
					DB.mergeOutlierObjectsWithClusters();
					DB.mergeClustersWithSameLabels();
					//DB.printClustersWithObjectIDsAndLabels(rc_behavior_modalities[j]);
				}
				createEndFile();
				//System.out.println("Done with all modalities");
			}
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void selectClustersToDisplay(String rc_behavior_modality, ClusterDB DB, ObjectClusterer result){
		if(result.getIDs().size() <= 5){
			HashMap<Integer, ObjectClusterer> clusterNumAndOCTable = DB.getClusterNumAndOCTable();
			for (Map.Entry<Integer, ObjectClusterer> e : clusterNumAndOCTable.entrySet()) {
				ArrayList<String> tempClusterIds = e.getValue().getIDs();
					
				if(tempClusterIds.equals(result.getIDs())){
					//System.out.println("New cluster: " + (Integer)e.getKey());
					// Send the modified clusters to be displayed
					createResponseFile(rc_behavior_modality, DB, result, (Integer)e.getKey());
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
					if(OCC.getIDs().size() <= 5)
						selectClustersToDisplay(rc_behavior_modality, DB, OCC);
					else
						selectClustersToDisplay(rc_behavior_modality, DB, OCC);
				}
			}
		}
	}
	
	public static void createEndFile(){
		System.out.println("Creating an end program file");
		try{
			//PrintWriter writer = new PrintWriter("/home/users/pkhante/Desktop/groundedResponse.txt", "UTF-8");
			PrintWriter writer = new PrintWriter("/Users/Priyanka/Desktop/groundedResponse.txt", "UTF-8");
			writer.println("EndOfAllModalities");
			writer.close();
		}
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void createResponseFile(String rc_behavior_modality, ClusterDB DB, ObjectClusterer OBC, int clusterNum){
		try{
			// Request text file code
			//PrintWriter writer = new PrintWriter("/home/users/pkhante/Desktop/groundedResponse.txt", "UTF-8");
			PrintWriter writer = new PrintWriter("/Users/Priyanka/Desktop/groundedResponse.txt", "UTF-8");
			writer.println(rc_behavior_modality);
			writer.println(clusterNum);
			// Get the cluster IDs
			ArrayList<String> IDs = OBC.getIDs();
			//System.out.println("Sending clusters to display: " + IDs.toString());
			for(int g=0; g<IDs.size(); g++){
				writer.println(IDs.get(g));
			}
			writer.close();
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Wait for the request.txt file to exist
		checkIfRequestFileExists(rc_behavior_modality, DB, OBC, clusterNum);
	}
	
	public static void checkIfRequestFileExists(String rc_behavior_modality, ClusterDB DB, ObjectClusterer OBC, int clusterNum){
		try{
			// Check if request.txt file exists and sleep till it does
			File request = new File("/Users/Priyanka/Desktop/groundedRequest.txt");
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
		readRequestFile(rc_behavior_modality, DB, OBC, clusterNum);
	}

	public static void readRequestFile(String rc_behavior_modality, ClusterDB DB, ObjectClusterer OBC, int clusterNum){
		try{
			File request = new File("/Users/Priyanka/Desktop/groundedRequest.txt");
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
        		object_to_delete = params.get(1);
        		object_label = params.get(2);
        		
        		/////// Code added for the new algorithm
        		outliers_list.add(object_label);
        		///////
        		
 				DB.setOutlierObjectLabels(object_to_delete, object_label);
     			DB.deleteIDFromAllClusters(object_to_delete);
     			DB.deleteFromClusterTable(clusterNum, object_to_delete);
     			//System.out.println("ClusterNum: " + clusterNum);
     			DB.clearInvertedClusterTable();
     			DB.createInvertedClusterTable();
     			
				// Get clusterNumber
				ArrayList<String> clusterids1 = OBC.getIDs();
				String listString1 = "";
				for(String clusterIDs : clusterids1){
					listString1 += clusterIDs + ",";
				}
				int clusterNum1 = DB.getClusterNumber(listString1);
			    //System.out.println("ClusterNum1: " + clusterNum1);
			    // Send the modified clusters to be displayed
				createResponseFile(rc_behavior_modality, DB, OBC, clusterNum1);
        	}
        	
        	// Send new cluster
        	if(params.get(0).equals("1")){
        		//System.out.println("Size of params:" + params.size());
        		previousClusterNum = params.get(1);
        		cluster_label = params.get(2);
        		if(previousClusterNum.equals(Integer.toString(clusterNum)))
 					DB.setLabelForCluster(clusterNum, cluster_label);
        		
        		// After you get a cluster label, then build rebuild that classifier
        		
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
         						createResponseFile(rc_behavior_modality, DB, OCC, (Integer)e.getKey());
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
	
	public static boolean deleteDirectory(File directory) {
	    if(directory.exists()){
	        File[] files = directory.listFiles();
	        if(null!=files){
	            for(int i=0; i<files.length; i++) {
	                if(files[i].isDirectory()) {
	                    deleteDirectory(files[i]);
	                }
	                else {
	                    files[i].delete();
	                }
	            }
	        }
	    }
	    return(directory.delete());
	}
	
	public static void computePairwiseSimilarityMatrices(String[] rc_behavior_modalities) throws Exception{
		DataLoaderCY DL = new DataLoaderCY();
		ArrayList<String> object_list = DL.getObjectList();
		
		// Generate the test and training object sets
		ArrayList<String> test_objects = DL.getRandomTestObjectSet(object_list, 45);
		//ArrayList<String> test_objects = new ArrayList<String>();
		ArrayList<String> objects = DL.getRandomTrainObjectSet(object_list, test_objects);
		
		/*for(int i=0; i<test_objects.size(); i++){
			System.out.println("Test Objects: "+test_objects.get(i));
		}
		
		for(int i=0; i<objects.size(); i++){
			System.out.println("Train Objects: "+objects.get(i));
		}*/
		
		//load pre-computed features
		FeatureDataLoader FDL = new FeatureDataLoader();
		FDL.loadContextsData(rc_behavior_modalities);
		
		//full set of trials
		ArrayList<InteractionTrial> trials = DL.generateTrials(objects, 6);
		
		//labeling function labels trials by object ID
		IClassLabelFunction LF = new ClassLabelID(objects);
		ArrayList<String> class_values = LF.getValueSet();
		String [] classVals = new String[class_values.size()];
		for (int i = 0; i < class_values.size(); i++)
			classVals[i]=class_values.get(i);
		
		//output path
		//String output_path = new String("/home/users/pkhante/extracted_feature_vectors/output_files");
		//File output_files = new File(output_path);
		//deleteDirectory(output_files);
		
		for (int b = 0; b <rc_behavior_modalities.length; b++){
			System.out.print("Computing similarity for context:\t["+rc_behavior_modalities[b]+"]\n\t");
				
			//setup instances in order to setup kernel
			ContextInstancesCreator IC = new ContextInstancesCreator();
			IC.setLabelFunction(LF);
			IC.setData(FDL.getData(rc_behavior_modalities[b]));
			IC.generateHeader();
			Instances data_mb = IC.generateFullSet(trials);
			
			//System.out.println(data_mb.instance(59));
			
			//kernel
			PolyKernelJS K = new PolyKernelJS(data_mb,250007,1.0,false);
			
			//generic sim db
			GenericSimDB R_mb = new GenericSimDB(objects);
	
			//for each pair of objects
			for (int i = 0; i < objects.size();i++){
				Instances data_i = IC.getDataForObject(objects.get(i), 6);
				for (int j = i; j < objects.size(); j++){
					Instances data_j = IC.getDataForObject(objects.get(j), 6);
				
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
			
			System.out.println("\n\nObject Clustering:\n");
			OC.printClustering(10);
			
			//deleteIDFromAllClusters("tin_can", OC);
			//System.out.println("\n\nObject Clustering2:\n");
			//OC.printClustering(10);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void createOutliersFile(ArrayList<String> outliers_list, String rc_behavior_modality){
		// Read the original object list first and find the data of the outliers
		try {
			FeatureDataLoader FDL = new FeatureDataLoader();
			String currentFilePath = FDL.getCurrentDataFilePath(rc_behavior_modality);
			File file = new File(currentFilePath);
		    
		    //int lineNum = 0;
		    int trialCount = 0;
		   
		    // Create an outlier data file in the same folder as the original one
		    String outlierFilePath = currentFilePath.substring(0, currentFilePath.length()-4) + "_outliers.csv";
		    //System.out.println("Outlier File Path length: " + outlierFilePath);
		    PrintWriter writer = new PrintWriter(outlierFilePath, "UTF-8");
		    
		    for(int i=0;i<outliers_list.size();i++){
		    	Scanner scanner = new Scanner(file);
		    	while (scanner.hasNextLine()) {
			        String line = scanner.nextLine();
			        //lineNum++;
			        if(line.contains(outliers_list.get(i))) { 
			        	trialCount++;
			        	writer.println(line);
			            //System.out.println("Outlier " + outliers_list.get(i) +"exists at " +lineNum);
			        }
			        if(trialCount == 6){
			        	trialCount = 0;
			        	break;
			        }
			    }
		    	scanner.close();
		    }
		    writer.close();
		} catch(Exception e) { 
			e.printStackTrace();
		}
	}
	
	//Simple k-Means Clustering
	public static void performSimpleKMeansClustering(String[] rc_behavior_modality) throws Exception{
		DataLoaderCY DL = new DataLoaderCY();
		ArrayList<String> object_list = DL.getObjectList();
		for(int i=0; i<object_list.size();i++){
			orderedObjectList.put(object_list.get(i), i);
		}
		
		int[] outlier_ranks = new int[outliers_list.size()];
		// Order the outliers list according to the object order in the original list
		for(int i=0;i<outliers_list.size();i++){
			//System.out.println(outliers_list.get(i));
			int rank = orderedObjectList.get(outliers_list.get(i));
			outlier_ranks[i] = rank;
		}
		
		Arrays.sort(outlier_ranks);
		
		//Put back the outliers into the list in the sorted order
		outliers_list.clear();
		for(int i=0;i<outlier_ranks.length;i++){
			for (Map.Entry<String, Integer> e : orderedObjectList.entrySet()) {
				int num = e.getValue();
				if(num == outlier_ranks[i]){
					outliers_list.add(e.getKey());
					break;
				}
			}
		}
		
		// Create an object lists file with just the outliers
		try{
			// Request text file code
			//PrintWriter writer = new PrintWriter("/home/users/pkhante/Desktop/groundedResponse.txt", "UTF-8");
			PrintWriter writer = new PrintWriter("/Users/Priyanka/Downloads/extracted_feature_vectors/object_list_outliers.csv", "UTF-8");
			for(int i=0;i<outliers_list.size();i++){
				writer.println(outliers_list.get(i));
			}
			writer.close();
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Create the extracted feature files for the outliers as well
		createOutliersFile(outliers_list, rc_behavior_modality[0]);

		DataLoaderOutliers DLO = new DataLoaderOutliers();
		
		// Generate the object sets
		//ArrayList<String> test_objects = DL.getRandomTestObjectSet(object_list, 45);
		ArrayList<String> test_objects = new ArrayList<String>();
		ArrayList<String> objects = DLO.getRandomTrainObjectSet(outliers_list, test_objects);
		
		/*for(int i=0; i<test_objects.size(); i++){
			System.out.println("Test Objects: "+test_objects.get(i));
		}
		
		for(int i=0; i<objects.size(); i++){
			System.out.println("Train Objects: "+objects.get(i));
		}*/
		
		//load pre-computed features
		FeatureDataLoader FDL = new FeatureDataLoader();
		FDL.loadContextsData(rc_behavior_modality);
		
		//full set of trials
		ArrayList<InteractionTrial> trials = DLO.generateTrials(objects, 6);
		
		//labeling function labels trials by object ID
		IClassLabelFunction LF = new ClassLabelID(objects);
		ArrayList<String> class_values = LF.getValueSet();
		String [] classVals = new String[class_values.size()];
		for (int i = 0; i < class_values.size(); i++)
			classVals[i]=class_values.get(i);
		
		for (int b = 0; b<rc_behavior_modality.length; b++){
			System.out.print("Computing k-means clustering for context:\t["+rc_behavior_modality[0]+"]\n\t");
				
			ContextInstancesCreatorForKMeans IC = new ContextInstancesCreatorForKMeans();
			IC.setLabelFunction(LF);
			IC.setData(FDL.getData(rc_behavior_modality[0]));
			IC.generateHeader();
			Instances data_mb = IC.generateFullSet(trials);
			
			// Converts string attributes to Nominal attributes
			/*Instances filteredTrainingSet = data_mb;
			StringToNominal stringToNominal;
			for (int attIndex=0; attIndex < data_mb.numAttributes() - 1; attIndex++){
			      if (data_mb.attribute(attIndex).isString()) {
			        stringToNominal=new StringToNominal();
			        stringToNominal.setInputFormat(filteredTrainingSet);
			        filteredTrainingSet=Filter.useFilter(filteredTrainingSet,stringToNominal);
			      }
			}*/
			
			// Take out the last - object name attribute before clustering.
			FilteredClusterer fc = new FilteredClusterer();
			SimpleKMeans kmeans = new SimpleKMeans();
			
			String[] options = new String[2];
			options[0] = "-R"; // "range"
			options[1] = Integer.toString(data_mb.numAttributes()-1); // we want to ignore the attribute that is in the position '1'
			Remove remove = new Remove(); // new instance of filter
			remove.setOptions(options); // set options
			remove.setInputFormat(data_mb); // inform filter about dataset
			fc.setFilter(remove); //add filter to remove attributes
			fc.setClusterer(kmeans); //bind FilteredClusterer to original clusterer
			
			kmeans.setSeed(10);
			 
			//important parameter to set: preserver order, number of cluster.
			kmeans.setPreserveInstancesOrder(true);
			kmeans.setNumClusters(6);
			
			fc.buildClusterer(data_mb);
			 
			// This array returns the cluster number (starting with 0) for each instance
			// The array has as many elements as the number of instances
			int[] assignments = kmeans.getAssignments();
			System.out.println("Assignments size: " + assignments.length);
	 
			/*Enumeration enume = data_mb.enumerateAttributes();
			while (enume.hasMoreElements()){
		        System.out.println(enume.nextElement()); 
		    }*/
			
			//System.out.println("Number of assignments:" + assignments.length);
			/*int i=0;
			for(int clusterNum : assignments) {
			    System.out.printf("Instance %d -> Cluster %d \n", i, clusterNum);
			    i++;
			}*/
			
			// Getting the mode of the 6 set of trials for each object. 
			// This is to deduce which cluster does each object belong to.
			int[] assignment_per_object = new int[outliers_list.size()];
			for(int k=0;k<assignments.length;k+=6){
				int[] calculate_mode = new int[6];
				for(int l=0;l<6;l++){
					calculate_mode[l] = assignments[k+l];
				} 
				int current_mode = mode(calculate_mode);
				assignment_per_object[k/6] = current_mode;
			}
			
			System.out.println("Computing the outlier clusters.....");
			System.out.println("Assignment list: " + assignment_per_object.length);
			for(int i=0;i<assignment_per_object.length;i++) {
			    System.out.println("Cluster: " + assignment_per_object[i] + " -> Object: " + outliers_list.get(i));
			}
			
			//System.out.println("Object: "+data_mb.instance(14).value(data_mb.numAttributes()-1));
		}
	}

	// Calculates the mode of the clusters in k-means for each objects (i.e. from 6 trials per object)
	public static int mode(int a[]) {
	    int maxValue=0, maxCount=0;

	    for (int i = 0; i < a.length; ++i) {
	        int count = 0;
	        for (int j = 0; j < a.length; ++j) {
	            if (a[j] == a[i]) ++count;
	        }
	        if (count > maxCount) {
	            maxCount = count;
	            maxValue = a[i];
	        }
	    }
	    return maxValue;
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
}
