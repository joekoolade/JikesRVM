/*
 *  This file is part of the Jikes RVM project (http://jikesrvm.org).
 *
 *  This file is licensed to You under the Eclipse Public License (EPL);
 *  You may not use this file except in compliance with the License. You
 *  may obtain a copy of the License at
 *
 *      http://www.opensource.org/licenses/eclipse-1.0.php
 *
 *  See the COPYRIGHT.txt file distributed with this work for information
 *  regarding copyright ownership.
 */
package org.jikesrvm.runtime;

import org.jikesrvm.classloader.Atom;

/**
 * Method names that are treated specially by compiler.
 * <p>
 * See also: Magic, various magic compilers.
 */
public class MagicNames {

  public static final Atom invokeClassInitializer = Atom.findOrCreateAsciiAtom("invokeClassInitializer");
  public static final Atom invokeMethodReturningVoid = Atom.findOrCreateAsciiAtom("invokeMethodReturningVoid");
  public static final Atom invokeMethodReturningInt = Atom.findOrCreateAsciiAtom("invokeMethodReturningInt");
  public static final Atom invokeMethodReturningLong = Atom.findOrCreateAsciiAtom("invokeMethodReturningLong");
  public static final Atom invokeMethodReturningFloat = Atom.findOrCreateAsciiAtom("invokeMethodReturningFloat");
  public static final Atom invokeMethodReturningDouble =
      Atom.findOrCreateAsciiAtom("invokeMethodReturningDouble");
  public static final Atom invokeMethodReturningObject =
      Atom.findOrCreateAsciiAtom("invokeMethodReturningObject");

  public static final Atom getFramePointer = Atom.findOrCreateAsciiAtom("getFramePointer");
  public static final Atom getTocPointer = Atom.findOrCreateAsciiAtom("getTocPointer");
  public static final Atom getJTOC = Atom.findOrCreateAsciiAtom("getJTOC");
  public static final Atom getThreadRegister = Atom.findOrCreateAsciiAtom("getThreadRegister");
  public static final Atom setThreadRegister = Atom.findOrCreateAsciiAtom("setThreadRegister");
  /* IA-specific */
  public static final Atom getESIAsThread = Atom.findOrCreateAsciiAtom("getESIAsThread");
  public static final Atom setESIAsThread = Atom.findOrCreateAsciiAtom("setESIAsThread");

  public static final Atom getTimeBase = Atom.findOrCreateAsciiAtom("getTimeBase");

  public static final Atom getCallerFramePointer = Atom.findOrCreateAsciiAtom("getCallerFramePointer");
  public static final Atom setCallerFramePointer = Atom.findOrCreateAsciiAtom("setCallerFramePointer");
  public static final Atom getCompiledMethodID = Atom.findOrCreateAsciiAtom("getCompiledMethodID");
  public static final Atom setCompiledMethodID = Atom.findOrCreateAsciiAtom("setCompiledMethodID");
  public static final Atom getNextInstructionAddress = Atom.findOrCreateAsciiAtom("getNextInstructionAddress");
  public static final Atom getReturnAddressLocation = Atom.findOrCreateAsciiAtom("getReturnAddressLocation");

  public static final Atom loadPrefix = Atom.findOrCreateAsciiAtom("load");
  public static final Atom loadByte = Atom.findOrCreateAsciiAtom("loadByte");
  public static final Atom loadChar = Atom.findOrCreateAsciiAtom("loadChar");
  public static final Atom loadInt = Atom.findOrCreateAsciiAtom("loadInt");
  public static final Atom loadWord = Atom.findOrCreateAsciiAtom("loadWord");
  public static final Atom loadObject = Atom.findOrCreateAsciiAtom("loadObject");
  public static final Atom loadAddress = Atom.findOrCreateAsciiAtom("loadAddress");
  public static final Atom loadShort = Atom.findOrCreateAsciiAtom("loadShort");
  public static final Atom loadFloat = Atom.findOrCreateAsciiAtom("loadFloat");
  public static final Atom loadLong = Atom.findOrCreateAsciiAtom("loadLong");
  public static final Atom loadDouble = Atom.findOrCreateAsciiAtom("loadDouble");
  public static final Atom loadObjectReference = Atom.findOrCreateAsciiAtom("loadObjectReference");
  public static final Atom store = Atom.findOrCreateAsciiAtom("store");
  public static final Atom pause = Atom.findOrCreateAsciiAtom("pause");
  public static final Atom sqrt  = Atom.findOrCreateAsciiAtom("sqrt");

