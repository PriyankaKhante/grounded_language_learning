import csv
import os
import pandas as pd
import numpy as np
import plotly.tools as tls
tls.set_credentials_file(username='pkhante', api_key='l002tdvw1k')
import plotly.plotly as py
import plotly.graph_objs as go

py.sign_in('pkhante', 'l002tdvw1k')

rootdir1 = '/home/priyanka/Documents/grounded_language_learning/AutomatedExpNewAlgo/ContextAttrMappingResults/'

contexts_list = ['drop_audio', 'grasp_size', 'hold_haptics', 'lift_haptics', 'look_color', 'look_shape', 'press_haptics', 'push_audio', 'revolve_audio', 'shake_audio', 'squeeze_haptics']

attributes_list = ['color', 'deformable', 'deformable_from_top', 'has_contents', 'height', 'material', 'shape', 'size', 'weight']

for context in contexts_list:
    # Read csv file and put the values in an array
    filepath_exp1_1 = rootdir1 + context + '/' + attributes_list[0] + '/PointsToPlot.csv'
    exp1_1 = np.genfromtxt(filepath_exp1_1, delimiter=',')

    filepath_exp1_2 = rootdir1 + context + '/' + attributes_list[1] '/PointsToPlot.csv'
    exp1_2 = np.genfromtxt(filepath_exp1_2, delimiter=',')

    filepath_exp1_3 = rootdir1 + context + '/' + attributes_list[2] + '/PointsToPlot.csv'
    exp1_3 = np.genfromtxt(filepath_exp1_3, delimiter=',')

    filepath_exp1_4 = rootdir1 + context + '/' + attributes_list[3] + '/PointsToPlot.csv'
    exp1_4 = np.genfromtxt(filepath_exp1_4, delimiter=',')

    filepath_exp1_5 = rootdir1 + context + '/' + attributes_list[4] + '/PointsToPlot.csv'
    exp1_5 = np.genfromtxt(filepath_exp1_5, delimiter=',')

    filepath_exp1_6 = rootdir1 + context + '/' + attributes_list[5] + '/PointsToPlot.csv'
    exp1_6 = np.genfromtxt(filepath_exp1_6, delimiter=',')

    filepath_exp1_7 = rootdir1 + context + '/' + attributes_list[6] + '/PointsToPlot.csv'
    exp1_7 = np.genfromtxt(filepath_exp1_7, delimiter=',')

    filepath_exp1_8 = rootdir1 + context + '/' + attributes_list[7] + '/PointsToPlot.csv'
    exp1_8 = np.genfromtxt(filepath_exp1_8, delimiter=',')
 
    filepath_exp1_9 = rootdir1 + context + '/' + attributes_list[8] + '/PointsToPlot.csv'
    exp1_9 = np.genfromtxt(filepath_exp1_9, delimiter=',')

    df0 = pd.DataFrame({"Kappa": exp1_1[:, 3], "QuestionCount": exp1_1[:, 0]})
    df1 = pd.DataFrame({"Kappa": exp1_2[:, 3], "QuestionCount": exp1_2[:, 0]})
    df2 = pd.DataFrame({"Kappa": exp1_3[:, 3], "QuestionCount": exp1_3[:, 0]})
    df3 = pd.DataFrame({"Kappa": exp1_4[:, 3], "QuestionCount": exp1_4[:, 0]})
    df4 = pd.DataFrame({"Kappa": exp1_5[:, 3], "QuestionCount": exp1_5[:, 0]})
    df5 = pd.DataFrame({"Kappa": exp1_6[:, 3], "QuestionCount": exp1_6[:, 0]})
    df6 = pd.DataFrame({"Kappa": exp1_7[:, 3], "QuestionCount": exp1_7[:, 0]})
    df7 = pd.DataFrame({"Kappa": exp1_8[:, 3], "QuestionCount": exp1_8[:, 0]})
    df8 = pd.DataFrame({"Kappa": exp1_9[:, 3], "QuestionCount": exp1_9[:, 0]})

    # Get the highest question count
    max1_1 = df1.QuestionCount.max()
    max1_2 = df2.QuestionCount.max()
    max1_3 = df3.QuestionCount.max()
    max1_4 = df4.QuestionCount.max()
    max1_5 = df5.QuestionCount.max()
    max1_6 = df6.QuestionCount.max()
    max1_7 = df7.QuestionCount.max()
    max1_8 = df8.QuestionCount.max()
    max1_9 = df9.QuestionCount.max()

    #print ("max2_1 for " + context + ": " + str(max2_1))
    #print ("max2_2 for " + context + ": " + str(max2_2))

    question_asked1 = []
    kappa1 = [] 
    question_asked2 = []
    kappa2 = [] 
    question_asked3 = []
    kappa3 = []
    question_asked4 = []
    kappa4 = [] 
    question_asked5 = []
    kappa5 = [] 
    question_asked6 = []
    kappa6 = []
    question_asked7 = []
    kappa7 = [] 
    question_asked8 = []
    kappa8 = [] 
    question_asked9 = []
    kappa9 = []

    kappa_sum1 = [0] * 27
    question_count1 = [0] * 27
    kappa_sum2 = [0] * 27
    question_count2 = [0] * 27
    kappa_sum3 = [0] * 27
    question_count3 = [0] * 27
    kappa_sum4 = [0] * 27
    question_count4 = [0] * 27
    kappa_sum5 = [0] * 27
    question_count5 = [0] * 27
    kappa_sum6 = [0] * 27
    question_count6 = [0] * 27
    kappa_sum7 = [0] * 27
    question_count7 = [0] * 27
    kappa_sum8 = [0] * 27
    question_count8 = [0] * 27
    kappa_sum9 = [0] * 27
    question_count9 = [0] * 27
    
    # exp1_1
    for i in range(0,len(df1.index)):
        # Reached a new trial
        if i != len(df1.index)-1:
            if df1.iloc[i+1].QuestionCount < df1.iloc[i].QuestionCount: 
                if df1.iloc[i].QuestionCount == max1_1:
                    question_asked1.append(df1.iloc[i].QuestionCount)
                    kappa1.append(df1.iloc[i].Kappa)
                    continue
                else:
                    for j in range(0, int(1+max1_1-df1.iloc[i].QuestionCount)):
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
        elif i == len(df1.index)-1 and df1.iloc[i].QuestionCount != max1_1:
            for j in range(0, int(1+max1_1-df1.iloc[i].QuestionCount)):
                question_asked1.append(df1.iloc[i].QuestionCount+j)
                kappa1.append(df1.iloc[i].Kappa)

    # exp2_2
    for i in range(0,len(df2.index)):
        if i != len(df2.index)-1:
            if df2.iloc[i+1].QuestionCount < df2.iloc[i].QuestionCount: 
                if df2.iloc[i].QuestionCount == max1_2:
                    question_asked2.append(df2.iloc[i].QuestionCount)
                    kappa2.append(df2.iloc[i].Kappa)
                    continue
                else:
                    for j in range(0, int(1+max1_2-df2.iloc[i].QuestionCount)):
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
        elif i == len(df2.index)-1 and df2.iloc[i].QuestionCount != max1_2:
            for j in range(0, int(1+max1_2-df2.iloc[i].QuestionCount)):
                question_asked2.append(df2.iloc[i].QuestionCount+j)
                kappa2.append(df2.iloc[i].Kappa)

    # For exp 2_1
    for i in range(0,len(question_asked1)):
        # Get all question counts from the first column 
        num1 = question_asked1[i]
        index1 = int(num1 % 27)           # all 27's go in [0]th index and nothing goes in [1]st index
        question_count1[index1] = question_count1[index1] + 1 
        kappa1[index1] = kappa1[index1]+kappa1[i]    # Sum the no. of kappa

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
        kappa_std_dev1[index1] = kappa_std_dev1[index1] + ((kappa_objs1[i] - kappa_mean1[index1]) ** 2)

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

        # Write out the question count, kappa's mean, kappa's variance and their standard deviation to a .csv filesums2 = [0] * 27
        with open(rootdir1 + context + attribute_list[0] + '/ContextAttrQC&Kappas.csv', 'wb') as csvfile:
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
        # Write out the question count, kappa's mean, kappa's variance and their standard deviation to a .csv filesums2 = [0] * 27 for LOOK_COLOR
        with open(rootdir1 + context + '/' + attribute_list[0] + '/ContextAttrQC&Kappas.csv', 'wb') as csvfile:
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
        kappa_sum2[index2] = kappa_sum2[index2]+kappa_objs2[i]    # Sum the no. of kappa

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

        # Write out the question count, kappa's mean, kappa's variance and their standard deviation to a .csv filesums2 = [0] * 27
        extra = [0] * 4 
        with open(rootdir2 + context + '/' + attribute_list[1] + '/ContextAttrQC&Kappas.csv', 'wb') as csvfile:
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
        # Write out the question count, kappa's mean, kappa's variance and their standard deviation to a .csv filesums2 = [0] * 27 for LOOK_COLOR
        extra = [0] * 8
        with open(rootdir2 + context + '/' + attribute_list[1] + '/ContextAttrQC&Kappas.csv', 'wb') as csvfile:
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
                        writer.writerow([i, kappa_mean2[i], kappa_variance2[i], kappakappa_std_dev2[i]]) 
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
        title= 'Questions asked VS Kappa co-efficients for ' + context,
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
