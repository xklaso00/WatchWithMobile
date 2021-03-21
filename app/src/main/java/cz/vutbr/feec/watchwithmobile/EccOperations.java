package cz.vutbr.feec.watchwithmobile;

import android.util.Log;

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

//class that holds info about curve, secret key for now and functions with EC
public class EccOperations {
    byte [] ID= new byte[]{(byte)0x10,
            (byte)0x20,
            (byte)0x30,
            (byte)0x40,
            (byte)0x50,
    };
    BigInteger prime = new BigInteger("115792089237316195423570985008687907853269984665640564039457584007908834671663");
    final static private BigInteger n = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);
    BigInteger A = new BigInteger("0");
    BigInteger B= new BigInteger("7");
    ECCurve curve = new ECCurve.Fp(prime,A,B);
    BigInteger Gx= new BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16);
    BigInteger Gy= new BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16);
    ECPoint G = curve.createPoint(Gx,Gy);
    BigInteger G2 = new BigInteger("0479BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8",16);
    BigInteger rand=null;
    byte[] Send=null;
    ECCurve ellipticCurve= new ECCurve.Fp(prime,A,B);
    BigInteger SecKey= new BigInteger("B7E151628AED2A6ABF7158809CF4F3C762E7160F38B4DA56A784D9045190CFEF",16); //generating keys is going to be implemented, not needed now for what we do
    //BigInteger PubKey= new BigInteger("DFF1D77F2A671C5F36183726DB2341BE58FEAE1DA2DECED843240F7B502BA659",16);
    ECPoint pubKey=null;
    BigInteger pubServerBig=new BigInteger("03CD58B4FAE7CD42D41A0AE52433143FAB6F43A15F5CD8D2B69E8F8ECDE72C2069",16);
    byte [] ServerPubKeyBytes= pubServerBig.toByteArray();
    public EccOperations() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {

        pubKey= G.multiply(SecKey);

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
            rand = new BigInteger(n.bitLength(), randNum);
        } while (rand.compareTo(n) >= 0);
        return rand;
    }
    //function was used to generate random points in java with random from java, next with random from C for testing and now it is not really used anymore
    public byte[] getRandPoint(){
        //rand=generateRandom();
        ECPoint S= G.multiply(rand);
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
        M= rand.add(hashBig.multiply(SecKey)).mod(n);
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
        this.rand = rand.mod(n);
    }
    //from C we have bytearray of X and Y cord, but spongy/bouncy only accepts bigint as x and y cords
    public byte [] getCompPointFromCord(byte[] cords)
    {
        BigInteger Px= utils.getXCord(cords);
        BigInteger Py= utils.getYCord(cords);
        ECPoint ecPoint=curve.createPoint(Px,Py); //create ecpoint so we can compress it
        try {
            curve.validatePoint(Px, Py);
        }
        catch ( Exception e)
        {
            Log.i("APDU","EROOR point");
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

        ECPoint PUKA= ellipticCurve.createPoint(PUK.getAffineX(),PUK.getAffineY());

        byte [] publicKeyA= PUKA.getEncoded(true);
        Log.i("APDUKEY","public key is "+utils.bytesToHex(publicKeyA));
        Log.i("APDUKEY","private key is "+utils.bytesToHex(utils.bytesFromBigInteger(SKA)));
        return bytesFromBigInteger(SKA);
    }
    public boolean verifyServer(byte[] sv, byte [] ev) throws NoSuchAlgorithmException, IOException {

        ECPoint PubServerEC= ellipticCurve.decodePoint(ServerPubKeyBytes);
        //BigInteger svBig= new BigInteger(1,sv);
        //BigInteger evBig=new BigInteger(1,ev);
        long startTime = System.nanoTime();
        /*ECPoint Gsv=G.multiply(svBig);
        Log.i("APDU","Gsv is "+utils.bytesToHex(Gsv.getEncoded(true)));

        ECPoint PkEv=PubServerEC.multiply(evBig);
        Log.i("APDU","PkEv is "+utils.bytesToHex(PkEv.getEncoded(true)));
        ECPoint tv= Gsv.add(PkEv);*/
        long duration=System.nanoTime()-startTime;
        Log.i("APDU","Ver in java took "+duration/1000000+" ms");
        startTime=System.nanoTime();
        //start of C implementation
        byte[] pubCByte=PubServerEC.getEncoded(false);
        pubCByte= Arrays.copyOfRange(pubCByte,1,pubCByte.length);
        pubCByte=utils.reverseByte(pubCByte);
        byte [] svC=utils.reverseByte32(sv);
        byte [] evC=utils.reverseByte32(ev);
        int[] intCPub=utils.byteArrayToItArray(pubCByte);
        int[] intSvC=utils.byteArrayToItArray(svC);
        int[] intEvC=utils.byteArrayToItArray(evC);

        int[] intTvC=verSignServer(intSvC,intCPub,intEvC);

        byte[] tvC=utils.intArrtoByteArr(intTvC);
        tvC=utils.reverseByte(tvC);
        byte [] compTvC= getCompPointFromCord(tvC);
        Log.i("APDU","from C tv is "+utils.bytesToHex(compTvC));
        duration=System.nanoTime()-startTime;
        Log.i("APDU","Ver in C took "+duration/1000000+" ms");
        //end of C implementation

        /*Log.i("APDU","tv is "+utils.bytesToHex(tv.getEncoded(true)));
        Log.i("APDU","ev is "+utils.bytesToHex(ev));
        Log.i("APDU","sv is "+utils.bytesToHex(sv));*/
        MessageDigest digest = null;
        digest = MessageDigest.getInstance("SHA-256");
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write(ID);
        outputStream.write(utils.bytesFromBigInteger(n));
        outputStream.write(compTvC);
        byte connectedBytes[] = outputStream.toByteArray( );
        byte [] hashToVer = digest.digest(connectedBytes);
        Log.i("APDU","hashToVer is  "+utils.bytesToHex(hashToVer));
        outputStream.close();


        if(utils.isEqual(hashToVer,ev))
            return true;
        else
            return false;

    }
    public native int[] verSignServer(int[] sv, int [] pub, int [] ev);
}