  public static final Atom getInlineDepth = Atom.findOrCreateAsciiAtom("getInlineDepth");
  public static final Atom getCompilerLevel = Atom.findOrCreateAsciiAtom("getCompilerLevel");
  public static final Atom isConstantParameter = Atom.findOrCreateAsciiAtom("isConstantParameter");
  public static final Atom getFrameSize = Atom.findOrCreateAsciiAtom("getFrameSize");

  public static final Atom getUnsignedByteAtOffset = Atom.findOrCreateAsciiAtom("getUnsignedByteAtOffset");
  public static final Atom getByteAtOffset = Atom.findOrCreateAsciiAtom("getByteAtOffset");
  public static final Atom getCharAtOffset = Atom.findOrCreateAsciiAtom("getCharAtOffset");
  public static final Atom getShortAtOffset = Atom.findOrCreateAsciiAtom("getShortAtOffset");
  public static final Atom getIntAtOffset = Atom.findOrCreateAsciiAtom("getIntAtOffset");
  public static final Atom getLongAtOffset = Atom.findOrCreateAsciiAtom("getLongAtOffset");
  public static final Atom getFloatAtOffset = Atom.findOrCreateAsciiAtom("getFloatAtOffset");
  public static final Atom getDoubleAtOffset = Atom.findOrCreateAsciiAtom("getDoubleAtOffset");
  public static final Atom getWordAtOffset = Atom.findOrCreateAsciiAtom("getWordAtOffset");
  public static final Atom getAddressAtOffset = Atom.findOrCreateAsciiAtom("getAddressAtOffset");
  public static final Atom getExtentAtOffset = Atom.findOrCreateAsciiAtom("getExtentAtOffset");
  public static final Atom getOffsetAtOffset = Atom.findOrCreateAsciiAtom("getOffsetAtOffset");
  public static final Atom getObjectAtOffset = Atom.findOrCreateAsciiAtom("getObjectAtOffset");
  public static final Atom getTIBAtOffset = Atom.findOrCreateAsciiAtom("getTIBAtOffset");
  public static final Atom setBooleanAtOffset = Atom.findOrCreateAsciiAtom("setBooleanAtOffset");
  public static final Atom setByteAtOffset = Atom.findOrCreateAsciiAtom("setByteAtOffset");
  public static final Atom setCharAtOffset = Atom.findOrCreateAsciiAtom("setCharAtOffset");
  public static final Atom setShortAtOffset = Atom.findOrCreateAsciiAtom("setShortAtOffset");
  public static final Atom setIntAtOffset = Atom.findOrCreateAsciiAtom("setIntAtOffset");
  public static final Atom setLongAtOffset = Atom.findOrCreateAsciiAtom("setLongAtOffset");
  public static final Atom setFloatAtOffset = Atom.findOrCreateAsciiAtom("setFloatAtOffset");
  public static final Atom setDoubleAtOffset = Atom.findOrCreateAsciiAtom("setDoubleAtOffset");
  public static final Atom setWordAtOffset = Atom.findOrCreateAsciiAtom("setWordAtOffset");
  public static final Atom setAddressAtOffset = Atom.findOrCreateAsciiAtom("setAddressAtOffset");
  public static final Atom setOffsetAtOffset = Atom.findOrCreateAsciiAtom("setOffsetAtOffset");
  public static final Atom setExtentAtOffset = Atom.findOrCreateAsciiAtom("setExtentAtOffset");
  public static final Atom setObjectAtOffset = Atom.findOrCreateAsciiAtom("setObjectAtOffset");

