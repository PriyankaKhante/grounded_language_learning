package features;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ClassLabelID implements IClassLabelFunction {

	ArrayList<String> values;
	HashMap<String, String> combination;
	ArrayList<String> classes = new ArrayList<String> ();
	
	// This method is alternatively used to set attribute labels as a class attribute for classification purposes
	// NOTE: For this particular experiment, attributes of the objects are class attributes and not the object names
	public ClassLabelID(ArrayList<String> objects, HashMap<String, String> pairs){
		values=objects;
		combination = pairs;
	}
	
	public String objectValue(String object_id){
		return object_id;
	}
	
	// The following method is modified for this particular experiment
	public String classValue(String object_id) {
		return combination.get(object_id);
	}

	public ArrayList<String> getObjectSet() {
		return values;
	}
	
	// The following method is modified for this particular experiment
	public ArrayList<String> getValueSet(ArrayList<String> training_objects) {
		ArrayList<String> values = new ArrayList<String>();
		for(int i=0;i<training_objects.size(); i++){
			for (Map.Entry<String, String> entry : combination.entrySet()) {
		        if(entry.getKey().equals(training_objects.get(i))) {
		        	values.add(entry.getValue());
		        	break;
		        }
			}
		}
		return values;
	}
	
	// The following method is added as we have duplicate values in the actual value set
	public ArrayList<String> getUniqueValueSet(ArrayList<String> training_objects){
		ArrayList<String> values = new ArrayList<String>();
		for(int i=0;i<training_objects.size(); i++){
			for (Map.Entry<String, String> entry : combination.entrySet()) {
		        if(entry.getKey().equals(training_objects.get(i))) {
		        	values.add(entry.getValue());
		        	break;
		        }
			}
		}
		
		ArrayList<String> uniqueValues = new ArrayList<String>();
		
		for (int i = 0; i < values.size(); i ++){
			if(!uniqueValues.contains(values.get(i)))
				uniqueValues.add(values.get(i));
		}
		
		return uniqueValues;
	}

	public int getActualClass(String object_id) {
		return values.indexOf(this.classValue(object_id));
	}

	public int getNumClasses(){
		for(int i = 0; i<values.size(); i++){
			if(!(classes.contains(combination.get(values.get(i)))))
				classes.add(combination.get(values.get(i)));
		}
		return classes.size();
	}
	
	public int getUniqueClassIndex(String className){
		return classes.indexOf(className);
	}
}
