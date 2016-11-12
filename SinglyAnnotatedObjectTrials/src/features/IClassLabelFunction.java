package features;

import java.util.ArrayList;

public interface IClassLabelFunction {

	public String classValue(String object_id);
	public String objectValue(String object_id);
	public ArrayList<String> getValueSet(ArrayList<String> objects);
	public ArrayList<String> getObjectSet();
	public ArrayList<String> getUniqueValueSet(ArrayList<String> objects);
	public int getActualClass(String object_id);
	public int getNumClasses();
	public int getUniqueClassIndex(String className);
}