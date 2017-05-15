package features;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import data.InteractionTrial;

public class FeatureDataLoader {
	HashMap<String,ContextFeatureData> CD_map;
	String [] BM;
	
	String audio_path = "C:\\Users\\Priyanka\\Documents\\extracted_feature_vectors\\audio";
	String haptics_path = "C:\\Users\\Priyanka\\Documents\\extracted_feature_vectors\\haptics";
	String size_path = "C:\\Users\\Priyanka\\Documents\\extracted_feature_vectors\\grasp_size";
	String color_path = "C:\\Users\\Priyanka\\Documents\\extracted_feature_vectors\\look_color";
	String shape_path = "C:\\Users\\Priyanka\\Documents\\extracted_feature_vectors\\look_shape";
	
	public String [] getBehaviorsAndModalities(){
		return BM;		
	}
	
	/*public boolean hasContext(String [] context, InteractionTrial trial){
		if (CD_map.get(new String(context[0]+"_"+context[1])).hasData(trial.getObject(), trial.getTrial()))
			return true;
		else return false;
	}
	
	public ArrayList<String[]> subsetContextsWithData(ArrayList<String[]> contexts, InteractionTrial trial){
		ArrayList<String[]> subset = new ArrayList<String[]>();
		
		for (int i = 0; i <contexts.size(); i++){
			String [] c_i  = contexts.get(i);
			if (CD_map.get(new String(c_i[0]+"_"+c_i[1])).hasData(trial.getObject(), trial.getTrial()))
				subset.add(c_i);
		}
		
		return subset;
	}*/
	
	public ContextFeatureData getData(String bm){
		String key = new String(bm);
		return CD_map.get(key);
	}
	
	/*public boolean hasData(String behavior, String [] modalities){
		for (int i = 0; i <modalities.length; i++)
			if (this.validCombination(behavior,modalities[i]))
				return true;
		return false;
	}
	
	public int getTotalNumContexts(String [] b, String [] m){
		int c = 0; 
		
		for (int i = 0; i <b.length; i++)
			for (int j = 0; j < m.length; j++)
				if (this.validCombination(b[i], m[j]))
					c++;
		return c;
	}*/
	
	public int getContextIndex(String context){
		int index = 0;
		for (int b = 0; b <BM.length; b++){
			if (BM[b]==context)
				return index;
			else 
				index++;
		}
		
		return -1;
	}
	
	public ArrayList<String> getContexts(){
		//generate list of contexts
		ArrayList<String> sm_contexts = new ArrayList<String>();
		for (int b = 0; b < BM.length; b++){
				String context_mb = BM[b];
				sm_contexts.add(context_mb);
		}
		return sm_contexts;
	}
	
	/*public ArrayList<String[]> getContextsForBehavior(String behavior){
		ArrayList<String[]> sm_contexts = new ArrayList<String[]>();
		for (int m = 0; m < M.length; m++){
			if (this.validCombination(behavior, M[m])){
				String [] context_mb = new String[2];
				context_mb[0]=behavior;
				context_mb[1]=M[m];
				sm_contexts.add(context_mb);
			}
		}
		return sm_contexts;
	}
	
	public ArrayList<String[]> getContextsForBehaviors(ArrayList<String> behaviors){
		ArrayList<String[]> sm_contexts = new ArrayList<String[]>();
		for (int i = 0; i < behaviors.size(); i++){
			sm_contexts.addAll(this.getContextsForBehavior(behaviors.get(i)));
			
			
		}
		return sm_contexts;
	}*/
	

	public void loadContextsData(ArrayList<String> objects, String [] bm){
		
		BM=bm;
		CD_map = new HashMap<String,ContextFeatureData>();
		int c = 0;
		
		for (int i = 0; i <bm.length; i++){
					ContextFeatureData CD_ij = this.loadContextData(bm[i]);
					CD_map.put(CD_ij.getName(), CD_ij);
					c++;
		}
	}
	
