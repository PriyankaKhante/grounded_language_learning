package experiments;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.File;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.ArrayList;
import java.io.FileReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystems;
import java.io.IOException;
import GroundedLanguage.*;

import org.supercsv.io.*;  
import org.supercsv.prefs.CsvPreference;

/* This class uses cluster data as input to facilitate automated question answering
 * A provided grounded truth table will allow for lookups for a corresponding question
 *
 * output is a table:
 | context | attribute | Label | object_id | Questions |
 _______________________________________________________
 where 'object_id' holds a list of object ids that are 'label' in 'attribute'
 Questions holds the number of questions required to obtain this pairing
 */

//************** TODO:
// 2) .CSV creation is off for some reason
// 3) writeQuestionCountTableToFile() is not printing the .csv file
// 4) QuestionCount is totally off
//********************

public class GroundedAutoQuestion {
	public static final String filePath = "/home/users/pkhante/Pictures/grounded_learning_images/";
	//public static final String reqFilePath	= "/home/users/pkhante/Desktop/";
	public static final String reqFilePath	= "/Users/Priyanka/Desktop/";
	public static final String responseName	= "groundedResponse.txt";
	public static final String requestName	= "groundedRequest.txt";

	static String modality;
	static String old_mod = "";
	static ArrayList<String> cur_cluster;
	static String clusterAttribute;
	static int clusterNum; //is this needed?
	static boolean firstTime;
	static HashMap<String, HashMap<String, String> > groundTruthTable;
	static HashMap<String, Integer> questionCountPerContext = new HashMap<String, Integer>();
	static HashMap<Pair, ArrayList<Triple> > labelTable = new HashMap<Pair, ArrayList<Triple> >();
	static ArrayList<String> outliers = new ArrayList<String>();
	static int questionCount = 0;
    static int questions_per_label = 0;

	public static void main(String[] args) {
		loadGroundTruthTable();
		// Create a first request file
		writeRequestFile(1,"","");
		sequence();
	}

	public GroundedAutoQuestion(){
		cur_cluster = new ArrayList<String>();
	}

