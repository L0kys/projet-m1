import os
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.neighbors import KNeighborsClassifier
from sklearn.metrics import accuracy_score
from sklearn.preprocessing import StandardScaler
import tensorflow as tf

# Chemin d'accès à notre dataset et liste des sous-répertoires
dataset_path = r"Projet_IA\DATASET\Dataset_start"
subdirectories = ["BENCH", "DEADLIFT", "SQUAT"]

# Variables pour stocker les données et les étiquettes
data = []
labels = []

# Parcours des sous-répertoires
for subdir in subdirectories:
    subdir_path = os.path.join(dataset_path, subdir) # Chemin complet des sous-répertoires
    
    # Parcours des fichiers dans les sous-répertoires
    for filename in os.listdir(subdir_path):
        file_path = os.path.join(subdir_path, filename) # Chemin complet du fichier
        
        # Charger les données à partir du fichier
        # Ici, nous supposons que nos données sont stockées dans un format numpy (.npy)
        # Si nos données sont stockées dans un autre format, nous devrons les charger en conséquence
        loaded_data = np.load(file_path)
        
        # Ajouter les données et les étiquettes correspondantes
        data.append(loaded_data)
        labels.append(subdir) # On utilise le nom du sous-répertoire comme étiquette de classe

# Convertir les listes en tableaux numpy
data = np.array(data)
labels = np.array(labels)

# Diviser les données en ensembles d'entraînement et de test
X_train, X_test, y_train, y_test = train_test_split(data, labels, test_size=0.2, random_state=42)

# Prétraitement des données : normalisation
scaler = StandardScaler()
X_train = scaler.fit_transform(X_train)
X_test = scaler.transform(X_test)


# Création et entraînement du classifieur KNN
knn = KNeighborsClassifier(n_neighbors=3)
knn.fit(X_train, y_train)

# Faire des prédictions sur les données de test
y_pred = knn.predict(X_test)

# Calcul de la précision du modèle
accuracy = accuracy_score(y_test, y_pred)
print("Precision du modele : {:.2f}%".format(accuracy * 100))

# Convertir le modèle KNN en un modèle TFLite
converter = tf.lite.TFLiteConverter.from_sklearn(model=knn)
tflite_model = converter.convert()

# Sauvegarder le modèle TFLite dans un fichier
with open("model.tflite", "wb") as f:
  f.write(tflite_model)
