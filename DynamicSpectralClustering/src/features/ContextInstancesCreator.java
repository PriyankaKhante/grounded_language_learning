package features;

import java.util.ArrayList;

import data.InteractionTrial;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import features.ContextFeatureData;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.PrincipalComponents;

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
	
	public Instances generateFullSet(ArrayList<InteractionTrial> trials) throws Exception{
		Instances data = new Instances(dataHeader);
		for (int i = 0; i <trials.size(); i++){
			Instance inst = this.generateInstance(trials.get(i).getObject(), trials.get(i).getTrial());
			if (inst != null)
				data.add(inst);
		}
		//Uncomment the following if you want to do PCA on the data
		//Instances PCA_data = this.performPCA(data);
		//return PCA_data;
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
	
	public Instances performPCA(Instances data) throws Exception{
		PrincipalComponents pca = new PrincipalComponents();
		//System.out.println("Number of attr: "+CD.getDim());
		pca.setMaximumAttributeNames(CD.getDim());
		pca.setInputFormat(data);
		Instances PCA_data = Filter.useFilter(data, pca);
		//System.out.println("PCA_Data: "+PCA_data);
		return PCA_data;
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
			String class_val = LF.classValue(object_id);
			
			Instance inst = new Instance(dataHeader.numAttributes());
			inst.setDataset(dataHeader);
			for (int i = 0; i < f.length; i++)
				inst.setValue(i, f[i]);
			
			inst.setValue(dataHeader.classAttribute(), class_val);
			
			return inst;
		}
		else return null;//in case data is missing for this point
	}
	
	public void generateHeader(){
		
		FastVector attrInfo = new FastVector();
		
		String context_name = CD.getName();
		for (int j = 0; j < CD.getDim(); j++){
			Attribute a = new Attribute(new String(context_name +"_a"+ j));
			attrInfo.addElement(a);
		}
		
		FastVector classValues = new FastVector();
		ArrayList<String> c_vals = LF.getValueSet();
		for (int i = 0; i < c_vals.size(); i ++){
			classValues.addElement(c_vals.get(i));
		}
		Attribute classAttr = new Attribute("class",classValues);
		attrInfo.addElement(classAttr);
		
		dataHeader = new Instances("data", attrInfo, 0);
		dataHeader.setClassIndex(dataHeader.numAttributes()-1);
	}
	
}
