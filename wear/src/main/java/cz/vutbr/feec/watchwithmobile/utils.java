package cz.vutbr.feec.watchwithmobile;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Arrays;
//same util class as used in NFC version, it is well commented there, so check it there if needed
public class utils {

    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
    /*
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
        if(Options.SECURITY_LEVEL==2)
        {
            if (b.length == 32) {
                return b;
            } else if (b.length > 32) {
                return Arrays.copyOfRange(b, b.length - 32, b.length);
            } else {
                byte[] buf = new byte[32];
                System.arraycopy(b, 0, buf, buf.length - b.length, b.length);
                return buf;
            }
        }
        else if(Options.SECURITY_LEVEL==1)
        {
            if (b.length > 28) {
                if (b[0] == 0)
                {
                    byte[] tmp = new byte[b.length - 1];
                    System.arraycopy(b, 1, tmp, 0, tmp.length);
                    b = tmp;
                }
            }

            return b;
        }
        else
        {
            if (b.length > 20)
            {
                if (b[0] == 0)
                {
                    byte[] tmp = new byte[b.length - 1];
                    System.arraycopy(b, 1, tmp, 0, tmp.length);
                    b = tmp;
                }
            }
            return b;
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
    public static byte[] FixFromC56(byte[] toFix)
    {
        /*byte[] temp=new byte[4];
        byte[] zeros= new byte[]{(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};
        int xCtr=0;
        int yCtr=0;
        for (int i=0;i<14;i++)
        {

        }



        byte[] new56= new byte[56];*/

        byte [] newByte= new byte[56];
        byte [] x=Arrays.copyOfRange(toFix,0,28);
        byte [] y= Arrays.copyOfRange(toFix,28,toFix.length-8);
        for(int i=0;i<28;i++)
        {
            newByte[i]=x[27-i];
        }
        for (int i=0;i<28;i++)
        {
            newByte[i+28]=y[27-i];
        }
        return  newByte;

    }
    public static byte[] ReverseOnly(byte [] toFix)
    {
        byte[] newByte = new byte[toFix.length];
        byte [] x=Arrays.copyOfRange(toFix,0,toFix.length/2);
        byte [] y= Arrays.copyOfRange(toFix,toFix.length/2,toFix.length);
        for(int i=0;i<toFix.length/2;i++)
        {
            newByte[i]=x[toFix.length/2-1-i];
        }
        for (int i=0;i<toFix.length/2;i++)
        {
            newByte[i+toFix.length/2]=y[toFix.length/2-1-i];
        }
        return  newByte;
    }
    public static  byte[]FixForC64(byte[] toFix)
    {
        if(toFix.length==64)
        {
            byte[] newByte = new byte[64];
            byte[] x = Arrays.copyOfRange(toFix, 0, 32);
            byte[] y = Arrays.copyOfRange(toFix, 32, toFix.length);
            for (int i = 0; i < 32; i++) {
                newByte[i] = x[31 - i];
            }
            for (int i = 0; i < 32; i++) {
                newByte[i + 32] = y[31 - i];
            }
            return newByte;
        }
        else if(toFix.length==56) //if we pass 56bytes long points to C we have to reverse them and pad them with zeros, so the point is in same format as in micro-ecc
        {
            byte [] newByte= new byte[64];
            byte [] x=Arrays.copyOfRange(toFix,0,28);
            byte [] y= Arrays.copyOfRange(toFix,28,toFix.length);
            /*for(int i=0;i<28;i++)
            {
                newByte[i]=x[27-i];
            }
            for (int i=0;i<4;i++)
            {
                newByte[i+28]=(byte)0x00;
            }
            for (int i=0;i<28;i++)
            {
                newByte[i+32]=y[27-i];
            }
            for (int i=0;i<4;i++)
            {
                newByte[i+60]=(byte)0x00;
            }*/
            for(int i=0;i<28;i++)
            {
                newByte[i]=x[27-i];
            }
            for (int i=0;i<28;i++)
            {
                newByte[i+28]=y[27-i];
            }
            for (int i=0;i<8;i++)
            {
                newByte[i+56]=(byte)0x00;
            }

            return  newByte;
        }
        return null;
    }
    public static byte[] FixForC32(byte[] toFix)
    {
        byte[] newByte = new byte[toFix.length];
        for (int i = 0; i < toFix.length; i++)
        {
            newByte[i] = toFix[toFix.length-1 - i];
        }
        return newByte;
    }
    public static byte[] appendByteArray(byte[] arr1,byte[] arr2) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(arr1);
        output.write(arr2);
        byte[] result=output.toByteArray();
        return result;
    }

}
