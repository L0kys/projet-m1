import os
from sklearn.model_selection import train_test_split
import numpy as np
import tensorflow as tf
from sklearn.metrics import confusion_matrix
from tensorflow import keras
import matplotlib.pyplot as plt
import seaborn as sns


# FUNCTIONS DATASET_V2


def list_files(dossier):
    """ Returns a list of names of files in the folder

    Args:
        dossier (String): path to the folder

    Returns:
        array: list of names of files in the folder
    """
    
    fichiers_txt = []
    for nom_fichier in os.listdir(dossier):
        fichiers_txt.append(nom_fichier)
    return fichiers_txt

#indice des coordonnées Z à supprimer
index = [2, 5, 8, 11, 14, 17, 20, 23, 26, 29, 32, 35]

def read_data(nom_fichier):
    """ Reads the data from file .npy and returns a numpy array of twelve 2D points in 1 vector of 24 floats

    Args:
        nom_fichier (String): path to the file

    Returns:
        numpy array: numpy array of twelve 3D points
    """    
    floats = np.load(nom_fichier)
    #suppression des coordonnées Z
    floats = np.delete(floats, index)
    return floats


# DATASET

y = []
X = []
path_to_bench = "DATASET_V2/BENCH"
path_to_deadlift = "DATASET_V2/DEADLIFT"
path_to_squat = "DATASET_V2/SQUAT"
path_to_undefined = "DATASET_V2/UNDEFINED"

deadlift_files = list_files(path_to_deadlift)
squat_files = list_files(path_to_squat)
bench_files = list_files(path_to_bench)
undefined = list_files(path_to_undefined)


#---------------------------data-------------------------------------
for nom_fichier in deadlift_files:
    points = read_data(path_to_deadlift + '/' + nom_fichier)
    y.append(0)
    X.append(points)


for nom_fichier in squat_files:
    points = read_data(path_to_squat + '/' + nom_fichier)    
    y.append(1)
    X.append(points)


for nom_fichier in bench_files:
    points = read_data(path_to_bench + '/' + nom_fichier)
    y.append(2)
    X.append(points)

for nom_fichier in undefined[:600]:
    points = read_data(path_to_undefined + '/' + nom_fichier)
    y.append(3)
    X.append(points)


X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.3, random_state=42)
#-------------------------------------------------------------------

########################################
X_train = np.array(X_train)
y_train = np.array(y_train)
X_test = np.array(X_test)
y_test = np.array(y_test)
y_train = y_train.reshape(-1, 1)
y_test = y_test.reshape(-1, 1)
########################################

#---------------------------model-----------------------------------
model = keras.Sequential([
    keras.layers.Dense(64, activation='relu', input_shape=(24,)),
    keras.layers.Dense(128, activation='relu'),
    keras.layers.Dense(64, activation='relu'),
    keras.layers.Dense(4, activation='softmax')
])

model.compile(optimizer='adam',
              loss='sparse_categorical_crossentropy',
              metrics=['accuracy'])
#-------------------------------------------------------------------


#------------------Training------------------
train_loss = []
train_accuracy = []
EPOCHS = 200

for epoch in range(EPOCHS):
    history = model.fit(X_train, y_train, epochs=1, batch_size=8, verbose=1)
    train_loss.append(history.history['loss'][0])
    train_accuracy.append(history.history['accuracy'][0])
    
#Test
_, accuracy = model.evaluate(X_test, y_test)
print("Accuracy : {:.2f}%".format(accuracy * 100))


#------------------Plot_train------------------
epochs = range(1, EPOCHS + 1)

plt.figure(figsize=(12, 4))

plt.subplot(1, 2, 1)
plt.plot(epochs, train_loss, 'b', label='Training loss')
plt.title('Training Loss')
plt.xlabel('Epochs')
plt.ylabel('Loss')
plt.legend()

plt.subplot(1, 2, 2)
plt.plot(epochs, train_accuracy, 'b', label='Training accuracy')
plt.title('Training Accuracy')
plt.xlabel('Epochs')
plt.ylabel('Accuracy')
plt.legend()

plt.show()
#----------------------------------------------------

#------------------Confusion matrix------------------
y_pred_prob = model.predict(X_test)
y_pred = np.argmax(y_pred_prob, axis=1)
y_test_int = np.array(y_test, dtype=int)

labels = ['Deadlift', 'Squat', 'Bench', 'Undefined']
cm = confusion_matrix(y_test_int, y_pred)

plt.figure(figsize=(8, 6))
sns.heatmap(cm, annot=True, fmt='d', cmap='Blues', xticklabels=labels, yticklabels=labels)
plt.title('Confusion Matrix')
plt.xlabel('Predicted')
plt.ylabel('True')

plt.show()
#----------------------------------------------------

#-----------------TFLITE CONVERSION------------------
converter = tf.lite.TFLiteConverter.from_keras_model(model)

tflite_model = converter.convert()

with open('model.tflite', 'wb') as f:
    f.write(tflite_model)
#----------------------------------------------------