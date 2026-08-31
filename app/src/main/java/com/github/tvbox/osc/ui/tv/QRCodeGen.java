package com.github.tvbox.osc.ui.tv;

import android.graphics.Bitmap;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Map;

/**
 * @author pj567
 * @date :2021/1/5
 * @description:
 */
public class QRCodeGen {

    /**
     * 生成二维码 Bitmap (支持自定义容错级别)
     *
     * @param content            二维码内容
     * @param width              宽度 (px)
     * @param height             高度 (px)
     * @param padding            白边大小 (建议 1-4)
     * @param errorCorrectionLevel 容错级别 (L, M, Q, H)
     * @return Bitmap
     */
    public static Bitmap generateBitmap(String content, int width, int height, int padding, ErrorCorrectionLevel errorCorrectionLevel) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        Map<EncodeHintType, Object> hints = new HashMap<>();
        
        // 设置字符集
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        
        // 设置容错级别
        // 如果传入 null，则不设置，ZXing 会默认使用 L 级
        if (errorCorrectionLevel != null) {
            hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
        }
        
        // 设置边距
        hints.put(EncodeHintType.MARGIN, padding);
        
        try {
            BitMatrix encode = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            int[] pixels = new int[width * height];
            
            // 遍历矩阵生成像素数组
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    if (encode.get(j, i)) {
                        // 前景色 (码点颜色)
                        pixels[i * width + j] = 0xffBC3F00; 
                    } else {
                        // 背景色
                        pixels[i * width + j] = 0xff6CEE6C; 
                    }
                }
            }
            return Bitmap.createBitmap(pixels, 0, width, width, height, Bitmap.Config.ARGB_8888);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 重载方法：最简调用，默认 Padding 为 1，电视端默认使用 H 级容错，抗干扰能力强
     */
    public static Bitmap generateBitmap(String content, int width, int height) {
        return generateBitmap(content, width, height, 1, ErrorCorrectionLevel.H);
    }
}

// 调用方法：
// 1. 使用默认高容错 (电视端推荐)
// ivQRCode.setImageBitmap(QRCodeGen.generateBitmap(address, AutoSizeUtils.dp2px(getContext(), 300), AutoSizeUtils.dp2px(getContext(), 300)));

// 2. 如果需要指定其它容错级别 (例如 M 级) & Padding
// ivQRCode.setImageBitmap(QRCodeGen.generateBitmap(
//    address, 
//    AutoSizeUtils.dp2px(getContext(), 300), 
//    AutoSizeUtils.dp2px(getContext(), 300), 
//    1, // padding
//    ErrorCorrectionLevel.M // 指定容错级别
// ));
