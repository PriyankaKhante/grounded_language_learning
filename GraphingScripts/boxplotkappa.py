import csv
import os
import pandas as pd
import numpy as np
import plotly.tools as tls
tls.set_credentials_file(username='pkhante', api_key='l002tdvw1k')
import plotly.plotly as py
import plotly.graph_objs as go
#from plotly.offline import download_plotlyjs, init_notebook_mode, iplot 
#from plotly.graph_objs import *
#init_notebook_mode()

#py.sign_in('pkhante', 'l002tdvw1k')

rootdir1 = '/home/priyanka/Documents/grounded_language_learning/SinglyAnnotatedObjectTrials/SinglyAnnotatedResults/'
rootdir2 = '/home/priyanka/Documents/grounded_language_learning/AutomatedExpNewAlgo/results_exp2/'

contexts_list = ['drop_audio', 'grasp_size', 'hold_haptics', 'lift_haptics', 'look_color', 'look_shape', 'press_haptics', 'push_audio', 'revolve_audio', 'shake_audio', 'squeeze_haptics']

for context in contexts_list:
    # Read csv file and put the values in an array
    filepath_exp1 = rootdir1 + context + '/PointsToPlot.csv'
    exp1 = np.genfromtxt(filepath_exp1, delimiter=',')

    filepath_exp2 = rootdir2 + context + '/PointsToPlot.csv'
    exp2 = np.genfromtxt(filepath_exp2, delimiter=',')
            
    df1 = pd.DataFrame({"Kappa": exp1[:, 2], "QuestionCount": exp1[:, 0]})
    df2 = pd.DataFrame({"Kappa": exp2[:, 2], "QuestionCount": exp2[:, 0]})

    # Bin the data frame by "Kappa" with 10 bins...
    bins1 = np.linspace(df1.Kappa.min(), df1.Kappa.max(), 6)
    groups1 = df1.groupby(pd.cut(df1.Kappa, bins1))
    bins2 = np.linspace(df2.Kappa.min(), df2.Kappa.max(), 6)
    groups2 = df2.groupby(pd.cut(df2.Kappa, bins2))

    # Get the mean of QuestionCount, binned by the values in Kappa
    kappa1 = []
    question_count1 = []
    kappa2 = []
    l = []

    # Replace all the kappa co-efficients with their respective kappa_means
    def replace(group):
        mean_kappa = group.mean()
        #print mean_kappa
        #print "Length: ", len(list(group)) 
        mask = group < 100000  # VERY VERY HACKY
        #print mask
        group[mask] = mean_kappa
        return group
    
    # HACKY ----> Write the results out to a .csv file
    groups1['Kappa'].transform(replace).to_csv(context+'_kappa_exp1.csv')
    groups2['Kappa'].transform(replace).to_csv(context+'_kappa_exp2.csv')
    
    def hack(group):
        return group

    # Write the questioncount out to another csv
    groups1['QuestionCount'].transform(hack).to_csv(context+'_QC_exp1.csv')
    groups2['QuestionCount'].transform(hack).to_csv(context+'_QC_exp2.csv')

    # Load both the files and create a boxplot and then delete both the files
    rootdir3 = '/home/priyanka/Documents/grounded_language_learning/'

    filepath_exp3 = rootdir3 + context + '_kappa_exp1.csv'
    exp1_1 = np.genfromtxt(filepath_exp3, delimiter=',')

    filepath_exp4 = rootdir3 + context + '_QC_exp1.csv'
    exp1_2 = np.genfromtxt(filepath_exp4, delimiter=',')

    filepath_exp5 = rootdir3 + context + '_kappa_exp2.csv'
    exp2_1 = np.genfromtxt(filepath_exp5, delimiter=',')

    filepath_exp6 = rootdir3 + context + '_QC_exp2.csv'
    exp2_2 = np.genfromtxt(filepath_exp6, delimiter=',')

    # Print out the groups
    """
    for key, item in trans_group1:
        print trans_group1.get_group(key), "\n\n"
    """

    # Delete the files as they use is done
    os.remove(context + '_kappa_exp1.csv')
    os.remove(context + '_QC_exp1.csv')
    os.remove(context + '_kappa_exp2.csv')
    os.remove(context + '_QC_exp2.csv')

    trace1 = go.Box(x = exp1_1[:, 1], y = exp1_2[:, 1], marker=dict(color='#3D9970'), name = 'Experiment 1')
    l.append(trace1)

    trace2 = go.Box(x = exp2_1[:, 1], y = exp2_2[:, 1], marker=dict(color='#FF851B'), name = 'Experiment 2')
    l.append(trace2)

    layout= go.Layout(
        autosize=True,
        #width=800,
        #height=400,
        title= 'Kappa Co-efficient VS Questions asked',
        hovermode= 'closest',
        xaxis= dict(
            title= 'Kappa Co-efficient',
            zeroline= False,
        ),
        yaxis=dict(
            title= 'Questions asked',
            zeroline= False,
        ),
        showlegend= True,
        boxgap=0.0,
        boxmode='group'
    )

    fig= go.Figure(data=l, layout=layout)
    #py.plot(fig)
    py.image.save_as(fig, filename = context +'.png')
