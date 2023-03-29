#include "com_computerenhance_sim86_Decoder.h"
#include "sim86_shared.h"
#include <string.h>
#include <stdio.h>
#include <stdlib.h>

/*
 * Class:     com_computerenhance_sim86_Decoder
 * Method:    version
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_computerenhance_sim86_Decoder_version
(JNIEnv *env, jclass cls)
{
  return Sim86_GetVersion();
}

void fail(char *s)
{
  printf("%s\n", s);
  exit(1);
}

#define check(x) if (!x) fail("no " #x)

/*
 * Class:     com_computerenhance_sim86_Decoder
 * Method:    decode
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_computerenhance_sim86_Decoder_decode
(JNIEnv *env, jobject obj)
{
  jclass objClass = (*env)->GetObjectClass(env, obj);
  check(objClass);

  jfieldID outBufferField = (*env)->GetFieldID(env, objClass, "outBuffer", "[B");
  check(outBufferField);
  jobject outBuffer = (*env)->GetObjectField(env, obj, outBufferField);
  check(outBuffer);
  jbyte *outArray = (*env)->GetByteArrayElements(env, outBuffer, 0);
  check(outArray);

  jfieldID inField = (*env)->GetFieldID(env, objClass, "in", "Ljava/nio/ByteBuffer;");
  check(inField);
  jobject in = (*env)->GetObjectField(env, obj, inField);
  check(in);

  jclass byteBufferClass = (*env)->GetObjectClass(env, in);
  check(byteBufferClass);
  jmethodID arrayMethod = (*env)->GetMethodID(env, byteBufferClass, "array", "()[B");
  check(arrayMethod);
  jmethodID setPositionMethod = (*env)->GetMethodID(env, byteBufferClass,
						    "position", "(I)Ljava/nio/ByteBuffer;");
  check(setPositionMethod);
  jmethodID positionMethod = (*env)->GetMethodID(env, byteBufferClass, "position", "()I");
  check(positionMethod);
  jmethodID limitMethod = (*env)->GetMethodID(env, byteBufferClass, "limit", "()I");
  check(limitMethod);

  jobject inBuffer = (*env)->CallObjectMethod(env, in, arrayMethod);
  jint position = (*env)->CallIntMethod(env, in, positionMethod);
  jint limit = (*env)->CallIntMethod(env, in, limitMethod);
  jbyte *inArray = (*env)->GetByteArrayElements(env, inBuffer, 0);

  instruction *instr = (instruction*)outArray;

  Sim86_Decode8086Instruction(limit - position, (void*) inArray + position, instr);

  (*env)->CallObjectMethod(env, in, setPositionMethod, position + instr->Size);

  (*env)->ReleaseByteArrayElements(env, inBuffer, inArray, 0);
  (*env)->ReleaseByteArrayElements(env, outBuffer, outArray, 0);
}

int writeStringToOutBuffer(JNIEnv *env, jobject obj, char const *s)
{
  jclass jclass = (*env)->GetObjectClass(env, obj);
  jfieldID outBufferField = (*env)->GetFieldID(env, jclass, "outBuffer", "[B");
  jobject outBuffer = (*env)->GetObjectField(env, obj, outBufferField);
  jbyte *buffer = (*env)->GetByteArrayElements(env, outBuffer, 0);

  int l = strlen(s);
  memcpy(buffer, s, l);

  (*env)->ReleaseByteArrayElements(env, outBuffer, buffer, 0);

  return l;
}

/*
 * Class:     com_computerenhance_sim86_Decoder
 * Method:    registerName
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_com_computerenhance_sim86_Decoder_registerName
(JNIEnv *env, jobject obj, jint index, jint offset, jint count)
{
  struct register_access access;
  access.Index = index;
  access.Offset = offset;
  access.Count = count;
  char const *s = Sim86_RegisterNameFromOperand(&access);
  return writeStringToOutBuffer(env, obj, s);
}

/*
 * Class:     com_computerenhance_sim86_Decoder
 * Method:    mnemonic
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_computerenhance_sim86_Decoder_mnemonic
(JNIEnv *env, jobject obj, jint operation)
{
  char const *s = Sim86_MnemonicFromOperationType(operation);
  return writeStringToOutBuffer(env, obj, s);
}