  public static final Atom getMemoryInt = Atom.findOrCreateAsciiAtom("getMemoryInt");
  public static final Atom setMemoryInt = Atom.findOrCreateAsciiAtom("setMemoryInt");
  public static final Atom getMemoryWord = Atom.findOrCreateAsciiAtom("getMemoryWord");
  public static final Atom setMemoryWord = Atom.findOrCreateAsciiAtom("setMemoryWord");
  public static final Atom getMemoryAddress = Atom.findOrCreateAsciiAtom("getMemoryAddress");

  public static final Atom preparePrefix = Atom.findOrCreateAsciiAtom("prepare");
  public static final Atom prepareInt = Atom.findOrCreateAsciiAtom("prepareInt");
  public static final Atom prepareObject = Atom.findOrCreateAsciiAtom("prepareObject");
  public static final Atom prepareObjectReference = Atom.findOrCreateAsciiAtom("prepareObjectReference");
  public static final Atom prepareAddress = Atom.findOrCreateAsciiAtom("prepareAddress");
  public static final Atom prepareWord = Atom.findOrCreateAsciiAtom("prepareWord");
  public static final Atom prepareLong = Atom.findOrCreateAsciiAtom("prepareLong");
  public static final Atom attempt = Atom.findOrCreateAsciiAtom("attempt");
  public static final Atom attemptInt = Atom.findOrCreateAsciiAtom("attemptInt");
  public static final Atom attemptObject = Atom.findOrCreateAsciiAtom("attemptObject");
  public static final Atom attemptObjectReference = Atom.findOrCreateAsciiAtom("attemptObjectReference");
  public static final Atom attemptAddress = Atom.findOrCreateAsciiAtom("attemptAddress");
  public static final Atom attemptWord = Atom.findOrCreateAsciiAtom("attemptWord");
  public static final Atom attemptLong = Atom.findOrCreateAsciiAtom("attemptLong");

  public static final Atom saveThreadState = Atom.findOrCreateAsciiAtom("saveThreadState");
  public static final Atom threadSwitch = Atom.findOrCreateAsciiAtom("threadSwitch");
  public static final Atom restoreHardwareExceptionState =
      Atom.findOrCreateAsciiAtom("restoreHardwareExceptionState");
  public static final Atom returnToNewStack = Atom.findOrCreateAsciiAtom("returnToNewStack");
  public static final Atom dynamicBridgeTo = Atom.findOrCreateAsciiAtom("dynamicBridgeTo");

  public static final Atom objectAsAddress = Atom.findOrCreateAsciiAtom("objectAsAddress");
  public static final Atom addressAsObject = Atom.findOrCreateAsciiAtom("addressAsObject");
  public static final Atom addressAsTIB = Atom.findOrCreateAsciiAtom("addressAsTIB");
  public static final Atom objectAsType = Atom.findOrCreateAsciiAtom("objectAsType");
  public static final Atom addressAsByteArray = Atom.findOrCreateAsciiAtom("addressAsByteArray");
  public static final Atom objectAsShortArray = Atom.findOrCreateAsciiAtom("objectAsShortArray");
  public static final Atom objectAsIntArray = Atom.findOrCreateAsciiAtom("objectAsIntArray");
  public static final Atom codeArrayAsObject = Atom.findOrCreateAsciiAtom("codeArrayAsObject");
  public static final Atom tibAsObject = Atom.findOrCreateAsciiAtom("tibAsObject");

  public static final Atom objectAsThread = Atom.findOrCreateAsciiAtom("objectAsThread");
  public static final Atom floatAsIntBits = Atom.findOrCreateAsciiAtom("floatAsIntBits");
  public static final Atom intBitsAsFloat = Atom.findOrCreateAsciiAtom("intBitsAsFloat");
  public static final Atom doubleAsLongBits = Atom.findOrCreateAsciiAtom("doubleAsLongBits");
  public static final Atom longBitsAsDouble = Atom.findOrCreateAsciiAtom("longBitsAsDouble");

