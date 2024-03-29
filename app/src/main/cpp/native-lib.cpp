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
const wordcount_t nativeNCount = uECC_curve_num_n_words(uECC_secp224r1());
const wordcount_t nativeNCount20 = uECC_curve_num_n_words(uECC_secp160r1());
//jbyte* pointTv=reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
jbyte * rando2= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
jbyte * rando20= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount20 * 2]());
int numOfBytesToGive=64;
//function to verify proof of knowledge in C

//just some declaration we will need

jint *rando= reinterpret_cast<jint *>(new uECC_word_t[nativeNCount*2]()); //random number

//function for generating random point in C

jint* tv= reinterpret_cast<jint *>(new uECC_word_t[nativeNCount*2]());

jint* randPoint = reinterpret_cast<jint *>(new uECC_word_t[nativeNCount *2]());
jbyte* randPoint2= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
jbyte* randPoint20= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount20 * 2]());
//function for generating random point in C

extern "C"
JNIEXPORT jbyteArray  JNICALL
Java_cz_vutbr_feec_watchwithmobile_EcOperations_randPoint2(JNIEnv *env, jobject thiz, jint SecLevel) {
    if(SecLevel==0)
    {
        uECC_generate_random_int(reinterpret_cast<uECC_word_t *>(rando20), curve->n, nativeNCount20);
        const uECC_word_t* g = uECC_curve_G(curve);

        uECC_point_mult(reinterpret_cast<uECC_word_t *>(randPoint20), g,
                        reinterpret_cast<const uECC_word_t *>(rando20), curve); //multiply G with random number, save to rando

        jbyteArray newArray=env->NewByteArray(numOfBytesToGive);
        env->SetByteArrayRegion(newArray,0,numOfBytesToGive,randPoint20);
        return newArray;
    } else{
        uECC_generate_random_int(reinterpret_cast<uECC_word_t *>(rando2), curve->n, nativeNCount); //generate random number max is n of curve
        const uECC_word_t* g = uECC_curve_G(curve);

        uECC_point_mult(reinterpret_cast<uECC_word_t *>(randPoint2), g,
                        reinterpret_cast<const uECC_word_t *>(rando2), curve); //multiply G with random number, save to rando

        jbyteArray newArray=env->NewByteArray(numOfBytesToGive);
        env->SetByteArrayRegion(newArray,0,numOfBytesToGive,randPoint2);
        return newArray;
    }

}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_cz_vutbr_feec_watchwithmobile_EcOperations_randReturn2(JNIEnv *env, jobject /* this */, jint SecLevel) { //just a function to return random number to C, only call this after randPoint has been called
    int byteLenght=0;
    if (SecLevel==0)
    {
        byteLenght=20;
        jbyteArray newArray = env->NewByteArray(byteLenght);
        env->SetByteArrayRegion(newArray,0,byteLenght ,rando20);
        return newArray;
    }

    else if(SecLevel==2)
        byteLenght=32;
    else
        byteLenght=28;
    jbyteArray newArray = env->NewByteArray(byteLenght);
    env->SetByteArrayRegion(newArray,0,byteLenght ,rando2);
    return newArray;
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_cz_vutbr_feec_watchwithmobile_EcOperations_generateTk2(JNIEnv *env, jobject /* this */, jbyteArray JavaPointTv, jint SecLevel) { //just a function to return random number to C, only call this after randPoint has been called
    jbyteArray cPointTv= reinterpret_cast<jbyteArray>(env->GetByteArrayElements(JavaPointTv, NULL));
    jbyte* point1;
    if(SecLevel==0)
    {
        point1= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount20 * 2]());
        uECC_point_mult(reinterpret_cast<uECC_word_t *>(point1),
                        reinterpret_cast<const uECC_word_t *>(cPointTv),
                        reinterpret_cast<const uECC_word_t *>(rando20), curve);
    }
    else{

        point1= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
        uECC_point_mult(reinterpret_cast<uECC_word_t *>(point1),
                reinterpret_cast<const uECC_word_t *>(cPointTv),
                reinterpret_cast<const uECC_word_t *>(rando2), curve);

    }
    jbyteArray newArray = env->NewByteArray(numOfBytesToGive);
    env->SetByteArrayRegion(newArray,0,numOfBytesToGive ,point1);
    return newArray;

}
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_cz_vutbr_feec_watchwithmobile_EcOperations_generateTWithWatch2(JNIEnv *env, jobject /* this */, jbyteArray t1, int SecLevel) {
    jbyteArray ct1= reinterpret_cast<jbyteArray>(env->GetByteArrayElements(t1, NULL));
    jbyte* point1;
    if(SecLevel==0){
        point1= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount20 * 2]());
        uECC_point_add(reinterpret_cast<const uECC_word_t *>(ct1),
                       reinterpret_cast<const uECC_word_t *>(randPoint20),
                       reinterpret_cast<uECC_word_t *>(point1), curve);
    }
    else{
        point1= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
        uECC_point_add(reinterpret_cast<const uECC_word_t *>(ct1),
                       reinterpret_cast<const uECC_word_t *>(randPoint2),
                       reinterpret_cast<uECC_word_t *>(point1), curve);
    }
    jbyteArray newArray = env->NewByteArray(numOfBytesToGive); //size is numOfBytesToGive, it has been changed in signature Verification
    env->SetByteArrayRegion(newArray, 0,numOfBytesToGive, point1);//we release the array for Java to use
    return newArray;
}
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_cz_vutbr_feec_watchwithmobile_EcOperations_generateTkWithWatch2(JNIEnv *env, jobject /* this */, jbyteArray tk2, jbyteArray JavaPointTv, jint SecLevel) {
    jbyteArray cTk2= reinterpret_cast<jbyteArray>(env->GetByteArrayElements(tk2, NULL));
    jbyteArray cPointTv= reinterpret_cast<jbyteArray>(env->GetByteArrayElements(JavaPointTv, NULL));
    jbyte* tk;
    if(SecLevel==0)
    {
        jbyte* TvR= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount20 * 2]());
        uECC_point_mult(reinterpret_cast<uECC_word_t *>(TvR),
                        reinterpret_cast<const uECC_word_t *>(cPointTv),
                        reinterpret_cast<const uECC_word_t *>(rando20), curve);
        tk= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount20 * 2]());
        uECC_point_add(reinterpret_cast<const uECC_word_t *>(TvR),
                       reinterpret_cast<const uECC_word_t *>(cTk2), reinterpret_cast<uECC_word_t *>(tk), curve);
    }
    else{
        jbyte* TvR= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
        uECC_point_mult(reinterpret_cast<uECC_word_t *>(TvR),
                        reinterpret_cast<const uECC_word_t *>(cPointTv),
                        reinterpret_cast<const uECC_word_t *>(rando2), curve);
        tk= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
        uECC_point_add(reinterpret_cast<const uECC_word_t *>(TvR),
                       reinterpret_cast<const uECC_word_t *>(cTk2), reinterpret_cast<uECC_word_t *>(tk), curve);
    }


    jbyteArray newArray = env->NewByteArray(numOfBytesToGive); //size is numOfBytesToGive, it has been changed in signature Verification
    env->SetByteArrayRegion(newArray, 0,numOfBytesToGive, tk);//we release the array for Java to use

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