	public void loadContextsData(String [] bm){
		
		BM=bm;
		CD_map = new HashMap<String,ContextFeatureData>();
		
		for (int i = 0; i <bm.length; i++){
					ContextFeatureData CD_ij = this.loadContextData(bm[i]);
					CD_map.put(CD_ij.getName(), CD_ij);
		}
	}
	
	// Loads the data from the newly created data files where objects also have the labels
	public void loadContextsDataWithLabels(String[] bm){
		BM=bm;
		CD_map = new HashMap<String,ContextFeatureData>();
		
		for(int i = 0; i < bm.length; i++){
			//createContextDataWithLabels(groundTruthTable, attr, bm[i], objectList);
			ContextFeatureData CD_ij = this.loadContextDataWithLabels(bm[i]);
			CD_map.put(CD_ij.getName(), CD_ij);
		}
	}

	// Loads the context data and creates a new data file for objects along with their labels
	// obj_name -> attribute_name (eg color) -> value (groundTruthTable)
	// Need to reload this newly created file file 
	public void createContextDataWithLabels(HashMap<String, HashMap<String, String>> groundTruthTable, String attr, String bm, ArrayList<String> objectList){
		String readFilePath = "";
		HashMap<String, String> object_label_table = new HashMap<String, String>();
		
		if (bm.equals("drop_audio"))
			readFilePath = audio_path+"\\drop_audio\\dft_extracted_features.csv";
		else if (bm.equals("grasp_audio"))
			readFilePath = audio_path+"\\grasp_audio\\dft_extracted_features.csv";
		else if (bm.equals("hold_audio"))
			readFilePath = audio_path+"\\hold_audio\\dft_extracted_features.csv";
		else if (bm.equals("lift_audio"))
			readFilePath = audio_path+"\\lift_audio\\dft_extracted_features.csv";
		else if (bm.equals("poke_audio"))
			readFilePath = audio_path+"\\poke_audio\\dft_extracted_features.csv";
		else if (bm.equals("press_audio"))
			readFilePath = audio_path+"\\press_audio\\dft_extracted_features.csv";
		else if (bm.equals("push_audio"))
			readFilePath = audio_path+"\\push_audio\\dft_extracted_features.csv";
		else if (bm.equals("revolve_audio"))
			readFilePath = audio_path+"\\revolve_audio\\dft_extracted_features.csv";
		else if (bm.equals("shake_audio"))
			readFilePath = audio_path+"\\shake_audio\\dft_extracted_features.csv";
		else if (bm.equals("squeeze_audio"))
			readFilePath = audio_path+"\\squeeze_audio\\dft_extracted_features.csv";
		else if (bm.equals("drop_haptics"))
			readFilePath = haptics_path+"\\drop_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("grasp_haptics"))
			readFilePath = haptics_path+"\\grasp_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("hold_haptics"))
			readFilePath = haptics_path+"\\hold_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("lift_haptics"))
			readFilePath = haptics_path+"\\lift_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("poke_haptics"))
			readFilePath = haptics_path+"\\poke_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("press_haptics"))
			readFilePath = haptics_path+"\\press_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("push_haptics"))
			readFilePath = haptics_path+"\\push_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("revolve_haptics"))
			readFilePath = haptics_path+"\\revolve_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("shake_haptics"))
			readFilePath = haptics_path+"\\shake_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("squeeze_haptics"))
			readFilePath = haptics_path+"\\squeeze_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("grasp_size"))
			readFilePath = size_path+"\\finger_position_vector.csv";
		else if (bm.equals("look_color"))
			readFilePath = color_path+"\\extracted_feature_colour.csv";
		else if (bm.equals("look_shape"))
			readFilePath = shape_path+"\\extracted_feature_fpfh.csv";
		
		String writeFilePath = readFilePath.substring(0, readFilePath.length()-4) + "_labelled_exp2.csv";
		
		// Access the labels for each attribute of each object and append it to the feature vector file
		// Object_label_Table will only contain labels for a particular attribute in each iteration
		for(int i=0; i<objectList.size(); i++){
			object_label_table.put(objectList.get(i), groundTruthTable.get(objectList.get(i)).get(attr));
		}

		readAndWriteDataFile(readFilePath, writeFilePath, object_label_table, objectList);
	}
	
