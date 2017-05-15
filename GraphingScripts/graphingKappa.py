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

contexts_list = ["drop_audio"]
#['drop_audio', 'grasp_size', 'hold_haptics', 'lift_haptics', 'look_color', 'look_shape', 'press_haptics', 'push_audio', 'revolve_audio', 'shake_audio', 'squeeze_haptics']

for context in contexts_list:
    # Read csv file and put the values in an array
    print context + ": \n"
    filepath_exp1 = rootdir1 + context + '/PointsToPlot.csv'
    exp1 = np.asarray(np.genfromtxt(filepath_exp1, delimiter=','))
    exp1.astype(float)

    filepath_exp2 = rootdir2 + context + '/PointsToPlot.csv'
    exp2 = np.asarray(np.genfromtxt(filepath_exp2, delimiter=','))
    exp2.astype(float)

    # Take out all data points that have a Kappa below 0 -> as it means the classifier and the ground truth are in total disagreement
    exp1_0 = exp1[exp1[:, 3] > 0]
    exp2_0 = exp2[exp2[:, 3] > 0]
    
    df1 = pd.DataFrame({"Kappa": exp1_0[:, 3], "QuestionCount": exp1_0[:, 0]})
    df2 = pd.DataFrame({"Kappa": exp2_0[:, 3], "QuestionCount": exp2_0[:, 0]})
    
    # Bin the data frame by "Kappa" with 4 bins...
    bins1 = np.linspace(df1.Kappa.min(), df1.Kappa.max(), 5)
    groups1 = df1.groupby(pd.cut(df1.Kappa, bins1))
    bins2 = np.linspace(df2.Kappa.min(), df2.Kappa.max(), 5)
    groups2 = df2.groupby(pd.cut(df2.Kappa, bins2))

    print groups1['Kappa'].size()
    
   
    # Get the mean of QuestionCount, binned by the values in Kappa
    kappa1 = []
    question_count1 = []
    kappa2 = []
    question_count2 = []
    l = []

    for i in range(0,len(groups1.mean().index)):
        if (~np.isnan(groups1.mean().iloc[i].QuestionCount)):
            question_count1.append(groups1.mean().iloc[i].QuestionCount)
        if (~np.isnan(groups1.mean().iloc[i].Kappa)):
            kappa1.append(groups1.mean().iloc[i].Kappa)

    for i in range(0,len(groups2.mean().index)):
        if (~np.isnan(groups2.mean().iloc[i].QuestionCount)):
            question_count2.append(groups2.mean().iloc[i].QuestionCount)
        if (~np.isnan(groups2.mean().iloc[i].Kappa)):
            kappa2.append(groups2.mean().iloc[i].Kappa)
    """
    for i in range(0, len(kappa1)):
        print (str(kappa1[i]))

    print ("_________________________________")

    for i in range(0, len(kappa1)):
        print (str(question_count1[i]))

    print ("_________________________________")

    for i in range(0, len(kappa1)):
        print (str(kappa2[i]))

    print ("_________________________________")

    for i in range(0, len(kappa1)):
        print (str(question_count2[i]))
    """
    trace1 = go.Bar(x = kappa1, y = question_count1, name = 'Experiment 1')
    l.append(trace1)

    trace2 = go.Bar(x = kappa2, y = question_count2, name = 'Experiment 2')
    l.append(trace2)

    layout= go.Layout(
        title= 'Kappa Co-efficient VS Questions answered',
        hovermode= 'closest',
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
        showlegend= True,
        barmode='group'
    )

    fig= go.Figure(data=l, layout=layout)
    py.plot(fig)
    #py.image.save_as(fig, filename = context +'.png')
  
