package data;

public class InteractionTrial {

	String object;
	int trial;
	
	public InteractionTrial(String o, int t){
		object = o;
		trial = t;
	}
	
	public String getTrialID(){
		return new String(object+"_"+trial);
	}

	public String getObject() {
		return object;
	}

	public void setObject(String object) {
		this.object = object;
	}

	public int getTrial() {
		return trial;
	}

	public void setTrial(int trial) {
		this.trial = trial;
	}
	
}