jbyte* point1= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
jbyte* point10= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount20 *2]());
jbyte* point2= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
jbyte* point20= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount20 *2]());
jbyte* cPub= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount20 *2]());
extern "C"
JNIEXPORT jbyteArray  JNICALL
Java_cz_vutbr_feec_watchwithmobile_EcOperations_verSignServer2(JNIEnv *env, jobject thiz, jbyteArray sv, jbyteArray pub, jbyteArray ev, jint SecLevel) {
    const uECC_word_t* g;
    jbyteArray cPub= reinterpret_cast<jbyteArray>(env->GetByteArrayElements(pub, NULL));
    jbyteArray cSv= reinterpret_cast<jbyteArray>(env->GetByteArrayElements(sv, NULL));
    jbyteArray cEv= reinterpret_cast<jbyteArray>(env->GetByteArrayElements(ev, NULL));
    if(SecLevel==1)
    {
        curve=uECC_secp224r1();
        g = uECC_curve_G(uECC_secp224r1());
        numOfBytesToGive=64;
    }

    else if(SecLevel==2)
    {
        curve=uECC_secp256k1();
        g = uECC_curve_G(uECC_secp256k1());
        numOfBytesToGive=64;
    } else
    {
        curve=uECC_secp160r1();
        g = uECC_curve_G(uECC_secp160r1());
        numOfBytesToGive=44;
        uECC_point_mult(reinterpret_cast<uECC_word_t *>(point10), g,
                        reinterpret_cast<const uECC_word_t *>(cSv), uECC_secp160r1());
        uECC_point_mult(reinterpret_cast<uECC_word_t *>(point20),
                        reinterpret_cast<const uECC_word_t *>(cPub),
                        reinterpret_cast<const uECC_word_t *>(cEv), uECC_secp160r1());
        jbyte* pointTv= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount20 * 2]());
        uECC_point_add(reinterpret_cast<const uECC_word_t *>(point10),
                       reinterpret_cast<const uECC_word_t *>(point20),
                       reinterpret_cast<uECC_word_t *>(pointTv), uECC_secp160r1());
        jbyteArray newArray=env->NewByteArray(numOfBytesToGive);
        env->SetByteArrayRegion(newArray,0,numOfBytesToGive,pointTv);
        return newArray;
    }

    uECC_point_mult(reinterpret_cast<uECC_word_t *>(point1), g,
                    reinterpret_cast<const uECC_word_t *>(cSv), curve);
    //jbyte* point2= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
    uECC_point_mult(reinterpret_cast<uECC_word_t *>(point2),
                    reinterpret_cast<const uECC_word_t *>(cPub),
                    reinterpret_cast<const uECC_word_t *>(cEv), curve);
    //jbyte* point3= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
    /*uECC_point_add(reinterpret_cast<const uECC_word_t *>(point1),
                   reinterpret_cast<const uECC_word_t *>(point2),
                   reinterpret_cast<uECC_word_t *>(tv), curve);*/
    jbyte* pointTv= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
    uECC_point_add(reinterpret_cast<const uECC_word_t *>(point1),
                   reinterpret_cast<const uECC_word_t *>(point2),
                   reinterpret_cast<uECC_word_t *>(pointTv), curve);
    //pointTv=point3;

    jbyteArray newArray=env->NewByteArray(numOfBytesToGive);
    env->SetByteArrayRegion(newArray,0,numOfBytesToGive,pointTv);
    //env->SetByteArrayRegion(newArray,64,128,point1);
    //env->SetByteArrayRegion(newArray,128,192,point2);
    return newArray;
}
extern "C"
JNIEXPORT jbyteArray  JNICALL
Java_cz_vutbr_feec_watchwithmobile_EcOperations_getPt1(JNIEnv *env, jobject thiz) {
    jbyteArray newArray=env->NewByteArray(44);
    env->SetByteArrayRegion(newArray,0,44,point10);

    return newArray;
}
extern "C"
JNIEXPORT jbyteArray  JNICALL
Java_cz_vutbr_feec_watchwithmobile_EcOperations_getPt2(JNIEnv *env, jobject thiz) {
    jbyteArray newArray=env->NewByteArray(44);
    env->SetByteArrayRegion(newArray,0,44,point20);

    return newArray;
}

