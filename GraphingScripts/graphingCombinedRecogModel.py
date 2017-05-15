import csv
import os
import pandas as pd
import numpy as np
import plotly.tools as tls
tls.set_credentials_file(username='pkhante', api_key='l002tdvw1k')
import plotly.plotly as py
import plotly.graph_objs as go

py.sign_in('pkhante', 'l002tdvw1k')

rootdir1 = '/home/priyanka/Documents/grounded_language_learning/SinglyAnnotatedObjectTrials/CombinedContextTrialResults/'
rootdir2 = '/home/priyanka/Documents/grounded_language_learning/AutomatedExpNewAlgo/CombinedRecogModelResults/'

contexts_list = ["drop_audio", "revolve_audio","push_audio", "hold_haptics", "lift_haptics", "press_haptics","squeeze_haptics","grasp_size", "shake_audio", "look_color","look_shape", "grasp_audio", "hold_audio", "lift_audio", "poke_audio", "press_audio", "squeeze_audio", "drop_haptics", "poke_haptics", "revolve_haptics", "push_haptics", "shake_haptics", "grasp_haptics"]

attr_list = ["weight"] #"color", "size", "material", "shape", "deformable", "height", "has_contents"

for attr in attr_list:
    context = ''
    if attr == 'size':
        context = 'grasp_haptics'
    if attr == 'material':
        context = 'push_audio'
    if attr == 'has_contents':
        context = 'shake_audio'
    if attr == 'weight':
        context = 'push_haptics'
    if attr == 'color':
        context = 'look_color'
    if attr == 'height':
        context = 'press_haptics'
    if attr == 'deformable':
        context = 'revolve_haptics'
    if attr == 'shape':
        context = 'look_shape'
    
    # Read csv file and put the values in an array
    filepath_exp0 = rootdir1 + attr + '/PointsToPlot.csv'
    exp1 = np.genfromtxt(filepath_exp0, delimiter=',')

    # Read csv file and put the values in an array
    filepath_exp1 = rootdir2 + attr + '/' + context + '/PointsToPlot.csv'
    exp2_1 = np.genfromtxt(filepath_exp1, delimiter=',')
    exp2_2 = np.genfromtxt(filepath_exp1, delimiter=',')

    df0 = pd.DataFrame({"Kappa": exp1[:, 3], "QuestionCount": exp1[:, 0]})
    df1 = pd.DataFrame({"Kappa": exp2_1[:, 5], "QuestionCount": exp2_1[:, 1]})
    df2 = pd.DataFrame({"Kappa": exp2_2[:, 5], "QuestionCount": exp2_2[:, 2]})

    # Get the highest question count
    max2_1 = df1.QuestionCount.max()
    max2_2 = df2.QuestionCount.max()

    question_asked1 = []
    kappa1 = [] 
    question_asked2 = []
    kappa2 = [] 
    question_asked0 = []
    kappa0 = []

    kappa_sum0 = [0] * 27
    question_count2 = [0] * 27
    kappa_sum2 = [0] * 27
    question_count1 = [0] * 27
    kappa_sum1 = [0] * 27

    # For exp1
    for i in range(0,len(df0.index)):
        question_asked0.append(df0.iloc[i].QuestionCount)
        kappa0.append(df0.iloc[i].Kappa)

    for i in range(0,len(question_asked0)):
        # Get all question counts from the first column 
        num0 = question_asked0[i]
        index0 = int(num0 % 27)              # all 27's go in [0]th index and nothing goes in [1]st index
        kappa_sum0[index0] = kappa_sum0[index0]+exp1[i,3]

    # Calculate the mean for each question count
    kappa_mean0 = [0] * 27             # The mean of 27 goes into [0]th index and nothing goes into [1]st
    kappa_variance0 = [0] * 27
    kappa_std_dev0 = [0] * 27

    for i in range(0, len(kappa_sum0)):
        kappa_mean0[i] = kappa_sum0[i]/100

    for i in range(0, len(question_asked0)):
        num0 = question_asked0[i]
        index0 = int(num0 % 27)
        kappa_std_dev0[index0] = kappa_std_dev0[index0] + ((kappa0[i] - kappa_mean0[index0]) ** 2)

    for i in range(0, len(kappa_std_dev0)):
        kappa_variance0[i] = (kappa_std_dev0[i]/100)
        kappa_std_dev0[i] = (kappa_std_dev0[i]/100) ** 0.5

    # Some adjustments to all the arrays
    kappa_mean0[1] = kappa_mean0[0]
    kappa_std_dev0[1] = kappa_std_dev0[0]
    kappa_variance0[1] = kappa_variance0[0]

    # Write out the question count, kappa's mean, kappa's variance and their standard deviation to a .csv file
    extras = [0] * 4
    with open(rootdir1 + '/' + attr + '/CombinedRecogModelQC&Kappa.csv', 'wb') as csvfile:
        writer = csv.writer(csvfile, delimiter=',')
        for i in range(1, len(kappa_mean0)):
            if kappa_mean0[i] != 0:
                if i == 1:
                    extras[0] = 27
                    extras[1] = kappa_mean0[i]
                    extras[2] = kappa_variance0[i]
                    extras[3] = kappa_std_dev0[i]
                else:
                    writer.writerow([i, kappa_mean0[i], kappa_variance0[i], kappa_std_dev0[i]]) 
        if (extras[0] != 0):
            writer.writerow([extras[0], extras[1], extras[2], extras[3]])

    # exp2_1
    for i in range(0,len(df1.index)):
        # Reached a new trial
        if i != len(df1.index)-1:
            if df1.iloc[i+1].QuestionCount < df1.iloc[i].QuestionCount: 
                if df1.iloc[i].QuestionCount == max2_1:
                    question_asked1.append(df1.iloc[i].QuestionCount)
                    kappa1.append(df1.iloc[i].Kappa)
                    continue
                else:
                    for j in range(0, int(1+max2_1-df1.iloc[i].QuestionCount)):
                        question_asked1.append(float(df1.iloc[i].QuestionCount+j))
                        kappa1.append(float(df1.iloc[i].Kappa))
            else:
                if df1.iloc[i+1].QuestionCount-df1.iloc[i].QuestionCount == 1:
                    question_asked1.append(df1.iloc[i].QuestionCount)
                    kappa1.append(df1.iloc[i].Kappa)
                else:
                    for j in range(0, int(df1.iloc[i+1].QuestionCount-df1.iloc[i].QuestionCount)):
                        question_asked1.append(float(df1.iloc[i].QuestionCount+j))
                        kappa1.append(df1.iloc[i].Kappa)
        elif i == len(df1.index)-1 and df1.iloc[i].QuestionCount != max2_1:
            for j in range(0, int(1+max2_1-df1.iloc[i].QuestionCount)):
                question_asked1.append(df1.iloc[i].QuestionCount+j)
                kappa1.append(df1.iloc[i].Kappa)

    # exp2_2
    for i in range(0,len(df2.index)):
        if i != len(df2.index)-1:
            if df2.iloc[i+1].QuestionCount < df2.iloc[i].QuestionCount: 
                if df2.iloc[i].QuestionCount == max2_2:
                    question_asked2.append(df2.iloc[i].QuestionCount)
                    kappa2.append(df2.iloc[i].Kappa)
                    continue
                else:
                    for j in range(0, int(1+max2_2-df2.iloc[i].QuestionCount)):
                        question_asked2.append(float(df2.iloc[i].QuestionCount+j))
                        kappa2.append(float(df2.iloc[i].Kappa))
            else:
                if df2.iloc[i+1].QuestionCount-df2.iloc[i].QuestionCount == 1:
                    question_asked2.append(df2.iloc[i].QuestionCount)
                    kappa2.append(df2.iloc[i].Kappa)
                else:
                    for j in range(0, int(df2.iloc[i+1].QuestionCount-df2.iloc[i].QuestionCount)):
                        question_asked2.append(float(df2.iloc[i].QuestionCount+j))
                        kappa2.append(df2.iloc[i].Kappa)
        elif i == len(df2.index)-1 and df2.iloc[i].QuestionCount != max2_2:
            for j in range(0, int(1+max2_2-df2.iloc[i].QuestionCount)):
                question_asked2.append(df2.iloc[i].QuestionCount+j)
                kappa2.append(df2.iloc[i].Kappa)

    # For exp 2_1
    for i in range(0,len(question_asked1)):
        # Get all question counts from the first column 
        num1 = question_asked1[i]
        index1 = int(num1 % 27)           # all 27's go in [0]th index and nothing goes in [1]st index
        question_count1[index1] = question_count1[index1] + 1 
        kappa_sum1[index1] = kappa_sum1[index1]+kappa1[i]    # Sum the no. of training objects

    # Calculate the mean for each question count
    kappa_mean1 = [0] * 27             # The mean of 27 goes into [0]th index and nothing goes into [1]st
    kappa_variance1 = [0] * 27
    kappa_std_dev1 = [0] * 27

    for i in range(0, len(kappa_sum1)):
        if question_count1[i] != 0:
            kappa_mean1[i] = kappa_sum1[i]/question_count1[i]

    for i in range(0, len(question_asked1)):
        num1 = question_asked1[i]
        index1 = int(num1 % 27)
        kappa_std_dev1[index1] = kappa_std_dev1[index1] + ((kappa1[i] - kappa_mean1[index1]) ** 2)

    for i in range(0, len(kappa_std_dev1)):
        if question_count1[i] != 0:
            kappa_variance1[i] = (kappa_std_dev1[i]/question_count1[i])
            kappa_std_dev1[i] = (kappa_std_dev1[i]/question_count1[i]) ** 0.5

    # Some adjustments to all the arrays
    if context != 'look_color':
        extra = [0] * 4 
        kappa_mean1[1] = kappa_mean1[0]
        kappa_std_dev1[1] = kappa_std_dev1[0]
        kappa_variance1[1] = kappa_variance1[0]

        # Write out the question count, training object's mean, training object's variance and their standard deviation to a .csv filesums2 = [0] * 27
        with open(rootdir2 + attr + '/' + context + '/CombinedRecogModelQC&Kappa.csv', 'wb') as csvfile:
            writer = csv.writer(csvfile, delimiter=',')
            for i in range(1, len(kappa_mean1)):
                if kappa_mean1[i] != 0:
                    if i == 1:
                        extra[0] = 27
                        extra[1] = kappa_mean1[i]
                        extra[2] = kappa_variance1[i]
                        extra[3] = kappa_std_dev1[i]
                    else:
                        writer.writerow([i, kappa_mean1[i], kappa_variance1[i], kappa_std_dev1[i]]) 
            if (extra[0] != 0):
                writer.writerow([extra[0], extra[1], extra[2], extra[3]])

    else:
        extra = [0] * 8 
        # Write out the question count, training object's mean, training object's variance and their standard deviation to a .csv filesums2 = [0] * 27 for LOOK_COLOR
        with open(rootdir2 + attr + '/' + context + '/CombinedRecogModelQC&Kappa.csv', 'wb') as csvfile:
            writer = csv.writer(csvfile, delimiter=',')
            for i in range(0, len(kappa_mean1)):
                if kappa_mean1[i] != 0:
                    if i == 0:
                        extra[0] = 27
                        extra[1] = kappa_mean1[i]
                        extra[2] = kappa_variance1[i]
                        extra[3] = kappa_std_dev1[i]
                    elif i == 1:
                        extra[4] = 28
                        extra[5] = kappa_mean1[i]
                        extra[6] = kappa_variance1[i]
                        extra[7] = kappa_std_dev1[i]
                    elif (i>1):
                        writer.writerow([i, kappa_mean1[i], kappa_variance1[i], kappa_std_dev1[i]])    
            if(extra[0] != 0):
                writer.writerow([extra[0], extra[1], extra[2], extra[3]])
                writer.writerow([extra[4], extra[5], extra[6], extra[7]])

    # For exp2_2
    for i in range(0,len(question_asked2)):
        # Get all question counts from the first column 
        num2 = question_asked2[i]
        index2 = int(num2 % 27)              # all 27's go in [0]th index and nothing goes in [1]st index
        question_count2[index2] = question_count2[index2] + 1 
        kappa_sum2[index2] = kappa_sum2[index2]+kappa2[i]    # Sum the no. of training objects

    # Calculate the mean for each question count
    kappa_mean2 = [0] * 27                # The mean of 27 goes into [0]th index and nothing goes into [1]st
    kappa_variance2 = [0] * 27
    kappa_std_dev2 = [0] * 27

    for i in range(0, len(kappa_sum2)):
        if question_count2[i] != 0:
            kappa_mean2[i] = kappa_sum2[i]/question_count2[i]

    for i in range(0, len(question_asked2)):
        num2 = question_asked2[i]
        index2 = int(num2 % 27)
        kappa_std_dev2[index2] = kappa_std_dev2[index2] + ((kappa2[i] - kappa_mean2[index2]) ** 2)

    for i in range(0, len(kappa_std_dev2)):
        if question_count2[i] != 0:
            kappa_variance2[i] = (kappa_std_dev2[i]/question_count2[i])
            kappa_std_dev2[i] = (kappa_std_dev2[i]/question_count2[i]) ** 0.5

    # Some adjustments to all the arrays
    if context != 'look_color':
        kappa_mean2[1] = kappa_mean2[0]
        kappa_std_dev2[1] = kappa_std_dev2[0]
        kappa_variance2[1] = kappa_variance2[0]

        # Write out the question count, training object's mean, training object's variance and their standard deviation to a .csv filesums2 = [0] * 27
        extra = [0] * 4 
        with open(rootdir2 + attr + '/' + context + '/CombinedRecogModelQC&Kappa2.csv', 'wb') as csvfile:
            writer = csv.writer(csvfile, delimiter=',')
            for i in range(1, len(kappa_mean2)):
                if kappa_mean2[i] != 0:
                    if i == 1:
                        extra[0] = 27
                        extra[1] = kappa_mean2[i]
                        extra[2] = kappa_variance2[i]
                        extra[3] = kappa_std_dev2[i]
                    else:
                        writer.writerow([i, kappa_mean2[i], kappa_variance2[i], kappa_std_dev2[i]]) 
            if (extra[0] != 0):
                writer.writerow([extra[0], extra[1], extra[2], extra[3]])

    else:
        # Write out the question count, training object's mean, training object's variance and their standard deviation to a .csv filesums2 = [0] * 27 for LOOK_COLOR
        extra = [0] * 8
        with open(rootdir2 + attr + '/' + context + '/CombinedRecogModelQC&Kappa2.csv', 'wb') as csvfile:
            writer = csv.writer(csvfile, delimiter=',')
            for i in range(0, len(kappa_mean2)):
                if kappa_mean2[i] != 0:
                    if i == 0:
                        extra[0] = 27
                        extra[1] = kappa_mean2[i]
                        extra[2] = kappa_variance2[i]
                        extra[3] = kappa_std_dev2[i]
                    elif i == 1:
                        extra[4] = 28
                        extra[5] = kappa_mean2[i]
                        extra[6] = kappa_variance2[i]
                        extra[7] = kappa_std_dev2[i]
                    elif (i>1):
                        writer.writerow([i, kappa_mean2[i], kappa_variance2[i], kappa_std_dev2[i]]) 
            if(extra[0] != 0):
                writer.writerow([extra[0], extra[1], extra[2], extra[3]])
                writer.writerow([extra[4], extra[5], extra[6], extra[7]])  

    # Reload the files and draw a plot
    exp1_0 = np.genfromtxt(rootdir1 + attr + '/CombinedRecogModelQC&Kappa.csv', delimiter=',')
    exp2_1 = np.genfromtxt(rootdir2 + attr + '/' + context + '/CombinedRecogModelQC&Kappa.csv', delimiter=',')
    exp2_2 = np.genfromtxt(rootdir2 + attr + '/' + context + '/CombinedRecogModelQC&Kappa2.csv', delimiter=',')

    trace1 = go.Scatter(x = exp1_0[:, 0], y = exp1_0[:, 1], mode = "lines+markers", name = 'Experiment 1', error_y = dict(type ='data', array = exp1_0[:,2], visible = True), marker = dict(
      	symbol = 'diamond', size = '10'))

    trace2 = go.Scatter(x = exp2_1[:, 0], y = exp2_1[:, 1], mode = "lines+markers", name = 'Experiment 2', error_y = dict(type ='data', array = exp2_1[:,2], visible = True), marker = dict(
      	symbol = 'triangle-up', size = '10'))

    trace3 = go.Scatter(x = exp2_2[:, 0], y = exp2_2[:, 1], mode = "lines+markers", name = 'Experiment 3', error_y = dict(type ='data', array = exp2_1[:,2], visible = True), marker = dict(
      	symbol = 'circle', size = '10'))

    data = [trace1, trace2, trace3]

    layout= go.Layout(
        title= 'Questions asked VS Kappa co-efficients (Attribute learnt: ' + attr + ')',
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
    py.image.save_as(fig, filename = context + '_' + attr + '.png')  

