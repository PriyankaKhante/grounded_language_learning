package experiments;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import categorization.ClusterDB;
import categorization.ObjectClusterer;

public class GLLUserInterface extends JFrame {
	//public static final String filePath = "/home/users/pkhante/Pictures/grounded_learning_images/";
	public static final String imagePath = "/etc/object_images/";
	public static final String reqFilePath	= "/home/priyanka/Desktop/";
	public static final String responseName	= "groundedResponse.txt";
	public static final String requestName	= "groundedRequest.txt";
	
	// To store the contexts with their specific general attributes
	static HashMap<String, String> contextAttributeTable = new HashMap<String, String>();
	
	static int clusterNum = -10000;
	static String old_mod = "";
	static boolean endOfModality = true;
	static boolean askForOutlierCategories = false;
	static int outlierCategories = -1000;
	
	static String modality;
	static ArrayList<String> cur_cluster;
	
	// Constructor
	public GLLUserInterface(){
		initUI();
		cur_cluster = new ArrayList<String>();
	}
	
	private void initUI(){
		// Create the UI 
		setTitle("Grounded Language Learning User Study");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);     // to close the JFrame
	}
	
	public static void main(String[] args) {
		try{
			final JFrame ui = new GLLUserInterface();
	        ui.setUndecorated(true);	   
	        ui.setExtendedState(JFrame.MAXIMIZED_BOTH); 
				    
		    final JPanel mainPanel = new JPanel();
		    mainPanel.setLayout(new BorderLayout());
		    
		    final JPanel buttonPanel = new JPanel();
		    buttonPanel.setPreferredSize(new Dimension(500, 80));
     
            JLabel info = new JLabel("<html>Read the following instruction properly before proceeding to the experiments:<br><br>"
            		+ "Numerous clusters of objects would be displayed to you. You can do one of the following things, at each point of time:<br>"
            		+ "<ol><li><span> Click “Skip” - If you do not wish to label these cluster of objects with an unifying attribute, you can go ahead and skip to the next one.</span>"
            		+ "<li><span> Click “Label” - If you wish to label the cluster of objects, click this button.</span>"
            		+ "<li><span> Pick outliers - You can check the boxes below the objects to mark them as outliers. In each cluster, you can only mark 1 or 2 outliers. You should only do this, if you want to label the rest of the objects in the cluster after taking out the outliers. If more than 2 outliers exist, “Skip” the cluster.</span></li></ol></html>");
            info.setFont(new Font("Times New Roman", Font.BOLD, 40));
		    
		    JButton beginButton = new JButton("Let's Begin!");
		    beginButton.setFont(new Font("Times New Roman", Font.BOLD, 40));
		    beginButton.setPreferredSize(new Dimension(400,70));
		    beginButton.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	            	// Create a first request file
	    			writeRequestFile(1,",");
	    			try{
		    			checkIfResponseFileExists();
		    			endOfModality = false;
		    			String resp = ask_free_resp("For the forthcoming clusters, what feature/attribute of the object ", modality);
		    			// Start with the experiment
		    			mainPanel.removeAll();
		    			buttonPanel.removeAll();
		            	ui.remove(mainPanel);
		            	
		    			// Create a JLabel and add it to the mainPanel
						JLabel attrLabel = new JLabel(resp);
						attrLabel.setFont(new Font("Times New Roman", Font.BOLD, 40));
					    
					    final JButton nextButton = new JButton("Next");
					    nextButton.setPreferredSize(new Dimension(400,70));
					    nextButton.setFont(new Font("Times New Roman", Font.BOLD, 40));
					    nextButton.setPreferredSize(new Dimension(400,70));
					    
					    // Create the action listener 
					    nextButton.addActionListener(new ActionListener() {
				            public void actionPerformed(ActionEvent ae) {
				            	System.out.println("The next button was pressed");
				            	if(nextButton.getText() == "Skip"){
				            		if(cur_cluster.size() <= 3)
		        						 JOptionPane.showMessageDialog(mainPanel, "You cannot skip this cluster. You can either label all the objects in this cluster or choose 2 or less outliers and label the rest.");
				            		else{
					            		writeRequestFile(2, "");
					            		checkIfResponseFileExists();
				            		}
				            	}
				            	if(outlierCategories > 0 ){
				            		System.out.println("Sending out outlier categories");
		        		    		writeRequestFileForKMeans(outlierCategories);
		        		    		checkIfResponseFileExists();
		        		    		outlierCategories = 0;
				            	}
				            	refreshContentPane();
				            }
				            
				            public void refreshContentPane(){
				            	System.out.println("Refreshing the content with new objects");
				            	// Remove all elements from the frame
				        		mainPanel.removeAll();
				        		buttonPanel.removeAll();
				            	ui.remove(mainPanel);
				        		
				            	// Create a JLabel and add it to the mainPanel
				            	String str = "<html> Learning <b>" + contextAttributeTable.get(modality) + "</b>.....</html>";
				        		JLabel label = new JLabel(str);
				        		label.setFont(new Font("Times New Roman", Font.BOLD, 40));
				        		
				        		if(endOfModality == true){
				        			endOfModality = false;
				        			System.out.println("************modailty has ended**********");
				        			System.out.println("Current cluster: " + cur_cluster);
				            		String resp = ask_free_resp("For the forthcoming clusters, what feature/attribute of the object ", modality);
					            	
					    			// Create a JLabel and add it to the mainPanel
									label.setText(resp);
									nextButton.setText("Next");
				            	}
					        		
				        		else{
						        	if(!askForOutlierCategories){
						        		// Display the images of the objects
						        		JPanel clusterImages = new JPanel(new GridBagLayout());
						        		GridBagConstraints gbc = new GridBagConstraints();
						        		gbc.fill = GridBagConstraints.HORIZONTAL;
						        		gbc.gridx = 0;
						        		gbc.gridy = 0;
						        		gbc.insets = new Insets(6, 6, 6, 6);
						        		gbc.anchor = GridBagConstraints.WEST;
						        		
						        		final ArrayList<String> outlierObjs = new ArrayList<String>();
						        		
						        		System.out.println("Current cluster: " + cur_cluster);
						        		
						        		for (final String objectName : cur_cluster) {
						        			
							        		// Checkbox for current cluster
							        		final JCheckBox checkbox = new JCheckBox();
							        		
							        		ImageIcon objectImg = new ImageIcon(this.getClass().getResource(imagePath+objectName+".JPG"));
							        	    final JLabel objLabel = new JLabel(objectImg);
							        	    objLabel.setToolTipText(objectName);
							        	    
							        		checkbox.addActionListener(new ActionListener() {
							        			 public void actionPerformed(ActionEvent ae) {
							        				 if(checkbox.isSelected()){
							        					 if(cur_cluster.size() == 1){
							        						 checkbox.setSelected(false);
							        						 JOptionPane.showMessageDialog(mainPanel, "Cannot choose an outlier here as there is only one object. Please label this object instead.");
							        					 }
							        					 
							        					 else if(cur_cluster.size() == 2 && outlierObjs.size() == 1){
							        						 checkbox.setSelected(false);
							        						 JOptionPane.showMessageDialog(mainPanel, "Can only choose one outlier here. Please label the remaining object.");
							        					 }
							        					 
							        					 else{
								        					 if(outlierObjs.size() == 2){
								        						 checkbox.setSelected(false);
								        						 JOptionPane.showMessageDialog(mainPanel, "Cannot choose more than 2 outliers. Skip this set of objects if you think there are more than 2 outliers");
								        					 }
								        					 else
								        					 {
									        					 outlierObjs.add(objLabel.getToolTipText());
									        					 objLabel.setIcon(new ImageIcon(this.getClass().getResource(imagePath+"outlier.png")));
									        					 objLabel.revalidate();
								        					 }
							        					 }
							        				 }
							        				 
							        				 if(!checkbox.isSelected()){
							        					 outlierObjs.remove(objLabel.getToolTipText());
							        					 objLabel.setIcon(new ImageIcon(this.getClass().getResource(imagePath+objectName+".JPG")));
							        					 objLabel.revalidate();
							        				 }
							        			 }
							        		});
							        		
							        		checkbox.setIcon(new SimpleCheckboxStyle(30));
							        		checkbox.setName("Displayed Clusters");
							        	    checkbox.setSelected(false);
							        	    if(gbc.gridx == 8){
							        	    	gbc.gridy++;
							        	    	gbc.gridx = 0;
							        	    }
							        	    clusterImages.add(checkbox, gbc);
							        	    gbc.gridx++;
	
							        	    clusterImages.add(objLabel, gbc);
							        	    gbc.gridx++;
						        		}
						        		System.out.println("Displaying the new cluster now");
						        	    mainPanel.add(clusterImages, BorderLayout.CENTER);
						        		
						        		JButton labelButton = new JButton("Label");
						        		labelButton.setFont(new Font("Times New Roman", Font.BOLD, 40));
						        		labelButton.setPreferredSize(new Dimension(400,70));
						        		labelButton.addActionListener(new ActionListener() {
						                    public void actionPerformed(ActionEvent aev) { 		
						                    	String clusterLabel = (String)JOptionPane.showInputDialog(ui,
						                                "Cluster label: ", "Labelling the cluster",
						                                JOptionPane.PLAIN_MESSAGE, null, null, "");
						                    	
						                    	// Send the cluster label back to be stored
						                    	if(clusterLabel != null && !clusterLabel.isEmpty()){
						                    		if(outlierObjs.size() != 0){
								                    	writeRequestFile(0, outlierObjs, clusterLabel);
								                    	checkIfResponseFileExists();
							                    	}
						                    		else{
								                    	writeRequestFile(1, clusterLabel);
								                    	checkIfResponseFileExists();
						                    		}
							                    	refreshContentPane();
						                    	}
						                    }
						        		});
						        		
						        		nextButton.setText("Skip");
						        		
						        		buttonPanel.add(labelButton);
						            }
					            	else{
					            		askForOutlierCategories = false;
					            		
					            		// Used only for getting the categories of the outliers
					        		    JPanel outlierPanel = new JPanel(new BorderLayout());
					        		    //outlierPanel.setMaximumSize(new Dimension(800, 200));
					        		    
						        		// Display the images of the objects
						        		JPanel outlierImages = new JPanel(new GridBagLayout());
						        		GridBagConstraints gbc = new GridBagConstraints();
						        		gbc.fill = GridBagConstraints.HORIZONTAL;
						        		gbc.gridx = 0;
						        		gbc.gridy = 0;
						        		gbc.insets = new Insets(6, 6, 6, 6);
						        		gbc.anchor = GridBagConstraints.WEST;
						        		
						        		for (final String objectName : cur_cluster) {
							        		ImageIcon objectImg = new ImageIcon(this.getClass().getResource(imagePath+objectName+".JPG"));
							        	    final JLabel objLabel = new JLabel(objectImg);
							        	    objLabel.setToolTipText(objectName);
						
							        		if(gbc.gridx == 4){
							        	    	gbc.gridy++;
							        	    	gbc.gridx = 0;
							        	    }
							        		
							        	    outlierImages.add(objLabel, gbc);
							        	    gbc.gridx++;
						        		}
						        	
						        		outlierPanel.add(outlierImages, BorderLayout.NORTH);
						        		
						        		JLabel label3 = new JLabel("<html>Please select the number of types of " + contextAttributeTable.get(modality) + "(s) that are present in the <br> above outliers.<html>");
						        		label3.setFont(new Font("Times New Roman", Font.BOLD, 40));
						        		outlierPanel.add(label3, BorderLayout.CENTER);
						        		
						        		String[] categories = {"Select a number", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
						        		
						        		final JComboBox categoryList = new JComboBox(categories);
						        		categoryList.setFont(new Font("Times New Roman", Font.BOLD, 40));
						        		categoryList.setSelectedIndex(0);
						        		categoryList.addActionListener(new ActionListener() {
						        		      public void actionPerformed(ActionEvent e) {
						        		    	  outlierCategories = Integer.parseInt((String)categoryList.getSelectedItem());
						        		      }
						        		    });
						        		
						        		outlierPanel.add(categoryList, BorderLayout.SOUTH);
	
						        		/*final JTextField inputCategories = new JTextField("",40);
						        		inputCategories.setFont(new Font("Times New Roman", Font.BOLD, 40));
						        		inputCategories.setColumns(2);
						        		
						        		inputCategories.addActionListener(new ActionListener() {
						        		      public void actionPerformed(ActionEvent e) {
						        		    	  if(!inputCategories.getText().equals("")){
						        		    		  System.out.println("Sending out outlier categories");
						        		    		  outlierCategories = Integer.parseInt(inputCategories.getText());
						        		    		  writeRequestFileForKMeans(outlierCategories);
						        		    		  checkIfResponseFileExists();
						        		    	  }
						        		      }
						        		    });*/
						        		
						        		nextButton.setText("Next");
				
						        		mainPanel.add(outlierPanel, BorderLayout.CENTER);
					            	}
				        		}
				        		
					        	buttonPanel.add(nextButton);
				        	    mainPanel.add(label, BorderLayout.NORTH);
				        	    mainPanel.add(buttonPanel, BorderLayout.SOUTH);
				                
				            	ui.setContentPane(mainPanel);
				            	ui.pack();
				            	ui.validate();
				                ui.repaint(); 
				            }
				        });
					   
					    buttonPanel.add(nextButton);
					    mainPanel.add(attrLabel, BorderLayout.NORTH);
					    mainPanel.add(buttonPanel, BorderLayout.SOUTH);
					    
					    System.out.println("EndOfModality at the end" + endOfModality);
	    			}catch(Exception ex){
	    				ex.printStackTrace();
	    			}
	            	ui.setContentPane(mainPanel);
	            	ui.validate();
	                ui.repaint();      	
	            }
	        });
		    
		    buttonPanel.add(beginButton);
		    mainPanel.add(info, BorderLayout.NORTH);
		    mainPanel.add(buttonPanel, BorderLayout.SOUTH);
            ui.setContentPane(mainPanel);
            ui.pack();
            ui.setVisible(true);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/*
	 * Question answering methods. The type of question asked determines the search method.
	 * Generally the questions will be assumed to be exact, ie super hardcoded so any changes to questions must also be reflected
	 * here.
	 */
	public static String ask_free_resp(String question, String modality){
		System.out.println("%%%%%% IN ASK_FREE_RESP %%%%%%%: " + modality);
		//Increment the questions asked by 1 every time this method is called
		// THE FOLLOWING IS COMMENTED OUT -> in comparison to the first experiment, where this question was not counted
		//questionCount++;
		if(question.equals("For the forthcoming clusters, what feature/attribute of the object ")){
			if(modality.equals("drop_audio")){
				contextAttributeTable.put("drop_audio", "material");
				return "<html>For the forthcoming clusters, the robot tried to learn the <b> MATERIAL </b> of the objects by dropping them and recording the audio.</html>";
			}
			if(modality.equals("revolve_audio")){
				contextAttributeTable.put("revolve_audio", "filled or empty");
				return "<html>For the forthcoming clusters, the robot tried to learn if the objects <b> ARE FILLED OR EMPTY </b> by revolving them and recording the audio.</html>";
			}
			if(modality.equals("push_audio")){
				contextAttributeTable.put("push_audio", "material");
				return "<html>For the forthcoming clusters, the robot tried to learn the <b> MATERIAL </b> of the objects by pushing them and recording the audio.</html>";
			}
			if(modality.equals("shake_audio")){
				contextAttributeTable.put("shake_audio", "filled or empty");
				return "<html>For the forthcoming clusters, the robot tried to learn if the objects <b> ARE FILLED OR EMPTY </b> by shaking them and recording the audio.</html>";
			}
			if(modality.equals("hold_haptics")){
				contextAttributeTable.put("hold_haptics", "how heavy");
				return "<html>For the forthcoming clusters, the robot tried to learn <b> HOW HEAVY </b> the objects are by holding them and recording the haptics.</html>";
			}
			if(modality.equals("lift_haptics")){
				contextAttributeTable.put("lift_haptics", "how heavy");
				return "<html>For the forthcoming clusters, the robot tried to learn <b> HOW HEAVY </b> the objects are by lifting them and recording the haptics.</html>";
			}
			if(modality.equals("press_haptics")){
				contextAttributeTable.put("press_haptics", "how tall");
				return "<html>For the forthcoming clusters, the robot tried to learn <b> HOW TALL </b> the objects are by pressing them on top and recording the haptics.</html>";
			}
			if(modality.equals("squeeze_haptics")){
				contextAttributeTable.put("squeeze_haptics", "deformable or not");
				return "<html>For the forthcoming clusters, the robot tried to learn if the objects <b> ARE DEFORMABLE OR NOT </b> by squeezing them and recording the haptics.</html>";
			}
			if(modality.equals("grasp_size")){
				contextAttributeTable.put("grasp_size", "size");
				return "<html>For the forthcoming clusters, the robot tried to learn the <b> SIZE </b> of the objects by grasping them by the side and recording the haptics.</html>";
			}
			if(modality.equals("look_color")){
				contextAttributeTable.put("look_color", "color");
				return "<html>For the forthcoming clusters, the robot tried to learn the <b> COLOR </b> of the objects by looking at them and recording the visual data.</html>";
			}
			if(modality.equals("look_shape")){
				contextAttributeTable.put("look_shape", "shape");
				return "<html>For the forthcoming clusters, the robot tried to learn the <b> SHAPE </b> of the objects by looking at them and recording the visual data.</html>";
			}
		}
		return null;
	}
	
	/*
	 * ONLY used for ID 1
	 */
	public static boolean writeRequestFile(int ID, String label){
		try{
			PrintWriter writer = new PrintWriter((reqFilePath + requestName), "UTF-8");

			if(writer != null){
				writer.println(ID);
				if(ID == 1){
					System.out.println("Requesting next cluster");
					writer.println(clusterNum);
					writer.println(label);
				}
				else
					System.out.println("Requesting a recluster");
				writer.close();
			}
			else {
				System.out.println("Unable to create file");
				return false;
			}
		}
		catch(IOException e){
			  e.printStackTrace();
		}
		return true;
	}
	
	/*
	 * Writes request file
	 * ID: 0 = remove -> name of object, its label
	 		1 = get next cluster -> current cluster number, its label
	 		2 = recluster -> no other arguments

	 	Returns success

	 	Input is ID and a vector, which can be null if the ID specifies an action not related to object IDS
	 */
	public static boolean writeRequestFile(int ID, ArrayList<String> outlier_objs, String label){
		try{
			PrintWriter writer = new PrintWriter((reqFilePath + requestName), "UTF-8");
			if(writer != null){
				writer.println(ID);
				if(ID == 0){
					//System.out.println("Requesting object removal");
					for(int i=0;i<outlier_objs.size(); i++){
						writer.println(outlier_objs.get(i));
						//System.out.println("Object to be deleted: " + outlier_objs.get(i));
					}
					writer.println(label);
					//System.out.println("Label of cluster: " + label);
				}
				else if(ID == 1){
					//System.out.println("Requesting next cluster");
					writer.println(clusterNum);
					writer.println(label);
				}
				else
					System.out.println("Requesting to recluster");
				writer.close();
			}
		}
		catch(IOException e){
			  e.printStackTrace();
		}

		return true;
	}
	
	public static void writeRequestFileForKMeans(int outlierCategories){
		System.out.println("Writing a request file for Kmeans");
		try{
			PrintWriter writer = new PrintWriter((reqFilePath + requestName), "UTF-8");
			if(writer != null){
				writer.println(outlierCategories);	
				writer.close();
			}
		}
		catch(IOException e){
			  e.printStackTrace();
		}
	}
	
	public static void checkIfResponseFileExists(){
		try{
			//System.out.println("Checking if request file exists");
			// Check if request.txt file exists and sleep till it does
			File response = new File("/home/priyanka/Desktop/groundedResponse.txt");
			//File request = new File("/home/users/pkhante/Desktop/groundedRequest.txt");
			while(!response.exists()){
			  //System.out.println("Going to sleep as file does not exist!");
			  Thread.sleep(2000); 
			}
			//System.out.println("Waking up as request.txt now exists!");
		}catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Read the contents of the request.txt file
		readResponseFile();
	}

	public static boolean readResponseFile(){
		System.out.println("Reading the response file");
		String line;
		int lineNum = 0;
		int ID;
		ArrayList<String> objects = new ArrayList<String>();
		String fullPath = (reqFilePath + responseName);
		try{
			BufferedReader myfile = new BufferedReader(new FileReader("/home/priyanka/Desktop/groundedResponse.txt"));
			//BufferedReader myfile = new BufferedReader(new FileReader("/home/users/pkhante/Desktop/groundedResponse.txt"));
			if(myfile != null){
				while((line = myfile.readLine()) != null){
					if(!(line.equals("EndOfAllModalities"))){
						if(lineNum == 0){
							if(line.equals("0")){
								File response = new File("/home/priyanka/Desktop/groundedResponse.txt");
								FileReader fileReader = new FileReader(response);
						        BufferedReader bufferedReader = new BufferedReader(fileReader);
						        String resLine = "";
						        ArrayList<String> params = new ArrayList<String>();
						     	
						     	// Read the contents of the request.txt file and then delete the file
						        while((resLine = bufferedReader.readLine()) != null) {
						        	//System.out.println(line);
						        	 if(!resLine.equals("0"))
						        		 params.add(resLine);
						        }
						        bufferedReader.close();  
								
						        modality = params.get(0);
						        clusterNum = Integer.parseInt(params.get(1));
						        for(int i=2;i<params.size();i++){
						        	objects.add(params.get(i));
						        }
						        
						        askForOutlierCategories = true;
						        
								break;
							}
								
							modality = line;
							int first = modality.compareTo(old_mod);

							if(first != 0){
								endOfModality = true;
								//Store the previous questionCount and reset it for the next context
								/*if(questionCount != 0)
									questionCountPerContext.put(old_mod, questionCount);*/
								
								//questionCount = 0;
								old_mod = modality;
							}
						}
						else if(lineNum == 1)
							clusterNum = Integer.parseInt(line);
						else{
							objects.add(line);
							System.out.println("Objects: " + line);
						}
						lineNum++;
					}
					else{
						// As all modalities are now done, exit after writing the labelTables to .csv files
						System.out.println("The program has ended");
						//writeLabelTableToFile(labelTable);
						//questionCountPerContext.put(modality, questionCount);
						//writeQuestionCountTableToFile();	
						System.exit(0);
					}
				}
				myfile.close();
			}
			else{
				System.out.println("Unable to open response file. Looking for " + fullPath);
				return false;
			}
			cur_cluster = objects;

			try {
		        Path path = FileSystems.getDefault().getPath(reqFilePath, responseName);
			    Files.delete(path);
				//System.out.println("Response file parsed and deleted successfully.");
			} catch (IOException x) {
			    System.err.format("Error deleting response file. %s: no such" + " file or directory%n", fullPath);
			}
		}
		catch(IOException e){
			  e.printStackTrace();
		}
		return true;
	}
}

