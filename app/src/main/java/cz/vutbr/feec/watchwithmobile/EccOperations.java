package cz.vutbr.feec.watchwithmobile;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECPoint;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

//class that holds info about curve, secret key for now and functions with EC
public class EccOperations {
    byte [] ID= new byte[]{(byte)0x10,
            (byte)0x20,
            (byte)0x30,
            (byte)0x40,
            (byte)0x50,
    };
    byte [] MYID= new byte[]{(byte)0x11,
            (byte)0x22,
            (byte)0x33,
            (byte)0x44,
            (byte)0x55,
    };
    //BigInteger prime = new BigInteger("115792089237316195423570985008687907853269984665640564039457584007908834671663");
    //final static private BigInteger n = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);
    //BigInteger A = new BigInteger("0");
    //BigInteger B= new BigInteger("7");
    //ECCurve curve = new ECCurve.Fp(prime,A,B);
   // BigInteger Gx= new BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16);
    //BigInteger Gy= new BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16);
    //ECPoint G = curve.createPoint(Gx,Gy);
    //BigInteger G2 = new BigInteger("0479BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8",16);
    BigInteger rand=null;
    byte[] Send=null;
    //ECCurve ellipticCurve= new ECCurve.Fp(prime,A,B);

    //ECCurve ec2=
    BigInteger SecKey;
    //BigInteger SecKey= new BigInteger("B7E151628AED2A6ABF7158809CF4F3C762E7160F38B4DA56A784D9045190CFEF",16); //generating keys is going to be implemented, not needed now for what we do
    //BigInteger PubKey= new BigInteger("DFF1D77F2A671C5F36183726DB2341BE58FEAE1DA2DECED843240F7B502BA659",16);
    ECPoint pubKey=null;
    //BigInteger pubServerBig=new BigInteger("03CD58B4FAE7CD42D41A0AE52433143FAB6F43A15F5CD8D2B69E8F8ECDE72C2069",16);
    BigInteger pubServerBig;
   // byte [] ServerPubKeyBytes= pubServerBig.toByteArray();
   byte [] ServerPubKeyBytes;
    private SecretKey AESKey;
    private byte[] lastAESIV;
    private byte [] TvPoint;
    CurvesSpecifics cs;
    public EccOperations() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        cs= new CurvesSpecifics();
        SecKey=Options.getPrivateKey();
        pubKey= cs.getG().multiply(SecKey);
        pubServerBig=Options.getServerPubKey256();
        ServerPubKeyBytes= pubServerBig.toByteArray();

    }
    //returns public key
    public byte [] givePub(){
        byte [] pub=pubKey.getEncoded(true);

        return pub;
    }
    //generate random Biginteger mod n
    public BigInteger generateRandom()
    {
        do {
            Random randNum = new Random();
            rand = new BigInteger(cs.getN().bitLength(), randNum);
        } while (rand.compareTo(cs.getN()) >= 0);
        return rand;
    }
    //function was used to generate random points in java with random from java, next with random from C for testing and now it is not really used anymore
    public byte[] getRandPoint(){
        //rand=generateRandom();
        ECPoint S= cs.getG().multiply(rand);
        Send= S.getEncoded(true);
        /*BigInteger Se= rand.multiply(G2).mod(prime);
        Send= bytesFromBigInteger(Se);*/
        return Send;
    }
    //function to create the proof of knowledge, pretty fast in Java
    public byte [] SignHash(byte[] hash)
    {
        BigInteger M = null;
        BigInteger hashBig= new BigInteger(1,hash);
        M= rand.add(hashBig.multiply(SecKey)).mod(cs.getN());
        byte [] M2= bytesFromBigInteger(M);

        return  M2;
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

    public BigInteger getRand() {
        return rand;
    }
    //functions takes random number from c and sets it here, mod(n) would probably not be needed, but just to make sure :)
    public void setRand(BigInteger rand) {
        this.rand = rand.mod(cs.getN());
    }
    //from C we have bytearray of X and Y cord, but spongy/bouncy only accepts bigint as x and y cords
    public byte [] getCompPointFromCord(byte[] cords)
    {
        BigInteger Px= utils.getXCord(cords);
        BigInteger Py= utils.getYCord(cords);
        ECPoint ecPoint=cs.getCurve().createPoint(Px,Py); //create ecpoint so we can compress it
        try {
            cs.getCurve().validatePoint(Px, Py);
        }
        catch ( Exception e)
        {
            Log.i("APDU","EROOR point "+utils.bytesToHex(cords));
        }
        byte [] comPoint = ecPoint.getEncoded(true); //compressed point, we need that first byte before x cord to identify where the poitn is, this method does that
        return comPoint;
    }

    public BigInteger getSecKey() {
        return SecKey;
    } //just for testing

    public byte[] genertateSecKey() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        g.initialize(new ECGenParameterSpec("secp256k1"), new SecureRandom());
        KeyPair aKeyPair = g.generateKeyPair();
        ECPrivateKey SecKeyA= (ECPrivateKey)aKeyPair.getPrivate();
        BigInteger SKA= SecKeyA.getS();
        ECPublicKey PubKeyA= (ECPublicKey)aKeyPair.getPublic();
        java.security.spec.ECPoint PUK=PubKeyA.getW();
        BigInteger pubByte = PUK.getAffineX();
        BigInteger pubByteY= PUK.getAffineY();

        ECPoint PUKA= cs.getCurve().createPoint(PUK.getAffineX(),PUK.getAffineY());

        byte [] publicKeyA= PUKA.getEncoded(true);
        Log.i("APDUKEY","public key is "+utils.bytesToHex(publicKeyA));
        Log.i("APDUKEY","private key is "+utils.bytesToHex(utils.bytesFromBigInteger(SKA)));
        return bytesFromBigInteger(SKA);
    }
    byte[] verServerMessage;
    public boolean verifyServer(byte[] sv, byte [] ev,byte[] message) throws NoSuchAlgorithmException, IOException {
        long startTime = System.nanoTime();
        verServerMessage=message;
        ECPoint PubServerEC= cs.getCurve().decodePoint(ServerPubKeyBytes);

        long startTime2=System.nanoTime();
        //start of C implementation its around 10 times faster than the same in java (6ms vs 60ms)
        if(Options.SECURITY_LEVEL==1)
        {
            return false;
        }
        else if(Options.SECURITY_LEVEL==2)
        {
            byte[] pubCByte = PubServerEC.getEncoded(false);
            pubCByte = Arrays.copyOfRange(pubCByte, 1, pubCByte.length);//this is important, ECPoints first byte is not cord uncompressed is 65bytes, we need 64
            /*int[] intCPub = utils.byteArrayToItArray(utils.reverseByte(pubCByte));//reversing the order of bytes to work in C and also making them int arr to work in C
            int[] intSvC = utils.byteArrayToItArray(utils.reverseByte32(sv));
            int[] intEvC = utils.byteArrayToItArray(utils.reverseByte32(ev));
            int[] intTvC = verSignServer(intSvC, intCPub, intEvC);
            byte[] tvC = utils.intArrtoByteArr(intTvC);
            tvC = utils.reverseByte(tvC);*/
            byte[] tvC= verSignServer2(utils.FixForC32(sv),utils.FixForC64(pubCByte),utils.FixForC32(ev),Options.SECURITY_LEVEL);
            if(Options.SECURITY_LEVEL==1)
                tvC=utils.FixFromC56(tvC);
            else
                tvC=utils.FixForC64(tvC);
            byte[] compTvC = getCompPointFromCord(tvC);
            TvPoint = compTvC;
            Log.i("APDU", "from C tv is " + utils.bytesToHex(compTvC));
            long duration2 = System.nanoTime() - startTime2;
            Log.i("APDU", "Ver in C took " + duration2 / 1000000 + " ms");
            //end of C implementation

            boolean isItLegit=compareHashesOfServer(ev);
            long duration = System.nanoTime() - startTime;
            Log.i("APDU", "All and all verify took  " + duration / 1000000 + " ms");
            if(isItLegit)
                return true;
            //if(utils.isEqual(hashToVer,ev))
                //return true;
        }
            return false;
    }
    public boolean compareHashesOfServer(byte [] ev) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = null;
        String hashFunction;
        if(Options.SECURITY_LEVEL==1)
            hashFunction="SHA-244";
        else
            hashFunction="SHA-256";
        digest = MessageDigest.getInstance(hashFunction);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(ID);
        outputStream.write(utils.bytesFromBigInteger(cs.getN()));
        outputStream.write(TvPoint);
        byte connectedBytes[] = outputStream.toByteArray();
        byte[] hashToVer = digest.digest(connectedBytes);
        Log.i("APDU", "hashToVer is  " + utils.bytesToHex(hashToVer));
        outputStream.close();
        if(utils.isEqual(hashToVer,ev))
            return true;
        else
            return false;

    }

    public byte [] generateProof2() throws NoSuchAlgorithmException, IOException {

        byte []t;
        if(Options.SECURITY_LEVEL==1)
            t=utils.FixFromC56(randPoint2());
        else
            t=utils.FixForC64(randPoint2());
        t=getCompPointFromCord(t);
        byte[] randNumber=utils.FixForC32(randReturn2(Options.SECURITY_LEVEL));

        byte [] Tk=generateTk2();
        if(Options.SECURITY_LEVEL==1)
            Tk=utils.FixFromC56(Tk);
        else
            Tk=utils.FixForC64(Tk);
        Tk=getCompPointFromCord(Tk);

        MessageDigest digest = null;
        digest = MessageDigest.getInstance("SHA-256");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write(MYID);
        outputStream.write(t);
        outputStream.write(Tk);
        outputStream.write(verServerMessage);
        byte connectedBytes[] = outputStream.toByteArray( );
        byte [] hash = digest.digest(connectedBytes);
        Log.i("APDU",utils.bytesToHex(hash));
        outputStream.reset();


        BigInteger mid= ((new BigInteger(1,hash)).multiply(SecKey)).mod(cs.getN());
        BigInteger sv = ((new BigInteger(1,randNumber)).subtract(mid)).mod(cs.getN());
        byte [] signature= utils.bytesFromBigInteger(sv);
        outputStream.write(MYID);
        outputStream.write(hash);
        outputStream.write(signature);
        byte[] finalMSG=outputStream.toByteArray();
        outputStream.close();
        InicializeAES(Tk);
        return finalMSG;
    }
    byte[] hashForBoth;
    public byte[] GenerateProofWithWatch(byte[] Tk2, byte[] t1) throws NoSuchAlgorithmException, IOException {

        byte []randPoint;
        if(Options.SECURITY_LEVEL==1)
            randPoint=utils.FixFromC56(randPoint2());
        else
            randPoint=utils.FixForC64(randPoint2());
        randPoint=getCompPointFromCord(randPoint);
        byte[] randNumber=utils.FixForC32(randReturn2(Options.SECURITY_LEVEL));

        byte [] t=generateTWithWatch2(getCBytePointFromCompressedByte(t1));
        if(Options.SECURITY_LEVEL==1)
            t=utils.FixFromC56(t);
        else
            t=utils.FixForC64(t);
        t=getCompPointFromCord(t);

        byte[] Tk=generateTkWithWatch2(getCBytePointFromCompressedByte(Tk2));
        if(Options.SECURITY_LEVEL==1)
            Tk=utils.FixFromC56(Tk);
        else
            Tk=utils.FixForC64(Tk);
        Tk=getCompPointFromCord(Tk);
        Log.i("APDU","Tk is"+utils.bytesToHex(Tk));
        Log.i("APDU","t is "+utils.bytesToHex(t));
        MessageDigest digest = null;
        digest = MessageDigest.getInstance("SHA-256");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write(MYID);
        outputStream.write(t);
        outputStream.write(Tk);
        outputStream.write(verServerMessage);
        byte connectedBytes[] = outputStream.toByteArray( );
        hashForBoth = digest.digest(connectedBytes);
        Log.i("APDU","Hash for both is "+utils.bytesToHex(hashForBoth));
        outputStream.reset();
        BigInteger mid= ((new BigInteger(1,hashForBoth)).multiply(SecKey)).mod(cs.getN());
        BigInteger sv = ((new BigInteger(1,randNumber)).subtract(mid)).mod(cs.getN());
        byte [] signature= utils.bytesFromBigInteger(sv);
        Log.i("APDU","sig from phone is "+utils.bytesToHex(signature));
        outputStream.write(MYID);
        outputStream.write(hashForBoth);
        outputStream.write(signature);
        byte[] finalMSG=outputStream.toByteArray();
        outputStream.close();
        InicializeAES(Tk);
        return finalMSG;

    }
    public byte[] getHashForBoth()
    {
        Log.i("APDU","Hash is "+utils.bytesToHex(hashForBoth));
        return hashForBoth;
    }
    public int[] getCPointFromCompressedByte(byte[] compressedPoint)
    {
        ECPoint point= cs.getCurve().decodePoint(compressedPoint);
        byte[] uncompressedJavaPoint=point.getEncoded(false);
        uncompressedJavaPoint=Arrays.copyOfRange(uncompressedJavaPoint,1,uncompressedJavaPoint.length);
        int[] CReadyPoint=utils.byteArrayToItArray(utils.reverseByte(uncompressedJavaPoint));
        return CReadyPoint;
    }
    public byte[] getCBytePointFromCompressedByte(byte[] compressedPoint)
    {
        ECPoint point= cs.getCurve().decodePoint(compressedPoint);
        byte[] uncompressedJavaPoint=point.getEncoded(false);
        byte []uncompressedPoint=Arrays.copyOfRange(uncompressedJavaPoint,1,uncompressedJavaPoint.length);
        uncompressedPoint=utils.FixForC64(uncompressedPoint);
        return uncompressedPoint;

    }
    public byte[] getCompPointFromCPoint(int[] CPoint)
    {
        byte[] point=utils.reverseByte(utils.intArrtoByteArr(CPoint));
        return getCompPointFromCord(point);
    }
    public void InicializeAES(byte[] Tk)
    {
        byte [] SecretKeyBytes= Arrays.copyOfRange(Tk,1,Tk.length);
        AESKey= new SecretKeySpec(SecretKeyBytes, 0, SecretKeyBytes.length, "AES");
        lastAESIV= new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(lastAESIV);
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public byte[] generateSecretAPDUMessage(byte[] msg) throws Exception {
        byte [] encrypted=AESGCMClass.encrypt(msg,AESKey,lastAESIV);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write(lastAESIV);
        outputStream.write(encrypted);
        byte connectedBytes[] = outputStream.toByteArray( );
        outputStream.close();
        return connectedBytes;

    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public byte[] decodeAESCommand(byte [] command) throws Exception {
        byte[] IV= Arrays.copyOfRange(command,5,17);
        byte[] AESed= Arrays.copyOfRange(command,17,command.length-1);
        byte[] decrypted= AESGCMClass.decrypt(AESed,AESKey,IV);
        return decrypted;
    }

    public native byte[] verSignServer2(byte[] sv, byte [] pub, byte [] ev, int SecLevel);
    
    public native byte[] randPoint2();
    public native byte[] randReturn2(int SevLevel);
    public native byte[] generateTk2();
    public native byte[] generateTWithWatch2(byte[] t1);
    public native byte[] generateTkWithWatch2(byte[] tk2);


    public byte[] getTvPoint() {
        return TvPoint;
    }
}
