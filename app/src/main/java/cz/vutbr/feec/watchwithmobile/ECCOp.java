package cz.vutbr.feec.watchwithmobile;

import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECPoint;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

//class that does operations on elliptic curve with the help of bouncy castle
public class ECCOp {
    UtilsV2 utilsV2 = new UtilsV2();
    //those are parameters of secp256k1 curve so we can create it to do point operations
    final static private BigInteger n = new BigInteger("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141", 16);
    BigInteger prime = new BigInteger("115792089237316195423570985008687907853269984665640564039457584007908834671663");
    BigInteger A = new BigInteger("0");
    BigInteger B= new BigInteger("7");
    BigInteger Gx= new BigInteger("79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798", 16);
    BigInteger Gy= new BigInteger("483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8", 16);
    ECCurve ellipticCurve= new ECCurve.Fp(prime,A,B);
    ECPoint G = ellipticCurve.createPoint(Gx,Gy);;
    BigInteger SecKey= new BigInteger("B7E151628AED2A6ABF7158809CF4F3C762E7160F38B4DA56A784D9045190CFEF",16);
    ECPoint pubKey=null;
    //computes sig*G
    //returns public key
    public ECCOp()
    {
        pubKey= G.multiply(SecKey);
    }
    public byte [] givePub(){
        byte [] pub=pubKey.getEncoded(true);

        return pub;
    }
    public byte[] givePubDecoded()
    {
        byte [] pub= pubKey.getEncoded(false);
        return Arrays.copyOfRange(pub,1,pub.length);
    }
    public ECPoint computeP1(BigInteger S){
        ECPoint P1= G.multiply(S);
        return P1;
    }
    public byte [] P1dec(BigInteger S)
    {
        ECPoint P1= G.multiply(S);
        return P1.getEncoded(false);
    }
    //computes R+P*c
    public ECPoint computeP2(byte[] r, byte[] pub,byte[] hash) throws IOException {
        ECPoint pubKey= ellipticCurve.decodePoint(pub);
        BigInteger hashInt= utilsV2.bigIntFromBytes(hash).mod(n);
        ECPoint mid= pubKey.multiply(hashInt);


        ECPoint R= ellipticCurve.decodePoint(r);
        ECPoint result = R.add(mid);
        return result;
    }

    //verifies the signature, if sig*G== R+P*c it is legit signature and proof that prover know the private key
    public boolean signVer(BigInteger S,byte[] r, byte[] pub,byte[] hash) throws IOException {
        ECPoint P1 = computeP1(S);
        ECPoint P2 = computeP2(r,pub,hash);
        if(P1.equals(P2)) return true;
        else return false;
    }
    public byte[] decodeEncoded(byte[] point)
    {
        ECPoint rr=ellipticCurve.decodePoint(point);
        byte [] toReturn=rr.getEncoded(false);
        return Arrays.copyOfRange(toReturn,1,toReturn.length);
    }

}