class SimpleCheckboxStyle implements Icon {

    int dim = 10;

    public SimpleCheckboxStyle (int dimension){
        this.dim = dimension;
    }

    protected int getDimension() {
        return dim;
    }

    public void paintIcon(Component component, Graphics g, int x, int y) {
        ButtonModel buttonModel = ((AbstractButton) component).getModel();

        int y_offset = (int) (component.getSize().getHeight() / 2) - (int) (getDimension() / 2);
        int x_offset = 2;
        int fontsize = 30;

        if (buttonModel.isRollover()) {
            g.setColor(new Color(0, 60, 120));
        } else if (buttonModel.isRollover()) {
            g.setColor(Color.BLACK);
        } else {
            g.setColor(Color.DARK_GRAY);
        }
        g.fillRect(x_offset, y_offset, fontsize, fontsize);
        if (buttonModel.isPressed()) {
            g.setColor(Color.RED);
        } else if (buttonModel.isRollover()) {
            g.setColor(new Color(240, 240, 250));
        } else {
            g.setColor(Color.WHITE);
        }
        g.fillRect(1 + x_offset, y_offset + 1, fontsize - 2, fontsize - 2);
        if (buttonModel.isSelected()) {
            int r_x = 1;
            g.setColor(Color.RED);
            g.fillRect(x_offset + r_x + 3, y_offset + 3 + r_x, fontsize - (7 + r_x), fontsize - (7 + r_x));
        }
    }

    public int getIconWidth() {
        return getDimension();
    }

    public int getIconHeight() {
        return getDimension();
    }
}
