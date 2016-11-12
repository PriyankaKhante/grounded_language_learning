import csv
import os
import pandas as pd
import numpy as np
import plotly.tools as tls
tls.set_credentials_file(username='pkhante', api_key='l002tdvw1k')
import plotly.plotly as py
import plotly.graph_objs as go

py.sign_in('pkhante', 'l002tdvw1k')

rootdir = '/home/priyanka/Documents/grounded_language_learning/AutomatedExpNewAlgo/ContextAttrMappingResults/'

contexts_list = ["drop_audio", "revolve_audio","push_audio", "hold_haptics", "lift_haptics", "press_haptics","squeeze_haptics","grasp_size", "shake_audio", "look_color","look_shape", "grasp_audio", "hold_audio", "lift_audio", "poke_audio", "press_audio", "squeeze_audio", "drop_haptics", "poke_haptics", "revolve_haptics", "push_haptics", "shake_haptics", "grasp_haptics"]

attributes_list = ['color', 'deformable', 'deformable_from_top', 'has_contents', 'height', 'material', 'shape', 'size', 'weight']

for context in contexts_list:
    for attr in attributes_list:
        # Read csv file and put the values in an array
        filepath_exp = rootdir + context + '/' + attr + '/PointsToPlot.csv'
        exp = np.genfromtxt(filepath_exp, delimiter=',')

        df = pd.DataFrame({"Kappa": exp[:, 4], "QuestionCount": exp[:, 1]})

        # Get the highest question count
        max_count = df.QuestionCount.max()

        question_asked = []
        kappa = [] 

        for i in range(0,len(df.index)):
            # Reached a new trial
            if i != len(df.index)-1:
                if df.iloc[i+1].QuestionCount < df.iloc[i].QuestionCount: 
                    if df.iloc[i].QuestionCount == max_count:
                        question_asked.append(df.iloc[i].QuestionCount)
                        kappa.append(df.iloc[i].Kappa)
                        continue
                    else:
                        for j in range(0, int(1+max_count-df.iloc[i].QuestionCount)):
                            question_asked.append(float(df.iloc[i].QuestionCount+j))
                            kappa.append(float(df.iloc[i].Kappa))
                else:
                    if df.iloc[i+1].QuestionCount-df.iloc[i].QuestionCount == 1:
                        question_asked.append(df.iloc[i].QuestionCount)
                        kappa.append(df.iloc[i].Kappa)
                    else:
                        for j in range(0, int(df.iloc[i+1].QuestionCount-df.iloc[i].QuestionCount)):
                            question_asked.append(float(df.iloc[i].QuestionCount+j))
                            kappa.append(df.iloc[i].Kappa)
            elif i == len(df.index)-1 and df.iloc[i].QuestionCount != max_count:
                for j in range(0, int(1+max_count-df.iloc[i].QuestionCount)):
                    question_asked.append(df.iloc[i].QuestionCount+j)
                    kappa.append(df.iloc[i].Kappa)

        multiplier = 27
        if max_count > 27:
            multiplier = max_count
        
        kappa_sum = [0] * multiplier
        question_count = [0] * multiplier

        for i in range(0,len(question_asked)):
            # Get all question counts from the first column 
            num = question_asked[i]
            index = int(num % multiplier)           # all 27's go in [0]th index and nothing goes in [1]st index
            question_count[index] = question_count[index] + 1 
            kappa_sum[index] = kappa_sum[index]+kappa[i]    # Sum the no. of kappa

        # Calculate the mean for each question count
        kappa_mean = [0] * multiplier             # The mean of 27 goes into [0]th index and nothing goes into [1]st
        kappa_variance = [0] * multiplier
        kappa_std_dev = [0] * multiplier

        for i in range(0, len(kappa_sum)):
            if question_count[i] != 0:
                kappa_mean[i] = kappa_sum[i]/question_count[i]

        for i in range(0, len(question_asked)):
            num = question_asked[i]
            index = int(num % multiplier)
            kappa_std_dev[index] = kappa_std_dev[index] + ((kappa[i] - kappa_mean [index]) ** 2)

        for i in range(0, len(kappa_std_dev)):
            if question_count[i] != 0:
                kappa_variance[i] = (kappa_std_dev[i]/question_count[i])
                kappa_std_dev[i] = (kappa_std_dev[i]/question_count[i]) ** 0.5

        #print kappa_mean
        #print kappa_std_dev

        # Some adjustments to all the arrays
        extra = [0] * 4 
        kappa_mean[1] = kappa_mean[0]
        kappa_std_dev[1] = kappa_std_dev[0]
        kappa_variance[1] = kappa_variance[0]

        # Write out the question count, kappa's mean, kappa's variance and their standard deviation to a .csv 
        with open(rootdir + context + '/' + attr + '/ContextAttrQC&Kappas.csv', 'wb') as csvfile:
            writer = csv.writer(csvfile, delimiter=',')
            for i in range(1, len(kappa_mean)):
                if kappa_mean[i] != 0:
                    if i == 1:
                        extra[0] = multiplier
                        extra[1] = kappa_mean[i]
                        extra[2] = kappa_variance[i]
                        extra[3] = kappa_std_dev[i]
                    else:
                        writer.writerow([i, kappa_mean[i], kappa_variance[i], kappa_std_dev[i]]) 
            if (extra[0] != 0):
                writer.writerow([extra[0], extra[1], extra[2], extra[3]])

    exp1_1 = np.genfromtxt(rootdir + context + '/' + attributes_list[0] + '/ContextAttrQC&Kappas.csv', delimiter=',')
    exp1_2 = np.genfromtxt(rootdir + context + '/' + attributes_list[1] + '/ContextAttrQC&Kappas.csv', delimiter=',')
    exp1_3 = np.genfromtxt(rootdir + context + '/' + attributes_list[2] + '/ContextAttrQC&Kappas.csv', delimiter=',')
    exp1_4 = np.genfromtxt(rootdir + context + '/' + attributes_list[3] + '/ContextAttrQC&Kappas.csv', delimiter=',')
    exp1_5 = np.genfromtxt(rootdir + context + '/' + attributes_list[4] + '/ContextAttrQC&Kappas.csv', delimiter=',')
    exp1_6 = np.genfromtxt(rootdir + context + '/' + attributes_list[5] + '/ContextAttrQC&Kappas.csv', delimiter=',')
    exp1_7 = np.genfromtxt(rootdir + context + '/' + attributes_list[6] + '/ContextAttrQC&Kappas.csv', delimiter=',')
    exp1_8 = np.genfromtxt(rootdir + context + '/' + attributes_list[7] + '/ContextAttrQC&Kappas.csv', delimiter=',')
    exp1_9 = np.genfromtxt(rootdir + context + '/' + attributes_list[8] + '/ContextAttrQC&Kappas.csv', delimiter=',')

    trace1 = go.Scatter(x = exp1_1[:, 0], y = exp1_1[:, 1], mode = "lines+markers", name = 'Attr: ' + attributes_list[0], #error_y = dict(type ='data', array = exp1_1[:,2], visible = True),
	marker = dict(symbol = 'diamond', size = '10'))

    trace2 = go.Scatter(x = exp1_2[:, 0], y = exp1_2[:, 1], mode = "lines+markers", name = 'Attr: ' + attributes_list[1], #error_y = dict(type ='data', array = exp1_2[:,2], visible = True), 
	marker = dict(symbol = 'triangle-up', size = '10'))

    trace3 = go.Scatter(x = exp1_3[:, 0], y = exp1_3[:, 1], mode = "lines+markers", name = 'Attr: ' + attributes_list[2], #error_y = dict(type ='data', array = exp1_3[:,2], visible = True), 
	marker = dict(symbol = 'circle', size = '10'))

    trace4 = go.Scatter(x = exp1_4[:, 0], y = exp1_4[:, 1], mode = "lines+markers", name = 'Attr: ' + attributes_list[3], #error_y = dict(type ='data', array = exp1_4[:,2], visible = True), 
	marker = dict(symbol = 'triangle-right', size = '10'))

    trace5 = go.Scatter(x = exp1_5[:, 0], y = exp1_5[:, 1], mode = "lines+markers", name = 'Attr: ' + attributes_list[4], #error_y = dict(type ='data', array = exp1_5[:,2], visible = True), 
marker = dict(symbol = 'cross', size = '10'))

    trace6 = go.Scatter(x = exp1_6[:, 0], y = exp1_6[:, 1], mode = "lines+markers", name = 'Attr: ' + attributes_list[5], #error_y = dict(type ='data', array = exp1_6[:,2], visible = True),
marker = dict(symbol = 'square', size = '10'))

    trace7 = go.Scatter(x = exp1_7[:, 0], y = exp1_7[:, 1], mode = "lines+markers", name = 'Attr: ' + attributes_list[6], #error_y = dict(type ='data', array = exp1_7[:,2], visible = True), 
marker = dict(symbol = 'star', size = '10'))

    trace8 = go.Scatter(x = exp1_8[:, 0], y = exp1_8[:, 1], mode = "lines+markers", name = 'Attr: ' + attributes_list[7], #error_y = dict(type ='data', array = exp1_8[:,2], visible = True), 
marker = dict(symbol = 'pentagon', size = '10'))

    trace9 = go.Scatter(x = exp1_9[:, 0], y = exp1_9[:, 1], mode = "lines+markers", name = 'Attr: ' + attributes_list[8], #error_y = dict(type ='data', array = exp1_9[:,2], visible = True), 
marker = dict(symbol = 'hourglass', size = '10'))
    data = [trace1, trace2, trace3, trace4, trace5, trace6, trace7, trace8, trace9]

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
