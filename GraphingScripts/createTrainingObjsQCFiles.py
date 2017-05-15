import csv
import os
import pandas as pd
import numpy as np
import plotly.tools as tls
tls.set_credentials_file(username='pkhante', api_key='l002tdvw1k')
import plotly.plotly as py
import plotly.graph_objs as go

py.sign_in('pkhante', 'gargi0704')

rootdir0 = 'C:\\Users\\Priyanka\\Documents\\grounded_language_learning\\SinglyAnnotatedObjectTrials\\CombinedContextTrialResults\\'
rootdir1 = 'C:\\Users\\Priyanka\\Documents\\grounded_language_learning\\AutomatedExpNewAlgo\\CombinedRecogTrainingObjsResults\\'
rootdir2 = 'C:\\Users\\Priyanka\\Documents\\grounded_language_learning\\AutomatedExpNewAlgo\\CombinedRecogTrainingObjsResults\\'
rootdir3 = 'C:\\Users\\Priyanka\\Documents\\grounded_language_learning\\AutomatedExpNewAlgo\\GLLUserStudyModified\\'

contexts_list = ['look_color', 'shake_audio', 'drop_haptics', 'drop_audio', 'press_haptics', 'grasp_size', 'push_audio', 'revolve_haptics']
#['shake_audio', 'drop_haptics', 'drop_audio', 'press_haptics', 'grasp_size', 'look_shape', 'lift_haptics', 'revolve_haptics']
#['drop_haptics', 'revolve_haptics', 'lift_haptics', 'press_audio', 'squeeze_haptics', 'grasp_haptics', 'push_audio', 'hold_haptics', 'push_haptics', 'shake_haptics']
#['drop_audio', 'grasp_size', 'hold_haptics', 'lift_haptics', 'look_color', 'look_shape', 'press_haptics', 'push_audio', 'revolve_audio', 'shake_audio', 'squeeze_haptics']

