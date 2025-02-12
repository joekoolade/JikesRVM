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
package org.jikesrvm.ia32;

import static org.jikesrvm.ia32.BaselineConstants.WORDSIZE;
import static org.jikesrvm.ia32.RegisterConstants.NUM_VOLATILE_GPRS;
import static org.jikesrvm.runtime.JavaSizeConstants.BYTES_IN_DOUBLE;
import static org.jikesrvm.runtime.UnboxedSizeConstants.BYTES_IN_ADDRESS;
import static org.jikesrvm.runtime.UnboxedSizeConstants.LOG_BYTES_IN_ADDRESS;

import org.jikesrvm.VM;
import org.vmmagic.unboxed.Address;
import org.vmmagic.unboxed.Offset;

/**
 * <pre>
 *-----------------------------------------------------------------------
 *                   Stackframe layout conventions - Intel version.
 *-----------------------------------------------------------------------
 * </pre>
 * A stack is an array of "slots", declared formally as integers, each slot
 * containing either a primitive (byte, int, float, etc), an object pointer,
 * a machine code pointer (a return address pointer), or a pointer to another
 * slot in the same stack (a frame pointer). The interpretation of a slot's
 * contents depends on the current value of IP, the machine instruction
 * address register.
 * Each machine code generator provides maps, for use by the garbage collector,
 * that tell how to interpret the stack slots at "safe points" in the
 * program's execution.
 * <p>
 * Here's a picture of what a stack might look like in memory.
 * <p>
 * Note: this (array) object is drawn upside down compared to other objects
 * because the hardware stack grows from high memory to low memory, but
 * array objects are layed out from low memory to high (header first).
 * <pre>
 *  hi-memory
 *              +---------------+                                            ...
 *              |     IP=0      |                                             .
 *              +---------------+                                             .
 *          +-&gt; |     FP=0      |   &lt;-- "end of vm stack" sentinel            .
 *          |   +---------------+                                             . caller's frame
 *          |   |    cmid=0      |   &lt;-- "invisible method" id                .
 *          |   +---------------+                                          ---.
 *          |   |   parameter0  |  \                                        | .
 *          |   +---------------+   \ parameter area                        | .
 *          |   |   parameter1  |   /  (== caller's operand stack area)     | .
 *   ---    |   +---------------+  /                                        |...
 *    |     |   |   saved IP    |  &lt;-- return address (in caller)           |
 *    |      \  +---------------+                                           |
 *  header FP-&gt; |   saved FP    |  &lt;-- this frame's caller's frame          |
 *    |         +---------------+                                           |
 *    |         |    cmid       |  &lt;-- this frame's compiledmethod id       |
 *    |         +---------------+                                           |
 *    |         |   saved GPRs  |  \                                        |
 *    |         +---------------+   \ nonvolatile register save area        |
 *    |         |   saved FPRS  |   /                                       | frame
 *    |         +---------------+                                           |
 *    |         |   local0      |  \                                        |
 *   body       +---------------+   \_local variables area                  |
 *    |         |   local1      |   /                                       |
 *    |         +---------------+  /                                        |
 *    |         |   operand0    |  \                                        |
 *    |         +---------------+   \_operand stack area                    |
 *    |    SP-&gt; |   operand1    |   /                                       |
 *    |         +---------------+  /                                        |
 *    |         |     ...       |                                           |
 *   ---        +===============+                                          ---
 *              |     ...       |
 *              +---------------+
 * stackLimit-&gt; |     ...       | \
 *              +---------------+  \_guard region for detecting &amp; processing stack overflow
 *              |     ...       |  /
 *              +---------------+ /
 *              |(object header)|
 *  low-memory  +---------------+
 * </pre>
 *
 *
 *  The opt compiler uses a different stackframe layout
 * <pre>
 *  hi-memory
 *              +---------------+                                            ...
 *              |     IP=0      |                                             .
 *              +---------------+                                             .
 *          +-&gt; |     FP=0      |   &lt;-- "end of vm stack" sentinel           .
 *          |   +---------------+                                             . caller's frame
 *          |   |    cmid=-1    |   &lt;-- "invisible method" id                .
 *          |   +---------------+                                          ---.
 *          |   |   parameter0  |  \                                        | .
 *          |   +---------------+   \ parameter area                        | .
 *          |   |   parameter1  |   /  (== caller's operand stack area)     | .
 *   ---    |   +---------------+  /                                        |...
 *    |     |   |   saved IP    |  &lt;-- return address (in caller)           |
 *    |      \  +---------------+                                           |
 *  header FP-&gt; |   saved FP    |  &lt;-- this frame's caller's frame          |
 *    |         +---------------+                                           |
 *    |         |    cmid       |  &lt;-- this frame's compiledmethod id       |
 *   ---        +---------------+                                           |
 *    |         |               |                                           |
 *    |         |  Spill Area   |  &lt;-- spills and other method-specific     |
 *    |         |     ...       |      compiler-managed storage             |
 *    |         +---------------+                                           |
 *    |         |   Saved FP    |     only SaveVolatile Frames              |
 *    |         |    State      |                                           |
 *    |         +---------------+                                           |
 *    |         |  VolGPR[0]    |                                           |
 *    |         |     ...       |     only SaveVolatile Frames              |
 *    |         |  VolGPR[n]    |                                           |
 *    |         +---------------+                                           |
 *   body       |  NVolGPR[k]   |  &lt;-- info.getUnsignedNonVolatileOffset()  | frame
 *    |         |     ...       |   k == info.getFirstNonVolatileGPR()      |
 *    |         |  NVolGPR[n]   |                                           |
 *    |         +---------------+                                           |
 *    |         |  NVolFPR[k]   |                                           |
 *    |         |     ...       |   k == info.getFirstNonVolatileFPR()      |
 *    |         |  NVolFPR[n]   |                                           |
 *    |         +---------------+                                           |
 *    |         |   parameter0  |  \                                        |
 *    |         +---------------+   \_parameters to callee frame            |
 *    |    SP-&gt; |   parameter1  |   /                                       |
 *    |         +---------------+  /                                        |
 *    |         |     ...       |                                           |
 *   ---        +===============+                                          ---
 *              |     ...       |
 *              +---------------+
 * stackLimit-&gt; |     ...       | \
 *              +---------------+  \_guard region for detecting &amp; processing stack overflow
 *              |     ...       |  /
 *              +---------------+ /
 *              |(object header)|
 *  low-memory  +---------------+
 *
 * </pre>
 */
