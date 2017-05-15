# In this file training objs are actually Kappa
import csv
import os
import pandas as pd
import numpy as np
import plotly.tools as tls
tls.set_credentials_file(username='pkhante', api_key='l002tdvw1k')
import plotly.plotly as py
import plotly.graph_objs as go

py.sign_in('pkhante', 'l002tdvw1k')

rootdir0 = '/home/priyanka/Documents/grounded_language_learning/SinglyAnnotatedObjectTrials/SinglyAnnotatedResults/'
rootdir1 = '/home/priyanka/Documents/grounded_language_learning/AutomatedExpNewAlgo/results_exp2/'
rootdir2 = '/home/priyanka/Documents/grounded_language_learning/AutomatedExpNewAlgo/results_extraQC/'

contexts_list = ['drop_audio', 'grasp_size', 'hold_haptics', 'lift_haptics', 'look_color', 'look_shape', 'press_haptics', 'push_audio', 'revolve_audio', 'shake_audio', 'squeeze_haptics']

for context in contexts_list:
    attr = ""
    if context == 'drop_audio':
        attr = 'Material'
    if context == 'grasp_size':
        attr = 'Size (Width)'
    if context == 'hold_haptics':
        attr = 'Weight'
    if context == 'lift_haptics':
        attr = 'Weight'
    if context == 'look_color':
        attr = 'Color'
    if context == 'look_shape':
        attr = 'Shape'
    if context == 'press_haptics':
        attr = 'Height'
    if context == 'push_audio':
        attr = 'Material'
    if context == 'revolve_audio':
        attr = 'Filled/Empty'
    if context == 'shake_audio':
        attr = 'Filled/Empty'
    if context == 'squeeze_haptics':
        attr = 'Deformable'

    # Read csv file and put the values in an array
    filepath_exp0 = rootdir0 + context + '/PointsToPlot.csv'
    exp1 = np.genfromtxt(filepath_exp0, delimiter=',')

    # Read csv file and put the values in an array
    filepath_exp1 = rootdir1 + context + '/PointsToPlot.csv'
    exp2_1 = np.genfromtxt(filepath_exp1, delimiter=',')

    filepath_exp2 = rootdir2 + context + '/PointsToPlot.csv'
    exp2_2 = np.genfromtxt(filepath_exp2, delimiter=',')

    df0 = pd.DataFrame({"TrainingObjects": exp1[:, 2], "QuestionCount": exp1[:, 0]})
    df1 = pd.DataFrame({"TrainingObjects": exp2_1[:, 3], "QuestionCount": exp2_1[:, 0]})
    df2 = pd.DataFrame({"TrainingObjects": exp2_2[:, 3], "QuestionCount": exp2_2[:, 0]})

    # Get the highest question count
    max2_1 = df1.QuestionCount.max()
    max2_2 = df2.QuestionCount.max()

    #print ("max2_1 for " + context + ": " + str(max2_1))
    #print ("max2_2 for " + context + ": " + str(max2_2))

    question_asked1 = []
    training_objs1 = [] 
    question_asked2 = []
    training_objs2 = [] 
    question_asked0 = []
    training_objs0 = []

    training_sum0 = [0] * 27
    question_count2 = [0] * 27
    training_sum2 = [0] * 27
    question_count1 = [0] * 27
    training_sum1 = [0] * 27

    # For exp1
    for i in range(0,len(df0.index)):
        question_asked0.append(df0.iloc[i].QuestionCount)
        training_objs0.append(df0.iloc[i].TrainingObjects)

    for i in range(0,len(question_asked0)):
        # Get all question counts from the first column 
        num0 = question_asked0[i]
        index0 = int(num0 % 27)              # all 27's go in [0]th index and nothing goes in [1]st index
        training_sum0[index0] = training_sum0[index0]+exp1[i,2]

    # Calculate the mean for each question count
    training_mean0 = [0] * 27             # The mean of 27 goes into [0]th index and nothing goes into [1]st
    training_variance0 = [0] * 27
    training_std_dev0 = [0] * 27

    for i in range(0, len(training_sum0)):
        training_mean0[i] = training_sum0[i]/100

    for i in range(0, len(question_asked0)):
        num0 = question_asked0[i]
        index0 = int(num0 % 27)
        training_std_dev0[index0] = training_std_dev0[index0] + ((training_objs0[i] - training_mean0[index0]) ** 2)

    for i in range(0, len(training_std_dev0)):
        training_variance0[i] = (training_std_dev0[i]/100)
        training_std_dev0[i] = (training_std_dev0[i]/100) ** 0.5

    # Some adjustments to all the arrays
    training_mean0[1] = training_mean0[0]
    training_std_dev0[1] = training_std_dev0[0]
    training_variance0[1] = training_variance0[0]

    # Write out the question count, training object's mean, training object's variance and their standard deviation to a .csv file
    extras = [0] * 4
    with open(rootdir0 + context + '/Exp1QC&Kappas.csv', 'wb') as csvfile:
        writer = csv.writer(csvfile, delimiter=',')
        for i in range(1, len(training_mean0)):
            if training_mean0[i] != 0:
                if i == 1:
                    extras[0] = 27
                    extras[1] = training_mean0[i]
                    extras[2] = training_variance0[i]
                    extras[3] = training_std_dev0[i]
                else:
                    writer.writerow([i, training_mean0[i], training_variance0[i], training_std_dev0[i]]) 
        if (extras[0] != 0):
            writer.writerow([extras[0], extras[1], extras[2], extras[3]])

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
                        training_objs1.append(float(df1.iloc[i].TrainingObjects))
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
                training_objs1.append(df1.iloc[i].TrainingObjects)

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
                        training_objs2.append(float(df2.iloc[i].TrainingObjects))
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
                training_objs2.append(df2.iloc[i].TrainingObjects)

    # For exp 2_1
    for i in range(0,len(question_asked1)):
        # Get all question counts from the first column 
        num1 = question_asked1[i]
        index1 = int(num1 % 27)           # all 27's go in [0]th index and nothing goes in [1]st index
        question_count1[index1] = question_count1[index1] + 1 
        training_sum1[index1] = training_sum1[index1]+training_objs1[i]    # Sum the no. of training objects

    # Calculate the mean for each question count
    training_mean1 = [0] * 27             # The mean of 27 goes into [0]th index and nothing goes into [1]st
    training_variance1 = [0] * 27
    training_std_dev1 = [0] * 27

    for i in range(0, len(training_sum1)):
        if question_count1[i] != 0:
            training_mean1[i] = training_sum1[i]/question_count1[i]

    for i in range(0, len(question_asked1)):
        num1 = question_asked1[i]
        index1 = int(num1 % 27)
        training_std_dev1[index1] = training_std_dev1[index1] + ((training_objs1[i] - training_mean1[index1]) ** 2)

    for i in range(0, len(training_std_dev1)):
        if question_count1[i] != 0:
            training_variance1[i] = (training_std_dev1[i]/question_count1[i])
            training_std_dev1[i] = (training_std_dev1[i]/question_count1[i]) ** 0.5

    # Some adjustments to all the arrays
    if context != 'look_color':
        extra = [0] * 4 
        training_mean1[1] = training_mean1[0]
        training_std_dev1[1] = training_std_dev1[0]
        training_variance1[1] = training_variance1[0]

        # Write out the question count, training object's mean, training object's variance and their standard deviation to a .csv filesums2 = [0] * 27
        with open(rootdir1 + context + '/Exp2QC&Kappas.csv', 'wb') as csvfile:
            writer = csv.writer(csvfile, delimiter=',')
            for i in range(1, len(training_mean1)):
                if training_mean1[i] != 0:
                    if i == 1:
                        extra[0] = 27
                        extra[1] = training_mean1[i]
                        extra[2] = training_variance1[i]
                        extra[3] = training_std_dev1[i]
                    else:
                        writer.writerow([i, training_mean1[i], training_variance1[i], training_std_dev1[i]]) 
            if (extra[0] != 0):
                writer.writerow([extra[0], extra[1], extra[2], extra[3]])

    else:
        extra = [0] * 8 
        # Write out the question count, training object's mean, training object's variance and their standard deviation to a .csv filesums2 = [0] * 27 for LOOK_COLOR
        with open(rootdir1 + context + '/Exp2QC&Kappas.csv', 'wb') as csvfile:
            writer = csv.writer(csvfile, delimiter=',')
            for i in range(0, len(training_mean1)):
                if training_mean1[i] != 0:
                    if i == 0:
                        extra[0] = 27
                        extra[1] = training_mean1[i]
                        extra[2] = training_variance1[i]
                        extra[3] = training_std_dev1[i]
                    elif i == 1:
                        extra[4] = 28
                        extra[5] = training_mean1[i]
                        extra[6] = training_variance1[i]
                        extra[7] = training_std_dev1[i]
                    elif (i>1):
                        writer.writerow([i, training_mean1[i], training_variance1[i], training_std_dev1[i]])    
            if(extra[0] != 0):
                writer.writerow([extra[0], extra[1], extra[2], extra[3]])
                writer.writerow([extra[4], extra[5], extra[6], extra[7]])          
       


    # For exp2_2
    for i in range(0,len(question_asked2)):
        # Get all question counts from the first column 
        num2 = question_asked2[i]
        index2 = int(num2 % 27)              # all 27's go in [0]th index and nothing goes in [1]st index
        question_count2[index2] = question_count2[index2] + 1 
        training_sum2[index2] = training_sum2[index2]+training_objs2[i]    # Sum the no. of training objects

    # Calculate the mean for each question count
    training_mean2 = [0] * 27                # The mean of 27 goes into [0]th index and nothing goes into [1]st
    training_variance2 = [0] * 27
    training_std_dev2 = [0] * 27

    for i in range(0, len(training_sum2)):
        if question_count2[i] != 0:
            training_mean2[i] = training_sum2[i]/question_count2[i]

    for i in range(0, len(question_asked2)):
        num2 = question_asked2[i]
        index2 = int(num2 % 27)
        training_std_dev2[index2] = training_std_dev2[index2] + ((training_objs2[i] - training_mean2[index2]) ** 2)

    for i in range(0, len(training_std_dev2)):
        if question_count2[i] != 0:
            training_variance2[i] = (training_std_dev2[i]/question_count2[i])
            training_std_dev2[i] = (training_std_dev2[i]/question_count2[i]) ** 0.5

    # Some adjustments to all the arrays
    if context != 'look_color':
        training_mean2[1] = training_mean2[0]
        training_std_dev2[1] = training_std_dev2[0]
        training_variance2[1] = training_variance2[0]

        # Write out the question count, training object's mean, training object's variance and their standard deviation to a .csv filesums2 = [0] * 27
        extra = [0] * 4 
        with open(rootdir2 + context + '/Exp2ExtraQC&Kappas.csv', 'wb') as csvfile:
            writer = csv.writer(csvfile, delimiter=',')
            for i in range(1, len(training_mean2)):
                if training_mean2[i] != 0:
                    if i == 1:
                        extra[0] = 27
                        extra[1] = training_mean2[i]
                        extra[2] = training_variance2[i]
                        extra[3] = training_std_dev2[i]
                    else:
                        writer.writerow([i, training_mean2[i], training_variance2[i], training_std_dev2[i]]) 
            if (extra[0] != 0):
                writer.writerow([extra[0], extra[1], extra[2], extra[3]])

    else:
        # Write out the question count, training object's mean, training object's variance and their standard deviation to a .csv filesums2 = [0] * 27 for LOOK_COLOR
        extra = [0] * 8
        with open(rootdir2 + context + '/Exp2ExtraQC&Kappas.csv', 'wb') as csvfile:
            writer = csv.writer(csvfile, delimiter=',')
            for i in range(0, len(training_mean2)):
                if training_mean2[i] != 0:
                    if i == 0:
                        extra[0] = 27
                        extra[1] = training_mean2[i]
                        extra[2] = training_variance2[i]
                        extra[3] = training_std_dev2[i]
                    elif i == 1:
                        extra[4] = 28
                        extra[5] = training_mean2[i]
                        extra[6] = training_variance2[i]
                        extra[7] = training_std_dev2[i]
                    elif (i>1):
                        writer.writerow([i, training_mean2[i], training_variance2[i], training_std_dev2[i]]) 
            if(extra[0] != 0):
                writer.writerow([extra[0], extra[1], extra[2], extra[3]])
                writer.writerow([extra[4], extra[5], extra[6], extra[7]])          

    # Reload the files and draw a plot
    exp1_0 = np.genfromtxt(rootdir0 + context + '/Exp1QC&Kappas.csv', delimiter=',')
    exp2_1 = np.genfromtxt(rootdir1 + context + '/Exp2QC&Kappas.csv', delimiter=',')
    exp2_2 = np.genfromtxt(rootdir2 + context + '/Exp2ExtraQC&Kappas.csv', delimiter=',')

    trace1 = go.Scatter(x = exp1_0[:, 0], y = exp1_0[:, 1], mode = "lines+markers", name = 'Experiment 1', error_y = dict(type ='data', array = exp1_0[:,2], visible = True), marker = dict(
      	symbol = 'diamond', size = '10'))

    trace2 = go.Scatter(x = exp2_1[:, 0], y = exp2_1[:, 1], mode = "lines+markers", name = 'Experiment 2', error_y = dict(type ='data', array = exp2_1[:,2], visible = True), marker = dict(
      	symbol = 'triangle-up', size = '10'))

    trace3 = go.Scatter(x = exp2_2[:, 0], y = exp2_2[:, 1], mode = "lines+markers", name = 'Experiment 3', error_y = dict(type ='data', array = exp2_1[:,2], visible = True), marker = dict(
      	symbol = 'circle', size = '10'))

    data = [trace1, trace2, trace3]

    layout= go.Layout(
        title= 'Questions asked VS Kappa co-efficients for ' + context + ' (Attribute learnt: ' + attr + ')',
        barmode='group',
        xaxis= dict(
            title= 'Questions asked',
            zeroline= True,
            gridwidth= 2,
        ),
        yaxis=dict(
            title= 'Kappa co-efficients',
            zeroline= True,
            gridwidth= 2,
        ),
        showlegend= True,
    )

    fig= go.Figure(data=data, layout=layout)
    #py.plot(fig)
    py.image.save_as(fig, filename = context +'.png')