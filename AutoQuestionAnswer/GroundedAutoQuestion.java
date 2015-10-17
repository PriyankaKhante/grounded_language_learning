import java.util.StringTokenizer;
import java.util.HashMap;
import java.io.BufferedReader;
import java.File;

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
		BufferedReader in = new BufferedReader(new FileReader("etc/groundedtruetable.csv"));
		String attributes[] = new String[8];
		//grab all the attribute names. Note that these should match that is output
		//by the modified weca program
		if(in.hasNextLine()){
			String line = in.readLine();
			StringTokenizer tokenizer = new StringTokenizer(line,",");
			int columnNum = 1;

			while (tokenizer.hasMoreTokens()){
				attributes[columnNum] = tokenizer.nextToken();
				columnNum++;
			}
		}
		HashMap<String, HashMap<String, String> > groundTruthTable = new HashMap<String, HashMap<String, String> >();
		//build the rest of the table
		while(in.hasNextLine()){
			String line = in.readLine();
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
		return true;
	}
	/*
	 * ONLY used for ID 1
	 */
	bool writeRequestFile(int ID, String label){
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
		BufferedReader myfile = new BufferedReader(new FileReader("etc/groundedtruetable.csv"));
		if(myfile != null){
			while(myfile.hasNextLine()){
				line = myfile.nextLine();
				if(lineNum == 0){
					String old_mod = modality;
					modality = line;
					firstTime = compareTo(modality,old_mod);
				}
				else if(lineNum == 1)
					clusterNum = Double.parseDouble(line);
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
			System.out.println("Got: " + cur_cluster.at(i));
		}

		try {
		    Files.delete(fullPath);
			System.out.println("Response file parsed and deleted successfully.");
		} catch (NoSuchFileException x) {
		    System.err.format("Error deleting response file. %s: no such" + " file or directory%n", fullPath);
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


	void sequence(){

		while(!responseFileExists()){		//wait for weca to respond with updated cluster
			sleep(.01);
		}
		readResponseFile();
		String att_from_above;
		boolean req_sent = false;
		while(true){
			if(firstTime){
				firstTime = false;
				String modality_copy = modality;
				String delimiter = "_";
				int index = modality_copy.find(delimiter);
				String action = modality_copy.substr(0, index);
				String modality = modality_copy.substr(index+1, modality_copy.length());
				String resp = ask_free_resp("Please specify what general attribute is described by " + action +"ing the object while recording the " + modality);
				//std::vector<std::string> feature_vec = splitString(resp);
				ArrayList<String> feature_vec = splitString(resp);


				clusterAttribute = feature_vec.at(0);
			}
			boolean common_att = ask_mult_choice("Are all of these objects similar in " + clusterAttribute + "?", "No", "Yes");
			if(common_att){ //user input 'Yes' to "Any attributes common to all objects?"
				//ask only once per tree : "Can you specify that attribute?"
					std::map<std::string, std::vector<std::string> >::iterator it;
					it = label_table.find(clusterAttribute);

					if(it != label_table.end()){
						std::vector<std::string> values = it->second;
						std::vector<std::string> values_buffer; //used to track new labels to be added to values
						bool done = false;
						/*for(int j = 0; j < values.size() && !done; j++){
							int mult_choice_ans = ask_mult_choice("Are they " + values.at(j) + " in " + clusterAttribute + "?",
									"No", "Yes");
							if(mult_choice_ans){
								values_buffer.push_back(values.at(j));
								done = true;
							}
						}*/
						if(!done){
						att_from_above = ask_free_resp("What/how " + clusterAttribute + " are they?");
						values.push_back(att_from_above);
						label_table[clusterAttribute] = values;
						//label_table.insert(std::pair<std::string,std::vector<std::string> >(feature_vec.at(i),
						//		values));	
						}
					}
					else {
						System.out.println("Attribute not found in table.");
						att_from_above = ask_free_resp("What/how " + clusterAttribute + " are they?");
						label_table.insert(std::pair<std::string,std::vector<std::string> >(clusterAttribute,
							splitString(att_from_above)));
					}
			}
			else{
				if(cur_cluster.size() <= 3 || ask_mult_choice("Are most of these objects similar in " + clusterAttribute + "?", "No", "Yes")){
					boolean mult_choice;
					if(cur_cluster.size() <= 3){
						mult_choice = !ask_mult_choice("How many objects don't fit the " + clusterAttribute + "?", "1 or 2");
					}
					else
						mult_choice = ask_mult_choice("How many objects don't fit the " + clusterAttribute + "?", ">2", "1 or 2");
					if(mult_choice){
						ArrayList<String> outliers = splitString(
								ask_free_resp("Please specify the names of the outlier(s), separated by spaces"));
						ArrayList<String> full_cluster = cur_cluster;
						for(int i = 0; i < outliers.size(); i++){
							String outlier_name = full_cluster.at(Integer.parseInt(outliers.at(i)));
							System.out.println("Outlier name: " + outlier_name);
							cur_cluster.clear();
							cur_cluster.add(outlier_name);
							
							//display only outliers.at(i);
							String answer = ask_free_resp("What is the " + clusterAttribute + " of this object?");
							//store answer as label
							//store in outlier map to be combined to any cluster with the same label
							writeRequestFile(0,outlier_name,answer);	//send request to Java prog
							while(!responseFileExists()){		//wait for Java to respond with updated cluster
								sleep(.01);
							}
							readResponseFile();
							sleep(1); //waits for the cv window to update
						}
						/* Here I need to grab the label for the attribute. Need to check label table for:
						 * the existance of the attribute. If it exists, add the label to the vector of labels already seen
						 * Otherwise, add an entry pair <feature, vec:labels>
						 */ 
						att_from_above = ask_free_resp("What " + clusterAttribute + " are these items?");
						std::map<std::string, std::vector<std::string> >::iterator it;
						it = label_table.find(clusterAttribute);
						if(it != label_table.end()){ //att exists
							std::vector<std::string> values = it->second;
							values.push_back(att_from_above);
							label_table[clusterAttribute] = values;
						}
						else{ //doesn't exist
							label_table.insert(std::pair<std::string,std::vector<std::string> >(clusterAttribute,
									splitString(att_from_above)));
						}
						
					}
					else{
						writeRequestFile(2, att_from_above, att_from_above); //recluster
						req_sent = true;
					}
				}
				else{
					writeRequestFile(2, att_from_above, att_from_above); //recluster
					req_sent = true;
				}
			}
			
			print_to_gui("Waiting...");

			//get next cluster
			if(!req_sent){
				writeRequestFile(1,att_from_above);
				req_sent = false;
			}
			while(!responseFileExists()){		//wait for Java to respond with updated cluster
					sleep(.01);
			}
			readResponseFile();				//grab next cluster if successful
			req_sent = false;
		}
	}


}