public final class StackframeLayoutConstants {

  public static final int LOG_BYTES_IN_STACKSLOT = LOG_BYTES_IN_ADDRESS;
  public static final int BYTES_IN_STACKSLOT = 1 << LOG_BYTES_IN_STACKSLOT;

  /** offset of caller's return address from FP */
  public static final Offset STACKFRAME_RETURN_ADDRESS_OFFSET = Offset.fromIntSignExtend(WORDSIZE);
  /** base of this frame */
  public static final Offset STACKFRAME_FRAME_POINTER_OFFSET = Offset.zero();
  /** offset of method id from FP */
  public static final Offset STACKFRAME_METHOD_ID_OFFSET = Offset.fromIntSignExtend(-WORDSIZE);
  /** offset of work area from FP */
  public static final Offset STACKFRAME_BODY_OFFSET = Offset.fromIntSignExtend(-2 * WORDSIZE);
  /** size of frame header, in bytes */
  public static final int STACKFRAME_HEADER_SIZE = 3 * WORDSIZE;

  /** space to save entire FPU state.  The FPU state is saved only for 'bridge' frames */
  public static final int X87_FPU_STATE_SIZE = 108;
  /** Baseline compiler: currently only use the low 8 bytes, only use 4 SSE2 params */
  public static final int BASELINE_XMM_STATE_SIZE = 8 * 4;

