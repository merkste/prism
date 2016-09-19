/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class hybrid_PrismHybrid */

#ifndef _Included_hybrid_PrismHybrid
#define _Included_hybrid_PrismHybrid
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_FreeGlobalRefs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_hybrid_PrismHybrid_PH_1FreeGlobalRefs
  (JNIEnv *, jclass);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_SetCUDDManager
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_hybrid_PrismHybrid_PH_1SetCUDDManager
  (JNIEnv *, jclass, jlong);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_SetMainLog
 * Signature: (Lprism/PrismLog;)V
 */
JNIEXPORT void JNICALL Java_hybrid_PrismHybrid_PH_1SetMainLog
  (JNIEnv *, jclass, jobject);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_SetTechLog
 * Signature: (Lprism/PrismLog;)V
 */
JNIEXPORT void JNICALL Java_hybrid_PrismHybrid_PH_1SetTechLog
  (JNIEnv *, jclass, jobject);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_SetExportIterations
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_hybrid_PrismHybrid_PH_1SetExportIterations
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_GetErrorMessage
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_hybrid_PrismHybrid_PH_1GetErrorMessage
  (JNIEnv *, jclass);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_GetLastUnif
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_hybrid_PrismHybrid_PH_1GetLastUnif
  (JNIEnv *, jclass);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_ProbBoundedUntil
 * Signature: (JJJIJIJJI)J
 */
JNIEXPORT jlong JNICALL Java_hybrid_PrismHybrid_PH_1ProbBoundedUntil
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jlong, jint, jlong, jlong, jint);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_ProbUntil
 * Signature: (JJJIJIJJ)J
 */
JNIEXPORT jlong JNICALL Java_hybrid_PrismHybrid_PH_1ProbUntil
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jlong, jint, jlong, jlong);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_ProbCumulReward
 * Signature: (JJJJJIJII)J
 */
JNIEXPORT jlong JNICALL Java_hybrid_PrismHybrid_PH_1ProbCumulReward
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jlong, jint, jlong, jint, jint);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_ProbInstReward
 * Signature: (JJJJIJII)J
 */
JNIEXPORT jlong JNICALL Java_hybrid_PrismHybrid_PH_1ProbInstReward
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint, jint);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_ProbReachReward
 * Signature: (JJJJJIJIJJJ)J
 */
JNIEXPORT jlong JNICALL Java_hybrid_PrismHybrid_PH_1ProbReachReward
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jlong, jint, jlong, jint, jlong, jlong, jlong);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_ProbTransient
 * Signature: (JJJJIJII)J
 */
JNIEXPORT jlong JNICALL Java_hybrid_PrismHybrid_PH_1ProbTransient
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint, jint);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_NondetBoundedUntil
 * Signature: (JJJIJIJIJJIZ)J
 */
JNIEXPORT jlong JNICALL Java_hybrid_PrismHybrid_PH_1NondetBoundedUntil
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jlong, jlong, jint, jboolean);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_NondetUntil
 * Signature: (JJJIJIJIJJZ)J
 */
JNIEXPORT jlong JNICALL Java_hybrid_PrismHybrid_PH_1NondetUntil
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jlong, jlong, jboolean);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_NondetReachReward
 * Signature: (JJJJJIJIJIJJJZ)J
 */
JNIEXPORT jlong JNICALL Java_hybrid_PrismHybrid_PH_1NondetReachReward
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jlong, jlong, jlong, jboolean);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_NondetProbQuantile
 * Signature: (JJJJIJIJIJJJJJJJJJLjava/lang/String;DZZ)J
 */
JNIEXPORT jlong JNICALL Java_hybrid_PrismHybrid_PH_1NondetProbQuantile
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jlong, jlong, jlong, jlong, jlong, jlong, jlong, jlong, jlong, jstring, jdouble, jboolean, jboolean);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_StochBoundedUntil
 * Signature: (JJJIJIJJDJ)J
 */
JNIEXPORT jlong JNICALL Java_hybrid_PrismHybrid_PH_1StochBoundedUntil
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jlong, jint, jlong, jlong, jdouble, jlong);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_StochCumulReward
 * Signature: (JJJJJIJID)J
 */
JNIEXPORT jlong JNICALL Java_hybrid_PrismHybrid_PH_1StochCumulReward
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jlong, jint, jlong, jint, jdouble);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_StochSteadyState
 * Signature: (JJJJIJI)J
 */
JNIEXPORT jlong JNICALL Java_hybrid_PrismHybrid_PH_1StochSteadyState
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_StochTransient
 * Signature: (JJJJIJID)J
 */
JNIEXPORT jlong JNICALL Java_hybrid_PrismHybrid_PH_1StochTransient
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint, jdouble);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_Power
 * Signature: (JJIJIJJJZ)J
 */
JNIEXPORT jlong JNICALL Java_hybrid_PrismHybrid_PH_1Power
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jlong, jlong, jlong, jboolean);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_JOR
 * Signature: (JJIJIJJJZZD)J
 */
JNIEXPORT jlong JNICALL Java_hybrid_PrismHybrid_PH_1JOR
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jlong, jlong, jlong, jboolean, jboolean, jdouble);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_SOR
 * Signature: (JJIJIJJJZZDZ)J
 */
JNIEXPORT jlong JNICALL Java_hybrid_PrismHybrid_PH_1SOR
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jlong, jlong, jlong, jboolean, jboolean, jdouble, jboolean);

/*
 * Class:     hybrid_PrismHybrid
 * Method:    PH_PSOR
 * Signature: (JJIJIJJJZZDZ)J
 */
JNIEXPORT jlong JNICALL Java_hybrid_PrismHybrid_PH_1PSOR
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jlong, jlong, jlong, jboolean, jboolean, jdouble, jboolean);

#ifdef __cplusplus
}
#endif
#endif
