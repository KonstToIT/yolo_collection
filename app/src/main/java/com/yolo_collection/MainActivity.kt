package com.yolo_collection

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.media.ExifInterface
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.min

class MainActivity : AppCompatActivity() {

    private val REQUEST_CAMERA_PERMISSION = 101
    private val REQUEST_IMAGE_CAPTURE = 102

    private lateinit var currentPhotoPath: String
    private var isRotationLocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lockScreenRotation()
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }

        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()

            }
            catch (ex: IOException) {
                // Handle error when creating the temporary file
                ex.printStackTrace()
                null
            }

            photoFile?.let {
                val photoUri = FileProvider.getUriForFile(this, "com.yolo_collection.fileprovider", it)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        currentPhotoPath = imageFile.absolutePath
        return imageFile
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Use currentPhotoPath to access the saved image
            setPic()

        }
    }
    private fun setPic() {
        // Получаем размеры ImageView
        val imageView = findViewById<ImageView>(R.id.Image_view)
        val targetW = imageView.width
        val targetH = imageView.height

        // Получаем размеры изображения
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            BitmapFactory.decodeFile(currentPhotoPath, this)
            val photoW = outWidth
            val photoH = outHeight

            // Определяем, насколько нужно масштабировать изображение
            val scaleFactor = min(photoW / targetW, photoH / targetH)

            // Загружаем изображение с заданным масштабом и ориентацией
            inJustDecodeBounds = false
            inSampleSize = scaleFactor
            inPurgeable = true
        }

        val bitmap = BitmapFactory.decodeFile(currentPhotoPath, options)

        // Поворачиваем изображение, если необходимо
        val rotatedBitmap = rotateImageIfRequired(bitmap)

        // производим сегментацию объектов на изображении
        var seg = Segmentation("cat.jpg")
        val segBitmap=seg.segmentImage(rotatedBitmap)
        // устанавливаем изображение в image_view
        imageView.setImageBitmap(segBitmap)

    }
    private fun rotateImageIfRequired(bitmap: Bitmap): Bitmap {
        val exifInterface = ExifInterface(currentPhotoPath)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(270f)
        }

        // Поворачиваем изображение
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }
    // Блокировка вращения экрана
    private fun lockScreenRotation() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        isRotationLocked = true
    }

    // Разблокировка вращения экрана
    private fun unlockScreenRotation() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        isRotationLocked = false
    }


    // Сохраняем изображение в галерее
    private fun saveImage(bitmap: Bitmap) {
        val filename = "${UUID.randomUUID()}.jpg"

        val resolver = contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.IS_PENDING, 1) // Отмечаем, что изображение будет добавлено
        }

        // Создаем URI для сохранения изображения в медиахранилище
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val item = resolver.insert(collection, contentValues)
        item?.let { uri ->
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    // Сохраняем оригинальное изображение без масштабирования и сжатия
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    outputStream.flush()
                }

                // Обновляем медиахранилище для распознавания нового изображения
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)

                Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                // В случае ошибки удаляем созданную запись из медиахранилища
                resolver.delete(uri, null, null)
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


