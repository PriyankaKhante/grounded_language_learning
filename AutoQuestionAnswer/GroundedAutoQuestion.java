import java.util.StringTokenizer;
import java.util.HashMap;
import java.io.BufferedReader;


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

	public static void main(String[] args) {

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

	void sequence(){

		while(!responseFileExists()){		//wait for Java to respond with updated cluster
			sleep(.01);
		}
		readResponseFile();
		std::string att_from_above;
		bool req_sent = false;
		while(true){
			if(firstTime){
				firstTime = false;
				std::string modality_copy = modality;
				std::string delimiter = "_";
				int index = modality_copy.find(delimiter);
				std::string action = modality_copy.substr(0, index);
				std::string modality = modality_copy.substr(index+1, modality_copy.length());
				std::string resp = ask_free_resp("Please specify what general attribute is described by " + action +"ing the object while recording the " + modality);
				std::vector<std::string> feature_vec = splitString(resp);
				clusterAttribute = feature_vec.at(0);
			}
			bool common_att = ask_mult_choice("Are all of these objects similar in " + clusterAttribute + "?", "No", "Yes");
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
						ROS_INFO("Attribute not found in table.");
						att_from_above = ask_free_resp("What/how " + clusterAttribute + " are they?");
						label_table.insert(std::pair<std::string,std::vector<std::string> >(clusterAttribute,
							splitString(att_from_above)));
					}
			}
			else{
				if(cur_cluster.size() <= 3 || ask_mult_choice("Are most of these objects similar in " + clusterAttribute + "?", "No", "Yes")){
					bool mult_choice;
					if(cur_cluster.size() <= 3){
						mult_choice = !ask_mult_choice("How many objects don't fit the " + clusterAttribute + "?", "1 or 2");
					}
					else
						mult_choice = ask_mult_choice("How many objects don't fit the " + clusterAttribute + "?", ">2", "1 or 2");
					if(mult_choice){
						std::vector<std::string> outliers = splitString(
								ask_free_resp("Please specify the names of the outlier(s), separated by spaces"));
						std::vector<std::string> full_cluster = cur_cluster;
						for(int i = 0; i < outliers.size(); i++){
							std::string outlier_name = full_cluster.at(atoi(outliers.at(i).c_str()));
							ROS_INFO("Outlier name: %s", outlier_name.c_str());
							cur_cluster.clear();
							cur_cluster.push_back(outlier_name);
							sleep(1);
							//display only outliers.at(i);
							std::string answer = ask_free_resp("What is the " + clusterAttribute + " of this object?");
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