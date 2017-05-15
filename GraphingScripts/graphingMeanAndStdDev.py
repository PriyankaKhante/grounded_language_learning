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

contexts_list = ['drop_audio', 'grasp_size', 'hold_haptics', 'lift_haptics', 'look_color', 'look_shape', 'press_haptics', 'push_audio', 'revolve_audio', 'shake_audio', 'squeeze_haptics']

for context in contexts_list:
    # Read csv file and put the values in an array
    filepath_exp1 = rootdir1 + context + '/PointsToPlot.csv'
    exp1 = np.asarray(np.genfromtxt(filepath_exp1, delimiter=','))

    filepath_exp2 = rootdir2+ context + '/PointsToPlot.csv'
    exp2 = np.asarray(np.genfromtxt(filepath_exp2, delimiter=','))
    
    sums1 = [0] * 27
    sums2 = [0] * 27
    quesCount2 = [0] * 27

    for i in range(0,len(exp1[:,0])):
        # Get all question counts from the first column 
        num1 = exp1[i,0]
        index1 = int(num1 % 27)              # all 27's go in [0]th index and nothing goes in [1]st index
        sums1[index1] = sums1[index1]+exp1[i,2]

    for i in range(0,len(exp2[:,0])):
        # Get all question counts from the first column 
        num2 = exp2[i,0]
        index2 = int(num2 % 27)              # all 27's go in [0]th index and nothing goes in [1]st index
        quesCount2[index2] = quesCount2[index2] + 1
        sums2[index2] = sums2[index2]+exp2[i,2]

    # Calculate the mean for each question count
    mean1 = [0] * 27                # The mean of 27 goes into [0]th index and nothing goes into [1]st
    variance1 = [0] * 27
    std_dev1 = [0] * 27

    mean2 = [0] * 27                # The mean of 27 goes into [0]th index and nothing goes into [1]st 
    variance2 = [0] * 27
    std_dev2 = [0] * 27

    for i in range(0, len(sums1)):
        mean1[i] = sums1[i]/100
        if quesCount2[i] != 0:
            mean2[i] = sums2[i]/quesCount2[i]

    print ("Question counts: ", quesCount2)

    print ("Mean1: ", mean1)
    print ("Mean2: ", mean2)

    for i in range(0, len(exp1[:,2])):
        num1 = exp1[i,0]
        index1 = int(num1 % 27)
        std_dev1[index1] = std_dev1[index1] + ((exp1[i,2] - mean1[index1]) ** 2)

    for i in range(0, len(exp2[:,2])):
        num2 = exp2[i,0]
        index2 = int(num2 % 27)
        std_dev2[index2] = std_dev2[index2] + ((exp2[i,2] - mean2[index2]) ** 2)

    for i in range(0, len(std_dev1)):
        variance1[i] = (std_dev1[i]/100)
        std_dev1[i] = (std_dev1[i]/100) ** 0.5

        if quesCount2[i] != 0:
            variance2[i] = (std_dev2[i]/quesCount2[i])
            std_dev2[i] = (std_dev2[i]/quesCount2[i]) ** 0.5

    print ("Variance1: ", variance1)
    print ("Variance2: ", variance2)

    print ("Std_dev1: ", std_dev1)
    print ("Std_dev2: ", std_dev2)

    # Some adjustments to all the arrays
    mean1[1] = mean1[0]
    std_dev1[1] = std_dev1[0]
    variance1[1] = variance1[0]

    mean2[1] = mean2[0]
    std_dev2[1] = std_dev2[0]
    variance2[1] = variance2[0]

    # Write out the question count, mean, variance and standard deviation to a .csv file
    with open(rootdir1 + context + '/Mean&StdDev.csv', 'wb') as csvfile:
        writer = csv.writer(csvfile, delimiter=',')
        for i in range(1, len(mean1)):
            writer.writerow([exp1[i+24,0], mean1[i], variance1[i], std_dev1[i]]) 
    
    with open(rootdir2 + context + '/Mean&StdDev.csv', 'wb') as csvfile:
        writer = csv.writer(csvfile, delimiter=',')
        for i in range(1, len(mean2)):
            if mean2[i] != 0:
                if i == 1:
                    writer.writerow(['27', mean2[i], variance2[i], std_dev2[i]]) 
                else:
                    writer.writerow([i, mean2[i], variance2[i], std_dev2[i]]) 

    # Reload the files and draw a plot
    exp1_1 = np.genfromtxt(rootdir1 + context + '/Mean&StdDev.csv', delimiter=',')
    exp2_1 = np.genfromtxt(rootdir2 + context + '/Mean&StdDev.csv', delimiter=',')

    trace1 = go.Bar(x = exp1_1[:, 0], y = exp1_1[:, 1], name = 'Experiment 1', error_y = dict(type ='data', array = exp1_1[:,3], visible = True))

    trace2 = go.Bar(x = exp2_1[:, 0], y = exp2_1[:, 1], name = 'Experiment 2', error_y = dict(type ='data', array = exp2_1[:,3], visible = True))

    data = [trace1, trace2]

    layout = go.Layout(barmode='group', title= 'Questions asked VS Kappa co-efficient', xaxis= dict(title= 'Questions answered',),yaxis=dict(title= 'Kappa co-efficient',))

    fig= go.Figure(data=data, layout=layout)
    #py.plot(fig)
    py.image.save_as(fig, filename = context +'.png')