  public static final Atom getObjectType = Atom.findOrCreateAsciiAtom("getObjectType");
  public static final Atom getArrayLength = Atom.findOrCreateAsciiAtom("getArrayLength");

  public static final Atom illegalInstruction = Atom.findOrCreateAsciiAtom("illegalInstruction");
  public static final Atom fence = Atom.findOrCreateAsciiAtom("fence");
  public static final Atom combinedLoadBarrier = Atom.findOrCreateAsciiAtom("combinedLoadBarrier");
  public static final Atom storeStoreBarrier = Atom.findOrCreateAsciiAtom("storeStoreBarrier");
  public static final Atom synchronizeInstructionCache = Atom.findOrCreateAsciiAtom("synchronizeInstructionCache");
  public static final Atom prefetch = Atom.findOrCreateAsciiAtom("prefetch");
  /* IA-specific */
  public static final Atom prefetchNTA = Atom.findOrCreateAsciiAtom("prefetchNTA");
  /* PowerPC-specific */
  public static final Atom dcbst = Atom.findOrCreateAsciiAtom("dcbst");
  public static final Atom dcbt = Atom.findOrCreateAsciiAtom("dcbt");
  public static final Atom dcbtst = Atom.findOrCreateAsciiAtom("dcbtst");
  public static final Atom dcbz = Atom.findOrCreateAsciiAtom("dcbz");
  public static final Atom dcbzl = Atom.findOrCreateAsciiAtom("dcbzl");
  public static final Atom icbi = Atom.findOrCreateAsciiAtom("icbi");

  // atoms related to Word, Address, Offset, Extent
  public static final Atom wordFromObject = Atom.findOrCreateAsciiAtom("fromObject");
  public static final Atom wordFromInt = Atom.findOrCreateAsciiAtom("fromInt");
  public static final Atom wordFromIntZeroExtend = Atom.findOrCreateAsciiAtom("fromIntZeroExtend");
  public static final Atom wordFromIntSignExtend = Atom.findOrCreateAsciiAtom("fromIntSignExtend");
  public static final Atom wordFromLong = Atom.findOrCreateAsciiAtom("fromLong");
  public static final Atom wordToObject = Atom.findOrCreateAsciiAtom("toObject");
  public static final Atom wordToObjectReference = Atom.findOrCreateAsciiAtom("toObjectReference");
  public static final Atom wordToInt = Atom.findOrCreateAsciiAtom("toInt");
  public static final Atom wordToLong = Atom.findOrCreateAsciiAtom("toLong");
  public static final Atom wordToWord = Atom.findOrCreateAsciiAtom("toWord");
  public static final Atom wordToAddress = Atom.findOrCreateAsciiAtom("toAddress");
  public static final Atom wordToOffset = Atom.findOrCreateAsciiAtom("toOffset");
  public static final Atom wordToExtent = Atom.findOrCreateAsciiAtom("toExtent");
  public static final Atom wordPlus = Atom.findOrCreateAsciiAtom("plus");
  public static final Atom wordMinus = Atom.findOrCreateAsciiAtom("minus");
  public static final Atom wordDiff = Atom.findOrCreateAsciiAtom("diff");
  public static final Atom wordEQ = Atom.findOrCreateAsciiAtom("EQ");
  public static final Atom wordNE = Atom.findOrCreateAsciiAtom("NE");
  public static final Atom wordLT = Atom.findOrCreateAsciiAtom("LT");
  public static final Atom wordLE = Atom.findOrCreateAsciiAtom("LE");
  public static final Atom wordGT = Atom.findOrCreateAsciiAtom("GT");
  public static final Atom wordGE = Atom.findOrCreateAsciiAtom("GE");
  public static final Atom wordsLT = Atom.findOrCreateAsciiAtom("sLT");
  public static final Atom wordsLE = Atom.findOrCreateAsciiAtom("sLE");
  public static final Atom wordsGT = Atom.findOrCreateAsciiAtom("sGT");
  public static final Atom wordsGE = Atom.findOrCreateAsciiAtom("sGE");
  public static final Atom wordZero = Atom.findOrCreateAsciiAtom("zero");
  public static final Atom wordNull = Atom.findOrCreateAsciiAtom("nullReference");
  public static final Atom wordOne = Atom.findOrCreateAsciiAtom("one");
  public static final Atom wordMax = Atom.findOrCreateAsciiAtom("max");
  public static final Atom wordIsNull = Atom.findOrCreateAsciiAtom("isNull");
  public static final Atom wordIsZero = Atom.findOrCreateAsciiAtom("isZero");
  public static final Atom wordIsMax = Atom.findOrCreateAsciiAtom("isMax");
  public static final Atom wordAnd = Atom.findOrCreateAsciiAtom("and");
  public static final Atom wordOr = Atom.findOrCreateAsciiAtom("or");
  public static final Atom wordNot = Atom.findOrCreateAsciiAtom("not");
  public static final Atom wordXor = Atom.findOrCreateAsciiAtom("xor");
  public static final Atom wordLsh = Atom.findOrCreateAsciiAtom("lsh");
  public static final Atom wordRshl = Atom.findOrCreateAsciiAtom("rshl");
  public static final Atom wordRsha = Atom.findOrCreateAsciiAtom("rsha");

