import java.util.StringTokenizer;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.File;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystems;
import java.io.IOException;
import GroundedLanguage.*;

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
	public static final String filePath		= "/home/users/pkhante/Pictures/grounded_learning_images/";
	public static final String reqFilePath	= "/home/users/pkhante/Desktop/";
	public static final String responseName	= "groundedResponse.txt";
	public static final String requestName	= "groundedRequest.txt";

	String modality;
	ArrayList<String> cur_cluster;
	String clusterAttribute;
	int clusterNum; //is this needed?
	boolean firstTime;
	HashMap<String, HashMap<String, String> > groundTruthTable;
	ArrayList<String> outliers;

	public static void main(String[] args) {

	}

	public GroundedAutoQuestion(){
		cur_cluster = new ArrayList<String>();
	}

	/*
	 * Outputs a convoluted table in hashmaps after the input of the grounded truth.
	 * Maps: obj_name -> attribute_name (eg color) -> value
	 * The following attributes have numerical output: height weight and width
	 */
	HashMap<String, HashMap<String, String> > loadGroundTruthTable(){
		try{
			BufferedReader in = new BufferedReader(new FileReader("etc/groundedtruetable.csv"));
			String attributes[] = new String[8];
			//grab all the attribute names. Note that these should match that is output
			//by the modified weca program
			String line;
			if((line = in.readLine()) != null){
				StringTokenizer tokenizer = new StringTokenizer(line,",");
				int columnNum = 1;

				while (tokenizer.hasMoreTokens()){
					attributes[columnNum] = tokenizer.nextToken();
					columnNum++;
				}
			}
			HashMap<String, HashMap<String, String> > groundTruthTable = new HashMap<String, HashMap<String, String> >();
			//build the rest of the table
			while((line = in.readLine()) != null){
				StringTokenizer tokenizer = new StringTokenizer(line,","); 
				int columnNum = 0;
				HashMap<String,String> objectTruthTable = new HashMap<String,String>();

				String name = tokenizer.nextToken();
				while (tokenizer.hasMoreTokens()){ 
					objectTruthTable.put(attributes[columnNum], tokenizer.nextToken());
					columnNum++;
				}
				groundTruthTable.put(name,objectTruthTable);
			}

			in.close();
		} catch(IOException e){
 		 e.printStackTrace();
		}
		return groundTruthTable;
	}


	/*
	 * Returns whether or not response file exists
	 */
	boolean responseFileExists(){
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
	boolean writeRequestFile(int ID, String object_name, String label){
		try{
			PrintWriter writer = new PrintWriter((reqFilePath + requestName), "UTF-8");
			if(writer != null){
				writer.println(ID);
				if(ID == 0){
					System.out.println("Requesting object removal");
					writer.println(object_name);
					writer.println(label);
				}
				else if(ID == 1){
					System.out.println("Requesting next cluster");
					writer.println(clusterNum);
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
	/*
	 * ONLY used for ID 1
	 */
	boolean writeRequestFile(int ID, String label){
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

	/*
	 * Reads the response file from external program
	 * IDs : N/A
	 * Expected format: 
	 	behavior_modality
	 	cluster_num
	 	object names
	 */

	boolean readResponseFile(){
		String line;
		int lineNum = 0;
		int ID;
		ArrayList<String> objects = new ArrayList<String>();
		String fullPath = (reqFilePath + responseName);
		try{
			BufferedReader myfile = new BufferedReader(new FileReader("etc/groundedtruetable.csv"));
			if(myfile != null){
				while((line = myfile.readLine()) != null){
					if(lineNum == 0){
						String old_mod = modality;
						modality = line;
						int first = modality.compareTo(old_mod);
						if(first == 0)
							firstTime = true;
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
				myfile.close();
			}
			else{
				System.out.println("Unable to open response file. Looking for " + fullPath);
				return false;
			}
			cur_cluster = objects;

			for(int i = 0; i < cur_cluster.size(); i++){
				System.out.println("Got: " + cur_cluster.get(i));
			}

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
	ArrayList<String> splitString(String input){
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
	String ask_free_resp(String question, String modality){
		if(question.equals("Please specify what general attribute is described by ")){
			if(modality.equals("drop_audio"))
				return "material";
			if(modality.equals("revolve_audio"))
				return "has_contents";
			if(modality.equals("push_audio"))
				return "material";
			if(modality.equals("hold_haptics"))
				return "weight";
			if(modality.equals("lift_haptics"))
				return "weight";
			if(modality.equals("press_haptics"))
				return "softness";
			if(modality.equals("squeeze_haptics"))
				return "rigity";
			if(modality.equals("grasp_size"))
				return "width";
			if(modality.equals("look_color"))
				return "color";
			if(modality.equals("look_shape"))
				return "shape";
		}
		/*
		* Here its being asked what label is held by this attirbute (modality) by the group of objects
		* or asking what label is held by the single outlier. In both cases the first object in the group
		* has the desired label
		*/
		if(question.equals("What/how ") || question.equals("What is the ") || question.equals("What ")){
			HashMap<String, String> objectLabels = groundTruthTable.get(cur_cluster.get(0));
			return objectLabels.get(modality);
		}
		/*
		 * Here we give the outlier names. presupposes that the outliers list has been set by a previous question
		 */
		if(question.equals("Please specify the names of the outlier(s), separated by spaces")){
			String result = "";
			for(int i = 0; i < outliers.size(); i++){
				result += outliers.get(i);
				if( i < outliers.size() - 1)
					result += " ";
			}
			return result;
		}
		return null;
	}
	/*
	 * Returns one of the options given. 
	 * op1 is always 'no' or '>2'.
	 * op2 is always 'yes' or '1 or 2'
	 */
	boolean ask_mult_choice(String question, String modality, String op1, String op2){
		if(question.equals("Are all of these objects similar in ")){
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

		}
		if(question.equals("Are most of these objects similar in ")){
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
			//search through gtt. if more 2 objects don't fit, return 'no' ie op1 ie true
			//AND set up the outlier list to include these objects
			String label;
			HashMap<String, ArrayList<String> > frequencyList = new HashMap<String,ArrayList<String> >();
			for(int i =0; i < cur_cluster.size(); i++){
				HashMap<String,String> objectEntry = groundTruthTable.get(cur_cluster.get(i));
				label = objectEntry.get(modality);

				if(!frequencyList.containsKey(label)){
					frequencyList.put(label, new ArrayList<String>());
				}
				else{
					ArrayList<String> oldObjs = frequencyList.get(label);
					oldObjs.add(cur_cluster.get(i));
					frequencyList.put(label, oldObjs);
				}
			}
			//find maximum labels, objects
			int max = Integer.MIN_VALUE;
			int index = 0; 					//tracks the location of the 'bulk' label
			int i = 0;
			for(String freqLabel : frequencyList.keySet()){
				if(frequencyList.get(freqLabel).size() > max){
					max = frequencyList.get(freqLabel).size();
					index = i;
				}
				i++;
			}

			//finally, answer the question
			outliers.clear();
			if(cur_cluster.size() - max > 2)
				return true;
			else{
				for(int j = 0; j < frequencyList.size(); j++){
					if(j != index){
						ArrayList<String> objectNames = frequencyList.get(j);
						for(int k = 0; k < objectNames.size(); k++)
							outliers.add(objectNames.get(k));
					}
				}
			}
		}
		return false;
	}

	void sequence(){

		while(!responseFileExists()){		//wait for weca to respond with updated cluster
			try {
				    Thread.sleep(100);
				} catch(InterruptedException ex) {
				    Thread.currentThread().interrupt();
				}
		}
		readResponseFile();
		String att_from_above = "";
		boolean req_sent = false;
		HashMap<Pair, ArrayList<Triple> > labelTable = new HashMap<Pair, ArrayList<Triple> >();
		Pair curContextPair = null;
		ArrayList<Triple> values = null;
		int questions_asked = 0;
		while(true){
			if(firstTime){
				firstTime = false;
				String modality_copy = modality;
				values = labelTable.get(curContextPair);

				//String delimiter = "_";
				//int index = modality_copy.find(delimiter);
				//String action = modality_copy.substr(0, index);
				//String modality = modality_copy.substr(index+1, modality_copy.length());
				String resp = ask_free_resp("Please specify what general attribute is described by ", modality);
				ArrayList<String> feature_vec = splitString(resp);

				/*
				 *got modality and attribute. make a pair and begin gathering labels.
				 */
				curContextPair = new Pair(modality,feature_vec.get(0));

				clusterAttribute = feature_vec.get(0);
			}
			boolean common_att = ask_mult_choice("Are all of these objects similar in ", clusterAttribute, "No", "Yes");
			if(common_att){ //user input 'Yes' to "Any attributes common to all objects?"

					//if context and general attribute are not exclusive, should break it apart like below
					//otherwise there can only be one attribute that matches, which has to exist because its whats being analyzed in the current
					//cycle.
					boolean done = false;
					if(!done){
						att_from_above = ask_free_resp("What/how " , clusterAttribute);
						//issue here. labelTriple is fine but labelTable expects a mapping to an arraylist of entries.
						//should probably just add labelTriple to an arraylist and add mapping once questions are over for this modality
						Triple labelTriple = new Triple(att_from_above, cur_cluster, questions_asked);
						ArrayList<Triple> temp = new ArrayList<Triple>();
						temp.add(labelTriple);
						labelTable.put(curContextPair,temp);
					}
					/*
					else {
						System.out.println("Attribute not found in table.");
						att_from_above = ask_free_resp("What/how " + clusterAttribute + " are they?");
						label_table.insert(std::pair<std::string,std::vector<std::string> >(clusterAttribute,
							splitString(att_from_above)));
					}*/
			}
			else{
				if(cur_cluster.size() <= 3 || ask_mult_choice("Are most of these objects similar in ", clusterAttribute, "No", "Yes")){
					boolean mult_choice;
					mult_choice = ask_mult_choice("How many objects don't fit the ", clusterAttribute, ">2", "1 or 2");
					if(mult_choice){
						ArrayList<String> outliers = splitString(
								ask_free_resp("Please specify the names of the outlier(s), separated by spaces", ""));
						ArrayList<String> full_cluster = cur_cluster;
						String outlier_name = null;
						for(int i = 0; i < outliers.size(); i++){
							outlier_name = full_cluster.get(Integer.parseInt(outliers.get(i)));
							System.out.println("Outlier name: " + outlier_name);
							full_cluster.remove(outlier_name);
							cur_cluster.clear();
							cur_cluster.add(outlier_name);
							
							String answer = ask_free_resp("What is the ", clusterAttribute);

							//store answer as label
							for(int j = 0; j < values.size(); j++){
								if(values.get(j).getLabel().equals(answer)){ 	//label exists in table. Add it to list of objects
									Triple temp = values.remove(j);				//must remove element, to update it, then add it back
									ArrayList<String> objects = temp.getObjects();
									objects.add(outlier_name);
									Triple update = new Triple(temp.getLabel(),objects,temp.getQuestionNum());
									values.add(update);							//finally add back the updated triple
								}
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
						}
						/* Here I need to grab the label for the attribute. Need to check label table for:
						 * the existance of the attribute. If it exists, add the label to the vector of labels already seen
						 * Otherwise, add an entry pair <feature, vec:labels>
						 */
						cur_cluster = full_cluster; //make the current cluster the previous current cluster minus the outliers
						att_from_above = ask_free_resp("What " , clusterAttribute);
						boolean extant = false;
						//store answer as label
						values = labelTable.get(curContextPair); //overwriting here?
						for(int i = 0; i < values.size() & !extant; i++){
							if(values.get(i).getLabel().equals(att_from_above)){ //label exists in table. Add it to list of objects
								Triple temp = values.remove(i);					//must remove element, to update it, then add it back
								ArrayList<String> objects = temp.getObjects();
								objects.add(outlier_name);
								Triple update = new Triple(temp.getLabel(),objects,temp.getQuestionNum());
								values.add(update);								//finally add back the updated triple
								extant = true;
							}
						}
						if(!extant){											//label doesn't exist in table. Create it
							Triple labelTriple = new Triple(att_from_above, cur_cluster, questions_asked);
							//add to list or put in table??
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

			//get next cluster
			if(!req_sent){
				writeRequestFile(1,att_from_above);
				req_sent = false;
			}
			while(!responseFileExists()){										//wait for Java to respond with updated cluster
				try {
				    Thread.sleep(100);
				} catch(InterruptedException ex) {
				    Thread.currentThread().interrupt();
				}
			}
			readResponseFile();													//grab next cluster if successful
			req_sent = false;
		}
	}


}