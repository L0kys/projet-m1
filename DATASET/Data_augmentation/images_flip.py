from PIL import Image, ImageOps
import os

path = # Chemin du dossier d'images

cpt = 1

# Parcourir chaques images dans le dossier
for image in os.listdir(path):

    im = Image.open(path + str(image)) # Ouvrir l'image
    
    im_flip = ImageOps.mirror(im) # Retourner l'image sur l'axe vertical
    
    im_flip.save(path + 'D_' + str(cpt) + '.png') # Enregistrer l'image
    
    cpt += 1