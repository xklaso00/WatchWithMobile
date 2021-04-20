#include <jni.h>
#include <string>
#include "micro-ecc/uECC.h"
#include "micro-ecc/uECC.c"
#include "micro-ecc/uECC_vli.h"
#include "micro-ecc/types.h"
#include "micro-ecc/SHA256.h"
#include "micro-ecc/SHA256.cpp"
#include "micro-ecc/Crypto.h"



extern "C" JNIEXPORT jstring JNICALL
Java_cz_vutbr_feec_klasovity_microeccimp_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}


uECC_Curve curve=uECC_secp224r1();
const wordcount_t nativeNCount = uECC_curve_num_n_words(uECC_secp256k1());
int byteLenght=28;
//jbyte* rando = reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount*4]());

//return random number used to java

//functions v2 start here
jbyte * rando2= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
extern "C"
JNIEXPORT jbyteArray  JNICALL
Java_cz_vutbr_feec_watchwithmobile_EccOperations_randPointWatch(JNIEnv *env, jobject thiz,jint SecLevel) {
    if(SecLevel==2) {
        curve = uECC_secp256k1();
        byteLenght=32;
    }
    else{
        curve=uECC_secp224r1();
        byteLenght=28;
    }

    uECC_generate_random_int(reinterpret_cast<uECC_word_t *>(rando2), curve->n, nativeNCount); //generate random number max is n of curve
    const uECC_word_t* g = uECC_curve_G(curve);
    jbyte * randPoint2= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
    uECC_point_mult(reinterpret_cast<uECC_word_t *>(randPoint2), g,
                    reinterpret_cast<const uECC_word_t *>(rando2), curve); //multiply G with random number, save to rando

    jbyteArray newArray=env->NewByteArray(byteLenght*2);
    env->SetByteArrayRegion(newArray,0,byteLenght*2,randPoint2);
    return newArray;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_cz_vutbr_feec_watchwithmobile_EccOperations_randReturnWatch(JNIEnv *env, jobject /* this */,jint SecLevel) { //just a function to return random number to C, only call this after randPoint has been called

    if (SecLevel==1)
        byteLenght=28;
    else
        byteLenght=32;
    jbyteArray newArray = env->NewByteArray(byteLenght);
    env->SetByteArrayRegion(newArray,0,byteLenght ,rando2);
    return newArray;
}
extern "C"
JNIEXPORT jbyteArray  JNICALL
Java_cz_vutbr_feec_watchwithmobile_EccOperations_CforTk2Watch(JNIEnv *env, jobject /* this */,jbyteArray Tv) {
    jbyteArray cTv= reinterpret_cast<jbyteArray>(env->GetByteArrayElements(Tv, NULL));
    jbyte* point1= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
    uECC_point_mult(reinterpret_cast<uECC_word_t *>(point1),
                    reinterpret_cast<const uECC_word_t *>(cTv),
                    reinterpret_cast<const uECC_word_t *>(rando2), curve);
    jbyteArray newArray = env->NewByteArray(byteLenght*2);
    env->SetByteArrayRegion(newArray, 0,byteLenght*2, point1);
    return newArray;

}
jbyte * pub32= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
jbyte * pub28= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
extern "C"
JNIEXPORT jbyteArray  JNICALL
Java_cz_vutbr_feec_watchwithmobile_EccOperations_GenerateSecKey(JNIEnv *env, jobject /* this */,jint SecLevel) {
    jbyte * randNum= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
    if (SecLevel==1)
    {
        uECC_generate_random_int(reinterpret_cast<uECC_word_t *>(randNum), uECC_secp224r1()->n, nativeNCount);
        byteLenght=28;
        uECC_point_mult(reinterpret_cast<uECC_word_t *>(pub28), uECC_secp224r1()->G,
                        reinterpret_cast<const uECC_word_t *>(randNum), uECC_secp224r1());
    }
    else
    {
        uECC_generate_random_int(reinterpret_cast<uECC_word_t *>(randNum), uECC_secp256k1()->n, nativeNCount);
        byteLenght=32;
        uECC_point_mult(reinterpret_cast<uECC_word_t *>(pub32), uECC_secp256k1()->G,
                        reinterpret_cast<const uECC_word_t *>(randNum), uECC_secp256k1());
    }
    jbyteArray newArray = env->NewByteArray(byteLenght);
    env->SetByteArrayRegion(newArray, 0,byteLenght, randNum);
    return newArray;
}
extern "C"
JNIEXPORT jbyteArray  JNICALL
Java_cz_vutbr_feec_watchwithmobile_EccOperations_GetPubKey(JNIEnv *env, jobject /* this */,jint SecLevel) {
    if (SecLevel==1)
    {
        byteLenght=28;
        jbyteArray newArray = env->NewByteArray(byteLenght*2);
        env->SetByteArrayRegion(newArray, 0,byteLenght*2, pub28);
        return newArray;
    }
    else{
        byteLenght=32;
        jbyteArray newArray = env->NewByteArray(byteLenght*2);
        env->SetByteArrayRegion(newArray, 0,byteLenght*2, pub32);
        return newArray;
    }

}