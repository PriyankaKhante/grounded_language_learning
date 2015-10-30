package features;

import java.util.ArrayList;

public interface IClassLabelFunction {

	public String classValue(String object_id);
	public ArrayList<String> getValueSet();
	public int getActualClass(String object_id);
	
}
