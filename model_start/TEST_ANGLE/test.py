import cv2
import mediapipe as mp
import time
import numpy as np
import operator

mp_drawing = mp.solutions.drawing_utils
mp_drawing_styles = mp.solutions.drawing_styles
mp_pose = mp.solutions.pose
mp_holistic = mp.solutions.holistic

def mediapipe_detection(image, model):
    image = cv2.cvtColor(image, cv2.COLOR_BGR2RGB) # COLOR CONVERSION BGR 2 RGB
    image.flags.writeable = False                  # Image is no longer writeable
    results = model.process(image)                 # Make prediction
    image.flags.writeable = True                   # Image is now writeable
    image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR) # COLOR COVERSION RGB 2 BGR
    return image, results

def draw_styled_landmarks(image, results):
    # Draw pose connections
    mp_drawing.draw_landmarks(image, results.pose_landmarks, mp_holistic.POSE_CONNECTIONS,
                             landmark_drawing_spec=mp_drawing_styles.get_default_pose_landmarks_style()
                             )

def extract_keypoints(results):
    pose = np.array([[res.x, res.y, res.z] for res in operator.itemgetter(11,12,13,14,15,16,23,24,25,26,27,28)(results.pose_landmarks.landmark)]).flatten() if results.pose_landmarks else np.zeros(33*4)
    return pose

cap = cv2.VideoCapture('BENCH/genoux3-4_3.mp4')
with mp_pose.Pose(min_detection_confidence=0.5,
									min_tracking_confidence=0.5) as pose:
    while cap.isOpened():
        success, image = cap.read()
        if not success:
            print("Ignoring empty camera frame.")
            #If loading a video, use 'break' instead of 'continue'.
            continue
            
        image, results = mediapipe_detection(image, pose)
        
        #Draw the pose annotation on the image.
        draw_styled_landmarks(image, results)
        keypoints = extract_keypoints(results)
        cv2.namedWindow('MediaPipe Pose', cv2.WINDOW_KEEPRATIO)
        cv2.resizeWindow('MediaPipe Pose', int(image.shape[1]/2.5), int(image.shape[0]/2))
        cv2.imshow('MediaPipe Pose', image)
        if cv2.waitKey(5) & 0xFF == 27:
            break

cap.release()
cv2.destroyAllWindows()
