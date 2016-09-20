/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class sparse_PrismSparse */

#ifndef _Included_sparse_PrismSparse
#define _Included_sparse_PrismSparse
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     sparse_PrismSparse
 * Method:    PS_FreeGlobalRefs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1FreeGlobalRefs
  (JNIEnv *, jclass);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_SetCUDDManager
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetCUDDManager
  (JNIEnv *, jclass, jlong);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_SetMainLog
 * Signature: (Lprism/PrismLog;)V
 */
JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetMainLog
  (JNIEnv *, jclass, jobject);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_SetTechLog
 * Signature: (Lprism/PrismLog;)V
 */
JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetTechLog
  (JNIEnv *, jclass, jobject);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_SetExportIterations
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_sparse_PrismSparse_PS_1SetExportIterations
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_GetErrorMessage
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_sparse_PrismSparse_PS_1GetErrorMessage
  (JNIEnv *, jclass);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_ProbBoundedUntil
 * Signature: (JJJIJIJJI)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1ProbBoundedUntil
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jlong, jint, jlong, jlong, jint);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_ProbUntil
 * Signature: (JJJIJIJJ)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1ProbUntil
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jlong, jint, jlong, jlong);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_ProbCumulReward
 * Signature: (JJJJJIJII)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1ProbCumulReward
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jlong, jint, jlong, jint, jint);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_ProbInstReward
 * Signature: (JJJJIJII)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1ProbInstReward
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint, jint);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_ProbReachReward
 * Signature: (JJJJJIJIJJJ)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1ProbReachReward
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jlong, jint, jlong, jint, jlong, jlong, jlong);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_ProbTransient
 * Signature: (JJJJIJII)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1ProbTransient
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint, jint);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_NondetBoundedUntil
 * Signature: (JJJIJIJIJJIZ)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1NondetBoundedUntil
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jlong, jlong, jint, jboolean);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_NondetUntil
 * Signature: (JJLjava/util/List;JJIJIJIJJZJ)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1NondetUntil
  (JNIEnv *, jclass, jlong, jlong, jobject, jlong, jlong, jint, jlong, jint, jlong, jint, jlong, jlong, jboolean, jlong);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_NondetCumulReward
 * Signature: (JJJJJIJIJIIZ)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1NondetCumulReward
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jint, jboolean);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_NondetInstReward
 * Signature: (JJJJIJIJIIZJ)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1NondetInstReward
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jint, jboolean, jlong);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_NondetReachReward
 * Signature: (JJLjava/util/List;JJJJIJIJIJJJZ)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1NondetReachReward
  (JNIEnv *, jclass, jlong, jlong, jobject, jlong, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jlong, jlong, jlong, jboolean);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_NondetProbQuantile
 * Signature: (JJJJIJIJIJJJJJJJJLjava/lang/String;[DZZZ)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1NondetProbQuantile
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint, jlong, jint, jlong, jlong, jlong, jlong, jlong, jlong, jlong, jlong, jstring, jdoubleArray, jboolean, jboolean, jboolean);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_NondetMultiObj
 * Signature: (JJIJIJIZJJJLjava/util/List;[J[I[J[D[I)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_sparse_PrismSparse_PS_1NondetMultiObj
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jlong, jint, jboolean, jlong, jlong, jlong, jobject, jlongArray, jintArray, jlongArray, jdoubleArray, jintArray);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_NondetMultiObjGS
 * Signature: (JJIJIJIZJJJ[J[J[D)[D
 */
JNIEXPORT jdoubleArray JNICALL Java_sparse_PrismSparse_PS_1NondetMultiObjGS
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jlong, jint, jboolean, jlong, jlong, jlong, jlongArray, jlongArray, jdoubleArray);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_NondetMultiReach
 * Signature: (JJLjava/util/List;JJIJIJI[J[I[DJJ)D
 */
JNIEXPORT jdouble JNICALL Java_sparse_PrismSparse_PS_1NondetMultiReach
  (JNIEnv *, jclass, jlong, jlong, jobject, jlong, jlong, jint, jlong, jint, jlong, jint, jlongArray, jintArray, jdoubleArray, jlong, jlong);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_NondetMultiReach1
 * Signature: (JJLjava/util/List;JJIJIJI[J[J[I[I[DJJ)D
 */
JNIEXPORT jdouble JNICALL Java_sparse_PrismSparse_PS_1NondetMultiReach1
  (JNIEnv *, jclass, jlong, jlong, jobject, jlong, jlong, jint, jlong, jint, jlong, jint, jlongArray, jlongArray, jintArray, jintArray, jdoubleArray, jlong, jlong);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_NondetMultiReachReward
 * Signature: (JJLjava/util/List;JJIJIJI[J[I[D[I[DJJ[JJ)D
 */
JNIEXPORT jdouble JNICALL Java_sparse_PrismSparse_PS_1NondetMultiReachReward
  (JNIEnv *, jclass, jlong, jlong, jobject, jlong, jlong, jint, jlong, jint, jlong, jint, jlongArray, jintArray, jdoubleArray, jintArray, jdoubleArray, jlong, jlong, jlongArray, jlong);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_NondetMultiReachReward1
 * Signature: (JJLjava/util/List;JJIJIJI[J[J[I[I[D[I[DJJ[JJ)D
 */
JNIEXPORT jdouble JNICALL Java_sparse_PrismSparse_PS_1NondetMultiReachReward1
  (JNIEnv *, jclass, jlong, jlong, jobject, jlong, jlong, jint, jlong, jint, jlong, jint, jlongArray, jlongArray, jintArray, jintArray, jdoubleArray, jintArray, jdoubleArray, jlong, jlong, jlongArray, jlong);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_StochBoundedUntil
 * Signature: (JJJIJIJJDJ)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1StochBoundedUntil
  (JNIEnv *, jclass, jlong, jlong, jlong, jint, jlong, jint, jlong, jlong, jdouble, jlong);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_StochCumulReward
 * Signature: (JJJJJIJID)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1StochCumulReward
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jlong, jint, jlong, jint, jdouble);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_StochSteadyState
 * Signature: (JJJJIJI)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1StochSteadyState
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_StochTransient
 * Signature: (JJJJIJID)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1StochTransient
  (JNIEnv *, jclass, jlong, jlong, jlong, jlong, jint, jlong, jint, jdouble);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_ExportMatrix
 * Signature: (JLjava/lang/String;JIJIJILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_sparse_PrismSparse_PS_1ExportMatrix
  (JNIEnv *, jclass, jlong, jstring, jlong, jint, jlong, jint, jlong, jint, jstring);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_ExportMDP
 * Signature: (JJLjava/util/List;Ljava/lang/String;JIJIJIJILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_sparse_PrismSparse_PS_1ExportMDP
  (JNIEnv *, jclass, jlong, jlong, jobject, jstring, jlong, jint, jlong, jint, jlong, jint, jlong, jint, jstring);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_ExportSubMDP
 * Signature: (JJLjava/lang/String;JIJIJIJILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_sparse_PrismSparse_PS_1ExportSubMDP
  (JNIEnv *, jclass, jlong, jlong, jstring, jlong, jint, jlong, jint, jlong, jint, jlong, jint, jstring);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_Power
 * Signature: (JJIJIJJJZ)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1Power
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jlong, jlong, jlong, jboolean);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_JOR
 * Signature: (JJIJIJJJZZD)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1JOR
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jlong, jlong, jlong, jboolean, jboolean, jdouble);

/*
 * Class:     sparse_PrismSparse
 * Method:    PS_SOR
 * Signature: (JJIJIJJJZZDZ)J
 */
JNIEXPORT jlong JNICALL Java_sparse_PrismSparse_PS_1SOR
  (JNIEnv *, jclass, jlong, jlong, jint, jlong, jint, jlong, jlong, jlong, jboolean, jboolean, jdouble, jboolean);

#ifdef __cplusplus
}
#endif
#endif
