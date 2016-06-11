package features;

import java.util.ArrayList;

public class ClassLabelID implements IClassLabelFunction {

	ArrayList<String> values;
	
	public ClassLabelID(ArrayList<String> objects){
		values=objects;
	}
	
	public String classValue(String object_id) {
		return object_id;
	}

	public ArrayList<String> getValueSet() {
		return values;
	}

	public int getActualClass(String object_id) {
		return values.indexOf(this.classValue(object_id));
	}

}
