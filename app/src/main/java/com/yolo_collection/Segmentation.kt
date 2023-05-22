package com.yolo_collection

import org.pytorch.IValue
import org.pytorch.Module
import org.pytorch.Tensor
import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap


class Segmentation(private val modelPath: String) {
    private lateinit var module: Module

    init {
        loadModel()
    }

    private fun loadModel() {
        // Загрузка модели YOLOv5s-seg из указанных файлов
        module = Module.load(modelPath)
    }

    fun segmentImage(imageBitmap: Bitmap): Bitmap {
        // Преобразование изображения в тензор
        val inputTensor = bitmapToFloat32Tensor(imageBitmap)

        // Передача тензора модели и получение результата сегментации
        val outputTensor = module.forward(IValue.from(inputTensor)).toTensor()

        // Преобразование тензора результата в изображение
        val outputImage = tensorToBitmap(outputTensor)

        return outputImage
    }
    private fun tensorToBitmap(tensor: Tensor): Bitmap {
        // Получение размеров тензора
        val height = tensor.shape()[2].toInt()
        val width = tensor.shape()[3].toInt()


        // Создание пустого изображения Bitmap с заданными размерами
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Копирование данных из тензора в пиксели изображения
        val pixels = IntArray(width * height)
        val data = tensor.dataAsFloatArray
        for (i in 0 until width * height) {
            val value = data[i]
            // Преобразование значения пикселя из диапазона [0, 1] в диапазон [0, 255]
            val pixelValue = (value * 255).toInt()
            // Установка значения пикселя
            pixels[i] = pixelValue or (pixelValue shl 8) or (pixelValue shl 16) or -0x1000000
        }

        // Установка пикселей в изображение Bitmap
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        return bitmap
    }
    fun bitmapToFloat32Tensor(bitmap: Bitmap, mean: FloatArray = floatArrayOf(0.485f, 0.456f, 0.406f), std: FloatArray = floatArrayOf(0.229f, 0.224f, 0.225f)): Tensor {
        val width = bitmap.width
        val height = bitmap.height
        val floatValues = FloatArray(width * height * 3)

        var pixelIndex = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                // Извлечение значений цвета RGB из пикселя
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // Нормализация значений цвета и сохранение в массив floatValues
                floatValues[pixelIndex++] = ((r - mean[0]) / std[0]) / 255.0f
                floatValues[pixelIndex++] = ((g - mean[1]) / std[1]) / 255.0f
                floatValues[pixelIndex++] = ((b - mean[2]) / std[2]) / 255.0f
            }
        }

        // Создание тензора FloatTensor и заполнение его значениями из массива floatValues
        val shape = longArrayOf(1, 3, height.toLong(), width.toLong())
        return Tensor.fromBlob(floatValues, shape)
    }
}