	public void readAndWriteDataFile(String readFilePath, String writeFilePath, HashMap<String, String> object_label_table, ArrayList<String> objectList){		
		File readFile = new File(readFilePath);
		
		try{
			FileReader fileReader = new FileReader(readFile);
		    BufferedReader bufferedReader = new BufferedReader(fileReader);
		    PrintWriter writer = new PrintWriter(writeFilePath, "UTF-8");
		    String line = "";
		    int line_counter = 0;
		    int i = 0; 
		    String label = "";
		    
		    // Write out the data to the file along with labels for each object
			while((line = bufferedReader.readLine()) != null) {
			    // Make sure that the object you fetch from the hashmap is in the same order as that of the object list
				if(line_counter % 6 == 0){
					// Gives us the label in the correct order
					label = object_label_table.get(objectList.get(i));
				    i++;
				} 
			    writer.println(line + "," + label);
				line_counter++;
			}
			bufferedReader.close();
			writer.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public ContextFeatureData loadContextDataWithLabels(String bm){
		String filePath = "";
		if (bm.equals("drop_audio"))
			filePath = audio_path+"\\drop_audio\\dft_extracted_features_labelled_exp2.csv";
		else if (bm.equals("grasp_audio"))
			filePath = audio_path+"\\grasp_audio\\dft_extracted_features_labelled_exp2.csv";
		else if (bm.equals("hold_audio"))
			filePath = audio_path+"\\hold_audio\\dft_extracted_features_labelled_exp2.csv";
		else if (bm.equals("lift_audio"))
			filePath = audio_path+"\\lift_audio\\dft_extracted_features_labelled_exp2.csv";
		else if (bm.equals("poke_audio"))
			filePath = audio_path+"\\poke_audio\\dft_extracted_features_labelled_exp2.csv";
		else if (bm.equals("press_audio"))
			filePath = audio_path+"\\press_audio\\dft_extracted_features_labelled_exp2.csv";
		else if (bm.equals("push_audio"))
			filePath = audio_path+"\\push_audio\\dft_extracted_features_labelled_exp2.csv";
		else if (bm.equals("revolve_audio"))
			filePath = audio_path+"\\revolve_audio\\dft_extracted_features_labelled_exp2.csv";
		else if (bm.equals("shake_audio"))
			filePath = audio_path+"\\shake_audio\\dft_extracted_features_labelled_exp2.csv";
		else if (bm.equals("squeeze_audio"))
			filePath = audio_path+"\\squeeze_audio\\dft_extracted_features_labelled_exp2.csv";
		else if (bm.equals("drop_haptics"))
			filePath = haptics_path+"\\drop_haptics\\haptic_extracted_features_labelled_exp2.csv";
		else if (bm.equals("grasp_haptics"))
			filePath = haptics_path+"\\grasp_haptics\\haptic_extracted_features_labelled_exp2.csv";
		else if (bm.equals("hold_haptics"))
			filePath = haptics_path+"\\hold_haptics\\haptic_extracted_features_labelled_exp2.csv";
		else if (bm.equals("lift_haptics"))
			filePath = haptics_path+"\\lift_haptics\\haptic_extracted_features_labelled_exp2.csv";
		else if (bm.equals("poke_haptics"))
			filePath = haptics_path+"\\poke_haptics\\haptic_extracted_features_labelled_exp2.csv";
		else if (bm.equals("press_haptics"))
			filePath = haptics_path+"\\press_haptics\\haptic_extracted_features_labelled_exp2.csv";
		else if (bm.equals("push_haptics"))
			filePath = haptics_path+"\\push_haptics\\haptic_extracted_features_labelled_exp2.csv";
		else if (bm.equals("revolve_haptics"))
			filePath = haptics_path+"\\revolve_haptics\\haptic_extracted_features_labelled_exp2.csv";
		else if (bm.equals("shake_haptics"))
			filePath = haptics_path+"\\shake_haptics\\haptic_extracted_features_labelled_exp2.csv";
		else if (bm.equals("squeeze_haptics"))
			filePath = haptics_path+"\\squeeze_haptics\\haptic_extracted_features_labelled_exp2.csv";
		else if (bm.equals("grasp_size"))
			filePath = size_path+"\\finger_position_vector_labelled_exp2.csv";
		else if (bm.equals("look_color"))
			filePath = color_path+"\\extracted_feature_colour_labelled_exp2.csv";
		else if (bm.equals("look_shape"))
			filePath = shape_path+"\\extracted_feature_fpfh_labelled_exp2.csv";
		
		boolean normalize = false;
		//if (m.equals("flow"))
		//	normalize = true;
		
		//if (m.equals("surf-vq"))
		//	normalize = true;
		
		ContextFeatureData CD = new ContextFeatureData(bm);
		CD.loadFromFile(filePath,normalize);
		
		return CD;
	}
	
	public ContextFeatureData loadContextData(String bm){
		String filePath = "";
		if (bm.equals("drop_audio"))
			filePath = audio_path+"\\drop_audio\\dft_extracted_features.csv";
		else if (bm.equals("grasp_audio"))
			filePath = audio_path+"\\grasp_audio\\dft_extracted_features.csv";
		else if (bm.equals("hold_audio"))
			filePath = audio_path+"\\hold_audio\\dft_extracted_features.csv";
		else if (bm.equals("lift_audio"))
			filePath = audio_path+"\\lift_audio\\dft_extracted_features.csv";
		else if (bm.equals("poke_audio"))
			filePath = audio_path+"\\poke_audio\\dft_extracted_features.csv";
		else if (bm.equals("press_audio"))
			filePath = audio_path+"\\press_audio\\dft_extracted_features.csv";
		else if (bm.equals("push_audio"))
			filePath = audio_path+"\\push_audio\\dft_extracted_features.csv";
		else if (bm.equals("revolve_audio"))
			filePath = audio_path+"\\revolve_audio\\dft_extracted_features.csv";
		else if (bm.equals("shake_audio"))
			filePath = audio_path+"\\shake_audio\\dft_extracted_features.csv";
		else if (bm.equals("squeeze_audio"))
			filePath = audio_path+"\\squeeze_audio\\dft_extracted_features.csv";
		else if (bm.equals("drop_haptics"))
			filePath = haptics_path+"\\drop_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("grasp_haptics"))
			filePath = haptics_path+"\\grasp_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("hold_haptics"))
			filePath = haptics_path+"\\hold_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("lift_haptics"))
			filePath = haptics_path+"\\lift_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("poke_haptics"))
			filePath = haptics_path+"\\poke_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("press_haptics"))
			filePath = haptics_path+"\\press_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("push_haptics"))
			filePath = haptics_path+"\\push_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("revolve_haptics"))
			filePath = haptics_path+"\\revolve_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("shake_haptics"))
			filePath = haptics_path+"\\shake_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("squeeze_haptics"))
			filePath = haptics_path+"\\squeeze_haptics\\haptic_extracted_features.csv";
		else if (bm.equals("grasp_size"))
			filePath = size_path+"\\finger_position_vector.csv";
		else if (bm.equals("look_color"))
			filePath = color_path+"\\extracted_feature_colour.csv";
		else if (bm.equals("look_shape"))
			filePath = shape_path+"\\extracted_feature_fpfh.csv";
		
		boolean normalize = false;
		//if (m.equals("flow"))
		//	normalize = true;
		
		//if (m.equals("surf-vq"))
		//	normalize = true;
		
		ContextFeatureData CD = new ContextFeatureData(bm);
		CD.loadFromFile(filePath,normalize);
		
		return CD;
	}
}
