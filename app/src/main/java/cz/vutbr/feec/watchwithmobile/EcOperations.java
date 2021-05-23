package cz.vutbr.feec.watchwithmobile;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.spongycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi;
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
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

//class that holds info about curve, secret key for now and functions with EC
public class EcOperations {
    static {
        System.loadLibrary("native-lib");
    }
    /*byte [] ID= new byte[]{(byte)0x10,
            (byte)0x20,
            (byte)0x30,
            (byte)0x40,
            (byte)0x50,
    };
    byte [] MYID= new byte[]{(byte)0x10,
            (byte)0x20,
            (byte)0x30,
            (byte)0x40,
            (byte)0x50,
    };*/
    private byte[] privateKey256;
    private byte[] privateKey224;
    private byte[] privateKey160;
    BigInteger rand=null;
    byte[] Send=null;
    //ECCurve ellipticCurve= new ECCurve.Fp(prime,A,B);
    byte[] CRand;
    //ECCurve ec2=
    BigInteger SecKey;
    ECPoint pubKey=null;
    //BigInteger pubServerBig=new BigInteger("03CD58B4FAE7CD42D41A0AE52433143FAB6F43A15F5CD8D2B69E8F8ECDE72C2069",16);
    BigInteger pubServerBig;
   // byte [] ServerPubKeyBytes= pubServerBig.toByteArray();
   //byte [] ServerPubKeyBytes;
    private SecretKey AESKey;
    private byte[] lastAESIV;
    private byte [] TvPoint;
    private byte[] PubKey256;
    private byte[] PubKey224;
    private byte[] PubKey160;
    CurvesSpecifics cs;
    public EcOperations() {
        cs= new CurvesSpecifics();
        SecKey=Options.getPrivateKey();
        //pubKey= cs.getG().multiply(SecKey);
        pubServerBig=Options.getServerPubKey();
        //ServerPubKeyBytes= pubServerBig.toByteArray();

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
        byte [] M2= utils.bytesFromBigInteger(M);

        return  M2;
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
        long time=System.nanoTime();
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
       // Log.i("CompTimer","Comp took "+(System.nanoTime()-time)+"ns");
        return comPoint;
    }

    public BigInteger getSecKey() {
        return SecKey;
    } //just for testing

