package com.example.ridiextractor;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.SecretKeySpec;

public class RidiDecrypter {
    public static String decryptHtml(File file, byte[] bArr) throws Exception {
        byte[] bArr2 = null;
        if (file.exists()) {
            byte[] a = AESEncrypt("AES/ECB/NoPadding", bArr, fileToBytes(file));
            boolean z2 = true;
            byte b = a[a.length - 1];
            if (b >= 1 && b <= 16) {
                for (int length = a.length - 2; length >= a.length - b; length--) {
                    z2 = false;
                }
                if (z2) {
                    a = Arrays.copyOfRange(a, 0, a.length - b);
                }
            }
            bArr2 = a;
        }
        return new String(bArr2);
    }

    private static byte[] fileToBytes(File file) throws IOException {
        int read;
        FileInputStream fileInputStream = new FileInputStream(file);
        FileChannel channel = fileInputStream.getChannel();
        long length = file.length();
        byte[] bArr = new byte[(int) length];
        ByteBuffer wrap = ByteBuffer.wrap(bArr);
        long j = 0;
        do {
            read = channel.read(wrap, j);
            j += read;
            if (j >= length) {
                break;
            }
        } while (read >= 0);
        channel.close();
        fileInputStream.close();
        return bArr;
    }

    private static byte[] AESEncrypt(String str, byte[] bArr, byte[] bArr2) throws Exception {
        SecretKeySpec secretKeySpec = new SecretKeySpec(bArr, "AES");
        Cipher cipher = Cipher.getInstance(str);
        cipher.init(2, secretKeySpec);
        CipherInputStream cipherInputStream = new CipherInputStream(new BufferedInputStream(new ByteArrayInputStream(bArr2)), cipher);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr3 = new byte[8192];
        while (true) {
            int read = cipherInputStream.read(bArr3);
            if (read > 0) {
                byteArrayOutputStream.write(bArr3, 0, read);
            } else {
                cipherInputStream.close();
                return byteArrayOutputStream.toByteArray();
            }
        }
    }

    public static byte[] generateKey(String str, File file) throws Exception {
        byte[] b = AESEncrypt("AES/ECB/NoPadding", str.substring(0, 16).getBytes(), fileToBytes(file));
        if (new String(b, 0, str.length()).equals(str)) {
            return new String(b, str.length() + 32, 16).getBytes();
        }
        return null;
    }
}
