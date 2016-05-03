package edu.stanford.stamp.test.sootdextest;

import android.app.Activity;
import android.os.Bundle;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class MainActivity extends Activity
{

	public static byte[] decode(byte[] source) {

		byte[] a = new byte[1];

		byte b = (byte) (source[0] & 0x7f);
		a[0] = b;

		return a;
	}
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }
}

/* Jimple:

    public static byte[] decode(byte[])
    {
        byte[] $r0, $r1;
        byte $b0;

        $r0 := @parameter0: byte[];
        $r1 = newarray (byte)[1];
        $b0 = $r0[0];
        $b0 = $b0 & 127;
        $b0 = (byte) $b0;
        $r1[0] = $b0;
        return $r1;
    }
 
*/

/* DEX:

    #1              : (in Ledu/stanford/stamp/test/sootdextest/MainActivity;)
      name          : 'decode'
      type          : '([B)[B'
      access        : 0x0009 (PUBLIC STATIC)
      code          -
      registers     : 4
      ins           : 1
      outs          : 0
      insns size    : 12 16-bit code units
001824:                                        |[001824] edu.stanford.stamp.test.sootdextest.MainActivity.decode:([B)[B
001834: 1202                                   |0000: const/4 v2, #int 0 // #0
001836: 1210                                   |0001: const/4 v0, #int 1 // #1
001838: 2300 2b00                              |0002: new-array v0, v0, [B // type@002b
00183c: 4801 0302                              |0004: aget-byte v1, v3, v2
001840: dd01 017f                              |0006: and-int/lit8 v1, v1, #int 127 // #7f
001844: 0000                                   |0008: nop // spacer
001846: 4f01 0002                              |0009: aput-byte v1, v0, v2
00184a: 1100                                   |000b: return-object v0

*/

/* Runtime error:

W/dalvikvm( 2261): VFY: invalid reg type 17 on aput instr (need 13)
W/dalvikvm( 2261): VFY:  rejecting opcode 0x4f at 0x0009
W/dalvikvm( 2261): VFY:  rejected Ledu/stanford/stamp/test/sootdextest/MainActivity;.decode ([B)[B
W/dalvikvm( 2261): Verifier rejected class Ledu/stanford/stamp/test/sootdextest/MainActivity;
W/dalvikvm( 2261): Class init failed in newInstance call (Ledu/stanford/stamp/test/sootdextest/MainActivity;)
D/AndroidRuntime( 2261): Shutting down VM
W/dalvikvm( 2261): threadid=1: thread exiting with uncaught exception (group=0xb1ab4ba8)
E/AndroidRuntime( 2261): FATAL EXCEPTION: main
E/AndroidRuntime( 2261): Process: edu.stanford.stamp.test.sootdextest, PID: 2261
E/AndroidRuntime( 2261): java.lang.VerifyError: edu/stanford/stamp/test/sootdextest/MainActivity
E/AndroidRuntime( 2261): 	at java.lang.Class.newInstanceImpl(Native Method)
E/AndroidRuntime( 2261): 	at java.lang.Class.newInstance(Class.java:1208)
E/AndroidRuntime( 2261): 	at android.app.Instrumentation.newActivity(Instrumentation.java:1061)
E/AndroidRuntime( 2261): 	at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:2112)
E/AndroidRuntime( 2261): 	at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:2245)
E/AndroidRuntime( 2261): 	at android.app.ActivityThread.access$800(ActivityThread.java:135)
E/AndroidRuntime( 2261): 	at android.app.ActivityThread$H.handleMessage(ActivityThread.java:1196)
E/AndroidRuntime( 2261): 	at android.os.Handler.dispatchMessage(Handler.java:102)
E/AndroidRuntime( 2261): 	at android.os.Looper.loop(Looper.java:136)
E/AndroidRuntime( 2261): 	at android.app.ActivityThread.main(ActivityThread.java:5017)
E/AndroidRuntime( 2261): 	at java.lang.reflect.Method.invokeNative(Native Method)
E/AndroidRuntime( 2261): 	at java.lang.reflect.Method.invoke(Method.java:515)
E/AndroidRuntime( 2261): 	at com.android.internal.os.ZygoteInit$MethodAndArgsCaller.run(ZygoteInit.java:779)
E/AndroidRuntime( 2261): 	at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:595)
E/AndroidRuntime( 2261): 	at dalvik.system.NativeStart.main(Native Method)
W/ActivityManager(  392):   Force finishing activity edu.stanford.stamp.test.sootdextest/.MainActivity

*/
