import cv2
import mediapipe as mp
import numpy as np
import operator
import os

# Fonction pour effectuer la détection de poses à l'aide de MediaPipe
def mediapipe_detection(image, model):
    image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB) # Conversion de couleur BGR à RGB
    image.flags.writeable = False                  # L’image est inaccessible en écriture
    results = model.process(image)                 # Detection de la pose à partir de l'image
    image.flags.writeable = True                   # L’image est accessible en écriture
    image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR) # Conversion de couleur RGB à BGR
    return image, results

# Fonction pour extraire les keypoints des résultats de détection
def extract_keypoints(results):
    pose = np.array([[res.x, res.y, res.z] for res in operator.itemgetter(11,12,13,14,15,16,23,24,25,26,27,28)(results.pose_landmarks.landmark)]).flatten() if results.pose_landmarks else np.zeros(36)
    return pose

# Chemin d'accès du répertoire contenant les images
path = # Chemin du dossier d'images

# Initialisation du modèle MediaPipe Holistic
mp_holistic = mp.solutions.holistic

cpt = 1

# Création d'une instance du modèle MediaPipe Holistic
with mp_holistic.Holistic(static_image_mode=True,
                          model_complexity=2,
                          enable_segmentation=True,
                          refine_face_landmarks=True) as holistic:
    
    # Parcours des fichiers dans le répertoire "IMAGES"
    for file in os.listdir(path + 'IMAGES'):
        
        image = cv2.imread(path + 'IMAGES/' + file) # Lecture de l'image
        
        image, results = mediapipe_detection(image, holistic) # Détection des poses sur l'image

        points = extract_keypoints(results) # Extraction des points

        im = np.array(points) # Conversion des points en un tableau NumPy
        
        name = 'S_' + str(cpt)
         
        np.save(path + 'TENSORS/' + name + '.npy', im) # Sauvegarde des points dans un fichier Numpy
        
        cpt += 1
