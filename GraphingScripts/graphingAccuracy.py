import csv
import os
import pandas as pd
import numpy as np
import plotly.tools as tls
tls.set_credentials_file(username='pkhante', api_key='l002tdvw1k')
import plotly.plotly as py
import plotly.graph_objs as go

py.sign_in('pkhante', 'l002tdvw1k')

rootdir1 = '/home/priyanka/Documents/grounded_language_learning/SinglyAnnotatedObjectTrials/SinglyAnnotatedResults/'
rootdir2 = '/home/priyanka/Documents/grounded_language_learning/AutomatedExpNewAlgo/results_exp2/'

contexts_list = ['drop_audio', 'grasp_size', 'hold_haptics', 'lift_haptics', 'look_color', 'look_shape', 'press_haptics', 'push_audio', 'revolve_audio', 'shake_audio', 'squeeze_haptics']

for context in contexts_list:
    # Read csv file and put the values in an array
    filepath_exp1 = rootdir1 + context + '/PointsToPlot.csv'
    exp1 = np.genfromtxt(filepath_exp1, delimiter=',')

    filepath_exp2 = rootdir2 + context + '/PointsToPlot.csv'
    exp2 = np.genfromtxt(filepath_exp2, delimiter=',')
            
    df1 = pd.DataFrame({"Accuracy": exp1[:, 1], "QuestionCount": exp1[:, 0]})
    df2 = pd.DataFrame({"Accuracy": exp2[:, 1], "QuestionCount": exp2[:, 0]})

    # Get the mean of QuestionCount, binned by the values in Kappa
    acc1 = []
    question_count1 = []
    acc2 = []
    question_count2 = []
    l = []

    for i in range(0,len(df1.index)):
        question_count1.append(df1.iloc[i].QuestionCount)
        acc1.append(df1.iloc[i].Accuracy)

    for i in range(0,len(df2.index)):
        question_count2.append(df2.iloc[i].QuestionCount)
        acc2.append(df2.iloc[i].Accuracy)
   

    trace1 = go.Scatter(x = question_count1, y = acc1, mode = 'markers', name = 'Experiment 1')
    l.append(trace1)

    trace2 = go.Scatter(x = question_count2, y = acc2, mode = 'markers', name = 'Experiment 2')
    l.append(trace2)

    layout= go.Layout(
        title= 'Questions asked VS Accuracy',
        hovermode= 'closest',
        xaxis= dict(
            title= 'Questions asked',
            ticklen= 5,
            zeroline= False,
            gridwidth= 2,
        ),
        yaxis=dict(
            title= 'Accuracy',
            ticklen= 5,
            zeroline= False,
            gridwidth= 2,
        ),
        showlegend= True
    )

    fig= go.Figure(data=l, layout=layout)
    #py.plot(fig)
    py.image.save_as(fig, filename = context +'.png')

