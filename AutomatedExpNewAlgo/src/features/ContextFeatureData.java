package features;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import cern.colt.Arrays;
import weka.core.Utils;

public class ContextFeatureData {

	ArrayList<String> tags;
	ArrayList<double[]> feature_vectors;
	ArrayList<String> labels;
	
	String behavior_modality;
	
	int f_dim=-1;
	
	public ContextFeatureData(String bm){
		behavior_modality=bm;
		tags = new ArrayList<String>();
		feature_vectors = new ArrayList<double[]>();
		labels = new ArrayList<String>();
	}
	
	public void setToZeros(){
		ArrayList<double[]> new_features = new ArrayList<double[]>();
		for (int i = 0; i < tags.size(); i++){
				double [] f = feature_vectors.get(i);
				for (int j =0; j < f.length; j++)
						f[j]=0.0;
				
				new_features.add(f);
		}
		
		feature_vectors = new_features;
	}
	
	public boolean hasData(String object, int trial_exec){
		String tag_i = new String(object+"_"+trial_exec);
		if (tags.indexOf(tag_i) != -1)
			return true;
		else return false;
	}
	
	public double [] get_features(String object, int trial_exec){
		String tag_i = new String(object+"_"+trial_exec);
		int index = tags.indexOf(tag_i);
		//System.out.println("Tag_i: " + tag_i + Arrays.toString(feature_vectors.get(index)));
		if (index >= 0)
			return feature_vectors.get(index);
	
		else return null;
	}
	
	public String getLabel(String object, int trial_exec){
		String tag_i = new String(object+"_"+trial_exec);
		int index = tags.indexOf(tag_i);
		if(index >= 0)
			return labels.get(index);
		
		else return null;
	}
	
	public String getName(){
		return new String(behavior_modality);
	}
	
	public int getDim(){
		return f_dim;
	}
	
	public int compute_dim(String line){
		String [] tokens = line.split(",");
		return tokens.length-1;
	}
	
	public void print(){
		for (int i = 0; i <tags.size();i++){
			System.out.print(tags.get(i));
			double [] f_i = feature_vectors.get(i);
			for (int f= 0; f <f_i.length; f++)
				System.out.print(","+f_i[f]);
			System.out.println();
		}
	}
	
	public void loadFromFile(String filename, boolean normalize){
		try {
			BufferedReader BR = new BufferedReader(new FileReader(new File(filename)));
			
			while (true){
				String line = BR.readLine();
			
				if (line == null){
					break;
				}
				
				if (f_dim == -1)
					f_dim = this.compute_dim(line);
				
				String [] tokens = line.split(",");
				
			  //System.out.println(filename+"\t"+line+"\t"+tokens.length);
				String tag = tokens[0];
				double [] f_i = new double[f_dim-1];
				
				for (int i = 0; i <f_i.length; i++)
					if (i+1 < tokens.length)
						f_i[i]=Double.parseDouble(tokens[i+1]);
					else f_i[i]=0.0;
				
				if (normalize && Utils.sum(f_i)>0.0)
					Utils.normalize(f_i);
				
				String label = tokens[f_dim];
				tags.add(tag);
				labels.add(label);
				feature_vectors.add(f_i);
			}
			
			BR.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
