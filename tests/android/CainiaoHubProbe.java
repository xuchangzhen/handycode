package com.fairycrane.handycode.tests;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.ValueCallback;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Presentation and lifecycle only; does not inspect or change account data. */
public final class CainiaoHubProbe extends Instrumentation {
    private Activity activity;
    private int checks;
    private Object field(Object o, String name) throws Exception { Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o); }
    private Object call(Object o, String name, Class<?>[] types, Object... values) throws Exception {
        Method m=o.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(o,values);
    }
    private void check(boolean v,String message) { checks++; if(!v)throw new AssertionError(message); }
    private void ui(Runnable work) { runOnMainSync(work); }
    @Override public void onCreate(Bundle args) { super.onCreate(args);start(); }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            Intent i=new Intent().setClassName("com.fairycrane.handycode","com.fairycrane.handycode.MainActivity")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK).putExtra("provider","cainiao");
            ActivityMonitor monitor=addMonitor("com.fairycrane.handycode.MainActivity",null,false);
            getTargetContext().startActivity(i); activity=waitForMonitorWithTimeout(monitor,10000);removeMonitor(monitor);
            check(activity!=null,"Activity started");
            final Throwable[] failure={null};
            ui(()->{try {
                Object hub=field(activity,"cainiaoHub"), packages=field(hub,"packages"), identity=field(hub,"identity");
                check(!(Boolean)field(hub,"showingIdentity"),"Parcels are selected by default");
                check(((View)packages).getVisibility()==View.VISIBLE && ((View)identity).getVisibility()==View.GONE,"Only parcel page is visible");
                check((Boolean)field(packages,"active"),"Parcel WebView is active");
                Object original=field(packages,"web");check(original!=null,"Parcel WebView initialized");
                ((View)field(hub,"identityTab")).performClick();
                check((Boolean)field(hub,"showingIdentity") && ((View)identity).getVisibility()==View.VISIBLE,"Identity remains accessible");
                check(!(Boolean)field(packages,"active"),"Parcel page pauses when identity is shown");
                ((View)field(hub,"parcelsTab")).performClick();
                check(!(Boolean)field(hub,"showingIdentity") && (Boolean)field(packages,"active"),"Returning resumes parcel page");
                check(field(packages,"web")==original,"Switching retains the official WebView");
                ((View)field(hub,"identityTab")).performClick();
                check((Boolean)call(hub,"goBack",new Class<?>[]{}),"Back handles the identity page");
                check(!(Boolean)field(hub,"showingIdentity"),"Back returns to the parcel page");
                call(hub,"resetPackages",new Class<?>[]{});
                check(field(packages,"web")==null,"Logout reset releases the previous parcel page");
                call(hub,"showPage",new Class<?>[]{});
                check(field(packages,"web")!=null && field(packages,"web")!=original,"A later visit recreates the page");
            }catch(Throwable e){failure[0]=e;}});
            if(failure[0]!=null)throw failure[0];
            Object hub=field(activity,"cainiaoHub"); CountDownLatch done=new CountDownLatch(1);String[] visible={"pending"};
            ui(()->{try{call(hub,"checkVisibleScanCode",new Class<?>[]{ValueCallback.class},(ValueCallback<String>)s->{visible[0]=s;done.countDown();});}catch(Exception e){throw new RuntimeException(e);}});
            check(done.await(3,TimeUnit.SECONDS) && "false".equals(visible[0]),"Parcel page never claims a visible identity code");
            result.putString("stream","PASS: "+checks+" Cainiao hub navigation, lifecycle and barcode-visibility checks\n");
        } catch(Throwable e) { result.putString("failure",e.toString());result.putString("stream","FAIL: "+e+"\n"); }
        finish(result.containsKey("failure")?0:-1,result);
    }
}
