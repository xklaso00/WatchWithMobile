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



//function to verify proof of knowledge in C
extern "C"
JNIEXPORT jboolean JNICALL
Java_cz_vutbr_feec_watchwithmobile_MainActivity_verSignC(JNIEnv *env, jobject /* this */,jintArray sign,jintArray rand, jintArray pub, jintArray hash) {
    //first we need to take those arguments from java
    jintArray cPub= reinterpret_cast<jintArray>(env->GetIntArrayElements(pub, NULL));
    jintArray cRand= reinterpret_cast<jintArray>(env->GetIntArrayElements(rand, NULL));
    jintArray cSign= reinterpret_cast<jintArray>(env->GetIntArrayElements(sign, NULL));
    jintArray cHash= reinterpret_cast<jintArray>(env->GetIntArrayElements(hash, NULL));
    const uECC_word_t* g = uECC_curve_G(uECC_secp256k1());
    //calculation is the same as on PC, just in C
    jint* point1= reinterpret_cast<jint *>(new uECC_word_t[nativeNCount*2 ]());
    uECC_point_mult(reinterpret_cast<uECC_word_t *>(point1), g,
                    reinterpret_cast<const uECC_word_t *>(cSign), curve);
    jint* point12= reinterpret_cast<jint *>(new uECC_word_t[nativeNCount*2 ]());
    uECC_point_mult(reinterpret_cast<uECC_word_t *>(point12),
                    reinterpret_cast<const uECC_word_t *>(cPub),
                    reinterpret_cast<const uECC_word_t *>(cHash), curve);
    jint* point2= reinterpret_cast<jint *>(new uECC_word_t[nativeNCount*2 ]());
    uECC_point_add(reinterpret_cast<const uECC_word_t *>(cRand),
                   reinterpret_cast<const uECC_word_t *>(point12),
                   reinterpret_cast<uECC_word_t *>(point2), curve);
    if(uECC_vli_equal(reinterpret_cast<const uECC_word_t *>(point1),
                      reinterpret_cast<const uECC_word_t *>(point2), nativeNCount * 2)==1)
        return true;
    else
        return false;
    /*jintArray newArray = env->NewIntArray(nativeNCount * 4);
    env->SetIntArrayRegion(newArray, 0,nativeNCount * 4, point1);
    return newArray;*/
}
//just some declaration we will need

jint *rando= reinterpret_cast<jint *>(new uECC_word_t[nativeNCount*2]()); //random number
extern "C"
JNIEXPORT jintArray JNICALL
Java_cz_vutbr_feec_watchwithmobile_MyHostApduService_randReturn(JNIEnv *env, jobject /* this */) { //just a function to return random number to C, only call this after randPoint has been called
    jintArray newArray = env->NewIntArray(nativeNCount*2);
    env->SetIntArrayRegion(newArray,0,nativeNCount*2 ,rando);
    return newArray;

}
//function for generating random point in C
extern "C"
JNIEXPORT jintArray  JNICALL
Java_cz_vutbr_feec_watchwithmobile_MyHostApduService_randPoint(JNIEnv *env, jobject thiz) {

    const uECC_word_t* n = uECC_curve_n(uECC_secp256k1());
    uECC_generate_random_int(reinterpret_cast<uECC_word_t *>(rando), uECC_secp256k1()->n, nativeNCount); //generate random number max is n of curve
    const uECC_word_t* g = uECC_curve_G(uECC_secp256k1());

    jint* randPoint = reinterpret_cast<jint *>(new uECC_word_t[nativeNCount *4]());
    uECC_point_mult(reinterpret_cast<uECC_word_t *>(randPoint), g,
                    reinterpret_cast<const uECC_word_t *>(rando), curve); //multiply G with random number, save to rando

    jintArray newArray = env->NewIntArray(nativeNCount * 4); //size has to be nativeNCount *4 it pretty much means it is 64 bytes when converted to Java
    env->SetIntArrayRegion(newArray, 0,nativeNCount * 4, randPoint);//we release the array for Java to use

    return newArray;


}
jint* tv= reinterpret_cast<jint *>(new uECC_word_t[nativeNCount*2]());
extern "C"
JNIEXPORT jintArray  JNICALL
Java_cz_vutbr_feec_watchwithmobile_EccOperations_verSignServer(JNIEnv *env, jobject thiz,jintArray sv, jintArray pub, jintArray ev) {

    jintArray cPub= reinterpret_cast<jintArray>(env->GetIntArrayElements(pub, NULL));
    jintArray cSv= reinterpret_cast<jintArray>(env->GetIntArrayElements(sv, NULL));
    jintArray cEv= reinterpret_cast<jintArray>(env->GetIntArrayElements(ev, NULL));
    const uECC_word_t* g = uECC_curve_G(uECC_secp256k1());
    jint* point1= reinterpret_cast<jint *>(new uECC_word_t[nativeNCount*2 ]());
    uECC_point_mult(reinterpret_cast<uECC_word_t *>(point1), g,
                    reinterpret_cast<const uECC_word_t *>(cSv), curve);
    jint* point2= reinterpret_cast<jint *>(new uECC_word_t[nativeNCount*2 ]());
    uECC_point_mult(reinterpret_cast<uECC_word_t *>(point2),
                    reinterpret_cast<const uECC_word_t *>(cPub),
                    reinterpret_cast<const uECC_word_t *>(cEv), curve);

    uECC_point_add(reinterpret_cast<const uECC_word_t *>(point1),
                   reinterpret_cast<const uECC_word_t *>(point2),
                   reinterpret_cast<uECC_word_t *>(tv), curve);

    jintArray newArray = env->NewIntArray(nativeNCount * 4);
    env->SetIntArrayRegion(newArray, 0,nativeNCount * 4, tv);
    return newArray;

}
extern "C"
JNIEXPORT jintArray JNICALL
Java_cz_vutbr_feec_watchwithmobile_EccOperations_generateTk(JNIEnv *env, jobject /* this */) { //just a function to return random number to C, only call this after randPoint has been called
    jint* point1= reinterpret_cast<jint *>(new uECC_word_t[nativeNCount*2 ]());
    uECC_point_mult(reinterpret_cast<uECC_word_t *>(point1),
                    reinterpret_cast<const uECC_word_t *>(tv),
                    reinterpret_cast<const uECC_word_t *>(rando), curve);

    jintArray newArray = env->NewIntArray(nativeNCount*4);
    env->SetIntArrayRegion(newArray,0,nativeNCount*4 ,point1);
    return newArray;

}

