package categorization;

import java.util.ArrayList;
import java.util.HashMap;

public class ObjectClusterer {
	//ids of objects in this node
	ArrayList<String> IDs;
	
	//children of this node
	ArrayList<ObjectClusterer> children;
	
	//node name
	String nodeID;
	
	//spectral clusterer
	SpectralWEKA SC;
	
	//sim db
	GenericSimDB sDB;
	
	int minNodeSize = 4;
	
	double alpha;
	
	public void setAlpha(double a){
		alpha = a;
	}
	
	public String [] getIDsArray(){
		String [] A= new String[IDs.size()];
		for (int i = 0; i < A.length; i++)
			A[i]=IDs.get(i);
		
		return A;
	}
	
	public ArrayList<ObjectClusterer> getLeafs(){
		 ArrayList<ObjectClusterer> leafs = new  ArrayList<ObjectClusterer>();
		 
		 if (children.size() == 0)
			 leafs.add(this);
		 else {
			 for (int i = 0; i < children.size(); i ++){
				 ArrayList<ObjectClusterer> leafs_i = children.get(i).getLeafs();
				 
				 leafs.addAll(leafs_i);
			 }
		 }
		 
		 return leafs;
	}
	
	public void printClustering(int depth){
		for (int i = 0; i < depth; i ++)
			System.out.print("\t");
		
		System.out.print("nodeSize = "+IDs.size()+"\tIDs: ");
		for (int i = 0; i < IDs.size(); i ++)
			System.out.print(IDs.get(i)+" ");
		System.out.println();
		
		for (int i = 0; i < children.size(); i ++){
			children.get(i).printClustering(depth+1);
		}
	}
	
	public void setMinNodeSize(int n){
		minNodeSize = n;
	}
	
	public void buildClustering() throws Exception{
		
		children = new ArrayList<ObjectClusterer>();
		
		//System.out.println("\nClustering node "+nodeID+" with "+IDs.size()+" datapoints.");
		//System.out.println(IDs.toString()+" and matrix:");
		//UtilsJS.printMatrix(sDB.getSubsetSimMatrix(IDs));
		
		if (IDs.size() >= minNodeSize){
		
			children = new ArrayList<ObjectClusterer>();
			
			
			SC = new SpectralWEKA();
			SC.setAlphaStar(alpha);
			
			try {
				SC.buildClusterer(IDs, sDB.getSubsetSimMatrix(IDs));
				//System.out.println("Found "+SC.numOfClusters +" clusters.");
			}
			catch (Exception E){
				System.out.println("Cannot build SC with IDs "+IDs.toString()+" and matrix:\n"+sDB.getSubsetSimMatrix(IDs).toString());
				return;
			}
			
			int numClusters = SC.numOfClusters;
			//System.out.println("\tFound "+numClusters+" clusters.");
			
			if (numClusters > 1){
				for (int i = 0; i < numClusters; i ++){
					ArrayList<String> clusterIDs = new ArrayList<String>();
					
					for (int k = 0; k < IDs.size(); k ++){
						if (SC.getClusterNumber(IDs.get(k)) == i)
							clusterIDs.add(IDs.get(k));
					}
					
					ObjectClusterer child_i = new ObjectClusterer();
					child_i.setSDB(sDB);
					child_i.setIDs(clusterIDs);
					
					children.add(child_i);
					
				}
				
				for (int i = 0; i < children.size(); i ++){
					children.get(i).setAlpha(alpha);
					children.get(i).buildClustering();
				}
			}
		}
	}
	
	
	public ArrayList<String> getIDs() {
		return IDs;
	}

	public void setIDs(ArrayList<String> ds) {
		IDs = ds;
	}

	public String getNodeID() {
		return nodeID;
	}

	public void setNodeID(String nodeID) {
		this.nodeID = nodeID;
	}

	public void setSDB(GenericSimDB sdb) {
		sDB = sdb;
	}
	
	public ArrayList<ObjectClusterer> getChildren(){
		return children;
	}
}