for context in contexts_list:
    print("Context: ", context)
    attr = ""

    if context == 'look_color':
        attr = 'color' 
    if context == 'shake_audio':
        attr = 'has_contents' 
    if context == 'drop_haptics':
        attr = 'weight'
    if context == 'drop_audio':
        attr = 'material'
    if context == 'press_haptics':
        attr = 'height'
    if context == 'grasp_size':
        attr = 'size'
    if context == 'push_audio':
        attr = 'shape' 
    if context == 'revolve_haptics':
        attr = 'deformable'
    

    # Read csv file and put the values in an array
    
    filepath_exp0 = rootdir0 + attr + '\\PointsToPlot.csv'
    exp1 = np.genfromtxt(filepath_exp0, delimiter=',')
    """
    # Read csv file and put the values in an array
    filepath_exp1 = rootdir1 + attr + '\\' + context + '\\TrainingObjsToPlot.csv'
    exp2_1 = np.genfromtxt(filepath_exp1, delimiter=',')

    filepath_exp2 = rootdir2 + attr + '\\' + context + '\\TrainingObjsToPlot.csv'
    exp2_2 = np.genfromtxt(filepath_exp2, delimiter=',')
    
    filepath_exp3 = rootdir3 + attr + '/TrainingObjsToPlot.csv'
    exp3 = np.genfromtxt(filepath_exp3, delimiter=',')

    df0 = pd.DataFrame({"TrainingObjects": exp1[:, 0], "QuestionCount": exp1[:, 0]})
    df1 = pd.DataFrame({"TrainingObjects": exp2_1[:, 0], "QuestionCount": exp2_1[:, 1]})
    df2 = pd.DataFrame({"TrainingObjects": exp2_2[:, 0], "QuestionCount": exp2_2[:, 2]})

    df3 = pd.DataFrame({"TrainingObjects": exp3[:,0], "QuestionCount": exp3[:,1]})
    df4 = pd.DataFrame({"TrainingObjects": exp3[:,0], "QuestionCount": exp3[:,2]})

    # Get the highest question count

    max2_1 = df1.QuestionCount.max()
    max2_2 = df2.QuestionCount.max()
    max3_1 = df3.QuestionCount.max()
    max3_2 = df4.QuestionCount.max()
    
    #print ("max2_1 for " + context + ": " + str(max2_1))
    #print ("max2_2 for " + context + ": " + str(max2_2))
    print ("max3_1 for " + context + ": " + str(max3_1))
    print ("max3_2 for " + context + ": " + str(max3_2))

    question_asked1 = []
    training_objs1 = [] 
    question_asked2 = []
    training_objs2 = []
    """
    question_asked0 = []
    training_objs0 = []
    """
    question_asked3 = []
    training_objs3 = []
    question_asked4 = []
    training_objs4 = []

    question_count2 = [0] * max2_2
    training_sum2 = [0] * max2_2
    question_count1 = [0] * max2_1
    training_sum1 = [0] * max2_1
    question_count3 = [0] * max3_1
    training_sum3 = [0] * max3_1
    question_count4 = [0] * max3_2
    training_sum4 = [0] * max3_2
    """
    # For exp1
    for i in range(0,len(df0.index)):
        question_asked0.append(df0.iloc[i].QuestionCount)
        training_objs0.append(df0.iloc[i].TrainingObjects)
    """
    # exp2_1
    for i in range(0,len(df1.index)):
        # Reached a new trial
        if i != len(df1.index)-1:
            if df1.iloc[i+1].QuestionCount < df1.iloc[i].QuestionCount: 
                if df1.iloc[i].QuestionCount == max2_1:
                    question_asked1.append(df1.iloc[i].QuestionCount)
                    training_objs1.append(df1.iloc[i].TrainingObjects)
                    continue
                else:
                    for j in range(0, int(1+max2_1-df1.iloc[i].QuestionCount)):
                        question_asked1.append(float(df1.iloc[i].QuestionCount+j))
                        training_objs1.append(27.0)
            else:
                if df1.iloc[i+1].QuestionCount-df1.iloc[i].QuestionCount == 1:
                    question_asked1.append(df1.iloc[i].QuestionCount)
                    training_objs1.append(df1.iloc[i].TrainingObjects)
                else:
                    for j in range(0, int(df1.iloc[i+1].QuestionCount-df1.iloc[i].QuestionCount)):
                        question_asked1.append(float(df1.iloc[i].QuestionCount+j))
                        training_objs1.append(df1.iloc[i].TrainingObjects)
        elif i == len(df1.index)-1 and df1.iloc[i].QuestionCount != max2_1:
            for j in range(0, int(1+max2_1-df1.iloc[i].QuestionCount)):
                question_asked1.append(df1.iloc[i].QuestionCount+j)
                training_objs1.append(27.0)

    # exp2_2
    for i in range(0,len(df2.index)):
        if i != len(df2.index)-1:
            if df2.iloc[i+1].QuestionCount < df2.iloc[i].QuestionCount: 
                if df2.iloc[i].QuestionCount == max2_2:
                    question_asked2.append(df2.iloc[i].QuestionCount)
                    training_objs2.append(df2.iloc[i].TrainingObjects)
                    continue
                else:
                    for j in range(0, int(1+max2_2-df2.iloc[i].QuestionCount)):
                        question_asked2.append(float(df2.iloc[i].QuestionCount+j))
                        training_objs2.append(27.0)
            else:
                if df2.iloc[i+1].QuestionCount-df2.iloc[i].QuestionCount == 1:
                    question_asked2.append(df2.iloc[i].QuestionCount)
                    training_objs2.append(df2.iloc[i].TrainingObjects)
                else:
                    for j in range(0, int(df2.iloc[i+1].QuestionCount-df2.iloc[i].QuestionCount)):
                        question_asked2.append(float(df2.iloc[i].QuestionCount+j))
                        training_objs2.append(df2.iloc[i].TrainingObjects)
        elif i == len(df2.index)-1 and df2.iloc[i].QuestionCount != max2_2:
            for j in range(0, int(1+max2_2-df2.iloc[i].QuestionCount)):
                question_asked2.append(df2.iloc[i].QuestionCount+j)
                training_objs2.append(27.0)

    # exp3_1
    for i in range(0,len(df3.index)):
        if i != len(df3.index)-1:
            if df3.iloc[i+1].QuestionCount < df3.iloc[i].QuestionCount: 
                if df3.iloc[i].QuestionCount == max3_1:
                    question_asked3.append(df3.iloc[i].QuestionCount)
                    training_objs3.append(df3.iloc[i].TrainingObjects)
                    continue
                else:
                    for j in range(0, int(1+max3_1-df3.iloc[i].QuestionCount)):
                        question_asked3.append(float(df3.iloc[i].QuestionCount+j))
                        training_objs3.append(27.0)
            else:
                if df3.iloc[i+1].QuestionCount-df3.iloc[i].QuestionCount == 1:
                    question_asked3.append(df3.iloc[i].QuestionCount)
                    training_objs3.append(df3.iloc[i].TrainingObjects)
                else:
                    for j in range(0, int(df3.iloc[i+1].QuestionCount-df3.iloc[i].QuestionCount)):
                        question_asked3.append(float(df3.iloc[i].QuestionCount+j))
                        training_objs3.append(df3.iloc[i].TrainingObjects)
        elif i == len(df3.index)-1 and df3.iloc[i].QuestionCount != max3_1:
            for j in range(0, int(1+max3_1-df3.iloc[i].QuestionCount)):
                question_asked3.append(df3.iloc[i].QuestionCount+j)
                training_objs3.append(27.0)

    # exp3_2
    for i in range(0,len(df4.index)):
        if i != len(df4.index)-1:
            if df4.iloc[i+1].QuestionCount < df4.iloc[i].QuestionCount: 
                if df4.iloc[i].QuestionCount == max3_2:
                    question_asked4.append(df4.iloc[i].QuestionCount)
                    training_objs4.append(df4.iloc[i].TrainingObjects)
                    continue
                else:
                    for j in range(0, int(1+max3_2-df4.iloc[i].QuestionCount)):
                        question_asked4.append(float(df4.iloc[i].QuestionCount+j))
                        training_objs4.append(27.0)
            else:
                if df4.iloc[i+1].QuestionCount-df4.iloc[i].QuestionCount == 1:
                    question_asked4.append(df4.iloc[i].QuestionCount)
                    training_objs4.append(df4.iloc[i].TrainingObjects)
                else:
                    for j in range(0, int(df4.iloc[i+1].QuestionCount-df4.iloc[i].QuestionCount)):
                        question_asked4.append(float(df4.iloc[i].QuestionCount+j))
                        training_objs4.append(df4.iloc[i].TrainingObjects)
        elif i == len(df4.index)-1 and df4.iloc[i].QuestionCount != max3_2:
            for j in range(0, int(1+max3_2-df4.iloc[i].QuestionCount)):
                question_asked4.append(df4.iloc[i].QuestionCount+j)
                training_objs4.append(27.0)

    # For exp 2_1
    for i in range(0,len(question_asked1)):
        # Get all question counts from the first column 
        num1 = question_asked1[i]
        index1 = int(num1 % max2_1)           # all 27's go in [0]th index
        question_count1[index1] = question_count1[index1] + 1 
        training_sum1[index1] = training_sum1[index1]+training_objs1[i]    # Sum the no. of training objects

    #print (question_count1)
    #print (training_sum1)

    # Calculate the mean for each question count
    training_mean1 = [0] * max2_1            # The mean of 27 goes into [0]th index
    training_variance1 = [0] * max2_1
    training_std_dev1 = [0] * max2_1

    for i in range(0, len(training_sum1)):
        if question_count1[i] != 0:
            training_mean1[i] = training_sum1[i]/question_count1[i]

    for i in range(0, len(question_asked1)):
        num1 = question_asked1[i]
        index1 = int(num1 % max2_1)
        training_std_dev1[index1] = training_std_dev1[index1] + ((training_objs1[i] - training_mean1[index1]) ** 2)

    for i in range(0, len(training_std_dev1)):
        if question_count1[i] != 0:
            training_variance1[i] = (training_std_dev1[i]/question_count1[i])
            training_std_dev1[i] = (training_std_dev1[i]/question_count1[i]) ** 0.5

    # Some adjustments to all the arrays
    if context != 'look_color':
        #training_mean1[1] = training_mean1[0]
        #training_std_dev1[1] = training_std_dev1[0]
        #training_variance1[1] = training_variance1[0]

        # Write out the question count, training object's mean, training object's variance and their standard deviation to a .csv filesums2 = [0] * 27
        with open(rootdir1 + attr + "\\" + context + '\\Exp2AQC&TrainingObjs.csv', 'w') as csvfile:
            writer = csv.writer(csvfile, delimiter=',')
            for i in range(0, len(training_mean1)):
                if training_mean1[i] != 0:
                    if i == 0:
                        writer.writerow([max2_1, training_mean1[i], training_variance1[i], training_std_dev1[i]]) 
                    else:
                        writer.writerow([i, training_mean1[i], training_variance1[i], training_std_dev1[i]]) 

    else:
        # Write out the question count, training object's mean, training object's variance and their standard deviation to a .csv filesums2 = [0] * 27 for LOOK_COLOR
        with open(rootdir1 + attr + "\\" + context + '\\Exp2AQC&TrainingObjs.csv', 'w') as csvfile:
            writer = csv.writer(csvfile, delimiter=',')
            for i in range(0, len(training_mean1)-1):
                if training_mean1[i] != 0:
                    if i == 0:
                        writer.writerow([max2_1, training_mean1[i], training_variance1[i], training_std_dev1[i]]) 
                    elif (i>1):
                        writer.writerow([i, training_mean1[i], training_variance1[i], training_std_dev1[i]])           


    # For exp2_2
    for i in range(0,len(question_asked2)):
        # Get all question counts from the first column 
        num2 = question_asked2[i]
        index2 = int(num2 % max2_2)              # all 27's go in [0]th index and nothing goes in [1]st index
        question_count2[index2] = question_count2[index2] + 1 
        training_sum2[index2] = training_sum2[index2]+training_objs2[i]    # Sum the no. of training objects

    # Calculate the mean for each question count
    training_mean2 = [0] * max2_2                # The mean of 27 goes into [0]th index and nothing goes into [1]st
    training_variance2 = [0] * max2_2
    training_std_dev2 = [0] * max2_2

    for i in range(0, len(training_sum2)):
        if question_count2[i] != 0:
            training_mean2[i] = training_sum2[i]/question_count2[i]

    for i in range(0, len(question_asked2)):
        num2 = question_asked2[i]
        index2 = int(num2 % max2_2)
        training_std_dev2[index2] = training_std_dev2[index2] + ((training_objs2[i] - training_mean2[index2]) ** 2)

    for i in range(0, len(training_std_dev2)):
        if question_count2[i] != 0:
            training_variance2[i] = (training_std_dev2[i]/question_count2[i])
            training_std_dev2[i] = (training_std_dev2[i]/question_count2[i]) ** 0.5

    # Some adjustments to all the arrays
    if context != 'look_color':
        #training_mean2[1] = training_mean2[0]
        #training_std_dev2[1] = training_std_dev2[0]
        #training_variance2[1] = training_variance2[0]

        # Write out the question count, training object's mean, training object's variance and their standard deviation to a .csv filesums2 = [0] * 27
        with open(rootdir2 + attr + "\\" + context + '\\Exp2ExtraQC&TrainingObjs.csv', 'w') as csvfile:
            writer = csv.writer(csvfile, delimiter=',')
            for i in range(0, len(training_mean2)-1):
                if training_mean2[i] != 0:
                    if i == 0:
                        writer.writerow([max2_2, training_mean2[i], training_variance2[i], training_std_dev2[i]]) 
                    else:
                        writer.writerow([i, training_mean2[i], training_variance2[i], training_std_dev2[i]]) 

    else:
        # Write out the question count, training object's mean, training object's variance and their standard deviation to a .csv filesums2 = [0] * 27 for LOOK_COLOR
        with open(rootdir2 + attr + "\\" + context + '\\Exp2ExtraQC&TrainingObjs.csv', 'w') as csvfile:
            writer = csv.writer(csvfile, delimiter=',')
            for i in range(0, len(training_mean2)):
                if training_mean2[i] != 0:
                    if i == 0:
                        writer.writerow([max2_2, training_mean2[i], training_variance2[i], training_std_dev2[i]]) 
                    elif (i>1):
                        writer.writerow([i, training_mean2[i], training_variance2[i], training_std_dev2[i]])

    # For exp3_1
    for i in range(0,len(question_asked3)):
        # Get all question counts from the first column 
        num3 = question_asked3[i]
        index3 = int(num3 % max3_1)              # all 27's go in [0]th index 
        question_count3[index3] = question_count3[index3] + 1 
        training_sum3[index3] = training_sum3[index3]+training_objs3[i]    # Sum the no. of training objects

    # Calculate the mean for each question count
    training_mean3 = [0] * max3_1                # The mean of 27 goes into [0]th index 
    training_variance3 = [0] * max3_1
    training_std_dev3 = [0] * max3_1

    for i in range(0, len(training_sum3)):
        if question_count3[i] != 0:
            training_mean3[i] = training_sum3[i]/question_count3[i]

    for i in range(0, len(question_asked3)):
        num3 = question_asked3[i]
        index3 = int(num3 % max3_1)
        training_std_dev3[index3] = training_std_dev3[index3] + ((training_objs3[i] - training_mean3[index3]) ** 2)

    for i in range(0, len(training_std_dev3)):
        if question_count3[i] != 0:
            training_variance3[i] = (training_std_dev3[i]/question_count3[i])
            training_std_dev3[i] = (training_std_dev3[i]/question_count3[i]) ** 0.5

    # Some adjustments to all the arrays
    if context != 'look_color':
        # Write out the question count, training object's mean, training object's variance and their standard deviation to a .csv filesums2 = [0] * 27
        with open(rootdir3 + attr + '\\Exp3QC&TrainingObjs.csv', 'w') as csvfile:
            writer = csv.writer(csvfile, delimiter=',')
            for i in range(0, len(training_mean3)-1):
                if training_mean3[i] != 0:
                    if i == 0:
                        writer.writerow([max3_1, training_mean3[i], training_variance3[i], training_std_dev3[i]]) 
                    else:
                        writer.writerow([i, training_mean3[i], training_variance3[i], training_std_dev3[i]]) 

    else:
        # Write out the question count, training object's mean, training object's variance and their standard deviation to a .csv filesums2 = [0] * 27 for LOOK_COLOR
        with open(rootdir3 + attr + '\\Exp3QC&TrainingObjs.csv', 'w') as csvfile:
            writer = csv.writer(csvfile, delimiter=',')
            for i in range(0, len(training_mean3)-1):
                if training_mean3[i] != 0:
                    if i == 0:
                        writer.writerow([max3_1, training_mean3[i], training_variance3[i], training_std_dev3[i]]) 
                    elif (i>1):
                        writer.writerow([i, training_mean3[i], training_variance3[i], training_std_dev3[i]])

    # For exp3_2
    for i in range(0,len(question_asked4)):
        # Get all question counts from the first column 
        num4 = question_asked4[i]
        index4 = int(num4 % max3_2)              # all 27's go in [0]th index 
        question_count4[index4] = question_count4[index4] + 1 
        training_sum4[index4] = training_sum4[index4]+training_objs4[i]    # Sum the no. of training objects

    # Calculate the mean for each question count
    training_mean4 = [0] * max3_2                # The mean of 27 goes into [0]th index 
    training_variance4 = [0] * max3_2
    training_std_dev4 = [0] * max3_2

    for i in range(0, len(training_sum4)):
        if question_count4[i] != 0:
            training_mean4[i] = training_sum4[i]/question_count4[i]

    for i in range(0, len(question_asked4)):
        num4 = question_asked4[i]
        index4 = int(num4 % max3_2)
        training_std_dev4[index4] = training_std_dev4[index4] + ((training_objs4[i] - training_mean4[index4]) ** 2)

    for i in range(0, len(training_std_dev4)):
        if question_count4[i] != 0:
            training_variance4[i] = (training_std_dev4[i]/question_count4[i])
            training_std_dev4[i] = (training_std_dev4[i]/question_count4[i]) ** 0.5

    # Some adjustments to all the arrays
    if context != 'look_color':
        # Write out the question count, training object's mean, training object's variance and their standard deviation to a .csv filesums2 = [0] * 27
        with open(rootdir3 + attr + '\\Exp3ExtraQC&TrainingObjs.csv', 'w') as csvfile:
            writer = csv.writer(csvfile, delimiter=',')
            for i in range(0, len(training_mean4)-1):
                if training_mean4[i] != 0:
                    if i == 0:
                        writer.writerow([max3_2, training_mean4[i], training_variance4[i], training_std_dev4[i]]) 
                    else:
                        writer.writerow([i, training_mean4[i], training_variance4[i], training_std_dev4[i]]) 

    else:
        # Write out the question count, training object's mean, training object's variance and their standard deviation to a .csv filesums2 = [0] * 27 for LOOK_COLOR
        with open(rootdir3 + attr + '\\Exp3ExtraQC&TrainingObjs.csv', 'w') as csvfile:
            writer = csv.writer(csvfile, delimiter=',')
            for i in range(0, len(training_mean4)-1):
                if training_mean4[i] != 0:
                    if i == 0:
                        writer.writerow([max3_2, training_mean4[i], training_variance4[i], training_std_dev4[i]]) 
                    elif (i>1):
                        writer.writerow([i, training_mean4[i], training_variance4[i], training_std_dev4[i]]) 
    """
    # Reload the files and draw a plot
    exp2_1 = np.genfromtxt(rootdir1 + attr + "\\" + context + '\\Exp2QC&TrainingObjs.csv', delimiter=',')
    exp2_2 = np.genfromtxt(rootdir2 + attr + "\\" + context + '\\Exp2ExtraQC&TrainingObjs.csv', delimiter=',')
    exp3_1 = np.genfromtxt(rootdir3 + attr + '\\Exp3QC&TrainingObjs.csv', delimiter=',')
    exp3_2 = np.genfromtxt(rootdir3 + attr + '\\Exp3ExtraQC&TrainingObjs.csv', delimiter=',')
    
    trace1 = go.Scatter(x = question_asked0, y = training_objs0, mode="lines+markers", name = 'Experiment 1', marker = dict(symbol = 'diamond', size = '10'))

    trace2 = go.Scatter(x = exp2_1[:, 0], y = exp2_1[:, 1], mode = "lines+markers", name = 'Experiment 2a w/o Skip Questions', error_y = dict(type ='data', array = exp2_1[:,3], visible = True), marker = dict(
      	symbol = 'triangle-up', size = '10'))

    trace3 = go.Scatter(x = exp2_2[:, 0], y = exp2_2[:, 1], mode = "lines+markers", name = 'Experiment 2a w/ Skip Questions', error_y = dict(type ='data', array = exp2_2[:,3], visible = True), marker = dict(
      	symbol = 'circle', size = '10'))
    
    trace4 = go.Scatter(x = exp3_1[:, 0], y = exp3_1[:, 1], mode = "lines+markers", name = 'Experiment 2b w/o Skip Questions', error_y = dict(type ='data', array = exp3_1[:,3], visible = True), marker = dict(
      	symbol = 'hourglass', size = '10'))

    trace5 = go.Scatter(x = exp3_2[:, 0], y = exp3_2[:, 1], mode = "lines+markers", name = 'Experiment 2b w/ Skip Questions', error_y = dict(type ='data', array = exp3_2[:,3], visible = True), marker = dict(
      	symbol = 'star', size = '10'))
    
    data = [trace1, trace2, trace3, trace4, trace5]

    layout= go.Layout(
        title= 'Questions asked VS Training objects for ' + context + ' (Attribute learnt: ' + attr + ')',
        barmode='group',
        xaxis= dict(
            title= 'Questions asked',
            zeroline= True,
            gridwidth= 2,
        ),
        yaxis=dict(
            title= 'Training Objects',
            zeroline= True,
            gridwidth= 2,
        ),
        showlegend= True,
    )

    fig= go.Figure(data=data, layout=layout)
    #py.plot(fig)
    py.image.save_as(fig, filename = "tr_" + context + '_' + attr + '.png')
