package com.songwj.openvideo.camera;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;

public class CameraFrameUtils {
    public static String writeContent(byte[] array, String textName) {
        char[] hexCharTable = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        };
        StringBuilder sb = new StringBuilder();
        for (byte b : array) {
            sb.append(hexCharTable[(b & 0xf0) >> 4]);
            sb.append(hexCharTable[b & 0x0f]);
        }
        Log.i("FileUtils", "writeContent: " + sb.toString());
        FileWriter writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = new FileWriter(Environment.getExternalStorageDirectory() + File.separator + textName, true);
            writer.write(sb.toString());
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static void writeBytes(byte[] array, String fileName) {
        FileOutputStream writer = null;
        try {
            writer = new FileOutputStream(Environment.getExternalStorageDirectory() + File.separator + fileName, true);
            writer.write(array);
            writer.write('\n');

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static byte[] YUV420ToNV21(Image image) {
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    channelOffset = width * height + 1;
                    outputStride = 2;
                    break;
                case 2:
                    channelOffset = width * height;
                    outputStride = 2;
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            int shift = i == 0 ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return data;
    }

    /**
     * yuv转成nv21
     * @param y
     * @param u
     * @param v
     * @param nv21
     * @param stride
     * @param height
     */
    public static void YUV420ToNV21(byte[] y, byte[] u, byte[] v, byte[] nv21, int stride, int height) {
        System.arraycopy(y, 0, nv21, 0, y.length);
        // 注意，若length值为 y.length * 3 / 2 会有数组越界的风险，需使用真实数据长度计算
        int length = y.length + u.length / 2 + v.length / 2;
        int uIndex = 0, vIndex = 0;
        for (int i = stride * height; i < length; i += 2) {
            nv21[i] = v[vIndex];
            nv21[i + 1] = u[uIndex];
            vIndex += 2;
            uIndex += 2;
        }
    }

    /**
     * yuv转成nv21
     * @param yuv
     * @param nv21
     * @param width
     * @param height
     */
    public static void YUV420ToNV21(byte[] yuv, byte[] nv21, int width, int height) {
        // yuv格式为：Y， width * height
        //          UV， width * height / 2
        //          VU， width * height / 2
        System.arraycopy(yuv, 0, nv21, 0, width * height);
        int offset = width * height;
        int length = (yuv.length - offset) / 2;
        int uOffset = offset;
        int vOffset = offset + length;
        for (int i = 0; i < length; i += 2) {
            nv21[offset + i] = yuv[vOffset + i]; // v
            nv21[offset + i + 1] = yuv[uOffset + i]; // u
        }
    }

    public static byte[] nv21toNV12(byte[] nv21) {
        int size = nv21.length;
        byte[] nv12 = new byte[size];
        int len = size * 2 / 3;
        System.arraycopy(nv21, 0, nv12, 0, len);
        int i = len;
        while (i < size - 1) {
            nv12[i] = nv21[i + 1];
            nv12[i + 1] = nv21[i];
            i += 2;
        }
        return nv12;
    }

    public static byte[] nv21toNV12(byte[] nv21, byte[] nv12) {
        int size = nv21.length;
        int len = size * 2 / 3;
        System.arraycopy(nv21, 0, nv12, 0, len);
        int i = len;
        while (i < size - 1) {
            nv12[i] = nv21[i + 1];
            nv12[i + 1] = nv21[i];
            i += 2;
        }
        return nv12;
    }

    public static byte[] nv21Rotate(byte[] data, byte[] output, int width, int height, int rotation) {
        if (rotation == 270) {
            return nv21RotateTo270(data, output, width, height);
        } else if (rotation == 180) {
            return nv21RotateTo180(data, output, width, height);
        } else if (rotation == 90) {
            return nv21RotateTo90(data, output, width, height);
        } else {
            System.arraycopy(data, 0, output, 0, (width * height * 3 / 2));
            return output;
        }
    }

    public static byte[] nv21RotateTo90(byte[] data, byte[] output, int width, int height) {
        int yLen = width * height;
        int buffserSize = yLen * 3 / 2;

        int i = 0;
        int startPos = (height - 1) * width;
        for (int x = 0; x < width; x++) {
            int offset = startPos;
            for (int y = height - 1; y >= 0; y--) {
                output[i] = data[offset + x];
                i++;
                offset -= width;
            }
        }
        // Rotate the U and V color components
        i = buffserSize - 1;
        for (int x = width - 1; x > 0; x = x - 2) {
            int offset = yLen;
            for (int y = 0; y < height / 2; y++) {
                output[i] = data[offset + x];
                i--;
                output[i] = data[offset + (x - 1)];
                i--;
                offset += width;
            }
        }
        return output;
    }

    public static byte[] nv21RotateTo180(byte[] data, byte[] output, int width, int height) {
        int yLen = width * height;
        int buffserSize = yLen * 3 / 2;

        // Rotate the Y
        for (int x = 0; x < yLen; x++) {
            output[x] = data[yLen - 1 - x];
        }
        // Rotate the U and V color components
        for (int x = yLen; x < buffserSize; x = (x + 2)) {
            int offset = (buffserSize - 1) - (x - yLen);
            output[x] = data[offset - 1];
            output[x + 1] = data[offset];
        }
        return output;
    }

    public static byte[] nv21RotateTo270(byte[] data, byte[] output, int width, int height) {
        int yLen = width * height;
        int buffserSize = yLen * 3 / 2;

        // Rotate the Y
        for (int x = (width - 1); x >= 0; x--) {
            for (int y = 0; y < height; y++) {
                output[((width - 1) - x) * height + y] = data[y * width + x];
            }
        }

        // Rotate the U and V color components
        int i = buffserSize - 1;
        for (int x = 1; x < width; x = x + 2) {
            int offset = buffserSize - 1;
            for (int y = 0; y < height / 2; y++) {
                output[i] = data[offset - (width - 1 - x)];
                i--;
                output[i] = data[offset - (width - 1 - x) - 1];
                i--;
                offset -= width;
            }
        }
        return output;
    }

    public static byte[] nv21Reversed(byte[] data, byte[] output, int width, int height) {
        int yLen = width * height;
        int buffserSize = yLen * 3 / 2;

        // Rotate the Y
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                output[width * y + x] = data[width * y + (width - 1 - x)];
            }
        }
        // Rotate the U and V color components
        for (int y = 0; y < height / 2; y++) {
            for (int x = 0; x < width; x = (x + 2)) {
                output[yLen + width * y + x] = data[yLen + width * y + (width - 1 - x - 1)];
                output[yLen + width * y + x + 1] = data[yLen + width * y + (width - 1 - x)];
            }
        }
        return output;
    }

    /**
     * 旋转90度
     */
    public static void portraitData2Raw(byte[] data, byte[] output, int width, int height) {
        int yLen = width * height;
        int uvHeight = height >> 1;
        int k = 0;
        for (int j = 0; j < width; j++) {
            for (int i = height - 1; i >= 0; i--) {
                output[k++] = data[width * i + j];
            }
        }
        for (int j = 0; j < width; j += 2) {
            for (int i = uvHeight - 1; i >= 0; i--) {
                output[k++] = data[yLen + width * i + j];
                output[k++] = data[yLen + width * i + j + 1];
            }
        }
    }
}
