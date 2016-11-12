import csv
import os
import pandas as pd
import numpy as np
import plotly.tools as tls
tls.set_credentials_file(username='pkhante', api_key='l002tdvw1k')
import plotly.plotly as py
import plotly.graph_objs as go

#py.sign_in('pkhante', 'l002tdvw1k')

rootdir1 = '/home/priyanka/Documents/grounded_language_learning/SinglyAnnotatedObjectTrials/SinglyAnnotatedResults/'
rootdir2 = '/home/priyanka/Documents/grounded_language_learning/AutomatedExpNewAlgo/results_exp2/'
rootdir3 = '/home/priyanka/Documents/grounded_language_learning/AutomatedExpNewAlgo/results_extraQC/'

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
    print context + ": \n"
    filepath_exp1 = rootdir1 + context + '/PointsToPlot.csv'
    exp1 = np.asarray(np.genfromtxt(filepath_exp1, delimiter=','))
    exp1.astype(float)

    filepath_exp2 = rootdir2 + context + '/PointsToPlot.csv'
    exp2 = np.asarray(np.genfromtxt(filepath_exp2, delimiter=','))
    exp2.astype(float)

    filepath_exp3 = rootdir3 + context + '/PointsToPlot.csv'
    exp3 = np.asarray(np.genfromtxt(filepath_exp3, delimiter=','))
    exp3.astype(float)

    # Take out all data points that have a Kappa below 0 -> as it means the classifier and the ground truth are in total disagreement
    exp1_0 = exp1[exp1[:, 2] > 0]
    exp2_0 = exp2[exp2[:, 3] > 0]
    exp3_0 = exp3[exp3[:, 3] > 0]
    
    df1 = pd.DataFrame({"Kappa": exp1_0[:, 2], "QuestionCount": exp1_0[:, 0]})
    df2 = pd.DataFrame({"Kappa": exp2_0[:, 3], "QuestionCount": exp2_0[:, 0]})
    df3 = pd.DataFrame({"Kappa": exp3_0[:, 3], "QuestionCount": exp3_0[:, 0]})

    df1.sort_values(by="Kappa", axis=0, ascending=True, inplace=True, kind='quicksort', na_position='last')
    df2.sort_values(by="Kappa", axis=0, ascending=True, inplace=True, kind='quicksort', na_position='last')
    df3.sort_values(by="Kappa", axis=0, ascending=True, inplace=True, kind='quicksort', na_position='last')

    # Deduce the number of points that should go in each bin
    exp1_num_bins = df1['Kappa'].size/9
    exp2_num_bins = df2['Kappa'].size/9
    exp3_num_bins = df3['Kappa'].size/9

    count1 = 0
    bins1 = 0
    new_kappa1 = []
    new_qs1 = []
    check1 = False
    mean_kappa1 = []
    mean_qs1 = []

    count2 = 0
    bins2 = 0
    new_kappa2 = []
    new_qs2 = []
    check2 = False
    mean_kappa2 = []
    mean_qs2 = []

    count3 = 0
    bins3 = 0
    new_kappa3 = []
    new_qs3 = []
    check3 = False
    mean_kappa3 = []
    mean_qs3 = []

    for index, row in df1.iterrows():
        count1 = count1+1
        if check1 == True:
            if(new_kappa1[-1] != row['Kappa']):
                bins1 = bins1 + 1
                # Calculate the mean of kappa and question count
                mean_kappa1.append(np.mean(new_kappa1))
                mean_qs1.append(np.mean(new_qs1))
                del new_kappa1[:]
                del new_qs1[:]
                check1 = False

        new_kappa1.append(row['Kappa'])
        new_qs1.append(row['QuestionCount'])
        
        if count1 % exp1_num_bins == 0 and bins1!=8:
            check1 = True

    for index, row in df2.iterrows():
        count2 = count2+1
        if check2 == True:
            if(new_kappa2[-1] != row['Kappa']):
                bins2 = bins2 + 1
                mean_kappa2.append(np.mean(new_kappa2))
                mean_qs2.append(np.mean(new_qs2))
                del new_kappa2[:]
                del new_qs2[:]
                check2 = False

        new_kappa2.append(row['Kappa'])
        new_qs2.append(row['QuestionCount'])
        
        if count2 % exp2_num_bins == 0 and bins2!=8:
            check2 = True

    for index, row in df3.iterrows():
        count3 = count3+1
        if check3 == True:
            if(new_kappa3[-1] != row['Kappa']):
                bins3 = bins3 + 1
                mean_kappa3.append(np.mean(new_kappa3))
                mean_qs3.append(np.mean(new_qs3))
                del new_kappa3[:]
                del new_qs3[:]
                check3 = False

        new_kappa3.append(row['Kappa'])
        new_qs3.append(row['QuestionCount'])
        
        if count3 % exp3_num_bins == 0 and bins3!=8:
            check3 = True
 
    l = []
    trace1 = go.Scatter(x = mean_kappa1, y = mean_qs1, mode = 'lines+markers', name = 'Experiment 1')
    l.append(trace1)

    trace2 = go.Scatter(x = mean_kappa2, y = mean_qs2, mode = 'lines+markers', name = 'Experiment 2')
    l.append(trace2)

    trace3 = go.Scatter(x = mean_kappa3, y = mean_qs3, mode = 'lines+markers', name = 'Experiment 3')
    l.append(trace3)

    layout= go.Layout(
        title= 'Kappa Co-efficient VS Questions Answered for ' + context + ' (Attribute learnt: ' + attr + ')',
        hovermode= 'closest',
        width = 800,
        xaxis= dict(
            title= 'Kappa Co-efficient',
            ticklen= 5,
            zeroline= True,
            gridwidth= 2,
        ),
        yaxis=dict(
            title= 'Questions answered',
            ticklen= 5,
            zeroline= True,
            gridwidth= 2,
        ),
        showlegend= True
    )

    fig= go.Figure(data=l, layout=layout)
    #py.plot(fig)
    py.image.save_as(fig, filename = context +'.png')


    

