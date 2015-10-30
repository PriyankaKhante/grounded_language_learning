package experiments;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;

import data.DataLoaderCY;
import weka.core.Instances;

public class SinglyAnnotatedObjectEXP {
	static Instances data;
	HashMap<String, HashMap<String, String>> groundTruthTable;
	public static void main(String[] args) {
		//behaviour-modailities to use
		String [] rc_behavior_modalities = {"drop_audio"};
					//,"revolve_audio","push_audio","shake_audio", "hold_haptics","lift_haptics",
					//"press_haptics","squeeze_haptics","grasp_size","look_color","look_shape"};
				
		// Modalities that have been taken out
		// grasp-audio, hold-audio, lift-audio, poke-audio, press-audio, squeeze-audio, drop-haptics,
		// poke-haptics, revolve-haptics, push-haptics, shake-haptics, grasp-haptics
		
		Collections.shuffle(Arrays.asList(rc_behavior_modalities));
		
		
		commenceExperiments();

	}
	
	/*
	 * Outputs a convoluted table in hashmaps after the input of the grounded truth.
	 * Maps: obj_name -> attribute_name (eg color) -> value
	 * The following attributes have numerical output: height weight and width
	 */
	HashMap<String, HashMap<String, String> > loadGroundTruthTable(){
		try{
			BufferedReader in = new BufferedReader(new FileReader("/Users/Priyanka/Downloads/groundTruthTable.csv"));
			String attributes[] = new String[8];
			//grab all the attribute names. Note that these should match that is output
			//by the modified weka program
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
	
	public static void commenceExperiments(){
		// Create a test and train set
		DataLoaderCY DL = new DataLoaderCY();
		ArrayList<String> object_list = DL.getObjectList();
		
		// Train and test sets are created in this way -> generate a set a 10 seeds first
		int[] seeds_array = randomGenerator(78, 1000);
		
		for(int i=0;i<seeds_array.length;i++){
			// Generate the test and training object sets for each seed
			ArrayList<String> test_objects = DL.getRandomTestObjectSet(object_list, seeds_array[i]);
			ArrayList<String> objects = DL.getRandomTrainObjectSet(object_list, test_objects);
			
			//shuffle only the training set 10 times and generate more such trials. 
			for(int j=0;j<10;j++){
				Collections.shuffle(objects);
				
				
			}
		}
	}

	// Method to generate the 10 seeds for trials
	public static int[] randomGenerator(int seed, int max) {
		int[] seeds_array = new int[10];
	    Random generator = new Random(seed);
	    
	    for(int i=0;i<10;i++){
	    	int num = generator.nextInt();
	    	seeds_array[i] = num;
	    }
	    return seeds_array;
	}
}