	/*
	 * Outputs a convoluted table in hashmaps after the input of the grounded truth.
	 * Maps: obj_name -> attribute_name (eg color) -> value
	 * The following attributes have numerical output: height weight and width
	 */
	static void loadGroundTruthTable(){
		try{
			BufferedReader in = new BufferedReader(new FileReader("/Users/Priyanka/Documents/grounded_language_learning/AutoQuestionAnswer/src/etc/ground_truth_table.csv"));
			//BufferedReader in = new BufferedReader(new FileReader("/home/users/pkhante/grounded_language_learning/AutoQuestionAnswer/src/etc/ground_truth_table.csv"));
			String attributes[] = new String[9];
			//grab all the attribute names. Note that these should match that is output
			//by the modified weka program
			String line;
			if((line = in.readLine()) != null){
				StringTokenizer tokenizer = new StringTokenizer(line,",");
				int columnNum = 1;

				while (tokenizer.hasMoreTokens()){
					attributes[columnNum-1] = tokenizer.nextToken();
					columnNum++;
				}
			}
			groundTruthTable = new HashMap<String, HashMap<String, String> >();
			//build the rest of the table
			while((line = in.readLine()) != null){
				StringTokenizer tokenizer = new StringTokenizer(line,","); 
				int columnNum = 1;
				HashMap<String,String> objectTruthTable = new HashMap<String,String>();

				String name = tokenizer.nextToken();
				while (tokenizer.hasMoreTokens()){ 
					objectTruthTable.put(attributes[columnNum], tokenizer.nextToken());
					columnNum++;
				}
				groundTruthTable.put(name,objectTruthTable);
			}
		 
			//System.out.println(groundTruthTable.get("red_tall_cup").get("height").toString());
		  in.close();
		} catch(IOException e){
 		 e.printStackTrace();
		}
		//return groundTruthTable;
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
	public static boolean writeRequestFile(int ID, String object_name, String label){
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

	/*
	 * Reads the response file from external program
	 * IDs : N/A
	 * Expected format: 
	 	behavior_modality
	 	cluster_num
	 	object names
	 */

	public static boolean readResponseFile(){
		String line;
		int lineNum = 0;
		int ID;
		ArrayList<String> objects = new ArrayList<String>();
		String fullPath = (reqFilePath + responseName);
		try{
			BufferedReader myfile = new BufferedReader(new FileReader("/Users/Priyanka/Desktop/groundedResponse.txt"));
			//BufferedReader myfile = new BufferedReader(new FileReader("/home/users/pkhante/Desktop/groundedResponse.txt"));
			if(myfile != null){
				while((line = myfile.readLine()) != null){
					if(!(line.equals("EndOfAllModalities"))){
						if(lineNum == 0){
							modality = line;
							//System.out.println("Modality: " + modality);
							int first = modality.compareTo(old_mod);
							//System.out.println("Old_mod: " + old_mod);
							old_mod = modality;
							//System.out.println("Value of first: " + first);
							if(first == 10){
								firstTime = true;
								//Store the previous questionCount and reset it for the next context
								if(questionCount != 0)
									questionCountPerContext.put(old_mod, questionCount);
								questionCount = 0;
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
		//Increment the questions asked by 1 every time this method is called
		questionCount++;
		questions_per_label++;
		if(question.equals("Please specify what general attribute is described by ")){
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
				return "deformable";
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
		* Here its being asked what label is held by this attirbute (modality) by the group of objects
		* or asking what label is held by the single outlier. In both cases the first object in the group
		* has the desired label
		*/
		if(question.equals("What/how ") || question.equals("What is the ") || question.equals("What ")){
			HashMap<String, String> objectLabels = groundTruthTable.get(cur_cluster.get(0));
			System.out.println("**********Label***************: " +objectLabels.get(modality));
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
			System.out.println("**********OUTLIERS***************: " +outliers.toString());
			return result;
		}
		return null;
	}
	/*
	 * Returns one of the options given. 
	 * op1 is always 'no' or '>2'.
	 * op2 is always 'yes' or '1 or 2'
	 */
	public static boolean ask_mult_choice(String question, String modality, String op1, String op2){
		//Increment the questions asked by 1 every time this method is called
		questionCount++;
		questions_per_label++;
		if(question.equals("Are all of these objects similar in ")){
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
		}
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
				outliers.clear();
		
				for(Map.Entry<String, ArrayList<String> > freqLabel : frequencyList.entrySet()){
					if(!freqLabel.getKey().equals(max_label)){
						for(int k = 0; k < freqLabel.getValue().size(); k++)
							outliers.add(freqLabel.getValue().get(k));
					}
				}
				System.out.println("******Added outliers********: " + outliers.size());
			}
		}
		return false;
	}

	public static void sequence(){

		while(!responseFileExists()){		//wait for weka to respond with updated cluster
			try {
				    Thread.sleep(100);
				} catch(InterruptedException ex) {
				    Thread.currentThread().interrupt();
				}
		}
		readResponseFile();
		String att_from_above = "";
		boolean req_sent = false;
		Pair curContextPair = null;
		ArrayList<Triple> value = null;
		questions_per_label = 0;
		while(true){
			if(firstTime){
				firstTime = false;
				String modality_copy = modality;

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
				System.out.println("Current context pair: " + modality + "   "+  feature_vec.get(0));
				value = new ArrayList<Triple>();
				labelTable.put(curContextPair, value);
				System.out.println("Size of label table at the beginning: " + labelTable.size());

				clusterAttribute = feature_vec.get(0);
				//System.out.println("*********CLUSTER ATTRIBUTE********** : " + clusterAttribute);
			}
			boolean common_att = ask_mult_choice("Are all of these objects similar in ", clusterAttribute, "No", "Yes");
			if(!common_att){ //user input 'Yes' to "Any attributes common to all objects?"

					//if context and general attribute are not exclusive, should break it apart like below
					//otherwise there can only be one attribute that matches, which has to exist because its whats being analyzed in the current
					//cycle.
				    ArrayList<Triple> temp = labelTable.get(curContextPair);
					att_from_above = ask_free_resp("What/how " , clusterAttribute);
					//issue here. labelTriple is fine but labelTable expects a mapping to an arraylist of entries.
					//should probably just add labelTriple to an arraylist and add mapping once questions are over for this modality
					Triple labelTriple = new Triple(att_from_above, cur_cluster, questions_per_label);
					temp.add(labelTriple);
					labelTable.put(curContextPair,temp);
					System.out.println("Cur_Cluster when all labels are same: " + cur_cluster.toString());
					//System.out.println("Size of label table in !done: " + labelTable.size());
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
						}
						/* Here I need to grab the label for the attribute. Need to check label table for:
						 * the existance of the attribute. If it exists, add the label to the vector of labels already seen
						 * Otherwise, add an entry pair <feature, vec:labels>
						 */
						
						cur_cluster = full_cluster; //make the current cluster the previous current cluster minus the outliers
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
	
	public static void writeLabelTableToFile(HashMap<Pair, ArrayList<Triple> > labelTable){
		System.out.println("The number of items in LabelTable: "+labelTable.size()); 
		try{
			 PrintWriter writer = new PrintWriter("/Users/Priyanka/Documents/grounded_language_learning/AutoQuestionAnswer/src/etc/LabelTable.csv", "UTF-8");
		     writer.println("Context,GeneralAttribute,Label,Objects,QuestionCount");
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
		        			 output.append(objects.get(j) + "\",");
		        		 else
		        			 output.append(objects.get(j) + ",");
		        	 }
		        	 String questionCount = Integer.toString(entry.getValue().get(i).getQuestionNum());
		        	 System.out.println("QuestionCount: " + questionCount + "\n");
		        	 output.append(questionCount);
		        	 writer.println(output);
		          }
		     }
			 writer.close();
		}
		catch(IOException e){
			 e.printStackTrace();
		}
		 
		 /*try (ICsvListWriter listWriter = new CsvListWriter(output, CsvPreference.STANDARD_PREFERENCE)){
		     for (Entry<Pair, ArrayList<Triple>> entry : labelTable.entrySet()){
		          listWriter.write(entry.getKey(), entry.getValue());
		     }
		     
		     //PrintWriter writer = new PrintWriter("/etc/LabelTable.csv", "UTF-8");
		     PrintWriter writer = new PrintWriter("/Users/Priyanka/Documents/LabelTable.csv", "UTF-8");
		     writer.println("Context,GeneralAttribute,Label,Objects,QuestionCount");
		     writer.println(output);
		     writer.close();
		 }
		 catch(IOException e){
			 e.printStackTrace();
		 }*/
	}
	
	public static void writeQuestionCountTableToFile(){
		 StringWriter output = new StringWriter();
		 try (ICsvListWriter listWriter = new CsvListWriter(output, CsvPreference.STANDARD_PREFERENCE)){
		      for (Entry<String, Integer> entry : questionCountPerContext.entrySet()){
		          listWriter.write(entry.getKey(), entry.getValue());
		      }
		      
		      PrintWriter writer = new PrintWriter("/Users/Priyanka/Documents/grounded_language_learning/AutoQuestionAnswer/src/etc/QuestionCountTable.csv", "UTF-8");
		      writer.println("Context,TotalQuestionCount");
		      writer.println(output);
		      writer.close();
		 }
		 catch(IOException e){
			 e.printStackTrace();
		 } 
	}
}

