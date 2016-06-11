package categorization;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;

import utils.UtilsJS;
import weka.core.Utils;

public class GenericSimDB {
	
	String tag;
	ArrayList<String> IDs;
	double [][] simMatrix;
	
	HashMap<String,Double> hm;
	boolean hashed;
	
	public GenericSimDB(){
		hashed=false;
	}
	
	public void makeHashMap(){
		hm = new HashMap<String,Double>();
		for (int i = 0; i < simMatrix.length; i ++){
			for (int j = 0; j < simMatrix.length; j ++){
				String key1 = new String(IDs.get(i)+"_"+IDs.get(j));
				hm.put(key1, new Double(simMatrix[i][j]));
				
				String key2 = new String(IDs.get(j)+"_"+IDs.get(i));
				hm.put(key2, new Double(simMatrix[i][j]));
			}
		}
		hashed=true;
	}
	
	public GenericSimDB(ArrayList<String> id_list){
		IDs = id_list;
		simMatrix = new double[IDs.size()][IDs.size()];
		for (int i = 0; i < simMatrix.length; i ++){
			for (int j = 0; j < simMatrix.length; j ++){
				simMatrix[i][j]=0.0;
			}
		}
		hashed=false;
	}
	
	public void addValue(int i, int j, double val){
		simMatrix[i][j]+=val;
	}
	
	public void setEntry(int i, int j, double val){
		simMatrix[i][j]=val;
	}
	
	public ArrayList<String> getIDs() {
		return IDs;
	}                                      
	
	public void setIDs(ArrayList<String> ds) {
		IDs = ds;
	}
	
	public double[][] getMatrix() {
		return simMatrix;
	}
	
	
	
	public double getValue(int i, int j){
		return simMatrix[i][j];
	}
	
	public double getValue(String id1, String id2){
		return this.getValue(IDs.indexOf(id1), IDs.indexOf(id2));
	}
	
	public void setMatrix(double[][] matrix) {
		this.simMatrix = new double[matrix.length][matrix[0].length];
		for (int i = 0; i < simMatrix.length; i++){
			for (int j = 0; j < matrix[i].length;j++)
				simMatrix[i][j]=matrix[i][j];
		}            
			
			
		
	}
	
	public void scaleMinMax(){
		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;
		
		for (int i = 0; i < simMatrix.length; i++){
			for (int j = i+1; j < simMatrix.length; j++){
				double v = (simMatrix[i][j]);
				if (v > max)
					max = v;
				if (v < min)
					min = v;
			}
		}
		
		//System.out.println("Max:\t"+max+"\tMin:\t"+min);
		
		for (int i = 0; i < simMatrix.length; i++){
			for (int j = i+1; j < simMatrix.length; j++){
				simMatrix[i][j]=(simMatrix[i][j]-min)/(max-min);
				simMatrix[j][i]=(simMatrix[j][i]-min)/(max-min);
			}
		}
	}
	
	public double [] getMinAndMax(){
		double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;
		
		for (int i = 0; i < simMatrix.length; i++){
			for (int j = i+1; j < simMatrix.length; j++){
				double v = (simMatrix[i][j]);
				if (v > max)
					max = v;
				if (v < min)
					min = v;
			}
		}
		
		double [] r = {min,max};
		return r;
	}
	
	public double [] getValuesNoDiagonal(){
		ArrayList<Double> values = new ArrayList<Double>();
		for (int i = 0; i < simMatrix.length; i++){
			for (int j = i+1; j < simMatrix.length; j++){
				double v = (simMatrix[i][j]);
				values.add(new Double(v));
			}
		}
		
		double [] values_array = new double[values.size()];
		for (int i = 0; i < values.size(); i++)
			values_array[i]=values.get(i).doubleValue();
		return values_array;
	}
	
	public void normalizeNonDiagonal(){
		int numEntries = (int) (simMatrix.length*(simMatrix.length-1))/2;
		double [] values = new double[numEntries];
		int count = 0;
		for (int i = 0; i < simMatrix.length; i++){
			for (int j = i+1; j < simMatrix.length; j++){
				values[count]=simMatrix[i][j];
				count++;
			}
		}
		
		double mean = Utils.mean(values);
		double std = Math.sqrt(Utils.variance(values));
		
		//System.out.println("Mean:\t"+mean+"\tstdev:\t"+std);
		
		/*double max = Double.MIN_VALUE;
		double min = Double.MAX_VALUE;
		
		for (int i = 0; i < simMatrix.length; i++){
			for (int j = i+1; j < simMatrix.length; j++){
				double v = (simMatrix[i][j]-mean)/std;
				simMatrix[i][j]=v;
				simMatrix[j][i]=v;
				
				if (v > max)
					max = v;
				if (v < min)
					min = v;
			}
		}*/
		
		for (int i = 0; i < simMatrix.length; i++){
			for (int j = i+1; j < simMatrix.length; j++){
				simMatrix[i][j]=(simMatrix[i][j])/(std);
				simMatrix[j][i]=(simMatrix[j][i])/(std);
			}
		}
			
	}
	
