package cz.vutbr.feec.watchwithmobile;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;

public class utils {
    //function to convert bytearray to string so we can use it in logs
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    /**
     originally from: http://stackoverflow.com/a/9855338
     */
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    //we just need to compare the fisrt 2 bytes to know what command we got, could be changed in future if parameters are added
    public static boolean isCommand(byte[] a, byte[] b){
        if(a[1]==b[1] && a[0]== b[0])
            return true;
        else return false;


    }
    //function to reverse bytes of rand number from C, 32bytes
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
    //function to reverse bytes of a point passed from C(64bytes), for some reason this has to be done, either micro-ecc stores points differently, or converting from intarr to bytearr makes it, not sure
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
    //function to compare byteArrays
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
    //functions to get cords from byte we got from C
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
    //convert byte arry to int array, can be used to pass to C if needed
    public static int[] byteArrayToItArray(byte[] toDo){
        IntBuffer intBuf =
                ByteBuffer.wrap(toDo)
                        .order(ByteOrder.BIG_ENDIAN)
                        .asIntBuffer();
        int[] array = new int[intBuf.remaining()];
        intBuf.get(array);
        return array;
    }
    //convert int array from c to byte array
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
    //take 2 arrays make it one
    public static byte[] appendByteArray(byte[] arr1,byte[] arr2) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(arr1);
        output.write(arr2);
        byte[] result=output.toByteArray();
        return result;
    }


}
