// This class launches the GUI interface for the user study

package experiments;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
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
import java.awt.Font; 
import javax.swing.plaf.FontUIResource; 

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
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
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;

import categorization.ClusterDB;
import categorization.ObjectClusterer;
import gui.RequestFocusListener;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

// TODO: 1) If there is only one outlier dont ask number of categories
// 2) If learning (filled or empty) or (deformable or not) --> Dont ask number of categories but count questions
public class GLLUserInterface extends JFrame {
	//public static final String filePath = "/home/users/pkhante/Pictures/grounded_learning_images/";
	//public static final String imagePath = "C:\\Users\\Priyanka\\Documents\\grounded_language_learning\\AutomatedExpNewAlgo\\bin\\etc\\object_images\\";
	public static final String reqFilePath	= "C:\\Users\\Priyanka\\Desktop\\";
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
	
	static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
 
	// Constructor
	public GLLUserInterface(){
		initUI();
		cur_cluster = new ArrayList<String>();
	}
	
	private void initUI(){
		// Create the UI 
		setTitle("Attribute Learning User Study");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);     // to close the JFrame
	}
	
	public static void main(String[] args) {
		try{
			// Have a file writer to write out the timestamps when everything is clicked
			final PrintWriter writer = new PrintWriter("C:\\Users\\Priyanka\\Documents\\grounded_language_learning\\AutomatedExpNewAlgo\\GLLUserStudyResults\\User18\\timestamp.txt", "UTF-8");
			
			final JFrame ui = new GLLUserInterface();
	        ui.setUndecorated(true);	   
	        ui.setExtendedState(JFrame.MAXIMIZED_BOTH); 
				    
		    final JPanel mainPanel = new JPanel();
		    mainPanel.setLayout(new BorderLayout());
		    
		    final JPanel buttonPanel = new JPanel();
		    buttonPanel.setPreferredSize(new Dimension(600, 90));
     
            JLabel info = new JLabel("<html>This experiment is being carried out to make the robot learn about the different attributes of real-world objects. "
            		+ "For each attribute, the robot tries to group similar objects into clusters and with your help will learn the labels for these groups.<br><br><br>"
            		+ "Read the following instruction properly before proceeding to the experiments:<br><br>"
            		+ "Numerous groups of objects would be displayed to you. You can do one of the following things, at each point of time:<br>"
            		+ "<ol><li><span> Click <i>Skip this cluster</i> if you do not wish to label these cluster of objects with an unifying attribute, you can go ahead and skip to the next one.</span>"
            		+ "<li><span> Click <i>Label this cluster</i> if you wish to label the cluster of objects.</span>"
            		+ "<li><span> Pick outliers - You can check the boxes at the side of the objects to mark them as outliers. In each cluster, you can only mark 1 or 2 outliers. You should only do this, if you want to label the rest of the objects in the cluster after taking out the outliers. If more than 2 outliers exist, click <i>Skip this cluster</i>.</span></li></ol>"
            		+ "<br>Things to Note: 1) Images are not to scale. So please look at the real objects in front of you if need be. <br>"
            		+ "2) There is no back button in this interface. So please think carefully and choose what to do. <br>"
            		+ "3) The interface is a bit slow, so once you click a button, please wait until the window refreshes. Thank you! </html>");
            info.setFont(new Font("Times New Roman", Font.BOLD, 60));
            JButton firstButton = new JButton("Next");
		    
		    JButton beginButton = new JButton("Let's Begin!");
		    beginButton.setFont(new Font("Times New Roman", Font.BOLD, 60));
		    beginButton.setPreferredSize(new Dimension(600,90));
		    beginButton.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent e) {
	            	// Start the time of the experiment
	            	Timestamp timestamp = new Timestamp(System.currentTimeMillis());
	            	writer.println("Let's begin: " + sdf.format(timestamp));
	            	
	            	// Create a first request file
	    			writeRequestFile(1,",");
	    			try{
		    			checkIfResponseFileExists(writer);
		    			endOfModality = false;
		    			String resp = ask_free_resp("For the forthcoming clusters, what feature/attribute of the object ", modality);
		    			// Start with the experiment
		    			mainPanel.removeAll();
		    			buttonPanel.removeAll();
		            	ui.remove(mainPanel);
		            	
		    			// Create a JLabel and add it to the mainPanel
						JLabel attrLabel = new JLabel(resp);
						attrLabel.setFont(new Font("Times New Roman", Font.BOLD, 60));
					    
					    final JButton nextButton = new JButton("Next");
					    nextButton.setPreferredSize(new Dimension(600,90));
					    nextButton.setFont(new Font("Times New Roman", Font.BOLD, 60));
					    
					    // Create the action listener 
					    nextButton.addActionListener(new ActionListener() {
				            public void actionPerformed(ActionEvent ae) {
				            	System.out.println("The next button was pressed");
				            	if(nextButton.getText() == "Skip this cluster"){
				            		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
					            	writer.println("Skip cluster: " + sdf.format(timestamp));
				            		System.out.println("The text was skip");
				            		if(cur_cluster.size() <= 3){
				            			JLabel errorText = new JLabel("<html> You cannot skip this cluster. You can either label all the objects in this cluster or choose 2 or less outliers and label the rest.</html>");
		        						errorText.setFont(new Font("Times New Roman", Font.BOLD, 40));
		        						errorText.setPreferredSize(new Dimension(1000,200));
		        						 
		        						JPanel errorPanel = new JPanel();
		        						errorPanel.add(errorText);
					                    errorPanel.setPreferredSize(new Dimension(1000,200)); 
				            			
					                    UIManager.put("OptionPane.buttonFont", new FontUIResource(new Font("TIMES NEW ROMAN",Font.PLAIN,35)));
		        						JOptionPane.showMessageDialog(mainPanel, errorPanel, "Error", JOptionPane.ERROR_MESSAGE);
				            		}
		        					else{
					            		writeRequestFile(2, "");
					            		checkIfResponseFileExists(writer);
				            		}
				            	}
				            	if(outlierCategories > 0){
				            		System.out.println("Sending out outlier categories");
		        		    		writeRequestFileForKMeans(outlierCategories);
		        		    		checkIfResponseFileExists(writer);
		        		    		outlierCategories = 0;
				            	}
				            	
				            	if(outlierCategories != -500)
				            		refreshContentPane();
				            	
				            	if(outlierCategories == -500){
				            		outlierCategories = -1000;
				            		JLabel errorText = new JLabel("<html>Cannot proceed unless a number is chosen from the drop-down list.</html>");
	        						errorText.setFont(new Font("Times New Roman", Font.BOLD, 40));
	        						errorText.setPreferredSize(new Dimension(1000,200));
	        						 
	        						JPanel errorPanel = new JPanel();
	        						errorPanel.add(errorText);
				                    errorPanel.setPreferredSize(new Dimension(1000,200));
				                    
				                    UIManager.put("OptionPane.buttonFont", new FontUIResource(new Font("TIMES NEW ROMAN",Font.PLAIN,35)));
	        						JOptionPane.showMessageDialog(mainPanel, errorPanel, "Error", JOptionPane.ERROR_MESSAGE);
	        						
	        						
				            	}
				            }
				            
				            public void refreshContentPane(){
				            	System.out.println("Refreshing the content with new objects");
				            	// Remove all elements from the frame
				        		mainPanel.removeAll();
				        		buttonPanel.removeAll();
				            	ui.remove(mainPanel);
				        		
				            	// Create a JLabel and add it to the mainPanel
				            	String str = getAttrInfo(modality);
				        		JLabel label = new JLabel(str);
				        		label.setFont(new Font("Times New Roman", Font.BOLD, 80));
				        		//Border paddingBorder = BorderFactory.createEmptyBorder(0,20,0,0);
				        		//label.setBorder(BorderFactory.createCompoundBorder(null,paddingBorder));
				        		
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
						        		System.out.println("Not asking for outliers");
						        		// Display the instructions at the side always
						        		JLabel instructionLabel = new JLabel("<html><strong>Instructions: </strong><ol><li><span> Click <i>Skip this cluster</i> - If you do not wish to label this group of objects with an unifying attribute, you can go ahead and skip to the next one.</span>"
            		+ "<li><span> Click <i>Label this cluster</i> - If you wish to label this group of objects with a unifying attribute.</span>"
            		+ "<li><span> Pick outliers - You can check the boxes at the side of the objects to mark them as outliers. In each cluster, you can only mark 1 or 2 outliers. You should only do this, if you want to label the rest of the objects in the cluster after taking out the outliers. If more than 2 outliers exist, <i>Skip this cluster</i>.</span></li></ol>"
            		+ "<br><strong>NOTE: Images are not to scale. So please look at the real objects in front of you if need be.</strong></html>");
						        		instructionLabel.setFont(new Font("Times New Roman", Font.BOLD, 60));
						        		instructionLabel.setPreferredSize(new Dimension(1000,1550));
						        		
						        		//JPanel instructionPanel = new JPanel();
						        		//instructionPanel.add(instructionLabel);
						        		//instructionPanel.setPreferredSize(new Dimension(500,900));
						        		
						        		mainPanel.add(instructionLabel, BorderLayout.EAST);
						        		
						        		// Display the images of the objects
						        		JPanel clusterImages = new JPanel(new GridBagLayout());
						        		clusterImages.setPreferredSize(new Dimension(5000,5000));
						           		GridBagConstraints gbc = new GridBagConstraints();
						        		gbc.fill = GridBagConstraints.HORIZONTAL;
						        		gbc.gridx = 0;
						        		gbc.gridy = 0;
						        		gbc.insets = new Insets(4, 4, 4, 4);
						        		//gbc.insets = new Insets(2, 2, 2, 2);
						        		//gbc.anchor = GridBagConstraints.CENTER;
						        		
						        		final ArrayList<String> outlierObjs = new ArrayList<String>();
						        		
						        		System.out.println("Current cluster: " + cur_cluster);
						        		
						        		for (final String objectName : cur_cluster) {
							        		// Checkbox for current cluster
							        		final JCheckBox checkbox = new JCheckBox();
							        		//System.out.println("Image path: " + this.getClass().getResource("/stackoverflow.png"));
							        		ImageIcon objectImg = new ImageIcon(this.getClass().getResource("/"+objectName+".JPG"));
							        	    //Image objectImg = objectIcon.getImage();
							        	    //BufferedImage bi = new BufferedImage(objectImg.getWidth(null), objectImg.getHeight(null), BufferedImage.TYPE_INT_ARGB);
							        	    //Graphics g = bi.createGraphics();
							        	    //g.drawImage(objectImg, 0, 0, , HEIGHT, null);
							        	    //ImageIcon objectImage = new ImageIcon(bi);
							        	    final JLabel objLabel = new JLabel(objectImg);
							        	    objLabel.setPreferredSize(new Dimension(400,400));
							        	    objLabel.setToolTipText(objectName); 
							        	    
							        		checkbox.addActionListener(new ActionListener() {
							        			 public void actionPerformed(ActionEvent ae) {
							        				 if(checkbox.isSelected()){
							        					 if(cur_cluster.size() == 1){
							        						 JLabel errorText = new JLabel("<html>Cannot choose an outlier here as there is only one object. Please label this object instead.</html>");
							        						 errorText.setFont(new Font("Times New Roman", Font.BOLD, 40));
							        						 errorText.setPreferredSize(new Dimension(1000,200));
							        						 
							        						 JPanel errorPanel = new JPanel();
							        						 errorPanel.add(errorText);
										                     errorPanel.setPreferredSize(new Dimension(1000,200));
										                    	
							        						 checkbox.setSelected(false);
							        						 UIManager.put("OptionPane.buttonFont", new FontUIResource(new Font("TIMES NEW ROMAN",Font.PLAIN,35)));
							        						 JOptionPane.showMessageDialog(mainPanel, errorPanel, "Error", JOptionPane.ERROR_MESSAGE);
							        					 }
							        					 
							        					 else if(cur_cluster.size() == 2 && outlierObjs.size() == 1){
							        						 JLabel errorText = new JLabel("<html>Can only choose one outlier here. Please label the remaining object.</html>");
							        						 errorText.setFont(new Font("Times New Roman", Font.BOLD, 40));
							        						 errorText.setPreferredSize(new Dimension(1000,200));
							        						 
							        						 JPanel errorPanel = new JPanel();
							        						 errorPanel.add(errorText);
										                     errorPanel.setPreferredSize(new Dimension(1000,200));
										                     
							        						 checkbox.setSelected(false);
							        						 UIManager.put("OptionPane.buttonFont", new FontUIResource(new Font("TIMES NEW ROMAN",Font.PLAIN,35)));
							        						 JOptionPane.showMessageDialog(mainPanel, errorPanel, "Error", JOptionPane.ERROR_MESSAGE);
							        					 }
							        					 
							        					 else{
								        					 if(outlierObjs.size() == 2){
								        						 JLabel errorText = new JLabel("<html>Cannot choose more than 2 outliers. Skip this set of objects if you think there are more than 2 outliers. </html>");
								        						 errorText.setFont(new Font("Times New Roman", Font.BOLD, 40));
								        						 errorText.setPreferredSize(new Dimension(1000,200));
								        						 
								        						 JPanel errorPanel = new JPanel();
								        						 errorPanel.add(errorText);
											                     errorPanel.setPreferredSize(new Dimension(1000,200));
											                     
								        						 checkbox.setSelected(false);
								        						 UIManager.put("OptionPane.buttonFont", new FontUIResource(new Font("TIMES NEW ROMAN",Font.PLAIN,35)));
								        						 JOptionPane.showMessageDialog(mainPanel, errorPanel, "Error", JOptionPane.ERROR_MESSAGE);
								        					 }
								        					 else
								        					 {
									        					 outlierObjs.add(objLabel.getToolTipText());
									        					 // TODO - Change outlier image size
									        					 objLabel.setIcon(new ImageIcon(this.getClass().getResource("/outlier.png")));
									        					 objLabel.revalidate();
								        					 }
							        					 }
							        				 }
							        				 
							        				 if(!checkbox.isSelected()){
							        					 outlierObjs.remove(objLabel.getToolTipText());
							        					 objLabel.setIcon(new ImageIcon(this.getClass().getResource("/"+objectName+".JPG")));
							        					 objLabel.revalidate();
							        				 }
							        			 }
							        		});
							        		
							        		checkbox.setIcon(new SimpleCheckboxStyle(30));
							        		checkbox.setName("Displayed Clusters");
							        	    checkbox.setSelected(false);
							        	    if(gbc.gridx == 8){//4){
							        	    	gbc.gridy++;
							        	    	gbc.gridx = 0;
							        	    }
							        	    clusterImages.add(checkbox, gbc);
							        	    gbc.gridx++;
							        	  
							        	    clusterImages.add(objLabel, gbc);
							        	    gbc.gridx++;
							        	    
						        		}
						        		System.out.println("Displaying the new cluster now");
						        		//clusterImages.setPreferredSize(new Dimension(800,1500));
						        		mainPanel.add(clusterImages,BorderLayout.CENTER);
						        		
						        		JButton labelButton = new JButton("Label this cluster");
						        		labelButton.setFont(new Font("Times New Roman", Font.BOLD, 60));
						        		labelButton.setPreferredSize(new Dimension(600,90));
						        		labelButton.addActionListener(new ActionListener() {
						                    public void actionPerformed(ActionEvent aev) {
						                    	JLabel labelMsg = new JLabel("Cluster label:");
						                    	labelMsg.setFont(new Font("Times New Roman", Font.BOLD, 60));
						                    	
						                    	final JTextField labelText = new JTextField();
						                    	labelText.setPreferredSize(new Dimension(600, 70));
						                    	labelText.setFont(new Font("Times New Roman", Font.PLAIN, 50));
						                    	labelText.addAncestorListener(new RequestFocusListener());
						                    	
						                    	JPanel label = new JPanel(new BorderLayout());
						                    	label.add(labelMsg, BorderLayout.NORTH);
						                    	label.add(labelText, BorderLayout.SOUTH);
						                    	label.setPreferredSize(new Dimension(1000,200));
						              
						                    	UIManager.put("OptionPane.buttonFont", new FontUIResource(new Font("TIMES NEW ROMAN",Font.PLAIN,35)));
						                    	int result = JOptionPane.showConfirmDialog(ui,
						                                label, "Labelling the cluster",
						                                JOptionPane.OK_CANCEL_OPTION);
						                    	
						                    	String clusterLabel = "";
						                    	if (result==JOptionPane.OK_OPTION) {
						                    		clusterLabel = labelText.getText();
						                    	}
						                    	
						                    	// Send the cluster label back to be stored
						                    	if(clusterLabel != null && !clusterLabel.isEmpty()){
						                    		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
						                    		writer.println("Label: " + sdf.format(timestamp));
						                    		if(outlierObjs.size() != 0){
								                    	writeRequestFile(0, outlierObjs, clusterLabel);
								                    	checkIfResponseFileExists(writer);
							                    	}
						                    		else{
								                    	writeRequestFile(1, clusterLabel);
								                    	checkIfResponseFileExists(writer);
						                    		}
							                    	refreshContentPane();
						                    	}
						                    }
						        		});
						        		
						        		nextButton.setText("Skip this cluster");
						        		
						        		buttonPanel.add(labelButton);
						            }
					            	else{
					            		askForOutlierCategories = false;
					            		
					            		// Dont ask for the types of outliers if its a single object
					            		if(cur_cluster.size() == 1){
					            			// Assume we know that only one category exists
					            			outlierCategories = 1;
					            			writeRequestFileForKMeans(outlierCategories);
				        		    		checkIfResponseFileExists(writer);
				        		    		outlierCategories = 0;
				        		    		refreshContentPane();
					            		}
					            		
					            		else{
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
								        		ImageIcon objectImg = new ImageIcon(this.getClass().getResource("/"+objectName+".JPG"));
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
							        		
							        		JLabel label3;
							        		if(modality.equals("shake_audio")){
							        			System.out.println("The modality is shake_audio");
							        			label3 = new JLabel("<html>Please select (1) from the drop-down if all the above objects are either filled OR all of them are empty. Select (2) if both categories are present.<html>");
							        		}
							        		
							        		else if(modality.equals("revolve_haptics")){
							        			label3 = new JLabel("<html>Please select (1) from the drop-down if all the above objects are either squeezable OR all of them are not squeezable. Select (2) if both categories are present.<html>");
							        		}
							        		
							        		else{
							        			System.out.println("I have no freaking clue");
							        			label3 = new JLabel("<html>Please select the number of categories of " + contextAttributeTable.get(modality) + "(s) that are present in the <br> above objects.<html>");
							        		}
							        		
							        		label3.setFont(new Font("Times New Roman", Font.BOLD, 60));
							        		outlierPanel.add(label3, BorderLayout.CENTER);
							        		
							        		String[] categories = {"Select a number", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
							        		outlierCategories = -500;
							        		
							        		final JComboBox categoryList = new JComboBox(categories);
							        		categoryList.setFont(new Font("Times New Roman", Font.BOLD, 60));
							        		categoryList.setSelectedIndex(0);
							        		categoryList.addActionListener(new ActionListener() {
							        		      public void actionPerformed(ActionEvent e) {
							        		    	  Timestamp timestamp = new Timestamp(System.currentTimeMillis());
							        		    	  writer.println("Outlier categories: " + sdf.format(timestamp));
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
				        		}
				        		
					        	buttonPanel.add(nextButton);
				        	    mainPanel.add(label, BorderLayout.NORTH);
				        	    mainPanel.add(buttonPanel, BorderLayout.SOUTH);
				                
				            	ui.setContentPane(mainPanel);
				            	//ui.pack();
				            	ui.invalidate();
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
	            	ui.invalidate();
	            	ui.validate();
	                ui.repaint();      	
	            }
	        });
		    
		    buttonPanel.add(beginButton);
		    mainPanel.add(info, BorderLayout.NORTH);
		    mainPanel.add(buttonPanel, BorderLayout.SOUTH);
            ui.setContentPane(mainPanel);
            //ui.pack();
            ui.invalidate();
            ui.validate();
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
				contextAttributeTable.put("drop_audio", "MATERIALS");
				return "<html>For the forthcoming clusters, the robot tried to learn the <b> MATERIAL </b> attribute of the objects.</html>";
			}
			if(modality.equals("shake_audio")){
				contextAttributeTable.put("shake_audio", "FILLED OR EMPTY");
				return "<html>For the forthcoming clusters, the robot tried to learn if the objects are <b> FILLED OR EMPTY. </b></html>";
			}
			if(modality.equals("drop_haptics")){
				contextAttributeTable.put("drop_haptics", "WEIGHTS");
				return "<html>For the forthcoming clusters, the robot tried to learn the different categories of <b> WEIGHTS </b> of objects.</html>";
			}
			if(modality.equals("press_haptics")){
				contextAttributeTable.put("press_haptics", "HEIGHTS");
				return "<html>For the forthcoming clusters, the robot tried to learn the different categories of <b> HEIGHTS </b> of objects.</html>";
			}
			if(modality.equals("revolve_haptics")){
				contextAttributeTable.put("revolve_haptics", "SQUEEZABLE OR NOT SQUEEZABLE");
				return "<html>For the forthcoming clusters, the robot tried to learn if the objects are <b>SQUEEZABLE OR NOT SQUEEZABLE. </b></html>";
			}
			if(modality.equals("grasp_size")){
				contextAttributeTable.put("grasp_size", "WIDTHS/THICKNESS");
				return "<html>For the forthcoming clusters, the robot tried to learn the different categories of <b> WIDHTS/THICKNESS </b> of objects.</html>";
			}
			if(modality.equals("look_color")){
				contextAttributeTable.put("look_color", "PREDOMINANT COLORS");
				return "<html>For the forthcoming clusters, the robot tried to learn the <b> PREDOMINANT COLORS </b> of the objects.</html>";
			}
			if(modality.equals("push_audio")){
				contextAttributeTable.put("push_audio", "SHAPES");
				return "<html>For the forthcoming clusters, the robot tried to learn the different categories of <b> SHAPES </b> of objects.</html>";
			}
		}
		return null;
	}
	
	public static String getAttrInfo(String modality){
		System.out.println("%%%%%% IN GETATTRINFO %%%%%%%: " + modality);
		//Increment the questions asked by 1 every time this method is called
		// THE FOLLOWING IS COMMENTED OUT -> in comparison to the first experiment, where this question was not counted
		//questionCount++;
		if(modality.equals("drop_audio")){
			return "<html> Provide the <b>MATERIAL</b> attribute for the following objects...</html>";
		}
		if(modality.equals("shake_audio")){
			return "<html> Provide the <b>FILLED OR EMPTY</b> attribute for the following objects...</html>";
		}
		if(modality.equals("drop_haptics")){
			return "<html> Provide the different categories of <b>WEIGHTS</b> for the following objects...</html>";
		}
		if(modality.equals("press_haptics")){
			return "<html> Provide the different categories of <b>HEIGHTS</b> for the following objects...</html>";
		}
		if(modality.equals("revolve_haptics")){
			return "<html> Provide the <b>SQUEEZABLE OR NOT SQUEEZABLE</b> attribute for the following objects...</html>";
		}
		if(modality.equals("grasp_size")){
			return "<html> Provide the different categories of <b>WIDTHS/THICKNESS</b> for the following objects...</html>";
		}
		if(modality.equals("look_color")){
			return "<html> Provide the <b>PREDOMINANT COLOR</b> attribute for the following objects...</html>";
		}
		if(modality.equals("push_audio")){
			return "<html> Provide the different categories of <b>SHAPES</b> for the following objects...</html>";
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
	
	public static void checkIfResponseFileExists(PrintWriter writer){
		try{
			//System.out.println("Checking if request file exists");
			// Check if request.txt file exists and sleep till it does
			File response = new File("C:\\Users\\Priyanka\\Desktop\\groundedResponse.txt");
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
		readResponseFile(writer);
	}

	public static boolean readResponseFile(PrintWriter writer){
		System.out.println("Reading the response file");
		String line;
		int lineNum = 0;
		int ID;
		ArrayList<String> objects = new ArrayList<String>();
		String fullPath = (reqFilePath + responseName);
		try{
			BufferedReader myfile = new BufferedReader(new FileReader("C:\\Users\\Priyanka\\Desktop\\groundedResponse.txt"));
			//BufferedReader myfile = new BufferedReader(new FileReader("/home/users/pkhante/Desktop/groundedResponse.txt"));
			if(myfile != null){
				while((line = myfile.readLine()) != null){
					if(!(line.equals("EndOfAllModalities"))){
						if(lineNum == 0){
							if(line.equals("0")){
								File response = new File("C:\\Users\\Priyanka\\Desktop\\groundedResponse.txt");
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
						Timestamp timestamp = new Timestamp(System.currentTimeMillis());
						writer.println("End time: " + sdf.format(timestamp));
						writer.close();
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
