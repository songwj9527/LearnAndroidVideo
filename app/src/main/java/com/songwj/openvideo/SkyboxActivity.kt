package com.songwj.openvideo

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.songwj.openvideo.opengl.renders.SkyboxAbsRender
import kotlinx.android.synthetic.main.activity_skybox.*

class SkyboxActivity : AppCompatActivity(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var sensor: Sensor? = null
    private val rotationMatrix = FloatArray(16)

    private var skyboxAbsRender: SkyboxAbsRender? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_skybox)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        if (sensorManager != null) {
            sensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        }
        Matrix.setIdentityM(rotationMatrix, 0)

        gl_surface_view.setEGLContextClientVersion(3)
        skyboxAbsRender = SkyboxAbsRender()
        gl_surface_view.setRenderer(skyboxAbsRender)
        gl_surface_view.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
    }

    override fun onResume() {
        super.onResume()
        if (sensorManager != null && sensor != null) {
            sensorManager!!.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        if (sensorManager != null && sensor != null) {
            sensorManager!!.unregisterListener(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        skyboxAbsRender?.release()
    }

    override fun onSensorChanged(event: SensorEvent) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        gl_surface_view.queueEvent(Runnable {
            skyboxAbsRender?.rotation(rotationMatrix)
        })
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
}