  // atoms related to WordArray, AddressArray, OffsetArray, ExtentArray, CodeArray
  public static final Atom addressArrayCreate = Atom.findOrCreateAsciiAtom("create");
  public static final Atom addressArrayLength = Atom.findOrCreateAsciiAtom("length");
  public static final Atom addressArrayGet = Atom.findOrCreateAsciiAtom("get");
  public static final Atom addressArraySet = Atom.findOrCreateAsciiAtom("set");
  
  // Segment register manipulations
  public static final Atom setCS = Atom.findOrCreateAsciiAtom("setCS");
  public static final Atom cpuId = Atom.findOrCreateAsciiAtom("cpuId");
  public static final Atom rdMsr = Atom.findOrCreateAsciiAtom("readMSR");
  public static final Atom wrMsr = Atom.findOrCreateAsciiAtom("writeMSR");
  
  public static final Atom byteSwap = Atom.findOrCreateAsciiAtom("byteSwap");

  public static final Atom ioLoadByte = Atom.findOrCreateAsciiAtom("ioLoadByte");
  public static final Atom ioLoadChar = Atom.findOrCreateAsciiAtom("ioLoadChar");
  public static final Atom ioLoadShort = Atom.findOrCreateAsciiAtom("ioLoadShort");
  public static final Atom ioLoadInt = Atom.findOrCreateAsciiAtom("ioLoadInt");
  public static final Atom ioStore = Atom.findOrCreateAsciiAtom("ioStore");
  
  public static final Atom halt = Atom.findOrCreateAsciiAtom("halt");
  public static final Atom yield = Atom.findOrCreateAsciiAtom("yield");
  public static final Atom dceil = Atom.findOrCreateAsciiAtom("dceil");

  public static final Atom saveContext = Atom.findOrCreateAsciiAtom("saveContext");
  public static final Atom restoreContext = Atom.findOrCreateAsciiAtom("restoreContext");
  public static final Atom restoreThreadContextNoErrCode = Atom.findOrCreateAsciiAtom("restoreThreadContextNoErrCode");
  public static final Atom restoreThreadContextErrCode = Atom.findOrCreateAsciiAtom("restoreThreadContextErrCode");
  public static final Atom switchStack = Atom.findOrCreateAsciiAtom("switchStack");
  public static final Atom startThread = Atom.findOrCreateAsciiAtom("startThread");
  public static final Atom enableInterrupts = Atom.findOrCreateAsciiAtom("enableInterrupts");
  public static final Atom disableInterrupts = Atom.findOrCreateAsciiAtom("disableInterrupts");

  public static final Atom setIdt = Atom.findOrCreateAsciiAtom("setIdt");

  
}
