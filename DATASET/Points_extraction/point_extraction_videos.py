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

# Chemin d'accès du répertoire contenant les vidéos
path = # Chemin du dossier de vidéos

mp_pose = mp.solutions.pose # Initialisation du modèle MediaPipe Pose

cpt_video = 1

# Création d'une instance du modèle MediaPipe Pose
with mp_pose.Pose(min_detection_confidence=0.5, min_tracking_confidence=0.5) as pose:
    
    # Parcours des fichiers dans le répertoire "IMAGES"
    for file in os.listdir(path + 'VIDEOS'):
        cap = cv2.VideoCapture(path + 'VIDEOS/' + file) # Ouvrir la vidéos avec l'outil vidéos de OpenCV
        
        video = np.array([])
        cpt = 1
        
        # Parcourir chaque image de la vidéo
        while cap.isOpened():
            success, image = cap.read() # Lire une frame de la vidéo

            if success == True :

                image, results = mediapipe_detection(image, pose) # Détection des poses sur l'image
                
                points = extract_keypoints(results) # Extraction des points
                
                if cpt==1:
                    
                    video = np.array([points]) # Conversion des points en un tableau NumPy
                    
                else :
                    
                    video = np.concatenate((video, np.array([points]))) # Ajout des points au nouveau tableau Numpy

            else :
                break
                
            cpt +=1
         
        np.save(path + 'TENSORS/' + 'S_' + str(cpt_video) + '.npy', video) # Sauvegarde des points dans un fichier Numpy
        
        cpt_video += 1  
    
    # Libérer les ressources
    cap.release()
    cv2.destroyAllWindows()