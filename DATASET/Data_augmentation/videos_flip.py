import cv2
import os

path = # Chemin du dossier de vidéos

cpt = 1

# Parcourir chaques vidéos dans le dossier
for video in os.listdir(path):

    cap = cv2.VideoCapture(path + '/' + video) # Ouvrir la vidéos avec l'outil vidéos de OpenCV

    fps = cap.get(cv2.CAP_PROP_FPS)  # Nombre d'images par seconde de la vidéo d'origine
    frame_width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))  # Largeur des images de la vidéo
    frame_height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))  # Hauteur des images de la vidéo
    
    output_path = path + '/S_' + str(cpt) + '.mp4' # Chemin vers la nouvelle vidéo
    output_video = cv2.VideoWriter(output_path, cv2.VideoWriter_fourcc(*'mp4v'), fps, (frame_width, frame_height)) # Création de la nouvelle vidéo
    
    # Parcourir chaque image de la vidéo
    while cap.isOpened():
        
        success, image = cap.read() 
        
        if success == True :
            image = cv2.flip(image, 1) # retourne l'image sur l'axe vertical
            
            output_video.write(image) # Ajoute l'image retournée à la vidéo de sortie
        else :
            break
    cpt+=1
    
    # Libérer les ressources        
    cap.release()
    output_video.release()
    cv2.destroyAllWindows()
