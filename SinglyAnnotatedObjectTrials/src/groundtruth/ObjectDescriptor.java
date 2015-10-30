package groundtruth;

public class ObjectDescriptor {
	// Name of the object
	String object_name;
	// Colour
	String colour;
	//Shape
	String shape;
	//Material
	String material;
	//Has_contents - Empty or filled
	String has_contents;
	//Is deformable? 
	boolean deformable;
	//Size - Large or small
	String size; 
	//Weight - Light or heavy
	String weight;
	
	// Setter methods for all attributes defined above
	public void setObjectName(String object_name){
		this.object_name = object_name;
	}
	
	public void setColour(String colour){
		this.colour = colour;
	}
	
	public void setShape(String shape){
		this.shape = shape;
	}
	
	public void setMaterial(String material){
		this.material = material;
	}
	
	public void setHasContents(String has_contents){
		this.has_contents = has_contents;
	}
	
	public void isDeformable(boolean deformable){
		this.deformable = deformable;
	}
	
	public void setSize(String size){
		this.size = size;
	}
	
	public void setWeight(String weight){
		this.weight = weight;
	}
	
	// Getter methods for all attributes defined above
	public String getObjectName(){
		return object_name;
	}
	
	public String getColour(){
		return colour;
	}
	
	public String getShape(){
		return shape;
	}
	
	public String getMaterial(){
		return material;
	}
	
	public String getHasContents(){
		return has_contents;
	}
	
	public boolean isDeformable(){
		return deformable;
	}
	
	public String getSize(){
		return size;
	}
	
	public String getWeight(){
		return weight;
	}
}