extern "C"
JNIEXPORT jbyteArray  JNICALL
Java_cz_vutbr_feec_watchwithmobile_EccOperations_getsvg(JNIEnv *env, jobject thiz,jbyteArray sv, jint SecLevel) {
    if (SecLevel == 1)
        curve = uECC_secp224r1();
    else if (SecLevel == 2)
        curve = uECC_secp256k1();
    jbyteArray cSv= reinterpret_cast<jbyteArray>(env->GetByteArrayElements(sv, NULL));
    jbyte* point1= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
    const uECC_word_t* g = uECC_curve_G(curve);
    uECC_point_mult(reinterpret_cast<uECC_word_t *>(point1), g,
                    reinterpret_cast<const uECC_word_t *>(cSv), curve);
    jbyteArray newArray=env->NewByteArray(64);
    env->SetByteArrayRegion(newArray,0,64,point1);
    return newArray;
}
extern "C"
JNIEXPORT jbyteArray  JNICALL
Java_cz_vutbr_feec_watchwithmobile_EccOperations_getCPUBSV(JNIEnv *env, jobject thiz,jbyteArray pub,jbyteArray ev, jint SecLevel) {
    if (SecLevel == 1)
        curve = uECC_secp224r1();
    else if (SecLevel == 2)
        curve = uECC_secp256k1();

    jbyteArray cPub= reinterpret_cast<jbyteArray>(env->GetByteArrayElements(pub, NULL));
    jbyteArray cEv= reinterpret_cast<jbyteArray>(env->GetByteArrayElements(ev, NULL));
    jbyte* point2= reinterpret_cast<jbyte *>(new uECC_word_t[nativeNCount * 2]());
    uECC_point_mult(reinterpret_cast<uECC_word_t *>(point2),
                    reinterpret_cast<const uECC_word_t *>(cPub),
                    reinterpret_cast<const uECC_word_t *>(cEv), curve);
    jbyteArray newArray=env->NewByteArray(64);
    env->SetByteArrayRegion(newArray,0,64,point2);
    return newArray;
}


