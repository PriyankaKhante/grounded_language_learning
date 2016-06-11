package categorization;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ClusterDB {
	// Object of ObjectClusterer root for that particular behaviour_modality
	ObjectClusterer OC;
	
	// number of clusters in a particular behaviour_modality
	int clusterNumbers = 0;
	
	// The objectIDs in each cluster of each behaviour_modality
	HashMap<Integer, ArrayList<String>> clusterTable;
	
	// To store the learnt labels along with the clusterIDs
	HashMap<Integer, String> clusterLabelTable;
	
	// Inverted cluster table
	HashMap<String, Integer> invertedClusterTable;
	
	// For each behaviour-modality -> ClusterNumber and OC pairing
	HashMap<Integer, ObjectClusterer> clusterNumAndOCTable;
	
	// For each behaviour-modality -> objects that are deleted from their clusters with their label pairing
	HashMap<String, String> outlierObjectLabels;
	
	HashMap<String, String> clustersToBeAdded;
 	
	public ClusterDB(ObjectClusterer OC){
		this.OC = OC;
		clusterTable = new HashMap<Integer, ArrayList<String>>();
		clusterLabelTable = new HashMap<Integer, String>();
		invertedClusterTable = new HashMap<String, Integer>();
		clusterNumAndOCTable = new HashMap<Integer, ObjectClusterer>();
		outlierObjectLabels = new HashMap<String, String>();
		clustersToBeAdded = new HashMap<String, String>();
	}
	
	// Method to merge outlier objects with the clusters depending on their labels
	// Call the following method before calling the mergeClustersWithSameLabels() method
	public void mergeOutlierObjectsWithClusters(){
		Iterator it = outlierObjectLabels.entrySet().iterator();
		while (it.hasNext()) {
			boolean exists = false;
	        Map.Entry pair1 = (Map.Entry)it.next();
	        Iterator iterate = clusterLabelTable.entrySet().iterator();
	        while(iterate.hasNext()){
	        	Map.Entry pair2 = (Map.Entry)iterate.next();
	        	if(pair1.getValue().equals(pair2.getValue())){
	        		exists = true;
	        		//System.out.println("Key: " + pair1.getKey() + " and " + pair2.getKey());
	        		// Object needs to be added back to that cluster
	        		ObjectClusterer OBC = clusterNumAndOCTable.get((Integer)pair2.getKey());
	        		ArrayList<String> objectIDs = OBC.getIDs();
	        		objectIDs.add((String)pair1.getKey());
	        		OBC.setIDs(objectIDs);
	        		break;
	        	}
	        }
	        if(!exists){
	        	clustersToBeAdded.put((String)pair1.getKey(), (String)pair1.getValue());
	        }
		}
		//System.out.println("Outlier object labels: " + outlierObjectLabels.size());
		//System.out.println("Clusters to be added: " + clustersToBeAdded.size());
		//outlierObjectLabels.clear();
	}
	
	// Method to merge clusters with the same labels in each behaviour-modality
	public void mergeClustersWithSameLabels(){
		Set<String> valueSet = new HashSet<String>(clusterLabelTable.values());
		Iterator<String> iterate1 = valueSet.iterator();
		
		HashMap<Integer,String> uniqueClusterLabelTable = new HashMap<Integer,String>();
		HashMap<Integer,String> repeatedClusterLabelTable = new HashMap<Integer,String>();
		 
		while(iterate1.hasNext()){
			String value = iterate1.next();
			for(Entry<Integer,String> e: clusterLabelTable.entrySet()){
				if(value.equals(e.getValue()) && !uniqueClusterLabelTable.containsValue(value)){
					uniqueClusterLabelTable.put(e.getKey(), value);
					continue;
				}
				if(value.equals(e.getValue()) && uniqueClusterLabelTable.containsValue(value)){
					repeatedClusterLabelTable.put(e.getKey(), value);
				}
			}
		}
		
		clusterLabelTable.clear();
		clusterLabelTable = uniqueClusterLabelTable;
		
		// Combine the ObjectIDs for these clusters as well
		Iterator it = clusterLabelTable.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pair1 = (Map.Entry)it.next();
			ObjectClusterer OBC1 = clusterNumAndOCTable.get((Integer)pair1.getKey());
			ArrayList<String> mergedObjectIDs = OBC1.getIDs();
	        Iterator iterate = repeatedClusterLabelTable.entrySet().iterator();
	        while(iterate.hasNext()){
	        	Map.Entry pair2 = (Map.Entry)iterate.next();
	        	if(pair1.getValue().equals(pair2.getValue())){
	        		//System.out.println("Key: " + pair1.getKey() + "and " + pair2.getKey());
	        		ObjectClusterer OBC2 = clusterNumAndOCTable.get((Integer)pair2.getKey());
	        		ArrayList<String> objectIDs = OBC2.getIDs();
	        		mergedObjectIDs.addAll(objectIDs);
	        		clusterNumAndOCTable.remove(pair2.getKey());
	        	}
	        }
	      OBC1.setIDs(mergedObjectIDs);
		}
		
		 // Add outlier objects which dont belong to any cluster
		 Iterator it2 = clustersToBeAdded.entrySet().iterator();
		 int index = 100;
		 while (it2.hasNext()) {
			 Map.Entry pair = (Map.Entry)it2.next();
			 clusterLabelTable.put(index+1, (String)pair.getValue());
			 ObjectClusterer OBC = new ObjectClusterer();
			 ArrayList<String> object = new ArrayList<String>();
			 object.add((String)pair.getKey());
			 OBC.setIDs(object);
			 clusterNumAndOCTable.put(index+1, OBC);
		 }
		 // Cut down all access to these tables as they dont contain the correct objectIDs anymore
		 clusterTable.clear();
		 invertedClusterTable.clear();
	}
	
	public void setOutlierObjectLabels(String objectID, String objectLabel){
		outlierObjectLabels.put(objectID, objectLabel);
	}
	
	// Method to print out the final clustering with their learnt labels
	public void printClustersWithObjectIDsAndLabels(String rc_behavior_modality){
		System.out.println("For behavior_modality: " + rc_behavior_modality);
		Iterator iterate = clusterLabelTable.entrySet().iterator();
		 while(iterate.hasNext()){
			 Map.Entry pair = (Map.Entry)iterate.next();
			 ObjectClusterer OBC = clusterNumAndOCTable.get((Integer)pair.getKey());
			 ArrayList<String> objectIDs = OBC.getIDs();
			 System.out.println();
			 System.out.println("CLUSTER NUMBER: " + pair.getKey() + " with ObjectIDs: " + objectIDs.toString() + " have LABEL: " + pair.getValue());
		 }
	}
	
	public void printKeysOfOCTable(String rc_behavior_modality){
		System.out.println("For behavior_modality: " + rc_behavior_modality);
		Iterator iterate = clusterNumAndOCTable.entrySet().iterator();
		 while(iterate.hasNext()){
			 Map.Entry pair = (Map.Entry)iterate.next();
			 System.out.println("Cluster Number: " + (Integer)pair.getKey());
		 }
	}
	
	public void createInvertedClusterTable(){
		for(Entry<Integer, ArrayList<String>> e : clusterTable.entrySet()){
			String listString = "";
			for(String clusterIDs : e.getValue()){
				listString += clusterIDs + ",";
			}
			invertedClusterTable.put(listString, e.getKey());
			//System.out.println("clustersID: "+ listString + "clusterNumbers: " + e.getKey());
		}
		//System.out.println();
	}
	
	// Call this after an object is deleted from a cluster
	public void clearInvertedClusterTable(){
		invertedClusterTable.clear();
	}
	
	public int getClusterNumber(String clusterIDs){
		int clusterNumber = invertedClusterTable.get(clusterIDs);
		return clusterNumber;
	}
	
	// Always call this method before giveIDsForClusters()
	public void resetClusterNumbers(){
		clusterNumbers = 0;
	}
	
	public HashMap<Integer, ObjectClusterer> getClusterNumAndOCTable(){
		return clusterNumAndOCTable;
	}
	
	public void giveIDsForClusters(){
		giveIDsForClusters(OC);
	}
	
	// Labeling scheme - Pre-order traversal (Skip the root cluster that contains all objects)
	public void giveIDsForClusters(ObjectClusterer OC){
		ObjectClusterer OC_original = OC;
		if(OC.getChildren().size() != 0){
			ArrayList<ObjectClusterer> child = OC.getChildren();
			for(int j=0; j < child.size(); j++){
				clusterNumbers++;
				OC = OC.getChildren().get(j);
				ArrayList<String> clusterIDs = child.get(j).getIDs();
				clusterTable.put(clusterNumbers, clusterIDs);
				clusterNumAndOCTable.put(clusterNumbers, OC);
				giveIDsForClusters(OC);
				OC = OC_original;
			}
		}
		else{
			ArrayList<ObjectClusterer> children = OC.getChildren();
			for (int i = 0; i < children.size(); i ++){
				ArrayList<String> clusterIDs = children.get(i).getIDs();
				clusterTable.put(clusterNumbers, clusterIDs);
				clusterNumAndOCTable.put(clusterNumbers, OC);
			}
			return;
		}
	}
	
	public ArrayList<String> getClustersFromClusterNumber(int clusterNumber){
		return clusterTable.get(clusterNumber);
	}
	
	public void setLabelForCluster(int clusterNumber, String label){
		clusterLabelTable.put(clusterNumber, label);
	}
	
	public void deleteIDFromAllClusters(String objectID){
		// Removes from the root node
		ArrayList<String> clustersIDs = OC.getIDs();
		for(int j=0; j< clustersIDs.size();j++){
			//System.out.println("ClusterID: " + clustersIDs.get(j) + " not equals " + objectID);
			if(clustersIDs.get(j).equals(objectID)){
				clustersIDs.remove(objectID);
				OC.setIDs(clustersIDs);
				//System.out.println("After deleting: " + OC.getIDs().toString());
			}
		}
		deleteIDFromAllChildren(objectID, OC);
	}
	
	public void deleteIDFromAllChildren(String objectID, ObjectClusterer OC){
		ObjectClusterer OC_original = OC;
		if(OC.getChildren().size() != 0){
			//System.out.println("On Node: "+OC.getIDsArray().length);
			ArrayList<ObjectClusterer> child = OC.getChildren();
			for(int j=0; j < child.size(); j++){
				OC = OC.getChildren().get(j);
				ArrayList<String> clusterIDs = child.get(j).getIDs();
				for(int k=0; k< clusterIDs.size();k++){
					if(clusterIDs.get(k).equals(objectID)){
						clusterIDs.remove(objectID);
						OC.setIDs(clusterIDs);
						//System.out.println("After deleting: " + OC.getIDs().toString());
					}
				}
				deleteIDFromAllChildren(objectID, OC);
				OC = OC_original;
			}
		}
		else{
			ArrayList<ObjectClusterer> children = OC.getChildren();
			for (int i = 0; i < children.size(); i ++){
				//System.out.println("Children :" + children.get(i).getIDs().toString());
				ArrayList<String> clusterIDs = children.get(i).getIDs();
				for(int j=0; j< clusterIDs.size();j++){
					if(clusterIDs.get(j).equals(objectID)){
						clusterIDs.remove(objectID);
						OC.setIDs(clusterIDs);
						//System.out.println("After deleting: " + OC.getIDs().toString());
					}
				}	
			}
			return;
		}
	}
	
	public void deleteFromClusterTable(int clusterNum, String object_to_delete){
		ArrayList<String> clusterIds = clusterTable.get(clusterNum);
		for(int i=0; i<clusterIds.size(); i++){
			if(clusterIds.get(i).equals(object_to_delete))
				clusterIds.remove(i);
		}
	}
	
	public void printClusterIDs(){
		System.out.println("Size: "+clusterTable.size());
		Iterator it = clusterTable.entrySet().iterator();
	    while (it.hasNext()) {
	        Map.Entry pair = (Map.Entry)it.next();
	        System.out.println("Cluster: " + pair.getKey() + "->" + pair.getValue().toString());
	    }
	        		
	}
}