	public void makeSymetric(){	
		double [][] newMatrix = new double[simMatrix.length][simMatrix.length];
		for (int i = 0; i < simMatrix.length; i ++){
			for (int j = 0; j < simMatrix.length; j ++){
				newMatrix[i][j] = (simMatrix[i][j]+simMatrix[j][i])/2.0;
			}
		}
		
		simMatrix = newMatrix;
	}
	
	public void makeDistanceMatrix(double max){
		for (int i = 0; i < simMatrix.length; i ++){
			for (int j = 0; j < simMatrix.length; j ++){
				if (i==j)
					simMatrix[i][j]=0.0;
				else {
					simMatrix[i][j]=max-simMatrix[i][j];
				}
			}
		}
	}
	
	public void makeSimilarityMatrix(double diagonal_value){
		for (int i = 0; i < simMatrix.length; i ++){
			for (int j = 0; j < simMatrix.length; j ++){
				if (i==j)
					simMatrix[i][j]=diagonal_value;
				else {
					simMatrix[i][j]=1/simMatrix[i][j];
				}
			}
		}
	}
	
	public void multiplyBy(double v){
		for (int i = 0; i < simMatrix.length; i ++){
			for (int j = 0; j < simMatrix.length; j ++){
				simMatrix[i][j]=simMatrix[i][j]*v;
			}
		}
	}
	
	public void divideBy(double v){
		for (int i = 0; i < simMatrix.length; i ++){
			for (int j = 0; j < simMatrix.length; j ++){
				simMatrix[i][j]=simMatrix[i][j]/v;
			}
		}
	}
	
	public void setDiagonal(double v){
		for (int i = 0; i < simMatrix.length; i ++){
			simMatrix[i][i]=v;
		}
	}
	
	/*
	 * returns the mean and variance of all entries in the matrix
	 */
	public double [] getMeanAndVariance(){
		double [] entries = new double[simMatrix.length*simMatrix[0].length];
		int c = 0;
		for (int i = 0; i < simMatrix.length; i ++){
			for (int j = 0; j < simMatrix.length; j ++){
				entries[c]=simMatrix[i][j];
				c++;
			}
		}
		
		double mean = Utils.mean(entries);
		double variance = Utils.variance(entries);
		double [] mv = {mean,variance};
		return mv;
	}
	
	public int getDegreeThreshold(String id, double k_var){
		double [] mv = this.getMeanAndVariance();
		
		int numLinks = 0;
		
		for (int i = 0; i < IDs.size(); i++){
			double s_i = this.getSim(id, IDs.get(i));
			
			
			if (s_i > mv[0]+k_var*mv[1])
				numLinks ++;
		}
		
		return numLinks;
	}
	
	//returns the degree of a node
	public double getDegreeSum(String id){
		double sum = 0;
		for (int i = 0; i < IDs.size(); i++){
			sum+=this.getSim(id, IDs.get(i));
		}
		
		return sum;
	}
	

	public double getSim(String id1, String id2){
		
		if (!hashed){
			if (id1.charAt(id1.length()-1)==' '){
				id1 = new String(id1.substring(0, id1.length()-1));
			}
			if (id2.charAt(id2.length()-1)==' '){
				id2 = new String(id2.substring(0, id2.length()-1));
			}
			
			int index1 = IDs.indexOf(id1);
			int index2 = IDs.indexOf(id2);
			
			return simMatrix[index1][index2];
		}
		else {
			String key1 = new String(id1+"_"+id2);
			return hm.get(key1).doubleValue();
		}
	}
	
	public void printDebug(){
		if (tag!=null){
			System.out.println(tag);
		}
		
		for (int i = 0; i < IDs.size(); i ++){
			System.out.print(IDs.get(i)+" ");
		}
		System.out.println();
		for (int i = 0; i < simMatrix.length; i ++){
			for (int j = 0; j < simMatrix.length; j ++){
				System.out.print(simMatrix[i][j]+"\t");
			}
			System.out.println();
		}
	}
	
	public GenericSimDB getSubsetSimDB(ArrayList<String> subsetIDs){
		GenericSimDB S = new GenericSimDB();
		S.setIDs(subsetIDs);
		S.setMatrix(this.getSubsetSimMatrix(subsetIDs));
		S.setTag("subset matrix");
		return S;
	}
	
	public double [][] getSubsetSimMatrix(ArrayList<String> subsetIDs){
		double [][] m = new double[subsetIDs.size()][subsetIDs.size()];
		
		for (int i = 0; i < subsetIDs.size(); i ++){
			for (int j = i; j < subsetIDs.size(); j ++){
				double s = this.getSim(subsetIDs.get(i), subsetIDs.get(j));
				m[i][j] = s;
				m[j][i] = s;
			}	
		}
		return m;
	}
	
