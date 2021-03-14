package cz.vutbr.feec.watchwithmobile;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Random;

public class UtilsV2 {

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    /**
     * Simple way to output byte[] to hex (my readable preference)
     * This version quite speedy; originally from: http://stackoverflow.com/a/9855338
     *
     * @param bytes yourByteArray
     * @return string
     *
     */
    public static BigInteger bigIntFromBytes(byte[] b) {
        return new BigInteger(1, b);
    }
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    public static boolean isCommand(byte[] a, byte[] b){
        if(a[1]==b[1] && a[0]== b[0])
            return true;
        else return false;


    }
    /*
     * Constant-time Byte Array Comparison
     * Less overheard, safer. Originally from: http://codahale.com/a-lesson-in-timing-attacks/
     *
     * @param bytes yourByteArrayA
     * @param bytes yourByteArrayB
     * @return boolean
     *
     */
    public static byte[] reverseByte32(byte[] reverseMe)
    {
        byte [] newByte= new byte[32];
        int c=3;
        int ctr=0;
        int i=31;
        while(i>-1)
        {
            if (c==-1) {
                c=3;
                ctr=ctr+4;
            }
            newByte[i]=reverseMe[c+ctr];
            i--;
            c--;
        }
        return newByte;
    }
    public static byte[] reverseByte(byte[] reverseMe)
    {
        byte [] newByte= new byte[64];
       int c=3;
       int ctr=0;
        int i=31;
        while(i>-1)
        {
            if (c==-1) {
                c=3;
                ctr=ctr+4;
            }
            newByte[i]=reverseMe[c+ctr];
            i--;
            c--;
        }
        i=63;
        ctr=32;
        c=3;
        while(i>31)
        {
            if (c==-1) {
                c=3;
                ctr=ctr+4;
            }
            newByte[i]=reverseMe[c+ctr];
            i--;
            c--;
        }

        return newByte;
    }
    public static boolean isEqual(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
    public static byte[] bytesFromBigInteger(BigInteger n) {

        byte[] b = n.toByteArray();

        if(b.length == 32) {
            return b;
        }
        else if(b.length > 32) {
            return Arrays.copyOfRange(b, b.length - 32, b.length);
        }
        else {
            byte[] buf = new byte[32];
            System.arraycopy(b, 0, buf, buf.length - b.length, b.length);
            return buf;
        }
    }
    public static BigInteger getXCord(byte [] cord)
    {
        byte [] xByte=Arrays.copyOfRange(cord,0,cord.length/2);
        BigInteger xCord= new BigInteger(1,xByte);
        return xCord;
    }
    public static BigInteger getYCord(byte [] cord)
    {
        byte [] yByte=Arrays.copyOfRange(cord,cord.length/2,cord.length);
        BigInteger yCord= new BigInteger(1,yByte);
        return yCord;
    }

    public static int[] byteArrayToItArray(byte[] toDo){
        IntBuffer intBuf =
                ByteBuffer.wrap(toDo)
                        .order(ByteOrder.BIG_ENDIAN)
                        .asIntBuffer();
        int[] array = new int[intBuf.remaining()];
        intBuf.get(array);
        return array;
    }
    public static byte[] intArrtoByteArr(int[] intArr)
    {
        ByteBuffer byteBuffer = ByteBuffer.allocate(intArr.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(intArr);
        byteBuffer.clear();
        intBuffer.clear();
        byte[] byteArr = byteBuffer.array();
        return byteArr;
    }
    public byte [] generateHashToSend(byte [] RandPoint, byte[] PubKey) throws NoSuchAlgorithmException, IOException {
        byte [] randMsg=randomBytes();
        MessageDigest digest = null;
        digest = MessageDigest.getInstance("SHA-256");
        byte[] randMsgHash = digest.digest(randMsg);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write(RandPoint);
        outputStream.write(PubKey);
        outputStream.write(randMsgHash);
        byte connectedBytes[] = outputStream.toByteArray( );
        byte [] hashToReturn = digest.digest(connectedBytes);
        outputStream.close();
        return hashToReturn;
    }
    public byte [] randomBytes()
    {
        byte[] b = new byte[32];
        new Random().nextBytes(b);
        return b;
    }




}
