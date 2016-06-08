package features;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ClassLabelID implements IClassLabelFunction {

	ArrayList<String> values;
	HashMap<String, String> combination;
	
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
	public ArrayList<String> getValueSet() {
		ArrayList<String> values = new ArrayList<String> (combination.values());
		return values;
	}

	public int getActualClass(String object_id) {
		return values.indexOf(this.classValue(object_id));
	}

}
