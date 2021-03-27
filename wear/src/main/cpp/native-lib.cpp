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


uECC_Curve curve=uECC_secp256k1();
const wordcount_t nativeNCount = uECC_curve_num_n_words(uECC_secp256k1());
//jbyte* rando = reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount*4]());
jint *rando= reinterpret_cast<jint *>(new uECC_word_t[nativeNCount*2]());
//return random number used to java
extern "C"
JNIEXPORT jintArray JNICALL
Java_cz_vutbr_feec_watchwithmobile_EccOperations_randReturn(JNIEnv *env, jobject /* this */) {
    jintArray newArray = env->NewIntArray(nativeNCount*2);

    env->SetIntArrayRegion(newArray,0,nativeNCount*2 ,rando);
    return newArray;

}
//generate random point, same as NFC version
extern "C"
JNIEXPORT jintArray  JNICALL
Java_cz_vutbr_feec_watchwithmobile_EccOperations_randPoint(JNIEnv *env, jobject /* this */) {

    const uECC_word_t* n = uECC_curve_n(uECC_secp256k1());
    uECC_generate_random_int(reinterpret_cast<uECC_word_t *>(rando), uECC_secp256k1()->n, nativeNCount);
    const uECC_word_t* g = uECC_curve_G(uECC_secp256k1());
    jint* randPoint = reinterpret_cast<jint *>(new uECC_word_t[nativeNCount *2]());
    uECC_point_mult(reinterpret_cast<uECC_word_t *>(randPoint), g,
                    reinterpret_cast<const uECC_word_t *>(rando), curve);
    jintArray newArray = env->NewIntArray(nativeNCount * 4);
    env->SetIntArrayRegion(newArray, 0,nativeNCount * 4, randPoint);

    return newArray;


}
extern "C"
JNIEXPORT jintArray  JNICALL
Java_cz_vutbr_feec_watchwithmobile_EccOperations_CforTk2(JNIEnv *env, jobject /* this */,jintArray Tv) {
    jintArray cTv= reinterpret_cast<jintArray>(env->GetIntArrayElements(Tv, NULL));
    jint* point1= reinterpret_cast<jint *>(new uECC_word_t[nativeNCount*2 ]());
    uECC_point_mult(reinterpret_cast<uECC_word_t *>(point1),
                    reinterpret_cast<const uECC_word_t *>(cTv),
                    reinterpret_cast<const uECC_word_t *>(rando), curve);
    jintArray newArray = env->NewIntArray(nativeNCount * 4);
    env->SetIntArrayRegion(newArray, 0,nativeNCount * 4, point1);
    return newArray;

}