	public void writeToFile(String fileName){
		try {
			FileWriter FW = new FileWriter(fileName);
			for (int i = 0; i < IDs.size(); i ++){
				FW.write(new String(IDs.get(i)+"\t"));
			}
			FW.write("\n");
			
			for (int i = 0; i < simMatrix.length; i ++){
				for (int j = 0; j < simMatrix[i].length; j ++){
					FW.write(new String(simMatrix[i][j]+"\t"));
				}
				FW.write("\n");
			}
			
			FW.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeTagsToFile(String fileName){
		try {
			FileWriter FW = new FileWriter(fileName);
			for (int i = 0; i < IDs.size(); i ++){
				FW.write(new String(""+IDs.get(i)+""));
				FW.write("\n");
			}
			FW.close();
			
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void writeToCSVFile(String fileName){
		try {
			FileWriter FW = new FileWriter(fileName);
		
			
			for (int i = 0; i < simMatrix.length; i ++){
				for (int j = 0; j < simMatrix[i].length; j ++){
					if (j!=simMatrix[i].length-1)
						FW.write(new String(simMatrix[i][j]+","));
					else 
						FW.write(new String(simMatrix[i][j]+"\n"));
				}
				
			}
			FW.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void writeToFileNoTags(String fileName){
		try {
			FileWriter FW = new FileWriter(fileName);
		
			
			for (int i = 0; i < simMatrix.length; i ++){
				for (int j = 0; j < simMatrix[i].length; j ++){
					FW.write(new String(simMatrix[i][j]+"\t"));
					
				}
				FW.write("\n");
			}
			FW.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void loadFromFile(String fileName){
		try {
			FileReader FR = new FileReader(new File(fileName));
			BufferedReader BR = new BufferedReader(FR);
			
			String idsLine = BR.readLine();
			StringTokenizer st = new StringTokenizer(idsLine);
			IDs = new ArrayList<String>();
			while(st.hasMoreTokens()){
				IDs.add(st.nextToken());
			}
			
			simMatrix = new double[IDs.size()][IDs.size()];
			
			for (int i = 0; i < simMatrix.length; i ++){
				String line = BR.readLine();
				//System.out.println(line);
				st = new StringTokenizer(line);
				for (int j = 0; j < simMatrix.length;j ++){
					simMatrix[i][j] = Double.parseDouble(st.nextToken());
				}
			}
			
		} catch (FileNotFoundException e) {
			
			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		}
	}
	
	public void shuffle(Random rGen){
		double [][] newMatrix = new double[simMatrix.length][simMatrix.length];
		
		ArrayList<String> shuffled_ids = UtilsJS.shuffle(this.IDs,rGen);
		
		for (int i = 0; i < shuffled_ids.size(); i ++){
			for (int j = 0; j < shuffled_ids.size(); j++){
				double v_ij = this.getValue(shuffled_ids.get(i), shuffled_ids.get(j));
				newMatrix[i][j]=v_ij;
			}
		}
		
		this.simMatrix=newMatrix;
	}
	
	public void randomize(Random rGen){
		double [][] newMatrix = new double[simMatrix.length][simMatrix.length];
		
		
		double idValue = 0;
		for (int i = 0; i < newMatrix.length; i ++){
			for (int j = i; j < newMatrix.length; j ++){
				if (i==j){
					newMatrix[i][j] = simMatrix[i][j];
					idValue = newMatrix[i][j];
				}
				else {
					newMatrix[i][j] = simMatrix[rGen.nextInt(newMatrix.length)][rGen.nextInt(newMatrix.length)];
					newMatrix[j][i] = newMatrix[i][j];
				}
			}
		}
		
		for (int i = 0; i < newMatrix.length; i ++){
			for (int j = 0; j < newMatrix.length; j ++){
				if (i == j){
					if (newMatrix[i][j] != idValue)
						System.out.println("id mistake.");
					
				}
				else {
					if (newMatrix[i][j] != newMatrix[j][i])
						System.out.println("matrix not symetric.");
				}
			}
		}
		
		simMatrix = newMatrix;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}
	
	public static GenericSimDB average(ArrayList<GenericSimDB> inputs){
		GenericSimDB output = new GenericSimDB(inputs.get(0).getIDs());

		
		double weight = 1.0/(double)inputs.size();
		int size = inputs.get(0).getIDs().size();
		
		for (int i = 0; i < size; i ++){
			for (int j = 0; j < size; j++){
				double value_ij = 0.0;
				
				for (int k = 0; k < inputs.size(); k++){
					value_ij += weight * inputs.get(k).getValue(i, j);
				}
				
				output.setEntry(i, j, value_ij);
			}
		}
		
		
		return output;
	}
}