  /** Optimizing compiler: space for volatile GPRs in opt save volatile frames */
  public static final int OPT_SAVE_VOLATILE_SPACE_FOR_VOLATILE_GPRS = BYTES_IN_ADDRESS * NUM_VOLATILE_GPRS;
  /** Optimizing compiler: space for FPU state in opt save volatile frames */
  public static final int OPT_SAVE_VOLATILE_SPACE_FOR_FPU_STATE = (VM.BuildForSSE2Full) ?
      (8 * BYTES_IN_DOUBLE) : X87_FPU_STATE_SIZE;
  /* Optimizing compiler: total space for saving of volatile registers */
  public static final int OPT_SAVE_VOLATILE_TOTAL_SIZE = OPT_SAVE_VOLATILE_SPACE_FOR_VOLATILE_GPRS +
      OPT_SAVE_VOLATILE_SPACE_FOR_FPU_STATE;

  /** fp value indicating end of stack walkback */
  public static final Address STACKFRAME_SENTINEL_FP = Address.fromIntSignExtend(-2);
  /** marker for "assembler" frames that have no associated RVMMethod */
  public static final int INVISIBLE_METHOD_ID = -1;
  public static final int INTERRUPT_METHOD_ID = -2;     // an interrupt frame  
  public static final int THREAD_START_METHOD_ID = -3;  // thread starup frame

  // Stackframe alignment.
  // Align to 8 byte boundary for good floating point save/restore performance (on powerPC, anyway).
  //
  public static final int STACKFRAME_ALIGNMENT = 8;

  // Sizes for stacks and subregions thereof.
  // Values are in bytes and must be a multiple of WORDSIZE (size of a stack slot).
  //
  /** how much to grow stack when overflow detected */
  public static final int STACK_SIZE_GROW = (VM.BuildFor64Addr ? 16 : 8) * 1024;
  /** max space needed for stack overflow trap processing */
  public static final int STACK_SIZE_GUARD = 64 * 1024;
  /** max space needed for any native code called by vm */
  public static final int STACK_SIZE_SYSCALL = (VM.BuildFor64Addr ? 8 : 4) * 1024;
  /** max space needed for dlopen sys call */
  public static final int STACK_SIZE_DLOPEN = 30 * 1024;
  /** max space needed while running with gc disabled */
  public static final int STACK_SIZE_GCDISABLED = (VM.BuildFor64Addr ? 8 : 4) * 1024;

   // Complications:
   // - STACK_SIZE_GUARD must be greater than STACK_SIZE_NATIVE or STACK_SIZE_GCDISABLED
   //   to ensure that frames allocated by stack growing code will fit within guard region.
   // - STACK_SIZE_GROW must be greater than STACK_SIZE_NATIVE or STACK_SIZE_GCDISABLED
   //   to ensure that, if stack is grown prior to disabling gc or calling native code,
   //   the new stack will accommodate that code without generating a stack overflow trap.
   // - Values chosen for STACK_SIZE_NATIVE and STACK_SIZE_GCDISABLED are pure guesswork
   //   selected by trial and error.

   // Stacks for "normal" threads grow as needed by trapping on guard region.
   // Stacks for "boot" and "collector" threads are fixed in size and cannot grow.
   //

  /** initial stack space to allocate for normal thread (includes guard region) */
  public static final int STACK_SIZE_NORMAL =
      STACK_SIZE_GUARD +
      STACK_SIZE_GCDISABLED +
      200 * 1024;
  /** total stack space to allocate for boot thread (includes guard region) */
  public static final int STACK_SIZE_BOOT =
      STACK_SIZE_GUARD +
      STACK_SIZE_GCDISABLED +
      30 * 1024;
  /** total stack space to allocate for collector thread (includes guard region) */
  public static final int STACK_SIZE_COLLECTOR =
      STACK_SIZE_GUARD +
      STACK_SIZE_GCDISABLED +
      20 * 1024;
  /** upper limit on stack size (includes guard region) */
  public static final int STACK_SIZE_MAX =
      STACK_SIZE_GUARD + STACK_SIZE_GCDISABLED + 200 * 1024;

  public static final int STACK_SIZE_JNINATIVE_GROW = 0; // TODO!!;

  /**
   * The
   * "System V Application Binary Interface AMD64 Architecture Processor Supplement"
   * mandates that signal and interrupt handlers don't modify the
   * 128 bytes beyond the stack pointer. This area is known as
   * the red zone.
   */
  public static final int RED_ZONE_SIZE = 128;

  private StackframeLayoutConstants() {
    // prevent instantiation
  }
}
