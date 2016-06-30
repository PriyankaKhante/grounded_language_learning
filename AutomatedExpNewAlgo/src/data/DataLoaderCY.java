package data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

import utils.CombinationGenerator;
import features.IClassLabelFunction;

public class DataLoaderCY {

	public ArrayList<String> getObjectsInData( ArrayList<InteractionTrial> data){
		ArrayList<String> objects = new ArrayList<String>();
		for (int i = 0; i < data.size(); i ++){
			if (!objects.contains(data.get(i).object))
				objects.add(data.get(i).object);
		}
		return objects;
	}
	
	public ArrayList<String> getObjectsFromCategoryInData(String category, ArrayList<InteractionTrial> data, IClassLabelFunction LF_i){
		ArrayList<String> objects = new ArrayList<String>();
		for (int i = 0; i < data.size(); i ++){
			if ( LF_i.classValue(data.get(i).object).equals(category)){
				if (!objects.contains(data.get(i).object))
					objects.add(data.get(i).object);
			}
		}
		return objects;
	}
	
	public ArrayList<InteractionTrial> getTestSetForObjectCV(ArrayList<InteractionTrial> trials, String test_object){
		ArrayList<InteractionTrial> subset = new ArrayList<InteractionTrial>();
		
		for (int i = 0; i < trials.size(); i++){
			if (trials.get(i).getObject().equals(test_object))
				subset.add(trials.get(i));
		}
		return subset;
	}
	
	public ArrayList<InteractionTrial> getTrainSetForObjectCV(ArrayList<InteractionTrial> trials, String test_object){
		ArrayList<InteractionTrial> subset = new ArrayList<InteractionTrial>();
		
		for (int i = 0; i < trials.size(); i++){
			if (!trials.get(i).getObject().equals(test_object))
				subset.add(trials.get(i));
		}
		return subset;
	}
	
	//gives a random train set for category recognition assuming object-based cross validation
	public ArrayList<String> getRandomTrainObjectSet(ArrayList<String> objects, ArrayList<String> test_objects){
		
		ArrayList<String> train_objects = objects;
		
		for (int j = 0; j < test_objects.size(); j++){
			for(int k = 0; k < train_objects.size(); k++){
				if (train_objects.get(k).equals(test_objects.get(j)))
					train_objects.remove(test_objects.get(j));
			}
		}
		
		return train_objects;
	}
	
	// Always make the testing set first before calling train objects
	//gives a random test set for category recognition assuming object-based cross validation
	public ArrayList<String> getRandomTestObjectSet(ArrayList<String> objects, int random_seed){
		
		Random r = new Random(random_seed);
		
		ArrayList<String> test_objects = new ArrayList<String>();
		
		for(int i=0; i < 5; i++){
			int test_object = r.nextInt(objects.size()-1);
			if(!test_objects.contains(objects.get(test_object)))
				test_objects.add(objects.get(test_object));
			else
				i--;
		}
		
		return test_objects;
	}
	
	public int [] getRandomCombination(CombinationGenerator CG, int seed){
		int total = CG.getTotal().intValue();
		
		Random r = new Random(seed);
		int chosen = r.nextInt(total);
		
		int c = 0;
		while (true){
			if (c==chosen)
				return CG.getNext();
			else {
				CG.getNext();
				c++;
			}
		}
	}
	
	public ArrayList<InteractionTrial> getTrialsWithObjects(ArrayList<InteractionTrial> trials, ArrayList<String> objects){
		ArrayList<InteractionTrial> subset = new ArrayList<InteractionTrial>();
		for (int i = 0; i < trials.size(); i++){	
			if (objects.contains(trials.get(i).getObject()))
				subset.add(trials.get(i));
		}
		
		return subset;
	}
	
	public ArrayList<InteractionTrial> getTrialsWithObject(ArrayList<InteractionTrial> trials, String object){
		ArrayList<InteractionTrial> subset = new ArrayList<InteractionTrial>();
		for (int i = 0; i < trials.size(); i++){	
			if (trials.get(i).getObject().equals(object))
				subset.add(trials.get(i));
		}
		return subset;
	}
	
	public ArrayList<String> removeObjectsFromList(ArrayList<String> objects, ArrayList<String> objects_to_remove){
		ArrayList<String> subset = new ArrayList<String>();
		
		for (int i = 0; i < objects.size(); i++)
			if (!objects_to_remove.contains(objects.get(i)))
				subset.add(objects.get(i));
		
		return subset;
	}
	
	public ArrayList<InteractionTrial> getTestSetForTestTrial(ArrayList<InteractionTrial> trials, int test_trial){
		ArrayList<InteractionTrial> subset = new ArrayList<InteractionTrial>();
		
		for (int i = 0; i < trials.size(); i++){
			if (trials.get(i).getTrial() == test_trial)
				subset.add(trials.get(i));
		}
		return subset;
	}
	
	public ArrayList<InteractionTrial> getTrainSetForTestTrial(ArrayList<InteractionTrial> trials, int test_trial){
		ArrayList<InteractionTrial> subset = new ArrayList<InteractionTrial>();
		
		for (int i = 0; i < trials.size(); i++){
			if (trials.get(i).getTrial() != test_trial)
				subset.add(trials.get(i));
		}
		return subset;
	}
	
	public ArrayList<InteractionTrial> generateTrials(String object, int num_exec){
		ArrayList<InteractionTrial> trials = new ArrayList<InteractionTrial>();
		for (int e = 1; e <= num_exec; e++)
			trials.add(new InteractionTrial(object,e));

		return trials;
	}
	
	public ArrayList<InteractionTrial> generateTrials(ArrayList<String> objects, int num_exec){
		ArrayList<InteractionTrial> trials = new ArrayList<InteractionTrial>();
		for (int i = 0; i <objects.size();i++)
			for (int e = 1; e <= num_exec; e++)
				trials.add(new InteractionTrial(objects.get(i),e));
	
		return trials;
	}

	public ArrayList<String> getObjectList(){
		ArrayList<String> objects = new ArrayList<String>();
		
		try {
			BufferedReader BR = new BufferedReader(new FileReader(new File("/home/priyanka/Documents/extracted_feature_vectors/object_list.csv")));
			
			while (true){
				String line = BR.readLine();
				if (line == null)
					break;
				
				StringTokenizer st = new StringTokenizer(line);
				objects.add(st.nextToken());
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return objects;
	
	}
}
