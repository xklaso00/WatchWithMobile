package cz.vutbr.feec.watchwithmobile;

import android.util.Log;

import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECPoint;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Random;
//class that holds info about curve, secret key for now and functions with EC, pretty much the same as NFC version, it is better commented there
public class EccOperations {
    public byte[] PubKey224;
    public byte[] PubKey256;
    private byte[] privateKey256;
    private byte[] privateKey224;
    BigInteger prime = new BigInteger("115792089237316195423570985008687907853269984665640564039457584007908834671663");
    //final static private BigInteger n = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);
    BigInteger A = new BigInteger("0");
    BigInteger B= new BigInteger("7");
    //ECCurve curve = new ECCurve.Fp(prime,A,B);
    BigInteger Gx= new BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16);
    BigInteger Gy= new BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16);
    //ECPoint G = curve.createPoint(Gx,Gy);
    BigInteger G2 = new BigInteger("0479BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8",16);
    BigInteger rand=null;
    byte[] Send=null;
    BigInteger SecKey;
    CurvesSpecifics cs;
    //BigInteger PubKey= new BigInteger("DFF1D77F2A671C5F36183726DB2341BE58FEAE1DA2DECED843240F7B502BA659",16);
    ECPoint pubKey=null;
    public EccOperations() {
        cs= new CurvesSpecifics();
        SecKey=Options.getPrivateKey();
        pubKey= cs.getG().multiply(SecKey);

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

    public byte[] getRandPoint(){
        rand=generateRandom();
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
        M= (hashBig.multiply(Options.getPrivateKey())).mod(cs.getN());
        M=(rand.subtract(M)).mod(cs.getN());
        byte [] M2= utils.bytesFromBigInteger(M);
        //Log.i("neco", "M is " +ByteArrayToHexString(M2));
        return  M2;
    }






    public BigInteger getRand() {
        return rand;
    }

    public void setRand(BigInteger rand) {
        this.rand = rand.mod(cs.getN());
    }
    //from C we have bytearray of X and Y cord, but spongy/bouncy only accepts bigint as x and y cords
    public byte [] getCompPointFromCord(byte[] cords)
    {
        BigInteger Px= utils.getXCord(cords);
        BigInteger Py= utils.getYCord(cords);
        ECPoint ecPoint=cs.getCurve().createPoint(Px,Py);
        byte [] comPoint = ecPoint.getEncoded(true);
        return comPoint;
    }
    public byte[] createT1()
    {
        //int[] T1Int=randPoint();
        //byte []t1=utils.reverseByte(utils.intArrtoByteArr(T1Int));
        //t1=getCompPointFromCord(t1);
        //int[] randNumberInt=randReturn();
        //byte[] randNumber=utils.reverseByte32(utils.intArrtoByteArr(randNumberInt));
        //rand=new BigInteger(1,randNumber);
       // return t1;
        byte [] T1=randPointWatch(Options.SECURITY_LEVEL);
        Log.i("watch","T1 form C before fix is "+utils.bytesToHex(T1));
        if(Options.SECURITY_LEVEL==1)
            T1=utils.ReverseOnly(T1);
        else
            T1=utils.FixForC64(T1);
        Log.i("watch","T1 form C after fix is "+utils.bytesToHex(T1));
        T1=getCompPointFromCord(T1);
        byte[] randNumber=randReturnWatch(Options.SECURITY_LEVEL);
        randNumber=utils.FixForC32(randNumber);
        rand=new BigInteger(1,randNumber);
        return T1;


    }
    public byte [] createTK2( byte [] Tv)
    {
        ECPoint TvPoint= cs.getCurve().decodePoint(Tv);
        byte[] TVLong=TvPoint.getEncoded(false);
        byte[] TVforC= Arrays.copyOfRange(TVLong,1,TVLong.length);
        Log.i("watch","TV for C is "+utils.bytesToHex(utils.ReverseOnly(TVforC)));
        TVforC=utils.ReverseOnly(TVforC);
        byte[] Tk2=CforTk2Watch(TVforC);
        Log.i("watch","tk2 from C is "+utils.bytesToHex(utils.ReverseOnly(Tk2)));
        if(Options.SECURITY_LEVEL==1)
            Tk2=utils.ReverseOnly(Tk2);
        else
            Tk2=utils.FixForC64(Tk2);
        Tk2=getCompPointFromCord(Tk2);
        return Tk2;
    }

    public BigInteger getSecKey() {
        return SecKey;
    }

    public byte[] genertateSecKeyJava() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        String curveName;
        if(Options.SECURITY_LEVEL==1)
            curveName="secp224r1";
        else
            curveName="secp256k1";
        g.initialize(new ECGenParameterSpec(curveName), new SecureRandom());
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
        if(Options.SECURITY_LEVEL==1)
            PubKey224=publicKeyA;
        else
            PubKey256=publicKeyA;
        return utils.bytesFromBigInteger(SKA);
    }
    public byte[] registerDev() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        long startTimer=System.nanoTime();
       /* Options.setSecurityLevel(2);
        privateKey256=genertateSecKeyJava();
        Options.setSecurityLevel(1);
        privateKey224=genertateSecKeyJava();*/
       privateKey224=GenerateSecKey(1);
       privateKey256=GenerateSecKey(2);
       privateKey224=utils.FixForC32(privateKey224);
       privateKey256=utils.FixForC32(privateKey256);
       PubKey256=GetPubKey(2);
       PubKey224=GetPubKey(1);
       Options.setSecurityLevel(2);
       PubKey256=getCompPointFromCord(utils.FixForC64(PubKey256));
       Options.setSecurityLevel(1);
       PubKey224=getCompPointFromCord(utils.ReverseOnly(PubKey224));
       Log.i("watchKey","256 pub is "+utils.bytesToHex(PubKey256));
        Log.i("watchKey","224 pub is "+utils.bytesToHex(PubKey224));



        Log.i("WatchGen","Generating keys took in C "+(System.nanoTime()-startTimer)/1000000+"ms");
        byte[] bothKeys=utils.appendByteArray(PubKey256,PubKey224);
        return bothKeys;
    }
    public byte[] getPrivateKey256()
    {
        return privateKey256;
    }
    public byte[] getPrivateKey224()
    {
        return privateKey224;
    }

    public native byte[] randPointWatch(int SecLevel);
    public native byte [] randReturnWatch(int SecLevel);
    public native byte [] CforTk2Watch(byte[] Tv);
    public native byte[] GenerateSecKey(int SecLevel);
    public native byte[] GetPubKey(int SecLevel);


}
