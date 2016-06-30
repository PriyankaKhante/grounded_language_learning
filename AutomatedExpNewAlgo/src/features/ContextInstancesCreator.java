package features;

import java.util.ArrayList;

import data.InteractionTrial;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import features.ContextFeatureData;

public class ContextInstancesCreator {

	ContextFeatureData CD;
	IClassLabelFunction LF;
	
	Instances dataHeader;
	
	public void setData(ContextFeatureData d){
		CD = d;
	}
	
	public void setLabelFunction(IClassLabelFunction lf_i){
		LF=lf_i;
	}
	
	public Instances getHeader(){
		return dataHeader;
	}

	public Instances generateFullSet(ArrayList<InteractionTrial> trials){
		Instances data = new Instances(dataHeader);
		//System.out.println("Trials size: " + trials.size());
		for (int i = 0; i <trials.size(); i++){
			//System.out.println("Info : " + trials.get(i).getObject()+  "   " + trials.get(i).getTrial());
			Instance inst = this.generateInstance(trials.get(i).getObject(), trials.get(i).getTrial());
			if (inst != null)
				data.add(inst);
		}
	
		return data;
	}
	
	public Instances getDataForObject(String object, int num_exec){
		Instances data = new Instances(dataHeader);
		for (int i = 1; i <= num_exec; i++){
			Instance inst = this.generateInstance(object, i);
			if (inst != null)
				data.add(inst);
		}	
		return data;
	}
	
	public Instance getAverageDataForObject(String object, int num_exec){
		double [] sum_f = new double[CD.get_features(object, 1).length];
		for (int i = 1; i <= num_exec; i++){
			double[] f = CD.get_features(object, i);
			if (f != null){
				for(int j=0;j<f.length;j++){
					sum_f[j] = sum_f[j] + f[j];
				}
			}
		}
		
		if(sum_f != null){
			for(int j=0;j<sum_f.length;j++){
				sum_f[j] = sum_f[j]/num_exec;
			}
		
			// Should set the attribute (property) of the object as the class value
			String class_val = LF.classValue(object);
			//System.out.println("Class val: " + class_val);
			Instance inst = new Instance(dataHeader.numAttributes());
			inst.setDataset(dataHeader);
			for (int i = 0; i < sum_f.length; i++)
				inst.setValue(i, sum_f[i]);
			
			inst.setValue(dataHeader.classAttribute(), class_val);
			
			return inst;
		}
		else return null;//in case data is missing for this point
	}
	
	public Instances generateAveragedFullSet(ArrayList<String> objects, int num_exec){
		Instances data = new Instances(dataHeader);
		
		for (int i = 0; i <objects.size(); i++){
			//System.out.println("Info : " + trials.get(i).getObject()+  "   " + trials.get(i).getTrial());
			Instance inst = this.getAverageDataForObject(objects.get(i), num_exec);
			if (inst != null)
				data.add(inst);
		}
	
		return data;
	}
	
	/*public Instances generateTestSetForObject(String object, int num_exec, int test_exec){
		Instances data = new Instances(dataHeader);
		Instance inst = this.generateInstance(object, test_exec);
		if (inst != null)
			data.add(inst);
		return data;
	}
	
	public Instances generateTrainSetOR(ArrayList<String> object_set, int num_exec, int test_exec){
		Instances data = new Instances(dataHeader);
		
		for (int i = 0; i < object_set.size(); i++){
			for (int t = 1; t <=num_exec; t++){
				if (t != test_exec){
					Instance inst = this.generateInstance(object_set.get(i), t);
					if (inst != null)
						data.add(inst);
				}
			}
		}
		return data;
	}
	
	public Instances generateTestSetOR(ArrayList<String> object_set, int num_exec, int test_exec){
		Instances data = new Instances(dataHeader);
		
		for (int i = 0; i < object_set.size(); i++){
			for (int t = 1; t <=num_exec; t++){
				if (t == test_exec){
					Instance inst = this.generateInstance(object_set.get(i), t);
					if (inst != null)
						data.add(inst);
				}
			}
		}
		return data;
	}
	
	public Instances generateFullSetNoClass(ArrayList<String> object_set, int num_exec){
		Instances data = new Instances(dataHeader);
		
		for (int i = 0; i < object_set.size(); i++){
			for (int t = 1; t <=num_exec; t++){
				Instance inst = this.generateInstance(object_set.get(i), t);
				if (inst != null){
					inst.setClassMissing();
					data.add(inst);		
				}
			}
		}
		return data;
	}

	public Instances generateFullSet(ArrayList<String> object_set, int num_exec){
		Instances data = new Instances(dataHeader);
		
		for (int i = 0; i < object_set.size(); i++){
			for (int t = 1; t <=num_exec; t++){
				Instance inst = this.generateInstance(object_set.get(i), t);
				if (inst != null)
					data.add(inst);
			}
		}
		return data;
	}*/
	
	public Instance generateInstance(String object_id, int trial){
		double [] f = CD.get_features(object_id, trial);
		if (f != null){
			// Should set the attribute (property) of the object as the class value
			String class_val = LF.classValue(object_id);
			//System.out.println("Class val: " + class_val);
			Instance inst = new Instance(dataHeader.numAttributes());
			inst.setDataset(dataHeader);
			for (int i = 0; i < f.length; i++)
				inst.setValue(i, f[i]);
			
			inst.setValue(dataHeader.classAttribute(), class_val);
			
			return inst;
		}
		else return null;//in case data is missing for this point
	}
	
	public int generateHeader(){
		FastVector attrInfo = new FastVector();
		
		String context_name = CD.getName();
		//System.out.println("CD: " + CD.getDim());
		for (int j = 0; j < CD.getDim()-1; j++){
			//System.out.println("@attribute a"+ j + " numeric");
			Attribute a = new Attribute(new String(context_name +"_a"+ j));
			attrInfo.addElement(a);
		}
		
		// Create the nominal attribute to add to the header
		FastVector objValues = new FastVector();
		ArrayList<String> o_vals = LF.getObjectSet();
		for (int i = 0; i < o_vals.size(); i ++){
			objValues.addElement(o_vals.get(i));
		}
		
		Attribute objAttr = new Attribute("objects",objValues);
		attrInfo.addElement(objAttr);
		
		// Create a class attribute to add to the header
		FastVector classValues = new FastVector();
		ArrayList<String> c_vals = LF.getValueSet();
		for (int i = 0; i < c_vals.size(); i ++){
			if(!classValues.contains(c_vals.get(i)))
				classValues.addElement(c_vals.get(i));
		}
		
		Attribute classAttr = new Attribute("class",classValues);
		attrInfo.addElement(classAttr);
		
		dataHeader = new Instances("data", attrInfo, 0);
		//System.out.println("Nominal Attr: " + dataHeader.attribute(dataHeader.numAttributes()-2));
		//System.out.println("Class: " + dataHeader.attribute(dataHeader.numAttributes()-1));
		dataHeader.setClassIndex(dataHeader.numAttributes()-1);
		
		return 1;
	}	
}
