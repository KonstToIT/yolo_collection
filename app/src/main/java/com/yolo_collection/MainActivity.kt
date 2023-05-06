package com.yolo_collection

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private val REQUEST_CAMERA_PERMISSION = 101
    private val REQUEST_IMAGE_CAPTURE = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Проверяем наличие разрешений на использование камеры
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }

        // Находим кнопки и назначаем обработчики нажатия на них
        val takePictureButton = findViewById<Button>(R.id.take_picture_button)
        takePictureButton.setOnClickListener {
            takePicture()
        }
        val saveButton = findViewById<Button>(R.id.save_picture_button)
        saveButton.setOnClickListener {
            onSaveButtonClicked()
        }
    }

    private fun takePicture() {
        // Проверяем наличие разрешений на использование камеры
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
            return
        }

        // Создаем интент для вызова камеры
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // Вызываем камеру и ожидаем результата
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    // Обрабатываем результат вызова камеры
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            // Выводим изображение на экран
            val imageView = findViewById<ImageView>(R.id.Image_view)
            imageView.setImageBitmap(imageBitmap)
        }
    }





    // Сохраняем изображение в галерее
    private fun saveImage(bitmap: Bitmap) {
        val filename = "${UUID.randomUUID()}.png"
        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
        }

        // Сохраняем изображение в галерее
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            try {
                resolver.openOutputStream(it)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                resolver.delete(it, null, null)
                Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }

    // Обработчик нажатия на кнопку "Сохранить"
    private fun onSaveButtonClicked() {
        // Получаем изображение из ImageView
        val imageView = findViewById<ImageView>(R.id.Image_view)
        val drawable = imageView.drawable as? BitmapDrawable
        val bitmap = drawable?.bitmap ?: return

        // Сохраняем изображение в галерее
        saveImage(bitmap)
    }
}


