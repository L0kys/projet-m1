import numpy as np
import os

# Fonction permettant la rotation sur l'axe Y des points d'un tableau array en fonction du degrès choisi
def rotation(degree, array) : 
    
    theta = np.radians(degree)
    
    # Définir la matrice de rotation autour de l'axe y
    rotation_matrix = np.array([[np.cos(theta), 0, np.sin(theta)],
                                [0, 1, 0],
                                [-np.sin(theta), 0, np.cos(theta)]])
    
    return np.dot(array, rotation_matrix)

# Chemin d'accès du répertoire contenant les images
#path = # Chemin du dossier d'images

cpt = 1

# Parcourir les fichiers dans le dossier "TENSORS"
for file in os.listdir(path + 'TENSORS/'):
    
    tensor = np.load(path + 'TENSORS/' + str(file)) # Charger le tenseur à partir du fichier
    
    l = len(tensor) # Nombre de tenseurs
    
    degree = [-14,-9,-4,5,10,15] # Liste des degrés de rotation
    
    # Parcourir chaque degré de rotation
    for d in degree :
        
        # Parcourir chaque tenseur
        for i in range(l):

            # Parcourir chaque point (3 coordonnées) dans le tenseur
            for j in range(36):
                
                # Vérifier si l'indice est un multiple de 3 (coordonnée y)
                if j % 3 == 0 :

                    t = np.array([tensor[i][j],tensor[i][j+1],tensor[i][j+2]]) # Coordonnées du point
                    t1 = rotation(d, t) # Effectuer la rotation sur les coordonnées

                    if j == 0 :

                        sub_tensor = np.array([t1]) # Premier point de chaque sous-tenseur

                    else :

                        sub_tensor = np.concatenate((sub_tensor, np.array([t1]))) # Ajouter le point au sous-tenseur

            if i == 0 : 
                new_tensor = np.array([sub_tensor.flatten()]) # Premier sous-tenseur du nouveau tenseur

            else : 
                new_tensor = np.concatenate((new_tensor, np.array([sub_tensor.flatten()]))) # Ajouter le sous-tenseur au nouveau tenseur


        np.save(path + 'NEW_TENSORS/' + 'D_' + str(cpt) + '.npy', new_tensor) # Sauvegarder le nouveau tenseur dans un fichier

        cpt +=1