    public byte[] genertateSecKey() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
        String curveName;
        if(Options.SECURITY_LEVEL==1)
            curveName="secp224r1";
        else if(Options.SECURITY_LEVEL==2)
            curveName="secp256k1";
        else
            curveName="secp160r1";

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
        else if(Options.SECURITY_LEVEL==2)
            PubKey256=publicKeyA;
        else
            PubKey160=publicKeyA;
        return utils.bytesFromBigInteger(SKA);
    }

    byte[] verServerMessage;
    byte[] serverTimeStamp;
    @RequiresApi(api = Build.VERSION_CODES.O)

    public boolean verifyServer(byte[] sv, byte [] ev, byte[] message, byte[] timeStamp) throws NoSuchAlgorithmException, IOException {
        long startTime = System.nanoTime();
        verServerMessage=message;
        serverTimeStamp=timeStamp;
        byte[] ourTime=GenerateTimeStamp();
        if(Options.timeChecking) {

            if (utils.isEqual(timeStamp, ourTime))
                Log.i("ECCOP", "Time stamps are the same");
            else {
                if (utils.isEqual(Arrays.copyOfRange(timeStamp, 0, timeStamp.length - 2), Arrays.copyOfRange(ourTime, 0, ourTime.length - 2)))
                    Log.i("ECCOP", "Time stamps are almost the same");
                else {
                    Log.i("ECCOP", "Time stamps different too much, false returned");
                    return false;
                }
            }
        }
        ECPoint PubServerEC= cs.getCurve().decodePoint(utils.bytesFromBigInteger(Options.getServerPubKey()));

        long startTime2=System.nanoTime();
        //start of C implementation its around 10 times faster than the same in java (6ms vs 60ms)
        /*if(Options.SECURITY_LEVEL==1)
        {
            byte[] pubCByte = PubServerEC.getEncoded(false);
            pubCByte = Arrays.copyOfRange(pubCByte, 1, pubCByte.length);
            byte[] tvC= verSignServer2(utils.FixForC32(sv),utils.FixForC64(pubCByte),utils.FixForC32(ev),Options.SECURITY_LEVEL);
            return false;

        }*/
        //else if(Options.SECURITY_LEVEL==2)
            byte[] pubCByte = PubServerEC.getEncoded(false);
            pubCByte = Arrays.copyOfRange(pubCByte, 1, pubCByte.length);//this is important, ECPoints first byte is not cord uncompressed is 65bytes, we need 64
/*
            Log.i("APDU", "sv is " + utils.bytesToHex(sv));
            Log.i("APDU", "ev is " + utils.bytesToHex(ev));
        Log.i("APDU", "fixed sv is " + utils.bytesToHex(utils.FixForC32(sv)));
        Log.i("APDU", "fixed ev is " + utils.bytesToHex(utils.FixForC32(ev)));
        Log.i("APDU", "publickey fixed is " + utils.bytesToHex(utils.FixForC64(pubCByte)));*/
        byte[] tvC;
        if(Options.SECURITY_LEVEL==2)
            tvC= verSignServer2(utils.FixForC32(sv),utils.FixForC64(pubCByte),utils.FixForC32(ev),Options.SECURITY_LEVEL);
        else
            tvC= verSignServer2(utils.PadWithZeros(utils.FixForC32(sv)),utils.FixForC64(pubCByte),utils.PadWithZeros(utils.FixForC32(ev)),Options.SECURITY_LEVEL);

        long duration2 = System.nanoTime() - startTime2;
        Log.i("Timer", "Ver in C took " + duration2 / 1000000 + " ms");
            Log.i("ECCOP","TVC is "+utils.bytesToHex(tvC));
           // byte[] svgC=getPt1();
            //byte[] PubEv=getPt2();
            //tvC=Arrays.copyOfRange(tvC,0,64);
        //Log.i("APDU","SVG in C is "+utils.bytesToHex(utils.FixFromCBytesWithSec(svgC)));
        //Log.i("APDU","Pub in C is "+utils.bytesToHex(utils.FixFromCBytesWithSec(PubEv)));
            if(Options.SECURITY_LEVEL==1)
                tvC=utils.FixFromC56(tvC);
            else if(Options.SECURITY_LEVEL==2)
                tvC=utils.FixForC64(tvC);
            else
                tvC=utils.FixFrom40(tvC);
            byte[] compTvC = getCompPointFromCord(tvC);
            TvPoint = compTvC;

            Log.i("APDU", "from C tv is " + utils.bytesToHex(compTvC));

            //end of C implementation

            boolean isItLegit=compareHashesOfServer(ev,timeStamp);
        duration2 = System.nanoTime() - startTime2;
        Log.i("Timer", "with hashes Ver in C took " + duration2 / 1000000 + " ms");
            if(isItLegit)
                return true;
            computeVerInJava(sv,ev);
            isItLegit=compareHashesOfServer(ev,timeStamp);
        long duration = System.nanoTime() - startTime;
        Log.i("Timer", "All and all verify took  " + duration / 1000000 + " ms");
            if(isItLegit)
                return true;
            //if(utils.isEqual(hashToVer,ev))
                //return true;
            Log.i("apdu","server not legit");
            return false;
    }
    public byte[] computeVerInJava(byte[] sv, byte[] ev)
    {
        BigInteger svBig= new BigInteger(1,sv);
        BigInteger evBig=new BigInteger(1,ev);
        ECPoint gSv= cs.getG().multiply(svBig);
        Log.i("APDU","Svg in java is "+utils.bytesToHex(gSv.getEncoded(false)));
        ECPoint PubServerEC= cs.getCurve().decodePoint(utils.bytesFromBigInteger(Options.getServerPubKey()));
        ECPoint pkEv=PubServerEC.multiply(evBig);
        Log.i("APDU","PUBEV in java is "+utils.bytesToHex(pkEv.getEncoded(false)));
        ECPoint res=gSv.add(pkEv);
        byte[] tv=res.getEncoded(true);
        Log.i("APDU","tv in java is "+utils.bytesToHex(tv));
        TvPoint=res.getEncoded(true);
        return tv;
    }

    public boolean compareHashesOfServer(byte [] ev, byte[] timeStamp) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = null;
        String hashFunction=Options.getHashName();

        /*if(Options.SECURITY_LEVEL==1)
            hashFunction="SHA-224";
        else
            hashFunction="SHA-256";*/

            digest = MessageDigest.getInstance(hashFunction);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(Options.MYID);
        outputStream.write(utils.bytesFromBigInteger(cs.getN()));
        outputStream.write(TvPoint);
        outputStream.write(timeStamp);
        byte connectedBytes[] = outputStream.toByteArray();
        byte[] hashToVer = digest.digest(connectedBytes);
        Log.i("APDU", "hashToVer is  " + utils.bytesToHex(hashToVer));
        outputStream.close();
        if(utils.isEqual(hashToVer,ev))
            return true;
        else
            return false;

    }

    public byte [] generateProofForSingle() throws NoSuchAlgorithmException, IOException {

        byte []t=randPoint2(Options.SECURITY_LEVEL);
        Log.i("APDU"," T before fix is "+utils.bytesToHex(t));
        if(Options.SECURITY_LEVEL==1)
            t=utils.FixFromC56(t);
        else if(Options.SECURITY_LEVEL==2)
            t=utils.FixForC64(t);
        else
            t=utils.FixFrom40(t);
        Log.i("APDU"," T after fix is "+utils.bytesToHex(t));
        t=getCompPointFromCord(t);
        byte[] randNumber=utils.FixForC32(randReturn2(Options.SECURITY_LEVEL));
        CRand=randNumber;
        byte [] Tk=generateTk2(getCBytePointFromCompressedByte(TvPoint),Options.SECURITY_LEVEL);
        Log.i("APDU"," Tk before fix is "+utils.bytesToHex(Tk));
        if(Options.SECURITY_LEVEL==1)
            Tk=utils.FixFromC56(Tk);
        else if(Options.SECURITY_LEVEL==2)
            Tk=utils.FixForC64(Tk);
        else
            Tk=utils.FixFrom40(Tk);
        Log.i("APDU"," T after fix is "+utils.bytesToHex(Tk));
        Tk=getCompPointFromCord(Tk);

        String hashFunction=Options.getHashName();

        MessageDigest digest = null;
        digest = MessageDigest.getInstance(hashFunction);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write(Options.MYID);
        outputStream.write(utils.bytesFromBigInteger(cs.getN()));
        outputStream.write(serverTimeStamp);
        outputStream.write(t);
        outputStream.write(Tk);
        //outputStream.write(verServerMessage);
        byte connectedBytes[] = outputStream.toByteArray( );
        byte [] hash = digest.digest(connectedBytes);
        Log.i("ECCOP", "Hash is"+utils.bytesToHex(hash));


        outputStream.reset();


        BigInteger mid= ((new BigInteger(1,hash)).multiply(Options.getPrivateKey())).mod(cs.getN());
        BigInteger sv = ((new BigInteger(1,randNumber)).subtract(mid)).mod(cs.getN());
        byte [] signature= utils.bytesFromBigInteger(sv);
        outputStream.write(Options.MYID);
        outputStream.write(hash);
        outputStream.write(signature);
        byte[] finalMSG=outputStream.toByteArray();
        outputStream.close();
        Log.i("APDU"," Sig is"+utils.bytesToHex(signature));
        InicializeAES(Tk);
        return finalMSG;
    }
    byte[] hashForBoth;
    public byte[] GenerateProofWithWatch(byte[] Tk2, byte[] t1) throws NoSuchAlgorithmException, IOException {

        byte []randPoint=utils.FixFromCBytesWithSec(randPoint2(Options.SECURITY_LEVEL));
        /*if(Options.SECURITY_LEVEL==1)
            randPoint=utils.FixFromC56(randPoint2());
        else
            randPoint=utils.FixForC64(randPoint2());*/

        randPoint=getCompPointFromCord(randPoint);
        byte[] randNumber=utils.FixForC32(randReturn2(Options.SECURITY_LEVEL));

        byte [] t=generateTWithWatch2(getCBytePointFromCompressedByte(t1),Options.SECURITY_LEVEL);
        /*if(Options.SECURITY_LEVEL==1)
            t=utils.FixFromC56(t);
        else
            t=utils.FixForC64(t);*/
        t=utils.FixFromCBytesWithSec(t);
        t=getCompPointFromCord(t);

        byte[] Tk=generateTkWithWatch2(getCBytePointFromCompressedByte(Tk2),getCBytePointFromCompressedByte(TvPoint),Options.SECURITY_LEVEL);
        /*if(Options.SECURITY_LEVEL==1)
            Tk=utils.FixFromC56(Tk);
        else
            Tk=utils.FixForC64(Tk);*/
        Tk=utils.FixFromCBytesWithSec(Tk);
        Tk=getCompPointFromCord(Tk);
        Log.i("APDU","Tk is"+utils.bytesToHex(Tk));
        Log.i("APDU","t is "+utils.bytesToHex(t));
        MessageDigest digest = null;
        String hashFunction=Options.getHashName();
       /* if(Options.SECURITY_LEVEL==1)
            hashFunction="SHA-224";
        else
            hashFunction="SHA-256";*/
        digest = MessageDigest.getInstance(hashFunction);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write(Options.MYID);
        outputStream.write(utils.bytesFromBigInteger(cs.getN()));
        outputStream.write(serverTimeStamp);
        outputStream.write(t);
        outputStream.write(Tk);
        //outputStream.write(verServerMessage);
        byte connectedBytes[] = outputStream.toByteArray( );
        hashForBoth = digest.digest(connectedBytes);
        Log.i("APDU","Hash for both is "+utils.bytesToHex(hashForBoth));
        outputStream.reset();
        long SignStart=System.nanoTime();
        BigInteger mid= ((new BigInteger(1,hashForBoth)).multiply(Options.getPrivateKey())).mod(cs.getN());
        BigInteger sv = ((new BigInteger(1,randNumber)).subtract(mid)).mod(cs.getN());
        Log.i("Timer","Sign in phone took "+ (System.nanoTime()-SignStart)+" ns");
        byte [] signature= utils.bytesFromBigInteger(sv);
        Log.i("APDU","sig from phone is "+utils.bytesToHex(signature));
        outputStream.write(Options.MYID);
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
    public void InicializeAES(byte[] Tk) throws NoSuchAlgorithmException {
        if(Options.SECURITY_LEVEL==1||Options.SECURITY_LEVEL==2)
        {
            MessageDigest digest = null;
            digest = MessageDigest.getInstance("SHA-256");
            byte [] hash = digest.digest(Tk);
            byte [] SecretKeyBytes= Arrays.copyOfRange(hash,0,hash.length/2);
            AESKey= new SecretKeySpec(SecretKeyBytes, 0, SecretKeyBytes.length, "AES");
        /*byte [] SecretKeyBytes= Arrays.copyOfRange(Tk,1,Tk.length);
        AESKey= new SecretKeySpec(SecretKeyBytes, 0, SecretKeyBytes.length, "AES");*/
            lastAESIV= new byte[12];
            SecureRandom random = new SecureRandom();
            random.nextBytes(lastAESIV);
        }
        else
        {
            MessageDigest digest = null;
            digest = MessageDigest.getInstance("SHA-256");
            byte [] hash = digest.digest(Tk);
            byte [] SecretKeyBytes= Arrays.copyOfRange(hash,0,24);
            AESKey= new SecretKeySpec(SecretKeyBytes, 0, SecretKeyBytes.length, "DESede");
            lastAESIV= new byte[8];
            SecureRandom random = new SecureRandom();
            random.nextBytes(lastAESIV);
        }

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
        if(Options.SECURITY_LEVEL==1||Options.SECURITY_LEVEL==2) {
            byte[] IV= Arrays.copyOfRange(command,5,17);
            byte[] AESed= Arrays.copyOfRange(command,17,command.length-1);
            byte[] decrypted= AESGCMClass.decrypt(AESed,AESKey,IV);
            return decrypted;
        }
        else
        {
            byte[] IV= Arrays.copyOfRange(command,5,13);
            byte[] AESed= Arrays.copyOfRange(command,13,command.length-1);
            byte[] decrypted= DESClass.decrypt(AESed,AESKey,IV);
            return decrypted;
        }

    }
    public byte[] registerDev(byte []ID) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        Options.setSecurityLevel(2);
        privateKey256=genertateSecKey();
        Options.setSecurityLevel(1);
        privateKey224=genertateSecKey();
        Options.setSecurityLevel(0);
        privateKey160=genertateSecKey();
        byte[] bothKeys=utils.appendByteArray(PubKey256,PubKey224);
        byte[] allKeys= utils.appendByteArray(bothKeys,PubKey160);
        return allKeys;
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static byte[] GenerateTimeStamp()
    {
        String stamp= ZonedDateTime
                .now( ZoneId.systemDefault() )
                .format( DateTimeFormatter.ofPattern( "uuuu.MM.dd.HH.mm.ss" ) );
        Log.i("TIMECHECK","our time "+stamp);
        byte[] timeBytes=stamp.getBytes();
        return timeBytes;
    }
    public byte[] getPrivateKey256()
    {
        return privateKey256;
    }
    public byte[] getPrivateKey224()
    {
        return privateKey224;
    }
    public byte[] getPrivateKey160()
    {
        return privateKey160;
    }

   public byte[] comuteInJavaProof() throws NoSuchAlgorithmException, IOException {
       ECPoint tpt=cs.getG().multiply(new BigInteger(1,CRand));
       byte []t= tpt.getEncoded(true);
       Log.i("APDU"," T JAVA"+utils.bytesToHex(t));
       ECPoint TkPt=cs.getCurve().decodePoint(TvPoint).multiply(new BigInteger(1,CRand));

       byte [] Tk=TkPt.getEncoded(true);


       Log.i("APDU"," Tk JAVA "+utils.bytesToHex(Tk));


       String hashFunction=Options.getHashName();

       MessageDigest digest = null;
       digest = MessageDigest.getInstance(hashFunction);
       ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
       outputStream.write(Options.MYID);
       outputStream.write(t);
       outputStream.write(Tk);
       outputStream.write(verServerMessage);
       byte connectedBytes[] = outputStream.toByteArray( );
       byte [] hash = digest.digest(connectedBytes);
       Log.i("APDU"," Hash JAVA is"+utils.bytesToHex(hash));

       outputStream.reset();


       BigInteger mid= ((new BigInteger(1,hash)).multiply(Options.getPrivateKey())).mod(cs.getN());
       BigInteger sv = ((new BigInteger(1,CRand)).subtract(mid)).mod(cs.getN());
       byte [] signature= utils.bytesFromBigInteger(sv);
       outputStream.write(Options.MYID);
       outputStream.write(hash);
       outputStream.write(signature);
       byte[] finalMSG=outputStream.toByteArray();
       outputStream.close();
       Log.i("APDU"," Sig JAVA is"+utils.bytesToHex(signature));
       InicializeAES(Tk);
       return finalMSG;
   }


    public native byte[] verSignServer2(byte[] sv, byte [] pub, byte [] ev, int SecLevel);
    
    public native byte[] randPoint2(int SecLevel);
    public native byte[] randReturn2(int SevLevel);
    public native byte[] generateTk2(byte[] tv, int SecLevel);
    public native byte[] generateTWithWatch2(byte[] t1, int SecLevel);
    public native byte[] generateTkWithWatch2(byte[] tk2,byte[] tv,int SecLevel);
    public native byte[] getPt1();
    public native byte[] getPt2();

    public byte[] getTvPoint() {
        return TvPoint;
    }
}
