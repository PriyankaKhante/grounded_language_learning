package experiments;

import features.ClassLabelID;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.classifiers.functions.supportVector.NormalizedPolyKernel;
import categorization.ClusterDB;
import categorization.GenericSimDB;
import categorization.ObjectClusterer;
import weka.PolyKernelJS;
import data.DataLoaderCY;
import data.InteractionTrial;

public class AttributeLearningEXP{
	static Instances data;
	static HashMap<String, ClusterDB> behavior_modality_clusters = new HashMap<String, ClusterDB>();
	static HashMap<ClusterDB, ObjectClusterer> objectClusterTable = new HashMap<ClusterDB, ObjectClusterer>();
	
	// To store the contexts with their specific general attributes
	static HashMap<String, String> contextAttributeTable = new HashMap<String, String>();
	
	public static void main(String[] args) throws Exception{
		//behaviour-modailities to use
		String [] rc_behavior_modalities = {"look_color"};//{"drop_audio","revolve_audio","push_audio","shake_audio", "hold_haptics","lift_haptics",
											//"press_haptics","squeeze_haptics","grasp_size","look_color","look_shape"};
		
		// Modalities that have been taken out
		// grasp-audio, hold-audio, lift-audio, poke-audio, press-audio, squeeze-audio, drop-haptics,
		// poke-haptics, revolve-haptics, push-haptics, shake-haptics, grasp-haptics
		
		Collections.shuffle(Arrays.asList(rc_behavior_modalities));
		
		try {
			computePairwiseSimilarityMatrices(rc_behavior_modalities);
			
			// Check if the first request is for a new cluster
			File req = new File("/home/users/pkhante/Desktop/groundedRequest.txt");
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
					else{
						selectClustersToDisplay(rc_behavior_modality, DB, OCC);
					}
				}
			}
		}
	}
	
	public static void createEndFile(){
		System.out.println("Creating an end program file");
		try{
			PrintWriter writer = new PrintWriter("/home/users/pkhante/Desktop/groundedResponse.txt", "UTF-8");
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
			PrintWriter writer = new PrintWriter("/home/users/pkhante/Desktop/groundedResponse.txt", "UTF-8");
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
			File request = new File("/home/users/pkhante/Desktop/groundedRequest.txt");
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
			File request = new File("/home/users/pkhante/Desktop/groundedRequest.txt");
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
		String output_path = new String("/home/users/pkhante/extracted_feature_vectors/output_files");
		//File output_files = new File(output_path);
		//deleteDirectory(output_files);
		
		for (int b = 0; b <rc_behavior_modalities.length; b++){
			//System.out.print("Computing similarity for context:\t["+rc_behavior_modalities[b]+"]\n\t");
				
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
			
			String filename = new String(output_path+"/"+rc_behavior_modalities[b]+"_sim.txt");
			String filename_notags = new String(output_path+"/"+rc_behavior_modalities[b]+"_sim_notags.txt");
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