extern "C"
JNIEXPORT jintArray JNICALL
Java_cz_vutbr_feec_watchwithmobile_EccOperations_randReturn(JNIEnv *env, jobject /* this */) { //just a function to return random number to C, only call this after randPoint has been called
    jintArray newArray = env->NewIntArray(nativeNCount*2);
    env->SetIntArrayRegion(newArray,0,nativeNCount*2 ,rando);
    return newArray;

}
jint* randPoint = reinterpret_cast<jint *>(new uECC_word_t[nativeNCount *2]());
//function for generating random point in C
extern "C"
JNIEXPORT jintArray  JNICALL
Java_cz_vutbr_feec_watchwithmobile_EccOperations_randPoint(JNIEnv *env, jobject thiz) {

    const uECC_word_t* n = uECC_curve_n(uECC_secp256k1());
    uECC_generate_random_int(reinterpret_cast<uECC_word_t *>(rando), uECC_secp256k1()->n, nativeNCount); //generate random number max is n of curve
    const uECC_word_t* g = uECC_curve_G(uECC_secp256k1());


    uECC_point_mult(reinterpret_cast<uECC_word_t *>(randPoint), g,
                    reinterpret_cast<const uECC_word_t *>(rando), curve); //multiply G with random number, save to rando

    jintArray newArray = env->NewIntArray(nativeNCount * 4); //size has to be nativeNCount *4 it pretty much means it is 64 bytes when converted to Java
    env->SetIntArrayRegion(newArray, 0,nativeNCount * 4, randPoint);//we release the array for Java to use

    return newArray;
}
extern "C"
JNIEXPORT jintArray JNICALL
Java_cz_vutbr_feec_watchwithmobile_EccOperations_generateTWithWatch(JNIEnv *env, jobject /* this */,jintArray t1) {
    jintArray ct1= reinterpret_cast<jintArray>(env->GetIntArrayElements(t1, NULL));
    jint* point1= reinterpret_cast<jint *>(new uECC_word_t[nativeNCount*2 ]());
    uECC_point_add(reinterpret_cast<const uECC_word_t *>(ct1),
                   reinterpret_cast<const uECC_word_t *>(randPoint),
                   reinterpret_cast<uECC_word_t *>(point1), curve);

    jintArray newArray = env->NewIntArray(nativeNCount * 4); //size has to be nativeNCount *4 it pretty much means it is 64 bytes when converted to Java
    env->SetIntArrayRegion(newArray, 0,nativeNCount * 4, point1);//we release the array for Java to use

    return newArray;
}
extern "C"
JNIEXPORT jintArray JNICALL
Java_cz_vutbr_feec_watchwithmobile_EccOperations_generateTkWithWatch(JNIEnv *env, jobject /* this */,jintArray tk2) {
    jintArray cTk2= reinterpret_cast<jintArray>(env->GetIntArrayElements(tk2, NULL));

    jint* TvR= reinterpret_cast<jint *>(new uECC_word_t[nativeNCount*2 ]());
    uECC_point_mult(reinterpret_cast<uECC_word_t *>(TvR),
                    reinterpret_cast<const uECC_word_t *>(tv),
                    reinterpret_cast<const uECC_word_t *>(rando), curve);
    jint* tk= reinterpret_cast<jint *>(new uECC_word_t[nativeNCount*2 ]());
    uECC_point_add(reinterpret_cast<const uECC_word_t *>(TvR),
                   reinterpret_cast<const uECC_word_t *>(cTk2), reinterpret_cast<uECC_word_t *>(tk), curve);

    jintArray newArray = env->NewIntArray(nativeNCount * 4); //size has to be nativeNCount *4 it pretty much means it is 64 bytes when converted to Java
    env->SetIntArrayRegion(newArray, 0,nativeNCount * 4, tk);//we release the array for Java to use

    return newArray;
}