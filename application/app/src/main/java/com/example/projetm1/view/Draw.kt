package com.example.projetm1.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark


/**
 * Cette classe a pour but de dessiner sur une vue le squelette de la personne qui est detectée par le pose detector de MLKIT
 * **/

class Draw(context: Context?, var pose: Pose, var imageWidth : Int, var imageHeight : Int) : View(context) {
    lateinit var boundaryPaint: Paint
    lateinit var leftPaint: Paint
    lateinit var rightPaint: Paint
    private var scaleFactor = 1.0f
    private var postScaleWidthOffset = 0f
    private var postScaleHeightOffset = 0f
    private var viewHeight = 0
    private var viewWidth = 0




    // Fonction qui permet d'ajuster la taille de l'affichage dès qu'elle change
     override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
         viewHeight = h
         viewWidth = w
         init()
    }

    // Fonction qui se relance dès que la taille d'affichage change et qui permet de recalculer les paramètres de scaling en fonction de la nouvelle taille
    private fun init() {
        boundaryPaint = Paint()
        boundaryPaint.color = Color.WHITE
        boundaryPaint.strokeWidth = 10f
        boundaryPaint.style = Paint.Style.STROKE

        leftPaint = Paint()
        leftPaint.strokeWidth = 10f
        leftPaint.color = Color.GREEN
        rightPaint = Paint()
        rightPaint.strokeWidth = 10f
        rightPaint.color = Color.YELLOW


        if( imageHeight < 0 || imageWidth < 0){
            return
        }
        else {
            // calcul du scaleFactor important pour que les points s'affichent bien aux positions correspondantessur la preview
            val viewAspectRatio = width.toFloat() / height
            val imageAspectRatio: Float = imageWidth.toFloat() / imageHeight
            postScaleWidthOffset = 0f
            postScaleHeightOffset = 0f
            if (viewAspectRatio > imageAspectRatio) {
                // The image needs to be vertically cropped to be displayed in this view.
                scaleFactor = width.toFloat() / imageWidth
                postScaleHeightOffset = (width.toFloat() / imageAspectRatio - height) / 2
            } else {
                // The image needs to be horizontally cropped to be displayed in this view.
                scaleFactor = height.toFloat() / imageHeight
                postScaleWidthOffset = (height.toFloat() * imageAspectRatio - width) / 2
            }
        }

    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val landmarks = pose.allPoseLandmarks

        // On trace les points
        for (landmark in landmarks) {

            canvas?.drawCircle(translateX(landmark.position.x),translateY(landmark.position.y),8.0f,boundaryPaint)

        }

        // On cherche les coordonnées des points qui nous intéressent
        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY)
        val rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY)
        val leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX)
        val rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX)
        val leftThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB)
        val rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB)
        val leftHeel = pose.getPoseLandmark(PoseLandmark.LEFT_HEEL)
        val rightHeel = pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL)
        val leftFootIndex = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX)
        val rightFootIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX)



        // On trace les lignes entre les points qui doivent être reliés
        if ( leftShoulder != null && rightShoulder != null && leftElbow != null && rightElbow != null && leftWrist != null && rightWrist != null && leftHip != null && rightHip != null &&
            leftKnee != null && rightKnee != null && leftAnkle != null && rightAnkle != null && leftPinky != null && rightPinky != null && leftIndex != null && rightIndex != null &&
                leftThumb != null && rightThumb != null && leftHeel != null && rightHeel != null && leftFootIndex != null && rightFootIndex != null) {
            canvas?.drawLine(
                translateX(leftShoulder.position.x),
                translateY(leftShoulder.position.y),
                translateX(rightShoulder.position.x),
                translateY(rightShoulder.position.y),
                boundaryPaint
            )
            canvas?.drawLine(
                translateX(leftHip.position.x),
                translateY(leftHip.position.y),
                translateX(rightHip.position.x),
                translateY(rightHip.position.y),
                boundaryPaint
            )

            //Left body

            canvas?.drawLine(
                translateX(leftShoulder.position.x),
                translateY(leftShoulder.position.y),
                translateX(leftElbow.position.x),
                translateY(leftElbow.position.y),
                leftPaint
            )
            canvas?.drawLine(
                translateX(leftElbow.position.x),
                translateY(leftElbow.position.y),
                translateX(leftWrist.position.x),
                translateY(leftWrist.position.y),
                leftPaint
            )
            canvas?.drawLine(
                translateX(leftShoulder.position.x),
                translateY(leftShoulder.position.y),
                translateX(leftHip.position.x),
                translateY(leftHip.position.y),
                leftPaint
            )
            canvas?.drawLine(
                translateX(leftHip.position.x),
                translateY(leftHip.position.y),
                translateX(leftKnee.position.x),
                translateY(leftKnee.position.y),
                leftPaint
            )
            canvas?.drawLine(
                translateX(leftKnee.position.x),
                translateY(leftKnee.position.y),
                translateX(leftAnkle.position.x),
                translateY(leftAnkle.position.y),
                leftPaint
            )
            canvas?.drawLine(
                translateX(leftWrist.position.x),
                translateY(leftWrist.position.y),
                translateX(leftThumb.position.x),
                translateY(leftThumb.position.y),
                leftPaint
            )
            canvas?.drawLine(
                translateX(leftWrist.position.x),
                translateY(leftWrist.position.y),
                translateX(leftPinky.position.x),
                translateY(leftPinky.position.y),
                leftPaint
            )
            canvas?.drawLine(
                translateX(leftWrist.position.x),
                translateY(leftWrist.position.y),
                translateX(leftIndex.position.x),
                translateY(leftIndex.position.y),
                leftPaint
            )
            canvas?.drawLine(
                translateX(leftIndex.position.x),
                translateY(leftIndex.position.y),
                translateX(leftPinky.position.x),
                translateY(leftPinky.position.y),
                leftPaint
            )
            canvas?.drawLine(
                translateX(leftAnkle.position.x),
                translateY(leftAnkle.position.y),
                translateX(leftHeel.position.x),
                    translateY(leftHeel.position.y),
                leftPaint
            )
            canvas?.drawLine(
                translateX(leftHeel.position.x),
                translateY(leftHeel.position.y),
                translateX(leftFootIndex.position.x),
                translateY(leftFootIndex.position.y),
                leftPaint
            )

            //Right body
            canvas?.drawLine(
                translateX(rightShoulder.position.x),
                translateY(rightShoulder.position.y),
                translateX(rightElbow.position.x),
                translateY(rightElbow.position.y),
                rightPaint
            )
            canvas?.drawLine(
                translateX(rightElbow.position.x),
                translateY(rightElbow.position.y),
                translateX(rightWrist.position.x),
                translateY(rightWrist.position.y),
                rightPaint
            )
            canvas?.drawLine(
                translateX(rightShoulder.position.x),
                translateY(rightShoulder.position.y),
                translateX(rightHip.position.x),
                translateY(rightHip.position.y),
                rightPaint
            )
            canvas?.drawLine(
                translateX(rightHip.position.x),
                translateY(rightHip.position.y),
                translateX(rightKnee.position.x),
                translateY(rightKnee.position.y),
                rightPaint
            )
            canvas?.drawLine(
                translateX(rightKnee.position.x),
                translateY(rightKnee.position.y),
                translateX(rightAnkle.position.x),
                translateY(rightAnkle.position.y),
                rightPaint
            )
            canvas?.drawLine(
                translateX(rightWrist.position.x),
                translateY(rightWrist.position.y),
                translateX(rightThumb.position.x),
                translateY(rightThumb.position.y),
                rightPaint
            )
            canvas?.drawLine(
                translateX(rightWrist.position.x),
                translateY(rightWrist.position.y),
                translateX(rightPinky.position.x),
                translateY(rightPinky.position.y),
                rightPaint
            )
            canvas?.drawLine(
                translateX(rightWrist.position.x),
                translateY(rightWrist.position.y),
                translateX(rightIndex.position.x),
                translateY(rightIndex.position.y),
                rightPaint
            )
            canvas?.drawLine(
                translateX(rightIndex.position.x),
                translateY(rightIndex.position.y),
                translateX(rightPinky.position.x),
                translateY(rightPinky.position.y),
                rightPaint
            )
            canvas?.drawLine(
                translateX(rightAnkle.position.x),
                translateY(rightAnkle.position.y),
                translateX(rightHeel.position.x),
                translateY(rightHeel.position.y),
                rightPaint
            )
            canvas?.drawLine(
                translateX(rightHeel.position.x),
                translateY(rightHeel.position.y),
                translateX(rightFootIndex.position.x),
                translateY(rightFootIndex.position.y),
                rightPaint
            )
        }
    }


    // Ces trois fonctions permettent simplement de passer de la taille de l'image prise dans l'analyseur à la taille de l'affichage
    private fun scale(imagePixel: Float): Float {
        return imagePixel * scaleFactor
    }

    private fun translateX(x: Float): Float {
        return (scale(x) - postScaleWidthOffset)

    }

    private fun translateY(y: Float): Float {
        return  (scale(y) - postScaleHeightOffset)

    }
}